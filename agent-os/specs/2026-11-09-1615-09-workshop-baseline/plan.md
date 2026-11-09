---
spec: 09-workshop-baseline
source: docs/spec-pack/specs/09-workshop-baseline.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-09T16:23:06Z
qa-lead: on
base: factory
spec-branch: sdd/09-workshop-baseline/spec
modules: [root, bookstore, distributor, frontend, guide]
---

# Plan: Spec 09 Workshop baseline (pack tasks 1 to 3)

Restates `docs/spec-pack/specs/09-workshop-baseline.md` (source of truth) for pack tasks 1 to 3. Pack task 4 (run the
publish script against the GitHub remote, verify `main`, tag `level-0-start`) is the owner's gate and is run by the
main session after his approval; pack task 5 (Level 0 rehearsal) is a facilitator activity with its record template in
`rehearsal-record-template.md`. Scope and decisions D1 to D9 are in `shape.md`, standards in `standards.md`, research
in `references.md`. The QA strategy (Gate 1 findings G1 to G14, common directive C0, per-task directives, qa-final
focus, residuals R1 to R6) is in the QA lead's ledger
`.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-09-workshop-baseline.md` (section "QA strategy"), passed to every
agent by path. Precedence for a task's acceptance criteria: this plan's task section, then the Gate 1 findings it
names, then the research reports.

## Summary

Demo seed data loads only outside tests through a repeatable Flyway migration in `db/seed`. The `http/` collection
works against a seeded database and gains `assistant.http` and `demo-flow.http`. `README.md` and
`docs/architecture.md` are written for a developer team. `scripts/publish-workshop-main.sh` builds a single-commit
`main` without agent configuration and moves `level-0-start`, locally by default and on a remote only with `--push`;
its bash test runs in CI on Linux, and the `no-agent-config` workflow guards `main` and the tag with the same path
list. Referee `./mvnw clean verify` from the repo root, green after every task, budget 60 s.

## Ground rules for every task

- The pack spec decides what; `docs/spec-pack/product/architecture-standard.md`, `tech-stack.md` and
  `docs/spec-pack/orchestrator/decisions.md` win. Bookstore code and tests go through the `hexagonal-slice` and
  `layer-testing` skills. A contradiction is reported as blocked, never resolved by the implementer.
- Only the task's fileScope is edited. Mutation probes on production files are the verifier's job: the implementer
  describes each mutant in its outbox.
- Never skip, disable, delete or weaken a test or an ArchUnit rule; never edit `ArchitectureTest.java` or an existing
  migration; never `--no-verify`; no new dependency.
- Plain English, no em dashes; every new or changed file is ASCII. Product files never reference `docs/spec-pack`,
  `agent-os`, `.claude` or `CLAUDE.md`, except the publish script, its test and the guard workflow, which name the
  stripped paths by design. `ANTHROPIC_API_KEY` appears only as the README warning; nobody sets, reads or writes it.
- Never run the publish script against `origin`; never push, force or delete `main` or any tag on GitHub. Git writes
  by agents happen only in scratch repositories; in the project worktree git is read-only for every agent.
- Live runs use an isolated PostgreSQL (own `docker run` on a free port, `--spring.docker.compose.enabled=false`),
  a port preflight and a quoted teardown; never `docker compose down -v` (it removes the shared volume).
- Scratch and evidence files: session scratchpad
  `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/`
  with prefix `s09-T<n>-<role>-`. Long runs through `bash ~/.claude/scripts/with-budget.sh <s> ...`.

## Task table

