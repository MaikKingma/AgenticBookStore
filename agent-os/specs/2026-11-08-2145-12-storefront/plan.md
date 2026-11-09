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
| T3 | root | [T1, T2] | 3 | elevated | sonnet | root module list `frontend`, `bookstore`, `distributor`; `actions/cache` for `frontend/node` and the npm cache in both CI jobs; `jar tf` evidence (GQ14) | #74 | sdd/12-storefront/T3 | PASS r2 | ENDORSE |
| T4 | frontend | [T3] | 4 | elevated | sonnet | OpenBook styling foundation: `tokens.css`, self-hosted Montserrat plus OFL, mobile-first `global.css`, contrast test (GQ2, GQ3) and style contract test, title "OpenBook" | #77 | sdd/12-storefront/T4 | PASS r3 | ENDORSE |
| T5 | frontend | [T3] | 5 | critical | opus | API client, ProblemDetail normalisation (including plain Boot error JSON), endpoint functions, `client.test.ts`, fetch mock | #81 | sdd/12-storefront/T5 | PASS r3 | ENDORSE |
| T6 | frontend | [T4, T5] | 6 | elevated | sonnet | in-house `HashRouter`, sticky header with the OpenBook wordmark (light header), `ProblemDetailAlert`, `SearchForm`, `BookCard`, `StockBadge`, `CataloguePage` | #85 | sdd/12-storefront/T6 | PASS r2 | ENDORSE |
| T7 | frontend | [T6] | 7 | elevated | sonnet | `limits.ts`, basket state (sessionStorage), `estimate.ts`, `BookDetailPage`, header badge counting items (GQ11), deep link | #88 | sdd/12-storefront/T7 | PASS r2 | ENDORSE |
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
- FF-T3 (qa-lead, T3 r2; applies to T4 to T10 and qa-final): new baselines: referee 22.249 s (first run including the
  frontend), local warning inventory 34 / 9, CI warnings `verify` 31 and `windows-build` 2. T10 cache-hit evidence:
  caches saved by a pull_request run do not reach later PRs, so cite the save lines of the spec-branch push run plus
  the restore lines of a later run, the npm-Windows restore included. The Windows `npm_config_cache` is set only on the
  `Compile only` step; any later Windows step that runs npm must set the same value. T3 bytes are in the QA ledger.
- FF-T4 (qa-lead, T4 r3; applies to T5 to T10 and qa-final): Vitest baseline 5 files, 132 tests (130 passed + 2
  expected fail); exactly 2 expected fail (setupTests.test.ts:56, :64) up to the spec tip, any other `.fails(` is a C0-3
  FAIL; referee time baseline stays 22.249 s. T6 to T9 use the 17 `CONTRACT_CLASSES` (`wordmark` and `wordmark-book`
  for "Open" and "Book"); `global.css` changes are additions only (`git diff -U0 -- frontend/src/styles/global.css` shows
  no `^-[^-]` line unless the card names it). For every new `color` rule with an ob-on comment the verifier quotes the
  surface it sits on (file:line) and FAILs a claim no CSS backs; a new focusable element on mist, teal or the error
  background needs its ring surface checked (blue 2.91 on mist, 2.73 on teal, 3.70 on error background). C0-6 greps
  scan test titles, comments and fixtures: implementers run the literal C0-6 grep set before handoff and build fixtures
  holding a banned form from string parts; the only exemptions are A2 and A4. T4 bytes (contrast.test.ts except the A5
  insertion in T6, styleContract.test.ts, tokens.css, fonts.css, OFL.txt, woff2, indexHtml.test.ts,
  setupTests.test.ts, setupTests.ts) keep their T4 hashes; only `main.tsx` (T6) and `global.css` (T6 to T9, additions
  only) may change.
- FF-T5 (qa-lead, T5 r3; applies to T6 to T10 and qa-final): Vitest baseline 8 files, 206 tests (204 passed + 2
  expected fail); the 10 T5 files keep their T5 hashes (QA ledger). Pages use the T5 API surface and never
  re-implement it: `getJson`, `postJson`, `ApiError` (`.problem`), `Problem {status, title, detail?, code?, details}`
  (network failure status 0 "Cannot reach the bookstore"; Boot error JSON has no code); pages key on `problem.code`
  (429 on the status); endpoints `searchBooks`, `getBook`, `getStock`, `getOrder`, `findOrders`, `placeOrder`, `chat`;
  test helpers `mockFetch(routes)` (`calls`, `assertAllMatched`, `takeUnmatched`), `jsonResponse`, `problemResponse`,
  `textResponse`; routes match exactly in URLSearchParams encoding. Unmatched-route trap: an unmatched request becomes
  the network alert, so a test expecting "Cannot reach the bookstore" answers through a matched route whose `respond`
  throws and asserts `mock.calls.map((c) => c.path)` exactly; every other error test asserts code-specific text. T7
  renders authors from a list unordered on full string, surname and length (for example `['Ian Goodfellow',
  'Yoshua Bengio', 'Aaron Courville']`) and probes a surname sort. Fixture tabulation: before handoff implementers
  tabulate every array inside a pass-through or order assertion (three or more elements, every attribute a peak,
  valley or zigzag in string and number order, UUIDs one entity each, first hex digit 0 to 7); verifiers rebuild the
  table and run one sort sweep per attribute and direction.
