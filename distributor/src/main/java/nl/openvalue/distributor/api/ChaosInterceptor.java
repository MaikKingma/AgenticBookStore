package nl.openvalue.distributor.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.LongConsumer;
import java.util.random.RandomGenerator;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies the demo knobs of {@link ChaosProperties}: waits first, then fails the call with a
 * {@link TemporarilyUnavailableException} when the random draw falls below the failure rate. At a failure rate of 0.0
 * no random number is drawn.
 */
public class ChaosInterceptor implements HandlerInterceptor {

    private final ChaosProperties properties;
    private final RandomGenerator random;
    private final LongConsumer sleeper;

    public ChaosInterceptor(ChaosProperties properties, RandomGenerator random, LongConsumer sleeper) {
        this.properties = properties;
        this.random = random;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (properties.latencyMs() > 0) {
            sleeper.accept(properties.latencyMs());
        }
        if (properties.failureRate() > 0.0 && random.nextDouble() < properties.failureRate()) {
            throw new TemporarilyUnavailableException();
        }
        return true;
    }
}