| id | summary | module | dependsOn | batch | qaTier | qaModel | implModel | status | verdict | qa-lead | rounds | pr | branch | commit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Demo seed, Flyway locations per profile, empty-catalogue guard, seed census test | bookstore | [] | 1 | elevated | sonnet | sonnet | done | PASS | ENDORSE (r0 REJECT) | 1 | #121 | sdd/09-workshop-baseline/T1 | 7664a56 |
| T4 | Publish script, its bash test, ci.yml job, no-agent-config guard | root | [] | 1 | critical | opus | opus | done | PASS (r0 FAIL) | ENDORSE | 1 | #123 | sdd/09-workshop-baseline/T4 | cd354c6, 30303e6 |
| T2 | http collection for a seeded database, assistant.http, demo-flow.http | root | [T1] | 2 | elevated | sonnet | sonnet | done | PASS | ENDORSE | 0 | (see PR list) | sdd/09-workshop-baseline/T2 | 146efe0 |
| T3 | docs/architecture.md and README.md for the team | root | [T1, T2] | 3 | elevated | sonnet | sonnet | done | PASS (r0 BLOCKED by orchestrator, r1, r2) | ENDORSE | 2 | (see PR list) | sdd/09-workshop-baseline/T3 | d604307 |
| T5 | Follow-ups in root: FU-T4-1 test (remote baseline `main` wins) and the two false `http/` header comments (A1, A1b) | root | [T4, T2] | 4 | critical | opus | opus | todo | | | | | | |
| T6 | Folded into T8 (A3): the census test lists every ISBN the http collection adds or imports | bookstore | [T1, T2] | - | elevated | sonnet | sonnet | folded | | | | | | |
| T7 | Spec 02 search cap from 100 to 200 results (D12) | bookstore | [] | 5 | standard | sonnet | sonnet | todo | | | | | | |
| T8 | Seed grows to 100 books, at least 10 AVAILABLE per genre (D12), plus FU-T2-1 | bookstore | [T7] | 6 | (QA strategy) | | | needs-decision (Q1) | | | | | | |
| T9 | Count updates "40" to the new seed count in README.md, http/demo-flow.http and docs (D12) | root | [T8] | 7 | (QA strategy) | | | todo | | | | | | |

Execution order, one task at a time: T1, T4, T2, T3, T5, T7, then T8 and T9 once Q1 is answered.

## T1 Demo seed data and Flyway locations per profile

- fileScope: `bookstore/src/main/resources/application.yml`; `bookstore/src/test/resources/application-test.yml`;
  `bookstore/src/main/resources/db/seed/R__seed_demo_data.sql`; new or changed tests under
  `bookstore/src/test/java/nl/openvalue/bookstore/` (the empty-catalogue guard, a new seed census test class).
