package nl.openvalue.bookstore.assistant;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * A book as the assistant's tools show it to the model: the price is a plain decimal string in EUR, the genre an enum
 * name, the authors keep their order, and the book is in stock when more than zero copies are available (the same rule
 * as the shop's own book views).
 */
public record BookSuggestionView(UUID id, String title, List<String> authors, String genre, String price,
        boolean inStock) {

    public BookSuggestionView {
        authors = List.copyOf(authors);
    }

    public static BookSuggestionView from(BookDTO book, StockItemDTO stock) {
        return new BookSuggestionView(book.id(), book.title(), book.authors(), book.genre(),
                book.priceAmount().toPlainString(), stock.available() > 0);
    }
}
