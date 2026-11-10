# Level 4 Spec Kit record: staging and consistency check (decision D14, amendments A2, A5, A10)

Branch `start/level-4-speckit` (the name `step_branch level-4-speckit` prints, `row:10`): the chain Level 1, Level 2 and Level 3
(tree `309786e0`, `build:63`) plus the 29 rows of `level-4-speckit/manifest.tsv`, all `add` rows of group `config`: the nine
Spec Kit skills, the `.specify/` folder as the init generated it, and one file written directly (the constitution). There is no
Level 4 done or solution branch and no implementation of the Level 4 brief anywhere (decision D17 addendum, amendment A10):
this task stages only what `start/level-4-speckit` holds. No Claude Code session was run (decision D14): no `claude` command,
no login, no plugin or marketplace command, no `ANTHROPIC_API_KEY`. Every `/speckit-...` string below is a skill name taken from
the staged files, the pack or the guide, not a command that was run. Nothing here claims a Claude result. The scenarios that
need a session are the draft rows RR-S1 to RR-S4 in this record for Maik's rehearsal; the closing round adds the pointers and
the "Rehearsed in time" rows to `rehearsal-checklist.md` (amendment A5, not this task).

Evidence files are in `evidence/level-4-speckit/`, `name:N` means line N of that file. Short names: `build`
sandbox-build.txt, `init` specify-init.txt, `files` init-files.txt, `paths` user-level-paths.txt, `f5` f5-hashes.txt, `con`
constitution-check.txt, `trace` constitution-trace.tsv.txt, `inv` token-inventory.txt, `grep` contradiction-grep.txt, `ver`
sandbox-verify.txt, `claims` claim-searches.txt, `regen` regen-compare.txt, `res` resolve-template-run.txt, `st`
staging-selftest.txt, `docs` claude-docs-quotes.txt, `row` row-dry-run.txt, `hyg` hygiene.txt, `ref` referee-verify.txt, `uvm` uv-managed-python.txt. The scripts
are kept as `check-constitution.py.txt` and `token-inventory.py.txt`. Every output line in the evidence files is a byte copy of
the raw output kept in the scratchpad; paths are masked while the files are written (`$SBX` the init sandbox, `$SBX2` the
fresh build, `$HOME` and `$HOME2` isolated home folders, `$SP` the scratchpad). Two files change the raw bytes in a
documented way: `init` replaces the box-drawing characters of the tool output by `|`, `grep` prints the non-ASCII bytes of generated
lines as `?`.

## Verdict

| Check | Result |
|---|---|
| Sandbox: simulated level-0, then `build-level.sh level-3` | tree `309786e063b813e0f9248714f404b927d277d879` (`build:63`), as expected |
| `specify init` without a global install, under isolated HOME, XDG and uv folders | `specify 1.3.1` (`init:4`), exit 0 (`init:7`), 29 new untracked files and no modified tracked file (`init:59-90`) |
| Version and skills | `speckit_version` is `1.3.1` (`files:11`); all 9 skills carry `disable-model-invocation: true` (`files:35-51`, search with control `claims:3-6`) |
| F5 and pack step 3: level-3 files byte-identical across the init | list of 446 tracked files equal before and after (`f5:3-4`); 13 listed config files equal (`f5:6-31`); the Spec Kit commit holds only add rows (`claims:9-10`); `.claude/settings.json` blob is the same at level 3 and at level 4 (`f5:36`) |
| Constitution written directly, no Claude run | no placeholder token, five principles with their key terms, every statement traced to a pack line or to the generated skill (`con:10-12`, table below); 16 controls and 4 trace controls all reported (`con:13-35`); staged file equals the built tree's (`con:36-38`) |
| Staged files against a fresh build | tree of the hand commit in the init sandbox equals the tree of a fresh `simulate-level-0.sh` plus `build-level.sh level-4-speckit`: `c9d758b95c060a9cb24a8c1c7260920fbcea5cef` both (`build:65`, `build:154-156`) |
| `./mvnw clean verify` on the fresh build | `BUILD SUCCESS`, 1428 bookstore tests and 190 distributor tests, 0 failures, `ArchitectureTest` 22 tests, 0 failures, 0 errors (`ver:4-5`, `ver:11`, `ver:15`); the same result on a fresh clone of the branch (`row:69-71`) |
| Independent second init run | same 29 files; 25 byte-identical, 3 differ only in `installed_at` and `updated_at` timestamps, 1 is the constitution (`regen:4-12`, `regen:13-29`) |
| Contradiction review of the generated files | 10 raw hits in the generated files, none a contradiction; two product findings from other searches (PF-T12-3, PF-T12-5) (`grep:4-13`, `grep:32-35`, `grep:74-93`) |
| Token inventory and spelling across pack, spec 11 and guide pages | 137 rows: 122 consistent, 8 dotted notation, 3 run-time paths, 4 unpinned `specify-cli` mentions (product finding PF-T12-1), 0 mismatch; 6 of 6 controls reported (`inv:148-149`, `inv:150-158`) |
| User-level paths | the `uvx` run writes only the uv cache and two lock files in the uv tool folder; the pack's `uv tool install` writes 3 more places (`paths:3-84`), table below; the list assumes a Python 3.11+ on PATH (`product/tech-stack.md` line 71), otherwise uv first downloads a managed CPython 3.11.18 (25.5 MiB, 2400 paths) into `$XDG_DATA_HOME/uv/python` (`uvm:8`, `uvm:23`, `uvm:30-34`) |
| Staging self-test | `PASSED all 58 cases` after the staging (`st:6-8`) |
| Scenario statuses | one each, table below; no Claude result is claimed |

No row is a mismatch in the task's own files or in the staged files, so there is no FAIL to report and no question for the
orchestrator. Seven product findings for Maik are listed below (PF-T12-1 to PF-T12-7).

## Inputs and shas

