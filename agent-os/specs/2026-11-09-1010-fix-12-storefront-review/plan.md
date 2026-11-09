---
spec: fix-12-storefront-review
source: docs/spec-pack/specs/12-storefront.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-09T10:08:33Z
origin: read-only spec 12 review at 6c17253 (main session), findings R19, R15 and a comment finding
gate1: 2026-11-09T10:11:41Z
qa-lead: on
base: factory
branch: sdd/fix-12-storefront-review/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
kind: bugfix
severity: S3
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [R19 stale storefront after a rebuild, R15 double order and wiped basket, limits.ts comment owner]
---

# Plan: a fresh storefront after every rebuild, one order per submit, and a basket a late answer cannot wipe

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
ROOT = the repository root. FE = `frontend/`. All commands run from ROOT unless they say `in FE`.

## Ground rules

- The pack spec `docs/spec-pack/specs/12-storefront.md`, `docs/spec-pack/product/tech-stack.md` and
  `docs/spec-pack/product/architecture-standard.md` win. Bookstore Java goes through the `hexagonal-slice` skill;
  `StorefrontConfig` is configuration in the root package, so steps 0 and 8 of the skill apply and steps 1 to 7 do not
  (spec 12, Tasks). No new class, no POM change, no new npm or Maven dependency.
- The spec 12 checkout scenarios stay exactly as written: on 202 the basket is emptied of the submitted lines and the
  order page of the answer is shown. Every pre-existing test passes unmodified: new tests are appended, never edited
  into old ones (`git diff <pr-base> --numstat -- <existing test file>` shows 0 deletions).
- Never skip, disable, delete or weaken a test or an ArchUnit rule. Never `--no-verify`. Never set, read or write
  `ANTHROPIC_API_KEY`. ASCII only in every file touched; docs and comments plain English without em dashes.
- Referee: `./mvnw clean verify` from ROOT, green, within 60 seconds. Base referee on 6452d6e (orchestrator,
  2026-11-09): 24.9 s; Vitest 22 files, 588 passed + 2 expected fail (590); bookstore 977, distributor 190.
- Implementers do not git commit, branch or stash; the orchestrator owns git. Implementers never mutate production
  files outside their fileScope to self-probe: describe each mutant in the outbox, the verifier runs it.
- Evidence files in the shared scratchpad carry the role and task prefix (`t1-impl-*`, `t2-qa-*`).

## Path ruling (reference.md section 6)

Lite path (the default) - all hold:
- [x] reproduced in 1a.2: T1 by the jar probe (every `static/shop/` entry dated 1980-02-01) plus the handler code,
  T2 deterministically by reading (page-local `pending`, `basket.clear()` of all lines, live back link)
- [x] root cause named at file:line and confirmed: `StorefrontConfig.java:17-22`, `CheckoutPage.tsx:17` (with `:31`,
  `:57`)
- [x] one obvious fix design readable from the code: a resource handler for the entry document with `no-cache` and no
  date validator; the in-flight state in the basket context
- [x] blast radius enumerable by grep now: bug.md Blast radius (index.html the only unhashed file; AssistantPage
  deferred)
- [x] no already-wrong persisted data or produced artifacts
- [x] not S1; not money, numbering, auth or concurrency in the server sense (the in-flight guard is browser state)
Bundled owner decisions: none. Open question the code does not settle in minutes: none.
-> path: lite (no lead, no research fan-out; three tasks; QA lead registered at Phase 2/3, every task elevated).

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (T1): `no-cache` alone does not fix the 304 branch, because a browser revalidating with `If-Modified-Since`
  against the fixed 1980-02-01 date still gets 304. The entry document is therefore served with `no-cache` AND without
  the date validator (or with a validator that changes with the content). The criteria test the behaviour, a
  conditional GET answered 200, not one mechanism.
- D2 (T1): the policy is scoped to the entry document (`/shop`, `/shop/`, `/shop/index.html`; there is no server-side
  SPA fallback, spec 12 uses hash routing). Hashed assets under `/shop/assets/` keep Boot's default handling, because
  spec 12 says nothing about their caching and the main session said to leave them. Spec 12 "Routes and serving" says
  the static files are served by Boot's default handling; one extra handler for the entry document is the minimal
  deviation and is reported to Maik as a pack follow-up. No global `spring.web.resources.*` property.
