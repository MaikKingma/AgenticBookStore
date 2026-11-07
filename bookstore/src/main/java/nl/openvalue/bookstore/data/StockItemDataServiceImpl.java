package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService;
import nl.openvalue.bookstore.domaininteraction.StockItemDataService.StockItemNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Stores stock items with JPA, one per book, keyed by the book id. It runs inside the caller's transaction (the
 * flow's) and maps to DTOs before it returns.
 */
@Component
public class StockItemDataServiceImpl implements StockItemDataService {

    private final StockItemRepository repository;

    public StockItemDataServiceImpl(StockItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(StockItemDTO stockItem) {
        repository.findById(stockItem.bookId())
                .ifPresentOrElse(stored -> update(stored, stockItem), () -> insert(stockItem));
    }

    /** Changes the managed entity, so Hibernate checks its version; the flush surfaces a stale update here. */
    private void update(StockItemJPA stored, StockItemDTO stockItem) {
        StockItemJPAMapper.copyInto(stockItem, stored);
        repository.saveAndFlush(stored);
    }

    /**
     * The flush surfaces a missing book here, not later at commit. The foreign-key violation is not translated:
     * the flows only save stock items of books that exist.
     */
    private void insert(StockItemDTO stockItem) {
        repository.saveAndFlush(StockItemJPAMapper.toNewEntity(stockItem));
    }

    @Override
    public StockItemDTO findByBookId(UUID bookId) {
        return repository.findById(bookId).map(StockItemJPAMapper::toDTO)
                .orElseThrow(() -> new StockItemNotFoundException(bookId));
    }

    @Override
    public List<StockItemDTO> findLowStock() {
        return repository.findLowStock().stream().map(StockItemJPAMapper::toDTO).toList();
    }
}
