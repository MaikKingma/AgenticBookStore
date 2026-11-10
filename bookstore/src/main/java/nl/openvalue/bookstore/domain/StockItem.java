package nl.openvalue.bookstore.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The stock of one book. New stock items come from {@link #openFor}, stored ones from {@link #restore}; both check
 * every invariant. The clock is kept to stamp the events this stock item registers. A stock item has at most one
 * pending restock: a restock the distributor confirmed and that no delivery has completed yet.
 */
public final class StockItem {

    private static final int DEFAULT_REORDER_LEVEL = 3;
    private static final int DEFAULT_REORDER_QUANTITY = 10;
    private static final int MIN_RECEIVED = 1;
    private static final int MAX_RECEIVED = 1000;
    private static final int MIN_REASON_LENGTH = 3;
    private static final int MAX_REASON_LENGTH = 200;

    private final BookId bookId;
    private int onHand;
    private int reserved;
    private final int reorderLevel;
    private final int reorderQuantity;
    private boolean lowStockSignalled;
    private PendingRestock pendingRestock;
    private final Clock clock;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private StockItem(BookId bookId, int onHand, int reserved, int reorderLevel, int reorderQuantity,
            boolean lowStockSignalled, PendingRestock pendingRestock, Clock clock) {
        this.bookId = Objects.requireNonNull(bookId, "bookId");
        this.clock = Objects.requireNonNull(clock, "clock");
        requireNotNegative(reserved, "reserved stock");
        requireNotNegative(reorderLevel, "reorder level");
        requireNotNegative(reorderQuantity, "reorder quantity");
        requireOnHandNotBelowReserved(bookId, onHand, reserved);
        this.onHand = onHand;
        this.reserved = reserved;
        this.reorderLevel = reorderLevel;
        this.reorderQuantity = reorderQuantity;
        this.lowStockSignalled = lowStockSignalled;
        this.pendingRestock = pendingRestock;
    }

    /**
     * Opens an empty stock item for a book with the default reorder settings and no pending restock; it registers no
     * event.
     */
    public static StockItem openFor(BookId bookId, Clock clock) {
        return new StockItem(bookId, 0, 0, DEFAULT_REORDER_LEVEL, DEFAULT_REORDER_QUANTITY, false, null, clock);
    }

    /**
     * Rebuilds a stored stock item with every figure, its signal and its pending restock ({@code null} when there is
     * none), checking every invariant again.
     */
    public static StockItem restore(BookId bookId, int onHand, int reserved, int reorderLevel, int reorderQuantity,
            boolean lowStockSignalled, PendingRestock pendingRestock, Clock clock) {
        return new StockItem(bookId, onHand, reserved, reorderLevel, reorderQuantity, lowStockSignalled,
                pendingRestock, clock);
    }

    /**
     * Adds a delivery of 1 to 1000 copies to the stock on hand. When the available stock ends above the reorder
     * level, the low-stock signal is reset so that the next dip registers a new event. Every accepted delivery clears a
     * pending restock, whatever its size: a delivery smaller than the pending quantity completes the restock too, and
     * the stock can stay low and signalled until a new restock is requested. A rejected delivery changes nothing.
     * Never registers an event.
     */
    public void receive(Quantity quantity) {
        if (quantity.value() < MIN_RECEIVED || quantity.value() > MAX_RECEIVED) {
            throw new ReceivedQuantityOutOfRangeException(quantity.value(), MIN_RECEIVED, MAX_RECEIVED);
        }
        onHand += quantity.value();
        pendingRestock = null;
        if (available() > reorderLevel) {
            lowStockSignalled = false;
        }
    }

    /**
     * Reserves copies out of the available stock. When the available stock is too low, nothing changes. Registers a
     * {@link StockRunningLowEvent} when this reservation starts a dip to or below the reorder level.
     */
    public void reserve(Quantity quantity) {
        if (available() < quantity.value()) {
            throw new InsufficientStockException(bookId, quantity.value(), available());
        }
        reserved += quantity.value();
        signalLowStockOncePerDip();
    }

    /**
     * Ships reserved copies: on hand and reserved both drop, so the available stock stays the same. When fewer copies
     * are reserved, nothing changes. Never registers an event and never touches the low-stock signal.
     */
    public void ship(Quantity quantity) {
        if (reserved < quantity.value()) {
            throw new InsufficientReservedStockException(bookId, quantity.value(), reserved);
        }
        onHand -= quantity.value();
        reserved -= quantity.value();
    }

    /**
     * Sets the stock on hand after a stocktake. The reason has 3 to 200 characters of any kind and is checked before
     * the reserved stock; on hand never drops below reserved. A rejected correction changes nothing. Registers a
     * {@link StockRunningLowEvent} when this correction starts a dip to or below the reorder level, and never resets
     * the low-stock signal.
     */
    public void correct(Quantity newOnHand, String reason) {
        requireValidReason(reason);
        requireOnHandNotBelowReserved(bookId, newOnHand.value(), reserved);
        onHand = newOnHand.value();
        signalLowStockOncePerDip();
    }

    /**
     * Records the restock the distributor confirmed: the confirmed order id, quantity and expected delivery date, and
     * whether the distributor has to wait for new stock. Figures and the low-stock signal stay as they are and no event
     * is registered. A book has at most one pending restock: when one is pending, nothing changes.
     */
    public void restockRequested(String restockOrderId, int quantity, LocalDate expectedDeliveryDate,
            boolean backordered) {
        if (pendingRestock != null) {
            throw new RestockAlreadyPendingException(bookId, pendingRestock);
        }
        pendingRestock = new PendingRestock(restockOrderId, quantity, expectedDeliveryDate, backordered);
    }

    /**
     * Records that asking the distributor for a restock failed: it resets the low-stock signal so that the next
     * reservation starts a new attempt. A pending restock, the figures and the events stay as they are, and calling it
     * again changes nothing. A distributor that refuses the title is not a failure in this sense: the signal stays.
     */
    public void restockRequestFailed() {
        lowStockSignalled = false;
    }

    /** Returns the events registered since the last pull, and forgets them. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public BookId bookId() {
        return bookId;
    }

    public int onHand() {
        return onHand;
    }

    public int reserved() {
        return reserved;
    }

    /** The stock that can still be reserved: on hand minus reserved. */
    public int available() {
        return onHand - reserved;
    }

    public int reorderLevel() {
        return reorderLevel;
    }

    public int reorderQuantity() {
        return reorderQuantity;
    }

    public boolean lowStockSignalled() {
        return lowStockSignalled;
    }

    /** True when a restock was confirmed by the distributor and has not been delivered yet. */
    public boolean hasPendingRestock() {
        return pendingRestock != null;
    }

    /** The confirmed restock that has not been delivered yet, or empty when there is none. */
    public Optional<PendingRestock> pendingRestock() {
        return Optional.ofNullable(pendingRestock);
    }

    /** True when the available stock is at or below the reorder level. */
    public boolean lowStock() {
        return available() <= reorderLevel;
    }

    private void signalLowStockOncePerDip() {
        if (lowStock() && !lowStockSignalled) {
            domainEvents.add(new StockRunningLowEvent(bookId.value(), available(), reorderQuantity, clock.instant()));
            lowStockSignalled = true;
        }
    }

    private static void requireValidReason(String reason) {
        if (reason == null) {
            throw new InvalidStocktakeReasonException("A stocktake correction needs a reason");
        }
        if (reason.length() < MIN_REASON_LENGTH || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidStocktakeReasonException("A stocktake reason has " + MIN_REASON_LENGTH + " to "
                    + MAX_REASON_LENGTH + " characters, not " + reason.length());
        }
    }

    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new InvalidQuantityException("The " + name + " cannot be negative, not " + value);
        }
    }

    private static void requireOnHandNotBelowReserved(BookId bookId, int onHand, int reserved) {
        if (onHand < reserved) {
            throw new OnHandBelowReservedException(bookId, onHand, reserved);
        }
    }
}
