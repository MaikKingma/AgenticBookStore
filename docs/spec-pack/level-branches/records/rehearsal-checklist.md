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
| Setup, per run | A fresh session each time (three in total), each with clean Claude Code auto memory. Auto memory lives outside the repository and is keyed by the clone path, so a `git reset` in the same folder does not clear it. Default (added here): a new clone folder per run, `git clone --branch solution/level-1 <path or URL of the repository> <new empty folder>`, and in it `git rev-parse HEAD` prints `<start>` (the commit recorded once in R1-1 Setup). In that folder start `claude`, then `/model sonnet` (workshop plan line 78). New sessions start in auto permission mode; leave it (plan lines 101 to 102). Added here, before every run (1, 2 and 3), after the judging of the previous run: `git reset --hard <start> && git clean -fdx` in the folder you use (it changes nothing in a fresh clone and is the same state reset as in R1-1), then start a new session. Reusing the R1-1 clone or an earlier run's folder instead of a new folder is allowed only if `/memory` in the new session, before the prompt, lists no auto memory entry written by an earlier session; otherwise use a new folder. The plan's own reset (line 114) is `git restore . && git clean -fd`; it is not enough here, because `target/` is git-ignored, so run 1's `target/classes/db/migration/V5__*.sql` would survive it and a stale migration would carry into run 2, and a run may also commit, which `git restore .` does not undo. |
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
reset commands (`git reset --hard <start> && git clean -fdx`, in R1-1 after step 3 and before every R1-2 run), the new clone folder per R1-2 run with the `/memory` check as the alternative (clean auto memory), the R1-1 recording rule, the `ArchitectureTest` check (from the plan's "no ArchUnit violation"), the optional
guide-checklist observation, and row R1-3.

## Level 2

Record: `level-2.md` (consistency check, zero-model proofs). The rows below are the parts of Level 2 that need a Claude
Code session or the macOS LSP rehearsal. Pack scenarios: `10-harness-reference-solutions.md` lines 185 to 213.

Common preconditions for every Level 2 row:

- The Level 1 preconditions above hold (the three commands print a path), because `solution/level-2` contains the
  Level 1 files.
- `solution/level-2` exists, cut by the procedure in `README.md` ("Cut and verify procedure"). Check:
  `git ls-tree -r --name-only solution/level-2 -- .mcp.json .claude/settings.json docs/lsp-setup.md` prints the three
  paths.
- Docker is running (`docker info`), Java 25 (`java -version`), Claude Code logged in (`claude`, then `/status`). The
  Level 2 rows need no workshop key.
- For R2-2 to R2-5: `jdtls` is on the `PATH` (`which jdtls` prints a path) and `java -version` shows 21 or later, the
  floor of `jdtls` (`docs/lsp-setup.md` line 11; the pack, the plan and the guide still say 17, finding PF-T6-1 in
  `level-2.md`). On macOS `brew install jdtls` is Maik's step: no agent ran it.
- Clone recipe for every row (added here, FF-T3-a and FF-T3-b): `git clone --branch solution/level-2 <path or URL of the
  repository> <new empty folder>`, in it `git rev-parse HEAD`, written down as `<start>` (a 40-character sha), and
  `git status --short` prints nothing. Use a separate clone, not `git worktree add`: Claude Code keeps auto memory in
  `~/.claude/projects/<project>/memory/`, derived from the git repository, and all worktrees of one repository share it
  (docs, `evidence/level-2/claude-docs-pages.txt`). A new clone folder per fresh session starts with clean auto memory;
  reusing a folder is allowed only after a `/memory` check shows no auto memory entry from an earlier session.
- Undo after a row that edits files, and judge against `<start>` with commits included: `git reset --hard <start> &&
  git clean -fdx`; `git log --oneline <start>..HEAD` and `git diff --stat <start>`; verify results always from
  `./mvnw clean verify`. Why `-fdx` and `clean`: `target/` is git-ignored, so a plain `git restore .` would leave stale
  build output such as `target/classes/db/migration/V5__*.sql` from an earlier run in the folder (no Level 2 row adds a
  migration, but the same text is copied into the later level sections).
- Suggested order in one clone and one session, because the later rows reuse the plugin and the warm index of the
  earlier ones: R2-5 (starting the session is its WHEN), then R2-4 with R2-3, then R2-2 (it edits a file), then undo.
  This order mirrors Part A of the workshop plan. In the "No server running yet" branch of R2-3 the order changes: R2-2 prompt 1 is sent inside R2-3, R2-2 then ends there with its prompt 2 and the undo (branch B of R2-3, when prompt 1 printed no `Found N` line: no prompt 2), and R2-4 Measure 2 (b) follows after that undo. The Claude Code docs say "The language server starts the first time
  Claude edits a file with one of the plugin's extensions" (`evidence/level-2/claude-docs-pages.txt`, code intelligence
  page, step "Confirm the server starts"; the research probe logged the start attempt at the first `.java` edit,
  `level-2.md` note N7). No source says that a read-only request such as find references starts the server, so the
  trigger for the R2-3 request is undocumented: R2-3 records what happens, and if it starts nothing, R2-3 says what to
  do. R2-1 can share the session when the distributor runs. R2-6 and R2-7 are separate.
