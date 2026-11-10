package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Guards that the demo seed never loads in tests. Every integration test empties the tables before it runs, so an
 * empty table proves nothing about the seed. What does prove it: the locations that the application's own Flyway bean
 * uses, and the migration history, which the cleaner keeps and which holds a row for every script that ever ran.
 * The history proves the seed stayed out when its successful versioned rows are exactly the schema migrations on the
 * classpath, in version order, and no repeatable script ran. It does not pin today's list, so a new schema migration
 * keeps it green.
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
    void theMigrationHistoryHoldsExactlyTheSchemaMigrationsOnTheClasspathAndNoRepeatableOne() throws IOException {
        List<String> expectedVersions = schemaMigrationVersionsOnTheClasspath();
        List<String> versioned = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL AND success ORDER BY installed_rank",
                String.class);
        Integer repeatable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IS NULL", Integer.class);

        assertThat(expectedVersions).isNotEmpty();
        assertThat(versioned).containsExactlyElementsOf(expectedVersions);
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

    /**
     * The versions of the schema migrations on the classpath, read the way Flyway reads a file name (the text between
     * the V prefix and the double underscore, with an underscore standing for a dot) and in Flyway's version order.
     * The scan is Spring's own, so the expectation does not come from the object under test. It covers every
     * classpath root and every subdirectory, because Flyway scans its location recursively.
     */
    private static List<String> schemaMigrationVersionsOnTheClasspath() throws IOException {
        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/migration/**/V*__*.sql");
        return Arrays.stream(scripts)
                .map(Resource::getFilename)
                .map(name -> name.substring(1, name.indexOf("__")).replace('_', '.'))
                .map(MigrationVersion::fromVersion)
                .sorted()
                .map(MigrationVersion::getVersion)
                .toList();
    }
}
