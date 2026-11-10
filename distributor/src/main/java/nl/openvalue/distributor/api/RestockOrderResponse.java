package nl.openvalue.distributor.api;

import java.time.LocalDate;
import nl.openvalue.distributor.catalog.RestockOrder;
import nl.openvalue.distributor.catalog.RestockStatus;

/**
 * The wire view of a restock order: exactly five keys. The retailer reference is stored but not echoed.
 */
public record RestockOrderResponse(
        String restockOrderId,
        String ean,
        int quantity,
        RestockStatus status,
        LocalDate expectedDeliveryDate) {

    static RestockOrderResponse of(RestockOrder order) {
        return new RestockOrderResponse(order.id(), order.ean(), order.quantity(), order.status(),
                order.expectedDeliveryDate());
    }
}
