package nl.openvalue.bookstore;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Empties every application table in schema public, so integration tests start from an empty database without
 * transaction rollback. The Flyway history table is kept.
 */
public class DatabaseCleaner {

    private static final String APPLICATION_TABLES = """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'""";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        List<String> tables = jdbcTemplate.queryForList(APPLICATION_TABLES, String.class);
        if (tables.isEmpty()) {
            return;
        }
        String quotedTables = tables.stream()
                .map(table -> "\"public\".\"" + table.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("TRUNCATE TABLE " + quotedTables + " RESTART IDENTITY CASCADE");
    }
}
