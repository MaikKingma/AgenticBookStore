package nl.openvalue.bookstore.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(InventoryCommands.class)
class InventoryCommandsTest {

    private static final UUID BOOK_ID = UUID.fromString("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryFlow flow;

    // --- receive ---

    @Test
    void receiveReturns202WithoutBodyAndPassesTheQuantity() throws Exception {
        postReceive("{\"quantity\":25}")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).receive(BOOK_ID, 25);
    }

    @Test
    void receiveRejectsAQuantityOf0With400() throws Exception {
        ResultActions result = postReceive("{\"quantity\":0}");

        expectValidationFailed(result, "quantity");
        verifyNoInteractions(flow);
    }

    @Test
    void receiveRejectsAQuantityOf1001With400() throws Exception {
        ResultActions result = postReceive("{\"quantity\":1001}");

        expectValidationFailed(result, "quantity");
        verifyNoInteractions(flow);
    }

    @Test
    void receiveRejectsAMissingQuantityWith400() throws Exception {
        ResultActions result = postReceive("{}");

        expectValidationFailed(result, "quantity");
        verifyNoInteractions(flow);
    }

    @Test
    void receiveAcceptsTheBounds1And1000() throws Exception {
        postReceive("{\"quantity\":1}")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        postReceive("{\"quantity\":1000}")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).receive(BOOK_ID, 1);
        verify(flow).receive(BOOK_ID, 1000);
    }

    @Test
    void receiveMapsNotFoundTo404BookNotFound() throws Exception {
        doThrow(new StockItemDataService.StockItemNotFoundException(BOOK_ID)).when(flow).receive(BOOK_ID, 25);

        ResultActions result = postReceive("{\"quantity\":25}");

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- correct ---

    @Test
    void correctReturns202AndPassesOnHandAndReason() throws Exception {
        postCorrect("{\"onHand\":12,\"reason\":\"Annual stocktake\"}")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).correct(BOOK_ID, 12, "Annual stocktake");
    }

    @Test
    void correctRejectsMissingFieldsWith400ListingBoth() throws Exception {
        ResultActions result = postCorrect("{}");

        expectValidationFailed(result, "onHand", "reason");
        verifyNoInteractions(flow);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 10001})
    void correctRejectsAnOnHandOfMinus1And10001With400(int onHand) throws Exception {
        ResultActions result = postCorrect(correctBody(onHand, "abc"));

        expectValidationFailed(result, "onHand");
        verifyNoInteractions(flow);
    }

    @Test
    void correctAcceptsAnOnHandOf0And10000() throws Exception {
        String shortReason = "abc";
        String longReason = "d".repeat(200);

        postCorrect(correctBody(0, shortReason))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        postCorrect(correctBody(10000, longReason))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).correct(BOOK_ID, 0, shortReason);
        verify(flow).correct(BOOK_ID, 10000, longReason);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 201})
    void correctRejectsReasonsOf2And201CharactersWith400(int length) throws Exception {
        ResultActions result = postCorrect(correctBody(5, "r".repeat(length)));

        expectValidationFailed(result, "reason");
        verifyNoInteractions(flow);
    }

    @Test
    void correctRejectsABlankReasonWith400() throws Exception {
        ResultActions result = postCorrect(correctBody(5, "   "));

        expectValidationFailed(result, "reason");
        verifyNoInteractions(flow);
    }

    @Test
    void correctAcceptsReasonsOf3And200Characters() throws Exception {
        String threeCharacters = "abc";
        String twoHundredCharacters = "z".repeat(200);

        postCorrect(correctBody(5, threeCharacters))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        postCorrect(correctBody(5, twoHundredCharacters))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).correct(BOOK_ID, 5, threeCharacters);
        verify(flow).correct(BOOK_ID, 5, twoHundredCharacters);
    }

    @Test
    void correctMapsConflictTo409OnHandBelowReserved() throws Exception {
        doThrow(new ConflictException("on-hand-below-reserved", "On hand below reserved",
                "The on hand quantity 1 is below the reserved quantity 2"))
                .when(flow).correct(BOOK_ID, 1, "Recount");

        ResultActions result = postCorrect(correctBody(1, "Recount"));

        expectProblem(result, 409, "on-hand-below-reserved", "On hand below reserved")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void correctMapsNotFoundTo404BookNotFound() throws Exception {
        doThrow(new StockItemDataService.StockItemNotFoundException(BOOK_ID))
                .when(flow).correct(BOOK_ID, 12, "Recount");

        ResultActions result = postCorrect(correctBody(12, "Recount"));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- request restock ---

    @Test
    void requestRestockReturns202WithoutBodyAndPassesTheBookId() throws Exception {
        postRequestRestock()
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).requestRestock(BOOK_ID);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void requestRestockNeedsNeitherABodyNorAContentType() throws Exception {
        // No Content-Type and no body at all: a required request body would answer 415 or 400 here.
        MvcResult result = mockMvc.perform(post("/inventory/{bookId}/commands/request-restock", BOOK_ID))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""))
                .andReturn();

        assertThat(result.getRequest().getContentType()).isNull();
        verify(flow).requestRestock(BOOK_ID);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void requestRestockMapsADistributorOutageTo503DistributorUnavailable() throws Exception {
        doThrow(new DistributorAppService.DistributorUnavailableException("The distributor answered status 503"))
                .when(flow).requestRestock(BOOK_ID);

        ResultActions result = postRequestRestock();

        expectProblem(result, 503, "distributor-unavailable", "Distributor unavailable")
                .andExpect(jsonPath("$.detail").value("The distributor answered status 503"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void requestRestockMapsAnUnknownTitleTo404TitleNotFoundAtDistributor() throws Exception {
        doThrow(new DistributorAppService.TitleNotFoundAtDistributorException("9780141439518"))
                .when(flow).requestRestock(BOOK_ID);

        ResultActions result = postRequestRestock();

        expectProblem(result, 404, "title-not-found-at-distributor", "Title not found at distributor")
                .andExpect(jsonPath("$.detail").value("The distributor has no title with ISBN 9780141439518"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void requestRestockMapsNotFoundTo404BookNotFound() throws Exception {
        doThrow(new StockItemDataService.StockItemNotFoundException(BOOK_ID)).when(flow).requestRestock(BOOK_ID);

        ResultActions result = postRequestRestock();

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void requestRestockMapsARefusedTitleTo422TitleNotOrderable() throws Exception {
        doThrow(new DistributorAppService.TitleNotOrderableException("9780141439518"))
                .when(flow).requestRestock(BOOK_ID);

        ResultActions result = postRequestRestock();

        expectProblem(result, 422, "title-not-orderable", "Title not orderable")
                .andExpect(jsonPath("$.detail")
                        .value("The title with ISBN 9780141439518 cannot be ordered from the distributor"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void requestRestockNeverCallsTheEventPathUseCase() throws Exception {
        postRequestRestock().andExpect(status().isAccepted());

        verify(flow, never()).handleStockRunningLow(BOOK_ID);
    }

    // --- helpers ---

    private ResultActions postRequestRestock() throws Exception {
        return mockMvc.perform(post("/inventory/{bookId}/commands/request-restock", BOOK_ID));
    }

    private ResultActions postReceive(String body) throws Exception {
        return mockMvc.perform(post("/inventory/{bookId}/commands/receive", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postCorrect(String body) throws Exception {
        return mockMvc.perform(post("/inventory/{bookId}/commands/correct", BOOK_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String correctBody(int onHand, String reason) {
        return "{\"onHand\":%d,\"reason\":\"%s\"}".formatted(onHand, reason);
    }

    private static ResultActions expectValidationFailed(ResultActions result, String... fields) throws Exception {
        return expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(fields.length))
                .andExpect(jsonPath("$.details[*].field").value(contains(fields)))
                .andExpect(jsonPath("$.details[*].message").value(everyItem(not(emptyOrNullString()))));
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
