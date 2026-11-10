package nl.openvalue.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Unit test of the chaos wiring. The interceptor is one singleton that runs on every request thread at once, and the
 * JDK's default generator is not thread-safe, so every draw must ask the calling thread for its own generator.
 */
class ChaosWebConfigTest {

    /** Exposes the registered interceptors, which {@link InterceptorRegistry} keeps protected. */
    private static final class RecordingRegistry extends InterceptorRegistry {

        @Override
        public List<Object> getInterceptors() {
            return super.getInterceptors();
        }
    }

    @Test
    void everyRequestThreadDrawsFromItsOwnThreadLocalRandom() throws Exception {
        RecordingRegistry registry = new RecordingRegistry();

        new ChaosWebConfig(new ChaosProperties(0, 0.5)).addInterceptors(registry);

        List<Object> interceptors = registry.getInterceptors();
        assertThat(interceptors).hasSize(1);
        assertThat(interceptors.getFirst()).isInstanceOf(MappedInterceptor.class);
        MappedInterceptor mapped = (MappedInterceptor) interceptors.getFirst();
        assertThat(mapped.getIncludePathPatterns()).containsExactly("/api/**");
        assertThat(mapped.getInterceptor()).isInstanceOf(ChaosInterceptor.class);
        ChaosInterceptor chaos = (ChaosInterceptor) mapped.getInterceptor();

        // Asked on a thread other than the test thread, as a request thread does.
        CompletableFuture<RandomGenerator> drawnOnAnotherThread = new CompletableFuture<>();
        Thread requestThread = new Thread(() -> {
            try {
                drawnOnAnotherThread.complete(chaos.randomSource().get());
            } catch (RuntimeException failure) {
                drawnOnAnotherThread.completeExceptionally(failure);
            }
        });
        requestThread.start();
        requestThread.join();

        assertThat(drawnOnAnotherThread.get()).isInstanceOf(ThreadLocalRandom.class);
    }
}
