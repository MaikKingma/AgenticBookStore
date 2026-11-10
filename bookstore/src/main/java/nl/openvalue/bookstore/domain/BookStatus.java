package nl.openvalue.bookstore.domain;

/** Whether a book can still be sold. Discontinued books stay in the catalogue. */
public enum BookStatus {
    AVAILABLE,
    DISCONTINUED
}
