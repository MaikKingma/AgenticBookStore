package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockItemTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-15T10:00:00Z");
    private static final BookId BOOK_ID = new BookId(UUID.fromString("3f1c2b9e-6d4a-4c1e-9b7a-2e5d8f0a1b2c"));
    private static final String REASON = "Stocktake after counting the shelf";

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
        StockItem item = StockItem.restore(BOOK_ID, 9, 2, 4, 6, true, CLOCK);

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
        assertThat(StockItem.restore(BOOK_ID, 9, 2, 4, 6, false, CLOCK).lowStockSignalled()).isFalse();
    }

    @Test
    void restoreRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, false, CLOCK);

        assertThat(item.lowStock()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void restoreAcceptsReservedEqualToOnHand() {
        StockItem item = StockItem.restore(BOOK_ID, 4, 4, 3, 10, false, CLOCK);

        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void restoreRejectsReservedAboveOnHand() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 4, 3, 10, false, CLOCK))
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
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, -1, 3, 10, false, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreRejectsANegativeReorderLevel() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 0, -1, 10, false, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreRejectsANegativeReorderQuantity() {
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 3, 0, 3, -1, false, CLOCK))
                .isInstanceOf(InvalidQuantityException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void restoreAcceptsAZeroReorderLevelAndReorderQuantity() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 0, 0, 0, false, CLOCK);

        assertThat(item.reorderLevel()).isEqualTo(0);
        assertThat(item.reorderQuantity()).isEqualTo(0);
    }

    @Test
    void factoriesRejectAMissingBookIdOrClock() {
        assertThatThrownBy(() -> StockItem.openFor(null, CLOCK)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.openFor(BOOK_ID, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.restore(null, 5, 0, 3, 10, false, CLOCK))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void receivingAddsTheQuantityToOnHand() {
        StockItem item = StockItem.restore(BOOK_ID, 4, 1, 3, 10, false, CLOCK);

        item.receive(new Quantity(5));

        assertThat(item.onHand()).isEqualTo(9);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(8);
    }

    @Test
    void receiveRejects0And1001AndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 1, 3, 10, true, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, true, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(5);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingToOneAboveTheReorderLevelResetsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, true, CLOCK);

        item.receive(new Quantity(3));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStockSignalled()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingAboveTheDefaultButNotAboveAHigherOwnReorderLevelKeepsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 5, 7, true, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void receivingAboveALowerOwnReorderLevelButNotAboveTheDefaultResetsTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 0, 0, 1, 10, true, CLOCK);

        item.receive(new Quantity(2));

        assertThat(item.available()).isEqualTo(2);
        assertThat(item.lowStockSignalled()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void lowStockComparesTheAvailableStockWithTheReorderLevel() {
        assertThat(StockItem.restore(BOOK_ID, 5, 2, 3, 10, true, CLOCK).lowStock()).isTrue();
        assertThat(StockItem.restore(BOOK_ID, 6, 2, 3, 10, true, CLOCK).lowStock()).isFalse();
    }

    @Test
    void reservingMoreThanAvailableThrowsInsufficientStockAndKeepsReserved() {
        StockItem item = StockItem.restore(BOOK_ID, 3, 2, 3, 10, false, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 10, 2, 3, 10, false, CLOCK);

        item.reserve(new Quantity(8));

        assertThat(item.onHand()).isEqualTo(10);
        assertThat(item.reserved()).isEqualTo(10);
        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void reservingDownToTheReorderLevelRegistersOneEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, CLOCK);

        item.reserve(new Quantity(2));

        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStock()).isTrue();
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void reservingDownToTheReorderLevelWithStockAlreadyReservedReportsTheAvailableStock() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 1, 3, 10, false, CLOCK);

        item.reserve(new Quantity(2));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.reserved()).isEqualTo(3);
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void reservingThatStaysAboveTheReorderLevelRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, CLOCK);

        item.reserve(new Quantity(1));

        assertThat(item.available()).isEqualTo(4);
        assertThat(item.lowStock()).isFalse();
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void reservingAgainInTheSameDipRegistersNoNewEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, CLOCK);
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
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, Clock.fixed(otherInstant, ZoneOffset.UTC));

        item.reserve(new Quantity(2));

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, otherInstant));
    }

    @Test
    void correctingDownToTheReorderLevelRegistersOneEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 2, 3, 10, false, CLOCK);

        item.correct(new Quantity(5), REASON);

        assertThat(item.onHand()).isEqualTo(5);
        assertThat(item.available()).isEqualTo(3);
        assertThat(item.lowStockSignalled()).isTrue();
        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 3, 10, FIXED_INSTANT));
    }

    @Test
    void correctingDownToANonDefaultReorderLevelRegistersItsOwnReorderQuantity() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 0, 5, 7, false, CLOCK);

        item.correct(new Quantity(5), REASON);

        assertThat(item.pullDomainEvents())
                .containsExactly(new StockRunningLowEvent(BOOK_ID.value(), 5, 7, FIXED_INSTANT));
    }

    @Test
    void correctingToOneAboveANonDefaultReorderLevelRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 0, 5, 7, false, CLOCK);

        item.correct(new Quantity(6), REASON);

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void correctingAgainInTheSameDipRegistersNoNewEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, CLOCK);
        item.correct(new Quantity(3), REASON);
        item.pullDomainEvents();

        item.correct(new Quantity(2), REASON);

        assertThat(item.onHand()).isEqualTo(2);
        assertThat(item.pullDomainEvents()).isEmpty();
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void correctingAboveTheReorderLevelDoesNotResetTheSignal() {
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 4, 1, 3, 10, false, CLOCK);

        item.correct(new Quantity(9), REASON);

        assertThat(item.onHand()).isEqualTo(9);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(8);
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void correctAcceptsOnHandEqualToReserved() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, true, CLOCK);

        item.correct(new Quantity(4), REASON);

        assertThat(item.onHand()).isEqualTo(4);
        assertThat(item.reserved()).isEqualTo(4);
        assertThat(item.available()).isEqualTo(0);
    }

    @Test
    void correctRejectsOnHandBelowReservedAndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, false, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, CLOCK);

        item.correct(new Quantity(7), "abc");
        assertThat(item.onHand()).isEqualTo(7);

        item.correct(new Quantity(6), "a".repeat(200));
        assertThat(item.onHand()).isEqualTo(6);
    }

    @Test
    void correctRejectsReasonsOf2And201CharactersAndNull() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, CLOCK);

        assertRejectedReason(item, "ab");
        assertRejectedReason(item, "a".repeat(201));
        assertRejectedReason(item, null);
    }

    @Test
    void correctAcceptsAnyThreeCharacterReason() {
        StockItem item = StockItem.restore(BOOK_ID, 8, 0, 3, 10, false, CLOCK);

        item.correct(new Quantity(7), "   ");
        assertThat(item.onHand()).isEqualTo(7);

        item.correct(new Quantity(6), Character.toString(0x2003).repeat(3));
        assertThat(item.onHand()).isEqualTo(6);
    }

    @Test
    void correctChecksTheReasonBeforeTheReservedStock() {
        StockItem item = StockItem.restore(BOOK_ID, 6, 4, 3, 10, false, CLOCK);

        assertThatThrownBy(() -> item.correct(new Quantity(3), "ab"))
                .isInstanceOf(InvalidStocktakeReasonException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
        assertUnchanged(item, 6, 4, false);
    }

    @Test
    void shippingLowersOnHandAndReservedAndKeepsAvailable() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, false, CLOCK);

        item.ship(new Quantity(3));

        assertThat(item.onHand()).isEqualTo(7);
        assertThat(item.reserved()).isEqualTo(1);
        assertThat(item.available()).isEqualTo(6);
        assertThat(item.pullDomainEvents()).isEmpty();
    }

    @Test
    void shippingMoreThanReservedThrowsAndLeavesStockUnchanged() {
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, true, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 10, 4, 3, 10, false, CLOCK);

        item.ship(new Quantity(4));

        assertThat(item.onHand()).isEqualTo(6);
        assertThat(item.reserved()).isEqualTo(0);
        assertThat(item.available()).isEqualTo(6);
    }

    @Test
    void shippingFromALowAndUnsignalledStockItemRegistersNoEvent() {
        StockItem item = StockItem.restore(BOOK_ID, 5, 2, 3, 10, false, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 2, 0, 3, 10, true, CLOCK);

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
        StockItem item = StockItem.restore(BOOK_ID, 5, 0, 3, 10, false, CLOCK);
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
