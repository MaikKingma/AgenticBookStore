package nl.openvalue.bookstore.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Registered once per dip when the available stock of a book drops to or below its reorder level. Carries JDK types
 * only, so a listener needs nothing else from the domain.
 */
public record StockRunningLowEvent(UUID bookId, int available, int reorderQuantity, Instant occurredAt)
        implements DomainEvent {
}