- qaDirective: ledger C0 plus T1-A to T1-F.
- Acceptance criteria:
  1. `application.yml` sets `spring.flyway.locations` to `classpath:db/migration,classpath:db/seed`;
     `application-test.yml` sets `classpath:db/migration` only. No file in `db/migration` changes.
  2. `db/seed/R__seed_demo_data.sql` is a repeatable migration in PostgreSQL syntax, ASCII only; every statement is
     idempotent (`INSERT ... ON CONFLICT DO NOTHING` or `INSERT ... SELECT ... WHERE NOT EXISTS`). Executing the
     SQL a second time on a seeded database changes no row: the content of every seeded table is compared, and the
     second run really executes the SQL (G4).
  3. 40 books: exactly the 20 ISBNs of `distributor/src/main/resources/catalog/bookstore-shared-isbns.txt` with the
     distributor's titles and authors; 20 bookstore-only books with checksum-valid ISBN-13s that appear neither in
     the distributor catalogue nor in the `http/` collection (9789900010029, 9780141439471, 9780141439600,
     9780141439563, 9789900010050). At least 6 distinct genres (exact enum names), no genre on more than 10 books;
     2 to 4 discontinued books, all bookstore-only (G1). Every row satisfies the domain restore rules (price in
     (0, 500], publication year 1450 to next year, 1 to 5 authors, fixed UUIDs, version 0).
  4. Every book has a stock item. At least 5 books at or below their reorder level (`on_hand - reserved <=
     reorder_level`), all among the 20 shared ISBNs, and no bookstore-only book at or below its level. Exactly 3
     books with `on_hand = 0`, all shared, and no other book with available 0 (G2). No pending restock. Every item
     above its reorder level has `low_stock_signalled` false; the census pins the value of each item at or below its
     level; at least one AVAILABLE shared book with the signal false sits 1 to 9 copies above its reorder level, so
     one demo order can dip it (the margin book for `demo-flow.http`) (G3). The outbox names the margin book and
     explains the signal choice.
  5. 6 orders for `reader@example.com`, `bookworm@example.org`, `night.owl@example.net` (lower case), 2 SHIPPED
     with `shipped_at` set, 4 PLACED; order lines within the domain limits; for every stock item, `reserved` equals
     the sum of its lines in PLACED orders and `on_hand >= reserved`.
  6. Tests. An integration test in the shared test context asserts an empty catalogue at start in a non-vacuous way:
     `GET /books` (or the catalogue port) is empty, the Flyway bean's configured locations are exactly
     `classpath:db/migration`, the history holds the V0 to V4 rows and no repeatable-migration row (G5). A seed
     census test applies `db/migration` plus `db/seed` with plain Flyway to an isolated database (never the
     application context's datasource, no new Spring context), reads the default-profile locations from the main
     `application.yml` instead of hard-coding them (G6), and asserts every census fact of criteria 3 to 5 plus
     criterion 2.
  7. `./mvnw clean verify` green within 60 s; no test skipped, deleted or weakened; `ArchitectureTest` unchanged.
  8. Live: with the default profile against an isolated PostgreSQL, `GET /books` returns 40 books, `/shop/` lists
     them, and shipping one PLACED demo order succeeds.

## T4 Publish script, its bash test, the no-agent-config guard

- fileScope: `scripts/publish-workshop-main.sh`; `scripts/test-publish-workshop-main.sh`;
  `.github/workflows/no-agent-config.yml`; `.github/workflows/ci.yml` (one new job only).
- qaDirective: ledger C0 plus T4-A to T4-H. Design reference: res-3 report and prototype.
- Acceptance criteria:
  1. `scripts/publish-workshop-main.sh` (bash, executable, LF): the list of 11 paths (`CLAUDE.md`, `CLAUDE.local.md`,
     `AGENTS.md`, `.claude/`, `.mcp.json`, `openspec/`, `.specify/`, `specs/`, `agent-os/`, `.githooks/`,
     `docs/spec-pack/`) lives once, in the script; `--list-paths` prints it; `--check <rev>` prints every tracked
     path under a listed path and exits non-zero naming it, zero otherwise, with the same result from any
     directory of the repository.
  2. Default run: refuses unless HEAD is `factory`, the working tree is clean (tracked changes or untracked
     non-ignored files) and `main` is not checked out in any worktree; builds, from a temporary worktree of
     `factory` that is removed on exit (also on failure), a single parentless commit "Agentic Bookstore: workshop
     baseline" without the listed paths (the `agent-os/product` symlink, `agent-os/specs`, `.claude/agent-memory`
     and `.claude/skills` included); moves local `main` and tag `level-0-start` to it; prints what it removed after
     the refs moved.
  3. `--push [--remote <name>]` (remote `origin` by default) also pushes `main` and the tag in one atomic push with a
     lease on each ref (expected old values; an absent ref expected absent); refuses if local `factory` differs
     from the remote's `factory`. `--dry-run` changes no ref. Without `--push` no network operation happens: the
     default mode succeeds with a broken `origin` URL, and `--remote` is proven with a broken `origin` plus a good
     named remote (G12).
  4. Idempotent: a second run with `factory` unchanged, or changed only inside stripped paths, reuses the existing
     `main` (same commit); a real change makes a new single commit and moves the tag. The test varies the committer
     date between runs, so an "always create a new commit" mutant fails.
  5. Portable: runs on macOS `/bin/bash` 3.2 with BSD tools and on ubuntu-latest bash 5 with GNU tools (no `mapfile`,
     associative arrays, `sed -i`, `readlink -f`, `realpath`, empty-array expansion under `set -u`); every expansion
     quoted (the repo path has a space). shellcheck clean.
  6. `scripts/test-publish-workshop-main.sh` is hermetic (temporary HOME or `GIT_CONFIG_GLOBAL`, local identity, no
     signing) and, against a temporary repository with a `factory` branch holding `CLAUDE.md`, `.claude/`
     (`agent-memory/`, `skills/`, `settings.json`), `agent-os/` (`product` symlink, `specs/`), `docs/spec-pack/` and
     normal files, plus a local bare repository as remote, asserts: `main` has exactly one commit with none of the
     listed paths and the normal files intact (an executable file keeps its bit); `level-0-start` points to `main`
     locally and, after `--push`, on the bare remote; removed paths printed; idempotency; every refusal (dirty tree,
     untracked file, not on `factory`, `main` checked out in a worktree, stale `factory` with `--push`); `--dry-run`
     changes nothing; atomicity and leases (an `update` hook that rejects the tag leaves both refs unmoved; a remote
     `main` moved before the push makes it fail); `--check` fails and names `.claude/settings.json` on a commit that
     adds it, passes on the published `main`, and is not fooled when run from a subdirectory.
  7. `ci.yml` gains one job (ubuntu-latest, SHA-pinned actions, read-only permissions, timeout) that runs the bash
     test on every push and pull request; the existing jobs are unchanged (insertion-only hunk).
     `.github/workflows/no-agent-config.yml` with job `no-agent-config` triggers only on push to `main`, push of tag
     `level-0-start` and pull requests into `main`, runs `bash scripts/publish-workshop-main.sh --check HEAD` from
     the repository top, uses the checkout SHA pin of `ci.yml` with its version comment, `permissions: contents:
     read` and a timeout. actionlint clean.
  8. Proven without the GitHub remote: in a scratch clone of the task branch with the task files copied in and
     committed there, and a temporary bare repository as `origin`, `--push` produces a `main` whose fresh clone has
     one commit, passes the guard, and `./mvnw clean verify` is green on it (budget 900 s, separate from the
     referee) (G13).
  9. Nothing touches GitHub's `main` or tags: `git ls-remote origin main refs/tags/level-0-start` is identical
     before and after every agent run.

