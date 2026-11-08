---
spec: fix-05-distributor-review
source: docs/spec-pack/specs/05-distributor-service.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-08T17:12:25Z
origin: spec 05 read-only review (main session), four findings confirmed as bugs 2026-11-08
gate1: 2026-11-08T17:37:09Z
qa-lead: on
base: factory
branch: sdd/fix-05-distributor-review/spec
module-vocabulary: [root, bookstore, distributor]
kind: bugfix
severity: S3
path: lite+research
fix-strategy: root-cause
regression-since: not-a-regression
closes: [spec 05 review finding 1, finding 2, finding 3, finding 4]
---

# Plan: fix the four spec 05 review findings (distributor)

bug.md (same folder) holds the report, reproduction, root causes, blast radius, strategy and prevention.
MAIN = `distributor/src/main/java/nl/openvalue/distributor`, TEST = `distributor/src/test/java/nl/openvalue/distributor`.

## Ground rules

- The pack spec `docs/spec-pack/specs/05-distributor-service.md` wins. The distributor is a plain Spring app
  (packages `api`, `mcp`, `catalog`); the bookstore skills and ArchUnit rules do not apply to it. Mirror the style of
  the existing spec 05 tests (`ApiTestFixtures.expectErrorEnvelope`, AssertJ, MockMvc).
- Only `distributor/` files and `distributor/README.md` change. No change under `bookstore/`, no POM change, no new
  dependency, no global Jackson setting. Spring Boot 4.1.2 and Spring AI 2.0.2 stay.
- Never skip, disable, delete or weaken a test. Pre-existing tests change only where a task's criteria name it. Never
  `@DirtiesContext`. Never `--no-verify`. Never set, read or write `ANTHROPIC_API_KEY`.
- ASCII only in every file touched (`rg -n '[^\x00-\x7F]' <file>` prints nothing). Docs plain English, no em dashes.
- Referee: `./mvnw clean verify` from the repo root, 60 s budget (about 21 s today). Run long commands as
  `bash ~/.claude/scripts/with-budget.sh <s> <command>`.
- Evidence files in the scratchpad carry a role and task prefix (`fix05-t1-impl-*`, `fix05-t1-qa-*`).

## Lite path checklist

- [x] reproduced in 1a.2 (all four, res-1 probe tree, bug.md §Reproduction)
- [x] root cause named at file:line AND confirmed by the reproduction
- [x] one obvious fix design readable from the code: the framework questions (scoped advice with a null handler,
  Jackson float-to-int coercion, a deterministic test for a thread-safety defect) were settled by one probe each
- [x] blast radius enumerable by grep now (bug.md §Blast radius, every site has a disposition)
- [x] no already-wrong persisted data or produced artifacts
- [x] not S1; not money/numbering/auth. Concurrency: finding 1 is a concurrency defect, but the mechanism is
  JDK-documented and the fix and its test were verified in the probe by the senior researcher, which is what the full
  path would buy; T1 implementation runs on opus per the concurrency model rule. Ruling: lite holds.
Bundled owner decisions in the report: none. Open question the code did not settle in minutes: one combined brief,
res-1 (senior-research-agent, opus), lead: null. -> path: lite+research. The QA lead's strategy assignment is skipped;
the reference §5 directives are the strategy, every task is `qaTier: elevated`.

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (finding 4 error codes): dedicated `METHOD_NOT_ALLOWED` (405) and `UNSUPPORTED_MEDIA_TYPE` (415). Spec 05 names
  no code; the README table keeps one code per status (today `INVALID_REQUEST` means 400 only), codes stay
  self-describing, and spec 06 maps neither (it maps 404 `TITLE_NOT_FOUND`, 422 `NOT_ORDERABLE`, 5xx). Cost: two
  README rows.
- D2 (finding 2 integral floats): `10.0`, `500.0` and `1e2` stay accepted (they are accepted today, and JSON numbers
  carry no int/float distinction). Only values with a non-zero fraction are rejected. The string `"10.0"` becomes
  accepted as a side effect of `BigDecimal` binding (the string `"10"` already is); recorded, harmless.
