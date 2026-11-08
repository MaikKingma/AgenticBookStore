package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domain.OrderPlacedEvent;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The orders end to end: real HTTP into the full application on PostgreSQL. Every test starts on an empty database with
 * empty event recorders, so the events a test sees are the ones its own requests delivered. Bodies are read as plain
 * JSON maps, so money and instants are compared as the strings the views give and counts as numbers.
 *
 * <p>The pack scenarios add their books through the catalogue and receive stock through the inventory, as a client
 * would. Tests that claim an order over fixed books seed the books with fixed ids by SQL instead, so that no attribute of
 * those books (id, isbn, title, price, quantity, line total, stock) runs in the asserted order in either direction.
 */
class OrderIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_ARRAY =
            new ParameterizedTypeReference<>() {
            };

    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    /** The application clock that stamps orders and events; bracketing with it keeps the tests valid under any clock. */
    @Autowired
    private Clock clock;

    @Test
    void placingAnOrderReservesStockAndPublishesOneOrderPlacedEventAfterCommit() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 2);
        Instant before = clock.instant();

        UUID id = placeOrder("Reader@Example.com", List.of(line(a, 2), line(b, 1)));

        Instant after = clock.instant();
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("id")).isEqualTo(id.toString());
        assertThat(order.get("customerEmail")).isEqualTo("reader@example.com");
        assertThat(order.get("total")).isEqualTo("33.00");
        assertThat(order.get("status")).isEqualTo("PLACED");
        assertThat(order).containsEntry("shippedAt", null);
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 2, 3, true));
        assertThat(getStock(b)).isEqualTo(stockView(b, 2, 1, 1, true));

        Instant placedAt = Instant.parse((String) order.get("placedAt"));
        assertThat(placedAt).isBetween(before.truncatedTo(ChronoUnit.MICROS), after);
        assertThat(orderEvents.committed())
                .containsExactly(new OrderPlacedEvent(id, "reader@example.com", new BigDecimal("33.00"), placedAt));
        assertThat(orderEvents.committed().getFirst().occurredAt())
                .isBetween(before.truncatedTo(ChronoUnit.MICROS), after);
        assertThat(orderEvents.rolledBack()).isEmpty();

        // A ends at 3 and B at 1 available, both at or below the reorder level 3: each signals once, in book-id order.
        assertThat(stockEvents.committed()).hasSize(2);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId)
                .containsExactlyElementsOf(inBookIdOrder(a, b));
        assertThat(stockEvents.committed()).filteredOn(event -> event.bookId().equals(a)).singleElement()
                .satisfies(event -> {
                    assertThat(event.available()).isEqualTo(3);
                    assertThat(event.reorderQuantity()).isEqualTo(10);
                    assertThat(event.occurredAt()).isBetween(before, after);
                });
        assertThat(stockEvents.committed()).filteredOn(event -> event.bookId().equals(b)).singleElement()
                .satisfies(event -> {
                    assertThat(event.available()).isEqualTo(1);
                    assertThat(event.reorderQuantity()).isEqualTo(10);
                    assertThat(event.occurredAt()).isBetween(before, after);
                });
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void orderViewExposesExactlyTheOrderViewFields() {
        // Request order Pride, Sense, Emma. In that order the book id (5, 1, 3), isbn (518, 662, 587), title
        // (P, S, E), unit price (7.25, 11.40, 6.30, also as strings), quantity (3, 1, 2), line total (21.75, 11.40,
        // 12.60, also as strings) and stock on hand (12, 8, 10) each go up and down, so only the request order
        // explains the order of the lines.
        UUID pride = UUID.fromString("50000000-0000-4000-8000-000000000001");
        UUID sense = UUID.fromString("10000000-0000-4000-8000-000000000002");
        UUID emma = UUID.fromString("30000000-0000-4000-8000-000000000003");
        seedBook(pride, "9780141439518", "Pride and Prejudice", "7.25", 12, 0);
        seedBook(sense, "9780141439662", "Sense and Sensibility", "11.40", 8, 0);
        seedBook(emma, "9780141439587", "Emma", "6.30", 10, 0);

        UUID id = placeOrder("Avid.Reader@Example.net", List.of(line(pride, 3), line(sense, 1), line(emma, 2)));
        setPlacedAt(id, "2026-06-15T10:00:00.123456Z");

        Map<String, Object> order = getOrder(id);

        assertThat(order.keySet()).containsExactlyInAnyOrder(
                "id", "customerEmail", "status", "placedAt", "shippedAt", "lines", "total", "currency");
        assertThat(order).extractingByKey("lines", InstanceOfAssertFactories.LIST)
                .allSatisfy(line -> assertThat(line).asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsOnlyKeys("isbn", "title", "quantity", "unitPrice", "lineTotal"));
        assertThat(order).isEqualTo(orderView(id, "avid.reader@example.net", "PLACED",
                "2026-06-15T10:00:00.123456Z", null,
                List.of(
                        lineView("9780141439518", "Pride and Prejudice", 3, "7.25", "21.75"),
                        lineView("9780141439662", "Sense and Sensibility", 1, "11.40", "11.40"),
                        lineView("9780141439587", "Emma", 2, "6.30", "12.60")),
                "45.75", "EUR"));
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::customerEmail)
                .containsExactly("avid.reader@example.net");
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::total)
                .containsExactly(new BigDecimal("45.75"));
        assertThat(orderEvents.rolledBack()).isEmpty();
        // Every book stays above its reorder level, so no stock event.
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void orderKeepsThePriceItWasPlacedAt() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID id = placeOrder("Second.Reader@Example.org", List.of(line(a, 2)));

        restTestClient.post().uri("/books/{id}/commands/change-price", a)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("price", "15.00"))
                .exchange()
                .expectStatus().isAccepted();

        assertThat(getBook(a).get("price")).isEqualTo("15.00");
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("customerEmail")).isEqualTo("second.reader@example.org");
        assertThat(order.get("lines"))
                .isEqualTo(List.of(lineView("9780141439600", "A Tale of Two Cities", 2, "12.50", "25.00")));
        assertThat(order.get("total")).isEqualTo("25.00");
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void severalShortagesAreReportedTogetherAndNothingIsReserved() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 1);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");

        Map<String, Object> problem = problem(place("reader@example.com", List.of(line(a, 2), line(b, 1))), 409);

        assertThat(problem.get("code")).isEqualTo("insufficient-stock");
        assertThat(problem.get("title")).isEqualTo("Insufficient stock");
        assertThat(problem.get("detail")).asString().isNotBlank();
        // Two books, as the scenario gives them; the request-order rule over three books is the next test.
        assertThat(problem.get("details")).isEqualTo(List.of(
                Map.of("bookId", a.toString(), "requested", "2", "available", "1"),
                Map.of("bookId", b.toString(), "requested", "1", "available", "0")));
        assertThat(getStock(a)).isEqualTo(stockView(a, 1, 0, 1, true));
        assertThat(getStock(b)).isEqualTo(stockView(b, 0, 0, 0, true));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void shortagesAreReportedInRequestOrderNamingOnlyTheShortBooks() {
        // Request order Mathilda (short), Wuthering Heights (enough), A Tale of Two Cities (short), The History of
        // England (short). Over the three short books the book id (4, 2, 6), isbn (747, 600, 617), title (M, A, T),
        // price (5.10, 3.20, 9.90), requested (5, 9, 6), available (2, 4, 1), on hand (6, 7, 6) and reserved
        // (4, 3, 5) each go up and down, so only the request order explains the order of the details.
        UUID mathilda = UUID.fromString("40000000-0000-4000-8000-000000000001");
        UUID wuthering = UUID.fromString("10000000-0000-4000-8000-000000000002");
        UUID tale = UUID.fromString("20000000-0000-4000-8000-000000000003");
        UUID history = UUID.fromString("60000000-0000-4000-8000-000000000004");
        seedBook(mathilda, "9780141439747", "Mathilda", "5.10", 6, 4);
        seedBook(wuthering, "9780141439563", "Wuthering Heights", "8.00", 8, 0);
        seedBook(tale, "9780141439600", "A Tale of Two Cities", "3.20", 7, 3);
        seedBook(history, "9780141439617", "The History of England", "9.90", 6, 5);

        Map<String, Object> problem = problem(place("reader@example.com",
                List.of(line(mathilda, 5), line(wuthering, 1), line(tale, 9), line(history, 6))), 409);

        assertThat(problem.get("code")).isEqualTo("insufficient-stock");
        assertThat(problem.get("title")).isEqualTo("Insufficient stock");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of(
                Map.of("bookId", mathilda.toString(), "requested", "5", "available", "2"),
                Map.of("bookId", tale.toString(), "requested", "9", "available", "4"),
                Map.of("bookId", history.toString(), "requested", "6", "available", "1")));
        assertThat(getStock(mathilda)).isEqualTo(stockView(mathilda, 6, 4, 2, true));
        assertThat(getStock(wuthering)).isEqualTo(stockView(wuthering, 8, 0, 8, false));
        assertThat(getStock(tale)).isEqualTo(stockView(tale, 7, 3, 4, false));
        assertThat(getStock(history)).isEqualTo(stockView(history, 6, 5, 1, true));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void duplicateLinesAreMergedIntoOneLine() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 8);

        UUID id = placeOrder("reader@example.com", List.of(line(a, 3), line(a, 3)));

        Map<String, Object> order = getOrder(id);
        assertThat(order.get("lines"))
                .isEqualTo(List.of(lineView("9780141439600", "A Tale of Two Cities", 6, "12.50", "75.00")));
        assertThat(order.get("total")).isEqualTo("75.00");
        assertThat(countRows("order_line")).isEqualTo(1);
        assertThat(getStock(a)).isEqualTo(stockView(a, 8, 6, 2, true));
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::available).containsExactly(2);
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void aMergedQuantityAbove10IsRejectedWith422AndNothingIsReserved() {
        // 20 on hand: the stock alone would allow the merged 12, so only the merged quantity rule rejects it.
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 20);

        Map<String, Object> problem = problem(place("reader@example.com", List.of(line(a, 6), line(a, 6))), 422);

        assertFieldProblem(problem, "quantity-out-of-range", "Quantity out of range", "lines");
        assertThat(getStock(a)).isEqualTo(stockView(a, 20, 0, 20, false));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void aDiscontinuedBookIsRejectedWith409AndNothingIsReserved() {
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 4);
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        restTestClient.post().uri("/books/{id}/commands/discontinue", a).exchange().expectStatus().isAccepted();

        Map<String, Object> problem = problem(place("reader@example.com", List.of(line(b, 1), line(a, 2))), 409);

        assertProblem(problem, "book-not-orderable", "Book not orderable");
        assertThat(getStock(b)).isEqualTo(stockView(b, 4, 0, 4, false));
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 0, 5, false));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void anUnknownBookIsRejectedWith404() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);

        Map<String, Object> problem = problem(place("reader@example.com", List.of(line(a, 2), line(UNKNOWN_ID, 1))), 404);

        assertProblem(problem, "book-not-found", "Book not found");
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 0, 5, false));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void invalidPlacePayloadIsRejectedWith400() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);

        Map<String, Object> badEmail = problem(place("reader", List.of(line(a, 2))), 400);
        Map<String, Object> badQuantity = problem(place("reader@example.com", List.of(line(a, 0))), 400);

        assertValidationFailed(badEmail, "customerEmail");
        assertValidationFailed(badQuantity, "lines[0].quantity");
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 0, 5, false));
        assertNothingStoredAndNoEvent();
    }

    @Test
    void shippingTurnsReservationsIntoStockReductions() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID id = placeOrder("reader@example.com", List.of(line(a, 2)));
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 2, 3, true));
        Instant before = clock.instant();

        ship(id).expectStatus().isAccepted().expectBody().isEmpty();

        Instant after = clock.instant();
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("status")).isEqualTo("SHIPPED");
        assertThat(Instant.parse((String) order.get("shippedAt")))
                .isBetween(before.truncatedTo(ChronoUnit.MICROS), after);
        assertThat(getStock(a)).isEqualTo(stockView(a, 3, 0, 3, true));
        assertThat(version(id)).isEqualTo(1L);
        // Shipping registers no event: the order event and the stock signal are the ones of the placement.
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void shippingTwiceIsRejectedWith409AndChangesNothing() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID id = placeOrder("reader@example.com", List.of(line(a, 2)));
        ship(id).expectStatus().isAccepted();
        Object shippedAt = getOrder(id).get("shippedAt");
        long version = version(id);

        Map<String, Object> problem = problem(ship(id), 409);

        assertProblem(problem, "order-already-shipped", "Order already shipped");
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("status")).isEqualTo("SHIPPED");
        assertThat(order.get("shippedAt")).isNotNull().isEqualTo(shippedAt);
        assertThat(version(id)).isEqualTo(version);
        assertThat(getStock(a)).isEqualTo(stockView(a, 3, 0, 3, true));
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void unknownOrderIsNotFoundOnShipAndRead() {
        Map<String, Object> onShip = problem(ship(UNKNOWN_ID), 404);
        Map<String, Object> onRead = problem(restTestClient.get().uri("/orders/{id}", UNKNOWN_ID).exchange(), 404);

        assertProblem(onShip, "order-not-found", "Order not found");
        assertProblem(onRead, "order-not-found", "Order not found");
        assertNothingStoredAndNoEvent();
    }

    @Test
    void orderHistoryMatchesTheEmailCaseInsensitivelyNewestFirst() {
        UUID a = UUID.fromString("40000000-0000-4000-8000-000000000004");
        UUID b = UUID.fromString("70000000-0000-4000-8000-000000000005");
        UUID c = UUID.fromString("10000000-0000-4000-8000-000000000006");
        UUID d = UUID.fromString("30000000-0000-4000-8000-000000000007");
        seedBook(a, "9780141439600", "Pride and Prejudice", "12.50", 20, 0);
        seedBook(b, "9780141439747", "Wuthering Heights", "15.00", 20, 0);
        seedBook(c, "9780141439518", "Emma", "4.25", 20, 0);
        seedBook(d, "9780141439662", "Sense and Sensibility", "8.00", 20, 0);
        UUID first = placeOrder("Reader@Example.com", List.of(line(d, 1)));
        UUID second = placeOrder("Reader@Example.com", List.of(line(a, 2)));
        UUID third = placeOrder("Reader@Example.com", List.of(line(c, 1), line(b, 4)));
        UUID otherCustomers = placeOrder("Other.Reader@Example.com", List.of(line(a, 1)));
        ship(third).expectStatus().isAccepted();
        // Newest first is second, third, first. In that order the placement order (2, 3, 1), item count (2, 5, 1),
        // total (25.00, 64.25, 8.00, also as strings), line count (1, 2, 1), status (PLACED, SHIPPED, PLACED) and
        // shipped time (none, set, none) each go up and down. Each listed order has its own books, and third holds
        // both the smallest and the largest book id (1, 7), isbn (518, 747), title (Emma, Wuthering Heights) and unit
        // price (4.25, 15.00) of all lines, and the largest quantity (4). So no line column, sorted either way over
        // the joined rows, gives second, third, first; only the placed time explains the order. The other customer's
        // order is the newest of all and must still be left out.
        setPlacedAt(first, "2026-06-15T10:00:00.123456Z");
        setPlacedAt(second, "2026-06-17T09:15:00.654321Z");
        setPlacedAt(third, "2026-06-16T14:30:00.000123Z");
        setPlacedAt(otherCustomers, "2026-06-18T08:00:00.987654Z");
        List<Map<String, Object>> expected = List.of(
                summaryView(second, "PLACED", "2026-06-17T09:15:00.654321Z", 2, "25.00"),
                summaryView(third, "SHIPPED", "2026-06-16T14:30:00.000123Z", 5, "64.25"),
                summaryView(first, "PLACED", "2026-06-15T10:00:00.123456Z", 1, "8.00"));

        List<Map<String, Object>> lowerCase = history("/orders?customerEmail=reader@example.com");
        List<Map<String, Object>> upperCase = history("/orders?customerEmail=READER@EXAMPLE.COM");

        assertThat(lowerCase).extracting(view -> view.get("id"))
                .containsExactly(second.toString(), third.toString(), first.toString());
        assertThat(lowerCase).isEqualTo(expected);
        assertThat(upperCase).isEqualTo(expected);
        assertThat(jdbcTemplate.queryForList("select distinct customer_email from customer_order where id in (?, ?, ?)",
                String.class, first, second, third)).containsExactly("reader@example.com");
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId)
                .containsExactly(first, second, third, otherCustomers);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void orderSummaryExposesExactlyTheSummaryFields() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 2);
        UUID id = placeOrder("Reader@Example.com", List.of(line(a, 2), line(b, 1)));
        setPlacedAt(id, "2026-06-14T08:45:30.250001Z");

        List<Map<String, Object>> summaries = history("/orders?customerEmail=reader@example.com");

        assertThat(summaries).singleElement().satisfies(summary -> {
            assertThat(summary.keySet()).containsExactlyInAnyOrder("id", "status", "placedAt", "itemCount", "total");
            assertThat(summary).isEqualTo(summaryView(id, "PLACED", "2026-06-14T08:45:30.250001Z", 3, "33.00"));
        });
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId)
                .containsExactlyElementsOf(inBookIdOrder(a, b));
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void orderHistoryOfACustomerWithoutOrdersIsEmpty() {
        UUID a = UUID.fromString("20000000-0000-4000-8000-000000000004");
        seedBook(a, "9780141439518", "Pride and Prejudice", "12.50", 20, 0);
        UUID id = placeOrder("reader@example.com", List.of(line(a, 1)));

        assertThat(history("/orders?customerEmail=nobody@example.com")).isEmpty();
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void orderHistoryWithoutCustomerEmailIsRejectedWith400() {
        Map<String, Object> problem = problem(restTestClient.get().uri("/orders").exchange(), 400);

        assertThat(problem.get("code")).isEqualTo("validation-failed");
        assertThat(problem.get("title")).isEqualTo("Validation failed");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of(Map.of("field", "customerEmail", "message", "is required")));
    }

    @Test
    void orderHistoryWithAnInvalidEmailIsRejectedWith422() {
        List<String> uris = List.of(
                "/orders?customerEmail=not-an-email",
                "/orders?customerEmail=",
                "/orders?customerEmail=a@b.co&customerEmail=c@d.co");

        for (String uri : uris) {
            Map<String, Object> problem = problem(restTestClient.get().uri(uri).exchange(), 422);

            assertThat(problem.get("code")).as(uri).isEqualTo("invalid-email");
            assertThat(problem.get("title")).as(uri).isEqualTo("Invalid e-mail address");
            assertThat(problem.get("detail")).as(uri).asString().isNotBlank();
            assertThat(problem).as(uri).extractingByKey("details", InstanceOfAssertFactories.LIST)
                    .singleElement(InstanceOfAssertFactories.MAP)
                    .containsOnlyKeys("field", "message")
                    .containsEntry("field", "customerEmail")
                    .extractingByKey("message").asString().isNotBlank();
        }
    }

    @Test
    void noOrderOrReservationSurvivesARollbackBeforeCommit() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 2);
        orderEvents.failBeforeCommit();

        // The failure is not a use-case exception, so Boot answers with its own error JSON, not a problem detail.
        place("reader@example.com", List.of(line(a, 2), line(b, 1))).expectStatus().isEqualTo(500);

        assertThat(countRows("customer_order")).isZero();
        assertThat(countRows("order_line")).isZero();
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 0, 5, false));
        assertThat(getStock(b)).isEqualTo(stockView(b, 2, 0, 2, true));
        assertThat(orderEvents.committed()).isEmpty();
        assertThat(orderEvents.rolledBack()).singleElement().satisfies(event -> {
            assertThat(event.orderId()).isNotNull();
            assertThat(event.customerEmail()).isEqualTo("reader@example.com");
            assertThat(event.total()).isEqualTo(new BigDecimal("33.00"));
        });
        UUID rolledBackId = orderEvents.rolledBack().getFirst().orderId();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).extracting(StockRunningLowEvent::bookId)
                .containsExactlyElementsOf(inBookIdOrder(a, b));

        // The signals rolled back with the stock: the same order now commits and both books signal again.
        orderEvents.reset();
        stockEvents.reset();
        UUID id = placeOrder("reader@example.com", List.of(line(a, 2), line(b, 1)));

        assertThat(id).isNotEqualTo(rolledBackId);
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(id);
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId)
                .containsExactlyElementsOf(inBookIdOrder(a, b));
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::available)
                .containsExactlyElementsOf(a.compareTo(b) < 0 ? List.of(3, 1) : List.of(1, 3));
        assertThat(stockEvents.rolledBack()).isEmpty();
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 2, 3, true));
        assertThat(getStock(b)).isEqualTo(stockView(b, 2, 1, 1, true));
    }

    @Test
    void aFailingStockSaveUndoesTheOrderAndEarlierReservations() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 2);
        // The first stock save of the order is real, the second one breaks.
        doCallRealMethod().doThrow(new RuntimeException("Simulated failure saving the second reserved stock item"))
                .when(stockItemDataService).save(any());

        place("reader@example.com", List.of(line(a, 2), line(b, 1))).expectStatus().isEqualTo(500);

        assertThat(countRows("customer_order")).isZero();
        assertThat(countRows("order_line")).isZero();
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 0, 5, false));
        assertThat(getStock(b)).isEqualTo(stockView(b, 2, 0, 2, true));
        assertThat(orderEvents.committed()).isEmpty();
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void aFailingStockSaveUndoesTheShipment() {
        UUID a = addBook("9780141439600", "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook("9780141439563", "Wuthering Heights", "8.00");
        receive(b, 2);
        UUID id = placeOrder("reader@example.com", List.of(line(a, 2), line(b, 1)));
        long version = version(id);
        orderEvents.reset();
        stockEvents.reset();
        // The first stock save of the shipment is real, the second one breaks.
        doCallRealMethod().doThrow(new RuntimeException("Simulated failure saving the second shipped stock item"))
                .when(stockItemDataService).save(any());

        ship(id).expectStatus().isEqualTo(500);

        Map<String, Object> order = getOrder(id);
        assertThat(order.get("status")).isEqualTo("PLACED");
        assertThat(order).containsEntry("shippedAt", null);
        assertThat(version(id)).isEqualTo(version);
        assertThat(getStock(a)).isEqualTo(stockView(a, 5, 2, 3, true));
        assertThat(getStock(b)).isEqualTo(stockView(b, 2, 1, 1, true));
        assertThat(orderEvents.committed()).isEmpty();
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    private void assertNothingStoredAndNoEvent() {
        assertThat(countRows("customer_order")).isZero();
        assertThat(countRows("order_line")).isZero();
        assertThat(orderEvents.committed()).isEmpty();
        assertThat(orderEvents.rolledBack()).isEmpty();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    /** Asserts a problem without details: the code, the title, a detail and an empty details list. */
    private static void assertProblem(Map<String, Object> problem, String code, String title) {
        assertThat(problem.get("code")).isEqualTo(code);
        assertThat(problem.get("title")).isEqualTo(title);
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
    }

    /** Asserts a problem about one field: the code, the title, a detail and exactly one entry for that field. */
    private static void assertFieldProblem(Map<String, Object> problem, String code, String title, String field) {
        assertThat(problem.get("code")).isEqualTo(code);
        assertThat(problem.get("title")).isEqualTo(title);
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .singleElement(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("field", "message")
                .containsEntry("field", field)
                .extractingByKey("message").asString().isNotBlank();
    }

    /**
     * Asserts a bean validation problem: the code, the title, a detail, and one entry per expected field in field
     * order, each with a non-empty message. The messages are Hibernate Validator defaults and are not compared.
     */
    private static void assertValidationFailed(Map<String, Object> problem, String... fields) {
        assertThat(problem.get("code")).isEqualTo("validation-failed");
        assertThat(problem.get("title")).isEqualTo("Validation failed");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .extracting(detail -> (Object) ((Map<?, ?>) detail).get("field"))
                .containsExactly((Object[]) fields);
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .allSatisfy(detail -> assertThat(detail).asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsOnlyKeys("field", "message")
                        .extractingByKey("message").asString().isNotBlank());
    }

    /** The given book ids in ascending {@link UUID#compareTo} order, the order in which stock items are saved. */
    private static List<UUID> inBookIdOrder(UUID... bookIds) {
        return Stream.of(bookIds).sorted().toList();
    }

    /** The stock view as JSON gives it, with the default reorder level 3. */
    private static Map<String, Object> stockView(UUID bookId, int onHand, int reserved, int available,
            boolean lowStock) {
        return Map.of(
                "bookId", bookId.toString(),
                "onHand", onHand,
                "reserved", reserved,
                "available", available,
                "reorderLevel", 3,
                "lowStock", lowStock);
    }

    /** The order view as JSON gives it; the shipped time may be null, so the map allows a null value. */
    private static Map<String, Object> orderView(UUID id, String customerEmail, String status, String placedAt,
            String shippedAt, List<Map<String, Object>> lines, String total, String currency) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id.toString());
        view.put("customerEmail", customerEmail);
        view.put("status", status);
        view.put("placedAt", placedAt);
        view.put("shippedAt", shippedAt);
        view.put("lines", lines);
        view.put("total", total);
        view.put("currency", currency);
        return view;
    }

    private static Map<String, Object> lineView(String isbn, String title, int quantity, String unitPrice,
            String lineTotal) {
        return Map.of("isbn", isbn, "title", title, "quantity", quantity, "unitPrice", unitPrice, "lineTotal",
                lineTotal);
    }

    private static Map<String, Object> summaryView(UUID id, String status, String placedAt, int itemCount,
            String total) {
        return Map.of("id", id.toString(), "status", status, "placedAt", placedAt, "itemCount", itemCount, "total",
                total);
    }

    private static Map<String, Object> line(UUID bookId, int quantity) {
        return Map.of("bookId", bookId, "quantity", quantity);
    }

    private UUID addBook(String isbn, String title, String price) {
        CommandResult result = restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("isbn", isbn, "title", title, "authors", List.of("Charles Dickens"), "genre", "fiction",
                        "price", price, "publicationYear", 1859))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
    }

    private void receive(UUID bookId, int quantity) {
        restTestClient.post().uri("/inventory/{bookId}/commands/receive", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity))
                .exchange()
                .expectStatus().isAccepted();
    }

    /**
     * Seeds an available book with a fixed id, one author and its stock item by SQL, so a test controls every
     * attribute of the book, the id included. The low-stock signal is set when the available stock is at or below the
     * default reorder level 3, as the inventory leaves it after such a dip.
     */
    private void seedBook(UUID id, String isbn, String title, String price, int onHand, int reserved) {
        assertThat(jdbcTemplate.update("""
                insert into book (id, isbn, title, genre, price_amount, price_currency, publication_year, status,
                                  version)
                values (?, ?, ?, 'FICTION', ?, 'EUR', 1815, 'AVAILABLE', 0)""",
                id, isbn, title, new BigDecimal(price))).isEqualTo(1);
        assertThat(jdbcTemplate.update("insert into book_author (book_id, position, name) values (?, 0, 'Jane Austen')",
                id)).isEqualTo(1);
        assertThat(jdbcTemplate.update("""
                insert into stock_item (book_id, on_hand, reserved, reorder_level, reorder_quantity,
                                        low_stock_signalled, version)
                values (?, ?, ?, 3, 10, ?, 0)""",
                id, onHand, reserved, onHand - reserved <= 3)).isEqualTo(1);
    }

    private RestTestClient.ResponseSpec place(String customerEmail, List<Map<String, Object>> lines) {
        return restTestClient.post().uri("/orders/commands/place")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerEmail", customerEmail, "lines", lines))
                .exchange();
    }

    private UUID placeOrder(String customerEmail, List<Map<String, Object>> lines) {
        CommandResult result = place(customerEmail, lines)
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
    }

    private RestTestClient.ResponseSpec ship(UUID orderId) {
        return restTestClient.post().uri("/orders/{id}/commands/ship", orderId).exchange();
    }

    private Map<String, Object> getOrder(UUID id) {
        return restTestClient.get().uri("/orders/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private Map<String, Object> getStock(UUID bookId) {
        return restTestClient.get().uri("/inventory/{bookId}", bookId).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private Map<String, Object> getBook(UUID id) {
        return restTestClient.get().uri("/books/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private List<Map<String, Object>> history(String uri) {
        return restTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_ARRAY).returnResult().getResponseBody();
    }

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    private static Map<String, Object> problem(RestTestClient.ResponseSpec response, int status) {
        return response.expectStatus().isEqualTo(status)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    /**
     * Sets the placed time of an order directly in the table, so that the history order does not depend on the
     * wall-clock order of the requests and the view shows a known instant.
     */
    private void setPlacedAt(UUID orderId, String placedAt) {
        assertThat(jdbcTemplate.update("update customer_order set placed_at = ? where id = ?",
                OffsetDateTime.ofInstant(Instant.parse(placedAt), ZoneOffset.UTC), orderId)).isEqualTo(1);
    }

    private long version(UUID orderId) {
        Long version = jdbcTemplate.queryForObject("select version from customer_order where id = ?", Long.class,
                orderId);
        assertThat(version).isNotNull();
        return version;
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        assertThat(count).isNotNull();
        return count;
    }
}
