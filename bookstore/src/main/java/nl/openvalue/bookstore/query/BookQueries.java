package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Queries that read the catalogue. */
@RestController
@RequestMapping("/books")
public class BookQueries {

    private final CatalogueFlow catalogue;

    public BookQueries(CatalogueFlow catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<BookView> search(@RequestParam(required = false) String q,
            @RequestParam(required = false) String title, @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre) {
        return catalogue.search(q, title, author, genre).stream().map(BookView::from).toList();
    }

    @GetMapping("/{id}")
    public BookDetailView get(@PathVariable UUID id) {
        return BookDetailView.from(catalogue.findById(id));
    }
}
