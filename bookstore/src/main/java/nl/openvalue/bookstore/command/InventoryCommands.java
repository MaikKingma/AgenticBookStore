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
}
