package nl.openvalue.bookstore.command;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Commands that change the catalogue. Every command is accepted with 202. */
@RestController
@RequestMapping("/books")
public class BookCommands {

    private final CatalogueFlow catalogue;

    public BookCommands(CatalogueFlow catalogue) {
        this.catalogue = catalogue;
    }

    @PostMapping("/commands/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandResult add(@Valid @RequestBody AddBookPayload payload) {
        UUID id = catalogue.addBook(payload.isbn(), payload.title(), payload.authors(), payload.genre(),
                new BigDecimal(payload.price()), payload.publicationYear());
        return new CommandResult(id);
    }

    @PostMapping("/commands/import-from-distributor")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandResult importFromDistributor(@Valid @RequestBody ImportBookPayload payload) {
        BigDecimal price = payload.price() == null ? null : new BigDecimal(payload.price());
        UUID id = catalogue.importFromDistributor(payload.isbn(), price);
        return new CommandResult(id);
    }

    @PostMapping("/{id}/commands/change-price")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void changePrice(@PathVariable UUID id, @Valid @RequestBody ChangePricePayload payload) {
        catalogue.changePrice(id, new BigDecimal(payload.price()));
    }

    @PostMapping("/{id}/commands/discontinue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void discontinue(@PathVariable UUID id) {
        catalogue.discontinue(id);
    }
}
