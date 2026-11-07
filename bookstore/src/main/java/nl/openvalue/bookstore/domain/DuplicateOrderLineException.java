package nl.openvalue.bookstore.domain;

/** Thrown when an order has more than one line for the same book. */
public class DuplicateOrderLineException extends RuntimeException {

    private final BookId bookId;

    public DuplicateOrderLineException(BookId bookId) {
        super("An order has one line per book, but book " + bookId.value() + " appears more than once");
        this.bookId = bookId;
    }

    public BookId bookId() {
        return bookId;
    }
}
