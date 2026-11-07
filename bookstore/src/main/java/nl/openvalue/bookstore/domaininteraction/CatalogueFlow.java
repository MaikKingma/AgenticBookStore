package nl.openvalue.bookstore.domaininteraction;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import nl.openvalue.bookstore.domain.AuthorCountOutOfRangeException;
import nl.openvalue.bookstore.domain.AuthorName;
import nl.openvalue.bookstore.domain.Book;
import nl.openvalue.bookstore.domain.BookDiscontinuedException;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domain.InvalidAuthorNameException;
import nl.openvalue.bookstore.domain.InvalidIsbnException;
import nl.openvalue.bookstore.domain.InvalidMoneyException;
import nl.openvalue.bookstore.domain.InvalidTitleException;
import nl.openvalue.bookstore.domain.Isbn;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.PriceOutOfRangeException;
import nl.openvalue.bookstore.domain.PublicationYearOutOfRangeException;
import nl.openvalue.bookstore.domain.Title;
import nl.openvalue.bookstore.domain.UnknownGenreException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The catalogue use cases. Every domain exception is translated here into one of the four use-case exceptions; port
 * exceptions pass through unchanged.
 */
@Service
@Transactional
public class CatalogueFlow {

    private static final int SEARCH_LIMIT = 100;

    private final BookDataService books;
    private final Clock clock;

    public CatalogueFlow(BookDataService books, Clock clock) {
        this.books = books;
        this.clock = clock;
    }

    /**
     * Adds an available book and returns its id. Every value is checked before the ISBN is looked up, so an invalid
     * request is rejected as such even when its ISBN is already in the catalogue.
     */
    public UUID addBook(String isbn, String title, List<String> authors, String genre, BigDecimal price,
            int publicationYear) {
        Book book = translatingDomainExceptions(() -> Book.addToCatalogue(new Isbn(isbn), new Title(title),
                authors.stream().map(AuthorName::new).toList(), Genre.parse(genre), Money.of(price),
                publicationYear, clock));
        String normalisedIsbn = book.isbn().value();
        books.findByIsbn(normalisedIsbn).ifPresent(existing -> {
            throw new BookDataService.IsbnAlreadyExistsException(normalisedIsbn);
        });
        books.save(new BookDTO(book));
        return book.id().value();
    }

    /** Changes the price of a book; the book decides whether its price may change. */
    public void changePrice(UUID bookId, BigDecimal price) {
        BookDTO stored = books.findById(bookId);
        Book book = translatingDomainExceptions(() -> {
            Book restored = BookDomainMapper.toDomain(stored, clock);
            restored.changePrice(Money.of(price));
            return restored;
        });
        books.save(new BookDTO(book));
    }

    /** Discontinues a book; discontinuing a discontinued book changes nothing. */
    public void discontinue(UUID bookId) {
        BookDTO stored = books.findById(bookId);
        Book book = translatingDomainExceptions(() -> {
            Book restored = BookDomainMapper.toDomain(stored, clock);
            restored.discontinue();
            return restored;
        });
        books.save(new BookDTO(book));
    }

    /**
     * Searches the catalogue for at most {@value #SEARCH_LIMIT} books. Surrounding whitespace is stripped from each
     * criterion and a blank criterion counts as absent; a genre is matched by its canonical name.
     */
    @Transactional(readOnly = true)
    public List<BookDTO> search(String q, String title, String author, String genre) {
        String genreCriterion = absentWhenBlank(genre);
        String genreName = genreCriterion == null
                ? null
                : translatingDomainExceptions(() -> Genre.parse(genreCriterion).name());
        return books.search(absentWhenBlank(q), absentWhenBlank(title), absentWhenBlank(author), genreName,
                SEARCH_LIMIT);
    }

    @Transactional(readOnly = true)
    public BookDTO findById(UUID bookId) {
        return books.findById(bookId);
    }

    private static String absentWhenBlank(String criterion) {
        return criterion == null || criterion.isBlank() ? null : criterion.strip();
    }

    /** Runs a call into the domain and translates any domain exception it throws. */
    private static <T> T translatingDomainExceptions(Supplier<T> domainCall) {
        try {
            return domainCall.get();
        } catch (InvalidIsbnException e) {
            throw invalidField("invalid-isbn", "Invalid ISBN", "isbn", e);
        } catch (UnknownGenreException e) {
            throw unknownGenre(e);
        } catch (PriceOutOfRangeException | InvalidMoneyException e) {
            throw invalidField("price-out-of-range", "Price out of range", "price", e);
        } catch (InvalidTitleException e) {
            throw invalidField("invalid-title", "Invalid title", "title", e);
        } catch (InvalidAuthorNameException e) {
            throw invalidField("invalid-author-name", "Invalid author name", "authors", e);
        } catch (AuthorCountOutOfRangeException e) {
            throw invalidField("author-count-out-of-range", "Number of authors out of range", "authors", e);
        } catch (PublicationYearOutOfRangeException e) {
            throw invalidField("publication-year-out-of-range", "Publication year out of range", "publicationYear",
                    e);
        } catch (BookDiscontinuedException e) {
            throw new ConflictException("book-discontinued", "Book discontinued", e.getMessage());
        }
    }

    private static InvalidInputException invalidField(String code, String title, String field,
            RuntimeException domainException) {
        String message = domainException.getMessage();
        return new InvalidInputException(code, title, message, List.of(Map.of("field", field, "message", message)));
    }

    private static InvalidInputException unknownGenre(UnknownGenreException domainException) {
        List<Map<String, String>> validGenres = Arrays.stream(Genre.values())
                .map(genre -> Map.of("genre", genre.name()))
                .toList();
        return new InvalidInputException("unknown-genre", "Unknown genre", domainException.getMessage(), validGenres);
    }
}
