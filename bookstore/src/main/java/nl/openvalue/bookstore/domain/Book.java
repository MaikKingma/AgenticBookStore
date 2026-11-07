package nl.openvalue.bookstore.domain;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Objects;

/**
 * A book in the catalogue. New books come from {@link #addToCatalogue}, stored books from {@link #restore}; both
 * check every invariant. The clock only sets the latest allowed publication year and is not kept.
 */
public final class Book {

    private static final int MIN_AUTHORS = 1;
    private static final int MAX_AUTHORS = 5;
    private static final Money PRICE_LOWER_BOUND_EXCLUSIVE = Money.of("0.00");
    private static final Money PRICE_UPPER_BOUND_INCLUSIVE = Money.of("500.00");
    private static final int EARLIEST_PUBLICATION_YEAR = 1450;

    private final BookId id;
    private final Isbn isbn;
    private final Title title;
    private final List<AuthorName> authors;
    private final Genre genre;
    private Money price;
    private final int publicationYear;
    private BookStatus status;

    private Book(BookId id, Isbn isbn, Title title, List<AuthorName> authors, Genre genre, Money price,
            int publicationYear, BookStatus status, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.isbn = Objects.requireNonNull(isbn, "isbn");
        this.title = Objects.requireNonNull(title, "title");
        this.authors = List.copyOf(Objects.requireNonNull(authors, "authors"));
        this.genre = Objects.requireNonNull(genre, "genre");
        this.price = Objects.requireNonNull(price, "price");
        this.publicationYear = publicationYear;
        this.status = Objects.requireNonNull(status, "status");
        requireAuthorCountInRange(this.authors);
        requirePriceInRange(price);
        requirePublicationYearInRange(publicationYear, Objects.requireNonNull(clock, "clock"));
    }

    /** Adds a new, available book to the catalogue with a new id. */
    public static Book addToCatalogue(Isbn isbn, Title title, List<AuthorName> authors, Genre genre, Money price,
            int publicationYear, Clock clock) {
        return new Book(BookId.newId(), isbn, title, authors, genre, price, publicationYear, BookStatus.AVAILABLE,
                clock);
    }

    /** Rebuilds a stored book with its id and status, checking every invariant again. */
    public static Book restore(BookId id, Isbn isbn, Title title, List<AuthorName> authors, Genre genre,
            Money price, int publicationYear, BookStatus status, Clock clock) {
        return new Book(id, isbn, title, authors, genre, price, publicationYear, status, clock);
    }

    /** Changes the price of an available book. A discontinued book keeps its price, whatever the new price is. */
    public void changePrice(Money newPrice) {
        if (status == BookStatus.DISCONTINUED) {
            throw new BookDiscontinuedException(id);
        }
        requirePriceInRange(newPrice);
        this.price = newPrice;
    }

    /** Marks the book discontinued; discontinuing a discontinued book changes nothing. */
    public void discontinue() {
        this.status = BookStatus.DISCONTINUED;
    }

    public BookId id() {
        return id;
    }

    public Isbn isbn() {
        return isbn;
    }

    public Title title() {
        return title;
    }

    public List<AuthorName> authors() {
        return authors;
    }

    public Genre genre() {
        return genre;
    }

    public Money price() {
        return price;
    }

    public int publicationYear() {
        return publicationYear;
    }

    public BookStatus status() {
        return status;
    }

    private static void requireAuthorCountInRange(List<AuthorName> authors) {
        if (authors.size() < MIN_AUTHORS || authors.size() > MAX_AUTHORS) {
            throw new AuthorCountOutOfRangeException(authors.size(), MIN_AUTHORS, MAX_AUTHORS);
        }
    }

    private static void requirePriceInRange(Money price) {
        if (!price.isGreaterThan(PRICE_LOWER_BOUND_EXCLUSIVE) || price.isGreaterThan(PRICE_UPPER_BOUND_INCLUSIVE)) {
            throw new PriceOutOfRangeException(price, PRICE_LOWER_BOUND_EXCLUSIVE, PRICE_UPPER_BOUND_INCLUSIVE);
        }
    }

    private static void requirePublicationYearInRange(int publicationYear, Clock clock) {
        int latest = Year.now(clock).getValue() + 1;
        if (publicationYear < EARLIEST_PUBLICATION_YEAR || publicationYear > latest) {
            throw new PublicationYearOutOfRangeException(publicationYear, EARLIEST_PUBLICATION_YEAR, latest);
        }
    }
}
