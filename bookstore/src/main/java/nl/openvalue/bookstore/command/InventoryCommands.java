package nl.openvalue.bookstore.command;

import jakarta.validation.Valid;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Commands that change the stock of a book. Every command is accepted with 202. */
@RestController
@RequestMapping("/inventory")
public class InventoryCommands {

    private final InventoryFlow inventory;

    public InventoryCommands(InventoryFlow inventory) {
        this.inventory = inventory;
    }

    @PostMapping("/{bookId}/commands/receive")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@PathVariable UUID bookId, @Valid @RequestBody ReceiveStockPayload payload) {
        inventory.receive(bookId, payload.quantity());
    }

    @PostMapping("/{bookId}/commands/correct")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void correct(@PathVariable UUID bookId, @Valid @RequestBody CorrectStockPayload payload) {
        inventory.correct(bookId, payload.onHand(), payload.reason());
    }

    /**
     * Asks the distributor to restock the book now. It takes no body. The distributor's failures are not hidden: an
     * outage is 503, an unknown or refused title is 404 or 422, and a book without a stock item is 404.
     */
    @PostMapping("/{bookId}/commands/request-restock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestRestock(@PathVariable UUID bookId) {
        inventory.requestRestock(bookId);
    }
}
