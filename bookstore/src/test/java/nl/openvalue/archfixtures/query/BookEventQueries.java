package nl.openvalue.archfixtures.query;

import nl.openvalue.archfixtures.domain.BookAddedEvent;

/**
 * Fixture: a query adapter that holds a domain event. Rule 1 must report it, because the exception for events applies
 * to process only.
 */
public class BookEventQueries {

    BookAddedEvent event;
}
