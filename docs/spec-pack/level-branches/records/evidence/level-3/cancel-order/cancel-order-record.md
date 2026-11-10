# Level 3 exercise reference: cancel order (T9 record)

Use case: "Customers must be able to cancel an order that has not shipped yet." Built directly by the implementer,
test first, through the `hexagonal-slice` recipe and the staged `new-use-case/reference.md` rows (amendment A2: no
Claude Code session; the skill-selection and `/ship` scenarios are rehearsal rows of T10). Paths are masked: `$SBX`
is the exercise sandbox, `$RB` the fresh rebuild, `$SP` the scratchpad, `$WT` the worktree.

## Provenance

| item | value | evidence |
|---|---|---|
| level-0-start | b668073eb44bfea453c291509a615f6de081e54a | sandbox-build.txt |
| simulated level-0 commit | 391d5f74d69591a8e7a4b3cf718a07fe9dbae03c | sandbox-build.txt |
| sandbox after build-level level-3 (config rows only at that time) | 163e5d1a6aea758293e4c19370bcac97ceb24606 | sandbox-build.txt |
| sandbox exercise commit (git hook active) | 608f819e253892b7d302465d74b730141996ee9e, subject `feat(orders): cancel an order that has not shipped yet` | sandbox-commit.txt |
| tree of the exercise commit and of the fresh rebuild | 309786e063b813e0f9248714f404b927d277d879 (both) | staging-and-rebuild.txt |

## New classes and the reference.md rows

The staged `reference.md` is the source for package and suffix. T9 adds one class and one migration; every changed
class keeps its package and suffix.

| new file | package | reference.md row or rule | suffix |
|---|---|---|---|
| `domain/OrderAlreadyCancelledException.java` | `domain` | row `domain`: "no suffix rule: name after the business"; rule "Make an invariant throw an unchecked domain exception" | `Exception`, like the existing `OrderAlreadyShippedException`; extends `RuntimeException`, imports nothing outside `java..` |
| `db/migration/V5__add_order_cancelled_at.sql` | `bookstore/src/main/resources/db/migration` | rule "Change the schema only through a new Flyway migration `V{n}__{description}.sql`" | not a class |

| changed class | package (row) | suffix the row names |
|---|---|---|
| `OrderCommands` | `command` | `Commands` |
| `OrderView` | `query` | `View` |
| `OrderFlow`, `OrderDTO`, `OrderDomainMapper`, `OrderDataService` | `domaininteraction` | `Flow`, `DTO`, `DomainMapper`, `DataService` |
| `OrderJPA`, `OrderJPAMapper` | `data` | `JPA`, `JPAMapper` |
| `Order`, `OrderStatus`, `StockItem`, `OrderAlreadyShippedException`, `InsufficientReservedStockException` | `domain` | no suffix rule |

ArchitectureTest: 22 tests, 0 failures, 0 errors in `$SBX` (tdd-red-green.txt, last section) and in `$RB`
(staging-and-rebuild.txt, Surefire XML).

## Behaviour and where it is tested

