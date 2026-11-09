---
spec: 07-restock-process
source: docs/spec-pack/specs/07-restock-process.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-09T03:45:00Z
qa-lead: on
base: factory
spec-branch: sdd/07-restock-process/spec
modules: [root, bookstore, distributor, frontend, guide]
---

# Plan: Spec 07 Restock process adapter

Restates `docs/spec-pack/specs/07-restock-process.md` (source of truth). Shaping decisions D1 to D7 and O1 are in `shape.md`, standards in `standards.md`, research summary in `references.md`. The full task cards (file scope, criteria, named tests, forced test updates, risks) are section 4 of the tech-lead strategy; the QA strategy (tiers, Gate 1 findings G1 to G20, common directive C0, per-task directives, scenario-to-test plan, qa-final focus, residuals) is in the QA lead's ledger. Both live in the cycle's comms and are passed to every agent by path: `.claude/comms/mailboxes/lead-1/outbox/strategy-07-restock-process.md` (section 3 design calls, section 4 task cards) and `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-07-restock-process.md` (section "QA strategy"). Precedence for a task's acceptance criteria: this plan's task section, then the Gate 1 findings it names (they win over the card), then the card.

Path abbreviations: B = `bookstore/src/main/java/nl/openvalue/bookstore/`, T = `bookstore/src/test/java/nl/openvalue/bookstore/`, F = `bookstore/src/test/java/nl/openvalue/archfixtures/`, M = `bookstore/src/main/resources/db/migration/`.

## Summary

When stock runs low the bookstore asks the distributor for more. `StockItem` gains a pending restock (`restockRequested`, cleared by `receive`, `hasPendingRestock()`), persisted through four nullable columns added by `V4__add_pending_restock.sql`. `InventoryFlow` gets two use cases sharing one private implementation: `handleStockRunningLow(UUID)` (the event path, catches the three port exceptions itself) and `requestRestock(UUID)` (the manual path, lets them propagate). The `process` layer gets `EventProcessor` (`@TransactionalEventListener(phase = AFTER_COMMIT)` on `StockRunningLowEvent`) handing the event to `RequestRestockDelegate` (`@Transactional(propagation = REQUIRES_NEW)`, one flow call). `POST /inventory/{bookId}/commands/request-restock` gives 202, 503, 404 or 422. `StockView` gains `pendingRestock` (null when none). `IntegrationTestBase` gets a default distributor answer (CONFIRMED, delivery in 2 days). `RestockProcessIntegrationTest` covers every scenario. ArchUnit keeps exactly one documented exception, sharpened by three fixtures. Referee `./mvnw clean verify` from the repo root, green after every task, budget 60 s (baseline 17.48 s on 824af83: bookstore 931 tests, distributor 190, 10 Started lines in the bookstore, ArchitectureTest 22 rules).

## Ground rules for every task

- The pack spec decides what; `docs/spec-pack/product/architecture-standard.md`, `tech-stack.md` and `docs/spec-pack/orchestrator/decisions.md` win. Bookstore code goes through the `hexagonal-slice` skill (with `clean-hexagonal-onion`, `ddd-building-blocks`, `layer-testing`); the standard and the spec win over the skills. A contradiction is reported as blocked, never resolved by the implementer.
- Only the task's fileScope is edited, never anything else, not even temporarily. Mutation probes on production code are the verifier's job: the implementer describes each mutant in its outbox.
- Never skip, disable, delete or weaken a test or an ArchUnit rule; never edit `ArchitectureTest.java`; never add `ignoreDependency`; no `@Disabled`, `assume*`, `@DirtiesContext`; never `--no-verify`.
- No new dependency; no `@Async`, `@EnableAsync`, `@JsonInclude`, outbox or retry; no `@Transactional` in `acl`; no `@Transactional` on `EventProcessor`. No test calls the real distributor (live runs are qa-final evidence only).
- Time only through a fixed `Clock` in unit tests; the integration base computes its default date from the autowired context `Clock` (QA C0-5e as rewritten by G6).
- Pins: no `Map.of` for JSON pins that contain a null value (use one null-tolerant helper per class). A key that must be present with a null value is asserted with `hasKey` or the key count, never `doesNotExist()`. Stub the shared distributor mock in subclasses with `doThrow(...)/doAnswer(...).when(mock)`.
- Falsifiers first: two values per pass-through field, both starting states for every "unchanged" claim, witnesses that differ from every default (reorder quantity, delivery date, id, backordered).
- Plain English, no em dashes; every new or changed file is ASCII (`rg -n '[^\x00-\x7F]' <file>` empty). Product files and test code never reference `docs/spec-pack`, `agent-os`, `.claude`, `CLAUDE.md`. Never `ANTHROPIC_API_KEY`.
- Git is read-only for every agent; the orchestrator owns commits, branches and PRs.
- Every implementer records its final test method names in its outbox as the task's rows of the scenario-to-test map, and names every falsifier of its card and of its Gate 1 rows with the final method name.
- Scratch and evidence files: session scratchpad `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/` with prefix `s07-T<n>-<role>-`. Long runs through `bash ~/.claude/scripts/with-budget.sh <s> ...`.

