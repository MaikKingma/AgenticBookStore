package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.DuplicateOrderLineException;
import nl.openvalue.bookstore.domain.EmailAddress;
import nl.openvalue.bookstore.domain.InvalidEmailAddressException;
import nl.openvalue.bookstore.domain.InvalidMoneyException;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.Order;
import nl.openvalue.bookstore.domain.OrderLine;
import nl.openvalue.bookstore.domain.OrderLineCountOutOfRangeException;
import nl.openvalue.bookstore.domain.OrderLineRequest;
import nl.openvalue.bookstore.domain.OrderStatus;
import nl.openvalue.bookstore.domain.OrderedQuantityOutOfRangeException;
import nl.openvalue.bookstore.domain.Quantity;
import org.junit.jupiter.api.Test;

class OrderDomainMapperTest {

    private static final Clock NANO_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T10:00:00.123456789Z"), ZoneOffset.UTC);

    private static final Instant PLACED_AT = Instant.parse("2026-06-15T10:00:00.123456Z");
    private static final Instant SHIPPED_AT = Instant.parse("2026-06-16T09:30:15.654321Z");

    private static final UUID ORDER_ID = UUID.fromString("2c9a7f10-4b3d-4e8a-9d52-6a1f0c3b8e77");

    // Three lines, sorted in no column in either direction: the middle line holds the extreme of every column.
    private static final UUID BOOK_A = UUID.fromString("3a1d5e90-7c24-4b6f-8e13-5d9a2c4f0b61");
    private static final UUID BOOK_B = UUID.fromString("7e4b2c18-9a05-4d3e-b7f6-1c8d0a5e3f92");
    private static final UUID BOOK_C = UUID.fromString("1f6c8a24-5b97-4e01-a3d8-9e2b7c4d6a05");

