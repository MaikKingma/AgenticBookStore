package nl.openvalue.bookstore.data;

import java.util.List;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;
import nl.openvalue.bookstore.domaininteraction.OrderLineDTO;

/**
 * Maps between {@link OrderDTO} and {@link OrderJPA}. Id, e-mail address, placed time and lines are written only when
 * an entity is created.
 */
final class OrderJPAMapper {

    private OrderJPAMapper() {
    }

    static OrderJPA toNewEntity(OrderDTO dto) {
        List<OrderLineJPA> lines = dto.lines().stream()
                .map(line -> new OrderLineJPA(line.bookId(), line.isbn(), line.title(), line.unitPriceAmount(),
                        line.unitPriceCurrency(), line.quantity()))
                .toList();
        OrderJPA entity = new OrderJPA(dto.id(), dto.customerEmail(), dto.placedAt(), lines);
        copyInto(dto, entity);
        return entity;
    }

    /**
     * Copies the only changeable fields, the status, the shipped time and the cancelled time, into the entity. The
     * lines are never touched: replacing them, even with equal values, would bump the version of an unchanged order.
     */
    static void copyInto(OrderDTO dto, OrderJPA entity) {
        entity.setStatus(dto.status());
        entity.setShippedAt(dto.shippedAt());
        entity.setCancelledAt(dto.cancelledAt());
    }

    /** Maps the entity with its lines in position order; inside a transaction, because the lines are lazy. */
    static OrderDTO toDTO(OrderJPA entity) {
        List<OrderLineDTO> lines = entity.getLines().stream()
                .map(line -> new OrderLineDTO(line.getBookId(), line.getIsbn(), line.getTitle(),
                        line.getUnitPriceAmount(), line.getUnitPriceCurrency(), line.getQuantity()))
                .toList();
        return new OrderDTO(entity.getId(), entity.getCustomerEmail(), entity.getStatus(), entity.getPlacedAt(),
                entity.getShippedAt(), entity.getCancelledAt(), lines);
    }
}
