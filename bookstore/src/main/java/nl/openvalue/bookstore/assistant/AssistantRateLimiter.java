package nl.openvalue.bookstore.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;

/**
 * Guards the shared key: at most {@code bookstore.assistant.max-requests-per-minute} chat requests reach the model per
 * application instance. The window slides: an accepted request counts for exactly one minute from the moment it was
 * accepted, by the application clock. A rejected request is not counted.
 */
@Component
public class AssistantRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final int maxRequestsPerMinute;
    private final Deque<Instant> accepted = new ArrayDeque<>();

    public AssistantRateLimiter(Clock clock, AssistantProperties properties) {
        this.clock = clock;
        this.maxRequestsPerMinute = properties.maxRequestsPerMinute();
    }

    /**
     * Takes a slot for one request, or rejects it with 429 Too Many Requests when the last minute is full.
     *
     * @throws ErrorResponseException with status 429 when the limit is reached
     */
    public synchronized void acquire() {
        Instant now = clock.instant();
        Instant windowStart = now.minus(WINDOW);
        while (!accepted.isEmpty() && !accepted.peekFirst().isAfter(windowStart)) {
            accepted.removeFirst();
        }
        if (accepted.size() >= maxRequestsPerMinute) {
            throw tooManyRequests();
        }
        accepted.addLast(now);
    }

    private ErrorResponseException tooManyRequests() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Too many assistant requests: at most " + maxRequestsPerMinute + " per minute");
        return new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, problem, null);
    }
}