    private static OrderDTO threeLineOrder(String status, Instant shippedAt) {
        return new OrderDTO(ORDER_ID, "reader@example.com", status, PLACED_AT, shippedAt, List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 2),
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "EUR", 7),
                new OrderLineDTO(BOOK_C, "9780192833598", "Persuasion", new BigDecimal("21.99"), "EUR", 3)));
    }

    private static OrderLineRequest request(UUID bookId, String isbn, String title, String price, int quantity) {
        return new OrderLineRequest(new BookId(bookId), isbn, title, Money.of(price), new Quantity(quantity));
    }

    private static List<OrderLineRequest> threeRequests() {
        return List.of(
                request(BOOK_A, "9780141439518", "Middlemarch", "12.50", 2),
                request(BOOK_B, "9780140449136", "Emma", "8.00", 7),
                request(BOOK_C, "9780192833598", "Persuasion", "21.99", 3));
    }

    @Test
    void roundTripsAPlacedOrder() {
        OrderDTO original = threeLineOrder("PLACED", null);

        Order order = OrderDomainMapper.toDomain(original);

        assertThat(new OrderDTO(order)).isEqualTo(original);
    }

    @Test
    void roundTripsAShippedOrder() {
        OrderDTO original = threeLineOrder("SHIPPED", SHIPPED_AT);

        Order order = OrderDomainMapper.toDomain(original);

        assertThat(new OrderDTO(order)).isEqualTo(original);
    }

    @Test
    void toDomainKeepsEveryValue() {
        Order order = OrderDomainMapper.toDomain(threeLineOrder("SHIPPED", SHIPPED_AT));

        assertThat(order.id().value()).isEqualTo(ORDER_ID);
        assertThat(order.customerEmail().value()).isEqualTo("reader@example.com");
        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00.123456Z"));
        assertThat(order.shippedAt()).isEqualTo(Instant.parse("2026-06-16T09:30:15.654321Z"));
        assertThat(order.lines()).hasSize(3);
        OrderLine first = order.lines().get(0);
        assertThat(first.bookId().value()).isEqualTo(UUID.fromString("3a1d5e90-7c24-4b6f-8e13-5d9a2c4f0b61"));
        assertThat(first.isbn()).isEqualTo("9780141439518");
        assertThat(first.title()).isEqualTo("Middlemarch");
        assertThat(first.unitPrice().amount().toPlainString()).isEqualTo("12.50");
        assertThat(first.unitPrice().currency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(first.quantity().value()).isEqualTo(2);
        OrderLine second = order.lines().get(1);
        assertThat(second.bookId().value()).isEqualTo(UUID.fromString("7e4b2c18-9a05-4d3e-b7f6-1c8d0a5e3f92"));
        assertThat(second.isbn()).isEqualTo("9780140449136");
        assertThat(second.title()).isEqualTo("Emma");
        assertThat(second.unitPrice().amount().toPlainString()).isEqualTo("8.00");
        assertThat(second.quantity().value()).isEqualTo(7);
        OrderLine third = order.lines().get(2);
        assertThat(third.bookId().value()).isEqualTo(UUID.fromString("1f6c8a24-5b97-4e01-a3d8-9e2b7c4d6a05"));
        assertThat(third.isbn()).isEqualTo("9780192833598");
        assertThat(third.title()).isEqualTo("Persuasion");
        assertThat(third.unitPrice().amount().toPlainString()).isEqualTo("21.99");
        assertThat(third.quantity().value()).isEqualTo(3);
    }

    @Test
    void toDomainKeepsAPlacedOrderWithoutAShippedTime() {
        Order order = OrderDomainMapper.toDomain(threeLineOrder("PLACED", null));

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.shippedAt()).isNull();
    }

    @Test
    void toDomainKeepsAnotherIdAndPlacedTime() {
        OrderDTO dto = new OrderDTO(UUID.fromString("9b5e1c3a-6d2f-4a87-b0e4-3c7f9a1d5e68"), "reader@example.com",
                "PLACED", Instant.parse("2025-12-31T23:59:59.987654Z"), null, List.of(
                        new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 2)));

        Order order = OrderDomainMapper.toDomain(dto);

        assertThat(order.id().value()).isEqualTo(UUID.fromString("9b5e1c3a-6d2f-4a87-b0e4-3c7f9a1d5e68"));
        assertThat(order.placedAt()).isEqualTo(Instant.parse("2025-12-31T23:59:59.987654Z"));
    }

    @Test
    void newOrderDTOCopiesEveryValueOfAPlacedOrder() {
        Order order = Order.place(new EmailAddress("Reader@Example.COM"), threeRequests(), NANO_CLOCK);

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.id()).isEqualTo(order.id().value());
        assertThat(dto.customerEmail()).isEqualTo("reader@example.com");
        assertThat(dto.status()).isEqualTo("PLACED");
        assertThat(dto.placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00.123456Z"));
        assertThat(dto.shippedAt()).isNull();
        assertThat(dto.lines()).hasSize(3);
        OrderLineDTO first = dto.lines().get(0);
        assertThat(first.bookId()).isEqualTo(UUID.fromString("3a1d5e90-7c24-4b6f-8e13-5d9a2c4f0b61"));
        assertThat(first.isbn()).isEqualTo("9780141439518");
        assertThat(first.title()).isEqualTo("Middlemarch");
        assertThat(first.unitPriceAmount().toPlainString()).isEqualTo("12.50");
        assertThat(first.unitPriceCurrency()).isEqualTo("EUR");
        assertThat(first.quantity()).isEqualTo(2);
        OrderLineDTO second = dto.lines().get(1);
        assertThat(second.bookId()).isEqualTo(UUID.fromString("7e4b2c18-9a05-4d3e-b7f6-1c8d0a5e3f92"));
        assertThat(second.isbn()).isEqualTo("9780140449136");
        assertThat(second.title()).isEqualTo("Emma");
        assertThat(second.unitPriceAmount().toPlainString()).isEqualTo("8.00");
        assertThat(second.quantity()).isEqualTo(7);
        OrderLineDTO third = dto.lines().get(2);
        assertThat(third.bookId()).isEqualTo(UUID.fromString("1f6c8a24-5b97-4e01-a3d8-9e2b7c4d6a05"));
        assertThat(third.isbn()).isEqualTo("9780192833598");
        assertThat(third.title()).isEqualTo("Persuasion");
        assertThat(third.unitPriceAmount().toPlainString()).isEqualTo("21.99");
        assertThat(third.quantity()).isEqualTo(3);
    }

    @Test
    void newOrderDTOCopiesTheStatusAndShippedTimeOfAShippedOrder() {
        Order order = OrderDomainMapper.toDomain(threeLineOrder("SHIPPED", SHIPPED_AT));

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.status()).isEqualTo("SHIPPED");
        assertThat(dto.shippedAt()).isEqualTo(Instant.parse("2026-06-16T09:30:15.654321Z"));
        assertThat(dto.placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00.123456Z"));
    }

    @Test
    void newOrderDTOCopiesAnotherCustomerEmail() {
        Order order = Order.place(new EmailAddress("Other@Example.ORG"), threeRequests(), NANO_CLOCK);

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.customerEmail()).isEqualTo("other@example.org");
    }

    @Test
    void toDomainStillChecksTheOrderRules() {
        OrderLineDTO line = new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"),
                "EUR", 2);
        OrderLineDTO otherLine = new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"),
                "EUR", 1);

        assertThatThrownBy(() -> OrderDomainMapper.toDomain(order("reader@example.com", List.of())))
                .isInstanceOf(OrderLineCountOutOfRangeException.class);
        assertThatThrownBy(() -> OrderDomainMapper.toDomain(order("reader@example.com", List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 11)))))
                .isInstanceOf(OrderedQuantityOutOfRangeException.class);
        assertThatThrownBy(() -> OrderDomainMapper.toDomain(
                order("reader@example.com", List.of(line, otherLine, line))))
                .isInstanceOf(DuplicateOrderLineException.class);
        assertThatThrownBy(() -> OrderDomainMapper.toDomain(order("not an address", List.of(line))))
                .isInstanceOf(InvalidEmailAddressException.class);
    }

    private static OrderDTO order(String email, List<OrderLineDTO> lines) {
        return new OrderDTO(ORDER_ID, email, "PLACED", PLACED_AT, null, lines);
    }

    @Test
    void toDomainRejectsAStoredLineInAnotherCurrency() {
        OrderDTO dto = order("reader@example.com", List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 2),
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "USD", 7),
                new OrderLineDTO(BOOK_C, "9780192833598", "Persuasion", new BigDecimal("21.99"), "EUR", 3)));

        assertThatThrownBy(() -> OrderDomainMapper.toDomain(dto)).isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void toDomainRejectsAnUnknownStoredStatus() {
        OrderDTO dto = new OrderDTO(ORDER_ID, "reader@example.com", "BOGUS", PLACED_AT, null, List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 2)));

        assertThatThrownBy(() -> OrderDomainMapper.toDomain(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toDomainRegistersNoEvent() {
        Order order = OrderDomainMapper.toDomain(threeLineOrder("PLACED", null));

        assertThat(order.pullDomainEvents()).isEmpty();
    }

    @Test
    void toLineRequestSnapshotsTheBookAndTakesTheQuantity() {
        UUID bookId = UUID.fromString("4d8e2a6c-1b3f-4c75-9e0a-7f5b3d1c8a24");
        BookDTO book = new BookDTO(bookId, "9780679783268", "Pride and Prejudice", List.of("Jane Austen"),
                "FICTION", new BigDecimal("17.35"), "EUR", 1813, "AVAILABLE");

        OrderLineRequest request = OrderDomainMapper.toLineRequest(book, new Quantity(4));

        assertThat(request.bookId().value()).isEqualTo(UUID.fromString("4d8e2a6c-1b3f-4c75-9e0a-7f5b3d1c8a24"));
        assertThat(request.isbn()).isEqualTo("9780679783268");
        assertThat(request.title()).isEqualTo("Pride and Prejudice");
        assertThat(request.unitPrice().amount().toPlainString()).isEqualTo("17.35");
        assertThat(request.unitPrice().currency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(request.quantity().value()).isEqualTo(4);
    }

    @Test
    void toLineRequestRejectsABookPricedInAnotherCurrency() {
        BookDTO dollars = new BookDTO(UUID.fromString("4d8e2a6c-1b3f-4c75-9e0a-7f5b3d1c8a24"), "9780679783268",
                "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal("17.35"), "USD", 1813,
                "AVAILABLE");

        assertThatThrownBy(() -> OrderDomainMapper.toLineRequest(dollars, new Quantity(4)))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void toLineRequestKeepsAnotherBookId() {
        BookDTO book = new BookDTO(UUID.fromString("8c2f6e4a-0d9b-4e13-a7c5-2b1f8d6e3a90"), "9780679783268",
                "Pride and Prejudice", List.of("Jane Austen"), "FICTION", new BigDecimal("17.35"), "EUR", 1813,
                "AVAILABLE");

        OrderLineRequest request = OrderDomainMapper.toLineRequest(book, new Quantity(4));

        assertThat(request.bookId().value()).isEqualTo(UUID.fromString("8c2f6e4a-0d9b-4e13-a7c5-2b1f8d6e3a90"));
    }

    @Test
    void derivedTotalsAgreeWithTheAggregate() {
        Order order = Order.place(new EmailAddress("reader@example.com"), List.of(
                request(BOOK_A, "9780141439518", "Middlemarch", "12.50", 2),
                request(BOOK_B, "9780140449136", "Emma", "8.00", 1)), NANO_CLOCK);

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.lines().get(0).lineTotal()).isEqualTo(order.lines().get(0).lineTotal().amount());
        assertThat(dto.lines().get(1).lineTotal()).isEqualTo(order.lines().get(1).lineTotal().amount());
        assertThat(dto.lines().get(0).lineTotal().toPlainString()).isEqualTo("25.00");
        assertThat(dto.lines().get(1).lineTotal().toPlainString()).isEqualTo("8.00");
        assertThat(dto.total()).isEqualTo(order.total().amount());
        assertThat(dto.total().toPlainString()).isEqualTo("33.00");
    }

    @Test
    void derivedTotalsAgreeWithTheAggregateForTheSmallestPrice() {
        Order order = Order.place(new EmailAddress("reader@example.com"),
                List.of(request(BOOK_A, "9780141439518", "Middlemarch", "0.01", 1)), NANO_CLOCK);

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.lines().get(0).lineTotal().toPlainString()).isEqualTo("0.01");
        assertThat(dto.total()).isEqualTo(order.total().amount());
        assertThat(dto.total().toPlainString()).isEqualTo("0.01");
    }

    @Test
    void derivedTotalsAgreeWithTheAggregateForTwentyLinesAtTheLargestPrice() {
        List<OrderLineRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            requests.add(request(new UUID(i, 0L), "9780141439518", "Title " + i, "500.00", 10));
        }
        Order order = Order.place(new EmailAddress("reader@example.com"), requests, NANO_CLOCK);

        OrderDTO dto = new OrderDTO(order);

        assertThat(dto.lines().get(0).lineTotal().toPlainString()).isEqualTo("5000.00");
        assertThat(dto.total()).isEqualTo(order.total().amount());
        assertThat(dto.total().toPlainString()).isEqualTo("100000.00");
    }

    @Test
    void lineTotalHasScaleTwoForAnyStoredScale() {
        OrderLineDTO oneDecimal = new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.5"),
                "EUR", 2);
        OrderLineDTO threeDecimalsDown = new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch",
                new BigDecimal("0.125"), "EUR", 1);
        OrderLineDTO threeDecimalsUp = new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch",
                new BigDecimal("0.135"), "EUR", 1);

        assertThat(oneDecimal.lineTotal().toPlainString()).isEqualTo("25.00");
        assertThat(oneDecimal.lineTotal().scale()).isEqualTo(2);
        assertThat(threeDecimalsDown.lineTotal().toPlainString()).isEqualTo("0.12");
        assertThat(threeDecimalsUp.lineTotal().toPlainString()).isEqualTo("0.14");
    }

    @Test
    void totalHasScaleTwoForAnyStoredScale() {
        OrderDTO dto = new OrderDTO(ORDER_ID, "reader@example.com", "PLACED", PLACED_AT, null, List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.5"), "EUR", 2),
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8"), "EUR", 1)));

        assertThat(dto.total().toPlainString()).isEqualTo("33.00");
        assertThat(dto.total().scale()).isEqualTo(2);
    }

    @Test
    void currencyIsTheCurrencyOfTheLines() {
        OrderDTO dto = new OrderDTO(ORDER_ID, "reader@example.com", "PLACED", PLACED_AT, null, List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "USD", 2),
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "USD", 1)));

        assertThat(dto.currency()).isEqualTo("USD");
    }

    @Test
    void currencyIsTheCurrencyOfTheFirstLine() {
        OrderDTO dto = new OrderDTO(ORDER_ID, "reader@example.com", "PLACED", PLACED_AT, null, List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "USD", 2),
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "EUR", 1),
                new OrderLineDTO(BOOK_C, "9780192833598", "Persuasion", new BigDecimal("21.99"), "EUR", 3)));

        assertThat(dto.currency()).isEqualTo("USD");
    }

    @Test
    void orderDTOCopiesTheLineList() {
        List<OrderLineDTO> source = new ArrayList<>(List.of(
                new OrderLineDTO(BOOK_A, "9780141439518", "Middlemarch", new BigDecimal("12.50"), "EUR", 2)));
        OrderDTO dto = new OrderDTO(ORDER_ID, "reader@example.com", "PLACED", PLACED_AT, null, source);

        source.add(new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "EUR", 1));
        source.remove(0);

        assertThat(dto.lines()).hasSize(1);
        assertThat(dto.lines().get(0).bookId()).isEqualTo(BOOK_A);
        assertThatThrownBy(() -> dto.lines().add(
                new OrderLineDTO(BOOK_B, "9780140449136", "Emma", new BigDecimal("8.00"), "EUR", 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
