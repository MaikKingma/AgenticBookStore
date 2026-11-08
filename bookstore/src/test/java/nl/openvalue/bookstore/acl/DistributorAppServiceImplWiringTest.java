package nl.openvalue.bookstore.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.DistributorUnavailableException;
import nl.openvalue.bookstore.domaininteraction.DistributorTitleDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Proves the real adapter bean starts with Spring's {@code RestClient.Builder}, binds its properties and applies its
 * timeouts. The HTTP tests use a JDK server on an ephemeral loopback port, never a real distributor.
 */
class DistributorAppServiceImplWiringTest {

    private static final String PRIDE_AND_PREJUDICE = "9780141439518";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(DistributorAppServiceImpl.class);

    private HttpServer server;
    private ExecutorService handlerThreads;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
        if (handlerThreads != null) {
            handlerThreads.shutdownNow();
        }
    }

    @Test
    void startsExactlyOneDistributorAppService() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DistributorAppService.class);
            assertThat(context.getBean(DistributorAppService.class))
                    .isExactlyInstanceOf(DistributorAppServiceImpl.class);
        });
    }

    @Test
    void bindsTheDefaultProperties() {
        contextRunner.run(context -> assertThat(context.getBean(DistributorProperties.class)).isEqualTo(
                new DistributorProperties("http://localhost:8090", Duration.ofSeconds(2), Duration.ofSeconds(5))));
    }

    @Test
    void bindsOverriddenProperties() {
        contextRunner
                .withPropertyValues("distributor.base-url=http://distributor.test",
                        "distributor.connect-timeout=1500ms", "distributor.read-timeout=250ms")
                .run(context -> assertThat(context.getBean(DistributorProperties.class)).isEqualTo(
                        new DistributorProperties("http://distributor.test", Duration.ofMillis(1500),
                                Duration.ofMillis(250))));
    }

    @Test
    void answerSlowerThanTheReadTimeoutIsDistributorUnavailable() throws IOException {
        startServer(Duration.ofSeconds(2));

        contextRunner
                .withPropertyValues("distributor.base-url=" + baseUrl(), "distributor.connect-timeout=5s",
                        "distributor.read-timeout=250ms")
                .run(context -> {
                    DistributorAppService distributor = context.getBean(DistributorAppService.class);

                    long start = System.nanoTime();
                    Throwable thrown = catchThrowable(() -> distributor.findTitle(PRIDE_AND_PREJUDICE));
                    long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

                    assertThat(thrown).isExactlyInstanceOf(DistributorUnavailableException.class);
                    assertThat(elapsedMillis).isBetween(200L, 1800L);
                });
    }

    @Test
    void answerWithinTheTimeoutsIsTranslated() throws IOException {
        startServer(Duration.ZERO);

        contextRunner
                .withPropertyValues("distributor.base-url=" + baseUrl(), "distributor.connect-timeout=5s",
                        "distributor.read-timeout=250ms")
                .run(context -> assertThat(context.getBean(DistributorAppService.class).findTitle(PRIDE_AND_PREJUDICE))
                        .contains(new DistributorTitleDTO(PRIDE_AND_PREJUDICE, "Pride and Prejudice",
                                List.of("Jane Austen"), "FICTION", new BigDecimal("9.99"), "EUR", 1813, true)));
    }

    /**
     * Starts a loopback server on a free port that answers the Pride and Prejudice title after the given delay, with
     * the committed contract file as the body.
     */
    private void startServer(Duration delay) throws IOException {
        byte[] body = DistributorAppServiceImplTest.contract("title-" + PRIDE_AND_PREJUDICE + ".json")
                .getBytes(StandardCharsets.UTF_8);
        handlerThreads = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(handlerThreads);
        server.createContext("/api/titles/" + PRIDE_AND_PREJUDICE, exchange -> answer(exchange, delay, body));
        server.start();
    }

    private static void answer(HttpExchange exchange, Duration delay, byte[] body) throws IOException {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private String baseUrl() {
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }
}
