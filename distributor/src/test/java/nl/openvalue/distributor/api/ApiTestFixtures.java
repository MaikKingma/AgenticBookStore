package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import nl.openvalue.distributor.catalog.Availability;
import nl.openvalue.distributor.catalog.Contributor;
import nl.openvalue.distributor.catalog.ContributorRole;
import nl.openvalue.distributor.catalog.Format;
import nl.openvalue.distributor.catalog.Subject;
import nl.openvalue.distributor.catalog.Title;
import nl.openvalue.distributor.catalog.TitleCatalog;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared data and helpers of the two API slice tests.
 *
 * <p>The catalogue holds 25 titles. They are inserted in an order that follows neither the title name nor the EAN,
 * and the EANs are scrambled against the title names, so a controller or service that sorts by the wrong key or
 * counts the wrong offset cannot return the asserted EANs by accident. The two "Twin Peaks of Ash" titles are
 * inserted with the higher EAN first.
 */
final class ApiTestFixtures {

    static final String UNKNOWN_EAN = "9780000000002";

    /** Anvil Tales, the first title by name. */
    static final String IN_STOCK_EAN = "9789900010008";

    /** Bridge of Glass. */
    static final String BACKORDER_EAN = "9789900010015";

    /** Garden Wall. */
    static final String OUT_OF_PRINT_EAN = "9789900010025";

    /** Moon Cartographer: two contributors of different roles, two codes, a non-default format and price. */
    static final String MOON_EAN = "9789900010017";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String AUTHOR = "Fixture Author";

    private ApiTestFixtures() {
    }

    static TitleCatalog catalog() {
        return new TitleCatalog(titles(), subjects());
    }

    /** Subjects in code order, with labels whose alphabetical order differs from the code order. */
    static List<Subject> subjects() {
        return List.of(
                new Subject("COM051000", "Programming"),
                new Subject("FIC009000", "Fantasy"),
                new Subject("FIC028000", "Science Fiction"),
                new Subject("HIS002000", "Aviation history"));
    }

    static Title moonCartographer() {
        return new Title(MOON_EAN, "Moon Cartographer",
                List.of(new Contributor("Nora Vale", ContributorRole.AUTHOR),
                        new Contributor("Ines Marsh", ContributorRole.TRANSLATOR)),
                List.of("FIC028000", "FIC009000"), Format.HARDCOVER, 1850, "EUR", Availability.IN_STOCK, 77,
                LocalDate.of(2021, 10, 1));
    }

    private static List<Title> titles() {
        return List.of(
                row("9789900010023", "Twin Peaks of Ash", Availability.IN_STOCK, "POE000000"),
                row("9789900010025", "Garden Wall", Availability.OUT_OF_PRINT, "POE000000"),
                row("9789900010024", "Nightfall Archive", Availability.IN_STOCK, "HIS002000"),
                row("9789900010015", "Bridge of Glass", Availability.BACKORDER, "SCI028000"),
                row("9789900010001", "Zephyr Notes", Availability.IN_STOCK, "POE000000"),
                row("9789900010003", "Kestrel Run", Availability.IN_STOCK, "FIC028020"),
                row("9789900010002", "River Ledger", Availability.IN_STOCK, "HIS002000"),
                row("9789900010004", "Dune Runner", Availability.IN_STOCK, "FIC028000"),
                row("9789900010014", "Iron Orchard", Availability.IN_STOCK, "FIC028000"),
                row("9789900010012", "Velvet Compass", Availability.IN_STOCK, "HIS002000", "FIC028010"),
                row("9789900010008", "Anvil Tales", Availability.IN_STOCK, "FIC022000"),
                row("9789900010016", "Twin Peaks of Ash", Availability.IN_STOCK, "POE000000"),
                row("9789900010011", "Echo Station", Availability.IN_STOCK, "FIC009000", "FIC028010"),
                custom("9789900010020", "Quiet Engine", Availability.IN_STOCK,
                        List.of(new Contributor(AUTHOR, ContributorRole.AUTHOR),
                                new Contributor("Mira Harborne", ContributorRole.TRANSLATOR)),
                        "COM051000"),
                row("9789900010010", "Lantern Row", Availability.BACKORDER, "JUV000000"),
                custom("9789900010022", "Cedar Hollow", Availability.IN_STOCK,
                        List.of(new Contributor("Zora Wellesley", ContributorRole.AUTHOR)), "HIS002000"),
                row("9789900010019", "Willow Bank", Availability.IN_STOCK, "JUV000000"),
                row("9789900010007", "Harbor Lights", Availability.IN_STOCK, "COM051000"),
                row("9789900010006", "Orbit Runner", Availability.BACKORDER, "FIC028000"),
                row("9789900010009", "Salt Road", Availability.BACKORDER, "FIC022000"),
                row("9789900010021", "Jade Harbor", Availability.IN_STOCK, "FIC027000"),
                row("9789900010005", "Umber Runway", Availability.IN_STOCK, "FIC009000"),
                row("9789900010018", "Fable of Salt", Availability.IN_STOCK, "FIC009000"),
                row("9789900010013", "Paper Kingdom", Availability.OUT_OF_PRINT, "FIC009000"),
                moonCartographer());
    }

    private static Title row(String ean, String titleName, Availability availability, String... subjectCodes) {
        return custom(ean, titleName, availability, List.of(new Contributor(AUTHOR, ContributorRole.AUTHOR)),
                subjectCodes);
    }

    private static Title custom(String ean, String titleName, Availability availability,
            List<Contributor> contributors, String... subjectCodes) {
        int warehouseQuantity = availability == Availability.IN_STOCK ? 40 : 0;
        return new Title(ean, titleName, contributors, List.of(subjectCodes), Format.PAPERBACK, 999, "EUR",
                availability, warehouseQuantity, LocalDate.of(2001, 1, 1));
    }

    static Map<String, Object> readMap(String json) {
        return JSON.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    static List<Object> readList(String json) {
        return JSON.readValue(json, new TypeReference<List<Object>>() {
        });
    }

    /**
     * Asserts the whole error contract of one response: the status, JSON (not problem+json) content type, exactly the
     * keys {@code errorCode} and {@code message}, the error code and a message that is not blank.
     */
    static void expectErrorEnvelope(ResultActions result, int expectedStatus, String expectedErrorCode)
            throws Exception {
        MvcResult mvcResult = result
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        Map<String, Object> body = readMap(mvcResult.getResponse().getContentAsString());
        assertThat(body).containsOnlyKeys("errorCode", "message");
        assertThat(body.get("errorCode")).isEqualTo(expectedErrorCode);
        assertThat(body.get("message")).asString().isNotBlank();
    }
}
