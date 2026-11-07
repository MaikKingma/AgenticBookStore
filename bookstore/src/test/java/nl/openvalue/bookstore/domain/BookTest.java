package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class BookTest {

    // The current year is 2026, so the latest allowed publication year is 2027.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final Isbn ISBN = new Isbn("9780141439518");
    private static final Title TITLE = new Title("Pride and Prejudice");
    private static final List<AuthorName> AUTHORS = List.of(new AuthorName("Jane Austen"));
    private static final Money PRICE = Money.of("9.99");
    private static final int YEAR = 1813;

    @Test
    void addToCatalogueCreatesAnAvailableBookWithANewId() {
        Book book = Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK);

        assertThat(book.id()).isNotNull();
        assertThat(book.id().value()).isNotNull();
        assertThat(book.isbn()).isEqualTo(ISBN);
        assertThat(book.title()).isEqualTo(TITLE);
        assertThat(book.authors()).containsExactlyElementsOf(AUTHORS);
        assertThat(book.genre()).isEqualTo(Genre.FICTION);
        assertThat(book.price()).isEqualTo(PRICE);
        assertThat(book.publicationYear()).isEqualTo(YEAR);
        assertThat(book.status()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void addToCatalogueGivesEveryBookItsOwnId() {
        Book first = Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK);
        Book second = Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK);

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void acceptsOneAuthor() {
        assertThat(bookWithAuthors(authors(1)).authors()).hasSize(1);
    }

    @Test
    void acceptsFiveAuthors() {
        assertThat(bookWithAuthors(authors(5)).authors()).containsExactlyElementsOf(authors(5));
    }

    @Test
    void rejectsABookWithoutAuthors() {
        assertRejected(() -> bookWithAuthors(List.of()), AuthorCountOutOfRangeException.class);
    }

    @Test
    void rejectsABookWithSixAuthors() {
        assertRejected(() -> bookWithAuthors(authors(6)), AuthorCountOutOfRangeException.class);
    }

    @Test
    void keepsTheAuthorsInTheirGivenOrder() {
        List<AuthorName> unordered = List.of(
                new AuthorName("Mary Shelley"), new AuthorName("Jane Austen"), new AuthorName("Charlotte Bronte"));

        assertThat(bookWithAuthors(unordered).authors()).containsExactlyElementsOf(unordered);
    }

    @Test
    void keepsItsOwnCopyOfTheAuthors() {
        List<AuthorName> callersList = new ArrayList<>(List.of(new AuthorName("Jane Austen")));
        Book book = bookWithAuthors(callersList);

        callersList.add(new AuthorName("Mary Shelley"));
        callersList.set(0, new AuthorName("Charlotte Bronte"));

        assertThat(book.authors()).containsExactly(new AuthorName("Jane Austen"));
    }

    @Test
    void doesNotLetTheAuthorsBeChanged() {
        Book book = bookWithAuthors(AUTHORS);

        assertThatThrownBy(() -> book.authors().add(new AuthorName("Mary Shelley")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(book.authors()).containsExactlyElementsOf(AUTHORS);
    }

    @Test
    void acceptsTheLowestPrice() {
        assertThat(bookWithPrice(Money.of("0.01")).price()).isEqualTo(Money.of("0.01"));
    }

    @Test
    void acceptsTheHighestPrice() {
        assertThat(bookWithPrice(Money.of("500.00")).price()).isEqualTo(Money.of("500.00"));
    }

    @Test
    void rejectsAPriceOfZero() {
        assertRejected(() -> bookWithPrice(Money.of("0.00")), PriceOutOfRangeException.class);
    }

    @Test
    void rejectsAPriceAboveTheHighestPrice() {
        assertRejected(() -> bookWithPrice(Money.of("500.01")), PriceOutOfRangeException.class);
    }

    @Test
    void acceptsTheEarliestPublicationYear() {
        assertThat(bookFrom(1450).publicationYear()).isEqualTo(1450);
    }

    @Test
    void acceptsNextYearAsPublicationYear() {
        assertThat(bookFrom(2027).publicationYear()).isEqualTo(2027);
    }

    @Test
    void rejectsAPublicationYearBeforeTheEarliest() {
        assertRejected(() -> bookFrom(1449), PublicationYearOutOfRangeException.class);
    }

    @Test
    void rejectsAPublicationYearAfterNextYear() {
        assertRejected(() -> bookFrom(2028), PublicationYearOutOfRangeException.class);
    }

    @Test
    void takesTheLatestPublicationYearFromTheClock() {
        Clock in2030 = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);

        assertThat(Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, 2031, in2030).publicationYear())
                .isEqualTo(2031);
        assertRejected(() -> Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, 2032, in2030),
                PublicationYearOutOfRangeException.class);
    }

    @Test
    void changePriceUpdatesTheAvailableBooksPrice() {
        Book book = availableBook();

        book.changePrice(Money.of("12.50"));

        assertThat(book.price()).isEqualTo(Money.of("12.50"));
        assertThat(book.status()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void changePriceAcceptsTheBoundaryPrices() {
        Book book = availableBook();

        book.changePrice(Money.of("500.00"));
        assertThat(book.price()).isEqualTo(Money.of("500.00"));

        book.changePrice(Money.of("0.01"));
        assertThat(book.price()).isEqualTo(Money.of("0.01"));
    }

    @Test
    void changePriceRejectsAPriceOfZeroAndKeepsTheOldPrice() {
        Book book = availableBook();

        assertRejected(() -> book.changePrice(Money.of("0.00")), PriceOutOfRangeException.class);

        assertThat(book.price()).isEqualTo(PRICE);
    }

    @Test
    void changePriceRejectsAPriceAboveTheHighestAndKeepsTheOldPrice() {
        Book book = availableBook();

        assertRejected(() -> book.changePrice(Money.of("500.01")), PriceOutOfRangeException.class);

        assertThat(book.price()).isEqualTo(PRICE);
    }

    @Test
    void changePriceOnADiscontinuedBookIsRejected() {
        Book book = availableBook();
        book.discontinue();

        assertRejected(() -> book.changePrice(Money.of("12.50")), BookDiscontinuedException.class);

        assertThat(book.price()).isEqualTo(PRICE);
    }

    @Test
    void discontinuedCheckPrecedesThePriceRange() {
        Book book = availableBook();
        book.discontinue();

        assertRejected(() -> book.changePrice(Money.of("500.01")), BookDiscontinuedException.class);
        assertRejected(() -> book.changePrice(Money.of("0.00")), BookDiscontinuedException.class);

        assertThat(book.price()).isEqualTo(PRICE);
    }

    @Test
    void discontinueMarksTheBookDiscontinued() {
        Book book = availableBook();

        book.discontinue();

        assertThat(book.status()).isEqualTo(BookStatus.DISCONTINUED);
    }

    @Test
    void discontinuingTwiceIsANoOp() {
        Book book = availableBook();
        book.discontinue();

        assertThatCode(book::discontinue).doesNotThrowAnyException();

        assertThat(book.status()).isEqualTo(BookStatus.DISCONTINUED);
        assertThat(book.price()).isEqualTo(PRICE);
    }

    @Test
    void restoreKeepsIdAndStatus() {
        BookId id = new BookId(UUID.fromString("0b6f1c2e-6a3d-4f4e-9a51-2f7c1d0e8b11"));
        List<AuthorName> authors = List.of(new AuthorName("Mary Shelley"), new AuthorName("Jane Austen"));

        Book book = Book.restore(id, ISBN, TITLE, authors, Genre.ROMANCE, PRICE, YEAR, BookStatus.DISCONTINUED,
                CLOCK);

        assertThat(book.id()).isEqualTo(id);
        assertThat(book.status()).isEqualTo(BookStatus.DISCONTINUED);
        assertThat(book.isbn()).isEqualTo(ISBN);
        assertThat(book.title()).isEqualTo(TITLE);
        assertThat(book.authors()).containsExactlyElementsOf(authors);
        assertThat(book.genre()).isEqualTo(Genre.ROMANCE);
        assertThat(book.price()).isEqualTo(PRICE);
        assertThat(book.publicationYear()).isEqualTo(YEAR);
    }

    @Test
    void restoreKeepsAnAvailableStatus() {
        BookId id = BookId.newId();

        Book book = Book.restore(id, ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, BookStatus.AVAILABLE, CLOCK);

        assertThat(book.id()).isEqualTo(id);
        assertThat(book.status()).isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    void restoreStillValidatesInvariantsForTheAuthorCount() {
        assertRejected(() -> restoreWith(authors(6), PRICE, YEAR), AuthorCountOutOfRangeException.class);
        assertRejected(() -> restoreWith(List.of(), PRICE, YEAR), AuthorCountOutOfRangeException.class);
    }

    @Test
    void restoreStillValidatesInvariantsForThePrice() {
        assertRejected(() -> restoreWith(AUTHORS, Money.of("0.00"), YEAR), PriceOutOfRangeException.class);
        assertRejected(() -> restoreWith(AUTHORS, Money.of("500.01"), YEAR), PriceOutOfRangeException.class);
    }

    @Test
    void restoreStillValidatesInvariantsForThePublicationYear() {
        assertRejected(() -> restoreWith(AUTHORS, PRICE, 2028), PublicationYearOutOfRangeException.class);
        assertRejected(() -> restoreWith(AUTHORS, PRICE, 1449), PublicationYearOutOfRangeException.class);
    }

    @Test
    void newIdGivesADifferentIdEachTime() {
        assertThat(BookId.newId()).isNotEqualTo(BookId.newId());
    }

    @Test
    void bookIdRejectsAMissingValue() {
        assertRejected(() -> new BookId(null), NullPointerException.class);
    }

    @Test
    void addToCatalogueRejectsAMissingIsbn() {
        assertRejected(() -> Book.addToCatalogue(null, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK),
                NullPointerException.class);
    }

    @Test
    void addToCatalogueRejectsAMissingTitle() {
        assertRejected(() -> Book.addToCatalogue(ISBN, null, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK),
                NullPointerException.class);
    }

    @Test
    void addToCatalogueRejectsAMissingGenre() {
        assertRejected(() -> Book.addToCatalogue(ISBN, TITLE, AUTHORS, null, PRICE, YEAR, CLOCK),
                NullPointerException.class);
    }

    @Test
    void restoreRejectsAMissingId() {
        assertRejected(() -> Book.restore(null, ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR,
                BookStatus.AVAILABLE, CLOCK), NullPointerException.class);
    }

    @Test
    void restoreRejectsAMissingStatus() {
        assertRejected(() -> Book.restore(BookId.newId(), ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, null,
                CLOCK), NullPointerException.class);
    }

    private static Book availableBook() {
        return Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, YEAR, CLOCK);
    }

    private static Book bookWithAuthors(List<AuthorName> authors) {
        return Book.addToCatalogue(ISBN, TITLE, authors, Genre.FICTION, PRICE, YEAR, CLOCK);
    }

    private static Book bookWithPrice(Money price) {
        return Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, price, YEAR, CLOCK);
    }

    private static Book bookFrom(int publicationYear) {
        return Book.addToCatalogue(ISBN, TITLE, AUTHORS, Genre.FICTION, PRICE, publicationYear, CLOCK);
    }

    private static Book restoreWith(List<AuthorName> authors, Money price, int publicationYear) {
        return Book.restore(BookId.newId(), ISBN, TITLE, authors, Genre.FICTION, price, publicationYear,
                BookStatus.AVAILABLE, CLOCK);
    }

    private static List<AuthorName> authors(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> new AuthorName("Author " + i)).toList();
    }

    private static void assertRejected(ThrowingCallable call, Class<? extends RuntimeException> expected) {
        assertThatThrownBy(call)
                .isInstanceOf(expected)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
