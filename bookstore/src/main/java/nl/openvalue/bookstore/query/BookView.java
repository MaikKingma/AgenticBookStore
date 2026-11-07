package nl.openvalue.bookstore.query;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;

/**
 * A book as one row of a search result: the authors joined into one line, the price as a plain decimal string.
 * The publication year is only in the detail view.
 */
public record BookView(UUID id, String isbn, String title, String authors, String genre, String price,
        String currency, String status) {

    public static BookView from(BookDTO book) {
        return new BookView(book.id(), book.isbn(), book.title(), String.join(", ", book.authors()), book.genre(),
                book.priceAmount().toPlainString(), book.priceCurrency(), book.status());
    }
}
