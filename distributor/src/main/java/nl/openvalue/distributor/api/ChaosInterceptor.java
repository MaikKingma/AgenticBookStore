package nl.openvalue.distributor.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies the demo knobs of {@link ChaosProperties}: waits first, then fails the call with a
 * {@link TemporarilyUnavailableException} when the random draw falls below the failure rate. At a failure rate of 0.0
 * no random number is drawn.
 *
 * <p>The interceptor is a singleton that runs on every request thread at once, so it asks its random source for a
 * generator on every draw and never keeps one. The source decides which generator that is: production passes the
 * calling thread's own generator, tests pass a fixed fake.
 */
public class ChaosInterceptor implements HandlerInterceptor {

    private final ChaosProperties properties;
    private final Supplier<RandomGenerator> randomSource;
    private final LongConsumer sleeper;

    /** Draws every random number from the one given generator. Only safe on one thread, as in the unit tests. */
    public ChaosInterceptor(ChaosProperties properties, RandomGenerator random, LongConsumer sleeper) {
        this(properties, () -> random, sleeper);
    }

    /** Asks {@code randomSource} for a generator on every draw. */
    ChaosInterceptor(ChaosProperties properties, Supplier<RandomGenerator> randomSource, LongConsumer sleeper) {
        this.properties = properties;
        this.randomSource = randomSource;
        this.sleeper = sleeper;
    }

    /** The sleeper used in production. */
    static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** The source every draw asks for its generator. Package-private for the wiring test. */
    Supplier<RandomGenerator> randomSource() {
        return randomSource;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.latencyMs() > 0) {
            sleeper.accept(properties.latencyMs());
        }
        if (properties.failureRate() > 0.0 && randomSource.get().nextDouble() < properties.failureRate()) {
            throw new TemporarilyUnavailableException();
        }
        return true;
    }
}