| Input | Value |
|---|---|
| Base | tag `level-0-start` = `b668073eb44bfea453c291509a615f6de081e54a`; simulated level-0 commit `391d5f74d69591a8e7a4b3cf718a07fe9dbae03c` (`build:5`) |
| Track branch | `sdd/10-harness-reference-solutions/T12`, cut from the spec tip `74a6113` |
| Init sandbox (sbx-1) | level-3 commit `1326c9ed1ed794e8773cd726feaecbc3bd43b9eb`, then the init, the constitution and one hand commit `6aeae990b4805d54fcf07a5e21567b91105d503b` with the step's message `Level 4 start: Spec Kit installed and initialised` (`build:63-65`) |
| Fresh build (sbx-2) | `simulate-level-0.sh level-0-start` and `build-level.sh level-4-speckit`: HEAD `b98d908e74a9f11caf832d472f83c6150a496cfb`, tree `c9d758b9...` (`build:147`, `build:154`) |
| Second init run (sbx-3) | the level-3 tree again (`309786e0`), `regen:4` |
| Tool | `specify-cli==1.3.1` through `uvx --from`, uv 0.13.7, Python 3.13 found on the path; PyPI is the only network use (`init:9-16`, help text `claims:27`) |
| Pack text | `docs/spec-pack/specs/10-harness-reference-solutions.md` lines 430 to 433 (Level 4 intro), 455 to 465 (`start/level-4-speckit`), 477 to 481 (acceptance scenario), 483 to 502 (tasks); `product/tech-stack.md` line 71 (pin) |
| Guide and workshop sources | `docs/spec-pack/workshop/sdd-framework-guide.md`, `workshop-plan.md`, `level-4-feature-briefs.md`; `guide/docs/**` read with `git show` at `origin/sdd/11-workshop-guide/spec` = `b7f053c7a9c64de408831179b44289404c8bb4e5` and `origin/factory` = `711dd9dcdc7562e1d7cae5f5ef7f45ca6fdd8f86`; the three guide pages with Spec Kit lines (`05-level-4-spec-driven-development.mdx`, `06-wrap-up.md`, `reference/cheat-sheet.md`) are identical at the two refs (`inv:5-7`) |
| Claude Code docs | page `skills`, markdown form, read with curl on 2026-11-10 (`docs:2`) |

Environment: every command ran with `TMPDIR` and `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...` in the session scratchpad (HR1);
the init and the `uv` runs ran under `env -i` with `HOME`, `XDG_CONFIG_HOME`, `XDG_CACHE_HOME`, `XDG_DATA_HOME`, `XDG_STATE_HOME`, `UV_CACHE_DIR` and
`UV_TOOL_DIR` in scratchpad folders, so no variable of the real environment (no token) reached them. The init help lists no telemetry
or network switch (`$ specify init --help`: "initialization does not need network access", `claims:27`); none was set. `ANTHROPIC_API_KEY` was neither read
nor written. The Maven runs print `Picked up JAVA_TOOL_OPTIONS` once; a participant's run does not. The init and `uv` runs of the sections above had the real `claude` program on their PATH (it sits next to the `python3` used); a rerun with a logging stub in its place exits 0, writes the same 29 files and does not call the stub in that run, so the init looks the program up and does not start it (`uvm:19`, `uvm:35-36`, `row:157`).

## What the init generated (29 files, staged as they are)

The init output is `init:9-58`; its step list says `Constitution setup (copied from template)` (`init:32`) and `Ensure scripts
executable (6 updated)` (`init:31`). All 29 files are untracked and no tracked file changed (`init:59-90`). A second, independent run
generates the same 29 paths and the same bytes except four lines of timestamps (`regen:4-12`); the three files with a timestamp are
`.specify/integrations/claude.manifest.json`, `.specify/integrations/speckit.manifest.json` and `.specify/workflows/workflow-registry.json`
(`regen:13-29`). A verifier who regenerates the init therefore sees these three files differ in `installed_at` and `updated_at`,
and the constitution differ (the fresh init writes the template, `regen:30-31`), and nothing else.

| Group | Files | Staged as |
|---|---|---|
| Skills (9) | `.claude/skills/speckit-{analyze,checklist,clarify,constitution,converge,implement,plan,specify,tasks}/SKILL.md`, each with `user-invocable: true` and `disable-model-invocation: true` | `files/dot-claude/skills/...`, mode 644 |
| State (5) | `.specify/.gitignore`, `.specify/init-options.json`, `.specify/integration.json`, `.specify/integrations/{claude,speckit}.manifest.json` | `dot-specify/...` |
| Constitution (2) | `.specify/memory/.constitution-template.json` (records the sha256 of the template, `files:64-68`) and `.specify/memory/constitution.md` (written directly, see below) | `dot-specify/memory/dot-constitution-template.json`, `dot-specify/memory/constitution.md` |
| Scripts (6) | `.specify/scripts/bash/{check-prerequisites,common,create-new-feature,resolve-template,setup-plan,setup-tasks}.sh`, mode 755 (`claims:11-19`) | `dot-specify/scripts/bash/...` |
| Templates (5) | `.specify/templates/{checklist,constitution,plan,spec,tasks}-template.md` | `dot-specify/templates/...` |
| Workflow (2) | `.specify/workflows/speckit/workflow.yml`, `.specify/workflows/workflow-registry.json` | `dot-specify/workflows/...` |

The path encoding rule holds for all 29 paths: the staging script refuses a path whose component starts with `dot-` or is named
`CLAUDE.md`, `CLAUDE.local.md` or `AGENTS.md` before it writes anything, and refused none; the dotted file names below a
dotted folder are `.specify/.gitignore` (staged as `dot-gitignore`) and `.specify/memory/.constitution-template.json` (staged as
`dot-constitution-template.json`). `--script sh` generated no PowerShell file (`claims:7-8`), so decision D16 holds. The init
generates no AGENTS.md, no `.specify/extensions.yml` (so no extension hook, no git branch or commit hook, `grep:27-28`) and no
`specs/` folder (it is created by `/speckit-specify` at run time, `grep:38-42`). `.specify/feature.json`, which the pack and the guide
name, is not generated either: `/speckit-specify` writes it and the generated `.specify/.gitignore` ignores it (`inv:59`, `inv:96`, `inv:132`).
Generated files are exempt from the ASCII and em dash rules of this task: 17 of the 29 files hold non-ASCII text (the nine skills, the three scripts, three templates, the workflow file and `.specify/.gitignore`; the written constitution is ASCII, `claims:24-25`), and
28 of the 29 end with a newline; the exception, kept as generated, is `.specify/workflows/workflow-registry.json` (`claims:20-23`;
the root `.editorconfig` asks editors for a final newline, no check in the repository enforces it).

## Directly written file for Maik's review: the constitution

`.specify/memory/constitution.md` is the only file written by hand. No `/speckit-constitution` run produced it (decision D14).
It follows the structure of the template the init generated (`files:70-75`): the title, `## Core Principles` with five `###` principles,
two further sections, `## Governance` and the closing version line. Each statement is traced to a pack line or, for the
amendment rules, to the generated skill (`trace` has one row per fragment, 40 rows; the script checks that the fragment is in the
constitution and in the cited source line, and that every statement line has at least one traced fragment, `con:10-12`). Text from the pack and the
product docs is reused as written; the three places where I chose a value are listed under "Choices that are mine".

