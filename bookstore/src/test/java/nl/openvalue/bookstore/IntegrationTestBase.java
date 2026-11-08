package nl.openvalue.bookstore;

import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * example to make opening a stock item fail, and Mockito resets it after every test.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({PostgresTestConfig.class, StockEventRecorder.class, OrderEventRecorder.class})
public abstract class IntegrationTestBase {

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

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
        stockEvents.reset();
        orderEvents.reset();
    }
}