- Distributor for R2-1: from a clone, `./mvnw -pl distributor spring-boot:run` (or `java -jar
  distributor/target/distributor.jar` after `./mvnw -pl distributor package`), listening on port 8090. Check:
  `curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8090/actuator/health` prints `200`. If something else owns
  port 8090, start the distributor with `--server.port=<free port>` and export
  `DISTRIBUTOR_MCP_URL=http://localhost:<free port>/mcp` in the terminal that starts `claude`; stop only the process you
  started.

### R2-1 MCP is available

| Field | Content |
|---|---|
| Branch | `solution/level-2` |
| Setup (added here, the pack gives only the GIVEN) | Clone recipe above (`<start>` recorded). Start the distributor as above and run the health check (`200`). Then in the clone root start a fresh session: `claude`. Accept the workspace trust dialog if it appears. The first start in a folder asks to approve the project MCP server `distributor` from `.mcp.json`; approve it (pack: "the user approves the project MCP server"; guide: "If Claude Code asks you to approve the project MCP server, approve it. Restart Claude Code if needed."). Until it is approved, the Claude Code docs show the server as pending approval (`evidence/level-2/claude-docs-pages.txt`). |
| The command | `/mcp` |
| Expected (pack) | "`/mcp` shows `distributor` connected with 4 tools". Guide checkpoint, same words: "Expected: `/mcp` shows `distributor` connected with 4 tools." |
| Also look at (added here) | Open the `distributor` entry in `/mcp`: the four tools are `search_titles`, `get_title`, `list_subjects`, `check_availability` (zero-model proof P1 in `level-2.md`: `tools/list` returns exactly these four on `/mcp`). Then ask the guide's question: `Which science-fiction titles does the distributor have that we do not sell yet?` The tools show up as `mcp__distributor__*` (plan lines 209 to 210; guide: "The tools show up as `mcp__distributor__*`."). |
| Failure | (a) `distributor` is not listed after approval and a restart. (b) It is listed as failed, disconnected, pending approval or needing authentication. (c) The tool count is not 4 or a name differs from the four above. (d) The URL shows up unexpanded (`${DISTRIBUTOR_MCP_URL...}`). (e) The health check was not `200` before the session started: not a failure of the branch, fix the distributor first. |
| Notes, not failures | If `/mcp` cannot connect while `curl http://127.0.0.1:8090/mcp` answers, the machine resolves `localhost` to `::1` first and the distributor listens on 127.0.0.1 only (`level-2.md` note N2): record it as a finding. Optional variant for the variable: stop the distributor, start it on another port as in the preconditions, export `DISTRIBUTOR_MCP_URL`, start `claude`: `/mcp` shows `distributor` connected as well. |
| Recording rule (added here, the pack gives none for this scenario) | One session, one result: date, branch and commit (`git rev-parse --short HEAD`), the lines of `/mcp` that name `distributor` and the tool count (quote them), pass, fail or note. |
| Pack fallback | None stated for this scenario in spec 10. The guide (`reference/troubleshooting.md`, "MCP not connecting") names a STDIO fallback: `./mvnw -pl distributor package`, then `claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio` (the profile file `application-stdio.yml` exists, `level-2.md` row M13). On failure report which of (a) to (d) happened with the quoted lines. |

### R2-2 Type error is reported and fixed