| Constitution text | Source |
|---|---|
| Title `Agentic Bookstore Constitution` | pack spec 10 line 27 (title of the `CLAUDE.md` block) |
| I. Code follows docs/architecture.md; `ArchitectureTest` MUST stay green and its rules MUST NOT be weakened | spec 10 lines 460 and 461 (pack input principle I) |
| I. The layer rules are enforced by ArchUnit, never by convention alone | `product/architecture-standard.md` line 43 |
| II. Tests MUST come first, per layer; one test per acceptance scenario | spec 10 line 461 (principle II) |
| II. Every new behaviour has a test in the layer where the behaviour lives | spec 10 line 41 (`CLAUDE.md` block, Definition of done) |
| II. Every requirement scenario in a spec maps to at least one test | `product/architecture-standard.md` line 165 |
| III. Money is exact, always `Money` (`BigDecimal`, scale 2, HALF_EVEN), never floating point | spec 10 lines 461, 462 (principle III) and 47 (`CLAUDE.md` Money rule) |
| III. Never use `double` or `float` for money | `product/architecture-standard.md` line 91 |
| IV. Schema changes only through new Flyway migrations in `bookstore/src/main/resources/db/migration` | spec 10 lines 462 (principle IV) and 48 |
| IV. An existing migration is never edited | spec 10 line 49 |
| V. Changes MUST be small and scoped; stay inside the scope of the request | spec 10 line 462 (principle V) and 54 |
| V. `./mvnw verify` MUST pass before every commit; always use the Maven wrapper, never `mvn` | spec 10 lines 463 (principle V) and 34 |
| Technology Constraints: Java 25, Spring Boot 4.1.2, Maven wrapper, modules `frontend`, `bookstore`, `distributor` | `product/tech-stack.md` lines 10, 11, 12 |
| Technology Constraints: PostgreSQL only; no H2, no embedded or in-memory database, tests included | `product/tech-stack.md` line 14; `product/architecture-standard.md` line 163 |
| Technology Constraints: No Lombok and no MapStruct, records and small hand-written mappers | spec 10 line 50; `product/architecture-standard.md` lines 170 and 171 |
| Development Workflow: definition of done is `./mvnw verify` green including `ArchitectureTest`, show the last lines | spec 10 line 39 |
| Development Workflow: never say tests pass without having run them in this session | spec 10 line 40 |
| Development Workflow: small commits with conventional commit messages | spec 10 line 55 |
| Governance: compliance is checked by `ArchitectureTest` (I) and `./mvnw verify` (V) | spec 10 lines 460 and 463 |
| Governance: amended with `/speckit-constitution`; semantic versioning rules MAJOR, MINOR, PATCH | spec 10 line 459; the generated skill `speckit-constitution/SKILL.md` lines 103 to 105 and the description on line 3 |

Checks: no `[A-Z_]+` placeholder token (`con:10-12`; red first on the template: 38 findings, `con:3-9`); each principle has its key terms
(I `docs/architecture.md`, `ArchitectureTest`, "weakened"; II "first", "per layer", "one test per acceptance scenario"; III `Money`,
`BigDecimal`, "floating point"; IV "Flyway", "new Flyway migrations"; V `./mvnw verify`, "before every commit"); ASCII and no em dash
(`con:39`); no forbidden thing is allowed (Lombok, MapStruct, `double`, `float`, H2, embedded or in-memory database, `--no-verify`, a PowerShell
name: each may appear only on a line that forbids it; mutants `con:22-24`). Against `docs/architecture.md` on `level-0-start` and the
architecture standard there is no contradiction: lines 5, 102, 145 to 146, 179 and 224 to 225 of `docs/architecture.md` say the same
(`grep:66-72`); `ArchitectureTest.java` exists on the tree (`bookstore/src/test/java/nl/openvalue/bookstore/`).

Choices that are mine, not pack text (Maik: change freely):

- Version `1.0.0`, `Ratified` and `Last Amended` both `2026-11-10`, the date of this staging. The template requires the version
  line; the skill gives only bump rules for a version that exists (`speckit-constitution/SKILL.md` lines 102 to 105) and the template's own example is `2.1.1` (template line 50), so `1.0.0` for a first version is my choice; the pack names none of the three values.
- The Sync Impact Report comment at the top (plan decision J3: keep what Spec Kit writes). No Spec Kit run wrote it, so I wrote the one the skill
  describes for a first ratification (`none -> 1.0.0`, five principles added, no deferred item). The skill itself calls the report
  "temporary scratch material ... expected to be removed before the amended constitution file is committed" (skill lines 114 to 116). Options for
  Maik: keep it (it explains the file), or strip the eight comment lines (the staged file, the trace and the
  check then need the same edit). The pack says "Commit the generated files as they are", which this file is not in either case.
- The headings `Technology Constraints`, `Development Workflow` and `Governance`: the template has two free sections and a governance
  section, and the pack input has only five principles, so the two free sections carry only statements the pack already makes elsewhere
  (rows above), not new rules. The governance statements have no pack source for an amendment or review procedure: only the skill's versioning rule
  and the two pack checks are used. The unfilled template wants "amendment procedure ... compliance review expectations" (skill line 112); this file gives the
  skill's versioning rule and names the two checks, nothing more.

## F5 and pack step 3: level-3 files across the init

Before the init the sandbox is the clean level-3 tree. After the init, the sha256 list of all 446 tracked files is equal to the list before
(`f5:3-4`), including `CLAUDE.md`, `.claude/settings.json` (the Level 3 hooks and permissions and the Level 2 plugin declaration), both
hooks, `.claude/commands/ship.md`, `.claude/agents/architecture-reviewer.md`, the `new-use-case` skill, `.githooks/pre-commit`,
`.mcp.json` and the three rules (`f5:6-31`). On the final trees the 29 rows of the Spec Kit commit are all adds (`claims:9-10`; control: the level-3 commit
has 22 rows that are not adds) and the blobs of ten of these files are the same at `HEAD~1` and `HEAD` (`f5:35-44`). I did not write or edit any settings file (HR2); the settings file was read with Read only.
The init changed no tracked file, so there is nothing to stop and ask about.

## User-level paths (FF-T10-c)

Where the pack says `uv tool install specify-cli==1.3.1`, the staging ran `uvx --from specify-cli==1.3.1 specify init ...` instead (plan decision J3), so nothing
was installed. Both routes were run in isolated home folders; nothing was installed on the real user account.

