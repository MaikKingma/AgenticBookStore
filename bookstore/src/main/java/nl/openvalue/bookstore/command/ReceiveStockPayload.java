package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for receiving a delivery of a book. Unlike the catalogue payloads, this one repeats the domain range
 * (1 to 1000 per delivery) because an invalid quantity is a 400 and never reaches the domain.
 */
public record ReceiveStockPayload(@NotNull @Min(1) @Max(1000) Integer quantity) {
}
