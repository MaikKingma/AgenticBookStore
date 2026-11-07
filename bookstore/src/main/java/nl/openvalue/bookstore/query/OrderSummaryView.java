package nl.openvalue.bookstore.query;

import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;
import nl.openvalue.bookstore.domaininteraction.OrderLineDTO;

/**
 * An order as one row of a customer's history: its status, when it was placed (an ISO-8601 string), how many items it
 * holds (the sum of the line quantities) and the total as a plain decimal string.
 */
public record OrderSummaryView(UUID id, String status, String placedAt, int itemCount, String total) {

    public static OrderSummaryView from(OrderDTO order) {
        return new OrderSummaryView(order.id(), order.status(), order.placedAt().toString(),
                order.lines().stream().mapToInt(OrderLineDTO::quantity).sum(), order.total().toPlainString());
    }
}
