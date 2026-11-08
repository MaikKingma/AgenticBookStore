package nl.openvalue.distributor.catalog;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The in-memory catalogue. Immutable after it is built.
 */
public class TitleCatalog {

    private static final Comparator<Title> BY_TITLE_NAME_THEN_EAN =
            Comparator.comparing(Title::titleName, String.CASE_INSENSITIVE_ORDER).thenComparing(Title::ean);

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

    /**
     * Looks a title up by EAN. A {@code null} EAN is simply unknown.
     */
    public Optional<Title> findByEan(String ean) {
        if (ean == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(titlesByEan.get(ean));
    }

    public List<Subject> subjects() {
        return subjects;
    }

    /**
     * Searches the catalogue. {@code q} is a case-insensitive contains match on the title name or any contributor
     * name; {@code subjectPrefix} is a case-insensitive prefix match on any of the title's subject codes;
     * {@code availability} is an exact match. A blank {@code q} or {@code subjectPrefix} and a {@code null}
     * {@code availability} are ignored; the filters combine with AND. Results are sorted by title name
     * (case-insensitive), then EAN. {@code page} is zero-based.
     *
     * @throws IllegalArgumentException if {@code page} is negative or {@code size} is below 1
     */
    public TitlePage search(String q, String subjectPrefix, Availability availability, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative: " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1: " + size);
        }
        String needle = isBlank(q) ? null : q.toLowerCase(Locale.ROOT);
        String prefix = isBlank(subjectPrefix) ? null : subjectPrefix.toLowerCase(Locale.ROOT);

        List<Title> matches = titles.stream()
                .filter(title -> needle == null || matchesText(title, needle))
                .filter(title -> prefix == null || matchesSubject(title, prefix))
                .filter(title -> availability == null || title.availability() == availability)
                .sorted(BY_TITLE_NAME_THEN_EAN)
                .toList();

        long offset = (long) page * size;
        if (offset >= matches.size()) {
            return new TitlePage(List.of(), page, size, matches.size());
        }
        int from = (int) offset;
        int to = (int) Math.min(offset + size, matches.size());
        return new TitlePage(matches.subList(from, to), page, size, matches.size());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean matchesText(Title title, String needle) {
        return title.titleName().toLowerCase(Locale.ROOT).contains(needle)
                || title.contributors().stream()
                        .anyMatch(contributor -> contributor.name().toLowerCase(Locale.ROOT).contains(needle));
    }

    private static boolean matchesSubject(Title title, String prefix) {
        return title.subjectCodes().stream().anyMatch(code -> code.toLowerCase(Locale.ROOT).startsWith(prefix));
    }
}
