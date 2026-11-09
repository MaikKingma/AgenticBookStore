package nl.openvalue.bookstore.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body of a chat message. The conversation id keeps the memory of one conversation apart from every other.
 * The maximum length of the message is a setting, so {@link AssistantCommands} checks it, not an annotation.
 */
public record ChatPayload(@NotNull UUID conversationId, @NotBlank String message) {
}
