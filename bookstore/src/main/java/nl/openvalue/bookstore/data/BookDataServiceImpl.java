package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.BookDataService.BookNotFoundException;
import nl.openvalue.bookstore.domaininteraction.BookDataService.IsbnAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * Stores books with JPA. It runs inside the caller's transaction (the flow's): authors are lazy, so every method maps
 * to DTOs before it returns.
 */
@Component
public class BookDataServiceImpl implements BookDataService {

    private final BookRepository repository;

    public BookDataServiceImpl(BookRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(BookDTO book) {
        repository.findById(book.id())
                .ifPresentOrElse(stored -> update(stored, book), () -> insert(book));
    }

    /** Changes the managed entity, so Hibernate checks its version; the flush surfaces a stale update here. */
    private void update(BookJPA stored, BookDTO book) {
        BookJPAMapper.copyInto(book, stored);
        repository.saveAndFlush(stored);
    }

    /** The flush surfaces the unique ISBN constraint here, not later at commit. */
    private void insert(BookDTO book) {
        try {
            repository.saveAndFlush(BookJPAMapper.toNewEntity(book));
        } catch (DataIntegrityViolationException e) {
            throw new IsbnAlreadyExistsException(book.isbn());
        }
    }

    @Override
    public BookDTO findById(UUID id) {
        return repository.findById(id).map(BookJPAMapper::toDTO).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Override
    public Optional<BookDTO> findByIsbn(String isbn) {
        return repository.findByIsbn(isbn).map(BookJPAMapper::toDTO);
    }

    @Override
    public List<BookDTO> search(String q, String title, String author, String genre, int limit) {
        return repository.search(containsPattern(q), containsPattern(title), containsPattern(author), genre,
                        Limit.of(limit))
                .stream()
                .map(BookJPAMapper::toDTO)
                .toList();
    }

    /**
     * A lower-cased {@code like} pattern that matches the value anywhere, with the value's own backslash, percent
     * and underscore taken literally. The backslash is escaped first, so the escapes added after it stay intact.
     */
    private static String containsPattern(String value) {
        if (value == null) {
            return null;
        }
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
