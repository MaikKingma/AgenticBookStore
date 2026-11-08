package nl.openvalue.bookstore.data;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The stored shape of an order. Id, e-mail address, placed time and lines are fixed at creation; only the status and
 * the shipped time change afterwards. The version is managed by Hibernate for optimistic locking and never set by
 * code. Lines are lazy, so map to a DTO inside a transaction.
 */
@Entity
@Table(name = "customer_order")
public class OrderJPA {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_email", length = 254, nullable = false, updatable = false)
    private String customerEmail;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "position")
    private List<OrderLineJPA> lines = new ArrayList<>();

    protected OrderJPA() {
        // for Hibernate
    }

    OrderJPA(UUID id, String customerEmail, Instant placedAt, List<OrderLineJPA> lines) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.placedAt = placedAt;
        this.lines.addAll(lines);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public List<OrderLineJPA> getLines() {
        return List.copyOf(lines);
    }

    void setStatus(String status) {
        this.status = status;
    }

    void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }
}
