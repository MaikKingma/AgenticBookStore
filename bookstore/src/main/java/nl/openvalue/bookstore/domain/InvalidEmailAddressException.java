package nl.openvalue.bookstore.domain;

/** Thrown when a value is missing, too long or not shaped like an e-mail address. */
public class InvalidEmailAddressException extends RuntimeException {

    public InvalidEmailAddressException(String message) {
        super(message);
    }
}
