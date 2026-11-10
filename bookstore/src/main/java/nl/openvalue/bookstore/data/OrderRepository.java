package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link OrderJPA}. */
public interface OrderRepository extends JpaRepository<OrderJPA, UUID> {

    /**
     * Every order whose stored e-mail address equals the given one exactly, with its lines fetched in the same
     * statement: newest first, a tie on the placed time broken by id, descending. The comparison is plain equality,
     * because the stored address is already normalised. Hibernate returns each order once; no limit applies, because
     * a limit together with a fetch join is ignored.
     */
    @Query("select o from OrderJPA o join fetch o.lines where o.customerEmail = :customerEmail order by o.placedAt desc, o.id desc")
    List<OrderJPA> findByCustomerEmailNewestFirst(@Param("customerEmail") String customerEmail);
}
