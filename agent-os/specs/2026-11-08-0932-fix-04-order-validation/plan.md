---
spec: fix-04-order-validation
source: docs/spec-pack/specs/04-orders.md
status: in-progress
accepted-by: orchestrator (main session, self-accepted per Maik 2026-11-08)
created: 2026-11-08T09:32:31Z
origin: spec 04 read-only review (main session), decisions by Maik 2026-11-08
gate1: 2026-11-08T09:30:00Z
qa-lead: on
base: factory
branch: sdd/fix-04-order-validation/spec
module-vocabulary: [root, bookstore, distributor]
kind: bugfix
severity: S3
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [spec 04 review finding 1, spec 04 review finding 1b]
---

# Plan: fix spec 04 place-order validation

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention. MAIN = `bookstore/src/main/java/nl/openvalue/bookstore`, TEST = `bookstore/src/test/java/nl/openvalue/bookstore`.

## Ground rules

- The pack spec `docs/spec-pack/specs/04-orders.md` and `docs/spec-pack/product/architecture-standard.md` win over skills. Load the project skills `hexagonal-slice`, `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing` before writing bookstore code; mirror the style of the existing spec 04 tests.
- Never skip, disable, delete or weaken a test or an ArchUnit rule, except the pre-existing test changes the acceptance criteria name (Maik's decided behaviour). Never `@DirtiesContext`, never `--no-verify`, no POM change.
- ASCII only in every file touched (`rg -n '[^\x00-\x7F]' <file>` empty); non-ASCII test data from code points. No word "cancel" or "release" in `bookstore/src/main`. Plain English, no em dashes.
- Referee: `./mvnw clean verify` from the repo root (Docker running), 60 s budget.

## Lite path checklist

- [x] reproduced in 1a.2 (existing green tests pin both behaviours)
- [x] root cause named at file:line AND confirmed by the reproduction
- [x] one obvious fix design readable from the code
- [x] blast radius enumerable by grep (bug.md)
- [x] no already-wrong persisted data or produced artifacts
- [x] not S1; not money/numbering/auth/concurrency
Bundled owner decisions: both behaviours are Maik's decisions of 2026-11-08, one task each. Open question: none. path: lite.

## Task table

| id | summary | module | dependsOn | batch | qaTier | implModel | qaModel | pr | qa-lead |
|---|---|---|---|---|---|---|---|---|---|
| T1 | A single order line with quantity 0, 11 or -1 gets 422 `quantity-out-of-range`; a missing quantity stays 400 | bookstore | [] | 1 | elevated | sonnet | opus | | |
| T2 | Place accepts a `customerEmail` with surrounding whitespace, stores it trimmed and lower-cased; malformed stays 400 | bookstore | [T1] | 2 | elevated | sonnet | opus | | |

## Tasks

### T1: single-line quantity outside 1 to 10 is 422

fileScope: `MAIN/command/OrderLinePayload.java`, `TEST/command/OrderCommandsTest.java`, `TEST/OrderIntegrationTest.java`

Acceptance criteria:
1. **Integration regression test.** `OrderIntegrationTest` gains
   `aSingleLineWithAQuantityOutside1To10IsRejectedWith422` (parameterized: 0, 11, -1). Each case asserts:
   - 422 `quantity-out-of-range`;
   - stock unchanged;
   - no order stored and no event committed.
2. **Slice regression test.** `OrderCommandsTest` gains `placePassesALineQuantityOutside1To10ToTheFlow` (0 and 11).
   It asserts that the flow is called with those quantities, and that the flow's `InvalidInputException` maps to 422.
3. **Negative control (reference.md §4).** On the pre-fix `OrderLinePayload.java`, the integration test fails with
   expected status 422 but actual 400. With the fix it passes.
4. **Production change.** `OrderLinePayload` keeps `@NotNull` on quantity and loses `@Min` and `@Max`. Its Javadoc
   says the flow owns the 1 to 10 rule.
5. **Pre-existing tests, the approved exception.** Only these change, to Maik's decided behaviour:
   - `placeRejectsLineQuantitiesOf0And11AndAMissingQuantityWith400` keeps only the missing-quantity case, renamed
     `placeRejectsAMissingLineQuantityWith400`;
   - `placeListsEveryFaultyLineFieldInOrder` no longer lists `lines[0].quantity`;
   - `invalidPlacePayloadIsRejectedWith400` keeps the bad-e-mail case and moves quantity 0 to the new test.

   No other existing test changes: `git diff --numstat` shows deletions only in those methods.
6. **Gates.** The bookstore suite and `./mvnw clean verify` from the repo root are green. No "cancel" in
   `bookstore/src/main/java`. ASCII only.

Mandatory bugfix criteria (reference §4):
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java` > "aSingleLineWithAQuantityOutside1To10IsRejectedWith422" exists at the end-to-end layer (per layer-testing), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#aSingleLineWithAQuantityOutside1To10IsRejectedWith422' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `expected status 422 UNPROCESSABLE_CONTENT, received 400 BAD_REQUEST`: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`.
- No behaviour change outside the root cause: `./mvnw -pl bookstore verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes, modified only in the methods named above.
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#aSingleLineWithAQuantityOutside1To10IsRejectedWith422' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java:12`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl bookstore verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#aSingleLineWithAQuantityOutside1To10IsRejectedWith422' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java:12`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
Approved exception to step 7 "a pre-existing test was modified" (Maik decisions 2026-11-08, Gate 1): only the pre-existing test methods named in this task's acceptance criteria change, to the decided behaviour; any other modified or weakened pre-existing test is still a FAIL.
```

leadDirective: evidence chain for T1: regression test `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java#aSingleLineWithAQuantityOutside1To10IsRejectedWith422`; failure shape on unfixed code: status 400 `validation-failed` on `lines[0].quantity` where 422 `quantity-out-of-range` is expected; root cause `bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java:12` (`@Min(1) @Max(10)` pre-empting the flow's merged-quantity rule). Check impl `## Red evidence`, the verifier's independent `## Negative control`, that the fix is the removal of those two constraints only, and that pre-existing test changes are confined to the three named methods.

### T2: space-padded e-mail is accepted and normalised

fileScope: `MAIN/command/PlaceOrderPayload.java`, `TEST/command/OrderCommandsTest.java`, `TEST/OrderIntegrationTest.java`

Acceptance criteria:
1. **Integration regression test.** `OrderIntegrationTest` gains `placeTrimsAndLowerCasesASpacePaddedCustomerEmail`.
   The request is `"  Reader@Example.com "` with one line, and the test asserts:
   - 202;
   - `GET /orders?customerEmail=reader@example.com` lists the order;
   - the stored `customer_email` is `reader@example.com`.
2. **Slice regression test.** `OrderCommandsTest` gains `placePassesASpacePaddedEmailToTheFlowUnchanged`. It covers a
   leading space, a trailing space and a tab, and asserts the flow receives the raw value.
3. **Length rules.**
   - A 254-character address with one leading and one trailing space is accepted.
   - An unpadded 255-character address is still 400 on `customerEmail`.
   - A padded address whose trimmed length is 255 is 400 on `customerEmail`.
4. **Malformed addresses stay 400 on `customerEmail`.** Every other case of `placeRejectsAMalformedCustomerEmailWith400`
   still gives 400. That includes inner spaces and tabs, a missing `@`, two `@`, and the empty and blank values.
5. **Negative control.** On the pre-fix `PlaceOrderPayload.java`, the integration test fails with expected status 202
   but actual 400. With the fix it passes.
6. **Production change.** Only the constraints and the Javadoc of `PlaceOrderPayload.customerEmail` change. The new
   rules allow characters at or below U+0020 around the address, the same set `String.trim()` strips, and apply the
   254 limit to the trimmed value. The domain stays unchanged.
7. **Pre-existing tests, the approved exception.** The two padded cases move from the malformed list to the new
   accepted test. Nothing else changes.
8. **Gates.** Same as T1.

Each task also gets the verbatim reference.md §5 `implDirective` and `qaDirective`, and a `leadDirective` with the
evidence chain (test path, failure shape, root cause file:line).

Mandatory bugfix criteria (reference §4):
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java` > "placeTrimsAndLowerCasesASpacePaddedCustomerEmail" exists at the end-to-end layer (per layer-testing), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#placeTrimsAndLowerCasesASpacePaddedCustomerEmail' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `expected status 202 ACCEPTED, received 400 BAD_REQUEST`: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`.
- No behaviour change outside the root cause: `./mvnw -pl bookstore verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes, modified only where criterion 7 names.
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#placeTrimsAndLowerCasesASpacePaddedCustomerEmail' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java:17`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl bookstore verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl bookstore test -Dtest='OrderIntegrationTest#placeTrimsAndLowerCasesASpacePaddedCustomerEmail' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java:17`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
Approved exception to step 7 "a pre-existing test was modified" (Maik decisions 2026-11-08, Gate 1): only the pre-existing test methods named in this task's acceptance criteria change, to the decided behaviour; any other modified or weakened pre-existing test is still a FAIL.
```

leadDirective: evidence chain for T2: regression test `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java#placeTrimsAndLowerCasesASpacePaddedCustomerEmail`; failure shape on unfixed code: status 400 `validation-failed` on `customerEmail` where 202 is expected; root cause `bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java:17` (the `@Pattern` and raw-value `@Size(max = 254)` validating the untrimmed value). Check impl `## Red evidence`, the verifier's independent `## Negative control`, that only the constraints and Javadoc of `customerEmail` change, that malformed addresses (inner whitespace, missing or double `@`, empty, blank, trimmed length 255) still give 400 on `customerEmail`, and that pre-existing test changes are confined to moving the two padded cases.

## QA feed-forward

(none yet)

## Amendments

(none)
