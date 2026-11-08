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
| T2 | frontend | [] | 2 | elevated | sonnet | `frontend/pom.xml`, create-vite 9.3.4 react-ts scaffold with exact pins, `vite.config.ts`, `setupTests.ts` plus `setupTests.test.ts` guard and console trap (GQ4), smoke test, README; standalone `-f frontend/pom.xml` build | | | | |
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

## Amendments

(none yet)

## Decisions log (self-accepted gates and judgment calls)

- Gate 1 self-accepted by the orchestrator on 2026-11-08: every task has summary, criteria, fileScope, one module,
  dependsOn, qaTier, qaModel, qaDirective; untestable criteria rewritten (GQ2, GQ8) or pinned (GQ5, GQ6, GQ11); the
  spec is consistent with the mission, tech stack, standards and strategy; no decision outside authority.
- See `shape.md` "Decisions" 1 to 9.
