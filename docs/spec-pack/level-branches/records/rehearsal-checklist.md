# Rehearsal checklist for Maik (2026-11-10)

Decision D14: no agent runs Claude Code for spec 10. Every scenario that needs a Claude Code session, plus the macOS
LSP rehearsal and "Rehearsed in time", is a row here. The consistency checks that need no model are in
`level-1.md` and the later `level-<n>.md` records. This file holds one section per level; the sections of Levels 2 to 4
are added by the tasks that stage those levels.

How to read a row: Branch, Setup, the exact prompt or command, the expected observation (quoted from the pack where the
pack states it), what counts as failure, the pack's fallback or recording rule, and a word check against the pack
scenario. Text in a row that is not pack text is marked "added here" so it can be changed freely. No row claims a
Claude result: every expected observation is what the pack and the guide say should happen.

Record each row as: date, branch and commit (`git rev-parse --short HEAD`), the exact output lines you quote, and
pass, fail or note. Write notes for the guide as notes, not failures.

## Level 1

Common preconditions for every Level 1 row:

- Spec 09 has published `main` with the tag `level-0-start`, and the branches are cut by the procedure in
  `README.md` ("Cut and verify procedure"), so `solution/level-1` exists.
- The base holds the two files that `CLAUDE.md` and the data rule name. All three commands print a path:
  `git ls-tree level-0-start -- docs/architecture.md`,
  `git ls-tree level-0-start -- bookstore/src/main/resources/db/seed/R__seed_demo_data.sql`,
  `git grep -n "classpath:db/seed" level-0-start -- bookstore/src/main/resources/application.yml`
  (record `level-1.md` rows C12, M7 and M8: today they are arrivals with spec 09).
- Docker is running (`docker info`), Java 25 (`java -version`), Claude Code logged in (`claude`, then `/status`).
  The Level 1 runs need no workshop key.