| Field | Content |
|---|---|
| Branch | `solution/level-2`, `jdtls` installed |
| Setup (added here, the pack gives only the GIVEN) | Clone recipe above (`<start>` recorded). The plugin is active in the session: R2-5 passed in it, or run `/plugin install jdtls-lsp@claude-plugins-official` (first `/plugin marketplace add anthropics/claude-plugins-official` if the marketplace is missing) and confirm the plugin on the **Installed** tab of `/plugin` (plan lines 191 to 194). In the "No server running yet" branch of R2-3, prompt 1 was already sent there, at its step (3): do not send it again, take its `Found N new diagnostic issues` line as the expected result after prompt 1, and, when that line appeared, continue with prompt 2 (R2-3, row "No server running yet", step (5)); when no such line appeared, branch B of R2-3 applies (failure (a) below with the note given there, no prompt 2). |
| Prompt 1 (literal, plan lines 197 to 198 and guide) | `In `Book.java`, make `price()` return `String` instead of `Money`. Do not touch the callers yet.` The file is `bookstore/src/main/java/nl/openvalue/bookstore/domain/Book.java`, the method is at line 91 and returns the field `price` (a `Money`) at line 92, so the edit makes line 92 a type error (`level-2.md` row K2). |
| Expected after prompt 1 | Pack: "Claude Code reports the diagnostic". Guide checkpoint: "Expected: Claude Code prints `Found N new diagnostic issues ...` under the edit." The Claude Code docs show the line as `Found N new diagnostic issues in M files (ctrl+o to expand)`; Ctrl+O shows the issues. The line appears under the edit before the agent runs any build command. Derived from the code, not observed: N is at least 1 (line 92), and callers that would break are `domaininteraction/BookDTO.java` line 23, `BookTest` and `BookDomainMapperTest`. |
| Prompt 2 (literal, plan line 199) | `Fix the new diagnostics.` |
| Expected after prompt 2 | Pack: "the agent fixes it". Guide: "Watch the agent read the diagnostics and repair the code." Judge outside Claude: `git diff <start> -- bookstore/src/main/java/nl/openvalue/bookstore/domain/Book.java` is empty, or the new code compiles with all callers; `./mvnw clean verify` ends `BUILD SUCCESS`. Quote the last lines. |
| Failure | (a) No `Found N new diagnostic issues` line after the edit (language server not running: see R2-5, `/plugin` tab **Errors**, `claude --debug`). (b) The error is found only after the agent compiles with Maven, so the report did not come from the language server. (c) After prompt 2 diagnostics remain or `./mvnw clean verify` is red. |
| Recording rule (added here) | Date, branch and commit, the quoted `Found N ...` line, the final `git diff --stat <start>` and the tail of `./mvnw clean verify`, pass, fail or note. Undo: `git reset --hard <start> && git clean -fdx`. |
| Pack fallback | None stated for this scenario. Plan lines 200 to 202 and `docs/lsp-setup.md` (Troubleshooting): "Nothing appears? Check `/plugin` (tab **Errors**) and start `claude --debug`. The error `Executable not found in $PATH: "jdtls"` means the binary is missing." Pack task 3: "If LSP fails, stop and ask Maik before writing a workaround." |

### R2-3 Find references across layers (`Money.of`)