| rule | tests |
|---|---|
| `Order.cancel(Clock)`: status CANCELLED, `cancelledAt` = clock truncated to microseconds | `OrderTest.cancelSetsCancelledAndTheTruncatedCancelledTime`, `OrderFlowTest.cancelOrderStampsTheCancelledTimeFromTheFlowClock` |
| reload comparison: an order cancelled by a clock with nanoseconds reloads equal | `OrderDataServiceImplTest.anOrderCancelledByAClockWithNanosecondsReloadsUnchanged` |
| cancel of SHIPPED: `OrderAlreadyShippedException`, 409 `order-already-shipped` | `OrderTest.cancellingAShippedOrderThrowsOrderAlreadyShippedAndChangesNothing`, `OrderTest.cancellingARestoredShippedOrderThrowsOrderAlreadyShipped`, `OrderFlowTest.cancelOrderRejectsAShippedOrderWith409BeforeTouchingStock` |
| cancel of CANCELLED: `OrderAlreadyCancelledException`, 409 `order-already-cancelled` | `OrderTest.cancellingTwiceThrowsOrderAlreadyCancelledAndKeepsTheFirstCancelledTime`, `OrderTest.cancellingARestoredCancelledOrderThrowsOrderAlreadyCancelled`, `OrderFlowTest.cancelOrderRejectsACancelledOrderWith409BeforeTouchingStock` |
| `ship` of CANCELLED: same exception and code | `OrderTest.shippingACancelledOrderThrowsOrderAlreadyCancelledAndChangesNothing`, `OrderTest.shippingARestoredCancelledOrderThrowsOrderAlreadyCancelled`, `OrderFlowTest.shipRejectsACancelledOrderWith409BeforeTouchingStock` |
| no domain event | `OrderTest.cancelRegistersNoEvent`; flow tests `verifyNoInteractions(events)`; every cancel IT asserts both event recorders (only the placement's order event, no stock event; none at all for the unknown id) |
| `StockItem.release`: reserved drops, on hand stays | `StockItemTest.releasingLowersReservedAndKeepsOnHand` |
| release more than reserved is rejected; exactly reserved succeeds | `StockItemTest.releasingMoreThanReservedThrowsAndLeavesStockUnchanged` (5 of 4), `StockItemTest.releasingExactlyTheReservedQuantitySucceeds` (4 of 4) |
| low-stock signal as in `receive`: reset only when available ends above the reorder level | `StockItemTest.releasingToExactlyTheReorderLevelKeepsTheSignal` (available 3, level 3), `StockItemTest.releasingToOneAboveTheReorderLevelResetsTheSignal` (available 4, level 3), the two own-level twins, `StockItemTest.releasingWhileLowAndUnsignalledRegistersNoEventAndKeepsTheSignalUnset`; flow twins `cancelOrderKeepsTheLowStockSignalWhenTheAvailableStockEndsAtTheReorderLevel`, `cancelOrderResetsTheLowStockSignalWhenTheAvailableStockRisesAboveTheReorderLevel` |
| `OrderFlow.cancelOrder` releases every line and saves order and stock | `OrderFlowTest.cancelOrderCancelsTheOrderAndReleasesTheStockOfEveryLine` (three lines, books 3, 1, 2, quantities 2, 1, 3), `OrderFlowTest.cancelOrderSavesTheOrderFirstThenTheStockItemsInBookIdOrder`, `OrderIntegrationTest.cancellingAPlacedOrderReleasesTheReservedStockOfEveryLine` (books 5, 1, 3, quantities 3, 1, 2) |
| nothing saved on a rejection (version unchanged) | flow: `verify(orders, never()).save(any())` in every rejection test; IT: `version(id)` before equals after in the three 409 scenarios; the 404 scenario stores nothing |
| translation of the new domain exception | `OrderFlowTest.cancelOrderRejectsACancelledOrderWith409BeforeTouchingStock`, `OrderFlowTest.shipRejectsACancelledOrderWith409BeforeTouchingStock`; without it the ITs get 500 (self-probe P15 in self-probes.txt) |

### Every status of the enum, by name (FF-T5-b)

On level-0-start the enum is `PLACED, SHIPPED` (claim-searches.txt C3). `PLACED` is "placed with its stock reserved"
(OrderStatus Javadoc; `placeOrder` reserves every line before the order is saved), so releasing the reserved stock of
every line is settled by the code for the only cancellable status; no status waits for restock. Per status:

| status | cancel | tests |
|---|---|---|
| `PLACED` | allowed | `cancelSetsCancelledAndTheTruncatedCancelledTime` (placed and restored) |
| `SHIPPED` | `order-already-shipped` | the two `cancelling...Shipped...` tests |
| `CANCELLED` (new) | `order-already-cancelled` | the two `cancelling...Cancelled...` tests |

An allow-list (`PLACED` only) and a deny-list (`SHIPPED`, `CANCELLED`) behave the same on every value the enum has, so
no test value outside the set exists. The pin that prevents a silent new value is the existing enum test, extended,
never removed: `OrderTest.orderStatusHasExactlyPlacedShippedAndCancelled` (before: `orderStatusHasExactlyPlacedAndShipped`).

## HTTP contract, one web slice test and one integration test per scenario

| scenario | web slice (`OrderCommandsTest`) | integration (`OrderIntegrationTest`) |
|---|---|---|
| cancel a placed order: 202, empty body | `cancelReturns202WithoutBodyAndPassesTheId` | `cancellingAPlacedOrderReleasesTheReservedStockOfEveryLine` |
| unknown id: 404 `order-not-found` | `cancelMapsOrderNotFoundTo404` | `cancellingAnUnknownOrderIsNotFound` |
| shipped: 409 `order-already-shipped` | `cancelMapsOrderAlreadyShippedTo409` | `cancellingAShippedOrderIsRejectedWith409AndChangesNothing` |
| cancel twice: 409 `order-already-cancelled` | `cancelMapsOrderAlreadyCancelledTo409` | `cancellingTwiceIsRejectedWith409AndChangesNothing` |
| ship after cancel: 409 `order-already-cancelled` | `shipMapsOrderAlreadyCancelledTo409` | `shippingACancelledOrderIsRejectedWith409AndChangesNothing` |

Each asserts the problem `code` and `title` (web slice: `expectProblem`; IT: `assertProblem`).

## Persistence and the mirror sites of `shippedAt` (G8)

Exactly one new migration, `V5__add_order_cancelled_at.sql`, adds `cancelled_at timestamptz` (nullable, no default:
`OrderDataServiceImplTest.v5AddsANullableCancelledAtColumnWithoutDefault`); `git diff --name-status HEAD~1 HEAD` of the
exercise commit lists no other migration (sandbox-commit.txt). Mirror sites: `Order` (field, `restore` parameter,
accessor), `OrderDTO` (component), `OrderDomainMapper` (`toDomain`), `OrderJPA` (column, getter, setter),
`OrderJPAMapper.copyInto` and `toDTO`, `OrderView` (field). `OrderSummaryView` is unchanged (it has no `shippedAt`,
claim-searches.txt C8). The IT `cancellingAPlacedOrderReleasesTheReservedStockOfEveryLine` reads `cancelledAt` through
`GET /orders/{id}` and the released stock through `GET /inventory/{bookId}`.

Exact-field pins, extended and never removed (lines copied from `git diff HEAD~1 HEAD` in `$SBX`):

`OrderQueriesTest.getReturnsTheOrderViewWithExactlyItsFields`, before and after:

```
-                .andExpect(jsonPath("$.*", hasSize(8)));
+                .andExpect(jsonPath("$.*", hasSize(9)));
-                "lines", "total", "currency");
+                "cancelledAt", "lines", "total", "currency");
```

`OrderIntegrationTest.orderViewExposesExactlyTheOrderViewFields`, before and after:

```
-                "id", "customerEmail", "status", "placedAt", "shippedAt", "lines", "total", "currency");
+                "id", "customerEmail", "status", "placedAt", "shippedAt", "cancelledAt", "lines", "total", "currency");
```

The new view field is asserted with two distinct values (`2026-04-03T11:22:33.445566Z` and
`2025-12-01T00:00:00.000001Z` in `OrderQueriesTest.getRendersTheCancelledTimeOfACancelledOrder`) and null for placed and
shipped orders.

## Test first, per layer (G9)

Every red below fails on assertions (lines in tdd-red-green.txt); a stub was added first wherever a test would not
compile otherwise.

| layer | tests written before the production change | stub before the red run | red | green |
|---|---|---|---|---|
| domain (stock) | the 9 `StockItemTest.releasing...` tests | empty `release(Quantity)` | 8 failures | 107/0/0 |
| domain (order) | the 10 new `OrderTest` cancel and restore tests | `CANCELLED`, `OrderAlreadyCancelledException`, `restore` parameter, empty `cancel`, `cancelledAt()` returning null | 9 failures | 36/0/0 |
| flow (mapping) | 3 new `OrderDomainMapperTest` tests | `OrderDTO.cancelledAt` component, null at the three call sites | 3 failures | 26/0/0 |
| flow | the 11 new `OrderFlowTest` cancel tests | empty `cancelOrder(UUID)` | 11 failures | 48/0/0 |
| web | 5 new `OrderCommandsTest` tests | none needed (HTTP only) | 4 failures (404 instead of 202, 409) | 61/0/0 |
| persistence | 3 new `OrderDataServiceImplTest` tests, the renamed and extended `updateCopiesOnlyStatusShippedAtAndCancelledAt` and the migration-shape test | the V5 migration only | 4 failures (second run, see below) | 19/0/0 |
| view | extended pin plus `getRendersTheCancelledTimeOfACancelledOrder` | none needed | 2 failures | 16/0/0 |
| integration | the 5 cancel ITs | endpoint change withdrawn | 5 failures | 30/0/0 |

Tests that passed already on the stub (vacuous until the behaviour existed, kept as guards):
`StockItemTest.releasingKeepsAPendingRestock`, `OrderTest.placeLeavesTheCancelledTimeEmpty`,
`OrderTest.cancelRegistersNoEvent`, `OrderCommandsTest.shipMapsOrderAlreadyCancelledTo409` (the existing handler maps
every `ConflictException`), `OrderDataServiceImplTest.v5AddsANullableCancelledAtColumnWithoutDefault` (pins the stub
migration itself). The pre-existing `OrderTest.orderStatusHasExactlyPlacedAndShipped` failed on the domain stub (it is
in the domain (order) red) and was extended as described above.

Departures from a clean test-first order, stated as they happened:

- Persistence: the first red run had 1 failure and 3 errors (NullPointerException in the test helper `instant()`). The
  helper now asserts a `Timestamp` first; the second red run fails 4 times on assertions. Both runs are in
  tdd-red-green.txt.
- Integration: the ITs were written after the endpoint existed. Their red was produced by withdrawing the endpoint
  change (`OrderCommands.java` copied from the level-3 baseline sandbox) and running with `clean`; the change was then
  re-applied byte-identically (SHA-256 `644c7b05...` before and after). A first attempt without `clean` used the stale
  compiled class and came out green; it proves nothing and its raw output was overwritten by the `clean` rerun.
- Refactor after green: the stock loop of `ship` and `cancelOrder` was extracted into `changeStockOfEveryLine`; the
  full `clean verify` after it is the last section of tdd-red-green.txt.

## Engineering choices inside J2

- Releasing more than reserved throws the existing `InsufficientReservedStockException` (the flow already translates
  it to 409 `insufficient-reserved-stock`) through a new constructor that names the action: "... so 5 cannot be
  released". The three-argument constructor still says "cannot be shipped" (`StockItemTest` pins both messages).
- `cancel` checks SHIPPED before CANCELLED, like `ship`; a stored order has one status, so the order of the two checks
  never changes the outcome.

## Residuals

- Frontend: `frontend/src/pages/OrderPage.tsx` line 29 shows "Not shipped yet" whenever `shippedAt` is null, so a
  cancelled order reads "Not shipped yet", and `frontend/src/api/types.ts` has no `cancelledAt` (claim-searches.txt C7).
  The display of CANCELLED is out of scope for T9.
- The verifier's mutation probes P1 to P15 are binding; the implementer's self-probes on a copy are in
  self-probes.txt (all 15 killed).

## Evidence files

sandbox-build.txt, tdd-red-green.txt, sandbox-commit.txt, staging-and-rebuild.txt, factory-unchanged.txt,
self-probes.txt, claim-searches.txt. Every non-heading line in them is a copy of a line of the raw output kept in
`$SP` (file named in each heading), with paths masked while writing.
