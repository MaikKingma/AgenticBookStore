package nl.openvalue.bookstore.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * The stored shape of a stock item. The book id is assigned, not generated, and fixed at creation; it is also the
 * foreign key to the book, but no relationship is mapped. The version is managed by Hibernate for optimistic locking
 * and never set by code.
 */
@Entity
@Table(name = "stock_item")
public class StockItemJPA {

    @Id
    @Column(name = "book_id", nullable = false, updatable = false)
    private UUID bookId;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "reorder_quantity", nullable = false)
    private int reorderQuantity;

    @Column(name = "low_stock_signalled", nullable = false)
    private boolean lowStockSignalled;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StockItemJPA() {
        // for Hibernate
    }

    StockItemJPA(UUID bookId) {
        this.bookId = bookId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public int getReorderQuantity() {
        return reorderQuantity;
    }

    public boolean isLowStockSignalled() {
        return lowStockSignalled;
    }

    void setOnHand(int onHand) {
        this.onHand = onHand;
    }

    void setReserved(int reserved) {
        this.reserved = reserved;
    }

    void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    void setReorderQuantity(int reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    void setLowStockSignalled(boolean lowStockSignalled) {
        this.lowStockSignalled = lowStockSignalled;
    }
}
