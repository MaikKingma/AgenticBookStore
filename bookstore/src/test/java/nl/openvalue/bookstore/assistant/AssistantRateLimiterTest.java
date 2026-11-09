package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** The sliding window of the assistant's rate limit, driven by a clock the test moves; no Spring context. */
class AssistantRateLimiterTest {

    /** A past instant with a sub-second fraction, so that a limiter that truncates to whole seconds frees too early. */
    private static final Instant START = Instant.parse("2001-06-15T12:00:30.600Z");

    private static final int THREADS = 50;
    private static final int ROUNDS = 50;

    private final MutableClock clock = new MutableClock(START);

    private AssistantRateLimiter limiter(int maxRequestsPerMinute) {
        return new AssistantRateLimiter(clock, new AssistantProperties(maxRequestsPerMinute, 1000));
    }

    @Test
    void allowsTwenty() {
        AssistantRateLimiter limiter = limiter(20);

        assertThat(acceptedOf(limiter, 20)).isEqualTo(20);
    }

    @Test
    void rejectsTheTwentyFirst() {
        AssistantRateLimiter limiter = limiter(20);
        acceptAll(limiter, 20);

        ErrorResponseException rejection = rejectionOf(limiter);

        assertThat(rejection.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        ProblemDetail problem = rejection.getBody();
        assertThat(problem.getStatus()).isEqualTo(429);
        assertThat(problem.getDetail()).isEqualTo("Too many assistant requests: at most 20 per minute");
        assertThat(problem.getProperties()).as("no code and no details: the storefront knows a 429 by its status")
                .isNullOrEmpty();
    }

    @Test
    void otherLimitIsHonoured() {
        AssistantRateLimiter limiter = limiter(3);
        acceptAll(limiter, 3);

        ErrorResponseException rejection = rejectionOf(limiter);

        assertThat(rejection.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rejection.getBody().getDetail()).isEqualTo("Too many assistant requests: at most 3 per minute");

        // the advice answers the rejection as it is, so the 429 body names this limit, not the default 20
        ProblemDetail answer = new AssistantExceptionHandler().handleErrorResponse(rejection);

        assertThat(answer.getStatus()).isEqualTo(429);
        assertThat(answer.getDetail()).isEqualTo("Too many assistant requests: at most 3 per minute");
        assertThat(answer.getProperties()).as("no code and no details").isNullOrEmpty();
    }

    @Test
    void stillFullJustBeforeAMinute() {
        AssistantRateLimiter limiter = limiter(20);
        acceptAll(limiter, 20);

        clock.advance(Duration.ofMillis(59_999));

        rejectionOf(limiter);
    }

    @Test
    void freesExactlyAfterAMinute() {
        AssistantRateLimiter limiter = limiter(20);
        acceptAll(limiter, 20);

        clock.advance(Duration.ofSeconds(60));

        assertThatCode(limiter::acquire).doesNotThrowAnyException();
    }

    @Test
    void slidesInsteadOfResettingEachMinute() {
        AssistantRateLimiter limiter = limiter(20);
        acceptAll(limiter, 10);
        // 12:01:00.600, in the next clock minute and 30 s after the first ten
        clock.advance(Duration.ofSeconds(30));
        acceptAll(limiter, 10);

        rejectionOf(limiter);

        // 12:01:30.600: the first ten are a minute old, the second ten are not
        clock.advance(Duration.ofSeconds(30));
        assertThat(acceptedOf(limiter, 10)).isEqualTo(10);
        rejectionOf(limiter);
    }

    @Test
    void rejectedRequestsAreNotCounted() {
        AssistantRateLimiter limiter = limiter(20);
        acceptAll(limiter, 20);
        clock.advance(Duration.ofSeconds(30));
        for (int rejected = 0; rejected < 5; rejected++) {
            rejectionOf(limiter);
        }

        clock.advance(Duration.ofSeconds(30));

        assertThat(acceptedOf(limiter, 20)).isEqualTo(20);
        rejectionOf(limiter);
    }

    @Test
    void concurrentRequestsGetExactlyTheLimit() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(THREADS);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                assertThat(acceptedConcurrently(limiter(20), threads)).as("round %d", round).isEqualTo(20);
            }
        } finally {
            threads.shutdownNow();
            assertThat(threads.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    /**
     * Two requests released together never read the clock at the same time: the clock holds each caller for 50 ms, so
     * a limiter that lets both in at once shows two threads inside {@code instant()}.
     */
    @Test
    void neverTwoRequestsInsideTheLimiterAtOnce() throws Exception {
        InspectingClock inspectingClock = new InspectingClock(START);
        AssistantRateLimiter limiter = new AssistantRateLimiter(inspectingClock, new AssistantProperties(20, 1000));
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> calls = new ArrayList<>();
            for (int thread = 0; thread < 2; thread++) {
                calls.add(threads.submit(() -> {
                    ready.countDown();
                    start.await();
                    limiter.acquire();
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> call : calls) {
                call.get(10, TimeUnit.SECONDS);
            }
        } finally {
            threads.shutdownNow();
            assertThat(threads.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(inspectingClock.reads()).as("both requests read the clock").isEqualTo(2);
        assertThat(inspectingClock.maxThreadsInside()).isEqualTo(1);
    }

    /**
     * A clock that stands still, holds every caller of {@code instant()} for 50 ms on a latch nobody opens and records
     * the most threads it saw inside at once.
     */
    private static final class InspectingClock extends Clock {

        private final Instant instant;
        private final AtomicInteger inside = new AtomicInteger();
        private final AtomicInteger maxInside = new AtomicInteger();
        private final AtomicInteger reads = new AtomicInteger();
        private final CountDownLatch never = new CountDownLatch(1);

        InspectingClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public Instant instant() {
            reads.incrementAndGet();
            maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
            try {
                never.await(50, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inside.decrementAndGet();
            }
            return instant;
        }

        int maxThreadsInside() {
            return maxInside.get();
        }

        int reads() {
            return reads.get();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("InspectingClock runs in UTC only");
        }
    }

    /** Lets {@link #THREADS} requests call the limiter at the same instant, released together, and counts the accepted. */
    private static int acceptedConcurrently(AssistantRateLimiter limiter, ExecutorService threads) throws Exception {
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int thread = 0; thread < THREADS; thread++) {
            results.add(threads.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    limiter.acquire();
                    return true;
                } catch (ErrorResponseException rejected) {
                    return false;
                }
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        int accepted = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                accepted++;
            }
        }
        return accepted;
    }

    private static void acceptAll(AssistantRateLimiter limiter, int requests) {
        for (int request = 1; request <= requests; request++) {
            assertThatCode(limiter::acquire).as("request %d", request).doesNotThrowAnyException();
        }
    }

    /** Sends requests until the first rejection or the given number, and returns how many were accepted. */
    private static int acceptedOf(AssistantRateLimiter limiter, int requests) {
        int accepted = 0;
        for (int request = 1; request <= requests; request++) {
            try {
                limiter.acquire();
                accepted++;
            } catch (ErrorResponseException rejected) {
                return accepted;
            }
        }
        return accepted;
    }

    private static ErrorResponseException rejectionOf(AssistantRateLimiter limiter) {
        ErrorResponseException rejection = catchThrowableOfType(ErrorResponseException.class, limiter::acquire);
        assertThat(rejection).as("the request is rejected").isNotNull();
        assertThat(rejection.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        return rejection;
    }
}
