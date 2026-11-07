package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Queries that read the stock of the books. */
@RestController
@RequestMapping("/inventory")
public class InventoryQueries {

    private final InventoryFlow inventory;

    public InventoryQueries(InventoryFlow inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/{bookId}")
    public StockView get(@PathVariable UUID bookId) {
        return StockView.from(inventory.findByBookId(bookId));
    }

    /**
     * Lists the stock items at or below their reorder level, most urgent first. The filter is mandatory and accepts
     * only the exact text {@code true}; every other value, or none, is a bad request.
     */
    @GetMapping
    public List<StockView> lowStock(@RequestParam(required = false) String lowStock) {
        if (!"true".equals(lowStock)) {
            throw new InvalidQueryParameterException("lowStock", "must be true");
        }
        return inventory.findLowStock().stream().map(StockView::from).toList();
    }

    /** A query parameter has a value this endpoint does not accept. The handler turns it into a 400 problem. */
    public static final class InvalidQueryParameterException extends RuntimeException {

        private final String parameter;

        public InvalidQueryParameterException(String parameter, String message) {
            super(message);
            this.parameter = parameter;
        }

        public String parameter() {
            return parameter;
        }
    }
}
