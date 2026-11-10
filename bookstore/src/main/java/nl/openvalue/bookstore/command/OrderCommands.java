package nl.openvalue.bookstore.command;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.OrderFlow;
import nl.openvalue.bookstore.domaininteraction.PlaceOrderLineDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Commands that place, ship and cancel orders. Every command is accepted with 202. */
@RestController
@RequestMapping("/orders")
public class OrderCommands {

    private final OrderFlow orders;

    public OrderCommands(OrderFlow orders) {
        this.orders = orders;
    }

    @PostMapping("/commands/place")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandResult place(@Valid @RequestBody PlaceOrderPayload payload) {
        List<PlaceOrderLineDTO> lines = payload.lines().stream()
                .map(line -> new PlaceOrderLineDTO(line.bookId(), line.quantity()))
                .toList();
        return new CommandResult(orders.placeOrder(payload.customerEmail(), lines));
    }

    @PostMapping("/{id}/commands/ship")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ship(@PathVariable UUID id) {
        orders.ship(id);
    }

    @PostMapping("/{id}/commands/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cancel(@PathVariable UUID id) {
        orders.cancelOrder(id);
    }
}