| Field | Content |
|---|---|
| Branch | `solution/level-2`, `jdtls` installed |
| Setup (added here, the pack gives only the GIVEN) | As R2-2 (same session is fine, run this before R2-2: it only reads). If the answer comes from the `LSP` tool (see "How to tell it was the language server"), this request is also the cold start measured in R2-4. The docs name the first edit of a `.java` file as the start trigger, not a references request, so a first request before any edit may find no running server: see "No server running yet" below. |
| Prompt (literal, plan line 195 and guide) | `Find all references to `Money.of`.` |
| Expected (pack) | "the LSP tool returns usages in at least two layers". Plan: "The answer comes from the language server, not from a text search, and spans several layers (domain, flows and mappers, tests)." Reference from the factory code (`level-2.md` row K1, `evidence/level-2/resolution-on-factory.txt` R5): layer `domain` (`Book.java` lines 16 and 17, `Order.java` line 108); layer `domaininteraction` (`BookDomainMapper.java` line 27, `CatalogueFlow.java` lines 60, 86, 100 and 110, `OrderDomainMapper.java` lines 26 and 44); tests in seven files. Ten call sites in five main files in all. |
| How to tell it was the language server (added here) | The tool call shown for the answer is the `LSP` tool (the docs list "LSP" among the tools: find references), and no `Grep` call or `grep` in a Bash call comes before the answer. |
| No server running yet (added here, the docs do not say that a references request starts the server) | The Claude Code docs say "The language server starts the first time Claude edits a file with one of the plugin's extensions" (`evidence/level-2/claude-docs-pages.txt`; `level-2.md` note N7). If this is the first language server request in the session and the answer shows no running server (the tool call is not the `LSP` tool, or the `LSP` tool returns an error, and no `Found N new diagnostic issues` line has appeared yet in the session), that alone is neither failure (b) nor failure (c). Do this: (1) quote the answer; (2) record the observation "a references request before any edit did not start the server" as a note for the guide (workshop plan Part A step 3, guide 03 line 72, which put the references request before the first edit); (3) run R2-2 prompt 1, the first edit, and wait for its `Found N new diagnostic issues` line; (4) ask R2-3 again (prompt 1 touches `price()` at lines 91 and 92 only, so the `Money.of` calls stay, unless the agent edited more: say so in the record). Failures (a) to (c) are judged on the second answer; record both answers. If R2-2 prompt 1 does not print a `Found N` line either, that is R2-2 failure (a). Branch B (added here: R2-2 prompt 1 prints no `Found N new diagnostic issues` line under the edit, t0 = Enter on R2-2 prompt 1): (a) write down t1, the end of prompt 1's answer, and still do step (4). (b) If the step (4) answer comes from the `LSP` tool, the server started at the edit but its diagnostics were not ready: record R2-2 failure (a) with the note "no diagnostic under the first edit; server running at step (4)" (so that it is told apart from "no server"), send no prompt 2 (nothing was reported to fix), undo with `git reset --hard <start> && git clean -fdx` and only then run R2-4 Measure 2 (b). R2-4 Measure 1 is then the range "(d): between t1-t0 and t3-t0 seconds, includes my wait", with t3 = the end of the first `LSP` answer that lists usages; if the step (4) answer from the `LSP` tool is empty, the (d) rule of the Failure row applies with the same t0 (t1 = the end of that empty answer). (c) If there is no running server at step (4) either (the tool call is not the `LSP` tool, or the `LSP` tool returns an error): record R2-4 failure (c) "the server does not start", send no prompt 2, undo, stop and report (pack: "If LSP fails, stop and ask Maik before writing a workaround"); R2-4 Measure 2 (b) is not judged, because a missing `Found N` line proves nothing without a running server. (5) Added here, only when the `Found N` line of step (3) appeared (otherwise branch B above): do not send R2-2 prompt 1 again. Its `Found N new diagnostic issues` line from step (3) is R2-2's "Expected after prompt 1". Continue with R2-2 prompt 2, `Fix the new diagnostics.`, and judge R2-2 as written in that row. Until then `Book.java` stays edited (R2-2 prompt 1, step (3)). Then undo: `git reset --hard <start> && git clean -fdx`. Only after that undo run R2-4 Measure 2 (b), because it needs the unchanged tree. In this branch the first-index time of R2-4 starts at the prompt that started the server, see R2-4 Measure 1. Extra source for the error case (added here): the Claude Code tools page says "Claude Code returns an error result for each LSP call on a file whose language server it can't start." (`evidence/level-3/t7/lsp-tools-page-quote.txt`). It says what an LSP call returns when the server cannot start; it does not say that a references request starts the server. |
| Failure | (a) Usages from fewer than two layers (for example only `domain`), in the answer that counts (the first, or the second in the branch above or after an empty first answer, (d) below). (b) The agent answers by text search, in the answer that counts. (c) An LSP error such as `Executable not found in $PATH: "jdtls"` (the binary is missing), in the answer that counts. (d) An empty answer from the `LSP` tool, so the server runs but has not finished indexing: wait for the first index (R2-4), ask again once and record both answers. (d) is judged on the second answer: if that answer comes from the `LSP` tool and lists usages, (d) is only the cold-start observation reported as the range below (and in R2-4), and failures (a) to (c) are judged on that second answer; if the second answer is empty again, (d) stands. Clock times (added here): t0 = Enter on the R2-3 prompt, t1 = the empty answer ends, t2 = Enter on the second ask, t3 = the answer that lists the references ends. Report the cold first-index time as "(d): between t1-t0 and t3-t0 seconds, includes my wait", never t1-t0 alone: the empty answer shows only that the index was not ready then, and t2-t1 is your own waiting time. Both ends are approximate, because a time is taken when an answer ends, not when the index finished. In the branch above the same rule holds for the re-asked R2-3 prompt, with t0 = Enter on R2-2 prompt 1 (the prompt that started the server). An answer that shows no running server at all is the branch above, not (d). |
| Notes, not failures | `Money.of` has three overloads (`Money.java` lines 34, 45 and 49). A references request on one overload lists only its callers; a request by name or on the class lists all. Any answer with usages in `domain` and `domaininteraction` meets the pack words. |
| Recording rule (added here) | Date, branch and commit, the tool name, the layers and files named in the answer (quote them), whether the first request found a server running (in the branch above: both answers, and the note for the guide; in branch B also t1 and the step (4) result: running server or none), the time of the first answer from the `LSP` tool and which prompt it answered (R2-4; after an empty first answer, failure (d), the clock times t0 to t3 and the range reported there), pass, fail or note. |
| Pack fallback | None stated for this scenario. As R2-2: on LSP failure stop and report; no workaround. |

### R2-4 macOS LSP rehearsal (first-index time, multi-module result)