## T2 http collection for a seeded database

- fileScope: `http/catalogue.http`; `http/inventory.http`; `http/orders.http`; `http/distributor.http`;
  `http/assistant.http`; `http/demo-flow.http`.
- qaDirective: ledger C0 plus T2-A to T2-F.
- Acceptance criteria:
  1. `catalogue.http`, `inventory.http`, `orders.http` and `distributor.http` each run top to bottom on a freshly
     seeded database (bookstore with the default profile plus the distributor), alone and in the documented order on
     one database, with every response matching the expected status in the comment above each request block; a
     request that depends on a run mode states that mode (G8). No request fails because the seed holds its ISBN
     (today `catalogue.http` adds 9780141439518 and imports 9789900010159, both seeded); `inventory.http` keeps
     9789900010029 (Sense and Sensibility), which the seed leaves out.
  2. `assistant.http` calls `POST /assistant/chat` per the spec 08 contract (body `conversationId` UUID and
     `message`), documents the offline 503 `assistant-offline` answer without `BOOKSTORE_AI_API_KEY` and the 429
     limit. In this cycle it is checked against the spec 08 text and plan and the storefront client; the live
     replay is residual R1, re-run after spec 08 merges (G9).
  3. `demo-flow.http` walks: search, place an order that dips the T1 margin book to or below its reorder level, see
     it in the low-stock list, see its pending restock, receive stock; ids are captured, no UUID literal; each
     step's expected status in a comment.
  4. Plain English comments, no em dashes, ASCII only; no instruction addressed to AI agents; requests use
     `localhost:8080` and `localhost:8090` like the existing files.
  5. Live replay evidence for criteria 1 and 3 against an isolated database.

## T3 docs/architecture.md and README.md for the team

