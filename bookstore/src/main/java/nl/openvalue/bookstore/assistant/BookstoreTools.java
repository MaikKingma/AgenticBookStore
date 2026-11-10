package nl.openvalue.bookstore.assistant;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The read-only tools the model may call. They use the catalogue and inventory flows only.
 *
 * <p>A failed tool call reaches the model as one short line, never a stack trace: a missing book or stock item and a
 * rejected genre keep the flow's own detail (an unknown genre gets the valid genres appended), a malformed argument
 * gets a fixed text, and anything else becomes "&lt;tool&gt; failed for a technical reason." while the exception is
 * logged here at WARN. Spring AI's default exception processor hands exactly that message to the model.
 */
@Component
public class BookstoreTools {

    private static final Logger LOG = LoggerFactory.getLogger(BookstoreTools.class);

    private static final int SEARCH_LIMIT = 10;
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int UUID_TEXT_LENGTH = 36;

    private static final String MALFORMED_ID = "Not a valid book id. Use an id returned by searchCatalogue or "
            + "recommendByGenre.";
    private static final String GENRE_REQUIRED = "A genre is required.";

    /** Newest publication first; equal years by title (natural String order), then by ISBN. */
    private static final Comparator<BookDTO> NEWEST_FIRST = Comparator.comparingInt(BookDTO::publicationYear)
            .reversed()
            .thenComparing(BookDTO::title)
            .thenComparing(BookDTO::isbn);

    private final CatalogueFlow catalogue;
    private final InventoryFlow inventory;

    public BookstoreTools(CatalogueFlow catalogue, InventoryFlow inventory) {
        this.catalogue = catalogue;
        this.inventory = inventory;
    }

    @Tool(description = "Search the bookstore catalogue by title or author, optionally filtered by genre. "
            + "Use this before mentioning any book. "
            + "The query is one title or one author name, or a fragment of one, never a title and an author together.")
    public List<BookSuggestionView> searchCatalogue(
            @ToolParam(description = "One title or one author name, or a fragment of one; "
                    + "never a title and an author together.") String query,
            @ToolParam(required = false) String genre) {
        return run("searchCatalogue", () -> catalogue.search(query, null, null, genre).stream()
                .limit(SEARCH_LIMIT)
                .map(book -> BookSuggestionView.from(book, inventory.findByBookId(book.id())))
                .toList());
    }

    @Tool(description = "Get details of one book from the catalogue by its id.")
    public BookDetailsView getBookDetails(String bookId) {
        return run("getBookDetails", () -> {
            UUID id = parseBookId(bookId);
            return BookDetailsView.from(catalogue.findById(id), inventory.findByBookId(id));
        });
    }

    @Tool(description = "Check how many copies are available and whether a restock is on its way.")
    public AvailabilityView checkAvailability(String bookId) {
        return run("checkAvailability",
                () -> AvailabilityView.from(inventory.findByBookId(parseBookId(bookId))));
    }

    @Tool(description = "Recommend in-stock books of a genre, newest publication first. limit max 5.")
    public List<BookSuggestionView> recommendByGenre(String genre, Integer limit) {
        return run("recommendByGenre", () -> {
            if (genre == null || genre.isBlank()) {
                throw new ToolFailure(GENRE_REQUIRED);
            }
            int cap = limit == null ? MAX_RECOMMENDATIONS : Math.clamp(limit, 1, MAX_RECOMMENDATIONS);
            return catalogue.search(null, null, null, genre).stream()
                    .sorted(NEWEST_FIRST)
                    .map(book -> new BookInStock(book, inventory.findByBookId(book.id())))
                    .filter(candidate -> candidate.stock().available() > 0)
                    .limit(cap)
                    .map(candidate -> BookSuggestionView.from(candidate.book(), candidate.stock()))
                    .toList();
        });
    }

    private static UUID parseBookId(String bookId) {
        if (bookId == null || bookId.length() != UUID_TEXT_LENGTH) {
            throw new ToolFailure(MALFORMED_ID);
        }
        try {
            return UUID.fromString(bookId);
        } catch (IllegalArgumentException e) {
            throw new ToolFailure(MALFORMED_ID);
        }
    }

    /** Runs a tool body and turns every failure into the one-line text the model gets. */
    private <T> T run(String tool, Supplier<T> body) {
        try {
            return body.get();
        } catch (ToolFailure e) {
            throw e;
        } catch (NotFoundException e) {
            throw new ToolFailure(e.getMessage());
        } catch (InvalidInputException e) {
            throw new ToolFailure(e.getMessage() + validGenres(e));
        } catch (RuntimeException e) {
            LOG.warn("Tool {} failed unexpectedly", tool, e);
            throw new ToolFailure(tool + " failed for a technical reason.");
        }
    }

    /** The genre names the flow listed in the details of its rejection, as a suffix; empty when it listed none. */
    private static String validGenres(InvalidInputException rejection) {
        List<String> genres = rejection.details().stream()
                .map(detail -> detail.get("genre"))
                .filter(Objects::nonNull)
                .toList();
        return genres.isEmpty() ? "" : " Valid genres: " + String.join(", ", genres);
    }

    /** A book with its stock, so the stock is looked up once per candidate. */
    private record BookInStock(BookDTO book, StockItemDTO stock) {
    }

    /** The text for the model; it carries no stack trace and no cause. */
    private static final class ToolFailure extends RuntimeException {

        ToolFailure(String message) {
            super(message, null, false, false);
        }
    }
}
