package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.DistributorUnavailableException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotFoundAtDistributorException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotOrderableException;
import nl.openvalue.bookstore.domaininteraction.RestockConfirmationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The restock process end to end, on the event path: an order that dips the stock of a book commits, and after the
 * commit the process asks the distributor mock to restock the book in a transaction of its own. Real HTTP into the
 * full application on PostgreSQL; every test starts on an empty database with the default distributor answer. Books
 * have the default reorder level 3 and reorder quantity 10. Log lines are read from the captured console output.
 */
@ExtendWith(OutputCaptureExtension.class)
class RestockProcessIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final String TALE_ISBN = "9780141439600";

    private static final String WUTHERING_ISBN = "9780141439563";

    @Test
    void orderDrivingStockBelowTheReorderLevelRequestsOneRestock() {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 4);

        placeOrder(List.of(line(a, 2)));

        ArgumentCaptor<String> reference = ArgumentCaptor.forClass(String.class);
        verify(distributorAppService).requestRestock(eq("9780141439600"), eq(10), reference.capture());
        verifyNoMoreInteractions(distributorAppService);
        assertThat(reference.getValue()).matches("^" + a + "/\\d+$");
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        Map<String, Object> stock = getStock(a);
        assertThat(stock.get("pendingRestock")).isEqualTo(Map.of(
                "restockOrderId", "c4f1a7e2-3b9d-4e8a-b6f5-2d1e0a9c8b7f",
                "quantity", 10,
                "expectedDeliveryDate", defaultExpectedDeliveryDate.toString(),
                "backordered", false));
        assertThat(stock).isEqualTo(withDefaultPendingRestock(stockView(a, 4, 2, 2, true), 10));
    }

    @Test
    void rolledBackOrderRequestsNoRestock() {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 4);
        UUID b = addBook(WUTHERING_ISBN, "Wuthering Heights", "8.00");
        receive(b, 1);

        // A alone would dip to 2, but B is short, so the whole order is rejected before anything is reserved.
        Map<String, Object> problem = problem(place(List.of(line(a, 2), line(b, 2))), 409);

        assertThat(problem.get("code")).isEqualTo("insufficient-stock");
        verifyNoInteractions(distributorAppService);
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
        assertThat(countRows("customer_order")).isZero();
        assertThat(getStock(a)).isEqualTo(stockView(a, 4, 0, 4, false));
        assertThat(getStock(b)).isEqualTo(stockView(b, 1, 0, 1, true));
    }

    @Test
    void orderRolledBackAfterPublishingRequestsNoRestock() {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 4);
        stockEvents.failBeforeCommit();

        // The running-low event is published, then the transaction fails just before its commit.
        place(List.of(line(a, 2))).expectStatus().isEqualTo(500);

        assertThat(stockEvents.rolledBack()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(stockEvents.committed()).isEmpty();
        verifyNoInteractions(distributorAppService);
        assertThat(countRows("customer_order")).isZero();
        assertThat(getStock(a)).isEqualTo(stockView(a, 4, 0, 4, false));
    }

    @Test
    void distributorDownKeepsTheOrderAndAllowsANewAttempt(CapturedOutput output) {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 4);
        doThrow(new DistributorUnavailableException("Distributor answered status 503"))
                .doAnswer(this::defaultConfirmation)
                .when(distributorAppService).requestRestock(any(), anyInt(), any());

        UUID id = placeOrder(List.of(line(a, 1)));

        assertOneWarningNaming(output, a);
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("status")).isEqualTo("PLACED");
        assertThat(order.get("lines"))
                .isEqualTo(List.of(lineView("9780141439600", "A Tale of Two Cities", 1, "12.50", "12.50")));
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(getStock(a)).isEqualTo(stockView(a, 4, 1, 3, true));
        verify(distributorAppService, times(1)).requestRestock(eq("9780141439600"), eq(10), any());

        // The signal was reset, so the next reservation that leaves the stock low starts a new attempt.
        placeOrder(List.of(line(a, 1)));

        verify(distributorAppService, times(2)).requestRestock(eq("9780141439600"), eq(10), any());
        verifyNoMoreInteractions(distributorAppService);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a, a);
        assertThat(getStock(a)).isEqualTo(withDefaultPendingRestock(stockView(a, 4, 2, 2, true), 10));
    }

    static Stream<RuntimeException> refusals() {
        return Stream.of(
                new TitleNotFoundAtDistributorException("9780141439600"),
                new TitleNotOrderableException("9780141439600"));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void refusedTitleKeepsTheSignalAndDoesNotRetry(RuntimeException refusal, CapturedOutput output) {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 4);
        doThrow(refusal).when(distributorAppService).requestRestock(any(), anyInt(), any());

        placeOrder(List.of(line(a, 1)));

        assertOneWarningNaming(output, a);
        assertThat(getStock(a)).isEqualTo(stockView(a, 4, 1, 3, true));

        // The signal stays set, so the next reservation in the same dip asks nothing.
        placeOrder(List.of(line(a, 1)));

        verify(distributorAppService, times(1)).requestRestock(eq("9780141439600"), eq(10), any());
        verifyNoMoreInteractions(distributorAppService);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(getStock(a)).isEqualTo(stockView(a, 4, 2, 2, true));
        assertOneWarningNaming(output, a);
    }

    @Test
    void unexpectedFailureIsContainedAndOtherBooksStillRestock(CapturedOutput output) {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 5);
        UUID b = addBook(WUTHERING_ISBN, "Wuthering Heights", "8.00");
        receive(b, 5);
        // The first restock request fails, whichever book it is for; the second one gets the default answer.
        doThrow(new IllegalStateException("Simulated failure of the first restock request"))
                .doAnswer(this::defaultConfirmation)
                .when(distributorAppService).requestRestock(any(), anyInt(), any());

        UUID id = placeOrder(List.of(line(a, 2), line(b, 2)));

        ArgumentCaptor<String> isbns = ArgumentCaptor.forClass(String.class);
        verify(distributorAppService, times(2)).requestRestock(isbns.capture(), eq(10), any());
        assertThat(isbns.getAllValues()).containsExactlyInAnyOrder("9780141439600", "9780141439563");
        UUID failed = isbns.getAllValues().getFirst().equals("9780141439600") ? a : b;
        UUID restocked = failed.equals(a) ? b : a;
        assertThat(output.getOut())
                .containsOnlyOnce("TransactionSynchronization.afterCompletion threw exception")
                .containsOnlyOnce("Simulated failure of the first restock request")
                .containsPattern(" ERROR .*TransactionSynchronization\\.afterCompletion threw exception\\R+"
                        + "java\\.lang\\.IllegalStateException: Simulated failure of the first restock request");
        Map<String, Object> order = getOrder(id);
        assertThat(order.get("status")).isEqualTo("PLACED");
        assertThat(order.get("lines")).isEqualTo(List.of(
                lineView("9780141439600", "A Tale of Two Cities", 2, "12.50", "25.00"),
                lineView("9780141439563", "Wuthering Heights", 2, "8.00", "16.00")));
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactlyInAnyOrder(a, b);
        assertThat(getStock(failed)).isEqualTo(stockView(failed, 5, 2, 3, true));
        assertThat(getStock(restocked)).isEqualTo(withDefaultPendingRestock(stockView(restocked, 5, 2, 3, true), 10));

        // The failure left the signal of the failed book set: its next reservation asks nothing.
        placeOrder(List.of(line(failed, 1)));

        verify(distributorAppService, times(2)).requestRestock(any(), anyInt(), any());
        verifyNoMoreInteractions(distributorAppService);
        assertThat(getStock(failed)).isEqualTo(stockView(failed, 5, 3, 2, true));
    }

    @Test
    void pendingRestockSurvivesALaterOrder() {
        UUID a = addBook(TALE_ISBN, "A Tale of Two Cities", "12.50");
        receive(a, 5);
        placeOrder(List.of(line(a, 2)));
        assertThat(getStock(a)).isEqualTo(withDefaultPendingRestock(stockView(a, 5, 2, 3, true), 10));
        // Another answer from now on, so a second recorded restock would show.
        doAnswer(invocation -> new RestockConfirmationDTO("9b2e6d14-a7c3-4f05-8e1d-2c5a0f7b3e98",
                invocation.getArgument(1), LocalDate.of(2026, 7, 3), true))
                .when(distributorAppService).requestRestock(any(), anyInt(), any());

        placeOrder(List.of(line(a, 1)));

        verify(distributorAppService, times(1)).requestRestock(eq("9780141439600"), eq(10), any());
        verifyNoMoreInteractions(distributorAppService);
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(a);
        assertThat(getStock(a)).isEqualTo(withDefaultPendingRestock(stockView(a, 5, 3, 2, true), 10));
    }

    /** The answer the base gives by default: the default order for the requested quantity, delivery in two days. */
    private RestockConfirmationDTO defaultConfirmation(InvocationOnMock invocation) {
        return new RestockConfirmationDTO(DEFAULT_RESTOCK_ORDER_ID, invocation.getArgument(1),
                defaultExpectedDeliveryDate, false);
    }

    /** Asserts exactly one warning line that names the book, level and book id on the same line. */
    private static void assertOneWarningNaming(CapturedOutput output, UUID bookId) {
        assertThat(output.getOut())
                .containsPattern(" WARN .*" + bookId)
                .doesNotContainPattern("(?s) WARN [^\\n]*" + bookId + ".* WARN [^\\n]*" + bookId);
    }

    /** The stock view as JSON gives it with no restock pending, with the default reorder level 3. */
    private static Map<String, Object> stockView(UUID bookId, int onHand, int reserved, int available,
            boolean lowStock) {
        Map<String, Object> view = new LinkedHashMap<>(Map.of(
                "bookId", bookId.toString(),
                "onHand", onHand,
                "reserved", reserved,
                "available", available,
                "reorderLevel", 3,
                "lowStock", lowStock));
        view.put("pendingRestock", null);
        return view;
    }

    private static Map<String, Object> lineView(String isbn, String title, int quantity, String unitPrice,
            String lineTotal) {
        return Map.of("isbn", isbn, "title", title, "quantity", quantity, "unitPrice", unitPrice, "lineTotal",
                lineTotal);
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

    private RestTestClient.ResponseSpec place(List<Map<String, Object>> lines) {
        return restTestClient.post().uri("/orders/commands/place")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerEmail", "reader@example.com", "lines", lines))
                .exchange();
    }

    private UUID placeOrder(List<Map<String, Object>> lines) {
        CommandResult result = place(lines)
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
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

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    private static Map<String, Object> problem(RestTestClient.ResponseSpec response, int status) {
        return response.expectStatus().isEqualTo(status)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private int countRows(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        assertThat(count).isNotNull();
        return count;
    }
}
