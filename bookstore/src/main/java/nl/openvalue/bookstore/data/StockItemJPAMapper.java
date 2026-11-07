package nl.openvalue.bookstore.data;

import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/** Maps between {@link StockItemDTO} and {@link StockItemJPA}. The book id is written only on creation. */
final class StockItemJPAMapper {

    private StockItemJPAMapper() {
    }

    static StockItemJPA toNewEntity(StockItemDTO dto) {
        StockItemJPA entity = new StockItemJPA(dto.bookId());
        copyInto(dto, entity);
        return entity;
    }

    /** Copies every changeable field into the entity; the book id never changes. */
    static void copyInto(StockItemDTO dto, StockItemJPA entity) {
        entity.setOnHand(dto.onHand());
        entity.setReserved(dto.reserved());
        entity.setReorderLevel(dto.reorderLevel());
        entity.setReorderQuantity(dto.reorderQuantity());
        entity.setLowStockSignalled(dto.lowStockSignalled());
    }

    static StockItemDTO toDTO(StockItemJPA entity) {
        return new StockItemDTO(entity.getBookId(), entity.getOnHand(), entity.getReserved(),
                entity.getReorderLevel(), entity.getReorderQuantity(), entity.isLowStockSignalled());
    }
}
