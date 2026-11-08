package nl.openvalue.distributor.api;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Demo knobs for the REST API, both off by default.
 *
 * @param latencyMs   milliseconds every call under {@code /api} waits before it is handled, 0 or more
 * @param failureRate share of calls under {@code /api} answered with 503, from 0.0 (never) to 1.0 (always)
 */
@ConfigurationProperties("distributor.chaos")
public record ChaosProperties(@DefaultValue("0") long latencyMs, @DefaultValue("0.0") double failureRate) {

    public ChaosProperties {
        if (latencyMs < 0) {
            throw new IllegalArgumentException("distributor.chaos.latency-ms must be 0 or more, was " + latencyMs);
        }
        // Written as a positive range check so that NaN is rejected as well.
        if (!(failureRate >= 0.0 && failureRate <= 1.0)) {
            throw new IllegalArgumentException(
                    "distributor.chaos.failure-rate must be between 0.0 and 1.0, was " + failureRate);
        }
    }
}
