package nl.openvalue.bookstore.data;

import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.OrderDTO;
import nl.openvalue.bookstore.domaininteraction.OrderDataService;
import nl.openvalue.bookstore.domaininteraction.OrderDataService.OrderNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Stores orders with JPA, each with its lines. It runs inside the caller's transaction (the flow's): the lines are
 * lazy, so every method maps them to DTOs before it returns.
 */
@Component
public class OrderDataServiceImpl implements OrderDataService {

    private final OrderRepository repository;

    public OrderDataServiceImpl(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(OrderDTO order) {
        repository.findById(order.id())
                .ifPresentOrElse(stored -> update(stored, order), () -> insert(order));
    }

    /** Changes the managed entity, so Hibernate checks its version; the flush surfaces a stale update here. */
    private void update(OrderJPA stored, OrderDTO order) {
        OrderJPAMapper.copyInto(order, stored);
        repository.saveAndFlush(stored);
    }

    /**
     * The flush surfaces a constraint violation here, not later at commit. It is not translated: the flows only save
     * orders whose books exist and whose lines name each book once.
     */
    private void insert(OrderDTO order) {
        repository.saveAndFlush(OrderJPAMapper.toNewEntity(order));
    }

    @Override
    public OrderDTO findById(UUID id) {
        return repository.findById(id).map(OrderJPAMapper::toDTO).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public List<OrderDTO> findByCustomerEmail(String customerEmail) {
        return repository.findByCustomerEmailNewestFirst(customerEmail).stream().map(OrderJPAMapper::toDTO).toList();
    }
}
