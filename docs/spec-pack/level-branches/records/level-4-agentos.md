# Level 4 record, Agent OS track: consistency check and rehearsal rows (decision D14, amendments A2, A5, A10)

Branch `start/level-4-agentos` (the name is frozen by D17; `step_branch level-4-agentos` prints it): the Level 1, 2 and 3
chain plus the 22 rows of `level-4-agentos/manifest.tsv`: five installed Agent OS command files, three product files, thirteen
standards files and the standards index (rework round 1: 22 rows, 21 in round 0). There is no done or solution branch for Level 4 and nothing here implements the
Level 4 brief (D17 addendum, A10); the step stages only what `start/level-4-agentos` holds: the level-3 tree, Agent OS
installed, the product docs and standards written, committed. No Claude Code session was run for this record (decision D14,
plan amendment A2): no `claude` command, no login, no plugin or marketplace command, no `ANTHROPIC_API_KEY`. The files that
`/plan-product`, `/discover-standards` and `/index-standards` would generate were written directly from the pack and the
code, and are listed below for Maik's review. Every command name below that begins with `/` is quoted text of a command
file, the pack, the guide or the docs, never a command that was run. Nothing here claims a Claude result. The scenarios
that need a session are rows R4C-1 to R4C-4 below, for Maik's rehearsal; the closing round adds them to
`rehearsal-checklist.md` (plan amendment A5); the closing round r2 (below) did that, as the last Level 4 track to merge.

Evidence files are in `evidence/level-4-agentos/`, and `name:N` means line N of that file. Short names: `clone`
agent-os-clone.txt, `inst` install-run.txt, `ulp` user-level-paths.txt, `f5` f5-before-after.txt, `san` sandbox-build.txt,
`ver` sandbox-verify.txt, `stg` staging.txt, `stt` staging-selftest.txt, `chk` written-files-check.txt, `ctl`
written-files-controls.txt, `trace` written-files-trace.txt, `claims` claim-searches.txt, `tok` token-inventory.txt, `nam`
class-naming-table.txt, `idx` index-rebuild.txt, `spell` token-spelling.txt, `docs` docs-quotes.txt, `reh`
rehearsal-setup-dry-run.txt, `hyg` hygiene.txt, `ref` referee-verify.txt, `words` installed-files-word-search.txt, `arch` arch-constants-coverage.txt, `rr` r4c5-and-r4c3-runs.txt. The scripts that made them are kept next to them as `.txt` copies. Every
path in the evidence is masked while it is written (`$SBX`, `$SP`, `$WT`, `$HOME`).

## Rework round 1 (QA verdict r0 FAIL: RA1 to RA3 and the nits)

Evidence convention. The files in the evidence root are the round 0 results (tree `b533a9a10f37e0308304401befb266e7b78d0234`, 21
rows); they are kept byte-unchanged because line citations point at them, and they remain true for round 0. The round 1 results are
new files in `evidence/level-4-agentos/r1/`, cited below with the prefix `r1/` (for example `r1/ver:4-15`). In the `r1/` files the
labels "sandbox 1", "sandbox 2" and "sandbox 3" are sandboxes 4 (hand run, committed), 5 (fresh build, verify) and 6 (second fresh
build). Where a result changed, the verdict table below cites the `r1/` file. Pre-edit copies of the record and of the round 0
evidence are in `$SP` (`s10-t13-impl-record-r0.orig.txt`, `s10-t13-impl-evidence-r0.orig/`), not in the worktree.

| Required action | What changed | Evidence |
|---|---|---|
| RA1 dropped ArchUnit rule `APP_SERVICE_IMPL_NESTED_CLASSES_ONLY_IN_ACL` and the `acl` adapter | new standard `architecture/outbound-acl.md` (8 lines: `*AppServiceImpl` implements the port with `RestClient`, payloads are private nested records that never leave `acl`, every external failure becomes a port exception, base URL `distributor.base-url`); rule added to `dependency-rules.md` line 21; `class-naming.md` line 17 states the `*AppServiceImpl` exception to the top-level-record preference; entry `outbound-acl` in `index.yml`; manifest row 22 | `r1/arch:1-25` (all 22 constants covered; control dropping the rule reports it `r1/arch:26-28`; the round 0 tree misses it `r1/arch:32-34`), `r1/claims:127-134` (C29 to C32: 5 catch blocks, 4 throw and 1 error-body reader returns null; 5 private nested records; `DistributorProperties` the only top-level record; base URL property), trace rows for the new lines (`r1/trace`), `r1/red-green-round-1.txt` (the round 0 trace fails on the round 1 files, the round 1 trace passes) |
| RA2 R4C-5 premise | row rewritten below: run folder named (a clone of `solution/level-3`), the prompt on a clone of the start branch stated with both answers, the temp file listed, Expected and Failure from the real output | `r1/rr:3-101` |
| RA3 false "unchanged" literal | the staged `index.yml` now ends with the blank line that the installed builder writes, so `--commands-only` leaves a clone of the start branch byte-equal and `git status` empty; the round 0 file differed by exactly that line (control in the same file); the three places and the citation are corrected | `r1/rr:103-116` |
| Nits N1 to N6 | `domain-model.md` line 9 and `flows-and-ports.md` line 19 reworded to what the code shows (`Order` and `StockItem` only; 3 DTOs without a constructor from a domain object, a product finding, PF-T13-9); hygiene row wording; R4C-4 factory-checkout note; `idx` citation; evidence count | `r1/claims:127-134`, this record |

Files changed since the round 0 hash file, with the action each serves: `level-4-agentos/manifest.tsv` (22 rows, RA1),
`level-4-agentos/files/agent-os/standards/architecture/outbound-acl.md` (new, RA1), `.../class-naming.md` (RA1), `.../dependency-rules.md`
(RA1), `.../domain-model.md` (N1), `.../flows-and-ports.md` (N1), `.../standards/index.yml` (RA1 entry, RA3 trailing blank line),
this record (RA1 to RA3, nits), and the new files under `evidence/level-4-agentos/r1/` (41 files, including `.txt` copies of the
scripts). No round 0 evidence file was edited. `r1/changed-files-since-r0.txt` lists the staged diff. Generated check logs were
not edited in place: they were regenerated as new `r1/` files because the tree changed (declared here, nothing cites the r1 copies
before this record).

## Closing round r2 (last Level 4 track to merge: A5, A11, CR-1 to CR-8)

