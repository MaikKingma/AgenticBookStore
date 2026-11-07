package nl.openvalue.bookstore.domain;

import java.util.Objects;

/**
 * One line of an {@link Order}: a book with snapshots of its isbn, title and unit price at the time of ordering, and 1
 * to 10 copies. Lines exist only inside an order.
 */
public final class OrderLine {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 10;

    private final BookId bookId;
    private final String isbn;
    private final String title;
    private final Money unitPrice;
    private final Quantity quantity;

    OrderLine(OrderLineRequest request) {
        Objects.requireNonNull(request, "An order line needs a request");
        this.bookId = request.bookId();
        this.isbn = request.isbn();
        this.title = request.title();
        this.unitPrice = request.unitPrice();
        this.quantity = quantityOf(request.quantity().value());
    }

    /**
     * The one home of the rule that an order line holds 1 to 10 copies: returns the quantity, or throws
     * {@link OrderedQuantityOutOfRangeException} for any other number, negative numbers included.
     */
    public static Quantity quantityOf(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new OrderedQuantityOutOfRangeException(quantity, MIN_QUANTITY, MAX_QUANTITY);
        }
        return new Quantity(quantity);
    }

    public BookId bookId() {
        return bookId;
    }

    public String isbn() {
        return isbn;
    }

    public String title() {
        return title;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Quantity quantity() {
        return quantity;
    }

    /** The unit price times the quantity. */
    public Money lineTotal() {
        return unitPrice.times(quantity.value());
    }
}
