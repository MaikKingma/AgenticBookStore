package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domain.EmailAddress;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.OrderAlreadyShippedException;
import nl.openvalue.bookstore.domain.OrderId;
import nl.openvalue.bookstore.domain.OrderLine;
import nl.openvalue.bookstore.domain.OrderLineCountOutOfRangeException;
import nl.openvalue.bookstore.domain.OrderPlacedEvent;
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
class OrderFlowTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    private static final Clock NANO_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00.123456789Z"), ZoneOffset.UTC);

    /** A clock no other test uses, so only the flow's own clock can give its instant. */
    private static final Clock EVENT_CLOCK =
            Clock.fixed(Instant.parse("2024-02-29T23:59:58.987654321Z"), ZoneOffset.UTC);

    // Fixed book ids whose first hex digit is 0 to 7; the requests list them in an order other than id order.
    private static final UUID BOOK_1 = UUID.fromString("1a000000-0000-4000-8000-000000000001");
    private static final UUID BOOK_2 = UUID.fromString("2b000000-0000-4000-8000-000000000002");
    private static final UUID BOOK_3 = UUID.fromString("3c000000-0000-4000-8000-000000000003");

    private static final BookDTO BOOK_1_DTO = book(BOOK_1, "9780141439600", "A Tale of Two Cities", "12.50");
    private static final BookDTO BOOK_2_DTO = book(BOOK_2, "9780141439563", "Wuthering Heights", "8.00");
    private static final BookDTO BOOK_3_DTO = book(BOOK_3, "9780141439518", "Pride and Prejudice", "9.99");

    // In book-id order the on hand (20, 9, 30), reserved (4, 0, 7), available (16, 9, 23), reorder level (3, 2, 4)
    // and reorder quantity (10, 12, 8) are sorted in no direction, so a sort by any stock column changes the order.
    private static final StockItemDTO STOCK_1 = new StockItemDTO(BOOK_1, 20, 4, 3, 10, false);
    private static final StockItemDTO STOCK_2 = new StockItemDTO(BOOK_2, 9, 0, 2, 12, false);
    private static final StockItemDTO STOCK_3 = new StockItemDTO(BOOK_3, 30, 7, 4, 8, false);

    private static final UUID ORDER_ID = UUID.fromString("4d000000-0000-4000-8000-000000000004");

    // A stored order's lines: books 3, 1, 2 with the distinct quantities 2, 1, 3. In stored order and in book-id
    // order the isbn (...518, ...600, ...501 and ...600, ...501, ...518), title, price and quantity are sorted in no
    // direction, so a sort of the lines by any of them changes the order.
    private static final List<OrderLineDTO> STORED_LINES = List.of(
            new OrderLineDTO(BOOK_3, "9780141439518", "Pride and Prejudice", new BigDecimal("9.99"), "EUR", 2),
            new OrderLineDTO(BOOK_1, "9780141439600", "A Tale of Two Cities", new BigDecimal("12.50"), "EUR", 1),
            new OrderLineDTO(BOOK_2, "9780141439501", "Wuthering Heights", new BigDecimal("8.00"), "EUR", 3));

    // Stock reserved for the stored order. In book-id order on hand (20, 9, 30), reserved (5, 3, 9) and available
    // (15, 6, 21) are sorted in no direction, before and after shipping.
    private static final StockItemDTO SHIP_STOCK_1 = new StockItemDTO(BOOK_1, 20, 5, 3, 10, false);
    private static final StockItemDTO SHIP_STOCK_2 = new StockItemDTO(BOOK_2, 9, 3, 2, 12, false);
    private static final StockItemDTO SHIP_STOCK_3 = new StockItemDTO(BOOK_3, 30, 9, 4, 8, false);

    @Mock
    private OrderDataService orders;

    @Mock
    private BookDataService books;

    @Mock
    private StockItemDataService stockItems;

    @Mock
    private ApplicationEventPublisher events;

    @Captor
    private ArgumentCaptor<OrderDTO> savedOrder;

    @Captor
    private ArgumentCaptor<StockItemDTO> savedStock;

    @Captor
    private ArgumentCaptor<Object> published;

    private OrderFlow flow;

    @BeforeEach
    void setUp() {
        flow = new OrderFlow(orders, books, stockItems, events, CLOCK);
    }

    @Test
    void placeOrderSavesTheOrderWithSnapshotsAndReturnsItsId() {
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, BOOK_2_DTO);
        stubStock(STOCK_3, STOCK_1, STOCK_2);

        UUID id = flow.placeOrder("  Reader@Example.COM ",
                List.of(line(BOOK_3, 2), line(BOOK_1, 1), line(BOOK_2, 3)));

        verify(orders).save(savedOrder.capture());
        assertThat(id).isNotNull().isEqualTo(savedOrder.getValue().id());
        assertThat(savedOrder.getValue()).isEqualTo(new OrderDTO(id, "reader@example.com", "PLACED",
                Instant.parse("2026-06-15T10:00:00Z"), null, List.of(
                        new OrderLineDTO(BOOK_3, "9780141439518", "Pride and Prejudice", new BigDecimal("9.99"),
                                "EUR", 2),
                        new OrderLineDTO(BOOK_1, "9780141439600", "A Tale of Two Cities", new BigDecimal("12.50"),
                                "EUR", 1),
                        new OrderLineDTO(BOOK_2, "9780141439563", "Wuthering Heights", new BigDecimal("8.00"),
                                "EUR", 3))));
    }

    @Test
    void placeOrderReservesTheStockOfEveryLine() {
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, BOOK_2_DTO);
        stubStock(STOCK_3, STOCK_1, STOCK_2);

        flow.placeOrder("reader@example.com", List.of(line(BOOK_3, 2), line(BOOK_1, 1), line(BOOK_2, 3)));

        verify(stockItems, times(3)).save(savedStock.capture());
        assertThat(savedStock.getAllValues()).containsExactly(
                new StockItemDTO(BOOK_1, 20, 5, 3, 10, false),
                new StockItemDTO(BOOK_2, 9, 3, 2, 12, false),
                new StockItemDTO(BOOK_3, 30, 9, 4, 8, false));
        verify(stockItems).findByBookId(BOOK_1);
        verify(stockItems).findByBookId(BOOK_2);
        verify(stockItems).findByBookId(BOOK_3);
    }

    @Test
    void placeOrderSavesTheOrderFirstThenTheStockItemsInBookIdOrderThenPublishes() {
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, BOOK_2_DTO);
        stubStock(STOCK_3, STOCK_1, STOCK_2);

        flow.placeOrder("reader@example.com", List.of(line(BOOK_3, 2), line(BOOK_1, 1), line(BOOK_2, 3)));

        InOrder inOrder = inOrder(orders, stockItems, events);
        inOrder.verify(orders).save(any());
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_1, 20, 5, 3, 10, false));
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_2, 9, 3, 2, 12, false));
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_3, 30, 9, 4, 8, false));
        inOrder.verify(events).publishEvent(any(Object.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void placeOrderPublishesTheOrderPlacedEventAndThePulledStockEvents() {
        // All three reservations start a dip: book 1 drops to 3 available (reorder level 4), book 2 to 2 (level 2) and
        // book 3 to 4 (level 5). The request lists 3, 1, 2, so the stock events must come in book-id order 1, 2, 3.
        // In book-id order on hand (6, 4, 9), reserved (3, 2, 5), available (3, 2, 4), reorder level (4, 2, 5),
        // reorder quantity (10, 7, 12), isbn (...600, ...631, ...518), title and price (12.50, 8.00, 9.99) are sorted
        // in no direction, so a sort by any stock or book column changes the order.
        OrderFlow flowOnEventClock = new OrderFlow(orders, books, stockItems, events, EVENT_CLOCK);
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, book(BOOK_2, "9780141439631", "Wuthering Heights", "8.00"));
        stubStock(new StockItemDTO(BOOK_3, 9, 2, 5, 12, false), new StockItemDTO(BOOK_1, 6, 0, 4, 10, false),
                new StockItemDTO(BOOK_2, 4, 1, 2, 7, false));

        UUID id = flowOnEventClock.placeOrder("READER@example.COM",
                List.of(line(BOOK_3, 3), line(BOOK_1, 3), line(BOOK_2, 1)));

        InOrder inOrder = inOrder(orders, stockItems, events);
        inOrder.verify(orders).save(any());
        inOrder.verify(stockItems, times(3)).save(savedStock.capture());
        inOrder.verify(events, times(4)).publishEvent(published.capture());
        assertThat(published.getAllValues()).containsExactly(
                new OrderPlacedEvent(id, "reader@example.com", new BigDecimal("75.47"),
                        Instant.parse("2024-02-29T23:59:58.987654Z")),
                new StockRunningLowEvent(BOOK_1, 3, 10, Instant.parse("2024-02-29T23:59:58.987654321Z")),
                new StockRunningLowEvent(BOOK_2, 2, 7, Instant.parse("2024-02-29T23:59:58.987654321Z")),
                new StockRunningLowEvent(BOOK_3, 4, 12, Instant.parse("2024-02-29T23:59:58.987654321Z")));
        assertThat(savedStock.getAllValues()).containsExactly(
                new StockItemDTO(BOOK_1, 6, 3, 4, 10, true),
                new StockItemDTO(BOOK_2, 4, 2, 2, 7, true),
                new StockItemDTO(BOOK_3, 9, 5, 5, 12, true));
        verifyNoMoreInteractions(events);
    }

    @Test
    void placeOrderStampsTheOrderWithTheFlowsClock() {
        OrderFlow flowOnNanoClock = new OrderFlow(orders, books, stockItems, events, NANO_CLOCK);
        stubBooks(BOOK_2_DTO);
        stubStock(new StockItemDTO(BOOK_2, 4, 0, 3, 10, false));

        UUID id = flowOnNanoClock.placeOrder("Shopper@Example.org", List.of(line(BOOK_2, 1)));

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00.123456Z"));
        verify(events, times(2)).publishEvent(published.capture());
        assertThat(published.getAllValues()).containsExactly(
                new OrderPlacedEvent(id, "shopper@example.org", new BigDecimal("8.00"),
                        Instant.parse("2026-06-15T10:00:00.123456Z")),
                new StockRunningLowEvent(BOOK_2, 3, 10, Instant.parse("2026-06-15T10:00:00.123456789Z")));
    }

    @Test
    void placeOrderKeepsTheLinesInRequestOrder() {
        // Request order 2, 1, 3 is sorted in no column in either direction: book id, isbn, title, price, quantity.
        stubBooks(BOOK_2_DTO, BOOK_1_DTO, BOOK_3_DTO);
        stubStock(STOCK_2, STOCK_1, STOCK_3);

        flow.placeOrder("reader@example.com", List.of(line(BOOK_2, 1), line(BOOK_1, 3), line(BOOK_3, 2)));

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().lines()).containsExactly(
                new OrderLineDTO(BOOK_2, "9780141439563", "Wuthering Heights", new BigDecimal("8.00"), "EUR", 1),
                new OrderLineDTO(BOOK_1, "9780141439600", "A Tale of Two Cities", new BigDecimal("12.50"), "EUR",
                        3),
                new OrderLineDTO(BOOK_3, "9780141439518", "Pride and Prejudice", new BigDecimal("9.99"), "EUR",
                        2));
    }

    @Test
    void placeOrderMergesDuplicateLinesPerBook() {
        stubBooks(BOOK_3_DTO, BOOK_1_DTO);
        stubStock(STOCK_3, STOCK_1);

        flow.placeOrder("reader@example.com", List.of(line(BOOK_3, 3), line(BOOK_1, 1), line(BOOK_3, 3)));

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().lines()).containsExactly(
                new OrderLineDTO(BOOK_3, "9780141439518", "Pride and Prejudice", new BigDecimal("9.99"), "EUR", 6),
                new OrderLineDTO(BOOK_1, "9780141439600", "A Tale of Two Cities", new BigDecimal("12.50"), "EUR",
                        1));
        verify(books, times(1)).findById(BOOK_3);
        verify(stockItems, times(1)).findByBookId(BOOK_3);
        verify(stockItems, times(2)).save(savedStock.capture());
        assertThat(savedStock.getAllValues()).containsExactly(
                new StockItemDTO(BOOK_1, 20, 5, 3, 10, false),
                new StockItemDTO(BOOK_3, 30, 13, 4, 8, false));
    }

    @Test
    void placeOrderAcceptsAMergedQuantityOf10() {
        stubBooks(BOOK_2_DTO);
        stubStock(new StockItemDTO(BOOK_2, 15, 0, 2, 12, false));

        flow.placeOrder("reader@example.com", List.of(line(BOOK_2, 4), line(BOOK_2, 6)));

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().lines()).containsExactly(
                new OrderLineDTO(BOOK_2, "9780141439563", "Wuthering Heights", new BigDecimal("8.00"), "EUR", 10));
        verify(stockItems).save(new StockItemDTO(BOOK_2, 15, 10, 2, 12, false));
    }

    @Test
    void placeOrderRejectsAMergedQuantityAbove10BeforeLoadingAnyBook() {
        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 6), line(BOOK_1, 2), line(BOOK_3, 6))));

        assertInvalidField(thrown, "quantity-out-of-range", "Quantity out of range", "lines",
                domainMessage(() -> OrderLine.quantityOf(12)));
        verifyNoInteractions(books, stockItems, orders, events);
    }

    @Test
    void placeOrderRejectsARequestedQuantityOutside1To10() {
        assertRequestedQuantityRejected(List.of(line(BOOK_1, 0)), 0);
        assertRequestedQuantityRejected(List.of(line(BOOK_1, 11)), 11);
        assertRequestedQuantityRejected(List.of(line(BOOK_1, -1)), -1);
        // 11 and -1 for the same book would merge to an allowed 10: each requested quantity is checked first.
        assertRequestedQuantityRejected(List.of(line(BOOK_1, 11), line(BOOK_1, -1)), 11);
        // Two bad lines: the first in request order is the one reported, here neither first nor last of the request.
        assertRequestedQuantityRejected(List.of(line(BOOK_3, 5), line(BOOK_1, 0), line(BOOK_2, 11)), 0);
        verifyNoInteractions(books, stockItems, orders, events);
    }

    @Test
    void placeOrderTranslatesAnInvalidEmailTo422BeforeAnyLineCheck() {
        Throwable thrown = catchThrowable(
                () -> flow.placeOrder("not-an-email", List.of(line(BOOK_3, 6), line(BOOK_3, 6))));

        assertInvalidField(thrown, "invalid-email", "Invalid e-mail address", "customerEmail",
                domainMessage(() -> new EmailAddress("not-an-email")));
        verifyNoInteractions(books, stockItems, orders, events);
    }

    @Test
    void placeOrderTranslatesTooManyAndTooFewLinesTo422() {
        // 21 distinct, known books in stock, one copy each: only the order itself can refuse them.
        List<PlaceOrderLineDTO> twentyOneLines = new ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            UUID bookId = UUID.fromString(String.format("%08x-0000-4000-8000-%012x", i, i));
            stubBooks(book(bookId, "isbn-" + i, "Title " + i, "1.00"));
            stubStock(new StockItemDTO(bookId, 10, 0, 3, 10, false));
            twentyOneLines.add(line(bookId, 1));
        }

        Throwable tooMany = catchThrowable(() -> flow.placeOrder("reader@example.com", twentyOneLines));
        Throwable tooFew = catchThrowable(() -> flow.placeOrder("reader@example.com", List.of()));

        assertInvalidField(tooMany, "line-count-out-of-range", "Number of lines out of range", "lines",
                new OrderLineCountOutOfRangeException(21, 1, 20).getMessage());
        assertInvalidField(tooFew, "line-count-out-of-range", "Number of lines out of range", "lines",
                new OrderLineCountOutOfRangeException(0, 1, 20).getMessage());
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderPassesBookNotFoundThroughAndStopsAtTheFirstUnknownBook() {
        // V sorts before U and before the known book, so a lookup in id order would hit V first.
        UUID unknownU = UUID.fromString("5e000000-0000-4000-8000-000000000005");
        UUID unknownV = UUID.fromString("0f000000-0000-4000-8000-00000000000f");
        stubBooks(BOOK_3_DTO);
        BookDataService.BookNotFoundException notFound = new BookDataService.BookNotFoundException(unknownU);
        when(books.findById(unknownU)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 1), line(unknownU, 2), line(unknownV, 3))));

        assertThat(thrown).isSameAs(notFound);
        verify(books, never()).findById(unknownV);
        verifyNoInteractions(stockItems);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderRejectsADiscontinuedBookWith409BeforeAnyStockLookup() {
        stubBooks(BOOK_3_DTO, discontinued(BOOK_1_DTO));

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 1), line(BOOK_1, 2), line(BOOK_2, 1))));

        assertBookNotOrderable(thrown, BOOK_1);
        verify(books, never()).findById(BOOK_2);
        verifyNoInteractions(stockItems);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderStopsAtADiscontinuedBookBeforeALaterUnknownBook() {
        UUID unknownU = UUID.fromString("5e000000-0000-4000-8000-000000000005");
        stubBooks(discontinued(BOOK_2_DTO));

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_2, 1), line(unknownU, 1))));

        assertBookNotOrderable(thrown, BOOK_2);
        verify(books, never()).findById(unknownU);
        verifyNoInteractions(stockItems);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderCollectsEveryShortageIntoOne409() {
        // Request order: book 3 short (5 of 2), book 2 fits, book 1 short (9 of 4), book 2f short (6 of 1). The short
        // entries in request order have book ids (3c, 1a, 2f), requested (5, 9, 6), available (2, 4, 1), isbn
        // (...518, ...600, ...556), title, price (9.99, 12.50, 7.99), on hand (4, 7, 1), reserved (2, 3, 0), reorder
        // level (5, 2, 4) and reorder quantity (8, 12, 6) sorted in no direction, so a sort by any detail, book or
        // stock column changes the order.
        UUID book2f = UUID.fromString("2f000000-0000-4000-8000-00000000002f");
        stubBooks(BOOK_3_DTO, BOOK_2_DTO, BOOK_1_DTO, book(book2f, "9780141439556", "Dracula", "7.99"));
        stubStock(new StockItemDTO(BOOK_3, 4, 2, 5, 8, false), new StockItemDTO(BOOK_2, 9, 0, 3, 10, false),
                new StockItemDTO(BOOK_1, 7, 3, 2, 12, false), new StockItemDTO(book2f, 1, 0, 4, 6, false));

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 5), line(BOOK_2, 1), line(BOOK_1, 9), line(book2f, 6))));

        assertThat(thrown).isExactlyInstanceOf(ConflictException.class);
        ConflictException conflict = (ConflictException) thrown;
        assertThat(conflict.code()).isEqualTo("insufficient-stock");
        assertThat(conflict.title()).isEqualTo("Insufficient stock");
        assertThat(conflict.getMessage()).isNotBlank();
        assertThat(conflict.details()).containsExactly(
                Map.of("bookId", "3c000000-0000-4000-8000-000000000003", "requested", "5", "available", "2"),
                Map.of("bookId", "1a000000-0000-4000-8000-000000000001", "requested", "9", "available", "4"),
                Map.of("bookId", "2f000000-0000-4000-8000-00000000002f", "requested", "6", "available", "1"));
        verify(stockItems).findByBookId(BOOK_3);
        verify(stockItems).findByBookId(BOOK_2);
        verify(stockItems).findByBookId(BOOK_1);
        verify(stockItems).findByBookId(book2f);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderPassesStockItemNotFoundThrough() {
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, BOOK_2_DTO);
        stubStock(STOCK_3);
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_1);
        when(stockItems.findByBookId(BOOK_1)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 2), line(BOOK_1, 1), line(BOOK_2, 3))));

        assertThat(thrown).isSameAs(notFound);
        verify(stockItems, never()).findByBookId(BOOK_2);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderTranslatesAStoredStockItemThatFailsRestoreTo409() {
        StockItemDTO onHandBelowReserved = new StockItemDTO(BOOK_1, 1, 2, 3, 10, false);
        stubBooks(BOOK_3_DTO, BOOK_1_DTO, BOOK_2_DTO);
        stubStock(STOCK_3, onHandBelowReserved);

        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com",
                List.of(line(BOOK_3, 2), line(BOOK_1, 1), line(BOOK_2, 3))));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved",
                domainMessage(() -> StockItemDomainMapper.toDomain(onHandBelowReserved, CLOCK)));
        verify(stockItems, never()).findByBookId(BOOK_2);
        verifyNothingSavedOrPublished();
    }

    @Test
    void placeOrderTranslatesAnInvalidStoredPriceTo422() {
        BookDTO negativePrice = new BookDTO(BOOK_1, "9780141439600", "A Tale of Two Cities",
                List.of("Charles Dickens"), "FICTION", new BigDecimal("-1.00"), "EUR", 1859, "AVAILABLE");
        BookDTO priceInDollars = new BookDTO(BOOK_1, "9780141439600", "A Tale of Two Cities",
                List.of("Charles Dickens"), "FICTION", new BigDecimal("12.50"), "USD", 1859, "AVAILABLE");

        for (BookDTO stored : List.of(negativePrice, priceInDollars)) {
            stubBooks(stored);
            stubStock(STOCK_1);

            Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com", List.of(line(BOOK_1, 2))));

            assertInvalidField(thrown, "price-out-of-range", "Price out of range", "lines",
                    domainMessage(() -> Money.of(stored.priceAmount(), stored.priceCurrency())));
        }
        verifyNothingSavedOrPublished();
    }

    @Test
    void shipShipsTheOrderAndTheStockOfEveryLine() {
        OrderFlow flowOnNanoClock = new OrderFlow(orders, books, stockItems, events, NANO_CLOCK);
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(STORED_LINES));
        stubStock(SHIP_STOCK_3, SHIP_STOCK_1, SHIP_STOCK_2);

        flowOnNanoClock.ship(ORDER_ID);

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue()).isEqualTo(new OrderDTO(ORDER_ID, "shopper@example.org", "SHIPPED",
                Instant.parse("2026-06-14T09:08:07.654321Z"), Instant.parse("2026-06-15T10:00:00.123456Z"),
                STORED_LINES));
        verify(stockItems, times(3)).save(savedStock.capture());
        assertThat(savedStock.getAllValues()).containsExactly(
                new StockItemDTO(BOOK_1, 19, 4, 3, 10, false),
                new StockItemDTO(BOOK_2, 6, 0, 2, 12, false),
                new StockItemDTO(BOOK_3, 28, 7, 4, 8, false));
        verifyNoInteractions(books, events);
    }

    @Test
    void shipSavesTheOrderFirstThenTheStockItemsInBookIdOrder() {
        // Stored lines 3, 1, 2; the stock items are read and saved in book-id order 1, 2, 3, after the order.
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(STORED_LINES));
        stubStock(SHIP_STOCK_3, SHIP_STOCK_1, SHIP_STOCK_2);

        flow.ship(ORDER_ID);

        InOrder inOrder = inOrder(orders, stockItems, events);
        inOrder.verify(orders).findById(ORDER_ID);
        inOrder.verify(stockItems).findByBookId(BOOK_1);
        inOrder.verify(stockItems).findByBookId(BOOK_2);
        inOrder.verify(stockItems).findByBookId(BOOK_3);
        inOrder.verify(orders).save(any());
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_1, 19, 4, 3, 10, false));
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_2, 6, 0, 2, 12, false));
        inOrder.verify(stockItems).save(new StockItemDTO(BOOK_3, 28, 7, 4, 8, false));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void shipRejectsAShippedOrderWith409BeforeTouchingStock() {
        when(orders.findById(ORDER_ID)).thenReturn(new OrderDTO(ORDER_ID, "shopper@example.org", "SHIPPED",
                Instant.parse("2026-06-14T09:08:07.654321Z"), Instant.parse("2026-06-14T18:30:00.000001Z"),
                STORED_LINES));

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertConflict(thrown, "order-already-shipped", "Order already shipped",
                new OrderAlreadyShippedException(new OrderId(ORDER_ID)).getMessage());
        verifyNoInteractions(stockItems, events);
        verify(orders, never()).save(any());
    }

    @Test
    void shipPassesOrderNotFoundThrough() {
        OrderDataService.OrderNotFoundException notFound = new OrderDataService.OrderNotFoundException(ORDER_ID);
        when(orders.findById(ORDER_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertThat(thrown).isSameAs(notFound);
        verifyNoInteractions(stockItems, events);
        verify(orders, never()).save(any());
    }

    @Test
    void shipTranslatesInsufficientReservedStockTo409() {
        // Book 2 is second in book-id order: book 1 is already shipped in memory, yet nothing is saved.
        StockItemDTO tooFewReserved = new StockItemDTO(BOOK_2, 9, 1, 2, 12, false);
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(STORED_LINES));
        stubStock(SHIP_STOCK_1, tooFewReserved);

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertConflict(thrown, "insufficient-reserved-stock", "Insufficient reserved stock", domainMessage(
                () -> StockItemDomainMapper.toDomain(tooFewReserved, CLOCK).ship(new Quantity(3))));
        verify(stockItems, never()).findByBookId(BOOK_3);
        verifyNothingSavedOrPublished();
    }

    @Test
    void shipTranslatesAStoredStockItemThatFailsRestoreTo409() {
        StockItemDTO onHandBelowReserved = new StockItemDTO(BOOK_2, 2, 3, 2, 12, false);
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(STORED_LINES));
        stubStock(SHIP_STOCK_1, onHandBelowReserved);

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertConflict(thrown, "on-hand-below-reserved", "On hand below reserved",
                domainMessage(() -> StockItemDomainMapper.toDomain(onHandBelowReserved, CLOCK)));
        verify(stockItems, never()).findByBookId(BOOK_3);
        verifyNothingSavedOrPublished();
    }

    @Test
    void shipPassesStockItemNotFoundThrough() {
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(STORED_LINES));
        stubStock(SHIP_STOCK_1);
        StockItemDataService.StockItemNotFoundException notFound =
                new StockItemDataService.StockItemNotFoundException(BOOK_2);
        when(stockItems.findByBookId(BOOK_2)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertThat(thrown).isSameAs(notFound);
        verify(stockItems, never()).findByBookId(BOOK_3);
        verifyNothingSavedOrPublished();
    }

    @Test
    void shipTranslatesAStoredOrderThatFailsRestore() {
        OrderLineDTO line3 = STORED_LINES.get(0);
        OrderLineDTO line1 = STORED_LINES.get(1);
        assertStoredOrderRejected(new OrderDTO(ORDER_ID, "not-an-email", "PLACED",
                Instant.parse("2026-06-14T09:08:07.654321Z"), null, STORED_LINES),
                "invalid-email", "Invalid e-mail address", "customerEmail");
        assertStoredOrderRejected(placedOrder(List.of(line1, line3, new OrderLineDTO(BOOK_1, "9780141439600",
                        "A Tale of Two Cities", new BigDecimal("12.50"), "EUR", 4))),
                "duplicate-order-line", "Duplicate order line", "lines");
        assertStoredOrderRejected(placedOrder(List.of(line3, new OrderLineDTO(BOOK_1, "9780141439600",
                        "A Tale of Two Cities", new BigDecimal("12.50"), "EUR", -1))),
                "quantity-out-of-range", "Quantity out of range", "lines");
        assertStoredOrderRejected(placedOrder(List.of()),
                "line-count-out-of-range", "Number of lines out of range", "lines");
        assertStoredOrderRejected(placedOrder(List.of(new OrderLineDTO(BOOK_3, "9780141439518",
                        "Pride and Prejudice", new BigDecimal("9.99"), "USD", 2))),
                "price-out-of-range", "Price out of range", "lines");
        verifyNoInteractions(stockItems, events);
        verify(orders, never()).save(any());
    }

    @Test
    void shipLeavesAnUnknownStoredStatusUntranslated() {
        // Accepted as a 500 by the plan: a stored status outside PLACED and SHIPPED is corrupted data.
        when(orders.findById(ORDER_ID)).thenReturn(new OrderDTO(ORDER_ID, "shopper@example.org", "BOGUS",
                Instant.parse("2026-06-14T09:08:07.654321Z"), null, STORED_LINES));

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(stockItems, events);
        verify(orders, never()).save(any());
    }

    @Test
    void shipPublishesNoEventBecauseShippingRegistersNone() {
        // Available stays 1, below the reorder level 3, with the signal unset: shipping still registers no event.
        when(orders.findById(ORDER_ID)).thenReturn(placedOrder(List.of(new OrderLineDTO(BOOK_1, "9780141439600",
                "A Tale of Two Cities", new BigDecimal("12.50"), "EUR", 2))));
        stubStock(new StockItemDTO(BOOK_1, 3, 2, 3, 10, false));

        flow.ship(ORDER_ID);

        verify(orders).save(any());
        verify(stockItems).save(new StockItemDTO(BOOK_1, 1, 0, 3, 10, false));
        verifyNoInteractions(events);
    }

    @Test
    void findByIdReturnsTheStoredOrder() {
        OrderDTO stored = new OrderDTO(ORDER_ID, "shopper@example.org", "SHIPPED",
                Instant.parse("2026-06-14T09:08:07.654321Z"), Instant.parse("2026-06-14T18:30:00.000001Z"),
                STORED_LINES);
        when(orders.findById(ORDER_ID)).thenReturn(stored);

        assertThat(flow.findById(ORDER_ID)).isSameAs(stored);
        verify(orders, never()).save(any());
        verifyNoInteractions(books, stockItems, events);
    }

    @Test
    void findByIdPassesOrderNotFoundThrough() {
        OrderDataService.OrderNotFoundException notFound = new OrderDataService.OrderNotFoundException(ORDER_ID);
        when(orders.findById(ORDER_ID)).thenThrow(notFound);

        Throwable thrown = catchThrowable(() -> flow.findById(ORDER_ID));

        assertThat(thrown).isSameAs(notFound);
        verify(orders, never()).save(any());
        verifyNoInteractions(books, stockItems, events);
    }

    @Test
    void findByCustomerAsksThePortWithTheNormalisedAddress() {
        // The port's order is sorted in no column, ascending or descending: id 2, 3, 1; placed 12, 14, 11 June;
        // status PLACED, SHIPPED, PLACED; quantity 2, 1, 3; total 25.00, 8.00, 29.97. The flow must keep it.
        OrderDTO first = new OrderDTO(UUID.fromString("2e000000-0000-4000-8000-0000000000e2"), "reader@example.com",
                "PLACED", Instant.parse("2026-06-12T08:00:00.000002Z"), null, List.of(new OrderLineDTO(BOOK_1,
                        "9780141439600", "A Tale of Two Cities", new BigDecimal("12.50"), "EUR", 2)));
        OrderDTO second = new OrderDTO(UUID.fromString("3e000000-0000-4000-8000-0000000000e3"),
                "reader@example.com", "SHIPPED", Instant.parse("2026-06-14T08:00:00Z"),
                Instant.parse("2026-06-14T12:00:00Z"), List.of(new OrderLineDTO(BOOK_2, "9780141439563",
                        "Wuthering Heights", new BigDecimal("8.00"), "EUR", 1)));
        OrderDTO third = new OrderDTO(UUID.fromString("1e000000-0000-4000-8000-0000000000e1"), "reader@example.com",
                "PLACED", Instant.parse("2026-06-11T08:00:00Z"), null, List.of(new OrderLineDTO(BOOK_3,
                        "9780141439518", "Pride and Prejudice", new BigDecimal("9.99"), "EUR", 3)));
        when(orders.findByCustomerEmail("reader@example.com")).thenReturn(List.of(first, second, third));

        assertThat(flow.findByCustomer("  Reader@Example.COM ")).containsExactly(first, second, third);
        verify(orders, never()).save(any());
        verifyNoInteractions(books, stockItems, events);
    }

    @Test
    void findByCustomerTranslatesAnInvalidEmailTo422() {
        for (String customerEmail : List.of("", "   ", "no-at-sign")) {
            Throwable thrown = catchThrowable(() -> flow.findByCustomer(customerEmail));

            assertInvalidField(thrown, "invalid-email", "Invalid e-mail address", "customerEmail",
                    domainMessage(() -> new EmailAddress(customerEmail)));
        }
        verifyNoInteractions(orders, books, stockItems, events);
    }

    private void assertStoredOrderRejected(OrderDTO stored, String code, String title, String field) {
        when(orders.findById(ORDER_ID)).thenReturn(stored);

        Throwable thrown = catchThrowable(() -> flow.ship(ORDER_ID));

        assertInvalidField(thrown, code, title, field, domainMessage(() -> OrderDomainMapper.toDomain(stored)));
    }

    private static OrderDTO placedOrder(List<OrderLineDTO> lines) {
        return new OrderDTO(ORDER_ID, "shopper@example.org", "PLACED", Instant.parse("2026-06-14T09:08:07.654321Z"),
                null, lines);
    }

    private static void assertBookNotOrderable(Throwable thrown, UUID bookId) {
        assertThat(thrown).isExactlyInstanceOf(BookNotOrderableException.class);
        BookNotOrderableException notOrderable = (BookNotOrderableException) thrown;
        assertThat(notOrderable.code()).isEqualTo("book-not-orderable");
        assertThat(notOrderable.title()).isEqualTo("Book not orderable");
        assertThat(notOrderable.getMessage()).contains(bookId.toString());
        assertThat(notOrderable.details()).isEmpty();
    }

    private static BookDTO discontinued(BookDTO book) {
        return new BookDTO(book.id(), book.isbn(), book.title(), book.authors(), book.genre(), book.priceAmount(),
                book.priceCurrency(), book.publicationYear(), "DISCONTINUED");
    }

    private void assertRequestedQuantityRejected(List<PlaceOrderLineDTO> lines, int reportedQuantity) {
        Throwable thrown = catchThrowable(() -> flow.placeOrder("reader@example.com", lines));

        assertInvalidField(thrown, "quantity-out-of-range", "Quantity out of range", "lines",
                domainMessage(() -> OrderLine.quantityOf(reportedQuantity)));
    }

    private void verifyNothingSavedOrPublished() {
        verify(orders, never()).save(any());
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

    private void stubBooks(BookDTO... dtos) {
        for (BookDTO dto : dtos) {
            when(books.findById(dto.id())).thenReturn(dto);
        }
    }

    private void stubStock(StockItemDTO... dtos) {
        for (StockItemDTO dto : dtos) {
            when(stockItems.findByBookId(dto.bookId())).thenReturn(dto);
        }
    }

    private static PlaceOrderLineDTO line(UUID bookId, int quantity) {
        return new PlaceOrderLineDTO(bookId, quantity);
    }

    private static BookDTO book(UUID id, String isbn, String title, String price) {
        return new BookDTO(id, isbn, title, List.of("Some Author"), "FICTION", new BigDecimal(price), "EUR", 1900,
                "AVAILABLE");
    }
}
