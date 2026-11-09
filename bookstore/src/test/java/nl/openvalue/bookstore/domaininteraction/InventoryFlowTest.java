package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryFlowTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final Clock OTHER_CLOCK = Clock.fixed(Instant.parse("2021-03-04T05:06:07Z"), ZoneOffset.UTC);

    private static final UUID BOOK_ID = UUID.fromString("7a1e2b3c-4d5e-4f60-8a9b-0c1d2e3f4a5b");

    // Two pending restocks that differ in every field; neither quantity equals the reorder quantity of a stock item it
    // is used with, and neither date is two days after a clock of this class.
    private static final PendingRestockDTO PENDING =
            new PendingRestockDTO("R-7781", 13, LocalDate.of(2026, 7, 3), true);
    private static final PendingRestockDTO OTHER_PENDING =
            new PendingRestockDTO("DIST-RO-0042", 5, LocalDate.of(2027, 11, 30), false);

    @Mock
    private StockItemDataService stockItems;

    @Mock
    private ApplicationEventPublisher events;

    @Captor
    private ArgumentCaptor<StockItemDTO> saved;

    @Captor
    private ArgumentCaptor<Object> published;

    private InventoryFlow flow;

    @BeforeEach
    void setUp() {
        flow = new InventoryFlow(stockItems, events, CLOCK);
    }

    @Test
    void receiveAddsTheQuantityAndSavesTheStockItem() {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false));

        flow.receive(BOOK_ID, 5);

        InOrder inOrder = inOrder(stockItems, events);
        inOrder.verify(stockItems).findByBookId(BOOK_ID);
        inOrder.verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 14, 2, 4, 6, false));
        verifyNoInteractions(events);
    }

    @Test
    void receiveNeverPublishes() {
        // After receiving 1 the stock item is still low and unsignalled, yet receiving registers no event.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 0, 0, 3, 10, false));

        flow.receive(BOOK_ID, 1);

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 1, 0, 3, 10, false));
        verifyNoInteractions(events);
    }

    @Test
    void receiveResetsTheSignalInTheSavedStockItem() {
        // On hand 3, reserved 2, signalled; receiving 3 ends at available 4, one above the reorder level 3.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 3, 2, 3, 10, true));

        flow.receive(BOOK_ID, 3);

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 6, 2, 3, 10, false));
        verifyNoInteractions(events);
    }

    @Test
    void receiveFromAPendingItemSavesTheStockItemWithoutAPendingRestock() {
        // Receiving 1 ends at available 1, three below the reorder level 4: the stock stays low and signalled, and the
        // delivery still completes the pending restock.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 0, 0, 4, 7, true, PENDING));

        flow.receive(BOOK_ID, 1);

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 1, 0, 4, 7, true, null));
        assertThat(saved.getValue().pendingRestock()).isNull();
        verifyNoInteractions(events);
    }

    @Test
    void receiveAboveTheReorderLevelFromAPendingItemSavesTheStockItemWithoutAPendingRestock() {
        when(stockItems.findByBookId(BOOK_ID))
                .thenReturn(new StockItemDTO(BOOK_ID, 3, 2, 3, 10, true, OTHER_PENDING));

        flow.receive(BOOK_ID, 3);

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 6, 2, 3, 10, false, null));
        verifyNoInteractions(events);
    }

    @Test
    void receiveTranslatesAnOutOfRangeQuantityTo422WithTheQuantityField() {
        StockItemDTO stored = new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(stored);

        for (int quantity : List.of(0, 1001, -1, 10001)) {
            Throwable thrown = catchThrowable(() -> flow.receive(BOOK_ID, quantity));

            assertInvalidField(thrown, "quantity-out-of-range", "Quantity out of range", "quantity",
                    domainMessage(() -> StockItemDomainMapper.toDomain(stored, CLOCK).receive(new Quantity(quantity))));
        }
        verifyNothingSavedOrPublished();
    }

    @Test
    void receivePassesStockItemNotFoundThrough() {
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_ID);
        when(stockItems.findByBookId(BOOK_ID)).thenThrow(notFound);

        // An invalid quantity: a value object built before the load would turn the 404 into a 422.
        Throwable thrown = catchThrowable(() -> flow.receive(BOOK_ID, -1));

        assertThat(thrown).isSameAs(notFound);
        verifyNothingSavedOrPublished();
    }

    @Test
    void receiveTranslatesAStoredItemThatFailsRestoreTo409() {
        StockItemDTO onHandBelowReserved = new StockItemDTO(BOOK_ID, 1, 2, 3, 10, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(onHandBelowReserved);

        Throwable thrown = catchThrowable(() -> flow.receive(BOOK_ID, 5));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved",
                domainMessage(() -> StockItemDomainMapper.toDomain(onHandBelowReserved, CLOCK)));
        verifyNothingSavedOrPublished();
    }

    @Test
    void correctPublishesTheLowStockEventAfterSaving() {
        // On hand 20, reserved 2; correcting to 5 ends at available 3, exactly the reorder level.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 20, 2, 3, 10, false));

        flow.correct(BOOK_ID, 5, "stocktake");

        InOrder inOrder = inOrder(stockItems, events);
        inOrder.verify(stockItems).findByBookId(BOOK_ID);
        inOrder.verify(stockItems).save(saved.capture());
        inOrder.verify(events).publishEvent(published.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 5, 2, 3, 10, true));
        assertThat(published.getValue()).isEqualTo(new StockRunningLowEvent(BOOK_ID, 3, 10, CLOCK.instant()));
        verifyNoMoreInteractions(events);
    }

    @Test
    void correctStampsTheEventWithTheFlowsClock() {
        InventoryFlow flowOnOtherClock = new InventoryFlow(stockItems, events, OTHER_CLOCK);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 20, 2, 3, 10, false));

        flowOnOtherClock.correct(BOOK_ID, 5, "stocktake");

        verify(events).publishEvent(published.capture());
        assertThat(published.getValue()).isEqualTo(
                new StockRunningLowEvent(BOOK_ID, 3, 10, Instant.parse("2021-03-04T05:06:07Z")));
        verifyNoMoreInteractions(events);
    }

    @Test
    void correctDoesNotPublishAgainInTheSameDip() {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 3, 0, 3, 10, true));

        flow.correct(BOOK_ID, 2, "stocktake");

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 2, 0, 3, 10, true));
        verifyNoInteractions(events);
    }

    @Test
    void correctSetsOnHandWithoutAnEventAboveTheReorderLevel() {
        // Correcting to 7 with reserved 2 ends at available 5, one above the non-default reorder level 4.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false));

        flow.correct(BOOK_ID, 7, "stocktake");

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 7, 2, 4, 6, false));
        verifyNoInteractions(events);
    }

    @Test
    void correctKeepsThePendingRestockInTheSavedStockItem() {
        // Correcting to 7 with reserved 2 ends at available 5, above the reorder level 4: no event.
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false, PENDING));

        flow.correct(BOOK_ID, 7, "stocktake");

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 7, 2, 4, 6, false, PENDING));
        assertThat(saved.getValue().pendingRestock())
                .isEqualTo(new PendingRestockDTO("R-7781", 13, LocalDate.of(2026, 7, 3), true));
        verifyNoInteractions(events);
    }

    @Test
    void correctThatStartsADipKeepsThePendingRestockAndPublishesTheEvent() {
        // On hand 20, reserved 1; correcting to 4 ends at available 3, exactly the reorder level. No figure equals a
        // field of the pending restock.
        when(stockItems.findByBookId(BOOK_ID))
                .thenReturn(new StockItemDTO(BOOK_ID, 20, 1, 3, 10, false, OTHER_PENDING));

        flow.correct(BOOK_ID, 4, "stocktake");

        InOrder inOrder = inOrder(stockItems, events);
        inOrder.verify(stockItems).save(saved.capture());
        inOrder.verify(events).publishEvent(published.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 4, 1, 3, 10, true, OTHER_PENDING));
        assertThat(saved.getValue().pendingRestock())
                .isEqualTo(new PendingRestockDTO("DIST-RO-0042", 5, LocalDate.of(2027, 11, 30), false));
        assertThat(published.getValue()).isEqualTo(new StockRunningLowEvent(BOOK_ID, 3, 10, CLOCK.instant()));
        verifyNoMoreInteractions(events);
    }

    @Test
    void correctTranslatesOnHandBelowReservedTo409() {
        StockItemDTO stored = new StockItemDTO(BOOK_ID, 9, 4, 3, 10, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(stored);

        Throwable thrown = catchThrowable(() -> flow.correct(BOOK_ID, 3, "stocktake"));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved", domainMessage(
                () -> StockItemDomainMapper.toDomain(stored, CLOCK).correct(new Quantity(3), "stocktake")));
        verifyNothingSavedOrPublished();
    }

    @Test
    void correctTranslatesAnOutOfRangeOnHandTo422WithTheOnHandField() {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false));

        for (int onHand : List.of(-1, 10001)) {
            Throwable thrown = catchThrowable(() -> flow.correct(BOOK_ID, onHand, "stocktake"));

            assertInvalidField(thrown, "quantity-out-of-range", "Quantity out of range", "onHand",
                    domainMessage(() -> new Quantity(onHand)));
        }
        verifyNothingSavedOrPublished();
    }

    @Test
    void correctTranslatesAnInvalidReasonTo422() {
        StockItemDTO stored = new StockItemDTO(BOOK_ID, 9, 2, 4, 6, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(stored);

        for (String reason : Arrays.asList("ab", "x".repeat(201), null)) {
            Throwable thrown = catchThrowable(() -> flow.correct(BOOK_ID, 5, reason));

            assertInvalidField(thrown, "invalid-reason", "Invalid reason", "reason", domainMessage(
                    () -> StockItemDomainMapper.toDomain(stored, CLOCK).correct(new Quantity(5), reason)));
        }
        verifyNothingSavedOrPublished();
    }

    @Test
    void correctPassesStockItemNotFoundThrough() {
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_ID);
        when(stockItems.findByBookId(BOOK_ID)).thenThrow(notFound);

        // An invalid on hand and reason: a check before the load would turn the 404 into a 422.
        Throwable thrown = catchThrowable(() -> flow.correct(BOOK_ID, -1, "ab"));

        assertThat(thrown).isSameAs(notFound);
        verifyNothingSavedOrPublished();
    }

    @Test
    void correctTranslatesAStoredItemThatFailsRestoreTo409() {
        StockItemDTO onHandBelowReserved = new StockItemDTO(BOOK_ID, 1, 2, 3, 10, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(onHandBelowReserved);

        Throwable thrown = catchThrowable(() -> flow.correct(BOOK_ID, 5, "stocktake"));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved",
                domainMessage(() -> StockItemDomainMapper.toDomain(onHandBelowReserved, CLOCK)));
        verifyNothingSavedOrPublished();
    }

    @Test
    void findByBookIdReturnsTheStoredStockItem() {
        StockItemDTO stored = new StockItemDTO(BOOK_ID, 9, 2, 4, 6, true);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(stored);

        assertThat(flow.findByBookId(BOOK_ID)).isSameAs(stored);
        verify(stockItems, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void findByBookIdPassesNotFoundThrough() {
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_ID);
        when(stockItems.findByBookId(BOOK_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.findByBookId(BOOK_ID));

        assertThat(thrown).isSameAs(notFound);
        verifyNothingSavedOrPublished();
    }

    @Test
    void findLowStockReturnsThePortResultInOrder() {
        // The port's order is sorted in no column, ascending or descending: book id 3, 1, 2; on hand 2, 6, 4;
        // reserved 0, 6, 3; reorder level 3, 5, 4; reorder quantity 10, 7, 12; available 2, 0, 1. So any re-sort
        // by the flow changes the order, and the flow must keep it.
        StockItemDTO first = new StockItemDTO(UUID.fromString("3c000000-0000-4000-8000-000000000003"), 2, 0, 3, 10,
                true);
        StockItemDTO second = new StockItemDTO(UUID.fromString("1a000000-0000-4000-8000-000000000001"), 6, 6, 5, 7,
                false);
        StockItemDTO third = new StockItemDTO(UUID.fromString("2b000000-0000-4000-8000-000000000002"), 4, 3, 4, 12,
                true);
        when(stockItems.findLowStock()).thenReturn(List.of(first, second, third));

        assertThat(flow.findLowStock()).containsExactly(first, second, third);
        verifyNoInteractions(events);
    }

    private void verifyNothingSavedOrPublished() {
        verify(stockItems, never()).save(any());
        verifyNoInteractions(events);
    }

    /** The message of the domain exception the call throws, which the flow must hand on unchanged. */
    private static String domainMessage(ThrowingCallable call) {
        Throwable domainException = catchThrowable(call);
        assertThat(domainException).isNotNull();
        assertThat(domainException.getMessage()).isNotBlank();
        return domainException.getMessage();
    }

    private static void assertInvalidField(Throwable thrown, String code, String title, String field,
            String message) {
        assertThat(thrown).isExactlyInstanceOf(InvalidInputException.class);
        InvalidInputException invalid = (InvalidInputException) thrown;
        assertThat(invalid.code()).isEqualTo(code);
        assertThat(invalid.title()).isEqualTo(title);
        assertThat(invalid.getMessage()).isNotBlank().isEqualTo(message);
        assertThat(invalid.details()).containsExactly(Map.of("field", field, "message", message));
    }

    private static void assertConflict(Throwable thrown, String code, String title, String message) {
        assertThat(thrown).isExactlyInstanceOf(ConflictException.class);
        ConflictException conflict = (ConflictException) thrown;
        assertThat(conflict.code()).isEqualTo(code);
        assertThat(conflict.title()).isEqualTo(title);
        assertThat(conflict.getMessage()).isNotBlank().isEqualTo(message);
        assertThat(conflict.details()).isEmpty();
    }
}
