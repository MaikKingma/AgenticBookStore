package nl.openvalue.bookstore.domain;

/**
 * Thrown when a pending restock has a missing, blank or too long order id, a quantity below 1 or no expected delivery
 * date.
 */
public class InvalidPendingRestockException extends RuntimeException {

    public InvalidPendingRestockException(String message) {
        super(message);
    }
}
