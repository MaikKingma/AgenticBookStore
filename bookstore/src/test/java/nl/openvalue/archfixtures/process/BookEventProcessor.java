package nl.openvalue.archfixtures.process;

import nl.openvalue.archfixtures.domain.BookAddedEvent;

/**
 * Fixture: a compliant listener that references only a domain event. The documented exception allows this, so rule 1
 * must not report it.
 */
public class BookEventProcessor {

    void on(BookAddedEvent event) {
        event.bookId();
    }
}
