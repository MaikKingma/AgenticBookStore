package nl.openvalue.bookstore.domain;

/** Thrown when an order that has already been shipped is shipped again. */
public class OrderAlreadyShippedException extends RuntimeException {

    private final OrderId orderId;

    public OrderAlreadyShippedException(OrderId orderId) {
        super("Order " + orderId.value() + " has already been shipped");
        this.orderId = orderId;
    }

    public OrderId orderId() {
        return orderId;
    }
}
