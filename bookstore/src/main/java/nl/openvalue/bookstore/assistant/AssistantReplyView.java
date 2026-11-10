package nl.openvalue.bookstore.assistant;

import java.util.UUID;

/** The assistant's answer to one chat message, with the conversation it belongs to. */
public record AssistantReplyView(UUID conversationId, String reply) {
}
