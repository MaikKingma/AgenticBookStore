package nl.openvalue.bookstore.domain;

import java.util.Objects;
import java.util.UUID;

/** The identity of an order. */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "An order id needs a value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
