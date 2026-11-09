package nl.openvalue.bookstore.query;

import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;

/**
 * The restock the distributor confirmed for a book and that no delivery has completed yet, as a reader sees it. The
 * expected delivery date is the text of the date in ISO form, for example {@code 2026-07-03}.
 */
public record PendingRestockView(String restockOrderId, int quantity, String expectedDeliveryDate,
        boolean backordered) {

    public static PendingRestockView from(PendingRestockDTO pendingRestock) {
        return new PendingRestockView(pendingRestock.restockOrderId(), pendingRestock.quantity(),
                pendingRestock.expectedDeliveryDate().toString(), pendingRestock.backordered());
    }
}
