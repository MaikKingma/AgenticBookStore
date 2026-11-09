package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The tools with mocked flows and no Spring context: caps, ordering, view fields and the curated error texts, plus the
 * same tools through Spring AI's own callbacks.
 */
@ExtendWith(MockitoExtension.class)
class BookstoreToolsTest {

    // Eleven books in one fixed "flow order". Every column is non-monotone in either direction over the first ten and
    // over all eleven rows, so a re-sort by any column changes the asserted order.
    private static final int[] ISBN_RANK = {7, 2, 9, 0, 5, 10, 3, 8, 1, 6, 4};
    private static final int[] ID_RANK = {3, 9, 1, 6, 0, 8, 2, 10, 5, 4, 7};
    private static final String[] TITLES = {"Gamma", "Alpha", "Kappa", "Beta", "Iota", "Delta", "Eta", "Zeta",
        "Epsilon", "Theta", "Lambda"};
    private static final int[] YEARS = {1987, 1923, 2011, 1964, 2005, 1998, 1871, 2019, 1955, 1990, 1932};
    private static final String[] PRICES = {"14.20", "9.99", "23.50", "7.00", "12.50", "30.10", "8.45", "19.99",
        "11.00", "25.75", "6.30"};
    private static final String[] GENRES = {"FANTASY", "MYSTERY", "FICTION", "ROMANCE", "FANTASY", "HISTORY",
        "SCIENCE_FICTION", "MYSTERY", "POETRY", "FICTION", "CHILDREN"};
    private static final String[] AUTHORS = {"Ursula Le Guin", "Agatha Christie", "Mary Shelley", "Zadie Smith",
        "Jane Austen", "Haruki Murakami", "Octavia Butler", "Terry Pratchett", "Italo Calvino", "Neil Gaiman",
        "Toni Morrison"};
    private static final int[] AVAILABLE = {3, 0, 6, 1, 4, 9, 2, 7, 0, 5, 8};
    private static final int[] RESERVED = {2, 0, 1, 3, 0, 4, 2, 1, 5, 0, 2};

    @Mock
    private CatalogueFlow catalogue;

    @Mock
    private InventoryFlow inventory;

    private BookstoreTools tools() {
        return new BookstoreTools(catalogue, inventory);
    }

    /** Book {@code n} of the fixed fixture; its stock holds {@code AVAILABLE[n]} available copies. */
    private static BookDTO book(int n) {
        return new BookDTO(new UUID(0x1000L, ID_RANK[n] + 1), "isbn-%02d".formatted(ISBN_RANK[n]), TITLES[n],
                List.of(AUTHORS[n]), GENRES[n], new BigDecimal(PRICES[n]), "EUR", YEARS[n], "AVAILABLE");
    }

    private static StockItemDTO stockOf(int n) {
        UUID bookId = new UUID(0x1000L, ID_RANK[n] + 1);
        return new StockItemDTO(bookId, AVAILABLE[n] + RESERVED[n], RESERVED[n], 5, 20, false);
    }

    private static List<BookDTO> books(int count) {
        return IntStream.range(0, count).mapToObj(BookstoreToolsTest::book).toList();
    }

    private void stubStockOfFirst(int count) {
        for (int n = 0; n < count; n++) {
            when(inventory.findByBookId(book(n).id())).thenReturn(stockOf(n));
        }
    }

    /**
     * Stock of the eleventh book, stubbed leniently: a correct tool never asks for it, so a stricter stub would fail
     * the test for the right behaviour; a tool that does ask (a cap of eleven, or a lookup before the cap) is then
     * caught by the assertions on the result and on the verification instead of by a missing stub.
     */
    private void stubStockOfEleventhLeniently() {
        lenient().when(inventory.findByBookId(book(10).id())).thenReturn(stockOf(10));
    }

    private static BookSuggestionView suggestionOf(int n) {
        return new BookSuggestionView(book(n).id(), TITLES[n], List.of(AUTHORS[n]), GENRES[n], PRICES[n],
                AVAILABLE[n] > 0);
    }

    @Test
    void searchReturnsTenOfElevenInFlowOrder() {
        when(catalogue.search("austen", null, null, null)).thenReturn(books(11));
        stubStockOfFirst(10);
        stubStockOfEleventhLeniently();

        List<BookSuggestionView> hits = tools().searchCatalogue("austen", null);

        assertThat(hits).containsExactly(suggestionOf(0), suggestionOf(1), suggestionOf(2), suggestionOf(3),
                suggestionOf(4), suggestionOf(5), suggestionOf(6), suggestionOf(7), suggestionOf(8),
                suggestionOf(9));
        assertThat(hits).extracting(BookSuggestionView::id).doesNotContain(book(10).id());
        verify(inventory, never()).findByBookId(book(10).id());
    }

    @Test
    void searchReturnsAllOfTen() {
        when(catalogue.search("shelley", null, null, null)).thenReturn(books(10));
        stubStockOfFirst(10);

        List<BookSuggestionView> hits = tools().searchCatalogue("shelley", null);

        assertThat(hits).hasSize(10);
        assertThat(hits).containsExactly(suggestionOf(0), suggestionOf(1), suggestionOf(2), suggestionOf(3),
                suggestionOf(4), suggestionOf(5), suggestionOf(6), suggestionOf(7), suggestionOf(8),
                suggestionOf(9));
    }

    @Test
    void searchReturnsEmptyListForNoHit() {
        when(catalogue.search("nothing", null, null, null)).thenReturn(List.of());

        List<BookSuggestionView> hits = tools().searchCatalogue("nothing", null);

        assertThat(hits).isEmpty();
        verify(inventory, never()).findByBookId(any(UUID.class));
    }

