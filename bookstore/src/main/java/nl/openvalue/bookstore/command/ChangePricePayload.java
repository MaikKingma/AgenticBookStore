package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for changing the price of a book. The price is a decimal string such as "12.50". */
public record ChangePricePayload(@NotBlank @Pattern(regexp = "\\d+(\\.\\d+)?") String price) {
}
