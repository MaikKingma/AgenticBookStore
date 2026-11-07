package nl.openvalue.bookstore.domain;

import java.util.Objects;
import java.util.UUID;

/** The identity of a book in the catalogue. */
public record BookId(UUID value) {

    public BookId {
        Objects.requireNonNull(value, "A book id needs a value");
    }

    public static BookId newId() {
        return new BookId(UUID.randomUUID());
    }
}
