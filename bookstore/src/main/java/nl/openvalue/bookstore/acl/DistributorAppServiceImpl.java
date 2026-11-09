package nl.openvalue.bookstore.acl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService;
import nl.openvalue.bookstore.domaininteraction.DistributorTitleDTO;
import nl.openvalue.bookstore.domaininteraction.RestockConfirmationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Talks to the distributor's REST API and translates its wire format into the port's DTOs and exceptions. Only the
 * three port exceptions or an empty result leave a public method: every failure the translation does not name
 * (another status or error code, an unreadable or incomplete body, an answer about another EAN than the requested
 * one, a timeout, a refused connection) becomes
 * {@link DistributorUnavailableException} with one warning that names the status or the exception class.
 */
@Component
@EnableConfigurationProperties(DistributorProperties.class)
public class DistributorAppServiceImpl implements DistributorAppService {

    /**
     * Bookstore genre names by distributor subject code prefix. The longest prefix the code starts with wins, so
     * {@code FIC028000} is SCIENCE_FICTION and {@code FIC004000} is FICTION.
     */
    static final Map<String, String> GENRES_BY_SUBJECT_PREFIX = Map.of(
            "FIC028", "SCIENCE_FICTION",
            "FIC009", "FANTASY",
            "FIC022", "MYSTERY",
            "FIC027", "ROMANCE",
            "FIC", "FICTION",
            "HIS", "HISTORY",
            "SCI", "SCIENCE",
            "COM", "TECHNOLOGY",
            "JUV", "CHILDREN",
            "POE", "POETRY");

    /** The genre of a title whose first subject code matches no prefix, or that has no subject code. */
    static final String FALLBACK_GENRE = "FICTION";

    private static final String EUR = "EUR";
    private static final String TITLE_NOT_FOUND = "TITLE_NOT_FOUND";
    private static final String NOT_ORDERABLE = "NOT_ORDERABLE";

    private static final Logger LOG = LoggerFactory.getLogger(DistributorAppServiceImpl.class);

    private final RestClient restClient;

