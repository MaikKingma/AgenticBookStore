package nl.openvalue.bookstore.data;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * The stored shape of one order line: the book id and the snapshots of the book's isbn, title and price taken when the
 * order was placed. A line belongs to its order and is written once, with the order. The book id is also the foreign
 * key to the book, but no relationship is mapped.
 */
@Embeddable
public class OrderLineJPA {

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "isbn", length = 13, nullable = false)
    private String isbn;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "unit_price_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPriceAmount;

    @Column(name = "unit_price_currency", length = 3, nullable = false)
    private String unitPriceCurrency;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected OrderLineJPA() {
        // for Hibernate
    }

    OrderLineJPA(UUID bookId, String isbn, String title, BigDecimal unitPriceAmount, String unitPriceCurrency,
            int quantity) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.unitPriceAmount = unitPriceAmount;
        this.unitPriceCurrency = unitPriceCurrency;
        this.quantity = quantity;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public String getUnitPriceCurrency() {
        return unitPriceCurrency;
    }

    public int getQuantity() {
        return quantity;
    }
}
