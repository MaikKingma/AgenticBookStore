package nl.openvalue.bookstore.domaininteraction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The catalogue's store of books. It speaks DTOs and JDK types only. */
public interface BookDataService {

    /**
     * Stores the book: inserts it when its id is unknown, otherwise updates the stored book.
     *
     * @throws IsbnAlreadyExistsException when another book already holds the ISBN
     */
    void save(BookDTO book);

    /**
     * Loads one book.
     *
     * @throws BookNotFoundException when no book has the id
     */
    BookDTO findById(UUID id);

    /** Finds the book with the ISBN, empty when there is none. */
    Optional<BookDTO> findByIsbn(String isbn);

    /**
     * Searches the catalogue. A {@code null} argument means no constraint on it. {@code genre} is the name of a
     * {@code Genre}. The result is sorted by title and holds at most {@code limit} books.
     */
    List<BookDTO> search(String q, String title, String author, String genre, int limit);

    /** No book has the requested id. */
    final class BookNotFoundException extends NotFoundException {

        public BookNotFoundException(UUID id) {
            super("book-not-found", "Book not found", "No book with id " + id);
        }
    }

    /** Another book already holds the ISBN. */
    final class IsbnAlreadyExistsException extends ConflictException {

        public IsbnAlreadyExistsException(String isbn) {
            super("isbn-already-exists", "ISBN already in catalogue",
                    "A book with ISBN " + isbn + " is already in the catalogue");
        }
    }
}
