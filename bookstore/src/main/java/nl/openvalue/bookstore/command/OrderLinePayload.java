package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * One requested line of an order: a book and the number of copies. The payload only requires both fields. The flow
 * owns the 1 to 10 rule for the quantity, so that a quantity outside the range is a 422 and not a 400.
 */
public record OrderLinePayload(@NotNull UUID bookId, @NotNull Integer quantity) {
}