- D3 (finding 2 message): "quantity must be a whole number between 1 and 500" for every quantity rejection (missing,
  out of range, fractional). "a number between 1 and 500" would be false for 1.5. No test asserts the old text.
- D4 (finding 4 placement): a separate unscoped advice with a `/api/` path guard instead of removing the selectors
  from `ApiExceptionHandler`: removing them would apply every existing handler (for example
  `HttpMessageNotReadableException`) to actuator handler methods. Outside `/api/` the handler rethrows the same
  exception, so Spring's default resolver answers as today.
- D5 (finding 1 test): the regression test is structural (the wired random source yields `ThreadLocalRandom`), behind
  a package-private seam, because no deterministic behavioural test exists through the pre-fix interfaces (bug.md
  §Reproduction). The negative control restores only the root-cause file `ChaosWebConfig.java` from `HEAD`; the
  seam in `ChaosInterceptor.java` stays, so the test compiles and fails on its named assertion.
- D6 (finding 3 design): a shared `@Primary` fixed clock imported by both RANDOM_PORT test classes (one cached
  context, as today) and literal expected dates. The negative control is like-for-like: the same midnight probe clock
  in `CatalogConfig` (probe only, never committed) makes the pre-fix test red and leaves the post-fix test green.
- D7 (out of scope, follow-ups): 406 keeps its empty body; the `/mcp` stack trace on a bad `Accept` header; the review
  checklist additions (bug.md §Prevention).

## Task table

| id | summary | module | dependsOn | batch | qaTier | implModel | qaModel | closes | pr | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Chaos draws are thread-safe; a fractional restock quantity is 400 `INVALID_REQUEST`, never truncated | distributor | [] | 1 | elevated | opus | opus | findings 1, 2 | #62 (branch sdd/fix-05-distributor-review/T1) | ENDORSE (r1, verifier PASS) |
| T2 | 405 and 415 under `/api/` return the envelope with status and `Allow`/`Accept` kept; README lists them | distributor | [T1] | 2 | elevated | sonnet | sonnet | finding 4 | #63 (branch sdd/fix-05-distributor-review/T2) | ENDORSE (r0, verifier PASS) |
| T3 | `checkAvailabilityReportsStockAndDelivery` is deterministic on a shared fixed clock, one context as today | distributor | [T2] | 3 | elevated | sonnet | opus | finding 3 | | |

All three tasks share files (`RestockOrdersApiTest`, `README.md`, `HealthEndpointTest`) and one module, so they run
one at a time in this order.

## Tasks

### T1: thread-safe chaos draws and no truncated quantity

fileScope: `MAIN/api/ChaosInterceptor.java`, `MAIN/api/ChaosWebConfig.java`, `MAIN/api/RestockOrderRequest.java`,
`MAIN/api/RestockOrdersController.java`, `TEST/api/ChaosWebConfigTest.java` (new), `TEST/api/ChaosInterceptorTest.java`
(additions only), `TEST/api/RestockOrdersApiTest.java` (additions only), `distributor/README.md` (the
`POST /api/restock-orders` row only)

Acceptance criteria:
1. **F1 regression test.** New `TEST/api/ChaosWebConfigTest.java` method
   `everyRequestThreadDrawsFromItsOwnThreadLocalRandom`: builds `new ChaosWebConfig(new ChaosProperties(0, 0.5))`,
   calls `addInterceptors` on a test subclass of `InterceptorRegistry` that exposes the protected `getInterceptors()`,
   asserts exactly one `MappedInterceptor` wrapping a `ChaosInterceptor`, calls its package-private `randomSource()`
   `.get()` on a thread other than the test thread, and asserts `isInstanceOf(ThreadLocalRandom.class)`. No
   reflection.
2. **F1 supporting test.** `ChaosInterceptorTest.asksTheRandomSourceForAGeneratorOnEveryDraw`: a recording
   `Supplier<RandomGenerator>` passed to the package-private constructor is called once per `preHandle` (two calls ->
   two supplier calls), which pins "per draw, never cached".
