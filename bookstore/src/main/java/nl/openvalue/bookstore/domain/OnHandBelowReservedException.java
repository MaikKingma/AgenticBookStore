package nl.openvalue.bookstore.domain;

/** Thrown when the stock on hand of a book would be lower than the stock reserved for it. */
public class OnHandBelowReservedException extends RuntimeException {

    private final BookId bookId;
    private final int onHand;
    private final int reserved;

    public OnHandBelowReservedException(BookId bookId, int onHand, int reserved) {
        super("Book " + bookId.value() + " cannot have " + onHand + " on hand while " + reserved
                + " are reserved");
        this.bookId = bookId;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    public BookId bookId() {
        return bookId;
    }

    public int onHand() {
        return onHand;
    }

    public int reserved() {
        return reserved;
    }
}
