package nl.openvalue.bookstore.domain;

/** Thrown when the price of a discontinued book is changed. */
public class BookDiscontinuedException extends RuntimeException {

    public BookDiscontinuedException(BookId id) {
        super("Book " + id.value() + " is discontinued, so its price cannot change");
    }
}
