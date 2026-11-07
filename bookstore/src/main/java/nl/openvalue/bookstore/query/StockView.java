package nl.openvalue.bookstore.query;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * The stock of one book as a reader sees it: the figures on the shelf, what is still available and whether it is
 * low. The reorder quantity and the low-stock signal are bookkeeping of the stock item and are not shown.
 */
public record StockView(UUID bookId, int onHand, int reserved, int available, int reorderLevel, boolean lowStock) {

    public static StockView from(StockItemDTO stock) {
        return new StockView(stock.bookId(), stock.onHand(), stock.reserved(), stock.available(),
                stock.reorderLevel(), stock.lowStock());
    }
}
