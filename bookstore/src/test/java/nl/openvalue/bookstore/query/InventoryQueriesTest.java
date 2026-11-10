package nl.openvalue.bookstore.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;
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
                .andExpect(jsonPath("$.*", hasSize(7)));

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
                .andExpect(jsonPath("$.*", hasSize(7)));
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
    void getShowsNoPendingRestockAsAPresentKeyWithANullValue() throws Exception {
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 7, 2, 3, 10, true));

        // doesNotExist() would also pass for an absent key, so the key is asserted as present.
        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("pendingRestock")))
                .andExpect(jsonPath("$.pendingRestock").value(nullValue()))
                .andExpect(jsonPath("$.*", hasSize(7)));
    }

    @Test
    void getShowsABackorderedPendingRestockWithExactlyFourKeys() throws Exception {
        // Quantity 11 differs from the reorder quantity 10; the date has a single-digit month and day.
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 4, 2, 3, 10, true,
                new PendingRestockDTO("0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7", 11, LocalDate.of(2026, 7, 3), true)));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value("3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10"))
                .andExpect(jsonPath("$.onHand").value(4))
                .andExpect(jsonPath("$.available").value(2))
                .andExpect(jsonPath("$.lowStock").value(true))
                .andExpect(jsonPath("$.pendingRestock.restockOrderId").value("0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7"))
                .andExpect(jsonPath("$.pendingRestock.quantity").value(11))
                .andExpect(jsonPath("$.pendingRestock.expectedDeliveryDate").value("2026-07-03"))
                .andExpect(jsonPath("$.pendingRestock.backordered").value(true))
                .andExpect(jsonPath("$.pendingRestock.*", hasSize(4)))
                .andExpect(jsonPath("$.*", hasSize(7)));
    }

    @Test
    void getShowsAPendingRestockThatIsNotBackorderedWithItsOwnFigures() throws Exception {
        // The second witness: another id, another quantity, a two-digit month and day, backordered false.
        when(flow.findByBookId(OTHER_ID)).thenReturn(new StockItemDTO(OTHER_ID, 1, 0, 3, 12, false,
                new PendingRestockDTO("a4d19b60-72c8-4e05-b3f1-96e8d0c2a7f4", 4, LocalDate.of(2026, 12, 19), false)));

        mockMvc.perform(get("/inventory/{bookId}", OTHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingRestock.restockOrderId").value("a4d19b60-72c8-4e05-b3f1-96e8d0c2a7f4"))
                .andExpect(jsonPath("$.pendingRestock.quantity").value(4))
                .andExpect(jsonPath("$.pendingRestock.expectedDeliveryDate").value("2026-12-19"))
                .andExpect(jsonPath("$.pendingRestock.backordered").value(false))
                .andExpect(jsonPath("$.pendingRestock.*", hasSize(4)))
                .andExpect(jsonPath("$.*", hasSize(7)));
    }

    @Test
    void getShowsAPendingRestockOnAnItemThatIsNotLow() throws Exception {
        // Available 17 is above the reorder level 3, so lowStock is false and the pending restock is still shown.
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 20, 3, 3, 10, false,
                new PendingRestockDTO("e19b7a3c-0d54-4c82-9f6a-71b2c8d0e5f3", 9, LocalDate.of(2026, 8, 14), true)));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(17))
                .andExpect(jsonPath("$.lowStock").value(false))
                .andExpect(jsonPath("$.pendingRestock.restockOrderId").value("e19b7a3c-0d54-4c82-9f6a-71b2c8d0e5f3"))
                .andExpect(jsonPath("$.pendingRestock.quantity").value(9))
                .andExpect(jsonPath("$.pendingRestock.expectedDeliveryDate").value("2026-08-14"))
                .andExpect(jsonPath("$.pendingRestock.backordered").value(true))
                .andExpect(jsonPath("$.pendingRestock.*", hasSize(4)));
    }

    @Test
    void getShowsAPendingRestockOnAnItemAtExactlyTheReorderLevel() throws Exception {
        // Available 3 equals the reorder level 3, so lowStock is true and the pending restock is shown.
        when(flow.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 5, 2, 3, 10, true,
                new PendingRestockDTO("b6a0d4f2-83c1-4e57-a92d-5c7e1f3b8a40", 10, LocalDate.of(2026, 9, 21), false)));

        mockMvc.perform(get("/inventory/{bookId}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(3))
                .andExpect(jsonPath("$.lowStock").value(true))
                .andExpect(jsonPath("$.pendingRestock.restockOrderId").value("b6a0d4f2-83c1-4e57-a92d-5c7e1f3b8a40"))
                .andExpect(jsonPath("$.pendingRestock.quantity").value(10))
                .andExpect(jsonPath("$.pendingRestock.expectedDeliveryDate").value("2026-09-21"))
                .andExpect(jsonPath("$.pendingRestock.backordered").value(false))
                .andExpect(jsonPath("$.pendingRestock.*", hasSize(4)));
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
                .andExpect(jsonPath("$[0].*", hasSize(7)))
                .andExpect(jsonPath("$[1].bookId").value("1b2e7d94-3c50-4a8f-9e61-d4f7a2b8c935"))
                .andExpect(jsonPath("$[1].onHand").value(9))
                .andExpect(jsonPath("$[1].reserved").value(0))
                .andExpect(jsonPath("$[1].available").value(9))
                .andExpect(jsonPath("$[1].reorderLevel").value(8))
                .andExpect(jsonPath("$[1].lowStock").value(false))
                .andExpect(jsonPath("$[1].*", hasSize(7)))
                .andExpect(jsonPath("$[2].bookId").value("9c4a6f10-e8d3-4b27-a5c9-1f0e3d7b4a62"))
                .andExpect(jsonPath("$[2].onHand").value(2))
                .andExpect(jsonPath("$[2].reserved").value(2))
                .andExpect(jsonPath("$[2].available").value(0))
                .andExpect(jsonPath("$[2].reorderLevel").value(4))
                .andExpect(jsonPath("$[2].lowStock").value(true))
                .andExpect(jsonPath("$[2].*", hasSize(7)))
                .andExpect(jsonPath("$[3].bookId").value("3d8b5e27-0f1a-4c63-b7d2-8e9a4c1f5b30"))
                .andExpect(jsonPath("$[3].onHand").value(7))
                .andExpect(jsonPath("$[3].reserved").value(1))
                .andExpect(jsonPath("$[3].available").value(6))
                .andExpect(jsonPath("$[3].reorderLevel").value(6))
                .andExpect(jsonPath("$[3].lowStock").value(true))
                .andExpect(jsonPath("$[3].*", hasSize(7)));

        verify(flow).findLowStock();
        verifyNoMoreInteractions(flow);
    }

    /**
     * Three items, a pending restock only on the middle one: a mapping that takes the first or the last element for
     * every item, or shows the pending restock of the wrong item, fails here. Each item shows null or its own figures.
     */
    @Test
    void lowStockShowsThePendingRestockOfTheMiddleItemOnly() throws Exception {
        when(flow.findLowStock()).thenReturn(List.of(
                new StockItemDTO(UUID.fromString("5a1f0c3e-7b2d-4e96-8a14-c0d3e5f60718"), 1, 0, 3, 10, true),
                new StockItemDTO(UUID.fromString("1b2e7d94-3c50-4a8f-9e61-d4f7a2b8c935"), 2, 0, 3, 6, true,
                        new PendingRestockDTO("c7e5a2d0-91b4-4f38-8a6d-3e0f1b9c4d27", 6, LocalDate.of(2026, 7, 3),
                                true)),
                new StockItemDTO(UUID.fromString("9c4a6f10-e8d3-4b27-a5c9-1f0e3d7b4a62"), 3, 0, 3, 15, true)));

        mockMvc.perform(get("/inventory").param("lowStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].bookId").value("5a1f0c3e-7b2d-4e96-8a14-c0d3e5f60718"))
                .andExpect(jsonPath("$[0]", hasKey("pendingRestock")))
                .andExpect(jsonPath("$[0].pendingRestock").value(nullValue()))
                .andExpect(jsonPath("$[0].*", hasSize(7)))
                .andExpect(jsonPath("$[1].bookId").value("1b2e7d94-3c50-4a8f-9e61-d4f7a2b8c935"))
                .andExpect(jsonPath("$[1].pendingRestock.restockOrderId")
                        .value("c7e5a2d0-91b4-4f38-8a6d-3e0f1b9c4d27"))
                .andExpect(jsonPath("$[1].pendingRestock.quantity").value(6))
                .andExpect(jsonPath("$[1].pendingRestock.expectedDeliveryDate").value("2026-07-03"))
                .andExpect(jsonPath("$[1].pendingRestock.backordered").value(true))
                .andExpect(jsonPath("$[1].pendingRestock.*", hasSize(4)))
                .andExpect(jsonPath("$[1].*", hasSize(7)))
                .andExpect(jsonPath("$[2].bookId").value("9c4a6f10-e8d3-4b27-a5c9-1f0e3d7b4a62"))
                .andExpect(jsonPath("$[2]", hasKey("pendingRestock")))
                .andExpect(jsonPath("$[2].pendingRestock").value(nullValue()))
                .andExpect(jsonPath("$[2].*", hasSize(7)));
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
