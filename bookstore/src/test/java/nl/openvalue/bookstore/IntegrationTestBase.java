package nl.openvalue.bookstore;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base class for every test that needs the full application on PostgreSQL. All subclasses share one Spring context
 * and one container, so subclasses must not add their own context configuration. Each test starts with empty
 * application tables; there is no transaction rollback, so after-commit listeners fire as in production.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestConfig.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected RestTestClient restTestClient;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
    }
}
