package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for importing a book from the distributor. Only the shape is checked here. The optional price is the
 * retailer's own price; leaving it out takes the distributor's recommended price. The business ranges of the price
 * and the form of the ISBN are enforced by the domain.
 */
public record ImportBookPayload(
        @NotBlank String isbn,
        @Pattern(regexp = "-?\\d+(\\.\\d+)?") String price) {
}
