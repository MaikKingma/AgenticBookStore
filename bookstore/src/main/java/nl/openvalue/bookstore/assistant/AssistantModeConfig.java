package nl.openvalue.bookstore.assistant;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Switches the chat model off when no key is configured, so the application starts without one and the assistant
 * answers that it is offline. Runs after the configuration files are read, so the key is already resolved from
 * {@code BOOKSTORE_AI_API_KEY}. When the resolved key is blank and no chat model is chosen explicitly, it adds
 * {@code spring.ai.model.chat=none} as the lowest-precedence property source: the Anthropic chat model is then not
 * created and {@link AssistantConfig} provides the {@link OfflineChatModel} instead. Registered in
 * {@code META-INF/spring.factories}; it is not a bean.
 */
public class AssistantModeConfig implements EnvironmentPostProcessor {

    static final String CHAT_MODEL_PROPERTY = "spring.ai.model.chat";
    static final String OFFLINE_CHAT_MODEL = "none";

    private static final String KEY_PROPERTY = "spring.ai.anthropic.api-key";
    private static final String SOURCE_NAME = "assistantOfflineMode";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String key = environment.getProperty(KEY_PROPERTY);
        boolean keyMissing = key == null || key.isBlank();
        boolean chatModelChosen = environment.containsProperty(CHAT_MODEL_PROPERTY);
        if (keyMissing && !chatModelChosen) {
            environment.getPropertySources()
                    .addLast(new MapPropertySource(SOURCE_NAME, Map.of(CHAT_MODEL_PROPERTY, OFFLINE_CHAT_MODEL)));
        }
    }
}
