package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Queries that read the catalogue, each book shown with its stock. A book without a stock item is not hidden: the
 * missing stock item surfaces as its 404.
 */
@RestController
@RequestMapping("/books")
public class BookQueries {

    private final CatalogueFlow catalogue;

    private final InventoryFlow inventory;

    public BookQueries(CatalogueFlow catalogue, InventoryFlow inventory) {
        this.catalogue = catalogue;
        this.inventory = inventory;
    }

    @GetMapping
    public List<BookView> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String title, @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre) {
        return catalogue.search(q, title, author, genre).stream().map(this::withStock).toList();
    }

    @GetMapping("/{id}")
    public BookDetailView get(@PathVariable UUID id) {
        BookDTO book = catalogue.findById(id);
        return BookDetailView.from(book, inventory.findByBookId(id));
    }

    private BookView withStock(BookDTO book) {
        return BookView.from(book, inventory.findByBookId(book.id()));
    }
}
