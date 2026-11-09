package nl.openvalue.bookstore.acl;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the distributor lives and how long the bookstore waits for it: {@code distributor.base-url},
 * {@code distributor.connect-timeout} and {@code distributor.read-timeout}.
 */
@ConfigurationProperties("distributor")
public record DistributorProperties(
        @DefaultValue("http://localhost:8090") String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
