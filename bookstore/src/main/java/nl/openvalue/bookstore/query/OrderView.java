package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;

/**
 * An order as a reader sees it: who placed it, its status, when it was placed, shipped and cancelled (as ISO-8601
 * strings, the shipped time is null until the order is shipped and the cancelled time is null until the order is
 * cancelled), its lines in order, the total as a plain decimal string and the currency of the lines.
 */
public record OrderView(UUID id, String customerEmail, String status, String placedAt, String shippedAt,
        String cancelledAt, List<OrderLineView> lines, String total, String currency) {

    public static OrderView from(OrderDTO order) {
        return new OrderView(order.id(), order.customerEmail(), order.status(), order.placedAt().toString(),
                order.shippedAt() == null ? null : order.shippedAt().toString(),
                order.cancelledAt() == null ? null : order.cancelledAt().toString(),
                order.lines().stream().map(OrderLineView::from).toList(), order.total().toPlainString(),
                order.currency());
    }
}
