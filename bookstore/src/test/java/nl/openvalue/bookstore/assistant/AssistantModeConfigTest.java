package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * The environment post-processor switches the chat model off when no key is configured. Every environment here holds
 * only the sources a test adds: the process environment and the system properties are removed, so an exported
 * variable never changes a result.
 */
class AssistantModeConfigTest {

    private static final String KEY = "spring.ai.anthropic.api-key";
    private static final String CHAT_MODEL = "spring.ai.model.chat";

    @Test
    void emptyKeySwitchesTheModelOff() {
        StandardEnvironment environment = environmentOf(Map.of(KEY, ""));

        postProcess(environment);

        assertThat(environment.getProperty(CHAT_MODEL)).isEqualTo("none");
        // The switch is the lowest-precedence source, so any explicit configuration would still win.
        PropertySource<?> lowest = lastSourceOf(environment);
        assertThat(lowest.getProperty(CHAT_MODEL)).isEqualTo("none");
    }

    @Test
    void whitespaceKeySwitchesTheModelOff() {
        StandardEnvironment spaces = environmentOf(Map.of(KEY, "   "));
        StandardEnvironment tabAndNewline = environmentOf(Map.of(KEY, "\t\n"));
        // U+2003 (em space) is blank for isBlank() but survives trim(), so a trim-based check would keep the model on.
        StandardEnvironment emSpace = environmentOf(Map.of(KEY, Character.toString(0x2003)));

        postProcess(spaces);
        postProcess(tabAndNewline);
        postProcess(emSpace);

        assertThat(spaces.getProperty(CHAT_MODEL)).isEqualTo("none");
        assertThat(tabAndNewline.getProperty(CHAT_MODEL)).isEqualTo("none");
        assertThat(emSpace.getProperty(CHAT_MODEL)).isEqualTo("none");
    }

    @Test
    void missingKeySwitchesTheModelOff() {
        StandardEnvironment environment = environmentOf(Map.of());

        postProcess(environment);

        assertThat(environment.getProperty(CHAT_MODEL)).isEqualTo("none");
    }

    @Test
    void keyKeepsTheModelOn() {
        StandardEnvironment first = environmentOf(Map.of(KEY, "fake-value-one"));
        StandardEnvironment second = environmentOf(Map.of(KEY, " fake-value-two "));
        List<String> sourcesBefore = sourceNamesOf(first);

        postProcess(first);
        postProcess(second);

        assertThat(first.containsProperty(CHAT_MODEL)).isFalse();
        assertThat(second.containsProperty(CHAT_MODEL)).isFalse();
        assertThat(sourceNamesOf(first)).isEqualTo(sourcesBefore);
    }

    @Test
    void placeholderIsResolvedBeforeTheCheck() {
        StandardEnvironment withoutVariable = environmentOf(Map.of(KEY, "${BOOKSTORE_AI_API_KEY:}"));
        Map<String, Object> placeholderAndVariable = new LinkedHashMap<>();
        placeholderAndVariable.put(KEY, "${BOOKSTORE_AI_API_KEY:}");
        placeholderAndVariable.put("BOOKSTORE_AI_API_KEY", "fake-value-one");
        StandardEnvironment withVariable = environmentOf(placeholderAndVariable);

        postProcess(withoutVariable);
        postProcess(withVariable);

        assertThat(withoutVariable.getProperty(CHAT_MODEL)).isEqualTo("none");
        assertThat(withVariable.containsProperty(CHAT_MODEL)).isFalse();
    }

    @Test
    void explicitModelChoiceWins() {
        StandardEnvironment scripted = environmentOf(Map.of(KEY, "", CHAT_MODEL, "scripted"));
        StandardEnvironment anthropic = environmentOf(Map.of(KEY, "", CHAT_MODEL, "anthropic"));
        List<String> sourcesBefore = sourceNamesOf(scripted);

        postProcess(scripted);
        postProcess(anthropic);

        assertThat(scripted.getProperty(CHAT_MODEL)).isEqualTo("scripted");
        assertThat(anthropic.getProperty(CHAT_MODEL)).isEqualTo("anthropic");
        assertThat(sourceNamesOf(scripted)).isEqualTo(sourcesBefore);
    }

    private static void postProcess(StandardEnvironment environment) {
        new AssistantModeConfig().postProcessEnvironment(environment, new SpringApplication());
    }

    /** An environment of exactly the given properties, without the process environment and the system properties. */
    private static StandardEnvironment environmentOf(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addLast(new MapPropertySource("given", properties));
        return environment;
    }

    private static List<String> sourceNamesOf(StandardEnvironment environment) {
        List<String> names = new ArrayList<>();
        environment.getPropertySources().forEach(source -> names.add(source.getName()));
        return names;
    }

    private static PropertySource<?> lastSourceOf(StandardEnvironment environment) {
        List<PropertySource<?>> sources = new ArrayList<>();
        environment.getPropertySources().forEach(sources::add);
        return sources.getLast();
    }
}
