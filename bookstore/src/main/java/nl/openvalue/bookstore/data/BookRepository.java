package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link BookJPA}. */
public interface BookRepository extends JpaRepository<BookJPA, UUID> {

    Optional<BookJPA> findByIsbn(String isbn);

    /**
     * Searches books. {@code q}, {@code title} and {@code author} are lower-cased {@code like} patterns with
     * {@code \}, {@code %} and {@code _} escaped by a backslash; {@code genre} is an exact genre name; {@code null}
     * means no constraint. The patterns are built by the caller because PostgreSQL cannot type a null parameter
     * that is wrapped in a SQL function such as lower. Authors are matched with {@code exists}, so a book appears
     * once however many of its authors match, and the limit applies to books, not to joined rows.
     */
    @Query("""
            select b from BookJPA b
            where (:q is null
                   or lower(b.title) like :q escape '\\'
                   or exists (select 1 from b.authors qa where lower(qa) like :q escape '\\'))
              and (:title is null or lower(b.title) like :title escape '\\')
              and (:author is null
                   or exists (select 1 from b.authors aa where lower(aa) like :author escape '\\'))
              and (:genre is null or b.genre = :genre)
            order by b.title, b.isbn
            """)
    List<BookJPA> search(@Param("q") String q, @Param("title") String title, @Param("author") String author,
            @Param("genre") String genre, Limit limit);
}
