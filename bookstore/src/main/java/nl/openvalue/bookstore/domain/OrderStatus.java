package nl.openvalue.bookstore.domain;

/** Where an order stands: placed with its stock reserved, shipped, or cancelled with its stock released. */
public enum OrderStatus {
    PLACED,
    SHIPPED,
    CANCELLED
}
