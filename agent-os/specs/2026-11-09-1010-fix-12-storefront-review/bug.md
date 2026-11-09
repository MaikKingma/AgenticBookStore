---
bug: fix-12-storefront-review
severity: S3
path: lite
status: fixed
closes: [spec 12 review R19 stale storefront after a rebuild, R15 double order and wiped basket, limits.ts comment]
---
# Bug: the storefront goes blank after a rebuild, and a late order answer doubles the order or wipes the basket

Three findings of the read-only spec 12 review at 6c17253. Each is its own task (T1, T2, T3 in plan.md).

## Report

### R19 (T1): a stale storefront page after a rebuild
- Observed: under `java -jar`, a browser that has seen `/shop/` once keeps the old `index.html` after a rebuild, asks
  for the old `assets/index-<hash>.js`, gets a 404 and shows a blank page.
- Expected: after a rebuild the browser loads the new `index.html` and its new hashed assets.
- Environment: factory at 6c17253 (spec 12 merged), the packaged jar, any browser.
- First seen / last known good: spec 12 review; not a regression (the storefront is new in spec 12).
- Frequency: always, for every visitor who opened the shop before the rebuild.
- Impact: S3. The shop is blank until a forced reload; a forced reload is the workaround.
- Related: `docs/spec-pack/specs/12-storefront.md` (Routes and serving).

### R15 (T2): a double order and a basket wiped by a late answer
- Observed, scenario B: Confirm order, then Back to the basket, then Go to checkout, then Confirm order again sends two
  `POST /orders/commands/place`: two orders, stock reserved twice.
- Observed, scenario A: Confirm order, then Back to the basket, then add a book. The late 202 empties the whole basket,
  the new book included, and navigates to the order page.
- Expected: one order per submit while one is in flight; the back link is not usable while the order is being placed,
  with a visible reason; a late 202 removes only the lines that were submitted. On 202 the basket lines are removed and
  the order page is shown (spec 12 checkout scenario, unchanged).
- Environment: factory at 6c17253, any browser.
- First seen / last known good: spec 12 review; not a regression.
- Frequency: always when the visitor navigates away while the bookstore has not answered yet.
- Impact: S3. Duplicate orders and lost basket lines, and the storefront offers no way to cancel an order; the
  workaround is to wait for the answer before leaving the checkout page.

### T3 (S4): a wrong owner in two comments
- Observed: `frontend/src/limits.ts:5-6` and `:9-10` say the 1 to 10 copies rule is enforced on the server by the
  `OrderFlow` class.
- Expected: they name `domain/OrderLine.java` (spec 04), the one home of that rule (`OrderLine.java:30-37`).

## Reproduction

### T1
- Status: reproduced (mechanism proven by a probe and the code; the regression test is the deterministic replay).
- Evidence: `unzip -l bookstore/target/bookstore-0.0.1-SNAPSHOT.jar` on the base build (orchestrator, 2026-11-09)
  lists every storefront entry, `static/shop/index.html` included, as `02-01-1980 00:00`. Spring's resource handler
  takes `Last-Modified` from the jar entry and answers a conditional GET whose `If-Modified-Since` is not older than
  that with 304. `StorefrontConfig` registers only view controllers, so `index.html` gets Boot's default handling: no
  `Cache-Control` at all and a `Last-Modified` that never changes between builds.
- Recipe: GET `/shop/` and read the headers: no `Cache-Control`. GET `/shop/index.html` with `If-Modified-Since` set
  to the current time: 304, no body.
- Regression-test layer: end-to-end integration test on the real server (`IntegrationTestBase`, `restTestClient`),
  because spec 12 names `StorefrontConfigTest` with MockMvc or `@SpringBootTest` for the storefront entry points and the
  layer-testing skill puts web configuration behaviour there.
- Test file: `bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java` (new methods appended) -
  run with `./mvnw -pl bookstore test -Dtest=StorefrontConfigTest -Dsurefire.failIfNoSpecifiedTests=false`.

### T2
- Status: reproduced by reading (deterministic; the regression tests replay it with a deferred fetch).
- Evidence: `frontend/src/pages/CheckoutPage.tsx:17` keeps `pending` in the page's own state, so a new checkout page
  starts with `pending=false` while the first request is still open; `:23` guards only that page's flag; `:31` calls
  `basket.clear()`, which empties every line present when the answer arrives; `:56-58` renders "Back to the basket"
  as a live link at all times.
- Recipe, scenario B: seed a basket, open `#/checkout`, Confirm with the POST held open, go to `#/basket`, follow
  "Go to checkout", Confirm again: two POSTs. Scenario A: seed, Confirm with the POST held open, go to a book page and
  add a book that is not in the basket, release the 202: the stored basket is gone.
- Regression-test layer: component tests with Vitest and Testing Library rendering `App` (spec 12 test plan:
  `CheckoutPage.test.tsx`, `basket.test.tsx`).
- Test file: `frontend/src/pages/CheckoutPage.test.tsx` (new `describe` block appended) - run in `frontend/` with the
  pinned Node: `vitest run src/pages/CheckoutPage.test.tsx`.

### T3
- Status: confirmed by reading: `bookstore/src/main/java/nl/openvalue/bookstore/domain/OrderLine.java:11-12` holds
  `MIN_QUANTITY = 1`, `MAX_QUANTITY = 10` and `quantityOf` at `:33-37` throws `OrderedQuantityOutOfRangeException`;
  there is no `flow` package holding the rule.
- Regression test: not applicable (comment-only change, no behaviour). The negative control is not applicable either:
  a comment cannot make a test fail. QA checks the two comments by reading and a grep.

