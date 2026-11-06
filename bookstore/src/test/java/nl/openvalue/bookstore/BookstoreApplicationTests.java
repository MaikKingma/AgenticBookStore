package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ConnectionCallback;

class BookstoreApplicationTests extends IntegrationTestBase {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayRecordsTheBaselineMigrationAsVersionZero() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version, script, success FROM flyway_schema_history WHERE version = '0'");

        assertThat(rows).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("version", "0")
                .containsEntry("script", "V0__baseline.sql")
                .containsEntry("success", true));
    }

    @Test
    void runsOnPostgresql() {
        String product = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());

        assertThat(product).isEqualTo("PostgreSQL");
    }

    @Test
    void hibernateOnlyValidatesTheSchemaWithTheTestProfileActive() {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
