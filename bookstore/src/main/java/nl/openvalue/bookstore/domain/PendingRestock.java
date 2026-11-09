package nl.openvalue.bookstore.domain;

import java.time.LocalDate;

/**
 * A restock the distributor confirmed and that has not been delivered yet. The order id is a non-blank reference of at
 * most 64 characters, kept as given; the quantity is the confirmed quantity, at least 1; the expected delivery date is
 * required; {@code backordered} tells that the distributor has to wait for new stock.
 */
public record PendingRestock(String restockOrderId, int quantity, LocalDate expectedDeliveryDate,
        boolean backordered) {

    private static final int MAX_ORDER_ID_LENGTH = 64;
    private static final int MIN_QUANTITY = 1;

    public PendingRestock {
        if (restockOrderId == null || restockOrderId.isBlank()) {
            throw new InvalidPendingRestockException("A pending restock needs a restock order id");
        }
        if (restockOrderId.length() > MAX_ORDER_ID_LENGTH) {
            throw new InvalidPendingRestockException("A restock order id has at most " + MAX_ORDER_ID_LENGTH
                    + " characters, not " + restockOrderId.length());
        }
        if (quantity < MIN_QUANTITY) {
            throw new InvalidPendingRestockException(
                    "A pending restock has at least " + MIN_QUANTITY + " copy, not " + quantity);
        }
        if (expectedDeliveryDate == null) {
            throw new InvalidPendingRestockException("A pending restock needs an expected delivery date");
        }
    }
}
