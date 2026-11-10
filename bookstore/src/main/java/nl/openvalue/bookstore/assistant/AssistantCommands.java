package nl.openvalue.bookstore.assistant;

import jakarta.validation.Valid;
import java.util.List;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The chat endpoint of the assistant. A request is checked in this order, and the first failing check answers: the
 * payload (400), the length of the message (400), the offline mode (503), the rate limit (429). Only then the chat
 * client is called. A rejected request takes no slot of the rate limit and leaves nothing in the conversation memory.
 *
 * <p>A chat call that fails leaves nothing in the conversation memory either: the memory of the conversation is copied
 * right before the call and put back when the call throws, because the memory advisor stores the question before the
 * model answers. A use-case exception from the call keeps its own answer. Any other failure is logged once at WARN and
 * answered as 503 {@code assistant-unavailable}, without its message.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantCommands {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantCommands.class);

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final AssistantRateLimiter rateLimiter;
    private final int maxMessageLength;

    public AssistantCommands(ChatClient chatClient, ChatModel chatModel, ChatMemory chatMemory,
            AssistantRateLimiter rateLimiter, AssistantProperties properties) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
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
        String conversationId = payload.conversationId().toString();
        List<Message> before = List.copyOf(chatMemory.get(conversationId));
        String reply;
        try {
            reply = chatClient.prompt()
                    .user(payload.message())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
        } catch (NotFoundException | ConflictException | InvalidInputException | UnavailableException rejection) {
            restore(conversationId, before);
            throw rejection;
        } catch (RuntimeException failure) {
            restore(conversationId, before);
            LOG.warn("Assistant chat call failed, answering assistant-unavailable", failure);
            throw new UnavailableException("assistant-unavailable", "Assistant unavailable",
                    "The assistant is unavailable right now, try again later", failure);
        }
        return new AssistantReplyView(payload.conversationId(), reply);
    }

    /** Puts the conversation back as it was before the chat call, whatever the call stored or evicted meanwhile. */
    private void restore(String conversationId, List<Message> before) {
        chatMemory.clear(conversationId);
        if (!before.isEmpty()) {
            chatMemory.add(conversationId, before);
        }
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
