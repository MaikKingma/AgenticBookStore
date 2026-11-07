package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;

/** One book in full: the authors as an array in their order, the price as a plain decimal string. */
public record BookDetailView(UUID id, String isbn, String title, List<String> authors, String genre, String price,
        String currency, String status, int publicationYear) {

    public static BookDetailView from(BookDTO book) {
        return new BookDetailView(book.id(), book.isbn(), book.title(), book.authors(), book.genre(),
                book.priceAmount().toPlainString(), book.priceCurrency(), book.status(), book.publicationYear());
    }
}
