package nl.openvalue.bookstore.data;

import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * Maps between {@link StockItemDTO} and {@link StockItemJPA}. The book id is written only on creation. The pending
 * restock is stored in four columns that are all null when there is none.
 */
final class StockItemJPAMapper {

    private StockItemJPAMapper() {
    }

    static StockItemJPA toNewEntity(StockItemDTO dto) {
        StockItemJPA entity = new StockItemJPA(dto.bookId());
        copyInto(dto, entity);
        return entity;
    }

    /**
     * Copies every changeable field into the entity; the book id never changes. A stock item without a pending restock
     * clears all four pending restock columns.
     */
    static void copyInto(StockItemDTO dto, StockItemJPA entity) {
        entity.setOnHand(dto.onHand());
        entity.setReserved(dto.reserved());
        entity.setReorderLevel(dto.reorderLevel());
        entity.setReorderQuantity(dto.reorderQuantity());
        entity.setLowStockSignalled(dto.lowStockSignalled());
        PendingRestockDTO pending = dto.pendingRestock();
        entity.setPendingRestockOrderId(pending == null ? null : pending.restockOrderId());
        entity.setPendingRestockQuantity(pending == null ? null : pending.quantity());
        entity.setPendingRestockExpectedDeliveryDate(pending == null ? null : pending.expectedDeliveryDate());
        entity.setPendingRestockBackordered(pending == null ? null : pending.backordered());
    }

    static StockItemDTO toDTO(StockItemJPA entity) {
        return new StockItemDTO(entity.getBookId(), entity.getOnHand(), entity.getReserved(),
                entity.getReorderLevel(), entity.getReorderQuantity(), entity.isLowStockSignalled(),
                toPendingRestockDTO(entity));
    }

    /** The order id tells whether a pending restock is stored: the application writes all four columns or none. */
    private static PendingRestockDTO toPendingRestockDTO(StockItemJPA entity) {
        if (entity.getPendingRestockOrderId() == null) {
            return null;
        }
        return new PendingRestockDTO(entity.getPendingRestockOrderId(), entity.getPendingRestockQuantity(),
                entity.getPendingRestockExpectedDeliveryDate(), entity.getPendingRestockBackordered());
    }
}
