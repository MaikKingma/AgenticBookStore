package nl.openvalue.bookstore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The PostgreSQL container for every test that needs a database.
 * <p>
 * Boot stops the container bean when the context that owns it is closed, so never use {@code @DirtiesContext}
 * in the bookstore test suite and keep the number of distinct test contexts well under Spring's cache size (32).
 * Closing one context would stop the container under every other cached context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    // Static, so every Spring test context in the JVM (full context, JPA slice, web slice) gets the same container.
    // A container created inside the bean method would start once per distinct context.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
