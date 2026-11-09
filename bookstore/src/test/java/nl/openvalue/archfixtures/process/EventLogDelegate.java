package nl.openvalue.archfixtures.process;

import nl.openvalue.archfixtures.domain.BookEventLog;

/**
 * Fixture: a process class that holds a domain class whose name only contains Event. Rule 1 must report it, so the
 * exception for events stays limited to names ending with Event.
 */
public class EventLogDelegate {

    BookEventLog log;
}
