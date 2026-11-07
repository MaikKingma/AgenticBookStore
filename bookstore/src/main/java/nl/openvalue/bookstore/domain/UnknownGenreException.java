package nl.openvalue.bookstore.domain;

/** Thrown when a value does not name one of the genres. {@link #value()} is the value as it was given. */
public class UnknownGenreException extends RuntimeException {

    private final String value;

    public UnknownGenreException(String value) {
        super(value == null || value.isBlank() ? "No genre given" : "Unknown genre '" + value + "'");
        this.value = value;
    }

    public String value() {
        return value;
    }
}
