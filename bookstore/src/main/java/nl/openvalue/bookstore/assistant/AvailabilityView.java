package nl.openvalue.bookstore.assistant;

import nl.openvalue.bookstore.domaininteraction.StockItemDTO;

/**
 * How many copies of a book can be bought now and, when a restock is on its way, the date it is expected as an ISO
 * date string ({@code null} when no restock is pending).
 */
public record AvailabilityView(int available, String pendingRestockExpected) {

    public static AvailabilityView from(StockItemDTO stock) {
        String expected = stock.pendingRestock() == null ? null
                : stock.pendingRestock().expectedDeliveryDate().toString();
        return new AvailabilityView(stock.available(), expected);
    }
}
