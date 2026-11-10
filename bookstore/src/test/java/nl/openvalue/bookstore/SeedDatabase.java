package nl.openvalue.bookstore;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A database of its own inside the shared test PostgreSQL container, migrated with plain Flyway. It never touches the
 * application context's database, so the demo seed can be applied here while every other test keeps an empty one.
 * Closing it drops the database.
 */
final class SeedDatabase implements AutoCloseable {

    private final JdbcTemplate admin;
    private final String name;
    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    private SeedDatabase(JdbcTemplate admin, String name, Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.admin = admin;
        this.name = name;
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a new empty database in the container behind the given administrative connection and prepares Flyway
     * for the given locations, a comma separated list written like the {@code spring.flyway.locations} property.
     * Nothing is migrated yet.
     */
    static SeedDatabase create(PostgreSQLContainer container, JdbcTemplate admin, String locations) {
        String name = "seed_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE " + name);
        String url = "jdbc:postgresql://" + container.getHost() + ":" + container.getFirstMappedPort() + "/" + name;
        String[] locationList = Arrays.stream(locations.split(",")).map(String::trim).toArray(String[]::new);
        Flyway flyway = Flyway.configure()
                .dataSource(url, container.getUsername(), container.getPassword())
                .locations(locationList)
                .load();
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(url, container.getUsername(), container.getPassword());
        return new SeedDatabase(admin, name, flyway, new JdbcTemplate(dataSource));
    }

    /** Applies every pending migration and returns what Flyway reports about the run. */
    MigrateResult migrate() {
        return flyway.migrate();
    }

    JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    /** The scripts of the repeatable migrations that the history holds, oldest first. */
    List<String> repeatableScripts() {
        return jdbcTemplate.queryForList(
                "SELECT script FROM flyway_schema_history WHERE version IS NULL ORDER BY installed_rank",
                String.class);
    }

    @Override
    public void close() {
        admin.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
    }
}
