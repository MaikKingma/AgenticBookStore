package nl.openvalue.bookstore.domaininteraction;

import java.time.LocalDate;
import nl.openvalue.bookstore.domain.PendingRestock;

/**
 * A pending restock as it crosses a port: JDK types only. It is the restock the distributor confirmed for a stock item
 * and that no delivery has completed yet. It checks nothing itself; the domain checks every value when a stock item is
 * restored from it.
 */
public record PendingRestockDTO(String restockOrderId, int quantity, LocalDate expectedDeliveryDate,
        boolean backordered) {

    public PendingRestockDTO(PendingRestock pendingRestock) {
        this(pendingRestock.restockOrderId(), pendingRestock.quantity(), pendingRestock.expectedDeliveryDate(),
                pendingRestock.backordered());
    }
}
