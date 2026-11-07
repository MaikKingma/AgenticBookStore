package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.OrderFlow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Queries that read the orders. */
@RestController
@RequestMapping("/orders")
public class OrderQueries {

    private final OrderFlow orders;

    public OrderQueries(OrderFlow orders) {
        this.orders = orders;
    }

    @GetMapping("/{id}")
    public OrderView get(@PathVariable UUID id) {
        return OrderView.from(orders.findById(id));
    }

    /**
     * Lists the orders of a customer, in the order the flow returns them. The parameter is mandatory; any value that
     * is present, an empty or blank one included, goes to the flow, which judges it.
     */
    @GetMapping
    public List<OrderSummaryView> byCustomer(@RequestParam(required = false) String customerEmail) {
        if (customerEmail == null) {
            throw new MissingQueryParameterException("customerEmail");
        }
        return orders.findByCustomer(customerEmail).stream().map(OrderSummaryView::from).toList();
    }

    /** A required query parameter is missing. The handler turns it into a 400 problem. */
    public static final class MissingQueryParameterException extends RuntimeException {

        private final String parameter;

        public MissingQueryParameterException(String parameter) {
            super("The query parameter '" + parameter + "' is required");
            this.parameter = parameter;
        }

        public String parameter() {
            return parameter;
        }
    }
}
