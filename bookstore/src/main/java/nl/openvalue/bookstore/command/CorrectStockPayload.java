package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for correcting the stock on hand after a stocktake. Unlike the catalogue payloads, this one repeats the
 * domain ranges (on hand 0 to 10000, reason 3 to 200 characters) because an invalid quantity is a 400 and never
 * reaches the domain.
 */
public record CorrectStockPayload(
        @NotNull @Min(0) @Max(10000) Integer onHand,
        @NotBlank @Size(min = 3, max = 200) String reason) {
}
