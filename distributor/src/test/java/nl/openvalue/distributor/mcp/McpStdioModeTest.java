package nl.openvalue.distributor.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.LockSupport;
import nl.openvalue.distributor.DistributorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts the application in process with the {@code stdio} profile and talks MCP over swapped standard streams.
 */
class McpStdioModeTest {

    private static final Duration DEADLINE = Duration.ofSeconds(5);

    private static final String LOG_FILE_ARGUMENT = "--logging.file.name=target/distributor-stdio-test.log";

    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
            + "{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},"
            + "\"clientInfo\":{\"name\":\"mcp-stdio-mode-test\",\"version\":\"1.0.0\"}}}";

    private static final String INITIALIZED = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";

    private static final String TOOLS_LIST = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void stdioProfileAnswersInitializeOnStdout() {
        String stdout = runStdioSession(session -> {
            session.send(INITIALIZE);
            session.awaitLines(1);
        });

        Map<String, Object> response = parse(stdout.lines().toList().getFirst());
        assertThat(response).containsEntry("jsonrpc", "2.0").containsEntry("id", 1);
        Map<String, Object> result = objectAt(response, "result");
        assertThat(result).containsEntry("protocolVersion", "2025-11-25");
        assertThat(objectAt(result, "serverInfo"))
                .containsEntry("name", "distributor-catalog")
                .containsEntry("version", "1.0.0");
    }

    @Test
    void stdioProfileStartsNoWebServer() {
        runStdioSession(session -> {
            assertThat(session.context()).isNotInstanceOf(WebServerApplicationContext.class);
            assertThat(session.context().getBeanNamesForType(StdioServerTransportProvider.class)).hasSize(1);
        });
    }

    @Test
    void stdioListsTheFourAnnotatedTools() {
        String stdout = runStdioSession(McpStdioModeTest::initializeThenListTools);

        Map<String, Object> response = parse(stdout.lines().toList().get(1));
        assertThat(response).containsEntry("jsonrpc", "2.0").containsEntry("id", 2);
        List<Map<String, Object>> tools = listAt(objectAt(response, "result"), "tools");
        assertThat(tools).extracting(tool -> tool.get("name"))
                .containsExactlyInAnyOrder("search_titles", "get_title", "list_subjects", "check_availability");
    }

    @Test
    void stdoutCarriesOnlyJsonRpcLines() {
        String stdout = runStdioSession(McpStdioModeTest::initializeThenListTools);

        // Every captured line counts, including anything written while the context closed: no filtering.
        List<String> lines = stdout.lines().toList();
        assertThat(lines).hasSizeGreaterThanOrEqualTo(2);
        assertThat(lines).allSatisfy(line -> assertThat(parse(line)).containsEntry("jsonrpc", "2.0"));
        assertThat(lines).extracting(line -> parse(line).get("id")).containsExactly(1, 2);
    }

    private static void initializeThenListTools(StdioSession session) throws IOException {
        session.send(INITIALIZE);
        session.awaitLines(1);
        session.send(INITIALIZED);
        session.send(TOOLS_LIST);
        session.awaitLines(2);
    }

    /**
     * Runs one stdio session and returns everything the application wrote to stdout, unfiltered.
     *
     * <p>The stdio transport captures {@code System.in} and {@code System.out} when its bean is built, so both are
     * swapped before the context starts. The pipe stays open until the script has finished.
     *
     * <p>Logging is process-wide state in the shared Surefire fork: Boot configures logback once per JVM and skips
     * the stdio profile's console settings when an earlier test context already did, and it writes logging system
     * properties only when they are absent. So the session marks the logging system uninitialized before the start,
     * and afterwards restores the system properties and the default logging configuration for the tests that follow.
     */
    private String runStdioSession(SessionScript script) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        Properties originalProperties = new Properties();
        originalProperties.putAll(System.getProperties());
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ConfigurableApplicationContext context = null;
        loggingSystem().cleanUp();
        try (PipedOutputStream toServer = new PipedOutputStream();
                PipedInputStream serverIn = new PipedInputStream(toServer, 65536)) {
            try {
                System.setIn(serverIn);
                System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
                context = new SpringApplicationBuilder(DistributorApplication.class)
                        .main(DistributorApplication.class)
                        .profiles("stdio")
                        .run(LOG_FILE_ARGUMENT);
                script.run(new StdioSession(context, toServer, captured));
            }
            finally {
                if (context != null) {
                    context.close();
                }
                System.setIn(originalIn);
                System.setOut(originalOut);
                restoreSystemProperties(originalProperties);
                restoreDefaultLogging();
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static LoggingSystem loggingSystem() {
        return LoggingSystem.get(McpStdioModeTest.class.getClassLoader());
    }

    private static void restoreSystemProperties(Properties original) {
        for (String name : System.getProperties().stringPropertyNames()) {
            if (!original.containsKey(name)) {
                System.clearProperty(name);
            }
        }
        for (String name : original.stringPropertyNames()) {
            System.setProperty(name, original.getProperty(name));
        }
    }

    /** Configures logging as a context without a profile does: Boot defaults plus application.yml. */
    private static void restoreDefaultLogging() {
        StandardEnvironment environment = new StandardEnvironment();
        try {
            new YamlPropertySourceLoader().load("application.yml", new ClassPathResource("application.yml"))
                    .forEach(environment.getPropertySources()::addLast);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        LoggingSystem loggingSystem = loggingSystem();
        loggingSystem.cleanUp();
        loggingSystem.beforeInitialize();
        loggingSystem.initialize(new LoggingInitializationContext(environment), null, null);
    }

    private Map<String, Object> parse(String line) {
        return jsonMapper.readValue(line, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectAt(Map<String, Object> json, String key) {
        assertThat(json).containsKey(key);
        return (Map<String, Object>) json.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listAt(Map<String, Object> json, String key) {
        assertThat(json).containsKey(key);
        return (List<Map<String, Object>>) json.get(key);
    }

    @FunctionalInterface
    private interface SessionScript {

        void run(StdioSession session) throws IOException;
    }

    private record StdioSession(ConfigurableApplicationContext context, PipedOutputStream toServer,
            ByteArrayOutputStream captured) {

        void send(String jsonRpcLine) throws IOException {
            toServer.write((jsonRpcLine + "\n").getBytes(StandardCharsets.UTF_8));
            toServer.flush();
        }

        void awaitLines(int expected) {
            long deadline = System.nanoTime() + DEADLINE.toNanos();
            while (lineCount() < expected) {
                if (System.nanoTime() - deadline > 0) {
                    fail("Expected %d lines on stdout within %s, got: %s", expected, DEADLINE,
                            captured.toString(StandardCharsets.UTF_8));
                }
                LockSupport.parkNanos(Duration.ofMillis(20).toNanos());
            }
        }

        private int lineCount() {
            return (int) captured.toString(StandardCharsets.UTF_8).chars().filter(c -> c == '\n').count();
        }
    }
}
