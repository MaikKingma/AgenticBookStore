# Spec 03: Inventory (stock levels, receiving stock, low-stock event)

> Module: `bookstore` · Depends on: 02 · Capability: `inventory`

## Why
Orders (spec 04) need stock reservations, and the restock process (spec 07) needs a domain event. Inventory is a
separate aggregate from `Book`, which teaches agents (and participants) that one flow may coordinate two aggregates
while each aggregate guards its own invariants.

## What changes
- Domain: `StockItem` aggregate, value object `Quantity`, event `StockRunningLowEvent`, exception `InsufficientStockException`.
- Domain interaction: `InventoryFlow`, port `StockItemDataService`, `StockItemDTO`, `StockItemDomainMapper`.
- `CatalogueFlow.addBook` also creates an empty stock item for the new book (same transaction).
- Commands: receive stock, correct stock after a stocktake. Queries: stock for a book, books below reorder level.
- `BookView` gains `available` and `inStock`.
- Migration `V2__create_inventory.sql`.

## Out of scope
- Restock ordering at the distributor (specs 06 and 07). Releasing reservations (Lab 3 exercise, see spec 04).
- Multiple warehouses.

## Domain model
| Type | Kind | Rules |
|---|---|---|
| `Quantity` | record(int) | 0 to 10 000 |
| `StockItem` | aggregate | Fields: bookId, onHand, reserved, reorderLevel (default 3), reorderQuantity (default 10), lowStockSignalled (boolean). Derived: `available = onHand - reserved` |
| `StockRunningLowEvent` | record, implements marker interface `DomainEvent` | `UUID bookId`, `int available`, `int reorderQuantity`, `Instant occurredAt` (JDK types only, see the architecture standard) |

Behaviour on `StockItem`:
- `StockItem.openFor(BookId)`: onHand 0, reserved 0, lowStockSignalled false.
- `receive(Quantity q)`: q between 1 and 1000; onHand += q; if available is now above reorderLevel, reset lowStockSignalled.
- `reserve(Quantity q)`: requires available >= q, otherwise `InsufficientStockException(bookId, requested, available)`; reserved += q.
- `ship(Quantity q)`: requires reserved >= q; onHand -= q; reserved -= q.
- `correct(Quantity newOnHand, String reason)`: reason 3 to 200 characters; newOnHand must be >= reserved.
- After `reserve` and `correct`: if available <= reorderLevel and not lowStockSignalled, register
  `StockRunningLowEvent` and set lowStockSignalled. The event is raised once per dip below the threshold.
- `pullDomainEvents()` returns and clears registered events.

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /inventory/{bookId}/commands/receive` | `ReceiveStockPayload { quantity }` | 202 | 404, 400 |
| `POST /inventory/{bookId}/commands/correct` | `CorrectStockPayload { onHand, reason }` | 202 | 404, 400, 409 below reserved |
| `GET /inventory/{bookId}` | | 200 `StockView { bookId, onHand, reserved, available, reorderLevel, lowStock }` | 404 |
| `GET /inventory?lowStock=true` | | 200 `StockView[]` | |

`lowStock` means `available <= reorderLevel` (the same threshold that raises the event). Flow methods take
`UUID bookId` and `int` quantities; the flow creates `BookId` and `Quantity`.

`BookQueries` now combines `CatalogueFlow` and `InventoryFlow` results into `BookView` (query adapter composes, flows
stay independent).

## Requirements

### Requirement: Every book has a stock item
The system SHALL create a stock item with zero stock whenever a book is added to the catalogue.

#### Scenario: New book has no stock
- **WHEN** a book is added
- **THEN** `GET /inventory/{bookId}` returns onHand 0, reserved 0, available 0, and the book's `BookView` shows `inStock: false`

### Requirement: Receive stock
The system SHALL increase stock on hand when stock is received.

#### Scenario: Receiving stock
- **GIVEN** a book with onHand 2
- **WHEN** 5 units are received
- **THEN** onHand is 7, available is 7, and the book's `BookView` shows `available: 7, inStock: true`

#### Scenario: Invalid quantity
- **WHEN** 0 or 1001 units are received
- **THEN** the response is 400 and stock is unchanged

### Requirement: Reservations never exceed availability
The `StockItem` SHALL reject reservations larger than the available quantity.

#### Scenario: Insufficient stock
- **GIVEN** onHand 3 and reserved 2
- **WHEN** 2 units are reserved
- **THEN** `InsufficientStockException` is thrown with requested 2 and available 1, and reserved stays 2

### Requirement: Low stock is signalled once per dip
The `StockItem` SHALL register exactly one `StockRunningLowEvent` when available stock drops to or below the reorder level, and SHALL not register another until stock has been received above the reorder level.

#### Scenario: Crossing the threshold (equality counts)
- **GIVEN** onHand 5, reserved 0, reorderLevel 3
- **WHEN** 2 units are reserved
- **THEN** available is 3, `lowStock` is true, and one `StockRunningLowEvent` with available 3 and reorderQuantity 10 is registered

#### Scenario: No duplicate events
- **GIVEN** a stock item that already signalled low stock
- **WHEN** another unit is reserved
- **THEN** no new event is registered

#### Scenario: Reset after receiving
- **GIVEN** a stock item that signalled low stock with available 2
- **WHEN** 10 units are received and later stock drops to the reorder level again
- **THEN** a new event is registered

### Requirement: Events are published after commit
`InventoryFlow` (and every flow that changes a stock item) SHALL publish pulled domain events through `ApplicationEventPublisher` inside the use-case transaction.

#### Scenario: Event reaches a listener after commit
- **GIVEN** a test `@TransactionalEventListener(phase = AFTER_COMMIT)` bean in the test sources
- **WHEN** a stocktake correction lowers available stock to the reorder level
- **THEN** the listener receives one `StockRunningLowEvent` after the transaction commits, and none if the transaction rolls back

## Tests (required)
- `StockItemTest` covering every behaviour and the event rules, `QuantityTest`.
- `InventoryFlowTest` (mocked port and publisher: events published after save).
- `InventoryCommandsTest`, `InventoryQueriesTest`, `StockItemDataServiceImplTest`.
- `InventoryIntegrationTest` including the after-commit and rollback scenarios.
- Update `CatalogueIntegrationTest`: new books show `inStock: false`.

## Tasks
1. `Quantity`, `DomainEvent` marker interface, `StockRunningLowEvent`, `StockItem` with tests.
2. Port, DTO, mapper, `InventoryFlow` (`receive`, `correct`, `findByBookId`, `findLowStock`).
3. Extend `CatalogueFlow.addBook` to open a stock item.
4. Migration `V2`: table `stock_item` (`book_id uuid pk fk book`, `on_hand`, `reserved`, `reorder_level`,
   `reorder_quantity`, `low_stock_signalled`, `version`).
5. Data adapter, command and query adapters, `BookView` extension.
6. Integration tests and `http/inventory.http`.

## Definition of done
- Scenarios covered; `./mvnw verify` green; ArchUnit green (events live in `domain`, publishing happens in flows).
