package nl.openvalue.bookstore.domain;

/** Thrown when more copies of a book are reserved than are available. */
public class InsufficientStockException extends RuntimeException {

    private final BookId bookId;
    private final int requested;
    private final int available;

    public InsufficientStockException(BookId bookId, int requested, int available) {
        super("Book " + bookId.value() + " has " + available + " copies available, so " + requested
                + " cannot be reserved");
        this.bookId = bookId;
        this.requested = requested;
        this.available = available;
    }

    public BookId bookId() {
        return bookId;
    }

    public int requested() {
        return requested;
    }

    public int available() {
        return available;
    }
}
