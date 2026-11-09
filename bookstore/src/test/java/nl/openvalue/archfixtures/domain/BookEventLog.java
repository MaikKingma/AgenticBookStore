package nl.openvalue.archfixtures.domain;

import java.util.List;
import java.util.UUID;

/**
 * Fixture: a domain class whose name contains Event without ending with it. It is no domain event, so process may not
 * reference it.
 */
public record BookEventLog(List<UUID> bookIds) {
}
