package nl.openvalue.bookstore.domain;

/** Thrown when an order has fewer lines than the minimum or more than the maximum. */
public class OrderLineCountOutOfRangeException extends RuntimeException {

    private final int count;
    private final int min;
    private final int max;

    public OrderLineCountOutOfRangeException(int count, int min, int max) {
        super("An order has " + min + " to " + max + " lines, not " + count);
        this.count = count;
        this.min = min;
        this.max = max;
    }

    public int count() {
        return count;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}
