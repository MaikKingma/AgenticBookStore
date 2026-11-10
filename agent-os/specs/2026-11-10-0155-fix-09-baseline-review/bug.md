---
bug: fix-09-baseline-review
severity: S2
path: lite
status: open
closes: [review S2 migration history test pins the migration list, review S3 assistant-unavailable undocumented, D15 publish script strips the guide]
---
# Bug - a correct new migration turns a seed guard test red on the frozen main

## Report
- Observed (T1, S2): `EmptyCatalogueAtStartIntegrationTest` >
  `theMigrationHistoryHoldsTheFiveSchemaMigrationsAndNoRepeatableOne` asserts
  `containsExactly("0", "1", "2", "3", "4")` (line 40). A correct `V5__add_shelf_note.sql`
  (`ALTER TABLE book ADD COLUMN shelf_note varchar(40);`) turns exactly this test red (reviewer: Tests run: 51,
  Failures: 1), while the census and `CatalogueIntegrationTest` stay green.
- Expected (T1): a new, correct migration keeps the test green; the test still proves that tests never see the demo
  seed (only the schema location is migrated, no repeatable script ran, every applied version is a schema migration).
- Observed (T2, S3): `README.md:100-103` and `http/assistant.http:4` name only the offline 503 (`assistant-offline`),
  400 and 429. `AssistantCommands.java:75-76` answers 503 `assistant-unavailable` for any failed live model call
  (fix-08, decision D10): a wrong key, a spend or rate limit at Anthropic, a timeout.
