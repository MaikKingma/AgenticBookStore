# Shape: Spec 07 Restock process adapter

Source of truth: `docs/spec-pack/specs/07-restock-process.md`. This file records the shaping notes and the engineering decisions the pack spec leaves open. `plan.md` holds the task table; the full task cards are section 4 of the tech-lead strategy (path in `plan.md`).

## Scope

- Bookstore module, plus the root file `http/inventory.http`: the pending restock on `StockItem` (domain, DTO, JPA, migration `V4__add_pending_restock.sql`); the use cases `InventoryFlow.handleStockRunningLow(UUID)` and `InventoryFlow.requestRestock(UUID)`; the `process` layer (`EventProcessor`, `RequestRestockDelegate`); the command `POST /inventory/{bookId}/commands/request-restock`; `StockView.pendingRestock` with `PendingRestockView`; a default answer on the `IntegrationTestBase` distributor mock; `RestockProcessIntegrationTest`; ArchUnit fixtures for the documented `process` to domain event exception.
- Out of scope (pack spec): `@Async`, retries with backoff, an outbox. The listener runs synchronously after commit; the trade-off is documented in the `RequestRestockDelegate` Javadoc and the spec PR, and spec 09 carries it into `docs/architecture.md`.
- The distributor module and the ACL are not changed (only one Javadoc sentence on the port).

## Decisions

Decided by the orchestrator from the pack spec, the architecture standard, the tech stack and the research; cheap and reversible engineering calls.

- D1 `StockView.pendingRestock` is always present in the JSON and `null` when there is no pending restock (the spec says "null when none"; no `@JsonInclude` is used). Exact key-set pins in the tests move from 6 to 7 keys in the task that adds the component.
- D2 The scenario "Rolled back transaction triggers nothing" (an order failing with 409 because another line is short) is tested literally: no restock request is sent. `OrderFlow` throws before it publishes any event, so this scenario alone cannot prove the after-commit semantics; a second test publishes the event and then rolls back through the existing `StockEventRecorder.failBeforeCommit()` hook and asserts the distributor is never called.
- D3 The four V4 columns are nullable without defaults or constraints (no pending restock means all four are null); existing V2-column inserts in tests keep working; the domain owns the rules.
- D4 `InventoryFlow` gets `BookDataService` (the ISBN by book id) and `DistributorAppService` injected; the retailer reference is `"<bookId>/<epoch-seconds>"` from the injected `Clock`; the quantity is the stock item's reorder quantity; the stored pending restock carries the confirmed values from the distributor's answer.
- D5 Port exceptions are caught only inside `InventoryFlow.handleStockRunningLow`, around the port call and the in-memory recording of its answer, never around a save or another proxied call, and never in the delegate or the processor (an exception that crosses a transactional proxy marks the transaction rollback-only and the signal reset would be lost). The port implementation stays non-transactional.
- D6 Any other failure on the event path (data error, optimistic lock, pool timeout, commit failure) is contained by Spring: logged at ERROR, the order stays committed, `lowStockSignalled` stays set without a pending restock, and recovery is the manual command. Together with the phantom-restock window (an optimistic-lock or commit failure after the distributor confirmed) and the two pooled connections held per dipping book during the call (spec 06 residual R5), this is documented in the `RequestRestockDelegate` Javadoc and the spec PR, not fixed (out of scope).
- D7 (strategy design calls, accepted) `StockItem.restore` takes the pending restock (one rehydration factory that rebuilds every stored field); `StockItemDTO` keeps a 6-argument convenience constructor meaning "no pending restock"; domain record `PendingRestock` with `InvalidPendingRestockException`; one pending restock at a time is a domain invariant (`RestockAlreadyPendingException`) with a pre-check in the flow; `StockItem.restockRequestFailed()` resets the low-stock signal; no catch in `process`; the integration-test default answer is computed once per test from the application `Clock` (today plus 2 days, CONFIRMED, quantity echoed); three ArchUnit fixtures sharpen the existing documented exception, the rule count stays 22 and `ArchitectureTest.java` is unchanged.
- O1 In `handleStockRunningLow` an unusable distributor answer (for example quantity 0 or a blank id) is handled exactly like `DistributorUnavailableException` (WARN with the book id, signal reset, save), because the flow translates an unusable answer into that exception and the spec ties the reset to it. In `requestRestock` it propagates as 503.

## Accepted limits (spec-literal, recorded)

- `receive` clears a pending restock on every accepted delivery, also a partial one; if stock stays low, the signal stays set and the manual command is the recovery.
- A pending restock corrupted by hand-written SQL makes `restore` throw an untranslated domain exception; the application only writes validated values.

## Pack task mapping

Pack 1 = T1; pack 2 = T2 (persistence, V4) + T3 (flow, default mock); pack 3 = T5; pack 4 = T4; pack 5 = T5 (event-path integration scenarios and the trade-off Javadoc) + T6 (manual-path and delivery scenarios, ArchUnit fixtures, PR text) + T7 (`http/inventory.http`). T2 runs before T3 and T4 before T5 so every merge is green and every claim is proven in the task that makes it.
