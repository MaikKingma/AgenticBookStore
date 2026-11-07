package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domain.AuthorName;
import nl.openvalue.bookstore.domain.Book;
import nl.openvalue.bookstore.domain.BookStatus;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domain.InvalidMoneyException;
import nl.openvalue.bookstore.domain.Isbn;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.PriceOutOfRangeException;
import nl.openvalue.bookstore.domain.PublicationYearOutOfRangeException;
import nl.openvalue.bookstore.domain.Title;
import org.junit.jupiter.api.Test;

class BookDomainMapperTest {

    // The current year is 2026, so the latest allowed publication year is 2027.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final UUID ID = UUID.fromString("0b8f6a52-6d4e-4a53-9f43-2f1a8f3b7c10");

    // The authors are deliberately not in alphabetical order, so a mapper that sorts or reverses them is caught.
    private static final List<String> AUTHORS = List.of("Toni Morrison", "Chinua Achebe", "Jane Austen");

    private static BookDTO dto(String status) {
        return new BookDTO(ID, "9780747532743", "The Great Reading List", AUTHORS, "SCIENCE_FICTION",
                new BigDecimal("12.50"), "EUR", 1999, status);
    }

    @Test
    void mapsAnAvailableDtoToTheDomainAndBackToAnEqualDto() {
        BookDTO original = dto("AVAILABLE");

        Book book = BookDomainMapper.toDomain(original, CLOCK);

        assertThat(book.status()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(new BookDTO(book)).isEqualTo(original);
    }

    @Test
    void mapsADiscontinuedDtoToTheDomainAndBackToAnEqualDto() {
        BookDTO original = dto("DISCONTINUED");

        Book book = BookDomainMapper.toDomain(original, CLOCK);

        assertThat(book.status()).isEqualTo(BookStatus.DISCONTINUED);
        assertThat(new BookDTO(book)).isEqualTo(original);
    }

    @Test
    void restoresEveryFieldOfTheDtoIntoTheBook() {
        Book book = BookDomainMapper.toDomain(dto("AVAILABLE"), CLOCK);

        assertThat(book.id().value()).isEqualTo(ID);
        assertThat(book.isbn()).isEqualTo(new Isbn("9780747532743"));
        assertThat(book.title()).isEqualTo(new Title("The Great Reading List"));
        assertThat(book.authors()).containsExactly(
                new AuthorName("Toni Morrison"), new AuthorName("Chinua Achebe"), new AuthorName("Jane Austen"));
        assertThat(book.genre()).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(book.price()).isEqualTo(Money.of("12.50"));
        assertThat(book.publicationYear()).isEqualTo(1999);
    }

    @Test
    void buildsTheDtoOfANewBookFromEveryField() {
        Book book = Book.addToCatalogue(new Isbn("9780261103573"), new Title("The Fellowship of the Ring"),
                List.of(new AuthorName("J. R. R. Tolkien"), new AuthorName("Alan Lee")), Genre.FANTASY,
                Money.of("7.00"), 1954, CLOCK);

        BookDTO dto = new BookDTO(book);

        assertThat(dto.id()).isEqualTo(book.id().value());
        assertThat(dto.isbn()).isEqualTo("9780261103573");
        assertThat(dto.title()).isEqualTo("The Fellowship of the Ring");
        assertThat(dto.authors()).containsExactly("J. R. R. Tolkien", "Alan Lee");
        assertThat(dto.genre()).isEqualTo("FANTASY");
        assertThat(dto.priceAmount()).isEqualTo(new BigDecimal("7.00"));
        assertThat(dto.priceCurrency()).isEqualTo("EUR");
        assertThat(dto.publicationYear()).isEqualTo(1954);
        assertThat(dto.status()).isEqualTo("AVAILABLE");
    }

    @Test
    void dtoKeepsACopyOfTheAuthorsSoLaterChangesToTheSourceListDoNotShowThrough() {
        List<String> authors = new ArrayList<>(AUTHORS);
        BookDTO dto = new BookDTO(ID, "9780747532743", "A Title", authors, "FICTION", new BigDecimal("5.00"), "EUR",
                2001, "AVAILABLE");

        authors.add("Someone Else");

        assertThat(dto.authors()).containsExactlyElementsOf(AUTHORS);
        assertThatThrownBy(() -> dto.authors().add("Another"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void dtoRejectsNullAuthors() {
        assertThatThrownBy(() -> new BookDTO(ID, "9780747532743", "A Title", null, "FICTION", new BigDecimal("5.00"),
                "EUR", 2001, "AVAILABLE")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void stillAppliesTheBookInvariantsWhenRestoring() {
        BookDTO free = new BookDTO(ID, "9780747532743", "A Title", AUTHORS, "FICTION", new BigDecimal("0.00"), "EUR",
                1999, "AVAILABLE");

        assertThatThrownBy(() -> BookDomainMapper.toDomain(free, CLOCK))
                .isInstanceOf(PriceOutOfRangeException.class);
    }

    @Test
    void usesTheGivenClockForThePublicationYearBound() {
        BookDTO inTheFuture = new BookDTO(ID, "9780747532743", "A Title", AUTHORS, "FICTION", new BigDecimal("5.00"),
                "EUR", 2028, "AVAILABLE");

        assertThatThrownBy(() -> BookDomainMapper.toDomain(inTheFuture, CLOCK))
                .isInstanceOf(PublicationYearOutOfRangeException.class);
    }

    @Test
    void restoresThePriceInTheCurrencyOfTheDto() {
        BookDTO dollars = new BookDTO(ID, "9780747532743", "A Title", AUTHORS, "FICTION", new BigDecimal("5.00"),
                "USD", 1999, "AVAILABLE");

        assertThatThrownBy(() -> BookDomainMapper.toDomain(dollars, CLOCK))
                .isInstanceOf(InvalidMoneyException.class);
    }
}
