package nl.openvalue.bookstore.domain;

/** Thrown when a book title is missing, blank or longer than allowed. */
public class InvalidTitleException extends RuntimeException {

    public InvalidTitleException(String message) {
        super(message);
    }
}
