package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import nl.openvalue.bookstore.domain.InvalidQuantityException;
import nl.openvalue.bookstore.domain.InvalidStocktakeReasonException;
import nl.openvalue.bookstore.domain.OnHandBelowReservedException;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.ReceivedQuantityOutOfRangeException;
import nl.openvalue.bookstore.domain.StockItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inventory use cases. Every domain exception is translated here into one of the four use-case exceptions; port
 * exceptions pass through unchanged. A command saves the stock item first and then publishes the events it pulled,
 * inside the same transaction, so listeners bound to the commit only see events of saved stock.
 */
@Service
@Transactional
public class InventoryFlow {

    private final StockItemDataService stockItems;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public InventoryFlow(StockItemDataService stockItems, ApplicationEventPublisher events, Clock clock) {
        this.stockItems = stockItems;
        this.events = events;
        this.clock = clock;
    }

    /** Adds a delivery to the stock on hand; the stock item is loaded before the quantity is checked. */
    public void receive(UUID bookId, int quantity) {
        StockItemDTO stored = stockItems.findByBookId(bookId);
        StockItem item = translatingDomainExceptions("quantity", () -> {
            StockItem restored = StockItemDomainMapper.toDomain(stored, clock);
            restored.receive(new Quantity(quantity));
            return restored;
        });
        stockItems.save(new StockItemDTO(item));
        item.pullDomainEvents().forEach(events::publishEvent);
    }

    /**
     * Sets the stock on hand after a stocktake and publishes the running-low event when the correction starts a dip;
     * the stock item is loaded before the figures are checked.
     */
    public void correct(UUID bookId, int onHand, String reason) {
        StockItemDTO stored = stockItems.findByBookId(bookId);
        StockItem item = translatingDomainExceptions("onHand", () -> {
            StockItem restored = StockItemDomainMapper.toDomain(stored, clock);
            restored.correct(new Quantity(onHand), reason);
            return restored;
        });
        stockItems.save(new StockItemDTO(item));
        item.pullDomainEvents().forEach(events::publishEvent);
    }

    @Transactional(readOnly = true)
    public StockItemDTO findByBookId(UUID bookId) {
        return stockItems.findByBookId(bookId);
    }

    /** The stock items at or below their reorder level, in the order of the port: most urgent first. */
    @Transactional(readOnly = true)
    public List<StockItemDTO> findLowStock() {
        return stockItems.findLowStock();
    }

    /**
     * Runs a call into the domain and translates any domain exception it throws. The quantity field names the request
     * value a quantity error points at.
     */
    private static <T> T translatingDomainExceptions(String quantityField, Supplier<T> domainCall) {
        try {
            return domainCall.get();
        } catch (InvalidQuantityException | ReceivedQuantityOutOfRangeException e) {
            throw invalidField("quantity-out-of-range", "Quantity out of range", quantityField, e);
        } catch (InvalidStocktakeReasonException e) {
            throw invalidField("invalid-reason", "Invalid reason", "reason", e);
        } catch (OnHandBelowReservedException e) {
            throw new ConflictException("on-hand-below-reserved", "On hand below reserved", e.getMessage());
        }
    }

    private static InvalidInputException invalidField(String code, String title, String field,
            RuntimeException domainException) {
        String message = domainException.getMessage();
        return new InvalidInputException(code, title, message, List.of(Map.of("field", field, "message", message)));
    }
}
