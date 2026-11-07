package nl.openvalue.bookstore.domaininteraction;

import java.util.List;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.EmailAddress;
import nl.openvalue.bookstore.domain.Money;
import nl.openvalue.bookstore.domain.Order;
import nl.openvalue.bookstore.domain.OrderId;
import nl.openvalue.bookstore.domain.OrderLineRequest;
import nl.openvalue.bookstore.domain.OrderStatus;
import nl.openvalue.bookstore.domain.Quantity;

/** Rebuilds an {@link Order} from an {@link OrderDTO} and snapshots a book into an order line request. */
public final class OrderDomainMapper {

    private OrderDomainMapper() {
    }

    /** The order checks every rule again and registers no event. */
    public static Order toDomain(OrderDTO dto) {
        List<OrderLineRequest> lines = dto.lines().stream()
                .map(line -> new OrderLineRequest(
                        new BookId(line.bookId()),
                        line.isbn(),
                        line.title(),
                        Money.of(line.unitPriceAmount(), line.unitPriceCurrency()),
                        new Quantity(line.quantity())))
                .toList();
        return Order.restore(
                new OrderId(dto.id()),
                new EmailAddress(dto.customerEmail()),
                lines,
                OrderStatus.valueOf(dto.status()),
                dto.placedAt(),
                dto.shippedAt());
    }

    /** Snapshots the isbn, title and current price of the book; the quantity is the one asked for. */
    public static OrderLineRequest toLineRequest(BookDTO book, Quantity quantity) {
        return new OrderLineRequest(
                new BookId(book.id()),
                book.isbn(),
                book.title(),
                Money.of(book.priceAmount(), book.priceCurrency()),
                quantity);
    }
}