- fileScope: `README.md`; `docs/architecture.md`.
- qaDirective: ledger C0 plus T3-A to T3-F.
- Acceptance criteria:
  1. `README.md`: at most 120 lines, plain English, no em dashes, ASCII only, no text addressed to AI agents (grep
     per the ledger), and these sections (G10): what the app is; prerequisites (Java 25 and Docker; no Node.js, the
     build installs its own); run (`./mvnw verify` once, then `./mvnw -pl bookstore spring-boot:run`, which starts
     PostgreSQL through Docker Compose and serves the storefront at http://localhost:8080/shop/, and
     `./mvnw -pl distributor spring-boot:run`); the jar against an existing PostgreSQL (`SPRING_DATASOURCE_URL`,
     user and password `bookstore`); resetting the database (`docker compose -f bookstore/compose.yaml down`, also
     the fix when an older local database clashes with the seed, R3); module map including `frontend/` and
     `guide/` (one true sentence for `guide/`: the workshop guide site, a Docusaurus project built on its own,
     outside the Maven build) (G11); a short architecture paragraph with the onion diagram and a link to
     `docs/architecture.md`; endpoint overview; Storefront (`/shop/`, the Vite dev server with the Maven-installed
     npm: `frontend/node/npm run dev` in `frontend/` with `frontend/node` on PATH, proxy to 8080); Assistant
     (`BOOKSTORE_AI_API_KEY`, offline mode, the warning not to use `ANTHROPIC_API_KEY`, which appears exactly once
     in the product files); tests (`./mvnw verify` needs Docker running; IDE runs need the test profile); demo data
     (fictional where it is invented, loaded only in local runs, never in tests; Sense and Sensibility 9789900010029
     is left out because `http/inventory.http` adds it).
  2. Every factual claim of the README (ports, commands, endpoints, counts, paths, data claims per statement)
     matches the code on the task branch, or for the assistant the spec 08 contract; the outbox holds a census
     table mapping each claim to its source; the endpoint list is checked both ways.
  3. `docs/architecture.md` is the architecture standard rewritten for the team: layers and the onion, dependency
     rules, naming suffixes, testing per layer, the documented process exception (the one `ignoreDependency`), and
     the synchronous restock trade-off: the after-commit restock call is synchronous, so the order response waits
     for the distributor; the phantom restock (a restock confirmed at the distributor but lost in the bookstore when
     a later commit fails); a pending restock never expires while the distributor keeps restock orders only in
     memory, so after a distributor restart it blocks restocks until a manual receive. Rule and suffix names match
     `ArchitectureTest`. No reference to `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`.
  4. Level 0 traps verified present and not hinted: no discount, percentage or promotion concept; `ddl-auto:
     validate`; the money and JPA-only-in-data ArchUnit rules; one-sentence `package-info.java` files.

## Gate 1 rulings

- G1 to G6, G8 to G10, G12, G13: rewrites accepted into the criteria above.
- G7: option (a). A local database that already holds a shared ISBN under another id is out of scope; the README
  reset line covers it (residual R3).
- G11: the module map keeps `guide/` as the pack requires, with one sentence that is true of the guide site;
  `guide/` is absent from `factory` until spec 11 merges, so a `main` published before then names a missing folder
  (manual check for the owner).
- G14: root-only paths as the pack lists them (residual R4: a nested `CLAUDE.md`, `AGENTS.md` or `.claude/` is not
  guarded; none is tracked today). The optional refusal when `main` is checked out in a worktree is accepted into T4
  criterion 2.

## T5 Follow-ups in root (FU-T4-1, http header comments)

- fileScope: `scripts/test-publish-workshop-main.sh`; `http/catalogue.http`, `http/inventory.http`,
  `http/orders.http` (header comment lines 2 and 4 only).
- qaDirective: ledger C0, T4 directive, review log "T4 r1" (FU-T4-1, R10) and "T2 r0" (A1, A1b carry-over recipe).
- Acceptance criteria:
  0. In `catalogue.http`, `inventory.http` and `orders.http`, only the false sentences change (FF-7): on line 2 the
     sentence "Every ISBN used in this folder is one that the demo data leaves out." becomes "Every ISBN these requests
     add or import is one that the demo data leaves out." and every other sentence on that line stays as it is (the
     demo data count sentence is T9's); line 4 becomes "Run this file once per database: a second run is refused with
     409, because its ISBNs are in the catalogue by then." A diff against the T2 baseline shows only those sentences
     changed; the text gates are re-run with positive controls.
  1. A new test case after the existing reuse cases makes a local parentless commit with the published `main`'s tree,
     the baseline message and a different date, then runs `--push`: exit 0, remote `main` and tag unchanged, local
     `main` equal to the remote's.
  2. The case kills the mutant that swaps the remote-first reuse order (script:217-221) under `/bin/bash` 3.2 with a
     logging shim proving which bash ran; every earlier assertion stays green; no change to the script or workflows.
  3. Same safety rules as T4: scratch repositories only, GitHub's `main` and tags untouched.

## T6 Test-only follow-up for the seed census (FU-T2-1)

- fileScope: `bookstore/src/test/java/nl/openvalue/bookstore/DemoSeedCensusIntegrationTest.java`.
- qaDirective: ledger C0, T1 directive, review log "T2 r0" (FU-T2-1, R12).
- Acceptance criteria:
  1. The constant of ISBNs the http collection adds or imports lists all seven: 9780141439792, 9789900010791,
     9789900010050, 9780141439471, 9789900010029, 9780141439600, 9780141439563.
  2. A seed copy that holds 9780141439792 (and one that holds 9789900010791) fails the census test; the real seed
     stays green; no other test changes; referee green within 60 s.

