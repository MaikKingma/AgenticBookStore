package nl.openvalue.bookstore.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An order of a customer: 1 to 20 lines, at most one line per book, each with 1 to 10 copies. New orders come from
 * {@link #place}, stored ones from {@link #restore}; both check every rule. Timestamps are taken from the given clock
 * and truncated to microseconds, the precision the order is stored with.
 */
public final class Order {

    private static final int MIN_LINES = 1;
    private static final int MAX_LINES = 20;

    private final OrderId id;
    private final EmailAddress customerEmail;
    private final List<OrderLine> lines;
    private OrderStatus status;
    private final Instant placedAt;
    private Instant shippedAt;
    private Instant cancelledAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Order(OrderId id, EmailAddress customerEmail, List<OrderLineRequest> lines, OrderStatus status,
            Instant placedAt, Instant shippedAt, Instant cancelledAt) {
        this.id = Objects.requireNonNull(id, "An order needs an id");
        this.customerEmail = Objects.requireNonNull(customerEmail, "An order needs a customer e-mail address");
        Objects.requireNonNull(lines, "An order needs lines");
        this.status = Objects.requireNonNull(status, "An order needs a status");
        this.placedAt = Objects.requireNonNull(placedAt, "An order needs a placed time");
        this.lines = toLines(lines);
        this.shippedAt = shippedAt;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Places a new order with the lines in the given order and registers one {@link OrderPlacedEvent}. The placed time
     * is the instant of the clock, truncated to microseconds.
     */
    public static Order place(EmailAddress customerEmail, List<OrderLineRequest> lines, Clock clock) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Order order = new Order(OrderId.newId(), customerEmail, lines, OrderStatus.PLACED, now, null, null);
        order.domainEvents.add(
                new OrderPlacedEvent(order.id.value(), order.customerEmail.value(), order.total().amount(), now));
        return order;
    }

    /** Rebuilds a stored order with every value as given, checking every rule again; it registers no event. */
    public static Order restore(OrderId id, EmailAddress customerEmail, List<OrderLineRequest> lines,
            OrderStatus status, Instant placedAt, Instant shippedAt, Instant cancelledAt) {
        return new Order(id, customerEmail, lines, status, placedAt, shippedAt, cancelledAt);
    }

    /**
     * Ships a placed order: the status becomes SHIPPED and the shipped time is the instant of the clock, truncated to
     * microseconds. An order that is already shipped throws {@link OrderAlreadyShippedException}, a cancelled one
     * {@link OrderAlreadyCancelledException}; both keep their status and times. Never registers an event.
     */
    public void ship(Clock clock) {
        if (status == OrderStatus.SHIPPED) {
            throw new OrderAlreadyShippedException(id);
        }
        if (status == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(id);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        status = OrderStatus.SHIPPED;
        shippedAt = now;
    }

    /**
     * Cancels an order that has not shipped yet: the status becomes CANCELLED and the cancelled time is the instant of
     * the clock, truncated to microseconds like the shipped time. An order that is already shipped throws
     * {@link OrderAlreadyShippedException}, a cancelled one {@link OrderAlreadyCancelledException}; both keep their
     * status and times. Never registers an event: nothing reacts to a cancellation.
     */
    public void cancel(Clock clock) {
        if (status == OrderStatus.SHIPPED) {
            throw new OrderAlreadyShippedException(id);
        }
        if (status == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException(id);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        status = OrderStatus.CANCELLED;
        cancelledAt = now;
    }

    /** Returns the events registered since the last pull, and forgets them. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public OrderId id() {
        return id;
    }

    public EmailAddress customerEmail() {
        return customerEmail;
    }

    /** The lines in the order they were given; the list cannot be changed. */
    public List<OrderLine> lines() {
        return lines;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant placedAt() {
        return placedAt;
    }

    /** The shipped time, or null while the order is not shipped. */
    public Instant shippedAt() {
        return shippedAt;
    }

    /** The cancelled time, or null while the order is not cancelled. */
    public Instant cancelledAt() {
        return cancelledAt;
    }

    /** The sum of the line totals. */
    public Money total() {
        Money total = Money.of("0.00");
        for (OrderLine line : lines) {
            total = total.plus(line.lineTotal());
        }
        return total;
    }

    private static List<OrderLine> toLines(List<OrderLineRequest> requests) {
        if (requests.size() < MIN_LINES || requests.size() > MAX_LINES) {
            throw new OrderLineCountOutOfRangeException(requests.size(), MIN_LINES, MAX_LINES);
        }
        List<OrderLine> lines = new ArrayList<>(requests.size());
        Set<BookId> books = new HashSet<>();
        for (OrderLineRequest request : requests) {
            OrderLine line = new OrderLine(request);
            if (!books.add(line.bookId())) {
                throw new DuplicateOrderLineException(line.bookId());
            }
            lines.add(line);
        }
        return List.copyOf(lines);
    }
}
