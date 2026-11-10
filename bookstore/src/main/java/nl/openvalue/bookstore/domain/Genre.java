package nl.openvalue.bookstore.domain;

import java.util.Arrays;
import java.util.Locale;

/** The genre of a book. The declaration order is the order in which genres are listed. */
public enum Genre {
    FICTION,
    SCIENCE_FICTION,
    FANTASY,
    MYSTERY,
    ROMANCE,
    HISTORY,
    SCIENCE,
    TECHNOLOGY,
    CHILDREN,
    POETRY;

    /**
     * Parses a genre in any case, with {@code -} or {@code _} between words and surrounding whitespace ignored, so
     * {@code " science-fiction "} is {@link #SCIENCE_FICTION}.
     */
    public static Genre parse(String value) {
        if (value == null || value.isBlank()) {
            throw new UnknownGenreException(value);
        }
        String name = value.strip().replace('-', '_').toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(genre -> genre.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new UnknownGenreException(value));
    }
}
