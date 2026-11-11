package nl.openvalue.bookstore.domain;

/** Thrown when an order that has already been cancelled is cancelled again or shipped. */
public class OrderAlreadyCancelledException extends RuntimeException {

    private final OrderId orderId;

    public OrderAlreadyCancelledException(OrderId orderId) {
        super("Order " + orderId.value() + " has already been cancelled");
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
