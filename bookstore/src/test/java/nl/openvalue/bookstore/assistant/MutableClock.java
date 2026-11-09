package nl.openvalue.bookstore.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock in UTC that stands still until a test moves it forward. A plain class: a test creates it itself or offers it
 * as a bean from its own test configuration.
 */
public class MutableClock extends Clock {

    private volatile Instant instant;

    public MutableClock(Instant start) {
        this.instant = start;
    }

    /** Moves the clock forward by the given amount. */
    public synchronized void advance(Duration amount) {
        instant = instant.plus(amount);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** Only UTC is supported: the application clock runs in UTC. */
    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("MutableClock runs in UTC only");
    }
}