- D3 (T2): "a late 202 clears only the lines that were submitted" is read literally: the lines of the books that were
  in the submitted order are removed, whatever their quantity is by then; lines of other books stay. On 202 the order
  page is shown even when the visitor is on another page by then (spec 12: navigate after 202).
- D4 (T2): the back link is replaced, while an order is being placed, by a visible status text that gives the reason;
  it returns when the bookstore has answered with an error.
- D5: models per the main session: sonnet for every implementer and per-task verifier (no critical task), opus for the
  QA lead and qa-final. Never fable.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | status | verdict | qa-lead | pr | branch |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | `/shop`, `/shop/` and `/shop/index.html` are revalidated on every load; assets unchanged | bookstore | [] | 1 | elevated | sonnet | done | PASS r0 | ENDORSE | #105 | sdd/fix-12-storefront-review/T1 |
| T2 | the in-flight order lives in the basket context: one order per submit, back link held, late 202 removes only submitted lines | frontend | [] | 2 | elevated | sonnet | done | FAIL r0, PASS r1 | ENDORSE | #106 | sdd/fix-12-storefront-review/T2 |
| T3 | `limits.ts` names `OrderLine` (spec 04 domain) as the enforcer of the 1 to 10 rule | frontend | [T2] | 3 | elevated | sonnet | done | PASS r0 | ENDORSE | #108 | sdd/fix-12-storefront-review/T3 |

T3 depends on T2 only for ordering (same module, one task at a time).

### T1: the storefront entry document is revalidated on every load

- fileScope: `bookstore/src/main/java/nl/openvalue/bookstore/StorefrontConfig.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java`,
  `bookstore/src/test/resources/static/shop/assets/app-0a1b2c3d.js` (new test resource)
- Acceptance criteria:
  1. Regression test `bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java` > new methods (for
     example `shopEntryDocumentMustBeRevalidated` and `shopEntryDocumentIgnoresDateValidators`, one assertion set per
     path) exist at the end-to-end integration layer (per spec 12 test plan and the layer-testing skill), are named for
     the correct behaviour (not the bug id), and assert the expected behaviour stated in bug.md Reproduction: for each
     of `/shop`, `/shop/` and `/shop/index.html` the answer is 200, `text/html`, the body contains `id="root"`, and the
     `Cache-Control` header is present and its directives include `no-cache`.
  2. For each of the same three paths, a GET carrying `If-Modified-Since` set to the current time (RFC 1123) answers
     200 with the body containing `id="root"`, not 304. If the first answer carries `Last-Modified`, a GET repeating
     that value as `If-Modified-Since` also answers 200 with the body.
  3. Guard (green before and after): the new test resource `static/shop/assets/app-0a1b2c3d.js` served at
     `/shop/assets/app-0a1b2c3d.js` answers 200 and its `Cache-Control`, if any, does not contain `no-cache`
     (hashed assets keep the default handling).
  4. Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside +
     SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest=StorefrontConfigTest -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly the new tests of criteria 1 and 2 with
     `Response does not contain header 'Cache-Control'` (or the AssertJ equivalent naming the missing header) for
     criterion 1 and `expected: 200 but was: 304` (status 304 where 200 is expected) for criterion 2 - not with a
     compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the
     outbox artifact under `## Red evidence` and `## Green evidence`.
  5. No behaviour change outside the root cause: `./mvnw -pl bookstore test` is green (977 + the new tests); the
     production diff is confined to `StorefrontConfig.java`; the five pre-existing methods of
     `StorefrontConfigTest` pass unmodified (`git diff <pr-base> --numstat -- bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java` shows 0 deletions); `ArchitectureTest` green; no
     POM, no `application*.properties` change.
  6. The class comment of `StorefrontConfig` says in plain English that the entry document is revalidated on every
     load (no-cache, no date validator) because the hashed assets it names change with every build; ASCII only.
  7. `./mvnw clean verify` is green within 60 seconds.
