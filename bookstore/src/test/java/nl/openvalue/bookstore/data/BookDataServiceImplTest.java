package nl.openvalue.bookstore.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import nl.openvalue.bookstore.DatabaseCleaner;
import nl.openvalue.bookstore.PostgresTestConfig;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.BookDataService.BookNotFoundException;
import nl.openvalue.bookstore.domaininteraction.BookDataService.IsbnAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JPA slice test of the data adapter on PostgreSQL. Every test runs in its own transaction that is rolled back, and
 * starts by emptying the application tables inside that transaction, so exact counts never depend on other classes.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({PostgresTestConfig.class, BookDataServiceImpl.class})
class BookDataServiceImplTest {

    @Autowired
    private BookDataService bookDataService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        new DatabaseCleaner(jdbcTemplate).clean();
    }

    @Test
    void savesANewBookAndFindsItById() {
        BookDTO book = new BookDTO(UUID.randomUUID(), "9780141439518", "Pride and Prejudice",
                List.of("Jane Austen", "Anna Quindlen"), "FICTION", new BigDecimal("12.50"), "EUR", 1813,
                "DISCONTINUED");

        bookDataService.save(book);
        entityManager.clear();

        BookDTO found = bookDataService.findById(book.id());
        assertThat(found).isEqualTo(book);
        assertThat(found.priceAmount()).isEqualByComparingTo("12.50");
        assertThat(found.priceAmount().scale()).isEqualTo(2);
    }

    @Test
    void storesEveryColumnAndTheAuthorsWithTheirPositions() {
        BookDTO book = new BookDTO(UUID.randomUUID(), "9780141439471", "Frankenstein",
                List.of("Mary Shelley", "Charlotte Gordon", "Anne Mellor"), "SCIENCE_FICTION",
                new BigDecimal("9.90"), "EUR", 1818, "AVAILABLE");

        bookDataService.save(book);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select isbn, title, genre, price_amount, price_currency, publication_year, status, version"
                        + " from book where id = ?", book.id());
        assertThat(row.get("isbn")).isEqualTo("9780141439471");
        assertThat(row.get("title")).isEqualTo("Frankenstein");
        assertThat(row.get("genre")).isEqualTo("SCIENCE_FICTION");
        assertThat((BigDecimal) row.get("price_amount")).isEqualByComparingTo("9.90");
        assertThat(((BigDecimal) row.get("price_amount")).toPlainString()).isEqualTo("9.90");
        assertThat(row.get("price_currency")).isEqualTo("EUR");
        assertThat(row.get("publication_year")).isEqualTo(1818);
        assertThat(row.get("status")).isEqualTo("AVAILABLE");
        assertThat(row.get("version")).isEqualTo(0L);

        List<Map<String, Object>> authorRows = jdbcTemplate.queryForList(
                "select position, name from book_author where book_id = ? order by position", book.id());
        assertThat(authorRows).extracting(r -> r.get("position")).containsExactly(0, 1, 2);
        assertThat(authorRows).extracting(r -> r.get("name"))
                .containsExactly("Mary Shelley", "Charlotte Gordon", "Anne Mellor");
    }

    @Test
    void storesATitleOf200AndAnAuthorNameOf120CharactersUnchanged() {
        String longestTitle = "T".repeat(199) + "e";
        String longestAuthorName = "A".repeat(119) + "z";
        BookDTO book = new BookDTO(UUID.randomUUID(), "9780141439587", longestTitle, List.of(longestAuthorName),
                "FICTION", new BigDecimal("500.00"), "EUR", 1815, "AVAILABLE");

        bookDataService.save(book);

        assertThat(jdbcTemplate.queryForObject("select title from book where id = ?", String.class, book.id()))
                .hasSize(200).isEqualTo(longestTitle);
        assertThat(jdbcTemplate.queryForObject("select name from book_author where book_id = ?", String.class,
                book.id())).hasSize(120).isEqualTo(longestAuthorName);
        entityManager.clear();
        BookDTO found = bookDataService.findById(book.id());
        assertThat(found.title()).isEqualTo(longestTitle);
        assertThat(found.authors()).containsExactly(longestAuthorName);
    }

    @Test
    void keepsAuthorsInTheirOrder() {
        BookDTO book = fiction("9780141439662", "Sense and Sensibility", "Zadie Smith", "Amy Lowell", "Mary Beard");

        bookDataService.save(book);
        entityManager.clear();

        assertThat(bookDataService.findById(book.id()).authors())
                .containsExactly("Zadie Smith", "Amy Lowell", "Mary Beard");
    }

    @Test
    void findsABookByIsbn() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen");
        bookDataService.save(book);
        bookDataService.save(fiction("9780141439587", "Emma", "Jane Austen"));
        entityManager.clear();

        assertThat(bookDataService.findByIsbn("9780141439518")).contains(book);
    }

    @Test
    void findByIsbnIsEmptyForAnUnknownIsbn() {
        bookDataService.save(fiction("9780141439518", "Pride and Prejudice", "Jane Austen"));

        assertThat(bookDataService.findByIsbn("9780141439587")).isEmpty();
    }

    @Test
    void findByIdThrowsBookNotFoundForAnUnknownId() {
        bookDataService.save(fiction("9780141439518", "Pride and Prejudice", "Jane Austen"));
        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() -> bookDataService.findById(unknownId))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void updatesAnExistingBookAndIncrementsItsVersion() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen");
        bookDataService.save(book);
        entityManager.clear();
        assertThat(storedVersion(book.id())).isZero();

        BookDTO changed = new BookDTO(book.id(), book.isbn(), "Pride and Prejudice (Annotated)", book.authors(),
                "HISTORY", new BigDecimal("14.99"), "EUR", 1814, "DISCONTINUED");
        bookDataService.save(changed);

        assertThat(storedVersion(book.id())).isEqualTo(1L);
        entityManager.clear();
        assertThat(bookDataService.findById(book.id())).isEqualTo(changed);
    }

    @Test
    void updatesTheAuthorsOfAnExistingBook() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen", "Anna Quindlen");
        bookDataService.save(book);
        entityManager.clear();

        bookDataService.save(new BookDTO(book.id(), book.isbn(), book.title(),
                List.of("Vivien Jones", "Jane Austen"), book.genre(), book.priceAmount(), book.priceCurrency(),
                book.publicationYear(), book.status()));
        entityManager.clear();

        assertThat(bookDataService.findById(book.id()).authors()).containsExactly("Vivien Jones", "Jane Austen");
        assertThat(storedVersion(book.id())).isEqualTo(1L);
    }

    @Test
    void savingAnUnchangedBookKeepsItsVersion() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen", "Anna Quindlen");
        bookDataService.save(book);
        entityManager.clear();

        bookDataService.save(bookDataService.findById(book.id()));

        assertThat(storedVersion(book.id())).isZero();
    }

    @Test
    void anUpdateNeverChangesTheIsbn() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen");
        bookDataService.save(book);
        entityManager.clear();

        bookDataService.save(new BookDTO(book.id(), "9780141439587", "Emma", book.authors(), book.genre(),
                book.priceAmount(), book.priceCurrency(), book.publicationYear(), book.status()));

        assertThat(jdbcTemplate.queryForObject("select isbn from book where id = ?", String.class, book.id()))
                .isEqualTo("9780141439518");
        assertThat(jdbcTemplate.queryForObject("select title from book where id = ?", String.class, book.id()))
                .isEqualTo("Emma");
    }

    @Test
    void rejectsAStaleUpdate() {
        BookDTO book = fiction("9780141439518", "Pride and Prejudice", "Jane Austen");
        bookDataService.save(book);
        entityManager.clear();
        BookDTO loaded = bookDataService.findById(book.id());
        // Another transaction updates the book after this one loaded it.
        jdbcTemplate.update("update book set version = version + 1 where id = ?", book.id());
        BookDTO changed = new BookDTO(loaded.id(), loaded.isbn(), loaded.title(), loaded.authors(), loaded.genre(),
                new BigDecimal("15.00"), loaded.priceCurrency(), loaded.publicationYear(), loaded.status());

        assertThatThrownBy(() -> bookDataService.save(changed))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void rejectsASecondBookWithTheSameIsbn() {
        bookDataService.save(fiction("9780141439518", "Pride and Prejudice", "Jane Austen"));
        BookDTO sameIsbn = fiction("9780141439518", "Another Pride", "Someone Else");

        // The failed insert leaves the transaction unusable, so nothing touches the database after it.
        assertThatThrownBy(() -> bookDataService.save(sameIsbn))
                .isInstanceOf(IsbnAlreadyExistsException.class)
                .hasMessageContaining("9780141439518");
    }

    @Test
    void searchByTitleIgnoresAuthors() {
        saveAll(fiction("9780000000011", "Persuasion", "Jane Austen"),
                fiction("9780000000012", "Lives", "Persuasion Writer"));

        assertThat(titles(bookDataService.search(null, "suasi", null, null, 100))).containsExactly("Persuasion");
    }

    @Test
    void searchByAuthorIgnoresTitles() {
        saveAll(fiction("9780000000021", "Emma", "Jane Austen"),
                fiction("9780000000022", "Austen Country", "Mary Shelley"));

        assertThat(titles(bookDataService.search(null, null, "austen", null, 100))).containsExactly("Emma");
    }

    @Test
    void searchIsCaseInsensitiveContainsForTitleAuthorAndFreeText() {
        saveAll(fiction("9780000000031", "Pride and Prejudice", "Jane Austen"),
                fiction("9780000000032", "Frankenstein", "Mary Shelley"));

        assertThat(titles(bookDataService.search(null, "D PREJ", null, null, 100)))
                .containsExactly("Pride and Prejudice");
        assertThat(titles(bookDataService.search(null, null, "NE AUS", null, 100)))
                .containsExactly("Pride and Prejudice");
        assertThat(titles(bookDataService.search("rEjUd", null, null, null, 100)))
                .containsExactly("Pride and Prejudice");
        assertThat(titles(bookDataService.search("ARY SHEL", null, null, null, 100)))
                .containsExactly("Frankenstein");
    }

    @Test
    void searchByGenreIsExact() {
        saveAll(book("9780000000041", "Frankenstein", "SCIENCE_FICTION", "Mary Shelley"),
                fiction("9780000000042", "Emma", "Jane Austen"));

        assertThat(titles(bookDataService.search(null, null, null, "FICTION", 100))).containsExactly("Emma");
    }

    @Test
    void searchCombinesCriteriaWithAnd() {
        saveAll(fiction("9780000000051", "Emma", "Jane Austen"),
                book("9780000000052", "The History of England", "HISTORY", "Jane Austen"),
                fiction("9780000000053", "Frankenstein", "Mary Shelley"),
                fiction("9780000000054", "Emma Brown", "Clare Boylan"));

        assertThat(titles(bookDataService.search(null, null, "austen", "FICTION", 100))).containsExactly("Emma");
        assertThat(titles(bookDataService.search(null, "emma", "austen", null, 100))).containsExactly("Emma");
        assertThat(titles(bookDataService.search("emma", null, null, "HISTORY", 100))).isEmpty();
    }

    @Test
    void searchMatchesTitleOrAuthorForFreeText() {
        saveAll(fiction("9780000000061", "Emma", "Jane Austen"),
                book("9780000000062", "Jane Austen: A Life", "HISTORY", "Claire Tomalin"),
                book("9780000000063", "Frankenstein", "SCIENCE_FICTION", "Mary Shelley"));

        assertThat(titles(bookDataService.search("austen", null, null, null, 100)))
                .containsExactly("Emma", "Jane Austen: A Life");
    }

    @Test
    void searchListsEachBookOnce() {
        saveAll(fiction("9780000000071", "Brothers", "Ann Brown", "Bob Brown"),
                fiction("9780000000072", "Emma", "Jane Austen"));

        assertThat(titles(bookDataService.search(null, null, "brown", null, 100))).containsExactly("Brothers");
        assertThat(titles(bookDataService.search("bro", null, null, null, 100))).containsExactly("Brothers");
    }

    @Test
    void searchLimitCountsBooksNotAuthorRows() {
        // Brothers has two matching authors, so a limit applied to joined author rows would stop after Brothers.
        saveAll(fiction("9780000000141", "Brothers", "Ann Brown", "Bob Brown"),
                fiction("9780000000142", "Carrie", "Cy Brown"),
                fiction("9780000000143", "Zed", "Zoe Brown"));

        assertThat(titles(bookDataService.search(null, null, "brown", null, 2))).containsExactly("Brothers", "Carrie");
        assertThat(titles(bookDataService.search("brown", null, null, null, 2))).containsExactly("Brothers", "Carrie");
    }

    @Test
    void searchTreatsPercentLiterally() {
        saveAll(fiction("9780000000081", "100% Pure", "Ann Brown"),
                fiction("9780000000082", "1000 Pure", "Bob Brown"));

        assertThat(titles(bookDataService.search(null, "100%", null, null, 100))).containsExactly("100% Pure");
    }

    @Test
    void searchTreatsUnderscoreLiterally() {
        saveAll(fiction("9780000000091", "Emma", "A_B Collective"),
                fiction("9780000000092", "Persuasion", "AxB Studio"));

        assertThat(titles(bookDataService.search(null, null, "a_b", null, 100))).containsExactly("Emma");
    }

    @Test
    void searchTreatsBackslashLiterally() {
        saveAll(fiction("9780000000101", "Back\\Slash Notes", "Ann Brown"),
                fiction("9780000000102", "Backslash Notes", "Bob Brown"));

        assertThat(titles(bookDataService.search(null, "k\\s", null, null, 100)))
                .containsExactly("Back\\Slash Notes");
    }

    @Test
    void searchWithoutCriteriaReturnsEveryBookSortedByTitle() {
        // Inserted out of title order, and the ISBN order differs from the title order.
        saveAll(fiction("9780000000111", "Wuthering Heights", "Emily Bronte"),
                fiction("9780000000112", "Emma", "Jane Austen"),
                fiction("9780000000113", "Persuasion", "Jane Austen"),
                book("9780000000114", "Frankenstein", "SCIENCE_FICTION", "Mary Shelley"));

        assertThat(titles(bookDataService.search(null, null, null, null, 100)))
                .containsExactly("Emma", "Frankenstein", "Persuasion", "Wuthering Heights");
    }

    @Test
    void searchSortsBooksWithTheSameTitleByIsbn() {
        saveAll(fiction("9780000000122", "Emma", "Jane Austen"),
                fiction("9780000000121", "Emma", "Fay Weldon"));

        assertThat(bookDataService.search(null, null, null, null, 100)).extracting(BookDTO::isbn)
                .containsExactly("9780000000121", "9780000000122");
    }

    @Test
    void searchReturnsAtMostTheFirst100BooksByTitle() {
        // The alphabetically last title goes in first, then the others in reverse title order, so neither the
        // insertion order nor the ISBN order matches the title order.
        int inserted = 0;
        bookDataService.save(fiction(syntheticIsbn(inserted++), bulkTitle(100), "Bulk Author"));
        for (int n = 99; n >= 0; n--) {
            bookDataService.save(fiction(syntheticIsbn(inserted++), bulkTitle(n), "Bulk Author"));
        }

        List<String> found = titles(bookDataService.search(null, null, null, null, 100));

        List<String> first100 = IntStream.range(0, 100).mapToObj(BookDataServiceImplTest::bulkTitle).toList();
        assertThat(found).containsExactlyElementsOf(first100);
        assertThat(found).doesNotContain(bulkTitle(100));
    }

    @Test
    void searchReturnsNoMoreBooksThanTheLimit() {
        saveAll(fiction("9780000000131", "Persuasion", "Jane Austen"),
                fiction("9780000000132", "Emma", "Jane Austen"),
                fiction("9780000000133", "Frankenstein", "Mary Shelley"));

        assertThat(titles(bookDataService.search(null, null, null, null, 2))).containsExactly("Emma", "Frankenstein");
    }

    private void saveAll(BookDTO... books) {
        for (BookDTO book : books) {
            bookDataService.save(book);
        }
    }

    private static List<String> titles(List<BookDTO> books) {
        return books.stream().map(BookDTO::title).toList();
    }

    private static String syntheticIsbn(int n) {
        return String.format("979%010d", n);
    }

    private static String bulkTitle(int n) {
        return String.format("Book %03d", n);
    }

    private long storedVersion(UUID id) {
        return jdbcTemplate.queryForObject("select version from book where id = ?", Long.class, id);
    }

    private static BookDTO fiction(String isbn, String title, String... authors) {
        return book(isbn, title, "FICTION", authors);
    }

    private static BookDTO book(String isbn, String title, String genre, String... authors) {
        return new BookDTO(UUID.randomUUID(), isbn, title, List.of(authors), genre, new BigDecimal("12.50"), "EUR",
                1813, "AVAILABLE");
    }
}
