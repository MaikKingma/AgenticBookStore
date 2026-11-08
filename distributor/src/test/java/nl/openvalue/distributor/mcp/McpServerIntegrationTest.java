package nl.openvalue.distributor.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.openvalue.distributor.FixedClockTestConfiguration;
import nl.openvalue.distributor.catalog.Availability;
import nl.openvalue.distributor.catalog.Title;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

// Same class annotations as HealthEndpointTest, so both share one cached context.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(FixedClockTestConfiguration.class)
class McpServerIntegrationTest {

    private static final String PRIDE_AND_PREJUDICE = "9780141439518";

    @LocalServerPort
    private int port;

    @Autowired
    private TitleCatalog catalog;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Clock clock;

    private McpSyncClient client;
    private InitializeResult initializeResult;

    @BeforeEach
    void connect() {
        // 127.0.0.1, not localhost: the server binds the IPv4 loopback only and localhost may resolve to ::1 first.
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://127.0.0.1:" + port)
                .endpoint("/mcp")
                .build();
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        initializeResult = client.initialize();
    }

    @AfterEach
    void close() {
        client.closeGracefully();
    }

    @Test
    void automatedRoundTripListsFourToolsAndFindsPrideAndPrejudice() {
        assertThat(client.listTools().tools()).extracting(Tool::name)
                .containsExactlyInAnyOrder("search_titles", "get_title", "list_subjects", "check_availability");

        CallToolResult result = call("search_titles", Map.of("query", "austen"));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(text(result)).contains("Pride and Prejudice");
    }

    @Test
    void searchTitlesOutputStaysSmall() {
        CallToolResult result = call("search_titles", Map.of("query", "a", "limit", 50));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        String text = text(result);
        assertThat(text.length()).isLessThan(4000);
        List<Map<String, Object>> items = parse(text);
        assertThat(items).hasSizeBetween(1, 20);
        assertThat(items).allSatisfy(item -> assertThat(item).containsOnlyKeys(
                "ean", "titleName", "authors", "subjectCodes", "availability", "listPriceCents"));
    }

    @Test
    void everyToolIsReadOnlyAndNotDestructive() {
        assertThat(client.listTools().tools())
                .extracting(Tool::name,
                        tool -> tool.annotations().readOnlyHint(),
                        tool -> tool.annotations().destructiveHint(),
                        tool -> tool.annotations().idempotentHint(),
                        tool -> tool.annotations().openWorldHint())
                .containsExactlyInAnyOrder(
                        tuple("search_titles", true, false, true, false),
                        tuple("get_title", true, false, true, false),
                        tuple("list_subjects", true, false, true, false),
                        tuple("check_availability", true, false, true, false));
    }

    @Test
    void requiredParametersAreAdvertised() {
        Map<String, Object> required = client.listTools().tools().stream()
                .collect(Collectors.toMap(Tool::name, tool -> tool.inputSchema().getOrDefault("required", List.of())));

        assertThat(required).isEqualTo(Map.of(
                "search_titles", List.of("query"),
                "get_title", List.of("ean"),
                "list_subjects", List.of(),
                "check_availability", List.of("ean")));
    }

    @Test
    void searchTitlesWithoutQueryIsAToolError() {
        CallToolResult result = call("search_titles", Map.of("subject", "FIC"));

        assertThat(result.isError()).isTrue();
    }

    @Test
    void toolDescriptionsStateUnits() {
        Map<String, String> descriptions = client.listTools().tools().stream()
                .collect(Collectors.toMap(Tool::name, Tool::description));

        assertThat(descriptions).containsOnlyKeys("search_titles", "get_title", "list_subjects", "check_availability");
        assertThat(descriptions.values()).allSatisfy(description -> assertThat(description).isNotBlank());
        assertThat(descriptions.get("search_titles")).contains("cents").contains("ISBN-13");
        assertThat(descriptions.get("get_title")).contains("cents").contains("ISBN-13");
        assertThat(descriptions.get("check_availability")).contains("ISBN-13");
    }

