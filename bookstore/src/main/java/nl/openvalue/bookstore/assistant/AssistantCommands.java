package nl.openvalue.bookstore.assistant;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The chat endpoint of the assistant. A request is checked in this order, and the first failing check answers: the
 * payload (400), the length of the message (400), the offline mode (503), the rate limit (429). Only then the chat
 * client is called. A rejected request takes no slot of the rate limit and leaves nothing in the conversation memory.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantCommands {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final AssistantRateLimiter rateLimiter;
    private final int maxMessageLength;

    public AssistantCommands(ChatClient chatClient, ChatModel chatModel, AssistantRateLimiter rateLimiter,
            AssistantProperties properties) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.rateLimiter = rateLimiter;
        this.maxMessageLength = properties.maxMessageLength();
    }

    @PostMapping("/chat")
    public AssistantReplyView chat(@Valid @RequestBody ChatPayload payload) {
        if (payload.message().length() > maxMessageLength) {
            throw new MessageTooLongException(maxMessageLength);
        }
        // Checked here, before the chat client: its memory would store the message before the model answers offline.
        if (chatModel instanceof OfflineChatModel) {
            throw OfflineChatModel.offline();
        }
        rateLimiter.acquire();
        String reply = chatClient.prompt()
                .user(payload.message())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, payload.conversationId().toString()))
                .call()
                .content();
        return new AssistantReplyView(payload.conversationId(), reply);
    }

    /**
     * The message is longer than {@code bookstore.assistant.max-message-length} characters, counted as
     * {@link String#length()} counts them. The handler turns it into a 400 problem.
     */
    public static final class MessageTooLongException extends RuntimeException {

        public MessageTooLongException(int maxMessageLength) {
            super("must be at most " + maxMessageLength + " characters");
        }
    }
}
