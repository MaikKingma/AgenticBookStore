package nl.openvalue.bookstore.domaininteraction;

import java.util.List;
import java.util.Map;

/**
 * The request clashes with the current state of the system. The message is the detail; the code is a stable
 * kebab-case identifier and the title a short label.
 */
public class ConflictException extends RuntimeException {

    private final String code;
    private final String title;
    private final List<Map<String, String>> details;

    public ConflictException(String code, String title, String detail) {
        this(code, title, detail, List.of());
    }

    public ConflictException(String code, String title, String detail, List<Map<String, String>> details) {
        super(detail);
        this.code = code;
        this.title = title;
        this.details = List.copyOf(details);
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    /** An unmodifiable list of key and value maps, empty when there is nothing to list. */
    public List<Map<String, String>> details() {
        return details;
    }
}
