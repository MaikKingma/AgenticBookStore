package nl.openvalue.bookstore.domain;

/** A number of copies of a book: 0 to 10000. */
public record Quantity(int value) {

    private static final int MIN = 0;
    private static final int MAX = 10000;

    public Quantity {
        if (value < MIN || value > MAX) {
            throw new InvalidQuantityException(
                    "A quantity is between " + MIN + " and " + MAX + ", not " + value);
        }
    }
}
