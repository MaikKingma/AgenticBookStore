package nl.openvalue.bookstore.domaininteraction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domain.AuthorName;
import nl.openvalue.bookstore.domain.Book;

/**
 * A book as it crosses a port: JDK types only. The price is an amount plus a currency code, the genre and the status
 * are enum names. The authors keep their order.
 */
public record BookDTO(UUID id, String isbn, String title, List<String> authors, String genre,
        BigDecimal priceAmount, String priceCurrency, int publicationYear, String status) {

    public BookDTO {
        authors = List.copyOf(authors);
    }

    public BookDTO(Book book) {
        this(book.id().value(), book.isbn().value(), book.title().value(),
                book.authors().stream().map(AuthorName::value).toList(), book.genre().name(),
                book.price().amount(), book.price().currency().getCurrencyCode(), book.publicationYear(),
                book.status().name());
    }
}
