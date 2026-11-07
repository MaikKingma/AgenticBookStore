package nl.openvalue.bookstore.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @ValueSource(strings = {"abc", "12,50", "-5", "1e3", ".5", "12."})
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
