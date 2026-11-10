package nl.openvalue.bookstore.domain;

/** Thrown when more copies of a book are shipped or released than are reserved for it. */
public class InsufficientReservedStockException extends RuntimeException {

    private final BookId bookId;
    private final int requested;
    private final int reserved;

    /** The copies were to be shipped. */
    public InsufficientReservedStockException(BookId bookId, int requested, int reserved) {
        this(bookId, requested, reserved, "shipped");
    }

    /** The copies were to be taken out of the reserved stock in the given way, for example "released". */
    public InsufficientReservedStockException(BookId bookId, int requested, int reserved, String action) {
        super("Book " + bookId.value() + " has " + reserved + " copies reserved, so " + requested
                + " cannot be " + action);
        this.bookId = bookId;
        this.requested = requested;
        this.reserved = reserved;
    }

    public BookId bookId() {
        return bookId;
    }

    public int requested() {
        return requested;
    }

    public int reserved() {
        return reserved;
    }
}
