package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nl.openvalue.bookstore.domain.AuthorName;
import nl.openvalue.bookstore.domain.Book;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domain.Isbn;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.Title;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogueFlowTest {

    // The current year is 2026, so the latest allowed publication year is 2027.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final Clock CLOCK_IN_2000 = Clock.fixed(Instant.parse("2000-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final String ISBN = "9780141439518";

    private static final UUID BOOK_ID = UUID.fromString("5d0c1a7e-3f2b-4c8d-9e6a-1b2c3d4e5f60");

    // U+2003 EM SPACE, built from its code point so the source file stays ASCII.
    private static final String EM_SPACE = Character.toString(0x2003);

    @Mock
    private BookDataService books;

    @Mock
    private StockItemDataService stockItems;

    @Captor
    private ArgumentCaptor<BookDTO> saved;

    @Captor
    private ArgumentCaptor<StockItemDTO> savedStockItem;

    private CatalogueFlow flow;

    @BeforeEach
    void setUp() {
        flow = new CatalogueFlow(books, stockItems, CLOCK);
    }

    @Test
    void addsAValidBookAsAvailableAndReturnsItsId() {
        UUID id = flow.addBook(ISBN, "  Pride and Prejudice ", List.of("Jane Austen", " Fanny Burney "), "fiction",
                new BigDecimal("12.5"), 1813);

        verify(books).findByIsbn(ISBN);
        verify(books).save(saved.capture());
        BookDTO dto = saved.getValue();
        assertThat(dto.id()).isNotNull().isEqualTo(id);
        assertThat(dto.isbn()).isEqualTo(ISBN);
        assertThat(dto.title()).isEqualTo("Pride and Prejudice");
        assertThat(dto.authors()).containsExactly("Jane Austen", "Fanny Burney");
        assertThat(dto.genre()).isEqualTo("FICTION");
        assertThat(dto.priceAmount()).isEqualByComparingTo("12.50");
        assertThat(dto.priceAmount().scale()).isEqualTo(2);
        assertThat(dto.priceCurrency()).isEqualTo("EUR");
        assertThat(dto.publicationYear()).isEqualTo(1813);
        assertThat(dto.status()).isEqualTo("AVAILABLE");
    }

    @Test
    void rejectsADuplicateIsbn() {
        BookDTO existing = storedBook("AVAILABLE");
        when(books.findByIsbn(ISBN)).thenReturn(Optional.of(existing));

        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "fiction", new BigDecimal("12.50"), 1813));

        assertThat(thrown).isInstanceOf(BookDataService.IsbnAlreadyExistsException.class);
        ConflictException conflict = (ConflictException) thrown;
        assertThat(conflict.code()).isEqualTo("isbn-already-exists");
        assertThat(conflict.title()).isEqualTo("ISBN already in catalogue");
        assertThat(conflict.details()).isEmpty();
        verify(books, never()).save(any());
        verifyNoInteractions(stockItems);
    }

    @Test
    void checksDuplicatesWithTheNormalisedIsbn() {
        flow.addBook("978-0-14-143951-8", "Pride and Prejudice", List.of("Jane Austen"), "fiction",
                new BigDecimal("12.50"), 1813);

        verify(books).findByIsbn("9780141439518");
        verify(books).save(saved.capture());
        assertThat(saved.getValue().isbn()).isEqualTo("9780141439518");
    }

    @Test
    void addBookOpensAnEmptyStockItemForTheNewBook() {
        UUID returnedId = flow.addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"), "fiction",
                new BigDecimal("12.50"), 1813);

        verify(stockItems).save(savedStockItem.capture());
        assertThat(savedStockItem.getValue()).isEqualTo(new StockItemDTO(returnedId, 0, 0, 3, 10, false));
    }

    @Test
    void addBookSavesTheBookBeforeItsStockItem() {
        flow.addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"), "fiction", new BigDecimal("12.50"), 1813);

        InOrder order = inOrder(books, stockItems);
        order.verify(books).findByIsbn(ISBN);
        order.verify(books).save(any());
        order.verify(stockItems).save(any());
    }

    @Test
    void rejectsAnInvalidFieldBeforeLookingForADuplicateIsbn() {
        lenient().when(books.findByIsbn(any())).thenReturn(Optional.of(storedBook("AVAILABLE")));
        String title = "x".repeat(201);

        Throwable thrown = catchThrowable(() -> addBook(ISBN, title, List.of("Jane Austen"), "fiction",
                new BigDecimal("12.50"), 1813));

        assertInvalidField(thrown, "invalid-title", "Invalid title", "title",
                domainMessage(() -> new Title(title)));
        verify(books, never()).findByIsbn(any());
        verify(books, never()).save(any());
    }

    @Test
    void rejectsAnInvalidIsbnWithFieldDetails() {
        String badChecksum = "9780141439519";

        Throwable thrown = catchThrowable(() -> addBook(badChecksum, "Pride and Prejudice", List.of("Jane Austen"),
                "fiction", new BigDecimal("12.50"), 1813));

        assertInvalidField(thrown, "invalid-isbn", "Invalid ISBN", "isbn",
                domainMessage(() -> new Isbn(badChecksum)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAnInvalidTitle() {
        Throwable thrown = catchThrowable(() -> addBook(ISBN, "   ", List.of("Jane Austen"), "fiction",
                new BigDecimal("12.50"), 1813));

        assertInvalidField(thrown, "invalid-title", "Invalid title", "title", domainMessage(() -> new Title("   ")));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAnInvalidAuthorName() {
        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen", " "),
                "fiction", new BigDecimal("12.50"), 1813));

        assertInvalidField(thrown, "invalid-author-name", "Invalid author name", "authors",
                domainMessage(() -> new AuthorName(" ")));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAnInvalidNumberOfAuthors() {
        List<String> sixAuthors = List.of("A One", "B Two", "C Three", "D Four", "E Five", "F Six");

        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", sixAuthors, "fiction",
                new BigDecimal("12.50"), 1813));

        assertInvalidField(thrown, "author-count-out-of-range", "Number of authors out of range", "authors",
                domainMessage(() -> newBook(sixAuthors, new BigDecimal("12.50"), 1813, CLOCK)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAnUnknownGenreOnAddListingTheValidGenres() {
        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "westerns", new BigDecimal("12.50"), 1813));

        assertUnknownGenre(thrown);
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAPriceOutOfRangeOnAdd() {
        BigDecimal tooExpensive = new BigDecimal("600.00");

        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "fiction", tooExpensive, 1813));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> newBook(List.of("Jane Austen"), tooExpensive, 1813, CLOCK)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsANegativePriceOnAddAsPriceOutOfRange() {
        BigDecimal negative = new BigDecimal("-1.00");

        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "fiction", negative, 1813));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> Money.of(negative)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void rejectsAPublicationYearOutOfRange() {
        Throwable thrown = catchThrowable(() -> addBook(ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "fiction", new BigDecimal("12.50"), 1449));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("12.50"), 1449,
                        CLOCK)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void changesThePriceOfAnAvailableBook() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("AVAILABLE"));

        flow.changePrice(BOOK_ID, new BigDecimal("14.9"));

        verify(books).save(saved.capture());
        BookDTO dto = saved.getValue();
        assertThat(dto.id()).isEqualTo(BOOK_ID);
        assertThat(dto.isbn()).isEqualTo(ISBN);
        assertThat(dto.title()).isEqualTo("Pride and Prejudice");
        assertThat(dto.authors()).containsExactly("Jane Austen", "Fanny Burney");
        assertThat(dto.genre()).isEqualTo("FICTION");
        assertThat(dto.priceAmount()).isEqualByComparingTo("14.90");
        assertThat(dto.priceAmount().scale()).isEqualTo(2);
        assertThat(dto.priceCurrency()).isEqualTo("EUR");
        assertThat(dto.publicationYear()).isEqualTo(1813);
        assertThat(dto.status()).isEqualTo("AVAILABLE");
    }

    @Test
    void rejectsAPriceChangeOnADiscontinuedBook() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("DISCONTINUED"));

        Throwable thrown = catchThrowable(() -> flow.changePrice(BOOK_ID, new BigDecimal("14.90")));

        assertThat(thrown).isExactlyInstanceOf(ConflictException.class);
        ConflictException conflict = (ConflictException) thrown;
        assertThat(conflict.code()).isEqualTo("book-discontinued");
        assertThat(conflict.title()).isEqualTo("Book discontinued");
        assertThat(conflict.getMessage()).isEqualTo(domainMessage(
                () -> BookDomainMapper.toDomain(storedBook("DISCONTINUED"), CLOCK).changePrice(Money.of("14.90"))));
        assertThat(conflict.details()).isEmpty();
        verify(books, never()).save(any());
    }

    @Test
    void rejectsAnOutOfRangePriceChange() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("AVAILABLE"));
        BigDecimal tooExpensive = new BigDecimal("600.00");

        Throwable thrown = catchThrowable(() -> flow.changePrice(BOOK_ID, tooExpensive));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price", domainMessage(
                () -> BookDomainMapper.toDomain(storedBook("AVAILABLE"), CLOCK).changePrice(Money.of(tooExpensive))));
        verify(books, never()).save(any());
    }

    @Test
    void rejectsANegativePriceChangeAsPriceOutOfRange() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("AVAILABLE"));
        BigDecimal negative = new BigDecimal("-0.50");

        Throwable thrown = catchThrowable(() -> flow.changePrice(BOOK_ID, negative));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> Money.of(negative)));
        verify(books, never()).save(any());
    }

    @Test
    void changePricePassesBookNotFoundThrough() {
        BookDataService.BookNotFoundException notFound = new BookDataService.BookNotFoundException(BOOK_ID);
        when(books.findById(BOOK_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.changePrice(BOOK_ID, new BigDecimal("14.90")));

        assertThat(thrown).isSameAs(notFound);
        verify(books, never()).save(any());
    }

    @Test
    void discontinuesAnAvailableBook() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("AVAILABLE"));

        flow.discontinue(BOOK_ID);

        verify(books).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new BookDTO(BOOK_ID, ISBN, "Pride and Prejudice",
                List.of("Jane Austen", "Fanny Burney"), "FICTION", new BigDecimal("12.50"), "EUR", 1813,
                "DISCONTINUED"));
    }

    @Test
    void discontinuingADiscontinuedBookSucceeds() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("DISCONTINUED"));

        flow.discontinue(BOOK_ID);

        verify(books).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(storedBook("DISCONTINUED"));
    }

    @Test
    void discontinuePassesBookNotFoundThrough() {
        BookDataService.BookNotFoundException notFound = new BookDataService.BookNotFoundException(BOOK_ID);
        when(books.findById(BOOK_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.discontinue(BOOK_ID));

        assertThat(thrown).isSameAs(notFound);
        verify(books, never()).save(any());
    }

    @Test
    void changingThePriceOrDiscontinuingNeverTouchesStock() {
        when(books.findById(BOOK_ID)).thenReturn(storedBook("AVAILABLE"));

        flow.changePrice(BOOK_ID, new BigDecimal("14.90"));
        flow.discontinue(BOOK_ID);

        verify(books, times(2)).save(any());
        verifyNoInteractions(stockItems);
    }

    @Test
    void findByIdReturnsTheStoredBook() {
        BookDTO stored = storedBook("AVAILABLE");
        when(books.findById(BOOK_ID)).thenReturn(stored);

        assertThat(flow.findById(BOOK_ID)).isSameAs(stored);
    }

    @Test
    void findByIdPassesBookNotFoundThrough() {
        BookDataService.BookNotFoundException notFound = new BookDataService.BookNotFoundException(BOOK_ID);
        when(books.findById(BOOK_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.findById(BOOK_ID));

        assertThat(thrown).isSameAs(notFound);
    }

    @Test
    void addChecksThePublicationYearAgainstTheFlowsClock() {
        // In 2000 the latest allowed publication year is 2001; with the real clock 2027 would still be allowed.
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, CLOCK_IN_2000);

        Throwable thrown = catchThrowable(() -> flowIn2000.addBook(ISBN, "Pride and Prejudice",
                List.of("Jane Austen"), "fiction", new BigDecimal("12.50"), 2027));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("12.50"), 2027,
                        CLOCK_IN_2000)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void changePriceRestoresTheStoredBookWithTheFlowsClock() {
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, CLOCK_IN_2000);
        BookDTO publishedIn2027 = new BookDTO(BOOK_ID, ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "FICTION", new BigDecimal("12.50"), "EUR", 2027, "AVAILABLE");
        when(books.findById(BOOK_ID)).thenReturn(publishedIn2027);

        Throwable thrown = catchThrowable(() -> flowIn2000.changePrice(BOOK_ID, new BigDecimal("14.90")));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> BookDomainMapper.toDomain(publishedIn2027, CLOCK_IN_2000)));
        verify(books, never()).save(any());
    }

    @Test
    void discontinueRestoresTheStoredBookWithTheFlowsClock() {
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, CLOCK_IN_2000);
        BookDTO publishedIn2027 = new BookDTO(BOOK_ID, ISBN, "Pride and Prejudice", List.of("Jane Austen"),
                "FICTION", new BigDecimal("12.50"), "EUR", 2027, "AVAILABLE");
        when(books.findById(BOOK_ID)).thenReturn(publishedIn2027);

        Throwable thrown = catchThrowable(() -> flowIn2000.discontinue(BOOK_ID));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> BookDomainMapper.toDomain(publishedIn2027, CLOCK_IN_2000)));
        verify(books, never()).save(any());
    }

    @Test
    void searchesWithNormalisedCriteriaTheCanonicalGenreAndALimitOf100() {
        List<BookDTO> found = List.of(storedBook("AVAILABLE"), storedBook("DISCONTINUED"));
        when(books.search("pride", "Pride and Prejudice", "austen", "SCIENCE_FICTION", 100)).thenReturn(found);

        List<BookDTO> result = flow.search(" pride\t", "  Pride and Prejudice ", "  austen ", "science-fiction");

        assertThat(result).containsExactlyElementsOf(found);
        verify(books).search("pride", "Pride and Prejudice", "austen", "SCIENCE_FICTION", 100);
    }

    @Test
    void treatsBlankCriteriaAsAbsent() {
        flow.search("", "   ", "\t", "  ");

        verify(books).search(null, null, null, null, 100);
    }

    @Test
    void passesAbsentCriteriaOnAsAbsent() {
        flow.search(null, null, null, null);

        verify(books).search(null, null, null, null, 100);
    }

    @Test
    void stripsUnicodeWhitespaceAroundSearchCriteria() {
        // U+2003 EM SPACE is whitespace to strip() but not to trim().
        flow.search(EM_SPACE + "pride" + EM_SPACE, null, EM_SPACE + "austen" + EM_SPACE,
                EM_SPACE + "poetry" + EM_SPACE);

        verify(books).search("pride", null, "austen", "POETRY", 100);
    }

    @Test
    void rejectsAnUnknownGenreInSearchListingTheValidGenres() {
        Throwable thrown = catchThrowable(() -> flow.search(null, null, "austen", "westerns"));

        assertUnknownGenre(thrown);
        verify(books, never()).search(any(), any(), any(), any(), anyInt());
    }

    private void addBook(String isbn, String title, List<String> authors, String genre, BigDecimal price,
            int publicationYear) {
        flow.addBook(isbn, title, authors, genre, price, publicationYear);
    }

    private void verifyNothingLookedUpOrSaved() {
        verify(books, never()).findByIsbn(any());
        verify(books, never()).save(any());
        verifyNoInteractions(stockItems);
    }

    private static BookDTO storedBook(String status) {
        return new BookDTO(BOOK_ID, ISBN, "Pride and Prejudice", List.of("Jane Austen", "Fanny Burney"), "FICTION",
                new BigDecimal("12.50"), "EUR", 1813, status);
    }

    private static Book newBook(List<String> authors, BigDecimal price, int publicationYear, Clock clock) {
        return Book.addToCatalogue(new Isbn(ISBN), new Title("Pride and Prejudice"),
                authors.stream().map(AuthorName::new).toList(), Genre.FICTION, Money.of(price), publicationYear,
                clock);
    }

    /** The message of the domain exception the call throws, which the flow must hand on unchanged. */
    private static String domainMessage(ThrowingCallable call) {
        Throwable domainException = catchThrowable(call);
        assertThat(domainException).isNotNull();
        assertThat(domainException.getMessage()).isNotBlank();
        return domainException.getMessage();
    }

    private static void assertInvalidField(Throwable thrown, String code, String title, String field,
            String message) {
        assertThat(thrown).isExactlyInstanceOf(InvalidInputException.class);
        InvalidInputException invalid = (InvalidInputException) thrown;
        assertThat(invalid.code()).isEqualTo(code);
        assertThat(invalid.title()).isEqualTo(title);
        assertThat(invalid.getMessage()).isEqualTo(message);
        assertThat(invalid.details()).containsExactly(Map.of("field", field, "message", message));
    }

    private static void assertUnknownGenre(Throwable thrown) {
        assertThat(thrown).isExactlyInstanceOf(InvalidInputException.class);
        InvalidInputException invalid = (InvalidInputException) thrown;
        assertThat(invalid.code()).isEqualTo("unknown-genre");
        assertThat(invalid.title()).isEqualTo("Unknown genre");
        assertThat(invalid.getMessage()).isNotBlank();
        assertThat(invalid.details()).containsExactly(
                Map.of("genre", "FICTION"),
                Map.of("genre", "SCIENCE_FICTION"),
                Map.of("genre", "FANTASY"),
                Map.of("genre", "MYSTERY"),
                Map.of("genre", "ROMANCE"),
                Map.of("genre", "HISTORY"),
                Map.of("genre", "SCIENCE"),
                Map.of("genre", "TECHNOLOGY"),
                Map.of("genre", "CHILDREN"),
                Map.of("genre", "POETRY"));
    }
}