    /**
     * Builds the client from Spring's builder with the base URL and the connect and read timeouts. Spring uses this
     * constructor; the other one is for contract tests.
     */
    @Autowired
    public DistributorAppServiceImpl(RestClient.Builder builder, DistributorProperties properties) {
        this(builder.baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                        .withTimeouts(properties.connectTimeout(), properties.readTimeout())))
                .build());
    }

    /** Uses a ready client; contract tests pass one bound to a mock server. */
    DistributorAppServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<DistributorTitleDTO> findTitle(String isbn) {
        try {
            TitlePayload title = restClient.get()
                    .uri("/api/titles/{ean}", isbn)
                    .retrieve()
                    .body(TitlePayload.class);
            return Optional.of(toTitleDTO(isbn, title));
        } catch (RestClientResponseException e) {
            if (isError(e, HttpStatus.NOT_FOUND, TITLE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw translate(isbn, e);
        } catch (RestClientException e) {
            throw failed(isbn, e);
        }
    }

    @Override
    public RestockConfirmationDTO requestRestock(String isbn, int quantity, String retailerReference) {
        try {
            RestockPayload restock = restClient.post()
                    .uri("/api/restock-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RestockRequestPayload(isbn, quantity, retailerReference))
                    .retrieve()
                    .body(RestockPayload.class);
            return toRestockDTO(isbn, restock);
        } catch (RestClientResponseException e) {
            if (isError(e, HttpStatus.NOT_FOUND, TITLE_NOT_FOUND)) {
                throw new TitleNotFoundAtDistributorException(isbn);
            }
            throw translate(isbn, e);
        } catch (RestClientException e) {
            throw failed(isbn, e);
        }
    }

    private static DistributorTitleDTO toTitleDTO(String isbn, TitlePayload title) {
        if (title == null) {
            throw unusable(isbn, "no body");
        }
        require(isbn, title.ean(), "ean");
        if (!title.ean().equals(isbn)) {
            throw unusable(isbn, "ean " + title.ean() + " is not the requested EAN");
        }
        require(isbn, title.titleName(), "titleName");
        require(isbn, title.contributors(), "contributors");
        require(isbn, title.listPriceCents(), "listPriceCents");
        require(isbn, title.currency(), "currency");
        require(isbn, title.releaseDate(), "releaseDate");
        if (title.contributors().stream().anyMatch(Objects::isNull)) {
            throw unusable(isbn, "null contributor");
        }
        List<String> authors = title.contributors().stream()
                .filter(contributor -> "AUTHOR".equals(contributor.role()))
                .map(ContributorPayload::name)
                .toList();
        if (authors.contains(null)) {
            throw unusable(isbn, "no author name");
        }
        if (!EUR.equals(title.currency())) {
            throw new TitleNotOrderableException(isbn);
        }
        return new DistributorTitleDTO(title.ean(), title.titleName(), authors,
                genreOf(title.ean(), title.subjectCodes()), priceOf(isbn, title.listPriceCents()),
                title.currency(), dateOf(isbn, "releaseDate", title.releaseDate()).getYear(),
                !"OUT_OF_PRINT".equals(title.availability()));
    }

    private static RestockConfirmationDTO toRestockDTO(String isbn, RestockPayload restock) {
        if (restock == null) {
            throw unusable(isbn, "no body");
        }
        require(isbn, restock.restockOrderId(), "restockOrderId");
        require(isbn, restock.quantity(), "quantity");
        require(isbn, restock.status(), "status");
        require(isbn, restock.expectedDeliveryDate(), "expectedDeliveryDate");
        boolean backordered = switch (restock.status()) {
            case "CONFIRMED" -> false;
            case "BACKORDERED" -> true;
            default -> throw unusable(isbn, "unknown status " + restock.status());
        };
        return new RestockConfirmationDTO(restock.restockOrderId(), quantityOf(isbn, restock.quantity()),
                dateOf(isbn, "expectedDeliveryDate", restock.expectedDeliveryDate()), backordered);
    }

    /** The genre of the first subject code by longest prefix, or the fallback with a warning. */
    private static String genreOf(String ean, List<String> subjectCodes) {
        String code = subjectCodes == null || subjectCodes.isEmpty() ? null : subjectCodes.getFirst();
        Optional<String> prefix = code == null ? Optional.empty() : GENRES_BY_SUBJECT_PREFIX.keySet().stream()
                .filter(code::startsWith)
                .max(Comparator.comparingInt(String::length));
        if (prefix.isEmpty()) {
            LOG.warn("No bookstore genre for subject code {} of EAN {}, using {}", code, ean, FALLBACK_GENRE);
            return FALLBACK_GENRE;
        }
        return GENRES_BY_SUBJECT_PREFIX.get(prefix.get());
    }

    /** The price in euros with scale two; a cents value that is not a whole number is unusable, never rounded. */
    private static BigDecimal priceOf(String isbn, BigDecimal cents) {
        try {
            return new BigDecimal(cents.toBigIntegerExact(), 2);
        } catch (ArithmeticException e) {
            throw unusable(isbn, "listPriceCents " + cents.toPlainString() + " is not a whole number");
        }
    }

    /** The confirmed quantity; a value that is not a whole number is unusable, never truncated. */
    private static int quantityOf(String isbn, BigDecimal quantity) {
        try {
            return quantity.intValueExact();
        } catch (ArithmeticException e) {
            throw unusable(isbn, "quantity " + quantity.toPlainString() + " is not a whole number");
        }
    }

    private static LocalDate dateOf(String isbn, String key, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw unusable(isbn, key + " " + value + " is not a date");
        }
    }

    private static void require(String isbn, Object value, String key) {
        if (value == null) {
            throw unusable(isbn, "no " + key);
        }
    }

    /** True when the error answer has the given status and its body carries the given error code. */
    private static boolean isError(RestClientResponseException e, HttpStatus status, String errorCode) {
        return e.getStatusCode().isSameCodeAs(status) && errorCode.equals(errorCodeOf(e));
    }

    /** The error code of an error body; null when the body is empty or not the distributor's JSON error shape. */
    private static String errorCodeOf(RestClientResponseException e) {
        try {
            ErrorPayload error = e.getResponseBodyAs(ErrorPayload.class);
            return error == null ? null : error.errorCode();
        } catch (RestClientException unreadable) {
            return null;
        }
    }

    /** A 422 NOT_ORDERABLE is not orderable; every other error answer means the distributor is unusable. */
    private static RuntimeException translate(String isbn, RestClientResponseException e) {
        if (isError(e, HttpStatus.UNPROCESSABLE_CONTENT, NOT_ORDERABLE)) {
            return new TitleNotOrderableException(isbn);
        }
        LOG.warn("Distributor answered status {} for EAN {}", e.getStatusCode().value(), isbn);
        return unavailable(isbn);
    }

    private static DistributorUnavailableException failed(String isbn, RestClientException e) {
        LOG.warn("Distributor call for EAN {} failed with {}", isbn, e.getClass().getSimpleName());
        return unavailable(isbn);
    }

    private static DistributorUnavailableException unusable(String isbn, String reason) {
        LOG.warn("Distributor answer for EAN {} is unusable: {}", isbn, reason);
        return unavailable(isbn);
    }

    private static DistributorUnavailableException unavailable(String isbn) {
        return new DistributorUnavailableException("The distributor gave no usable answer for ISBN " + isbn);
    }

    private record TitlePayload(String ean, String titleName, List<ContributorPayload> contributors,
            List<String> subjectCodes, BigDecimal listPriceCents, String currency, String availability,
            String releaseDate) {
    }

    private record ContributorPayload(String name, String role) {
    }

    private record RestockRequestPayload(String ean, Integer quantity, String retailerReference) {
    }

    private record RestockPayload(String restockOrderId, BigDecimal quantity, String status,
            String expectedDeliveryDate) {
    }

    private record ErrorPayload(String errorCode, String message) {
    }
}
