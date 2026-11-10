package nl.openvalue.archfixtures.domain;

import java.util.UUID;

/** Fixture: a compliant domain event, the only kind of domain class process may reference. */
public record BookAddedEvent(UUID bookId, int copies) {
}
