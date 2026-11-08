package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domain.OrderPlacedEvent;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;

/**
 * Guards the hooks of {@link IntegrationTestBase}: state left behind before a test, here dirtied on purpose before
 * the base hooks run, must be gone when the test starts. Removing the database cleaning, the recorder reset or the
 * reset of the failure toggle from the base makes this test fail.
 * The same holds for the order recorder and its failure toggle, and for a leftover row in the order table.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTestBaseHooksTest extends IntegrationTestBase {

    private static final StockRunningLowEvent LEFTOVER_EVENT = new StockRunningLowEvent(
            UUID.fromString("00000000-0000-4000-8000-000000000002"), 3, 10, Instant.parse("2026-06-15T10:00:00Z"));

    private static final OrderPlacedEvent LEFTOVER_ORDER_EVENT = new OrderPlacedEvent(
            UUID.fromString("00000000-0000-4000-8000-000000000004"), "left.behind@example.com",
            new BigDecimal("9.99"), Instant.parse("2026-06-15T10:00:00Z"));

    @BeforeAll
    void leaveStateBehind() {
        jdbcTemplate.update("""
                insert into book (id, isbn, title, genre, price_amount, price_currency, publication_year, status,
                                  version)
                values (?, '9780000000002', 'Left behind', 'FICTION', 9.99, 'EUR', 2000, 'AVAILABLE', 0)""",
                UUID.fromString("00000000-0000-4000-8000-000000000003"));
        stockEvents.failBeforeCommit();
        stockEvents.recordCommitted(LEFTOVER_EVENT);
        stockEvents.recordRolledBack(LEFTOVER_EVENT);
        jdbcTemplate.update("""
                insert into customer_order (id, customer_email, status, placed_at, shipped_at, version)
                values (?, 'left.behind@example.com', 'PLACED', timestamptz '2026-06-15 10:00:00+00', null, 0)""",
                UUID.fromString("00000000-0000-4000-8000-000000000005"));
        orderEvents.failBeforeCommit();
        orderEvents.recordCommitted(LEFTOVER_ORDER_EVENT);
        orderEvents.recordRolledBack(LEFTOVER_ORDER_EVENT);
    }

    @Test
    void everyTestStartsWithAnEmptyDatabaseAndAResetRecorder() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from book", Integer.class)).isZero();
        assertThat(stockEvents.committed()).isEmpty();
        assertThat(stockEvents.rolledBack()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from customer_order", Integer.class)).isZero();
        assertThat(orderEvents.committed()).isEmpty();
        assertThat(orderEvents.rolledBack()).isEmpty();

        // The failure toggle is off: a stocktake that starts a dip commits.
        CommandResult added = restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("isbn", "9780141439518", "title", "Pride and Prejudice", "authors",
                        List.of("Jane Austen"), "genre", "fiction", "price", "9.99", "publicationYear", 1813))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(added).isNotNull();
        restTestClient.post().uri("/inventory/{bookId}/commands/receive", added.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", 5))
                .exchange()
                .expectStatus().isAccepted();
        restTestClient.post().uri("/inventory/{bookId}/commands/correct", added.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("onHand", 3, "reason", "Stocktake"))
                .exchange()
                .expectStatus().isAccepted();
        assertThat(stockEvents.committed()).extracting(StockRunningLowEvent::bookId).containsExactly(added.id());

        // The order failure toggle is off too: placing an order commits and delivers its event once.
        CommandResult placed = restTestClient.post().uri("/orders/commands/place")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerEmail", "reader@example.com", "lines",
                        List.of(Map.of("bookId", added.id(), "quantity", 1))))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(placed).isNotNull();
        assertThat(orderEvents.committed()).extracting(OrderPlacedEvent::orderId).containsExactly(placed.id());
        assertThat(orderEvents.rolledBack()).isEmpty();
    }
}
