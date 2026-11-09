package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.PendingRestock;
import nl.openvalue.bookstore.domain.StockItem;

/**
 * Rebuilds a {@link StockItem} from a {@link StockItemDTO}; the stock item checks every invariant again, and the
 * pending restock checks every value of its own.
 */
public final class StockItemDomainMapper {

    private StockItemDomainMapper() {
    }

    /** The clock is kept by the stock item and stamps the events it registers after it is restored. */
    public static StockItem toDomain(StockItemDTO dto, Clock clock) {
        return StockItem.restore(
                new BookId(dto.bookId()),
                dto.onHand(),
                dto.reserved(),
                dto.reorderLevel(),
                dto.reorderQuantity(),
                dto.lowStockSignalled(),
                toDomain(dto.pendingRestock()),
                clock);
    }

    /** A missing pending restock stays missing; any other one is checked by {@link PendingRestock}. */
    private static PendingRestock toDomain(PendingRestockDTO dto) {
        if (dto == null) {
            return null;
        }
        return new PendingRestock(dto.restockOrderId(), dto.quantity(), dto.expectedDeliveryDate(),
                dto.backordered());
    }
}
