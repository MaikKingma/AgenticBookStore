---
name: ddd-building-blocks
description: How to model domain code in the bookstore module. Covers aggregates with static factories and behaviour methods, value objects as validated records, domain events, invariants and domain exceptions, DTOs and domain mappers, and how flows translate domain exceptions into the four use-case exceptions. Use when adding or changing an aggregate, value object, domain event, business rule, exception, DTO or mapper.
---

# DDD building blocks

The source of truth is `docs/spec-pack/product/architecture-standard.md`, section 4, plus the domain model table of
the spec you are implementing. If they disagree with this skill, they win: stop and report the difference.

Where each block lives is in the `clean-hexagonal-onion` skill. This skill is about how each block behaves.

## Aggregates (`domain`)

- **Private constructor.** New instances come only from an intention-revealing static factory named after the
  domain: `Book.addToCatalogue(...)`, `StockItem.openFor(BookId)`, `Order.place(...)`.
- **`restore(...)` for rehydration.** A separate static factory used by the mapper when loading from storage. It skips
  creation rules (no new id, no events) but still validates invariants.
- **Behaviour, never setters.** State changes through methods named after the domain: `receive`, `reserve`, `ship`,
  `changePrice`, `discontinue`. No `setX`, no public mutable fields, getters only where a DTO or mapper needs them.
- **Invariants inside.** Every rule from the spec's domain model table is checked inside the aggregate. A violation
  throws a domain exception named after the broken rule.
- **Time comes in.** Behaviour that needs the current time gets it from a `java.time.Clock`, never from
  `Instant.now()` or `Year.now()` directly. The spec's signatures win: where the spec names a `Clock` parameter
  (`ship(Clock)`, `Order.place(..., Clock)`), take it there; where the spec's signature has no `Clock` but the
  behaviour needs the time (spec 03: `reserve` and `correct` stamp `occurredAt`; spec 02: the publication-year
  upper bound), the aggregate receives the `Clock` through its factories (`openFor`, `addToCatalogue`, `restore`)
  and the flow passes its injected `Clock`. Tests use a fixed clock either way.
- **Events are registered, not published.** The aggregate adds events to an internal list.
  `pullDomainEvents()` returns them and clears the list.

Shape, using `StockItem` from spec 03:

```java
public final class StockItem {

    private final BookId bookId;
    private int onHand;
    private int reserved;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private StockItem(BookId bookId, int onHand, int reserved /* , ... */) {
        // assign fields, then check invariants that must always hold
    }

    public static StockItem openFor(BookId bookId) {
        return new StockItem(bookId, 0, 0 /* , defaults from the spec */);
    }

    public static StockItem restore(BookId bookId, int onHand, int reserved /* , ... */) {
        return new StockItem(bookId, onHand, reserved /* , ... */); // validates, raises no events
    }

    public void reserve(Quantity quantity) {
        if (available() < quantity.value()) {
            throw new InsufficientStockException(bookId, quantity.value(), available());
        }
        reserved += quantity.value();
        // register StockRunningLowEvent when the spec's rule says so
    }

    public int available() {
        return onHand - reserved;
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
```

Fields, defaults and exact rules always come from the spec. The code above shows the shape, not the content.

## Value objects (`domain`)

- Java `record`s with validation in the compact constructor. Invalid input throws a domain exception
  (`InvalidIsbnException`).
- Normalise in the compact constructor where the spec says so (`Isbn` strips hyphens and spaces).
- Small factories and operations are welcome: `Money.of("12.99")`, `plus`, `times(int)`, `BookId.newId()`.
- Money is `BigDecimal` with scale 2 and `RoundingMode.HALF_EVEN`, currency EUR. Never `double` or `float`.
- Prefer a value object over a primitive as soon as a value has a rule (`Quantity` instead of `int`).

## Domain events (`domain`)

- Records that implement the marker interface `DomainEvent`, named after what happened, with the suffix `Event`,
  exactly as the spec names them (`StockRunningLowEvent`, `OrderPlacedEvent`).
- Fields use JDK types only (`UUID`, `int`, `BigDecimal`, `String`, `Instant occurredAt`). The `process` layer may
  depend on the event class and on nothing else from `domain`.
- Raised once per meaningful change, as the spec defines (for example "once per dip below the threshold").

## Domain exceptions (`domain`)

- Unchecked (`extends RuntimeException`), named after the broken rule (`InsufficientStockException`,
  `BookDiscontinuedException`, `OrderAlreadyShippedException`).
- They carry the facts needed to explain the failure (ids, requested and available amounts), not HTTP concerns.
- They never reach an adapter: the flow translates them (below).

## Flows translate at the use-case boundary (`domaininteraction`)

| Use-case exception | HTTP | Use for |
|---|---|---|
| `NotFoundException` | 404 | the thing addressed does not exist |
| `ConflictException` | 409 | the request clashes with the current state (discontinued, already shipped, insufficient stock) |
| `InvalidInputException` | 422 | a value breaks a domain rule (invalid ISBN, price out of range, unknown genre) |
| `UnavailableException` | 503 | a dependency is down (distributor, LLM) |

Each has a stable kebab-case `code` (`isbn-already-exists`), a short `title`, a `detail` message and optional
`details` (a list of key/value maps). Port exceptions are nested in their port and extend one of these four
(`BookDataService.BookNotFoundException`). Bean validation failures on payloads are not use-case exceptions; the
adapter's `*ExceptionHandler` turns them into 400 with `code = validation-failed`.

## Flow shape (`domaininteraction`)

```java
@Service
@Transactional
public class InventoryFlow {

    // constructor injection: ports, mappers, ApplicationEventPublisher, Clock if needed

    // illustrative only: spec 04 puts reservation in OrderFlow.placeOrder and collects all shortages first
    public void reserveStock(UUID bookId, int quantity) {          // JDK types in
        StockItem item = mapper.toDomain(stockItems.findByBookId(bookId)); // port throws its NotFound exception
        try {
            item.reserve(new Quantity(quantity));                  // the flow creates value objects
        } catch (InsufficientStockException e) {
            throw new ConflictException(/* code, title, detail, details as the spec defines */);
        }
        stockItems.save(new StockItemDTO(item));                   // DTO out through the port
        item.pullDomainEvents().forEach(events::publishEvent);     // same transaction, listeners run after commit
    }
}
```

- One flow per aggregate or business capability (`CatalogueFlow`, `InventoryFlow`, `OrderFlow`).
- Public methods are use cases. Parameters and return values are JDK types or DTOs.
- Business decisions belong in the aggregate. If a flow method grows `if` statements about business rules, move them
  into domain behaviour.

## DTOs and mappers (`domaininteraction`)

- `*DTO` records with JDK-typed components only (a price is `BigDecimal amount` plus `String currency`). They never
  carry domain types or events.
- A DTO has a convenience constructor from its aggregate: `new BookDTO(book)`.
- The reverse direction is a hand-written `*DomainMapper` that calls the aggregate's `restore(...)`.

## Review checklist before you move on

- [ ] No setters, no public constructors on aggregates.
- [ ] Every invariant from the spec's table has a domain test that proves the exception.
- [ ] Events are records with JDK types, implement `DomainEvent`, and are pulled and published by the flow.
- [ ] No domain type in any port, DTO, payload or view.
- [ ] Every domain exception a use case can hit is translated into a use-case exception with the spec's `code`.
- [ ] Money uses `Money`; time uses `Clock`.
