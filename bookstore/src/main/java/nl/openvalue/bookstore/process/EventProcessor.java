package nl.openvalue.bookstore.process;

import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to domain events once the transaction that registered them has committed, and hands each event to the
 * delegate for its action. A rolled back transaction delivers nothing. The processor opens no transaction of its own
 * and handles no failure: the delegate runs in a new transaction, and Spring logs whatever escapes it.
 */
@Component
public class EventProcessor {

    private final RequestRestockDelegate requestRestock;

    public EventProcessor(RequestRestockDelegate requestRestock) {
        this.requestRestock = requestRestock;
    }

    /** Hands a running-low event to the restock delegate after the commit that registered it. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockRunningLow(StockRunningLowEvent event) {
        requestRestock.handle(event);
    }
}
