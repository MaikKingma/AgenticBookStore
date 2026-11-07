package nl.openvalue.bookstore.domain;

/** Where an order stands: placed with its stock reserved, or shipped. */
public enum OrderStatus {
    PLACED,
    SHIPPED
}
