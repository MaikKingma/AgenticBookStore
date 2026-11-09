package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Pins the assistant settings of application.yml and of the test profile without a Spring context. Every environment
 * here is built from the yml files and the variables a test passes in only: the process environment and the system
 * properties are removed, so the tests give the same result whatever the shell exports.
 */
class AssistantSettingsTest {

    private static final String KEY = "spring.ai.anthropic.api-key";

    private static final List<String> SAMPLING_NAMES = List.of("temperature", "top-p", "top-k", "top_p", "top_k",
            "topp", "topk");

    @Test
    void keyIsEmptyWithoutTheBookstoreVariable() throws IOException {
        StandardEnvironment environment = mainFileWith(Map.of());

        assertThat(environment.containsProperty(KEY)).isTrue();
        assertThat(environment.getProperty(KEY)).isEqualTo("");
    }

    @Test
    void keyFollowsTheBookstoreVariable() throws IOException {
        assertThat(mainFileWith(Map.of("BOOKSTORE_AI_API_KEY", "fake-value-one")).getProperty(KEY))
                .isEqualTo("fake-value-one");
        assertThat(mainFileWith(Map.of("BOOKSTORE_AI_API_KEY", "fake-value-two")).getProperty(KEY))
                .isEqualTo("fake-value-two");
    }

    @Test
    void modelDefaultsToClaudeHaiku55() throws IOException {
        assertThat(bind(mainFileWith(Map.of()), "spring.ai.anthropic.chat.model", String.class))
                .isEqualTo("claude-haiku-5-5");
    }

    @Test
    void modelFollowsTheBookstoreModelVariable() throws IOException {
        assertThat(bind(mainFileWith(Map.of("BOOKSTORE_AI_MODEL", "workshop-model-one")),
                "spring.ai.anthropic.chat.model", String.class)).isEqualTo("workshop-model-one");
        assertThat(bind(mainFileWith(Map.of("BOOKSTORE_AI_MODEL", "workshop-model-two")),
                "spring.ai.anthropic.chat.model", String.class)).isEqualTo("workshop-model-two");
    }

    @Test
    void maxTokensIs1024() throws IOException {
        assertThat(bind(mainFileWith(Map.of()), "spring.ai.anthropic.chat.max-tokens", Integer.class))
                .isEqualTo(1024);
    }

    @Test
    void baseUrlIsPinnedToTheAnthropicApi() throws IOException {
        assertThat(mainFileWith(Map.of()).getProperty("spring.ai.anthropic.base-url"))
                .isEqualTo("https://api.anthropic.com");
    }

    @Test
    void noSamplingSettingIsConfigured() throws IOException {
        List<String> names = propertyNamesOfBothFiles();

        // The check is only meaningful if the files were read: the chat settings must be among the names.
        assertThat(names).contains("spring.ai.anthropic.chat.model", "spring.ai.anthropic.chat.max-tokens", KEY);
        assertThat(names).noneMatch(name -> SAMPLING_NAMES.stream()
                .anyMatch(sampling -> name.toLowerCase(Locale.ROOT).contains(sampling)));
    }

    @Test
    void toolErrorsAreNotThrown() throws IOException {
        List<String> names = propertyNamesOfBothFiles();

        assertThat(names).contains(KEY);
        assertThat(names).doesNotContain("spring.ai.tools.throw-exception-on-error");
        assertThat(names).noneMatch(name -> name.startsWith("spring.ai.tools."));
    }

    @Test
    void assistantPropertiesBindFromTheFile() throws IOException {
        AssistantProperties properties = bind(mainFileWith(Map.of()), "bookstore.assistant",
                AssistantProperties.class);

        assertThat(properties.maxRequestsPerMinute()).isEqualTo(20);
        assertThat(properties.maxMessageLength()).isEqualTo(1000);
    }

    @Test
    void assistantPropertiesFollowOverrides() throws IOException {
        AssistantProperties properties = bind(mainFileWith(Map.of(
                        "bookstore.assistant.max-requests-per-minute", "7",
                        "bookstore.assistant.max-message-length", "50")),
                "bookstore.assistant", AssistantProperties.class);

        assertThat(properties.maxRequestsPerMinute()).isEqualTo(7);
        assertThat(properties.maxMessageLength()).isEqualTo(50);
    }

    @Test
    void testProfileBlanksTheKey() throws IOException {
        StandardEnvironment testProfileOnly = environmentOf(Map.of(), "application-test.yml");
        assertThat(testProfileOnly.containsProperty(KEY)).isTrue();
        assertThat(testProfileOnly.getProperty(KEY)).isEqualTo("");

        // With the test profile active, the blank key wins over the variable that application.yml reads.
        StandardEnvironment testProfileActive = environmentOf(Map.of("BOOKSTORE_AI_API_KEY", "fake-value-one"),
                "application-test.yml", "application.yml");
        assertThat(testProfileActive.getProperty(KEY)).isEqualTo("");
    }

    private static StandardEnvironment mainFileWith(Map<String, Object> variables) throws IOException {
        return environmentOf(variables, "application.yml");
    }

    /**
     * An environment of the given variables, then the given yml files in descending precedence. The process
     * environment and the system properties are removed, so an exported variable never reaches these tests.
     */
    private static StandardEnvironment environmentOf(Map<String, Object> variables, String... files)
            throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addLast(new MapPropertySource("variables", variables));
        for (String file : files) {
            load(file).forEach(sources::addLast);
        }
        return environment;
    }

    private static List<PropertySource<?>> load(String file) throws IOException {
        return new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
    }

    private static List<String> propertyNamesOfBothFiles() throws IOException {
        List<String> names = new ArrayList<>();
        for (String file : List.of("application.yml", "application-test.yml")) {
            for (PropertySource<?> source : load(file)) {
                names.addAll(Arrays.asList(((EnumerablePropertySource<?>) source).getPropertyNames()));
            }
        }
        return names;
    }

    private static <T> T bind(StandardEnvironment environment, String name, Class<T> type) {
        return Binder.get(environment).bind(name, type).get();
    }
}
