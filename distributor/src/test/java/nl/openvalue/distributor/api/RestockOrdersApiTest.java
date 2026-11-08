package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.distributor.catalog.RestockOrder;
import nl.openvalue.distributor.catalog.RestockService;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(RestockOrdersController.class)
@Import(RestockService.class)
class RestockOrdersApiTest {

    private static final String IN_STOCK_EAN = ApiTestFixtures.IN_STOCK_EAN;
    private static final String BACKORDER_EAN = ApiTestFixtures.BACKORDER_EAN;
    private static final String OUT_OF_PRINT_EAN = ApiTestFixtures.OUT_OF_PRINT_EAN;
    private static final String QUANTITY_MESSAGE = "quantity must be a whole number between 1 and 500";

    @TestConfiguration
    static class FixtureWorld {

        @Bean
        TitleCatalog titleCatalog() {
            return ApiTestFixtures.catalog();
        }

        /** A Friday in 2021: a clock that ignores the zone or the fixed instant cannot give the asserted dates. */
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2021-11-05T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestockService restockService;

    @Test
    void inStockTitleOnFridayIsConfirmedForNextTuesday() throws Exception {
        Map<String, Object> body = created("{\"ean\":\"" + IN_STOCK_EAN
                + "\",\"quantity\":10,\"retailerReference\":\"PO-2021-77\"}");

        assertThat(body).containsOnlyKeys("restockOrderId", "ean", "quantity", "status", "expectedDeliveryDate");
        assertThat(UUID.fromString((String) body.get("restockOrderId")).toString())
                .isEqualTo(body.get("restockOrderId"));
        assertThat(body.get("ean")).isEqualTo("9789900010008");
        assertThat(body.get("quantity")).isEqualTo(10);
        assertThat(body.get("status")).isEqualTo("CONFIRMED");
        assertThat(body.get("expectedDeliveryDate")).isEqualTo("2021-11-09");
    }

    @Test
    void retailerReferenceIsStoredWhenGivenAndNullWhenOmitted() throws Exception {
        // The response never echoes the reference, so the stored order is the only place it can be seen.
        Map<String, Object> withReference = created("{\"ean\":\"" + IN_STOCK_EAN
                + "\",\"quantity\":10,\"retailerReference\":\"PO-2021-77\"}");
        Map<String, Object> withoutReference = created("{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":10}");

        assertThat(withReference).doesNotContainKey("retailerReference");
        assertThat(restockService.findOrder((String) withReference.get("restockOrderId")))
                .map(RestockOrder::retailerReference)
                .contains("PO-2021-77");
        assertThat(restockService.findOrder((String) withoutReference.get("restockOrderId")))
                .get()
                .extracting(RestockOrder::retailerReference)
                .isNull();
    }

    @Test
    void backorderTitleIsBackorderedFourteenDaysOut() throws Exception {
        Map<String, Object> body = created("{\"ean\":\"" + BACKORDER_EAN + "\",\"quantity\":37}");

        assertThat(body).containsOnlyKeys("restockOrderId", "ean", "quantity", "status", "expectedDeliveryDate");
        assertThat(body.get("ean")).isEqualTo("9789900010015");
        assertThat(body.get("quantity")).isEqualTo(37);
        assertThat(body.get("status")).isEqualTo("BACKORDERED");
        assertThat(body.get("expectedDeliveryDate")).isEqualTo("2021-11-19");
    }

