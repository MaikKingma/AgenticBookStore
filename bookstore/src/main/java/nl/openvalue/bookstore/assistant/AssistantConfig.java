package nl.openvalue.bookstore.assistant;

import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * The assistant's Spring configuration: its properties, the chat memory, the chat client and, while no key is
 * configured, the offline chat model. {@link AssistantModeConfig} sets {@code spring.ai.model.chat=none} in that case,
 * which also keeps the Anthropic chat model from being created, so there is exactly one chat model in either mode.
 *
 * <p>The chat client speaks with the product-owned system prompt, offers the {@link BookstoreTools} and keeps a window
 * of the last messages per conversation. Every call must name its conversation with the advisor parameter
 * {@link ChatMemory#CONVERSATION_ID}; there is no default conversation.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {

    /** Messages kept per conversation: the questions and final answers, never tool calls or tool results. */
    static final int MEMORY_MESSAGES = 20;

    private static final Resource SYSTEM_PROMPT = new ClassPathResource("prompts/assistant-system.st");

    @Bean
    ChatMemory assistantChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MEMORY_MESSAGES)
                .build();
    }

    @Bean
    ChatClient assistantChatClient(ChatClient.Builder builder, BookstoreTools bookstoreTools,
            ChatMemory assistantChatMemory) {
        return builder.defaultSystem(SYSTEM_PROMPT, StandardCharsets.UTF_8)
                .defaultTools(bookstoreTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(assistantChatMemory).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = AssistantModeConfig.CHAT_MODEL_PROPERTY,
            havingValue = AssistantModeConfig.OFFLINE_CHAT_MODEL)
    OfflineChatModel offlineChatModel() {
        return new OfflineChatModel();
    }
}
