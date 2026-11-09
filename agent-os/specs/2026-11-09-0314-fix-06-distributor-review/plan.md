---
spec: fix-06-distributor-review
source: docs/spec-pack/specs/06-distributor-acl.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-09T03:14:15Z
origin: spec 06 review (main session), defect confirmed at 824af83 with the decision on the behaviour, plus two review findings in the same adapter
gate1: 2026-11-09T03:18:09Z
qa-lead: on
base: factory
branch: sdd/fix-06-distributor-review/spec
module-vocabulary: [root, bookstore, distributor]
kind: bugfix
severity: S3
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [spec 06 review finding: ean not compared, finding: cause dropped, finding: CR/LF in logged distributor values]
---

# Plan: a distributor answer for another title is unusable; keep causes; no forged log lines

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
MAIN = `bookstore/src/main/java/nl/openvalue/bookstore`, TEST = `bookstore/src/test/java/nl/openvalue/bookstore`.

## Ground rules

- The pack spec `docs/spec-pack/specs/06-distributor-acl.md` and `docs/spec-pack/product/architecture-standard.md`
  win. Bookstore code goes through the `hexagonal-slice` skill (it pulls in `clean-hexagonal-onion`,
  `ddd-building-blocks` and `layer-testing`). No new class in `acl` (its naming rule allows only `AppServiceImpl` and
  `Properties`): helpers stay private in `DistributorAppServiceImpl`. No POM change, no new dependency.
- Never skip, disable, delete or weaken a test or an ArchUnit rule. Pre-existing tests change only where a task's
  criteria name the line. Never `@DirtiesContext`. Never `--no-verify`. Never set, read or write `ANTHROPIC_API_KEY`.
- ASCII only in every file touched (`rg -n '[^\x00-\x7F]' <file>` prints nothing). Docs plain English, no em dashes.
- Referee: `./mvnw clean verify` from the repo root, 60 s budget (17 s on the factory tip, bookstore 931 tests,
  distributor 190). Run long commands as `bash ~/.claude/scripts/with-budget.sh <s> <command>`.
- Evidence files in the scratchpad carry a role and task prefix (`fix06-t1-impl-*`, `fix06-t1-qa-*`).
- Implementers never edit production files outside their fileScope, and never mutate production files to self-probe:
  describe each mutant in the outbox; the verifier runs it.

## Lite path checklist

- [x] reproduced in 1a.2 (bug.md §Reproduction: the two tests that pin the defect are green on the factory tip)
- [x] root cause named at file:line AND confirmed by the reproduction (`DistributorAppServiceImpl.java:142`,
  `CatalogueFlow.java:92`)
- [x] one obvious fix design readable from the code, decided by the main session (unusable answer, 503, nothing stored;
  flow refuses a foreign DTO)
