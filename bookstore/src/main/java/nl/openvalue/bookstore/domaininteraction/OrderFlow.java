package nl.openvalue.bookstore.domaininteraction;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import nl.openvalue.bookstore.domain.BookStatus;
import nl.openvalue.bookstore.domain.DomainEvent;
import nl.openvalue.bookstore.domain.DuplicateOrderLineException;
import nl.openvalue.bookstore.domain.EmailAddress;
import nl.openvalue.bookstore.domain.InsufficientReservedStockException;
import nl.openvalue.bookstore.domain.InsufficientStockException;
import nl.openvalue.bookstore.domain.InvalidEmailAddressException;
import nl.openvalue.bookstore.domain.InvalidMoneyException;
import nl.openvalue.bookstore.domain.InvalidQuantityException;
import nl.openvalue.bookstore.domain.OnHandBelowReservedException;
import nl.openvalue.bookstore.domain.Order;
import nl.openvalue.bookstore.domain.OrderAlreadyShippedException;
import nl.openvalue.bookstore.domain.OrderLine;
import nl.openvalue.bookstore.domain.OrderLineCountOutOfRangeException;
import nl.openvalue.bookstore.domain.OrderLineRequest;
import nl.openvalue.bookstore.domain.OrderedQuantityOutOfRangeException;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.StockItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The order use cases. Placing and shipping an order change the order and the stock of several books in one
 * transaction. Every check runs before anything is written; then the order is saved first and the stock items follow
 * in book-id order, so concurrent orders lock the stock rows in the same order. The events of the order and of every
 * stock item are published last, inside the same transaction, so listeners bound to the commit only see saved data.
 * Every domain exception is translated here into one of the four use-case exceptions; port exceptions pass through
 * unchanged.
 */
@Service
@Transactional
public class OrderFlow {