3. **F1 production change.** `ChaosInterceptor` holds a `Supplier<RandomGenerator>`, has a package-private
   constructor taking it and a package-private accessor `randomSource()`, and draws `randomSource.get().nextDouble()`
   only when the failure rate is above 0.0. The public constructor `(ChaosProperties, RandomGenerator, LongConsumer)`
   stays and wraps the instance, so every existing `ChaosInterceptorTest` method passes unmodified.
   `ChaosWebConfig` passes `ThreadLocalRandom::current`; its Javadoc or a comment says every draw asks the calling
   thread's generator.
4. **F2 regression test.** `RestockOrdersApiTest.fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated`:
   for quantities `500.7`, `1.5`, `10.5` and `0.5`, each is 400 with the envelope `INVALID_REQUEST` and the message
   "quantity must be a whole number between 1 and 500".
5. **F2 guard tests (green before and after the fix, all in `RestockOrdersApiTest`).**
   - `integralValuedQuantitiesAreAccepted`: `10.0` -> 201 quantity 10, `1e2` -> 201 quantity 100, `500.0` -> 201
     quantity 500, `"10"` -> 201 quantity 10.
   - `blankEanIsReportedBeforeAFractionalQuantity`: blank ean with quantity `1.5` -> 400 "ean must not be blank".
   - The boundaries on both sides: `1` and `500` -> 201; `0.9999` and `500.0000000001` -> 400 with the quantity
     message (exact, no double rounding).
6. **F2 production change.** `RestockOrderRequest.quantity` is a `BigDecimal` (Javadoc says why); the controller,
   after the ean check, rejects a null, below-1, above-500 or non-whole value (range checked first, so a huge
   exponent never reaches the scale check) with `InvalidRequestException("quantity must be a whole number between 1
   and 500")`, then passes `quantity.intValueExact()`. The response shape is unchanged (integer `quantity`). No
   global Jackson setting, no `application.yml` change.
7. **README.** The `POST /api/restock-orders` row says the quantity is a whole number from 1 to 500. Nothing else in
   the README changes in this task.
8. **Pre-existing tests.** `git diff <pr-base> --numstat -- TEST/api/ChaosInterceptorTest.java TEST/api/RestockOrdersApiTest.java`
   shows 0 deletions. Every other pre-existing test file is untouched.
9. **Gates.** `./mvnw -pl distributor verify` and `./mvnw clean verify` from the repo root are green (60 s budget).
   ASCII only.

