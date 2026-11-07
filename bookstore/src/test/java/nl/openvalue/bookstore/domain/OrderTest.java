package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
    private static final Clock NANO_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00.123456789Z"), ZoneOffset.UTC);
    private static final Clock LATER_CLOCK =
            Clock.fixed(Instant.parse("2026-06-16T11:30:00.654321987Z"), ZoneOffset.UTC);
    private static final Instant NANO_INSTANT_TRUNCATED = Instant.parse("2026-06-15T10:00:00.123456Z");

    private static final BookId BOOK_A = new BookId(UUID.fromString("3a1c2b9e-6d4a-4c1e-9b7a-2e5d8f0a1b2c"));
    private static final BookId BOOK_B = new BookId(UUID.fromString("1b2d3c4e-5f60-4718-8a9b-0c1d2e3f4a5b"));
    private static final BookId BOOK_C = new BookId(UUID.fromString("5c6d7e8f-9a0b-4c1d-8e2f-3a4b5c6d7e8f"));
    private static final OrderId ORDER_ID = new OrderId(UUID.fromString("2d4e6f80-1a2b-4c3d-9e4f-5a6b7c8d9e0f"));
    private static final EmailAddress EMAIL = new EmailAddress("reader@example.com");

    // Three requests sorted in no column in either direction: book id 3, 1, 5; isbn 02, 09, 01;
    // title M, A, S; unit price 12.50, 30.00, 8.00; quantity 2, 1, 3.
    private static final OrderLineRequest REQUEST_A =
            request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 2);
    private static final OrderLineRequest REQUEST_B =
            request(BOOK_B, "9780900000007", "Adam Bede", "30.00", 1);
    private static final OrderLineRequest REQUEST_C =
            request(BOOK_C, "9780100000004", "Silas Marner", "8.00", 3);

    @Test
    void placeCreatesAPlacedOrderWithTheLinesInRequestOrder() {
        Order order = Order.place(EMAIL, List.of(REQUEST_A, REQUEST_B, REQUEST_C), CLOCK);

        assertThat(order.customerEmail()).isEqualTo(EMAIL);
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.shippedAt()).isNull();
        assertThat(order.lines())
                .extracting(OrderLine::bookId, OrderLine::isbn, OrderLine::title, OrderLine::unitPrice,
                        OrderLine::quantity)
                .containsExactly(
                        tuple(BOOK_A, "9780200000003", "Middlemarch", Money.of("12.50"), new Quantity(2)),
                        tuple(BOOK_B, "9780900000007", "Adam Bede", Money.of("30.00"), new Quantity(1)),
                        tuple(BOOK_C, "9780100000004", "Silas Marner", Money.of("8.00"), new Quantity(3)));
    }

    @Test
    void placeStampsThePlacedTimeFromTheClockTruncatedToMicroseconds() {
        Order order = Order.place(EMAIL, List.of(REQUEST_A), NANO_CLOCK);

        assertThat(order.placedAt()).isEqualTo(NANO_INSTANT_TRUNCATED);
    }

    @Test
    void placeRegistersOneOrderPlacedEventWithTheNormalisedEmailTheTotalAndThePlacedTime() {
        Order order = Order.place(new EmailAddress("Reader@Example.COM"),
                List.of(request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 2),
                        request(BOOK_B, "9780900000007", "Adam Bede", "8.00", 1)),
                NANO_CLOCK);

        assertThat(order.placedAt()).isEqualTo(NANO_INSTANT_TRUNCATED);
        assertThat(order.pullDomainEvents()).containsExactly(new OrderPlacedEvent(order.id().value(),
                "reader@example.com", new BigDecimal("33.00"), NANO_INSTANT_TRUNCATED));
    }

    @Test
    void placeGivesEveryOrderANewId() {
        Order first = Order.place(EMAIL, List.of(REQUEST_A), CLOCK);
        Order second = Order.place(EMAIL, List.of(REQUEST_A), CLOCK);

        assertThat(first.id()).isNotNull();
        assertThat(second.id()).isNotNull();
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void newOrderIdsAreDistinctAndAnOrderIdNeedsAValue() {
        assertThat(OrderId.newId()).isNotEqualTo(OrderId.newId());
        assertThat(OrderId.newId().value()).isNotNull();
        assertThatThrownBy(() -> new OrderId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void orderStatusHasExactlyPlacedAndShipped() {
        assertThat(OrderStatus.values()).containsExactly(OrderStatus.PLACED, OrderStatus.SHIPPED);
    }

    @Test
    void orderLineRequestRejectsEveryMissingComponent() {
        Money price = Money.of("12.50");
        Quantity quantity = new Quantity(2);

        assertThatThrownBy(() -> new OrderLineRequest(null, "9780200000003", "Middlemarch", price, quantity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLineRequest(BOOK_A, null, "Middlemarch", price, quantity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLineRequest(BOOK_A, "9780200000003", null, price, quantity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLineRequest(BOOK_A, "9780200000003", "Middlemarch", null, quantity))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLineRequest(BOOK_A, "9780200000003", "Middlemarch", price, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void totalIsTheSumOfTheLineTotals() {
        Order order = Order.place(EMAIL,
                List.of(request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 2),
                        request(BOOK_B, "9780900000007", "Adam Bede", "8.00", 1)),
                CLOCK);

        assertThat(order.total()).isEqualTo(Money.of("33.00"));
        assertThat(order.total().amount().toPlainString()).isEqualTo("33.00");

        Order large = Order.place(EMAIL, distinctRequests(20, "500.00", 10), CLOCK);

        assertThat(large.total()).isEqualTo(Money.of("100000.00"));
        assertThat(large.total().amount().toPlainString()).isEqualTo("100000.00");
    }

    @Test
    void lineTotalIsTheUnitPriceTimesTheQuantity() {
        Order order = Order.place(EMAIL, List.of(request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 3)),
                CLOCK);

        assertThat(order.lines().get(0).lineTotal()).isEqualTo(Money.of("37.50"));
    }

    @Test
    void quantityOfAccepts1And10() {
        assertThat(OrderLine.quantityOf(1)).isEqualTo(new Quantity(1));
        assertThat(OrderLine.quantityOf(10)).isEqualTo(new Quantity(10));
    }

    @Test
    void quantityOfRejects0And11AndMinus1() {
        assertThatThrownBy(() -> OrderLine.quantityOf(0)).satisfies(e -> assertQuantityOutOfRange(e, 0));
        assertThatThrownBy(() -> OrderLine.quantityOf(11)).satisfies(e -> assertQuantityOutOfRange(e, 11));
        assertThatThrownBy(() -> OrderLine.quantityOf(-1)).satisfies(e -> assertQuantityOutOfRange(e, -1));
    }

    @Test
    void placeRejectsLineQuantities0And11() {
        assertThatThrownBy(() -> Order.place(EMAIL,
                List.of(REQUEST_B, request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 0)), CLOCK))
                .satisfies(e -> assertQuantityOutOfRange(e, 0));
        assertThatThrownBy(() -> Order.place(EMAIL,
                List.of(REQUEST_B, request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 11)), CLOCK))
                .satisfies(e -> assertQuantityOutOfRange(e, 11));
    }

    @Test
    void placeAccepts1And20Lines() {
        Order single = Order.place(EMAIL, List.of(request(BOOK_B, "9780900000007", "Adam Bede", "30.00", 1)),
                CLOCK);

        assertThat(single.lines()).extracting(OrderLine::bookId, OrderLine::quantity)
                .containsExactly(tuple(BOOK_B, new Quantity(1)));
        assertThat(single.total()).isEqualTo(Money.of("30.00"));

        List<OrderLineRequest> twenty = distinctRequests(20, "500.00", 10);
        Order full = Order.place(EMAIL, twenty, CLOCK);

        assertThat(full.lines()).hasSize(20);
        assertThat(full.lines()).extracting(OrderLine::bookId)
                .containsExactlyElementsOf(twenty.stream().map(OrderLineRequest::bookId).toList());
        assertThat(full.lines()).extracting(OrderLine::quantity).containsOnly(new Quantity(10));
    }

    @Test
    void placeRejects0And21Lines() {
        assertThatThrownBy(() -> Order.place(EMAIL, List.of(), CLOCK))
                .satisfies(e -> assertLineCountOutOfRange(e, 0));
        assertThatThrownBy(() -> Order.place(EMAIL, distinctRequests(21, "12.50", 1), CLOCK))
                .satisfies(e -> assertLineCountOutOfRange(e, 21));
    }

    @Test
    void placeRejectsTwoLinesForTheSameBook() {
        OrderLineRequest secondA = request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 4);

        assertThatThrownBy(() -> Order.place(EMAIL, List.of(REQUEST_A, REQUEST_B, secondA), CLOCK))
                .satisfies(e -> assertDuplicateLine(e, BOOK_A));
        // The duplicated book is neither the first request nor next to its twin, so the reported id
        // must be the duplicated book and not simply the first request's book.
        assertThatThrownBy(() -> Order.place(EMAIL, List.of(REQUEST_B, REQUEST_A, REQUEST_C, secondA), CLOCK))
                .satisfies(e -> assertDuplicateLine(e, BOOK_A));
    }

    @Test
    void placeRejectsMissingArguments() {
        assertThatThrownBy(() -> Order.place(null, List.of(REQUEST_A), CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.place(EMAIL, null, CLOCK)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.place(EMAIL, List.of(REQUEST_A), null))
                .isInstanceOf(NullPointerException.class);
        List<OrderLineRequest> withNull = new ArrayList<>();
        withNull.add(REQUEST_A);
        withNull.add(null);
        assertThatThrownBy(() -> Order.place(EMAIL, withNull, CLOCK)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void linesCannotBeChangedFromOutside() {
        List<OrderLineRequest> requests = new ArrayList<>(List.of(REQUEST_A, REQUEST_B));
        Order order = Order.place(EMAIL, requests, CLOCK);
        OrderLine first = order.lines().get(0);

        assertThatThrownBy(() -> order.lines().add(first)).isInstanceOf(UnsupportedOperationException.class);

        requests.add(REQUEST_C);
        requests.remove(0);

        assertThat(order.lines()).extracting(OrderLine::bookId).containsExactly(BOOK_A, BOOK_B);
        assertThat(order.total()).isEqualTo(Money.of("55.00"));
    }

    @Test
    void restoreKeepsEveryStoredValue() {
        Instant placedAt = Instant.parse("2026-06-14T08:30:00.987654321Z");
        Instant shippedAt = Instant.parse("2026-06-14T09:45:00.123456789Z");

        Order placed = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_A, REQUEST_B, REQUEST_C),
                OrderStatus.PLACED, placedAt, null);

        assertThat(placed.id()).isEqualTo(ORDER_ID);
        assertThat(placed.customerEmail()).isEqualTo(EMAIL);
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.placedAt()).isEqualTo(placedAt);
        assertThat(placed.shippedAt()).isNull();
        assertThat(placed.lines())
                .extracting(OrderLine::bookId, OrderLine::isbn, OrderLine::title, OrderLine::unitPrice,
                        OrderLine::quantity)
                .containsExactly(
                        tuple(BOOK_A, "9780200000003", "Middlemarch", Money.of("12.50"), new Quantity(2)),
                        tuple(BOOK_B, "9780900000007", "Adam Bede", Money.of("30.00"), new Quantity(1)),
                        tuple(BOOK_C, "9780100000004", "Silas Marner", Money.of("8.00"), new Quantity(3)));

        OrderId shippedId = new OrderId(UUID.fromString("6e7f8091-a2b3-4c4d-8e5f-60718293a4b5"));
        EmailAddress shippedEmail = new EmailAddress("other@example.org");
        Order shipped = Order.restore(shippedId, shippedEmail, List.of(REQUEST_C, REQUEST_A),
                OrderStatus.SHIPPED, placedAt, shippedAt);

        assertThat(shipped.id()).isEqualTo(shippedId);
        assertThat(shipped.customerEmail()).isEqualTo(shippedEmail);
        assertThat(shipped.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(shipped.placedAt()).isEqualTo(placedAt);
        assertThat(shipped.shippedAt()).isEqualTo(shippedAt);
        assertThat(shipped.lines()).extracting(OrderLine::bookId).containsExactly(BOOK_C, BOOK_A);
    }

    @Test
    void restoreRegistersNoEvent() {
        Order placed = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_A), OrderStatus.PLACED,
                Instant.parse("2026-06-14T08:30:00Z"), null);
        Order shipped = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_A), OrderStatus.SHIPPED,
                Instant.parse("2026-06-14T08:30:00Z"), Instant.parse("2026-06-14T09:30:00Z"));

        assertThat(placed.pullDomainEvents()).isEmpty();
        assertThat(shipped.pullDomainEvents()).isEmpty();
    }

    @Test
    void restoreChecksTheLineRules() {
        Instant placedAt = Instant.parse("2026-06-14T08:30:00Z");

        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL, List.of(), OrderStatus.PLACED, placedAt, null))
                .satisfies(e -> assertLineCountOutOfRange(e, 0));
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL, distinctRequests(21, "12.50", 1),
                OrderStatus.PLACED, placedAt, null))
                .satisfies(e -> assertLineCountOutOfRange(e, 21));
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL,
                List.of(REQUEST_B, request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 11)),
                OrderStatus.PLACED, placedAt, null))
                .satisfies(e -> assertQuantityOutOfRange(e, 11));
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL,
                List.of(REQUEST_A, REQUEST_B, request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 4)),
                OrderStatus.SHIPPED, placedAt, Instant.parse("2026-06-14T09:30:00Z")))
                .satisfies(e -> assertDuplicateLine(e, BOOK_A));
        // Here the second copy of the duplicated book is not the last request, so the last request's
        // book cannot stand in for it either.
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL,
                List.of(REQUEST_A, REQUEST_C, request(BOOK_A, "9780200000003", "Middlemarch", "12.50", 4),
                        REQUEST_B),
                OrderStatus.PLACED, placedAt, null))
                .satisfies(e -> assertDuplicateLine(e, BOOK_A));
    }

    @Test
    void restoreRejectsMissingValues() {
        Instant placedAt = Instant.parse("2026-06-14T08:30:00Z");
        List<OrderLineRequest> lines = List.of(REQUEST_A);

        assertThatThrownBy(() -> Order.restore(null, EMAIL, lines, OrderStatus.PLACED, placedAt, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.restore(ORDER_ID, null, lines, OrderStatus.PLACED, placedAt, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL, null, OrderStatus.PLACED, placedAt, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL, lines, null, placedAt, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Order.restore(ORDER_ID, EMAIL, lines, OrderStatus.PLACED, null, null))
                .isInstanceOf(NullPointerException.class);

        assertThat(Order.restore(ORDER_ID, EMAIL, lines, OrderStatus.PLACED, placedAt, null).shippedAt()).isNull();
    }

    @Test
    void shipSetsShippedAndTheTruncatedShippedTime() {
        Order order = Order.place(EMAIL, List.of(REQUEST_A, REQUEST_B), CLOCK);

        order.ship(NANO_CLOCK);

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.shippedAt()).isEqualTo(NANO_INSTANT_TRUNCATED);
        assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00Z"));

        Order restored = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_C), OrderStatus.PLACED,
                Instant.parse("2026-06-14T08:30:00Z"), null);

        restored.ship(LATER_CLOCK);

        assertThat(restored.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(restored.shippedAt()).isEqualTo(Instant.parse("2026-06-16T11:30:00.654321Z"));
    }

    @Test
    void shipRegistersNoEvent() {
        Order placed = Order.place(EMAIL, List.of(REQUEST_A), CLOCK);
        placed.pullDomainEvents();

        placed.ship(NANO_CLOCK);

        assertThat(placed.pullDomainEvents()).isEmpty();

        Order restored = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_A), OrderStatus.PLACED,
                Instant.parse("2026-06-14T08:30:00Z"), null);

        restored.ship(NANO_CLOCK);

        assertThat(restored.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingTwiceThrowsOrderAlreadyShippedAndKeepsTheFirstShippedTime() {
        Order order = Order.place(EMAIL, List.of(REQUEST_A), CLOCK);
        order.pullDomainEvents();
        order.ship(NANO_CLOCK);

        assertThatThrownBy(() -> order.ship(LATER_CLOCK)).satisfies(e -> assertAlreadyShipped(e, order.id()));

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.shippedAt()).isEqualTo(NANO_INSTANT_TRUNCATED);
        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingARestoredShippedOrderThrows() {
        Instant shippedAt = Instant.parse("2026-06-14T09:45:00.123456789Z");
        Order order = Order.restore(ORDER_ID, EMAIL, List.of(REQUEST_A), OrderStatus.SHIPPED,
                Instant.parse("2026-06-14T08:30:00Z"), shippedAt);

        assertThatThrownBy(() -> order.ship(CLOCK)).satisfies(e -> assertAlreadyShipped(e, ORDER_ID));

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.shippedAt()).isEqualTo(shippedAt);
        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    void pullDomainEventsReturnsTheEventsAndClearsThem() {
        Order order = Order.place(EMAIL, List.of(REQUEST_A), CLOCK);
        OrderPlacedEvent placed = new OrderPlacedEvent(order.id().value(), "reader@example.com",
                new BigDecimal("25.00"), Instant.parse("2026-06-15T10:00:00Z"));

        List<DomainEvent> first = order.pullDomainEvents();

        assertThat(first).containsExactly(placed);
        assertThatThrownBy(() -> first.add(placed)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.pullDomainEvents()).isEmpty();
        assertThat(first).containsExactly(placed);
    }

    private static OrderLineRequest request(BookId bookId, String isbn, String title, String unitPrice,
            int quantity) {
        return new OrderLineRequest(bookId, isbn, title, Money.of(unitPrice), new Quantity(quantity));
    }

    /** Requests for distinct books whose ids start with the hex digit 0, all with the same price and quantity. */
    private static List<OrderLineRequest> distinctRequests(int count, String unitPrice, int quantity) {
        List<OrderLineRequest> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BookId bookId = new BookId(new UUID(0x0000_0000_0000_4000L + i, 0x8000_0000_0000_0000L + i));
            requests.add(request(bookId, "978030000" + String.format("%04d", i), "Title " + i, unitPrice, quantity));
        }
        return requests;
    }

    private static void assertQuantityOutOfRange(Throwable thrown, int quantity) {
        assertThat(thrown).isInstanceOf(OrderedQuantityOutOfRangeException.class);
        assertThat(thrown.getMessage()).isNotBlank();
        OrderedQuantityOutOfRangeException rejection = (OrderedQuantityOutOfRangeException) thrown;
        assertThat(rejection.quantity()).isEqualTo(quantity);
        assertThat(rejection.min()).isEqualTo(1);
        assertThat(rejection.max()).isEqualTo(10);
    }

    private static void assertLineCountOutOfRange(Throwable thrown, int count) {
        assertThat(thrown).isInstanceOf(OrderLineCountOutOfRangeException.class);
        assertThat(thrown.getMessage()).isNotBlank();
        OrderLineCountOutOfRangeException rejection = (OrderLineCountOutOfRangeException) thrown;
        assertThat(rejection.count()).isEqualTo(count);
        assertThat(rejection.min()).isEqualTo(1);
        assertThat(rejection.max()).isEqualTo(20);
    }

    private static void assertDuplicateLine(Throwable thrown, BookId bookId) {
        assertThat(thrown).isInstanceOf(DuplicateOrderLineException.class);
        assertThat(thrown.getMessage()).isNotBlank();
        assertThat(((DuplicateOrderLineException) thrown).bookId()).isEqualTo(bookId);
    }

    private static void assertAlreadyShipped(Throwable thrown, OrderId orderId) {
        assertThat(thrown).isInstanceOf(OrderAlreadyShippedException.class);
        assertThat(thrown.getMessage()).isNotBlank();
        assertThat(((OrderAlreadyShippedException) thrown).orderId()).isEqualTo(orderId);
    }
}
