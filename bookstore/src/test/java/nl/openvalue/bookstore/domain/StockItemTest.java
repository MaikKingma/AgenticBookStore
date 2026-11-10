package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class StockItemTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-15T10:00:00Z");
    private static final BookId BOOK_ID = new BookId(UUID.fromString("3f1c2b9e-6d4a-4c1e-9b7a-2e5d8f0a1b2c"));
    private static final String REASON = "Stocktake after counting the shelf";
    private static final PendingRestock NO_PENDING_RESTOCK = null;
    private static final PendingRestock PENDING = new PendingRestock("R-7781", 12, LocalDate.of(2026, 7, 3), true);
    private static final PendingRestock OTHER_PENDING =
            new PendingRestock("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10", 5, LocalDate.of(2001, 2, 3), false);

    @Test
    void openForStartsWithNoStockAndTheDefaultReorderSettings() {
        StockItem item = StockItem.openFor(BOOK_ID, CLOCK);

        assertThat(item.bookId()).isEqualTo(BOOK_ID);
        assertThat(item.onHand()).isEqualTo(0);
        assertThat(item.reserved()).isEqualTo(0);
        assertThat(item.available()).isEqualTo(0);
        assertThat(item.reorderLevel()).isEqualTo(3);
        assertThat(item.reorderQuantity()).isEqualTo(10);
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void openForIsLowStockFromTheStart() {
        assertThat(StockItem.openFor(BOOK_ID, CLOCK).lowStock()).isTrue();
    }

    @Test
    void openForRegistersNoEvent() {
        assertThat(StockItem.openFor(BOOK_ID, CLOCK).pullDomainEvents()).isEmpty();
    }

    @Test
    void restoreKeepsEveryStoredFigureAndTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, true, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.bookId()).isEqualTo(BOOK_ID);
        assertThat(item.onHand()).isEqualTo(9);
        assertThat(item.reserved()).isEqualTo(2);
        assertThat(item.available()).isEqualTo(7);
        assertThat(item.reorderLevel()).isEqualTo(4);
        assertThat(item.reorderQuantity()).isEqualTo(6);
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void restoreKeepsAnUnsetSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, false, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void restoreWithAPendingRestockExposesAllFourValues() {
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, true, PENDING, CLOCK);

        assertThat(item.hasPendingRestock()).isTrue();
        assertThat(item.pendingRestock()).hasValue(PENDING);
        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("R-7781");
        assertThat(pending.quantity()).isEqualTo(12);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(pending.backordered()).isTrue();
    }

    @Test
    void restoreWithADifferentPendingRestockExposesItToo() {
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, false, OTHER_PENDING, CLOCK);

        assertThat(item.pendingRestock()).hasValue(OTHER_PENDING);
        assertThat(item.pendingRestock().orElseThrow().backordered()).isFalse();
    }

    @Test
    void restoreWithoutAPendingRestockExposesNone() {
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, true, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
    }

    @Test
    void restoreWithAPendingRestockRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, false, PENDING, CLOCK);

        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void openForHasNoPendingRestock() {
        StockItem item = StockItem.openFor(BOOK_ID, CLOCK);

        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
    }

    @Test
    void restoreRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.lowStock()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void restoreAcceptsReservedEqualToOnHand() {
        StockItem item = StockItem.restore(BOOK_ID, 4, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void restoreRejectsReservedAboveOnHand() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK))
                .isInstanceOf(OnHandBelowReservedException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank())
                .satisfies(e -> {
                    OnHandBelowReservedException rejection = (OnHandBelowReservedException) e;
                    assertThat(rejection.bookId()).isEqualTo(BOOK_ID);
                    assertThat(rejection.onHand()).isEqualTo(3);
                    assertThat(rejection.reserved()).isEqualTo(4);
                });
    }

    @Test
    void restoreRejectsANegativeReserved() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, -1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreRejectsANegativeReorderLevel() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 0, -1, 10, false, NO_PENDING_RESTOCK, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreRejectsANegativeReorderQuantity() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 0, 3, -1, false, NO_PENDING_RESTOCK, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreAcceptsAZeroReorderLevelAndReorderQuantity() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 0, 0, 0, false, NO_PENDING_RESTOCK, CLOCK);

        assertThat(item.reorderLevel()).isEqualTo(0);
        assertThat(item.reorderQuantity()).isEqualTo(0);
    }

    @Test
    void factoriesRejectAMissingBookIdOrClock() {
        assertThatThrownBy(() -> StockItem.openFor(null, CLOCK)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.openFor(BOOK_ID, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.restore(null, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void receivingAddsTheQuantityToOnHand() {
        StockItem item = StockItem.restore(BOOK_ID, 4, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(5));

        assertThat(item.onHand()).isEqualTo(9);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(8);
    }

    @Test
    void receiveRejects0And1001AndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 1, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.receive(new Quantity(0)))
                .isInstanceOf(ReceivedQuantityOutOfRangeException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
        assertUnchanged(item, 2, 1, true);

        assertThatThrownBy(() -> item.receive(new Quantity(1001)))
                .isInstanceOf(ReceivedQuantityOutOfRangeException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
        assertUnchanged(item, 2, 1, true);
    }

    @Test
    void receiveAcceptsTheBounds1And1000() {
        StockItem item = StockItem.openFor(BOOK_ID, CLOCK);

        item.receive(new Quantity(1));
        assertThat(item.onHand()).isEqualTo(1);

        item.receive(new Quantity(1000));
        assertThat(item.onHand()).isEqualTo(1001);
    }

    @Test
    void receivingNeverRegistersAnEvent() {
        StockItem item = StockItem.openFor(BOOK_ID, CLOCK);

        item.receive(new Quantity(1));

        assertThat(item.available()).isEqualTo(1);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void receivingToExactlyTheReorderLevelKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(5);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingToOneAboveTheReorderLevelResetsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(3));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStockSignalled()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingAboveTheDefaultButNotAboveAHigherOwnReorderLevelKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 5, 7, true, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingAboveALowerOwnReorderLevelButNotAboveTheDefaultResetsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 0, 0, 1, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.available()).isEqualTo(2);
        assertThat(item.lowStockSignalled()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void lowStockComparesTheAvailableStockWithTheReorderLevel() {
        assertThat(StockItem.restore(BOOK_ID, 5, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK).lowStock()).isTrue();
        assertThat(StockItem.restore(BOOK_ID, 6, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK).lowStock()).isFalse();
    }

    @Test
    void reservingMoreThanAvailableThrowsInsufficientStockAndKeepsReserved() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.reserve(new Quantity(2)))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank())
                .satisfies(e -> {
                    InsufficientStockException rejection = (InsufficientStockException) e;
                    assertThat(rejection.bookId()).isEqualTo(BOOK_ID);
                    assertThat(rejection.requested()).isEqualTo(2);
                    assertThat(rejection.available()).isEqualTo(1);
                });
        assertUnchanged(item, 3, 2, false);
    }

    @Test
    void reservingExactlyTheAvailableQuantitySucceeds() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 2, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.reserve(new Quantity(8));

        assertThat(item.onHand()).isEqualTo(10);
        assertThat(item.reserved()).isEqualTo(10);
        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void reservingDownToTheReorderLevelRegistersOneEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.reserve(new Quantity(2));

        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void reservingDownToTheReorderLevelWithStockAlreadyReservedReportsTheAvailableStock() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.reserve(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.reserved()).isEqualTo(3);
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void reservingThatStaysAboveTheReorderLevelRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.reserve(new Quantity(1));

        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStock()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void reservingAgainInTheSameDipRegistersNoNewEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);
        item.reserve(new Quantity(2));
        item.pullDomainEvents();

        item.reserve(new Quantity(1));

        assertThat(item.available()).isEqualTo(2);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void theEventIsStampedWithTheClockOfTheStockItem() {
        Instant otherInstant = Instant.parse("2027-01-02T03:04:05Z");
        StockItem item = StockItem.restore(
                BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, Clock.fixed(otherInstant, ZoneOffset.UTC));

        item.reserve(new Quantity(2));

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, otherInstant));
    }

    @Test
    void correctingDownToTheReorderLevelRegistersOneEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 2, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(5), REASON);

        assertThat(item.onHand()).isEqualTo(5);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void correctingDownToANonDefaultReorderLevelRegistersItsOwnReorderQuantity() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 0, 5, 7, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(5), REASON);

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 5, 7, FIXED_INSTANT));
    }

    @Test
    void correctingToOneAboveANonDefaultReorderLevelRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 0, 5, 7, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(6), REASON);

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void correctingAgainInTheSameDipRegistersNoNewEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);
        item.correct(new Quantity(3), REASON);
        item.pullDomainEvents();

        item.correct(new Quantity(2), REASON);

        assertThat(item.onHand()).isEqualTo(2);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void correctingAboveTheReorderLevelDoesNotResetTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(8), REASON);

        assertThat(item.available()).isEqualTo(8);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();

        item.correct(new Quantity(3), REASON);

        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void correctSetsOnHand() {
        StockItem item = StockItem.restore(BOOK_ID, 4, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(9), REASON);

        assertThat(item.onHand()).isEqualTo(9);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(8);
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void correctAcceptsOnHandEqualToReserved() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(4), REASON);

        assertThat(item.onHand()).isEqualTo(4);
        assertThat(item.reserved()).isEqualTo(4);
        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void correctRejectsOnHandBelowReservedAndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.correct(new Quantity(3), REASON))
                .isInstanceOf(OnHandBelowReservedException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank())
                .satisfies(e -> {
                    OnHandBelowReservedException rejection = (OnHandBelowReservedException) e;
                    assertThat(rejection.bookId()).isEqualTo(BOOK_ID);
                    assertThat(rejection.onHand()).isEqualTo(3);
                    assertThat(rejection.reserved()).isEqualTo(4);
                });
        assertUnchanged(item, 6, 4, false);
    }

    @Test
    void correctAcceptsReasonsOf3And200Characters() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(7), "abc");
        assertThat(item.onHand()).isEqualTo(7);

        item.correct(new Quantity(6), "a".repeat(200));
        assertThat(item.onHand()).isEqualTo(6);
    }

    @Test
    void correctRejectsReasonsOf2And201CharactersAndNull() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertRejectedReason(item, "ab");
        assertRejectedReason(item, "a".repeat(201));
        assertRejectedReason(item, null);
    }

    @Test
    void correctAcceptsAnyThreeCharacterReason() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.correct(new Quantity(7), "   ");
        assertThat(item.onHand()).isEqualTo(7);

        item.correct(new Quantity(6), Character.toString(0x2003).repeat(3));
        assertThat(item.onHand()).isEqualTo(6);
    }

    @Test
    void correctChecksTheReasonBeforeTheReservedStock() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.correct(new Quantity(3), "ab"))
                .isInstanceOf(InvalidStocktakeReasonException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
        assertUnchanged(item, 6, 4, false);
    }

    @Test
    void shippingLowersOnHandAndReservedAndKeepsAvailable() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.ship(new Quantity(3));

        assertThat(item.onHand()).isEqualTo(7);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(6);
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingMoreThanReservedThrowsAndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.ship(new Quantity(5)))
                .isInstanceOf(InsufficientReservedStockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank())
                .satisfies(e -> {
                    InsufficientReservedStockException rejection = (InsufficientReservedStockException) e;
                    assertThat(rejection.bookId()).isEqualTo(BOOK_ID);
                    assertThat(rejection.requested()).isEqualTo(5);
                    assertThat(rejection.reserved()).isEqualTo(4);
                });
        assertUnchanged(item, 10, 4, true);
    }

    @Test
    void shippingExactlyTheReservedQuantitySucceeds() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.ship(new Quantity(4));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.reserved()).isEqualTo(0);
        assertThat(item.available()).isEqualTo(6);
    }

    @Test
    void shippingFromALowAndUnsignalledStockItemRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 2, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.ship(new Quantity(1));

        assertThat(item.onHand()).isEqualTo(4);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void receivingAboveTheLevelAfterASignalLetsTheNextDipRegisterAgain() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.receive(new Quantity(10));
        assertThat(item.lowStockSignalled()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();

        item.reserve(new Quantity(9));

        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void pullDomainEventsReturnsTheEventsAndClearsThem() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);
        item.reserve(new Quantity(2));
        StockRunningLowEvent firstEvent = new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT);

        List<DomainEvent> first = item.pullDomainEvents();

        assertThat(first).containsExactly(firstEvent);
        assertThatThrownBy(() -> first.add(firstEvent)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(first).containsExactly(firstEvent);

        item.receive(new Quantity(10));
        item.reserve(new Quantity(11));

        assertThat(first).containsExactly(firstEvent);
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 2, 10, FIXED_INSTANT));
        assertThat(first).containsExactly(firstEvent);
    }

    @Test
    void reservingWhileSignalledAboveTheReorderLevelKeepsTheSignalAndRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.reserve(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(10);
        assertThat(item.reserved()).isEqualTo(2);
        assertThat(item.available()).isEqualTo(8);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingWhileSignalledAboveTheReorderLevelKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.ship(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(8);
        assertThat(item.reserved()).isEqualTo(2);
        assertThat(item.available()).isEqualTo(6);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingWhileSignalledAtTheReorderLevelKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.ship(new Quantity(1));

        assertThat(item.onHand()).isEqualTo(4);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void restockRequestedStoresTheConfirmedValuesAsThePendingRestock() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequested("R-7781", 12, LocalDate.of(2026, 7, 3), true);

        assertThat(item.hasPendingRestock()).isTrue();
        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("R-7781");
        assertThat(pending.quantity()).isEqualTo(12);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(pending.backordered()).isTrue();
    }

    @Test
    void restockRequestedStoresAnotherSetOfConfirmedValuesWithoutBackorder() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 7, false, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequested("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10", 3, LocalDate.of(2001, 2, 3), false);

        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10");
        assertThat(pending.quantity()).isEqualTo(3);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2001, 2, 3));
        assertThat(pending.backordered()).isFalse();
        assertThat(item.reorderQuantity()).isEqualTo(7);
    }

    @ParameterizedTest
    @CsvSource({"8, 2, false", "8, 2, true", "2, 0, false", "2, 0, true"})
    void restockRequestedRegistersNoEventAndKeepsTheFiguresAndTheSignal(int onHand, int reserved,
            boolean lowStockSignalled) {
        StockItem item = StockItem.restore(BOOK_ID, onHand, reserved, 3, 10, lowStockSignalled, NO_PENDING_RESTOCK,
                CLOCK);

        item.restockRequested("R-7781", 12, LocalDate.of(2026, 7, 3), true);

        assertThat(item.hasPendingRestock()).isTrue();
        assertThat(item.onHand()).isEqualTo(onHand);
        assertThat(item.reserved()).isEqualTo(reserved);
        assertThat(item.available()).isEqualTo(onHand - reserved);
        assertThat(item.reorderLevel()).isEqualTo(3);
        assertThat(item.reorderQuantity()).isEqualTo(10);
        assertThat(item.lowStockSignalled()).isEqualTo(lowStockSignalled);
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRestockRequests")
    void restockRequestedRejectsInvalidValuesAndChangesNothing(String restockOrderId, int quantity,
            LocalDate expectedDeliveryDate) {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        assertThatThrownBy(() -> item.restockRequested(restockOrderId, quantity, expectedDeliveryDate, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());

        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
        assertUnchanged(item, 5, 1, false);
    }

    static Stream<Arguments> invalidRestockRequests() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        return Stream.of(
                Arguments.of(null, 5, date),
                Arguments.of("", 5, date),
                Arguments.of("   ", 5, date),
                Arguments.of("\t", 5, date),
                Arguments.of(Character.toString(0x2003), 5, date),
                Arguments.of("R".repeat(65), 5, date),
                Arguments.of("R".repeat(64) + " ", 5, date),
                Arguments.of("R-1", 0, date),
                Arguments.of("R-1", -1, date),
                Arguments.of("R-1", 5, null));
    }

    @Test
    void restockRequestedAcceptsTheBoundsOfAValidPendingRestock() {
        StockItem shortId = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);
        StockItem longId = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        shortId.restockRequested("R", 1, LocalDate.of(2026, 7, 3), false);
        longId.restockRequested("R".repeat(64), 1, LocalDate.of(2026, 7, 3), false);

        assertThat(shortId.pendingRestock().orElseThrow().restockOrderId()).isEqualTo("R");
        assertThat(shortId.pendingRestock().orElseThrow().quantity()).isEqualTo(1);
        assertThat(longId.pendingRestock().orElseThrow().restockOrderId()).hasSize(64);
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void restockRequestedWhilePendingIsRejectedAndChangesNothing(boolean lowStockSignalled) {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, lowStockSignalled, PENDING, CLOCK);

        assertThatThrownBy(() -> item.restockRequested("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10", 5,
                LocalDate.of(2001, 2, 3), false))
                .isExactlyInstanceOf(RestockAlreadyPendingException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank())
                .satisfies(e -> {
                    RestockAlreadyPendingException rejection = (RestockAlreadyPendingException) e;
                    assertThat(rejection.bookId()).isEqualTo(BOOK_ID);
                    assertThat(rejection.pendingRestock()).isEqualTo(PENDING);
                });

        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("R-7781");
        assertThat(pending.quantity()).isEqualTo(12);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(pending.backordered()).isTrue();
        assertUnchanged(item, 5, 1, lowStockSignalled);
    }

    @Test
    void aSecondRestockRequestedAfterTheFirstOneIsRejectedToo() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);
        item.restockRequested("R-7781", 12, LocalDate.of(2026, 7, 3), true);

        assertThatThrownBy(() -> item.restockRequested("R-9", 4, LocalDate.of(2026, 8, 9), false))
                .isExactlyInstanceOf(RestockAlreadyPendingException.class);

        assertThat(item.pendingRestock()).hasValue(PENDING);
    }

    @ParameterizedTest
    @CsvSource({"1", "5", "11", "12", "13", "20", "1000"})
    void receiveClearsThePendingRestockForAnyAcceptedDelivery(int delivered) {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, PENDING, CLOCK);

        item.receive(new Quantity(delivered));

        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
        assertThat(item.onHand()).isEqualTo(5 + delivered);
    }

    @Test
    void aPartialDeliveryThatLeavesStockLowStillClearsThePendingRestockAndKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, PENDING, CLOCK);

        item.receive(new Quantity(1));

        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void aPartialDeliveryThatLeavesStockBelowTheReorderLevelStillClearsThePendingRestock() {
        StockItem item = StockItem.restore(BOOK_ID, 1, 0, 3, 10, true, PENDING, CLOCK);

        item.receive(new Quantity(1));

        assertThat(item.available()).isEqualTo(2);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.hasPendingRestock()).isFalse();
        assertThat(item.pendingRestock()).isEmpty();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0", "1001"})
    void aRejectedReceiveKeepsThePendingRestock(int delivered) {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, true, PENDING, CLOCK);

        assertThatThrownBy(() -> item.receive(new Quantity(delivered)))
                .isExactlyInstanceOf(ReceivedQuantityOutOfRangeException.class);

        assertThat(item.hasPendingRestock()).isTrue();
        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("R-7781");
        assertThat(pending.quantity()).isEqualTo(12);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(pending.backordered()).isTrue();
        assertUnchanged(item, 5, 1, true);
    }

    @Test
    void aNewRestockCanBeRequestedOnceTheDeliveryClearedThePendingOne() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 1, 3, 10, false, PENDING, CLOCK);
        item.receive(new Quantity(12));

        item.restockRequested("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10", 5, LocalDate.of(2001, 2, 3), false);

        assertThat(item.pendingRestock()).hasValue(OTHER_PENDING);
    }

    @Test
    void restockRequestFailedResetsTheSignalOfALowItemAndRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequestFailed();

        assertThat(item.lowStock()).isTrue();
        assertUnchanged(item, 2, 0, false);
        assertThat(item.hasPendingRestock()).isFalse();
    }

    @Test
    void restockRequestFailedKeepsAnUnsetSignalUnsetOnALowItemAndRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, false, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequestFailed();

        assertUnchanged(item, 2, 0, false);
    }

    @Test
    void restockRequestFailedResetsTheSignalOfAnItemThatIsNotLowAndKeepsTheFigures() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 2, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequestFailed();

        assertThat(item.available()).isEqualTo(6);
        assertThat(item.reorderLevel()).isEqualTo(3);
        assertThat(item.reorderQuantity()).isEqualTo(10);
        assertUnchanged(item, 8, 2, false);
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void restockRequestFailedKeepsAPendingRestock(boolean lowStockSignalled) {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, lowStockSignalled, PENDING, CLOCK);

        item.restockRequestFailed();

        PendingRestock pending = item.pendingRestock().orElseThrow();
        assertThat(pending.restockOrderId()).isEqualTo("R-7781");
        assertThat(pending.quantity()).isEqualTo(12);
        assertThat(pending.expectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(pending.backordered()).isTrue();
        assertUnchanged(item, 2, 0, false);
    }

    @Test
    void restockRequestFailedIsIdempotent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);

        item.restockRequestFailed();
        item.restockRequestFailed();

        assertUnchanged(item, 2, 0, false);
    }

    @Test
    void theReservationAfterAFailedRestockRequestStartsANewAttempt() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, true, NO_PENDING_RESTOCK, CLOCK);
        item.restockRequestFailed();
        assertThat(item.pullDomainEvents()).isEmpty();

        item.reserve(new Quantity(2));

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void reservingKeepsAPendingRestock() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, PENDING, CLOCK);

        item.reserve(new Quantity(5));

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
        assertThat(item.pendingRestock()).hasValue(PENDING);
    }

    @Test
    void shippingKeepsAPendingRestock() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 4, 3, 10, true, OTHER_PENDING, CLOCK);

        item.ship(new Quantity(2));

        assertThat(item.pendingRestock()).hasValue(OTHER_PENDING);
        assertThat(item.pendingRestock().orElseThrow().backordered()).isFalse();
    }

    @Test
    void correctingKeepsAPendingRestockAlsoWhenTheCorrectionStartsADip() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, PENDING, CLOCK);

        item.correct(new Quantity(2), REASON);

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 2, 10, FIXED_INSTANT));
        assertThat(item.pendingRestock()).hasValue(PENDING);
    }

    @Test
    void aRejectedReservationKeepsAPendingRestock() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 0, 3, 10, true, PENDING, CLOCK);

        assertThatThrownBy(() -> item.reserve(new Quantity(4))).isInstanceOf(InsufficientStockException.class);

        assertThat(item.pendingRestock()).hasValue(PENDING);
    }

    private static void assertRejectedReason(StockItem item, String reason) {
        assertThatThrownBy(() -> item.correct(new Quantity(2), reason))
                .isInstanceOf(InvalidStocktakeReasonException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
        assertUnchanged(item, 8, 0, false);
    }

    private static void assertUnchanged(StockItem item, int onHand, int reserved, boolean lowStockSignalled) {
        assertThat(item.onHand()).isEqualTo(onHand);
        assertThat(item.reserved()).isEqualTo(reserved);
        assertThat(item.lowStockSignalled()).isEqualTo(lowStockSignalled);
        assertThat(item.pullDomainEvents()).isEmpty();
    }
}
