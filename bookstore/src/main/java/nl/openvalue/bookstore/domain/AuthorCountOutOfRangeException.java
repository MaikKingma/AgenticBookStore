package nl.openvalue.bookstore.domain;

/** Thrown when a book would have fewer or more authors than allowed. */
public class AuthorCountOutOfRangeException extends RuntimeException {

    public AuthorCountOutOfRangeException(int count, int min, int max) {
        super("A book has " + min + " to " + max + " authors, not " + count);
    }
}
