# References for Spec 04 Orders

## Similar Implementations

The spec 02 catalogue slice and the spec 03 inventory slice are the patterns every spec 04 class mirrors:

- `bookstore/src/main/java/nl/openvalue/bookstore/domain/Book.java`, `StockItem.java`, `Money.java`, `Quantity.java`, `BookId.java` and the domain exceptions: aggregates with private constructors and factories, an internal domain-event list, validated records, exceptions extending `RuntimeException` with Javadoc "Thrown when". `StockItem.reserve` and `ship` are called by `OrderFlow`; only `reserve` can register a `StockRunningLowEvent`.
- `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/InventoryFlow.java` and `CatalogueFlow.java` (private translator, load, translate, save, publish), `BookDataService.java`, `StockItemDataService.java` (nested port exceptions), `BookDTO.java`, `StockItemDTO.java`, the static domain mappers, and the four use-case exceptions (`details` is `List<Map<String, String>>`).
- `bookstore/src/main/java/nl/openvalue/bookstore/command/BookCommands.java` (`CommandResult`), `InventoryCommands.java`, `ReceiveStockPayload.java`, `CommandExceptionHandler.java` (the validation 400 handler, package-scoped through `basePackageClasses`, unchanged in spec 04).
- `bookstore/src/main/java/nl/openvalue/bookstore/query/InventoryQueries.java` (nested 400 exception), `StockView.java`, `QueryExceptionHandler.java` (the `problem(...)` helper the new 400 method reuses).
- `bookstore/src/main/java/nl/openvalue/bookstore/data/BookJPA.java` (its element collection for authors), `BookDataServiceImpl.java`, `StockItemDataServiceImpl.java`, the JPA mappers and repositories, and `bookstore/src/main/resources/db/migration/V1__create_catalogue.sql`, `V2__create_inventory.sql` (DDL style and constraint names `pk_`, `fk_`, `uq_`).
- Tests at every layer: `StockItemTest`, `InventoryFlowTest`, `InventoryCommandsTest`, `InventoryQueriesTest`, `StockItemDataServiceImplTest`, `InventoryIntegrationTest`, `StockEventRecorder`, `IntegrationTestBase`, `IntegrationTestBaseHooksTest`, `PostgresTestConfig`, `DatabaseCleaner`.
- `http/inventory.http`: the request collection style.

## Research used for shaping (session artifacts, not tracked)

The reports live under `.claude/comms/mailboxes/<id>/outbox/` of the factory session that shaped this spec. Their load-bearing findings are restated here so the spec stands on its own.

### Codebase facts spec 04 builds on (res-1, read from the code)

Reusable as is: `StockItem.reserve` and `ship`, `Money.times(int)` and `plus` (no zero constant; `Money.of("0.00")`), `Quantity` 0 to 10000 (so `reserve(0)` succeeds and the 1 to 10 rule lives in the order), `BookDTO` with price and status, the four use-case exceptions, `IntegrationTestBase` with the stock spy. Gaps the spec settles: `StockEventRecorder` records only `StockRunningLowEvent`; integration test helpers are private copies per class; `BookNotFoundException` and `StockItemNotFoundException` both use `book-not-found`; optimistic locking failures are untranslated (500); `InsufficientStockException` and `InsufficientReservedStockException` were translated nowhere before spec 04; no view rendered an `Instant` and no `timestamptz` column existed. All planned class names pass the suffix rules (21 rules, 40 self-tests, no change). Referee baseline after spec 03: bookstore 476 tests, distributor 3, 7 Spring `Started` lines in the bookstore section, 1 container, about 17 s. The cancel grep was empty; `release` and `CANCELLED` did not occur in bookstore main.

### Order persistence and the multi-aggregate transaction (res-2, senior, probed in a scratch Boot 4.1.2 project with Hibernate 7.4.13 on postgres:18.6 and the real V0 to V2)

`OrderJPA` on `customer_order` with the lines as an element collection of an embeddable `OrderLineJPA` in `order_line` and `@OrderColumn(name = "position")` validates, keeps the line order on reload, and ships with one `update customer_order ... where id=? and version=?` that never touches `order_line`; rewriting the lines, even with equal values, bumps the version, so the update path copies only status and shipped time. PostgreSQL rounds nanoseconds (`.123456789` reloads as `.123457`; `.999999999` as the next second) and Linux clocks deliver them, so the domain truncates to microseconds. `placeOrder` writes nothing before the 409. A test recorder with a before-commit failure toggle rolls back order, lines and stock (after rollback it saw one order event and two stock events). Saving stock items in opposite orders deadlocks two concurrent orders for about one second (`CannotAcquireLockException`); sorted by book id, the loser gets `ObjectOptimisticLockingFailureException`. `uq_order_line_book` and `fk_order_line_book` fire by name. Hibernate 7 drops duplicate roots of a `join fetch` and keeps newest first with the id tiebreak in one statement; a Spring Data `Limit` combined with `join fetch` is silently ignored. A lookup with the raw mixed-case address finds nothing, so the flow normalises through `EmailAddress` and the query uses plain equality. Test traps: truncate a bracket's lower bound to microseconds; set `placed_at` by `JdbcTemplate` for the newest-first test; ship the order before reading stock so a second ship leaves stock, shipped time and version unchanged.

### Cross-spec constraints (res-3, read from the pack documents)

Spec 12 expects 400 `validation-failed` with field `customerEmail` on place, 409 `insufficient-stock` with details `[{bookId, requested, available}]`, the `OrderView` and `OrderSummaryView` field names of spec 04, and mirrors the 1 to 10 quantity and 20 line limits. Spec 10 names `OrderCommands`, `PlaceOrderPayload`, `OrderFlow`, `Order`, `OrderDataServiceImpl` and `OrderIntegrationTest` and copies spec 04's requirements into the participants' repo. Spec 09 seeds orders straight into `customer_order` and `order_line` (lower-case e-mail, status text, reserved stock equal to the PLACED line sums). Spec 07 needs an all-or-nothing place with stock events published inside the transaction. Spec 08 has no order tools. The Level 3 names exist only in spec 10 and must be absent from main code. `itemCount` is defined nowhere.

### Tech-lead strategy (lead-1)

Approach A: layered mirror, inside out, falsifier tests first, with the bean-order flip (the data adapter merges before `OrderFlow`). Eight tasks T1 to T8, binding decisions B1 to B14, the ProblemDetail contract, the scenario-to-test map in `shape.md` and 22 mutation probes. Contradiction check: none blocks.
