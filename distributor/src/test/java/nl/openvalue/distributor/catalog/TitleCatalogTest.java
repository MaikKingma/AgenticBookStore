package nl.openvalue.distributor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TitleCatalogTest {

    private static final Contributor TOLSTOY = new Contributor("Leo Tolstoy", ContributorRole.AUTHOR);

    @Test
    void qMatchesTitleCaseInsensitively() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Pride and Prejudice", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000028", "Emma", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000035", "Persuasion", Availability.IN_STOCK, List.of("FIC004000")));

        assertThat(eans(catalog.search("PREJUD", null, null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search("prejud", null, null, 0, 10))).containsExactly("9789900000011");
    }

    @Test
    void qIsAContainsMatch() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Pride and Prejudice", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000028", "Emma", Availability.IN_STOCK, List.of("FIC004000")));

        assertThat(eans(catalog.search("judice", null, null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search("and Pre", null, null, 0, 10))).containsExactly("9789900000011");
    }

    @Test
    void qMatchesAnyContributorIncludingTranslators() {
        Title warAndPeace = new Title("9789900000011", "War and Peace",
                List.of(TOLSTOY,
                        new Contributor("Edith Editor", ContributorRole.EDITOR),
                        new Contributor("Constance Garnett", ContributorRole.TRANSLATOR)),
                List.of("FIC019000"), Format.PAPERBACK, 1299, "EUR", Availability.IN_STOCK, 3,
                LocalDate.of(1869, 1, 1));
        Title emma = title("9789900000028", "Emma", Availability.IN_STOCK, List.of("FIC004000"));
        TitleCatalog catalog = catalogOf(emma, warAndPeace);

        assertThat(eans(catalog.search("garnett", null, null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search("EDITH", null, null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search("tolstoy", null, null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search("jane austen", null, null, 0, 10))).containsExactly("9789900000028");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void blankQIsIgnored(String q) {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Pride and Prejudice", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000028", "Emma", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000035", "Persuasion", Availability.IN_STOCK, List.of("FIC004000")));

        TitlePage page = catalog.search(q, null, null, 0, 10);

        assertThat(eans(page)).containsExactly("9789900000028", "9789900000035", "9789900000011");
        assertThat(page.total()).isEqualTo(3);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void blankSubjectIsIgnored(String subject) {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Pride and Prejudice", Availability.IN_STOCK, List.of("FIC004000")),
                title("9789900000028", "Cosmos", Availability.IN_STOCK, List.of("SCI004000")),
                title("9789900000035", "Persuasion", Availability.IN_STOCK, List.of("FIC004000")));

        TitlePage page = catalog.search(null, subject, null, 0, 10);

        assertThat(eans(page)).containsExactly("9789900000028", "9789900000035", "9789900000011");
        assertThat(page.total()).isEqualTo(3);
    }

    @Test
    void subjectMatchesACodePrefix() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("FIC027000")),
                title("9789900000035", "Charlie", Availability.IN_STOCK, List.of("HIS037000")));

        assertThat(eans(catalog.search(null, "FIC", null, 0, 10))).containsExactly("9789900000011", "9789900000028");
        assertThat(eans(catalog.search(null, "FIC028", null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search(null, "FIC028000", null, 0, 10))).containsExactly("9789900000011");
    }

    @Test
    void subjectIsAPrefixNotAContainsMatch() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("FIC027000")));

        assertThat(catalog.search(null, "028", null, 0, 10).items()).isEmpty();
        assertThat(catalog.search(null, "IC028", null, 0, 10).items()).isEmpty();
        assertThat(catalog.search(null, "028", null, 0, 10).total()).isZero();
    }

    @Test
    void subjectPrefixIsCaseInsensitive() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("HIS037000")));

        assertThat(eans(catalog.search(null, "fic028", null, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search(null, "Fic", null, 0, 10))).containsExactly("9789900000011");
    }

    @Test
    void subjectMatchesAnyOfTheTitlesCodes() {
        TitleCatalog catalog = catalogOf(
                // only the middle of three codes matches SCI
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000", "SCI002000", "HIS003000")),
                // only the first of two codes matches
                title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("SCI005000", "FIC001000")),
                // only the last of two codes matches
                title("9789900000035", "Charlie", Availability.IN_STOCK, List.of("FIC001000", "SCI009000")),
                // no code matches
                title("9789900000042", "Delta", Availability.IN_STOCK, List.of("FIC001000", "HIS003000", "POE001000")));

        TitlePage page = catalog.search(null, "SCI", null, 0, 10);

        assertThat(eans(page)).containsExactly("9789900000011", "9789900000028", "9789900000035");
        assertThat(page.total()).isEqualTo(3);
    }

    @Test
    void availabilityFiltersExactly() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000028", "Bravo", Availability.BACKORDER, List.of("FIC001000")),
                title("9789900000035", "Charlie", Availability.OUT_OF_PRINT, List.of("FIC001000")),
                title("9789900000042", "Delta", Availability.BACKORDER, List.of("FIC001000")));

        assertThat(eans(catalog.search(null, null, Availability.BACKORDER, 0, 10)))
                .containsExactly("9789900000028", "9789900000042");
        assertThat(eans(catalog.search(null, null, Availability.OUT_OF_PRINT, 0, 10)))
                .containsExactly("9789900000035");
        assertThat(eans(catalog.search(null, null, Availability.IN_STOCK, 0, 10))).containsExactly("9789900000011");
        assertThat(eans(catalog.search(null, null, null, 0, 10)))
                .containsExactly("9789900000011", "9789900000028", "9789900000035", "9789900000042");
    }

    @Test
    void filtersCombineWithAnd() {
        TitleCatalog catalog = catalogOf(
                // matches all three filters
                title("9789900000011", "Storm Tales", Availability.IN_STOCK, List.of("FIC001000")),
                // only q excludes it
                title("9789900000028", "Calm Tales", Availability.IN_STOCK, List.of("FIC001000")),
                // only the subject excludes it
                title("9789900000035", "Storm Poems", Availability.IN_STOCK, List.of("POE001000")),
                // only the availability excludes it
                title("9789900000042", "Storm Notes", Availability.BACKORDER, List.of("FIC001000")));

        assertThat(eans(catalog.search("storm", "FIC", Availability.IN_STOCK, 0, 10)))
                .containsExactly("9789900000011");
        assertThat(eans(catalog.search("storm", "FIC", null, 0, 10)))
                .containsExactly("9789900000042", "9789900000011");
        assertThat(eans(catalog.search("storm", null, Availability.IN_STOCK, 0, 10)))
                .containsExactly("9789900000035", "9789900000011");
        assertThat(eans(catalog.search(null, "FIC", Availability.IN_STOCK, 0, 10)))
                .containsExactly("9789900000028", "9789900000011");
    }

    @Test
    void resultsAreSortedByTitleNameThenEan() {
        // Inserted sorted in no column; each tie group is inserted opposite to the asserted order.
        TitleCatalog catalog = catalogOf(
                title("9789900000066", "Dune", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000034", "Banana", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000029", "Cherry", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000058", "apple", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000042", "Dune", Availability.IN_STOCK, List.of("FIC028000")),
                title("9789900000010", "cherry", Availability.IN_STOCK, List.of("FIC028000")));

        TitlePage page = catalog.search(null, null, null, 0, 10);

        // apple before Banana only when case is ignored; cherry (lower EAN) before Cherry although a
        // case-sensitive order puts Cherry first; the two Dune titles by ascending EAN.
        assertThat(page.items()).extracting(Title::ean).containsExactly(
                "9789900000058", "9789900000034", "9789900000010", "9789900000029", "9789900000042",
                "9789900000066");
        assertThat(page.items()).extracting(Title::titleName)
                .containsExactly("apple", "Banana", "cherry", "Cherry", "Dune", "Dune");
    }

    @Test
    void pagingReturnsTheRequestedSlice() {
        TitleCatalog catalog = catalogOfSevenBooks();

        TitlePage second = catalog.search(null, null, null, 1, 3);
        assertThat(eans(second)).containsExactly("9789900000042", "9789900000059", "9789900000066");
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(3);
        assertThat(second.total()).isEqualTo(7);

        TitlePage partialLast = catalog.search(null, null, null, 3, 2);
        assertThat(eans(partialLast)).containsExactly("9789900000073");
        assertThat(partialLast.page()).isEqualTo(3);
        assertThat(partialLast.size()).isEqualTo(2);

        TitlePage first = catalog.search(null, null, null, 0, 2);
        assertThat(eans(first)).containsExactly("9789900000011", "9789900000028");
    }

    @Test
    void totalCountsAllMatchesNotJustThePage() {
        List<Title> titles = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            titles.add(title("97899000001" + i + "0", "Match " + (char) ('A' + i), Availability.IN_STOCK,
                    List.of("FIC001000")));
        }
        for (int i = 0; i < 3; i++) {
            titles.add(title("97899000002" + i + "0", "Other " + (char) ('A' + i), Availability.IN_STOCK,
                    List.of("FIC001000")));
        }
        TitleCatalog catalog = new TitleCatalog(titles, List.of());

        TitlePage firstPage = catalog.search("match", null, null, 0, 5);
        assertThat(firstPage.items()).hasSize(5);
        assertThat(firstPage.total()).isEqualTo(7);
        assertThat(catalog.all()).hasSize(10);

        TitlePage secondPage = catalog.search("match", null, null, 1, 5);
        assertThat(secondPage.items()).hasSize(2);
        assertThat(secondPage.total()).isEqualTo(7);
    }

    @Test
    void pageBeyondTheEndIsEmptyWithTheFullTotal() {
        TitleCatalog catalog = catalogOf(
                title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000035", "Charlie", Availability.IN_STOCK, List.of("FIC001000")));

        TitlePage beyond = catalog.search(null, null, null, 5, 2);
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.total()).isEqualTo(3);
        assertThat(beyond.page()).isEqualTo(5);
        assertThat(beyond.size()).isEqualTo(2);

        TitlePage huge = catalog.search(null, null, null, Integer.MAX_VALUE, 50);
        assertThat(huge.items()).isEmpty();
        assertThat(huge.total()).isEqualTo(3);
        assertThat(huge.page()).isEqualTo(Integer.MAX_VALUE);
        assertThat(huge.size()).isEqualTo(50);
    }

    @Test
    void negativePageIsRejected() {
        TitleCatalog catalog = catalogOf(title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")));

        assertThat(catalog.search(null, null, null, 0, 1).items()).hasSize(1);
        assertThatThrownBy(() -> catalog.search(null, null, null, -1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void sizeBelowOneIsRejected() {
        TitleCatalog catalog = catalogOf(title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")));

        assertThat(catalog.search(null, null, null, 0, 1).items()).hasSize(1);
        assertThatThrownBy(() -> catalog.search(null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0");
        assertThatThrownBy(() -> catalog.search(null, null, null, 0, -3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-3");
    }

    @Test
    void findByEanFindsAKnownEan() {
        Title alpha = title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000"));
        TitleCatalog catalog = catalogOf(alpha, title("9789900000028", "Bravo", Availability.IN_STOCK, List.of()));

        assertThat(catalog.findByEan("9789900000011")).contains(alpha);
    }

    @Test
    void findByEanIsEmptyForAnUnknownEan() {
        TitleCatalog catalog = catalogOf(title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")));

        assertThat(catalog.findByEan("9780000000002")).isEmpty();
    }

    @Test
    void findByEanOfNullIsEmpty() {
        TitleCatalog catalog = catalogOf(title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000")));

        assertThat(catalog.findByEan(null)).isEmpty();
    }

    @Test
    void catalogIsUnaffectedByLaterChangesToTheInputList() {
        Title alpha = title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000"));
        Title bravo = title("9789900000028", "Bravo", Availability.IN_STOCK, List.of("FIC001000"));
        Title intruder = title("9789900000035", "Intruder", Availability.IN_STOCK, List.of("FIC001000"));
        List<Title> input = new ArrayList<>(List.of(alpha, bravo));
        TitleCatalog catalog = new TitleCatalog(input, List.of());

        input.add(intruder);
        input.remove(alpha);

        assertThat(catalog.all()).containsExactly(alpha, bravo);
        assertThat(catalog.findByEan("9789900000011")).contains(alpha);
        assertThat(catalog.findByEan("9789900000035")).isEmpty();
        assertThat(eans(catalog.search(null, null, null, 0, 10))).containsExactly("9789900000011", "9789900000028");
    }

    @Test
    void allIsUnmodifiable() {
        Title alpha = title("9789900000011", "Alpha", Availability.IN_STOCK, List.of("FIC001000"));
        TitleCatalog catalog = catalogOf(alpha);

        assertThatThrownBy(() -> catalog.all().add(alpha)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalog.all().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(catalog.all()).containsExactly(alpha);
    }

    @Test
    void subjectsAreUnaffectedByLaterChangesToTheInputList() {
        Subject fiction = new Subject("FIC000000", "Fiction");
        Subject science = new Subject("SCI000000", "Science");
        List<Subject> input = new ArrayList<>(List.of(fiction));
        TitleCatalog catalog = new TitleCatalog(List.of(), input);

        input.add(science);

        assertThat(catalog.subjects()).containsExactly(fiction);
    }

    @Test
    void subjectsAreUnmodifiable() {
        Subject fiction = new Subject("FIC000000", "Fiction");
        TitleCatalog catalog = new TitleCatalog(List.of(), List.of(fiction));

        assertThatThrownBy(() -> catalog.subjects().add(fiction)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(catalog.subjects()).containsExactly(fiction);
    }

    private static TitleCatalog catalogOfSevenBooks() {
        // Names Book A .. Book G sort in this order; inserted in a scrambled order.
        return catalogOf(
                title("9789900000059", "Book E", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000011", "Book A", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000073", "Book G", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000042", "Book D", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000028", "Book B", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000066", "Book F", Availability.IN_STOCK, List.of("FIC001000")),
                title("9789900000035", "Book C", Availability.IN_STOCK, List.of("FIC001000")));
    }

    private static TitleCatalog catalogOf(Title... titles) {
        return new TitleCatalog(List.of(titles), List.of());
    }

    private static List<String> eans(TitlePage page) {
        return page.items().stream().map(Title::ean).toList();
    }

    private static Title title(String ean, String name, Availability availability, List<String> subjectCodes) {
        return new Title(ean, name, List.of(new Contributor("Jane Austen", ContributorRole.AUTHOR)), subjectCodes,
                Format.PAPERBACK, 999, "EUR", availability, 10, LocalDate.of(1813, 1, 28));
    }
}
