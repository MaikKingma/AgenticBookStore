package nl.openvalue.bookstore.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@WebMvcTest(BookCommands.class)
class BookCommandsTest {

    private static final String VALID_ADD_BODY = """
            {"isbn":"9780141439518","title":"Pride and Prejudice","authors":["Jane Austen"],
             "genre":"FICTION","price":"12.50","publicationYear":1813}""";

    private static final UUID BOOK_ID = UUID.fromString("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogueFlow flow;

    // --- add ---

    @Test
    void addReturns202WithTheNewBookId() throws Exception {
        UUID newId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001");
        when(flow.addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "FICTION",
                new BigDecimal("12.50"), 1813)).thenReturn(newId);

        mockMvc.perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(VALID_ADD_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("a1b2c3d4-0000-4000-8000-000000000001"));
    }

    @Test
    void addMapsInvalidInputTo422WithCodeAndDetails() throws Exception {
        when(flow.addBook(anyString(), anyString(), anyList(), anyString(), any(BigDecimal.class), anyInt()))
                .thenThrow(new InvalidInputException("invalid-isbn", "Invalid ISBN", "The ISBN is not valid",
                        List.of(Map.of("field", "isbn", "message", "bad checksum"))));

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(VALID_ADD_BODY));

        expectProblem(result, 422, "invalid-isbn", "Invalid ISBN")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("isbn"))
                .andExpect(jsonPath("$.details[0].message").value("bad checksum"));
    }

    @Test
    void addMapsConflictTo409IsbnAlreadyExists() throws Exception {
        when(flow.addBook(anyString(), anyString(), anyList(), anyString(), any(BigDecimal.class), anyInt()))
                .thenThrow(new ConflictException("isbn-already-exists", "ISBN already in catalogue",
                        "A book with this ISBN is already in the catalogue"));

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(VALID_ADD_BODY));

        expectProblem(result, 409, "isbn-already-exists", "ISBN already in catalogue")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void addRejectsMissingFieldsWithValidationFailedListingEveryField() throws Exception {
        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content("{}"));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(6))
                .andExpect(jsonPath("$.details[?(@.field=='isbn')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='authors')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='genre')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='price')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='publicationYear')]").exists())
                .andExpect(jsonPath("$.details[*].field")
                        .value(contains("authors", "genre", "isbn", "price", "publicationYear", "title")));
        verifyNoInteractions(flow);
    }

    @Test
    void addRejectsAMissingAuthorsListWith400() throws Exception {
        String body = """
                {"isbn":"9780141439518","title":"Pride and Prejudice",
                 "genre":"FICTION","price":"12.50","publicationYear":1813}""";

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(body));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[?(@.field=='authors')]").exists());
        verifyNoInteractions(flow);
    }

    @Test
    void addRejectsABlankAuthorNameNamingTheListElement() throws Exception {
        String body = """
                {"isbn":"9780141439518","title":"Pride and Prejudice","authors":["", "Jane Austen"],
                 "genre":"FICTION","price":"12.50","publicationYear":1813}""";

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(body));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[?(@.field=='authors[0]')]").exists());
        verifyNoInteractions(flow);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12,50", "1e3", ".5", "12.", "-"})
    void addRejectsANonDecimalPriceWith400(String price) throws Exception {
        String body = """
                {"isbn":"9780141439518","title":"Pride and Prejudice","authors":["Jane Austen"],
                 "genre":"FICTION","price":"%s","publicationYear":1813}""".formatted(price);

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(body));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details[?(@.field=='price')]").exists());
        verifyNoInteractions(flow);
    }

    @Test
    void addPassesANegativePriceToTheFlowSoTheDomainRejectsIt() throws Exception {
        when(flow.addBook(anyString(), anyString(), anyList(), anyString(), any(BigDecimal.class), anyInt()))
                .thenThrow(new InvalidInputException("price-out-of-range", "Price out of range",
                        "The price is out of range",
                        List.of(Map.of("field", "price", "message", "must be between 0.01 and 500.00"))));
        String body = """
                {"isbn":"9780141439518","title":"Pride and Prejudice","authors":["Jane Austen"],
                 "genre":"FICTION","price":"-5","publicationYear":1813}""";

        ResultActions result = mockMvc
                .perform(post("/books/commands/add").contentType(MediaType.APPLICATION_JSON).content(body));

        expectProblem(result, 422, "price-out-of-range", "Price out of range")
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verify(flow).addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "FICTION",
                new BigDecimal("-5"), 1813);
    }

    // --- change price ---

    @Test
    void changePriceReturns202WithoutABodyAndPassesTheAmount() throws Exception {
        mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":\"12.50\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).changePrice(BOOK_ID, new BigDecimal("12.50"));
    }

    @Test
    void changePriceMapsNotFoundTo404BookNotFound() throws Exception {
        doThrow(new NotFoundException("book-not-found", "Book not found", "No book with that id"))
                .when(flow).changePrice(any(UUID.class), any(BigDecimal.class));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"12.50\"}"));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void changePriceMapsConflictTo409BookDiscontinued() throws Exception {
        doThrow(new ConflictException("book-discontinued", "Book discontinued", "The book is discontinued"))
                .when(flow).changePrice(any(UUID.class), any(BigDecimal.class));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"12.50\"}"));

        expectProblem(result, 409, "book-discontinued", "Book discontinued")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void changePriceMapsInvalidInputTo422PriceOutOfRange() throws Exception {
        doThrow(new InvalidInputException("price-out-of-range", "Price out of range", "The price is out of range",
                List.of(Map.of("field", "price", "message", "must be between 0.01 and 500.00"))))
                .when(flow).changePrice(any(UUID.class), any(BigDecimal.class));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"900.00\"}"));

        expectProblem(result, 422, "price-out-of-range", "Price out of range")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("price"))
                .andExpect(jsonPath("$.details[0].message").value("must be between 0.01 and 500.00"));
    }

    @Test
    void changePriceRejectsAMissingPriceWith400() throws Exception {
        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[?(@.field=='price')]").exists());
        verifyNoInteractions(flow);
    }

    @Test
    void changePriceRejectsANonDecimalPriceWith400() throws Exception {
        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"twelve\"}"));

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details[?(@.field=='price')]").exists());
        verifyNoInteractions(flow);
    }

    // --- discontinue ---

    @Test
    void discontinueReturns202WithoutABody() throws Exception {
        mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).discontinue(BOOK_ID);
    }

    @Test
    void discontinueMapsNotFoundTo404() throws Exception {
        doThrow(new NotFoundException("book-not-found", "Book not found", "No book with that id"))
                .when(flow).discontinue(any(UUID.class));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void discontinueMapsNotFoundTo404WithTheDetailsOfTheException() throws Exception {
        doThrow(new NotFoundException("book-not-found", "Book not found", "No book with that id",
                List.of(Map.of("id", "3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))))
                .when(flow).discontinue(BOOK_ID);

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].id").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"));
    }

    @Test
    void changePriceMapsConflictTo409WithTheDetailsOfTheException() throws Exception {
        doThrow(new ConflictException("book-discontinued", "Book discontinued", "The book is discontinued",
                List.of(Map.of("id", "9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66"))))
                .when(flow).changePrice(BOOK_ID, new BigDecimal("12.50"));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/change-price", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":\"12.50\"}"));

        expectProblem(result, 409, "book-discontinued", "Book discontinued")
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].id").value("9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66"));
    }

    @Test
    void notFoundPassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        doThrow(new NotFoundException("sample-not-found", "Sample not found", "No sample with that id"))
                .when(flow).discontinue(any(UUID.class));

        mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-not-found"))
                .andExpect(jsonPath("$.title").value("Sample not found"))
                .andExpect(jsonPath("$.detail").value("No sample with that id"));
    }

    // --- import from distributor ---

    private static final UUID IMPORTED_ID = UUID.fromString("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a");

    private ResultActions postImport(String body) throws Exception {
        return mockMvc.perform(
                post("/books/commands/import-from-distributor").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void importReturns202WithTheNewBookIdAndPassesTheIsbnAsSent() throws Exception {
        when(flow.importFromDistributor("9780141439518", null)).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780141439518\"}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a"));

        verify(flow).importFromDistributor("9780141439518", null);
    }

    @Test
    void importReturnsTheIdTheFlowReturnedForAnotherIsbn() throws Exception {
        UUID otherId = UUID.fromString("c8f2d1ef-3b5a-4e7c-9d4b-7a6f5e4d3c2b");
        when(flow.importFromDistributor("9780140449136", null)).thenReturn(otherId);

        postImport("{\"isbn\":\"9780140449136\"}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("c8f2d1ef-3b5a-4e7c-9d4b-7a6f5e4d3c2b"));
    }

    @Test
    void importWithAnExplicitNullPriceReachesTheFlowWithoutAnOverride() throws Exception {
        when(flow.importFromDistributor("9780140449136", null)).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780140449136\",\"price\":null}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a"));

        verify(flow).importFromDistributor("9780140449136", null);
    }

    static Stream<Arguments> priceStringsAndTheAmountTheFlowReceives() {
        return Stream.of(
                Arguments.of("12.00", new BigDecimal("12.00")),
                Arguments.of("12.5", new BigDecimal("12.5")),
                Arguments.of("0", new BigDecimal("0")),
                Arguments.of("0.00", new BigDecimal("0.00")),
                Arguments.of("-5", new BigDecimal("-5")),
                Arguments.of("-0.50", new BigDecimal("-0.50")),
                Arguments.of("1200", new BigDecimal("1200")));
    }

    @ParameterizedTest
    @MethodSource("priceStringsAndTheAmountTheFlowReceives")
    void importPassesThePriceToTheFlowUnchangedInValueAndScale(String sent, BigDecimal expected) throws Exception {
        when(flow.importFromDistributor(anyString(), any(BigDecimal.class))).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780141439518\",\"price\":\"" + sent + "\"}").andExpect(status().isAccepted());

        ArgumentCaptor<BigDecimal> price = ArgumentCaptor.forClass(BigDecimal.class);
        verify(flow).importFromDistributor(eq("9780141439518"), price.capture());
        assertThat(price.getValue()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "978-0-14-143951-8", " 9780141439518", "9780141439518 ", "0-14-143951-3"})
    void importPassesAnyIsbnStringToTheFlowUnchanged(String isbn) throws Exception {
        when(flow.importFromDistributor(anyString(), isNull())).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"" + isbn + "\"}").andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(flow).importFromDistributor(sent.capture(), isNull());
        assertThat(sent.getValue()).isEqualTo(isbn);
    }

    static Stream<Arguments> priceStringsWithMoreThanTwoDecimalsOrDigits() {
        return Stream.of(
                Arguments.of("12.345", new BigDecimal("12.345")),
                Arguments.of("123456.789", new BigDecimal("123456.789")));
    }

    @ParameterizedTest
    @MethodSource("priceStringsWithMoreThanTwoDecimalsOrDigits")
    void importPassesAPriceWithManyDecimalsAndDigitsToTheFlowUnchanged(String sent, BigDecimal expected)
            throws Exception {
        when(flow.importFromDistributor(anyString(), any(BigDecimal.class))).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780141439518\",\"price\":\"" + sent + "\"}").andExpect(status().isAccepted());

        ArgumentCaptor<BigDecimal> price = ArgumentCaptor.forClass(BigDecimal.class);
        verify(flow).importFromDistributor(eq("9780141439518"), price.capture());
        assertThat(price.getValue()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "978014143951X"})
    void importPassesAnUpperCaseIsbnToTheFlowUnchanged(String isbn) throws Exception {
        when(flow.importFromDistributor(anyString(), isNull())).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"" + isbn + "\"}").andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(flow).importFromDistributor(sent.capture(), isNull());
        assertThat(sent.getValue()).isEqualTo(isbn);
    }

    @Test
    void importLeavesAnIsbnTheDomainRejectsToTheFlowAs422() throws Exception {
        when(flow.importFromDistributor("abc", null))
                .thenThrow(new InvalidInputException("invalid-isbn", "Invalid ISBN", "The ISBN is not valid",
                        List.of(Map.of("field", "isbn", "message", "must be 10 or 13 digits"))));

        ResultActions result = postImport("{\"isbn\":\"abc\"}");

        expectProblem(result, 422, "invalid-isbn", "Invalid ISBN")
                .andExpect(jsonPath("$.details[0].field").value("isbn"));
        verify(flow).importFromDistributor("abc", null);
    }

    @Test
    void importLeavesANegativePriceToTheFlowAs422() throws Exception {
        when(flow.importFromDistributor("9780141439518", new BigDecimal("-5")))
                .thenThrow(new InvalidInputException("price-out-of-range", "Price out of range",
                        "The price is out of range",
                        List.of(Map.of("field", "price", "message", "must be between 0.01 and 500.00"))));

        ResultActions result = postImport("{\"isbn\":\"9780141439518\",\"price\":\"-5\"}");

        expectProblem(result, 422, "price-out-of-range", "Price out of range")
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verify(flow).importFromDistributor("9780141439518", new BigDecimal("-5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "12,50", "1e3", ".5", "12.", "-", " 12.00"})
    void importRejectsANonDecimalPriceWith400NamingOnlyThePrice(String price) throws Exception {
        ResultActions result = postImport("{\"isbn\":\"9780141439518\",\"price\":\"" + price + "\"}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verifyNoInteractions(flow);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+5", "12.00 ", "5 "})
    void importRejectsAPriceWithASignOrTrailingSpaceWith400NamingOnlyThePrice(String price) throws Exception {
        ResultActions result = postImport("{\"isbn\":\"9780141439518\",\"price\":\"" + price + "\"}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verifyNoInteractions(flow);
    }

    @Test
    void importRejectsAnArabicIndicDigitPriceWith400NamingOnlyThePrice() throws Exception {
        String price = Character.toString(0x0661) + Character.toString(0x0662);

        ResultActions result = postImport("{\"isbn\":\"9780141439518\",\"price\":\"" + price + "\"}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verifyNoInteractions(flow);
    }

    @Test
    void importRejectsAMissingIsbnWith400NamingOnlyTheIsbn() throws Exception {
        ResultActions result = postImport("{}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("isbn"));
        verifyNoInteractions(flow);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"isbn\":null}", "{\"isbn\":\"\"}", "{\"isbn\":\"   \"}",
            "{\"isbn\":\"\",\"price\":\"12.00\"}"})
    void importRejectsABlankOrNullIsbnWith400NamingOnlyTheIsbn(String body) throws Exception {
        ResultActions result = postImport(body);

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("isbn"));
        verifyNoInteractions(flow);
    }

    @Test
    void importRejectsABadIsbnAndABadPriceTogetherListingBothFields() throws Exception {
        ResultActions result = postImport("{\"isbn\":\" \",\"price\":\"abc\"}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details[*].field").value(contains("isbn", "price")));
        verifyNoInteractions(flow);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "9780141439518|The distributor has no title with ISBN 9780141439518",
            "9780140449136|The distributor has no title with ISBN 9780140449136"})
    void importMapsATitleUnknownAtTheDistributorTo404(String isbn, String detail) throws Exception {
        when(flow.importFromDistributor(isbn, null))
                .thenThrow(new DistributorAppService.TitleNotFoundAtDistributorException(isbn));

        ResultActions result = postImport("{\"isbn\":\"" + isbn + "\"}");

        expectProblem(result, 404, "title-not-found-at-distributor", "Title not found at distributor")
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "9780141439518|A book with ISBN 9780141439518 is already in the catalogue",
            "9780140449136|A book with ISBN 9780140449136 is already in the catalogue"})
    void importMapsAnIsbnAlreadyInTheCatalogueTo409(String isbn, String detail) throws Exception {
        when(flow.importFromDistributor(isbn, null)).thenThrow(new BookDataService.IsbnAlreadyExistsException(isbn));

        ResultActions result = postImport("{\"isbn\":\"" + isbn + "\"}");

        expectProblem(result, 409, "isbn-already-exists", "ISBN already in catalogue")
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "9780141439518|The title with ISBN 9780141439518 cannot be ordered from the distributor",
            "9780140449136|The title with ISBN 9780140449136 cannot be ordered from the distributor"})
    void importMapsATitleTheDistributorCannotSupplyTo422(String isbn, String detail) throws Exception {
        when(flow.importFromDistributor(isbn, new BigDecimal("12.00")))
                .thenThrow(new DistributorAppService.TitleNotOrderableException(isbn));

        ResultActions result = postImport("{\"isbn\":\"" + isbn + "\",\"price\":\"12.00\"}");

        expectProblem(result, 422, "title-not-orderable", "Title not orderable")
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"The distributor does not answer within 2 seconds",
            "The distributor answered with status 500"})
    void importMapsAnUnavailableDistributorTo503(String detail) throws Exception {
        when(flow.importFromDistributor("9780141439518", null))
                .thenThrow(new DistributorAppService.DistributorUnavailableException(detail));

        ResultActions result = postImport("{\"isbn\":\"9780141439518\"}");

        expectProblem(result, 503, "distributor-unavailable", "Distributor unavailable")
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- import from distributor: how JSON scalars of the wrong type are read (observed, pinned) ---

    @Test
    void importReadsAJsonNumberPriceWithFractionAsItsDecimalText() throws Exception {
        when(flow.importFromDistributor(anyString(), any(BigDecimal.class))).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780141439518\",\"price\":12.5}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a"));

        verify(flow).importFromDistributor("9780141439518", new BigDecimal("12.5"));
    }

    @Test
    void importReadsAJsonIntegerPriceAsItsDigits() throws Exception {
        when(flow.importFromDistributor(anyString(), any(BigDecimal.class))).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":\"9780141439518\",\"price\":12}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a"));

        verify(flow).importFromDistributor("9780141439518", new BigDecimal("12"));
    }

    @Test
    void importReadsAJsonNumberIsbnAsItsDigits() throws Exception {
        when(flow.importFromDistributor(anyString(), isNull())).thenReturn(IMPORTED_ID);

        postImport("{\"isbn\":9780141439518}")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("b7e1c0de-2a4f-4d6b-8c3a-6f5e4d3c2b1a"));

        verify(flow).importFromDistributor("9780141439518", null);
    }

    @Test
    void importReadsABooleanPriceAsTheTextTrueWhichFailsThePricePattern() throws Exception {
        ResultActions result = postImport("{\"isbn\":\"9780141439518\",\"price\":true}");

        expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].field").value("price"));
        verifyNoInteractions(flow);
    }

    @Test
    void importReadsABooleanIsbnAsTheTextTrueAndLeavesItToTheFlow() throws Exception {
        when(flow.importFromDistributor("true", null))
                .thenThrow(new InvalidInputException("invalid-isbn", "Invalid ISBN", "The ISBN is not valid"));

        ResultActions result = postImport("{\"isbn\":true}");

        expectProblem(result, 422, "invalid-isbn", "Invalid ISBN");
        verify(flow).importFromDistributor("true", null);
    }

    @Test
    void importRejectsAnObjectPriceAsAnUnreadableBodyWith400() throws Exception {
        postImport("{\"isbn\":\"9780141439518\",\"price\":{}}")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        verifyNoInteractions(flow);
    }

    // --- unavailable ---

    @Test
    void unavailableMapsTo503WithTheCodeAndDetailsOfTheException() throws Exception {
        doThrow(new UnavailableException("distributor-unavailable", "Distributor unavailable",
                "The distributor does not answer", List.of(Map.of("service", "distributor"))))
                .when(flow).discontinue(any(UUID.class));

        ResultActions result = mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID));

        expectProblem(result, 503, "distributor-unavailable", "Distributor unavailable")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].service").value("distributor"));
    }

    @Test
    void unavailablePassesTheCodeTitleAndMessageOfTheExceptionToTheProblem() throws Exception {
        doThrow(new UnavailableException("sample-unavailable", "Sample unavailable", "The sample is unavailable"))
                .when(flow).discontinue(any(UUID.class));

        mockMvc.perform(post("/books/{id}/commands/discontinue", BOOK_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sample-unavailable"))
                .andExpect(jsonPath("$.title").value("Sample unavailable"))
                .andExpect(jsonPath("$.detail").value("The sample is unavailable"));
    }

    // --- the handler on its own ---

    @Test
    void aNullDefaultMessageRendersAsIsInvalid() throws Exception {
        MethodArgumentNotValidException exception = validationFailure(new FieldError("addBookPayload", "isbn", null));

        ProblemDetail problem = new CommandExceptionHandler().handleValidation(exception);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Validation failed");
        assertThat(problem.getProperties()).containsEntry("code", "validation-failed");
        assertThat(problem.getProperties()).containsEntry("details",
                List.of(Map.of("field", "isbn", "message", "is invalid")));
    }

    @Test
    void validationDetailsAreSortedByFieldThenMessage() throws Exception {
        MethodArgumentNotValidException exception = validationFailure(
                new FieldError("addBookPayload", "title", "must not be blank"),
                new FieldError("addBookPayload", "isbn", "must not be blank"),
                new FieldError("addBookPayload", "price", "must not be blank"),
                new FieldError("addBookPayload", "price", "must match the pattern"));

        ProblemDetail problem = new CommandExceptionHandler().handleValidation(exception);

        assertThat(problem.getProperties()).containsEntry("details", List.of(
                Map.of("field", "isbn", "message", "must not be blank"),
                Map.of("field", "price", "message", "must match the pattern"),
                Map.of("field", "price", "message", "must not be blank"),
                Map.of("field", "title", "message", "must not be blank")));
    }

    private static MethodArgumentNotValidException validationFailure(FieldError... errors) throws Exception {
        Method add = BookCommands.class.getMethod("add", AddBookPayload.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "addBookPayload");
        for (FieldError error : errors) {
            bindingResult.addError(error);
        }
        return new MethodArgumentNotValidException(new MethodParameter(add, 0), bindingResult);
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