    @Test
    void searchLooksStockUpOnlyForKeptHits() {
        when(catalogue.search("many", null, null, "FICTION")).thenReturn(books(11));
        stubStockOfFirst(10);
        stubStockOfEleventhLeniently();

        tools().searchCatalogue("many", "FICTION");

        for (int n = 0; n < 10; n++) {
            verify(inventory).findByBookId(book(n).id());
        }
        verify(inventory, never()).findByBookId(book(10).id());
    }

    @Test
    void searchPassesQueryAndGenreThrough() {
        when(catalogue.search("tolkien", null, null, "FANTASY")).thenReturn(List.of(book(0)));
        when(catalogue.search("  Pratchett ", null, null, "mystery")).thenReturn(List.of(book(1)));
        when(catalogue.search("dracula", null, null, null)).thenReturn(List.of(book(2)));
        stubStockOfFirst(3);

        assertThat(tools().searchCatalogue("tolkien", "FANTASY")).containsExactly(suggestionOf(0));
        assertThat(tools().searchCatalogue("  Pratchett ", "mystery")).containsExactly(suggestionOf(1));
        assertThat(tools().searchCatalogue("dracula", null)).containsExactly(suggestionOf(2));
    }

    // Two literal books for the pass-through checks: every field has a different value in each of them.
    private static final UUID PRIDE_ID = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
    private static final UUID OMENS_ID = UUID.fromString("c0ffee00-1234-4abc-8def-0123456789ab");

    private static BookDTO pride() {
        return new BookDTO(PRIDE_ID, "9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "ROMANCE",
                new BigDecimal("9.99"), "EUR", 1813, "AVAILABLE");
    }

    private static BookDTO omens() {
        return new BookDTO(OMENS_ID, "9780060853983", " Good Omens ", List.of("Terry Pratchett", "Neil Gaiman"),
                "FANTASY", new BigDecimal("12.50"), "EUR", 2020, "AVAILABLE");
    }

