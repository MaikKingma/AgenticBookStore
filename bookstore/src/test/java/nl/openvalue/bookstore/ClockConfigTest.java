package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ClockConfigTest extends IntegrationTestBase {

    @Autowired
    private Clock clock;

    @Test
    void providesAUtcClock() {
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