## Root cause

### T1
- Causal chain: blank page after a rebuild -> the browser runs an old `index.html` that names assets the new build no
  longer has -> the browser reuses `index.html` by heuristic freshness, or revalidates it with `If-Modified-Since` and
  gets 304 because `Last-Modified` is the fixed jar timestamp 1980-02-01 -> **root cause
  (`bookstore/src/main/java/nl/openvalue/bookstore/StorefrontConfig.java:17-22`): the storefront configuration gives
  the entry document no cache policy, so `index.html` is served without `Cache-Control` and with a date validator that
  never changes** -> missed because spec 12 and its tests check status and body of `/shop/`, never response headers or a
  second, conditional request.
- Root cause (one sentence): `StorefrontConfig` leaves `/shop/index.html` to Boot's default static handling, which
  sends no `Cache-Control` and a `Last-Modified` that the reproducible jar fixes at 1980-02-01, so browsers keep the old
  entry document after a rebuild.
- Contributing factors: the reproducible build timestamp (all jar entries 1980-02-01); hashed assets make a stale
  entry document fatal instead of merely old.
- Introduced by: spec 12 T1 (`StorefrontConfig`), intended: the three entry points and nothing else.

### T2
- Causal chain: duplicate order or wiped basket -> the place-order request outlives the checkout page that started it
  -> **root cause (`frontend/src/pages/CheckoutPage.tsx:17`): the in-flight state of placing an order lives in the
  page, not in the basket context that outlives the page**; its success handler (`:31`) clears whatever the basket
  holds at answer time, and the back link (`:57`) invites leaving while the request is open -> missed because the spec
  12 tests check a second submit only within one page instance.
- Root cause (one sentence): the "an order is being placed" state is page-local, so a remounted checkout page can
  submit again and a late answer acts on a basket that has changed since the submit.
- Contributing factors: `basket.clear()` has no notion of which lines were submitted.
- Introduced by: spec 12 checkout task, intended: disable the button while the bookstore thinks.

### T3
- Root cause: `frontend/src/limits.ts:6` and `:9-10` were written against an earlier assumption about where spec 04
  puts the quantity rule.

## Blast radius

- [x] Same pattern elsewhere. T1: `unzip -l` of the jar shows `index.html` is the only unhashed file under
  `static/shop/` (assets are `index-<hash>.js`, `index-<hash>.css`, `montserrat-*-<hash>.woff2`) -> task T1 covers it;
  assets keep the default handling (spec 12 says nothing about their caching). T2: `grep -rn "pending" frontend/src`
  finds one other page-local in-flight flag, `AssistantPage.tsx:35` -> deferred: a chat send changes no order, stock
  or basket, its messages are page-local by spec 12 design, and the main session scoped this cycle to R15; listed as a
  follow-up for Maik.
- [x] Other callers. `basket.clear()` is called only by `CheckoutPage.tsx:31` (`grep -rn "\.clear()" frontend/src`)
  -> task T2. `StorefrontConfig` has no callers; it is Spring configuration.
- [x] Persisted data already wrong: none. Duplicate orders from R15 would be real orders in a visitor's database; the
  workshop app has no production data -> deferred: nothing to remediate.
- [x] Artifacts already produced: none.
- [x] Docs, standards, ADRs or tests that encode the wrong assumption: `limits.ts` comments -> task T3. The Javadocs
  of `command/PlaceOrderPayload.java:23-24` and `command/OrderLinePayload.java:7-8` say the order flow owns the
  quantity rule -> checked, consistent: layer wording (the use-case flow answers, so a bad quantity is a 422, not a
  400) pinned by fix-04 criterion 4 (`agent-os/specs/2026-11-08-0932-fix-04-order-validation/plan.md`), and the flow
  applies `OrderLine.quantityOf`; the rule lives in `OrderLine` (QA lead ruling, T3 r0). Spec 12
  "Routes and serving" says the static files are served by Boot's default handling; T1 adds a handler for the one entry
  document only and leaves the pack spec untouched (the main session forbids pack edits) -> recorded in plan.md
  Decisions.
- [x] Sibling environments: the Vite dev server (`npm run dev`) serves fresh files itself; CI runs no browser cache ->
  nothing to do.

## Fix strategy

| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | T1: a resource handler for `/shop/index.html` with `no-cache` and no date validator. T2: the in-flight state moves into the basket context. Small, local, testable. |
| (b) revert | no | nothing to revert to; the storefront is new in spec 12 |
| (c) guarded workaround + follow-up | no | e.g. a timestamp query string or a page-level unmount flag would hide one symptom and leave the other |

Chosen: (a) for T1 and T2, because each cause is one place in one class and the fix is readable from the code.

## Prevention

- Gap T1: no test asserts response headers or a revalidation of the entry document of a single page app. Action: the
  T1 regression tests stay as the guard; follow-up for Maik: a sentence in the pack spec 12 "Routes and serving"
  naming the cache policy of `index.html`.
- Gap T2: the spec 12 test plan checks a second submit within one page instance only. Action: the T2 regression tests
  stay as the guard; follow-up: none beyond the AssistantPage item above.
- Gap T3: comments that name an owner class are not checked. Action: none, because a grep guard on comment text would
  cost more than it saves.

## Verification

- Regression tests red on unfixed, green on fixed (impl `## Red evidence`, QA `## Negative control`) for T1 and T2.
- Blast-radius items closed or deferred as listed above.
- `./mvnw clean verify` green within 60 seconds.
