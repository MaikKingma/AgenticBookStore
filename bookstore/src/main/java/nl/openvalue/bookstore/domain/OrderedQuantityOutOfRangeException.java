package nl.openvalue.bookstore.domain;

/** Thrown when one line of an order asks for fewer copies than the minimum or more than the maximum. */
public class OrderedQuantityOutOfRangeException extends RuntimeException {

    private final int quantity;
    private final int min;
    private final int max;

    public OrderedQuantityOutOfRangeException(int quantity, int min, int max) {
        super("An order line holds " + min + " to " + max + " copies, not " + quantity);
        this.quantity = quantity;
        this.min = min;
        this.max = max;
    }

    public int quantity() {
        return quantity;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}
