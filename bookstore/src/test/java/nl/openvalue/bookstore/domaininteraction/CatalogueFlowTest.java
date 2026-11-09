package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
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
import java.util.stream.Stream;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Mock
    private DistributorAppService distributor;

    @Captor
    private ArgumentCaptor<BookDTO> saved;

    @Captor
    private ArgumentCaptor<StockItemDTO> savedStockItem;

    private CatalogueFlow flow;

    @BeforeEach
    void setUp() {
        flow = new CatalogueFlow(books, stockItems, distributor, CLOCK);
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
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, distributor, CLOCK_IN_2000);

        Throwable thrown = catchThrowable(() -> flowIn2000.addBook(ISBN, "Pride and Prejudice",
                List.of("Jane Austen"), "fiction", new BigDecimal("12.50"), 2027));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("12.50"), 2027,
                        CLOCK_IN_2000)));
        verifyNothingLookedUpOrSaved();
    }

    @Test
    void changePriceRestoresTheStoredBookWithTheFlowsClock() {
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, distributor, CLOCK_IN_2000);
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
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, distributor, CLOCK_IN_2000);
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

    @Test
    void importsATitleWithTheRecommendedPriceBuiltFromTheRequestedIsbn() {
        // The request is hyphenated and the distributor answers for the normalised ISBN.
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN,
                "The Last Man", List.of("Mary Shelley", "Jane Austen"), "SCIENCE_FICTION", new BigDecimal("11.99"),
                "EUR", 1826, true)));

        UUID id = flow.importFromDistributor("978-0-14-143951-8", null);

        assertThat(id).isNotNull();
        verify(books).findByIsbn(ISBN);
        verify(distributor).findTitle(ISBN);
        verify(books).save(saved.capture());
        BookDTO dto = saved.getValue();
        assertThat(dto.authors()).containsExactly("Mary Shelley", "Jane Austen");
        assertThat(dto).isEqualTo(new BookDTO(id, ISBN, "The Last Man", List.of("Mary Shelley", "Jane Austen"),
                "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826, "AVAILABLE"));
        verify(stockItems).save(savedStockItem.capture());
        assertThat(savedStockItem.getValue()).isEqualTo(new StockItemDTO(id, 0, 0, 3, 10, false));
    }

    @Test
    void importsATitleWithThePriceOverrideInsteadOfTheRecommendedPrice() {
        when(distributor.findTitle("9789900010050")).thenReturn(Optional.of(new DistributorTitleDTO("9789900010050",
                "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal("9.99"), "EUR", 1813, true)));

        UUID id = flow.importFromDistributor("9789900010050", new BigDecimal("12.00"));

        assertThat(id).isNotNull();
        verify(books).findByIsbn("9789900010050");
        verify(books).save(saved.capture());
        BookDTO dto = saved.getValue();
        assertThat(dto.authors()).containsExactly("Jane Austen");
        assertThat(dto).isEqualTo(new BookDTO(id, "9789900010050", "Pride and Prejudice", List.of("Jane Austen"),
                "FICTION", new BigDecimal("12.00"), "EUR", 1813, "AVAILABLE"));
        verify(stockItems).save(savedStockItem.capture());
        assertThat(savedStockItem.getValue()).isEqualTo(new StockItemDTO(id, 0, 0, 3, 10, false));
    }

    static Stream<Arguments> distributorTitlesForAnotherIsbn() {
        return Stream.of(
                Arguments.of("9789900010050", true),
                Arguments.of("9789900010050", false),
                Arguments.of("978-0-14-143951-8", true),
                Arguments.of(null, true),
                Arguments.of("97801414395180", true),
                Arguments.of("9780141439518 ", true),
                Arguments.of("09780141439518", true),
                Arguments.of("978014143951", true),
                Arguments.of(" 9780141439518", true),
                Arguments.of("780141439518", true));
    }

    @ParameterizedTest(name = "answer isbn {0}, orderable {1}")
    @MethodSource("distributorTitlesForAnotherIsbn")
    void refusesADistributorTitleForAnotherIsbnAndSavesNothing(String answeredIsbn, boolean orderable) {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(answeredIsbn, "The Last Man",
                List.of("Mary Shelley", "Jane Austen"), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826,
                orderable)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-0-14-143951-8", null));

        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.DistributorUnavailableException.class);
        DistributorAppService.DistributorUnavailableException unavailable =
                (DistributorAppService.DistributorUnavailableException) thrown;
        assertThat(unavailable.code()).isEqualTo("distributor-unavailable");
        assertThat(unavailable.getMessage()).isEqualTo("The distributor gave no usable answer for ISBN 9780141439518");
        verify(distributor).findTitle(ISBN);
        verifyNothingSaved();
    }

    @Test
    void savesAWholeNumberOverrideWithTwoDecimals() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                true)));

        flow.importFromDistributor(ISBN, new BigDecimal("12"));

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("12.00"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void anOverrideReplacesARecommendedPriceInAnotherCurrency() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "USD", 1826,
                true)));

        flow.importFromDistributor(ISBN, new BigDecimal("12.00"));

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("12.00"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void rejectsAnInvalidIsbnOnImportBeforeAnyLookupOrRemoteCall() {
        lenient().when(books.findByIsbn(any())).thenReturn(Optional.of(storedBook("AVAILABLE")));
        String badChecksum = "9780141439519";

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(badChecksum, null));

        assertInvalidField(thrown, "invalid-isbn", "Invalid ISBN", "isbn", domainMessage(() -> new Isbn(badChecksum)));
        verifyNoInteractions(books, stockItems, distributor);
    }

    @Test
    void rejectsAnInvalidIsbnOnImportBeforeAnInvalidOverride() {
        String badChecksum = "9780141439519";

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(badChecksum, new BigDecimal("-5.00")));

        assertInvalidField(thrown, "invalid-isbn", "Invalid ISBN", "isbn", domainMessage(() -> new Isbn(badChecksum)));
        verifyNoInteractions(books, stockItems, distributor);
    }

    @Test
    void rejectsANegativeOverrideAsPriceOutOfRangeBeforeLookingUpTheIsbn() {
        lenient().when(books.findByIsbn(any())).thenReturn(Optional.of(storedBook("AVAILABLE")));
        BigDecimal negative = new BigDecimal("-5.00");

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, negative));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> Money.of(negative)));
        verify(books, never()).findByIsbn(any());
        verifyNoInteractions(distributor);
        verifyNothingSaved();
    }

    @Test
    void rejectsAnIsbnAlreadyInTheCatalogueWithoutCallingTheDistributor() {
        // Unstubbed, the distributor would answer "not found", so calling it first would give another exception.
        when(books.findByIsbn(ISBN)).thenReturn(Optional.of(storedBook("AVAILABLE")));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-0-14-143951-8", null));

        assertThat(thrown).isExactlyInstanceOf(BookDataService.IsbnAlreadyExistsException.class);
        ConflictException conflict = (ConflictException) thrown;
        assertThat(conflict.code()).isEqualTo("isbn-already-exists");
        assertThat(conflict.title()).isEqualTo("ISBN already in catalogue");
        assertThat(conflict.getMessage()).isEqualTo(new BookDataService.IsbnAlreadyExistsException(ISBN).getMessage());
        verifyNoInteractions(distributor);
        verifyNothingSaved();
    }

    @Test
    void reportsATitleUnknownAtTheDistributorAsNotFound() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-0-14-143951-8", null));

        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.TitleNotFoundAtDistributorException.class);
        NotFoundException notFound = (NotFoundException) thrown;
        assertThat(notFound.code()).isEqualTo("title-not-found-at-distributor");
        assertThat(notFound.title()).isEqualTo("Title not found at distributor");
        assertThat(notFound.getMessage()).isEqualTo("The distributor has no title with ISBN 9780141439518");
        assertThat(notFound.details()).isEmpty();
        verifyNothingSaved();
    }

    @Test
    void rejectsATitleTheDistributorNoLongerSuppliesBeforeBuildingTheBook() {
        // WESTERN is no bookstore genre, so building the book first would give unknown-genre instead.
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("WESTERN", "11.99", "EUR", 1826, false)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-0-14-143951-8", null));

        assertNotOrderable(thrown);
        verifyNothingSaved();
    }

    @Test
    void rejectsATitleTheDistributorNoLongerSuppliesEvenWithAPriceOverride() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                false)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, new BigDecimal("12.00")));

        assertNotOrderable(thrown);
        verifyNothingSaved();
    }

    @Test
    void passesDistributorUnavailableThroughUnchanged() {
        RuntimeException unavailable = new DistributorAppService.DistributorUnavailableException("Read timed out");
        when(distributor.findTitle(ISBN)).thenThrow(unavailable);

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertThat(thrown).isSameAs(unavailable);
        verifyNothingSaved();
    }

    @Test
    void passesTitleNotOrderableFromTheDistributorThroughUnchanged() {
        RuntimeException notOrderable = new DistributorAppService.TitleNotOrderableException("9789900010050");
        when(distributor.findTitle(ISBN)).thenThrow(notOrderable);

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, new BigDecimal("12.00")));

        assertThat(thrown).isSameAs(notOrderable);
        verifyNothingSaved();
    }

    @Test
    void passesTitleNotFoundFromTheDistributorThroughUnchanged() {
        RuntimeException notFound = new DistributorAppService.TitleNotFoundAtDistributorException("9789900010050");
        when(distributor.findTitle(ISBN)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertThat(thrown).isSameAs(notFound);
        verifyNothingSaved();
    }

    @Test
    void aDuplicateIsbnRaceOnSavingTheImportedBookPropagatesAndOpensNoStockItem() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                true)));
        RuntimeException race = new BookDataService.IsbnAlreadyExistsException(ISBN);
        doThrow(race).when(books).save(any());

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertThat(thrown).isSameAs(race);
        verifyNoInteractions(stockItems);
    }

    @Test
    void importAcceptsAPublicationYearOneAfterTheYearOfTheFlowsClock() {
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, distributor, CLOCK_IN_2000);
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 2001,
                true)));

        flowIn2000.importFromDistributor(ISBN, null);

        verify(books).save(saved.capture());
        assertThat(saved.getValue().publicationYear()).isEqualTo(2001);
    }

    @Test
    void importChecksThePublicationYearAgainstTheFlowsClock() {
        // In 2000 the latest allowed publication year is 2001; with the real clock 2002 would be allowed.
        CatalogueFlow flowIn2000 = new CatalogueFlow(books, stockItems, distributor, CLOCK_IN_2000);
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 2002,
                true)));

        Throwable thrown = catchThrowable(() -> flowIn2000.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("11.99"), 2002,
                        CLOCK_IN_2000)));
        verifyNothingSaved();
    }

    @Test
    void rejectsAGenreFromTheDistributorThatIsNoBookstoreGenre() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("WESTERN", "11.99", "EUR", 1826, true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertUnknownGenre(thrown);
        assertThat(thrown.getMessage()).isEqualTo(domainMessage(() -> Genre.parse("WESTERN")));
        verifyNothingSaved();
    }

    @Test
    void rejectsADistributorTitleWithoutAuthors() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "The Last Man",
                List.of(), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826, true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "author-count-out-of-range", "Number of authors out of range", "authors",
                domainMessage(() -> newBook(List.of(), new BigDecimal("11.99"), 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void rejectsARecommendedPriceInAnotherCurrencyWithoutAnOverride() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "USD", 1826,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> Money.of(new BigDecimal("11.99"), "USD")));
        verifyNothingSaved();
    }

    @Test
    void rejectsANegativeRecommendedPriceAsPriceOutOfRange() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "-9.99", "EUR", 1826,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> Money.of(new BigDecimal("-9.99"), "EUR")));
        verifyNothingSaved();
    }

    @Test
    void rejectsADistributorTitleWithABlankTitle() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "   ",
                List.of("Mary Shelley", "Jane Austen"), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "invalid-title", "Invalid title", "title", domainMessage(() -> new Title("   ")));
        verifyNothingSaved();
    }

    @Test
    void rejectsADistributorAuthorWithABlankName() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "The Last Man",
                List.of("Mary Shelley", " "), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826, true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "invalid-author-name", "Invalid author name", "authors",
                domainMessage(() -> new AuthorName(" ")));
        verifyNothingSaved();
    }

    @Test
    void savesAnOverrideWithCentsExactly() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "9.99", "EUR", 1826,
                true)));

        flow.importFromDistributor(ISBN, new BigDecimal("12.49"));

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("12.49"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void savesASecondRecommendedPriceExactly() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "8.50", "EUR", 1826,
                true)));

        flow.importFromDistributor(ISBN, null);

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("8.50"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void reportsAnotherUnknownTitleWithItsOwnIsbn() {
        when(distributor.findTitle("9789900010050")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-99-0001-005-0", null));

        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.TitleNotFoundAtDistributorException.class);
        assertThat(thrown.getMessage()).isEqualTo("The distributor has no title with ISBN 9789900010050");
        verifyNothingSaved();
    }

    @Test
    void rejectsAnotherTitleNoLongerSuppliedWithItsOwnIsbn() {
        when(distributor.findTitle("9789900010050")).thenReturn(Optional.of(new DistributorTitleDTO("9789900010050",
                "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal("9.99"), "EUR", 1813, false)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor("978-99-0001-005-0", null));

        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.TitleNotOrderableException.class);
        assertThat(thrown.getMessage())
                .isEqualTo("The title with ISBN 9789900010050 cannot be ordered from the distributor");
        verifyNothingSaved();
    }

    @Test
    void savesAnOverrideBelowTheRecommendedPrice() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                true)));

        flow.importFromDistributor(ISBN, new BigDecimal("7.50"));

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("7.50"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void rejectsAZeroOverrideAsPriceOutOfRange() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                true)));
        BigDecimal zero = new BigDecimal("0.00");

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, zero));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> newBook(List.of("Jane Austen"), zero, 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void anOverrideEqualToTheRecommendedAmountStillReplacesTheRecommendedPrice() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "USD", 1826,
                true)));

        flow.importFromDistributor(ISBN, new BigDecimal("11.99"));

        verify(books).save(saved.capture());
        assertThat(saved.getValue().priceAmount()).isEqualTo(new BigDecimal("11.99"));
        assertThat(saved.getValue().priceCurrency()).isEqualTo("EUR");
    }

    @Test
    void rejectsAnOverrideAboveTheHighestPriceAsPriceOutOfRange() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1826,
                true)));
        BigDecimal tooExpensive = new BigDecimal("500.01");

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, tooExpensive));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> newBook(List.of("Jane Austen"), tooExpensive, 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void rejectsAZeroRecommendedPriceAsPriceOutOfRange() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "0.00", "EUR", 1826,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("0.00"), 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void rejectsARecommendedPriceAboveTheHighestPriceAsPriceOutOfRange() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "500.01", "EUR", 1826,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "price-out-of-range", "Price out of range", "price",
                domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("500.01"), 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void rejectsADistributorTitleWithTooManyAuthors() {
        List<String> sixAuthors = List.of("Mary Shelley", "Jane Austen", "Fanny Burney", "Maria Edgeworth",
                "Ann Radcliffe", "Walter Scott");
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "The Last Man",
                sixAuthors, "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826, true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "author-count-out-of-range", "Number of authors out of range", "authors",
                domainMessage(() -> newBook(sixAuthors, new BigDecimal("11.99"), 1826, CLOCK)));
        verifyNothingSaved();
    }

    @Test
    void rejectsADistributorTitleWhoseFirstAuthorIsBlank() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "The Last Man",
                List.of(" ", "Mary Shelley"), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826, true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "invalid-author-name", "Invalid author name", "authors",
                domainMessage(() -> new AuthorName(" ")));
        verifyNothingSaved();
    }

    @Test
    void keepsTwoEqualDistributorAuthorsAsGiven() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(ISBN, "The Last Man",
                List.of("Mary Shelley", "Mary Shelley"), "SCIENCE_FICTION", new BigDecimal("11.99"), "EUR", 1826,
                true)));

        flow.importFromDistributor(ISBN, null);

        verify(books).save(saved.capture());
        assertThat(saved.getValue().authors()).containsExactly("Mary Shelley", "Mary Shelley");
    }

    @Test
    void rejectsADistributorPublicationYearBeforeTheEarliestYear() {
        when(distributor.findTitle(ISBN)).thenReturn(Optional.of(lastMan("SCIENCE_FICTION", "11.99", "EUR", 1449,
                true)));

        Throwable thrown = catchThrowable(() -> flow.importFromDistributor(ISBN, null));

        assertInvalidField(thrown, "publication-year-out-of-range", "Publication year out of range",
                "publicationYear", domainMessage(() -> newBook(List.of("Jane Austen"), new BigDecimal("11.99"), 1449,
                        CLOCK)));
        verifyNothingSaved();
    }

    private static DistributorTitleDTO lastMan(String genre, String recommendedPrice, String currency,
            int publicationYear, boolean orderable) {
        return new DistributorTitleDTO(ISBN, "The Last Man", List.of("Mary Shelley", "Jane Austen"), genre,
                new BigDecimal(recommendedPrice), currency, publicationYear, orderable);
    }

    private void verifyNothingSaved() {
        verify(books, never()).save(any());
        verifyNoInteractions(stockItems);
    }

    private static void assertNotOrderable(Throwable thrown) {
        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.TitleNotOrderableException.class);
        InvalidInputException notOrderable = (InvalidInputException) thrown;
        assertThat(notOrderable.code()).isEqualTo("title-not-orderable");
        assertThat(notOrderable.title()).isEqualTo("Title not orderable");
        assertThat(notOrderable.getMessage())
                .isEqualTo("The title with ISBN 9780141439518 cannot be ordered from the distributor");
        assertThat(notOrderable.details()).isEmpty();
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
