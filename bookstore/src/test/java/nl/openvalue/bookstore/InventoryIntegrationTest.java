package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The inventory end to end: real HTTP into the full application on PostgreSQL. Every test starts on an empty database
 * with an empty event recorder, so the running-low events a test sees are the ones its own requests delivered after
 * their commit. Bodies are read as plain JSON maps, so numbers and booleans are compared as JSON gives them.
 */
class InventoryIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_ARRAY =
            new ParameterizedTypeReference<>() {
            };

    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    void newBookHasAnEmptyStockItemAndIsShownAsNotInStock() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");

        Map<String, Object> stock = getStock(id);
        Map<String, Object> book = getBook(id);
        List<Map<String, Object>> books = search("/books");

        assertThat(stock).isEqualTo(stockView(id, 0, 0, 0, 3, true));
        assertThat(book.get("available")).isEqualTo(0);
        assertThat(book.get("inStock")).isEqualTo(false);
        assertThat(books).singleElement().satisfies(view -> {
            assertThat(view.get("id")).isEqualTo(id.toString());
            assertThat(view.get("available")).isEqualTo(0);
            assertThat(view.get("inStock")).isEqualTo(false);
        });
    }

    @Test
    void receivingStockIncreasesOnHandAndShowsTheBookInStock() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");

        receive(id, 2).expectStatus().isAccepted().expectBody().isEmpty();
        receive(id, 5).expectStatus().isAccepted().expectBody().isEmpty();

        assertThat(getStock(id)).isEqualTo(stockView(id, 7, 0, 7, 3, false));
        Map<String, Object> book = getBook(id);
        assertThat(book.get("available")).isEqualTo(7);
        assertThat(book.get("inStock")).isEqualTo(true);
        assertThat(search("/books")).singleElement().satisfies(view -> {
            assertThat(view.get("id")).isEqualTo(id.toString());
            assertThat(view.get("available")).isEqualTo(7);
            assertThat(view.get("inStock")).isEqualTo(true);
        });
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void receivingZeroOr1001UnitsIsRejectedWith400AndLeavesStockUnchanged() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 2).expectStatus().isAccepted();

        Map<String, Object> zero = problem(receive(id, 0), 400);
        Map<String, Object> tooMany = problem(receive(id, 1001), 400);

        assertValidationFailed(zero, "quantity");
        assertValidationFailed(tooMany, "quantity");
        assertThat(getStock(id)).isEqualTo(stockView(id, 2, 0, 2, 3, true));
    }

    @Test
    void stocktakeDownToTheReorderLevelDeliversOneEventAfterCommit() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();
        Instant before = Instant.now();

        // A reason with a leading space is three characters and not blank: the payload and the domain both accept it.
        correct(id, 3, " ab").expectStatus().isAccepted().expectBody().isEmpty();

        Instant after = Instant.now();
        assertThat(stockEvents.committed()).singleElement().satisfies(event -> {
            assertThat(event.bookId()).isEqualTo(id);
            assertThat(event.available()).isEqualTo(3);
            assertThat(event.reorderQuantity()).isEqualTo(10);
            assertThat(event.occurredAt()).isNotNull().isBetween(before, after);
        });
        assertThat(stockEvents.rolledBack()).isEmpty();
        assertThat(getStock(id)).isEqualTo(stockView(id, 3, 0, 3, 3, true));
    }

    @Test
    void aSecondCorrectionInTheSameDipPublishesNoNewEvent() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();
        correct(id, 3, "Stocktake").expectStatus().isAccepted();

        correct(id, 2, "Second stocktake").expectStatus().isAccepted().expectBody().isEmpty();

        assertThat(stockEvents.committed()).singleElement().satisfies(event -> {
            assertThat(event.bookId()).isEqualTo(id);
            assertThat(event.available()).isEqualTo(3);
            assertThat(event.reorderQuantity()).isEqualTo(10);
        });
        assertThat(getStock(id)).isEqualTo(stockView(id, 2, 0, 2, 3, true));
    }

    @Test
    void receivingAboveTheReorderLevelLetsTheNextDipSignalAgain() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();
        correct(id, 3, "Stocktake").expectStatus().isAccepted();

        receive(id, 10).expectStatus().isAccepted();
        assertThat(getStock(id)).isEqualTo(stockView(id, 13, 0, 13, 3, false));
        correct(id, 2, "Second stocktake").expectStatus().isAccepted();

        assertThat(stockEvents.committed()).hasSize(2);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(id, id);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::available).containsExactly(3, 2);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::reorderQuantity).containsExactly(10, 10);
        assertThat(stockEvents.rolledBack()).isEmpty();
    }

    @Test
    void noEventIsDeliveredWhenTheStocktakeRollsBack() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();
        stockEvents.failBeforeCommit();

        // The failure is not a use-case exception, so Boot answers with its own error JSON, not a problem detail.
        correct(id, 3, "Stocktake").expectStatus().isEqualTo(500);

        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).singleElement().satisfies(event -> {
            assertThat(event.bookId()).isEqualTo(id);
            assertThat(event.available()).isEqualTo(3);
            assertThat(event.reorderQuantity()).isEqualTo(10);
        });
        assertThat(getStock(id)).isEqualTo(stockView(id, 5, 0, 5, 3, false));

        // The signal rolled back with the stock: the same correction now starts the dip and signals once.
        stockEvents.reset();
        correct(id, 3, "Stocktake").expectStatus().isAccepted().expectBody().isEmpty();

        assertThat(stockEvents.committed()).singleElement().satisfies(event -> {
            assertThat(event.bookId()).isEqualTo(id);
            assertThat(event.available()).isEqualTo(3);
            assertThat(event.reorderQuantity()).isEqualTo(10);
        });
        assertThat(stockEvents.rolledBack()).isEmpty();
        assertThat(getStock(id)).isEqualTo(stockView(id, 3, 0, 3, 3, true));
    }

    @Test
    void stocktakeBelowReservedIsRejectedWith409() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();
        setReserved(id, 2);

        Map<String, Object> problem = problem(correct(id, 1, "Stocktake"), 409);

        assertThat(problem.get("code")).isEqualTo("on-hand-below-reserved");
        assertThat(problem.get("title")).isEqualTo("On hand below reserved");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertThat(getStock(id)).isEqualTo(stockView(id, 5, 2, 3, 3, true));
        assertThat(stockEvents.committed()).isEmpty();
    }

    @Test
    void unknownBookIsNotFoundOnStockReceiveAndCorrect() {
        Map<String, Object> onGet = problem(restTestClient.get().uri("/inventory/{bookId}", UNKNOWN_ID).exchange(),
                404);
        Map<String, Object> onReceive = problem(receive(UNKNOWN_ID, 5), 404);
        Map<String, Object> onCorrect = problem(correct(UNKNOWN_ID, 3, "Stocktake"), 404);

        assertBookNotFound(onGet);
        assertBookNotFound(onReceive);
        assertBookNotFound(onCorrect);
    }

    @Test
    void lowStockListsExactlyTheBooksAtOrBelowTheReorderLevelMostUrgentFirst() {
        // Added in the order D, E, B, C, A: the book that ends at 3 first, the book that ends at 0 last. In the
        // expected order A, B, C, D the insertion order (5, 3, 4, 1), on hand (0, 3, 2, 4), reserved (0, 2, 0, 1)
        // and reorder level (3, 5, 3, 3) each go up and down, so only available (0, 1, 2, 3) explains the order.
        // The available figures are distinct, so the book id tiebreak is not used here.
        UUID d = addBook("9780141439518", "Pride and Prejudice");
        receive(d, 4).expectStatus().isAccepted();
        setReserved(d, 1);
        UUID e = addBook("9780141439587", "Emma");
        receive(e, 4).expectStatus().isAccepted();
        UUID b = addBook("9780141439662", "Sense and Sensibility");
        receive(b, 3).expectStatus().isAccepted();
        setReserved(b, 2);
        setReorderLevel(b, 5);
        UUID c = addBook("9780141439617", "The History of England");
        receive(c, 2).expectStatus().isAccepted();
        UUID a = addBook("9780141439747", "Mathilda");

        List<Map<String, Object>> lowStock = search("/inventory?lowStock=true");

        // E has available 4 against its reorder level 3: one above the level, so it is not listed.
        assertThat(lowStock).extracting(view -> view.get("bookId"))
                .containsExactly(a.toString(), b.toString(), c.toString(), d.toString());
        assertThat(lowStock).containsExactly(
                stockView(a, 0, 0, 0, 3, true),
                stockView(b, 3, 2, 1, 5, true),
                stockView(c, 2, 0, 2, 3, true),
                stockView(d, 4, 1, 3, 3, true));
        assertThat(getStock(e)).isEqualTo(stockView(e, 4, 0, 4, 3, false));
    }

    @Test
    void lowStockFilterOtherThanTrueIsRejectedWith400() {
        List<String> uris = List.of(
                "/inventory",
                "/inventory?lowStock=",
                "/inventory?lowStock=false",
                "/inventory?lowStock=TRUE",
                "/inventory?lowStock=1",
                "/inventory?lowStock=maybe",
                "/inventory?lowStock=true&lowStock=false");

        for (String uri : uris) {
            Map<String, Object> problem = problem(restTestClient.get().uri(uri).exchange(), 400);

            assertThat(problem.get("code")).as(uri).isEqualTo("validation-failed");
            assertThat(problem.get("title")).as(uri).isEqualTo("Validation failed");
            assertThat(problem.get("detail")).as(uri).asString().isNotBlank();
            assertThat(problem.get("details")).as(uri)
                    .isEqualTo(List.of(Map.of("field", "lowStock", "message", "must be true")));
        }
    }

    @Test
    void invalidStocktakePayloadIsRejectedWith400() {
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 5).expectStatus().isAccepted();

        Map<String, Object> missing = problem(postCorrect(id, Map.of()), 400);
        Map<String, Object> belowRange = problem(postCorrect(id, Map.of("onHand", -1, "reason", "ab")), 400);
        Map<String, Object> aboveRange = problem(postCorrect(id, Map.of("onHand", 10001, "reason", "x".repeat(201))),
                400);
        Map<String, Object> blankReason = problem(postCorrect(id, Map.of("onHand", 3, "reason", "   ")), 400);

        assertValidationFailed(missing, "onHand", "reason");
        assertValidationFailed(belowRange, "onHand", "reason");
        assertValidationFailed(aboveRange, "onHand", "reason");
        assertValidationFailed(blankReason, "reason");
        assertThat(getStock(id)).isEqualTo(stockView(id, 5, 0, 5, 3, false));
        assertThat(stockEvents.committed()).isEmpty();
    }

    @Test
    void stockViewExposesExactlyTheStockViewFields() {
        // Every figure differs from every other one, so a swapped field cannot pass.
        UUID id = addBook("9780141439518", "Pride and Prejudice");
        receive(id, 9).expectStatus().isAccepted();
        setReserved(id, 2);
        setReorderLevel(id, 8);

        Map<String, Object> stock = getStock(id);

        assertThat(stock.keySet())
                .containsExactlyInAnyOrder("bookId", "onHand", "reserved", "available", "reorderLevel", "lowStock");
        assertThat(stock).isEqualTo(Map.of(
                "bookId", id.toString(),
                "onHand", 9,
                "reserved", 2,
                "available", 7,
                "reorderLevel", 8,
                "lowStock", true));
    }

    @Test
    void addingABookIsUndoneWhenItsStockItemCannotBeOpened() {
        doThrow(new RuntimeException("Simulated failure opening the stock item"))
                .when(stockItemDataService).save(any());

        restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookPayload("9780141439518", "Pride and Prejudice"))
                .exchange()
                .expectStatus().isEqualTo(500);

        assertThat(countRows("book")).isZero();
        assertThat(countRows("book_author")).isZero();
        assertThat(countRows("stock_item")).isZero();
    }

    private static void assertBookNotFound(Map<String, Object> problem) {
        assertThat(problem.get("code")).isEqualTo("book-not-found");
        assertThat(problem.get("title")).isEqualTo("Book not found");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
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

    /** The stock view as JSON gives it: the book id as a string, the figures as numbers, the flag as a boolean. */
    private static Map<String, Object> stockView(UUID bookId, int onHand, int reserved, int available,
            int reorderLevel, boolean lowStock) {
        return Map.of(
                "bookId", bookId.toString(),
                "onHand", onHand,
                "reserved", reserved,
                "available", available,
                "reorderLevel", reorderLevel,
                "lowStock", lowStock);
    }

    private static Map<String, Object> bookPayload(String isbn, String title) {
        return Map.of("isbn", isbn, "title", title, "authors", List.of("Jane Austen"), "genre", "fiction",
                "price", "9.99", "publicationYear", 1813);
    }

    private UUID addBook(String isbn, String title) {
        CommandResult result = restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bookPayload(isbn, title))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
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

    private List<Map<String, Object>> search(String uri) {
        return restTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_ARRAY).returnResult().getResponseBody();
    }

    private RestTestClient.ResponseSpec receive(UUID bookId, int quantity) {
        return restTestClient.post().uri("/inventory/{bookId}/commands/receive", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity))
                .exchange();
    }

    private RestTestClient.ResponseSpec correct(UUID bookId, int onHand, String reason) {
        return postCorrect(bookId, Map.of("onHand", onHand, "reason", reason));
    }

    private RestTestClient.ResponseSpec postCorrect(UUID bookId, Map<String, Object> payload) {
        return restTestClient.post().uri("/inventory/{bookId}/commands/correct", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange();
    }

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    private static Map<String, Object> problem(RestTestClient.ResponseSpec response, int status) {
        return response.expectStatus().isEqualTo(status)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    /** Sets the reserved stock directly in the table: no spec 03 use case reserves stock. */
    private void setReserved(UUID bookId, int reserved) {
        assertThat(jdbcTemplate.update("update stock_item set reserved = ? where book_id = ?", reserved, bookId))
                .isEqualTo(1);
    }

    /** Sets the reorder level directly in the table: no spec 03 use case changes it. */
    private void setReorderLevel(UUID bookId, int reorderLevel) {
        assertThat(jdbcTemplate.update("update stock_item set reorder_level = ? where book_id = ?", reorderLevel,
                bookId)).isEqualTo(1);
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        assertThat(count).isNotNull();
        return count;
    }
}
