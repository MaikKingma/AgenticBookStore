package nl.openvalue.distributor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class CatalogLoaderTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    @Test
    void releasedDaysAgoIsConvertedRelativeToTheClock() {
        CatalogLoader loader = loaderAt("2000-06-15T10:00:00Z");

        TitleCatalog catalog = loader.load(json("[" + newRelease("9789900090014", 5) + "]"), json("[]"));

        assertThat(catalog.findByEan("9789900090014")).get()
                .extracting(Title::releaseDate)
                .isEqualTo(LocalDate.of(2000, 6, 10));
    }

    @Test
    void releasedDaysAgoUsesTheClockZone() {
        // 23:30 UTC on 14 January is already 00:30 on 15 January in Amsterdam (CET, UTC+1).
        CatalogLoader loader = loaderAt("2000-01-14T23:30:00Z");

        TitleCatalog catalog = loader.load(json("[" + newRelease("9789900090014", 0) + "]"), json("[]"));

        assertThat(catalog.findByEan("9789900090014")).get()
                .extracting(Title::releaseDate)
                .isEqualTo(LocalDate.of(2000, 1, 15));
    }

    @Test
    void seedEntryIsMappedFieldByField() {
        String entry = "{\"ean\": \"9789900090021\", \"titleName\": \"The Copper Lantern\","
                + " \"contributors\": [{\"name\": \"Walter Ypsilon\", \"role\": \"AUTHOR\"},"
                + " {\"name\": \"Anna Brook\", \"role\": \"ILLUSTRATOR\"}],"
                + " \"subjectCodes\": [\"SCI000000\", \"FIC028000\"], \"format\": \"HARDCOVER\","
                + " \"listPriceCents\": 4321, \"currency\": \"GBP\", \"availability\": \"BACKORDER\","
                + " \"warehouseQuantity\": 17, \"releaseDate\": \"1923-04-05\"}";

        TitleCatalog catalog = loaderAt("2000-06-15T10:00:00Z").load(json("[" + entry + "]"), json("[]"));

        assertThat(catalog.all()).containsExactly(new Title(
                "9789900090021",
                "The Copper Lantern",
                List.of(new Contributor("Walter Ypsilon", ContributorRole.AUTHOR),
                        new Contributor("Anna Brook", ContributorRole.ILLUSTRATOR)),
                List.of("SCI000000", "FIC028000"),
                Format.HARDCOVER,
                4321,
                "GBP",
                Availability.BACKORDER,
                17,
                LocalDate.of(1923, 4, 5)));
    }

    @Test
    void entryWithBothDatesFailsNamingTheEan() {
        String titles = "[" + newRelease("9789900090014", 3) + ", "
                + entry("9789900090021", "\"releaseDate\": \"1900-01-01\", \"releasedDaysAgo\": 3") + "]";

        assertThatThrownBy(() -> loaderAt("2000-06-15T10:00:00Z").load(json(titles), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090021")
                .hasMessageNotContaining("9789900090014");
    }

    @Test
    void entryWithNoDateFails() {
        String titles = "[" + classic("9789900090014") + ", " + entry("9789900090038", "") + "]";

        assertThatThrownBy(() -> loaderAt("2000-06-15T10:00:00Z").load(json(titles), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090038")
                .hasMessageNotContaining("9789900090014");
    }

    @Test
    void releasedDaysAgoAbove60OrBelow0Fails() {
        CatalogLoader loader = loaderAt("2000-06-15T10:00:00Z");

        TitleCatalog accepted = loader.load(
                json("[" + newRelease("9789900090014", 0) + ", " + newRelease("9789900090021", 60) + "]"), json("[]"));
        assertThat(accepted.all()).extracting(Title::releaseDate)
                .containsExactly(LocalDate.of(2000, 6, 15), LocalDate.of(2000, 4, 16));

        assertThatThrownBy(() -> loader.load(json("[" + newRelease("9789900090038", -1) + "]"), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090038");
        assertThatThrownBy(() -> loader.load(json("[" + newRelease("9789900090045", 61) + "]"), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090045");
    }

    @Test
    void duplicateEanFails() {
        String titles = "[" + classic("9789900090014") + ", " + classic("9789900090021") + ", "
                + newRelease("9789900090014", 7) + "]";

        assertThatThrownBy(() -> loaderAt("2000-06-15T10:00:00Z").load(json(titles), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090014")
                .hasMessageNotContaining("9789900090021");
    }

    @Test
    void unknownJsonPropertyFails() {
        // A lenient mapper: only the loader's own strict reader can reject the typo.
        JsonMapper lenient = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        CatalogLoader loader = new CatalogLoader(lenient,
                Clock.fixed(Instant.parse("2000-06-15T10:00:00Z"), AMSTERDAM));
        String titles = "[" + classic("9789900090014") + ", "
                + entry("9789900090052", "\"releaseDate\": \"1900-01-01\", \"warehouseQuantiy\": 4") + "]";

        assertThatThrownBy(() -> loader.load(json(titles), json("[]")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9789900090052")
                .hasMessageContaining("warehouseQuantiy")
                .hasMessageNotContaining("9789900090014");
    }

    @Test
    void subjectsAreLoadedSortedByCode() {
        // File order FIC, POE, COM is sorted in neither column; label order would be POE, COM, FIC.
        String subjects = "[{\"code\": \"FIC028000\", \"label\": \"Fiction / Science Fiction\"},"
                + " {\"code\": \"POE000000\", \"label\": \"A Poetry Shelf\"},"
                + " {\"code\": \"COM000000\", \"label\": \"Computers / General\"}]";

        TitleCatalog catalog = loaderAt("2000-06-15T10:00:00Z").load(json("[]"), json(subjects));

        assertThat(catalog.subjects()).containsExactly(
                new Subject("COM000000", "Computers / General"),
                new Subject("FIC028000", "Fiction / Science Fiction"),
                new Subject("POE000000", "A Poetry Shelf"));
    }

    private static CatalogLoader loaderAt(String instant) {
        return new CatalogLoader(JsonMapper.builder().build(), Clock.fixed(Instant.parse(instant), AMSTERDAM));
    }

    private static InputStream json(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String newRelease(String ean, int releasedDaysAgo) {
        return entry(ean, "\"releasedDaysAgo\": " + releasedDaysAgo);
    }

    private static String classic(String ean) {
        return entry(ean, "\"releaseDate\": \"1900-01-01\"");
    }

    private static String entry(String ean, String extraFields) {
        return "{\"ean\": \"" + ean + "\", \"titleName\": \"Fixture " + ean + "\","
                + " \"contributors\": [{\"name\": \"Fixture Author\", \"role\": \"AUTHOR\"}],"
                + " \"subjectCodes\": [\"FIC004000\"], \"format\": \"PAPERBACK\", \"listPriceCents\": 1000,"
                + " \"currency\": \"EUR\", \"availability\": \"IN_STOCK\", \"warehouseQuantity\": 3"
                + (extraFields.isEmpty() ? "" : ", " + extraFields) + "}";
    }
}
