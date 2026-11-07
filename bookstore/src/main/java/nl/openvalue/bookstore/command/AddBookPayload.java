package nl.openvalue.bookstore.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Request body for adding a book to the catalogue. Only the shape is checked here; the business ranges (price,
 * number of authors, publication year) are enforced by the domain.
 */
public record AddBookPayload(
        @NotBlank String isbn,
        @NotBlank String title,
        @NotEmpty List<@NotBlank String> authors,
        @NotBlank String genre,
        @NotBlank @Pattern(regexp = "-?\\d+(\\.\\d+)?") String price,
        @NotNull Integer publicationYear) {
}
