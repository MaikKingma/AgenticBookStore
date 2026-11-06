# Spec 07: Restock process adapter (event-driven restocking)

> Module: `bookstore` · Depends on: 03, 04, 06 · Capability: `restocking`

## Why
The `process` layer is the least familiar part of the architecture for most developers and for agents: an inbound
adapter driven by domain events instead of HTTP. When stock runs low, the bookstore should automatically ask the
distributor for more. It shows why events are published after commit and why the follow-up runs in its own transaction.

## What changes
- `StockItem` gains a pending restock: `restockRequested(restockOrderId, quantity, expectedDeliveryDate, backordered)`;
  `receive(...)` clears it; `hasPendingRestock()`.
- Two `InventoryFlow` use cases sharing one private implementation (reorderQuantity from the stock item, ISBN from
  the book, reference `"<bookId>/<epoch-seconds>"`):
  - `handleStockRunningLow(UUID bookId)`: the event-driven path. Catches the port exceptions itself (see Behaviour),
    so no exception crosses the transactional proxy and the transaction is never marked rollback-only.
  - `requestRestock(UUID bookId)`: the manual path. Lets port exceptions propagate (503 / 404 / 422 to the caller).
- `process` layer: `EventProcessor` listens to `StockRunningLowEvent` after commit and hands it to
  `RequestRestockDelegate`, which runs in a new transaction (`REQUIRES_NEW`) and calls exactly one flow method:
  `InventoryFlow.handleStockRunningLow(event.bookId())`.
- Manual retry command `POST /inventory/{bookId}/commands/request-restock`.
- `StockView` gains `pendingRestock: PendingRestockView { restockOrderId, quantity, expectedDeliveryDate, backordered }`
  (null when none).
- Migration `V4__add_pending_restock.sql`.

## Out of scope
- Asynchronous execution (`@Async`), retries with backoff, outbox pattern. The listener runs synchronously after
  commit, so the HTTP response that triggered the event waits for the restock call. This is a conscious trade-off
  for simplicity and testability; document it in `docs/architecture.md`.

## Behaviour
- No restock is requested while one is pending (`requestRestock` returns without calling the distributor).
- In `handleStockRunningLow`, when the distributor is unavailable (`DistributorUnavailableException`), the flow logs a
  WARN with the book id and resets `lowStockSignalled` on the stock item in the same transaction, so the next
  reservation triggers a new attempt. The original order is never affected.
- `TitleNotOrderableException` or `TitleNotFoundAtDistributorException` (for example a bookstore-only title): log
  WARN, keep `lowStockSignalled` true (no automatic retries).
- Manual `request-restock`: 202 when a restock was requested or one is already pending; 503 when the distributor is
  unavailable; 404 when the title is unknown at the distributor; 422 when it is not orderable.
- Test isolation: from this spec on, any test that reserves stock can trigger a restock. `IntegrationTestBase`
  (spec 01) gets a `@MockitoBean DistributorAppService` with a default answer (CONFIRMED, delivery in 2 days), so no
  test ever calls a real distributor, including when one is running locally during Lab 3.

## Requirements

### Requirement: Low stock triggers a restock request after commit
The system SHALL request a restock from the distributor when a `StockRunningLowEvent` is published, only after the triggering transaction committed.

#### Scenario: Order drives stock below reorder level
- **GIVEN** book A with onHand 4, reserved 0, reorderLevel 3, reorderQuantity 10, and a distributor that confirms restocks
- **WHEN** a customer orders 2 x A
- **THEN** the order succeeds, the distributor receives one restock order for 10 units of A's ISBN, and `GET /inventory/{A}` shows a pending restock with the expected delivery date

#### Scenario: Rolled back transaction triggers nothing
- **GIVEN** an order that fails with 409 because another line is short
- **WHEN** the order transaction rolls back
- **THEN** no restock request is sent

### Requirement: One pending restock at a time
The system SHALL not request a second restock while one is pending.

#### Scenario: Manual retry while pending
- **GIVEN** book A has a pending restock
- **WHEN** a client posts `request-restock` for A
- **THEN** the response is 202 and the distributor is not called

#### Scenario: Manual retry while the distributor is down
- **GIVEN** book A has no pending restock and the distributor is unavailable
- **WHEN** a client posts `request-restock` for A
- **THEN** the response is 503 with `code` `distributor-unavailable`

### Requirement: Distributor failures never break orders
A failing restock request SHALL not affect the order that triggered it and SHALL allow a later retry.

#### Scenario: Distributor down
- **GIVEN** the distributor returns 503
- **WHEN** an order drives A below its reorder level
- **THEN** the order response is 202, a WARN is logged, A has no pending restock, and the next reservation of A triggers a new attempt

### Requirement: Receiving stock completes a restock
Receiving stock SHALL clear a pending restock.

#### Scenario: Delivery arrives
- **GIVEN** A has a pending restock for 10 units
- **WHEN** 10 units are received
- **THEN** A has no pending restock and available stock increased by 10

## Tests (required)
- `StockItemTest` additions (pending restock rules).
- `InventoryFlowTest` additions (pending check, exception handling per exception type in `handleStockRunningLow`,
  propagation in `requestRestock`).
- `EventProcessorTest` (delegates the event), `RequestRestockDelegateTest` (calls exactly one flow method).
- `RestockProcessIntegrationTest` (extends `IntegrationTestBase`; programs the mocked `DistributorAppService`): every
  scenario above, including the rollback case and a check that the order's own data is committed when the
  distributor is down.
- Update `OrderIntegrationTest`, `InventoryIntegrationTest` and `CatalogueIntegrationTest` if they relied on the
  absence of restock behaviour; all must pass with the default mocked port.
- ArchUnit: the documented `process` to `domain` event exception is named and commented; a fixture proves `process`
  may not use any other domain class.

## Tasks
1. Domain changes and tests.
2. Flow methods `handleStockRunningLow`, `requestRestock`; default mocked port in `IntegrationTestBase`; migration `V4`.
3. `EventProcessor`, `RequestRestockDelegate` (with `REQUIRES_NEW`).
4. Manual retry endpoint, `PendingRestockView`, `StockView` extension.
5. Integration test, ArchUnit exception and fixture, `docs/architecture.md` trade-off note, `http/inventory.http` update.

## Definition of done
- Scenarios covered; `./mvnw verify` green; ArchUnit green with exactly one documented exception.
