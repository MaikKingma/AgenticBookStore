---
spec: 12-storefront
source: docs/spec-pack/specs/12-storefront.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-08T20:46:00Z
qa-lead: on
base: factory
spec-branch: sdd/12-storefront/spec
---

# Plan: 12-storefront (OpenBook)

Every task card (summary, acceptance criteria, fileScope, directive and traps) is the "Task breakdown" section of
`strategy.md` in this folder, amended by the QA Gate 1 findings GQ1 to GQ14 of `qa-strategy.md` section 2 (all
accepted) and by the orchestrator decisions in `shape.md`. Each task's qaDirective is C0 (`qa-strategy.md` section 3)
plus the task's block (`qa-strategy.md` section 4), plus every entry under "QA feed-forward" below.

Tasks run one at a time in the order below. Each branch `sdd/12-storefront/<Tn>` is cut from the spec branch tip and
its PR is squash-merged into `sdd/12-storefront/spec` after gate PASS (verifier PASS and QA lead ENDORSE), the ship
gate and green checks.

## Tasks

| id | module | dependsOn | batch | qaTier | qaModel | summary | pr | branch | verdict | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | bookstore | [] | 1 | elevated | sonnet | `StorefrontConfig`, `StorefrontConfigTest` on `IntegrationTestBase`, test `static/shop/index.html`, `bookstore/pom.xml` resources (two parent entries re-declared plus the unfiltered dist entry) | #70 | sdd/12-storefront/T1 | PASS r1 | ENDORSE |
| T2 | frontend | [] | 2 | elevated | sonnet | `frontend/pom.xml`, create-vite 9.3.4 react-ts scaffold with exact pins, `vite.config.ts`, `setupTests.ts` plus `setupTests.test.ts` guard and console trap (GQ4), smoke test, README; standalone `-f frontend/pom.xml` build | #72 | sdd/12-storefront/T2 | PASS r2 | ENDORSE |
| T3 | root | [T1, T2] | 3 | elevated | sonnet | root module list `frontend`, `bookstore`, `distributor`; `actions/cache` for `frontend/node` and the npm cache in both CI jobs; `jar tf` evidence (GQ14) | | | | |
| T4 | frontend | [T3] | 4 | elevated | sonnet | OpenBook styling foundation: `tokens.css`, self-hosted Montserrat plus OFL, mobile-first `global.css`, contrast test (GQ2, GQ3) and style contract test, title "OpenBook" | | | | |
| T5 | frontend | [T3] | 5 | critical | opus | API client, ProblemDetail normalisation (including plain Boot error JSON), endpoint functions, `client.test.ts`, fetch mock | | | | |
| T6 | frontend | [T4, T5] | 6 | elevated | sonnet | in-house `HashRouter`, sticky header with the OpenBook wordmark (light header), `ProblemDetailAlert`, `SearchForm`, `BookCard`, `StockBadge`, `CataloguePage` | | | | |
| T7 | frontend | [T6] | 7 | elevated | sonnet | `limits.ts`, basket state (sessionStorage), `estimate.ts`, `BookDetailPage`, header badge counting items (GQ11), deep link | | | | |
| T8 | frontend | [T7] | 8 | critical | opus | basket page, checkout, order page, orders page, `format.ts` | | | | |
| T9 | frontend | [T7] | 9 | elevated | sonnet | conversation id, `AssistantPage`, `ChatMessageList`, chat length limit (spec 08 contract text) | | | | |
| T10 | root | [T8, T9] | 10 | critical | opus | clean-state referee without global Node (cold and warm times), probes, live checks on a throwaway Postgres (GQ1, no `docker compose`), browser walk-through, GQ8 checks, 14 screenshots in `visuals/`, `verification-record.md` | | | | |

Batches are sequential because tasks run one at a time; T1 and T2 are independent, T4 and T5 are independent, T8 and
T9 are independent.

## Models

Implementation sonnet (opus for any rework round 2 or later); verifiers per the table; qa-lead and qa-final opus.
Never fable.

## Manual checks (not automatable in this cycle)

