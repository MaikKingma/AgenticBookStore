package nl.openvalue.bookstore.domain;

/** The name of one author: surrounding whitespace is stripped, then 1 to 120 characters remain. */
public record AuthorName(String value) {

    private static final int MAX_LENGTH = 120;

    public AuthorName {
        if (value == null || value.isBlank()) {
            throw new InvalidAuthorNameException("An author name needs at least one character");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidAuthorNameException(
                    "An author name has at most " + MAX_LENGTH + " characters, not " + value.length());
        }
    }
}
