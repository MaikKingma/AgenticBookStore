package nl.openvalue.bookstore.domaininteraction;

import java.util.UUID;
import nl.openvalue.bookstore.domain.StockItem;

/**
 * A stock item as it crosses a port: JDK types only. The book id names the stock item, the low-stock signal says
 * whether the running-low event was already registered for the current dip. It carries no version and no events.
 */
public record StockItemDTO(UUID bookId, int onHand, int reserved, int reorderLevel, int reorderQuantity,
        boolean lowStockSignalled) {

    public StockItemDTO(StockItem item) {
        this(item.bookId().value(), item.onHand(), item.reserved(), item.reorderLevel(), item.reorderQuantity(),
                item.lowStockSignalled());
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
