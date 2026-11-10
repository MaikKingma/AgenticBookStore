package nl.openvalue.bookstore.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.DistributorUnavailableException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotFoundAtDistributorException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotOrderableException;
import nl.openvalue.bookstore.domaininteraction.DistributorTitleDTO;
import nl.openvalue.bookstore.domaininteraction.RestockConfirmationDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contract tests of the distributor adapter. Every distributor answer is a file captured from the real distributor
 * (see {@code contracts/distributor/README.md}); bodies no capture holds are built here from a captured file and
 * named as variants. A new case is one line: {@code stubTitle(ean, "title-<ean>.json")} and an assertion.
 */
@ExtendWith(OutputCaptureExtension.class)
class DistributorAppServiceImplTest {

    private static final String BASE_URL = "http://distributor.test";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_OF_KEYS = new TypeReference<>() {
    };

    private MockRestServiceServer server;
    private DistributorAppServiceImpl distributor;

    @BeforeEach
    void bindMockServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        distributor = new DistributorAppServiceImpl(builder.build());
    }

    @AfterEach
    void verifyEveryExpectedRequestWasSent() {
        server.verify();
    }

    @Test
    void translatesPrideAndPrejudiceIntoATitleDTO() {
        stubTitle("9780141439518", "title-9780141439518.json");

        DistributorTitleDTO title = distributor.findTitle("9780141439518").orElseThrow();

        assertThat(title.isbn()).isEqualTo("9780141439518");
        assertThat(title.title()).isEqualTo("Pride and Prejudice");
        assertThat(title.authors()).containsExactly("Jane Austen");
        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(title.recommendedPrice()).isEqualTo(new BigDecimal("9.99"));
        assertThat(title.currency()).isEqualTo("EUR");
        assertThat(title.publicationYear()).isEqualTo(1813);
        assertThat(title.orderable()).isTrue();
    }

    @Test
    void translatesTheLastManAsTheSecondValueOfEveryField() {
        stubTitle("9789900010050", "title-9789900010050.json");

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.isbn()).isEqualTo("9789900010050");
        assertThat(title.title()).isEqualTo("The Last Man");
        assertThat(title.authors()).containsExactly("Mary Shelley");
        assertThat(title.genre()).isEqualTo("SCIENCE_FICTION");
        assertThat(title.recommendedPrice()).isEqualTo(new BigDecimal("11.99"));
        assertThat(title.currency()).isEqualTo("EUR");
        assertThat(title.publicationYear()).isEqualTo(1826);
        assertThat(title.orderable()).isTrue();
    }

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource({
            "9789900010050, SCIENCE_FICTION",
            "9789900010791, FANTASY",
            "9789900010159, MYSTERY",
            "9789900010029, ROMANCE",
            "9789900010043, FICTION",
            "9789900010616, HISTORY",
            "9789900011163, SCIENCE",
            "9789900011187, TECHNOLOGY",
            "9789900010821, CHILDREN",
            "9789900010210, POETRY"
    })
    void mapsTheFirstSubjectCodeToABookstoreGenreThatParses(String ean, Genre expected, CapturedOutput output) {
        stubTitle(ean, "title-" + ean + ".json");

        DistributorTitleDTO title = distributor.findTitle(ean).orElseThrow();

        assertThat(title.genre()).isEqualTo(expected.name());
        assertThat(Genre.parse(title.genre())).isEqualTo(expected);
        assertThat(output.getOut()).doesNotContain("WARN");
    }

    @Test
    void genreTableAndFallbackCoverEveryBookstoreGenre() {
        Set<String> producible = new HashSet<>(DistributorAppServiceImpl.GENRES_BY_SUBJECT_PREFIX.values());
        producible.add(DistributorAppServiceImpl.FALLBACK_GENRE);

        assertThat(producible).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(Genre.values()).map(Genre::name).toList());
        assertThat(producible).allSatisfy(genre -> assertThat(Genre.parse(genre).name()).isEqualTo(genre));
    }

    @Test
    void prideAndPrejudiceLogsNoWarning(CapturedOutput output) {
        stubTitle("9780141439518", "title-9780141439518.json");

        distributor.findTitle("9780141439518");

        assertThat(output.getOut()).doesNotContain("WARN");
    }

    @Test
    void unmappedFirstSubjectCodeFallsBackToFictionWithOneWarning(CapturedOutput output) {
        stubTitle("9789900010050", "derived-title-unmapped-subject.json");

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").containsOnlyOnce("XYZ000000")
                .contains("No bookstore genre for subject code XYZ000000 of EAN 9789900010050, using FICTION");
    }

    @Test
    void subjectCodeHoldingAPrefixOnlyAfterItsStartFallsBackToFiction(CapturedOutput output) {
        stubTitleBody("9789900010050", variant("title-9789900010050.json",
                body -> body.put("subjectCodes", List.of("XYZFIC028"))));

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").containsOnlyOnce("XYZFIC028")
                .contains("No bookstore genre for subject code XYZFIC028 of EAN 9789900010050, using FICTION");
    }

    @Test
    void nullSubjectCodesFallBackToFictionWithOneWarning(CapturedOutput output) {
        stubTitleBody("9789900010050", variant("title-9789900010050.json", body -> body.put("subjectCodes", null)));

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("No bookstore genre for subject code null of EAN 9789900010050, using FICTION");
    }

    @Test
    void emptySubjectCodesFallBackToFictionWithOneWarning(CapturedOutput output) {
        stubTitleBody("9789900010050", variant("title-9789900010050.json",
                body -> body.put("subjectCodes", List.of())));

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("No bookstore genre for subject code null of EAN 9789900010050, using FICTION");
    }

    @Test
    void nullSubjectCodeAfterTheFirstIsNotRead(CapturedOutput output) {
        stubTitleBody("9789900010050", variant("title-9789900010050.json",
                body -> body.put("subjectCodes", Arrays.asList("FIC028000", null))));

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("SCIENCE_FICTION");
        assertThat(output.getOut()).doesNotContain("WARN");
    }

    @Test
    void nullFirstSubjectCodeIsNotSkippedToTheNextOne(CapturedOutput output) {
        stubTitleBody("9789900010050", variant("title-9789900010050.json",
                body -> body.put("subjectCodes", Arrays.asList(null, "FIC028000"))));

        DistributorTitleDTO title = distributor.findTitle("9789900010050").orElseThrow();

        assertThat(title.genre()).isEqualTo("FICTION");
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("No bookstore genre for subject code null of EAN 9789900010050, using FICTION");
    }

    @Test
    void roundPriceKeepsScaleTwo() {
        stubTitle("9780141439518", "derived-title-round-price.json");

        DistributorTitleDTO title = distributor.findTitle("9780141439518").orElseThrow();

        assertThat(title.recommendedPrice()).isEqualTo(new BigDecimal("12.00"));
    }

    @Test
    void negativeListPriceCentsKeepTheirSign() {
        stubTitleBody("9780141439518", variant("title-9780141439518.json",
                body -> body.put("listPriceCents", -999)));

        DistributorTitleDTO title = distributor.findTitle("9780141439518").orElseThrow();

        assertThat(title.recommendedPrice()).isEqualTo(new BigDecimal("-9.99"));
    }

    @Test
    void keepsTheAuthorsInTheDistributorsOrder() {
        stubTitle("9789900020080", "title-9789900020080.json");

        DistributorTitleDTO title = distributor.findTitle("9789900020080").orElseThrow();

        assertThat(title.authors()).containsExactly("Dagny Vellacourt", "Emeric Stroud-Halvorsen");
        assertThat(title.publicationYear()).isEqualTo(2026);
    }

    @Test
    void keepsReversedAuthorsReversed() {
        stubTitle("9789900020080", "derived-title-authors-reversed.json");

        DistributorTitleDTO title = distributor.findTitle("9789900020080").orElseThrow();

        assertThat(title.authors()).containsExactly("Emeric Stroud-Halvorsen", "Dagny Vellacourt");
    }

    @Test
    void keepsOnlyTheAuthorOfATitleWithATranslatorAndAnIllustrator() {
        stubTitle("9789900010395", "title-9789900010395.json");

        DistributorTitleDTO title = distributor.findTitle("9789900010395").orElseThrow();

        assertThat(title.title()).isEqualTo("The Divine Comedy: Inferno");
        assertThat(title.authors()).containsExactly("Dante Alighieri");
        assertThat(title.authors()).doesNotContain("Henry Wadsworth Longfellow", "Gustave Dore");
    }

    @Test
    void outOfPrintTitleIsNotOrderable() {
        stubTitle("9789900010678", "title-9789900010678.json");

        assertThat(distributor.findTitle("9789900010678").orElseThrow().orderable()).isFalse();
    }

    @Test
    void backorderedTitleIsStillOrderable() {
        stubTitle("9789900010272", "title-9789900010272.json");

        assertThat(distributor.findTitle("9789900010272").orElseThrow().orderable()).isTrue();
    }

    @Test
    void titleWithoutAvailabilityIsOrderable() {
        stubTitleBody("9780141439518", variant("title-9780141439518.json", body -> body.remove("availability")));

        assertThat(distributor.findTitle("9780141439518").orElseThrow().orderable()).isTrue();
    }

    @Test
    void titlePricedInAnotherCurrencyIsNotOrderable() {
        stubTitle("9780141439518", "derived-title-usd-currency.json");

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(TitleNotOrderableException.class)
                .satisfies(e -> assertNotOrderable((TitleNotOrderableException) e, "9780141439518"));
    }

    @Test
    void titleUnknownToTheDistributorIsEmpty() {
        expectTitleRequest("9780000000002").andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body(contract("title-not-found-404.json")));

        assertThat(distributor.findTitle("9780000000002")).isEmpty();
    }

    @Test
    void isbnIsSentAsAnEncodedPathVariable() {
        server.expect(requestTo("http://distributor.test/api/titles/978%2F0141439518"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body(contract("title-not-found-404.json")));

        assertThat(distributor.findTitle("978/0141439518")).isEmpty();
    }

    @Test
    void titleTheDistributorRefusesToSupplyIsNotOrderable() {
        expectTitleRequest("9789900010678").andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .contentType(MediaType.APPLICATION_JSON).body(contract("restock-not-orderable-response.json")));

        assertThatThrownBy(() -> distributor.findTitle("9789900010678"))
                .isExactlyInstanceOf(TitleNotOrderableException.class)
                .satisfies(e -> assertNotOrderable((TitleNotOrderableException) e, "9789900010678"));
    }

    @Test
    void confirmedRestockIsNotBackordered() {
        expectRestockRequest("restock-confirmed-request.json").andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON).body(contract("restock-confirmed-response.json")));

        RestockConfirmationDTO restock = distributor.requestRestock("9780141439518", 10, "workshop-order-1");

        assertThat(restock.restockOrderId()).isEqualTo("7597c6da-0041-473d-861a-d4b84c7fb204");
        assertThat(restock.quantity()).isEqualTo(10);
        assertThat(restock.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(restock.backordered()).isFalse();
    }

    @Test
    void backorderedRestockIsBackordered() {
        expectRestockRequest("restock-backordered-request.json").andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON).body(contract("restock-backordered-response.json")));

        RestockConfirmationDTO restock = distributor.requestRestock("9789900010272", 5, "workshop-order-2");

        assertThat(restock.restockOrderId()).isEqualTo("207e15be-cf0e-47da-90e8-60396068690f");
        assertThat(restock.quantity()).isEqualTo(5);
        assertThat(restock.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(restock.backordered()).isTrue();
    }

    @Test
    void restockOfATitleUnknownToTheDistributorIsTitleNotFound() {
        expectRestockRequest("restock-title-not-found-request.json").andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body(contract("restock-title-not-found-response.json")));

        assertThatThrownBy(() -> distributor.requestRestock("9780000000002", 5, "workshop-order-4"))
                .isExactlyInstanceOf(TitleNotFoundAtDistributorException.class)
                .satisfies(e -> {
                    TitleNotFoundAtDistributorException notFound = (TitleNotFoundAtDistributorException) e;
                    assertThat(notFound.code()).isEqualTo("title-not-found-at-distributor");
                    assertThat(notFound.title()).isEqualTo("Title not found at distributor");
                    assertThat(notFound.getMessage()).isEqualTo("The distributor has no title with ISBN 9780000000002");
                });
    }

    @Test
    void restockTheDistributorRefusesIsNotOrderable() {
        expectRestockRequest("restock-not-orderable-request.json").andRespond(
                withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body(contract("restock-not-orderable-response.json")));

        assertThatThrownBy(() -> distributor.requestRestock("9789900010678", 5, "workshop-order-3"))
                .isExactlyInstanceOf(TitleNotOrderableException.class)
                .satisfies(e -> assertNotOrderable((TitleNotOrderableException) e, "9789900010678"));
    }

    /** Every failure the translation table does not name, for both methods. */
    static Stream<Arguments> failures() {
        List<Failure> failures = List.of(
                new Failure("captured 503", "status 503", call -> withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON).body(contract("unavailable-503.json"))),
                new Failure("500 without a body", "status 500", call -> withServerError()),
                new Failure("read timeout", "ResourceAccessException",
                        call -> withException(new SocketTimeoutException("Read timed out"))),
                new Failure("connection refused", "ResourceAccessException",
                        call -> withException(new ConnectException("Connection refused"))),
                new Failure("captured 400 INVALID_REQUEST", "status 400",
                        call -> withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("restock-invalid-request-response.json"))),
                new Failure("variant 405 with the TITLE_NOT_FOUND body", "status 405",
                        call -> withStatus(HttpStatus.METHOD_NOT_ALLOWED).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("title-not-found-404.json"))),
                new Failure("variant 415 with the NOT_ORDERABLE body", "status 415",
                        call -> withStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("restock-not-orderable-response.json"))),
                new Failure("variant 429 with the 503 body", "status 429",
                        call -> withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("unavailable-503.json"))),
                new Failure("captured unknown-path 404", "status 404", call -> withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON).body(contract("unknown-path-404.json"))),
                new Failure("variant 404 with an empty body", "status 404",
                        call -> withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)),
                new Failure("variant 404 as text/html", "status 404", call -> withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML).body("<html><body>TITLE_NOT_FOUND</body></html>")),
                new Failure("variant 404 with malformed JSON", "status 404", call -> withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON).body(truncated(contract("title-not-found-404.json")))),
                new Failure("variant 422 with another errorCode", "status 422",
                        call -> withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("restock-invalid-request-response.json"))),
                new Failure("variant 422 with an empty body", "status 422",
                        call -> withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)),
                new Failure("variant 422 with malformed JSON", "status 422",
                        call -> withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                                .body(truncated(contract("restock-not-orderable-response.json")))),
                new Failure("variant 2xx with malformed JSON", "RestClientException",
                        call -> withSuccess(truncated(contract(call.successFile)), MediaType.APPLICATION_JSON)),
                new Failure("variant 2xx with an empty body", "is unusable: no body", call -> withSuccess()),
                new Failure("variant 2xx as text/html", "UnknownContentTypeException",
                        call -> withSuccess(contract(call.successFile), MediaType.TEXT_HTML)),
                new Failure("variant 2xx with an unparseable date", "is not a date",
                        call -> withSuccess(variant(call.successFile, body -> body.put(call.dateKey, "2026-02-30")),
                                MediaType.APPLICATION_JSON)));
        return Arrays.stream(Call.values())
                .flatMap(call -> failures.stream().map(failure -> Arguments.of(call, failure)));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("failures")
    void everyOtherFailureIsDistributorUnavailableWithOneWarning(Call call, Failure failure, CapturedOutput output) {
        expect(call).andRespond(failure.response().apply(call));

        assertThatThrownBy(() -> call.invoke(distributor))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains(failure.warning());
    }

    @Test
    void unavailableWarningNamesTheStatusButNotTheBodysMessage(CapturedOutput output) {
        expectTitleRequest("9780141439518").andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body(contract("unavailable-503.json")));

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(DistributorUnavailableException.class);
        assertThat(output.getOut())
                .contains("status 503")
                .doesNotContain("The distributor is temporarily unavailable, try again later")
                .doesNotContain("TEMPORARILY_UNAVAILABLE");
    }

    @Test
    void unavailableDetailNamesTheRequestedIsbn(CapturedOutput output) {
        expectTitleRequest("9789900010050").andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body(contract("unavailable-503.json")));

        assertThatThrownBy(() -> distributor.findTitle("9789900010050"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .hasMessage("The distributor gave no usable answer for ISBN 9789900010050")
                .satisfies(e -> {
                    DistributorUnavailableException unavailable = (DistributorUnavailableException) e;
                    assertThat(unavailable.code()).isEqualTo("distributor-unavailable");
                    assertThat(unavailable.title()).isEqualTo("Distributor unavailable");
                });
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("Distributor answered status 503 for EAN 9789900010050");
    }

    @ParameterizedTest(name = "title without {0}")
    @ValueSource(strings = {"ean", "titleName", "contributors", "listPriceCents", "currency", "releaseDate"})
    void titleMissingARequiredKeyIsDistributorUnavailable(String key, CapturedOutput output) {
        stubTitleBody("9780141439518", variant("title-9780141439518.json", body -> body.remove(key)));

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains("is unusable: no " + key);
    }

    @ParameterizedTest(name = "restock answer without {0}")
    @ValueSource(strings = {"restockOrderId", "quantity", "status", "expectedDeliveryDate"})
    void restockAnswerMissingARequiredKeyIsDistributorUnavailable(String key, CapturedOutput output) {
        expectRestockRequest("restock-confirmed-request.json").andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(variant("restock-confirmed-response.json", body -> body.remove(key))));

        assertThatThrownBy(() -> distributor.requestRestock("9780141439518", 10, "workshop-order-1"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains("is unusable: no " + key);
    }

    @ParameterizedTest(name = "null contributor {0}")
    @ValueSource(strings = {"alone", "first", "last", "middle"})
    void titleWithANullContributorIsDistributorUnavailable(String position, CapturedOutput output) {
        stubTitleBody("9780141439518", variant("title-9780141439518.json",
                body -> body.put("contributors", withBrokenContributor(body, position, null))));

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains("is unusable: null contributor");
    }

    @ParameterizedTest(name = "author without a name {0}")
    @ValueSource(strings = {"alone", "first", "last", "middle"})
    void authorWithoutANameIsDistributorUnavailable(String position, CapturedOutput output) {
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("name", null);
        author.put("role", "AUTHOR");
        stubTitleBody("9780141439518", variant("title-9780141439518.json",
                body -> body.put("contributors", withBrokenContributor(body, position, author))));

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains("is unusable: no author name");
    }

    @Test
    void fractionalListPriceCentsIsDistributorUnavailable(CapturedOutput output) {
        stubTitleBody("9780141439518", variant("title-9780141439518.json",
                body -> body.put("listPriceCents", 9.99)));

        assertThatThrownBy(() -> distributor.findTitle("9780141439518"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("is unusable: listPriceCents 9.99 is not a whole number");
    }

    @Test
    void restockAnswerWithAnUnknownStatusIsDistributorUnavailable(CapturedOutput output) {
        expectRestockRequest("restock-confirmed-request.json").andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(variant("restock-confirmed-response.json", body -> body.put("status", "SHIPPED"))));

        assertThatThrownBy(() -> distributor.requestRestock("9780141439518", 10, "workshop-order-1"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").contains("is unusable: unknown status SHIPPED");
    }

    @Test
    void fractionalRestockQuantityIsDistributorUnavailable(CapturedOutput output) {
        expectRestockRequest("restock-confirmed-request.json").andRespond(withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(variant("restock-confirmed-response.json", body -> body.put("quantity", 5.5))));

        assertThatThrownBy(() -> distributor.requestRestock("9780141439518", 10, "workshop-order-1"))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e));
        assertThat(output.getOut()).containsOnlyOnce(" WARN ")
                .contains("is unusable: quantity 5.5 is not a whole number");
    }

    @Test
    void payloadRecordsArePrivate() {
        List<Class<?>> records = Arrays.stream(DistributorAppServiceImpl.class.getDeclaredClasses())
                .filter(Class::isRecord)
                .toList();

        assertThat(records).isNotEmpty().allSatisfy(record ->
                assertThat(Modifier.isPrivate(record.getModifiers())).as(record.getName()).isTrue());
    }

    @Test
    void payloadRecordComponentsAreReferenceTypes() {
        List<RecordComponent> components = Arrays.stream(DistributorAppServiceImpl.class.getDeclaredClasses())
                .filter(Class::isRecord)
                .flatMap(record -> Arrays.stream(record.getRecordComponents()))
                .toList();

        assertThat(components).isNotEmpty().allSatisfy(component ->
                assertThat(component.getType().isPrimitive()).as(component.toString()).isFalse());
    }

    @Test
    void everyFieldOfTheAdapterAndItsPropertiesIsFinal() {
        List<Field> fields = Stream.of(DistributorAppServiceImpl.class, DistributorProperties.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredFields()))
                .toList();

        assertThat(fields).isNotEmpty().allSatisfy(field ->
                assertThat(Modifier.isFinal(field.getModifiers())).as(field.toString()).isTrue());
    }

    @Test
    void genreTableIsUnmodifiable() {
        assertThatThrownBy(() -> DistributorAppServiceImpl.GENRES_BY_SUBJECT_PREFIX.put("XYZ", "FICTION"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    static Stream<ForeignAnswer> foreignTitleAnswers() {
        return Stream.of(
                new ForeignAnswer("another seed title", "9789900010050", contract("title-9780141439518.json")),
                new ForeignAnswer("last digit differs", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "9789900010051"))),
                new ForeignAnswer("leading space", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", " 9789900010050"))),
                new ForeignAnswer("trailing space", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "9789900010050 "))),
                new ForeignAnswer("EAN with add-on", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "978990001005090000"))),
                new ForeignAnswer("hyphenated", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "978-99-0001-005-0"))),
                new ForeignAnswer("proper prefix of the request", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "978990001005"))),
                new ForeignAnswer("proper suffix of the request", "9789900010050", variant("title-9789900010050.json",
                        body -> body.put("ean", "789900010050"))),
                new ForeignAnswer("foreign and priced in USD", "9789900010050",
                        contract("derived-title-usd-currency.json")),
                new ForeignAnswer("foreign and unmapped subject code", "9780141439518",
                        contract("derived-title-unmapped-subject.json")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("foreignTitleAnswers")
    void aTitleAnswerForAnotherEanIsUnusableWithOneWarning(ForeignAnswer answer, CapturedOutput output) {
        stubTitleBody(answer.requested(), answer.body());

        assertThatThrownBy(() -> distributor.findTitle(answer.requested()))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .hasMessage("The distributor gave no usable answer for ISBN " + answer.requested());
        List<String> warnings = Arrays.stream(output.getOut().split("\\R"))
                .filter(line -> line.contains("WARN"))
                .toList();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst())
                .contains("Distributor answer for EAN " + answer.requested() + " is unusable");
        assertThat(output.getOut()).doesNotContain("No bookstore genre");
    }

    static Stream<CausedFailure> causedFailures() {
        return Stream.of(
                new CausedFailure("findTitle read timeout", Call.FIND_TITLE,
                        withException(new SocketTimeoutException("Read timed out")),
                        cause -> assertThat(cause).isExactlyInstanceOf(ResourceAccessException.class)
                                .hasRootCauseInstanceOf(SocketTimeoutException.class)),
                new CausedFailure("findTitle connection refused", Call.FIND_TITLE,
                        withException(new ConnectException("Connection refused")),
                        cause -> assertThat(cause).isExactlyInstanceOf(ResourceAccessException.class)
                                .hasRootCauseInstanceOf(ConnectException.class)),
                new CausedFailure("findTitle captured 503", Call.FIND_TITLE,
                        withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("unavailable-503.json")),
                        cause -> assertThat(cause).isInstanceOf(RestClientResponseException.class)
                                .satisfies(e -> assertThat(((RestClientResponseException) e).getStatusCode().value())
                                        .isEqualTo(503))),
                new CausedFailure("requestRestock captured 503", Call.REQUEST_RESTOCK,
                        withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                                .body(contract("unavailable-503.json")),
                        cause -> assertThat(cause).isInstanceOf(RestClientResponseException.class)
                                .satisfies(e -> assertThat(((RestClientResponseException) e).getStatusCode().value())
                                        .isEqualTo(503))),
                new CausedFailure("findTitle releaseDate that is no date", Call.FIND_TITLE,
                        withSuccess(variant("title-9780141439518.json", body -> body.put("releaseDate", "1813-13-45")),
                                MediaType.APPLICATION_JSON),
                        cause -> assertThat(cause).isExactlyInstanceOf(DateTimeParseException.class)),
                new CausedFailure("findTitle fractional listPriceCents", Call.FIND_TITLE,
                        withSuccess(variant("title-9780141439518.json", body -> body.put("listPriceCents", 999.5)),
                                MediaType.APPLICATION_JSON),
                        cause -> assertThat(cause).isExactlyInstanceOf(ArithmeticException.class)),
                new CausedFailure("requestRestock fractional quantity", Call.REQUEST_RESTOCK,
                        withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                                .body(variant("restock-confirmed-response.json", body -> body.put("quantity", 5.5))),
                        cause -> assertThat(cause).isExactlyInstanceOf(ArithmeticException.class)),
                new CausedFailure("requestRestock expectedDeliveryDate that is no date", Call.REQUEST_RESTOCK,
                        withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                                .body(variant("restock-confirmed-response.json",
                                        body -> body.put("expectedDeliveryDate", "2026-11-31"))),
                        cause -> assertThat(cause).isExactlyInstanceOf(DateTimeParseException.class)),
                new CausedFailure("guard: title without ean has no exception to keep", Call.FIND_TITLE,
                        withSuccess(variant("title-9780141439518.json", body -> body.remove("ean")),
                                MediaType.APPLICATION_JSON),
                        cause -> assertThat(cause).isNull()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("causedFailures")
    void keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable(CausedFailure failure) {
        expect(failure.call()).andRespond(failure.response());

        assertThatThrownBy(() -> failure.call().invoke(distributor))
                .isExactlyInstanceOf(DistributorUnavailableException.class)
                .satisfies(e -> assertUnavailable((DistributorUnavailableException) e))
                .satisfies(e -> failure.assertCause().accept(e.getCause()));
    }

    static Stream<LoggedValue> loggedDistributorValues() {
        String requestedPride = "9780141439518";
        String requestedLastMan = "9789900010050";
        String answer = "Distributor answer for EAN";
        String genre = "No bookstore genre for subject code";
        return Stream.of(
                new LoggedValue("restock status with CR LF", true, requestedPride,
                        variant("restock-confirmed-response.json",
                                body -> body.put("status", "SHIPPED\r\nWARN forged status")),
                        true, answer, "SHIPPED\\r\\nWARN forged status"),
                new LoggedValue("title releaseDate with LF", false, requestedPride,
                        variant("title-9780141439518.json", body -> body.put("releaseDate", "1813\nWARN forged date")),
                        true, answer, "1813\\nWARN forged date"),
                new LoggedValue("restock expectedDeliveryDate with CR", true, requestedPride,
                        variant("restock-confirmed-response.json",
                                body -> body.put("expectedDeliveryDate", "2026\rWARN forged date")),
                        true, answer, "2026\\rWARN forged date"),
                new LoggedValue("subject code with LF", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\nWARN forged subject"))),
                        false, genre, "XYZ\\nWARN forged subject"),
                new LoggedValue("foreign ean with LF", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("ean", requestedPride + "\nWARN forged ean")),
                        true, answer, requestedPride + "\\nWARN forged ean"),
                new LoggedValue("subject code with U+2028 line separator", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\u2028forged subject"))),
                        false, genre, "XYZ\\u2028forged subject"),
                new LoggedValue("subject code with U+2029 paragraph separator", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\u2029forged subject"))),
                        false, genre, "XYZ\\u2029forged subject"),
                new LoggedValue("subject code with U+0085 next line", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\u0085forged subject"))),
                        false, genre, "XYZ\\u0085forged subject"),
                new LoggedValue("subject code with a vertical tab", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\u000Bforged subject"))),
                        false, genre, "XYZ\\u000Bforged subject"),
                new LoggedValue("subject code with an escape character", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\u001Bforged subject"))),
                        false, genre, "XYZ\\u001Bforged subject"),
                new LoggedValue("subject code with a tab", false, requestedLastMan,
                        variant("title-9789900010050.json",
                                body -> body.put("subjectCodes", List.of("XYZ\tforged subject"))),
                        false, genre, "XYZ\\u0009forged subject"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loggedDistributorValues")
    void neutralisesLineBreaksInLoggedDistributorValues(LoggedValue value, CapturedOutput output) {
        Runnable call;
        if (value.restock()) {
            expectRestockRequest("restock-confirmed-request.json").andRespond(withStatus(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON).body(value.body()));
            call = () -> distributor.requestRestock(value.requested(), 10, "workshop-order-1");
        } else {
            stubTitleBody(value.requested(), value.body());
            call = () -> distributor.findTitle(value.requested());
        }

        if (value.fails()) {
            assertThatThrownBy(call::run).isExactlyInstanceOf(DistributorUnavailableException.class);
        } else {
            call.run();
        }

        List<String> lines = Arrays.asList(output.getOut().split("\\R"));
        List<String> forged = lines.stream().filter(line -> line.contains("forged")).toList();
        assertThat(forged).hasSize(1);
        assertThat(forged.getFirst()).contains("WARN").contains(value.prefix()).contains(value.escaped());
        assertThat(lines.stream().filter(line -> line.contains("WARN")).toList()).hasSize(1);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** One failure whose caught exception must survive as the cause, and what that cause must look like. */
    record CausedFailure(String name, Call call, ResponseCreator response, Consumer<Throwable> assertCause) {

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A distributor answer with a marker ({@code forged}) in the value under test. {@code prefix} is the adapter's
     * own wording that must share the one line the marker sits on, and {@code escaped} the value as it must be
     * logged.
     */
    record LoggedValue(String name, boolean restock, String requested, String body, boolean fails, String prefix,
            String escaped) {

        @Override
        public String toString() {
            return name;
        }
    }

    /** A 200 title body whose {@code ean} is not the requested one, and the EAN the adapter is asked for. */
    record ForeignAnswer(String name, String requested, String body) {

        @Override
        public String toString() {
            return name;
        }
    }

    /** The two port methods, each with the request it sends and the committed 2xx body it expects back. */
    enum Call {
        FIND_TITLE("title-9780141439518.json", "releaseDate"),
        REQUEST_RESTOCK("restock-confirmed-response.json", "expectedDeliveryDate");

        final String successFile;
        final String dateKey;

        Call(String successFile, String dateKey) {
            this.successFile = successFile;
            this.dateKey = dateKey;
        }

        Object invoke(DistributorAppServiceImpl distributor) {
            return switch (this) {
                case FIND_TITLE -> distributor.findTitle("9780141439518");
                case REQUEST_RESTOCK -> distributor.requestRestock("9780141439518", 10, "workshop-order-1");
            };
        }
    }

    /** One failure the distributor can show, the warning fragment it must log, and how the mock answers. */
    record Failure(String name, String warning, Function<Call, ResponseCreator> response) {

        @Override
        public String toString() {
            return name;
        }
    }

    private ResponseActions expect(Call call) {
        return switch (call) {
            case FIND_TITLE -> expectTitleRequest("9780141439518");
            case REQUEST_RESTOCK -> expectRestockRequest("restock-confirmed-request.json");
        };
    }

    private ResponseActions expectRestockRequest(String requestFile) {
        return server.expect(requestTo(BASE_URL + "/api/restock-orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(contract(requestFile), JsonCompareMode.STRICT));
    }

    /** A committed file cut before its last character, so the JSON is malformed. */
    private static String truncated(String json) {
        String trimmed = json.strip();
        return trimmed.substring(0, trimmed.length() - 1);
    }

    private static void assertUnavailable(DistributorUnavailableException e) {
        assertThat(e.code()).isEqualTo("distributor-unavailable");
        assertThat(e.title()).isEqualTo("Distributor unavailable");
        assertThat(e.getMessage()).isEqualTo("The distributor gave no usable answer for ISBN 9780141439518");
    }

    /** The committed contract file {@code contracts/distributor/<name>}, byte for byte. */
    static String contract(String name) {
        String path = "/contracts/distributor/" + name;
        try (InputStream in = DistributorAppServiceImplTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No contract file " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A variant of a committed contract file: the file parsed into a map, changed by {@code change} and written back.
     * Used only for bodies no capture holds.
     */
    static String variant(String contractFile, Consumer<Map<String, Object>> change) {
        Map<String, Object> body = JSON.readValue(contract(contractFile), MAP_OF_KEYS);
        change.accept(body);
        return JSON.writeValueAsString(body);
    }

    /**
     * The contributors of a variant body with one broken entry placed among the captured valid contributor (Jane
     * Austen of the Pride and Prejudice capture): {@code alone}, {@code first} or {@code last} of two, or in the {@code middle}
     * of three. A check that reads only one end of the list misses at least one of these.
     */
    private static List<Object> withBrokenContributor(Map<String, Object> body, String position, Object broken) {
        Object valid = ((List<?>) body.get("contributors")).getFirst();
        List<Object> contributors = new ArrayList<>();
        switch (position) {
            case "alone" -> contributors.add(broken);
            case "first" -> {
                contributors.add(broken);
                contributors.add(valid);
            }
            case "last" -> {
                contributors.add(valid);
                contributors.add(broken);
            }
            case "middle" -> {
                contributors.add(valid);
                contributors.add(broken);
                contributors.add(valid);
            }
            default -> throw new IllegalArgumentException("Unknown position " + position);
        }
        return contributors;
    }

    /** Expects one GET of the title and answers 200 with the given contract file. */
    private void stubTitle(String ean, String contractFile) {
        stubTitleBody(ean, contract(contractFile));
    }

    /** Expects one GET of the title and answers 200 with the given JSON body. */
    private void stubTitleBody(String ean, String json) {
        expectTitleRequest(ean).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private static void assertNotOrderable(TitleNotOrderableException e, String isbn) {
        assertThat(e.code()).isEqualTo("title-not-orderable");
        assertThat(e.title()).isEqualTo("Title not orderable");
        assertThat(e.getMessage()).isEqualTo("The title with ISBN " + isbn + " cannot be ordered from the distributor");
    }

    private ResponseActions expectTitleRequest(String ean) {
        return server.expect(requestTo(BASE_URL + "/api/titles/" + ean)).andExpect(method(HttpMethod.GET));
    }
}
