package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * One requested line of an order: a book and the number of copies. This payload repeats the domain rule (1 to 10
 * copies per line) so that a request the domain would refuse is a 400 and never reaches the domain.
 */
public record OrderLinePayload(@NotNull UUID bookId, @NotNull @Min(1) @Max(10) Integer quantity) {
}