- [x] blast radius enumerable by grep now (bug.md §Blast radius, every site has a disposition)
- [x] no already-wrong persisted data or produced artifacts
- [x] not S1; not money/numbering/auth/concurrency
Bundled owner decisions in the report: the two review findings (cause, CR/LF), both with an unambiguous design ->
task T3. Open question the code did not settle in minutes: none -> path: lite. The QA lead's strategy assignment is
skipped; the reference §5 directives are the strategy, every task is `qaTier: elevated`.

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (behaviour, main session): an answer whose `ean` does not equal the requested ISBN is unusable: the ACL throws
  `DistributorUnavailableException` through its existing `unusable` path (one WARN "Distributor answer for EAN <isbn>
  is unusable: ..."), the import answers 503 `distributor-unavailable`, nothing is stored. The flow refuses a DTO whose
  isbn differs from the normalised request ISBN with the same exception and the ACL's detail text "The distributor gave
  no usable answer for ISBN <isbn>" (defence in depth; the flow does not log).
- D2 (comparison): exact string equality with the ISBN the ACL was asked for; no normalisation in the ACL (the
  contract's `ean` is a 13-digit string, and the flow always asks with the normalised ISBN). The check runs right
  after `require(ean)`, before the currency check and the genre lookup, so a foreign answer is never "not orderable"
  and never logs a genre warning. In the flow the check runs before the `orderable` check for the same reason.
- D3 (existing tests, deviation from the brief): the main session named two tests to invert. The reproduction shows
  more fixtures with a DTO isbn that differs from the request, which the flow check would turn red without any
  assertion being wrong: `CatalogueFlowTest` lines 484 and 793 and the `lastMan` helper (line 931, used under
  9780141439518 by about twenty tests). Ruling: the mismatch scenario moves into new regression tests; the existing
  tests keep every assertion byte for byte and only the stubbed isbn literal is aligned with the request (plus the
  comment above lines 464 and 85). The line-464 and line-85 tests thereby become the happy-path guards the brief asks
  for (hyphenated request, matching DTO, normalised ISBN stored, override price stored). No assertion is removed or
  weakened; logged here as a spec-driven fixture change.
- D4 (503 detail text): the stubbed `DistributorUnavailableException` in
  `DistributorImportIntegrationTest.anUnavailableDistributorIsReportedWith503AndNothingIsStored` uses the real ACL text
  "The distributor gave no usable answer for ISBN 9780141439518" (stub and assertion), per the brief.
- D5 (escaping): in every distributor-supplied string that reaches a log line, CR is written as the two characters
  `\r`, LF as `\n`, and every other ISO control character (`Character.isISOControl`, which includes U+0085) and
  U+2028/U+2029 as `\uXXXX` with four uppercase hex digits. Values the distributor does not supply (the requested
  ISBN, the numeric status code, exception class names, `BigDecimal.toPlainString()`) are logged as today.
- D6 (causes): `UnavailableException` and `DistributorUnavailableException` get an additional constructor with a
  `Throwable cause`; the existing constructors stay. Every `DistributorUnavailableException` the ACL builds from a
  caught exception carries it (`failed`, `translate`, `priceOf`, `quantityOf`, `dateOf`); the problem response body is
  unchanged (the handlers read code, title, detail and details only).
- D7 (deferred, listed for the owner): the restock answer's `ean` echo is not compared (bug.md §Blast radius); the
  brief's deferred list: field-level 422 codes for distributor data the domain rejects, Jackson string-to-number
  coercion untested, distributor properties documented only in Javadoc, `IsbnTest` Arabic-Indic digits, the
  `WiringTest` timing window.

## Task table

| id | summary | module | dependsOn | batch | qaTier | implModel | qaModel | closes | pr | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | The ACL treats a title answer whose `ean` is not the requested ISBN as unusable (503, one WARN) | bookstore | [] | 1 | elevated | sonnet | sonnet | ean finding (acl) | #90 (branch sdd/fix-06-distributor-review/T1) | ENDORSE (r1, verifier PASS; r0 REJECT) |
| T2 | The import flow refuses a distributor title for another ISBN; fixtures aligned; e2e 503 with nothing stored | bookstore | [T1] | 2 | elevated | sonnet | sonnet | ean finding (flow, e2e) | #93 (branch sdd/fix-06-distributor-review/T2) | ENDORSE (r2, verifier PASS; r0 verifier FAIL, r1 REJECT) |
| T3 | ACL keeps the caught exception as the cause and neutralises line breaks in logged distributor values | bookstore | [T2] | 3 | elevated | sonnet | sonnet | cause and CR/LF findings | #95 (branch sdd/fix-06-distributor-review/T3) | ENDORSE (r0, verifier PASS) |

All three tasks are in the one `bookstore` module and T1/T3 share a file, so they run one at a time in this order.

## Tasks

### T1: a title answer for another EAN is unusable

fileScope: `MAIN/acl/DistributorAppServiceImpl.java`, `TEST/acl/DistributorAppServiceImplTest.java` (additions only)

Acceptance criteria:
1. **Regression test.** New parameterized method `DistributorAppServiceImplTest.aTitleAnswerForAnotherEanIsUnusableWithOneWarning`
   (with `CapturedOutput`). Each case asks `findTitle(<requested>)` and the mock server answers 200 with a body whose
   `ean` differs from the request:
   - requested `9789900010050`, body = the captured `title-9780141439518.json` (another seed title);
   - requested `9789900010050`, body = `variant("title-9789900010050.json", ...)` with `ean` `9789900010051` (last
     digit differs);
   - requested `9789900010050`, `ean` `" 9789900010050"` (leading space) and `ean` `"978-99-0001-005-0"` (hyphenated);
   - requested `9789900010050`, body = the derived `derived-title-usd-currency.json` (foreign and USD: must be
     unusable, NOT `TitleNotOrderableException`);
   - requested `9780141439518`, body = the derived `derived-title-unmapped-subject.json` (foreign and unmapped subject:
     no genre warning).
   For each: `assertThatThrownBy` gives exactly `DistributorUnavailableException` with message "The distributor gave no
   usable answer for ISBN <requested>"; the captured output has exactly one line containing `WARN`, that line contains
   "Distributor answer for EAN <requested> is unusable"; no line contains "No bookstore genre"; `server.verify()`.
2. **Production change.** In `toTitleDTO`, directly after `require(isbn, title.ean(), "ean")`, an answer whose `ean`
   is not `equals` to `isbn` throws `unusable(isbn, "ean " + title.ean() + " is not the requested EAN")` (the reason
   wording may differ; it names the answered ean). The class Javadoc lists "an answer about another EAN" among the
   unusable answers. Nothing else changes.
3. **Matching answers unchanged.** Every existing `DistributorAppServiceImplTest` method passes unmodified
   (`git diff <pr-base> --numstat -- TEST/acl/DistributorAppServiceImplTest.java` shows 0 deletions). The captured
   contract files are unchanged (`git diff <pr-base> --stat -- bookstore/src/test/resources/contracts` prints nothing).
4. **Gates.** `./mvnw -pl bookstore verify` and `./mvnw clean verify` from the repo root are green (60 s budget).
   ASCII only.

Mandatory bugfix criteria (reference §4):
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` > "aTitleAnswerForAnotherEanIsUnusableWithOneWarning" exists at the acl contract-test layer (per the layer-testing skill), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#aTitleAnswerForAnotherEanIsUnusableWithOneWarning' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with `Expecting code to raise a throwable.` for the foreign-EAN cases (the USD case may instead fail with an unexpected `TitleNotOrderableException` type): not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `DistributorAppServiceImpl.java`.)
- No behaviour change outside the root cause: `./mvnw -pl bookstore verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes unmodified (`git diff HEAD --numstat -- <existing test files>` shows 0 deletions).
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#aTitleAnswerForAnotherEanIsUnusableWithOneWarning' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java:142`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl bookstore verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl: build every foreign body from a committed contract file (`contract(...)` or `variant(...)`); never
add or edit a file under `bookstore/src/test/resources/contracts`. Assert the exception type exactly
(`isExactlyInstanceOf`) and the message exactly. Count WARN lines by splitting the captured output with `\R`. Describe
mutants (for example "compare after strip()", "check after the currency check") in the outbox; the verifier runs them.

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#aTitleAnswerForAnotherEanIsUnusableWithOneWarning' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java:142`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
```
Task note for QA: run at least the mutants "comparison after `strip()`", "check moved after the currency check" and
"check moved after `genreOf`"; each must turn a named case red.

leadDirective: evidence chain for T1: impl `## Red evidence` shows `Expecting code to raise a throwable.` (or the
USD case's unexpected `TitleNotOrderableException`) on the unfixed `DistributorAppServiceImpl.java`, not a compile or
fixture error; the verifier's `## Negative control` reproduced it independently; the fix sits at
`DistributorAppServiceImpl.java:142` (`toTitleDTO`, directly after `require(ean)`); no pre-existing test line was
deleted; contract files unchanged.

### T2: the import flow refuses a title for another ISBN

fileScope: `MAIN/domaininteraction/CatalogueFlow.java`, `MAIN/domaininteraction/DistributorAppService.java` (the
`findTitle` Javadoc only), `TEST/domaininteraction/CatalogueFlowTest.java`, `TEST/DistributorImportIntegrationTest.java`

Acceptance criteria:
1. **Flow regression test.** New parameterized method `CatalogueFlowTest.refusesADistributorTitleForAnotherIsbnAndSavesNothing`:
   the request is `"978-0-14-143951-8"`, `distributor.findTitle(ISBN)` returns a DTO with otherwise valid data whose
   isbn is, per case: `"9789900010050"` (orderable), `"9789900010050"` with `orderable = false` (must still be
   unavailable, NOT `TitleNotOrderableException`), `"978-0-14-143951-8"` (the raw hyphenated request) and `null`.
   For each: the thrown exception is exactly `DistributorAppService.DistributorUnavailableException` with code
   `distributor-unavailable` and message "The distributor gave no usable answer for ISBN 9780141439518";
   `verify(distributor).findTitle(ISBN)`; `books.save` and `stockItems.save` are never called.
2. **End-to-end regression test.** New method
   `DistributorImportIntegrationTest.aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored`: the port
   stub answers `findTitle(PRIDE_ISBN)` with `DistributorTitleDTO("9789900010050", "The Last Man", ...)`; importing
   `{"isbn": "978-0-14-143951-8", "price": "12.00"}` gives 503 with `code` `distributor-unavailable`, `title`
   "Distributor unavailable", `detail` "The distributor gave no usable answer for ISBN 9780141439518", `details` `[]`;
   `assertRowCounts(0, 0, 0)`; `verify(distributorAppService).findTitle("9780141439518")` and
   `verifyNoMoreInteractions(distributorAppService)`.
3. **Fixture alignment (D3), no assertion changes.** Only these lines of pre-existing tests change:
   - `CatalogueFlowTest`: the comment at line 463 (now: the request is hyphenated and the distributor answers for the
     normalised ISBN) and the DTO isbn at line 464 (`"9789900010050"` -> `ISBN`); the DTO isbn at line 484
     (`ISBN` -> `"9789900010050"`) and at line 793 (`ISBN` -> `"9789900010050"`); the isbn in the `lastMan` helper at
     line 931 (`"9789900010050"` -> `ISBN`).
   - `DistributorImportIntegrationTest`: the comment at line 84 (same meaning as above) and the DTO isbn at line 85-86
     (`"9789900010050"` -> `PRIDE_ISBN`); the stubbed detail at line 219 and the asserted detail at line 225
     (D4: "The distributor gave no usable answer for ISBN 9780141439518").
   `git diff <pr-base> -- TEST/domaininteraction/CatalogueFlowTest.java TEST/DistributorImportIntegrationTest.java`
   shows deletions on exactly those lines and no other; every other pre-existing test file is untouched.
4. **Production change.** In `CatalogueFlow.importFromDistributor`, directly after the title is found and before the
   `orderable` check: a title whose `isbn()` is not `equals` to the normalised ISBN throws
   `new DistributorAppService.DistributorUnavailableException("The distributor gave no usable answer for ISBN " + normalisedIsbn)`
   (null-safe: `normalisedIsbn.equals(title.isbn())`). The method Javadoc's step list says so, and step 5 no longer
   says the distributor's isbn is ignored. The port's `findTitle` Javadoc says a present result carries the requested
   ISBN and an answer about another title is unusable (`DistributorUnavailableException`). Nothing else changes.
5. **Gates.** `./mvnw -pl bookstore verify` and `./mvnw clean verify` from the repo root are green (60 s budget).
   ASCII only.

Mandatory bugfix criteria (reference §4), one set per regression test:
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlowTest.java` > "refusesADistributorTitleForAnotherIsbnAndSavesNothing" exists at the flow layer (per the layer-testing skill), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='CatalogueFlowTest#refusesADistributorTitleForAnotherIsbnAndSavesNothing' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with an AssertJ failure that no exception was thrown (`Expecting code to raise a throwable.` or `Expecting actual not to be null`; the not-orderable case may instead fail on the unexpected `TitleNotOrderableException` type): not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `CatalogueFlow.java`.)
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/DistributorImportIntegrationTest.java` > "aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored" exists at the end-to-end integration layer (per the layer-testing skill), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='DistributorImportIntegrationTest#aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with a status assertion that expected 503 and received 202: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `CatalogueFlow.java`.)
- No behaviour change outside the root cause: `./mvnw -pl bookstore verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes unmodified except the lines criterion 3 names (`git diff HEAD --numstat -- <existing test files>` shows deletions only there).
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `bookstore/src/test/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlowTest.java` and `bookstore/src/test/java/nl/openvalue/bookstore/DistributorImportIntegrationTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl bookstore test -Dtest='CatalogueFlowTest#refusesADistributorTitleForAnotherIsbnAndSavesNothing' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl bookstore test -Dtest='DistributorImportIntegrationTest#aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java:92`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl bookstore verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl: do the fixture alignment of criterion 3 together with the fix (step 3), not before the red run:
on the unfixed code the aligned fixtures are green either way. Record in the outbox the full `git diff <pr-base> --
<the two test files>` hunks of pre-existing lines, so the reviewer can see that no assertion changed. Do not touch
the ACL (T1 is merged; T3 follows).

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/DistributorAppService.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl bookstore test -Dtest='CatalogueFlowTest#refusesADistributorTitleForAnotherIsbnAndSavesNothing' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl bookstore test -Dtest='DistributorImportIntegrationTest#aDistributorTitleForAnotherIsbnIsReportedWith503AndNothingIsStored' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java:92`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
```
Task note for QA: "a pre-existing test was modified" in step 7 excludes exactly the lines plan.md T2 criterion 3
names (D3); check every changed pre-existing line against that list and confirm no assertion changed. Run at least the
mutants "check after the orderable check", "compare with the raw request instead of the normalised ISBN" and "a
`title.isbn() != null &&` guard"; each must turn a named case red.

leadDirective: evidence chain for T2: impl `## Red evidence` shows, on the unfixed `CatalogueFlow.java`, the
no-exception AssertJ failure for the flow test and the 503-expected/202-received status failure for the e2e test, not
a compile or fixture error; the verifier's `## Negative control` reproduced both independently; the fix sits at
`CatalogueFlow.java:92` (`importFromDistributor`, before the orderable check); the pre-existing test changes are
exactly the D3/D4 lines with no assertion changed.

### T3: keep the cause; no forged log lines from distributor values

fileScope: `MAIN/acl/DistributorAppServiceImpl.java`, `MAIN/domaininteraction/UnavailableException.java` (one added
constructor), `MAIN/domaininteraction/DistributorAppService.java` (one added constructor in
`DistributorUnavailableException`), `TEST/acl/DistributorAppServiceImplTest.java` (additions only)

Acceptance criteria:
1. **Cause regression test.** New parameterized method
   `DistributorAppServiceImplTest.keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable`, one case per path:
   - `findTitle` read timeout (`withException(new SocketTimeoutException())`): cause is a `ResourceAccessException`
     whose cause chain contains the `SocketTimeoutException`;
   - `findTitle` answered 503 with `unavailable-503.json`: cause is a `RestClientResponseException` with status 503;
   - `findTitle` with `releaseDate` `"1813-13-45"`: cause is a `DateTimeParseException`;
   - `findTitle` with `listPriceCents` `999.5`: cause is an `ArithmeticException`;
   - `requestRestock` with `quantity` `5.5`: cause is an `ArithmeticException`;
   - `requestRestock` with `expectedDeliveryDate` `"2026-11-31"`: cause is a `DateTimeParseException`.
   For each, the exception is exactly `DistributorUnavailableException` with the unchanged message "The distributor
   gave no usable answer for ISBN <isbn>". A guard case (green before and after): a 200 title body without `ean` gives
   `DistributorUnavailableException` with a null cause.
2. **Log regression test.** New parameterized method
   `DistributorAppServiceImplTest.neutralisesLineBreaksInLoggedDistributorValues` (with `CapturedOutput`), one case per
   site, each value carrying the marker `forged`:
   - restock `status` `"SHIPPED\r\nWARN forged status"` (line 159);
   - title `releaseDate` `"1813\nWARN forged date"` and restock `expectedDeliveryDate` `"2026\rWARN forged date"`
     (line 200);
   - first subject code `"XYZ\nWARN forged subject"` on an otherwise valid title (line 172, the genre fallback);
   - a foreign `ean` `"9780141439518\nWARN forged ean"` on a request for `9789900010050` (the T1 reason);
   - first subject code `"XYZ forged subject"` (U+2028).
   For each: split the captured output with `\R`; exactly one line contains `forged`, and that line also contains the
   adapter's WARN prefix ("Distributor answer for EAN" or "No bookstore genre for subject code") and the escaped
   value per D5 (for example the literal characters `SHIPPED\r\nWARN forged status` and `XYZ forged subject`).
3. **Production change.** A private static helper in `DistributorAppServiceImpl` escapes per D5 and is null-safe; it
   is applied to every distributor-supplied string that reaches a log line or an `unusable` reason: the restock
   `status` (line 159), the date values (line 200), the subject code (line 172) and the T1 `ean` reason. Exactly one
   WARN per failure, as today, with today's wording around the value. `UnavailableException(String code, String
   title, String detail, Throwable cause)` and `DistributorUnavailableException(String detail, Throwable cause)` are
   added; the existing constructors stay; `failed`, `translate`, `priceOf`, `quantityOf` and `dateOf` pass the caught
   exception (D6). The class Javadoc says causes are kept and logged distributor values are escaped.
4. **Pre-existing tests.** `git diff <pr-base> --numstat -- TEST/acl/DistributorAppServiceImplTest.java` shows 0
   deletions; every other pre-existing test file is untouched; every existing log assertion passes unmodified.
5. **Gates.** `./mvnw -pl bookstore verify` and `./mvnw clean verify` from the repo root are green (60 s budget).
   ASCII only (write U+2028 in Java source as the escape ` `).

Mandatory bugfix criteria (reference §4), one set per regression test:
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` > "keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable" exists at the acl contract-test layer (per the layer-testing skill), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with an AssertJ failure on a null cause (for example `Expecting actual not to be null` or `Expecting actual: null to be an instance of`) in every non-guard case: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `DistributorAppServiceImpl.java` only; the added constructors stay, so the test compiles.)
- Regression test `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` > "neutralisesLineBreaksInLoggedDistributorValues" exists at the acl contract-test layer (per the layer-testing skill), is named for the correct behaviour (not the bug id), and asserts the expected behaviour stated in bug.md §Reproduction.
- Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#neutralisesLineBreaksInLoggedDistributorValues' -Dsurefire.failIfNoSpecifiedTests=false` FAILS on exactly that test with an assertion that the forged text sits on its own line (the line containing `forged` lacks the WARN prefix, or more than one line contains `forged`) in every CR/LF case: not with a compile, import or fixture error; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`. (Restore `DistributorAppServiceImpl.java` only.)
- No behaviour change outside the root cause: `./mvnw -pl bookstore verify` is green; the production diff is confined to the declared fileScope; every pre-existing test file passes unmodified (`git diff HEAD --numstat -- <existing test files>` shows 0 deletions).
- `./mvnw clean verify` is green.

