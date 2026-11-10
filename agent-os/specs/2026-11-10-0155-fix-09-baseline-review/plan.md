---
spec: fix-09-baseline-review
source: docs/spec-pack/specs/09-workshop-baseline.md
status: done
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-10T01:57:00Z
origin: fresh read-only review of spec 09 at dfcd01b (main session); decision D15 (spec text at dfcd01b)
gate1: 2026-11-10T01:58:30Z
qa-lead: on
gate2: 2026-11-10T03:45:46Z
base: factory
branch: sdd/fix-09-baseline-review/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
kind: bugfix
severity: S2
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [review S2 migration history test pins the migration list, review S3 assistant-unavailable undocumented, D15 publish script strips the guide]
---

# Plan: a new migration keeps the seed guard green; the failed-call 503 is documented; the publish list holds the guide

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
ROOT = the repository root. All commands run from ROOT. `<pr-base>` = `origin/sdd/fix-09-baseline-review/spec` at
the moment the task branch was cut. `SCRATCH` = the session scratchpad named in the task prompt.

## Ground rules

- The pack wins: `docs/spec-pack/specs/09-workshop-baseline.md` (Requirement "Demo data in local runs only", "Human
  documentation", "Publishing main"), `docs/spec-pack/orchestrator/decisions.md` D10 and D15,
  `docs/spec-pack/product/architecture-standard.md`. Nothing under `docs/spec-pack/` is edited.
- Never skip, disable, delete or weaken a test or an ArchUnit rule. Never `--no-verify`. Pre-existing test files
  change only where a criterion names the change.
- Never push to `main`, never create or move tag `level-0-start`, never run `scripts/publish-workshop-main.sh` with
  `--push` or in its default (publish) mode against this repository; `--list-paths`, `--check <rev>` and `--help`
  are read-only and allowed. The script test runs only against its own temporary repositories.
- Secrets: never set, read or write `ANTHROPIC_API_KEY`; never commit a key. No `claude` CLI, no plugin or
  marketplace commands.
- Every `mktemp` uses an explicit template under `$TMPDIR` or SCRATCH. Maven runs set
  `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<SCRATCH>/jtmp`. No `docker compose down -v`; never touch the shared
  `bookstore-postgres-1` container (tests use Testcontainers only).
- ASCII only in every file touched; docs and comments in plain English without em dashes.
- Referee: `./mvnw clean verify` from ROOT, green, within 60 seconds. Base referee on dfcd01b (orchestrator,
  2026-11-10T01:57Z): 31 s wall (Maven 29.6 s); Vitest 689 passed + 2 expected fail (691); bookstore 1379;
  distributor 190. Script test base on /bin/bash 3.2.57: 235 assertions, 0 failures.
- Implementers do not git commit, branch or stash; the orchestrator owns git. Implementers never mutate files
  outside their fileScope to self-probe, except the temporary probes a criterion names (created and removed in the
  same step); other mutants are described in the outbox and the verifier runs them.
- Evidence files in SCRATCH carry the role and task prefix (`f09-t1-impl-*`, `f09-t3-qa-*`).
- After 3 failures on one item the item is blocked and the cycle stops with a needs-decision.

## Path ruling (reference.md section 6)

Lite path (the default) - all hold:
- [x] reproduced in 1a.2: T1 by the orchestrator (temporary V5 on the test classpath, bug.md Reproduction); T2 by
  `grep -c assistant-unavailable` (0, 0) against `AssistantCommands.java:75-76`; T3 by `--list-paths` (11 lines)
- [x] root cause named at file:line and confirmed: bug.md Root cause
- [x] one obvious fix design readable from the code: compare the history with the V files on the classpath; one
  sentence per doc; two list entries plus test cases
- [x] blast radius enumerable by grep now: bug.md Blast radius (one test, two docs, one list; residuals deferred)
- [x] no already-wrong persisted data or produced artifacts (`main` not yet published)
- [x] not S1; not money, numbering, auth or concurrency
Bundled owner decisions: D15 (publish list) -> own task T3; docs -> own task T2.
Open question the code does not settle in minutes: none.
-> path: lite (no lead, no research fan-out; three tasks; QA lead registered at Phase 2/3). The main session set T3
to publish-safety critical with opus implementer and verifier; T1 and T2 are elevated with sonnet.

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (T1): the expected version list is computed from the `V*__*.sql` resources under `classpath*:db/migration`
  (every classpath root, subdirectories included, because Flyway scans the location recursively), each version read
  from the file name as Flyway reads it (the text between the `V` prefix and the `__` separator, `_` read as `.`),
  ordered by Flyway's version order (`org.flywaydb.core.api.MigrationVersion`), and compared exactly, in order, with
  the successful versioned rows of `flyway_schema_history` by `installed_rank`. The scan uses Spring's
  `PathMatchingResourcePatternResolver`, not Flyway's own resolver, so the expectation does not come from the object
  under test. The list must be non-empty. The locations assertion, the no-repeatable assertion and
  `theCatalogueIsEmptyAtStart` stay as they are. The test method is renamed for what it proves.
- D2 (T1): the root-cause file is the test itself, so the negative control restores the pre-fix test file (copy-aside
  plus SHA-256) and adds the temporary probe migration on the test classpath
  (`bookstore/src/test/resources/db/migration/`); the probe never reaches a commit.
- D3 (T2): README tightening only rewraps or shortens wording; no section and no fact required by spec 09 "Human
  documentation" is dropped. The optional clause that the assistant runs without extended thinking may be added
  (true since fix-08 T4).
- D4 (T3): the two entries are appended after `docs/spec-pack`, in spec order: `guide` then
  `.github/workflows/guide.yml`. `.github/workflows/no-agent-config.yml` is unchanged (it reads the list).
- D5: models per the main session: opus for T3 (implementer and verifier), the QA lead and qa-final; sonnet for T1
  and T2. Never fable.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | status | verdict | qa-lead | pr | branch |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | the seed guard compares the migration history with the V files on the classpath, so a new migration stays green | bookstore | [] | 1 | elevated | sonnet | done | PASS r0 | ENDORSE | #151 | sdd/fix-09-baseline-review/T1 |
| T2 | README and assistant.http say a failed live model call answers 503 `assistant-unavailable` | root | [] | 2 | elevated | sonnet | done | PASS r0 | ENDORSE | #152 | sdd/fix-09-baseline-review/T2 |
| T3 | the publish script's path list holds `guide` and `.github/workflows/guide.yml`, tested on bash 3.2 and 5 | root | [] | 3 | critical | opus | done | PASS r0 | ENDORSE | #153 | sdd/fix-09-baseline-review/T3 |

T2 and T3 share the root module and run one after the other; one task at a time anyway (T1, T2, T3).

### T1: the seed guard compares the migration history with the V files on the classpath

- fileScope: `bookstore/src/test/java/nl/openvalue/bookstore/EmptyCatalogueAtStartIntegrationTest.java`.
  Temporary probes (created and removed within one step, never at handoff):
  `bookstore/src/test/resources/db/migration/V5__add_shelf_note.sql` (implementer and verifier); the verifier's
  falsifier probes of criterion 4.
- Acceptance criteria:
  1. Regression test `bookstore/src/test/java/nl/openvalue/bookstore/EmptyCatalogueAtStartIntegrationTest.java` >
     the history test (renamed for what it proves, for example
     `theMigrationHistoryHoldsExactlyTheSchemaMigrationsOnTheClasspathAndNoRepeatableOne`) exists at the end-to-end
     integration layer (per architecture standard section 5 and the layer-testing skill), is named for the correct
     behaviour (not the bug id), and asserts the expected behaviour stated in bug.md Reproduction: the successful
     versioned rows of `flyway_schema_history` in `installed_rank` order equal exactly, in order, the versions of the
     `V*__*.sql` files found under `classpath*:db/migration` per decision D1; that expected list is not empty; and
     no row has a NULL version (no repeatable script ran). The class comment states what the history proves.
  2. Negative control: on the pre-fix code (the test file restored from `git show HEAD:bookstore/src/test/java/nl/openvalue/bookstore/EmptyCatalogueAtStartIntegrationTest.java`
     via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`) with the temporary probe
     `bookstore/src/test/resources/db/migration/V5__add_shelf_note.sql` holding
     `ALTER TABLE book ADD COLUMN shelf_note varchar(40);`,
     `./mvnw -pl bookstore test -Dtest=EmptyCatalogueAtStartIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`
     FAILS on exactly `theMigrationHistoryHoldsTheFiveSchemaMigrationsAndNoRepeatableOne` with
     `but some elements were not expected:` `["5"]` - not with a compile, import or context error; with the fix in
     place and the same probe it PASSES (3 tests), and with the fix in place and no probe it PASSES (3 tests). The
     probe is removed afterwards and `git status --porcelain` lists only the test file. All outputs are recorded
     verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`.
  3. Not a weakening: the outbox states, with the evidence of criterion 4, that the proof that tests never see seeded
     rows stays intact: `theTestProfileMigratesOnlyTheSchemaLocation` and `theCatalogueIsEmptyAtStart` are byte-equal
     to `<pr-base>` (`git diff <pr-base> -- <file>` quoted), the no-repeatable assertion is kept, and the version
     comparison is still exact in both directions.
  4. The fixed test still catches what it guards (the verifier runs these probes; the implementer describes them in
     the outbox), each in one run with copy-aside restore: (a) a temporary `R__probe.sql` (`SELECT 1;`) under
     `bookstore/src/test/resources/db/migration/` makes the history test FAIL on the repeatable assertion; (b)
     `bookstore/src/test/resources/application-test.yml` temporarily set to
     `locations: classpath:db/migration,classpath:db/seed` makes the locations test FAIL and the history test FAIL
     (the seed's repeatable row); (c) a temporary `V4_1__probe.sql` (`SELECT 1;`) under the same directory leaves all
     three tests green (version `4.1`, Flyway order); (d) a temporary `probe/V6__probe.sql` (`SELECT 1;`) in a
     subdirectory of that directory leaves all three tests green (Flyway scans recursively). Each output is recorded.
  5. No behaviour change outside the root cause: `./mvnw -pl bookstore test` is green (1379 tests); the diff is
     confined to the one test file; no production file changes; ArchUnit green unmodified.
  6. `./mvnw clean verify` is green within 60 seconds.

### T2: README and assistant.http say a failed live model call answers 503 assistant-unavailable

- fileScope: `README.md`, `http/assistant.http`.
- Acceptance criteria:
  1. README "Assistant" section holds one sentence saying that with a key set, a failed live model call (for example
     a wrong key, a spend or rate limit at Anthropic, or a timeout) answers 503 with the code
     `assistant-unavailable`. Every claim in it matches `AssistantCommands.java:60-80` (the conversation is left as
     it was before the failed question, if mentioned). Optional (D3): one clause that the assistant runs without
     extended thinking.
  2. `wc -l README.md` prints at most 120. Every heading of `<pr-base>` README is still present
     (`## The app`, `## Prerequisites`, `## Run`, `### The jar against an existing PostgreSQL`,
     `### Reset the database`, `## Modules`, `## Architecture`, `## Endpoints`, `## Storefront`, `## Assistant`,
     `## Tests`, `## Demo data`) and each of these still greps: `./mvnw verify`, `./mvnw -pl bookstore spring-boot:run`,
     `./mvnw -pl distributor spring-boot:run`, `SPRING_DATASOURCE_URL`, `docker compose -f bookstore/compose.yaml down`,
     `frontend/`, `docs/architecture.md`, `/shop/`, `npm run dev`, `https://maikkingma.github.io/AgenticBookStore/`,
     `BOOKSTORE_AI_API_KEY`, `ANTHROPIC_API_KEY`, `assistant-offline`, `400`, `429`, `1000`. The outbox lists every
     changed README line with the reason (new sentence, or rewrap/shortening that drops no fact).
  3. `http/assistant.http` holds one comment sentence saying that with the key set, a failed model call (wrong key,
     spend or rate limit at Anthropic, timeout) answers 503 with the error code assistant-unavailable. Each
     `Expected status:` line stays true (it may add the failed-call 503). No request body or URL changes.
  4. ASCII only and no em dash: `LC_ALL=C grep -n '[^ -~]' README.md http/assistant.http` prints nothing. Plain
     English; no instruction addressed to AI agents in README.
  5. Negative control: on the pre-fix files (`git show HEAD:<file>`), `grep -c assistant-unavailable README.md` and
     `grep -c assistant-unavailable http/assistant.http` print 0 and 0; after the fix each prints at least 1. Both
     outputs are recorded under `## Red evidence` and `## Green evidence`.
  6. `./mvnw clean verify` is green within 60 seconds.

### T3: the publish script's path list holds guide and .github/workflows/guide.yml

- fileScope: `scripts/publish-workshop-main.sh`, `scripts/test-publish-workshop-main.sh`.
- Acceptance criteria:
  1. `STRIPPED_PATHS` in `scripts/publish-workshop-main.sh` gains exactly `guide` and `.github/workflows/guide.yml`,
     appended after `docs/spec-pack` (D4); nothing else in the list changes; any comment or usage text that counts or
     names the list stays true. `bash scripts/publish-workshop-main.sh --list-paths` prints 13 lines, the last two
     `guide` and `.github/workflows/guide.yml`.
  2. Regression test `scripts/test-publish-workshop-main.sh`: the oracle `EXPECTED_PATHS` (its own copy) gains the
     same two lines in the same order, its comment says 13, and the count-bearing test name says 13. The generic
     per-path assertions (fixture holds, main holds nothing under, prints removed) then cover both new paths.
  3. New cases, each with its own assertions: (a) a factory fixture with several files under `guide/` (nested, for
     example `guide/package.json` and `guide/docs/level-0.md`) and `.github/workflows/guide.yml` beside
     `.github/workflows/ci.yml`: the default run leaves main with nothing under `guide` or
     `.github/workflows/guide.yml`, prints `removed: guide` and `removed: .github/workflows/guide.yml`, and keeps
     `.github/workflows/ci.yml` and the look-alikes `guidelines.md`, `docs/guide/x.md`, `.github/workflows/guide.yaml`
     and a root file `guide.yml`; (b) `--check` on factory names the guide files and `.github/workflows/guide.yml`;
     (c) `--check HEAD` on a published main after adding a file under `guide/` exits non-zero and names exactly that
     path; (d) the same after adding `.github/workflows/guide.yml` names exactly that path.
  4. Negative control: with the pre-fix `scripts/publish-workshop-main.sh` (restored from
     `git show HEAD:scripts/publish-workshop-main.sh` via copy-aside + SHA-256; never `git checkout --`,
     `git restore` or `git stash`) and the new test, `/bin/bash scripts/test-publish-workshop-main.sh` exits non-zero
     with `not ok` lines for the 13-path list, for `main holds nothing under guide`, for
     `prints removed: .github/workflows/guide.yml` and for the `--check` guide cases - not with a bash syntax error or
     an aborted setup; with the fix it ends `N assertions, 0 failures`. Both outputs are recorded verbatim under
     `## Red evidence` and `## Green evidence`.
  5. Green on both interpreters: `/bin/bash scripts/test-publish-workshop-main.sh` ends
     `N assertions, 0 failures (script run with /bin/bash, bash 3.2.57(1)-release)` and
     `/opt/homebrew/bin/bash scripts/test-publish-workshop-main.sh` ends `N assertions, 0 failures` with bash 5;
     CI `verify` (which runs `bash scripts/test-publish-workshop-main.sh` on ubuntu) is green on the task PR.
  6. No behaviour change outside the list: every pre-existing assertion still passes and keeps its name, except the
     count-bearing names that say 11; `git diff <pr-base> --numstat -- scripts/test-publish-workshop-main.sh` and the
     outbox show only additions plus those count lines; no `--push` of the real repository, no publish run against
     this repository.
  7. `./mvnw clean verify` is green within 60 seconds.

## Directives

### implDirective (every task; placeholders per task above)

```
Bugfix task. The order below is mandatory; each step's output goes into your outbox artifact.
1. Read bug.md §Reproduction and §Root cause (in the spec folder next to plan.md).
2. Write the regression test at `<test path>` and run ONLY that test against the UNFIXED code: `<test command>`. It must fail for the reported reason (same error shape as §Reproduction). Paste the failing output under `## Red evidence`.
3. Apply the fix at the root cause named in bug.md §Root cause (`<file>:<line>`), not at the symptom. Stay inside your fileScope.
4. Re-run the test: PASS — paste under `## Green evidence`. Then run `<module suite command>`.
5. If step 2 cannot be made to fail on the unfixed code, or it only fails when asserting something other than the reported behaviour, the reproduction or the root cause is wrong: set your outbox to status: blocked, send a type: question to the orchestrator describing exactly what you observed, and STOP. Never adjust the test until it passes, and never write the fix before the test is red.
```

Per task: T1 test path = root-cause file = the test itself (step 2 is the pre-fix test plus the V5 probe, step 3 the
rewritten assertion; the probe goes away after the green run); test command
`./mvnw -pl bookstore test -Dtest=EmptyCatalogueAtStartIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`;
module suite `./mvnw -pl bookstore test`. T2: the "test" is the grep pair of criterion 5; module suite = criterion 2
and 4 checks. T3: test `scripts/test-publish-workshop-main.sh`, command `/bin/bash scripts/test-publish-workshop-main.sh`,
root cause `scripts/publish-workshop-main.sh:8-18`, module suite = the same test on `/opt/homebrew/bin/bash`.

### qaDirective (every task; reference.md section 5 verbatim)

```
Bugfix task. Beyond your standard gate, run an independent negative control — never trust the implementer's `## Red evidence`, re-run it yourself:
1. Identify the production files of the fix within the task's fileScope (`git diff HEAD --stat -- <files>`; ignore test files).
2. For each production file: record `shasum -a 256 <file>` and `git status --porcelain -- <file>`; `cp` it to your scratch dir as `<name>.fixed`; write the pre-fix baseline with `git show HEAD:<file> > <scratch>/<name>.base` and `cp` that over `<file>` (a file new in this task: `mv` it to scratch instead). NEVER `git checkout --`, `git restore` or `git stash` (uncommitted batch-mates share the tree and the stash).
3. Run ONLY the regression test: `<test command>`. It must FAIL with the failure shape the acceptance criteria name — read the message, not just the count. A compile/import/fixture error, or a pass, is a FAIL of this task ("the test does not pin the defect").
4. Restore every file with `cp <name>.fixed <file>`; verify hash equality AND that `git status --porcelain -- <file>` matches the pre-probe state; `mv` the backups to the scratch dir (`rm` is not allowlisted). Keep the mutation window to one run.
5. Re-run the regression test: PASS. Record both runs verbatim under `## Negative control` in your verdict artifact, and state whether the implementer's `## Red evidence` matches what you observed.
6. If the harness denies the test run right after your edit, restore immediately from the SHA'd copy, do not retry, and write "negative control not independently reproduced (harness denial)" in the verdict — the orchestrator runs it at branch level.
7. FAIL the task if: the regression test passes on the pre-fix code; the fix sits somewhere other than bug.md §Root cause (`<file>:<line>`) or only masks the symptom (catches the error, special-cases the input, widens a tolerance); a pre-existing test was modified; or the fix touches files outside fileScope.
Migration fixes: follow the repo's migration standard's replay/mutation ceremony instead of file swapping. Where present, follow `.claude/agent-memory/qa-test-verifier/mutation-and-negative-control-probes.md` for restore mechanics and e2e caveats (rebuild between a source swap and an e2e run; kill a stale dev server on the e2e port).
```

Per task: T1 "production file" = the test file itself (the root cause), the probe of criterion 2 is added for the
pre-fix run, and step 7's "pre-existing test was modified" applies to the two other test methods only; T2 the two doc
files, test = the grep pair; T3 `scripts/publish-workshop-main.sh`, test command
`/bin/bash scripts/test-publish-workshop-main.sh`.

## QA feed-forward

From the QA lead's T1 r0 meta-review (ledger `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-fix-09-baseline-review.md`):
- FF-1 (T2, T3): `grep -ciE "warn|deprecat|self-attach" <log of ./mvnw clean verify>` prints 162; any other count:
  diff the normalised lines against `f09-t1-qa-verify.log` (scratch) and classify every added line.
- FF-2 (T2, T3): the verdict ends with repo-relative `shasum -a 256` lines for every fileScope file; scratch names
  `f09-t<n>-qa-*`, cited by absolute path.
- FF-3 (T2, T3): the T1 test file's SHA-256 is
  `4960f2fdd5dea1732c9d65d5687b763f406d2cbae2173215bf848d3752d0a91f` at the start and end of the gate.
- FF-4 (T3): besides the negative control, named mutants of the fixed script, one run each, `cp` restore with SHA
  check, each failing on its quoted `not ok` name: (i) drop only `guide` (main holds nothing under guide, the
  `--check` guide case); (ii) drop only `.github/workflows/guide.yml` (its `removed:` line, its `--check` case);
  (iii) strip by prefix without a path separator (the look-alike keep assertions `guidelines.md`,
  `.github/workflows/guide.yaml`). A look-alike assertion no mutant turns red has no evidence.
- FF-5 (T2): trace each fact of the new sentences (503, `assistant-unavailable`, wrong key, spend or rate limit,
  timeout, conversation unchanged if mentioned) to `AssistantCommands.java:60-80` and to an asserting test by
  file:line; name any fact no test asserts.
From the QA lead's T2 r0 meta-review:
- FF-6 (T3): (a) the hash check also covers README `4f1f302ab48ebcad8d808605ed75089e18ed28c0d9f8a4f8cbda6e882fd768d5`
  and `http/assistant.http` `71d88e210698949f866181071b3e2e319a11af2f6aa447fa3e199715d8360421` at start and end;
  (b) the verdict reports the implementer artifact's `ls -lT` mtime next to its `updated` stamp (stamps from `date -u`).
- FF-7 (T3): every new case calls the script through the existing `"$BASH" "$SCRIPT"` helper; `grep -n '"\$SCRIPT"'`
  shows no call without `"$BASH"`; a logging PATH shim for `bash` records 0 calls in the `/bin/bash` run and the
  summary reads `bash 3.2.57(1)-release`.
- FF-8 (T3): each "names exactly that path" assertion (criteria 3c, 3d) fails on two mutants of the fixed script, one
  run each, `cp` restore with SHA check: (i) `--check` exits non-zero but prints nothing; (ii) it prints every listed
  path, or `guide` instead of the file. Also `bash scripts/publish-workshop-main.sh --check HEAD` on a published main
  in a scratch repo exits 0. Git writes only in scratch repos.
- Residuals noted (owner, not a QA bar): R-T2-1 no test names the SDK exception types behind the documented causes;
  R-T2-2 the jar paragraph alone no longer says the run seeds the database (README "Demo data" does).
- qa-final QF-3: at the spec tip the README and http hashes match the T2 baseline (or every later change is
  explained), README at most 120 lines, the grep pair prints at least 1 and 1, the sentence is under "## Assistant".
From the QA lead's T3 r0 meta-review (no feed-forward, last task):
- Residuals noted (owner, not a QA bar): R-T3-1 no test asserts the `--dry-run` "would remove:" listing (predates
  T3); R-T3-2 `--check` calls guide files "agent configuration" (consistent with D15).
- qa-final QF-4: at the spec tip both script hashes equal the T3 baseline (or every later change is explained); the
  test ends `263 assertions, 0 failures` on `/bin/bash` and `/opt/homebrew/bin/bash`; `--list-paths` prints 13 lines,
  the last two `guide` and `.github/workflows/guide.yml`; `.github/workflows/no-agent-config.yml` unchanged versus the
  spec base; CI `publish-script-test` green on the spec PR.
- qa-final QF-5: in a scratch clone of the tip with origin pointed at a scratch bare repo (`git remote -v` printed
  first), a factory commit adding `guide/x.md` and `.github/workflows/guide.yml`: `--dry-run` shows
  `would remove: guide` and `would remove: .github/workflows/guide.yml`; then the default run and `--check main`
  exits 0; this project's refs (`git for-each-ref refs/heads/main refs/tags/level-0-start`,
  `git ls-remote origin main level-0-start`) identical at start and end.
- qa-final (QF-1, QF-2): the two direction probes of the T1 comparison at `:49` (extra location with a V7 -> not
  expected ["7"]; V9 with `spring.flyway.target: "4"` -> could not find ["9"]), each a `clean test` run in a
  throwaway tree; the T1 test file's hash at the spec tip equals `4960f2fd...a91f`.

## Amendments

(none)

## Final QA and Gate 2

- qa-final (opus): PASS, all 14 items of the QA lead's directive on spec tip c247b74 against factory dfcd01b
  (verdict `.claude/comms/mailboxes/qa-final/outbox/task-final-verdict.md`). Referee `./mvnw clean verify` 26.3 s;
  T1 recipe with V5: recipe classes 24/0 and the whole bookstore module 1379/0; branch-level negative controls: T1
  factory test + V5 red at `:40` with `["5"]`, T2 grep 0/0 -> 1/1, T3 tip test vs factory script 263/19 with the gate's
  19 names; QF-1 both directions of `:49` red; QF-5 live scratch publish strips the guide and keeps T1 and T2 on main;
  this repository's main and level-0-start untouched. PR readiness: failing 0, skips 0 -> 0, gitlinks none, base
  ancestor yes; CI on #149 green.
- QA lead final-review: META ENDORSE final r0, Gate 2 QA confidence high (ledger
  `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-fix-09-baseline-review.md`): every criterion traced; T1 not a
  weakening (five pieces of evidence); the script test green on macOS /bin/bash 3.2.57 and in CI on bash 5.3.9; the
  accepted residuals deferred, not fixed.
- Residual risks (owner): R-T2-1 the documented causes are pinned only through the catch-all (optional test-only
  follow-up: `UnauthorizedException` and `RateLimitException` in `failedChatCalls()`); R-T3-1 no test asserts the
  `--dry-run` listing (since spec 09); R-T2-2 jar paragraph no longer says the run seeds the database ("Demo data"
  does); R-T3-2 wording "agent configuration" for guide files (consistent with D15).
- Standards proposals (follow-ups, not blockers): tests reading `flyway_schema_history` never pin a literal version
  list; a strip script's `--dry-run` test asserts every listed path; a task adding a ProblemDetail `code` updates every
  human doc listing that endpoint's errors.
- Gate 2 self-accepted by the orchestrator at 2026-11-10T03:45:46Z (checklist: qa-final PASS + ENDORSE, confidence
  high, no untraced criterion; T1 #151, T2 #152, T3 #153 merged; no stragglers; diff confined to the task file scopes,
  the spec folder and agent memory).
