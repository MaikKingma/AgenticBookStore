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

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;
import nl.openvalue.bookstore.domaininteraction.OrderDataService;
import nl.openvalue.bookstore.domaininteraction.OrderFlow;
import nl.openvalue.bookstore.domaininteraction.OrderLineDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(OrderQueries.class)
class OrderQueriesTest {

    private static final UUID ORDER_ID = UUID.fromString("7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92");

    private static final UUID SOME_BOOK = UUID.fromString("1c8e5a37-9b20-4d64-a3f1-6e0d7b2c4a85");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderFlow flow;

    private static OrderLineDTO line(UUID bookId, String isbn, String title, String unitPrice, String currency,
            int quantity) {
        return new OrderLineDTO(bookId, isbn, title, new BigDecimal(unitPrice), currency, quantity);
    }

    private static OrderDTO order(UUID id, String customerEmail, String status, String placedAt, String shippedAt,
            OrderLineDTO... lines) {
        return new OrderDTO(id, customerEmail, status, Instant.parse(placedAt),
                shippedAt == null ? null : Instant.parse(shippedAt), List.of(lines));
    }

    private static Map<String, Object> jsonObject(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$");
    }

    private static List<Map<String, Object>> jsonArray(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$");
    }

    // --- get one ---

    @Test
    void getReturnsTheOrderViewWithExactlyItsFields() throws Exception {
        when(flow.findById(ORDER_ID)).thenReturn(order(ORDER_ID, "reader@example.com", "PLACED",
                "2026-06-15T10:00:00.123456Z", null,
                line(SOME_BOOK, "9780140449136", "The Odyssey", "12.50", "EUR", 2),
                line(SOME_BOOK, "9780143105428", "Walden", "8.00", "EUR", 1)));

        ResultActions result = mockMvc.perform(get("/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.*", hasSize(8)));

        Map<String, Object> view = jsonObject(result);
        assertThat(view.keySet()).containsExactlyInAnyOrder("id", "customerEmail", "status", "placedAt", "shippedAt",
                "lines", "total", "currency");
        assertThat(view.get("id")).isEqualTo("7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92");
        assertThat(view.get("customerEmail")).isEqualTo("reader@example.com");
        assertThat(view.get("status")).isEqualTo("PLACED");
        assertThat(view.get("placedAt")).isEqualTo("2026-06-15T10:00:00.123456Z");
        assertThat(view).containsKey("shippedAt");
        assertThat(view.get("shippedAt")).isNull();
        assertThat(view.get("total")).isEqualTo("33.00");
        assertThat(view.get("currency")).isEqualTo("EUR");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) view.get("lines");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).keySet()).containsExactlyInAnyOrder("isbn", "title", "quantity", "unitPrice",
                "lineTotal");
        assertThat(lines.get(0).get("isbn")).isEqualTo("9780140449136");
        assertThat(lines.get(0).get("title")).isEqualTo("The Odyssey");
        assertThat(lines.get(0).get("quantity")).isEqualTo(2);
        assertThat(lines.get(0).get("unitPrice")).isEqualTo("12.50");
        assertThat(lines.get(0).get("lineTotal")).isEqualTo("25.00");
        assertThat(lines.get(1).keySet()).containsExactlyInAnyOrder("isbn", "title", "quantity", "unitPrice",
                "lineTotal");
        assertThat(lines.get(1).get("isbn")).isEqualTo("9780143105428");
        assertThat(lines.get(1).get("title")).isEqualTo("Walden");
        assertThat(lines.get(1).get("quantity")).isEqualTo(1);
        assertThat(lines.get(1).get("unitPrice")).isEqualTo("8.00");
        assertThat(lines.get(1).get("lineTotal")).isEqualTo("8.00");

        verify(flow).findById(ORDER_ID);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void getRendersTheShippedTimeOfAShippedOrder() throws Exception {
        UUID id = UUID.fromString("1e5f8a42-6c3b-4d97-b0a8-2f4e9c1d7a63");
        when(flow.findById(id)).thenReturn(order(id, "bookworm@shop.example", "SHIPPED",
                "2026-03-01T08:30:00.000001Z", "2026-03-02T17:45:10.654321Z",
                line(SOME_BOOK, "9781400079988", "Ulysses", "21.40", "EUR", 3)));

        ResultActions result = mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk());

        Map<String, Object> view = jsonObject(result);
        assertThat(view.get("id")).isEqualTo("1e5f8a42-6c3b-4d97-b0a8-2f4e9c1d7a63");
        assertThat(view.get("customerEmail")).isEqualTo("bookworm@shop.example");
        assertThat(view.get("status")).isEqualTo("SHIPPED");
        assertThat(view.get("placedAt")).isEqualTo("2026-03-01T08:30:00.000001Z");
        assertThat(view.get("shippedAt")).isEqualTo("2026-03-02T17:45:10.654321Z");
        assertThat(view.get("total")).isEqualTo("64.20");
        assertThat(view.get("currency")).isEqualTo("EUR");
    }

    /**
     * Three lines that are sorted in no column in either direction, in the order the flow returns them: book id
     * (3d, 0f, 6a), isbn (518, 846, 679), title (Middlemarch, Emma, Persuasion), quantity (3, 1, 2), unit price
     * (16.20, 7.25, 19.90) and line total (48.60, 7.25, 39.80) all go up and down. Book id and currency are not in
     * the view but the adapter holds them, so they are varied too.
     */
    @Test
    void getKeepsTheLinesInOrder() throws Exception {
        UUID id = UUID.fromString("5b2c7e19-4a86-4f03-9d1e-8c6a0b3f5d27");
        when(flow.findById(id)).thenReturn(order(id, "quill@example.org", "PLACED", "2026-01-09T23:59:59.999999Z",
                null,
                line(UUID.fromString("3d8b5e27-0f1a-4c63-b7d2-8e9a4c1f5b30"), "9780141439518", "Middlemarch",
                        "16.20", "GBP", 3),
                line(UUID.fromString("0f2a9c61-8b3d-4e75-a1c4-6d7e0b2f9a53"), "9780141439846", "Emma", "7.25",
                        "GBP", 1),
                line(UUID.fromString("6a4e1d85-c2f9-4b30-9d17-3e8a5c0b7f24"), "9780141439679", "Persuasion",
                        "19.90", "GBP", 2)));

        ResultActions result = mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk());

        Map<String, Object> view = jsonObject(result);
        assertThat(view.get("customerEmail")).isEqualTo("quill@example.org");
        assertThat(view.get("total")).isEqualTo("95.65");
        assertThat(view.get("currency")).isEqualTo("GBP");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) view.get("lines");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).get("isbn")).isEqualTo("9780141439518");
        assertThat(lines.get(0).get("title")).isEqualTo("Middlemarch");
        assertThat(lines.get(0).get("quantity")).isEqualTo(3);
        assertThat(lines.get(0).get("unitPrice")).isEqualTo("16.20");
        assertThat(lines.get(0).get("lineTotal")).isEqualTo("48.60");
        assertThat(lines.get(1).get("isbn")).isEqualTo("9780141439846");
        assertThat(lines.get(1).get("title")).isEqualTo("Emma");
        assertThat(lines.get(1).get("quantity")).isEqualTo(1);
        assertThat(lines.get(1).get("unitPrice")).isEqualTo("7.25");
        assertThat(lines.get(1).get("lineTotal")).isEqualTo("7.25");
        assertThat(lines.get(2).get("isbn")).isEqualTo("9780141439679");
        assertThat(lines.get(2).get("title")).isEqualTo("Persuasion");
        assertThat(lines.get(2).get("quantity")).isEqualTo(2);
        assertThat(lines.get(2).get("unitPrice")).isEqualTo("19.90");
        assertThat(lines.get(2).get("lineTotal")).isEqualTo("39.80");
    }

    @Test
    void getTakesTheCurrencyFromTheOrderLinesAndNotFromAConstant() throws Exception {
        UUID id = UUID.fromString("2f6a0d93-1b5c-4e78-a4d2-9c3e7b1f0a58");
        when(flow.findById(id)).thenReturn(order(id, "j.doe@example.net", "PLACED", "2025-12-24T00:00:00Z", null,
                line(SOME_BOOK, "9780679783268", "Dune", "5.50", "USD", 4)));

        ResultActions result = mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk());

        Map<String, Object> view = jsonObject(result);
        assertThat(view.get("currency")).isEqualTo("USD");
        assertThat(view.get("total")).isEqualTo("22.00");
        assertThat(view.get("placedAt")).isEqualTo("2025-12-24T00:00:00Z");
    }

    @Test
    void getRendersMoneyAsPlainStrings() throws Exception {
        UUID id = UUID.fromString("0a7c3e59-8d14-4b62-9f05-4e1b6d2a8c37");
        when(flow.findById(id)).thenReturn(order(id, "plain@example.io", "PLACED", "2026-02-02T02:02:02.000002Z",
                null, line(SOME_BOOK, "9780486284736", "Candide", "1E+2", "EUR", 1)));

        ResultActions result = mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk());

        Map<String, Object> view = jsonObject(result);
        assertThat(view.get("total")).isEqualTo("100.00");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) view.get("lines");
        assertThat(lines.get(0).get("unitPrice")).isEqualTo("100");
        assertThat(lines.get(0).get("lineTotal")).isEqualTo("100.00");
    }

    @Test
    void getMapsOrderNotFoundTo404() throws Exception {
        when(flow.findById(ORDER_ID)).thenThrow(new OrderDataService.OrderNotFoundException(ORDER_ID));

        ResultActions result = mockMvc.perform(get("/orders/{id}", ORDER_ID));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.code").value("order-not-found"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- by customer ---

    /**
     * Four summaries that are sorted in no column in either direction, in the order of the flow: id (4e, 0b, 6c, 2a),
     * status (PLACED, SHIPPED, PLACED, SHIPPED), placed time (06-02, 04-18, 05-27, 06-20), item count (3, 1, 4, 2),
     * total (41.00, 9.99, 120.50, 15.75) and, held by the adapter but not shown, the e-mail address (mia, zoe, bob,
     * eva). Both statuses are present.
     */
    @Test
    void byCustomerReturnsTheSummariesInTheOrderOfTheFlow() throws Exception {
        when(flow.findByCustomer("mia@example.nl")).thenReturn(List.of(
                order(UUID.fromString("4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47"), "mia@example.nl", "PLACED",
                        "2026-06-02T11:15:00.250001Z", null,
                        line(SOME_BOOK, "9780141439518", "Emma", "12.50", "EUR", 2),
                        line(SOME_BOOK, "9780141439846", "Persuasion", "16.00", "EUR", 1)),
                order(UUID.fromString("0b9d3f58-4a62-4c17-8e05-d2a7f1c93b68"), "zoe@example.nl", "SHIPPED",
                        "2026-04-18T08:05:30.000009Z", "2026-04-19T12:00:00.000008Z",
                        line(SOME_BOOK, "9780141439679", "Dracula", "9.99", "EUR", 1)),
                order(UUID.fromString("6c15e8a3-b074-4d29-9f3e-5a1c8d2e7b04"), "bob@example.nl", "PLACED",
                        "2026-05-27T19:40:12.123456Z", null,
                        line(SOME_BOOK, "9780141439518", "Emma", "30.00", "EUR", 3),
                        line(SOME_BOOK, "9780141439846", "Persuasion", "30.50", "EUR", 1)),
                order(UUID.fromString("2a60b4d7-e19c-4f83-b5a2-7c4d0e8f1a96"), "eva@example.nl", "SHIPPED",
                        "2026-06-20T07:30:45.000300Z", "2026-06-21T09:00:00.000100Z",
                        line(SOME_BOOK, "9780141439518", "Emma", "5.25", "EUR", 1),
                        line(SOME_BOOK, "9780141439846", "Persuasion", "10.50", "EUR", 1))));

        ResultActions result = mockMvc.perform(get("/orders").param("customerEmail", "mia@example.nl"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(4)));

        List<Map<String, Object>> summaries = jsonArray(result);
        for (Map<String, Object> summary : summaries) {
            assertThat(summary.keySet()).containsExactlyInAnyOrder("id", "status", "placedAt", "itemCount",
                    "total");
        }
        assertThat(summaries.get(0).get("id")).isEqualTo("4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47");
        assertThat(summaries.get(0).get("status")).isEqualTo("PLACED");
        assertThat(summaries.get(0).get("placedAt")).isEqualTo("2026-06-02T11:15:00.250001Z");
        assertThat(summaries.get(0).get("itemCount")).isEqualTo(3);
        assertThat(summaries.get(0).get("total")).isEqualTo("41.00");
        assertThat(summaries.get(1).get("id")).isEqualTo("0b9d3f58-4a62-4c17-8e05-d2a7f1c93b68");
        assertThat(summaries.get(1).get("status")).isEqualTo("SHIPPED");
        assertThat(summaries.get(1).get("placedAt")).isEqualTo("2026-04-18T08:05:30.000009Z");
        assertThat(summaries.get(1).get("itemCount")).isEqualTo(1);
        assertThat(summaries.get(1).get("total")).isEqualTo("9.99");
        assertThat(summaries.get(2).get("id")).isEqualTo("6c15e8a3-b074-4d29-9f3e-5a1c8d2e7b04");
        assertThat(summaries.get(2).get("status")).isEqualTo("PLACED");
        assertThat(summaries.get(2).get("placedAt")).isEqualTo("2026-05-27T19:40:12.123456Z");
        assertThat(summaries.get(2).get("itemCount")).isEqualTo(4);
        assertThat(summaries.get(2).get("total")).isEqualTo("120.50");
        assertThat(summaries.get(3).get("id")).isEqualTo("2a60b4d7-e19c-4f83-b5a2-7c4d0e8f1a96");
        assertThat(summaries.get(3).get("status")).isEqualTo("SHIPPED");
        assertThat(summaries.get(3).get("placedAt")).isEqualTo("2026-06-20T07:30:45.000300Z");
        assertThat(summaries.get(3).get("itemCount")).isEqualTo(2);
        assertThat(summaries.get(3).get("total")).isEqualTo("15.75");

        verify(flow).findByCustomer("mia@example.nl");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void summaryItemCountIsTheSumOfTheLineQuantities() throws Exception {
        when(flow.findByCustomer("sum@example.org")).thenReturn(List.of(
                order(UUID.fromString("3a9e5c71-0d24-4b68-8f13-7e2c6a4d1b05"), "sum@example.org", "PLACED",
                        "2026-07-07T07:07:07.000007Z", null,
                        line(SOME_BOOK, "9780141439518", "Emma", "4.00", "EUR", 2),
                        line(SOME_BOOK, "9780141439846", "Persuasion", "6.00", "EUR", 1))));

        ResultActions result = mockMvc.perform(get("/orders").param("customerEmail", "sum@example.org"))
                .andExpect(status().isOk());

        List<Map<String, Object>> summaries = jsonArray(result);
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).get("itemCount")).isEqualTo(3);
        assertThat(summaries.get(0).get("total")).isEqualTo("14.00");
    }

    @Test
    void byCustomerPassesTheRawValueToTheFlow() throws Exception {
        mockMvc.perform(get("/orders").param("customerEmail", " Reader@Example.com"))
                .andExpect(status().isOk());

        verify(flow).findByCustomer(" Reader@Example.com");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void byCustomerPassesAnEmptyValueToTheFlow() throws Exception {
        when(flow.findByCustomer("")).thenThrow(new InvalidInputException("invalid-email", "Invalid e-mail address",
                "The e-mail address is not valid",
                List.of(Map.of("field", "customerEmail", "message", "must contain one at sign"))));

        ResultActions result = mockMvc.perform(get("/orders?customerEmail="));

        result.andExpect(status().is(422))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid e-mail address"))
                .andExpect(jsonPath("$.code").value("invalid-email"))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("customerEmail"))
                .andExpect(jsonPath("$.details[0].message").value("must contain one at sign"));
        verify(flow).findByCustomer("");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void byCustomerPassesABlankValueToTheFlow() throws Exception {
        mockMvc.perform(get("/orders").param("customerEmail", "   ")).andExpect(status().isOk());

        verify(flow).findByCustomer("   ");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void byCustomerPassesARepeatedParameterCommaJoined() throws Exception {
        mockMvc.perform(get("/orders?customerEmail=a@b.co&customerEmail=c@d.co")).andExpect(status().isOk());

        verify(flow).findByCustomer("a@b.co,c@d.co");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void byCustomerReturnsAnEmptyArray() throws Exception {
        when(flow.findByCustomer("nobody@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/orders").param("customerEmail", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(flow).findByCustomer("nobody@example.com");
    }

    // --- the customerEmail parameter is required ---

    @Test
    void byCustomerWithoutCustomerEmailIsRejectedWith400() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders"));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.code").value("validation-failed"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("customerEmail"))
                .andExpect(jsonPath("$.details[0].message").value("is required"))
                .andExpect(jsonPath("$.details[0].*", hasSize(2)));
        verifyNoInteractions(flow);
    }

    @Test
    void handleMissingQueryParameterBuildsTheValidationProblem() {
        ProblemDetail problem = new QueryExceptionHandler()
                .handleMissingQueryParameter(new OrderQueries.MissingQueryParameterException("sample"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Validation failed");
        assertThat(problem.getDetail()).isNotBlank();
        assertThat(problem.getProperties()).containsEntry("code", "validation-failed");
        assertThat(problem.getProperties()).containsEntry("details",
                List.of(Map.of("field", "sample", "message", "is required")));
    }
}
