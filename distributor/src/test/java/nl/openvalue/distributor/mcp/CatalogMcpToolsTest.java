package nl.openvalue.distributor.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import nl.openvalue.distributor.catalog.Availability;
import nl.openvalue.distributor.catalog.Contributor;
import nl.openvalue.distributor.catalog.ContributorRole;
import nl.openvalue.distributor.catalog.Format;
import nl.openvalue.distributor.catalog.RestockService;
import nl.openvalue.distributor.catalog.Title;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class CatalogMcpToolsTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    // Friday 2021-11-05 00:30 in Amsterdam, still Thursday 2021-11-04 in UTC (a year well before any real run date).
    private static final Clock FRIDAY_IN_AMSTERDAM = Clock.fixed(Instant.parse("2021-11-04T23:30:00Z"), AMSTERDAM);

    private static final String IN_STOCK_EAN = "9789900000400";
    private static final String BACKORDER_EAN = "9789900000417";
    private static final String OUT_OF_PRINT_EAN = "9789900000424";

    private static final int EDGE_TITLE_COUNT = 7;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void limitDefaultsToTen() {
        CatalogMcpTools tools = toolsFor(shelfTitles(30));

        List<Map<String, Object>> items = searchItems(tools.searchTitles("shelf", null, null));

        assertThat(items).extracting(item -> item.get("ean")).containsExactlyElementsOf(shelfEans(10));
    }

    @Test
    void limitIsCappedAtTwenty() {
        List<Title> titles = shelfTitles(30);
        CatalogMcpTools tools = toolsFor(titles);
        // 21 items stay far under the cap, so only the limit can stop the result at 20.
        String twentyOneItems = jsonMapper.writeValueAsString(
                IntStream.rangeClosed(1, 21).mapToObj(i -> TitleSummary.of(shelfTitle(i))).toList());
        assertThat(twentyOneItems.length()).isLessThan(4000);

        assertThat(searchItems(tools.searchTitles("shelf", null, 20))).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(shelfEans(20));
        assertThat(searchItems(tools.searchTitles("shelf", null, 21))).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(shelfEans(20));
        assertThat(searchItems(tools.searchTitles("shelf", null, 50))).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(shelfEans(20));
        assertThat(searchItems(tools.searchTitles("shelf", null, 19))).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(shelfEans(19));
    }

    @Test
    void limitBelowOneIsRaisedToOne() {
        CatalogMcpTools tools = toolsFor(shelfTitles(30));

        assertThat(searchItems(tools.searchTitles("shelf", null, 0))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000001");
        assertThat(searchItems(tools.searchTitles("shelf", null, -1))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000001");
        assertThat(searchItems(tools.searchTitles("shelf", null, 1))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000001");
        assertThat(searchItems(tools.searchTitles("shelf", null, 2))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000001", "9789900000002");
    }

    @Test
    void authorsListsOnlyAuthorRoleJoinedInOrder() {
        // A price other than the fixture default 1250, so a hard-coded price cannot pass.
        Title mixed = title("9789900000103", "Mixed Roles", List.of(
                new Contributor("Bea Zed", ContributorRole.AUTHOR),
                new Contributor("Xan Vos", ContributorRole.TRANSLATOR),
                new Contributor("Abe Young", ContributorRole.AUTHOR),
                new Contributor("Ida Ink", ContributorRole.ILLUSTRATOR),
                new Contributor("Ed Edit", ContributorRole.EDITOR)),
                List.of("JUV000000", "FIC009000"), 999, Availability.BACKORDER, 0);
        Title solo = title("9789900000097", "Solo Roles", List.of(new Contributor("Cy Solo", ContributorRole.AUTHOR)),
                List.of("POE000000"), Availability.OUT_OF_PRINT, 0);
        CatalogMcpTools tools = toolsFor(List.of(solo, mixed));

        List<Map<String, Object>> items = searchItems(tools.searchTitles("roles", null, null));

        assertThat(items).containsExactly(
                Map.of("ean", "9789900000103", "titleName", "Mixed Roles", "authors", "Bea Zed, Abe Young",
                        "subjectCodes", List.of("JUV000000", "FIC009000"), "availability", "BACKORDER",
                        "listPriceCents", 999),
                Map.of("ean", "9789900000097", "titleName", "Solo Roles", "authors", "Cy Solo",
                        "subjectCodes", List.of("POE000000"), "availability", "OUT_OF_PRINT",
                        "listPriceCents", 1250));
    }

    @Test
    void subjectNarrowsTheSearch() {
        Author cora = new Author("Cora Common");
        CatalogMcpTools tools = toolsFor(List.of(
                title("9789900000202", "Star Road", cora.only(), List.of("FIC028000"), Availability.IN_STOCK, 4),
                title("9789900000219", "Dragon Gate", cora.only(), List.of("FIC009000"), Availability.IN_STOCK, 4),
                title("9789900000226", "Moon Port", cora.only(), List.of("HIS000000", "FIC028000", "POE000000"),
                        Availability.IN_STOCK, 4),
                title("9789900000233", "Comet Lane", new Author("Someone Else").only(), List.of("FIC028000"),
                        Availability.IN_STOCK, 4)));

        assertThat(searchItems(tools.searchTitles("common", null, null))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000219", "9789900000226", "9789900000202");
        assertThat(searchItems(tools.searchTitles("common", "FIC028", null))).extracting(item -> item.get("ean"))
                .containsExactly("9789900000226", "9789900000202");
    }

    @Test
    void resultTextIsTrimmedUnderTheCap() {
        List<Title> titles = new ArrayList<>();
        IntStream.iterate(20, i -> i >= 2, i -> i - 2).forEach(i -> titles.add(longTitle(i)));
        IntStream.iterate(1, i -> i <= 19, i -> i + 2).forEach(i -> titles.add(longTitle(i)));
        CatalogMcpTools tools = toolsFor(titles);

        String text = text(tools.searchTitles("long title", null, 20));

        assertThat(text.length()).isLessThan(4000);
        List<Map<String, Object>> items = jsonMapper.readValue(text, new TypeReference<>() {
        });
        int kept = items.size();
        assertThat(kept).isBetween(1, 19);
        assertThat(items).extracting(item -> item.get("ean")).containsExactlyElementsOf(
                IntStream.rangeClosed(1, kept).mapToObj(CatalogMcpToolsTest::longTitleEan).toList());
        // The trim is maximal: one more item would have reached the cap.
        String oneMore = jsonMapper.writeValueAsString(
                IntStream.rangeClosed(1, kept + 1).mapToObj(i -> TitleSummary.of(longTitle(i))).toList());
        assertThat(oneMore.length()).isGreaterThanOrEqualTo(4000);
    }

    @Test
    void resultTextOfExactly4000CharactersLosesTheLastItem() {
        List<Title> titles = edgeTitlesWhoseTextIs(4000);

        List<Map<String, Object>> items = searchItems(toolsFor(titles).searchTitles("edge", null, 20));

        assertThat(items).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(edgeEans(EDGE_TITLE_COUNT - 1));
    }

    @Test
    void resultTextOfExactly3999CharactersIsKeptWhole() {
        List<Title> titles = edgeTitlesWhoseTextIs(3999);

        CallToolResult result = toolsFor(titles).searchTitles("edge", null, 20);

        assertThat(text(result)).hasSize(3999);
        assertThat(searchItems(result)).extracting(item -> item.get("ean"))
                .containsExactlyElementsOf(edgeEans(EDGE_TITLE_COUNT));
    }

    @Test
    void checkAvailabilityDoesNotPlaceAnOrder() {
        TitleCatalog catalog = new TitleCatalog(stockTitles(), List.of());
        RestockService restockService = spy(new RestockService(catalog, FRIDAY_IN_AMSTERDAM));
        CatalogMcpTools tools = new CatalogMcpTools(catalog, restockService, jsonMapper);

        Map<String, Object> inStock = report(tools.checkAvailability(IN_STOCK_EAN));
        Map<String, Object> backorder = report(tools.checkAvailability(BACKORDER_EAN));

        assertThat(inStock).containsEntry("expectedDeliveryDate", "2021-11-09");
        assertThat(backorder).containsEntry("expectedDeliveryDate", "2021-11-19");
        verify(restockService, never()).placeOrder(any(), anyInt(), any());
    }

    @Test
    void checkAvailabilityForOutOfPrintHasNoDeliveryDate() {
        CallToolResult result = toolsFor(stockTitles()).checkAvailability(OUT_OF_PRINT_EAN);

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        Map<String, Object> report = report(result);
        assertThat(report).containsOnlyKeys("ean", "availability", "warehouseQuantity", "expectedDeliveryDate");
        assertThat(report).containsEntry("ean", OUT_OF_PRINT_EAN)
                .containsEntry("availability", "OUT_OF_PRINT")
                .containsEntry("warehouseQuantity", 0);
        assertThat(report.get("expectedDeliveryDate")).isNull();
    }

    @Test
    void checkAvailabilityForBackorderIsFourteenDaysOut() {
        CallToolResult result = toolsFor(stockTitles()).checkAvailability(BACKORDER_EAN);

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(report(result)).isEqualTo(Map.of("ean", BACKORDER_EAN, "availability", "BACKORDER",
                "warehouseQuantity", 0, "expectedDeliveryDate", "2021-11-19"));
    }

    @Test
    void checkAvailabilityForInStockIsTwoWorkingDaysOut() {
        CallToolResult result = toolsFor(stockTitles()).checkAvailability(IN_STOCK_EAN);

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(report(result)).isEqualTo(Map.of("ean", IN_STOCK_EAN, "availability", "IN_STOCK",
                "warehouseQuantity", 37, "expectedDeliveryDate", "2021-11-09"));
    }

    @Test
    void checkAvailabilityForAnUnknownEanIsTheSingleLineToolError() {
        CallToolResult result = toolsFor(stockTitles()).checkAvailability("9780000000002");

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("No title with EAN 9780000000002");
    }

    private static List<Title> stockTitles() {
        Author author = new Author("Stock Keeper");
        return List.of(
                title(OUT_OF_PRINT_EAN, "Gone Book", author.only(), List.of("FIC004000"), Availability.OUT_OF_PRINT, 0),
                title(IN_STOCK_EAN, "Kept Book", author.only(), List.of("FIC004000"), Availability.IN_STOCK, 37),
                title(BACKORDER_EAN, "Awaited Book", author.only(), List.of("FIC004000"), Availability.BACKORDER, 0));
    }

    private Map<String, Object> report(CallToolResult result) {
        return jsonMapper.readValue(text(result), new TypeReference<>() {
        });
    }

    // About 190 characters of title and five long AUTHOR names: some 650 characters of JSON per item.
    private static Title longTitle(int number) {
        String titleName = ("Long title %02d: ".formatted(number) + "a subtitle that keeps going on and on, ".repeat(5))
                .substring(0, 190).strip();
        List<Contributor> authors = IntStream.rangeClosed(1, 5)
                .mapToObj(a -> new Contributor("Augusta Philippa Wolstenholme-Fairweather the %d of %02d"
                        .formatted(a, number), ContributorRole.AUTHOR))
                .toList();
        return title(longTitleEan(number), titleName, authors, List.of("FIC014000", "HIS000000"),
                Availability.IN_STOCK, 9);
    }

    private static String longTitleEan(int number) {
        return "97899000003%02d".formatted(number);
    }

    // Fixture: "Edge 01" to "Edge 07", grown one character at a time at the end of the last title's name until the
    // seven-item TitleSummary array, written by the same mapper as the tools, is exactly the wanted length.
    private List<Title> edgeTitlesWhoseTextIs(int length) {
        for (int padding = 0; padding < 2000; padding++) {
            List<Title> titles = new ArrayList<>();
            IntStream.range(1, EDGE_TITLE_COUNT).forEach(i -> titles.add(edgeTitle(i, "")));
            titles.add(edgeTitle(EDGE_TITLE_COUNT, "x".repeat(padding)));
            String text = jsonMapper.writeValueAsString(titles.stream().map(TitleSummary::of).toList());
            assertThat(text.length()).as("padding %d overshoots %d", padding, length).isLessThanOrEqualTo(length);
            if (text.length() == length) {
                return titles;
            }
        }
        throw new IllegalStateException("No padding gives a text of " + length + " characters");
    }

    private static Title edgeTitle(int number, String padding) {
        String titleName = "Edge %02d ".formatted(number) + "words that fill the title of this book ".repeat(4)
                + padding;
        List<Contributor> authors = IntStream.rangeClosed(1, 4)
                .mapToObj(a -> new Contributor("Author Number %d Of Edge Title %02d".formatted(a, number),
                        ContributorRole.AUTHOR))
                .toList();
        return title(edgeEan(number), titleName, authors, List.of("FIC004000"), Availability.IN_STOCK, 3);
    }

    private static String edgeEan(int number) {
        return "97899000005%02d".formatted(number);
    }

    private static List<String> edgeEans(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(CatalogMcpToolsTest::edgeEan).toList();
    }

    private record Author(String name) {

        List<Contributor> only() {
            return List.of(new Contributor(name, ContributorRole.AUTHOR));
        }
    }

    // Fixture: "Shelf 01" to "Shelf <count>" by one author, short enough that 21 of them stay far under the cap.
    // Inserted even numbers descending, then odd numbers ascending, so only the search's own sort puts them in order.
    private static List<Title> shelfTitles(int count) {
        List<Title> titles = new ArrayList<>();
        IntStream.iterate(count - count % 2, i -> i >= 2, i -> i - 2).forEach(i -> titles.add(shelfTitle(i)));
        IntStream.iterate(1, i -> i <= count, i -> i + 2).forEach(i -> titles.add(shelfTitle(i)));
        return titles;
    }

    private static Title shelfTitle(int number) {
        return title(shelfEan(number), "Shelf %02d".formatted(number),
                List.of(new Contributor("Ann Ash", ContributorRole.AUTHOR)), List.of("FIC004000"),
                Availability.IN_STOCK, 3);
    }

    private static String shelfEan(int number) {
        return "97899000%05d".formatted(number);
    }

    private static List<String> shelfEans(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(CatalogMcpToolsTest::shelfEan).toList();
    }

    private static Title title(String ean, String titleName, List<Contributor> contributors, List<String> subjectCodes,
            Availability availability, int warehouseQuantity) {
        return title(ean, titleName, contributors, subjectCodes, 1250, availability, warehouseQuantity);
    }

    private static Title title(String ean, String titleName, List<Contributor> contributors, List<String> subjectCodes,
            int listPriceCents, Availability availability, int warehouseQuantity) {
        return new Title(ean, titleName, contributors, subjectCodes, Format.PAPERBACK, listPriceCents, "EUR",
                availability, warehouseQuantity, LocalDate.of(1901, 3, 4));
    }

    private CatalogMcpTools toolsFor(List<Title> titles) {
        TitleCatalog catalog = new TitleCatalog(titles, List.of());
        return new CatalogMcpTools(catalog, new RestockService(catalog, FRIDAY_IN_AMSTERDAM), jsonMapper);
    }

    private List<Map<String, Object>> searchItems(CallToolResult result) {
        return jsonMapper.readValue(text(result), new TypeReference<>() {
        });
    }

    private static String text(CallToolResult result) {
        assertThat(result.content()).hasSize(1);
        return ((TextContent) result.content().getFirst()).text();
    }
}
