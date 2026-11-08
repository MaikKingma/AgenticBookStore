package nl.openvalue.distributor.api;

/**
 * The body of {@code POST /api/restock-orders}. Every component may be missing on the wire, so the controller checks
 * them by hand. {@code retailerReference} is optional.
 */
public record RestockOrderRequest(String ean, Integer quantity, String retailerReference) {
}
