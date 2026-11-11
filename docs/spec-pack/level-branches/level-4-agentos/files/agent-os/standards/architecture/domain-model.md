# Domain Model

Aggregates (`Book`, `Order`, `StockItem`):

- Private constructor. Create through an intention-revealing static factory: `Book.addToCatalogue(...)`, `Order.place(...)`.
- Load from storage with a separate static `restore(...)`. It skips the creation rules but still checks the invariants.
- Change state through behaviour methods named after the business (`receive`, `reserve`, `ship`). No setters.
- Check invariants inside the aggregate. A violation throws an unchecked exception that lives in `domain`.
- An aggregate that raises domain events (`Order`, `StockItem`) collects them in a list inside the aggregate. `pullDomainEvents()` returns them and clears the list.

Domain events are records with JDK types only, so `process` needs nothing else from `domain`:

```java
public record StockRunningLowEvent(UUID bookId, int available, int reorderQuantity, Instant occurredAt)
```

Value objects (`Isbn`, `Money`, `Quantity`) are records that validate in the compact constructor.

No Lombok and no MapStruct: records and plain Java.