    @Test
    void getTitleForAnUnknownEanIsASingleLineToolError() {
        CallToolResult result = call("get_title", Map.of("ean", "9780000000002"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("No title with EAN 9780000000002").hasLineCount(1);
    }

    @Test
    void getTitleReturnsTheFullTitle() {
        CallToolResult result = call("get_title", Map.of("ean", PRIDE_AND_PREJUDICE));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(parseObject(text(result))).isEqualTo(Map.of(
                "ean", "9780141439518",
                "titleName", "Pride and Prejudice",
                "contributors", List.of(Map.of("name", "Jane Austen", "role", "AUTHOR")),
                "subjectCodes", List.of("FIC004000"),
                "format", "PAPERBACK",
                "listPriceCents", 999,
                "currency", "EUR",
                "availability", "IN_STOCK",
                "warehouseQuantity", 240,
                "releaseDate", "1813-01-28"));
    }

    @Test
    void checkAvailabilityReportsStockAndDelivery() {
        // The fixed clock of FixedClockTestConfiguration is in effect, so the delivery dates below are literals.
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 11, 6));
        Title backorder = anyTitleWith(Availability.BACKORDER);
        Title outOfPrint = anyTitleWith(Availability.OUT_OF_PRINT);

        Map<String, Object> inStockReport = parseObject(text(call("check_availability",
                Map.of("ean", PRIDE_AND_PREJUDICE))));
        Map<String, Object> backorderReport = parseObject(text(call("check_availability",
                Map.of("ean", backorder.ean()))));
        Map<String, Object> outOfPrintReport = parseObject(text(call("check_availability",
                Map.of("ean", outOfPrint.ean()))));

        assertThat(inStockReport).isEqualTo(Map.of("ean", "9780141439518", "availability", "IN_STOCK",
                "warehouseQuantity", 240, "expectedDeliveryDate", "2026-11-10"));
        assertThat(backorderReport).isEqualTo(Map.of("ean", backorder.ean(), "availability", "BACKORDER",
                "warehouseQuantity", backorder.warehouseQuantity(),
                "expectedDeliveryDate", "2026-11-20"));
        assertThat(outOfPrintReport)
                .containsOnlyKeys("ean", "availability", "warehouseQuantity", "expectedDeliveryDate")
                .containsEntry("ean", outOfPrint.ean())
                .containsEntry("availability", "OUT_OF_PRINT")
                .containsEntry("warehouseQuantity", outOfPrint.warehouseQuantity());
        assertThat(outOfPrintReport.get("expectedDeliveryDate")).isNull();
    }

    @Test
    void listSubjectsReturnsCodesAndLabels() {
        CallToolResult result = call("list_subjects", Map.of());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(parse(text(result))).containsExactly(
                Map.of("code", "COM000000", "label", "Computers / General"),
                Map.of("code", "FIC002000", "label", "Fiction / Action and Adventure"),
                Map.of("code", "FIC004000", "label", "Fiction / Classics"),
                Map.of("code", "FIC009000", "label", "Fiction / Fantasy"),
                Map.of("code", "FIC014000", "label", "Fiction / Historical"),
                Map.of("code", "FIC015000", "label", "Fiction / Horror"),
                Map.of("code", "FIC022000", "label", "Fiction / Mystery and Detective"),
                Map.of("code", "FIC027000", "label", "Fiction / Romance"),
                Map.of("code", "FIC028000", "label", "Fiction / Science Fiction"),
                Map.of("code", "HIS000000", "label", "History / General"),
                Map.of("code", "JUV000000", "label", "Juvenile Fiction / General"),
                Map.of("code", "POE000000", "label", "Poetry / General"),
                Map.of("code", "SCI000000", "label", "Science / General"));
    }

    @Test
    void serverIdentifiesAsDistributorCatalog() {
        assertThat(initializeResult.serverInfo().name()).isEqualTo("distributor-catalog");
        assertThat(initializeResult.serverInfo().version()).isEqualTo("1.0.0");
        assertThat(initializeResult.protocolVersion()).isEqualTo("2025-11-25");
        assertThat(initializeResult.instructions())
                .startsWith("Read-only catalogue of the book distributor.")
                .contains("search_titles", "get_title", "check_availability", "Prices are in cents.");
    }

    @Test
    void everyToolResultStaysUnderTheCap() {
        assertThat(text(call("list_subjects", Map.of())).length()).isLessThan(4000);
        assertThat(catalog.all()).isNotEmpty().allSatisfy(title -> {
            assertThat(text(call("get_title", Map.of("ean", title.ean()))).length()).isLessThan(4000);
            assertThat(text(call("check_availability", Map.of("ean", title.ean()))).length()).isLessThan(4000);
        });
    }

    private CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(CallToolRequest.builder(tool).arguments(arguments).build());
    }

    private Title anyTitleWith(Availability availability) {
        return catalog.all().stream().filter(title -> title.availability() == availability).findFirst().orElseThrow();
    }

    private List<Map<String, Object>> parse(String text) {
        return jsonMapper.readValue(text, new TypeReference<>() {
        });
    }

    private Map<String, Object> parseObject(String text) {
        return jsonMapper.readValue(text, new TypeReference<>() {
        });
    }

    private static String text(CallToolResult result) {
        assertThat(result.content()).hasSize(1);
        return ((TextContent) result.content().getFirst()).text();
    }
}