| Route | What it writes (isolated HOME, `paths:`) | See it | Undo |
|---|---|---|---|
| `uvx --from specify-cli==1.3.1 specify init ...` (what this task ran) | only the uv cache: 5970 paths under the uv cache folder (`archive-v0` 5698, `wheels-v7` 212, `simple-v27` 44, ...), plus `.gitignore` and `.lock` in the uv tool folder; no executable, no tool environment (`paths:3-29`). This holds when a Python 3.11+ is on PATH (the `python3` of that run is 3.13; `product/tech-stack.md` line 71 names Python 3.11+); with none, uv first downloads a managed CPython 3.11.18 (25.5 MiB, 2400 paths, 75372 KiB on disk) into `$XDG_DATA_HOME/uv/python` (default `~/.local/share/uv/python`), on top of the cache (`uvm:8`, `uvm:23`, `uvm:30-34`) | `uv cache dir`, `uv tool dir` (`paths:36-44`); `uv python dir` and `uv python list --only-installed` for the managed Python (`uvm:43-47`) | `uv cache clean specify-cli` removes the package entries (398 files in the dry run, `paths:77-80`); `uv cache clean` removes the whole cache; `uv python uninstall <version>` removes a managed Python (`uvm:48-54`) |
| `uv tool install specify-cli==1.3.1` (the pack's route) | the tool environment `~/.local/share/uv/tools/specify-cli/` (2599 paths) and its `.gitignore` and `.lock`, the executable `~/.local/bin/specify`, and the uv cache (2791 paths); uv warns when `~/.local/bin` is not on `PATH` (`paths:45-67`); uv resolves its default folders from `HOME` and `XDG_*` (`paths:36-44`) | `uv tool list` prints `specify-cli v1.3.1` and `specify`; `specify --version` prints `specify 1.3.1` (`paths:68-73`) | `uv tool uninstall specify-cli` removes the executable and the tool environment (0 paths left, `paths:74-76`); the cache stays until `uv cache clean specify-cli` (`paths:77-80`) |
| What this machine's real home holds before any row (read only check, no uv call against it) | `~/.local/bin/specify` and `~/.local/share/uv/tools/specify-cli` do not exist; `~/.cache/uv` exists, so a `uvx` run adds to an existing cache (`paths:81-84`) | | |

The rows below that run no `uvx` or `uv` command (RR-S1 to RR-S3) write no uv path: the branch carries the generated files, and the skills
call only `.specify/scripts/bash/*.sh`, never the `specify` program (`grep:36-37`). RR-S4 is the one row that runs `uvx`; it lists these paths and an isolated-HOME recipe.
No step in this record installs a Claude Code plugin or adds a marketplace. The branch carries the Level 2 plugin declaration (`extraKnownMarketplaces`,
`enabledPlugins`) in its settings file; if Claude Code offers to add that marketplace or install the plugin when a row starts a session,
that needs Maik's explicit OK, and declining it does not affect rows RR-S1 to RR-S3 (row R2-5 covers the plugin).

## Contradiction review of the generated files

Search: `lombok|mapstruct|double|float|--no-verify|git push|ddl-auto|powershell|pwsh|\.ps1`, case-insensitive, over the nine skills and `.specify`
(`grep:3-17`); the positive control hits `CLAUDE.md` of the same tree (`grep:18-20`). Product finding, not an edit of a generated file.

| Hit or search | Class |
|---|---|
| Seven lines "double-quote" shell-quoting advice in the skills `analyze` 79, `checklist` 92, `clarify` 71, `converge` 108 and in the lines of `implement` 60, `plan` 60 and `tasks` 60 that carry the same sentence (`grep:4-10`); `common.sh` line 378 (`grep:11`) | unrelated (shell quoting), not a Money rule |
| `common.sh` line 927 and `create-new-feature.sh` line 149 name "PowerShell" in a comment about twin implementations (`grep:12-13`) | comment only; no PowerShell file exists (`grep:22`, `claims:7-8`), decision D16 holds with `--script sh` |
| `lombok`, `mapstruct`, `--no-verify`, `git push`, `ddl-auto` | no hit in the generated files |
| `git` commands that write (`commit`, `checkout`, `switch`, `branch`, `push`, `add`, `reset`, `stash`, `clean`, `init`, `rm`) in `.specify/scripts` | none (`grep:23`; control: the word `git` appears in two comments, `grep:24-26`); the git extension that creates branches is not installed (`grep:27`), so the Level 3 git rules are not touched |
| `.specify/templates/tasks-template.md` line 12: "Tests are OPTIONAL - only include them if explicitly requested in the feature specification" (and lines 83, 109, 131 "OPTIONAL - only if tests requested") | contradicts `CLAUDE.md` line "Every new behaviour has a test in the layer where the behaviour lives" and constitution principle II. Product finding PF-T12-3: the skills `plan` (line 62), `tasks` (line 65) and `implement` (line 102) load the constitution, `analyze` rates a violated MUST as CRITICAL (lines 66 and 163; `grep:94-101`), but the guide's short path skips `/speckit-analyze` and the template text stays as generated; rehearsal row RR-S3 observes what `tasks.md` really contains |
| `tasks-template.md` line 250 "Commit after each task or logical group" | consistent with "Small commits"; with the Level 3 hook every `git commit` first runs `./mvnw verify` (about 30 s, `row:69-71`), so a commit per task costs time in RR-S3 |
| The skills run `.specify/scripts/bash/*.sh` (nine call lines, `grep:85-93`); the Level 3 allow list has `Bash(./mvnw *)` and four `git` entries only (`grep:74-83`) | Product finding PF-T12-5: expect permission prompts (not denials); no allow rule is added because the pack text is verbatim (decision J3) |
| `/speckit-specify` writes under a new `specs/` folder at the repository root; `specify` skill lines 84, 88, 93 | no collision: the built tree has no `specs/` folder (`grep:43-64`) |
| `plan-template.md` line 27 names `pytest` as an example | placeholder text of the template, not a rule |
| `speckit-implement/SKILL.md` line 158: "Follow TDD approach: Execute test tasks before their corresponding implementation tasks" | consistent with principle II |

## Consistency check of tokens and spellings (C0-13b, C0-13c)

`token-inventory.py.txt` scans pack spec 10, spec 11, the framework guide, the workshop plan, the briefs, the tech stack and 11 guide pages at each of the two refs
(28 documents, `inv:10`) for every skill name, the `specify-cli` version, every flag of the `specify init` command lines, the `.specify/` paths and the branch names,
and resolves each on the built tree. Result: 122 rows consistent, 8 dotted notation (`speckit.git.commit` style, the guide's own gotcha), 3 run-time paths
(`.specify/feature.json`), 4 unpinned `specify-cli` mentions, 0 mismatch (`inv:148-149`). The nine skills on the tree are exactly the nine names the pack, the framework guide
and the guide pages use (`inv:9`); `--no-model-invocation` is the integration option recorded in `.specify/integration.json` (`files:23-25`).
Negative controls: a misspelled skill name, version, init flag, integration option, path and branch name planted in an in-memory copy of one pack document are each
reported (`inv:152-157`). Cross-document: the pack, the framework guide and the guide pages agree on every Spec Kit token; the differences are the four unpinned
mentions and the Windows flag, below.

Product findings for Maik (pack and guide text; none is a rework target):

- PF-T12-1: `workshop/sdd-framework-guide.md` line 150 and `guide/docs/06-wrap-up.md` line 101 give `uv tool install specify-cli` without the pin `==1.3.1`
  that the pack and `tech-stack.md` line 71 use; line 7 of the framework guide names the version `1.3.1` only in prose, without a pin. An unpinned install gets whatever version is current,
  and its init output can differ from this branch. `06-wrap-up.md` line 107 also omits `--integration-options="--no-model-invocation"`, so a participant at home gets skills that Claude may select automatically.
- PF-T12-2: `workshop/sdd-framework-guide.md` line 150 and `guide/docs/06-wrap-up.md` line 110 say "Windows: `--script ps`"; decision D16 dropped PowerShell, and the init was run with `--script sh` only.
- PF-T12-3: the `tasks-template.md` wording about optional tests against principle II and `CLAUDE.md`, and the fact that only `/speckit-analyze`, which the Track B short path skips, rates a violated constitution MUST as CRITICAL (table above).
- PF-T12-4: pack Tasks step 6 (spec 10 line 502) still offers an optional Level 4 solution branch; amendment A10 dropped it. The pack's instruction in the Level 4 intro (lines 432 to 433) to "review them for anything that contradicts `CLAUDE.md`" is carried out in the table above.
- PF-T12-5: permission prompts for the Spec Kit scripts (table above); also `/ship` step 2 asks about "every changed file belongs to: $ARGUMENTS", and Spec Kit creates files under `specs/` and `.specify/` that a short argument such as `add book reviews` does not name: RR-S3 records what `/ship` asks.
- PF-T12-6: the Sync Impact Report, version, dates and governance wording of the constitution are my choices (section above), not pack text.
- PF-T12-7: `.specify/workflows/workflow-registry.json` has no final newline as generated, while the root `.editorconfig` requires one; kept as generated.

## Scenario statuses (one status each)

| Pack scenario or step | Status | Evidence |
|---|---|---|
| Level 4 intro: "solution/level-3 plus one framework installed, initialised and committed" | consistency-checked with evidence | `build:63-65`, `build:147-156`, `claims:9-10` |
| "Commit the generated files as they are" | zero-token run with evidence (29 files staged as generated, one file written) | `regen:4-12`, `con:36-38` |
| Step 1: `uv tool install` and `specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation"` | zero-token run with evidence (the install route replaced by `uvx --from`, plan decision J3; the pack's route also run in an isolated home) | `init:4-7`, `paths:3-84`, `inv:148-149` |
| Step 2: `/speckit-constitution` with the five principles, review, commit | constitution written directly and consistency-checked (decision D14); the Claude run is a rehearsal row | `con:10-35`, row RR-S1 |
| Step 3: diff `.claude/settings.json`, Level 3 hooks, permissions and plugin declaration intact | zero-token run with evidence | `f5:3-4`, `f5:36`, `claims:9-10` |
| `./mvnw clean verify` on the branch | zero-token run with evidence | `ver:4-15`, `row:69-71` |
| "Rehearsed in time" (Spec Kit) | rehearsal row for Maik (RR-S3); unproven until he rehearses | |
| The skills stay out of Claude's automatic skill listing (pack step 1 note) | consistency-checked with evidence for the flag; the listing behaviour is a rehearsal row (RR-S2) | `files:35-51`, `docs:7-10` |

## Draft rehearsal rows (for Maik, 2026-11-10)

No row claims a Claude result: every expected observation is what the pack, the guide, the generated skills or the docs say should
happen, and each says which. Text that is not pack text is marked "added here". The setup, reset and judging commands of every row were run in scratch clones
outside Claude (`row:3-124`); the clones came from a scratch repository that really has the branches named `start/level-4-speckit` and `solution/level-3`, so
`git clone --branch` and `git status -sb` print the real names (`row:3-7`, `row:17`). The sha in that dry run is the scratch repository's, not the real
branch's: write down your own `<start>`.

Common preconditions for every row:

- The branches are cut and published by the procedure in `README.md` ("Cut and verify procedure"); `start/level-4-speckit` exists. Check:
  `git ls-tree -r --name-only start/level-4-speckit -- .specify .claude/skills | wc -l` prints `31` (`row:162-163`; the 9 Spec Kit skills, the 2 files of `new-use-case`, and the 20 files below `.specify`).
- Docker is running (`docker info`), Java 25 (`java -version`), Claude Code logged in (`claude`, then `/status`). Added here: record `claude --version`.
- Clone recipe for every row (added here, FF-T3-a and FF-T3-b): `git clone --branch start/level-4-speckit <path or URL of the repository> <new empty folder>`, in it
  `git rev-parse HEAD` written down as `<start>`, `git status -sb` prints `## start/level-4-speckit...origin/start/level-4-speckit` and `git status --short` prints nothing (`row:16-20`).
  A new clone folder per fresh session starts with clean Claude Code auto memory (it lives outside the repository and is keyed by the clone path; a `git reset` in the
  same folder does not touch it); reuse a folder only after a `/memory` check shows no entry from an earlier run. Use a separate clone, not `git worktree add`.
- Undo after every row that changes the tree, and judge against `<start>` with commits included: `git reset --hard <start> && git clean -fdx`, then
  `git log --oneline <start>..HEAD`, `git diff --stat <start>` and `git status --short`. Why `-fdx`: `target/`, `frontend/node/` and `frontend/node_modules/` are git-ignored, so a plain
  `git restore .` leaves stale build output (`row:84-97` shows the clean removing five build folders and none left). Verify results always come from `./mvnw clean verify`
  (a fresh clone is green: `BUILD SUCCESS`, 1428 and 190 tests, about 30 s, `row:69-71`).
- When the skills and the permissions load (FF-T6-a, sourced): the nine skills sit in `.claude/skills/` and exist before the session starts; the docs say Claude Code
  "watches skill directories for file changes" and that adding or editing a skill under the project `.claude/skills/` is picked up in the running session (`docs:5`, quote K@318), so no restart is needed after a skill
  change; a skill with `disable-model-invocation: true` has its "Description not in context, full skill loads when invoked" (`docs:8`, K@587). Branch if a row
  sees no skill: run `/skills` (it lists `new-use-case` and the nine `speckit-` names), and check that you are in a clone of `start/level-4-speckit`; if a name is missing, record it and stop that row.
  The project's allow rules of `.claude/settings.json` apply only after you accept the workspace trust dialog (`evidence/level-3/t10/claude-docs-quotes.txt`, quote P6, cited by row R3-1 of the checklist).
- Do not edit `.claude/settings.json`, a hook or a skill during a row.

### RR-S1 `/speckit-constitution` with the pack input, compared with the staged constitution (pack step 2)

| Field | Content |
|---|---|
| Branch | `start/level-4-speckit` |
| Setup (added here, the pack gives only the command) | Clone recipe above. So that the skill starts from the template and not from the staged file (the skill loads an existing `.specify/memory/constitution.md` "as the source of current project-specific values", skill outline step 1, and would amend it instead of writing it): `git show <start>:.specify/templates/constitution-template.md > .specify/memory/constitution.md`; `git status --short` prints ` M .specify/memory/constitution.md` (`row:22-25`). The file now equals the template that the init writes (`cmp` equal, `regen:30-31`, `row:26`). Start `claude`, accept the trust dialog. |
| Prompt (the pack's input, literal) | `/speckit-constitution I. Onion layers: code follows docs/architecture.md; ArchitectureTest must stay green and its rules are never weakened. II. Tests first, per layer, one test per acceptance scenario. III. Money is exact (Money, BigDecimal), never floating point. IV. Schema changes only through new Flyway migrations. V. Small, scoped changes that pass ./mvnw verify before every commit.` (typed by you: the skill has `disable-model-invocation: true`, so Claude does not run it on its own) |
| Expected (pack and skill) | Pack: "Run `/speckit-constitution` with this input, review, commit `.specify/memory/constitution.md`". From the generated skill (derived, not observed): the skill first runs `.specify/scripts/bash/resolve-template.sh constitution-template --json` (skill line 85; the same call run without Claude exits 0 and prints the template as `TEMPLATE_CONTENT`, `res:3-8`), then writes the file with the five principles, replaces every `[ALL_CAPS]` placeholder, sets a version and dates, adds a Sync Impact Report comment at the top, writes only `.specify/memory/constitution.md` ("Write only `.specify/memory/constitution.md`"), and ends with a summary that includes a suggested commit message. The script call is not in the Level 3 allow list, so a permission prompt is expected (PF-T12-5); answer Yes. The Level 3 Stop hook runs at the end of the turn and exits 0 when no Java file changed (`.claude/hooks/architecture-check-on-stop.sh`, first lines). |
| Judge outside Claude | `git diff --name-only <start>` prints only `.specify/memory/constitution.md` (`row:28-36`); `git ls-files --others --exclude-standard` prints nothing (`row:178-179`); `grep -nE '\[[A-Z][A-Z0-9_]*\]' .specify/memory/constitution.md` prints nothing on the staged file and 18 lines on the restored template (`row:164-170`); each of the five principles is present with its key terms: I `docs/architecture.md` and `ArchitectureTest` and "weakened"; II "per layer" and "one test per acceptance scenario"; III `Money`, `BigDecimal`, "floating point"; IV "Flyway" and "new" migrations; V `./mvnw verify` and "before every commit". Compare with the staged file: `git diff <start> -- .specify/memory/constitution.md` (the working file against the staged one, `row:171-177`) or an editor diff. Expected differences: the staged file adds three sections (technology constraints, workflow, governance) from pack sources, the new file has whatever the skill chose; the staged text is the comparison, not the oracle. |
| Failure | (a) A bracket placeholder is left in the file. (b) A principle is missing or its meaning changed (for example money allowed as `double`, a migration edit allowed, tests after code). (c) A file other than `.specify/memory/constitution.md` changed (the skill's scope guard says it must not). (d) The skill is not found: `/skills` does not list `speckit-constitution` (wrong folder). (e) The new constitution contradicts `CLAUDE.md` (Lombok, MapStruct, `double`, an embedded database, an edited migration). |
| Recording rule (added here) | Date, branch and `<start>`, `claude --version`, the permission prompts you saw (quote), the version and dates the skill chose, whether the Sync Impact Report comment is present, the output of the judging commands, and a decision: keep the staged file, take the new one, or merge. Pack fallback: none stated; the staged constitution is the fallback. Undo: `git reset --hard <start> && git clean -fdx`; `.specify/memory/constitution.md` then has sha256 `e574f935323166bc1b89ec11e85e1b62c3258dbade901a1b9d613e2ecee279d6` (`row:46`). |

### RR-S2 The skills are invoked explicitly (pack step 1 note, added here)

| Field | Content |
|---|---|
| Branch | `start/level-4-speckit` |
| Setup | Clone recipe above (a new folder, `<start>` recorded). `ls .claude/skills` prints ten folders: `new-use-case` and the nine `speckit-` ones; the nine carry `disable-model-invocation: true`, `new-use-case` does not (`row:47-62`). Start `claude`, accept the trust dialog. |
| Step 1: Claude does not pick them on its own | Prompt, first in the new session (before `/skills` has run, so that the answer cannot come from a listing you just printed): `Which skills can you use without me typing a slash command? Name each one.` Expected, from the docs (K@587: "Description not in context, full skill loads when invoked"; K@903: "removes the skill from Claude's context entirely"; `docs:8-9`): `new-use-case` is named and none of the nine `speckit-` skills. The docs say the listing "always contains every skill name" for skills that are in it (`docs:10`), so a skill with the flag is outside it. |
| Step 2: list | After step 1: `/skills`. Expected (derived): the nine `speckit-` skills are listed as user-invocable with the others. Type `/speckit-` and read the completion list; expected: nine names (`analyze`, `checklist`, `clarify`, `constitution`, `converge`, `implement`, `plan`, `specify`, `tasks`). |
| Step 3: Claude refuses to start one itself | Prompt: `Run the speckit-specify skill for me.` Expected (docs, K@580): "Claude Code blocks the call and instructs it not to reproduce the deploy steps another way, so expect Claude to suggest running `/deploy` yourself" (the docs' example uses `/deploy`; here the name is `/speckit-specify`). Press Esc if Claude starts asking the questions of the skill: that already counts as failure (b). |
| Failure | (a) `/skills` does not list a `speckit-` skill. (b) Claude names a `speckit-` skill in step 1 or runs one in step 3 without you typing the slash command: the flag did not apply (record `claude --version` and the quoted answer). (c) `new-use-case` is missing from the answer of step 1 (an unrelated Level 3 failure; see row R3-4). |
| Recording rule (added here) | Date, `<start>`, `claude --version`, the lists you saw (quote), the answers to steps 1 and 3 (quote), pass, fail or note. Undo: delete the clone folder. Pack fallback: none stated. |

### RR-S3 "Rehearsed in time" for Spec Kit (pack acceptance scenario)

Pack scenario, word by word: **GIVEN** "a fresh clone on the track's start branch", **WHEN** "a facilitator who has not used the framework before ships Level 4 brief A following
`workshop/sdd-framework-guide.md`", **THEN** "the feature is committed through `/ship` with verify green within 35 minutes (record the time per track)".

| Field | Content |
|---|---|
| Branch | `start/level-4-speckit` (GIVEN) |
| Setup | Clone recipe above (a new folder, `<start>` recorded, `git status --short` prints nothing). The facilitator has not used Spec Kit before. Brief A is "Reviews and ratings" in `workshop/level-4-feature-briefs.md` (the guide's own text at `guide/docs/05-level-4-spec-driven-development.mdx`). Start `claude` in the clone root and accept the trust dialog (the footer shows the permission mode; write it down). |
| Steps (WHEN), the framework guide's Track B (lines 117 to 124) | 1. `/speckit-specify` with the brief and your decisions (what and why, not the tech stack). 2. `/speckit-clarify`, answer up to five questions. 3. `/speckit-plan` with the text `Use the existing onion layers, see docs/architecture.md and the constitution.` 4. `/speckit-tasks`. 5. `/speckit-implement`, then `/speckit-converge` once. 6. `/ship add book reviews`. The guide says to skip `/speckit-checklist` and `/speckit-analyze` today. |
| Timer (added here, FF-T6-c) | Start event: you press Enter on step 1 (the first `/speckit-specify`); write down the clock time `t0`. Also write down the time at which you opened the guide, so that reading time is visible and can be added. Stop event: `/ship` prints the commit hash and, outside Claude, `./mvnw clean verify` in the clone ends `BUILD SUCCESS`; clock time `t1`. Report `t1 - t0` (and `t1` minus the guide time). The pack's 35 minutes is judged against `t1 - t0`. If `/speckit-implement` or `/ship` has not finished at 35:00: write down which command is running or is the last one finished, whether the tests are green so far, and the time at which it finishes; that is "started but not ready" and counts as a miss, with the finishing time recorded. If a run stops on an infrastructure error (429, login, Docker), quote the message and repeat it in a new clone folder; a run that finished with a wrong result is not replaced. The time judged is that of the first run that reached `/ship`; report every run. |
| Expected (pack, guide, skills) | Pack: "the feature is committed through `/ship` with verify green within 35 minutes". Derived from the generated skills and the staged files, not observed: `/speckit-specify` creates a folder under `specs/` (skill line 88) and writes `.specify/feature.json` (skill line 101; the file is git-ignored by `.specify/.gitignore`); the skills run `.specify/scripts/bash/*.sh`, which prompt for permission (PF-T12-5; count the prompts and write them down, each costs time); `tasks.md` is produced from a template that calls tests optional (PF-T12-3), so look at whether it has test tasks for each acceptance scenario, as principle II and `CLAUDE.md` require. `/ship add book reviews` runs `git status --short` and `git diff --stat HEAD` first (the command file's two `!` lines), then `./mvnw verify` and shows the summary lines; its step 2 checks that every changed file belongs to "add book reviews" and asks before including anything outside; step 3 commits with a conventional message and the commit hook builds once more (about 30 s, `row:69-71`); step 4 reports the hash. Guide: `/speckit-converge` "once" and the Track B gotcha that the commands use a dash, not a dot. |
| Judge outside Claude | `git log --oneline <start>..HEAD` has at least one line, the last made by `/ship` with a conventional subject (`row:74-83` shows the command on a stand-in commit); `git show --stat HEAD` lists the files of the commit; `git status --short` prints nothing or only files you chose to leave out; `./mvnw clean verify` ends `BUILD SUCCESS` with the `ArchitectureTest` green (`ver:11`, `ver:15` give the numbers on the untouched branch). |
| Failure | (a) The commit was not made by `/ship` (a plain `git commit` typed by you or by Claude outside the command). (b) `./mvnw clean verify` is red after the commit. (c) More than 35 minutes by the timer above. (d) `/ship` asks to include `specs/` or `.specify/` files and you answer differently from what the guide implies: write the question and your answer down (PF-T12-5), it is a finding, not a failure of the branch. (e) The commit hook blocks the commit: quote the first stderr line (the row R3-1 failures apply). |
| Recording rule (added here) | Date, `<start>`, `claude --version`, the permission mode, the number of permission prompts, `t0`, `t1`, `t1 - t0`, which command was running at 35:00 if the time was missed, the final hash and subject, the verify summary lines, whether `tasks.md` had test tasks, pass, fail or note. The time is the per-track record the pack asks for. Pack fallback: none stated. Undo after the row: `git reset --hard <start> && git clean -fdx` (or delete the clone); judge a repeated run against `<start>` with commits included. |

### RR-S4 Regenerate the init (optional zero-token check, added here; FF-T10-c)

Not a Claude row and not a pack scenario: it lets Maik see that the staged files are what the pinned tool generates. It runs `uvx`, so it writes user-level paths.

| Field | Content |
|---|---|
| Branch | `solution/level-3` (a clone made like the others with `--branch solution/level-3`; `git status --short` prints nothing, `row:99-101`) |
| Command | `HOME=<scratch>/home XDG_CONFIG_HOME=<scratch>/home/xdg-config XDG_CACHE_HOME=<scratch>/home/xdg-cache XDG_DATA_HOME=<scratch>/home/xdg-data XDG_STATE_HOME=<scratch>/home/xdg-state UV_CACHE_DIR=<scratch>/home/uv-cache UV_TOOL_DIR=<scratch>/home/uv-tools uvx --from specify-cli==1.3.1 specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation" < /dev/null`. With these variables set, every user-level write goes below `<scratch>/home` (the folders may need `mkdir -p` first). Without them, the run writes into the real uv cache (see the table "User-level paths": the `uvx` row, 5970 paths in the dry run) plus the `.gitignore` and `.lock` of the uv tool folder. This list assumes a Python 3.11+ on PATH (`product/tech-stack.md` line 71); with none, uv first downloads CPython 3.11.18 (25.5 MiB, 2400 paths) into `<scratch>/home/xdg-data/uv/python`, see it with `uv python list --only-installed` and remove it with `uv python uninstall <version>` (`uvm:8`, `uvm:23`, `uvm:43-54`). The init also needs a `claude` program on PATH or the option `--ignore-agent-tools`: without either it exits 1 in the dry run and writes no file (`uvm:55-63`), with the option it exits 0 and writes the same 29 files (`uvm:64-66`); the documented fallback to `~/.claude/local/claude` (`uvm:40-41`) sits under HOME, which the isolated HOME hides. |
| Expected | Exit 0; `git status --short --untracked-files=all` prints 29 `??` lines (plain `git status --short` folds them into 10 lines: the nine `.claude/skills/speckit-*/` folders and `.specify/`) and `git diff --stat` prints nothing (`row:130-145`; the first dry run counted with `--untracked-files=all` and said "git status --short", corrected at `row:158`). Against the branch: every file equal to the same path on `start/level-4-speckit` except `.specify/integrations/claude.manifest.json`, `.specify/integrations/speckit.manifest.json`, `.specify/workflows/workflow-registry.json` (timestamps only) and `.specify/memory/constitution.md` (the template here, the written file on the branch) (`row:105-109`). Run it in a clone where `git status --short` prints nothing first (`row:99-101`). |
| Undo | `git clean -fdx` in the clone; delete `<scratch>/home`; or `uv cache clean specify-cli` for the real cache (`paths:77-80`); if the run downloaded a Python, `uv python uninstall <version>` (`uvm:48-54`). |

## Claims with counterexample searches (FF-T7-b)

Every universal or descriptive claim made in this record about the generated files has a search and a control in `claims`: all nine skills carry the flag (`claims:3-6`); no
PowerShell file exists (`claims:7-8`); the Spec Kit commit changes no existing file (`claims:9-10`); only the six scripts are executable (`claims:11-19`); 28 of 29 files end
with a newline and none has a carriage return, the exception being the registry file (`claims:20-23`); the init needs the network only for the package (`claims:26-27`). The one counterexample
(the missing final newline) is stated above, not hidden. Claims about the pack and the guide (spellings) are covered by `inv`.

## Scope, hygiene and gate

- Files of this task only: `S/level-4-speckit/**`, this record and `evidence/level-4-speckit/**`; no tracked file changed against the spec tip except this task's agent memory (the commit carries it), no path of another track and not
  `rehearsal-checklist.md` changed (`hyg:2-11`).
- Branch names: only `start/level-4-speckit` as `step_branch` prints it (`hyg:27-28`, `row:8-11`); the search for the names of a Level 4 done or solution branch over the track's three kinds of file
  prints nothing, with a positive control on a copy (`hyg:12-14`).
- Text hygiene of this record and the evidence: no leak of a home or scratchpad path or a token, with a positive control on the raw run files (`hyg:15-17`); ASCII and no em dash, with a positive control
  (`hyg:18-21`); only `.txt` evidence (`hyg:22`); no Claude invocation, no stream file, and every hit of a search for run or pass wording is a zero-token run or a recording instruction for Maik (`hyg:23-26`, `hyg:34-42`). Generated files are exempt from the ASCII rule and listed above.
- Staging: every file under `files/` is named by one manifest row and the reverse, no dotted staged component, no `CLAUDE.md` or `AGENTS.md`, 23 files mode 644 and 6 mode 755 (`hyg:29-33`).
- Gate: the staging self-test `PASSED all 58 cases` (`st:6-8`); the referee `./mvnw clean verify` in the track worktree: `BUILD SUCCESS`, 1314 bookstore tests and 190 distributor tests, 0 failures (`ref:3-11`; the bookstore count is lower than the 1428 of the Level 3 sandbox because the track worktree holds no Level 3 exercise code).
- Sandboxes kept under the scratchpad until the gate passes: `s10-t12-impl-sbx-1`, `-sbx-2`, `-sbx-3`, the clones in `s10-t12-impl-rows`, and the isolated homes `s10-t12-impl-home`, `-home-install`, `-home-probe`, `-home-regen`.
- Boy-scout fixes: none.

## Rework r1 (qa-lead META REJECT r0: RA1 to RA3, O1, O2; closed and doc-only)

- RA1: RR-S4 "Expected" now names `git status --short --untracked-files=all` (29 `??` lines) and the folded plain form (10 lines); `row:127-145` is a fresh dry run
  of both commands, `row:158` the correction for `row:103`. FF-T12-a: the judging commands of RR-S1 were run exactly as the row prints them (`row:160-179`).
- RA2: the "uv cache only" statements (verdict, table "User-level paths", RR-S4) now carry the Python 3.11+ assumption, the managed CPython download, how to see
  it and how to remove it, and RR-S4 names the `.gitignore` and `.lock` of the uv tool folder; evidence `uvm` (new file, an isolated zero-token `uvx` run with no Python 3.11+ on PATH and a `claude` stub that only logs).
- RA3: version `1.0.0` is stated as my choice. O1: the `claude` program or `--ignore-agent-tools` requirement (`uvm:55-66`) and the disclosure that the first runs had the real `claude` program on PATH. O2: RR-S2 asks the listing question first.
- Evidence changes: `row-dry-run.txt` appended (its first 124 lines are the r0 bytes), `uv-managed-python.txt` new, `hygiene.txt` appended. Everything else regenerated by the same scripts
  is byte-equal to the r0 hash list, except `token-inventory.txt`: `origin/factory` moved after r0, so the inventory was re-run against a scratch repository whose `origin/factory` is pinned to the r0 sha;
  the file has the same 158 lines, the same counts and the same cited lines, but its bytes are not equal to the r0 hash and the cause of the byte difference was not found (the r0 file was not kept).
- The 29 staged files and `manifest.tsv` are unchanged (`hyg`, section r1-7); the tree stays `c9d758b95c060a9cb24a8c1c7260920fbcea5cef`.

