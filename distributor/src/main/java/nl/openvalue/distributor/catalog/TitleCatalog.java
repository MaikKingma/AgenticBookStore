package nl.openvalue.distributor.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The in-memory catalogue. Immutable after it is built.
 */
public class TitleCatalog {

    private final List<Title> titles;
    private final Map<String, Title> titlesByEan;
    private final List<Subject> subjects;

    public TitleCatalog(List<Title> titles, List<Subject> subjects) {
        this.titles = List.copyOf(titles);
        Map<String, Title> byEan = new HashMap<>();
        this.titles.forEach(title -> byEan.putIfAbsent(title.ean(), title));
        this.titlesByEan = Map.copyOf(byEan);
        this.subjects = List.copyOf(subjects);
    }

    public List<Title> all() {
        return titles;
    }

    public Optional<Title> findByEan(String ean) {
        return Optional.ofNullable(titlesByEan.get(ean));
    }

    public List<Subject> subjects() {
        return subjects;
    }
}
