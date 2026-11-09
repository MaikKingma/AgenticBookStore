package nl.openvalue.bookstore.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The guardrails of the assistant endpoint: {@code bookstore.assistant.max-requests-per-minute} (per application
 * instance) and {@code bookstore.assistant.max-message-length} (in characters as {@link String#length()} counts them).
 */
@ConfigurationProperties("bookstore.assistant")
public record AssistantProperties(int maxRequestsPerMinute, int maxMessageLength) {
}
