package nl.openvalue.bookstore.query;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(BookQueries.class)
class BookQueriesTest {

    private static final UUID BOOK_ID = UUID.fromString("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10");

    private static final UUID OTHER_ID = UUID.fromString("9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66");

    private static final UUID EMMA_ID = UUID.fromString("50000000-0000-4000-8000-00000000000a");

    private static final UUID DRACULA_ID = UUID.fromString("20000000-0000-4000-8000-00000000000b");

    private static final UUID FRANKENSTEIN_ID = UUID.fromString("60000000-0000-4000-8000-00000000000c");

    private static final BookDTO EMMA = new BookDTO(EMMA_ID, "9780141439587", "Emma", List.of("Jane Austen"),
            "FICTION", new BigDecimal("7.99"), "EUR", 1815, "AVAILABLE");

    private static final BookDTO DRACULA = new BookDTO(DRACULA_ID, "9780141439846", "Dracula",
            List.of("Bram Stoker"), "FANTASY", new BigDecimal("6.99"), "EUR", 1897, "DISCONTINUED");

    private static final BookDTO FRANKENSTEIN = new BookDTO(FRANKENSTEIN_ID, "9780141439471", "Frankenstein",
            List.of("Mary Shelley"), "SCIENCE_FICTION", new BigDecimal("9.50"), "EUR", 1818, "AVAILABLE");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogueFlow flow;

    @MockitoBean
    private InventoryFlow inventory;

    /** Every book has a stock item unless a test says otherwise: 7 on hand, 2 reserved, so 5 available. */
    @BeforeEach
    void everyBookHasAStockItem() {
        when(inventory.findByBookId(any(UUID.class))).thenAnswer(invocation -> new StockItemDTO(
                invocation.getArgument(0), 7, 2, 3, 10, false));
    }

    // --- search ---

