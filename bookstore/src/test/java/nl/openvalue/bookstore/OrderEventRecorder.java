package nl.openvalue.bookstore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.openvalue.bookstore.domain.OrderPlacedEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Test listener for the order placed event, registered in the shared integration context by
 * {@link IntegrationTestBase} beside the stock event recorder and reset before every test. It records the events
 * delivered after a commit and after a rollback, and on request it makes the transaction break just before the commit,
 * so a test can prove that a rolled back order leaves no order, no reservation and no delivered event behind. The
 * listeners only record; tests check the copies in the test thread.
 */
public class OrderEventRecorder {

    private final CopyOnWriteArrayList<OrderPlacedEvent> committed = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<OrderPlacedEvent> rolledBack = new CopyOnWriteArrayList<>();

    private volatile boolean failBeforeCommit;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void failBeforeCommitWhenAsked(OrderPlacedEvent event) {
        if (failBeforeCommit) {
            throw new IllegalStateException("Simulated failure before commit");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void recordCommitted(OrderPlacedEvent event) {
        committed.add(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    void recordRolledBack(OrderPlacedEvent event) {
        rolledBack.add(event);
    }

    /** The events delivered after a commit since the last reset, as a copy. */
    public List<OrderPlacedEvent> committed() {
        return List.copyOf(committed);
    }

    /** The events delivered after a rollback since the last reset, as a copy. */
    public List<OrderPlacedEvent> rolledBack() {
        return List.copyOf(rolledBack);
    }

    /** Makes every following transaction that publishes an order placed event break just before its commit. */
    public void failBeforeCommit() {
        failBeforeCommit = true;
    }

    /** Clears both lists and switches the failure off again. */
    public void reset() {
        committed.clear();
        rolledBack.clear();
        failBeforeCommit = false;
    }
}