| Field | Content |
|---|---|
| Branch | `solution/level-2`, on macOS (decision D8: macOS only for now) |
| Setup (added here, the pack gives only the GIVEN) | A new clone folder whose name was never used before, for example `l2-lsp-<yyyymmdd-hhmmss>` (the language server keeps its workspace data per folder name, so a new name means a cold cache). Prove the cache is cold before the first request: `ls ~/Library/Caches/jdtls/jdtls-$(printf '%s' "$(basename "$PWD")" | shasum -a 1 | cut -d' ' -f1)` run in the clone root prints `No such file or directory`. This command is macOS only: on Linux and WSL the wrapper uses `~/.cache` and R2-7 gives its own command. Basis: the upstream wrapper `jdtls.py` (v1.63.0) uses `~/Library/Caches/jdtls/jdtls-<sha1 of the basename of the working folder>` on macOS, and `shasum -a 1` equals its sha-1 (`evidence/level-2/external-sources.txt`). Record the versions: `brew list --versions jdtls`, `java -version`. Then R2-5 starts the session. |
| Measure 1: first-index time (added here, the pack gives no definition) | With a stopwatch: from pressing Enter on the prompt that started the server to the end of its answer. That is the R2-3 prompt when its first answer came from the `LSP` tool (stop at the answer that lists the references). It is R2-2 prompt 1 when R2-3 found no running server (see R2-3, "No server running yet"; the docs name the first `.java` edit as the start trigger): stop at its `Found N new diagnostic issues` line. If prompt 1 printed no `Found N` line (branch B of R2-3) there is no stop event: when the server is running at R2-3 step (4), report the range "(d): between t1-t0 and t3-t0 seconds, includes my wait" with t0 = Enter on R2-2 prompt 1, t1 = the end of prompt 1's answer and t3 = the end of the first `LSP` answer that lists usages (if the step (4) answer from the `LSP` tool is empty, t1 is the end of that empty answer, as in R2-3 branch B (b)); when there is no server at step (4) either, report no time (failure (c)). Record which prompt it was. After that answer, the `ls` command above lists the folder. Record the time in seconds. If that answer was empty (R2-3 failure (d): the server started but was not ready), do not report t1-t0 alone: report the range of R2-3 failure (d), "(d): between t1-t0 and t3-t0 seconds, includes my wait". The plugin entry sets `startupTimeout` 120000 (milliseconds) for starting the server (`level-2.md` note N3); a timeout message is a finding. Optional warm comparison: `/exit`, start `claude` again in the same folder, ask again (repeat the prompt that started the server: the R2-3 prompt, or in the "No server running yet" branch R2-2 prompt 1 on a reset tree, first `git reset --hard <start> && git clean -fdx`, because that prompt needs `price()` to still return `Money`; undo again afterwards), record the time and which of the two prompts you repeated. |
| Measure 2: multi-module result (pack task 3) | (a) "references across `bookstore` packages": the R2-3 answer names usages in at least two packages under `nl.openvalue.bookstore`, one of them `domain` (the other `domaininteraction`). (b) "no false diagnostics" on the unchanged tree: ask for a harmless edit in a file that imports from several packages: `In bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java add the one-line comment // rehearsal above the class declaration.` The edit is followed by no `Found N new diagnostic issues` line. Undo: `git reset --hard <start> && git clean -fdx`. In the "No server running yet" branch of R2-3, run (b) only after the undo that ends R2-2, so that the tree is unchanged. In branch B of R2-3 (no `Found N` line under the first edit) that undo is the one after step (4), with no prompt 2 sent; if there is no server at step (4) either, (b) is not judged, because a missing `Found N` line proves nothing without a running server. |
| Expected (pack, task 3) | "Record the first-index time and whether the Maven multi-module build resolves (references across `bookstore` packages, no false diagnostics)." |
| Failure | (a) References from one package only, or none after the first index finished. (b) A `Found N new diagnostic issues` line after the harmless edit (a false diagnostic: quote it with file and line). (c) The server does not start. Pack: "If LSP fails, stop and ask Maik before writing a workaround." (d) After the answer that started the server (Measure 1) the folder named by the `ls` command of Setup does not exist (added here, the cold-cache assumption failed: the working folder of the language server process is not the clone root, so the folder name and the proof of a cold cache are wrong): record it, do not report the time as a cold measurement, and quote the names printed by `ls ~/Library/Caches/jdtls` (names only). |
| Recording rule (pack) | "Record the first-index time and whether the Maven multi-module build resolves": cold time and which prompt started the stopwatch (R2-3 or R2-2 prompt 1), warm time if measured, jdtls and Java versions, the packages named, the result of the harmless edit, pass, fail or note. Added here: when the first `LSP` answer was empty the cold time is the range "(d): between t1-t0 and t3-t0 seconds, includes my wait" with t0 to t3 as in R2-3 failure (d), and the warm time names the prompt it repeated. In branch B of R2-3 with a running server at step (4) the cold time is the range with t0 = Enter on R2-2 prompt 1 and t1 = the end of prompt 1's answer, and R2-2 failure (a) is recorded with its note; with no server at step (4) record failure (c) and no time. |
| Pack fallback | "If LSP fails, stop and ask Maik before writing a workaround." (spec 10 task 3, line 499) |

