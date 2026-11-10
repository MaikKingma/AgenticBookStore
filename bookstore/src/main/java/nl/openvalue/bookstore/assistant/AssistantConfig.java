package nl.openvalue.bookstore.assistant;

import java.nio.charset.StandardCharsets;
import org.springframework.ai.anthropic.AnthropicChatOptions;
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
 *
 * <p>The chat client switches the model's thinking off. Left unset, the model answers with signed thinking blocks, and
 * the pinned Spring AI (2.0.2) replays them out of order (the text before the thinking block) and stores the thinking
 * text as a second assistant message, so the API rejects the follow-up message of a conversation and a tool round with
 * a 400. Without thinking there is nothing to replay, and the whole token limit goes to the answer. The model and the
 * token limit still come from {@code spring.ai.anthropic.chat}; the property for thinking cannot be bound, so it is
 * set here.
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
                .defaultOptions(AnthropicChatOptions.builder().thinkingDisabled())
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
