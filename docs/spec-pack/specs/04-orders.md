# Spec 04: Orders (place and ship orders, stock reservation)

> Module: `bookstore` · Depends on: 03 · Capability: `ordering`

## Why
Ordering is where business rules collide: prices are snapshotted, stock is reserved across several aggregates in one
transaction, and failure must leave nothing half done. It gives the Lab 3 skill exercise ("cancel an order") a real
place to land, and gives the assistant (spec 08) something meaningful to talk about.

## What changes
- Domain: `Order` aggregate, `OrderLine` entity, `OrderId`, `EmailAddress`, `OrderStatus`, event `OrderPlacedEvent`.
- Domain interaction: `OrderFlow`, port `OrderDataService`, `OrderDTO`, `OrderLineDTO`, `OrderDomainMapper`.
- Commands: place an order, ship an order. Queries: one order, orders of a customer.
- Migration `V3__create_orders.sql`.

## Out of scope
- **Cancelling an order and releasing reserved stock.** This is the Lab 3 exercise. Do not add `cancel`,
  `release`, a CANCELLED status or a cancel endpoint. (`OrderStatus` has PLACED and SHIPPED only.)
- Payments, customers as an aggregate, addresses, shipping costs, discounts.

## Domain model
| Type | Kind | Rules |
|---|---|---|
| `OrderId` | record(UUID) | `OrderId.newId()` |
| `EmailAddress` | record(String) | Trimmed, lower-cased, simple pattern `^[^@\s]+@[^@\s]+\.[^@\s]+$`, max 254 |
| `OrderLine` | entity inside `Order` | bookId, isbn, title (snapshots), unitPrice (`Money` snapshot), quantity 1 to 10; `lineTotal()` |
| `Order` | aggregate | id, customerEmail, lines (1 to 20), status, placedAt, shippedAt; `total()` = sum of line totals |
| `OrderPlacedEvent` | record, `DomainEvent` | `UUID orderId`, `String customerEmail`, `BigDecimal total`, `Instant occurredAt` |

Behaviour:
- `Order.place(EmailAddress, List<OrderLineRequest>, Clock)` where `OrderLineRequest` (a domain record) carries the
  snapshot values: rejects duplicate book lines (the flow merges them first), applies the 1 to 10 rule, registers
  `OrderPlacedEvent`.
- `ship(Clock)`: only from PLACED, sets shippedAt; shipping twice throws `OrderAlreadyShippedException`.
- The `java.time.Clock` bean from spec 02 (`ClockConfig` in the root package) is injected into flows so tests control time. Do not declare a second one.

## Use case: place order (`OrderFlow.placeOrder`)
Within one `@Transactional` boundary:
0. Merge duplicate lines per book (quantities summed). A merged quantity outside 1 to 10:
   `InvalidInputException` code `quantity-out-of-range` (422).
1. Load every requested book once (`BookDataService`); unknown book: `BookNotFoundException` (404);
   discontinued book: `BookNotOrderableException` (in `domaininteraction`, extends `ConflictException`, 409).
2. Load each stock item once and call `reserve(quantity)`. Catch the domain `InsufficientStockException` per line,
   collect *all* shortages, then throw `ConflictException` with code `insufficient-stock` and
   `details = [{bookId, requested, available}]` (409).
3. `Order.place(...)` with snapshots of isbn, title and current price.
4. Save the order and the stock items; publish pulled events from the order and from the stock items.
If any step fails, nothing is saved and no event is published.

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /orders/commands/place` | `PlaceOrderPayload { customerEmail, lines: OrderLinePayload[] { bookId, quantity } }` | 202 + `CommandResult { id }` | 400, 404, 409, 422 |
| `POST /orders/{id}/commands/ship` | none | 202 | 404, 409 already shipped |
| `GET /orders/{id}` | | 200 `OrderView { id, customerEmail, status, placedAt, shippedAt, lines: OrderLineView[] { isbn, title, quantity, unitPrice, lineTotal }, total, currency }` | 404 |
| `GET /orders?customerEmail=` | | 200 `OrderSummaryView[] { id, status, placedAt, itemCount, total }` newest first | 422 invalid email (the flow parses `EmailAddress`) |

## Requirements

### Requirement: Place an order
The system SHALL place an order when every requested book exists, is available and has enough stock, reserving the stock and snapshotting prices.

#### Scenario: Successful order
- **GIVEN** book A (12.50, available 5) and book B (8.00, available 2)
- **WHEN** a customer places an order for 2 x A and 1 x B
- **THEN** the response is 202 with an order id, the order total is "33.00", A has reserved 2 and B has reserved 1, and one `OrderPlacedEvent` is published after commit

#### Scenario: Price snapshot
- **GIVEN** an order containing book A at 12.50
- **WHEN** the price of A changes to 15.00
- **THEN** the order still shows unitPrice "12.50"

#### Scenario: Several shortages are reported together
- **GIVEN** book A with available 1 and book B with available 0
- **WHEN** a customer orders 2 x A and 1 x B
- **THEN** the response is 409 with `code` `insufficient-stock` and `details` naming both books with requested and available quantities, and no stock is reserved

#### Scenario: Duplicate lines are merged
- **GIVEN** book A with available 8
- **WHEN** a customer orders book A twice with quantity 3 each
- **THEN** the order has one line for A with quantity 6 and A has reserved 6

#### Scenario: Merged quantity too large
- **WHEN** a customer orders book A twice with quantity 6 each
- **THEN** the response is 422 with `code` `quantity-out-of-range` and nothing is reserved

#### Scenario: Discontinued book
- **GIVEN** book A is discontinued
- **WHEN** a customer orders it
- **THEN** the response is 409 and nothing is reserved

### Requirement: Ship an order
The system SHALL ship a placed order, turning its reservations into stock reductions.

#### Scenario: Shipping reduces stock
- **GIVEN** a placed order for 2 x A, with A onHand 5 and reserved 2
- **WHEN** the order is shipped
- **THEN** the order status is SHIPPED, and A has onHand 3 and reserved 0

#### Scenario: Shipping twice
- **GIVEN** a shipped order
- **WHEN** it is shipped again
- **THEN** the response is 409 and stock is unchanged

### Requirement: Order history per customer
The system SHALL list a customer's orders, newest first, matching the e-mail address case-insensitively.

#### Scenario: Case-insensitive lookup
- **GIVEN** orders placed by "Reader@Example.com"
- **WHEN** a client calls `GET /orders?customerEmail=reader@example.com`
- **THEN** those orders are returned newest first

## Tests (required)
- Domain: `OrderTest` (merge, limits, total, ship rules), `EmailAddressTest`.
- Flow: `OrderFlowTest` with mocked ports (all shortages collected, nothing saved on failure, events published).
- Adapters: `OrderCommandsTest`, `OrderQueriesTest`, `OrderDataServiceImplTest`.
- End to end: `OrderIntegrationTest` for every scenario, including a rollback check (stock unchanged after a 409).

## Tasks
1. Value objects, `OrderLine`, `Order`, `OrderPlacedEvent` with tests.
2. Port, DTOs, mapper, `OrderFlow.placeOrder`, `ship`, `findById`, `findByCustomer`; reuse the `Clock` bean of spec 02
   in the root package.
3. Migration `V3`: `customer_order` (avoid the reserved word `order`), `order_line`.
4. Data, command, query adapters; exception handlers extended for the new exceptions.
5. Integration tests; `http/orders.http`.

## Definition of done
- Scenarios covered; `./mvnw verify` green; ArchUnit green.
- `grep -ri "cancel" bookstore/src/main/java` returns nothing (reserved for Lab 3).