### R2-5 Plugin loads from the project declaration

| Field | Content |
|---|---|
| Branch | `solution/level-2` |
| Setup (added here, the pack gives only the GIVEN) | (1) No user-scope install: in a folder that is not a clone of this repository start `claude`, run `/plugin`, open the **Installed** tab: `jdtls-lsp` is not listed. If it is listed at user scope, run `/plugin uninstall jdtls-lsp@claude-plugins-official` and note it; if you keep it, this row is void. (2) A fresh clone: clone recipe above (`<start>` recorded). (3) `which jdtls` prints a path. (4) Do not run `/plugin install` or `/plugin marketplace add` in this row. |
| The steps | In the clone root start `claude`. The workspace trust dialog appears: accept it. Then run `/plugin` and open the **Installed** tab. |
| Expected (pack) | "`/plugin` lists `jdtls-lsp` as enabled without a manual install". The plugin id is `jdtls-lsp@claude-plugins-official`, declared in `.claude/settings.json` (line 8 of the staged file). If it shows up only after leaving and restarting `claude`, record that exactly (the pack says the plugin "should simply load after trust"). |
| Failure | (a) `jdtls-lsp` is not listed. (b) It is listed but disabled. (c) A prompt asks to install it or the marketplace `claude-plugins-official` is reported missing (the pack: "The docs describe no install prompt"). (d) Something is listed in the **Errors** tab: quote the row. |
| Recording rule (pack) | "record the result": date, branch and commit, the quoted `/plugin` lines (Installed and Errors tabs), whether a restart was needed, pass or fail. |
| Pack fallback (quoted from spec 10 lines 160 to 162) | "If the plugin does not load from this declaration, remove both keys and document only the install command `/plugin install jdtls-lsp@claude-plugins-official`." Per FF-T4-a that is three edits, not one: (1) both plugin keys of `level-2/files/dot-claude/settings.json` (the file then holds no other key; whether it stays as an empty object or the manifest row for it goes is for the orchestrator to decide, not decided here); (2) the plugin lines of `docs/lsp-setup.md`, lines 25 and 26 ("On this branch the plugin is also declared in `.claude/settings.json`, so Claude Code can load it after you accept the workspace trust dialog."); (3) the same two keys in the level-3 settings file staged by task T7 (spec 10 line 223: "keep its `extraKnownMarketplaces` and `enabledPlugins` keys"). Texts that say the declaration exists would then need a look too: workshop plan lines 193 and 237, spec 10 lines 125 to 126 and 172 to 173. |

### R2-6 ACL contract test from real MCP data

| Field | Content |
|---|---|
| Branch | `solution/level-2` |
| Setup (added here, the pack gives only the GIVEN) | Clone recipe above (`<start>` recorded). Docker is running. No Claude session is needed. |
| The command | `./mvnw clean verify` in the clone root. |
| Expected (pack) | "the multi-role contributor contract test is green and asserts that only authors are imported". Checks (added here): the build ends `BUILD SUCCESS`; `grep -c -E '<(failure|error|skipped)' bookstore/target/surefire-reports/TEST-nl.openvalue.bookstore.acl.DistributorAppServiceImplTest.xml` prints `0`; `grep -c 'keepsOnlyTheAuthorOfATitleWithATranslatorAndAnIllustrator' bookstore/target/surefire-reports/TEST-nl.openvalue.bookstore.acl.DistributorAppServiceImplTest.xml` prints a number above 0 (the test ran; the test name is `keepsOnlyTheAuthorOfATitleWithATranslatorAndAnIllustrator`); the test file names the authors it keeps and the roles it drops: `grep -n -E 'containsExactly\("Dante Alighieri"\)|doesNotContain\("Henry Wadsworth Longfellow", "Gustave Dore"\)' bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` prints two lines (281 and 282 on the sandbox). On the sandbox the build was green: 1349 bookstore tests, 190 distributor tests, 131 tests in `DistributorAppServiceImplTest` (`level-2.md` P3); totals on the real branch can differ when the code base moved. |
| Failure | (a) The build is red. (b) The test name is not in the Surefire report. (c) The assertion lines are missing or name no author. |
| Recording rule (added here) | Date, branch and commit, the last lines of the verify output, the three check outputs, pass or fail. |
| Pack fallback | None stated for this scenario. |

