package nl.openvalue.distributor.api;

import java.math.BigDecimal;

/**
 * The body of {@code POST /api/restock-orders}. Every component may be missing on the wire, so the controller checks
 * them by hand. {@code retailerReference} is optional.
 *
 * <p>{@code quantity} is a {@link BigDecimal}, not an {@code Integer}: Jackson reads a JSON number such as
 * {@code 500.7} into an {@code Integer} by cutting off the fraction, so the controller would only ever see 500. As a
 * {@code BigDecimal} the exact value arrives and the controller can reject a quantity that is not a whole number.
 */
public record RestockOrderRequest(String ean, BigDecimal quantity, String retailerReference) {
}
