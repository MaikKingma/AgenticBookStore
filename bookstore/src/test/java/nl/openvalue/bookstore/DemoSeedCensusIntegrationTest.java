package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.openvalue.bookstore.domain.Genre;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The census of the demo seed, {@code db/seed/R__seed_demo_data.sql}. The seed is applied with plain Flyway to a
 * database of its own inside the shared container, with the locations that the default profile of the main
 * {@code application.yml} names, so the application context's database stays empty and no second Spring context starts.
 * The facts are the ones the demo needs: the book catalogue, the stock figures, the demo orders and the rule that the
 * script can run again without changing a row.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoSeedCensusIntegrationTest extends IntegrationTestBase {

    private static final String DEFAULT_PROFILE_LOCATIONS = "classpath:db/migration,classpath:db/seed";

    private static final Path SHARED_ISBNS = distributorCatalogFile("bookstore-shared-isbns.txt");
    private static final Path DISTRIBUTOR_TITLES = distributorCatalogFile("titles.json");

    /** The ISBN range of the bookstore-only books, which the distributor does not use. */
    private static final String BOOKSTORE_ONLY_ISBN_PREFIX = "978990003";

    /** The id of the last of the 40 books that earlier versions of the seed held; the books after it are newer. */
    private static final String LAST_EARLIER_BOOK_ID = "0b00c000-0000-4000-8000-000000000040";

    /** The ISBNs that the http collection adds or imports itself, so the seed must leave them out. */
    private static final Set<String> ISBNS_THE_HTTP_COLLECTION_ADDS = Set.of(
            "9789900010029", "9780141439471", "9780141439600", "9780141439563", "9789900010050",
            "9780141439792", "9789900010791");

    /** The distributor's title and author of each shared ISBN, in the order of the shared list. */
    private static final Map<String, List<String>> SHARED_TITLES_AND_AUTHORS = sharedTitlesAndAuthors();

    /** Every seeded book at or below its reorder level and the value of its low stock signal. */
    private static final Map<String, Boolean> AT_OR_BELOW_LEVEL_SIGNALLED = Map.of(
            "9789900010043", true,
            "9789900010074", true,
            "9789900010296", true,
            "9789900010302", true,
            "9789900010753", true,
            "9789900010777", true,
            "9789900010913", true);

    private static final Set<String> SOLD_OUT_ISBNS = Set.of("9789900010043", "9789900010296", "9789900010777");

    /** Treasure Island: shared, available, signal false, five copies above its level of three. */
    private static final String MARGIN_BOOK_ISBN = "9789900010715";

    private static final Set<String> DEMO_CUSTOMERS =
            Set.of("reader@example.com", "bookworm@example.org", "night.owl@example.net");

    /** The third demo order, PLACED for reader@example.com with three lines. */
    private static final String PLACED_ORDER_ID = "0d0e0000-0000-4000-8000-000000000003";

    /** The sixth demo order, PLACED for night.owl@example.net. */
    private static final String LAST_ORDER_ID = "0d0e0000-0000-4000-8000-000000000006";

    @Autowired
    private PostgreSQLContainer postgres;

    @Autowired
    private Clock applicationClock;

    private SeedDatabase seeded;
    private JdbcTemplate db;

    @BeforeAll
    void seedAnIsolatedDatabase() throws IOException {
        seeded = SeedDatabase.create(postgres, jdbcTemplate, defaultProfileLocations());
        seeded.migrate();
        db = seeded.jdbcTemplate();
    }

    @AfterAll
    void dropTheIsolatedDatabase() {
        seeded.close();
    }

    // Locations

    @Test
    void theDefaultProfileAppliesTheMigrationsAndTheSeed() throws IOException {
        assertThat(defaultProfileLocations()).isEqualTo(DEFAULT_PROFILE_LOCATIONS);
    }

    // Books

    @Test
    void holdsOneHundredAndThreeBooksWithUniqueIsbns() {
        List<Book> books = books();

        assertThat(books).hasSize(103);
        assertThat(books.stream().map(Book::isbn)).doesNotHaveDuplicates();
        assertThat(books.stream().map(Book::id)).doesNotHaveDuplicates();
    }

    @Test
    void theSharedBooksAreTheDistributorsTwentyIsbnsWithTheirTitlesAndAuthors() throws IOException {
        List<String> sharedIsbns = Files.readAllLines(SHARED_ISBNS).stream().map(String::trim)
                .filter(line -> !line.isEmpty()).toList();
        Map<String, Book> byIsbn = books().stream().collect(Collectors.toMap(Book::isbn, book -> book));

        assertThat(sharedIsbns).hasSize(20).containsExactlyInAnyOrderElementsOf(SHARED_TITLES_AND_AUTHORS.keySet());
        assertThat(byIsbn.keySet()).containsAll(sharedIsbns);
        for (String isbn : sharedIsbns) {
            Book book = byIsbn.get(isbn);
            assertThat(book.title()).as("title of %s", isbn).isEqualTo(SHARED_TITLES_AND_AUTHORS.get(isbn).get(0));
            assertThat(book.authors()).as("authors of %s", isbn)
                    .containsExactly(SHARED_TITLES_AND_AUTHORS.get(isbn).get(1));
        }
    }

    @Test
    void theEightyThreeBookstoreOnlyBooksHaveValidIsbnsFromTheirOwnRangeTheDistributorDoesNotList()
            throws IOException {
        Set<String> sharedIsbns = SHARED_TITLES_AND_AUTHORS.keySet();
        Set<String> distributorIsbns = distributorIsbns();
        List<String> bookstoreOnly = books().stream().map(Book::isbn).filter(isbn -> !sharedIsbns.contains(isbn))
                .toList();

        assertThat(distributorIsbns).hasSize(150);
        assertThat(bookstoreOnly).hasSize(83);
        assertThat(bookstoreOnly).allSatisfy(isbn -> assertThat(isValidIsbn13(isbn)).as(isbn).isTrue());
        assertThat(bookstoreOnly).doesNotContainAnyElementsOf(distributorIsbns);
        assertThat(bookstoreOnly).doesNotContainAnyElementsOf(ISBNS_THE_HTTP_COLLECTION_ADDS);
        assertThat(bookstoreOnly).allSatisfy(isbn -> assertThat(isbn).as("range of %s", isbn)
                .startsWith(BOOKSTORE_ONLY_ISBN_PREFIX));
    }

    /** The seven http ISBNs are checked over every seeded book, apart from the range and the distributor checks. */
    @Test
    void noSeededBookHasAnIsbnThatTheHttpCollectionAddsOrImports() {
        assertThat(ISBNS_THE_HTTP_COLLECTION_ADDS).hasSize(7);
        assertThat(books().stream().map(Book::isbn))
                .doesNotContainAnyElementsOf(ISBNS_THE_HTTP_COLLECTION_ADDS)
                .hasSize(103);
    }

    @Test
    void everyGenreHasExactlyTenAvailableBooks() {
        Map<String, Long> expected = Arrays.stream(Genre.values())
                .collect(Collectors.toMap(Enum::name, genre -> 10L));
        Map<String, Long> availablePerGenre = books().stream().filter(book -> book.status().equals("AVAILABLE"))
                .collect(Collectors.groupingBy(Book::genre, Collectors.counting()));

        assertThat(expected).hasSize(10);
        assertThat(availablePerGenre).isEqualTo(expected);
        assertThat(books()).allSatisfy(book -> assertThat(book.genre()).as("genre of %s", book.isbn())
                .isIn(expected.keySet()));
    }

    @Test
    void exactlyThreeBooksAreDiscontinuedAndNoneOfThemIsShared() {
        List<Book> discontinued = books().stream().filter(book -> book.status().equals("DISCONTINUED")).toList();

        assertThat(books()).allSatisfy(book -> assertThat(book.status()).isIn("AVAILABLE", "DISCONTINUED"));
        assertThat(discontinued.stream().map(Book::isbn)).containsExactlyInAnyOrder(
                "9789900030119", "9789900030140", "9789900030164");
        assertThat(discontinued.stream().map(Book::isbn)).doesNotContainAnyElementsOf(SHARED_TITLES_AND_AUTHORS.keySet());
        assertThat(discontinued.stream().collect(Collectors.toMap(Book::isbn, Book::genre))).isEqualTo(
                Map.of("9789900030119", "MYSTERY", "9789900030140", "HISTORY", "9789900030164", "SCIENCE"));
    }

    @Test
    void titlesAreDistinctAndTheInventedBooksReuseNoDistributorTitleOrAuthor() throws IOException {
        Set<String> sharedIsbns = SHARED_TITLES_AND_AUTHORS.keySet();
        Set<String> distributorTitles = distributorValues("titleName");
        Set<String> distributorPeople = distributorValues("name");
        List<Book> invented = books().stream().filter(book -> !sharedIsbns.contains(book.isbn())).toList();

        assertThat(books().stream().map(book -> book.title().strip().toLowerCase(Locale.ROOT))).hasSize(103)
                .doesNotHaveDuplicates();
        assertThat(distributorTitles).hasSize(150);
        assertThat(distributorPeople).isNotEmpty();
        assertThat(invented).hasSize(83).allSatisfy(book -> {
            assertThat(distributorTitles).as("distributor titles").doesNotContain(book.title().strip().toLowerCase(Locale.ROOT));
            assertThat(book.authors().stream().map(author -> author.strip().toLowerCase(Locale.ROOT)))
                    .as("authors of %s", book.isbn()).doesNotContainAnyElementsOf(distributorPeople);
        });
    }

    /** http/catalogue.http and http/demo-flow.http state what these searches find, so the seed must keep it true. */
    @Test
    void theSearchTermsTheHttpCollectionDescribesFindTheBooksItNames() {
        List<Book> austen = books().stream().filter(book -> mentions(book, "austen")).toList();

        assertThat(austen.stream().map(Book::isbn)).containsExactlyInAnyOrder("9780141439518", "9789900010012");
        assertThat(austen.stream().filter(book -> book.genre().equals("FICTION")).map(Book::isbn))
                .containsExactly("9780141439518");
        assertThat(books().stream().filter(book -> mentions(book, "treasure")).map(Book::isbn))
                .containsExactly(MARGIN_BOOK_ISBN);
    }

    @Test
    void everyBookSatisfiesTheRulesTheDomainChecksWhenItReadsARow() {
        int nextYear = LocalDate.ofInstant(applicationClock.instant(), applicationClock.getZone()).getYear() + 1;

        assertThat(books()).allSatisfy(book -> {
            assertThat(book.price()).as("price of %s", book.isbn())
                    .isGreaterThan(BigDecimal.ZERO).isLessThanOrEqualTo(new BigDecimal("500.00"));
            assertThat(book.price().scale()).isEqualTo(2);
            assertThat(book.currency()).isEqualTo("EUR");
            assertThat(book.year()).as("year of %s", book.isbn()).isBetween(1450, nextYear);
            assertThat(book.authors()).as("authors of %s", book.isbn()).hasSizeBetween(1, 5)
                    .allSatisfy(author -> assertThat(author).isNotBlank().isEqualTo(author.strip())
                            .hasSizeLessThanOrEqualTo(120));
            assertThat(book.title()).as("title of %s", book.isbn()).isNotBlank().isEqualTo(book.title().strip())
                    .hasSizeLessThanOrEqualTo(200);
            assertThat(book.version()).as("version of %s", book.isbn()).isZero();
        });
    }

    @Test
    void authorsKeepTheirPositionsFromZeroWithoutGaps() {
        Map<UUID, List<Integer>> positions = new LinkedHashMap<>();
        db.query("SELECT book_id, position FROM book_author ORDER BY book_id, position", rs -> {
            positions.computeIfAbsent(rs.getObject(1, UUID.class), id -> new ArrayList<>()).add(rs.getInt(2));
        });

        assertThat(positions).hasSize(103);
        assertThat(positions.values()).allSatisfy(list -> assertThat(list)
                .containsExactlyElementsOf(IntStream.range(0, list.size()).boxed().toList()));
    }

    // Stock

    @Test
    void everyBookHasAStockItemWithItsFiguresInRange() {
        List<Stock> stock = stock();

        assertThat(stock).hasSize(103);
        assertThat(stock.stream().map(Stock::isbn)).containsExactlyInAnyOrderElementsOf(
                books().stream().map(Book::isbn).toList());
        assertThat(stock).allSatisfy(item -> {
            assertThat(item.onHand()).as("on hand of %s", item.isbn()).isBetween(0, 10000);
            assertThat(item.reserved()).as("reserved of %s", item.isbn()).isBetween(0, item.onHand());
            assertThat(item.reorderLevel()).isGreaterThanOrEqualTo(0);
            assertThat(item.reorderQuantity()).isGreaterThanOrEqualTo(1);
            assertThat(item.version()).as("version of %s", item.isbn()).isZero();
        });
    }

    @Test
    void atLeastFiveBooksAreAtOrBelowTheirReorderLevelAndAllOfThemAreShared() {
        Set<String> sharedIsbns = SHARED_TITLES_AND_AUTHORS.keySet();
        List<String> lowStock = stock().stream().filter(item -> item.available() <= item.reorderLevel())
                .map(Stock::isbn).toList();

        assertThat(lowStock).hasSizeGreaterThanOrEqualTo(5);
        assertThat(lowStock).isSubsetOf(sharedIsbns);
        assertThat(lowStock).containsExactlyInAnyOrderElementsOf(AT_OR_BELOW_LEVEL_SIGNALLED.keySet());
    }

    @Test
    void exactlyThreeSharedBooksAreSoldOutAndNoOtherBookHasNothingAvailable() {
        Set<String> sharedIsbns = SHARED_TITLES_AND_AUTHORS.keySet();
        List<Stock> stock = stock();

        assertThat(stock.stream().filter(item -> item.onHand() == 0).map(Stock::isbn))
                .containsExactlyInAnyOrderElementsOf(SOLD_OUT_ISBNS)
                .isSubsetOf(sharedIsbns);
        assertThat(stock.stream().filter(item -> item.available() == 0).map(Stock::isbn))
                .containsExactlyInAnyOrderElementsOf(SOLD_OUT_ISBNS);
    }

    @Test
    void noStockItemHasAPendingRestock() {
        Integer withPendingColumns = db.queryForObject("SELECT count(*) FROM stock_item WHERE "
                + "pending_restock_order_id IS NOT NULL OR pending_restock_quantity IS NOT NULL "
                + "OR pending_restock_expected_delivery_date IS NOT NULL OR pending_restock_backordered IS NOT NULL",
                Integer.class);

        assertThat(stock()).hasSize(103);
        assertThat(withPendingColumns).isZero();
    }

    @Test
    void theLowStockSignalIsFalseAboveTheLevelAndPinnedAtOrBelowIt() {
        Map<String, Boolean> signalled = stock().stream()
                .filter(item -> item.available() <= item.reorderLevel())
                .collect(Collectors.toMap(Stock::isbn, Stock::signalled));

        assertThat(stock().stream().filter(item -> item.available() > item.reorderLevel()))
                .hasSize(96)
                .allSatisfy(item -> assertThat(item.signalled()).as("signal of %s", item.isbn()).isFalse());
        assertThat(signalled).isEqualTo(AT_OR_BELOW_LEVEL_SIGNALLED);
    }

    @Test
    void theSixtyThreeNewerBooksHaveNothingReservedAndStandAboveTheirLevelWithoutSignal() {
        Set<String> newerIsbns = books().stream().filter(book -> book.id().toString().compareTo(LAST_EARLIER_BOOK_ID) > 0)
                .map(Book::isbn).collect(Collectors.toSet());
        List<Stock> newerStock = stock().stream().filter(item -> newerIsbns.contains(item.isbn())).toList();

        assertThat(newerIsbns).hasSize(63);
        assertThat(newerStock).hasSize(63).allSatisfy(item -> {
            assertThat(item.reserved()).as("reserved of %s", item.isbn()).isZero();
            assertThat(item.available()).as("available of %s", item.isbn()).isGreaterThan(item.reorderLevel());
            assertThat(item.signalled()).as("signal of %s", item.isbn()).isFalse();
        });
    }

    @Test
    void anAvailableSharedBookWithoutSignalSitsOneToNineCopiesAboveItsLevel() {
        Map<String, Book> byIsbn = books().stream().collect(Collectors.toMap(Book::isbn, book -> book));
        List<String> marginBooks = stock().stream()
                .filter(item -> SHARED_TITLES_AND_AUTHORS.containsKey(item.isbn()))
                .filter(item -> byIsbn.get(item.isbn()).status().equals("AVAILABLE"))
                .filter(item -> !item.signalled())
                .filter(item -> item.available() - item.reorderLevel() >= 1
                        && item.available() - item.reorderLevel() <= 9)
                .map(Stock::isbn).toList();

        assertThat(marginBooks).contains(MARGIN_BOOK_ISBN);
        assertThat(byIsbn.get(MARGIN_BOOK_ISBN).title()).isEqualTo("Treasure Island");
        Stock margin = stock().stream().filter(item -> item.isbn().equals(MARGIN_BOOK_ISBN)).findFirst().orElseThrow();
        assertThat(margin.available() - margin.reorderLevel()).isEqualTo(5);
    }

    // Orders

    @Test
    void sixOrdersBelongToTheThreeDemoCustomersTwoShippedAndFourPlaced() {
        List<Order> orders = orders();
        Instant now = applicationClock.instant();

        assertThat(orders).hasSize(6);
        assertThat(orders.stream().map(Order::email).collect(Collectors.toSet())).isEqualTo(DEMO_CUSTOMERS);
        assertThat(orders).allSatisfy(order -> {
            assertThat(order.email()).isEqualTo(order.email().toLowerCase(Locale.ROOT));
            assertThat(order.placedAt()).as("placed at of %s", order.id()).isBefore(now);
            assertThat(order.version()).isZero();
        });
        for (String customer : DEMO_CUSTOMERS) {
            assertThat(orders.stream().filter(order -> order.email().equals(customer))).hasSize(2);
        }
        assertThat(orders.stream().filter(order -> order.status().equals("SHIPPED"))).hasSize(2)
                .allSatisfy(order -> assertThat(order.shippedAt()).isNotNull().isAfter(order.placedAt())
                        .isBefore(now));
        assertThat(orders.stream().filter(order -> order.status().equals("PLACED"))).hasSize(4)
                .allSatisfy(order -> assertThat(order.shippedAt()).isNull());
    }

    @Test
    void everyOrderHasOneToTwentyLinesOfOneToTenCopiesEachCopiedFromItsBook() {
        Map<UUID, Book> byId = books().stream().collect(Collectors.toMap(Book::id, book -> book));
        Map<UUID, List<Line>> linesPerOrder = lines().stream().collect(Collectors.groupingBy(Line::orderId));

        assertThat(linesPerOrder.keySet()).containsExactlyInAnyOrderElementsOf(
                orders().stream().map(Order::id).toList());
        linesPerOrder.forEach((orderId, lines) -> {
            assertThat(lines).as("lines of %s", orderId).hasSizeBetween(1, 20);
            assertThat(lines.stream().map(Line::bookId)).as("books of %s", orderId).doesNotHaveDuplicates();
            assertThat(lines.stream().map(Line::position).toList()).as("positions of %s", orderId)
                    .containsExactlyElementsOf(IntStream.range(0, lines.size()).boxed().toList());
        });
        assertThat(lines()).allSatisfy(line -> {
            Book book = byId.get(line.bookId());
            assertThat(book).as("book of a line of %s", line.orderId()).isNotNull();
            assertThat(line.quantity()).isBetween(1, 10);
            assertThat(line.isbn()).isEqualTo(book.isbn());
            assertThat(line.title()).isEqualTo(book.title());
            assertThat(line.unitPrice()).isEqualTo(book.price());
            assertThat(line.currency()).isEqualTo("EUR");
        });
    }

    @Test
    void reservedIsTheSumOfThePlacedLinesAndNeverExceedsOnHand() {
        Map<UUID, String> statusPerOrder = orders().stream().collect(Collectors.toMap(Order::id, Order::status));
        Map<UUID, Integer> placedPerBook = new LinkedHashMap<>();
        for (Line line : lines()) {
            if (statusPerOrder.get(line.orderId()).equals("PLACED")) {
                placedPerBook.merge(line.bookId(), line.quantity(), Integer::sum);
            }
        }

        assertThat(placedPerBook).isNotEmpty();
        assertThat(stock()).allSatisfy(item -> {
            assertThat(item.reserved()).as("reserved of %s", item.isbn())
                    .isEqualTo(placedPerBook.getOrDefault(item.bookId(), 0));
            assertThat(item.onHand()).as("on hand of %s", item.isbn()).isGreaterThanOrEqualTo(item.reserved());
        });
    }

    @Test
    void noPlacedOrderHoldsADiscontinuedBook() {
        Set<UUID> discontinued = books().stream().filter(book -> book.status().equals("DISCONTINUED"))
                .map(Book::id).collect(Collectors.toSet());
        Map<UUID, String> statusPerOrder = orders().stream().collect(Collectors.toMap(Order::id, Order::status));

        assertThat(discontinued).isNotEmpty();
        assertThat(lines().stream().filter(line -> statusPerOrder.get(line.orderId()).equals("PLACED")))
                .isNotEmpty()
                .noneMatch(line -> discontinued.contains(line.bookId()));
    }

    // Running the script again

    @Test
    void runningTheSeedAgainChangesNoRow() throws IOException {
        try (SeedDatabase second = SeedDatabase.create(postgres, jdbcTemplate, defaultProfileLocations())) {
            second.migrate();
            JdbcTemplate other = second.jdbcTemplate();
            Map<String, List<Map<String, Object>>> before = tableContents(other);
            other.update("DELETE FROM flyway_schema_history WHERE version IS NULL");

            MigrateResult rerun = second.migrate();

            assertThat(before.get("book")).hasSize(103);
            assertThat(before.get("stock_item")).hasSize(103);
            assertThat(before.get("customer_order")).hasSize(6);
            assertThat(before.get("order_line")).isNotEmpty();
            assertThat(before.get("book_author")).isNotEmpty();
            assertThat(rerun.migrationsExecuted).isEqualTo(1);
            assertThat(second.repeatableScripts()).containsExactly("R__seed_demo_data.sql");
            assertThat(tableContents(other)).isEqualTo(before);
        }
    }

    @Test
    void runningTheSeedAgainKeepsWhatADeveloperChangedInTheDemoData() throws IOException {
        try (SeedDatabase second = SeedDatabase.create(postgres, jdbcTemplate, defaultProfileLocations())) {
            second.migrate();
            JdbcTemplate other = second.jdbcTemplate();
            assertThat(other.update("UPDATE stock_item SET on_hand = 99 WHERE book_id = "
                    + "(SELECT id FROM book WHERE isbn = ?)", MARGIN_BOOK_ISBN)).isEqualTo(1);
            assertThat(other.update("UPDATE book SET title = 'Treasure Island, annotated' WHERE isbn = ?",
                    MARGIN_BOOK_ISBN)).isEqualTo(1);
            shipThePlacedOrderTheWayTheAppDoes(other);
            assertThat(other.update("UPDATE order_line SET quantity = 2, unit_price_amount = 7.49 "
                    + "WHERE order_id = ? AND position = 0", UUID.fromString(LAST_ORDER_ID))).isEqualTo(1);
            assertThat(other.update("UPDATE book SET price_amount = 11.49 WHERE isbn = ?", "9789900010807"))
                    .isEqualTo(1);
            assertThat(other.update("UPDATE book_author SET name = 'Jane Austen, edited' WHERE position = 0 AND "
                    + "book_id = (SELECT id FROM book WHERE isbn = ?)", "9789900010012")).isEqualTo(1);
            assertThat(other.update("UPDATE stock_item SET low_stock_signalled = false, reorder_level = 5 "
                    + "WHERE book_id = (SELECT id FROM book WHERE isbn = ?)", "9789900010302")).isEqualTo(1);
            Map<String, List<Map<String, Object>>> before = tableContents(other);
            other.update("DELETE FROM flyway_schema_history WHERE version IS NULL");

            MigrateResult rerun = second.migrate();

            Map<String, List<Map<String, Object>>> after = tableContents(other);
            assertThat(rerun.migrationsExecuted).isEqualTo(1);
            assertThat(after.get("stock_item")).isEqualTo(before.get("stock_item"));
            assertThat(after.get("book")).isEqualTo(before.get("book"));
            assertThat(after).isEqualTo(before);
        }
    }

    @Test
    void runningTheSeedAgainBringsBackLinesThatADeveloperDeleted() throws IOException {
        try (SeedDatabase second = SeedDatabase.create(postgres, jdbcTemplate, defaultProfileLocations())) {
            second.migrate();
            JdbcTemplate other = second.jdbcTemplate();
            assertThat(other.update("DELETE FROM order_line WHERE order_id IN "
                    + "(SELECT id FROM customer_order WHERE customer_email = 'reader@example.com')")).isPositive();
            Map<String, List<Map<String, Object>>> before = tableContents(other);
            other.update("DELETE FROM flyway_schema_history WHERE version IS NULL");

            second.migrate();

            Map<String, List<Map<String, Object>>> after = tableContents(other);
            assertThat(after.get("order_line")).hasSizeGreaterThan(before.get("order_line").size());
        }
    }

    /**
     * Ships the PLACED demo order of reader@example.com as the application does: status, shipped_at and version on
     * the order, and on and reserved down by the line quantity, version up, on each stock item of its lines.
     */
    private static void shipThePlacedOrderTheWayTheAppDoes(JdbcTemplate other) {
        UUID order = UUID.fromString(PLACED_ORDER_ID);
        assertThat(other.queryForObject("SELECT status FROM customer_order WHERE id = ?", String.class, order))
                .isEqualTo("PLACED");
        assertThat(other.update("UPDATE customer_order SET status = 'SHIPPED', shipped_at = '2026-11-04T12:00:00Z', "
                + "version = 1 WHERE id = ?", order)).isEqualTo(1);
        assertThat(other.update("UPDATE stock_item s SET on_hand = s.on_hand - l.quantity, "
                + "reserved = s.reserved - l.quantity, version = 1 FROM order_line l "
                + "WHERE l.order_id = ? AND l.book_id = s.book_id", order)).isEqualTo(3);
    }

    @Test
    void theSeedFileHoldsOnlyAsciiBytes() throws IOException {
        byte[] bytes;
        try (InputStream seed = new ClassPathResource("db/seed/R__seed_demo_data.sql").getInputStream()) {
            bytes = seed.readAllBytes();
        }

        int firstOffsetAbove0x7F = IntStream.range(0, bytes.length).filter(i -> bytes[i] < 0).findFirst().orElse(-1);

        assertThat(bytes).isNotEmpty();
        assertThat(firstOffsetAbove0x7F).as("offset of the first byte above 0x7F in the seed file").isEqualTo(-1);
    }

    @Test
    void everyFreshDatabaseGetsTheSameIdsAndValues() throws IOException {
        try (SeedDatabase second = SeedDatabase.create(postgres, jdbcTemplate, defaultProfileLocations())) {
            second.migrate();

            assertThat(tableContents(second.jdbcTemplate())).isEqualTo(tableContents(db));
        }
    }

    // Reading the seeded database

    private record Book(UUID id, String isbn, String title, String genre, BigDecimal price, String currency, int year,
                        String status, long version, List<String> authors) {
    }

    private record Stock(UUID bookId, String isbn, int onHand, int reserved, int reorderLevel, int reorderQuantity,
                         boolean signalled, long version) {

        int available() {
            return onHand - reserved;
        }
    }

    private record Order(UUID id, String email, String status, Instant placedAt, Instant shippedAt, long version) {
    }

    private record Line(UUID orderId, int position, UUID bookId, String isbn, String title, BigDecimal unitPrice,
                        String currency, int quantity) {
    }

    private List<Book> books() {
        Map<UUID, List<String>> authors = new LinkedHashMap<>();
        db.query("SELECT book_id, name FROM book_author ORDER BY book_id, position", rs -> {
            authors.computeIfAbsent(rs.getObject(1, UUID.class), id -> new ArrayList<>()).add(rs.getString(2));
        });
        return db.query("SELECT id, isbn, title, genre, price_amount, price_currency, publication_year, status, "
                + "version FROM book ORDER BY isbn", (rs, row) -> new Book(rs.getObject(1, UUID.class),
                rs.getString(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getString(6),
                rs.getInt(7), rs.getString(8), rs.getLong(9),
                authors.getOrDefault(rs.getObject(1, UUID.class), List.of())));
    }

    private List<Stock> stock() {
        return db.query("SELECT s.book_id, b.isbn, s.on_hand, s.reserved, s.reorder_level, s.reorder_quantity, "
                + "s.low_stock_signalled, s.version FROM stock_item s JOIN book b ON b.id = s.book_id "
                + "ORDER BY b.isbn", (rs, row) -> new Stock(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getBoolean(7), rs.getLong(8)));
    }

    private List<Order> orders() {
        return db.query("SELECT id, customer_email, status, placed_at, shipped_at, version FROM customer_order "
                + "ORDER BY placed_at", (rs, row) -> new Order(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getString(3), rs.getTimestamp(4).toInstant(),
                rs.getTimestamp(5) == null ? null : rs.getTimestamp(5).toInstant(), rs.getLong(6)));
    }

    private List<Line> lines() {
        return db.query("SELECT order_id, position, book_id, isbn, title, unit_price_amount, unit_price_currency, "
                + "quantity FROM order_line ORDER BY order_id, position", (rs, row) -> new Line(
                rs.getObject(1, UUID.class), rs.getInt(2), rs.getObject(3, UUID.class), rs.getString(4),
                rs.getString(5), rs.getBigDecimal(6), rs.getString(7), rs.getInt(8)));
    }

    /** The complete content of every table the seed writes, each in primary key order. */
    private static Map<String, List<Map<String, Object>>> tableContents(JdbcTemplate template) {
        Map<String, String> orderBy = new LinkedHashMap<>();
        orderBy.put("book", "id");
        orderBy.put("book_author", "book_id, position");
        orderBy.put("stock_item", "book_id");
        orderBy.put("customer_order", "id");
        orderBy.put("order_line", "order_id, position");
        Map<String, List<Map<String, Object>>> contents = new LinkedHashMap<>();
        orderBy.forEach((table, key) -> contents.put(table,
                template.queryForList("SELECT * FROM " + table + " ORDER BY " + key)));
        return contents;
    }

    // Sources of the expected facts

    /** The value of {@code spring.flyway.locations} that the main application.yml gives the default profile. */
    private static String defaultProfileLocations() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        Object value = sources.stream().map(source -> source.getProperty("spring.flyway.locations"))
                .filter(Objects::nonNull).findFirst().orElse(null);
        assertThat(value).as("spring.flyway.locations in the main application.yml").isInstanceOf(String.class);
        return (String) value;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (!isbn.matches("97[89]\\d{10}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10 == isbn.charAt(12) - '0';
    }

    private static Path distributorCatalogFile(String name) {
        // Surefire runs in the bookstore module directory; an IDE may run from the repository root.
        List<Path> candidates = List.of(
                Path.of("..", "distributor", "src", "main", "resources", "catalog", name),
                Path.of("distributor", "src", "main", "resources", "catalog", name));
        return candidates.stream().filter(Files::exists).findFirst().orElseThrow(
                () -> new IllegalStateException("The distributor catalogue file " + name + " was not found"));
    }

    /** Every value of the JSON key in titles.json, stripped and in lower case. */
    private static Set<String> distributorValues(String key) throws IOException {
        Matcher matcher = Pattern.compile("\"" + key + "\":\\s*\"([^\"\\\\]*)\"").matcher(Files.readString(DISTRIBUTOR_TITLES));
        Set<String> values = new HashSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1).strip().toLowerCase(Locale.ROOT));
        }
        return values;
    }

    /** Whether the term, in lower case, is part of the title or of any author, as the catalogue search matches. */
    private static boolean mentions(Book book, String term) {
        return book.title().toLowerCase(Locale.ROOT).contains(term)
                || book.authors().stream().anyMatch(author -> author.toLowerCase(Locale.ROOT).contains(term));
    }

    private static Set<String> distributorIsbns() throws IOException {
        Matcher matcher = Pattern.compile("\"ean\":\\s*\"(\\d{13})\"").matcher(Files.readString(DISTRIBUTOR_TITLES));
        Set<String> isbns = new HashSet<>();
        while (matcher.find()) {
            isbns.add(matcher.group(1));
        }
        return isbns;
    }

    private static Map<String, List<String>> sharedTitlesAndAuthors() {
        Map<String, List<String>> shared = new LinkedHashMap<>();
        shared.put("9780141439518", List.of("Pride and Prejudice", "Jane Austen"));
        shared.put("9789900010012", List.of("Emma", "Jane Austen"));
        shared.put("9789900010043", List.of("Frankenstein", "Mary Shelley"));
        shared.put("9789900010067", List.of("The Time Machine", "H. G. Wells"));
        shared.put("9789900010074", List.of("The War of the Worlds", "H. G. Wells"));
        shared.put("9789900010159", List.of("A Study in Scarlet", "Arthur Conan Doyle"));
        shared.put("9789900010296", List.of("Jane Eyre", "Charlotte Bronte"));
        shared.put("9789900010302", List.of("Wuthering Heights", "Emily Bronte"));
        shared.put("9789900010715", List.of("Treasure Island", "Robert Louis Stevenson"));
        shared.put("9789900010746", List.of("Dracula", "Bram Stoker"));
        shared.put("9789900010753", List.of("The Picture of Dorian Gray", "Oscar Wilde"));
        shared.put("9789900010760", List.of("Leaves of Grass", "Walt Whitman"));
        shared.put("9789900010777", List.of("The Adventures of Tom Sawyer", "Mark Twain"));
        shared.put("9789900010807", List.of("Moby-Dick", "Herman Melville"));
        shared.put("9789900010821", List.of("Little Women", "Louisa May Alcott"));
        shared.put("9789900010913", List.of("The Moonstone", "Wilkie Collins"));
        shared.put("9789900011040", List.of("Phantastes", "George MacDonald"));
        shared.put("9789900011149",
                List.of("The History of the Decline and Fall of the Roman Empire", "Edward Gibbon"));
        shared.put("9789900011163", List.of("On the Origin of Species", "Charles Darwin"));
        shared.put("9789900011187", List.of("Passages from the Life of a Philosopher", "Charles Babbage"));
        return shared;
    }
}