    @Test
    void outOfPrintTitleIs422NotOrderable() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + OUT_OF_PRINT_EAN + "\",\"quantity\":10}"), 422,
                "NOT_ORDERABLE");
    }

    @Test
    void unknownEanIs404TitleNotFound() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"9780000000002\",\"quantity\":10}"), 404,
                "TITLE_NOT_FOUND");
    }

    @Test
    void quantity0Is400InvalidRequest() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order(quantityBody(0)), 400, "INVALID_REQUEST");
    }

    @Test
    void negativeQuantityIs400InvalidRequest() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order(quantityBody(-1)), 400, "INVALID_REQUEST");
    }

    @Test
    void quantity501Is400InvalidRequest() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order(quantityBody(501)), 400, "INVALID_REQUEST");
    }

    @Test
    void quantities1And500AreAccepted() throws Exception {
        assertThat(created(quantityBody(1)).get("quantity")).isEqualTo(1);
        assertThat(created(quantityBody(500)).get("quantity")).isEqualTo(500);
    }

    @Test
    void missingQuantityIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + IN_STOCK_EAN + "\"}"), 400, "INVALID_REQUEST");
    }

    @Test
    void nullQuantityIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":null}"), 400,
                "INVALID_REQUEST");
    }

    @Test
    void nonNumericQuantityIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":\"abc\"}"), 400,
                "INVALID_REQUEST");
    }

    @Test
    void quantityBeyondTheIntRangeIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":99999999999}"),
                400, "INVALID_REQUEST");
    }

    @Test
    void malformedJsonIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\": "), 400, "INVALID_REQUEST");
    }

    @Test
    void missingBodyIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(
                mockMvc.perform(post("/api/restock-orders").contentType(MediaType.APPLICATION_JSON)), 400,
                "INVALID_REQUEST");
    }

    @Test
    void jsonNullBodyIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("null"), 400, "INVALID_REQUEST");
    }

    @Test
    void blankEanIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"\",\"quantity\":10}"), 400, "INVALID_REQUEST");
    }

    @Test
    void whitespaceEanIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"   \",\"quantity\":10}"), 400, "INVALID_REQUEST");
    }

    @Test
    void missingEanIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"quantity\":10}"), 400, "INVALID_REQUEST");
    }

    @Test
    void nullEanIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":null,\"quantity\":10}"), 400, "INVALID_REQUEST");
    }

    @Test
    void invalidQuantityIsRejectedBeforeOrderabilityIsChecked() throws Exception {
        // Out of print would be 422 and an unknown EAN 404, but the request check comes first.
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"" + OUT_OF_PRINT_EAN + "\",\"quantity\":0}"), 400,
                "INVALID_REQUEST");
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"9780000000002\",\"quantity\":501}"), 400,
                "INVALID_REQUEST");
        ApiTestFixtures.expectErrorEnvelope(order("{\"ean\":\"9780000000002\",\"quantity\":-1}"), 400,
                "INVALID_REQUEST");
    }

    @Test
    void fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated() throws Exception {
        // Truncated, 500.7, 1.5 and 10.5 would pass the range check and 0.5 would fail it; none may be truncated.
        for (String fractional : new String[] {"500.7", "1.5", "10.5", "0.5"}) {
            expectQuantityRejected(rawQuantityBody(fractional));
        }
    }

    @Test
    void integralValuedQuantitiesAreAccepted() throws Exception {
        assertThat(created(rawQuantityBody("10.0")).get("quantity")).isEqualTo(10);
        assertThat(created(rawQuantityBody("1e2")).get("quantity")).isEqualTo(100);
        assertThat(created(rawQuantityBody("500.0")).get("quantity")).isEqualTo(500);
        assertThat(created(rawQuantityBody("\"10\"")).get("quantity")).isEqualTo(10);
    }

    @Test
    void blankEanIsReportedBeforeAFractionalQuantity() throws Exception {
        ResultActions result = order("{\"ean\":\"\",\"quantity\":1.5}");

        ApiTestFixtures.expectErrorEnvelope(result, 400, "INVALID_REQUEST");
        assertThat(message(result)).isEqualTo("ean must not be blank");
    }

    @Test
    void quantityBoundariesAreExactOnBothSidesWithNoRounding() throws Exception {
        assertThat(created(rawQuantityBody("1")).get("quantity")).isEqualTo(1);
        expectQuantityRejected(rawQuantityBody("0.9999"));
        assertThat(created(rawQuantityBody("500")).get("quantity")).isEqualTo(500);
        expectQuantityRejected(rawQuantityBody("500.0000000001"));
    }

    @Test
    void fractionsBelowDoubleResolutionAreRejectedNotRoundedToTheBound() throws Exception {
        // As doubles these read 500.0 and 1.0, so a check done on a double would accept them and store 500 and 1.
        expectQuantityRejected(rawQuantityBody("500.00000000000001"));
        expectQuantityRejected(rawQuantityBody("1.0000000000000001"));
    }

    @Test
    void placedOrderIsFetchedById() throws Exception {
        Map<String, Object> first = created("{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":10}");
        Map<String, Object> second = created("{\"ean\":\"" + BACKORDER_EAN + "\",\"quantity\":37}");

        assertThat(first.get("restockOrderId")).isNotEqualTo(second.get("restockOrderId"));
        assertThat(fetched((String) first.get("restockOrderId"))).isEqualTo(first);
        assertThat(fetched((String) second.get("restockOrderId"))).isEqualTo(second);
    }

    @Test
    void unknownRestockOrderIs404RestockOrderNotFound() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(
                mockMvc.perform(get("/api/restock-orders/00000000-0000-0000-0000-000000000000")), 404,
                "RESTOCK_ORDER_NOT_FOUND");
    }

    private static String quantityBody(int quantity) {
        return "{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":" + quantity + "}";
    }

    /** A body with the IN_STOCK EAN and {@code rawQuantity} written into the JSON as is. */
    private static String rawQuantityBody(String rawQuantity) {
        return "{\"ean\":\"" + IN_STOCK_EAN + "\",\"quantity\":" + rawQuantity + "}";
    }

    private void expectQuantityRejected(String json) throws Exception {
        ResultActions result = order(json);
        ApiTestFixtures.expectErrorEnvelope(result, 400, "INVALID_REQUEST");
        assertThat(message(result)).isEqualTo(QUANTITY_MESSAGE);
    }

    private static Object message(ResultActions result) throws Exception {
        return ApiTestFixtures.readMap(result.andReturn().getResponse().getContentAsString()).get("message");
    }

    private ResultActions order(String json) throws Exception {
        return mockMvc.perform(post("/api/restock-orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Map<String, Object> created(String json) throws Exception {
        MvcResult result = order(json)
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        return ApiTestFixtures.readMap(result.getResponse().getContentAsString());
    }

    private Map<String, Object> fetched(String id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/restock-orders/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        return ApiTestFixtures.readMap(result.getResponse().getContentAsString());
    }
}