implDirective:
```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` and run ONLY that test against the UNFIXED code: `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#neutralisesLineBreaksInLoggedDistributorValues' -Dsurefire.failIfNoSpecifiedTests=false`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java:234-246` for the cause; `:159`, `:172`, `:200` and the T1 ean reason for the log values), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS: paste under `## Green evidence`. Then run `./mvnw -pl bookstore verify`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```
Task note for impl (ordering): the tests only call `findTitle`/`requestRestock` and read `getCause()` and the
captured output, so they compile on the unfixed code; add the two exception constructors after the red run, together
with the adapter change. The U+2028 case is not a CR/LF case and may already be on one line before the fix; it pins
D5 and needs only be green after. Name each site's case in the outbox with the line it covers. Bodies come from
`variant(...)` of committed contract files; no contract file changes.

qaDirective:
```
Bugfix task. Beyond your standard gate, run an independent negative control: never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/UnavailableException.java bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/DistributorAppService.java`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#keepsTheCaughtExceptionAsTheCauseOfDistributorUnavailable' -Dsurefire.failIfNoSpecifiedTests=false` and `./mvnw -pl bookstore test -Dtest='DistributorAppServiceImplTest#neutralisesLineBreaksInLoggedDistributorValues' -Dsurefire.failIfNoSpecifiedTests=false`. It must FAIL with the failure shape the acceptance criteria name: read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict: the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java:234-246` and `:159`, `:172`, `:200`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
```
Task note for QA (negative control scope): restore only `DistributorAppServiceImpl.java` from `HEAD`; the two added
constructors stay, because restoring them too would make the probe a compile error rather than the named failure.
Run at least the mutants "escape LF only (CR kept)", "escape applied to the status but not the date", "cause dropped
in `translate` only" and "cause dropped in `dateOf` only"; each must turn a named case red. Confirm the problem
response body of a 503 import is unchanged (no cause text).

leadDirective: evidence chain for T3: impl `## Red evidence` shows the null-cause AssertJ failure and the forged-line
assertion failure on the unfixed `DistributorAppServiceImpl.java` (constructors present), not a compile or fixture
error; the verifier's `## Negative control` reproduced both independently; the fix sits at
`DistributorAppServiceImpl.java:234-246` and at the logging sites `:159`, `:172`, `:200` and the T1 ean reason; no
pre-existing test line deleted.

