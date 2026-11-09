package nl.openvalue.bookstore.assistant;

import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The chat model while no key is configured. It never reaches a model: every call answers that the assistant is
 * offline. {@link #offline()} is the one source of that problem, for this model and for the endpoint's own check.
 */
public class OfflineChatModel implements ChatModel {

    /** The offline problem: code {@code assistant-offline}, nothing to list in its details. */
    public static UnavailableException offline() {
        return new UnavailableException("assistant-offline", "Assistant offline",
                "Assistant offline: set BOOKSTORE_AI_API_KEY");
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        throw offline();
    }
}
