package nl.openvalue.bookstore.domain;

/** The title of a book: surrounding whitespace is stripped, then 1 to 200 characters remain. */
public record Title(String value) {

    private static final int MAX_LENGTH = 200;

    public Title {
        if (value == null || value.isBlank()) {
            throw new InvalidTitleException("A title needs at least one character");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidTitleException(
                    "A title has at most " + MAX_LENGTH + " characters, not " + value.length());
        }
    }
}
