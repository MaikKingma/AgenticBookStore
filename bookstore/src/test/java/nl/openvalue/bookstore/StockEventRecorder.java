package nl.openvalue.bookstore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Test listener for the running-low event, registered in the shared integration context by {@link IntegrationTestBase}
 * and reset before every test. It records the events delivered after a commit and after a rollback, and on request it
 * fails the transaction just before the commit, so a test can prove that a rolled back stocktake delivers nothing.
 * The listeners only record; tests check the copies in the test thread.
 */
public class StockEventRecorder {

    private final CopyOnWriteArrayList<StockRunningLowEvent> committed = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<StockRunningLowEvent> rolledBack = new CopyOnWriteArrayList<>();

    private volatile boolean failBeforeCommit;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void failBeforeCommitWhenAsked(StockRunningLowEvent event) {
        if (failBeforeCommit) {
            throw new IllegalStateException("Simulated failure before commit");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void recordCommitted(StockRunningLowEvent event) {
        committed.add(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    void recordRolledBack(StockRunningLowEvent event) {
        rolledBack.add(event);
    }

    /** The events delivered after a commit since the last reset, as a copy. */
    public List<StockRunningLowEvent> committed() {
        return List.copyOf(committed);
    }

    /** The events delivered after a rollback since the last reset, as a copy. */
    public List<StockRunningLowEvent> rolledBack() {
        return List.copyOf(rolledBack);
    }

    /** Makes every following transaction that publishes a running-low event fail just before its commit. */
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