- FF-T6 (qa-lead, T6 r2; applies to T7 to T10 and qa-final; full text QA ledger "FF-T6" 1 to 9): Vitest baseline 11
  files, 332 tests (330 passed + 2 expected fail). Router contract: `useRoute()` (catalogue, book {id}, basket,
  checkout, orders, order {id}, assistant, not-found; ids decoded once), `navigate(path)` sets the hash as given
  (callers encode), `Link to` renders `href="#/..."`; App's `Page` switch is the only route-to-page map; router.tsx and
  router.test.tsx are frozen. PagePending replacement rule per A7. T7 (owns App.tsx, App.test.tsx) asserts the count
  badge and the `Main` navigation inside the banner and `toHaveClass('site-header')` on it. StockBadge (T7) keeps the
  catalogue on `inStock` (T7 verifier re-runs T6 P1, P2, C-badge-inverted); ProblemDetailAlert (T8) keeps its test
  green unchanged (T8 verifier re-runs P8 and the index-key probe). Every stale-answer guard tests a late success and a
  late failure with hand-resolved promises. Every form submit test asserts `event.defaultPrevented` from a `document`
  submit listener removed with `onTestFinished` (probes: removal and a late `setTimeout` cancel). The `Not implemented`
  grep on the verifier's own Vitest log (stderr captured) is the only guard for jsdom errors. T10 and qa-final: live
  sticky header check after scrolling, `PagePending` grep empty, T6 frozen lines hold.
- FF-T7 (qa-lead, T7 r2; applies to T8 to T10 and qa-final; full text QA ledger "FF-T7" 1 to 7): Vitest baseline 14
  files, 457 tests (455 passed + 2 expected fail); setupTests.ts pin b6ac2e72 (A8). Frozen T7 files (basket.tsx,
  basket.test.tsx except the A8 row, estimate.ts, estimate.test.ts, BookDetailPage.tsx, BookDetailPage.test.tsx,
  BasketBadge.tsx, StockBadge.tsx, setupTests.ts) keep their T7 hashes. Pages use `useBasket()` (`lines`, `itemCount`,
  `add`, `setQuantity`, `remove`, `clear`; `add`/`setQuantity` return the refusal message or undefined) and
  `lineTotal`, `basketTotal`, `isMoney` from state/estimate.ts, and show refusal messages from the action's return
  value; tests seed the basket with a raw JSON literal under `openbook.basket` and assert an empty basket as key null.
  T8 basket page: quantity changes from a line at 3 to 0, 11, `''`, `2.5` are refused (input and stored line stay 3),
  1 and 10 accepted; the only `Number(` outside estimate.ts is the quantity handler; trailing-zero witness (12.50 x 2
  shows "25.00"); sort sweep over displayed lines; the estimated total states its currency source. One basket action per
  event handler; checkout calls `clear()` once after the 202. After the 202 the key is null, the header badge shows "0"
  and the order page shows; on 409 the stored basket text is unchanged; error helpers live in T8's own files. T9 adds
  one constant to limits.ts with no removed line. T10: badge updates without reload, basket survives a reload.

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
- A3 (2026-11-08, orchestrator, after the T3 r1 FAIL): the T3 card literal `~\AppData\Local\npm-cache` for the Windows
  npm cache does not exist on `windows-latest` (PR run 42528266851 logged "Path Validation Error" and saved nothing).
  The Windows job now sets the location explicitly: the `Compile only` step gets `env: npm_config_cache:
  ${{ runner.temp }}\npm-cache` and the `Cache npm packages` step caches `${{ runner.temp }}\npm-cache` (the `runner`
  context is allowed in step `env` and `with`). Key, restore-key, pin and step order unchanged; the Linux job keeps
  `~/.npm`, which saved correctly. Consequence: T3 criterion 2's Windows path reads as amended. Gate re-run
  self-accepted.