- Added here: `ls ~/.claude/CLAUDE.md` should not hold a leftover line from Level 1 step 4 (for example "Always use
  Lombok"). User files may appear in `/memory` and `/context`; that is not a failure, only the three named items below
  count.

### R1-1 Rules load on demand

| Field | Content |
|---|---|
| Branch | `solution/level-1` |
| Setup | A throwaway clone: `git clone --branch solution/level-1 <path or URL of the repository> <empty folder>`, then in that folder `git status --short` prints nothing and `ls CLAUDE.md .claude/rules` lists `CLAUDE.md` and `data-and-migrations.md domain.md testing.md`. Added here: in that same folder, before anything else, record the start commit once: `git rev-parse HEAD`, and write the 40-character result down as `<start>` (R1-1 and R1-2 both reset to it). Start a fresh session in the clone root: `claude`. |
| Step 1: question that does not touch `domain` | Added here (the pack gives no wording): `Which command runs one test class in this project?` Any question that mentions neither the `domain` package nor a file in it will do. If Claude reads a `domain` file while answering, the check below is void: run `/clear`, ask another question. |
| Step 2: the command | `/context`. Then `/memory` as well (the pack allows either). |
| Expected after step 2 (pack) | "`CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md` is". Guide checkpoint, same words: "`CLAUDE.md` is loaded, and neither `docs/architecture.md` nor `.claude/rules/domain.md` is." |
| Step 3: make Claude edit a file in `domain` | Added here (the pack gives no wording): `In bookstore/src/main/java/nl/openvalue/bookstore/domain/Money.java add the one-line comment // rehearsal above the record declaration.` Then `/context` (or `/memory`) again. Afterwards undo: `git reset --hard <start> && git clean -fdx`. |
| Expected after step 3 (pack) | "after Claude edits a file in `domain`, the rule is loaded" (the rule is `.claude/rules/domain.md`). |
| Optional, from the guide checklist | "`/context` shows the path-scoped rules loading only for matching files": touch a file under `bookstore/src/test/` and one under `bookstore/src/main/resources/db/`, and see `testing.md` and `data-and-migrations.md` appear only then. `level-1.md` proves by glob test that the three `paths` share no file. |
| Failure | (a) After step 2, `docs/architecture.md` or `.claude/rules/domain.md` is listed as loaded (and Claude did not touch `domain`). (b) After step 2, `CLAUDE.md` is not listed. (c) After step 3, `.claude/rules/domain.md` is not loaded. (d) An `@` import shows up for `docs/architecture.md`: it must not, `level-1.md` P1 shows none in the staged file. |
| Notes, not failures | If the rule appears after Claude only reads a `domain` file (not an edit), record at which step it appeared: the pack and the guide say "edits". If `/context` or `/memory` shows files in a way the pack words do not describe, quote the lines. The glob semantics used in the glob test of `level-1.md` are an assumption about Claude Code; a loaded rule on a non-matching path, or a missing one on a matching path, is a finding for the orchestrator. |
| Recording rule (added here, the pack gives none for this scenario) | One session, one result: record the date, branch and commit, the lines of `/context` or `/memory` after step 2 and after step 3 (quote the lines that name `CLAUDE.md`, `docs/architecture.md` and `.claude/rules/domain.md`), and pass, fail or note. A void step 1 (Claude read a `domain` file) is noted and the step is repeated after `/clear`; the void attempt is recorded, not hidden. |
| Pack fallback | None stated for this scenario in spec 10. On failure stop and report which of (a) to (d) happened with the quoted lines. |

### R1-2 Level 0 task with context

| Field | Content |
|---|---|
| Branch | `solution/level-1` |
| Setup, per run | A fresh session each time (three in total): in the clone of R1-1 start `claude`, then `/model sonnet` (workshop plan line 78). New sessions start in auto permission mode; leave it (plan lines 101 to 102). Added here, before every run (1, 2 and 3), after the judging of the previous run: `git reset --hard <start> && git clean -fdx` (`<start>` is the commit recorded once in R1-1 Setup; a fresh clone per run in a new empty folder works as well), then start a new session. The plan's own reset (line 114) is `git restore . && git clean -fd`; it is not enough here, because `target/` is git-ignored, so run 1's `target/classes/db/migration/V5__*.sql` would survive it and a stale migration would carry into run 2, and a run may also commit, which `git restore .` does not undo. |
| Conduct during each run (pack) | Workshop plan line 103: "Do not help. Do not answer questions with conventions, only with "you decide"." If Claude asks a question, answer only "you decide" (quoted from `workshop-plan.md` line 103, step 2 of the plan's steps). |
| The prompt (literal, paste without adding hints) | Workshop plan lines 97 and 98: "`> Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people`" and "`> search the catalogue.`" Pasted as one paragraph: `Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people search the catalogue.` The guide (`01-level-0-break-the-agent.mdx` line 38) has the same words on one line. |
| Judge each run yourself, outside Claude (added here; every comparison is against `<start>`, so a commit made by the run is seen too) | 1. What changed: `git log --oneline <start>..HEAD`, `git diff --stat <start>` and `git status --short`. 2. Verify: `./mvnw clean verify` in the clone (`clean`, so no stale build output counts), quote the last lines: `BUILD SUCCESS` or not. 3. Domain file: `git diff -U0 <start> -- bookstore/src/main/java/nl/openvalue/bookstore/domain` plus the untracked domain files, `git grep --untracked -n -i discount -- bookstore/src/main/java/nl/openvalue/bookstore/domain`: the discount lives in a `domain` file, typed with `Money`, no `double` or `float` for a price. 4. Migration: exactly one new file named `V{n}__{snake_case}.sql` (V5 when the folder holds V0 to V4), counted as the `A` rows of `git diff --name-status <start> -- bookstore/src/main/resources/db/migration` plus the untracked `??` files of `git status --short bookstore/src/main/resources/db/migration`, and no `M`, `D` or `R` row for V0 to V4 in the same `git diff --name-status` output (a rename shows as `R`). 5. `grep "Tests run" bookstore/target/surefire-reports/nl.openvalue.bookstore.ArchitectureTest.txt`: no failures. |
| Expected per run (pack) | "at least two runs end with `./mvnw verify` green and a `Money`-based discount in the domain with a new migration". Plan, "Level cleared when": "In a fresh session, the Level 0 task ends with `./mvnw verify` green, shown in the terminal, and no ArchUnit violation." |
| Failure | A run is not green when any of these holds: your `./mvnw clean verify` is red; the discount is not in a `domain` file or not `Money`-based (a `double` or `float` price, or a plain `BigDecimal`); no new migration, or more than one new migration, or an existing `V0` to `V4` file is edited, deleted or renamed; `ArchitectureTest` fails. The scenario fails when fewer than two of the three runs are green. |
| Recording rule (pack) | Three runs in fresh sessions, "record outcomes": report all three attempts, red ones included, with the quoted tail of verify, the file and line of the `Money` discount, the migration file name and the `ArchitectureTest` line. Do not rerun a red attempt to improve the count. |
| Pack fallback | None stated for this scenario in spec 10. With fewer than two green runs record the outcomes and report to the orchestrator; do not change `CLAUDE.md` or a rule to make a run pass. |
| Base note (FF-T2-a) | On the real `level-0-start` base `docs/architecture.md` and the seed file exist. If a run tries to read either and finds it missing, the cut precondition above failed; that is a problem of the base, not of the run. |

Result table to fill in:

| Run | `./mvnw verify` tail | `Money` discount (file:line) | New migration | Existing migration edited | `ArchitectureTest` | Green |
|---|---|---|---|---|---|---|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |

### R1-3 Windows build command (added here, not a pack scenario)

| Field | Content |
|---|---|
| Branch | `solution/level-1` |
| Setup | A Windows machine with Java 25 and Docker, a clone of the branch. |
| Command | `mvnw.cmd verify` (the text of `CLAUDE.md` line 7). In PowerShell a command in the current folder needs the `.\` prefix: `.\mvnw.cmd verify`. |
| Expected | `BUILD SUCCESS`. `level-1.md` row C5: `mvnw.cmd` exists on the base but was not executed by any check. |
| Failure | The wrapper does not start or the build is red. A PowerShell prefix issue is a note for the guide, not a failure of the branch. |
| Pack fallback | None stated; report to the orchestrator. |

### Word check against the pack scenario text (Level 1)

Pack text, `docs/spec-pack/specs/10-harness-reference-solutions.md` lines 112 to 119, quoted whole:

> #### Scenario: Rules load on demand
> - **GIVEN** a fresh session on `solution/level-1`
> - **WHEN** the user asks a question that does not touch `domain` and then runs `/memory` or `/context`
> - **THEN** `CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md` is; after Claude edits a file in `domain`, the rule is loaded
>
> #### Scenario: Level 0 task with context
> - **WHEN** the Level 0 discount prompt is run three times in fresh sessions
> - **THEN** at least two runs end with `./mvnw verify` green and a `Money`-based discount in the domain with a new migration (record outcomes)

| Pack words | Where they are in the rows |
|---|---|
| "a fresh session on `solution/level-1`" | R1-1 Branch and Setup (`claude` in a clone of `solution/level-1`) |
| "asks a question that does not touch `domain`" | R1-1 Step 1 (the example question is added here; the "does not touch `domain`" condition and the void rule are the pack's) |
| "then runs `/memory` or `/context`" | R1-1 Step 2 (`/context`, then `/memory`) |
| "`CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md` is" | R1-1 Expected after step 2, failure (a) and (b) |
| "after Claude edits a file in `domain`, the rule is loaded" | R1-1 Step 3 and Expected after step 3, failure (c) (the edit prompt is added here) |
| "the Level 0 discount prompt" | R1-2 The prompt: workshop plan lines 97 and 98, quoted and pasted literally (the guide has the same words) |
| "run three times in fresh sessions" | R1-2 Setup, per run (three fresh sessions), Conduct during each run (the pack's answer policy) and Recording rule |
| "at least two runs end with `./mvnw verify` green" | R1-2 Expected per run, failure (verify red), "fewer than two of the three" |
| "a `Money`-based discount in the domain" | R1-2 judge step 3 and failure (not in a `domain` file or not `Money`-based) |
| "with a new migration" | R1-2 judge step 4 and failure (no new migration, or an edited one; the edit clause comes from `CLAUDE.md` line 22 to 23 and workshop plan line 107) |
| "(record outcomes)" | R1-2 Recording rule and the result table |

Not in the pack scenarios and added here: the preconditions list, the suggested question and edit prompt, the start
commit `<start>` recorded in R1-1 Setup, the judging commands (all against `<start>`, with `./mvnw clean verify`), the
reset commands (`git reset --hard <start> && git clean -fdx`, in R1-1 after step 3 and before every R1-2 run), the R1-1 recording rule, the `ArchitectureTest` check (from the plan's "no ArchUnit violation"), the optional
guide-checklist observation, and row R1-3.
