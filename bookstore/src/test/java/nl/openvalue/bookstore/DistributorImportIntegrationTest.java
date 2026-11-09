package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.DistributorUnavailableException;
import nl.openvalue.bookstore.domaininteraction.DistributorTitleDTO;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Importing a book from the distributor end to end: real HTTP into the full application on PostgreSQL. The
 * distributor port is the inherited mock, stubbed per test for the answer the scenario needs, so no test reaches a
 * distributor. Every test starts on an empty database. Bodies are read as plain JSON maps, so a price that arrives as
 * a JSON number never equals its string.
 */
class DistributorImportIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final String PRIDE_ISBN = "9780141439518";

    private static final List<Map<String, String>> TEN_GENRES = List.of(
            Map.of("genre", "FICTION"),
            Map.of("genre", "SCIENCE_FICTION"),
            Map.of("genre", "FANTASY"),
            Map.of("genre", "MYSTERY"),
            Map.of("genre", "ROMANCE"),
            Map.of("genre", "HISTORY"),
            Map.of("genre", "SCIENCE"),
            Map.of("genre", "TECHNOLOGY"),
            Map.of("genre", "CHILDREN"),
            Map.of("genre", "POETRY"));

    @Test
    void importWithTheRecommendedPriceAddsTheBookAndOpensAnEmptyStockItem() {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(prideAndPrejudice()));

        UUID id = importBook(Map.of("isbn", PRIDE_ISBN)).expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        assertThat(getBook(id)).isEqualTo(Map.ofEntries(
                Map.entry("id", id.toString()),
                Map.entry("isbn", "9780141439518"),
                Map.entry("title", "Pride and Prejudice"),
                Map.entry("authors", List.of("Jane Austen")),
                Map.entry("genre", "FICTION"),
                Map.entry("price", "9.99"),
                Map.entry("currency", "EUR"),
                Map.entry("status", "AVAILABLE"),
                Map.entry("publicationYear", 1813),
                Map.entry("available", 0),
                Map.entry("inStock", false)));
        assertThat(getStock(id)).isEqualTo(Map.of(
                "bookId", id.toString(),
                "onHand", 0,
                "reserved", 0,
                "available", 0,
                "reorderLevel", 3,
                "lowStock", true));
        assertRowCounts(1, 1, 1);
        verify(distributorAppService).findTitle(PRIDE_ISBN);
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void importWithAPriceAboveTheRecommendedOneAndAHyphenatedIsbnStoresTheOverrideAndTheNormalisedRequestIsbn() {
        // The request is hyphenated and the distributor answers for the normalised ISBN.
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "The Last Man", List.of("Percy Shelley", "Mary Shelley"), "SCIENCE_FICTION",
                new BigDecimal("11.99"), "EUR", 1826, true)));

        UUID id = importBook(Map.of("isbn", "978-0-14-143951-8", "price", "12.00")).expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        assertThat(getBook(id)).isEqualTo(Map.ofEntries(
                Map.entry("id", id.toString()),
                Map.entry("isbn", "9780141439518"),
                Map.entry("title", "The Last Man"),
                Map.entry("authors", List.of("Percy Shelley", "Mary Shelley")),
                Map.entry("genre", "SCIENCE_FICTION"),
                Map.entry("price", "12.00"),
                Map.entry("currency", "EUR"),
                Map.entry("status", "AVAILABLE"),
                Map.entry("publicationYear", 1826),
                Map.entry("available", 0),
                Map.entry("inStock", false)));
        assertRowCounts(1, 2, 1);
        verify(distributorAppService).findTitle("9780141439518");
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void importWithAPriceBelowTheRecommendedOneStoresTheOverride() {
        when(distributorAppService.findTitle("9780141439617")).thenReturn(Optional.of(new DistributorTitleDTO(
                "9780141439617", "The History of England", List.of("Oliver Goldsmith"), "HISTORY",
                new BigDecimal("14.99"), "EUR", 1791, true)));

        UUID id = importBook(Map.of("isbn", "9780141439617", "price", "7.50")).expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        assertThat(getBook(id)).isEqualTo(Map.ofEntries(
                Map.entry("id", id.toString()),
                Map.entry("isbn", "9780141439617"),
                Map.entry("title", "The History of England"),
                Map.entry("authors", List.of("Oliver Goldsmith")),
                Map.entry("genre", "HISTORY"),
                Map.entry("price", "7.50"),
                Map.entry("currency", "EUR"),
                Map.entry("status", "AVAILABLE"),
                Map.entry("publicationYear", 1791),
                Map.entry("available", 0),
                Map.entry("inStock", false)));
        assertRowCounts(1, 1, 1);
        verify(distributorAppService).findTitle("9780141439617");
        verifyNoMoreInteractions(distributorAppService);
    }

    /** The recommended price is 9.99; the override is below, equal to or above it, or at either end of the range. */
    @ParameterizedTest
    @CsvSource({
            "7.50, 7.50",
            "9.98, 9.98",
            "9.99, 9.99",
            "10.00, 10.00",
            "12, 12.00",
            "0.01, 0.01",
            "500.00, 500.00"
    })
    void anOverrideIsStoredWhetherItIsBelowEqualToAboveOrAtAnEndOfTheRange(String override, String storedPrice) {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(prideAndPrejudice()));

        UUID id = importBook(Map.of("isbn", PRIDE_ISBN, "price", override)).expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        assertThat(getBook(id).get("price")).isEqualTo(storedPrice);
        assertRowCounts(1, 1, 1);
    }

    @ParameterizedTest
    @CsvSource({"0", "0.00", "-0.01", "500.01"})
    void anOverrideOutsideTheRangeIsRejectedWith422AndNothingIsStored(String override) {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(prideAndPrejudice()));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN, "price", override)), 422);

        assertThat(problem.get("code")).isEqualTo("price-out-of-range");
        assertThat(problem.get("title")).isEqualTo("Price out of range");
        assertRowCounts(0, 0, 0);
    }

    /** Without an override the recommended price is used: it is stored at either end of the range. */
    @ParameterizedTest
    @CsvSource({"0.01, 0.01", "9.99, 9.99", "500.00, 500.00"})
    void aRecommendedPriceInTheRangeIsStored(String recommended, String storedPrice) {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal(recommended),
                "EUR", 1813, true)));

        UUID id = importBook(Map.of("isbn", PRIDE_ISBN)).expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody().id();

        assertThat(getBook(id).get("price")).isEqualTo(storedPrice);
    }

    @ParameterizedTest
    @CsvSource({"0.00", "500.01"})
    void aRecommendedPriceOutsideTheRangeIsRejectedWith422AndNothingIsStored(String recommended) {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal(recommended),
                "EUR", 1813, true)));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN)), 422);

        assertThat(problem.get("code")).isEqualTo("price-out-of-range");
        assertThat(problem.get("title")).isEqualTo("Price out of range");
        assertRowCounts(0, 0, 0);
    }

    @Test
    void anIsbnAlreadyInTheCatalogueIsRejectedWith409AndTheDistributorIsNotAsked() {
        addBook(PRIDE_ISBN, "Pride and Prejudice", "9.99");

        Map<String, Object> problem = problem(
                importBook(Map.of("isbn", "978-0-14-143951-8", "price", "12.00")), 409);

        assertThat(problem.get("code")).isEqualTo("isbn-already-exists");
        assertThat(problem.get("title")).isEqualTo("ISBN already in catalogue");
        assertThat(problem.get("detail"))
                .isEqualTo("A book with ISBN 9780141439518 is already in the catalogue");
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertRowCounts(1, 1, 1);
        assertThat(search("/books")).singleElement().satisfies(view -> {
            assertThat(view.get("title")).isEqualTo("Pride and Prejudice");
            assertThat(view.get("price")).isEqualTo("9.99");
        });
        verifyNoInteractions(distributorAppService);
    }

    @Test
    void anUnavailableDistributorIsReportedWith503AndNothingIsStored() {
        when(distributorAppService.findTitle(PRIDE_ISBN))
                .thenThrow(new DistributorUnavailableException(
                        "The distributor gave no usable answer for ISBN 9780141439518"));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", "978-0-14-143951-8")), 503);

        assertThat(problem.get("code")).isEqualTo("distributor-unavailable");
        assertThat(problem.get("title")).isEqualTo("Distributor unavailable");
        assertThat(problem.get("detail")).isEqualTo("The distributor gave no usable answer for ISBN 9780141439518");
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertRowCounts(0, 0, 0);
        verify(distributorAppService).findTitle("9780141439518");
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored() {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                "9789900010050", "The Last Man", List.of("Percy Shelley", "Mary Shelley"), "SCIENCE_FICTION",
                new BigDecimal("11.99"), "EUR", 1826, true)));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", "978-0-14-143951-8", "price", "12.00")), 503);

        assertThat(problem.get("code")).isEqualTo("distributor-unavailable");
        assertThat(problem.get("title")).isEqualTo("Distributor unavailable");
        assertThat(problem.get("detail")).isEqualTo("The distributor gave no usable answer for ISBN 9780141439518");
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertRowCounts(0, 0, 0);
        verify(distributorAppService).findTitle("9780141439518");
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void aTitleTheDistributorDoesNotKnowIsReportedWith404AndNothingIsStored() {
        when(distributorAppService.findTitle("9780141439617")).thenReturn(Optional.empty());

        Map<String, Object> problem = problem(importBook(Map.of("isbn", "978-0-14-143961-7")), 404);

        assertThat(problem.get("code")).isEqualTo("title-not-found-at-distributor");
        assertThat(problem.get("title")).isEqualTo("Title not found at distributor");
        assertThat(problem.get("detail")).isEqualTo("The distributor has no title with ISBN 9780141439617");
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertRowCounts(0, 0, 0);
        verify(distributorAppService).findTitle("9780141439617");
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void aTitleTheDistributorNoLongerSuppliesIsReportedWith422AndNothingIsStored() {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "Pamela", List.of("Samuel Richardson"), "ROMANCE", new BigDecimal("10.99"), "EUR", 1740,
                false)));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN, "price", "8.00")), 422);

        assertThat(problem.get("code")).isEqualTo("title-not-orderable");
        assertThat(problem.get("title")).isEqualTo("Title not orderable");
        assertThat(problem.get("detail"))
                .isEqualTo("The title with ISBN 9780141439518 cannot be ordered from the distributor");
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertRowCounts(0, 0, 0);
        verify(distributorAppService).findTitle(PRIDE_ISBN);
        verifyNoMoreInteractions(distributorAppService);
    }

    @Test
    void aNegativeOverrideIsRejectedWith422BeforeTheCatalogueAndTheDistributorAreAsked() {
        addBook(PRIDE_ISBN, "Pride and Prejudice", "9.99");

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN, "price", "-0.01")), 422);

        assertThat(problem.get("code")).isEqualTo("price-out-of-range");
        assertThat(problem.get("title")).isEqualTo("Price out of range");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .singleElement(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("field", "message")
                .containsEntry("field", "price");
        assertRowCounts(1, 1, 1);
        verifyNoInteractions(distributorAppService);
    }

    @Test
    void anInvalidIsbnIsRejectedWith422AndTheDistributorIsNotAsked() {
        Map<String, Object> problem = problem(importBook(Map.of("isbn", "9780141439519")), 422);

        assertThat(problem.get("code")).isEqualTo("invalid-isbn");
        assertThat(problem.get("title")).isEqualTo("Invalid ISBN");
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .singleElement(InstanceOfAssertFactories.MAP)
                .containsEntry("field", "isbn");
        assertRowCounts(0, 0, 0);
        verifyNoInteractions(distributorAppService);
    }

    @Test
    void aTitleWithAPublicationYearInTheFarFutureIsRejectedWith422AndNothingIsStored() {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal("9.99"), "EUR",
                9999, true)));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN)), 422);

        assertThat(problem.get("code")).isEqualTo("publication-year-out-of-range");
        assertThat(problem.get("title")).isEqualTo("Publication year out of range");
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .singleElement(InstanceOfAssertFactories.MAP)
                .containsEntry("field", "publicationYear");
        assertRowCounts(0, 0, 0);
    }

    @Test
    void aTitleWithAGenreTheBookstoreDoesNotKnowIsRejectedWith422AndNothingIsStored() {
        when(distributorAppService.findTitle(PRIDE_ISBN)).thenReturn(Optional.of(new DistributorTitleDTO(
                PRIDE_ISBN, "Pride and Prejudice", List.of("Jane Austen"), "COOKBOOKS", new BigDecimal("9.99"), "EUR",
                1813, true)));

        Map<String, Object> problem = problem(importBook(Map.of("isbn", PRIDE_ISBN)), 422);

        assertThat(problem.get("code")).isEqualTo("unknown-genre");
        assertThat(problem.get("title")).isEqualTo("Unknown genre");
        assertThat(problem.get("details")).isEqualTo(TEN_GENRES);
        assertRowCounts(0, 0, 0);
    }

    private static DistributorTitleDTO prideAndPrejudice() {
        return new DistributorTitleDTO(PRIDE_ISBN, "Pride and Prejudice", List.of("Jane Austen"), "FICTION",
                new BigDecimal("9.99"), "EUR", 1813, true);
    }

    /** Asserts the exact number of rows in the book, book_author and stock_item tables. */
    private void assertRowCounts(int books, int bookAuthors, int stockItems) {
        assertThat(count("book")).as("rows in book").isEqualTo(books);
        assertThat(count("book_author")).as("rows in book_author").isEqualTo(bookAuthors);
        assertThat(count("stock_item")).as("rows in stock_item").isEqualTo(stockItems);
    }

    private int count(String table) {
        Integer rows = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        assertThat(rows).isNotNull();
        return rows;
    }

    private RestTestClient.ResponseSpec importBook(Map<String, Object> payload) {
        return restTestClient.post().uri("/books/commands/import-from-distributor")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange();
    }

    private void addBook(String isbn, String title, String price) {
        restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("isbn", isbn, "title", title, "authors", List.of("Jane Austen"), "genre", "fiction",
                        "price", price, "publicationYear", 1813))
                .exchange()
                .expectStatus().isAccepted();
    }

    private Map<String, Object> getBook(UUID id) {
        return restTestClient.get().uri("/books/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private Map<String, Object> getStock(UUID bookId) {
        return restTestClient.get().uri("/inventory/{bookId}", bookId).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private List<Map<String, Object>> search(String uri) {
        return restTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }).returnResult().getResponseBody();
    }

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    private static Map<String, Object> problem(RestTestClient.ResponseSpec response, int status) {
        return response.expectStatus().isEqualTo(status)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }
}
