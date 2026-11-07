package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.command.CommandResult;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The catalogue end to end: real HTTP into the full application on PostgreSQL. Every test starts on an empty
 * database. Bodies are read as plain JSON maps, so a price that arrives as a JSON number never equals its string.
 */
class CatalogueIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Map<String, Object>>> JSON_ARRAY =
            new ParameterizedTypeReference<>() {
            };

    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    /** The valid genres as the problem details list them: every genre once, in declaration order. */
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
    void validBookIsAddedAndCanBeRead() {
        RestTestClient.ResponseSpec response = postAdd(payload("9780141439518", "Pride and Prejudice",
                List.of("Jane Austen"), "fiction", "9.99", 1813));

        CommandResult result = response.expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        Map<String, Object> book = getBook(result.id());
        assertThat(book.get("id")).isEqualTo(result.id().toString());
        assertThat(book.get("status")).isEqualTo("AVAILABLE");
        assertThat(book.get("price")).isEqualTo("9.99");
    }

    @Test
    void duplicateIsbnIsRejected() {
        addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);

        Map<String, Object> problem = problem(postAdd(payload("978-0-14-143951-8", "Emma",
                List.of("Jane Austen"), "fiction", "7.99", 1815)), 409);

        assertThat(problem.get("code")).isEqualTo("isbn-already-exists");
        assertThat(problem.get("title")).isEqualTo("ISBN already in catalogue");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
        assertThat(search("/books")).extracting(view -> view.get("title")).containsExactly("Pride and Prejudice");
    }

    @Test
    void invalidChecksumIsRejected() {
        Map<String, Object> problem = problem(postAdd(payload("9780141439519", "Pride and Prejudice",
                List.of("Jane Austen"), "fiction", "9.99", 1813)), 422);

        assertThat(problem.get("code")).isEqualTo("invalid-isbn");
        assertThat(problem.get("title")).isEqualTo("Invalid ISBN");
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .singleElement(InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("field", "message")
                .containsEntry("field", "isbn")
                .extractingByKey("message").asString().isNotBlank();
        assertThat(search("/books")).isEmpty();
    }

    @Test
    void pricesAreRoundedHalfEven() {
        UUID id = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "10.005",
                1813);
        assertThat(getBook(id).get("price")).isEqualTo("10.00");

        changePrice(id, "10.015").expectStatus().isAccepted().expectBody().isEmpty();

        assertThat(getBook(id).get("price")).isEqualTo("10.02");
    }

    @Test
    void priceChangeOnAnAvailableBook() {
        UUID id = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);

        changePrice(id, "12.50").expectStatus().isAccepted().expectBody().isEmpty();

        Map<String, Object> book = getBook(id);
        assertThat(book.get("price")).isEqualTo("12.50");
        assertThat(book.get("status")).isEqualTo("AVAILABLE");
    }

    @Test
    void negativePriceIsOutOfRange() {
        UUID id = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);

        Map<String, Object> onAdd = problem(postAdd(payload("9780141439600", "Emma", List.of("Jane Austen"),
                "fiction", "-5", 1815)), 422);
        Map<String, Object> onChange = problem(changePrice(id, "-0.01"), 422);

        assertThat(onAdd.get("code")).isEqualTo("price-out-of-range");
        assertThat(onChange.get("code")).isEqualTo("price-out-of-range");
        assertThat(getBook(id).get("price")).isEqualTo("9.99");
    }

    @Test
    void priceChangeOnADiscontinuedBookIsRejectedAndThePriceIsUnchanged() {
        UUID id = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);
        discontinue(id).expectStatus().isAccepted().expectBody().isEmpty();

        Map<String, Object> problem = problem(changePrice(id, "12.50"), 409);

        assertThat(problem.get("code")).isEqualTo("book-discontinued");
        assertThat(problem.get("title")).isEqualTo("Book discontinued");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
        Map<String, Object> book = getBook(id);
        assertThat(book.get("price")).isEqualTo("9.99");
        assertThat(book.get("status")).isEqualTo("DISCONTINUED");
    }

    @Test
    void searchByAuthorFragmentAndGenre() {
        // Added out of title order and out of ISBN order, so neither insertion nor ISBN order passes.
        addBook("9780141439662", "Sense and Sensibility", List.of("Jane Austen"), "fiction", "8.99", 1811);
        addBook("9780141439471", "Frankenstein", List.of("Mary Shelley"), "science-fiction", "6.99", 1818);
        addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);
        addBook("9780141439617", "The History of England", List.of("Jane Austen"), "history", "5.99", 1791);
        addBook("9780141439587", "Emma", List.of("Jane Austen"), "fiction", "7.99", 1815);
        addBook("9780141439747", "Mathilda", List.of("Mary Shelley"), "fiction", "6.49", 1819);

        List<Map<String, Object>> books = search("/books?author=austen&genre=FICTION");

        assertThat(books).extracting(view -> view.get("title"))
                .containsExactly("Emma", "Pride and Prejudice", "Sense and Sensibility");
        assertThat(books).extracting(view -> view.get("authors"))
                .containsExactly("Jane Austen", "Jane Austen", "Jane Austen");
        assertThat(books).extracting(view -> view.get("genre")).containsExactly("FICTION", "FICTION", "FICTION");
    }

    @Test
    void unknownGenreIsRejected() {
        Map<String, Object> problem = problem(restTestClient.get().uri("/books?genre=cookbooks").exchange(), 422);

        assertThat(problem.get("code")).isEqualTo("unknown-genre");
        assertThat(problem.get("title")).isEqualTo("Unknown genre");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(TEN_GENRES);
    }

    @Test
    void freeTextSearchMatchesTitleOrAuthor() {
        addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);
        addBook("9780141439471", "Frankenstein", List.of("Mary Shelley"), "science-fiction", "6.99", 1818);
        addBook("9780140296907", "Jane Austen: A Life", List.of("Claire Tomalin"), "history", "14.99", 1997);
        addBook("9780141439587", "Emma", List.of("Jane Austen"), "fiction", "7.99", 1815);

        List<Map<String, Object>> books = search("/books?q=austen");

        assertThat(books).extracting(view -> view.get("title"))
                .containsExactly("Emma", "Jane Austen: A Life", "Pride and Prejudice");
        assertThat(books).extracting(view -> view.get("authors"))
                .containsExactly("Jane Austen", "Claire Tomalin", "Jane Austen");
    }

    @Test
    void unknownBookIsNotFoundOnGet() {
        Map<String, Object> problem = problem(restTestClient.get().uri("/books/{id}", UNKNOWN_ID).exchange(), 404);

        assertBookNotFound(problem);
    }

    @Test
    void unknownBookIsNotFoundOnChangePrice() {
        Map<String, Object> problem = problem(changePrice(UNKNOWN_ID, "12.50"), 404);

        assertBookNotFound(problem);
    }

    @Test
    void unknownBookIsNotFoundOnDiscontinue() {
        Map<String, Object> problem = problem(discontinue(UNKNOWN_ID), 404);

        assertBookNotFound(problem);
    }

    @Test
    void discontinuingTwiceIsANoOp() {
        UUID id = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99", 1813);

        discontinue(id).expectStatus().isAccepted().expectBody().isEmpty();
        discontinue(id).expectStatus().isAccepted().expectBody().isEmpty();

        Map<String, Object> book = getBook(id);
        assertThat(book.get("status")).isEqualTo("DISCONTINUED");
        assertThat(book.get("price")).isEqualTo("9.99");
    }

    @Test
    void discontinuedBooksAreIncludedInSearchWithTheirStatus() {
        UUID pride = addBook("9780141439518", "Pride and Prejudice", List.of("Jane Austen"), "fiction", "9.99",
                1813);
        addBook("9780141439587", "Emma", List.of("Jane Austen"), "fiction", "7.99", 1815);
        addBook("9780141439471", "Frankenstein", List.of("Mary Shelley"), "science-fiction", "6.99", 1818);
        discontinue(pride).expectStatus().isAccepted();

        List<Map<String, Object>> books = search("/books?author=austen");

        assertThat(books).extracting(view -> view.get("title")).containsExactly("Emma", "Pride and Prejudice");
        assertThat(books).extracting(view -> view.get("status")).containsExactly("AVAILABLE", "DISCONTINUED");
    }

    @Test
    void incompleteBookIsRejectedWith400() {
        Map<String, Object> problem = problem(postAdd(Map.of("isbn", "9780141439518")), 400);

        assertThat(problem.get("code")).isEqualTo("validation-failed");
        assertThat(problem.get("title")).isEqualTo("Validation failed");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem).extractingByKey("details", InstanceOfAssertFactories.LIST)
                .extracting(detail -> (Object) ((Map<?, ?>) detail).get("field"))
                .containsExactly("authors", "genre", "price", "publicationYear", "title");
        assertThat(search("/books")).isEmpty();
    }

    @Test
    void unknownGenreOnAddIsRejected() {
        Map<String, Object> problem = problem(postAdd(payload("9780141439518", "Pride and Prejudice",
                List.of("Jane Austen"), "cookbooks", "9.99", 1813)), 422);

        assertThat(problem.get("code")).isEqualTo("unknown-genre");
        assertThat(problem.get("title")).isEqualTo("Unknown genre");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(TEN_GENRES);
        assertThat(search("/books")).isEmpty();
    }

    @Test
    void searchResultsExposeExactlyTheBookViewFields() {
        // Authors out of alphabetical order: the joined line must keep the order they were added in.
        UUID id = addBook("9780141439600", "Collected Letters", List.of("Mary Shelley", "Jane Austen"), "history",
                "12.50", 1990);

        List<Map<String, Object>> books = search("/books?q=collected");

        assertThat(books).singleElement().satisfies(view -> {
            assertThat(view.keySet()).containsExactlyInAnyOrder(
                    "id", "isbn", "title", "authors", "genre", "price", "currency", "status");
            assertThat(view).isEqualTo(Map.of(
                    "id", id.toString(),
                    "isbn", "9780141439600",
                    "title", "Collected Letters",
                    "authors", "Mary Shelley, Jane Austen",
                    "genre", "HISTORY",
                    "price", "12.50",
                    "currency", "EUR",
                    "status", "AVAILABLE"));
        });
    }

    @Test
    void bookDetailsExposeExactlyTheBookDetailViewFields() {
        // A hyphenated ISBN is stored and shown normalised; authors keep the order they were added in.
        UUID id = addBook("978-0-14-143960-0", "Collected Letters", List.of("Mary Shelley", "Jane Austen"),
                "history", "12.50", 1990);

        Map<String, Object> book = getBook(id);

        assertThat(book.keySet()).containsExactlyInAnyOrder(
                "id", "isbn", "title", "authors", "genre", "price", "currency", "status", "publicationYear");
        assertThat(book).isEqualTo(Map.of(
                "id", id.toString(),
                "isbn", "9780141439600",
                "title", "Collected Letters",
                "authors", List.of("Mary Shelley", "Jane Austen"),
                "genre", "HISTORY",
                "price", "12.50",
                "currency", "EUR",
                "status", "AVAILABLE",
                "publicationYear", 1990));
    }

    private static void assertBookNotFound(Map<String, Object> problem) {
        assertThat(problem.get("code")).isEqualTo("book-not-found");
        assertThat(problem.get("title")).isEqualTo("Book not found");
        assertThat(problem.get("detail")).asString().isNotBlank();
        assertThat(problem.get("details")).isEqualTo(List.of());
    }

    private UUID addBook(String isbn, String title, List<String> authors, String genre, String price,
            int publicationYear) {
        CommandResult result = postAdd(payload(isbn, title, authors, genre, price, publicationYear))
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
    }

    private RestTestClient.ResponseSpec changePrice(UUID id, String price) {
        return restTestClient.post().uri("/books/{id}/commands/change-price", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("price", price))
                .exchange();
    }

    private RestTestClient.ResponseSpec discontinue(UUID id) {
        return restTestClient.post().uri("/books/{id}/commands/discontinue", id).exchange();
    }

    private List<Map<String, Object>> search(String uri) {
        return restTestClient.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_ARRAY).returnResult().getResponseBody();
    }

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    private static Map<String, Object> problem(RestTestClient.ResponseSpec response, int status) {
        return response.expectStatus().isEqualTo(status)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    private RestTestClient.ResponseSpec postAdd(Map<String, Object> payload) {
        return restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange();
    }

    private static Map<String, Object> payload(String isbn, String title, List<String> authors, String genre,
            String price, int publicationYear) {
        return Map.of("isbn", isbn, "title", title, "authors", authors, "genre", genre, "price", price,
                "publicationYear", publicationYear);
    }

    private Map<String, Object> getBook(UUID id) {
        return restTestClient.get().uri("/books/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }
}
