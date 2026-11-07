package nl.openvalue.bookstore.domaininteraction;

import java.util.UUID;

/** The book exists but can no longer be ordered, because it is discontinued. */
public final class BookNotOrderableException extends ConflictException {

    public BookNotOrderableException(UUID bookId) {
        super("book-not-orderable", "Book not orderable", "The book with id " + bookId + " cannot be ordered");
    }
}
