---
bug: fix-04-order-validation
severity: S3
path: lite
status: open
closes: [spec 04 review finding 1 (quantity status), spec 04 review finding 1b (e-mail trimming)]
---
# Bug: placing an order rejects a single out-of-range line and a space-padded e-mail with 400

## Report
- **Observed, bug 1:** one order line with quantity 0 or 11 gets 400 `validation-failed` (field `lines[0].quantity`).
- **Observed, bug 2:** `customerEmail` with surrounding spaces (`" reader@example.com "`) gets 400 `validation-failed`
  (field `customerEmail`).
- **Expected, bug 1:** 422 `quantity-out-of-range`, nothing reserved. This is the same answer two lines of 6 for one
  book already get. Spec 04, use case step 0: "A merged quantity outside 1 to 10: `InvalidInputException` code
  `quantity-out-of-range` (422)". A single line merges to itself.
- **Expected, bug 2:** the order is accepted and stored with `reader@example.com`, and `GET /orders` finds it.
  Spec 04: `EmailAddress` is "Trimmed, lower-cased". A malformed address stays 400 on field `customerEmail`, which
  spec 12 expects.
- **Environment:** factory dc7edc3, tests on Testcontainers PostgreSQL 18.6.
- **Regression window:** not a regression. Both came in with spec 04 T5 (5bb30a2, PR #36), from orchestrator
  decision O2: "the payload repeats the domain rules so the domain is unreachable".
- **Frequency:** always.
- **Severity: S3.** The wrong status and code is a contract deviation, and clients can work around it. No data is
  wrong, and no money, numbering or auth is involved.

## Reproduction
The existing tests pin the wrong behaviour, and they are green in the referee on dc7edc3 (667/0/0/0):

- **Bug 1:**
  - `OrderCommandsTest.placeRejectsLineQuantitiesOf0And11AndAMissingQuantityWith400` asserts 400 for 0 and 11.
  - `OrderIntegrationTest.invalidPlacePayloadIsRejectedWith400` asserts 400 on `lines[0].quantity` for quantity 0.
- **Bug 2:** `OrderCommandsTest.placeRejectsAMalformedCustomerEmailWith400` lists `" reader@example.com"` and
  `"reader@example.com "` as rejected.
- **Regression-test layer:** end to end (`OrderIntegrationTest`, full context, PostgreSQL, real HTTP), because the
  scenarios cross the adapter, the flow and the store (layer-testing: one integration test per scenario that
  crosses layers). The web slice gets one supporting test each.
- **Command:** `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#<method>' -Dsurefire.failIfNoSpecifiedTests=false`,
  run from the repo root.

## Root cause
- **Bug 1:**
  - **Causal chain:** 400 on `lines[0].quantity`, because bean validation rejects the payload before the flow runs.
    The root cause is `@Min(1) @Max(10)` at `bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java:12`.
  - **Root cause:** the payload repeats the 1 to 10 rule, so the flow's merged-quantity check at `OrderFlow.java:171-183`
    (which already answers 422 `quantity-out-of-range` for 0, 11 and negative values) never sees a single bad line.
  - **Gap:** the spec 04 shape and QA accepted O2 as the "subset property" from spec 03. In spec 03 that property was
    right, because spec 03 itself names 400 for an invalid quantity. In spec 04 it was not, and nobody compared it
    with the step 0 text.
- **Bug 2:**
  - **Causal chain:** 400 on `customerEmail`. The root cause is the `@Pattern` at
    `bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java:17`
    (`[^@\x00-\x20]+@[^@\x00-\x20]+\.[^@\x00-\x20]+`), which rejects any character at or below U+0020, surrounding
    spaces included. `@Size(max = 254)` on the raw value would also reject a padded 254-character address.
  - **Root cause:** the payload validates the raw value, while the domain (`EmailAddress.java:20`,
    `value.trim().toLowerCase(Locale.ROOT)`) validates the trimmed value.
  - **Gap:** the same O2 decision.

Spec 04 orchestrator decision O2 (`.claude/comms/archive/2026-11-08-04-orders/mailboxes/orchestrator/outbox/run-04-orders.md`) is superseded by this fix for the place-order quantity and e-mail constraints. The spec 04 folder `agent-os/specs/2026-11-07-1724-04-orders/` is not edited.

## Blast radius
- **Payload constraints that repeat a domain rule:**
  - `grep -rn -E '@(Min|Max|Pattern|Size)\(' bookstore/src/main/java` finds `ReceiveStockPayload` and
    `CorrectStockPayload` (spec 03), plus `AddBookPayload` and `ChangePricePayload` (spec 02).
  - **Disposition: deferred.** Spec 03 names 400 for an invalid quantity (recorded decision c), and spec 02's regex
    was already fixed so that negative prices reach the domain's 422. Only the two place-order constraints are
    affected.
- **Other callers:** `OrderLinePayload` and `PlaceOrderPayload` are used only by `OrderCommands.place`. No disposition
  needed.
- **Persisted data:** none. Rejected requests store nothing.
- **Produced artifacts:** none.
- **Docs and tests that encode the old behaviour:**
  - the three tests above and `OrderCommandsTest.placeListsEveryFaultyLineFieldInOrder` (quantity 12 listed as a 400
    field) go to tasks T1 and T2;
  - the Javadoc of both payloads goes to T1 and T2;
  - O2 in `agent-os/specs/2026-11-07-1724-04-orders/` is recorded as superseded in `bug.md` (that folder is not edited).
- **Sibling environments:**
  - `http/orders.http` has no out-of-range example. Nothing to change.
  - Spec 12's storefront (not built yet) limits quantity in the UI and expects 400 for `customerEmail`. Both still
    hold.

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | two payload constraints change; flow and domain already implement the rules |
| (b) revert 5bb30a2 | no | removes the whole command adapter |
| (c) guarded workaround + follow-up | no | nothing to guard; the root cause is small |

Chosen: (a), because the 422 path and the trimming already exist behind the payload.

## Prevention
- **Gap:** "the payload accepts only what the domain accepts" was applied without checking which status the spec
  names for the rule.
- **Action, follow-up:** record a standards proposal in progress.md: "a payload constraint must not pre-empt a domain
  rule for which the spec names a 422". The spec 05 onwards review briefs get this check. No standard is changed in
  this cycle.

## Verification
- Regression tests red on unfixed, green on fixed (impl `## Red evidence`, QA `## Negative control`).
- Blast-radius items closed or deferred.
- `./mvnw clean verify` from the repo root green.