    @Test
    void viewsCopyEveryField() {
        when(catalogue.search("both", null, null, null)).thenReturn(List.of(pride(), omens()));
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 6, 2, 1, 5, false));
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 3, 3, 1, 5, false));
        when(catalogue.findById(PRIDE_ID)).thenReturn(pride());
        when(catalogue.findById(OMENS_ID)).thenReturn(omens());

        List<BookSuggestionView> found = tools().searchCatalogue("both", null);
        BookDetailsView prideDetails = tools().getBookDetails(PRIDE_ID.toString());
        BookDetailsView omensDetails = tools().getBookDetails(OMENS_ID.toString());

        assertThat(found).containsExactly(
                new BookSuggestionView(PRIDE_ID, "Pride and Prejudice", List.of("Jane Austen"), "ROMANCE", "9.99",
                        true),
                new BookSuggestionView(OMENS_ID, " Good Omens ", List.of("Terry Pratchett", "Neil Gaiman"),
                        "FANTASY", "12.50", false));
        assertThat(prideDetails).isEqualTo(new BookDetailsView(PRIDE_ID, "Pride and Prejudice",
                List.of("Jane Austen"), "ROMANCE", "9.99", true, 1813));
        assertThat(omensDetails).isEqualTo(new BookDetailsView(OMENS_ID, " Good Omens ",
                List.of("Terry Pratchett", "Neil Gaiman"), "FANTASY", "12.50", false, 2020));
        assertThat(omensDetails.authors()).containsExactly("Terry Pratchett", "Neil Gaiman");
    }

    @Test
    void inStockMirrorsTheShopRule() {
        UUID oneLeft = new UUID(0x2000L, 1);
        UUID soldOut = new UUID(0x2000L, 2);
        UUID reservedAway = new UUID(0x2000L, 3);
        UUID discontinued = new UUID(0x2000L, 4);
        List<BookDTO> hits = List.of(
                bookWith(oneLeft, "AVAILABLE"), bookWith(soldOut, "AVAILABLE"),
                bookWith(reservedAway, "AVAILABLE"), bookWith(discontinued, "DISCONTINUED"));
        when(catalogue.search("rule", null, null, null)).thenReturn(hits);
        // available 1 with a reorder level of 5: low stock but in stock
        when(inventory.findByBookId(oneLeft)).thenReturn(new StockItemDTO(oneLeft, 1, 0, 5, 20, true));
        // available 0 with a restock on its way
        when(inventory.findByBookId(soldOut)).thenReturn(new StockItemDTO(soldOut, 0, 0, 5, 20, true,
                new PendingRestockDTO("R-1", 20, LocalDate.of(2001, 3, 4), false)));
        // on hand 3, reserved 3
        when(inventory.findByBookId(reservedAway)).thenReturn(new StockItemDTO(reservedAway, 3, 3, 1, 20, false));
        // a discontinued book that still has stock
        when(inventory.findByBookId(discontinued)).thenReturn(new StockItemDTO(discontinued, 4, 0, 1, 20, false));
        for (UUID id : List.of(oneLeft, soldOut, reservedAway, discontinued)) {
            when(catalogue.findById(id)).thenReturn(hits.stream().filter(b -> b.id().equals(id)).findFirst().get());
        }

        assertThat(tools().searchCatalogue("rule", null)).extracting(BookSuggestionView::inStock)
                .containsExactly(true, false, false, true);
        assertThat(List.of(oneLeft, soldOut, reservedAway, discontinued).stream()
                .map(id -> tools().getBookDetails(id.toString()).inStock()).toList())
                .containsExactly(true, false, false, true);
    }

    private static BookDTO bookWith(UUID id, String status) {
        return new BookDTO(id, "isbn-" + id.getLeastSignificantBits(), "Title " + id.getLeastSignificantBits(),
                List.of("An Author"), "FICTION", new BigDecimal("10.00"), "EUR", 2000, status);
    }

    @Test
    void detailsCarryYearAndStock() {
        when(catalogue.findById(PRIDE_ID)).thenReturn(pride());
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 9, 2, 1, 5, false));
        when(catalogue.findById(OMENS_ID)).thenReturn(omens());
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 2, 2, 1, 5, false));

        BookDetailsView first = tools().getBookDetails(PRIDE_ID.toString());
        BookDetailsView second = tools().getBookDetails(OMENS_ID.toString());

        assertThat(first.publicationYear()).isEqualTo(1813);
        assertThat(first.inStock()).isTrue();
        assertThat(second.publicationYear()).isEqualTo(2020);
        assertThat(second.inStock()).isFalse();
    }

    @Test
    void availabilityShowsPendingDate() {
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 9, 2, 5, 20, true,
                new PendingRestockDTO("R-7", 20, LocalDate.of(2001, 3, 4), false)));
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 4, 4, 5, 20, true,
                new PendingRestockDTO("R-8", 30, LocalDate.of(2026, 11, 25), true)));

        assertThat(tools().checkAvailability(PRIDE_ID.toString()))
                .isEqualTo(new AvailabilityView(7, "2001-03-04"));
        assertThat(tools().checkAvailability(OMENS_ID.toString()))
                .isEqualTo(new AvailabilityView(0, "2026-11-25"));
    }

    @Test
    void availabilityWithoutPendingRestockIsNull() {
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 9, 2, 1, 20, false));
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 5, 0, 1, 20, false));

        AvailabilityView first = tools().checkAvailability(PRIDE_ID.toString());
        AvailabilityView second = tools().checkAvailability(OMENS_ID.toString());

        assertThat(first.available()).isEqualTo(7);
        assertThat(first.pendingRestockExpected()).isNull();
        assertThat(second.available()).isEqualTo(5);
        assertThat(second.pendingRestockExpected()).isNull();
    }

    // Seven recommendation candidates, listed in the order the tool must return them (rank 0 is the newest). No other
    // column is monotone in that order, so a sort by title, isbn, id, price, author or available gives another order.
    private static final int[] REC_YEARS = {2019, 2011, 2004, 1998, 1987, 1971, 1955};
    private static final String[] REC_TITLES = {"Delta", "Alpha", "Echo", "Bravo", "Foxtrot", "Charlie", "Golf"};
    private static final String[] REC_ISBNS = {"isbn-4", "isbn-1", "isbn-5", "isbn-2", "isbn-0", "isbn-3", "isbn-6"};
    private static final int[] REC_ID_RANKS = {2, 5, 1, 6, 3, 4, 0};
    private static final String[] REC_PRICES = {"11.00", "8.50", "14.75", "6.20", "19.99", "9.30", "7.45"};
    private static final String[] REC_AUTHORS = {"Wendy", "Anna", "Vic", "Ben", "Ursula", "Carl", "Xavier"};
    private static final int[] REC_AVAILABLE = {4, 1, 6, 2, 3, 5, 7};

    private static BookDTO ranked(int rank) {
        return new BookDTO(new UUID(0x3000L, REC_ID_RANKS[rank]), REC_ISBNS[rank], REC_TITLES[rank],
                List.of(REC_AUTHORS[rank]), "FANTASY", new BigDecimal(REC_PRICES[rank]), "EUR", REC_YEARS[rank],
                "AVAILABLE");
    }

    private static StockItemDTO rankedStock(int rank, int available) {
        return new StockItemDTO(ranked(rank).id(), available + 1, 1, 2, 10, false);
    }

    /** The first {@code count} ranked books as the flow would return them: oldest first, the reverse of the answer. */
    private static List<BookDTO> flowOrderOfRanked(int count) {
        return IntStream.range(0, count).mapToObj(rank -> ranked(count - 1 - rank)).toList();
    }

    private Map<UUID, StockItemDTO> stockOfRanked(int count) {
        Map<UUID, StockItemDTO> stock = new HashMap<>();
        for (int rank = 0; rank < count; rank++) {
            stock.put(ranked(rank).id(), rankedStock(rank, REC_AVAILABLE[rank]));
        }
        return stock;
    }

    private void stubRecommendation(String genre, List<BookDTO> flowOrder, Map<UUID, StockItemDTO> stock) {
        when(catalogue.search(null, null, null, genre)).thenReturn(flowOrder);
        when(inventory.findByBookId(any(UUID.class))).thenAnswer(call -> stock.get(call.<UUID>getArgument(0)));
    }

    private List<String> recommendedTitles(String genre, Integer limit) {
        return tools().recommendByGenre(genre, limit).stream().map(BookSuggestionView::title).toList();
    }

    @Test
    void recommendCapsAtFiveOfSix() {
        stubRecommendation("FANTASY", flowOrderOfRanked(6), stockOfRanked(6));

        assertThat(recommendedTitles("FANTASY", 5)).containsExactly("Delta", "Alpha", "Echo", "Bravo", "Foxtrot");
    }

    @Test
    void recommendCapsAtFiveForSeven() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", 7)).containsExactly("Delta", "Alpha", "Echo", "Bravo", "Foxtrot");
    }

    @Test
    void recommendCapsAtFiveForALimitOfSix() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", 6)).containsExactly("Delta", "Alpha", "Echo", "Bravo", "Foxtrot");
    }

    @Test
    void recommendHonoursThree() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", 3)).containsExactly("Delta", "Alpha", "Echo");
    }

    @Test
    void recommendHonoursOne() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", 1)).containsExactly("Delta");
    }

    @Test
    void recommendClampsZeroAndNegativeToOne() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", 0)).containsExactly("Delta");
        assertThat(recommendedTitles("FANTASY", -2)).containsExactly("Delta");
    }

    @Test
    void recommendTreatsNullLimitAsFive() {
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stockOfRanked(7));

        assertThat(recommendedTitles("FANTASY", null)).containsExactly("Delta", "Alpha", "Echo", "Bravo", "Foxtrot");
    }

    @Test
    void recommendReturnsFourOfFourInStock() {
        stubRecommendation("FANTASY", flowOrderOfRanked(4), stockOfRanked(4));

        assertThat(recommendedTitles("FANTASY", 5)).containsExactly("Delta", "Alpha", "Echo", "Bravo");
    }

    @Test
    void recommendOrdersNewestFirst() {
        stubRecommendation("FANTASY", flowOrderOfRanked(5), stockOfRanked(5));

        List<BookSuggestionView> recommended = tools().recommendByGenre("FANTASY", 5);

        assertThat(recommended).extracting(BookSuggestionView::id).containsExactly(ranked(0).id(), ranked(1).id(),
                ranked(2).id(), ranked(3).id(), ranked(4).id());
        assertThat(recommended).extracting(BookSuggestionView::price).containsExactly("11.00", "8.50", "14.75",
                "6.20", "19.99");
    }

    @Test
    void recommendSkipsOutOfStockNewest() {
        Map<UUID, StockItemDTO> stock = stockOfRanked(6);
        stock.put(ranked(0).id(), rankedStock(0, 0));
        stubRecommendation("FANTASY", flowOrderOfRanked(6), stock);

        // the newest book is sold out, so the oldest in-stock one still fits under the cap of five
        assertThat(recommendedTitles("FANTASY", 5)).containsExactly("Alpha", "Echo", "Bravo", "Foxtrot", "Charlie");
    }

    @Test
    void recommendSkipsOutOfStockInsideTheCap() {
        Map<UUID, StockItemDTO> stock = stockOfRanked(7);
        stock.put(ranked(1).id(), rankedStock(1, 0));
        stubRecommendation("FANTASY", flowOrderOfRanked(7), stock);

        // a cap applied before the stock filter would return only two books
        assertThat(recommendedTitles("FANTASY", 3)).containsExactly("Delta", "Echo", "Bravo");
    }

    @Test
    void recommendFiltersOnAvailabilityOnly() {
        Map<UUID, StockItemDTO> stock = stockOfRanked(6);
        // rank 0: discontinued but in stock, kept
        // rank 1: nothing available while a restock is on its way, dropped
        stock.put(ranked(1).id(), new StockItemDTO(ranked(1).id(), 0, 0, 2, 10, true,
                new PendingRestockDTO("R-3", 10, LocalDate.of(2026, 12, 1), false)));
        // rank 2: on hand 3 and reserved 3, dropped
        stock.put(ranked(2).id(), new StockItemDTO(ranked(2).id(), 3, 3, 2, 10, false));
        // rank 3: one left, below the reorder level, kept
        stock.put(ranked(3).id(), new StockItemDTO(ranked(3).id(), 1, 0, 2, 10, true));
        List<BookDTO> flowOrder = new ArrayList<>(flowOrderOfRanked(6));
        BookDTO newestDiscontinued = ranked(0);
        flowOrder.set(5, new BookDTO(newestDiscontinued.id(), newestDiscontinued.isbn(), newestDiscontinued.title(),
                newestDiscontinued.authors(), newestDiscontinued.genre(), newestDiscontinued.priceAmount(), "EUR",
                newestDiscontinued.publicationYear(), "DISCONTINUED"));
        stubRecommendation("FANTASY", flowOrder, stock);

        assertThat(recommendedTitles("FANTASY", 5)).containsExactly("Delta", "Bravo", "Foxtrot", "Charlie");
    }

    @Test
    void recommendPassesTheGenreThrough() {
        when(catalogue.search(null, null, null, "FANTASY")).thenReturn(List.of(ranked(0)));
        when(catalogue.search(null, null, null, " science-fiction ")).thenReturn(List.of(ranked(1)));
        when(inventory.findByBookId(ranked(0).id())).thenReturn(rankedStock(0, 4));
        when(inventory.findByBookId(ranked(1).id())).thenReturn(rankedStock(1, 1));

        assertThat(recommendedTitles("FANTASY", 5)).containsExactly("Delta");
        assertThat(recommendedTitles(" science-fiction ", 5)).containsExactly("Alpha");
    }

    @Test
    void recommendBreaksYearTiesByTitle() {
        // Three books of one year. Natural String order puts "Mango" and "Zebra" before "apple"; a case-insensitive
        // order would put "apple" first. The other columns are not monotone in the asserted order.
        BookDTO mango = tied(2, "isbn-1", "Mango", "Mia", "10.00");
        BookDTO zebra = tied(0, "isbn-3", "Zebra", "Abe", "7.00");
        BookDTO apple = tied(1, "isbn-2", "apple", "Zed", "12.00");
        Map<UUID, StockItemDTO> stock = new HashMap<>();
        stock.put(mango.id(), new StockItemDTO(mango.id(), 4, 1, 1, 5, false));
        stock.put(zebra.id(), new StockItemDTO(zebra.id(), 2, 1, 1, 5, false));
        stock.put(apple.id(), new StockItemDTO(apple.id(), 3, 1, 1, 5, false));
        stubRecommendation("MYSTERY", List.of(apple, zebra, mango), stock);

        assertThat(recommendedTitles("MYSTERY", 5)).containsExactly("Mango", "Zebra", "apple");
    }

    @Test
    void recommendBreaksYearAndTitleTiesByIsbn() {
        // Three books of one year and one title; only the ISBN tells them apart. Id, price and available are not
        // monotone in the asserted ISBN order.
        BookDTO first = tied(2, "isbn-1", "Echo", "Mia", "10.00");
        BookDTO second = tied(0, "isbn-2", "Echo", "Abe", "7.00");
        BookDTO third = tied(1, "isbn-3", "Echo", "Zed", "12.00");
        Map<UUID, StockItemDTO> stock = new HashMap<>();
        stock.put(first.id(), new StockItemDTO(first.id(), 4, 1, 1, 5, false));
        stock.put(second.id(), new StockItemDTO(second.id(), 2, 1, 1, 5, false));
        stock.put(third.id(), new StockItemDTO(third.id(), 3, 1, 1, 5, false));
        stubRecommendation("MYSTERY", List.of(third, second, first), stock);

        assertThat(tools().recommendByGenre("MYSTERY", 5)).extracting(BookSuggestionView::id)
                .containsExactly(first.id(), second.id(), third.id());
    }

    private static BookDTO tied(int idRank, String isbn, String title, String author, String price) {
        return new BookDTO(new UUID(0x4000L, idRank), isbn, title, List.of(author), "MYSTERY",
                new BigDecimal(price), "EUR", 2010, "AVAILABLE");
    }

    // ---- error texts ----

    private static final String MALFORMED_ID = "Not a valid book id. Use an id returned by searchCatalogue or "
            + "recommendByGenre.";
    private static final String GENRE_REQUIRED = "A genre is required.";
    private static final String ALL_GENRES = "FICTION, SCIENCE_FICTION, FANTASY, MYSTERY, ROMANCE, HISTORY, SCIENCE, "
            + "TECHNOLOGY, CHILDREN, POETRY";
    private static final String TECHNICAL_FAILURE = "could not execute statement [select * from book]\n"
            + "\tat org.hibernate.X";

    private static String failureOf(ThrowableAssert.ThrowingCallable call) {
        Throwable failure = catchThrowable(call);
        assertThat(failure).isInstanceOf(RuntimeException.class);
        return failure.getMessage();
    }

    private static void assertOneCleanLine(String text) {
        assertThat(text).isNotBlank()
                .doesNotContain("\n")
                .doesNotContain("\r")
                .doesNotContain("\tat ")
                .doesNotContain("Exception")
                .doesNotContainIgnoringCase("select");
    }

    private void assertNoWarning() {
        assertThat(logEvents.list).isEmpty();
    }

    @Test
    void unknownBookGivesItsDetail() {
        UUID first = PRIDE_ID;
        UUID second = OMENS_ID;
        when(catalogue.findById(first)).thenThrow(new BookDataService.BookNotFoundException(first));
        when(catalogue.findById(second)).thenThrow(new BookDataService.BookNotFoundException(second));

        assertThat(failureOf(() -> tools().getBookDetails(first.toString())))
                .isEqualTo("No book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertThat(failureOf(() -> tools().getBookDetails(second.toString())))
                .isEqualTo("No book with id c0ffee00-1234-4abc-8def-0123456789ab");
        verify(inventory, never()).findByBookId(any(UUID.class));
        assertNoWarning();
    }

    @Test
    void missingStockGivesItsDetail() {
        when(inventory.findByBookId(PRIDE_ID)).thenThrow(new StockItemDataService.StockItemNotFoundException(PRIDE_ID));
        when(inventory.findByBookId(OMENS_ID)).thenThrow(new StockItemDataService.StockItemNotFoundException(OMENS_ID));
        when(catalogue.findById(OMENS_ID)).thenReturn(omens());

        assertThat(failureOf(() -> tools().checkAvailability(PRIDE_ID.toString())))
                .isEqualTo("No stock item for the book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertThat(failureOf(() -> tools().checkAvailability(OMENS_ID.toString())))
                .isEqualTo("No stock item for the book with id c0ffee00-1234-4abc-8def-0123456789ab");
        assertThat(failureOf(() -> tools().getBookDetails(OMENS_ID.toString())))
                .isEqualTo("No stock item for the book with id c0ffee00-1234-4abc-8def-0123456789ab");
        assertNoWarning();
    }

    @Test
    void missingStockInsideAListGivesItsDetail() {
        when(catalogue.search("gap", null, null, null)).thenReturn(List.of(pride()));
        when(catalogue.search(null, null, null, "ROMANCE")).thenReturn(List.of(pride()));
        when(inventory.findByBookId(PRIDE_ID)).thenThrow(new StockItemDataService.StockItemNotFoundException(PRIDE_ID));

        assertThat(failureOf(() -> tools().searchCatalogue("gap", null)))
                .isEqualTo("No stock item for the book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertThat(failureOf(() -> tools().recommendByGenre("ROMANCE", 5)))
                .isEqualTo("No stock item for the book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertNoWarning();
    }

    private static List<String> malformedIds() {
        List<String> ids = new ArrayList<>();
        ids.add("not-a-uuid");
        ids.add(null);
        ids.add("");
        ids.add("   ");
        ids.add(Character.toString(0x2003));
        ids.add(" " + PRIDE_ID);
        ids.add(PRIDE_ID + " ");
        ids.add("3f2a9c1e5b7d4e8a9c2f1a2b3c4d5e6f");
        // the JDK parser alone would accept this as a UUID
        ids.add("1-2-3-4-5");
        // canonical length with dashes in place, but not hex digits: the JDK parser itself throws on these
        ids.add("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz");
        ids.add("3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6g");
        return ids;
    }

    @Test
    void malformedIdGivesAFixedText() {
        for (String bookId : malformedIds()) {
            assertThat(failureOf(() -> tools().getBookDetails(bookId))).as("getBookDetails %s", bookId)
                    .isEqualTo(MALFORMED_ID);
            assertThat(failureOf(() -> tools().checkAvailability(bookId))).as("checkAvailability %s", bookId)
                    .isEqualTo(MALFORMED_ID);
        }
        verifyNoInteractions(catalogue, inventory);
        assertNoWarning();
    }

    @Test
    void blankGenreIsRejectedWithoutSearching() {
        for (String genre : Arrays.asList(null, "", "   ", Character.toString(0x2003))) {
            assertThat(failureOf(() -> tools().recommendByGenre(genre, 3))).as("genre %s", genre)
                    .isEqualTo(GENRE_REQUIRED);
        }
        verifyNoInteractions(catalogue, inventory);
        assertNoWarning();
    }

    @Test
    void unknownGenreListsTheValidGenres() {
        InvalidInputException madeUp = new InvalidInputException("unknown-genre", "Unknown genre",
                "Unknown genre 'pulp'",
                List.of(Map.of("genre", "ZETA"), Map.of("genre", "ALPHA")));
        InvalidInputException real = new InvalidInputException("unknown-genre", "Unknown genre",
                "Unknown genre 'space opera'",
                Arrays.stream(Genre.values()).map(genre -> Map.of("genre", genre.name())).toList());
        when(catalogue.search("book", null, null, "pulp")).thenThrow(madeUp);
        when(catalogue.search(null, null, null, "pulp")).thenThrow(madeUp);
        when(catalogue.search("book", null, null, "space opera")).thenThrow(real);
        when(catalogue.search(null, null, null, "space opera")).thenThrow(real);

        // made-up names, not alphabetical: nothing is hard-coded or sorted
        assertThat(failureOf(() -> tools().searchCatalogue("book", "pulp")))
                .isEqualTo("Unknown genre 'pulp' Valid genres: ZETA, ALPHA");
        assertThat(failureOf(() -> tools().recommendByGenre("pulp", 5)))
                .isEqualTo("Unknown genre 'pulp' Valid genres: ZETA, ALPHA");
        // the real shape, built like the catalogue flow builds it
        assertThat(failureOf(() -> tools().searchCatalogue("book", "space opera")))
                .isEqualTo("Unknown genre 'space opera' Valid genres: " + ALL_GENRES);
        assertThat(failureOf(() -> tools().recommendByGenre("space opera", 5)))
                .isEqualTo("Unknown genre 'space opera' Valid genres: " + ALL_GENRES);
        assertNoWarning();
    }

    @Test
    void invalidInputWithoutAGenreListKeepsJustItsDetail() {
        when(catalogue.search("book", null, null, "x")).thenThrow(
                new InvalidInputException("invalid-thing", "Invalid thing", "Something is off"));

        assertThat(failureOf(() -> tools().searchCatalogue("book", "x"))).isEqualTo("Something is off");

        // A rejection whose details name a field, not a genre: no detail has a genre key, so nothing is appended.
        when(catalogue.search("long", null, null, null)).thenThrow(
                new InvalidInputException("invalid-query", "Invalid query", "Query too long",
                        List.of(Map.of("field", "q", "message", "Query too long"))));

        assertThat(failureOf(() -> tools().searchCatalogue("long", null))).isEqualTo("Query too long");
    }

    @Test
    void unexpectedFailureGivesAGenericText() {
        IllegalStateException technical = new IllegalStateException(TECHNICAL_FAILURE);
        when(catalogue.search("austen", null, null, null)).thenThrow(technical);
        when(catalogue.findById(PRIDE_ID)).thenThrow(technical);
        when(inventory.findByBookId(OMENS_ID)).thenThrow(technical);
        when(catalogue.search(null, null, null, "FANTASY")).thenThrow(technical);

        String search = failureOf(() -> tools().searchCatalogue("austen", null));
        String details = failureOf(() -> tools().getBookDetails(PRIDE_ID.toString()));
        String availability = failureOf(() -> tools().checkAvailability(OMENS_ID.toString()));
        String recommend = failureOf(() -> tools().recommendByGenre("FANTASY", 5));

        assertThat(search).isEqualTo("searchCatalogue failed for a technical reason.");
        assertThat(details).isEqualTo("getBookDetails failed for a technical reason.");
        assertThat(availability).isEqualTo("checkAvailability failed for a technical reason.");
        assertThat(recommend).isEqualTo("recommendByGenre failed for a technical reason.");
        for (String text : List.of(search, details, availability, recommend)) {
            assertOneCleanLine(text);
        }
    }

    @Test
    void otherFailuresGiveTheGenericTextToo() {
        when(catalogue.search("one", null, null, null)).thenThrow(
                new ConflictException("conflict", "Conflict", "select * from book for update failed"));
        when(catalogue.search("two", null, null, null)).thenThrow(
                new UnavailableException("distributor-unavailable", "Distributor unavailable",
                        "select failed on host db-1"));
        when(catalogue.search("three", null, null, null)).thenThrow(
                new IllegalArgumentException("select * from book where id = 3"));
        when(catalogue.search("four", null, null, null)).thenThrow(new NullPointerException());

        for (String query : List.of("one", "two", "three", "four")) {
            assertThat(failureOf(() -> tools().searchCatalogue(query, null))).as(query)
                    .isEqualTo("searchCatalogue failed for a technical reason.");
        }
    }

    @Test
    void unexpectedFailureIsLoggedWithItsCause() {
        IllegalStateException first = new IllegalStateException(TECHNICAL_FAILURE);
        IllegalStateException second = new IllegalStateException("could not open connection");
        when(catalogue.search("austen", null, null, null)).thenThrow(first);
        when(catalogue.findById(PRIDE_ID)).thenThrow(second);
        when(catalogue.findById(OMENS_ID)).thenThrow(new BookDataService.BookNotFoundException(OMENS_ID));

        failureOf(() -> tools().searchCatalogue("austen", null));
        assertThat(logEvents.list).hasSize(1);
        assertThat(logEvents.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logEvents.list.get(0).getFormattedMessage()).isEqualTo("Tool searchCatalogue failed unexpectedly");
        assertThat(((ThrowableProxy) logEvents.list.get(0).getThrowableProxy()).getThrowable()).isSameAs(first);

        failureOf(() -> tools().getBookDetails(PRIDE_ID.toString()));
        assertThat(logEvents.list).hasSize(2);
        assertThat(logEvents.list.get(1).getLevel()).isEqualTo(Level.WARN);
        assertThat(logEvents.list.get(1).getFormattedMessage()).isEqualTo("Tool getBookDetails failed unexpectedly");
        assertThat(((ThrowableProxy) logEvents.list.get(1).getThrowableProxy()).getThrowable()).isSameAs(second);

        // curated paths log nothing
        failureOf(() -> tools().getBookDetails(OMENS_ID.toString()));
        failureOf(() -> tools().getBookDetails("not-a-uuid"));
        failureOf(() -> tools().recommendByGenre(" ", 5));
        assertThat(logEvents.list).hasSize(2);
    }

    // ---- through Spring AI's callbacks ----

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DefaultToolExecutionExceptionProcessor PROCESSOR = DefaultToolExecutionExceptionProcessor
            .builder().build();

    private Map<String, ToolCallback> callbacks() {
        Map<String, ToolCallback> byName = new HashMap<>();
        for (ToolCallback callback : MethodToolCallbackProvider.builder().toolObjects(tools()).build()
                .getToolCallbacks()) {
            byName.put(callback.getToolDefinition().name(), callback);
        }
        return byName;
    }

    /** What the model gets when the tool fails: the callback's exception run through Spring AI's default processor. */
    private String textTheModelGets(String tool, String arguments) {
        ToolCallback callback = callbacks().get(tool);
        ToolExecutionException failure = catchThrowableOfType(ToolExecutionException.class,
                () -> callback.call(arguments));
        assertThat(failure).as("%s %s", tool, arguments).isNotNull();
        String text = PROCESSOR.process(failure);
        assertOneCleanLine(text);
        return text;
    }

    @Test
    void errorTextsReachTheModelThroughTheCallback() {
        IllegalStateException technical = new IllegalStateException(TECHNICAL_FAILURE);
        when(catalogue.search("austen", null, null, null)).thenThrow(technical);
        when(catalogue.findById(PRIDE_ID)).thenThrow(new BookDataService.BookNotFoundException(PRIDE_ID));
        when(catalogue.findById(OMENS_ID)).thenThrow(technical);
        when(inventory.findByBookId(PRIDE_ID)).thenThrow(
                new StockItemDataService.StockItemNotFoundException(PRIDE_ID));
        when(inventory.findByBookId(OMENS_ID)).thenThrow(technical);
        when(catalogue.search(null, null, null, "FANTASY")).thenThrow(technical);
        InvalidInputException unknownGenre = new InvalidInputException("unknown-genre", "Unknown genre",
                "Unknown genre 'pulp'", List.of(Map.of("genre", "ZETA"), Map.of("genre", "ALPHA")));
        when(catalogue.search("book", null, null, "pulp")).thenThrow(unknownGenre);
        when(catalogue.search(null, null, null, "pulp")).thenThrow(unknownGenre);

        // unexpected failure, once per tool
        assertThat(textTheModelGets("searchCatalogue", "{\"query\":\"austen\"}"))
                .isEqualTo("searchCatalogue failed for a technical reason.");
        assertThat(textTheModelGets("getBookDetails", "{\"bookId\":\"" + OMENS_ID + "\"}"))
                .isEqualTo("getBookDetails failed for a technical reason.");
        assertThat(textTheModelGets("checkAvailability", "{\"bookId\":\"" + OMENS_ID + "\"}"))
                .isEqualTo("checkAvailability failed for a technical reason.");
        assertThat(textTheModelGets("recommendByGenre", "{\"genre\":\"FANTASY\",\"limit\":3}"))
                .isEqualTo("recommendByGenre failed for a technical reason.");
        // flow details
        assertThat(textTheModelGets("getBookDetails", "{\"bookId\":\"" + PRIDE_ID + "\"}"))
                .isEqualTo("No book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertThat(textTheModelGets("checkAvailability", "{\"bookId\":\"" + PRIDE_ID + "\"}"))
                .isEqualTo("No stock item for the book with id 3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f");
        assertThat(textTheModelGets("searchCatalogue", "{\"query\":\"book\",\"genre\":\"pulp\"}"))
                .isEqualTo("Unknown genre 'pulp' Valid genres: ZETA, ALPHA");
        assertThat(textTheModelGets("recommendByGenre", "{\"genre\":\"pulp\",\"limit\":2}"))
                .isEqualTo("Unknown genre 'pulp' Valid genres: ZETA, ALPHA");
        // tool-level input problems
        assertThat(textTheModelGets("getBookDetails", "{\"bookId\":\"not-a-uuid\"}")).isEqualTo(MALFORMED_ID);
        assertThat(textTheModelGets("checkAvailability", "{\"bookId\":null}")).isEqualTo(MALFORMED_ID);
        assertThat(textTheModelGets("checkAvailability", "{}")).isEqualTo(MALFORMED_ID);
        assertThat(textTheModelGets("recommendByGenre", "{\"genre\":\"  \",\"limit\":2}")).isEqualTo(GENRE_REQUIRED);
        assertThat(textTheModelGets("recommendByGenre", "{\"genre\":null,\"limit\":null}")).isEqualTo(GENRE_REQUIRED);
    }

    @Test
    void toolsCarryTheSpecDescriptions() {
        Map<String, ToolCallback> byName = callbacks();

        assertThat(byName.keySet()).containsExactlyInAnyOrder("searchCatalogue", "getBookDetails",
                "checkAvailability", "recommendByGenre");
        assertThat(byName.get("searchCatalogue").getToolDefinition().description()).isEqualTo(
                "Search the bookstore catalogue by title or author, optionally filtered by genre. "
                        + "Use this before mentioning any book.");
        assertThat(byName.get("getBookDetails").getToolDefinition().description())
                .isEqualTo("Get details of one book from the catalogue by its id.");
        assertThat(byName.get("checkAvailability").getToolDefinition().description())
                .isEqualTo("Check how many copies are available and whether a restock is on its way.");
        assertThat(byName.get("recommendByGenre").getToolDefinition().description())
                .isEqualTo("Recommend in-stock books of a genre, newest publication first. limit max 5.");
    }

    @Test
    void toolSchemasRequireTheRightParameters() {
        Map<String, ToolCallback> byName = callbacks();

        assertSchema(byName.get("searchCatalogue"), List.of("query", "genre"), List.of("query"));
        assertSchema(byName.get("getBookDetails"), List.of("bookId"), List.of("bookId"));
        assertSchema(byName.get("checkAvailability"), List.of("bookId"), List.of("bookId"));
        assertSchema(byName.get("recommendByGenre"), List.of("genre", "limit"), List.of("genre", "limit"));
    }

    @SuppressWarnings("unchecked")
    private static void assertSchema(ToolCallback callback, List<String> properties, List<String> required) {
        Map<String, Object> schema = JSON.readValue(callback.getToolDefinition().inputSchema(),
                new TypeReference<Map<String, Object>>() {
                });
        assertThat(((Map<String, Object>) schema.get("properties")).keySet()).containsExactlyInAnyOrderElementsOf(
                properties);
        assertThat((List<String>) schema.get("required")).containsExactlyInAnyOrderElementsOf(required);
    }

    @Test
    void suggestionJsonHasExactlySixKeys() {
        when(catalogue.search("both", null, null, null)).thenReturn(List.of(omens(), pride()));
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 3, 1, 1, 5, false));
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 1, 1, 1, 5, false));

        String json = callbacks().get("searchCatalogue").call("{\"query\":\"both\"}");
        List<Map<String, Object>> hits = JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() {
        });

        assertThat(hits).hasSize(2);
        for (Map<String, Object> hit : hits) {
            assertThat(hit.keySet()).containsExactlyInAnyOrder("id", "title", "authors", "genre", "price",
                    "inStock");
        }
        assertThat(hits.get(0)).containsEntry("id", "c0ffee00-1234-4abc-8def-0123456789ab")
                .containsEntry("title", " Good Omens ")
                .containsEntry("authors", List.of("Terry Pratchett", "Neil Gaiman"))
                .containsEntry("genre", "FANTASY")
                .containsEntry("price", "12.50")
                .containsEntry("inStock", true);
        assertThat(hits.get(1)).containsEntry("id", "3f2a9c1e-5b7d-4e8a-9c2f-1a2b3c4d5e6f")
                .containsEntry("price", "9.99")
                .containsEntry("inStock", false);
    }

    @Test
    void detailsAndAvailabilityJsonHaveExactKeys() {
        when(catalogue.findById(OMENS_ID)).thenReturn(omens());
        when(inventory.findByBookId(OMENS_ID)).thenReturn(new StockItemDTO(OMENS_ID, 3, 1, 1, 5, false));
        when(inventory.findByBookId(PRIDE_ID)).thenReturn(new StockItemDTO(PRIDE_ID, 9, 2, 5, 20, true,
                new PendingRestockDTO("R-7", 20, LocalDate.of(2001, 3, 4), false)));

        Map<String, Object> details = JSON.readValue(
                callbacks().get("getBookDetails").call("{\"bookId\":\"" + OMENS_ID + "\"}"),
                new TypeReference<Map<String, Object>>() {
                });
        Map<String, Object> withRestock = JSON.readValue(
                callbacks().get("checkAvailability").call("{\"bookId\":\"" + PRIDE_ID + "\"}"),
                new TypeReference<Map<String, Object>>() {
                });
        Map<String, Object> withoutRestock = JSON.readValue(
                callbacks().get("checkAvailability").call("{\"bookId\":\"" + OMENS_ID + "\"}"),
                new TypeReference<Map<String, Object>>() {
                });

        assertThat(details.keySet()).containsExactlyInAnyOrder("id", "title", "authors", "genre", "price",
                "inStock", "publicationYear");
        assertThat(details).containsEntry("publicationYear", 2020).containsEntry("price", "12.50");
        assertThat(withRestock.keySet()).containsExactlyInAnyOrder("available", "pendingRestockExpected");
        assertThat(withRestock).containsEntry("available", 7).containsEntry("pendingRestockExpected", "2001-03-04");
        assertThat(withoutRestock.keySet()).containsExactlyInAnyOrder("available", "pendingRestockExpected");
        assertThat(withoutRestock).containsEntry("available", 2).containsEntry("pendingRestockExpected", null);
    }

    // ---- log recorder ----

    private final ListAppender<ILoggingEvent> logEvents = new ListAppender<>();
    private Logger toolsLogger;
    private Level originalLevel;
    private boolean originalAdditive;

    @BeforeEach
    void recordToolsLog() {
        toolsLogger = (Logger) LoggerFactory.getLogger(BookstoreTools.class);
        originalLevel = toolsLogger.getLevel();
        originalAdditive = toolsLogger.isAdditive();
        // Every level is recorded, and nothing reaches the console, so the expected WARN traces stay out of the
        // build log.
        toolsLogger.setLevel(Level.TRACE);
        toolsLogger.setAdditive(false);
        logEvents.start();
        toolsLogger.addAppender(logEvents);
    }

    @AfterEach
    void restoreToolsLog() {
        toolsLogger.detachAppender(logEvents);
        logEvents.stop();
        toolsLogger.setLevel(originalLevel);
        toolsLogger.setAdditive(originalAdditive);
    }
}
