package nl.openvalue.archfixtures.process;

import nl.openvalue.archfixtures.domain.BookRefusedException;

/**
 * Fixture: a process class that holds a domain exception. Rule 1 must report it, so the exception for events never
 * covers domain exceptions.
 */
public class RefusalDelegate {

    BookRefusedException refusal;
}
