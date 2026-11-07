package nl.openvalue.bookstore.domaininteraction;

import java.util.List;
import java.util.UUID;

/** The store of stock items, one per book. It speaks DTOs and JDK types only. */
public interface StockItemDataService {

    /** Stores the stock item: inserts it when no stock item exists for the book, otherwise updates the stored one. */
    void save(StockItemDTO stockItem);

    /**
     * Loads the stock item of one book.
     *
     * @throws StockItemNotFoundException when no stock item exists for the book
     */
    StockItemDTO findByBookId(UUID bookId);

    /**
     * Finds every stock item whose available stock is at or below its own reorder level. The result is ordered by
     * available stock ascending, then by book id.
     */
    List<StockItemDTO> findLowStock();

    /** No stock item exists for the requested book. */
    final class StockItemNotFoundException extends NotFoundException {

        public StockItemNotFoundException(UUID bookId) {
            super("book-not-found", "Book not found", "No stock item for the book with id " + bookId);
        }
    }
}