- Expected (T2): both files say so in one accurate sentence each; README stays at most 120 lines (spec 09 "Human
  documentation"), every required README part kept.
- Observed (T3, D15): `scripts/publish-workshop-main.sh` (STRIPPED_PATHS, lines 8-18) lists 11 paths, without
  `guide` and `.github/workflows/guide.yml`; decision D15 (spec text at dfcd01b) adds both, so the publish and the
  `no-agent-config` guard (`--check`) must cover them.
- Environment: branch `factory` at dfcd01b (spec 09 merged as 2cd03f4), macOS, Docker for Testcontainers.
- First seen / last known good: spec 09 review (2026-11-10) -> regression window: not a regression (T1 and T2 were
  written so in spec 09 and fix-08; T3 is a new requirement from D15).
- Frequency: always.
- Impact: `main` is frozen into tag `level-0-start` and spec 10 cuts every level branch from it. Level 0 (discount
  plus migration), Level 3 (`cancelled_at` migration) and spec 10 ("verify green ... with a new migration") all add
  V5, so on the frozen main every correct participant solution goes red and invites editing a test (S2: blocks the
  workshop flow). T2 misleads a participant who sees a 503 with a key set (S3). T3 protects every republish after
  spec 11 merges.
- Related: `docs/spec-pack/specs/09-workshop-baseline.md` (Requirement "Demo data in local runs only", "Human
  documentation", "Publishing main"), `docs/spec-pack/orchestrator/decisions.md` D10 and D15.

## Reproduction
- Status: reproduced (T1 by the orchestrator, T2 and T3 by reading the code).
- Recipe T1: in a scratch worktree of dfcd01b, create
  `bookstore/src/test/resources/db/migration/V5__add_shelf_note.sql` holding
  `ALTER TABLE book ADD COLUMN shelf_note varchar(40);`, then run
  `./mvnw -pl bookstore test -Dtest='EmptyCatalogueAtStartIntegrationTest,CatalogueIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`.
- Evidence T1 (orchestrator, 2026-11-10T01:54Z, scratchpad `f09-orch-repro-t1-red.log`):
  `Tests run: 24, Failures: 1`; `EmptyCatalogueAtStartIntegrationTest.theMigrationHistoryHoldsTheFiveSchemaMigrationsAndNoRepeatableOne:40`
  `Expecting actual: ["0", "1", "2", "3", "4", "5"] to contain exactly (and in same order): ["0", "1", "2", "3", "4"] but some elements were not expected: ["5"]`;
  `CatalogueIntegrationTest` 21 run, 0 failures. A migration file on the test classpath is applied by the test
  profile, so the probe needs no production file.
- Evidence T2: `grep -c assistant-unavailable README.md http/assistant.http` gives 0 and 0 on dfcd01b;
  `AssistantCommands.java:75-76` throws `UnavailableException("assistant-unavailable", ...)` for any
  `RuntimeException` of the chat call.
- Evidence T3: `bash scripts/publish-workshop-main.sh --list-paths` on dfcd01b prints 11 lines, no `guide`.
- Regression-test layer: T1 end-to-end integration (the test under repair is itself the guard, per architecture
  standard section 5 and the layer-testing skill); T2 none (docs; grep criteria); T3 the script's own bash test
  `scripts/test-publish-workshop-main.sh` (the spec 09 test of the publish script).
- Test files: T1 `bookstore/src/test/java/nl/openvalue/bookstore/EmptyCatalogueAtStartIntegrationTest.java` - run
  with `./mvnw -pl bookstore test -Dtest=EmptyCatalogueAtStartIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`;
  T3 `scripts/test-publish-workshop-main.sh` - run with `/bin/bash scripts/test-publish-workshop-main.sh`.

## Root cause
- Causal chain T1: a participant adds V5 -> the history query returns 0..5 -> the assertion at
  **`EmptyCatalogueAtStartIntegrationTest.java:40`** compares with a hard-coded list of today's five versions -> red.
  The gates missed it because the suite never adds a migration; the census and the catalogue tests do not pin the
  version list.
- Root cause T1 (one sentence): the test pins the current migration list instead of what it means to prove, namely
  that every applied version is one of the schema migrations on `classpath:db/migration` and that the seed never ran.
- Causal chain T2: fix-08 (D10) added the `assistant-unavailable` 503 at `AssistantCommands.java:75-76` -> the README
  assistant section (`README.md:100-103`) and `http/assistant.http:4` were written before it and fix-08 changed only
  the answer-language sentence -> the docs list only offline, 400 and 429.
- Root cause T2: `README.md:100-103` and `http/assistant.http:4` do not describe the failed-live-call 503.
- Causal chain T3: D15 adds two paths to the spec's list -> the single list in
  **`scripts/publish-workshop-main.sh:8-18`** (STRIPPED_PATHS) does not hold them -> publish and `--check` ignore the
  guide; the test oracle (`scripts/test-publish-workshop-main.sh:42-53`) has the same 11 paths.
- Contributing factors: no participant-style "add a migration" check in spec 09's verification.
- Introduced by: T1 spec 09 (2cd03f4) - intended: prove the seed is absent in tests; T2 fix-08 (D10) docs scope;
  T3 decision D15 (dfcd01b), a new requirement.

## Blast radius
- [x] Same pattern elsewhere: `grep -rn 'flyway_schema_history' bookstore/src/test` - `BookstoreApplicationTests`
  checks only version '0' (stays true with V5), `SeedDatabase`, `DemoSeedCensusIntegrationTest` and
  `DatabaseCleaner*` use the history without pinning the version list. No other exact list. -> task T1 only.
- [x] Other callers: none (test-only defect). T2: other docs naming the assistant 503s: only `README.md` and
  `http/assistant.http` (`grep -rn assistant-offline README.md http docs frontend/README.md`) -> task T2.
- [x] Persisted data already wrong: none.
- [x] Artifacts already produced: `main` is not yet published; the main session publishes after this merge.
  -> none to remediate.
- [x] Docs, standards, tests encoding the wrong assumption: the script test oracle (T3), README and assistant.http (T2).
- [x] Sibling environments: `.github/workflows/no-agent-config.yml` reads the list from the script, no change needed.
- Deferred, accepted residuals (do not fix in this cycle, main session decision):
  - deferred: R18 `.gitignore` lines naming `guide/` and `.claude` paths (accepted residual).
  - deferred: R16 the guide URL in README answers 404 until spec 11 deploys (accepted residual).
  - deferred: R3 an older local database clashing with the seed (README "Reset the database" covers it).
  - deferred: R14 the assistant search tool cap of 10 (accepted residual).
  - deferred: the publish guard matches root paths only (accepted residual).
  - deferred: package-info sentence counts (accepted residual).
  - deferred: comments naming "spec NN" (accepted residual).

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | T1 compares the history with the V files on the classpath; T2 one sentence each; T3 two list entries plus tests |
| (b) revert | no | nothing to revert to; the tests and docs are correct otherwise |
| (c) guarded workaround + follow-up | no | not needed |
Chosen: (a) - each defect has one obvious fix readable from the code.

## Prevention
- Gap: spec 09's verification never simulated a participant adding a migration.
- Action: T1's negative control is that simulation (temporary V5 on the test classpath), recorded in the evidence;
  follow-up: spec 10's own "verify green with a new migration" check covers it on every level branch. No standard
  change.

## Verification
- T1: unfixed test plus temporary V5 red, fixed test plus V5 green (impl `## Red evidence`, QA `## Negative control`);
  R__ probe and seed-location probe still red on the fixed test.
- T2: grep counts 0 -> at least 1; README at most 120 lines.
- T3: updated script test red on the unfixed script, green on the fixed one, on /bin/bash 3.2 and bash 5.
- `./mvnw clean verify` green within 60 s.
