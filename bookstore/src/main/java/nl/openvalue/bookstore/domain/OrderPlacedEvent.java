package nl.openvalue.bookstore.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registered once when an order is placed. Carries the normalised e-mail address of the customer, the order total and
 * the placement time, in JDK types only, so a listener needs nothing else from the domain.
 */
public record OrderPlacedEvent(UUID orderId, String customerEmail, BigDecimal total, Instant occurredAt)
        implements DomainEvent {
}
