package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatabaseCleanerIntegrationTest extends IntegrationTestBase {

    // Probe tables with names no migration will ever use, so this test never collides with a real table.
    private static final String PARENT = "\"CleanerProbe\"";
    private static final String CHILD = "cleaner_probe_line";

    @AfterEach
    void dropTheProbeTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + CHILD + ", " + PARENT);
    }

    @Test
    void emptiesEveryApplicationTableAndRestartsTheirIdentities() {
        createTwoLinkedTablesWithRows();
        long historyRows = historyRowCount();

        new DatabaseCleaner(jdbcTemplate).clean();

        assertThat(count(PARENT)).isZero();
        assertThat(count(CHILD)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "INSERT INTO " + PARENT + " (customer) VALUES ('after clean') RETURNING id", Long.class)).isEqualTo(1L);
        assertThat(historyRowCount()).isEqualTo(historyRows).isPositive();
    }

    @Test
    void leavesTheMigrationHistoryUntouched() {
        long historyRows = historyRowCount();

        new DatabaseCleaner(jdbcTemplate).clean();

        assertThat(historyRowCount()).isEqualTo(historyRows).isPositive();
    }

    private void createTwoLinkedTablesWithRows() {
        // The parent name is mixed case, so the cleaner only handles it with quoted identifiers.
        jdbcTemplate.execute("CREATE TABLE " + PARENT + " ("
                + " id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                + " customer varchar(100) NOT NULL)");
        jdbcTemplate.execute("CREATE TABLE " + CHILD + " ("
                + " id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                + " parent_id bigint NOT NULL REFERENCES " + PARENT + " (id),"
                + " isbn varchar(20) NOT NULL)");
        jdbcTemplate.update("INSERT INTO " + PARENT + " (customer) VALUES ('first'), ('second')");
        jdbcTemplate.update("INSERT INTO " + CHILD
                + " (parent_id, isbn) VALUES (1, '9780134685991'), (2, '9781617294945')");
        assertThat(count(PARENT)).isEqualTo(2);
        assertThat(count(CHILD)).isEqualTo(2);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long historyRowCount() {
        return count("flyway_schema_history");
    }
}
