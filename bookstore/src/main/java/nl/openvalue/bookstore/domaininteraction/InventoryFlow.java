package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import nl.openvalue.bookstore.domain.InvalidPendingRestockException;
import nl.openvalue.bookstore.domain.InvalidQuantityException;
import nl.openvalue.bookstore.domain.InvalidStocktakeReasonException;
import nl.openvalue.bookstore.domain.OnHandBelowReservedException;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.ReceivedQuantityOutOfRangeException;
import nl.openvalue.bookstore.domain.StockItem;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.DistributorUnavailableException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotFoundAtDistributorException;
import nl.openvalue.bookstore.domaininteraction.DistributorAppService.TitleNotOrderableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inventory use cases. Every domain exception is translated here into one of the four use-case exceptions; port
 * exceptions pass through unchanged, except where the event-driven restock handles the distributor's exceptions
 * itself. A command saves the stock item first and then publishes the events it pulled, inside the same transaction,
 * so listeners bound to the commit only see events of saved stock.
 */
@Service
@Transactional
public class InventoryFlow {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryFlow.class);

    private final StockItemDataService stockItems;
    private final BookDataService books;
    private final DistributorAppService distributor;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public InventoryFlow(StockItemDataService stockItems, BookDataService books, DistributorAppService distributor,
            ApplicationEventPublisher events, Clock clock) {
        this.stockItems = stockItems;
        this.books = books;
        this.distributor = distributor;
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

    /**
     * Asks the distributor to restock the book on request of a client and stores the restock it confirmed. While a
     * restock is pending it returns without calling the distributor. The distributor's exceptions pass through, and an
     * answer that is no usable restock becomes a {@link DistributorUnavailableException}.
     */
    public void requestRestock(UUID bookId) {
        Optional<RestockRequest> prepared = prepareRestock(bookId);
        if (prepared.isEmpty()) {
            return;
        }
        RestockRequest request = prepared.get();
        StockItem item = request.item();
        recordConfirmation(item, distributor.requestRestock(request.isbn(), request.quantity(), request.reference()));
        saveAndPublish(item);
    }

    /**
     * Asks the distributor to restock the book after its stock ran low and stores the restock it confirmed. While a
     * restock is pending it returns without calling the distributor. It handles the distributor's own exceptions here,
     * so none of them crosses the transaction boundary: when the distributor is unavailable or answers unusably, it
     * logs a warning and resets the low-stock signal so that the next reservation starts a new attempt; when the
     * distributor refuses the title, it logs a warning and keeps the signal, so there is no automatic retry. Only the
     * distributor call and the recording of its answer sit inside the handling, never a save; every other exception
     * passes through.
     */
    public void handleStockRunningLow(UUID bookId) {
        Optional<RestockRequest> prepared = prepareRestock(bookId);
        if (prepared.isEmpty()) {
            return;
        }
        RestockRequest request = prepared.get();
        StockItem item = request.item();
        try {
            recordConfirmation(item,
                    distributor.requestRestock(request.isbn(), request.quantity(), request.reference()));
        } catch (DistributorUnavailableException e) {
            LOG.warn("Restock request for book {} failed, the low-stock signal is reset: {}", bookId, e.getMessage());
            item.restockRequestFailed();
        } catch (TitleNotFoundAtDistributorException | TitleNotOrderableException e) {
            LOG.warn("Distributor refused the restock of book {}, the low-stock signal stays: {}", bookId,
                    e.getMessage());
            return;
        }
        saveAndPublish(item);
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
     * The shared first step of both restock use cases: loads the stock item, and when no restock is pending, gathers
     * what the distributor needs. The quantity is the reorder quantity, the reference is the book id and the epoch
     * seconds of the clock. Empty when a restock is pending, before the book is looked up.
     */
    private Optional<RestockRequest> prepareRestock(UUID bookId) {
        StockItemDTO stored = stockItems.findByBookId(bookId);
        StockItem item = translatingDomainExceptions("quantity", () -> StockItemDomainMapper.toDomain(stored, clock));
        if (item.hasPendingRestock()) {
            return Optional.empty();
        }
        String isbn = books.findById(bookId).isbn();
        return Optional.of(new RestockRequest(item, isbn, item.reorderQuantity(),
                bookId + "/" + clock.instant().getEpochSecond()));
    }

    /**
     * Records the confirmed restock on the stock item, in memory only. An answer that breaks a rule of a pending
     * restock is a distributor that answers unusably.
     */
    private static void recordConfirmation(StockItem item, RestockConfirmationDTO confirmation) {
        try {
            // No RestockAlreadyPendingException here: prepareRestock gives no request while a restock is pending.
            item.restockRequested(confirmation.restockOrderId(), confirmation.quantity(),
                    confirmation.expectedDeliveryDate(), confirmation.backordered());
        } catch (InvalidPendingRestockException e) {
            throw new DistributorUnavailableException("The distributor answered the restock request for book "
                    + item.bookId().value() + " unusably: " + e.getMessage(), e);
        }
    }

    private void saveAndPublish(StockItem item) {
        stockItems.save(new StockItemDTO(item));
        item.pullDomainEvents().forEach(events::publishEvent);
    }

    /** What the distributor needs for one restock request, with the loaded stock item that records the answer. */
    private record RestockRequest(StockItem item, String isbn, int quantity, String reference) {
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
