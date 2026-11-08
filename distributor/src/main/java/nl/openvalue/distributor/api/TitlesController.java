package nl.openvalue.distributor.api;

import java.util.List;
import nl.openvalue.distributor.catalog.Availability;
import nl.openvalue.distributor.catalog.Subject;
import nl.openvalue.distributor.catalog.Title;
import nl.openvalue.distributor.catalog.TitleCatalog;
import nl.openvalue.distributor.catalog.TitleNotFoundException;
import nl.openvalue.distributor.catalog.TitlePage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TitlesController {

    private static final int MAX_SIZE = 50;

    private final TitleCatalog catalog;

    public TitlesController(TitleCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/titles")
    public TitlePage search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Availability availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new InvalidRequestException("page must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("size must be between 1 and " + MAX_SIZE);
        }
        return catalog.search(q, subject, availability, page, size);
    }

    @GetMapping("/titles/{ean}")
    public Title title(@PathVariable String ean) {
        return catalog.findByEan(ean).orElseThrow(() -> new TitleNotFoundException(ean));
    }

    @GetMapping("/subjects")
    public List<Subject> subjects() {
        return catalog.subjects();
    }
}