- Fix design (readable from the code, the implementer confirms it): in `StorefrontConfig`, `addResourceHandlers`
  registers `/shop/index.html` at `classpath:/static/shop/` with `CacheControl.noCache()` and
  `setUseLastModified(false)`; the two forwards reach that handler. Everything else keeps Boot's default handler.

### T2: the in-flight order lives in the basket context

- fileScope: `frontend/src/state/basket.tsx`, `frontend/src/state/basket.test.tsx`,
  `frontend/src/pages/CheckoutPage.tsx`, `frontend/src/pages/CheckoutPage.test.tsx`, `frontend/src/styles/global.css`
  (only if the status text needs a class; no colour literal outside `tokens.css`)
- Acceptance criteria:
  1. Regression test `frontend/src/pages/CheckoutPage.test.tsx` > "sends one order when the checkout is opened again
     while the first order is being placed" (new `describe` block appended, for example "CheckoutPage while an order is
     being placed") exists at the component layer (per spec 12 test plan: Vitest and Testing Library rendering `App`),
     is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md
     Reproduction (scenario B): seeded basket, Confirm order with the POST held open by a deferred response, go to
     `#/basket` by setting `window.location.hash` (an interface that exists pre-fix), follow "Go to checkout", submit
     again by clicking Confirm order and by `fireEvent.submit` on the form: exactly one `POST /orders/commands/place`
     has been sent before the deferred 202 is released; after the release the order page of the answer is shown.
  2. Regression test `frontend/src/pages/CheckoutPage.test.tsx` > "keeps a book added while the order was being
     placed and removes only the submitted lines" (scenario A): seeded basket of three lines, Confirm order with the
     POST held open, go to `#/books/<id>` of a fourth book (GET of the book and its stock mocked as `BookDetailPage`
     needs), click "Add to basket", release the 202: the order page of the answer is shown, `sessionStorage` key
     `openbook.basket` holds exactly one line, the added book with quantity 1 (asserted on the parsed list with
     `toEqual`), and the basket badge counts 1.
  3. While an order is being placed, the checkout page shows no link named "Back to the basket" and shows a visible
     status text (`role="status"`) saying the order is being placed and the basket can be changed again when the
     bookstore has answered; the Confirm order button is disabled. The same holds on a checkout page opened again
     during the flight (navigate away and back by hash). After an error answer the link is back and the status text is
     gone.
  4. A failure that arrives while a checkout page opened again during the flight is shown: Confirm order with the POST
     held open, go to `#/basket`, back to `#/checkout`, release a 409 insufficient-stock problem: the alert is shown on
     that page, the button is enabled again, and the stored basket is byte for byte the seeded text.
  5. The lines of every submitted book are removed by a late 202 whatever their quantity is by then, and lines of
     other books stay (plan.md D3): a test raises the quantity of one submitted line during the flight and adds a new
     book; after the 202 only the new book's line remains.
  6. Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside +
     SHA-256; never `git checkout --`, `git restore` or `git stash`), `vitest run src/pages/CheckoutPage.test.tsx` (in
     FE, pinned Node from `frontend/node`) FAILS on exactly the tests of criteria 1 and 2 with
     `expected 2 to be 1` (or the equivalent: two place requests where one is expected) for criterion 1 and a stored
     basket of `null` (or the full emptied basket) where the added book's line is expected for criterion 2 - not with a
     compile, import, type or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the
     outbox artifact under `## Red evidence` and `## Green evidence`. Criteria 3 to 5 are green after only.
  7. The fix sits at the root cause: the in-flight state and the "remove the submitted lines" step live in
     `BasketProvider` (`frontend/src/state/basket.tsx`), shared by every checkout page; not a module-level variable or a
     ref in the page. Two submits in the same tick still send one request (the existing spec 12 test stays green).
  8. No behaviour change outside the root cause: all 590 baseline Vitest tests pass (2 of them the expected-fail
     `it.fails`), plus the new ones; every pre-existing test file passes unmodified (`git diff <pr-base> --numstat -- frontend/src/pages/CheckoutPage.test.tsx frontend/src/state/basket.test.tsx` shows 0 deletions); no console error
     or warning (the setupTests trap); no new npm dependency; `npm run build` (tsc and Vite) green.
  9. `./mvnw clean verify` is green within 60 seconds.

