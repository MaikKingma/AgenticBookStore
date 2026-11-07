package nl.openvalue.bookstore.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for placing an order. This payload repeats the domain rules (an e-mail address of at most 254
 * characters without spaces or control characters, one to 20 lines) so that a request the domain would refuse is a
 * 400 and never reaches the domain.
 */
public record PlaceOrderPayload(
        @NotNull @Size(max = 254) @Pattern(regexp = "[^@\\x00-\\x20]+@[^@\\x00-\\x20]+\\.[^@\\x00-\\x20]+")
        String customerEmail,
        @NotNull @Size(min = 1, max = 20) List<@Valid @NotNull OrderLinePayload> lines) {
}
