package nl.openvalue.bookstore.domaininteraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import nl.openvalue.bookstore.domain.OrderLine;

/**
 * One order line as it crosses a port: JDK types only. The unit price is an amount plus a currency code, the isbn,
 * title and price are the snapshots taken when the order was placed.
 */
public record OrderLineDTO(UUID bookId, String isbn, String title, BigDecimal unitPriceAmount,
        String unitPriceCurrency, int quantity) {

    public OrderLineDTO(OrderLine line) {
        this(line.bookId().value(), line.isbn(), line.title(), line.unitPrice().amount(),
                line.unitPrice().currency().getCurrencyCode(), line.quantity().value());
    }

    /** The unit price times the quantity, with two decimals, rounded half even. The same rule as {@code OrderLine}. */
    public BigDecimal lineTotal() {
        return unitPriceAmount.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