## T7 Spec 02 search cap from 100 to 200 results (D12)

- fileScope: `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java` (line 37 only);
  `bookstore/src/test/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlowTest.java`; one integration test
  file (a new method in `CatalogueIntegrationTest.java` or a new `*IntegrationTest` extending `IntegrationTestBase`).
- qaDirective: ledger C0 plus T7-A to T7-F (C0-7 not run: no live claim).
- Acceptance criteria (Gate findings G15 to G18 folded in):
  1. The whole production diff is one line, `CatalogueFlow.java:37`, `SEARCH_LIMIT` 100 to 200; the search Javadoc
     keeps `{@value #SEARCH_LIMIT}`. `BookstoreTools`, `BookstoreToolsTest`, `BookDataServiceImpl`, `BookRepository`,
     `BookDataServiceImplTest` and `IntegrationTestBase` stay byte-identical (G18).
  2. `CatalogueFlowTest` lines 424 to 454 pin 200 instead of 100 (method name included); the `never()` check at line
     462 is unchanged; nothing deleted or loosened (G17).
  3. A full-context integration test through `GET /books` (extends `IntegrationTestBase`, no new context key) in two
     phases: 200 books, all 200 titles returned in title order; then one more book whose title sorts first, inserted
     last: exactly 200 returned, that title first, the previously last title gone. Titles share a prefix plus a
     zero-padded number, inserted out of title order, checksum-valid ISBNs whose order follows title order in neither
     direction, every book with a stock item, assertions on the parsed title list (G15, G16, T7-C).
  4. `./mvnw clean verify` green within 60 s, the same number of Spring contexts, the new test under 10 s;
     ArchitectureTest unchanged.

## T8 Seed grows to 100 books (D12), plus FU-T2-1

Blocked on Q1 (amendment A3). Criteria are written once Q1 is answered; T1 criteria 3 to 6 apply with the D12
counts, and the T6 criteria fold in.

## T9 Count updates for the new seed (D12)

- fileScope: `README.md`, `http/demo-flow.http`, `docs/architecture.md`, and line 2 of `http/catalogue.http`,
  `http/inventory.http`, `http/orders.http` (count sentences only). Runs after T8. Covers every "40" or "forty" and
  every derived count such as README "The other 20 books" (N3).

## Amendments

