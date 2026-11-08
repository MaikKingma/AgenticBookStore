package nl.openvalue.distributor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * A fixed "today" for the full-application tests: Friday 2026-11-06 in Amsterdam, so delivery dates do not depend
 * on the day the suite runs and a run that crosses midnight cannot move them.
 *
 * <p>Both RANDOM_PORT test classes import this configuration. Spring caches a context by its configuration, so
 * importing the same class everywhere keeps them on one shared context instead of starting a second one.
 *
 * <p>The bean method is not called {@code clock}, so it does not override the production bean; it wins by being
 * {@link Primary}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));
    }
}
