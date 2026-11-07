package nl.openvalue.bookstore.domaininteraction;

import java.util.List;
import java.util.UUID;

/** The store of orders. It speaks DTOs and JDK types only. */
public interface OrderDataService {

    /**
     * Stores the order: inserts it when the id is unknown, otherwise updates only the status and the shipped time.
     * The lines and the other values never change after the insert.
     */
    void save(OrderDTO order);

    /**
     * Loads one order.
     *
     * @throws OrderNotFoundException when no order has the id
     */
    OrderDTO findById(UUID id);

    /**
     * Finds the orders whose stored e-mail address equals the given, already normalised address. The result is
     * ordered by placed time, newest first; a tie on the placed time is broken by id, descending.
     */
    List<OrderDTO> findByCustomerEmail(String customerEmail);

    /** No order exists with the requested id. */
    final class OrderNotFoundException extends NotFoundException {

        public OrderNotFoundException(UUID id) {
            super("order-not-found", "Order not found", "No order with id " + id);
        }
    }
}
