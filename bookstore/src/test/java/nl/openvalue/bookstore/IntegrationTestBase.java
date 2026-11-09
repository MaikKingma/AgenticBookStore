package nl.openvalue.bookstore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService;
import nl.openvalue.bookstore.domaininteraction.RestockConfirmationDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base class for every test that needs the full application on PostgreSQL. All subclasses share one Spring context
 * and one container, so subclasses must not add their own context configuration. Each test starts with empty
 * application tables; there is no transaction rollback, so after-commit listeners fire as in production.
 *
 * <p>The shared context also holds the stock event recorder, which records the running-low events delivered after a
 * commit or after a rollback and can fail a transaction just before its commit, and the order event recorder, which
 * does the same for the order placed events; both are reset before every test. The
 * stock item store is a Mockito spy around the real store: it behaves as in production unless a test stubs it, for
 * example to make opening a stock item fail, and Mockito resets it after every test. The distributor port is a
 * Mockito mock, so no full-context test can reach a distributor; a test stubs it for the answer it needs. Before every
 * test it confirms each restock request by default: the order {@link #DEFAULT_RESTOCK_ORDER_ID} for the requested
 * quantity, not backordered, for delivery in two days by the application clock ({@link #defaultExpectedDeliveryDate},
 * taken once per test). A test replaces that answer with {@code doThrow(...)} or {@code doAnswer(...)} on the mock.
 * Because every committed dip of a book's stock asks the distributor for a restock, a stock view taken after such a
 * dip shows the pending restock of that answer; {@link #withDefaultPendingRestock} builds the expected view.
 *
 * <p>The one deliberate fork of this context is the assistant integration test, which adds a scripted chat model and
 * its own rate limit; this context stays offline for the assistant, so no test through it can reach a model.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({PostgresTestConfig.class, StockEventRecorder.class, OrderEventRecorder.class})
public abstract class IntegrationTestBase {

    /** The id of every restock order the distributor mock confirms by default, shaped like the distributor's ids. */
    public static final String DEFAULT_RESTOCK_ORDER_ID = "c4f1a7e2-3b9d-4e8a-b6f5-2d1e0a9c8b7f";

    /** The delivery date of the default restock answer in the current test: two days after today. */
    protected LocalDate defaultExpectedDeliveryDate;

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StockEventRecorder stockEvents;

    @Autowired
    protected OrderEventRecorder orderEvents;

    @MockitoSpyBean
    protected StockItemDataService stockItemDataService;

    @MockitoBean
    protected DistributorAppService distributorAppService;

    /** The application clock; the default restock answer dates its delivery from it. */
    @Autowired
    private Clock clock;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
        stockEvents.reset();
        orderEvents.reset();
        defaultExpectedDeliveryDate = LocalDate.now(clock).plusDays(2);
        doAnswer(invocation -> new RestockConfirmationDTO(DEFAULT_RESTOCK_ORDER_ID, invocation.getArgument(1),
                defaultExpectedDeliveryDate, false))
                .when(distributorAppService).requestRestock(any(), anyInt(), any());
    }

    /**
     * A copy of a stock view as JSON gives it, with the pending restock that the default distributor answer leaves for
     * the given quantity: the default order id, that quantity, the default delivery date as a string, not backordered.
     * Any pending restock in the given view is replaced.
     */
    protected Map<String, Object> withDefaultPendingRestock(Map<String, Object> view, int quantity) {
        Map<String, Object> expected = new LinkedHashMap<>(view);
        expected.put("pendingRestock", Map.of(
                "restockOrderId", DEFAULT_RESTOCK_ORDER_ID,
                "quantity", quantity,
                "expectedDeliveryDate", defaultExpectedDeliveryDate.toString(),
                "backordered", false));
        return expected;
    }
}
