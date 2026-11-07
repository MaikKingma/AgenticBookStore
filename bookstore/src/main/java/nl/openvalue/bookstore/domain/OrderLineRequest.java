package nl.openvalue.bookstore.domain;

import java.util.Objects;

/**
 * What one line of an order needs: the book and snapshots of its isbn, title and current price, plus the quantity. The
 * snapshots are copies of values the catalogue already validated; the order rules are checked by {@link Order}.
 */
public record OrderLineRequest(BookId bookId, String isbn, String title, Money unitPrice, Quantity quantity) {

    public OrderLineRequest {
        Objects.requireNonNull(bookId, "An order line needs a book id");
        Objects.requireNonNull(isbn, "An order line needs an isbn");
        Objects.requireNonNull(title, "An order line needs a title");
        Objects.requireNonNull(unitPrice, "An order line needs a unit price");
        Objects.requireNonNull(quantity, "An order line needs a quantity");
    }
}
