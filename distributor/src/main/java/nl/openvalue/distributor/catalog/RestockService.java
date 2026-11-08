package nl.openvalue.distributor.catalog;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Computes delivery quotes and keeps restock orders in memory (they are lost on restart). Placing an order never
 * changes a title's warehouse quantity, and a confirmed order does not depend on how much stock there is.
 */
@Service
public class RestockService {

    private static final int IN_STOCK_WORKING_DAYS = 2;
    private static final int BACKORDER_CALENDAR_DAYS = 14;

    private final TitleCatalog catalog;
    private final Clock clock;
    private final Map<String, RestockOrder> orders = new ConcurrentHashMap<>();

    public RestockService(TitleCatalog catalog, Clock clock) {
        this.catalog = catalog;
        this.clock = clock;
    }

    /**
     * What a restock of this title would look like today: IN_STOCK is CONFIRMED in two working days (Saturday and
     * Sunday do not count), BACKORDER is BACKORDERED in fourteen calendar days, OUT_OF_PRINT cannot be ordered.
     */
    public Optional<DeliveryQuote> quote(Title title) {
        LocalDate today = LocalDate.now(clock);
        return switch (title.availability()) {
            case IN_STOCK -> Optional.of(new DeliveryQuote(
                    RestockStatus.CONFIRMED, addWorkingDays(today, IN_STOCK_WORKING_DAYS)));
            case BACKORDER -> Optional.of(new DeliveryQuote(
                    RestockStatus.BACKORDERED, today.plusDays(BACKORDER_CALENDAR_DAYS)));
            case OUT_OF_PRINT -> Optional.empty();
        };
    }

    /**
     * Places a restock order. Quantity and reference validation is the caller's job.
     *
     * @throws TitleNotFoundException if no title has the EAN
     * @throws NotOrderableException if the title is out of print
     */
    public RestockOrder placeOrder(String ean, int quantity, String retailerReference) {
        Title title = catalog.findByEan(ean).orElseThrow(() -> new TitleNotFoundException(ean));
        DeliveryQuote quote = quote(title).orElseThrow(() -> new NotOrderableException(ean));
        RestockOrder order = new RestockOrder(UUID.randomUUID().toString(), ean, quantity, retailerReference,
                quote.status(), quote.expectedDeliveryDate());
        orders.put(order.id(), order);
        return order;
    }

    /**
     * Looks an order up by id. A {@code null} id is simply unknown.
     */
    public Optional<RestockOrder> findOrder(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(orders.get(id));
    }

    int orderCount() {
        return orders.size();
    }

    private static LocalDate addWorkingDays(LocalDate start, int workingDays) {
        LocalDate date = start;
        int remaining = workingDays;
        while (remaining > 0) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }
        return date;
    }
}