Mandatory bugfix criteria (reference §4), one set per regression test:
- Regression test `distributor/src/test/java/nl/openvalue/distributor/api/ChaosWebConfigTest.java` > "everyRequestThreadDrawsFromItsOwnThreadLocalRandom" exists at the unit layer (per the spec 05 test layout), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl distributor test -Dtest='ChaosWebConfigTest#everyRequestThreadDrawsFromItsOwnThreadLocalRandom' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `Expecting actual: jdk.internal.random.L32X64MixRandom@... to be an instance of: java.util.concurrent.ThreadLocalRandom`: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore only `ChaosWebConfig.java`, the root-cause file, per D5.)
- Regression test `distributor/src/test/java/nl/openvalue/distributor/api/RestockOrdersApiTest.java` > "fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated" exists at the web slice layer (per the spec 05 test layout), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `Status expected:<400> but was:<201>`: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `RestockOrderRequest.java` and `RestockOrdersController.java`.)
- No behaviour change outside the root cause: `./mvnw -pl distributor verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes unmodified (`git diff HEAD --numstat -- <existing test files>` shows 0 deletions).
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `distributor/src/test/java/nl/openvalue/distributor/api/ChaosWebConfigTest.java` and `distributor/src/test/java/nl/openvalue/distributor/api/RestockOrdersApiTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl distributor test -Dtest='ChaosWebConfigTest#everyRequestThreadDrawsFromItsOwnThreadLocalRandom' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`distributor/src/main/java/nl/openvalue/distributor/api/ChaosWebConfig.java:26` and `distributor/src/main/java/nl/openvalue/distributor/api/RestockOrderRequest.java:7`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl distributor verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl (F1 ordering): the F1 test needs the package-private seam to compile. Add the seam to `ChaosInterceptor.java` first (the public constructor wraps the instance; behaviour unchanged), keep `ChaosWebConfig.java` as it is, run the F1 test and record the red (the instance-of failure), then change `ChaosWebConfig.java:26`. The seam alone is not the fix; the wiring is. For F2 the test needs no seam: write it, run it red on the unfixed files, then fix. Name both sides of every boundary (`1`/`0.9999`, `500`/`500.0000000001`) and pin every guard test listed in criterion 5.

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- distributor/src/main/java/nl/openvalue/distributor/api/ChaosInterceptor.java distributor/src/main/java/nl/openvalue/distributor/api/ChaosWebConfig.java distributor/src/main/java/nl/openvalue/distributor/api/RestockOrderRequest.java distributor/src/main/java/nl/openvalue/distributor/api/RestockOrdersController.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl distributor test -Dtest='ChaosWebConfigTest#everyRequestThreadDrawsFromItsOwnThreadLocalRandom' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`distributor/src/main/java/nl/openvalue/distributor/api/ChaosWebConfig.java:26` and `distributor/src/main/java/nl/openvalue/distributor/api/RestockOrderRequest.java:7`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
Task-specific (plan.md D5): run the two controls separately. F1: restore ONLY `ChaosWebConfig.java` from HEAD (the root-cause file); `ChaosInterceptor.java` keeps its seam because the test reads it, and restoring it too gives a compile error, which is not the control. F2: restore `RestockOrderRequest.java` and `RestockOrdersController.java` together. Also confirm that a capture-once wiring (`ThreadLocalRandom.current()` passed through the public constructor) is not what the code does, by reading the wiring line (the structural test cannot tell them apart). Pre-existing test files may gain methods but show 0 deletions.
```

leadDirective: evidence chain for T1, two regression tests. F1: `distributor/src/test/java/nl/openvalue/distributor/api/ChaosWebConfigTest.java#everyRequestThreadDrawsFromItsOwnThreadLocalRandom`; failure shape on unfixed `ChaosWebConfig.java`: `Expecting actual: jdk.internal.random.L32X64MixRandom@... to be an instance of: java.util.concurrent.ThreadLocalRandom`; root cause `ChaosWebConfig.java:26` (one shared `RandomGenerator.getDefault()` for the singleton interceptor); the seam in `ChaosInterceptor.java` is test support, the wiring is the fix (plan.md D5); check the wiring is a per-call method reference, not a captured instance. F2: `distributor/src/test/java/nl/openvalue/distributor/api/RestockOrdersApiTest.java#fractionalQuantityIs400WithTheQuantityMessageAndIsNeverTruncated`; failure shape on unfixed `RestockOrderRequest.java` + `RestockOrdersController.java`: `Status expected:<400> but was:<201>`; root cause `RestockOrderRequest.java:7` (`Integer quantity` lets Jackson truncate); guards: integral floats accepted (D2), blank ean first, both sides of 1 and 500. Check impl `## Red evidence`, the verifier's independent `## Negative control` for both, no global Jackson change, and pre-existing test files with 0 deletions.

### T2: 405 and 415 return the envelope

fileScope: `MAIN/api/ApiRequestMismatchHandler.java` (new), `TEST/api/RestockOrdersApiTest.java` (additions only),
`TEST/api/TitlesApiTest.java` (additions only), `TEST/HealthEndpointTest.java` (additions only),
`distributor/README.md` (the error table only)

Acceptance criteria:
1. **Regression tests (web slice).**
   - `RestockOrdersApiTest.wrongMethodIs405WithTheEnvelopeAndTheAllowHeader`: `PUT /api/restock-orders` -> 405,
     envelope `METHOD_NOT_ALLOWED` (via `ApiTestFixtures.expectErrorEnvelope`), header `Allow: POST`.
   - `RestockOrdersApiTest.nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader`: `POST /api/restock-orders` with
     `Content-Type: text/plain` and with no content type -> 415, envelope `UNSUPPORTED_MEDIA_TYPE`, `Accept` header
     contains `application/json`.
   - `TitlesApiTest.wrongMethodIs405WithTheEnvelopeAndTheAllowHeader`: `DELETE /api/titles` -> 405, envelope
     `METHOD_NOT_ALLOWED`, header `Allow: GET`.
   - The 405 message names the method (for example "Method PUT is not allowed here"); the 415 message says the body
     must be `application/json`.
