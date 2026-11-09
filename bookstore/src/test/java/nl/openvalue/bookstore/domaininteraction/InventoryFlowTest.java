package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
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

    private static final String ISBN = "9789900010029";

    private static final BookDTO BOOK = new BookDTO(BOOK_ID, ISBN, "Sense and Sensibility", List.of("Jane Austen"),
            "FICTION", new BigDecimal("9.99"), "EUR", 1811, "AVAILABLE");

    // The retailer reference is the book id and the epoch seconds of the flow's clock: 2026-06-15T10:00:00Z for CLOCK,
    // 2021-03-04T05:06:07Z for OTHER_CLOCK. Only the second one ends inside a minute.
    private static final String REFERENCE = "7a1e2b3c-4d5e-4f60-8a9b-0c1d2e3f4a5b/1781517600";
    private static final String OTHER_CLOCK_REFERENCE = "7a1e2b3c-4d5e-4f60-8a9b-0c1d2e3f4a5b/1614834367";

    // Two distributor answers that differ in every field; neither quantity is the requested reorder quantity 7, and
    // neither date is two days after a clock of this class. The first id has the distributor's own shape: a lowercase
    // UUID.
    private static final RestockConfirmationDTO BACKORDERED_CONFIRMATION = new RestockConfirmationDTO(
            "0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7", 8, LocalDate.of(2026, 7, 3), true);
    private static final RestockConfirmationDTO IN_STOCK_CONFIRMATION = new RestockConfirmationDTO(
            "RO-20211125-0017", 11, LocalDate.of(2021, 11, 25), false);

    @Mock
    private StockItemDataService stockItems;

    @Mock
    private BookDataService books;

    @Mock
    private DistributorAppService distributor;

    @Mock
    private ApplicationEventPublisher events;

    @Captor
    private ArgumentCaptor<StockItemDTO> saved;

    @Captor
    private ArgumentCaptor<Object> published;

    private InventoryFlow flow;

    @BeforeEach
    void setUp() {
        flow = new InventoryFlow(stockItems, books, distributor, events, CLOCK);
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
        InventoryFlow flowOnOtherClock = new InventoryFlow(stockItems, books, distributor, events, OTHER_CLOCK);
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

    static Stream<Arguments> confirmedRestocks() {
        PendingRestockDTO backordered = new PendingRestockDTO(
                "0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7", 8, LocalDate.of(2026, 7, 3), true);
        PendingRestockDTO inStock = new PendingRestockDTO("RO-20211125-0017", 11, LocalDate.of(2021, 11, 25), false);
        return Stream.of(UseCase.values()).flatMap(useCase -> Stream.of(
                Arguments.of(useCase, false, BACKORDERED_CONFIRMATION, backordered),
                Arguments.of(useCase, true, BACKORDERED_CONFIRMATION, backordered),
                Arguments.of(useCase, false, IN_STOCK_CONFIRMATION, inStock),
                Arguments.of(useCase, true, IN_STOCK_CONFIRMATION, inStock)));
    }

    @ParameterizedTest(name = "{0}, signalled {1}, {2}")
    @MethodSource("confirmedRestocks")
    void restockAsksForTheReorderQuantityOfTheBooksIsbnAndSavesTheConfirmedRestock(UseCase useCase,
            boolean signalled, RestockConfirmationDTO confirmation, PendingRestockDTO expectedPending,
            CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(signalled));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(any(), anyInt(), any())).thenReturn(confirmation);

        useCase.runOn(flow, BOOK_ID);

        InOrder inOrder = inOrder(stockItems, books, distributor);
        inOrder.verify(stockItems).findByBookId(BOOK_ID);
        inOrder.verify(books).findById(BOOK_ID);
        inOrder.verify(distributor).requestRestock(ISBN, 7, REFERENCE);
        inOrder.verify(stockItems).save(saved.capture());
        // Figures and signal as stored; the pending restock holds what the distributor confirmed.
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 5, 2, 4, 7, signalled, expectedPending));
        verifyNoMoreInteractions(stockItems, books, distributor);
        verifyNoInteractions(events);
        assertThat(output.getOut()).doesNotContain(BOOK_ID.toString());
    }

    @ParameterizedTest
    @EnumSource(UseCase.class)
    void restockReferenceCarriesTheEpochSecondsOfTheFlowsClock(UseCase useCase) {
        InventoryFlow flowOnOtherClock = new InventoryFlow(stockItems, books, distributor, events, OTHER_CLOCK);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(false));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(any(), anyInt(), any())).thenReturn(IN_STOCK_CONFIRMATION);

        useCase.runOn(flowOnOtherClock, BOOK_ID);

        verify(distributor).requestRestock(ISBN, 7, OTHER_CLOCK_REFERENCE);
        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 5, 2, 4, 7, false,
                new PendingRestockDTO("RO-20211125-0017", 11, LocalDate.of(2021, 11, 25), false)));
        verifyNoInteractions(events);
    }

    static Stream<Arguments> pendingStockItems() {
        // Both pending restocks, from both signal states; the stock is low, so only the pending restock stops the call.
        return Stream.of(UseCase.values()).flatMap(useCase -> Stream.of(
                Arguments.of(useCase, new StockItemDTO(BOOK_ID, 5, 2, 4, 7, true, PENDING)),
                Arguments.of(useCase, new StockItemDTO(BOOK_ID, 5, 2, 4, 7, false, OTHER_PENDING))));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("pendingStockItems")
    void restockIsNotRequestedWhileOneIsPending(UseCase useCase, StockItemDTO pending, CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(pending);

        useCase.runOn(flow, BOOK_ID);

        verify(stockItems).findByBookId(BOOK_ID);
        verifyNoMoreInteractions(stockItems);
        verifyNoInteractions(books, distributor, events);
        assertThat(output.getOut()).doesNotContain(BOOK_ID.toString());
    }

    static Stream<RuntimeException> distributorExceptions() {
        return Stream.of(new DistributorAppService.DistributorUnavailableException("Distributor answered status 503"),
                new DistributorAppService.TitleNotFoundAtDistributorException(ISBN),
                new DistributorAppService.TitleNotOrderableException(ISBN));
    }

    @ParameterizedTest
    @MethodSource("distributorExceptions")
    void requestRestockPassesEveryDistributorExceptionThrough(RuntimeException distributorException) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenThrow(distributorException);

        Throwable thrown = catchThrowable(() -> flow.requestRestock(BOOK_ID));

        assertThat(thrown).isSameAs(distributorException);
        verifyNothingSavedOrPublished();
    }

    static Stream<RestockConfirmationDTO> unusableConfirmations() {
        // Each answer breaks exactly one rule of a pending restock: no quantity, or an id of blanks only.
        return Stream.of(new RestockConfirmationDTO("0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7", 0,
                        LocalDate.of(2026, 7, 3), false),
                new RestockConfirmationDTO("   ", 8, LocalDate.of(2026, 7, 3), true));
    }

    @ParameterizedTest
    @MethodSource("unusableConfirmations")
    void requestRestockTurnsAnUnusableConfirmationIntoDistributorUnavailable(RestockConfirmationDTO unusable) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenReturn(unusable);

        Throwable thrown = catchThrowable(() -> flow.requestRestock(BOOK_ID));

        assertThat(thrown).isExactlyInstanceOf(DistributorAppService.DistributorUnavailableException.class);
        UnavailableException unavailable = (UnavailableException) thrown;
        assertThat(unavailable.code()).isEqualTo("distributor-unavailable");
        assertThat(unavailable.title()).isEqualTo("Distributor unavailable");
        verifyNothingSavedOrPublished();
    }

    @ParameterizedTest
    @MethodSource("unusableConfirmations")
    void handleStockRunningLowResetsTheSignalWhenTheConfirmationIsUnusable(RestockConfirmationDTO unusable,
            CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenReturn(unusable);

        flow.handleStockRunningLow(BOOK_ID);

        verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 5, 2, 4, 7, false, null));
        verifyNoInteractions(events);
        assertOneWarningNamingTheBook(output);
    }

    @Test
    void handleStockRunningLowResetsTheSignalWhenTheDistributorIsUnavailable(CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenThrow(
                new DistributorAppService.DistributorUnavailableException("Distributor answered status 503"));

        flow.handleStockRunningLow(BOOK_ID);

        InOrder inOrder = inOrder(distributor, stockItems);
        inOrder.verify(distributor).requestRestock(ISBN, 7, REFERENCE);
        inOrder.verify(stockItems).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new StockItemDTO(BOOK_ID, 5, 2, 4, 7, false, null));
        verifyNoInteractions(events);
        assertOneWarningNamingTheBook(output);
    }

    static Stream<RuntimeException> refusals() {
        return Stream.of(new DistributorAppService.TitleNotFoundAtDistributorException(ISBN),
                new DistributorAppService.TitleNotOrderableException(ISBN));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void handleStockRunningLowKeepsTheSignalWhenTheDistributorRefusesTheTitle(RuntimeException refusal,
            CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenThrow(refusal);

        flow.handleStockRunningLow(BOOK_ID);

        verifyNothingSavedOrPublished();
        assertOneWarningNamingTheBook(output);
    }

    static Stream<RuntimeException> otherExceptions() {
        // The base classes of the three caught exceptions, and an exception of no use-case type at all.
        return Stream.of(new UnavailableException("distributor-unavailable", "Distributor unavailable", "Down"),
                new NotFoundException("title-not-found-at-distributor", "Title not found at distributor", "Unknown"),
                new InvalidInputException("title-not-orderable", "Title not orderable", "Refused"),
                new IllegalStateException("Connection pool exhausted"));
    }

    @ParameterizedTest
    @MethodSource("otherExceptions")
    void handleStockRunningLowPassesEveryOtherExceptionThrough(RuntimeException other, CapturedOutput output) {
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(lowStockItem(true));
        when(books.findById(BOOK_ID)).thenReturn(BOOK);
        when(distributor.requestRestock(ISBN, 7, REFERENCE)).thenThrow(other);

        Throwable thrown = catchThrowable(() -> flow.handleStockRunningLow(BOOK_ID));

        assertThat(thrown).isSameAs(other);
        verifyNothingSavedOrPublished();
        assertThat(output.getOut()).doesNotContain(" WARN ").doesNotContain(BOOK_ID.toString());
    }

    @ParameterizedTest
    @EnumSource(UseCase.class)
    void restockPassesStockItemNotFoundThrough(UseCase useCase) {
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_ID);
        when(stockItems.findByBookId(BOOK_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> useCase.runOn(flow, BOOK_ID));

        assertThat(thrown).isSameAs(notFound);
        verifyNoInteractions(books, distributor);
        verifyNothingSavedOrPublished();
    }

    @ParameterizedTest
    @EnumSource(UseCase.class)
    void restockTranslatesAStoredItemThatFailsRestoreTo409(UseCase useCase) {
        StockItemDTO onHandBelowReserved = new StockItemDTO(BOOK_ID, 1, 2, 4, 7, false);
        when(stockItems.findByBookId(BOOK_ID)).thenReturn(onHandBelowReserved);

        Throwable thrown = catchThrowable(() -> useCase.runOn(flow, BOOK_ID));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved",
                domainMessage(() -> StockItemDomainMapper.toDomain(onHandBelowReserved, CLOCK)));
        verifyNoInteractions(books, distributor);
        verifyNothingSavedOrPublished();
    }

    /**
     * A low stock item without a pending restock: on hand 5, reserved 2, reorder level 4, reorder quantity 7, so the
     * available stock 3 and every figure differ from the reorder quantity and from the default quantity 10.
     */
    private static StockItemDTO lowStockItem(boolean signalled) {
        return new StockItemDTO(BOOK_ID, 5, 2, 4, 7, signalled);
    }

    /** The two restock use cases, which share one implementation and differ only in how failures are handled. */
    enum UseCase {
        REQUEST_RESTOCK(InventoryFlow::requestRestock),
        HANDLE_STOCK_RUNNING_LOW(InventoryFlow::handleStockRunningLow);

        private final BiConsumer<InventoryFlow, UUID> call;

        UseCase(BiConsumer<InventoryFlow, UUID> call) {
            this.call = call;
        }

        void runOn(InventoryFlow flow, UUID bookId) {
            call.accept(flow, bookId);
        }
    }

    /** Exactly one WARN line was written, and that same line names the book. */
    private static void assertOneWarningNamingTheBook(CapturedOutput output) {
        assertThat(output.getOut()).containsOnlyOnce(" WARN ").containsPattern(" WARN .*" + BOOK_ID);
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
