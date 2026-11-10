package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Guards that the demo seed never loads in tests. Every integration test empties the tables before it runs, so an
 * empty table proves nothing about the seed. What does prove it: the locations that the application's own Flyway bean
 * uses, and the migration history, which the cleaner keeps and which holds a row for every script that ever ran.
 */
class EmptyCatalogueAtStartIntegrationTest extends IntegrationTestBase {

    @Autowired
    private Flyway flyway;

    @Test
    void theTestProfileMigratesOnlyTheSchemaLocation() {
        List<String> locations = Arrays.stream(flyway.getConfiguration().getLocations())
                .map(Location::getDescriptor).toList();

        assertThat(locations).containsExactly("classpath:db/migration");
    }

    @Test
    void theMigrationHistoryHoldsTheFiveSchemaMigrationsAndNoRepeatableOne() {
        List<String> versioned = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL AND success ORDER BY installed_rank",
                String.class);
        Integer repeatable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IS NULL", Integer.class);

        assertThat(versioned).containsExactly("0", "1", "2", "3", "4");
        assertThat(repeatable).isZero();
    }

    @Test
    void theCatalogueIsEmptyAtStart() {
        List<Map<String, Object>> books = restTestClient.get().uri("/books").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .returnResult().getResponseBody();

        assertThat(books).isEmpty();
    }
}
