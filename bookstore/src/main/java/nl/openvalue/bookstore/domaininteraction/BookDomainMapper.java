package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import nl.openvalue.bookstore.domain.AuthorName;
import nl.openvalue.bookstore.domain.Book;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.BookStatus;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domain.Isbn;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.Title;

/** Rebuilds a {@link Book} from a {@link BookDTO}; the book checks every invariant again. */
public final class BookDomainMapper {

    private BookDomainMapper() {
    }

    /** The clock only sets the latest allowed publication year while the book is restored. */
    public static Book toDomain(BookDTO dto, Clock clock) {
        return Book.restore(
                new BookId(dto.id()),
                new Isbn(dto.isbn()),
                new Title(dto.title()),
                dto.authors().stream().map(AuthorName::new).toList(),
                Genre.valueOf(dto.genre()),
                Money.of(dto.priceAmount(), dto.priceCurrency()),
                dto.publicationYear(),
                BookStatus.valueOf(dto.status()),
                clock);
    }
}
