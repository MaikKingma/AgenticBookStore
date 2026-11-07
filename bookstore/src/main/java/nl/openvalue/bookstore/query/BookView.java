package nl.openvalue.bookstore.query;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * A book as one row of a search result: the authors joined into one line, the price as a plain decimal string, the
 * available stock and whether the book is in stock (more than zero available). The publication year is only in the
 * detail view.
 */
public record BookView(UUID id, String isbn, String title, String authors, String genre, String price,
        String currency, String status, int available, boolean inStock) {

    public static BookView from(BookDTO book, StockItemDTO stock) {
        return new BookView(book.id(), book.isbn(), book.title(), String.join(", ", book.authors()), book.genre(),
                book.priceAmount().toPlainString(), book.priceCurrency(), book.status(), stock.available(),
                stock.available() > 0);
    }
}