## Task table

| id | summary | module | dependsOn | batch | qaTier | qaModel | implModel | status | verdict | qa-lead | rounds | pr | commit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Domain pending restock: `PendingRestock`, `InvalidPendingRestockException`, `RestockAlreadyPendingException`, `restockRequested`, `hasPendingRestock`, `pendingRestock()`, `restockRequestFailed`, `receive` clears, `restore` takes the pending restock | bookstore | [] | 1 | elevated | sonnet | sonnet (r2 opus) | done (branch `sdd/07-restock-process/T1`) | PASS (r2) | ENDORSE | 2 | #94 | c8365c7 |
| T2 | Persistence: `PendingRestockDTO`, `StockItemDTO` 7 components plus 6-argument convenience constructor, mapper both ways, `V4__add_pending_restock.sql`, `StockItemJPA`, `StockItemJPAMapper` | bookstore | [T1] | 2 | critical | opus | opus | done (branch `sdd/07-restock-process/T2`) | PASS (r0) | ENDORSE | 0 | #97 | c95a161 |
| T3 | `InventoryFlow.requestRestock` and `handleStockRunningLow` (O1), WARN logs, port Javadoc, `IntegrationTestBase` default answer | bookstore | [T2] | 3 | critical | opus | opus | done (branch `sdd/07-restock-process/T3`) | PASS (r0) | ENDORSE | 0 | #98 | afcac9c |
| T4 | `POST /inventory/{bookId}/commands/request-restock`, `PendingRestockView`, `StockView.pendingRestock`, 6 to 7 key pins | bookstore | [T3] | 4 | elevated | sonnet | sonnet | done (branch `sdd/07-restock-process/T4`) | PASS (r1) | ENDORSE | 1 | #102 | 7f8cf9b |
| T5 | `EventProcessor`, `RequestRestockDelegate` (trade-off Javadoc), process unit tests, `RestockProcessIntegrationTest` event path, post-dip pins | bookstore | [T4] | 5 | critical | opus | opus | done (branch `sdd/07-restock-process/T5`) | PASS (r0) | ENDORSE | 0 | #103 | e62b914 |
| T6 | Manual-path and delivery scenarios in `RestockProcessIntegrationTest` (closes spec 06 R6), three ArchUnit fixtures, PR trade-off text | bookstore | [T5] | 6 | critical | opus | opus | todo | | | | | |
| T7 | `http/inventory.http` request-restock examples | root | [T4, T5] | 7 | elevated | sonnet | sonnet | todo | | | | | |

Execution and merge order, one task at a time: T1, T2, T3, T4, T5, T6, T7.

qaDirective of every task: C0 plus the task's T<n> block of the QA ledger, plus every `## QA feed-forward` entry recorded before the task starts.

## Tasks

### T1: Domain pending restock
- fileScope: `B/domain/StockItem.java`, `B/domain/PendingRestock.java`, `B/domain/InvalidPendingRestockException.java`, `B/domain/RestockAlreadyPendingException.java`, `B/domaininteraction/StockItemDomainMapper.java` (call site only, passes `null`), `T/domain/StockItemTest.java`, `T/domain/PendingRestockTest.java`, `T/domaininteraction/StockItemDomainMapperTest.java` (call sites only).
- Acceptance criteria: strategy card T1, plus Gate 1 finding G17.

### T2: Persistence of the pending restock
- fileScope: `B/domaininteraction/PendingRestockDTO.java`, `B/domaininteraction/StockItemDTO.java`, `B/domaininteraction/StockItemDomainMapper.java`, `M/V4__add_pending_restock.sql`, `B/data/StockItemJPA.java`, `B/data/StockItemJPAMapper.java`, `T/domaininteraction/StockItemDomainMapperTest.java`, `T/domaininteraction/OrderFlowTest.java` and `T/domaininteraction/InventoryFlowTest.java` (additions only), `T/data/StockItemDataServiceImplTest.java` (additions only).
- Acceptance criteria: strategy card T2, plus G16 and G18.