    @Test
    void searchReturnsBookViewsWithJoinedAuthorsAndAStringPrice() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of(book(BOOK_ID, "12.50",
                List.of("Jane Austen", "Mary Shelley"))));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))
                .andExpect(jsonPath("$[0].isbn").value("9780141439518"))
                .andExpect(jsonPath("$[0].title").value("Pride and Prejudice"))
                .andExpect(jsonPath("$[0].authors").value("Jane Austen, Mary Shelley"))
                .andExpect(jsonPath("$[0].genre").value("FICTION"))
                .andExpect(jsonPath("$[0].price").isString())
                .andExpect(jsonPath("$[0].price").value("12.50"))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].publicationYear").doesNotExist())
                .andExpect(jsonPath("$[0].*", hasSize(10)));
    }

    @Test
    void searchKeepsTheAuthorOrderOfTheBookAndTheOrderOfTheResult() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of(
                book(BOOK_ID, "12.50", List.of("Mary Shelley", "Jane Austen")),
                book(OTHER_ID, "8.00", List.of("Zadie Smith", "Ann Leckie", "Mary Shelley"))));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))
                .andExpect(jsonPath("$[0].authors").value("Mary Shelley, Jane Austen"))
                .andExpect(jsonPath("$[1].id").value("9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66"))
                .andExpect(jsonPath("$[1].authors").value("Zadie Smith, Ann Leckie, Mary Shelley"))
                .andExpect(jsonPath("$[1].price").value("8.00"));
    }

    @Test
    void searchReturnsAnEmptyArrayWhenNothingMatches() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchPassesTheFourRawQueryParametersToTheFlowInOrder() throws Exception {
        mockMvc.perform(get("/books")
                        .param("q", " Emma ")
                        .param("title", "Persuasion")
                        .param("author", "Austen")
                        .param("genre", "science-fiction"))
                .andExpect(status().isOk());

        verify(flow).search(" Emma ", "Persuasion", "Austen", "science-fiction");
    }

    @Test
    void searchPassesAbsentQueryParametersAsNull() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk());

        verify(flow).search(null, null, null, null);
    }

    @Test
    void searchMapsInvalidInputTo422UnknownGenreWithTheGenreList() throws Exception {
        when(flow.search(null, null, null, "nonsense")).thenThrow(new InvalidInputException("unknown-genre",
                "Unknown genre", "The genre 'nonsense' is not known", List.of(
                        Map.of("genre", "FICTION"), Map.of("genre", "SCIENCE_FICTION"), Map.of("genre", "FANTASY"),
                        Map.of("genre", "MYSTERY"), Map.of("genre", "ROMANCE"), Map.of("genre", "HISTORY"),
                        Map.of("genre", "SCIENCE"), Map.of("genre", "TECHNOLOGY"), Map.of("genre", "CHILDREN"),
                        Map.of("genre", "POETRY"))));

        ResultActions result = mockMvc.perform(get("/books").param("genre", "nonsense"));

        expectProblem(result, 422, "unknown-genre", "Unknown genre")
                .andExpect(jsonPath("$.details", hasSize(10)))
                .andExpect(jsonPath("$.details[*].genre").value(contains("FICTION", "SCIENCE_FICTION", "FANTASY",
                        "MYSTERY", "ROMANCE", "HISTORY", "SCIENCE", "TECHNOLOGY", "CHILDREN", "POETRY")));
    }

    @Test
    void searchMapsConflictTo409() throws Exception {
        when(flow.search(null, null, null, null)).thenThrow(new ConflictException("book-discontinued",
                "Book discontinued", "The book is discontinued"));

        ResultActions result = mockMvc.perform(get("/books"));

        expectProblem(result, 409, "book-discontinued", "Book discontinued")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void searchMapsUnavailableTo503WithTheCodeAndDetailsOfTheException() throws Exception {
        when(flow.search(null, null, null, null)).thenThrow(new UnavailableException("distributor-unavailable",
                "Distributor unavailable", "The distributor does not answer", List.of(Map.of("service",
                        "distributor"))));

        ResultActions result = mockMvc.perform(get("/books"));

        expectProblem(result, 503, "distributor-unavailable", "Distributor unavailable")
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].service").value("distributor"));
    }

    /**
     * Three rows in the order of the flow, sorted in no column in either direction: every column goes up then
     * down or down then up. The ids start with 5, 2 and 6; all three first hex digits are 0 to 7, so Java's signed
     * {@code UUID.compareTo} and PostgreSQL's unsigned byte order agree, and both see 5, 2, 6. ISBN 587, 846, 471;
     * title E, D, F; authors J, B, M; genre FICTION, FANTASY, SCIENCE_FICTION; price 7.99, 6.99, 9.50; status
     * AVAILABLE, DISCONTINUED, AVAILABLE; year 1815, 1897, 1818; on hand 8, 2, 4; reserved 3, 2, 3; available 5, 0,
     * 1; inStock true, false, true; reorder level 2, 5, 3; reorder quantity 10, 20, 15; signal false, true, false.
     */
    @Test
    void searchShowsTheAvailableStockAndInStockOfEachBook() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of(EMMA, DRACULA, FRANKENSTEIN));
        when(inventory.findByBookId(EMMA_ID)).thenReturn(new StockItemDTO(EMMA_ID, 8, 3, 2, 10, false));
        when(inventory.findByBookId(DRACULA_ID)).thenReturn(new StockItemDTO(DRACULA_ID, 2, 2, 5, 20, true));
        when(inventory.findByBookId(FRANKENSTEIN_ID)).thenReturn(new StockItemDTO(FRANKENSTEIN_ID, 4, 3, 3, 15,
                false));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id").value(contains("50000000-0000-4000-8000-00000000000a",
                        "20000000-0000-4000-8000-00000000000b", "60000000-0000-4000-8000-00000000000c")))
                .andExpect(jsonPath("$[*].isbn").value(contains("9780141439587", "9780141439846",
                        "9780141439471")))
                .andExpect(jsonPath("$[*].title").value(contains("Emma", "Dracula", "Frankenstein")))
                .andExpect(jsonPath("$[*].authors").value(contains("Jane Austen", "Bram Stoker", "Mary Shelley")))
                .andExpect(jsonPath("$[*].genre").value(contains("FICTION", "FANTASY", "SCIENCE_FICTION")))
                .andExpect(jsonPath("$[*].price").value(contains("7.99", "6.99", "9.50")))
                .andExpect(jsonPath("$[*].status").value(contains("AVAILABLE", "DISCONTINUED", "AVAILABLE")))
                .andExpect(jsonPath("$[*].available").value(contains(5, 0, 1)))
                .andExpect(jsonPath("$[*].inStock").value(contains(true, false, true)));

        verify(inventory).findByBookId(EMMA_ID);
        verify(inventory).findByBookId(DRACULA_ID);
        verify(inventory).findByBookId(FRANKENSTEIN_ID);
        verifyNoMoreInteractions(inventory);
    }

    @Test
    void searchShowsAvailableAsANumberAndInStockAsABoolean() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of(book(BOOK_ID, "12.50",
                List.of("Jane Austen"))));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").isNumber())
                .andExpect(jsonPath("$[0].available").value(5))
                .andExpect(jsonPath("$[0].inStock").isBoolean())
                .andExpect(jsonPath("$[0].inStock").value(true));
    }

    @Test
    void searchPassesAMissingStockItemThroughAs404() throws Exception {
        when(flow.search(null, null, null, null)).thenReturn(List.of(EMMA, DRACULA));
        when(inventory.findByBookId(DRACULA_ID)).thenThrow(
                new StockItemDataService.StockItemNotFoundException(DRACULA_ID));

        ResultActions result = mockMvc.perform(get("/books"));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- get one ---

    @Test
    void findByIdReturnsTheDetailViewWithTheAuthorsArrayAndThePublicationYear() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(book(BOOK_ID, "12.50", List.of("Jane Austen", "Mary Shelley")));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))
                .andExpect(jsonPath("$.isbn").value("9780141439518"))
                .andExpect(jsonPath("$.title").value("Pride and Prejudice"))
                .andExpect(jsonPath("$.authors").isArray())
                .andExpect(jsonPath("$.authors", contains("Jane Austen", "Mary Shelley")))
                .andExpect(jsonPath("$.genre").value("FICTION"))
                .andExpect(jsonPath("$.price").isString())
                .andExpect(jsonPath("$.price").value("12.50"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.publicationYear").value(1813))
                .andExpect(jsonPath("$.*", hasSize(11)));
    }

    @Test
    void findByIdKeepsTheAuthorOrderOfTheBook() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(book(BOOK_ID, "12.50", List.of("Mary Shelley", "Jane Austen")));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors", contains("Mary Shelley", "Jane Austen")));
    }

    @Test
    void findByIdShowsADiscontinuedBookWithItsStatusName() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(new BookDTO(BOOK_ID, "9780141439518", "Pride and Prejudice",
                List.of("Jane Austen"), "SCIENCE_FICTION", new BigDecimal("7"), "EUR", 1813, "DISCONTINUED"));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre").value("SCIENCE_FICTION"))
                .andExpect(jsonPath("$.status").value("DISCONTINUED"))
                .andExpect(jsonPath("$.price").value("7"));
    }

    @Test
    void findByIdMapsNotFoundTo404BookNotFound() throws Exception {
        when(flow.findById(any(UUID.class))).thenThrow(new NotFoundException("book-not-found", "Book not found",
                "No book with this id is in the catalogue"));

        ResultActions result = mockMvc.perform(get("/books/{id}", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void findByIdMapsNotFoundTo404WithTheDetailsOfTheException() throws Exception {
        when(flow.findById(BOOK_ID)).thenThrow(new NotFoundException("book-not-found", "Book not found",
                "No book with this id is in the catalogue", List.of(Map.of("id",
                        "3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))));

        ResultActions result = mockMvc.perform(get("/books/{id}", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].id").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"));
    }

    @Test
    void searchMapsConflictTo409WithTheDetailsOfTheException() throws Exception {
        when(flow.search(null, null, null, null)).thenThrow(new ConflictException("book-discontinued",
                "Book discontinued", "The book is discontinued", List.of(Map.of("id",
                        "9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66"))));

        ResultActions result = mockMvc.perform(get("/books"));

        expectProblem(result, 409, "book-discontinued", "Book discontinued")
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].id").value("9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66"));
    }

    @Test
    void findByIdMapsUnavailableTo503() throws Exception {
        when(flow.findById(any(UUID.class))).thenThrow(new UnavailableException("distributor-unavailable",
                "Distributor unavailable", "The distributor does not answer"));

        ResultActions result = mockMvc.perform(get("/books/{id}", BOOK_ID));

        expectProblem(result, 503, "distributor-unavailable", "Distributor unavailable")
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void notFoundPassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        when(flow.findById(any(UUID.class))).thenThrow(new NotFoundException("sample-not-found", "Sample not found",
                "No sample with that id"));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-not-found"))
                .andExpect(jsonPath("$.title").value("Sample not found"))
                .andExpect(jsonPath("$.detail").value("No sample with that id"));
    }

    @Test
    void conflictPassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        when(flow.search(null, null, null, null)).thenThrow(new ConflictException("sample-conflict",
                "Sample conflict", "The sample is in conflict"));

        mockMvc.perform(get("/books"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-conflict"))
                .andExpect(jsonPath("$.title").value("Sample conflict"))
                .andExpect(jsonPath("$.detail").value("The sample is in conflict"));
    }

    @Test
    void invalidInputPassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        when(flow.search(null, null, null, null)).thenThrow(new InvalidInputException("sample-invalid",
                "Sample invalid", "The sample is invalid", List.of(Map.of("field", "sample", "message", "bad"))));

        mockMvc.perform(get("/books"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-invalid"))
                .andExpect(jsonPath("$.title").value("Sample invalid"))
                .andExpect(jsonPath("$.detail").value("The sample is invalid"));
    }

    @Test
    void unavailablePassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        when(flow.findById(any(UUID.class))).thenThrow(new UnavailableException("sample-unavailable",
                "Sample unavailable", "The sample is unavailable"));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-unavailable"))
                .andExpect(jsonPath("$.title").value("Sample unavailable"))
                .andExpect(jsonPath("$.detail").value("The sample is unavailable"));
    }

    @Test
    void findByIdShowsTheAvailableStockAndInStock() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(book(BOOK_ID, "12.50", List.of("Jane Austen")));
        when(inventory.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 4, 4, 3, 10, true));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").isNumber())
                .andExpect(jsonPath("$.available").value(0))
                .andExpect(jsonPath("$.inStock").isBoolean())
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    void findByIdShowsInStockWhenOneCopyIsAvailable() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(book(BOOK_ID, "12.50", List.of("Jane Austen")));
        when(inventory.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 3, 2, 3, 10, true));

        mockMvc.perform(get("/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(1))
                .andExpect(jsonPath("$.inStock").isBoolean())
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void findByIdPassesAMissingStockItemThroughAs404() throws Exception {
        when(flow.findById(BOOK_ID)).thenReturn(book(BOOK_ID, "12.50", List.of("Jane Austen")));
        when(inventory.findByBookId(BOOK_ID)).thenThrow(new StockItemDataService.StockItemNotFoundException(BOOK_ID));

        ResultActions result = mockMvc.perform(get("/books/{id}", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void findByIdDoesNotAskForStockWhenTheBookIsUnknown() throws Exception {
        when(flow.findById(BOOK_ID)).thenThrow(new BookDataService.BookNotFoundException(BOOK_ID));

        ResultActions result = mockMvc.perform(get("/books/{id}", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isEmpty());
        verifyNoInteractions(inventory);
    }

    // --- helpers ---

    private static BookDTO book(UUID id, String price, List<String> authors) {
        return new BookDTO(id, "9780141439518", "Pride and Prejudice", authors, "FICTION", new BigDecimal(price),
                "EUR", 1813, "AVAILABLE");
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code, String title)
            throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details").exists());
    }
}
