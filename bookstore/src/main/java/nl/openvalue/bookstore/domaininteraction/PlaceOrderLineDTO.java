package nl.openvalue.bookstore.domaininteraction;

import java.util.UUID;

/** One requested line of a new order as it enters the flow: the book and the number of copies. */
public record PlaceOrderLineDTO(UUID bookId, int quantity) {
}
