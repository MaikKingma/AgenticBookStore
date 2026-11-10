package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TitlesController.class)
class TitlesApiTest {

    @TestConfiguration
    static class FixtureCatalogue {

        @Bean
        TitleCatalog titleCatalog() {
            return ApiTestFixtures.catalog();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void subjectPrefixReturnsAtMostSizeItemsAndTotalCountsAllMatches() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("subject", "FIC028").param("size", "5"));

        assertThat(body.get("total")).isEqualTo(7);
        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(5);
        List<Map<String, Object>> items = items(body);
        assertThat(items).extracting(item -> item.get("ean"))
                .containsExactly("9789900010004", "9789900010011", "9789900010014", "9789900010003",
                        "9789900010017");
        assertThat(items).allSatisfy(item -> assertThat(codes(item)).anyMatch(code -> code.startsWith("FIC028")));
    }

    @Test
    void unknownEanIs404WithTitleNotFoundEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles/9780000000002")), 404,
                "TITLE_NOT_FOUND");
    }

    @Test
    void titleByEanHasTheWireShape() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/titles/9789900010017"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> body = ApiTestFixtures.readMap(result.getResponse().getContentAsString());
        assertThat(body).isEqualTo(Map.ofEntries(
                Map.entry("ean", "9789900010017"),
                Map.entry("titleName", "Moon Cartographer"),
                Map.entry("contributors", List.of(
                        Map.of("name", "Nora Vale", "role", "AUTHOR"),
                        Map.of("name", "Ines Marsh", "role", "TRANSLATOR"))),
                Map.entry("subjectCodes", List.of("FIC028000", "FIC009000")),
                Map.entry("format", "HARDCOVER"),
                Map.entry("listPriceCents", 1850),
                Map.entry("currency", "EUR"),
                Map.entry("availability", "IN_STOCK"),
                Map.entry("warehouseQuantity", 77),
                Map.entry("releaseDate", "2021-10-01")));
    }

    @Test
    void pageResponseHasExactlyItemsPageSizeTotal() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("q", "moon"));

        assertThat(body).containsOnlyKeys("items", "page", "size", "total");
        assertThat(items(body)).singleElement().satisfies(item -> assertThat(item).containsOnlyKeys(
                "ean", "titleName", "contributors", "subjectCodes", "format", "listPriceCents", "currency",
                "availability", "warehouseQuantity", "releaseDate"));
    }

    @Test
    void defaultsArePageZeroSizeTwenty() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles"));

        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("size")).isEqualTo(20);
        assertThat(body.get("total")).isEqualTo(25);
        List<Map<String, Object>> items = items(body);
        assertThat(items).hasSize(20);
        assertThat(items.get(0).get("ean")).isEqualTo("9789900010008");
        assertThat(items.get(19).get("ean")).isEqualTo("9789900010016");
    }

    @Test
    void pageAndSizeArePassedThrough() throws Exception {
        Map<String, Object> secondPage = okBody(get("/api/titles").param("page", "1").param("size", "5"));

        assertThat(secondPage.get("page")).isEqualTo(1);
        assertThat(secondPage.get("size")).isEqualTo(5);
        assertThat(secondPage.get("total")).isEqualTo(25);
        assertThat(items(secondPage)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010018", "9789900010025", "9789900010007", "9789900010014",
                        "9789900010021");

        Map<String, Object> lastPage = okBody(get("/api/titles").param("page", "2").param("size", "10"));

        assertThat(lastPage.get("page")).isEqualTo(2);
        assertThat(lastPage.get("size")).isEqualTo(10);
        assertThat(lastPage.get("total")).isEqualTo(25);
        assertThat(items(lastPage)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010023", "9789900010005", "9789900010012", "9789900010019",
                        "9789900010001");
    }

    @Test
    void qMatchesTitleOrContributorCaseInsensitively() throws Exception {
        Map<String, Object> byTitleAndTranslator = okBody(get("/api/titles").param("q", "HARBOR"));

        assertThat(byTitleAndTranslator.get("total")).isEqualTo(3);
        assertThat(items(byTitleAndTranslator)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010007", "9789900010021", "9789900010020");

        Map<String, Object> byAuthor = okBody(get("/api/titles").param("q", "wELLESLEY"));

        assertThat(byAuthor.get("total")).isEqualTo(1);
        assertThat(items(byAuthor)).extracting(item -> item.get("ean")).containsExactly("9789900010022");
    }

    @Test
    void availabilityFilterApplies() throws Exception {
        Map<String, Object> backorder = okBody(get("/api/titles").param("availability", "BACKORDER"));

        assertThat(backorder.get("total")).isEqualTo(4);
        assertThat(items(backorder)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010015", "9789900010010", "9789900010006", "9789900010009");

        Map<String, Object> outOfPrint = okBody(get("/api/titles").param("availability", "OUT_OF_PRINT"));

        assertThat(outOfPrint.get("total")).isEqualTo(2);
        assertThat(items(outOfPrint)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010025", "9789900010013");
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        // "run" also matches an out-of-subject title (Umber Runway) and a backordered one (Orbit Runner).
        Map<String, Object> body = okBody(
                get("/api/titles").param("subject", "FIC028").param("availability", "IN_STOCK").param("q", "run"));

        assertThat(body.get("total")).isEqualTo(2);
        assertThat(items(body)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010004", "9789900010003");
    }

    @Test
    void unknownAvailabilityIs400InvalidRequest() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("availability", "NOPE")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void size50IsAccepted() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("size", "50"));

        assertThat(body.get("size")).isEqualTo(50);
        assertThat(body.get("total")).isEqualTo(25);
        assertThat(items(body)).hasSize(25);
    }

    @Test
    void size1IsAccepted() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("size", "1"));

        assertThat(body.get("size")).isEqualTo(1);
        assertThat(items(body)).extracting(item -> item.get("ean")).containsExactly("9789900010008");
    }

    @Test
    void size51Is400InvalidRequest() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("size", "51")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void size0Is400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("size", "0")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void negativeSizeIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("size", "-1")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void negativePageIs400() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("page", "-1")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void pageZeroIsAccepted() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("page", "0").param("size", "2"));

        assertThat(body.get("page")).isEqualTo(0);
        assertThat(items(body)).extracting(item -> item.get("ean"))
                .containsExactly("9789900010008", "9789900010015");
    }

    @Test
    void nonNumericPageIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("page", "abc")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void nonNumericSizeIs400WithTheEnvelope() throws Exception {
        ApiTestFixtures.expectErrorEnvelope(mockMvc.perform(get("/api/titles").param("size", "abc")), 400,
                "INVALID_REQUEST");
    }

    @Test
    void hugePageIsEmpty() throws Exception {
        Map<String, Object> body = okBody(get("/api/titles").param("page", "2147483647").param("size", "50"));

        assertThat(items(body)).isEmpty();
        assertThat(body.get("page")).isEqualTo(2147483647);
        assertThat(body.get("size")).isEqualTo(50);
        assertThat(body.get("total")).isEqualTo(25);
    }

    @Test
    void subjectsAreReturnedWithCodesAndLabelsInCodeOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        List<Object> body = ApiTestFixtures.readList(result.getResponse().getContentAsString());
        assertThat(body).isEqualTo(List.of(
                Map.of("code", "COM051000", "label", "Programming"),
                Map.of("code", "FIC009000", "label", "Fantasy"),
                Map.of("code", "FIC028000", "label", "Science Fiction"),
                Map.of("code", "HIS002000", "label", "Aviation history")));
    }

    @Test
    void wrongMethodIs405WithTheEnvelopeAndTheAllowHeader() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/titles"));

        ApiTestFixtures.expectErrorEnvelope(result, 405, "METHOD_NOT_ALLOWED");
        assertThat(result.andReturn().getResponse().getHeader("Allow")).isEqualTo("GET");
        Map<String, Object> body = ApiTestFixtures.readMap(result.andReturn().getResponse().getContentAsString());
        assertThat(body.get("message")).isEqualTo("Method DELETE is not allowed here");
    }

    private Map<String, Object> okBody(MockHttpServletRequestBuilder request) throws Exception {
        ResultActions result = mockMvc.perform(request);
        MvcResult mvcResult = result
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        return ApiTestFixtures.readMap(mvcResult.getResponse().getContentAsString());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("items");
    }

    @SuppressWarnings("unchecked")
    private static List<String> codes(Map<String, Object> item) {
        return (List<String>) item.get("subjectCodes");
    }
}