### T3: `limits.ts` names the real owner of the 1 to 10 rule

- fileScope: `frontend/src/limits.ts`
- Acceptance criteria:
  1. The comments above `MAX_QUANTITY_PER_LINE` and `MIN_QUANTITY_PER_LINE` say the rule is enforced on the server by
     the `OrderLine` class of the domain (spec 04), in plain English, ASCII only, without em dashes.
  2. `git diff <pr-base> -- frontend/src/limits.ts` changes only those comment lines; no value, no export, no other
     comment changes. `grep -rn "OrderFlow" frontend/src` prints nothing.
  3. Negative control: not applicable, comment-only change (bug.md Reproduction T3); recorded as N/A with that reason.
  4. Vitest green with the same totals as after T2; `./mvnw clean verify` green within 60 seconds.

## QA feed-forward

- FF-T1 (qa-lead, T1 r0; applies to T2): scratch names `f12-t2-qa-*` by absolute path, `.orig` copies of every
  fileScope file at snap-before, repo-relative `shasum -a 256` lines per round; the negative control restores
  `basket.tsx` and `CheckoutPage.tsx` (and `global.css` if touched) from `git show <pr-base>:<file>`, red set exactly
  the criterion 1 and 2 tests; console trap green plus a grep of a stdout+stderr log for `Not implemented|act\(|Warning:`;
  must-fail probes one at a time: (a) position-based removal, (b) removal by exact submitted quantity, (c)
  `basket.clear()` on 202, (d) flag in page state or page ref, (e) flag not reset on error, (f) back link hidden
  without `role="status"` text, or status text with the button enabled; every absence assertion after an awaited
  presence assertion; sessionStorage read right after the 202 without unmounting; criterion 4 byte for byte; cover a
  late success and a late failure on a remounted checkout page.
- FF-T1 (applies to T3): quote `git diff <pr-base> -- frontend/src/limits.ts`; check the named owner against
  `OrderLine.java` (constants and `quantityOf`); Vitest totals equal T2's after-state; T2's gated files unchanged.
- FF-T1 (applies to qa-final): a live `java -jar` check on a throwaway database: `curl -sI` on `/shop`, `/shop/`,
  `/shop/index.html` shows `Cache-Control: no-cache` and no `Last-Modified`; `If-Modified-Since` now and 1980-02-01
  both answer 200 with the body; one hashed asset has no `no-cache`.
- FF-T2 (qa-lead, T2 r1; applies to T3): at snap-before hash T2's five gated files against the T2 baseline in the
  ledger (at the T3 branch tip once T2 is merged); Vitest totals equal T2's after-state (22 files, 598 passed + 2
  expected fail) and the referee log shows `tsc -b && vite build`; `git diff <pr-base> --numstat` lists only
  `frontend/src/limits.ts` (plus agent memory), every changed line is a comment line, and with comments stripped
  `limits.ts` is byte-identical to the PR base.
- FF-T2 (applies to qa-final): a live walk of T2 on the built app in a fresh browser context, holding the place POST
  with a route interception: scenario B sends exactly one place POST (network log); scenario A leaves only the added
  book in `sessionStorage['openbook.basket']` and the badge at 1; the `role="status"` text is visible and the console
  clean; evidence is the request count, the storage value and a screenshot. Check the T2 hash baseline at the spec tip.
  Report (not pass/fail) what happens when the visitor leaves the checkout after a 409 and comes back.

## Amendments

- A1 (2026-11-09, orchestrator, on the QA lead's T2 r1 ruling): T2 criterion 6 reads "fails on the tests of criteria
  1 and 2 with the named shapes and on no pre-existing test; tests of criteria 3 to 5 may fail before and must pass
  after". Consequence: none for the code; the T2 red set of six tests (criteria 1, 2, 5 and three criterion 3/4
  tests) is in line with the criterion. Gate re-run self-accepted.
- Follow-ups for Maik (QA lead residuals, not criteria of this cycle): the last order's error alert stays in the
  basket context until the next order is sent and reappears when the visitor returns to checkout (keep it, or clear
  it when the visitor leaves the checkout); `state/basket.tsx` imports `pages/problem` (`problemOf` could move to
  `api/`).
