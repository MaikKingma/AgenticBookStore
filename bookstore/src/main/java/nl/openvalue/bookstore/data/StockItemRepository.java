package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data repository for {@link StockItemJPA}. */
public interface StockItemRepository extends JpaRepository<StockItemJPA, UUID> {

    /**
     * Every stock item whose available stock (on hand minus reserved) is at or below its own reorder level, the most
     * urgent first: ordered by available stock ascending, then by book id.
     */
    @Query("select s from StockItemJPA s where s.onHand - s.reserved <= s.reorderLevel order by s.onHand - s.reserved, s.bookId")
    List<StockItemJPA> findLowStock();
}
