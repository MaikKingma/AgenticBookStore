package nl.openvalue.distributor.catalog;

import java.time.LocalDate;

/**
 * What the distributor promises for a restock of a title: the order status and the expected delivery date.
 */
public record DeliveryQuote(RestockStatus status, LocalDate expectedDeliveryDate) {
}