- A4 (2026-11-08, orchestrator, during T4): `frontend/src/assets/fonts/OFL.txt` is a byte-identical copy of the
  pinned licence (`@fontsource/montserrat` 5.3.0 `LICENSE`, sha256
  `f429f739351287574c0c3175a9222bb3e49c7cad3f568a04f2c08d74c107724e`); a licence file is never edited. The C0-6e URL
  grep and the C0-6c `reserved` grep exempt exactly that file (licence text, not code). Also accepted in T4: the two
  style tests read CSS with `node:fs` because Vitest 5.1.0 empties every CSS import including `?raw` (card criterion 2's
  "Vite `?raw`" reads as amended); the 17 classes of `CONTRACT_CLASSES` in `styleContract.test.ts` are the styling
  contract for T6 to T9: "pinned" means a rule names each class, and declarations are pinned only for the criterion 5
  primitives; Vitest totals count the two `it.fails` "expected fail" tests. Gate re-run self-accepted.
- A5 (2026-11-08, orchestrator, on the QA lead's T4 r3 recommendation): T6's fileScope gains
  `frontend/src/styles/contrast.test.ts` (test-only, insertion-only) for an ob-on surface check: every
  `/* ob-on: ... */` surface on a descendant selector `.X ...` must equal the background of the `.X` rule (or a
  documented ancestor surface), with a fixture proving a false claim fails. Closes residual R10 (deleting the
  `.site-footer` base rule left white footer links on snow undetected). Gate re-run self-accepted.
- A6 (2026-11-09, orchestrator, on the QA lead's T5 r3 recommendation): T5's fileScope includes
  `frontend/src/api/endpoints.test.ts` and `frontend/src/api/fetchMock.test.ts`. Accepted implementer decisions inside
  the card: `searchBooks` omits missing, empty or whitespace-only criteria and sends other values as typed (no trim);
  the `Accept` header is exactly `application/json, application/problem+json`; queries use URLSearchParams encoding;
  a 2xx with a non-JSON, empty or malformed body and an unreadable body become an `ApiError`; `mockFetch` has an
  optional `whenTestFinishes` parameter. C0-17 reads per field (every string sent needs one edge-whitespace witness).
  Residual R11 (no test pins the default `onTestFinished` wiring of `mockFetch`; a deliberately failing test would be
  needed) is carried to Gate 2, mitigated by the T5 hash pin. Gate re-run self-accepted.
- A7 (2026-11-09, orchestrator, on the QA lead's T6 r2 recommendation): accepted T6 implementer decisions inside the
  card: strict route matching (trailing slash, empty id, extra segments, wrong case, malformed escape give "Page not
  found"); active navigation (book marks Catalogue, checkout marks Basket, an order marks Orders, unknown marks
  nothing); genre shown as the server sends it and price as `<price> <currency>`; last search in sessionStorage key
  `openbook.lastSearch`, invalid stored values ignored; an error replaces the list, a non-ApiError shows "Something
  went wrong". PagePending replacement rule: T7 replaces `book`, T8 `basket`, `checkout`, `orders`, `order`, T9
  `assistant`; the App.test.tsx row of a replaced route may change only to install `mockFetch` and to use the real
  page's level-1 heading; after T9 `grep -rn PagePending frontend/src` is empty. Residual R13 (the comment at
  `setupTests.ts:5` claims jsdom "Not implemented" errors reach the console trap; they do not, the C0-7 grep is the
  only guard): T7's fileScope gains a comment-only correction of that line (no code change; the new setupTests.ts
  hash replaces the FF-T2 pin). Gate re-run self-accepted.
- A8 (2026-11-09, orchestrator, on the QA lead's T7 r2 recommendation): accepted T7 implementer decisions inside the
  card: storage key `openbook.basket`, an empty basket removes the key; corrupt storage gives an empty basket; an
  unknown book id on `setQuantity` or `remove` is a no-op; the price parser accepts 0 to 2 fraction digits, else
  `RangeError`; a failed book or stock request shows the alert; the level-1 heading is "Book" while loading or failed;
  the header badge always renders (0 when empty) next to the Basket link inside the banner; `basketTotal` adds
  without a currency check (spec 02 money is EUR only). The setupTests.ts pin is now
  `b6ac2e72aa9032f8299a3e92882f97831b8946e229e67d9a286102ad9c55ac2c` (comment-only change; FF-T2's P10, P10c, P11
  trigger applies to code edits of setupTests.ts only). T8's fileScope gains `frontend/src/state/basket.test.tsx` for
  one insertion-only `['null', null]` row in the mixed-list table (closes survivor N-P9i3). Gate re-run self-accepted.

## Decisions log (self-accepted gates and judgment calls)

- Gate 1 self-accepted by the orchestrator on 2026-11-08: every task has summary, criteria, fileScope, one module,
  dependsOn, qaTier, qaModel, qaDirective; untestable criteria rewritten (GQ2, GQ8) or pinned (GQ5, GQ6, GQ11); the
  spec is consistent with the mission, tech stack, standards and strategy; no decision outside authority.
- See `shape.md` "Decisions" 1 to 9.