- A1 (2026-11-09, orchestrator, after the T4 r1 ENDORSE): (1) T4 criterion 7 reads "adds no actionlint finding"
  (the base `ci.yml` has one SC2016 info finding at line 28 on the existing verify job, which the insertion-only rule
  keeps) and "runs on every push and pull request that `ci.yml` already triggers on" (its existing triggers). (2) T5
  added as a test-only follow-up for residual R10 (a remote baseline `main` must win over a different local one, or a
  second machine's run would force-move public `main` to an equal-tree commit). (3) T4 accepts `--remote` without a
  value or without `--push` as a usage error (exit 2). Consequence: none for T1 to T3; qa-final gains the T4 hash,
  shim, lint and R10 harness checks.
- A2 (2026-11-09, orchestrator, after the T2 r0 ENDORSE): the false header comments of three `http/` files (A1, A1b)
  join T5, which now also covers those comment lines; T6 added as a test-only follow-up (FU-T2-1, residual R12: the
  census constant missed the two new http ISBNs). Consequence: none for T3; qa-final checks the http hashes against
  the T5 baseline.
- A3 (2026-11-09, orchestrator, on the main session's relay of Maik's decisions D12 and D13; factory 39f5e5e,
  8e7334e, 3d467fc, merged into the spec branch as e4f978a):
  - D12 search cap: new task T7, `CatalogueFlow.SEARCH_LIMIT` from 100 to 200 with its Javadoc and a test that pins
    200 (more than 100 matches returned, the 201st cut off); the assistant's `BookstoreTools` limits stay.
  - D12 seed: new task T8 (depends on T7), the seed grows to 100 books with at least 10 AVAILABLE per genre; the 20
    shared ISBNs stay as they are; the new books are invented, bookstore-only, checksum-valid, unique against the seed,
    the distributor catalogue and 9789900010029, stocked above their reorder level, no pending restock; stock, order
    and reserved rules unchanged. T6 (FU-T2-1) folds into T8 because both edit the census test. New task T9 updates
    every "40" or "forty" in README.md, http/demo-flow.http and docs.
  - Open question Q1, blocking T8: `Genre` has exactly 10 values, so 100 books with at least 10 AVAILABLE per genre
    means every book is AVAILABLE (exactly 10 per genre), which contradicts "a few discontinued (bookstore-only ones)"
    in the same Seed data section and T1 criterion 3. Options: (a) 100 books, all AVAILABLE, drop the discontinued
    line; (b) 103 books, 100 AVAILABLE plus the 3 discontinued bookstore-only books; (c) 100 books, at least 10 per
    genre counting discontinued ones, keep 3 discontinued. Put to the main session; no pick.
  - D13: README and docs never send readers to an Assistant page, nav link or route; the assistant is part of the shop
    at /shop/. Applied in T3 round 2. `POST /assistant/chat` stays, so `http/assistant.http` is unchanged.
  - QA strategy for T7 to T9 comes from the QA lead before each task runs. Consequence: T5 runs as planned; qa-final
    waits for T8 and T9.

## QA feed-forward

- FF-1 (T1): margin book Treasure Island 9789900010715 (on hand 8, reorder level 3, signal false; an order of 5 to 8
  copies dips it, 9 or more exceeds what is available); the 7 items at or below their level carry signal true and
  restock only through the manual request; sold out 9789900010043, 9789900010296, 9789900010777; discontinued
  9789900030119, 9789900030140, 9789900030164; `q=austen&genre=FICTION` matches only Pride and Prejudice. A claim
  that `demo-flow.http` can run again needs a run on a database already changed by a first run. README data claims are
  checked against the same facts.
- FF-2 (T1): more than one test class is selected with `-Dtest=A,B`, never `A+B` (that runs zero tests and still
  reports BUILD SUCCESS); every targeted run shows `Tests run: N` with N above 0. Surefire XML has no timestamps; take
  run order from the `Running` lines.
- FF-3 (qa-final): on the tip seed `grep -c 'ON CONFLICT DO NOTHING'` is 5 and `grep -c 'DO UPDATE'` is 0 (R8); the
  non-ASCII gate runs over every file of the spec diff; the seed SQL and census test hashes equal the T1 baseline.
- FF-4 (T4, all later tasks): `updated` and `created` stamps only from `date -u`, never estimated.
- FF-5 (T4, qa-final): tip hashes of the four T4 files equal the r1 baseline (or the T5 test file its final hash);
  `/bin/bash scripts/test-publish-workshop-main.sh` with a logging Homebrew-bash shim first on PATH is green with 0
  shim calls; shellcheck 0 on both scripts, actionlint only the base SC2016 at `ci.yml:28`; an R10 harness (second
  clone of `factory` without local `main`, default run, then `--push`: remote `main` and tag unchanged) with the
  swapped-order script as a control that force-moves. Residual R9: CI runs the test on Linux bash 5 only, so bash 3.2
  stays a manual macOS check.
- FF-6 (T2, T3 and qa-final): greps use `rg -ni 'a|b'`, never `rg -E` or `-niE` (`-E` is `--encoding`, exit 2);
  "nothing found" is exit 1 with a positive control exiting 0. README claims about the http folder: the collection
  adds or imports 9780141439792, 9789900010791, 9789900010050, 9780141439471, 9789900010029, 9780141439600,
  9780141439563; documented order on one database: catalogue, inventory, orders, distributor, demo-flow; demo-flow
  runs once per fresh database; distributor.http can run again. qa-final: http hashes equal the final baseline, a
  collision check on the tip with a seeded control, demo-flow green on the published clone.
- FF-7 to FF-9 (T3, ledger Cross-task patterns): a "line N reads X" criterion replaces only the named sentence; the
  D13 change cycle must add `POST /assistant/chat/stream` to the README endpoint list (outside this spec); warning
  baselines after the spec 08 merge come from a base run on the PR base, not the pre-merge 142. qa-final adds the T3
  hashes at the tip, the endpoint and architecture scripts on the tip with controls, and the README data claims against
  the seed after T8 and T9.
