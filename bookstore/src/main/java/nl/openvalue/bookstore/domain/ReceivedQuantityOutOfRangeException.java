package nl.openvalue.bookstore.domain;

/** Thrown when the quantity of one delivery of stock is below the minimum or above the maximum. */
public class ReceivedQuantityOutOfRangeException extends RuntimeException {

    public ReceivedQuantityOutOfRangeException(int quantity, int min, int max) {
        super("A delivery holds " + min + " to " + max + " copies, not " + quantity);
    }
}
