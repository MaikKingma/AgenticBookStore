package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatabaseCleanerIntegrationTest extends IntegrationTestBase {

    private static final String APPLICATION_TABLE_COUNT = """
            SELECT count(*) FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'""";

    @AfterEach
    void dropTheTemporaryTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS order_line, \"Order\"");
    }

    @Test
    void emptiesEveryApplicationTableAndRestartsTheirIdentities() {
        createTwoLinkedTablesWithRows();
        long historyRows = historyRowCount();

        new DatabaseCleaner(jdbcTemplate).clean();

        assertThat(count("\"Order\"")).isZero();
        assertThat(count("order_line")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "INSERT INTO \"Order\" (customer) VALUES ('after clean') RETURNING id", Long.class)).isEqualTo(1L);
        assertThat(historyRowCount()).isEqualTo(historyRows).isPositive();
    }

    @Test
    void leavesTheMigrationHistoryUntouchedWhenNoApplicationTableExists() {
        assertThat(jdbcTemplate.queryForObject(APPLICATION_TABLE_COUNT, Long.class)).isZero();
        long historyRows = historyRowCount();

        new DatabaseCleaner(jdbcTemplate).clean();

        assertThat(historyRowCount()).isEqualTo(historyRows).isPositive();
    }

    private void createTwoLinkedTablesWithRows() {
        // "Order" is a reserved word in mixed case, so the cleaner only handles it with quoted identifiers.
        jdbcTemplate.execute("""
                CREATE TABLE "Order" (
                    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    customer varchar(100) NOT NULL)""");
        jdbcTemplate.execute("""
                CREATE TABLE order_line (
                    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    order_id bigint NOT NULL REFERENCES "Order" (id),
                    isbn varchar(20) NOT NULL)""");
        jdbcTemplate.update("INSERT INTO \"Order\" (customer) VALUES ('first'), ('second')");
        jdbcTemplate.update("INSERT INTO order_line (order_id, isbn) VALUES (1, '9780134685991'), (2, '9781617294945')");
        assertThat(count("\"Order\"")).isEqualTo(2);
        assertThat(count("order_line")).isEqualTo(2);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long historyRowCount() {
        return count("flyway_schema_history");
    }
}
