package nl.openvalue.bookstore.assistant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The assistant's Spring configuration: its properties and, while no key is configured, the offline chat model.
 * {@link AssistantModeConfig} sets {@code spring.ai.model.chat=none} in that case, which also keeps the Anthropic
 * chat model from being created, so there is exactly one chat model in either mode.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {

    @Bean
    @ConditionalOnProperty(name = AssistantModeConfig.CHAT_MODEL_PROPERTY,
            havingValue = AssistantModeConfig.OFFLINE_CHAT_MODEL)
    OfflineChatModel offlineChatModel() {
        return new OfflineChatModel();
    }
}