### R2-7 Linux and Windows through WSL (manual check for Maik, decision D8)

| Field | Content |
|---|---|
| Branch | `solution/level-2` |
| Setup (added here, the pack gives only the GIVEN) | A Linux machine (Arch Linux: `yay -S jdtls`; other Linux: extract a JDT LS release and put a `jdtls` wrapper script on the `PATH`) and a Windows machine with WSL (install as on Linux inside WSL; native Windows is not verified). `which jdtls` prints a path, `java -version` shows 21 or later, Docker works. Clone recipe above in the Linux or WSL file system. |
| The steps | The same as R2-5, R2-4 with R2-3, R2-2, R2-1 and R2-6, run inside Linux or WSL, with one change: R2-4's cold-cache command (`~/Library/Caches/...`) is macOS only and is replaced by the one in the next row. |
| Cold-cache proof for Linux and WSL (added here, replaces the macOS command of R2-4) | In a new clone folder whose name was never used before, before the first language server request, in the clone root: `ls ~/.cache/jdtls/jdtls-$(printf '%s' "$(basename "$PWD")" | sha1sum | cut -d' ' -f1)` prints `No such file or directory`. Basis: upstream `jdtls.py` v1.63.0, line 91 `elif system == 'Linux' and 'HOME' in os.environ:`, line 92 `cachedir = Path(os.environ['HOME']) / '.cache'` and line 99 `jdtls_data_path = os.path.join(cachedir, "jdtls-" + sha1(cwd_name.encode()).hexdigest())` (lines 83 and 98: `cwd_name` is the basename of the working folder, the data folder sits under `jdtls`). WSL runs the Linux branch of that script. After the answer that started the server (R2-4 Measure 1) the folder exists; if it does not, the working folder of the language server is not the clone root: record it, do not report the first-index time as cold, and quote the names printed by `ls ~/.cache/jdtls` (names only). Native Windows is not covered (decision D8). |
| Expected | The same as in those rows, quoted there. Spec 10, definition of done: "Linux and Windows through WSL listed as a manual check for Maik (decision D8)." |
| Failure | Any failure listed in those rows. Also: `jdtls` found in WSL but not by a Claude Code that runs on the Windows side (the PATH differs): record where `claude` and `jdtls` run. |
| Recording rule (added here) | Per machine: OS and version, `jdtls` version, Java version, the output of the cold-cache command before the first request, first-index time (cold only when that command printed `No such file or directory` and the folder exists afterwards), one pass, fail or note per row. |
| Pack fallback | None stated. Pack task 3: "If LSP fails, stop and ask Maik before writing a workaround." |

### Word check against the pack scenario text (Level 2)

Pack text, `docs/spec-pack/specs/10-harness-reference-solutions.md` lines 185 to 213, quoted whole:

> ### Requirement: The language server works on the real repository
> On `solution/level-2`, Claude Code's LSP tool SHALL report diagnostics and resolve references for the bookstore
> module through the `jdtls-lsp` plugin.
>
> #### Scenario: Type error is reported and fixed
> - **GIVEN** `solution/level-2` and `jdtls` installed
> - **WHEN** a type error is introduced in a bookstore domain class
> - **THEN** Claude Code reports the diagnostic and the agent fixes it
>
> #### Scenario: Find references across layers
> - **GIVEN** `solution/level-2` and `jdtls` installed
> - **WHEN** Claude is asked to find all references to `Money.of`
> - **THEN** the LSP tool returns usages in at least two layers
>
> #### Scenario: Plugin loads from the project declaration
> - **GIVEN** a fresh clone of `solution/level-2`, `jdtls` on the `PATH`, and no user-scope install of `jdtls-lsp`
> - **WHEN** Claude Code starts in the repository and the participant accepts the workspace trust dialog
> - **THEN** `/plugin` lists `jdtls-lsp` as enabled without a manual install (record the result; if it fails, apply
>   the fallback above)
>
> ### Acceptance scenarios
> #### Scenario: MCP is available
> - **GIVEN** the distributor is running
> - **WHEN** Claude Code starts in the repository and the user approves the project MCP server
> - **THEN** `/mcp` shows `distributor` connected with 4 tools
>
> #### Scenario: ACL contract test from real MCP data
> - **WHEN** `./mvnw verify` runs on `solution/level-2`
> - **THEN** the multi-role contributor contract test is green and asserts that only authors are imported

