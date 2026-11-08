package nl.openvalue.bookstore.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import nl.openvalue.bookstore.DatabaseCleaner;
import nl.openvalue.bookstore.PostgresTestConfig;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;
import nl.openvalue.bookstore.domaininteraction.OrderDataService;
import nl.openvalue.bookstore.domaininteraction.OrderLineDTO;
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
 * JPA slice test of the order data adapter on PostgreSQL. Every test runs in its own transaction that is rolled back,
 * and starts by emptying the application tables inside that transaction. Order lines reference books, so each test
 * seeds the books it uses through JDBC.
 * <p>
 * Ordering assertions use fixed UUIDs whose first hex digit is 0 to 7: PostgreSQL orders {@code uuid} as unsigned
 * bytes while {@code UUID.compareTo} is signed, and the two agree only in that range. Stored values are read back by
 * JDBC, which never flushes Hibernate, so a read shows what the save itself wrote.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresTestConfig.class, OrderDataServiceImpl.class})
class OrderDataServiceImplTest {

    private static final UUID BOOK_A = UUID.fromString("10000000-0000-0000-0000-0000000000b1");
    private static final UUID BOOK_B = UUID.fromString("30000000-0000-0000-0000-0000000000b1");
    private static final UUID BOOK_C = UUID.fromString("50000000-0000-0000-0000-0000000000b1");
    private static final UUID BOOK_D = UUID.fromString("20000000-0000-0000-0000-0000000000b1");
    private static final UUID BOOK_E = UUID.fromString("40000000-0000-0000-0000-0000000000b1");
    private static final UUID BOOK_F = UUID.fromString("60000000-0000-0000-0000-0000000000b1");
    /** U+2003, a space that {@code trim()} keeps; the adapter stores strings exactly as given. */
    private static final String EM_SPACE = Character.toString(0x2003);

