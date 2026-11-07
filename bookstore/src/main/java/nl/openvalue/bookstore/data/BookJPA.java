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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The stored shape of a book. Id and ISBN are fixed at creation; the version is managed by Hibernate for optimistic
 * locking and never set by code. Authors are lazy, so map to a DTO inside a transaction.
 */
@Entity
@Table(name = "book")
public class BookJPA {

    @Id
    private UUID id;

    @Column(name = "isbn", length = 13, unique = true, nullable = false, updatable = false)
    private String isbn;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @ElementCollection
    @CollectionTable(name = "book_author", joinColumns = @JoinColumn(name = "book_id"))
    @OrderColumn(name = "position")
    @Column(name = "name", length = 120, nullable = false)
    private List<String> authors = new ArrayList<>();

    @Column(name = "genre", length = 32, nullable = false)
    private String genre;

    @Column(name = "price_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3, nullable = false)
    private String priceCurrency;

    @Column(name = "publication_year", nullable = false)
    private Integer publicationYear;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected BookJPA() {
        // for Hibernate
    }

    BookJPA(UUID id, String isbn) {
        this.id = id;
        this.isbn = isbn;
    }

    public UUID getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return List.copyOf(authors);
    }

    public String getGenre() {
        return genre;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public String getStatus() {
        return status;
    }

    void setTitle(String title) {
        this.title = title;
    }

    /** Replaces the authors in place, so Hibernate keeps tracking the same collection. */
    void replaceAuthors(List<String> newAuthors) {
        authors.clear();
        authors.addAll(newAuthors);
    }

    void setGenre(String genre) {
        this.genre = genre;
    }

    void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    void setStatus(String status) {
        this.status = status;
    }
}
