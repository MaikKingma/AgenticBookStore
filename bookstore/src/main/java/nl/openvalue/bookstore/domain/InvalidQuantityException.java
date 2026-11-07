package nl.openvalue.bookstore.domain;

/** Thrown when a quantity, a reserved stock figure or a reorder setting is outside its allowed range. */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
