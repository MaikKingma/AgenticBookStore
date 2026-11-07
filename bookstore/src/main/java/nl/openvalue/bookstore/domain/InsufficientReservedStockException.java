package nl.openvalue.bookstore.domain;

/** Thrown when more copies of a book are shipped than are reserved for it. */
public class InsufficientReservedStockException extends RuntimeException {

    private final BookId bookId;
    private final int requested;
    private final int reserved;

    public InsufficientReservedStockException(BookId bookId, int requested, int reserved) {
        super("Book " + bookId.value() + " has " + reserved + " copies reserved, so " + requested
                + " cannot be shipped");
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
