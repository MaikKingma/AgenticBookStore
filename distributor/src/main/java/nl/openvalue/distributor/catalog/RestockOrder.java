package nl.openvalue.distributor.catalog;

import java.time.LocalDate;

/**
 * A restock order held in memory. {@code retailerReference} is optional and may be {@code null}.
 */
public record RestockOrder(
        String id,
        String ean,
        int quantity,
        String retailerReference,
        RestockStatus status,
        LocalDate expectedDeliveryDate) {
}
