package nl.openvalue.bookstore.domain;

/** Thrown when a value is not a valid ISBN-13 with prefix 978 or 979. */
public class InvalidIsbnException extends RuntimeException {

    public InvalidIsbnException(String isbn, String reason) {
        super(isbn == null ? "An ISBN is required" : "ISBN '" + isbn + "' is invalid: " + reason);
    }
}
