package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests of the chaos knobs. The sleeper and the random generator are recording fakes, so nothing here sleeps and
 * the draws are exact.
 */
class ChaosInterceptorTest {

    /** Records every draw in the shared event list and answers with a fixed value. */
    private static final class FixedRandom implements RandomGenerator {

        private final double value;
        private final List<String> events;
        int draws;

        FixedRandom(double value, List<String> events) {
            this.value = value;
            this.events = events;
        }

        @Override
        public double nextDouble() {
            draws++;
            events.add("draw");
            return value;
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException("the interceptor only draws doubles");
        }
    }

    private final List<String> events = new ArrayList<>();

    private boolean handle(ChaosProperties properties, double random) {
        return handle(properties, new FixedRandom(random, events));
    }

    private boolean handle(ChaosProperties properties, FixedRandom random) {
        ChaosInterceptor interceptor = new ChaosInterceptor(properties, random, millis -> events.add("sleep " + millis));
        return interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());
    }

    @Test
    void failureRateZeroNeverFailsAndDoesNotDrawARandom() {
        FixedRandom random = new FixedRandom(0.0, events);

        boolean proceed = handle(new ChaosProperties(0, 0.0), random);

        assertThat(proceed).isTrue();
        assertThat(random.draws).isZero();
    }

    @Test
    void failureRateOneAlwaysFails() {
        assertThatThrownBy(() -> handle(new ChaosProperties(0, 1.0), 0.999999))
                .isInstanceOf(TemporarilyUnavailableException.class);
        assertThatThrownBy(() -> handle(new ChaosProperties(0, 1.0), 0.0))
                .isInstanceOf(TemporarilyUnavailableException.class);
    }

    @Test
    void failsWhenTheRandomIsBelowTheRate() {
        assertThatThrownBy(() -> handle(new ChaosProperties(0, 0.3), 0.29))
                .isInstanceOf(TemporarilyUnavailableException.class);
    }

    @Test
    void passesWhenTheRandomEqualsTheRate() {
        FixedRandom random = new FixedRandom(0.3, events);

        boolean proceed = handle(new ChaosProperties(0, 0.3), random);

        assertThat(proceed).isTrue();
        assertThat(random.draws).isEqualTo(1);
    }

    @Test
    void passesWhenTheRandomIsAboveTheRate() {
        assertThat(handle(new ChaosProperties(0, 0.3), 0.31)).isTrue();
    }

    @Test
    void latencyIsAppliedBeforeHandling() {
        boolean proceed = handle(new ChaosProperties(150, 0.3), 0.3);

        assertThat(proceed).isTrue();
        assertThat(events).containsExactly("sleep 150", "draw");
    }

    @Test
    void latencyIsAppliedAlsoWhenTheCallFails() {
        assertThatThrownBy(() -> handle(new ChaosProperties(150, 1.0), 0.5))
                .isInstanceOf(TemporarilyUnavailableException.class);

        assertThat(events).first().isEqualTo("sleep 150");
        assertThat(events).filteredOn(event -> event.startsWith("sleep")).containsExactly("sleep 150");
    }

    @Test
    void latencyIsAppliedAlsoWhenNoFailureIsConfigured() {
        assertThat(handle(new ChaosProperties(37, 0.0), 0.0)).isTrue();

        assertThat(events).containsExactly("sleep 37");
    }

    @Test
    void zeroLatencyDoesNotSleep() {
        assertThat(handle(new ChaosProperties(0, 0.3), 0.5)).isTrue();

        assertThat(events).containsExactly("draw");
    }

    @Test
    void oneMillisecondOfLatencyIsApplied() {
        assertThat(handle(new ChaosProperties(1, 0.0), 0.0)).isTrue();

        assertThat(events).containsExactly("sleep 1");
    }

    @Test
    void propertiesRejectAFailureRateOutsideZeroToOne() {
        assertThatThrownBy(() -> new ChaosProperties(0, -0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChaosProperties(0, 1.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChaosProperties(0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new ChaosProperties(0, 0.0)).doesNotThrowAnyException();
        assertThatCode(() -> new ChaosProperties(0, 1.0)).doesNotThrowAnyException();
        assertThatCode(() -> new ChaosProperties(0, 0.5)).doesNotThrowAnyException();
    }

    @Test
    void propertiesRejectANegativeLatency() {
        assertThatThrownBy(() -> new ChaosProperties(-1, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new ChaosProperties(0, 0.0)).doesNotThrowAnyException();
        assertThatCode(() -> new ChaosProperties(1, 0.0)).doesNotThrowAnyException();
    }

    @Test
    void propertiesDefaultToNoLatencyAndNoFailures() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of()));

        ChaosProperties properties = binder.bindOrCreate("distributor.chaos", ChaosProperties.class);

        assertThat(properties.latencyMs()).isEqualTo(0L);
        assertThat(properties.failureRate()).isEqualTo(0.0);
    }

    @Test
    void propertiesBindFromTheKebabCaseKeys() {
        Binder binder = new Binder(new MapConfigurationPropertySource(
                Map.of("distributor.chaos.latency-ms", "250", "distributor.chaos.failure-rate", "0.25")));

        ChaosProperties properties = binder.bindOrCreate("distributor.chaos", ChaosProperties.class);

        assertThat(properties).isEqualTo(new ChaosProperties(250, 0.25));
    }
}
