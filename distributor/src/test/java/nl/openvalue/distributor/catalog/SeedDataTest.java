package nl.openvalue.distributor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guards the seed files against the rules of the distributor spec and the invariants of their consumers: the
 * bookstore import (spec 09 seeds the shared ISBNs) and the anti-corruption layer (spec 06 maps the first subject
 * code by longest prefix).
 */
class SeedDataTest {

    private static final String SHARED_ISBNS_RESOURCE = "catalog/bookstore-shared-isbns.txt";
    private static final String PRIDE_AND_PREJUDICE = "9780141439518";

    /** A fixed clock far from the run date, so nothing here depends on the day the build runs. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2000-06-15T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));

    /** Spec 06 mapping keys, matched by longest prefix on the FIRST subject code. */
    private static final List<String> SPEC_06_MAPPING_KEYS =
            List.of("FIC028", "FIC009", "FIC022", "FIC027", "FIC", "HIS", "SCI", "COM", "JUV", "POE");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final CatalogLoader loader = new CatalogLoader(jsonMapper, CLOCK);

    @Test
    void everyEanIsAValidIsbn13() {
        assertThat(seeds()).filteredOn(seed -> !isValidIsbn13(seed.ean())).extracting(TitleSeed::ean).isEmpty();
    }

    @Test
    void isbnChecksumHelperRejectsAWrongCheckDigit() {
        assertThat(isValidIsbn13("9780141439518")).isTrue();
        assertThat(isValidIsbn13("9780000000002")).isTrue();
        assertThat(isValidIsbn13("9780141439600")).isTrue();
        assertThat(isValidIsbn13("9789900010012")).isTrue();

        assertThat(isValidIsbn13("9780141439519")).as("wrong check digit").isFalse();
        assertThat(isValidIsbn13("4006381333931")).as("valid EAN-13 outside 978 and 979").isFalse();
        assertThat(isValidIsbn13("978014143951")).as("12 digits").isFalse();
        assertThat(isValidIsbn13("97801414395180")).as("14 digits").isFalse();
        assertThat(isValidIsbn13("978014143951X")).as("a letter").isFalse();
    }

    @Test
    void eansAreUnique() {
        List<String> eans = seeds().stream().map(TitleSeed::ean).toList();

        assertThat(eans).doesNotHaveDuplicates();
    }

    @Test
    void seedHasAbout150TitlesWithAbout120ClassicsAnd30NewReleases() {
        List<TitleSeed> seeds = seeds();

        assertThat(seeds).hasSizeBetween(140, 160);
        assertThat(seeds.stream().filter(SeedDataTest::isClassic).count()).isBetween(110L, 130L);
        assertThat(seeds.stream().filter(SeedDataTest::isNewRelease).count()).isBetween(25L, 35L);
    }

    @Test
    void everyEntryHasExactlyOneOfReleaseDateOrReleasedDaysAgo() {
        assertThat(seeds())
                .filteredOn(seed -> (seed.releaseDate() == null) == (seed.releasedDaysAgo() == null))
                .extracting(TitleSeed::ean)
                .isEmpty();
    }

    @Test
    void releasedDaysAgoIsBetween0And60() {
        List<Integer> daysAgo = seeds().stream().filter(SeedDataTest::isNewRelease)
                .map(TitleSeed::releasedDaysAgo).toList();

        assertThat(daysAgo).isNotEmpty().allSatisfy(days -> assertThat(days).isBetween(0, 60));
        assertThat(daysAgo).as("the spread includes both ends").contains(0, 60);
    }

    @Test
    void classicsAreReleasedBetween1450And1955() {
        assertThat(seeds()).filteredOn(SeedDataTest::isClassic)
                .isNotEmpty()
                .allSatisfy(seed -> assertThat(seed.releaseDate().getYear()).as(seed.ean()).isBetween(1450, 1955));
    }

    @Test
    void atLeastTenTitlesHaveATranslatorOrIllustrator() {
        long count = titles().stream()
                .filter(title -> title.contributors().stream().anyMatch(contributor ->
                        contributor.role() == ContributorRole.TRANSLATOR
                                || contributor.role() == ContributorRole.ILLUSTRATOR))
                .count();

        assertThat(count).isGreaterThanOrEqualTo(10);
    }

    @Test
    void atLeastFiveTitlesAreBackorder() {
        assertThat(titles().stream().filter(title -> title.availability() == Availability.BACKORDER).count())
                .isGreaterThanOrEqualTo(5);
    }

