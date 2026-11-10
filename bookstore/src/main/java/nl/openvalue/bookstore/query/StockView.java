package nl.openvalue.bookstore.query;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * The stock of one book as a reader sees it: the figures on the shelf, what is still available, whether it is low
 * and the restock that is on its way. The pending restock is always part of the JSON and is {@code null} when no
 * restock is pending. The reorder quantity and the low-stock signal are bookkeeping of the stock item and are not
 * shown.
 */
public record StockView(UUID bookId, int onHand, int reserved, int available, int reorderLevel, boolean lowStock,
        PendingRestockView pendingRestock) {

    public static StockView from(StockItemDTO stock) {
        PendingRestockDTO pending = stock.pendingRestock();
        return new StockView(stock.bookId(), stock.onHand(), stock.reserved(), stock.available(),
                stock.reorderLevel(), stock.lowStock(), pending == null ? null : PendingRestockView.from(pending));
    }
}