### T3: Flow use cases and the default mocked port
- fileScope: `B/domaininteraction/InventoryFlow.java`, `B/domaininteraction/DistributorAppService.java` (one Javadoc sentence), `T/domaininteraction/InventoryFlowTest.java`, `T/IntegrationTestBase.java`, `T/IntegrationTestBaseHooksTest.java`.
- Acceptance criteria: strategy card T3 with O1 (shape.md), plus G1 (replaces the card's unusable-confirmation criterion), G2, G3, G4, G5, G7.

### T4: Manual endpoint and view
- fileScope: `B/command/InventoryCommands.java`, `B/query/StockView.java`, `B/query/PendingRestockView.java`, `T/command/InventoryCommandsTest.java`, `T/query/InventoryQueriesTest.java`, `T/InventoryIntegrationTest.java`, `T/OrderIntegrationTest.java`, `T/DistributorImportIntegrationTest.java`.
- Acceptance criteria: strategy card T4, plus G8.

### T5: Process adapter and event-path integration test
- fileScope: `B/process/EventProcessor.java`, `B/process/RequestRestockDelegate.java`, `T/process/EventProcessorTest.java`, `T/process/RequestRestockDelegateTest.java`, `T/RestockProcessIntegrationTest.java` (new), `T/OrderIntegrationTest.java`, `T/InventoryIntegrationTest.java`, `T/IntegrationTestBase.java`, `T/IntegrationTestBaseHooksTest.java` (pin updates and an expected-default-pending helper on the base only).
- Acceptance criteria: strategy card T5, plus G9 (replaces the card's two-book test), G10, G11, G19, G20.

### T6: Manual-path integration test, ArchUnit fixtures, trade-off text
- fileScope: `T/RestockProcessIntegrationTest.java`, `T/ArchitectureSelfTest.java` (additions only), `F/domain/BookEventLog.java`, `F/domain/BookRefusedException.java`, `F/process/EventLogDelegate.java`, `F/process/RefusalDelegate.java`, `F/query/BookEventQueries.java`, `B/process/RequestRestockDelegate.java` (Javadoc only, if needed).
- Acceptance criteria: strategy card T6, plus G12, G13, G14, G20. The PR-description trade-off text goes into the T6 outbox.

### T7: HTTP collection
- module: root.
- fileScope: `http/inventory.http`.
- Acceptance criteria: strategy card T7, with G15 (the ISBN condition reads: no other `http/*.http` request adds or imports 9789900010029 in the bookstore on port 8080; distributor-side reads on port 8090 are fine).

## Scenario-to-test map

Planned in the QA ledger section "Scenario-to-test traceability plan"; final method names are recorded here after each task.

## QA feed-forward

- FF1 (from T1 r1, every verifier T2 to T7): a test outline offered in a Required Action must also kill the boundary twin of the mutant it targets (`>` vs `>=`, `<` vs `<=`, +1/-1); run the twin against the witness before offering it. For a criterion of the form "every X does Y", use one witness from strictly inside each region that any comparison in the method (or a plausible mutant's guard) splits the input into, not only the boundary value.
- FF2 (from T1 r1, T2 verifier): PostgreSQL truncates a string longer than a `varchar(n)` without error when the extra characters are all spaces; a length witness for `pending_restock_order_id varchar(64)` uses a 65th character that is not a space.
- FF3 (from T1 r2, T2 implementer and verifier): (a) the `InventoryFlowTest` case "receive from a pending item saves a DTO with null pending" starts from onHand 0, reserved 0, reorderLevel 4, lowStockSignalled true, a pending restock, and receives 1; the saved DTO has onHand 1, signal true, pending null; the `StockItem` mutants X1m1 and X1m2 (`if (available() >= reorderLevel - 1)` or `- 2` around `pendingRestock = null;`) must fail against `InventoryFlowTest`. (b) The `StockItemDomainMapperTest` witness that the `PendingRestock` hand-off rejects uses the id `" " + "R".repeat(64)` and asserts `isExactlyInstanceOf(InvalidPendingRestockException.class)`; the `PendingRestock` mutant D8stripL must fail against `StockItemDomainMapperTest`.
- FF4 (from T2 r0, T3 and T5 implementers and verifiers): the distributor emits restock order ids as lowercase UUID strings. (a) T3: one of the two confirmation `restockOrderId` literals in `InventoryFlowTest` (G2) is a lowercase UUID literal containing at least one letter a to f (for example `"0f8e2c1a-5b7d-4e3f-9a1b-c2d3e4f5a6b7"`), asserted as a literal in the saved DTO; `DEFAULT_RESTOCK_ORDER_ID` (G7) is a lowercase UUID literal containing at least one letter a to f; the mutant J3d-idU (`toUpperCase(Locale.ROOT)` on the id in `PendingRestockDTO(PendingRestock)`) must fail against `InventoryFlowTest`. (b) T5 verifier: the mutants Cw-idU (`toUpperCase(Locale.ROOT)` on the id in `StockItemJPAMapper.copyInto`, null-safe) and J3j-idU (the same in `toPendingRestockDTO`) must fail against `RestockProcessIntegrationTest` (the post-dip pins show the default id).
- FF5 (from T3 r0, orchestrator): after merging origin/factory (fix-06) into the spec branch and before T4, re-baseline C0 on the post-merge spec tip and record it under Amendments (A3); the T4 to T7 verifiers take every baseline from there.
- FF6 (from T3 r0, T5 to T7 implementers and verifiers; replaces the test half of C0-5e): `grep -rn -E 'LocalDate(Time)?\.now\(|Instant\.now\(|Clock\.system|ofInstant\(|clock\.instant\(\)|clock\.millis\(\)' bookstore/src/test` prints exactly: the `IntegrationTestBase` `LocalDate.now(clock)`, the `IntegrationTestBaseHooksTest` `LocalDate.ofInstant(clock.instant(), clock.getZone())`, two `clock.instant()` lines in `InventoryIntegrationTest`, four `clock.instant()` lines plus one `OffsetDateTime.ofInstant` on a parsed literal in `OrderIntegrationTest`, nothing from any other file. New integration tests take the default date from `defaultExpectedDeliveryDate` (in the view its `toString()`), never from a fresh clock read; `\.now\(\)` in the tests stays empty.
- FF7 (from T4 r1, T6 implementer and verifier, inside G13): the G13 stub answer's `restockOrderId` contains at least one uppercase letter (`"R-7781"` as planned) and the manual-path test asserts it as a literal in `GET /inventory/{id}` (`$.pendingRestock.restockOrderId`) and in the JDBC read of `pending_restock_order_id`; the mutant I2 (`.toLowerCase(Locale.ROOT)` on `restockOrderId()` in `PendingRestockView.from`) and the same in `StockItemJPAMapper` (`copyInto`, `toPendingRestockDTO`, null-safe) must fail against `RestockProcessIntegrationTest`.
- FF8 (from T4 r1, T5 verifier, G11 pin audit): T4 wrapped every IT stock-view pin in a per-class `withNoPendingRestock(Map.of(...))` helper. The G11 word diff runs against the post-T4 spec tip: a post-dip pin moving from `withNoPendingRestock(` to the base's expected-default-pending helper is the expected change, no figure literal inside the `Map.of` is removed or altered, and every pin left on `withNoPendingRestock` is one before a dip, after a rollback or after a receive (T5 outbox list, count equal to the diff).
- FF9 (from T5 r0, T6 implementer and verifier): (a) `RestockProcessIntegrationTest` takes additions only; T5's eight test methods and helpers keep their bytes (`git diff -U0 <post-T5 tip> -- bookstore/src/test/java/nl/openvalue/bookstore/RestockProcessIntegrationTest.java | grep -E '^-[^-]'` prints nothing). (b) The class shares one captured output and T5's D6 test asserts `containsOnlyOnce("TransactionSynchronization.afterCompletion threw exception")`: no T6 test provokes an afterCompletion ERROR or reuses a T5 literal; every T6 log assertion names a fresh book id; C0-18 plus one `-Dsurefire.runOrder=alphabetical` run. (c) Must-fail probe X-G14: removing the `if (item.hasPendingRestock())` early return from `InventoryFlow.prepareRestock` must fail in `automaticRestockIsSkippedWhileOneIsPending` itself. (d) C0-6 base after T5: afterCompletion ERROR 1 (D6 test only), dispatcherServlet ERROR 6, InventoryFlow WARN 8, ACL WARN 97, HHH000247 5, DefaultHandlerExceptionResolver 1, total WARN 116; bookstore total 1138; manual-path 503/404/422 add no dispatcherServlet ERROR.
- FF10 (from T5 r0, T7 verifier): with the event path live, the `correct` that dips Sense and Sensibility creates the pending restock through the automatic path before any manual request. The replay reads `GET /inventory/{id}` right after `correct` (pending present, quantity 10, backordered false) and again after the second request-restock, and one check asserts the same `restockOrderId` both times; the "correct to 20" negative control must fail at the post-correct check; with the distributor stopped only the Frankenstein 503 is expected (a Sense and Sensibility dip there gives 202, no pending, a WARN); every comment in the file about which request creates the restock matches this order.
- FF11 (from T6 r1, T7 implementer and verifier): every comment or check in `http/inventory.http` about a manual request-restock answer matches the T6 pins: 503 `distributor-unavailable` leaves the stock and `lowStockSignalled` unchanged and records no pending restock (the signal reset on 503 belongs to the automatic event path only); 404 `title-not-found-at-distributor` and 422 `title-not-orderable` leave the stock row unchanged; a manual request while a restock is pending answers 202 without a second distributor order; 202 has an empty body. The verifier quotes each such comment next to the `RestockProcessIntegrationTest` assertion it restates.

## Amendments

- A1 (2026-11-09, orchestrator, from the T1 QA review): the strategy card's word "final" for the new domain exceptions is dropped; the repo's domain exceptions are plain `public class ... extends RuntimeException`, and T1 follows that style. No other consequence.
- A2 (2026-11-09, orchestrator, from the T3 QA review and the fix-06 merge): fix-06 added `DistributorUnavailableException(String, Throwable)` to keep the cause of an unusable distributor answer. T4 also switches `InventoryFlow.recordConfirmation` to that constructor (cause = the `InvalidPendingRestockException`; the detail text stays as it is, naming only the book id and figures) and adds to `InventoryFlowTest.requestRestockTurnsAnUnusableConfirmationIntoDistributorUnavailable` that the cause is exactly an `InvalidPendingRestockException` and `details()` is empty. T4's fileScope gains `B/domaininteraction/InventoryFlow.java` (that one call only) and `T/domaininteraction/InventoryFlowTest.java` (additions only). Reason: the spec branch must not reintroduce the dropped-cause pattern the fix-06 review closed; cheap and reversible.
- A3 (2026-11-09, orchestrator, FF5): origin/factory (d8e6db1 plus progress-only 8eb295c, fix-06 PR #89) merged into the spec branch as merge commit M = 119c744 (one agent-memory conflict in `qa-lead/qa-test-data-falsifiers.md`, resolved as a union). Baselines for T4 to T7 on M: PR base B = M (or the then-current spec tip), never db45ec5. Referee on M: green, Total time 25.942 s (27 s wall, machine under load from parallel builds), bookstore 1110/0/0/0 (1069 after T3 plus 41 from fix-06), distributor 190/0/0/0, ArchitectureTest 22, ArchitectureSelfTest 58, 10 Started lines in the bookstore, 1 PostgreSQL container. Per class: InventoryFlowTest 54, StockItemTest 98, InventoryIntegrationTest 14, OrderIntegrationTest 25, DistributorImportIntegrationTest 28, DistributorAppServiceImplTest 130, CatalogueFlowTest 80, InventoryCommandsTest 17, InventoryQueriesTest 14. C0-6 b counts in the bookstore section: ACL WARN 97 (was 67; fix-06 added ACL tests with captured output), HHH000247 5, dispatcherServlet ERROR 5, DefaultHandlerExceptionResolver WARN 1, InventoryFlow WARN 5, afterCompletion 0, total WARN 113. C0-1 reads "bookstore total = 1110 plus the tests T4 onward add". `InventoryFlow.java` is unchanged on M (sha256 77fe9833...c570e); `DistributorAppService.java` on M has sha256 560eae1e...2a4747 and both the T3 sentence (`grep -c 'Implementations must stay non-transactional'` prints 1) and the fix-06 cause constructor. The C0-5 grep set (a to l) is dry-run on M by the T4 verifier as its first step; every output that differs from the T3 records is named with the fix-06 file behind it in its verdict. Referee log: scratchpad `s07-merge-M-orch-referee.log`.
- A4 (2026-11-09, orchestrator, wording fixes from the T4 and T5 QA reviews, no gate effect): (1) T4 card clause "a malformed id behaves like the existing receive test" names a receive test that does not exist; it reads "a malformed id on request-restock answers like receive (400 with an empty body, measured by QA; no test pins either endpoint, residual R15)". (2) G9 "one ERROR line containing the literal" reads "Spring's afterCompletion ERROR line followed by `java.lang.IllegalStateException: <literal>`, each once". (3) T5 card "the failBeforeCommit test kills a plain `@EventListener`" reads "a plain `@EventListener` blocks on the row lock (measured at T5, informational); the failBeforeCommit test kills AFTER_COMPLETION".

## Final sweep and Gate 2

(pending)