    @Test
    void atLeastFiveTitlesAreOutOfPrint() {
        assertThat(titles().stream().filter(title -> title.availability() == Availability.OUT_OF_PRINT).count())
                .isGreaterThanOrEqualTo(5);
    }

    @Test
    void atLeastThreeTitlesAreEbookOnly() {
        // Each EAN has exactly one format, so "only an EBOOK format" means format EBOOK.
        assertThat(titles().stream().filter(title -> title.format() == Format.EBOOK).count())
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void sharedIsbnFileListsExactly20DistinctIsbnsOnePerLine() {
        String content = new String(bytes(SHARED_ISBNS_RESOURCE), StandardCharsets.US_ASCII);
        List<String> lines = content.lines().toList();

        assertThat(content).as("ends with a newline").endsWith("\n");
        assertThat(lines).hasSize(20).doesNotHaveDuplicates()
                .allSatisfy(line -> assertThat(line).as("one 13-digit ISBN, nothing else").matches("[0-9]{13}"))
                .contains(PRIDE_AND_PREJUDICE);
    }

    @Test
    void everySharedIsbnExistsIsInStockAndHasOnlyAuthorContributors() {
        TitleCatalog catalog = catalog();

        assertThat(sharedIsbns()).hasSize(20).allSatisfy(isbn -> {
            Optional<Title> title = catalog.findByEan(isbn);
            assertThat(title).as(isbn).isPresent();
            assertThat(title.get().availability()).as(isbn).isEqualTo(Availability.IN_STOCK);
            assertThat(title.get().contributors()).as(isbn).isNotEmpty()
                    .extracting(Contributor::role).containsOnly(ContributorRole.AUTHOR);
        });
    }

    @Test
    void sharedIsbnsAreClassics() {
        List<TitleSeed> seeds = seeds();

        assertThat(sharedIsbns()).hasSize(20).allSatisfy(isbn -> {
            TitleSeed seed = seeds.stream().filter(candidate -> candidate.ean().equals(isbn)).findFirst().orElseThrow();
            assertThat(seed.releasedDaysAgo()).as(isbn).isNull();
            assertThat(seed.releaseDate()).as(isbn).isNotNull();
            assertThat(seed.releaseDate().getYear()).as(isbn).isBetween(1450, 1955);
        });
    }

    @Test
    void prideAndPrejudiceMatchesTheSpecExample() {
        assertThat(catalog().findByEan("9780141439518")).contains(new Title(
                "9780141439518",
                "Pride and Prejudice",
                List.of(new Contributor("Jane Austen", ContributorRole.AUTHOR)),
                List.of("FIC004000"),
                Format.PAPERBACK,
                999,
                "EUR",
                Availability.IN_STOCK,
                240,
                LocalDate.of(1813, 1, 28)));
    }

    @Test
    void everySubjectCodeHasALabelAndEveryLabelIsUsed() {
        List<Subject> fileOrder = rawSubjects();
        Set<String> labelledCodes = fileOrder.stream().map(Subject::code).collect(Collectors.toCollection(TreeSet::new));
        Set<String> usedCodes = titles().stream().flatMap(title -> title.subjectCodes().stream())
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(fileOrder).as("subjects.json is sorted by code")
                .isSortedAccordingTo(Comparator.comparing(Subject::code));
        assertThat(fileOrder).extracting(Subject::code).doesNotHaveDuplicates();
        assertThat(fileOrder).allSatisfy(subject -> assertThat(subject.label()).as(subject.code()).isNotBlank());
        assertThat(usedCodes).as("codes used by titles").isEqualTo(labelledCodes);
        assertThat(titles()).allSatisfy(title -> assertThat(title.subjectCodes()).as(title.ean()).hasSizeBetween(1, 3));
    }

    @Test
    void subjectFamiliesCoverTheSpec06Mapping() {
        Set<String> keysHitByFirstCode = titles().stream()
                .map(title -> spec06MappingKey(title.subjectCodes().getFirst()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        assertThat(keysHitByFirstCode).containsExactlyInAnyOrderElementsOf(SPEC_06_MAPPING_KEYS);
    }

    @Test
    void everySubjectCodeIsInASpecFamily() {
        String specFamily = "(FIC|HIS|SCI|COM|JUV|POE)[0-9]{6}";

        assertThat(rawSubjects()).extracting(Subject::code)
                .allSatisfy(code -> assertThat(code).matches(specFamily));
        assertThat(titles()).flatExtracting(Title::subjectCodes)
                .allSatisfy(code -> assertThat(code).matches(specFamily));
    }

    @Test
    void atLeastSixScienceFictionTitles() {
        long count = titles().stream()
                .filter(title -> title.subjectCodes().stream().anyMatch(code -> code.startsWith("FIC028")))
                .count();

        assertThat(count).isGreaterThanOrEqualTo(6);
    }

    @Test
    void scenarioUnknownEanIsAbsent() {
        assertThat(catalog().findByEan("9780000000002")).isEmpty();
    }

    @Test
    void janeAustenAndMaryShelleyArePresent() {
        Contributor maryShelley = new Contributor("Mary Shelley", ContributorRole.AUTHOR);
        Contributor janeAusten = new Contributor("Jane Austen", ContributorRole.AUTHOR);

        assertThat(titles()).filteredOn(title -> title.titleName().equals("Frankenstein"))
                .singleElement()
                .satisfies(title -> assertThat(title.contributors()).contains(maryShelley));
        assertThat(titles()).filteredOn(title -> title.contributors().contains(janeAusten))
                .extracting(Title::ean)
                .contains(PRIDE_AND_PREJUDICE)
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void availabilityAndWarehouseQuantityAgree() {
        assertThat(titles()).allSatisfy(title -> {
            assertThat(title.warehouseQuantity()).as(title.ean()).isNotNegative();
            assertThat(title.availability() == Availability.IN_STOCK).as(title.ean())
                    .isEqualTo(title.warehouseQuantity() > 0);
        });
    }

    @Test
    void everyTitleIsImportableByTheBookstore() {
        assertThat(titles()).allSatisfy(title -> {
            assertThat(title.currency()).as(title.ean()).isEqualTo("EUR");
            assertThat(title.listPriceCents()).as(title.ean()).isBetween(1, 50000);
            assertThat(title.titleName()).as(title.ean()).isNotBlank().hasSizeLessThanOrEqualTo(200)
                    .isEqualTo(title.titleName().strip());
            assertThat(title.contributors()).as(title.ean()).allSatisfy(contributor -> {
                assertThat(contributor.role()).isNotNull();
                assertThat(contributor.name()).isNotBlank().hasSizeLessThanOrEqualTo(120)
                        .isEqualTo(contributor.name().strip());
            });
            assertThat(title.contributors().stream().filter(contributor -> contributor.role() == ContributorRole.AUTHOR))
                    .as(title.ean() + " authors").hasSizeBetween(1, 5);
            assertThat(title.format()).as(title.ean()).isNotNull();
            assertThat(title.availability()).as(title.ean()).isNotNull();
            assertThat(title.releaseDate().getYear()).as(title.ean()).isGreaterThanOrEqualTo(1450);
        });
    }

    @Test
    void seedFilesAreAscii() {
        for (String resource : List.of(CatalogLoader.TITLES_RESOURCE, CatalogLoader.SUBJECTS_RESOURCE,
                SHARED_ISBNS_RESOURCE)) {
            byte[] bytes = bytes(resource);
            assertThat(bytes).as(resource).isNotEmpty();
            for (int i = 0; i < bytes.length; i++) {
                assertThat(bytes[i] & 0xFF).as(resource + " byte " + i).isLessThan(0x80);
            }
        }
    }

    static boolean isValidIsbn13(String value) {
        if (value == null || !value.matches("97[89][0-9]{10}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = value.charAt(i) - '0';
            sum += i % 2 == 0 ? digit : 3 * digit;
        }
        int checkDigit = (10 - sum % 10) % 10;
        return checkDigit == value.charAt(12) - '0';
    }

    private static Optional<String> spec06MappingKey(String code) {
        return SPEC_06_MAPPING_KEYS.stream()
                .filter(code::startsWith)
                .max(Comparator.comparingInt(String::length));
    }

    private static boolean isClassic(TitleSeed seed) {
        return seed.releaseDate() != null;
    }

    private static boolean isNewRelease(TitleSeed seed) {
        return seed.releasedDaysAgo() != null;
    }

    private List<TitleSeed> seeds() {
        try (InputStream in = new ClassPathResource(CatalogLoader.TITLES_RESOURCE).getInputStream()) {
            return loader.readTitleSeeds(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TitleCatalog catalog() {
        return loader.loadSeed();
    }

    private List<Title> titles() {
        return catalog().all();
    }

    private List<Subject> rawSubjects() {
        try (InputStream in = new ClassPathResource(CatalogLoader.SUBJECTS_RESOURCE).getInputStream()) {
            return jsonMapper.readValue(in, new TypeReference<List<Subject>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> sharedIsbns() {
        return new String(bytes(SHARED_ISBNS_RESOURCE), StandardCharsets.US_ASCII).lines().toList();
    }

    private static byte[] bytes(String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
