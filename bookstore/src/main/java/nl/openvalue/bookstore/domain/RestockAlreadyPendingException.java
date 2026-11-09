package nl.openvalue.bookstore.domain;

/** Thrown when a restock is requested for a book that already has a pending restock. */
public class RestockAlreadyPendingException extends RuntimeException {

    private final BookId bookId;
    private final PendingRestock pendingRestock;

    public RestockAlreadyPendingException(BookId bookId, PendingRestock pendingRestock) {
        super("Book " + bookId.value() + " already has restock " + pendingRestock.restockOrderId() + " pending");
        this.bookId = bookId;
        this.pendingRestock = pendingRestock;
    }

    public BookId bookId() {
        return bookId;
    }

    public PendingRestock pendingRestock() {
        return pendingRestock;
    }
}