    @Autowired
    private OrderDataService orderDataService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    private final Set<UUID> seededBooks = new HashSet<>();

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
    }

    @Test
    void savesANewOrderAndFindsItWithItsLinesInOrder() {
        // Lines for books C, A, B: sorted in no column in either direction.
        OrderDTO order = new OrderDTO(orderId("31"), "reader@example.com", "PLACED",
                Instant.parse("2026-06-15T10:00:00.123456Z"), null, List.of(
                        line(BOOK_C, "9780000000002", "Middlemarch", "12.50", "EUR", 2),
                        line(BOOK_A, "9780000000003", "Persuasion", "8.00", "GBP", 1),
                        line(BOOK_B, "9780000000001", "Beloved", "19.99", "EUR", 10)));
        saveOrder(order);
        entityManager.clear();

        OrderDTO reloaded = orderDataService.findById(orderId("31"));

        assertThat(reloaded.lines()).extracting(OrderLineDTO::bookId).containsExactly(BOOK_C, BOOK_A, BOOK_B);
        assertThat(reloaded).isEqualTo(order);
        assertThat(reloaded.id()).isEqualTo(UUID.fromString("31000000-0000-0000-0000-0000000000a1"));
        assertThat(reloaded.customerEmail()).isEqualTo("reader@example.com");
        assertThat(reloaded.status()).isEqualTo("PLACED");
        assertThat(reloaded.placedAt()).isEqualTo(Instant.parse("2026-06-15T10:00:00.123456Z"));
        assertThat(reloaded.shippedAt()).isNull();
    }

    @Test
    void storesEveryColumn() {
        saveOrder(new OrderDTO(orderId("62"), "shipped.reader@example.org", "SHIPPED",
                Instant.parse("2026-05-01T08:30:15.654321Z"), Instant.parse("2026-05-02T17:45:59.000001Z"), List.of(
                        line(BOOK_D, "9781111111117", "Dubliners", "23.45", "USD", 3),
                        line(BOOK_F, "9782222222228", "Ulysses", "5.10", "EUR", 7),
                        line(BOOK_E, "9783333333339", EM_SPACE + "Orlando", "499.99", "USD", 4))));

        Map<String, Object> order = storedOrder(orderId("62"));
        assertThat(order.get("id")).isEqualTo(UUID.fromString("62000000-0000-0000-0000-0000000000a1"));
        assertThat(order.get("customer_email")).isEqualTo("shipped.reader@example.org");
        assertThat(order.get("status")).isEqualTo("SHIPPED");
        assertThat(instant(order.get("placed_at"))).isEqualTo(Instant.parse("2026-05-01T08:30:15.654321Z"));
        assertThat(instant(order.get("shipped_at"))).isEqualTo(Instant.parse("2026-05-02T17:45:59.000001Z"));
        assertThat(order.get("version")).isEqualTo(0L);
        UUID storedId = UUID.fromString("62000000-0000-0000-0000-0000000000a1");
        assertThat(storedLines(orderId("62"))).containsExactly(
                lineRow(storedId, 0, BOOK_D, "9781111111117", "Dubliners", "23.45", "USD", 3),
                lineRow(storedId, 1, BOOK_F, "9782222222228", "Ulysses", "5.10", "EUR", 7),
                lineRow(storedId, 2, BOOK_E, "9783333333339", EM_SPACE + "Orlando", "499.99", "USD", 4));
    }

    @Test
    void shippingUpdatesStatusAndShippedAtAndBumpsTheVersion() {
        List<OrderLineDTO> lines = List.of(
                line(BOOK_B, "9780000000027", "Emma", "7.25", "EUR", 5),
                line(BOOK_A, "9780000000034", "Sense and Sensibility", "31.00", "EUR", 6));
        saveOrder(new OrderDTO(orderId("43"), "second.reader@example.net", "PLACED",
                Instant.parse("2026-06-01T12:00:00.000500Z"), null, lines));
        entityManager.clear();
        List<Map<String, Object>> linesBefore = storedLines(orderId("43"));

        orderDataService.save(new OrderDTO(orderId("43"), "second.reader@example.net", "SHIPPED",
                Instant.parse("2026-06-01T12:00:00.000500Z"), Instant.parse("2026-06-03T09:15:30.250001Z"), lines));

        Map<String, Object> order = storedOrder(orderId("43"));
        assertThat(order.get("status")).isEqualTo("SHIPPED");
        assertThat(instant(order.get("shipped_at"))).isEqualTo(Instant.parse("2026-06-03T09:15:30.250001Z"));
        assertThat(order.get("version")).isEqualTo(1L);
        assertThat(storedLines(orderId("43"))).isEqualTo(linesBefore).hasSize(2);
    }

    @Test
    void savingAnUnchangedOrderKeepsItsVersion() {
        saveOrder(new OrderDTO(orderId("24"), "placed.reader@example.com", "PLACED",
                Instant.parse("2026-06-02T14:20:00.000020Z"), null, List.of(
                        line(BOOK_C, "9780000000041", "Jane Eyre", "11.11", "EUR", 1),
                        line(BOOK_D, "9780000000058", "Villette", "22.22", "EUR", 2))));
        saveOrder(new OrderDTO(orderId("35"), "shipped.reader@example.com", "SHIPPED",
                Instant.parse("2026-06-02T15:00:00.000030Z"), Instant.parse("2026-06-04T16:00:00.000040Z"), List.of(
                        line(BOOK_E, "9780000000065", "Shirley", "33.33", "USD", 3),
                        line(BOOK_F, "9780000000072", "Agnes Grey", "44.44", "USD", 4))));
        entityManager.clear();

        orderDataService.save(orderDataService.findById(orderId("24")));
        orderDataService.save(orderDataService.findById(orderId("35")));

        assertThat(storedVersion(orderId("24"))).isZero();
        assertThat(storedVersion(orderId("35"))).isZero();
    }

    @Test
    void updateCopiesOnlyStatusAndShippedAt() {
        seedBook(BOOK_F);
        saveOrder(new OrderDTO(orderId("54"), "first.reader@example.com", "PLACED",
                Instant.parse("2026-04-20T07:05:00.000007Z"), null, List.of(
                        line(BOOK_E, "9784444444440", "Rebecca", "14.00", "EUR", 2))));
        entityManager.clear();

        // Same id; every other value differs, the lines in book id, isbn, title, price, currency and quantity.
        orderDataService.save(new OrderDTO(orderId("54"), "changed.reader@example.com", "SHIPPED",
                Instant.parse("2026-04-21T00:00:00Z"), Instant.parse("2026-04-22T11:11:11.111111Z"), List.of(
                        line(BOOK_F, "9785555555551", "Jamaica Inn", "99.99", "USD", 9))));

        Map<String, Object> order = storedOrder(orderId("54"));
        assertThat(order.get("customer_email")).isEqualTo("first.reader@example.com");
        assertThat(instant(order.get("placed_at"))).isEqualTo(Instant.parse("2026-04-20T07:05:00.000007Z"));
        assertThat(order.get("status")).isEqualTo("SHIPPED");
        assertThat(instant(order.get("shipped_at"))).isEqualTo(Instant.parse("2026-04-22T11:11:11.111111Z"));
        UUID storedId = UUID.fromString("54000000-0000-0000-0000-0000000000a1");
        assertThat(storedLines(orderId("54"))).containsExactly(
                lineRow(storedId, 0, BOOK_E, "9784444444440", "Rebecca", "14.00", "EUR", 2));
    }

    @Test
    void rejectsAStaleUpdate() {
        saveOrder(new OrderDTO(orderId("15"), "stale.reader@example.com", "PLACED",
                Instant.parse("2026-06-05T06:00:00.000600Z"), null, List.of(
                        line(BOOK_A, "9780000000089", "Cranford", "9.50", "EUR", 3))));
        entityManager.clear();
        OrderDTO loaded = orderDataService.findById(orderId("15"));
        // Another transaction updates the order after this one loaded it.
        jdbcTemplate.update("update customer_order set version = version + 1 where id = ?", orderId("15"));
        OrderDTO shipped = new OrderDTO(loaded.id(), loaded.customerEmail(), "SHIPPED", loaded.placedAt(),
                Instant.parse("2026-06-06T06:00:00.000600Z"), loaded.lines());

        assertThatThrownBy(() -> orderDataService.save(shipped))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void findByIdThrowsOrderNotFoundForAnUnknownId() {
        saveOrder(new OrderDTO(orderId("16"), "known.reader@example.com", "PLACED",
                Instant.parse("2026-06-07T07:00:00.000700Z"), null, List.of(
                        line(BOOK_B, "9780000000096", "North and South", "10.00", "EUR", 1))));
        UUID unknownId = orderId("77");

        assertThatThrownBy(() -> orderDataService.findById(unknownId))
                .isInstanceOf(OrderDataService.OrderNotFoundException.class)
                .hasMessageContaining("77000000-0000-0000-0000-0000000000a1");
    }

    @Test
    void savingALineForAnUnknownBookViolatesTheForeignKey() {
        seedBook(BOOK_A);
        UUID unknownBook = UUID.fromString("70000000-0000-0000-0000-0000000000b1");
        OrderDTO withUnknownBook = new OrderDTO(orderId("17"), "fk.reader@example.com", "PLACED",
                Instant.parse("2026-06-08T08:00:00.000800Z"), null, List.of(
                        line(BOOK_A, "9780000000102", "Wives and Daughters", "12.00", "EUR", 1),
                        line(unknownBook, "9780000000119", "Unknown Book", "13.00", "EUR", 2)));

        // The failed insert leaves the transaction unusable, so nothing touches the database after it.
        assertThatThrownBy(() -> orderDataService.save(withUnknownBook))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_order_line_book");
    }

    @Test
    void savingTwoLinesForTheSameBookViolatesTheUniqueConstraint() {
        seedBook(BOOK_A);
        seedBook(BOOK_B);
        // The duplicate book is not adjacent: A, B, A.
        OrderDTO withDuplicateBook = new OrderDTO(orderId("18"), "duplicate.reader@example.com", "PLACED",
                Instant.parse("2026-06-09T09:00:00.000900Z"), null, List.of(
                        line(BOOK_A, "9780000000126", "Mary Barton", "15.00", "EUR", 1),
                        line(BOOK_B, "9780000000133", "Ruth", "16.00", "EUR", 2),
                        line(BOOK_A, "9780000000126", "Mary Barton", "15.00", "EUR", 3)));

        // The failed insert leaves the transaction unusable, so nothing touches the database after it.
        assertThatThrownBy(() -> orderDataService.save(withDuplicateBook))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_order_line_book");
    }

    @Test
    void findByCustomerEmailReturnsTheCustomersOrdersNewestFirst() {
        // Insertion order 5, 1, 3; id order 1, 3, 5; placed-time order (newest first) 3, 5, 1. The status and the
        // shipped time run in no order either, nor do the line values (quantities 3, 4, 1 in the expected order).
        // Another customer's order is the newest of all and is excluded.
        saveOrder(new OrderDTO(orderId("51"), "history.reader@example.com", "PLACED",
                Instant.parse("2026-06-11T09:00:00.000002Z"), null, List.of(
                        line(BOOK_A, "9780000000140", "Kim", "6.00", "EUR", 4))));
        saveOrder(new OrderDTO(orderId("11"), "history.reader@example.com", "SHIPPED",
                Instant.parse("2026-06-10T09:00:00.000001Z"), Instant.parse("2026-06-14T09:00:00Z"), List.of(
                        line(BOOK_B, "9780000000157", "Nostromo", "7.00", "EUR", 1))));
        saveOrder(new OrderDTO(orderId("33"), "history.reader@example.com", "SHIPPED",
                Instant.parse("2026-06-12T09:00:00.000003Z"), Instant.parse("2026-06-13T09:00:00Z"), List.of(
                        line(BOOK_C, "9780000000164", "Lord Jim", "8.00", "EUR", 3))));
        saveOrder(new OrderDTO(orderId("44"), "other.reader@example.com", "PLACED",
                Instant.parse("2026-06-20T09:00:00Z"), null, List.of(
                        line(BOOK_D, "9780000000171", "Victory", "9.00", "EUR", 1))));
        entityManager.clear();

        assertThat(orderDataService.findByCustomerEmail("history.reader@example.com"))
                .extracting(OrderDTO::id)
                .containsExactly(orderId("33"), orderId("51"), orderId("11"));
    }

    @Test
    void findByCustomerEmailBreaksAPlacedAtTieByIdDescending() {
        // Three orders share the placed time and must come out by id descending: 3..., 2..., 1.... They go in as
        // 1..., 2..., 3..., the opposite order. In the expected order the status runs SHIPPED, PLACED, SHIPPED and
        // the shipped time 06-17, null, 06-18, so neither column sorts the group in either direction. PostgreSQL
        // puts NULL first under desc and last under asc; the null sits in the middle, so neither position fits.
        // Within the group the line book, isbn, title, price and quantity also run in no order (book 5..., 2..., 4...;
        // isbn ...201, ...317, ...188; title L, O, H). The later order (id 0...) goes in last and comes first.
        Instant tie = Instant.parse("2026-06-16T16:16:16.161616Z");
        saveOrder(new OrderDTO(orderId("10"), "tie.reader@example.com", "SHIPPED", tie,
                Instant.parse("2026-06-18T10:00:00Z"), List.of(
                        line(BOOK_E, "9780000000188", "Hard Times", "20.00", "EUR", 9))));
        saveOrder(new OrderDTO(orderId("20"), "tie.reader@example.com", "PLACED", tie, null, List.of(
                line(BOOK_D, "9780000000317", "Our Mutual Friend", "19.00", "EUR", 1))));
        saveOrder(new OrderDTO(orderId("30"), "tie.reader@example.com", "SHIPPED", tie,
                Instant.parse("2026-06-17T10:00:00Z"), List.of(
                        line(BOOK_C, "9780000000201", "Little Dorrit", "22.00", "EUR", 3))));
        saveOrder(new OrderDTO(orderId("00"), "tie.reader@example.com", "PLACED",
                Instant.parse("2026-06-18T08:00:00Z"), null, List.of(
                        line(BOOK_F, "9780000000195", "Bleak House", "21.00", "EUR", 4))));
        entityManager.clear();

        assertThat(orderDataService.findByCustomerEmail("tie.reader@example.com"))
                .extracting(OrderDTO::id)
                .containsExactly(orderId("00"), orderId("30"), orderId("20"), orderId("10"));
    }

    @Test
    void findByCustomerEmailLoadsEveryOrderOnceWithItsLinesInOrder() {
        // The address keeps a leading U+2003, which the domain accepts and keeps; the adapter must not trim it.
        String customerEmail = EM_SPACE + "lines.reader@example.com";
        OrderDTO older = new OrderDTO(orderId("26"), customerEmail, "SHIPPED",
                Instant.parse("2026-03-01T10:10:10.101010Z"), Instant.parse("2026-03-02T20:20:20.202020Z"), List.of(
                        line(BOOK_B, "9780000000218", "Adam Bede", "17.50", "USD", 4),
                        line(BOOK_C, "9780000000225", "Silas Marner", "3.75", "USD", 1),
                        line(BOOK_A, "9780000000232", "Romola", "26.00", "EUR", 8)));
        OrderDTO newer = new OrderDTO(orderId("62"), customerEmail, "PLACED",
                Instant.parse("2026-03-05T05:05:05.050505Z"), null, List.of(
                        line(BOOK_E, "9780000000249", EM_SPACE + "Daniel Deronda", "18.25", "EUR", 6),
                        line(BOOK_D, "9780000000256", "Felix Holt", "4.40", "EUR", 2),
                        line(BOOK_F, "9780000000263", "Scenes of Clerical Life", "27.30", "EUR", 7)));
        saveOrder(older);
        saveOrder(newer);
        entityManager.clear();

        List<OrderDTO> found = orderDataService.findByCustomerEmail(EM_SPACE + "lines.reader@example.com");

        assertThat(found).containsExactly(newer, older);
        assertThat(found.get(0).customerEmail()).isEqualTo(EM_SPACE + "lines.reader@example.com");
        assertThat(found.get(0).lines().get(0).title()).isEqualTo(EM_SPACE + "Daniel Deronda");
        assertThat(found.get(0).lines()).extracting(OrderLineDTO::bookId).containsExactly(BOOK_E, BOOK_D, BOOK_F);
        assertThat(found.get(1).lines()).extracting(OrderLineDTO::bookId).containsExactly(BOOK_B, BOOK_C, BOOK_A);
    }

    @Test
    void findByCustomerEmailMatchesTheStoredValueExactly() {
        // The adapter never normalises: the flow passes an already normalised address. An order saved with a
        // mixed-case address as given is found by that exact string only.
        saveOrder(new OrderDTO(orderId("37"), "exact.reader@example.com", "PLACED",
                Instant.parse("2026-06-19T19:00:00.000019Z"), null, List.of(
                        line(BOOK_A, "9780000000270", "Tess", "11.00", "EUR", 1))));
        saveOrder(new OrderDTO(orderId("57"), "Mixed.Reader@Example.COM", "PLACED",
                Instant.parse("2026-06-19T20:00:00.000020Z"), null, List.of(
                        line(BOOK_B, "9780000000287", "Jude the Obscure", "12.00", "EUR", 2))));
        entityManager.clear();

        assertThat(orderDataService.findByCustomerEmail("Exact.Reader@Example.com")).isEmpty();
        assertThat(orderDataService.findByCustomerEmail("mixed.reader@example.com")).isEmpty();
        assertThat(orderDataService.findByCustomerEmail("exact.reader@example.com"))
                .extracting(OrderDTO::id).containsExactly(orderId("37"));
        assertThat(orderDataService.findByCustomerEmail("Mixed.Reader@Example.COM"))
                .extracting(OrderDTO::id).containsExactly(orderId("57"));
    }

    @Test
    void findByCustomerEmailReturnsAnEmptyListForAnUnknownCustomer() {
        saveOrder(new OrderDTO(orderId("38"), "first.customer@example.com", "PLACED",
                Instant.parse("2026-06-21T21:00:00Z"), null, List.of(
                        line(BOOK_C, "9780000000294", "Far from the Madding Crowd", "13.00", "EUR", 1))));
        saveOrder(new OrderDTO(orderId("58"), "second.customer@example.com", "SHIPPED",
                Instant.parse("2026-06-22T22:00:00Z"), Instant.parse("2026-06-23T23:00:00Z"), List.of(
                        line(BOOK_D, "9780000000300", "The Woodlanders", "14.00", "EUR", 2))));
        entityManager.clear();

        assertThat(orderDataService.findByCustomerEmail("unknown.customer@example.com")).isEmpty();
    }

    @Test
    void v3CreatesTheCustomerEmailIndex() {
        String indexDefinition = jdbcTemplate.queryForObject("select indexdef from pg_indexes"
                        + " where schemaname = 'public' and tablename = 'customer_order'"
                        + " and indexname = 'ix_customer_order_customer_email'",
                String.class);

        assertThat(indexDefinition).isEqualTo("CREATE INDEX ix_customer_order_customer_email"
                + " ON public.customer_order USING btree (customer_email, placed_at DESC, id DESC)");
    }

    /** A fixed order id starting with the given hex digits; keep the first digit 0 to 7 (see the class comment). */
    private static UUID orderId(String leadingHexDigits) {
        return UUID.fromString(leadingHexDigits + "000000-0000-0000-0000-0000000000a1");
    }

    private static OrderLineDTO line(UUID bookId, String isbn, String title, String unitPriceAmount,
            String unitPriceCurrency, int quantity) {
        return new OrderLineDTO(bookId, isbn, title, new BigDecimal(unitPriceAmount), unitPriceCurrency, quantity);
    }

    /** The expected JDBC row of one order line, with the column names of V3. */
    private static Map<String, Object> lineRow(UUID orderId, int position, UUID bookId, String isbn, String title,
            String unitPriceAmount, String unitPriceCurrency, int quantity) {
        return Map.of("order_id", orderId, "position", position, "book_id", bookId, "isbn", isbn, "title", title,
                "unit_price_amount", new BigDecimal(unitPriceAmount), "unit_price_currency", unitPriceCurrency,
                "quantity", quantity);
    }

    private static Instant instant(Object timestamp) {
        return ((Timestamp) timestamp).toInstant();
    }

    private Map<String, Object> storedOrder(UUID id) {
        return jdbcTemplate.queryForMap("select id, customer_email, status, placed_at, shipped_at, version"
                + " from customer_order where id = ?", id);
    }

    private List<Map<String, Object>> storedLines(UUID orderId) {
        return jdbcTemplate.queryForList("select order_id, position, book_id, isbn, title, unit_price_amount,"
                + " unit_price_currency, quantity from order_line where order_id = ? order by position", orderId);
    }

    private long storedVersion(UUID id) {
        return jdbcTemplate.queryForObject("select version from customer_order where id = ?", Long.class, id);
    }

    /** Seeds the book rows the foreign key needs, then saves the order through the port. */
    private void saveOrder(OrderDTO order) {
        order.lines().forEach(line -> seedBook(line.bookId()));
        orderDataService.save(order);
    }

    /**
     * Inserts a book row through JDBC once per test, with a synthetic ISBN of 13 characters that is unique within the
     * test. The order line keeps its own isbn and title snapshots, which need not match the book row.
     */
    private void seedBook(UUID bookId) {
        if (!seededBooks.add(bookId)) {
            return;
        }
        jdbcTemplate.update("insert into book (id, isbn, title, genre, price_amount, price_currency,"
                        + " publication_year, status, version) values (?, ?, ?, 'FICTION', 12.50, 'EUR', 1813,"
                        + " 'AVAILABLE', 0)",
                bookId, String.format("979%010d", seededBooks.size()), "Ordered Book " + seededBooks.size());
    }
}