- Live assistant round trip on `/shop/#/assistant` against the running bookstore, after spec 08 merges (GQ9). Also
  re-check the 429 and 503 body shapes against the T9 fixtures (OQ-4). Until then T10 records what was observed (page
  loads, empty state, a sent message shows the alert for the absent endpoint).
- "Modern", "generous whitespace" and "prominent search bar" (D7) are judged by Maik from the screenshots (GQ8).

## QA feed-forward

- FF-T1 (qa-lead, T1 r1; applies to T2 to T10): compare against the spec 12 baselines: Java tests bookstore 689,
  distributor 190, ArchitectureTest 21, ArchitectureSelfTest 40, skipped 0; Started lines 15 (bookstore 10,
  distributor 5), exactly one postgres:18.6 container; C0-7 warning inventory against the T1 referee log
  (30 `warn|deprecat|self-attach` lines, 5 error-pattern lines) plus this task's classified npm lines; C0-6g non-ASCII
  prints exactly `IsbnTest.java:67` and `:74`; the four T1 files (`StorefrontConfig.java`, `StorefrontConfigTest.java`,
  test `static/shop/index.html`, `bookstore/pom.xml`) keep their T1 hashes (QA ledger, Cross-task patterns). No later
  task's fileScope names a bookstore file.
- FF-T2 (qa-lead, T2 r2; applies to T3 to T10 and qa-final): Vitest baseline 2 files, 4 tests; each later total is the
  previous total plus the task's added tests, minus only a removal its card names. `frontend/src/setupTests.ts` keeps
  sha256 `4b4e2e2dc4d7775e0b1ef457fe0fa178c7bc6293b70179f523eb655e2da68e7a` unless a task's fileScope names it. Every
  frontend verifier runs two must-fail probes in one of its own task's test files, `console.error('probe')` and
  separately `console.warn('probe')`, each failing with "console.error or console.warn was called during the test";
  a task that changes setupTests.ts also runs P10, P10c and P11 as must-fail. Hash files hold only `shasum -a 256`
  lines (`shasum -c` prints no "improperly formatted" warning). Quote the C0-3 grep in full (including `fails` and the
  `xit`/`xdescribe`/`xtest` forms). T3 warning inventory: exactly 4 new `[ERROR] npm warn install-scripts ...
  fsevents@2.3.3` lines per frontend run, counted in both inventories (30+4 and 5+4). Carried to qa-final: the README
  claim that `./mvnw clean verify` packages dist into the bookstore becomes true at T3; check it at the tip.

## Amendments

- A1 (2026-11-08, orchestrator, during T2): the `frontend/README.md` sentence that colour tokens live in
  `src/styles/tokens.css` moves from T2 to T4, because the file only exists from T4. T4's fileScope gains
  `frontend/README.md`. Also accepted in T2: `server.host: '127.0.0.1'` in `vite.config.ts` (the dev server listens on
  the address the README documents). Consequence: none for other tasks. Gate re-run self-accepted.
- A2 (2026-11-08, orchestrator, on the QA lead's T2 r2 recommendation): T4's fileScope gains
  `frontend/src/setupTests.test.ts` (test-only, insertion-only) for a guard that proves the console trap fails a test:
  one `it.fails` per console method that silences the spy and logs, each registering `onTestFailed` to record the
  error message in a describe-scoped array, plus a following test asserting the array holds two entries containing
  "console.error or console.warn was called during the test". C0-3 gets an exemption for exactly those `it.fails`
  lines in `setupTests.test.ts` (nowhere else; the `{ fails: true }` options form stays banned). T4's verifier runs
  P10, P10c and P11 as must-fail probes. Consequence: residual R9 closes at T4. Gate re-run self-accepted (test-only
  addition inside the cycle's scope).

## Decisions log (self-accepted gates and judgment calls)

- Gate 1 self-accepted by the orchestrator on 2026-11-08: every task has summary, criteria, fileScope, one module,
  dependsOn, qaTier, qaModel, qaDirective; untestable criteria rewritten (GQ2, GQ8) or pinned (GQ5, GQ6, GQ11); the
  spec is consistent with the mission, tech stack, standards and strategy; no decision outside authority.
- See `shape.md` "Decisions" 1 to 9.
