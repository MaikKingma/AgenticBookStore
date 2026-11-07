package nl.openvalue.bookstore.domaininteraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domain.Order;

/**
 * An order as it crosses a port: JDK types only. The status is the enum name, the shipped time is null until the order
 * is shipped, the lines keep their order. It carries no version and no events.
 */
public record OrderDTO(UUID id, String customerEmail, String status, Instant placedAt, Instant shippedAt,
        List<OrderLineDTO> lines) {

    public OrderDTO {
        lines = List.copyOf(lines);
    }

    public OrderDTO(Order order) {
        this(order.id().value(), order.customerEmail().value(), order.status().name(), order.placedAt(),
                order.shippedAt(), order.lines().stream().map(OrderLineDTO::new).toList());
    }

    /** The sum of the line totals, with two decimals. The same rule as {@code Order}. */
    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        for (OrderLineDTO line : lines) {
            total = total.add(line.lineTotal());
        }
        return total;
    }

    /** The currency code of the lines; every line of an order shares it. The same rule as {@code Order}. */
    public String currency() {
        return lines.get(0).unitPriceCurrency();
    }
}