Pack text of task 3 (lines 496 to 499) and of the definition of done (lines 507 to 508):

> 3. LSP rehearsal: run the LSP scenarios on macOS against the real repository (decision D8: macOS only for now; the
>    Linux and Windows-through-WSL runs are a manual check for Maik before the workshop). Record the first-index time
>    and whether the Maven multi-module build resolves (references across `bookstore` packages, no false
>    diagnostics). If LSP fails, stop and ask Maik before writing a workaround.
>
> - LSP rehearsal on macOS listed as Maik's manual rehearsal check (decision D14); Linux and Windows through WSL listed as a
>   manual check for Maik (decision D8).

| Pack words | Where they are in the rows |
|---|---|
| "On `solution/level-2`, Claude Code's LSP tool SHALL report diagnostics and resolve references for the bookstore module through the `jdtls-lsp` plugin." | R2-2 (diagnostics), R2-3 (references), R2-5 (the plugin), R2-4 (bookstore module) |
| "`solution/level-2` and `jdtls` installed" (twice) | R2-2 and R2-3 Branch and Setup (clone recipe, `which jdtls`) |
| "a type error is introduced in a bookstore domain class" | R2-2 Prompt 1 (`Book.java` in the `domain` package; the prompt is the plan's and the guide's) |
| "Claude Code reports the diagnostic and the agent fixes it" | R2-2 Expected after prompt 1 and after prompt 2, failure (a) to (c) |
| "Claude is asked to find all references to `Money.of`" | R2-3 Prompt (plan line 195 and guide, quoted literally) |
| "the LSP tool returns usages in at least two layers" | R2-3 Expected, "How to tell it was the language server", failure (a) and (b) |
| "a fresh clone of `solution/level-2`, `jdtls` on the `PATH`, and no user-scope install of `jdtls-lsp`" | R2-5 Setup (1) to (3) |
| "Claude Code starts in the repository and the participant accepts the workspace trust dialog" | R2-5 The steps |
| "`/plugin` lists `jdtls-lsp` as enabled without a manual install" | R2-5 Expected, failure (a) to (c), Setup (4) |
| "(record the result; if it fails, apply the fallback above)" | R2-5 Recording rule and Pack fallback (quoted, with the three companion edits) |
| "the distributor is running" | R2-1 Setup (start and health check) |
| "Claude Code starts in the repository and the user approves the project MCP server" | R2-1 Setup (trust dialog, approval) |
| "`/mcp` shows `distributor` connected with 4 tools" | R2-1 The command, Expected, failure (a) to (c) |
| "`./mvnw verify` runs on `solution/level-2`" | R2-6 The command (`./mvnw clean verify`, `clean` so no stale build output counts) |
| "the multi-role contributor contract test is green and asserts that only authors are imported" | R2-6 Expected and failure (a) to (c) |
| "Record the first-index time and whether the Maven multi-module build resolves (references across `bookstore` packages, no false diagnostics)" | R2-4 Measure 1, Measure 2 and Recording rule |
| "the Linux and Windows-through-WSL runs are a manual check for Maik before the workshop" (D8) | R2-7 |

Not in the pack scenarios and added here: the preconditions list, the clone recipe with the new clone folder per fresh
session and the `/memory` rule (clean auto memory), the suggested order, the `<start>` commit and the reset and judging
commands, the health check of the distributor and the free-port variant, the tool names and the guide's question in
R2-1, the `LSP` tool check in R2-3 and its row "No server running yet" (the branch that runs R2-2 prompt 1 and asks
again, with the docs sentence on the start trigger, also quoted in the suggested order), the cold-cache proof (macOS,
with its failure branch d), the stopwatch definition of the first-index time with its start rule (the prompt that
started the server, R2-3 or R2-2 prompt 1, recorded) and the harmless edit prompt in R2-4, the clock times t0 to t3
with the reported range for an empty first answer (R2-3 failure (d), R2-4), step (5) of the "No server running yet"
branch (R2-2 prompt 2, judging and undo before R2-4 Measure 2 (b); noted in R2-2 and in the suggested order as well),
branch B of that row (R2-2 prompt 1 prints no `Found N` line: the three sub-cases, the range for R2-4 Measure 1 and
the rule for Measure 2 (b)), the warm comparison that repeats the prompt that started the server, the tools page
sentence in R2-3, the user-scope check and `/plugin uninstall` in R2-5, the Surefire and `grep` checks in
R2-6, row R2-7 setup, steps (with the one change), the Linux and WSL cold-cache proof and recording rule (only the D8
sentence of R2-7's Expected is the pack's), and all recording rules except the pack's "record the result" (R2-5) and
"Record the first-index time and ..." (R2-4).