2. **Guard tests (live server, shared context, green before and after).** In `HealthEndpointTest`:
   - `wrongMethodOnActuatorKeepsSpringBootsDefaultBody`: `POST /actuator/health` -> 405, `Allow: GET`, JSON
     `$.status == 405`, no `$.errorCode`.
   - `unknownApiPathKeepsSpringBootsDefault404Body`: `GET /api/no-such-resource` -> 404, `$.status == 404`, no
     `$.errorCode` (the accepted known limit of spec 05).
   They must not add a Spring context (the module run shows the same number of context startups as before, 5).
3. **Production change.** New `MAIN/api/ApiRequestMismatchHandler.java`: an unscoped `@RestControllerAdvice` whose
   Javadoc says why it cannot be scoped (405 is raised before a handler is chosen). It handles
   `HttpRequestMethodNotSupportedException` and `HttpMediaTypeNotSupportedException`: for a request URI under
   `<contextPath>/api/` it returns the `ErrorEnvelope` with the exception's status, the exception's headers (`Allow`,
   `Accept`) and `application/json`; for any other path it rethrows the same exception, so `/actuator` and `/mcp`
   answer as today. `ApiExceptionHandler` is unchanged (plan.md D4). No other production file changes.
4. **README.** The error table gains `405 | METHOD_NOT_ALLOWED | The method is not supported on this path` and
   `415 | UNSUPPORTED_MEDIA_TYPE | A request body that is not application/json` (wording may vary, plain English,
   no em dashes); the "Every error is JSON" line notes that an unknown path answers with Spring Boot's default 404.
5. **Pre-existing tests.** `git diff <pr-base> --numstat` on the three test files shows 0 deletions; every other
   pre-existing test file is untouched.
6. **Gates.** As T1.

