package nl.openvalue.bookstore.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.DatabaseCleaner;
import nl.openvalue.bookstore.PostgresTestConfig;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JPA slice test of the stock item data adapter on PostgreSQL. Every test runs in its own transaction that is rolled
 * back, and starts by emptying the application tables inside that transaction. A stock item needs its book row, so
 * each test seeds the books it uses through JDBC.
 * <p>
 * Ordering assertions use fixed UUIDs whose first hex digit is 0 to 7: PostgreSQL orders {@code uuid} as unsigned
 * bytes while {@code UUID.compareTo} is signed, and the two agree only in that range.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresTestConfig.class, StockItemDataServiceImpl.class})
class StockItemDataServiceImplTest {

    @Autowired
    private StockItemDataService stockItemDataService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    private int seededBooks;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
    }

    @Test
    void savesANewStockItemAndFindsItByBookId() {
        StockItemDTO stockItem = new StockItemDTO(UUID.randomUUID(), 9, 2, 4, 6, true);

        saveStock(stockItem);
        entityManager.clear();

        assertThat(stockItemDataService.findByBookId(stockItem.bookId())).isEqualTo(stockItem);
    }

    @Test
    void storesEveryColumn() {
        UUID bookId = UUID.randomUUID();

        saveStock(new StockItemDTO(bookId, 11, 5, 7, 13, true));

        Map<String, Object> row = storedRow(bookId);
        assertThat(row.get("book_id")).isEqualTo(bookId);
        assertThat(row.get("on_hand")).isEqualTo(11);
        assertThat(row.get("reserved")).isEqualTo(5);
        assertThat(row.get("reorder_level")).isEqualTo(7);
        assertThat(row.get("reorder_quantity")).isEqualTo(13);
        assertThat(row.get("low_stock_signalled")).isEqualTo(true);
        assertThat(row.get("version")).isEqualTo(0L);
    }

    @Test
    void updatesAnExistingStockItemAndBumpsTheVersion() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, false));
        entityManager.clear();
        assertThat(storedVersion(bookId)).isZero();

        stockItemDataService.save(new StockItemDTO(bookId, 15, 3, 8, 12, true));

        Map<String, Object> row = storedRow(bookId);
        assertThat(row.get("on_hand")).isEqualTo(15);
        assertThat(row.get("reserved")).isEqualTo(3);
        assertThat(row.get("reorder_level")).isEqualTo(8);
        assertThat(row.get("reorder_quantity")).isEqualTo(12);
        assertThat(row.get("low_stock_signalled")).isEqualTo(true);
        assertThat(row.get("version")).isEqualTo(1L);
    }

    @Test
    void savingAnUnchangedStockItemKeepsItsVersion() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, true));
        entityManager.clear();

        stockItemDataService.save(stockItemDataService.findByBookId(bookId));

        assertThat(storedVersion(bookId)).isZero();
    }

    @Test
    void rejectsAStaleUpdate() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, false));
        entityManager.clear();
        StockItemDTO loaded = stockItemDataService.findByBookId(bookId);
        // Another transaction updates the stock item after this one loaded it.
        jdbcTemplate.update("update stock_item set version = version + 1 where book_id = ?", bookId);
        StockItemDTO changed = new StockItemDTO(loaded.bookId(), loaded.onHand() + 5, loaded.reserved(),
                loaded.reorderLevel(), loaded.reorderQuantity(), loaded.lowStockSignalled());

        assertThatThrownBy(() -> stockItemDataService.save(changed))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void findByBookIdThrowsStockItemNotFoundForAnUnknownBook() {
        saveStock(new StockItemDTO(UUID.randomUUID(), 9, 2, 4, 6, true));
        UUID unknownBookId = UUID.randomUUID();

        assertThatThrownBy(() -> stockItemDataService.findByBookId(unknownBookId))
                .isInstanceOf(StockItemDataService.StockItemNotFoundException.class)
                .hasMessageContaining(unknownBookId.toString());
    }

    @Test
    void savingAStockItemForAnUnknownBookViolatesTheForeignKey() {
        StockItemDTO withoutBook = new StockItemDTO(UUID.randomUUID(), 9, 2, 4, 6, false);

        // The failed insert leaves the transaction unusable, so nothing touches the database after it.
        assertThatThrownBy(() -> stockItemDataService.save(withoutBook))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_stock_item_book");
    }

    @Test
    void findLowStockIncludesItemsAtOrBelowTheReorderLevelOnly() {
        // Default level 3 and level 5: one row each at available = level - 1, = level and = level + 1.
        StockItemDTO belowDefaultLevel = new StockItemDTO(id("31"), 2, 0, 3, 10, false);
        StockItemDTO atDefaultLevel = new StockItemDTO(id("32"), 4, 1, 3, 10, true);
        StockItemDTO aboveDefaultLevel = new StockItemDTO(id("33"), 4, 0, 3, 10, false);
        StockItemDTO belowLevel5 = new StockItemDTO(id("51"), 4, 0, 5, 7, false);
        StockItemDTO atLevel5 = new StockItemDTO(id("52"), 7, 2, 5, 7, true);
        StockItemDTO aboveLevel5 = new StockItemDTO(id("53"), 6, 0, 5, 7, false);
        // On hand above the level, available below it. It ties with the 31 row at available 2 and sorts first by id,
        // although its on hand and reserved are the higher ones.
        StockItemDTO onHandAboveLevel = new StockItemDTO(id("30"), 5, 3, 3, 10, false);
        for (StockItemDTO stockItem : List.of(belowDefaultLevel, atDefaultLevel, aboveDefaultLevel, belowLevel5,
                atLevel5, aboveLevel5, onHandAboveLevel)) {
            saveStock(stockItem);
        }
        entityManager.clear();

        assertThat(stockItemDataService.findLowStock())
                .containsExactly(onHandAboveLevel, belowDefaultLevel, atDefaultLevel, belowLevel5, atLevel5);
    }

    @Test
    void findLowStockReturnsAnEmptyListWhenNothingIsLow() {
        saveStock(new StockItemDTO(UUID.randomUUID(), 4, 0, 3, 10, false));
        saveStock(new StockItemDTO(UUID.randomUUID(), 8, 2, 5, 7, true));
        // Available 2 is above level 0, though below the default level 3.
        saveStock(new StockItemDTO(UUID.randomUUID(), 2, 0, 0, 1, false));
        entityManager.clear();

        assertThat(stockItemDataService.findLowStock()).isEmpty();
    }

    @Test
    void findLowStockOrdersByAvailableThenBookId() {
        UUID mostUrgent = id("70");
        UUID secondMostUrgent = id("60");
        UUID tieWithLowerId = id("10");
        UUID tieWithHigherId = id("20");
        // Available order (1, 2, 3, 3) differs from the on-hand order (2, 3, 5, 6), from the book-id order and from
        // the insertion order. The tie row with the higher id goes in first and has the lower on hand and reserved.
        saveStock(new StockItemDTO(tieWithHigherId, 3, 0, 3, 10, false));
        saveStock(new StockItemDTO(mostUrgent, 5, 4, 3, 10, false));
        saveStock(new StockItemDTO(tieWithLowerId, 6, 3, 3, 10, false));
        saveStock(new StockItemDTO(secondMostUrgent, 2, 0, 3, 10, false));
        entityManager.clear();

        assertThat(stockItemDataService.findLowStock()).extracting(StockItemDTO::bookId)
                .containsExactly(mostUrgent, secondMostUrgent, tieWithLowerId, tieWithHigherId);
    }

    /** A fixed book id starting with the given hex digits; keep the first digit 0 to 7 (see the class comment). */
    private static UUID id(String leadingHexDigits) {
        return UUID.fromString(leadingHexDigits + "000000-0000-0000-0000-000000000001");
    }

    /** Reads the stored row by JDBC, which never flushes Hibernate, so it shows what the save itself wrote. */
    private Map<String, Object> storedRow(UUID bookId) {
        return jdbcTemplate.queryForMap("select book_id, on_hand, reserved, reorder_level, reorder_quantity,"
                + " low_stock_signalled, version from stock_item where book_id = ?", bookId);
    }

    private long storedVersion(UUID bookId) {
        return jdbcTemplate.queryForObject("select version from stock_item where book_id = ?", Long.class, bookId);
    }

    /** Seeds the book row the foreign key needs, then saves the stock item through the port. */
    private void saveStock(StockItemDTO stockItem) {
        seedBook(stockItem.bookId());
        stockItemDataService.save(stockItem);
    }

    /** Inserts a book row through JDBC, with a synthetic ISBN of 13 characters that is unique within the test. */
    private void seedBook(UUID bookId) {
        jdbcTemplate.update("insert into book (id, isbn, title, genre, price_amount, price_currency,"
                        + " publication_year, status, version) values (?, ?, ?, 'FICTION', 12.50, 'EUR', 1813,"
                        + " 'AVAILABLE', 0)",
                bookId, String.format("979%010d", ++seededBooks), "Stocked Book " + seededBooks);
    }
}
