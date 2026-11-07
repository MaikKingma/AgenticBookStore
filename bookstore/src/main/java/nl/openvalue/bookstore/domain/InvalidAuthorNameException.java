package nl.openvalue.bookstore.domain;

/** Thrown when an author name is missing, blank or longer than allowed. */
public class InvalidAuthorNameException extends RuntimeException {

    public InvalidAuthorNameException(String message) {
        super(message);
    }
}