Mandatory bugfix criteria (reference §4):
- Regression test `distributor/src/test/java/nl/openvalue/distributor/api/RestockOrdersApiTest.java` > "wrongMethodIs405WithTheEnvelopeAndTheAllowHeader" (with "nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader" and `TitlesApiTest` > "wrongMethodIs405WithTheEnvelopeAndTheAllowHeader") exists at the web slice layer (per the spec 05 test layout), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader+nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader,TitlesApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly those tests with `java.lang.AssertionError: Content type not set`: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (The new `ApiRequestMismatchHandler.java` is moved aside.)
- No behaviour change outside the root cause: `./mvnw -pl distributor verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes unmodified (`git diff HEAD --numstat -- <existing test files>` shows 0 deletions).
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `distributor/src/test/java/nl/openvalue/distributor/api/RestockOrdersApiTest.java` and `distributor/src/test/java/nl/openvalue/distributor/api/TitlesApiTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader+nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader,TitlesApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`distributor/src/main/java/nl/openvalue/distributor/api/ApiExceptionHandler.java:17`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl distributor verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl: the root cause is the scope of `ApiExceptionHandler.java:17`; the fix is the sibling unscoped advice `ApiRequestMismatchHandler.java` (plan.md D4), and `ApiExceptionHandler.java` stays unchanged. Write the two `HealthEndpointTest` guard tests too and show them green before and after; show the path-guard test red when the guard is replaced by `true` (a probe, restored from a SHA'd copy) and record it under `## Red evidence`.

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- distributor/src/main/java/nl/openvalue/distributor/api/ApiRequestMismatchHandler.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl distributor test -Dtest='RestockOrdersApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader+nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader,TitlesApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`distributor/src/main/java/nl/openvalue/distributor/api/ApiExceptionHandler.java:17`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
Task-specific (plan.md D4): the fix for the scoping root cause at `ApiExceptionHandler.java:17` is the sibling unscoped advice `ApiRequestMismatchHandler.java`; an unchanged `ApiExceptionHandler.java` is expected and is not "fix elsewhere". Also run the path-guard control yourself: replace the guard with `true` (SHA'd copy, one run), `HealthEndpointTest#wrongMethodOnActuatorKeepsSpringBootsDefaultBody` must go red; restore. Confirm the guard tests add no Spring context (count context startups in the module run). Pre-existing test files may gain methods but show 0 deletions.
```

leadDirective: evidence chain for T2: regression tests `RestockOrdersApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader`, `RestockOrdersApiTest#nonJsonBodyIs415WithTheEnvelopeAndTheAcceptHeader`, `TitlesApiTest#wrongMethodIs405WithTheEnvelopeAndTheAllowHeader`; failure shape with `ApiRequestMismatchHandler.java` moved aside: `java.lang.AssertionError: Content type not set`; root cause `ApiExceptionHandler.java:17` (scoped advice, never consulted for the null-handler 405, no handler for 415); fix is the sibling unscoped, path-guarded advice (D4), codes per D1. Check impl `## Red evidence`, the verifier's independent `## Negative control`, the path-guard control (guard -> `true` turns the actuator guard test red), the `Allow`/`Accept` headers asserted, the known-limit 404 guard, no new Spring context, and 0 deletions in pre-existing test files.

### T3: deterministic MCP availability test

fileScope: `TEST/FixedClockTestConfiguration.java` (new), `TEST/mcp/McpServerIntegrationTest.java`,
`TEST/HealthEndpointTest.java` (the `@Import` only)

Acceptance criteria:
1. **Shared fixed clock.** New public top-level `TEST/FixedClockTestConfiguration.java`:
   `@TestConfiguration(proxyBeanMethods = false)` with `@Bean @Primary Clock fixedClock()` returning
   `Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneId.of("Europe/Amsterdam"))` (a Friday). The bean method is
   not named `clock`, so no bean overriding is needed. Javadoc says why it is shared (one cached context).
2. **Both RANDOM_PORT classes import it.** `McpServerIntegrationTest` and `HealthEndpointTest` carry the same
   `@Import(FixedClockTestConfiguration.class)`; the module run shows the same number of Spring context startups as
   before (5), and `HealthEndpointTest` reuses the `McpServerIntegrationTest` context.
3. **Regression test (approved change of a pre-existing test).** `McpServerIntegrationTest.checkAvailabilityReportsStockAndDelivery`
   asserts `LocalDate.now(clock)` equals 2026-11-06 (the fixed clock is in effect), and literal dates: IN_STOCK
   `"2026-11-10"` (two working days skip the weekend), BACKORDER `"2026-11-20"`; the OUT_OF_PRINT assertions stay.
   The unused `plusTwoWorkingDays` helper and its imports go. No other method of `McpServerIntegrationTest` changes.
4. **Negative control (like-for-like, plan.md D6).** With the probe `MidnightProbeClock` (below) substituted for the
   production clock in `distributor/src/main/java/nl/openvalue/distributor/catalog/CatalogConfig.java` (copy-aside +
   SHA-256, one run each, restored, never committed): the pre-fix test files (`McpServerIntegrationTest.java` and
   `HealthEndpointTest.java` from `git show HEAD:`, `FixedClockTestConfiguration.java` moved aside) FAIL
   `checkAvailabilityReportsStockAndDelivery` with `expected: {... "expectedDeliveryDate"="2026-11-09" ...} but was:
   {... "expectedDeliveryDate"="2026-11-10" ...}`; the post-fix test files PASS under the same probe.
5. **Pre-existing tests.** Only `checkAvailabilityReportsStockAndDelivery`, the removed helper and imports, and the
   two `@Import` lines change; `HealthEndpointTest` shows 0 deletions; every other test file is untouched. No
   production file changes.
6. **Gates.** As T1.

Probe (CatalogConfig.clock() returns it during the control only):
```java
static final class MidnightProbeClock extends Clock {
    public ZoneId getZone() { return ZoneId.of("Europe/Amsterdam"); }
    public Clock withZone(ZoneId zone) { return this; }
    public Instant instant() {
        return "main".equals(Thread.currentThread().getName())
                ? Instant.parse("2026-11-05T22:59:59.999Z") : Instant.parse("2026-11-05T23:00:00.001Z");
    }
}
```

Mandatory bugfix criteria (reference §4, adapted for a test-only fix: the "pre-fix code" is the pre-fix test, the
probe clock reproduces the midnight crossing):
- Regression test `distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java` > "checkAvailabilityReportsStockAndDelivery" exists at the integration layer (per the spec 05 test layout), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: with the probe clock in `CatalogConfig.java` and the pre-fix test files restored from `git show HEAD:<file>` via copy-aside + SHA-256 (never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl distributor test -Dtest='McpServerIntegrationTest#checkAvailabilityReportsStockAndDelivery' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `expected: {... "expectedDeliveryDate"="2026-11-09" ...} but was: {... "expectedDeliveryDate"="2026-11-10" ...}`: not with a compile, import or fixture error; with the post-fix test files under the same probe it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`.
- No behaviour change outside the root cause: `./mvnw -pl distributor verify` is green; no production file changes; pre-existing test changes are confined to criterion 5.
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl distributor test -Dtest='McpServerIntegrationTest#checkAvailabilityReportsStockAndDelivery' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java:171`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl distributor verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl (test-only fix): "the UNFIXED code" in step 2 means the pre-fix test under the midnight probe clock (plan.md T3 criterion 4): substitute the probe in `CatalogConfig.java` from a SHA'd copy, run the unchanged test red, restore, then write the fix; finally run the post-fix test under the same probe (green) and restore again. `CatalogConfig.java` must be byte-identical to HEAD at the end (`shasum -a 256` and `git status --porcelain` recorded). Count context startups (`grep -c "Starting " ` on the module run log, or the Spring banner count) before and after and record both.

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- distributor/src/test/java/nl/openvalue/distributor/FixedClockTestConfiguration.java distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java distributor/src/test/java/nl/openvalue/distributor/HealthEndpointTest.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl distributor test -Dtest='McpServerIntegrationTest#checkAvailabilityReportsStockAndDelivery' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java:171`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
Task-specific (test-only fix, plan.md D6): the "files of the fix" in steps 1-2 are the three test files (this task has no production change). The control runs under the midnight probe clock: also copy-aside `distributor/src/main/java/nl/openvalue/distributor/catalog/CatalogConfig.java` (SHA-256) and make its `clock()` return the `MidnightProbeClock` from plan.md T3. Run 1: pre-fix test files + probe -> FAIL with the 2026-11-09 vs 2026-11-10 shape. Run 2: post-fix test files + the same probe -> PASS. Then restore `CatalogConfig.java` byte-identical to HEAD. A bracket or "either date" assertion is a FAIL (it weakens the test). Approved exception to step 7 "a pre-existing test was modified": only `checkAvailabilityReportsStockAndDelivery`, the removed `plusTwoWorkingDays` helper and its imports, and the two `@Import` lines change. Confirm the context count is unchanged (5).
```

leadDirective: evidence chain for T3 (test-only fix): regression test `distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java#checkAvailabilityReportsStockAndDelivery`; failure shape of the pre-fix test under the midnight probe clock: `expected: {... "expectedDeliveryDate"="2026-11-09" ...} but was: {... "expectedDeliveryDate"="2026-11-10" ...}`; root cause `McpServerIntegrationTest.java:171` (expectation from the live clock read on the JUnit thread). Check impl `## Red evidence`, the verifier's independent run of both probe runs (pre-fix red, post-fix green under the same probe), `CatalogConfig.java` byte-identical to HEAD afterwards, literal dates (no bracket), the unchanged context count (5), and that pre-existing test changes are confined to criterion 5.

## Final sweep and Gate 2

(filled at the end of the cycle)

## QA feed-forward

- FF-T1 for T2 (qa-lead, T1 meta-review): (1) warning inventory of the module run (`warn|deprecat|self-attach|\|-ERROR`): baseline BeanPostProcessorChecker 6, Mockito block 1+1+4, ToolInputValidator 1, logback empty-pattern 4, context starts 5; any new line is quoted and classified; `Failure in @ExceptionHandler` must count 0 (it falsifies "rethrows the same exception"; prove that once with a probe that rethrows a new exception of the same type). (2) The 415 test asserts a second `Accept` value (`application/yaml`) or the full list, not only "contains application/json"; keep both `Allow` values (POST, GET) and both methods in the 405 messages (PUT, DELETE). (3) One 405 or 415 request with `Accept: application/problem+json` asserts `application/json` and the envelope. (4) implDirective: never edit a production file to probe, not even temporarily and not even inside the fileScope; mutation probes are the verifier's job (describe the mutant in the outbox). (5) T1 methods in `RestockOrdersApiTest.java` unchanged (diff against the T1 commit insertion-only); README: only the error table and the "Every error is JSON" line change. (6) Scratch files `fix05-t2-<role>-*` and a per-round hash file for every fileScope file.
- FF-T1 for T3 (qa-lead): (1) the midnight probe cannot prove the fixed clock reaches the server (its server-thread date equals the fixed date); add a control: post-fix test files with `CatalogConfig.clock()` returning `Clock.fixed(Instant.parse("2000-06-15T10:00:00Z"), ZoneId.of("Europe/Amsterdam"))` (SHA'd copy, one run, restored) must PASS; the BACKORDER literal `2026-11-20` is the one that bites on a real-clock run near the fixed date. (2) Context count 5, BeanPostProcessorChecker 6, `Registered tools: 4` twice unchanged; `HealthEndpointTest` has no own `Started` line. (3) `CatalogConfig.java` byte-identical to HEAD after every probe (hash and porcelain per probe, restore by `cp`). (4) Same implDirective sentence as FF-T2 item 4; scratch prefix `fix05-t3-*`, per-round hash file.
- FF-T2 for T3 (qa-lead, T2 meta-review): (5) new warning baseline: module grep 18 lines (FF-T1 list plus exactly 1 `DefaultHandlerExceptionResolver ... Request method 'POST' is not supported` from the T2 actuator guard), referee 34, `Failure in @ExceptionHandler` 0, context starts 5; the fixed-clock `@Import` on `HealthEndpointTest` must keep it in the context shared with `McpServerIntegrationTest` (identical import on both; a 6th `Started` line is a FAIL). (6) T2 lines in `HealthEndpointTest.java` unchanged: the diff against the T2 commit is insertion-only (the import and the annotation); both T2 guard tests stay green. (7) Artifact `updated` stamps from `date -u`, never estimated.

## Amendments

- A1, 2026-11-08, orchestrator (within authority, sent to qa-t1 as a directive before its r0 run): T1 criterion 5 said every guard test is green before and after the fix. The boundary test's rejected side (`0.9999`, `500.0000000001`) asserts the new D3 message and `500.0000000001` is the truncation defect itself, so it is green only after the fix. `integralValuedQuantitiesAreAccepted`, `blankEanIsReportedBeforeAFractionalQuantity` and the accepted side (1, 500) are green before and after.
- R1, 2026-11-08, from the qa-t1 r0 FAIL: criterion 5's "exact, no double rounding" was not pinned by its own values (both are distinct doubles; a double-based check mutant survived). T1 gained `fractionsBelowDoubleResolutionAreRejectedNotRoundedToTheBound` (`500.00000000000001`, `1.0000000000000001` -> 400), test only; each line kills the mutant.
- A2, 2026-11-08, orchestrator (from FF-T1 item 4): the T2 task note's path-guard probe ("guard -> true", run by the implementer) moved to the verifier, because implementers no longer edit production files to probe; qa-t2 ran it (mutant M1, red as expected).
- Follow-ups from the T2 review (not gate findings, deferred): an optional unit test of `ApiRequestMismatchHandler` that pins the 415 path guard and the context-path prefix (both unreachable in the shipped config, proven by reading lines 38 and 45); the guard reads the raw `getRequestURI()` while Spring matches the decoded path without parameters, so `DELETE /api;v=1/titles` or `/%61pi/titles` may get Boot's 405 body (qa-final records it).
