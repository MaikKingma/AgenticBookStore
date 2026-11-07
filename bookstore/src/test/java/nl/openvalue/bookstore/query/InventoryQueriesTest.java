package nl.openvalue.bookstore.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(InventoryQueries.class)
class InventoryQueriesTest {

    private static final UUID BOOK_ID = UUID.fromString("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10");

    private static final UUID OTHER_ID = UUID.fromString("9c1d7e52-6a3b-4f08-8d2c-5e4b1a7f3c66");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryFlow flow;

    // --- get one ---

    @Test
    void getReturnsTheStockViewWithTheDerivedFigures() throws Exception {
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 7, 2, 3, 10, true));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.bookId").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))
                .andExpect(jsonPath("$.onHand").value(7))
                .andExpect(jsonPath("$.reserved").value(2))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.reorderLevel").value(3))
                .andExpect(jsonPath("$.lowStock").value(false))
                .andExpect(jsonPath("$.reorderQuantity").doesNotExist())
                .andExpect(jsonPath("$.lowStockSignalled").doesNotExist())
                .andExpect(jsonPath("$.*", hasSize(6)));

        verify(flow).findByBookId(BOOK_ID);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void getJudgesLowStockAgainstTheReorderLevelOfTheStockItem() throws Exception {
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 7, 2, 5, 12, false));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(7))
                .andExpect(jsonPath("$.reserved").value(2))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.reorderLevel").value(5))
                .andExpect(jsonPath("$.lowStock").value(true))
                .andExpect(jsonPath("$.*", hasSize(6)));
    }

    @Test
    void getShowsLowStockAtExactlyTheReorderLevel() throws Exception {
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 5, 2, 3, 10, false));
        when(flow.findByBookId(OTHER_ID)).thenReturn(new StockItemDTO(OTHER_ID, 6, 2, 3, 10, false));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(3))
                .andExpect(jsonPath("$.lowStock").value(true));
        mockMvc.perform(get("/inventory/{bookId}", OTHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(4))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void getMapsNotFoundTo404BookNotFound() throws Exception {
        when(flow.findByBookId(BOOK_ID)).thenThrow(new StockItemDataService.StockItemNotFoundException(BOOK_ID));

        ResultActions result = mockMvc.perform(get("/inventory/{bookId}", BOOK_ID));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Book not found"))
                .andExpect(jsonPath("$.code").value("book-not-found"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- low stock ---

    /**
     * The flow returns four items in an order that is sorted in no column, in neither direction: book id, on hand,
     * reserved, available, reorder level and low stock all go up and down. One item sits exactly on its reorder level
     * and one is above its level, so an added re-filter or an added sort in the adapter fails here.
     */
    @Test
    void lowStockReturnsTheItemsInTheOrderOfTheFlow() throws Exception {
        when(flow.findLowStock()).thenReturn(List.of(
                new StockItemDTO(UUID.fromString("5a1f0c3e-7b2d-4e96-8a14-c0d3e5f60718"), 4, 3, 3, 10, true),
                new StockItemDTO(UUID.fromString("1b2e7d94-3c50-4a8f-9e61-d4f7a2b8c935"), 9, 0, 8, 6, false),
                new StockItemDTO(UUID.fromString("9c4a6f10-e8d3-4b27-a5c9-1f0e3d7b4a62"), 2, 2, 4, 10, false),
                new StockItemDTO(UUID.fromString("3d8b5e27-0f1a-4c63-b7d2-8e9a4c1f5b30"), 7, 1, 6, 15, true)));

        mockMvc.perform(get("/inventory").param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].bookId").value("5a1f0c3e-7b2d-4e96-8a14-c0d3e5f60718"))
                .andExpect(jsonPath("$[0].onHand").value(4))
                .andExpect(jsonPath("$[0].reserved").value(3))
                .andExpect(jsonPath("$[0].available").value(1))
                .andExpect(jsonPath("$[0].reorderLevel").value(3))
                .andExpect(jsonPath("$[0].lowStock").value(true))
                .andExpect(jsonPath("$[0].*", hasSize(6)))
                .andExpect(jsonPath("$[1].bookId").value("1b2e7d94-3c50-4a8f-9e61-d4f7a2b8c935"))
                .andExpect(jsonPath("$[1].onHand").value(9))
                .andExpect(jsonPath("$[1].reserved").value(0))
                .andExpect(jsonPath("$[1].available").value(9))
                .andExpect(jsonPath("$[1].reorderLevel").value(8))
                .andExpect(jsonPath("$[1].lowStock").value(false))
                .andExpect(jsonPath("$[1].*", hasSize(6)))
                .andExpect(jsonPath("$[2].bookId").value("9c4a6f10-e8d3-4b27-a5c9-1f0e3d7b4a62"))
                .andExpect(jsonPath("$[2].onHand").value(2))
                .andExpect(jsonPath("$[2].reserved").value(2))
                .andExpect(jsonPath("$[2].available").value(0))
                .andExpect(jsonPath("$[2].reorderLevel").value(4))
                .andExpect(jsonPath("$[2].lowStock").value(true))
                .andExpect(jsonPath("$[2].*", hasSize(6)))
                .andExpect(jsonPath("$[3].bookId").value("3d8b5e27-0f1a-4c63-b7d2-8e9a4c1f5b30"))
                .andExpect(jsonPath("$[3].onHand").value(7))
                .andExpect(jsonPath("$[3].reserved").value(1))
                .andExpect(jsonPath("$[3].available").value(6))
                .andExpect(jsonPath("$[3].reorderLevel").value(6))
                .andExpect(jsonPath("$[3].lowStock").value(true))
                .andExpect(jsonPath("$[3].*", hasSize(6)));

        verify(flow).findLowStock();
        verifyNoMoreInteractions(flow);
    }

    @Test
    void lowStockReturnsAnEmptyArray() throws Exception {
        when(flow.findLowStock()).thenReturn(List.of());

        mockMvc.perform(get("/inventory").param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(flow).findLowStock();
    }

    // --- the lowStock filter ---

    @Test
    void theInvalidQueryParameterHandlerAnswers400WithTheParameterAndTheMessageOfTheException() {
        ProblemDetail problem = new QueryExceptionHandler().handleInvalidQueryParameter(
                new InventoryQueries.InvalidQueryParameterException("sample", "sample message"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Validation failed");
        assertThat(problem.getDetail()).isNotBlank();
        assertThat(problem.getProperties()).containsEntry("code", "validation-failed");
        assertThat(problem.getProperties()).containsEntry("details",
                List.of(Map.of("field", "sample", "message", "sample message")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/inventory", "/inventory?lowStock=", "/inventory?lowStock=false",
            "/inventory?lowStock=TRUE", "/inventory?lowStock=1", "/inventory?lowStock=maybe",
            "/inventory?lowStock=true&lowStock=false"})
    void lowStockRejectsEveryValueOtherThanTrueWith400(String uri) throws Exception {
        ResultActions result = mockMvc.perform(get(uri));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.code").value("validation-failed"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("lowStock"))
                .andExpect(jsonPath("$.details[0].message").value("must be true"))
                .andExpect(jsonPath("$.details[0].*", hasSize(2)));
        verifyNoInteractions(flow);
    }
}