The branch of this task now contains the spec tip `9089969` (T11 #163 and T12 #164) as the merge commit `ca66a3b`; every edit
below was made on the merged tree. Evidence of this round is in `evidence/level-4-agentos/r2/` (cited `r2/<name>:N`); no earlier
evidence file was edited, except two appended lines (insertion only, pre-edit copies in `$SP`, `s10-t13-impl-r2-*.orig.txt`).

| Item | What changed | Evidence |
|---|---|---|
| CR-1 merge | done by the orchestrator before this round; memory directory of this task curated from 806 to 776 lines (one narrow QA-rule section of `integration-test-techniques.md` pruned), `check-agent-memory.mjs` 0 violations | `r2/hygiene.txt` scope lines |
| CR-2 checklist FU-T10-1 | `S/records/rehearsal-checklist.md` against the merged tip is exactly four hunks: lines 402, 463 and 543 (the three FU-T10-1 sentences: "Z4 lists the four cases of the commit hook (the fifth row is the Stop handler)", "redo R3-1 setup steps 2 and 3", "variants C, D and E of R3-1") and one insertion after the Level 3 section (`@@ -547,0 +548,112 @@`) | `r2/checklist-hunks.txt` |
| CR-3 Level 4 section | the insertion holds 12 pointer rows (R4A-1 to R4A-3, RR-S1 to RR-S4, R4C-1 to R4C-5), each naming its record and the exact heading line (every heading resolves by `grep -F`, count 1), and three "Rehearsed in time" rows R4-A, R4-B and R4-C with GIVEN, WHEN and THEN from spec 10 lines 478 to 481, `git clone --branch` into a new folder, `<start>` recorded, the timer (start: Enter on the first framework command; stop: `/ship` prints the hash and `./mvnw clean verify` outside Claude ends `BUILD SUCCESS`), the 35:00 "started but not ready" rule, the infrastructure-error repeat rule, the time per track, and undo `git reset --hard <start> && git clean -fdx`; RR-S2 keeps the listing question first and RR-S4 `git status --short --untracked-files=all`; text that is not pack text is marked "added here". To keep the Agent OS row equal to its record, R4C-4 of this record now uses `<start>` (it said `START`), the same timer, the 35:00 rule and the infrastructure-error rule, and its clone literal is on one line | `r2/checklist-level4-checks.txt:3-15` (headings), `:17` (181 literals and headings, 0 not found), `:19-21` (control reports the broken literal), `:22` (branch names equal `step_branch`), `:29-31` (pack words) |
| CR-8 | the OpenSpec row R4-A and the pointer for R4A-2 say to keep `OPENSPEC_TELEMETRY=0` exported in the shell that starts `claude`, marked "added here" | `r2/checklist-level4-checks.txt:17` |
| CR-4 | one line appended to `evidence/level-3/t10/skill-selection-static.txt`: "# correction (closing round): the behaviour row is R3-4, not R3-5"; the 2402 bytes of round 1 are a byte prefix | `r2/hygiene.txt` (scope), prefix proof in this record's hand-off |
| CR-5 | `level-4-speckit.md` line 337 only: the only byte difference is the `origin/factory` sha on line 4 (`c3d44d7` in the r0 file, `711dd9dc` as in the Inputs table); nothing else in that file changed | `r2/hygiene.txt` scope (one changed line of that record) |
| CR-6 | this record: the DTO count is three (`DistributorTitleDTO`, `PlaceOrderLineDTO`, `RestockConfirmationDTO`) in the claims row and in PF-T13-9; check C32 now DERIVES the list (DTO files minus those with a public constructor from a class of the domain package), with a synthetic control and the check that `PendingRestockDTO` has such a constructor; a correction line is appended to `r1/claim-searches.txt` | `r2/claim-searches.txt:68-70`, `:136-138`, `:140` (112 searches, 0 failures) |
| CR-7 no staged file of any track changed | fresh trees on the merged tree: OpenSpec `1c279415`, Spec Kit `c9d758b9`, Agent OS `0a55b23f`, manifest rows 16, 29 and 22 equal to the files changed by each step commit; `test-staging.sh` `PASSED all 58 cases`; referee `BUILD SUCCESS`, 1314 and 190 tests | `r2/fresh-trees.txt`, `r2/staging-selftest.txt`, `r2/referee-verify.txt` |

Files changed in this round, each against its item: `S/records/rehearsal-checklist.md` (CR-2, CR-3, CR-8),
`S/records/evidence/level-3/t10/skill-selection-static.txt` (CR-4, appended line), `S/records/level-4-speckit.md` line 337 (CR-5),
this record (CR-3 text of R4C-4, CR-6, this section), `S/records/evidence/level-4-agentos/r1/claim-searches.txt` (CR-6, appended
line), new files under `S/records/evidence/level-4-agentos/r2/` (CR-3, CR-6, CR-7, hygiene, copies of the scripts), the memory
directory of this task (CR-1). No file under `S/level-4-*/` changed. Hygiene of this round: `r2/hygiene.txt` (leak grep 0 hits with
control, no non-ASCII, no dash, no Level 4 done or solution branch name, scope).

## Verdict

| Check | Result |
|---|---|
| Agent OS cloned into a scratch HOME, commit recorded, `.git` removed | `main` at `e892e61d322759165e7d9173e92c1010406c9f4d` (`v3.0.0-8-ge892e61`, 2026-10-07), 22 files, no `.git` (`clone:4-8`, `clone:9`); the real `~/agent-os` was never touched |
| `project-install.sh` in a level-3 sandbox | exit 0, no prompt; wrote 5 command files and `agent-os/standards/index.yml` (header only) in the sandbox, nothing in HOME, XDG or TMPDIR (`inst:32`, `inst:40-45`, `inst:47-53`) |
| User-level paths of the pack step, how to see and undo them (FF-T10-c) | `~/agent-os` (22 files, 168 KB) is the only user-level path; one temp file of the script is made and removed; no plugin, no marketplace (`ulp:3-6`, `ulp:9`, `ulp:22-26`) |
| Sandbox starts from `level-0-start`, no factory carry-over | level-3 tree `309786e063b813e0f9248714f404b927d277d879`; no `agent-os/` path in it (`san:9`, `san:12-14`) |
| F5: level-3 `CLAUDE.md`, settings, hooks, skills, commands and agents unchanged | 446 tracked files hash-equal before and after the install; 13 named F5 files `same`; the level-4 commit holds 22 `A` rows and no `M` (`r1/f5:3-4`, `r1/f5:6-18`, `r1/f5:19-21`) |
| Written files traced, no placeholder, no forbidden technology | 17 files, 199 trace rows, 0 uncovered content lines, `ALL PASS`; 13 negative controls bite; the round 0 trace fails on the round 1 files (RED) (`r1/chk:29-34`, `r1/ctl:3-16`, `r1/red-green-round-1.txt`) |
| Counterexample searches for every universal claim (FF-T7-b) | 110 searches on `level-0-start` and on the level-4 tree, 0 failures, a control per search (`r1/claims:135-136`) |
| Class naming table, token inventory, index | table equals `ArchitectureTest` and `docs/architecture.md` (`r1/nam:10`); 213 of 213 backticked tokens resolve (`r1/tok:215`); all 22 `@ArchTest` constants are covered by a written standard (`r1/arch:25`); the installed index builder (`--commands-only`) leaves a clone of the start branch byte-equal, `git status` empty (`r1/rr:106-111`) |
| Fresh build equals the hand run | fresh `simulate-level-0.sh level-0-start` plus `build-level.sh level-4-agentos`: tree `0a55b23f20d4288ba9301d44fc70de7e9a3d868e` on both (round 0: `b533a9a1...`); the commit changes exactly the 22 manifest targets (`r1/san:38-43`) |
| `./mvnw clean verify` on the level-4 tree | `BUILD SUCCESS`, 1428 bookstore and 190 distributor tests, 0 failures, `ArchitectureTest` 22 tests, 0 failures, 0 errors (`r1/ver:4-15`) |
| Staging self-test and referee | `test-staging.sh` `PASSED all 58 cases` (`r1/stt:14`); referee `./mvnw clean verify` in the track worktree: `BUILD SUCCESS`, 1314 bookstore and 190 distributor tests, `ArchitectureTest` 22 tests (`r1/ref:3-12`) |
| Scenario statuses | the pack scenario is a rehearsal row; the three Claude-generated outputs are consistency-checked files plus rehearsal rows; the installer is a zero-token run with evidence; no Claude result is claimed |

No row is a mismatch in the staged files or in the task's own files. Nine rows are product findings in pack and guide text
(PF-T13-1 to PF-T13-9 below); six items need Maik's decision or review (D-1 to D-6 below).

## Inputs and shas

| Input | Value |
|---|---|
| Base | tag `level-0-start` = `b668073eb44bfea453c291509a615f6de081e54a`; the simulated level-0 commit of every sandbox is `391d5f74d69591a8e7a4b3cf718a07fe9dbae03c` (`san:20`) |
| Agent OS | `https://github.com/buildermethods/agent-os.git`, branch `main`, commit `e892e61d322759165e7d9173e92c1010406c9f4d` ("Run coding agents in Claude Code cloud sessions (#353)", 2026-10-07), 8 commits after the tag `v3.0.0` (`clone:4-7`); `config.yml` says `version: 3.0` |
| Sandbox 1 (hand run, round 0) | level-3 build, install, written files, one commit `Level 4 start: Agent OS installed and initialised`; HEAD tree `b533a9a10f37e0308304401befb266e7b78d0234`, level-3 tree `309786e0...` (`san:9-10`) |
| Sandbox 2 | fresh build from the staged step, the same tree; `./mvnw clean verify` ran here (`ver:1`) |
| Sandbox 3 | a second fresh build for the evidence, again the same tree (`san:38-41`) |
| Round 1 sandboxes | sandbox 4 (clone of sandbox 1 reset to the level-3 commit, installer run again, written files with the round 1 edits, committed: HEAD tree `0a55b23f20d4288ba9301d44fc70de7e9a3d868e`), sandbox 5 (fresh build, same tree, `./mvnw clean verify` ran here, `r1/ver:1-2`), sandbox 6 (second fresh build, `r1/san:38-41`) |
| Pack text | `docs/spec-pack/specs/10-harness-reference-solutions.md` lines 430-433 (the three start branches), 467-475 (`start/level-4-agentos`), 477-481 (the scenario), 483-491 (D14 tasks text); `product/mission.md`, `product/tech-stack.md`, `product/roadmap.md`, `product/architecture-standard.md`; `workshop/sdd-framework-guide.md`, `level-4-feature-briefs.md`, `workshop-plan.md` |
| Guide sources | pages 05, 06, `reference/cheat-sheet.md` and `reference/troubleshooting.md` on `origin/sdd/11-workshop-guide/spec` (`b7f053c7a9c6`) and `origin/factory` (`711dd9dcdc75`), read with `git show`; the Agent OS lines are the same on both refs for pages 05, 06 and the cheat sheet; factory adds catch-up lines to the troubleshooting page (`spell:69-74`) |
| Code and rulebook | `docs/architecture.md` and `bookstore/src/**` on the built tree (equal to `level-0-start` for every searched claim, `r1/claims:3-68`), `ArchitectureTest.java` |
| Claude Code docs | page `skills` read with curl on 2026-11-10, sha256 `50c0f7621b93...` (`docs:2`) |
| Tools | bash 5.3.20 and macOS `/bin/bash` 3.2.57 (the installer's `#!/bin/bash` line runs 3.2), Python 3 with `-I`, Maven wrapper, Docker, shared `~/.m2` (J10) |

Environment: every framework script ran with `HOME`, `XDG_CONFIG_HOME`, `XDG_CACHE_HOME`, `XDG_DATA_HOME`, `XDG_STATE_HOME` and
`TMPDIR` under the session scratchpad (HR1); the Maven runs set `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<scratch>`.
`ANTHROPIC_API_KEY` was neither read nor written. No settings file was written by me (HR2) and the install script writes none
(`f5:27-29`).

## What the installer wrote (criterion 2)

`~/agent-os/scripts/project-install.sh` is bash 3.2 compatible and takes `--profile`, `--commands-only` and `--verbose`
(`clone:33-36` lists the executable bit). Run without flags in the level-3 sandbox it printed (`inst:7-30`): profile
`default`, no standards installed ("No standards to install (profile is empty)", because the script copies only
`profiles/<name>/standards/**` and the clone holds `profiles/default/global/tech-stack.md`, script line 224 and `clone:29`),
an index created with no entries, and "Installed 5 commands to .claude/commands/agent-os/". The sandbox gained exactly 6
files: the five command files and `agent-os/standards/index.yml` of 28 bytes (a one-line header) (`inst:40-45`). It asked no
question because `agent-os/standards/` did not exist (script lines 170-190: the prompt `Do you want to continue? (y/N)`
appears only when that folder exists).

## Files written from the pack (criterion 4, C0-13f), for Maik's review

17 files, written directly (round 1: the 16 of round 0 plus `outbound-acl.md`): the output the installed commands define, in the form they define. Each line of each file is in a
trace row of `trace` (written file and lines, source and lines, needles that must occur in both texts; `r1/chk:29-30` proves 0
uncovered content lines and every needle in both; round 0: `chk:29-30`). Files with `ARCH` sources follow `docs/architecture.md` on the built tree
(section numbers in `trace`), `STD` is the pack `architecture-standard.md`, `CODE` is a path with lines on the built tree.

| File | Lines | Form defined by | Sources (what the trace rows cite) |
|---|---|---|---|
| `agent-os/product/mission.md` | 28 | `plan-product.md:131-145` (Problem, Target Users, Solution) | pack `mission.md:4-25`, copied; one substitution: `architecture-standard.md` becomes `docs/architecture.md` (D-4); the check proves every content line is a pack line (`chk:31`) |
| `agent-os/product/roadmap.md` | 16 | `plan-product.md:149-159` (Phase 1: MVP, Phase 2: Post-Launch) | pack `roadmap.md:22-29` and `:33` (build order rows), see D-1 |
| `agent-os/product/tech-stack.md` | 42 | `plan-product.md:163-183` (Frontend, Backend, Database, Other) | pack `tech-stack.md`, 39 trace rows; every number is in the pack (`chk:32`) |
| `agent-os/standards/architecture/clean-hexagonal-onion.md` | 12 | `discover-standards.md:92-95,120,161-164` (`[folder]/[standard].md`) | `ARCH` 3-14, 18-41, `CLAUDE.md:25`, `.claude/rules/testing.md:11` |
| `.../architecture/dependency-rules.md` | 22 | same | `STD:43-53` verbatim rules, `ARCH:153-162`, `ARCH:62-63`, `ArchitectureTest.java:56,120` |
| `.../architecture/class-naming.md` | 17 | same | `ARCH:73-87`, `ARCH:62-63`, `STD:61`; table equals `ArchitectureTest` (`r1/nam:1-10`) |
| `.../architecture/domain-model.md` | 19 | same | `ARCH:93-102`, `224-225`, `StockRunningLowEvent.java:10` |
| `.../architecture/money-is-exact.md` | 10 | same | `Money.java`, `BookDTO.java`, `BookView.java`, `AddBookPayload.java`, `V1__create_catalogue.sql:8-9`, `ARCH:64-65,101-102` (code only for payloads and views, see PF-T13-5) |
| `.../architecture/flows-and-ports.md` | 31 | same | `ARCH:106-122`, `STD:105-106`, `OrderFlow.java:30`, `DistributorAppService.java:51` |
| `.../architecture/outbound-acl.md` | 8 | same | `ARCH:38,62-63,148-149`, `DistributorProperties.java:8` |
| `.../architecture/inbound-adapters.md` | 28 | same | `ARCH:126-139`, `EventProcessor.java:23`, `RequestRestockDelegate.java:44` |
| `.../database/flyway-migrations.md` | 9 | same (the folder name is D-2) | `CLAUDE.md:22-23`, `.claude/rules/data-and-migrations.md:8-12`, `ARCH:145-147`, `application.yml:10-18` |
| `.../database/persistence-adapter.md` | 11 | same | `ARCH:37,54,144-145`, `.claude/rules/data-and-migrations.md:7`, `BookDataServiceImpl.java:21` |
| `.../testing/layer-tests.md` | 18 | same | `ARCH:166-176` verbatim table, `TECH:59`, `.claude/rules/testing.md:6` |
| `.../testing/database-in-tests.md` | 12 | same | `ARCH:172-182`, `PostgresTestConfig.java`, `IntegrationTestBase.java`, `application-test.yml:2` |
| `.../testing/test-conventions.md` | 8 | same | `ARCH:171,178,183-184`, `CLAUDE.md:13-15`, `.claude/rules/testing.md:9-11`, `SPEC10:460-461` |
| `agent-os/standards/index.yml` | 34 | `index-standards.md:66-99`, `discover-standards.md:139-147` (folder, file, `description`, alphabetised) | one entry per standards file, 13 entries, ending with the blank line the builder writes; the installed builder regenerates the same file byte for byte (`r1/rr:106-111`) |

Quotes of the lines followed: `discover-standards.md:163-164` "All standards: `agent-os/standards/[folder]/[standard].md`
Index file: `agent-os/standards/index.yml`"; `discover-standards.md:92-93` "Determine the appropriate folder (create if needed):
`api/`, `database/`, `javascript/`, `css/`, `backend/`, `testing/`, `global/`"; `discover-standards.md:168-174` (lead with the rule,
code examples, one standard per concept, bullet points); `index-standards.md:74-78` (alphabetise folders and files, no `.md`
extension, one-line descriptions); `plan-product.md:129-183` (the three file templates); `shape-spec.md:77,94` (the command
reads `agent-os/product/` and `agent-os/standards/index.yml`).

Checks on these files, each with a control that bites:

| Check | Result | Control |
|---|---|---|
| No `[A-Z_]+` bracket, brace or angle placeholder, no TODO-like word; every ALL_CAPS token (20) occurs in a source | PASS (`r1/chk:23-26`) | N1 and N3 (`r1/ctl:4,6`) |
| ASCII only, no dash character other than a hyphen | PASS (`r1/chk:24`) | N2 (`r1/ctl:5`) |
| Lombok, MapStruct, `double` or `float`, embedded database, edited migration appear only as prohibitions (10 lines) | PASS (`r1/chk:27-28`) | N4 to N7 (`r1/ctl:7-10`) |
| Every content line is in a trace row; needles occur in the written lines and in the cited source lines | PASS, 199 rows (`r1/chk:29-30`) | RED run with an empty trace, 190 uncovered lines (`r1/chk:10-15`); the round 0 trace on the round 1 files (`r1/red-green-round-1.txt`); N8, N12 and a misspelled needle (`r1/ctl:11,15`, `r1/chk:35-38`) |
| `mission.md` lines are lines of the pack mission | PASS (`r1/chk:31`) | N9 (`r1/ctl:12`) |
| Every number of `tech-stack.md` is in the pack tech stack | PASS (`r1/chk:32`) | N10 (`r1/ctl:13`) |
| Index entries equal the standards files, alphabetised | PASS (`r1/chk:33`) | N11 (`r1/ctl:14`) |
| Class naming table equals `ArchitectureTest` and `docs/architecture.md` for 7 packages | PASS (`r1/nam:1-10`) | one suffix dropped, reported (`r1/nam:11-14`) |
| 213 backticked tokens resolve in the tracked text or the pack | PASS (`r1/tok:215`) | one class name misspelled, reported (`r1/tok:217-218`) |
| All 22 `@ArchTest` constants are covered by a written standard (rule text found in the named file) | PASS, 22 of 22 (`r1/arch:1-25`) | one mapping dropped, an invented constant, and the round 0 tree (rule missing) are reported (`r1/arch:26-34`) |
| The installed index builder (`--commands-only`) leaves `index.yml` byte-equal on a clone of the start branch, `git status` empty | PASS (`r1/rr:106-111`); the modulo-blank-lines compare also passes (`r1/idx:27`) | the round 0 index file (no trailing blank line) is changed by the same builder, ` M agent-os/standards/index.yml`, 1 insertion (`r1/rr:112-116`); a removed entry and an extra file come back as "Needs description - run /index-standards" (`r1/idx:29-36`) |

Tech stack list diff (every version and technology of the generated file occurs in the pack file; the pack file is the
superset): the generated file leaves out, on purpose and because they are not on the branch or are not dependencies of the
code, the pack's Docusaurus 3.10.2 and the guide site hosting rows (the `guide/` folder is not on `main` based branches),
the OpenSpec 1.16.0 and Spec Kit 1.3.1 and MCP Inspector tooling rows (they belong to the other tracks and to the
facilitator), and the spec 08 and spec 12 build instructions (start.spring.io ids). Agent OS 3.0 and Claude Code stay (the
"Agent tooling" line, `trace` rows for `TECH:65,69,72`).

Mission list diff: the generated mission omits the pack section "What success looks like" (the template has no such section,
`plan-product.md:131-145`). Everything else is verbatim (`r1/chk:31`). No feature, version or rule is invented: the roadmap lists
only pack build order rows (D-1).

### Contradiction review (pack: review the generated files for anything that contradicts `CLAUDE.md`)

`CLAUDE.md` lines 18-25 (read `docs/architecture.md`; domain is plain Java; money is `Money`; schema only through a new
migration; no Lombok, no MapStruct; distributor only through the port) are each restated, never contradicted, by the written
standards (`trace` rows for `CLAUDE:22-25`, `r1/chk:27-28`). The five installed command files were searched for the words the
C0 block names; every hit is a generic example of the upstream files, none instructs a rule against `CLAUDE.md`:

| Word | Hits | Class |
|---|---|---|
| `lombok`, `mapstruct`, `double`, `float`, `--no-verify`, `git push`, `ddl-auto`, `hsqldb`, `embedded`, `in-memory`, `h2` | 0 in each of the 9 upstream files (5 commands, 2 scripts, the default profile file) (`words:3-14`); control: `AskUserQuestion` 27 hits, `agent-os/standards` 25 hits in the same files (`words:17`) | none |

The default profile file `profiles/default/global/tech-stack.md` describes an unrelated stack (React 18, Tailwind, Express); the
installer does not copy it (`words:18-36`, PF-T13-3).

The installed `plan-product.md` and `shape-spec.md` lines that name `agent-os/standards/global/tech-stack.md`
(`plan-product.md:91`) and `agent-os/specs/` (`shape-spec.md:123,138,186`) are about files the install or a later command
creates; see PF-T13-3.

## Counterexample searches (FF-T7-b)

Every universal or descriptive claim of the standards got a search on `level-0-start` (`git archive` of the real tag) and on
the level-4 tree (round 1: 110 searches; round 0: 94, `claims`), with a control that finds a known hit (`claims`). Results that matter:

| Claim | Search | Result |
|---|---|---|
| exactly eight packages under the root; root holds three classes | `r1/claims:4-6` | 8 packages named as the standard says; `BookstoreApplication`, `ClockConfig`, `StorefrontConfig` |
| aggregates have private constructors, no setters, events are records | `r1/claims:7-12` | 3 private constructors, 0 setters, 0 non-record events (the interface `DomainEvent` is excluded by name) |
| `domain` imports only `java.*`; flows are `@Service` and `@Transactional`; only the named framework types in `domaininteraction` | `r1/claims:13-18` | 0 other imports; 3 of 3 flows; imports are SLF4J, `ApplicationEventPublisher`, `@Service`, `@Transactional` |
| no query signature returns a DTO; every command POST answers 202; one advice class per adapter that has controllers; each maps 404, 409, 422, 503 | `r1/claims:20-28` | 0; counts equal; 3 handlers with 4 mappings each |
| layer-restricted types stay in their layer | `r1/claims:31-36` | `@RestController` in `assistant command query`; JPA and Spring Data only in `data`; `RestClient` only in `acl`; Spring AI only in `assistant` |
| no `double` or `float`, no Lombok, no MapStruct, no embedded database | `r1/claims:37-42` | 0 in bookstore main code, poms and sources |
| no `@DirtiesContext` as an annotation; `@DataJpaTest` imports `PostgresTestConfig` and keeps the datasource | `r1/claims:42-46` | 0; every class does |
| every `@SpringBootTest` class except the base extends `IntegrationTestBase` | `r1/claims:47` | none missing |
| migration names and money columns | `r1/claims:49-55` | all `V{n}__snake_case.sql`; every `numeric` is `numeric(10,2)`; every currency column is `varchar(3)` |
| every domain exception is referenced from `domaininteraction`; the four use-case exceptions are unchecked | `r1/claims:58-60` | 0 unreferenced; 4 of 4 |
| `acl`: every catch of a `RestClient` exception throws a port exception (4 of 5; the fifth is the error-body reader that returns null); payloads are private nested records (5), the only top-level record is `DistributorProperties`; the base URL property is `distributor.base-url` | `r1/claims:61-65` | as stated |
| aggregates that raise events: only `Order` and `StockItem` define `pullDomainEvents`; `Book` does not | `r1/claims:66-67` | the standard says "an aggregate that raises domain events (`Order`, `StockItem`)" |
| DTOs with a convenience constructor from a domain object | `r1/claims:68` | `DistributorTitleDTO`, `PlaceOrderLineDTO`, `RestockConfirmationDTO` have none (no aggregate behind them; `PendingRestockDTO` has `public PendingRestockDTO(PendingRestock pendingRestock)`, so it is not listed), so the standard says "a DTO that mirrors an aggregate" (PF-T13-9) |

One counterexample was found and shaped the text. The rule file and the Level 1 rule say end-to-end tests end with
`IntegrationTest`; five full-application tests do not (`BookstoreApplicationTests`, `ClockConfigTest`,
`IntegrationTestBaseHooksTest`, `IntegrationTestOfflineTest`, `StorefrontConfigTest`, `r1/claims:48`, same on both trees
`r1/claims:114`). The standard therefore states the suffix as an instruction ("Name an end-to-end test `*IntegrationTest`, for
example `CatalogueIntegrationTest`") and does not claim that every such test already has it (FF-T7-d, product finding
PF-T13-6).

## User-level paths of the Agent OS steps (FF-T10-c)

| Step | What it writes outside the repository | See it | Undo it |
|---|---|---|---|
| Pack step 1: `git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git` | `~/agent-os`: 22 files, 168 KB on disk (`clone:9`, list `clone:11-33`); nothing else | `ls -la ~/agent-os`, `find ~/agent-os -type f \| sort` | `rm -rf ~/agent-os` |
| The same line when `~/agent-os` already exists | nothing: `git clone` stops with "destination path ... already exists and is not an empty directory", exit 128, and the `&&` skips `rm -rf` (`ulp:27-30`) | the error text | none needed |
| Pack step 1: `~/agent-os/scripts/project-install.sh` in a repository | only inside the repository: `.claude/commands/agent-os/` (5 files) and `agent-os/standards/index.yml`; plus one temp file of the script from bare `mktemp`, which macOS puts in the per-user temp folder (not `TMPDIR`) and the script's `trap` removes on exit (`ulp:22-26`) | `git status` in the repository | `git clean -fd .claude/commands/agent-os agent-os` |
| Any Agent OS slash command in a session | files under `agent-os/` of the repository; Claude Code itself keeps its own session and memory state under `~/.claude` (Claude Code behaviour, not an Agent OS step) | `git status` | `git clean -fd agent-os` |
| npm, uv or a package cache | none; the install script calls neither (`ulp:10-21`) | | |
| Claude Code plugin or marketplace | none; no step adds one, so no step here needs an explicit OK from Maik | | |

The proof is my own zero-token run with HOME and XDG variables in the scratchpad: file lists of HOME, XDG and TMPDIR before and
after differ only by the shim's log file (`inst:46-53`, `ulp:8-9`); a grep of both installer scripts for HOME, `~`, XDG,
`.config`, `.cache`, `.claude`, settings, plugin and marketplace finds only the project-relative commands folder and regex
operators (`ulp:10-21`). The only run outside the isolation would have been a bare `mktemp`; I ran the script with a `mktemp`
shim that gives a call without arguments a template under `TMPDIR` (`inst:2-4`, `inst:33-35`) and showed what macOS does
without it (`ulp:22-24`).

## Consistency check (C0-13b, C0-13c)

The token inventory (`spell`, script `token-spelling.py.txt`) lists 31 tokens that the pack scenario, the guide and the
installed files name, with their hits in pack spec 10, spec 11, the SDD guide, the workshop plan, the briefs, the guide pages
on both refs, the installed files and the built tree. Pages 05, 06 and the cheat sheet have identical Agent OS lines on both refs; `origin/factory` adds the Level 4 catch-up lines 132-135 to `reference/troubleshooting.md`, which spell the same branch name (`spell:69-74`). Every token has a hit, except the two below; a misspelled control
token comes out `MISSING` (`spell:65-66`). Path tokens are also checked on disk (`PATH exists`).

| Token | Pack and guide | Installed files and tree | Verdict |
|---|---|---|---|
| `start/level-4-agentos` | spec 10 lines 13 and 467, workshop plan line 307, guide page 05 line 38, cheat sheet line 29 (`spell:5-10`) | the name `step_branch` prints | consistent |
| `git clone ... ~/agent-os && rm -rf ~/agent-os/.git`, `~/agent-os/scripts/project-install.sh` | spec 10 lines 468-469, SDD guide lines 79-80 and 151, guide page 06 lines 115 and 121 (`spell:11-14`) | the script exists and is executable (`spell:13-14`, `clone:34-35`) | consistent |
| `/plan-product`, `/discover-standards`, `/index-standards` | spec 10 lines 470 and 473-474, SDD guide lines 82 and 84 (`spell:15-20`) | each is a file in `.claude/commands/agent-os/`; the files spell the short form (`plan-product.md:197`, `index-standards.md:120`) | the files exist; the typed form differs, see PF-T13-1 |
| `/shape-spec`, `/inject-standards`, `/inject-standards testing` | SDD guide lines 85-86 and 131-133, workshop plan line 307, guide page 05 lines 38, 174, 182, cheat sheet line 29 (`spell:21-26`) | files exist; `testing` is a folder of `index.yml` and of the standards | files and folder consistent; typed form, PF-T13-1 |
| `agent-os/product/{mission,roadmap,tech-stack}.md` | SDD guide lines 27-30 name `product/...` and `agent-os/product/...` | `plan-product.md:192-194` lists exactly these three file names (`spell:29-34`) | consistent (the pack text lists two inputs, PF-T13-2) |
| `agent-os/standards/`, `agent-os/standards/index.yml`, `agent-os/standards/architecture/clean-hexagonal-onion.md` | spec 10 line 474, SDD guide lines 29 and 83, guide page 05 line 163 | the index path is `agent-os/standards/index.yml` (`discover-standards.md:164`); the architecture file exists with the guide's name | consistent |
| `agent-os/specs/` | SDD guide lines 18 and 132, guide page 05 line 177 | `shape-spec.md:123,138,186` creates it | consistent (created at use, not staged) |
| `agent-os/standards/global/tech-stack.md` | not named | `plan-product.md:91` reads it; the installer does not create it | PF-T13-3 |
| `/agent-os:plan-product` (the namespaced form) | not in pack, SDD guide or guide pages | not in any installed file; the Claude Code docs derive it (`docs:9`, `skills.md:449`) | PF-T13-1 |
| `Agent OS 3.0` | SDD guide line 7, tech stack row | the clone is `v3.0.0-8-ge892e61` | consistent with "main, includes install-script fixes after v3.0.0" (spec 10 line 468) |

Invocation form (criterion 7): the installed files sit at `.claude/commands/agent-os/<name>.md`. The Claude Code docs say a file
in a subdirectory of `.claude/commands/` is named "Subdirectory path relative to `commands/` with each `/` replaced by `:`"
(`docs:9`, `skills.md:449`: `.claude/commands/frontend/component.md` gives `/frontend:component`). So participants type
`/agent-os:plan-product`, `/agent-os:discover-standards`, `/agent-os:index-standards`, `/agent-os:shape-spec` and
`/agent-os:inject-standards`, whereas the pack, the guide and the installed files themselves write the short names. The docs
do not say whether the short name also resolves; rows R4C-1 and R4C-4 make Maik record what the `/` menu lists and which form
works.

## Scenario status (one status each, C0-11 reduced)

| Scenario or step | Status | Where |
|---|---|---|
| Pack step 1: clone Agent OS, remove `.git`, run `project-install.sh` | zero-token run with evidence | `clone`, `inst`, `ulp`; optional row R4C-5 for Maik |
| Pack step 2: `/plan-product` with the pack mission and tech stack | consistency-checked files plus rehearsal row | files `agent-os/product/*`, row R4C-1 |
| Pack step 3: `/discover-standards` for architecture, testing and persistence | consistency-checked files plus rehearsal row | files `agent-os/standards/**`, row R4C-2 |
| Pack step 3: `/index-standards` | zero-token run with evidence plus rehearsal row | `r1/rr:103-116`, row R4C-3 |
| Pack step 4: commit everything | zero-token run with evidence | sandbox 4 commit (`r1/san:3-10`), fresh build tree (`r1/san:38-43`); round 0: `san:3-10`, `san:38-43` |
| Scenario "Rehearsed in time" (all three tracks) | rehearsal row for Maik, unproven until he rehearses | R4C-4 |
| `./mvnw clean verify` on the branch content | zero-token run with evidence | `r1/ver:4-15` (round 0: `ver:4-15`) |

## Rehearsal rows (draft for the Level 4 section of the checklist)

Common rules, all rows: the branch is `start/level-4-agentos`; every run starts with `git clone --branch start/level-4-agentos <path or URL of the repository> <new run folder>` and a recorded start commit `<start>` (`git rev-parse HEAD`), and a NEW run
folder per run (the default; Claude Code's auto memory is keyed by the clone path and lives outside the repository, FF-T3-b), or
a `/memory` check before each run in the same folder. Between repeated runs in one folder: `git reset --hard <start> && git clean -fdx` (a stale ignored `bookstore/target/classes/db/migration` file survives a plain reset, `reh:25-26`). Judge against `<start>`,
commits included: `git diff --stat <start>` and `git log --oneline <start>..HEAD` (`reh:9-15`). Verify is judged with
`./mvnw clean verify` in the run folder. Commands load when the session starts: the docs say live change detection covers
`.claude/skills/` and that commands of an added directory are not watched (`docs:6-8`), and no line says the project
`.claude/commands/` is watched, so every row starts a new `claude` session after the clone, and if the `/` menu lacks the
`agent-os` entries, quit and start `claude` again in the run folder before judging anything. These rows run the Agent OS
commands, which are interactive (the command files call `AskUserQuestion`, `plan-product.md:7`), so run them in an
interactive session. The staged files are the pack fallback for rows R4C-1 to R4C-3: keep them (pack spec 10 lines 488-491).

### R4C-1 Command names and `/plan-product`

| Field | Content |
|---|---|
| Branch | `start/level-4-agentos` |
| Setup | Common rules, then `claude` in the run folder. Type `/agent-os` in the prompt and read the menu. |
| Prompt, part 1 | Open the `/` menu (type `/agent-os`) and write down the five names it lists. Expected from the docs: `agent-os:plan-product`, `agent-os:discover-standards`, `agent-os:index-standards`, `agent-os:inject-standards`, `agent-os:shape-spec` (`docs:9`). Then try the short form `/plan-product` and write down whether it resolves. |
| Prompt, part 2 | Run the form that works. If it asks "I found existing product documentation" (`plan-product.md:23-33`), answer `1` (start fresh). Answer the questions from the pack text: problem, users and unique solution from `docs/spec-pack/product/mission.md` (Problem, Target users, Solution), MVP and post-launch features from `docs/spec-pack/product/roadmap.md` lines 22-33 (the build order), the tech stack from `docs/spec-pack/product/tech-stack.md`. The pack folder exists only on the factory branch: paste from a factory checkout. |
| Expected | It asks one question at a time through `AskUserQuestion` (`plan-product.md:7-9`), writes `agent-os/product/mission.md`, `roadmap.md` and `tech-stack.md` with the headings of `plan-product.md:131-183`, and prints "Product documentation created:" with the three paths (`plan-product.md:189-198`). It will first look for `agent-os/standards/global/tech-stack.md` (`plan-product.md:91`); that file is absent, so it asks the free tech stack question (PF-T13-3). `git diff <start>` shows the differences from the staged files; read them against `trace` (staged mission = pack lines verbatim). |
| Failure | (a) No `agent-os` entry in the menu after a restart. (b) It asks for or writes files with other names or headings. (c) The generated mission or tech stack contains a version, feature or rule that is not in the pack text you pasted. (d) It never calls `AskUserQuestion` and asks in plain text (record it: the command says always use the tool). |
| Pack fallback | The staged product files are the pack's own text (spec 10 lines 488-491): keep them; do not commit the generated ones unless you prefer them. |
| Recording rule | Per run: date, `<start>`, `claude --version`, the five menu names as listed, the form that worked, whether `AskUserQuestion` was called, and the diff summary. Undo: delete the run folder. |

### R4C-2 `/discover-standards` for architecture, testing and persistence

| Field | Content |
|---|---|
| Branch | `start/level-4-agentos`, a new run folder (rows share a folder only with the reset of the common rules) |
| Setup | Common rules; `claude`; run the form that worked in R4C-1 for `discover-standards`. Keep the staged standards in place and read each proposal against the staged file of the same topic. |
| Prompt | Name the area when you run the command: `architecture`, then answer its questions; repeat for `testing` and for `persistence` (the command accepts an area, `discover-standards.md:15`). Select all proposed standards. Answer "why" questions from `docs/architecture.md` sections 2 to 6. |
| Expected | For each area the command proposes standards (`discover-standards.md:50-66`), asks 1-2 questions per selected standard, shows a draft and writes it to `agent-os/standards/<folder>/<name>.md` (`discover-standards.md:92-121`), then offers index entries and updates `agent-os/standards/index.yml` (`discover-standards.md:123-147`). The folder for persistence may be `database` (the command's own example list) or another name; record it (D-2). |
| Failure | (a) A written standard contradicts `docs/architecture.md`, or recommends Lombok, MapStruct, a `double` or `float` price, an embedded test database or an edited migration. That is a defect of the proposal: do not accept it. (b) A rule with no source in the docs or the code. (c) `index.yml` out of date after the run (run R4C-3). |
| Pack fallback | Keep the staged standards (spec 10 lines 488-491); the pack says to review the generated files against `docs/architecture.md` (spec 10 line 473). |
| Recording rule | Per run: date, `<start>`, the three areas, the folders and file names it created, the list of rules you rejected and why, and `git diff --stat <start>`. |

### R4C-3 `/index-standards`

| Field | Content |
|---|---|
| Branch | `start/level-4-agentos` on the staged tree (a new run folder) or after R4C-2 (if R4C-2 changed standards, expect a different count and an `index.yml` diff) |
| Setup | Common rules; `claude`; the form that worked in R4C-1 for `index-standards`. |
| Prompt | Run `index-standards` with no argument. |
| Expected | On the staged tree it finds 13 standards, none new, none stale, and prints a report of the form of `index-standards.md:105-111` ("Index updated:", "entries unchanged", "Total: 13 standards indexed"); `git diff <start> -- agent-os/standards/index.yml` is empty, or at most differs by trailing blank lines (Claude may write the file without the blank line that the installed builder adds). Zero-token equivalent that needs no session, run verbatim on a clone of the start branch: `~/agent-os/scripts/project-install.sh --commands-only` prints "Updated index.yml (13 entries)" and `git status --porcelain` stays empty, because the staged file already ends with the blank line the builder writes (`r1/rr:103-111`). In round 0 the staged file lacked that line and the same command showed ` M agent-os/standards/index.yml` with one inserted line (`r1/rr:112-116`). |
| Failure | It asks to describe entries that already have a description, or changes the file layout, or removes entries (`index-standards.md:55-62`). |
| Pack fallback | The staged `index.yml`. |
| Recording rule | Date, `<start>`, the report text, the diff of `index.yml`. |

### R4C-4 Rehearsed in time (Agent OS), the pack scenario

Pack text (spec 10 lines 477-481), checked word by word below: "Scenario: Rehearsed in time. GIVEN a fresh clone on the track's
start branch. WHEN a facilitator who has not used the framework before ships Level 4 brief A following
`workshop/sdd-framework-guide.md`. THEN the feature is committed through `/ship` with verify green within 35 minutes (record
the time per track)".

| Field | Content |
|---|---|
| Branch | `start/level-4-agentos` (GIVEN: "a fresh clone on the track's start branch") |
| Setup | Common rules (`git status --short` prints nothing in the fresh clone), then the guide's common start: `claude`, then `/model sonnet` (`sdd-framework-guide.md:93-98`). Open the pack brief A in `docs/spec-pack/workshop/level-4-feature-briefs.md` lines 6-12 (reviews and ratings); like all of `docs/spec-pack/`, that file exists only in a checkout of the factory branch, never in a clone of `start/level-4-agentos` (paste the brief from the facilitator's copy). |
| Prompt and steps | Track C of `sdd-framework-guide.md:129-136`: (1) read `agent-os/standards/` and fix anything wrong; (2) enter plan mode (`/plan` or `Shift+Tab`), then run `shape-spec` (form from R4C-1) with the brief and your decisions; it refuses outside plan mode (`shape-spec.md:13-23`); (3) answer its questions and let it save `agent-os/specs/<date>-<slug>/`; (4) approve the plan, let Claude implement it, use `inject-standards testing` if tests drift; (5) `/ship add book reviews`. |
| Time | Start event: you press Enter on the first framework command (`shape-spec` in plan mode); write down the clock time `t0`, and also the time at which you began reading `agent-os/standards/` (step 1), so that reading time is visible. Stop event: `/ship` prints the commit hash and, outside Claude, `./mvnw clean verify` in the run folder ends `BUILD SUCCESS`; clock time `t1`. The pack's 35 minutes is judged against `t1 - t0`. If a command or `/ship` has not finished at 35:00, write down which one is running or last finished and when it finishes ("started but not ready"); that counts as a miss. If the `agent-os` commands are missing from the menu or `/ship` does not start, write the time lost and the restart and keep the clock running. If a run stops on an infrastructure error (429, login, Docker), quote the message and repeat it in a new clone folder; a run that finished with a wrong result is not replaced. |
| Expected | A commit through `/ship` (the level-3 commit hook runs the build first), `agent-os/specs/<date>-<slug>/` holds the shaped spec, `git diff --stat <start>` shows the feature, its tests and that folder, and `./mvnw clean verify` ends in `BUILD SUCCESS` with `ArchitectureTest` green (`r1/ver:11-15` shows the form). Every acceptance scenario of the spec maps to a test (guide "Level cleared when"). |
| Failure | More than 35 minutes to the commit; the commit is not made through `/ship`; verify not green; `/shape-spec` (any form) unusable; the facilitator had used the framework before (then say so: the GIVEN clause is not met and the time is a lower bound). |
| Pack fallback | None stated for this scenario. On failure keep `t0`, `t1`, the run folder and report the step that took longest. |
| Recording rule | Per track one line: date, `<start>`, `claude --version`, the model, `t0`, `t1`, `t1 - t0`, pass or fail, brief, whether the facilitator was new to Agent OS. Undo: `git reset --hard <start> && git clean -fdx`, or delete the run folder. |

Word check against the pack scenario: GIVEN "a fresh clone on the track's start branch" is the setup row (new clone, `<start>`
recorded, FF-T3-a and FF-T3-b); WHEN "a facilitator who has not used the framework before" is the failure column's last item;
"ships Level 4 brief A" is the setup row (brief A) and the steps; "following `workshop/sdd-framework-guide.md`" is the steps
(`sdd-framework-guide.md:129-136`, the same five steps as guide page 05 lines 161-191); THEN "committed through `/ship`" is the
expected and failure columns; "with verify green" is the expected column; "within 35 minutes (record the time per track)" is the
time and recording rows.

### R4C-5 Optional: the pack's install step on a facilitator machine (FF-T10-c)

Only for a machine that has no `~/agent-os` yet and where Maik wants to see the install work end to end; the branch already holds
the result, so the other rows do not need it. Every command below was run verbatim in round 1 with HOME, XDG and TMPDIR in a
scratch folder (`r1/rr`), so the expected lines are the real ones.

| Field | Content |
|---|---|
| Run folder | A clone of `solution/level-3`, which has no `agent-os/` folder: `git clone --branch solution/level-3 <path or URL of the repository> <run folder>`. Record `<start>` (`git rev-parse HEAD`). Not a clone of `start/level-4-agentos`: that branch already holds the install result and the installer asks first (variant B below). |
| Setup | `git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git` (use `HOME=<scratch>/home` in front of both commands and of the next one for a scratch copy), then `cd <run folder> && ~/agent-os/scripts/project-install.sh`. |
| Paths written | (1) `~/agent-os`: one new folder, 22 files, about 168 KB. (2) In the run folder: `.claude/commands/agent-os/` (5 files) and `agent-os/standards/index.yml` (28 bytes). (3) One temp file: the script calls bare `mktemp` when it installs standards, macOS puts the file in the per-user temp folder (`/var/folders/.../T/tmp.XXXXXX`, not `TMPDIR`) and the script's `trap` removes it on exit; none is left behind (`rr:36-39`, `ulp:22-26`). Nothing else: no npm, uv or pip cache, no `~/.config` entry, no Claude Code plugin or marketplace, so no extra OK is needed. |
| See it | `find ~/agent-os -type f \| sort`, `git status --porcelain` in the run folder. Undo: `rm -rf ~/agent-os`, and `git clean -fd .claude/commands/agent-os agent-os` in the run folder (or delete the run folder). |
| Expected (variant A, the run folder above) | No prompt, exit status 0, the output of `rr:6-29`: `Profile: default`, `Commands only: false`, "Created agent-os/ directory structure", "No standards to install (profile is empty)", "Created index.yml (no standards to index)", "Installed 5 commands to .claude/commands/agent-os/", "Agent OS installed successfully!" (the check marks print as `?` in the evidence file). `git status --porcelain` shows exactly `?? .claude/commands/agent-os/` and `?? agent-os/`; `agent-os/standards/` holds only `index.yml` (`rr:31-35`). |
| Variant B (a clone of `start/level-4-agentos`) | The installer prints "Existing standards folder detected at: <run folder>/agent-os/standards" and "This will overwrite your existing standards with standards from the 'default' profile.", then waits for one key. Answer `n`: "Installation cancelled." and a hint to use `--commands-only`, exit 0, nothing changes (`rr:41-63`). Answer `y`: "No standards to install (profile is empty)", "Updated index.yml (13 entries)", "Installed 5 commands", exit 0, and `git status --porcelain` stays empty, because the empty profile copies nothing and the index file comes back byte-equal (`rr:65-101`). So neither answer harms the branch content; the prompt is the installer's own and not a failure there. |
| Failure | (a) In variant A: any prompt, a nonzero exit status, a path outside the three groups above in `git status`, or a modified tracked file. (b) In variant B: any entry in `git status` after either answer. (c) The clone line stops with "destination path ... already exists and is not an empty directory" (exit 128): then `~/agent-os` already exists, nothing was deleted (`ulp:27-30`); use the scratch HOME. |
| Pack fallback | None stated; the branch holds the installed result. |

## Directly written files, for the closing list of the checklist (plan amendment A2)

Written by hand from the pack and the code, to be reviewed by Maik in rows R4C-1 to R4C-3:
`agent-os/product/mission.md`, `agent-os/product/roadmap.md`, `agent-os/product/tech-stack.md`,
`agent-os/standards/architecture/{class-naming,clean-hexagonal-onion,dependency-rules,domain-model,flows-and-ports,inbound-adapters,money-is-exact,outbound-acl}.md`,
`agent-os/standards/database/{flyway-migrations,persistence-adapter}.md`,
`agent-os/standards/testing/{database-in-tests,layer-tests,test-conventions}.md`, `agent-os/standards/index.yml`.
Installed upstream, byte copies of the clone at `e892e61d`, exempt from the ASCII and dash rules because they are upstream text:
the five files `.claude/commands/agent-os/{discover-standards,index-standards,inject-standards,plan-product,shape-spec}.md`
(`r1/stg:40-48`: 23, 8, 29, 4 and 23 lines with non-ASCII characters).

## Decisions and review items for Maik

| # | Item | Options (none chosen for you) |
|---|---|---|
| D-1 | The roadmap source. `/plan-product` writes `roadmap.md` and asks for MVP and post-launch features (`plan-product.md:69-87`), but spec 10 line 470 names only `mission.md` and `tech-stack.md` as input, while the SDD guide line 82 names three. I used the pack build order (`roadmap.md:22-29` as Phase 1, `:33` as Phase 2), leaving out the foundation, baseline, reference solutions and guide rows. | (a) keep; (b) all rows of the pack table; (c) "To be defined" placeholder text, which the command allows (`plan-product.md:203`) but the criteria forbid for placeholders |
| D-2 | Folder name for persistence: `database/` (the command's own example list, `discover-standards.md:93`) or `persistence/` (the pack's area word, spec 10 line 473). | rename the folder and the index key; nothing else changes |
| D-3 | The architecture standard is 8 concise files, not a copy of the pack's `architecture-standard.md`. The SDD guide line 83 says to copy it to `agent-os/standards/architecture/clean-hexagonal-onion.md`; spec 10 line 473 says to discover standards. The staged file with that name is the concise one. | (a) keep concise; (b) replace the one file by the full copy |
| D-4 | `mission.md` is the pack text with one substitution: `architecture-standard.md` (a factory-only file) becomes `docs/architecture.md` (on every branch). | keep, or restore the original name |
| D-5 | The clone is pinned by sha `e892e61d`; upstream `main` may have moved on the day. The staged command files are byte copies of that sha. | re-run `agent-os-clone.sh` and `cmp` the five command files before the workshop |
| D-6 | The written standards are my reading of the rulebook and the code. Rows R4C-2 and R4C-3 are where Maik compares them with what `/discover-standards` proposes. | accept, edit, or replace by the generated ones |

## Product findings (pack, guide and upstream text, never rework targets of this task)

| # | Finding | Evidence |
|---|---|---|
| PF-T13-1 | The typed command names differ. The installed files live in `.claude/commands/agent-os/`, which Claude Code names `agent-os:<name>`; the pack, the SDD guide, guide page 05 (`/shape-spec`, `/inject-standards testing`) and the cheat sheet write the short names. Participants who type the short form may get "unknown command" or an ambiguity; Maik's R4C-1 shows which. | `docs:9`, `spell:15-26`, `spell:57-58` |
| PF-T13-2 | `/plan-product` takes no files: it asks questions through `AskUserQuestion`. Spec 10 line 470 ("with `product/mission.md` and `product/tech-stack.md` as input") reads as a file input; the facilitator pastes answers. The command also asks for a roadmap that spec 10 does not name (D-1). | `plan-product.md:41-121`, `spell:15-16` |
| PF-T13-3 | The installer copies only `profiles/<name>/standards/**` but the default profile ships `global/tech-stack.md`, so after `project-install.sh` the file `agent-os/standards/global/tech-stack.md` that `/plan-product` step 4 looks for does not exist ("No standards to install (profile is empty)"). The branch "Same as standard" never fires. | `plan-product.md:91`, script line 224, `clone:29`, `inst:18` |
| PF-T13-4 | Guide page 05 Track C says the standards "were discovered from the codebase". On this branch they were written by hand from the pack and the code (decision D14); the sentence holds only once Maik has rehearsed R4C-2 and kept the generated ones. | guide page 05 line 163 on both refs (`spell:35-36`) |
| PF-T13-5 | `docs/architecture.md` lines 65-66 say a price outside the domain is "a `BigDecimal` plus a currency code"; payloads and views carry the amount as a plain decimal string. The written `money-is-exact.md` follows the code and says so (code only, `trace` rows for `AddBookPayload` and `BookView`); same finding as PF-QA-T7-3. | `trace` (money rows), `claims:37` |
| PF-T13-6 | Five full-application tests are not named `*IntegrationTest`, against the Level 1 rule and `docs/architecture.md` section 6 wording. | `r1/claims:48`, `r1/claims:114` |
| PF-T13-7 | The SDD guide line 83 and the pack `architecture-standard.md` line 5 say the architecture standard can be copied to `agent-os/standards/architecture/clean-hexagonal-onion.md`; spec 10 line 473 says to discover it. Two instructions for one file (D-3). | `spell:39-40` |
| PF-T13-8 | `project-install.sh` calls bare `mktemp`, which on macOS writes to the per-user temp folder and ignores `TMPDIR`; harmless (the script removes it), but it is a write outside the project. | `ulp:22-26`, script line 217 |
| PF-T13-9 | Three DTOs have no convenience constructor from a domain object (`DistributorTitleDTO`, `PlaceOrderLineDTO`, `RestockConfirmationDTO`), and `Book` collects no events; `docs/architecture.md` lines 98 and 118 word both as general rules. The written standards follow the code ("an aggregate that raises domain events", "a DTO that mirrors an aggregate"). | `r1/claims:66-68` (round 1 listed four; corrected in the closing round: the list is derived in `r2/claims`) |

Not findings: the pack says "Agent OS 3.0 (clone `main`, it includes post-3.0 install-script fixes)"; the clone is 8 commits
after `v3.0.0` (`clone:7`), so that sentence is true at the recorded sha.

## Residuals

All three Claude-session steps and the scenario "Rehearsed in time" are unproven until Maik rehearses them (rows R4C-1 to
R4C-4). The commands depend on `AskUserQuestion` (27 mentions in the five files), which needs an interactive session. No
agent measured a time. The branch `start/level-4-agentos` does not exist until task T14 cuts it; the rehearsal commands
were dry-run against a local bare repository that holds the built commit under that branch name (`reh`).

## Hygiene

| Check | Result |
|---|---|
| Leak grep over this record, the staged step and the evidence, both folders (positive control) | 0 hits; the control copy gives 1 (`r1/hyg:3-5`) |
| Non-ASCII and dash characters in the record, the evidence and the written files (positive control) | 0 files with non-ASCII bytes (record, both evidence folders, written files), 0 dashes, no trailing space in the record and the written files; generated evidence lines may carry trailing spaces and are not checked (`r1/hyg:8-13`); controls give 1 each (`r1/hyg:9`, `r1/hyg:11`) |
| No done or solution branch name for Level 4 anywhere in the track's files (positive control on a copy, FF-T10-b) | 0 hits, control 1; the only branch name used is `start/level-4-agentos`, as `step_branch` prints it (`r1/hyg:15-18`) |
| Scope: only `S/level-4-agentos/**`, this record and `S/records/evidence/level-4-agentos/**` changed (plus the agent memory directory of this task; two files under `.claude/agent-memory/qa-test-verifier/` are the verifier's, not mine); `rehearsal-checklist.md` is byte-equal to the spec tip; the 39 round 0 evidence files are byte-equal to their pre-edit copies (`cmp`) | `r1/hyg:24-32` |
| No `.log` file, no `*.jsonl`, no Claude invocation, no user-scope read (C0-13a) | 0 hits for the invocation pattern, 0 jsonl, 0 log files (`r1/hyg:20-21`) |
| Referee `./mvnw clean verify` in the track worktree | `BUILD SUCCESS`, totals equal the factory baseline (`r1/ref:3-12`) |
| Sandboxes kept under `$SP` until the gate passes | `s10-t13-impl-sbx-1` to `-sbx-6` (1 to 3 are round 0, 4 to 6 round 1), `s10-t13-impl-home`, `s10-t13-impl-xdg`, `s10-t13-impl-clone-1`, `s10-t13-impl-remote.git`, `s10-t13-impl-raw` |
