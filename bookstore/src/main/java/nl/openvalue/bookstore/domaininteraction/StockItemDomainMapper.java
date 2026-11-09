package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.StockItem;

/** Rebuilds a {@link StockItem} from a {@link StockItemDTO}; the stock item checks every invariant again. */
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
                null, // the stock item DTO does not carry a pending restock yet
                clock);
    }
}
