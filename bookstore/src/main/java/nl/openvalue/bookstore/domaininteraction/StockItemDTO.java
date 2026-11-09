package nl.openvalue.bookstore.domaininteraction;

import java.util.UUID;
import nl.openvalue.bookstore.domain.StockItem;

/**
 * A stock item as it crosses a port: JDK types only. The book id names the stock item, the low-stock signal says
 * whether the running-low event was already registered for the current dip, and the pending restock is the restock the
 * distributor confirmed and no delivery has completed yet ({@code null} when there is none). It carries no version and
 * no events.
 */
public record StockItemDTO(UUID bookId, int onHand, int reserved, int reorderLevel, int reorderQuantity,
        boolean lowStockSignalled, PendingRestockDTO pendingRestock) {

    /**
     * A convenience for a stock item without a pending restock: the same as passing {@code null} as the pending
     * restock. Production code always uses the full form, so a pending restock is never dropped on the way.
     */
    public StockItemDTO(UUID bookId, int onHand, int reserved, int reorderLevel, int reorderQuantity,
            boolean lowStockSignalled) {
        this(bookId, onHand, reserved, reorderLevel, reorderQuantity, lowStockSignalled, null);
    }

    public StockItemDTO(StockItem item) {
        this(item.bookId().value(), item.onHand(), item.reserved(), item.reorderLevel(), item.reorderQuantity(),
                item.lowStockSignalled(), item.pendingRestock().map(PendingRestockDTO::new).orElse(null));
    }

    /** The stock that can still be reserved: on hand minus reserved. The same rule as {@code StockItem}. */
    public int available() {
        return onHand - reserved;
    }

    /** True when the available stock is at or below the reorder level. The same rule as {@code StockItem}. */
    public boolean lowStock() {
        return available() <= reorderLevel;
    }
}