    private final OrderDataService orders;
    private final BookDataService books;
    private final StockItemDataService stockItems;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderFlow(OrderDataService orders, BookDataService books, StockItemDataService stockItems,
            ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.books = books;
        this.stockItems = stockItems;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Places an order and reserves its stock. The checks run in this order: the e-mail address; each requested
     * quantity; the quantities merged per book, keeping the position of the first appearance; each book once, in that
     * order, stopping at the first unknown or discontinued book; each stock item once, in that order, collecting every
     * shortage into one conflict; finally the order rules. Returns the id of the new order.
     */
    public UUID placeOrder(String customerEmail, List<PlaceOrderLineDTO> lines) {
        EmailAddress email = translatingDomainExceptions(() -> new EmailAddress(customerEmail));
        Map<UUID, Quantity> merged = translatingDomainExceptions(() -> mergedQuantities(lines));

        Map<UUID, BookDTO> orderable = new LinkedHashMap<>();
        for (UUID bookId : merged.keySet()) {
            BookDTO book = books.findById(bookId);
            if (BookStatus.DISCONTINUED.name().equals(book.status())) {
                throw new BookNotOrderableException(bookId);
            }
            orderable.put(bookId, book);
        }

        List<StockItem> reserved = new ArrayList<>();
        List<InsufficientStockException> shortages = new ArrayList<>();
        for (Map.Entry<UUID, Quantity> entry : merged.entrySet()) {
            StockItemDTO stored = stockItems.findByBookId(entry.getKey());
            StockItem item = translatingDomainExceptions(() -> StockItemDomainMapper.toDomain(stored, clock));
            try {
                item.reserve(entry.getValue());
            } catch (InsufficientStockException e) {
                shortages.add(e);
            }
            reserved.add(item);
        }
        if (!shortages.isEmpty()) {
            throw insufficientStock(shortages);
        }

        Order order = translatingDomainExceptions(() -> {
            List<OrderLineRequest> requests = new ArrayList<>();
            for (Map.Entry<UUID, Quantity> entry : merged.entrySet()) {
                requests.add(OrderDomainMapper.toLineRequest(orderable.get(entry.getKey()), entry.getValue()));
            }
            return Order.place(email, requests, clock);
        });
        saveAndPublish(order, reserved);
        return order.id().value();
    }

    /**
     * Ships a placed order: the order is shipped before any stock item is read, then the reserved stock of every line
     * is shipped in book-id order.
     */
    public void ship(UUID orderId) {
        OrderDTO stored = orders.findById(orderId);
        Order order = translatingDomainExceptions(() -> {
            Order restored = OrderDomainMapper.toDomain(stored);
            restored.ship(clock);
            return restored;
        });

        List<OrderLine> linesInBookIdOrder = new ArrayList<>(order.lines());
        linesInBookIdOrder.sort((first, second) -> first.bookId().value().compareTo(second.bookId().value()));
        List<StockItem> shipped = new ArrayList<>();
        for (OrderLine line : linesInBookIdOrder) {
            StockItemDTO storedItem = stockItems.findByBookId(line.bookId().value());
            shipped.add(translatingDomainExceptions(() -> {
                StockItem item = StockItemDomainMapper.toDomain(storedItem, clock);
                item.ship(line.quantity());
                return item;
            }));
        }
        saveAndPublish(order, shipped);
    }

    @Transactional(readOnly = true)
    public OrderDTO findById(UUID orderId) {
        return orders.findById(orderId);
    }

    /**
     * The orders of a customer, in the order of the port: newest first. The address is normalised before the lookup,
     * so the lookup ignores case and surrounding spaces.
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> findByCustomer(String customerEmail) {
        EmailAddress email = translatingDomainExceptions(() -> new EmailAddress(customerEmail));
        return orders.findByCustomerEmail(email.value());
    }

    /**
     * Saves the order, then every changed stock item in ascending book-id order, then publishes the events of the
     * order and the events of every stock item in the same book-id order.
     */
    private void saveAndPublish(Order order, List<StockItem> changedStock) {
        List<StockItem> inBookIdOrder = new ArrayList<>(changedStock);
        inBookIdOrder.sort((first, second) -> first.bookId().value().compareTo(second.bookId().value()));
        orders.save(new OrderDTO(order));
        for (StockItem item : inBookIdOrder) {
            stockItems.save(new StockItemDTO(item));
        }
        for (DomainEvent event : order.pullDomainEvents()) {
            events.publishEvent(event);
        }
        for (StockItem item : inBookIdOrder) {
            for (DomainEvent event : item.pullDomainEvents()) {
                events.publishEvent(event);
            }
        }
    }

    /**
     * Checks every requested quantity in request order, then merges the quantities per book, keeping the position of
     * the first appearance, and checks every merged quantity.
     */
    private static Map<UUID, Quantity> mergedQuantities(List<PlaceOrderLineDTO> lines) {
        for (PlaceOrderLineDTO line : lines) {
            OrderLine.quantityOf(line.quantity());
        }
        Map<UUID, Integer> summed = new LinkedHashMap<>();
        for (PlaceOrderLineDTO line : lines) {
            summed.merge(line.bookId(), line.quantity(), Integer::sum);
        }
        Map<UUID, Quantity> merged = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : summed.entrySet()) {
            merged.put(entry.getKey(), OrderLine.quantityOf(entry.getValue()));
        }
        return merged;
    }

    /** One conflict naming every short book in request order, with the requested and available copies. */
    private static ConflictException insufficientStock(List<InsufficientStockException> shortages) {
        List<Map<String, String>> details = shortages.stream()
                .map(shortage -> Map.of(
                        "bookId", shortage.bookId().value().toString(),
                        "requested", String.valueOf(shortage.requested()),
                        "available", String.valueOf(shortage.available())))
                .toList();
        return new ConflictException("insufficient-stock", "Insufficient stock",
                "Not enough stock for " + shortages.size() + " of the requested books", details);
    }

    /** Runs a call into the domain and translates any domain exception it throws into a use-case exception. */
    private static <T> T translatingDomainExceptions(Supplier<T> domainCall) {
        try {
            return domainCall.get();
        } catch (InvalidEmailAddressException e) {
            throw invalidField("invalid-email", "Invalid e-mail address", "customerEmail", e);
        } catch (OrderedQuantityOutOfRangeException | InvalidQuantityException e) {
            throw invalidField("quantity-out-of-range", "Quantity out of range", "lines", e);
        } catch (OrderLineCountOutOfRangeException e) {
            throw invalidField("line-count-out-of-range", "Number of lines out of range", "lines", e);
        } catch (DuplicateOrderLineException e) {
            throw invalidField("duplicate-order-line", "Duplicate order line", "lines", e);
        } catch (InvalidMoneyException e) {
            throw invalidField("price-out-of-range", "Price out of range", "lines", e);
        } catch (OrderAlreadyShippedException e) {
            throw new ConflictException("order-already-shipped", "Order already shipped", e.getMessage());
        } catch (InsufficientReservedStockException e) {
            throw new ConflictException("insufficient-reserved-stock", "Insufficient reserved stock",
                    e.getMessage());
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
