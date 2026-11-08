package nl.openvalue.distributor.mcp;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.ArrayList;
import java.util.List;
import nl.openvalue.distributor.catalog.DeliveryQuote;
import nl.openvalue.distributor.catalog.RestockService;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The four read-only MCP tools of the distributor catalogue. Every result is JSON text written by the application's
 * JSON mapper and stays under 4000 characters. An unknown EAN is a tool error with a single line of text, never an
 * exception (a thrown exception would repeat its message in the tool result).
 */
@Component
public class CatalogMcpTools {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;
    private static final int MAX_TEXT_LENGTH = 4000;

    private final TitleCatalog catalog;
    private final RestockService restockService;
    private final JsonMapper jsonMapper;

    public CatalogMcpTools(TitleCatalog catalog, RestockService restockService, JsonMapper jsonMapper) {
        this.catalog = catalog;
        this.restockService = restockService;
        this.jsonMapper = jsonMapper;
    }

    @McpTool(name = "search_titles",
            description = "Search the distributor's book catalogue by words in the title or in a contributor's name. "
                    + "Use this first to find titles; then use get_title for all details of one title or "
                    + "check_availability before suggesting a title to stock. Returns a JSON array of at most 20 "
                    + "compact results (10 by default) sorted by title: ean (EAN = ISBN-13, 13 digits), titleName, "
                    + "authors (AUTHOR names only, comma separated), subjectCodes, availability (IN_STOCK, BACKORDER "
                    + "or OUT_OF_PRINT) and listPriceCents (list price in euro cents, so 999 is EUR 9.99).",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public CallToolResult searchTitles(
            @McpToolParam(description = "Words to find in the title or in a contributor's name, case-insensitive, "
                    + "for example austen.") String query,
            @McpToolParam(description = "Optional subject code prefix, case-insensitive, for example FIC028 for "
                    + "science fiction. Use list_subjects to see the codes.", required = false) String subject,
            @McpToolParam(description = "Optional maximum number of results, 1 to 20 (default 10). Values outside "
                    + "that range are clamped to it.", required = false) Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, MIN_LIMIT, MAX_LIMIT);
        List<TitleSummary> summaries = new ArrayList<>(catalog.search(query, subject, null, 0, size).items().stream()
                .map(TitleSummary::of)
                .toList());
        String text = jsonMapper.writeValueAsString(summaries);
        while (text.length() >= MAX_TEXT_LENGTH && !summaries.isEmpty()) {
            summaries.removeLast();
            text = jsonMapper.writeValueAsString(summaries);
        }
        return CallToolResult.builder().addTextContent(text).build();
    }

    @McpTool(name = "get_title",
            description = "Get the full catalogue record of one title by its EAN (EAN = ISBN-13, 13 digits without "
                    + "dashes). Use this after search_titles when you need every detail: all contributors with their "
                    + "roles (AUTHOR, EDITOR, TRANSLATOR, ILLUSTRATOR), subjectCodes, format, listPriceCents (list "
                    + "price in euro cents, so 999 is EUR 9.99), currency, availability, warehouseQuantity (copies "
                    + "in the warehouse) and releaseDate (ISO date). An unknown EAN gives the tool error "
                    + "\"No title with EAN <ean>\".",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public CallToolResult getTitle(
            @McpToolParam(description = "The title's EAN, which is its ISBN-13: 13 digits without dashes, for "
                    + "example 9780141439518.") String ean) {
        return catalog.findByEan(ean).map(this::success).orElseGet(() -> unknownEan(ean));
    }

    @McpTool(name = "list_subjects",
            description = "List every subject code with its label, sorted by code, for example FIC028000 is "
                    + "\"Fiction / Science Fiction\". Use this to find a code or code prefix for the subject "
                    + "parameter of search_titles. Takes no parameters.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public String listSubjects() {
        return jsonMapper.writeValueAsString(catalog.subjects());
    }

    @McpTool(name = "check_availability",
            description = "Check whether one title can be restocked, by its EAN (EAN = ISBN-13, 13 digits without "
                    + "dashes). Use this before suggesting a title to stock. Returns availability (IN_STOCK, "
                    + "BACKORDER or OUT_OF_PRINT), warehouseQuantity (copies in the warehouse) and "
                    + "expectedDeliveryDate (ISO date) for a restock of 10 copies ordered today: two working days "
                    + "for IN_STOCK, fourteen days for BACKORDER, null for OUT_OF_PRINT. It never places an order. "
                    + "An unknown EAN gives the tool error \"No title with EAN <ean>\".",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public CallToolResult checkAvailability(
            @McpToolParam(description = "The title's EAN, which is its ISBN-13: 13 digits without dashes, for "
                    + "example 9780141439518.") String ean) {
        return catalog.findByEan(ean)
                .map(title -> new AvailabilityReport(title.ean(), title.availability(), title.warehouseQuantity(),
                        restockService.quote(title).map(DeliveryQuote::expectedDeliveryDate).orElse(null)))
                .map(this::success)
                .orElseGet(() -> unknownEan(ean));
    }

    private CallToolResult success(Object value) {
        return CallToolResult.builder().addTextContent(jsonMapper.writeValueAsString(value)).build();
    }

    private static CallToolResult unknownEan(String ean) {
        return CallToolResult.builder().isError(true).addTextContent("No title with EAN " + ean).build();
    }
}
