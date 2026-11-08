package nl.openvalue.distributor.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the seed files into a {@link TitleCatalog}. Fails fast, naming the EAN, on any seed entry it cannot trust.
 */
public class CatalogLoader {

    public static final String TITLES_RESOURCE = "catalog/titles.json";
    public static final String SUBJECTS_RESOURCE = "catalog/subjects.json";

    static final int MAX_RELEASED_DAYS_AGO = 60;

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public CatalogLoader(JsonMapper jsonMapper, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public TitleCatalog loadSeed() {
        try (InputStream titles = new ClassPathResource(TITLES_RESOURCE).getInputStream();
                InputStream subjects = new ClassPathResource(SUBJECTS_RESOURCE).getInputStream()) {
            return load(titles, subjects);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the catalogue seed", e);
        }
    }

    public TitleCatalog load(InputStream titlesJson, InputStream subjectsJson) {
        LocalDate today = LocalDate.now(clock);
        Set<String> seenEans = new HashSet<>();
        List<Title> titles = new ArrayList<>();
        for (TitleSeed seed : readTitleSeeds(titlesJson)) {
            if (!seenEans.add(seed.ean())) {
                throw new IllegalStateException("Seed title " + seed.ean() + " appears more than once");
            }
            titles.add(toTitle(seed, today));
        }
        return new TitleCatalog(titles, readSubjects(subjectsJson));
    }

    List<TitleSeed> readTitleSeeds(InputStream titlesJson) {
        ObjectReader reader = strictReader(TitleSeed.class);
        List<TitleSeed> seeds = new ArrayList<>();
        for (JsonNode node : jsonMapper.readTree(titlesJson)) {
            String ean = node.path("ean").asString("(no ean)");
            try {
                seeds.add(reader.readValue(node));
            } catch (JacksonException e) {
                throw new IllegalStateException("Seed title " + ean + " cannot be read: " + e.getOriginalMessage(), e);
            }
        }
        return seeds;
    }

    List<Subject> readSubjects(InputStream subjectsJson) {
        ObjectReader reader = strictReader(Subject.class);
        List<Subject> subjects = new ArrayList<>();
        for (JsonNode node : jsonMapper.readTree(subjectsJson)) {
            subjects.add(reader.readValue(node));
        }
        subjects.sort(Comparator.comparing(Subject::code));
        return subjects;
    }

    private ObjectReader strictReader(Class<?> type) {
        return jsonMapper.readerFor(type).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static Title toTitle(TitleSeed seed, LocalDate today) {
        return new Title(seed.ean(), seed.titleName(), seed.contributors(), seed.subjectCodes(), seed.format(),
                seed.listPriceCents(), seed.currency(), seed.availability(), seed.warehouseQuantity(),
                releaseDate(seed, today));
    }

    private static LocalDate releaseDate(TitleSeed seed, LocalDate today) {
        if (seed.releaseDate() != null && seed.releasedDaysAgo() != null) {
            throw new IllegalStateException(
                    "Seed title " + seed.ean() + " has both releaseDate and releasedDaysAgo; set exactly one");
        }
        if (seed.releaseDate() != null) {
            return seed.releaseDate();
        }
        if (seed.releasedDaysAgo() == null) {
            throw new IllegalStateException(
                    "Seed title " + seed.ean() + " has neither releaseDate nor releasedDaysAgo; set exactly one");
        }
        int daysAgo = seed.releasedDaysAgo();
        if (daysAgo < 0 || daysAgo > MAX_RELEASED_DAYS_AGO) {
            throw new IllegalStateException("Seed title " + seed.ean() + " has releasedDaysAgo " + daysAgo
                    + "; it must be between 0 and " + MAX_RELEASED_DAYS_AGO);
        }
        return today.minusDays(daysAgo);
    }
}
