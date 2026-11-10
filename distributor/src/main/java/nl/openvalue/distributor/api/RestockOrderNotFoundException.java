package nl.openvalue.distributor.api;

/**
 * No restock order has the given id. Answered with 404 {@code RESTOCK_ORDER_NOT_FOUND}.
 */
public class RestockOrderNotFoundException extends RuntimeException {

    public RestockOrderNotFoundException(String id) {
        super("No restock order with id " + id);
    }
}
