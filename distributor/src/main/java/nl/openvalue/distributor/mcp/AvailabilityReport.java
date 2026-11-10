package nl.openvalue.distributor.mcp;

import java.time.LocalDate;
import nl.openvalue.distributor.catalog.Availability;

/**
 * The {@code check_availability} result. {@code expectedDeliveryDate} is when a restock placed today would arrive,
 * or {@code null} when the title cannot be ordered (OUT_OF_PRINT).
 */
public record AvailabilityReport(
        String ean,
        Availability availability,
        int warehouseQuantity,
        LocalDate expectedDeliveryDate) {
}
