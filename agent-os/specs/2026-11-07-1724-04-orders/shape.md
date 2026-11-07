# Spec 04 Orders: Shaping Notes

Source of truth: `docs/spec-pack/specs/04-orders.md`. These notes restate it, record the engineering decisions the spec leaves open, and map every scenario to a test. Shaped autonomously by the orchestrator agent on 2026-11-07 with two research agents, one senior research agent, one tech-lead strategist and the QA lead.

## Scope

In scope (the pack spec's "What changes"): the `Order` aggregate with the `OrderLine` entity, `OrderId`, `EmailAddress`, `OrderStatus` (PLACED and SHIPPED only) and `OrderPlacedEvent`, plus the domain record `OrderLineRequest` and the domain exceptions the behaviour list implies; `OrderFlow` (`placeOrder`, `ship`, `findById`, `findByCustomer`), the `OrderDataService` port, `OrderDTO`, `OrderLineDTO`, `PlaceOrderLineDTO`, `OrderDomainMapper`, `BookNotOrderableException`; the command adapter (place an order, ship an order), the query adapter (one order, orders of a customer); the data adapter with `V3__create_orders.sql` (`customer_order`, `order_line`); `OrderIntegrationTest` with an order event recorder and the rollback checks; `http/orders.http`. Spec 03 follow-up F28 (tests only) is folded into the domain task.

Out of scope: cancelling an order and releasing reserved stock (the Lab 3 exercise: no cancel use case, no release, no CANCELLED status, no cancel endpoint, and the word appears nowhere in main code), payments, customers as an aggregate, addresses, shipping costs, discounts, mapping a concurrent or stale update to 409, ArchUnit changes.

## Decisions

Decisions from the main session (spawn prompt), the orchestrator's choices after research (O1 to O10, run ledger) and the strategy's binding decisions (B1 to B14 in `plan.md`, which hold the exact shapes).

- Main session: reuse the spec 02 `Clock` bean (`ClockConfig`); every flow that changes a stock item publishes its pulled domain events inside the use-case transaction (`placeOrder` and `ship`); table `customer_order`; invalid query parameters are 400 `validation-failed` (spec 03 precedent); error codes kebab-case; ProblemDetail carries `code` and `details`; the cancel grep stays empty.
- O1 `itemCount` in `OrderSummaryView` is the sum of the line quantities (spec 12 says "item count"; no document defines it). View-only and reversible; listed as a product follow-up.
- O2 and B8 Place payload validation gives 400 `validation-failed`: `customerEmail` is constrained in `PlaceOrderPayload` (spec 12 expects field `customerEmail`), `lines` 1 to 20, each line with a `bookId` and an `Integer` quantity 1 to 10. The payload accepts only what the domain accepts, so the domain 422s are unreachable over HTTP except the merged quantity (the spec's 422) and one documented e-mail edge (a 254-character address that grows when lower-cased).
- O3 and B9 `GET /orders` without `customerEmail` is 400 `validation-failed` (field `customerEmail`, message "is required"); any value present goes to the flow, which parses `EmailAddress`; an invalid, empty or blank value is 422 `invalid-email` (the spec's 422).
- O4 New codes: `order-not-found` 404, `order-already-shipped` 409, `book-not-orderable` 409, `invalid-email` 422, `insufficient-reserved-stock` 409 (unreachable), `line-count-out-of-range` and `duplicate-order-line` 422 (unreachable). `insufficient-stock` details values are strings (`"requested": "2"`), because the use-case exceptions carry `List<Map<String, String>>` (spec 12 reads them as strings; listed as a product follow-up).
- O5 and B5 Check order of `placeOrder`: e-mail, each requested quantity, merge per book and each merged quantity, books in first-appearance order stopping at the first unknown (404) or discontinued (409) book, then stock with every shortage collected into one 409; nothing is written before the last check passes.
- O6, B1 and B10 Timestamps are truncated to microseconds in the domain (PostgreSQL rounds nanoseconds and Linux clocks deliver them); views render instants as ISO-8601 strings, money as plain scale-2 strings, `currency` from the lines, `shippedAt` as JSON null before shipping.
- O7, B6 and B11 Persistence: `OrderJPA` with an element collection of `OrderLineJPA` and a `position` order column (spec 02 `book_author` pattern); FK from `order_line` to `book` (no spec deletes books), `uq_order_line_book`, index `ix_customer_order_customer_email`, no check constraint on status (Lab 3 need not alter V3); the update path copies only status and shipped time; the customer list is one `join fetch` query on the stored lower-case address, newest first with id descending as the tiebreak, no `lower()`, no limit. Saves run order first, then stock items in book-id order, then publication (the opposite order deadlocks two concurrent orders; sorted, the loser gets the accepted optimistic-lock 500).
- O8 and B12 Test devices: `OrderEventRecorder` (after commit, after rollback, and a before-commit failure toggle) registered and reset in `IntegrationTestBase` beside `StockEventRecorder`; the existing `StockItemDataService` spy proves rollback across order, lines and stock.
- O9 and B13 F28: additions-only `StockItemTest` cases prove `reserve` and `ship` keep a set signal; `StockItem.java` is unchanged.
- O10 and B14 `OrderPlacedEvent.customerEmail` is the normalised address; `total` is the order total amount; `occurredAt` equals `placedAt`.
- Task order flips the pack's: the data adapter (T3) merges before `OrderFlow` (T4), because a `@Service` without its port bean breaks every full-context test.
- Contradiction check (strategy): none blocks. Additions beyond the spec's name list are not renames (`PlaceOrderLineDTO`, four domain exceptions, `OrderLine.quantityOf`, `OrderQueries.MissingQueryParameterException`). Pack task 4's "exception handlers extended" is satisfied by the existing advices plus one query 400 method. The standard's "adding a new exception requires a spec change" concerns ArchUnit exceptions, not Java exception classes.
- Accepted defaults carried from specs 02 and 03: a stale or concurrent update is an unmapped 500; malformed JSON and non-UUID values give Boot's default 400; Jackson coerces `"5"` to 5 and truncates `1.5` to 1 for `Integer` fields.

## Context

- **Visuals:** None.
- **References:** the spec 02 catalogue and spec 03 inventory slices are the pattern (see `references.md`).
- **Product alignment:** mission (a real domain where business rules collide; test-enforced architecture); roadmap item 04 (ordering, the place for the Lab 3 exercise); tech stack (Boot 4.1.2, Java 25, Jackson 3, Jakarta Validation, Flyway, Testcontainers, PostgreSQL 18.6; nothing new pinned). Later specs rely on the shapes: spec 12 (payload, views, codes), spec 10 (class names, requirements copied to the participants' repo), spec 09 (seed SQL writes `customer_order` and `order_line` directly), spec 07 (all-or-nothing place, `OrderIntegrationTest` kept by name).

## Standards Applied

- `architecture` (pointer to `docs/spec-pack/product/architecture-standard.md`): sections 1 to 5 in full; see `standards.md`.

## Scenario-to-test map (planned names; final method names are recorded in the task outbox artifacts)

| pack item | end to end (T7 `OrderIntegrationTest`) | layer falsifiers |
|---|---|---|
| Scenario: Successful order | `placingAnOrderReservesStockAndPublishesOneOrderPlacedEventAfterCommit`, `orderViewExposesExactlyTheOrderViewFields` | T1 `totalIsTheSumOfTheLineTotals`, `placeRegistersOneOrderPlacedEventWithTheNormalisedEmailTheTotalAndThePlacedTime`; T2 `derivedTotalsAgreeWithTheAggregate`; T4 `placeOrderSavesTheOrderWithSnapshotsAndReturnsItsId`, `placeOrderReservesTheStockOfEveryLine`, `placeOrderPublishesTheOrderPlacedEventAndThePulledStockEvents`, `placeOrderSavesTheOrderFirstThenTheStockItemsInBookIdOrderThenPublishes`; T5 `placeReturns202WithTheOrderIdAndPassesTheLinesInOrder`; T6 `getReturnsTheOrderViewWithExactlyItsFields` |
| Scenario: Price snapshot | `orderKeepsThePriceItWasPlacedAt` | T1 `placeCreatesAPlacedOrderWithTheLinesInRequestOrder`; T2 `toLineRequestSnapshotsTheBookAndTakesTheQuantity`; T3 `savesANewOrderAndFindsItWithItsLinesInOrder`, `updateCopiesOnlyStatusAndShippedAt` |
| Scenario: Several shortages are reported together | `severalShortagesAreReportedTogetherAndNothingIsReserved` | T4 `placeOrderCollectsEveryShortageIntoOne409`; T5 `placeMapsInsufficientStockTo409WithTheShortagesInDetails` |
| Scenario: Duplicate lines are merged | `duplicateLinesAreMergedIntoOneLine` | T4 `placeOrderMergesDuplicateLinesPerBook`, `placeOrderAcceptsAMergedQuantityOf10`; T5 `placePassesDuplicateLinesUnmerged`; T1 `placeRejectsTwoLinesForTheSameBook` |
| Scenario: Merged quantity too large | `aMergedQuantityAbove10IsRejectedWith422AndNothingIsReserved` | T4 `placeOrderRejectsAMergedQuantityAbove10BeforeLoadingAnyBook`, `placeOrderRejectsARequestedQuantityOutside1To10`; T1 `quantityOfRejects0And11AndMinus1`; T5 `placeMapsQuantityOutOfRangeTo422` |
| Scenario: Discontinued book | `aDiscontinuedBookIsRejectedWith409AndNothingIsReserved` | T4 `placeOrderRejectsADiscontinuedBookWith409BeforeAnyStockLookup`; T2 `bookNotOrderableIsAConflictWithTheBookNotOrderableCodeAndTitle`; T5 `placeMapsBookNotOrderableTo409` |
| Scenario: Shipping reduces stock | `shippingTurnsReservationsIntoStockReductions` | T1 `shipSetsShippedAndTheTruncatedShippedTime`; T4 `shipShipsTheOrderAndTheStockOfEveryLine`, `shipSavesTheOrderFirstThenTheStockItemsInBookIdOrder`; T3 `shippingUpdatesStatusAndShippedAtAndBumpsTheVersion`; T5 `shipReturns202WithoutBodyAndPassesTheId` |
| Scenario: Shipping twice | `shippingTwiceIsRejectedWith409AndChangesNothing` | T1 `shippingTwiceThrowsOrderAlreadyShippedAndKeepsTheFirstShippedTime`; T4 `shipRejectsAShippedOrderWith409BeforeTouchingStock`; T5 `shipMapsOrderAlreadyShippedTo409` |
| Scenario: Case-insensitive lookup | `orderHistoryMatchesTheEmailCaseInsensitivelyNewestFirst`, `orderSummaryExposesExactlyTheSummaryFields` | T1 `trimsAndLowerCasesTheAddress`, `lowerCasesWithTheRootLocale`; T3 `findByCustomerEmailReturnsTheCustomersOrdersNewestFirst`, `findByCustomerEmailBreaksAPlacedAtTieByIdDescending`, `findByCustomerEmailMatchesTheStoredValueExactly`; T4 `findByCustomerAsksThePortWithTheNormalisedAddress`; T6 `byCustomerReturnsTheSummariesInTheOrderOfTheFlow`, `summaryItemCountIsTheSumOfTheLineQuantities` |
| Rollback check (Tests required, end to end) | `severalShortagesAreReportedTogetherAndNothingIsReserved`, `noOrderOrReservationSurvivesARollbackBeforeCommit`, `aFailingStockSaveUndoesTheOrderAndEarlierReservations`, `aFailingStockSaveUndoesTheShipment` | T4: every rejection verifies save and publish never |
| API errors beyond the scenarios | `anUnknownBookIsRejectedWith404`, `invalidPlacePayloadIsRejectedWith400`, `unknownOrderIsNotFoundOnShipAndRead`, `orderHistoryWithoutCustomerEmailIsRejectedWith400`, `orderHistoryWithAnInvalidEmailIsRejectedWith422`, `orderHistoryOfACustomerWithoutOrdersIsEmpty` | T5 and T6 400 and mapping tests; T4 pass-through tests |
| Tests (required): Domain `OrderTest` (merge, limits, total, ship rules), `EmailAddressTest` | | T1 (the merge itself is a flow rule; `OrderTest` covers the duplicate rejection the merge prevents, the limits, the total and the ship rules) |
| Tests (required): Flow `OrderFlowTest` (all shortages collected, nothing saved on failure, events published) | | T4 |
| Tests (required): Adapters `OrderCommandsTest`, `OrderQueriesTest`, `OrderDataServiceImplTest` | | T5, T6, T3 |
| Tests (required): End to end `OrderIntegrationTest` for every scenario, including a rollback check | all rows above | T7 |
| Definition of done: scenarios covered, `./mvnw verify` green, ArchUnit green, cancel grep empty | referee at every task gate and at qa-final; `ArchitectureTest` 21 green | `grep -ri "cancel" bookstore/src/main/java` empty at every gate (C0) |
| Spec 03 follow-up F28 | | T1 `StockItemTest` additions |

## Follow-ups for the product owner

- `itemCount` meaning (O1: sum of quantities).
- `insufficient-stock` details values as strings (O4).
