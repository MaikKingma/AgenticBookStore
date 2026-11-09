package nl.openvalue.bookstore.process;

import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asks the distributor to restock a book whose stock ran low. The event processor calls it after the transaction that
 * registered the event committed; it runs in a new transaction of its own and hands the book id to the inventory
 * flow, which handles the distributor's expected answers itself (unavailable: a warning and the low-stock signal
 * reset; title refused: a warning and the signal kept).
 *
 * <p>The costs of this design, accepted on purpose:
 * <ul>
 * <li>It runs synchronously on the request thread after the triggering transaction committed, so the response that
 * dipped the stock waits for the distributor: up to 2 seconds to connect plus 5 seconds to read per dipping book
 * (the defaults of {@code distributor.connect-timeout} and {@code distributor.read-timeout}). Several dipping books
 * in one order are handled one after the other.</li>
 * <li>During the call the request holds two JDBC connections: the committed outer one, which Spring releases only
 * after every after-commit listener finished, and the one of this new transaction. With the default pool of 10
 * connections, five such requests at once during a slow distributor fill the pool.</li>
 * <li>Exceptions never reach the caller. Any other failure on this path (a data error, an optimistic lock failure, a
 * pool timeout, a failed commit) is logged at ERROR by Spring, the order stays committed, and the low-stock signal
 * stays set without a pending restock, so no automatic retry follows. The recovery is the manual request-restock
 * command.</li>
 * <li>Phantom restock: when an optimistic lock failure or a failed commit happens after the distributor confirmed,
 * the distributor holds a restock order that the bookstore did not record. A manual request then orders a second
 * time.</li>
 * </ul>
 * Asynchronous execution or an outbox would remove these costs; both are out of scope.
 */
@Component
public class RequestRestockDelegate {

    private final InventoryFlow inventory;

    public RequestRestockDelegate(InventoryFlow inventory) {
        this.inventory = inventory;
    }

    /** Requests a restock of the event's book, in a new transaction that commits on its own. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(StockRunningLowEvent event) {
        inventory.handleStockRunningLow(event.bookId());
    }
}
