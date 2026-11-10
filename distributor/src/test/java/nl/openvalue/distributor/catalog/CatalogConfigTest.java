package nl.openvalue.distributor.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CatalogConfigTest {

    @Test
    void productionClockUsesEuropeAmsterdam() {
        Clock clock = new CatalogConfig().clock();

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Europe/Amsterdam"));
        // The production clock ticks with real time: a fixed clock would drift away from now.
        assertThat(Duration.between(clock.instant(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
    }
}
