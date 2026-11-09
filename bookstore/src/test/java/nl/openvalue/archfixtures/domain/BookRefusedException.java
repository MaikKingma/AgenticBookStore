package nl.openvalue.archfixtures.domain;

/** Fixture: a domain exception. It is no domain event, so process may not reference it. */
public class BookRefusedException extends RuntimeException {

    public BookRefusedException(String message) {
        super(message);
    }
}