## QA feed-forward

- FF1 (QA lead, T1 r0/r1, for T3): the T3 log test splits the captured output with `\R`, not `String.lines()`
  (`lines()` does not split on U+2028, so the U+2028 case would test nothing); the T3 verifier runs the probe "escape
  CR and LF only, keep U+2028", which must turn the U+2028 case red. T3 edits the T1 `ean` reason line: on T3's code,
  re-run `aTitleAnswerForAnotherEanIsUnusableWithOneWarning` plus the T1 mutants M1 (`strip()`), M2 (check after the
  currency check), M3 (check after `genreOf`), M4 (`startsWith`) and M11 (`stripTrailing`), regenerated against T3's
  production file.
- FF2 (QA lead, T1 r0): an identity or exact-equality criterion needs witnesses that differ from the expected value
  only at the end (trailing space, a longer value with the expected one as prefix), not only at the start or middle.
- FF3 (QA lead, rework rounds): a re-verification round voids carried-forward probe results when an asserting line
  changed; re-run the whole mutant set, and the named mutants must be red on the new cases.
- FF4 (QA lead, T2 r1/r2): for any exact-equality check, witness rows cover both sides and both receiver orders
  (leading pad, trailing pad, extension at the front, extension at the end, proper prefix, proper suffix, null), and
  the mutant set includes `a.op(b)` and `b.op(a)` for op in startsWith/endsWith/contains, plus strip, stripLeading and
  stripTrailing. Late finding on T1 (merged): no T1 case is a proper prefix or suffix of the requested EAN, so
  `isbn.startsWith(title.ean())` and `isbn.endsWith(title.ean())` would survive; closed in T3 by amendment A1.
