package nl.openvalue.bookstore.domaininteraction;

import java.time.LocalDate;

/**
 * The distributor's answer to a restock request: the id of its restock order, the quantity ordered, the date it
 * expects to deliver and whether the order is backordered.
 */
public record RestockConfirmationDTO(String restockOrderId, int quantity, LocalDate expectedDeliveryDate,
        boolean backordered) {
}
