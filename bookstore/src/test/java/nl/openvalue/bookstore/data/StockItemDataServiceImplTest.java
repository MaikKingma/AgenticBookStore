package nl.openvalue.bookstore.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.DatabaseCleaner;
import nl.openvalue.bookstore.PostgresTestConfig;
import nl.openvalue.bookstore.domaininteraction.PendingRestockDTO;
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

    // Two pending restocks that differ in every field; no quantity equals a figure of its stock item.
    private static final PendingRestockDTO PENDING =
            new PendingRestockDTO("R-7781", 12, LocalDate.of(2026, 7, 3), true);
    private static final PendingRestockDTO OTHER_PENDING =
            new PendingRestockDTO("DIST-RO-0042", 5, LocalDate.of(2027, 11, 30), false);

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

    @Test
    void savesANewStockItemWithAPendingRestockAndFindsItByBookId() {
        StockItemDTO stockItem = new StockItemDTO(UUID.randomUUID(), 9, 2, 4, 6, true, PENDING);

        saveStock(stockItem);
        entityManager.clear();

        assertThat(stockItemDataService.findByBookId(stockItem.bookId())).isEqualTo(stockItem);
    }

    @Test
    void storesEveryPendingRestockColumnOnInsert() {
        UUID bookId = UUID.randomUUID();

        saveStock(new StockItemDTO(bookId, 11, 5, 7, 13, true, PENDING));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", "R-7781"),
                entry("pending_restock_quantity", 12),
                entry("pending_restock_expected_delivery_date", LocalDate.of(2026, 7, 3)),
                entry("pending_restock_backordered", true));
    }

    @Test
    void storesNullInEveryPendingRestockColumnOnInsertWithoutAPendingRestock() {
        UUID bookId = UUID.randomUUID();

        saveStock(new StockItemDTO(bookId, 11, 5, 7, 13, true));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", null),
                entry("pending_restock_quantity", null),
                entry("pending_restock_expected_delivery_date", null),
                entry("pending_restock_backordered", null));
        entityManager.clear();
        assertThat(stockItemDataService.findByBookId(bookId).pendingRestock()).isNull();
    }

    @Test
    void anUpdateStoresANewPendingRestock() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, false));
        entityManager.clear();

        stockItemDataService.save(new StockItemDTO(bookId, 9, 2, 4, 6, false, OTHER_PENDING));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", "DIST-RO-0042"),
                entry("pending_restock_quantity", 5),
                entry("pending_restock_expected_delivery_date", LocalDate.of(2027, 11, 30)),
                entry("pending_restock_backordered", false));
        assertThat(storedVersion(bookId)).isEqualTo(1L);
        entityManager.clear();
        assertThat(stockItemDataService.findByBookId(bookId))
                .isEqualTo(new StockItemDTO(bookId, 9, 2, 4, 6, false, OTHER_PENDING));
    }

    @Test
    void anUpdateWithoutAPendingRestockClearsEveryPendingRestockColumn() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, true, PENDING));
        entityManager.clear();

        stockItemDataService.save(new StockItemDTO(bookId, 10, 2, 4, 6, true));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", null),
                entry("pending_restock_quantity", null),
                entry("pending_restock_expected_delivery_date", null),
                entry("pending_restock_backordered", null));
        entityManager.clear();
        assertThat(stockItemDataService.findByBookId(bookId))
                .isEqualTo(new StockItemDTO(bookId, 10, 2, 4, 6, true, null));
    }

    @Test
    void anUpdateReplacesOnePendingRestockByAnother() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, true, PENDING));
        entityManager.clear();

        stockItemDataService.save(new StockItemDTO(bookId, 9, 2, 4, 6, true, OTHER_PENDING));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", "DIST-RO-0042"),
                entry("pending_restock_quantity", 5),
                entry("pending_restock_expected_delivery_date", LocalDate.of(2027, 11, 30)),
                entry("pending_restock_backordered", false));
        entityManager.clear();
        assertThat(stockItemDataService.findByBookId(bookId).pendingRestock()).isEqualTo(OTHER_PENDING);
    }

    @Test
    void anUpdateReplacesAPendingRestockThatIsNotBackorderedByOneThatIs() {
        UUID bookId = UUID.randomUUID();
        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, true, OTHER_PENDING));
        entityManager.clear();

        stockItemDataService.save(new StockItemDTO(bookId, 9, 2, 4, 6, true, PENDING));

        assertThat(storedPendingRestock(bookId)).containsExactly(
                entry("pending_restock_order_id", "R-7781"),
                entry("pending_restock_quantity", 12),
                entry("pending_restock_expected_delivery_date", LocalDate.of(2026, 7, 3)),
                entry("pending_restock_backordered", true));
        entityManager.clear();
        assertThat(stockItemDataService.findByBookId(bookId).pendingRestock()).isEqualTo(PENDING);
    }

    @Test
    void findLowStockCarriesThePendingRestockOfEachItem() {
        // All three items are low: the first and the last in the result have a pending restock, the middle one none.
        StockItemDTO lowWithPending = new StockItemDTO(id("41"), 3, 2, 3, 10, true, OTHER_PENDING);
        StockItemDTO lowWithoutPending = new StockItemDTO(id("42"), 2, 0, 3, 10, true);
        StockItemDTO lowWithOtherPending = new StockItemDTO(id("43"), 4, 1, 4, 7, true, PENDING);
        saveStock(lowWithoutPending);
        saveStock(lowWithPending);
        saveStock(lowWithOtherPending);
        entityManager.clear();

        assertThat(stockItemDataService.findLowStock())
                .containsExactly(lowWithPending, lowWithoutPending, lowWithOtherPending);
    }

    @Test
    void storesAPendingRestockOrderIdOf64Characters() {
        UUID bookId = UUID.randomUUID();
        PendingRestockDTO longestId = new PendingRestockDTO("R".repeat(63) + "9", 12, LocalDate.of(2026, 7, 3), true);

        saveStock(new StockItemDTO(bookId, 9, 2, 4, 6, true, longestId));
        entityManager.clear();

        assertThat(stockItemDataService.findByBookId(bookId).pendingRestock()).isEqualTo(longestId);
    }

    @Test
    void rejectsAPendingRestockOrderIdOf65Characters() {
        // The 65th character is no space: PostgreSQL silently cuts trailing spaces that do not fit the column.
        UUID bookId = UUID.randomUUID();
        seedBook(bookId);
        StockItemDTO tooLong = new StockItemDTO(bookId, 9, 2, 4, 6, true,
                new PendingRestockDTO("R".repeat(64) + "9", 12, LocalDate.of(2026, 7, 3), true));

        // The failed insert leaves the transaction unusable, so nothing touches the database after it.
        assertThatThrownBy(() -> stockItemDataService.save(tooLong))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(64)");
    }

    @Test
    void theStockItemTableHasExactlyTheFourNullablePendingRestockColumnsWithoutDefaults() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                select column_name, data_type, character_maximum_length, is_nullable, column_default
                from information_schema.columns
                where table_schema = current_schema() and table_name = 'stock_item'
                  and column_name like 'pending%'
                order by column_name""");

        assertThat(columns).containsExactly(
                column("pending_restock_backordered", "boolean", null),
                column("pending_restock_expected_delivery_date", "date", null),
                column("pending_restock_order_id", "character varying", 64),
                column("pending_restock_quantity", "integer", null));
    }

    @Test
    void theStockItemTableHasNoOtherColumnsThanTheStoredFields() {
        List<String> columns = jdbcTemplate.queryForList("""
                select column_name from information_schema.columns
                where table_schema = current_schema() and table_name = 'stock_item'
                order by ordinal_position""", String.class);

        assertThat(columns).containsExactly("book_id", "on_hand", "reserved", "reorder_level", "reorder_quantity",
                "low_stock_signalled", "version", "pending_restock_order_id", "pending_restock_quantity",
                "pending_restock_expected_delivery_date", "pending_restock_backordered");
    }

    @Test
    void thePendingRestockColumnsAddNoConstraint() {
        // PostgreSQL 18 lists NOT NULL as constraints of type 'n'; every other constraint is the one V2 created.
        List<String> constraints = jdbcTemplate.queryForList("""
                select conname from pg_constraint
                where conrelid = 'stock_item'::regclass and contype <> 'n'
                order by conname""", String.class);

        assertThat(constraints).containsExactly("fk_stock_item_book", "pk_stock_item");
    }

    /** One row of {@code information_schema.columns} for a nullable column without a default. */
    private static Map<String, Object> column(String name, String dataType, Integer maximumLength) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("column_name", name);
        column.put("data_type", dataType);
        column.put("character_maximum_length", maximumLength);
        column.put("is_nullable", "YES");
        column.put("column_default", null);
        return column;
    }

    /**
     * Reads the four pending restock columns by JDBC, in column order, with the date as a {@link LocalDate}. JDBC never
     * flushes Hibernate, so it shows what the save itself wrote.
     */
    private Map<String, Object> storedPendingRestock(UUID bookId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("select pending_restock_order_id, pending_restock_quantity,"
                + " pending_restock_expected_delivery_date, pending_restock_backordered from stock_item"
                + " where book_id = ?", bookId);
        Object date = row.get("pending_restock_expected_delivery_date");
        row.put("pending_restock_expected_delivery_date", date == null ? null : ((Date) date).toLocalDate());
        return row;
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
