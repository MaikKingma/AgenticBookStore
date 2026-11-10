package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import nl.openvalue.bookstore.assistant.OfflineChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * The shared integration test context cannot reach the Anthropic API whatever the shell exports. Operating system
 * environment variables such as {@code SPRING_AI_ANTHROPIC_API_KEY} or {@code SPRING_AI_ANTHROPIC_BASE_URL} outrank a
 * configuration file, so the key and the base URL must be pinned in a test property source that ranks above
 * {@code systemEnvironment}. The blank key keeps the production offline switch in charge of the chat model, and the
 * loopback base URL means no request can leave the machine. This class adds no property source of its own: it checks
 * what {@link IntegrationTestBase} gives every subclass. It makes no request to the chat endpoint.
 */
class IntegrationTestOfflineTest extends IntegrationTestBase {

    private static final String CHAT_MODEL_PROPERTY = "spring.ai.model.chat";
    private static final String API_KEY_PROPERTY = "spring.ai.anthropic.api-key";
    private static final String BASE_URL_PROPERTY = "spring.ai.anthropic.base-url";
    private static final String LOOPBACK_BASE_URL = "http://127.0.0.1:9";

    /** The source the production offline switch adds when the key is blank and no model is chosen. */
    private static final String OFFLINE_SWITCH_SOURCE = "assistantOfflineMode";

    /** Boot's own view over all the other sources; it is not a source of values. */
    private static final String ATTACHED_VIEW = "configurationProperties";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Test
    void theEnvironmentResolvesABlankKeyAndALoopbackBaseUrl() {
        assertThat(environment.getProperty(API_KEY_PROPERTY)).isEqualTo("");
        assertThat(environment.getProperty(BASE_URL_PROPERTY)).isEqualTo(LOOPBACK_BASE_URL);
    }

    @Test
    void theKeyAndTheBaseUrlComeFromASourceAheadOfTheOperatingSystemEnvironment() {
        List<String> names = propertySourceNames();
        int systemEnvironment = names.indexOf("systemEnvironment");
        assertThat(systemEnvironment).as("position of systemEnvironment in %s", names).isNotNegative();

        for (String property : List.of(API_KEY_PROPERTY, BASE_URL_PROPERTY)) {
            Optional<String> source = firstSourceHolding(property);
            assertThat(source).as("a property source holds %s, among %s", property, names).isPresent();
            assertThat(names.indexOf(source.get()))
                    .as("position of source '%s' that supplies %s, among %s", source.get(), property, names)
                    .isNotNegative()
                    .isLessThan(systemEnvironment);
        }
    }

    @Test
    void theProductionOfflineSwitchChoosesTheOfflineChatModel() {
        assertThat(environment.getProperty(CHAT_MODEL_PROPERTY)).isEqualTo("none");
        assertThat(firstSourceHolding(CHAT_MODEL_PROPERTY)).contains(OFFLINE_SWITCH_SOURCE);
    }

    @Test
    void onlyTheOfflineChatModelExists() {
        Map<String, ChatModel> chatModels = context.getBeansOfType(ChatModel.class);

        assertThat(chatModels).hasSize(1);
        assertThat(chatModels.values()).allSatisfy(model -> assertThat(model).isInstanceOf(OfflineChatModel.class));
        assertThat(context.getBeanNamesForType(AnthropicChatModel.class)).isEmpty();
    }

    private List<String> propertySourceNames() {
        return ((ConfigurableEnvironment) environment).getPropertySources().stream()
                .map(PropertySource::getName)
                .filter(name -> !ATTACHED_VIEW.equals(name))
                .toList();
    }

    private Optional<String> firstSourceHolding(String property) {
        return ((ConfigurableEnvironment) environment).getPropertySources().stream()
                .filter(source -> !ATTACHED_VIEW.equals(source.getName()))
                .filter(source -> source.containsProperty(property))
                .map(PropertySource::getName)
                .findFirst();
    }
}
