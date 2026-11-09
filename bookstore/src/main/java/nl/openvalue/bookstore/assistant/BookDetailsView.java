package nl.openvalue.bookstore.assistant;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * One book in detail: everything {@link BookSuggestionView} shows plus the publication year. The stock rule for
 * {@code inStock} is the same as there.
 */
public record BookDetailsView(UUID id, String title, List<String> authors, String genre, String price,
        boolean inStock, int publicationYear) {

    public BookDetailsView {
        authors = List.copyOf(authors);
    }

    public static BookDetailsView from(BookDTO book, StockItemDTO stock) {
        return new BookDetailsView(book.id(), book.title(), book.authors(), book.genre(),
                book.priceAmount().toPlainString(), stock.available() > 0, book.publicationYear());
    }
}