- FF5 (QA lead, T3 r0, for qa-final): advisories A-1 (no test rows for the ISO-control boundaries U+000C, U+007F,
  U+009F, U+001F, NUL; production uses the decided `Character.isISOControl` form) and A-2 (no assertion that
  `details()` of a caused exception stays empty) were accepted by inspection; qa-final runs them as known survivors,
  re-runs the 503 handler body probe for an exception with a cause on the final tip, runs the four regression tests
  together, and drives one end-to-end 503 import whose ACL failure carries a cause. Orchestrator ruling: no tail task
  (lite path caps tasks at three); the items go to the qa-final brief and, if not raised there, to the follow-ups.

## Amendments

- A1 (2026-11-09, orchestrator, within authority: test fixtures inside a task, per the QA lead's T2 r1/r2
  feed-forward): T3 also adds two additions-only cases to the T1 test
  `DistributorAppServiceImplTest.aTitleAnswerForAnotherEanIsUnusableWithOneWarning`: `ean` `"978990001005"` (a
  proper prefix of the request 9789900010050) and `"789900010050"` (a proper suffix), built with `variant(...)` of
  `title-9789900010050.json`. They are green on the current code (the T1 fix is merged) and pin the T1 criterion; the
  T3 verifier runs `isbn.startsWith(title.ean())` and `isbn.endsWith(title.ean())` (and the rest of the FF4 operator
  family) against `toTitleDTO` as must-fail probes. T3's fileScope is unchanged. Consequence: none on production;
  the T1 test grows from eight to ten cases. Gate 1 re-run self-accepted.
