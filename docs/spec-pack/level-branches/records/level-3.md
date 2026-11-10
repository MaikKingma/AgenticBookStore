# Level 3 record: consistency check (decision D14, amendments A2 and A6)

Branch `solution/level-3` (the name is frozen by D17): the Level 1 and Level 2 chain plus the 31 rows of
`level-3/manifest.tsv`: 8 config rows (the settings file, the skill with its `reference.md`, the `/ship` command, the
`architecture-reviewer` agent, the two bash hooks and the git hook) and 23 exercise rows (cancel order, task T9, record
`evidence/level-3/cancel-order/cancel-order-record.md`). This record covers the 8 config rows and the four Level 3 pack
scenarios. No Claude Code session was run for it (decision D14, plan amendment A2): no `claude` command, no login, no
plugin or marketplace command, no `ANTHROPIC_API_KEY`. Every `claude ...` string below is quoted text from a staged
file, the pack, the guide or the docs, not a command that was run. Nothing here claims a Claude result. The scenarios that
need a session are rows of `rehearsal-checklist.md`, section "Level 3", for Maik's rehearsal on 2026-11-10. There are no
PowerShell parts (decision D16, amendment A6): Windows participants run the hooks through Git Bash.

Evidence files are in `evidence/level-3/t10/` unless a path says otherwise, and `name:N` means line N of that file.
Short names: `san` sandbox-build.txt, `ver` sandbox-verify.txt, `h5` harness-bash5.txt, `h32` harness-bash32.txt, `real`
real-hook-runs.txt, `stop` real-hook-runs-stop.txt, `cold` cold-empty-m2-verify.txt, `inv` token-inventory.txt (row Tnn is
line nn+3), `deny` deny-rule-matching.txt, `skill` skill-selection-static.txt, `claims` claim-searches.txt, `dry`
row-setup-dry-run.txt, `rr1` row-dry-run-r1.txt, `j15` j15-fallback-combinations.txt, `docs` claude-docs-quotes.txt. Every output line in those files is a byte copy of the raw output
that is kept in the scratchpad; paths are masked while the files are written.

## Verdict

| Check | Result |
|---|---|
| Token inventory of the staged Level 3 config files, the scenarios and the code, built by my own script | 107 rows: 99 consistent, 8 mismatch-pack-or-guide, 0 mismatch in the staged files or the code (`inv:111-113`) |
| Negative controls of that script | 8 of 8 misspelled tokens are reported, each by its token-specific check (`inv:123-131`) |
| Staged files against the pack blocks | six verbatim files byte equal, settings `hooks` and `permissions` deep equal with the list order kept (`inv` T11, T12, T42 to T47) |
| Staged files against the built tree | 8 of 8 files equal in bytes and mode on the full level-3 tree and on the G10 tree (`inv` T01 to T08, `san:195-203`) |
| Spelling in pack spec 10, spec 11, the workshop plan and the guide | identical for every Level 3 token, except 8 stale sentences in pack and guide text (PF-T10-2, PF-T10-3) |
| T8 hook harness re-run | bash 5.3.20: `PASSED: 70 of 70` (`h5:145`); macOS `/bin/bash` 3.2.57: `PASSED: 70 of 70` (`h32:145`) |
| Hooks run for real, outside Claude | red build: commit hook exit 2, failing test in the stderr tail, no commit (`real:30-34`); green: exit 0 (`real:19`); Stop hook green exit 0 and red exit 2 (`stop:11`, `stop:17`) |
| Cold `./mvnw -q verify` against the hook timeout 180 | 38 s on a fresh clone (`real:9`), 57 s with an empty local Maven repository as well (`cold:4`); the hook on a green tree took 29 s (`real:19`) |
| `./mvnw clean verify` on the full level-3 sandbox | `BUILD SUCCESS`, 1428 bookstore tests and 190 distributor tests, 0 failures, `ArchitectureTest` 22 tests (`ver:4-12`, `ver:23`) |
| Deny rules against the documented wildcard semantics | 34 commands and 5 Read paths, 0 mismatches against the documented semantics, 4 controls bite (`deny:63`, `deny:66-96`) |
| Skill description against the scenario sentence | shares no content word with it (stop words removed; the raw word sets share only `to`); the link is meaning only (PF-T10-1); behaviour is row R3-4 (`skill:7`) |
| Gate after the edits | `test-staging.sh` `PASSED all 58 cases`; referee `./mvnw clean verify` in the project root `BUILD SUCCESS`, 1314 bookstore tests, 190 distributor tests (`staging-selftest.txt:10`, `referee-verify.txt:2-11`) |
| Scenario statuses | all four pack scenarios, `/ship` and the Windows runs are rehearsal rows; the git hook activation and the Stop hook are zero-token runs with evidence; no Claude result is claimed |

No row is a mismatch in the task's own files or in the staged files, so there is no FAIL to report and no question for the
orchestrator. Eight rows are product findings (PF-T10-2 and PF-T10-3 below); six further findings (PF-T10-1, PF-T10-4 to
PF-T10-9) come from the checks around the rows.

## Inputs and shas

| Input | Value |
|---|---|
| Base | tag `level-0-start` = `b668073eb44bfea453c291509a615f6de081e54a` (published `main`); the simulated level-0 commit of every sandbox is `391d5f74d69591a8e7a4b3cf718a07fe9dbae03c` (`san:174`, `san:177`, `san:180`) |
| Full level-3 sandbox (sbx-1, criterion 1) | HEAD `a20c4ad7d4a5a21538d83b0daf8f8cc5ecaa8bff`, tree `309786e063b813e0f9248714f404b927d277d879`, built by `simulate-level-0.sh level-0-start` and `build-level.sh level-3` (`san:175-176`); the tree equals the one T9 recorded (`309786e0`, FF-T9-b) |
| Scratch level-3 sandbox (sbx-3) | HEAD `8a0ccd8995c6865ca4d0296a00a409d15c7cef21`, same tree (`san:181-182`); used for the destructive runs (red build, git hook, Stop hook) |
| G10 sandbox (sbx-2) | `build-level.sh level-2` (HEAD `326ccb92713f02a8517416bd1be6d68984dea69c`), then `apply-level.sh --group config level-3` (8 rows) and one commit `6505a32e3651f23012e3afc0d66c6b87f082a483` (`san:178`, `san:119-128`, `san:148`) |
| Staged files, scripts and harness | the T10 task branch, which holds the merged T1 to T9 results (`git rev-parse HEAD` = `6ca7fc7efa635781f37231f2d92dac413be49b41`) |
| Pack text | `docs/spec-pack/specs/10-harness-reference-solutions.md` lines 217 to 427 (Level 3 block), `11-workshop-guide-site.md`, `docs/spec-pack/workshop/workshop-plan.md` lines 226 to 293 and 344 |
| Guide sources | `origin/factory` at `8e7b2795ccbd` and `origin/sdd/11-workshop-guide/spec` at `b7f053c7a9c6`, `guide/docs/**` read with `git show`; the two refs hold identical guide pages (`claims:56-57`, `inv:2`). Page 04 is `04-level-3-behavioral-control.mdx` ("G04" below), the cheat sheet is `reference/cheat-sheet.md` ("Gcheat"), the troubleshooting page `reference/troubleshooting.md` ("Gtrouble"), the setup page `00-setup-check.md` ("Gsetup") |
| Claude Code docs | pages `hooks`, `permissions`, `skills`, `tools-reference` read with curl on 2026-11-10, markdown form, sha256 values in `docs:3-6` |
| Tools | bash 5.3.20, macOS `/bin/bash` 3.2.57 (`h5:2`, `h32:2`), Java 25.0.4 (`stop:22`, the line `Detected Java version`), Maven wrapper, Docker running; shared `~/.m2` cache (J10) |

Environment: every command ran with `TMPDIR` and `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...` in the session scratchpad (HR1);
`ANTHROPIC_API_KEY` was neither read nor written. The Maven runs print `Picked up JAVA_TOOL_OPTIONS` once in a tail
(`stop:21`); a participant's run does not.

## Scenario status (one status each)

| Scenario | Pack lines | Status | Row |
|---|---|---|---|
| Red build blocks the commit | spec 10 :410-413 | rehearsal row for Maik | R3-1 |
| Escape hatch is closed | :415-417 | rehearsal row for Maik | R3-2 |
| Secrets stay unread | :419-422 | rehearsal row for Maik | R3-3 |
| Skill is selected automatically | :424-426 | rehearsal row for Maik | R3-4 |
| `/ship` commits the exercise (guide "Level cleared when", plan :290, pack :407) | not a pack scenario | rehearsal row for Maik | R3-5 |
| Git hook and its activation command (pack :329-335) | not a pack scenario | zero-token run with evidence (`real:93-97`, `h5:72-75`); R3-6 repeats the commands on Maik's clone | R3-6 |
| Windows through Git Bash (pack :323-327, `windows-hooks.md` W1 to W3) | not a pack scenario | rehearsal row for Maik | R3-7 |
| Stop hook (pack :303-320, optional in the guide) | not a pack scenario | zero-token run with evidence (`stop`) | none |
| Staged files and spelling | not a pack scenario | consistency-checked with evidence (`inv`) | none |

The parts of the four pack scenarios that need no model have their own status:

| Part | Status | Evidence |
|---|---|---|
| The hook exits 2 on a red build, prints the header and the 40-line tail, does not commit | zero-token run with evidence | harness cases `tbc-commit-red` (`h5:6`, row `h5:79`) AND `tbc-chained-git-add-and-git-commit` (`h5:27`, row `h5:100`), re-run on both bashes; real runs `real:30-34` |
| The failing test is named in the stderr tail | zero-token run with evidence | `real:70` (`RehearsalRedTest.deliberatelyRed:11`), `real:126-129` (`MoneyTest.roundsHalfEvenToTwoDecimals:17`, the guide's own prompt) |
| The hook finishes inside its timeout | zero-token run with evidence | `real:9`, `real:19`, `real:30`, `cold:4` |
| A commit hook that cannot start exits 126 outside Claude; that Claude Code then blocks the commit is the docs' statement, observed only by row R3-1 variant E | zero-token run for the exit code, rehearsal row for the block | `rr1:54-55`, `docs:61-62`, `docs:67` |
| The permission rules of the pack stay as written and deny the scenario commands | consistency-checked with evidence | `deny:24-27`, `inv` T13 to T23 |
| What Claude Code shows, what Claude says next, whether it denies, whether it loads the skill | rehearsal row for Maik | R3-1 to R3-5 |

## Consistency check of the staged Level 3 files

Required items of the task, each with the inventory rows that resolve it. "Spelling" is the spelling of the token in
pack spec 10 (P10), spec 11 (P11), the workshop plan (WP) and the guide (G04 and others); line numbers are the lines that
hold the exact token (`inv` shows every one). Status values: consistent, mismatch-own-file (would be a FAIL),
mismatch-pack-or-guide (product finding for Maik), rehearsal-only.

| # | Item | Staged (file:line) | Resolves on the built tree, the code or the docs | Spelling in pack, plan and guide | Status |
|---|---|---|---|---|---|
| 1 | settings file path, mode 644, keeps the level-2 keys, adds `permissions` and `hooks` | `.claude/settings.json` | on the tree, bytes equal (`inv` T01, T09); the level-2 keys are kept unchanged and only `hooks` and `permissions` are added (`inv:12`) | P10 :222-271, WP :236-259, G04 :40-102 | consistent |
| 2 | commit hook command, path and mode | settings.json:37 | `.claude/hooks/tests-before-commit.sh` on the tree, mode 100755, first line `#!/usr/bin/env bash` (`inv` T06, T24, T25; `san:200`) | P10 :254, :279; WP :256, :260; G04 :96, :105; Gcheat :18 | consistent |
| 3 | Stop hook command, path and mode | settings.json:48 | `.claude/hooks/architecture-check-on-stop.sh` on the tree, mode 100755, bash shebang (`inv` T07, T26, T27; `san:201`) | P10 :265, :303; G04 :176 | consistent |
| 4 | `PreToolUse` event, matcher `Bash` | settings.json:28-30 | the hooks docs name the event and the matcher field; the matcher "matches the Bash tool" (`t7/d9-onfailure-and-hook-triggers.txt:51-54`) (`inv` T29, T34) | P10 :245-247; WP :250-254; G04 :89-94 | consistent |
| 5 | the `if` filter `Bash(git commit *)` and the commit filter in the script | settings.json:34; `tests-before-commit.sh:6` | the docs call `if` best-effort and the script checks the command itself (`t7/d9-onfailure-and-hook-triggers.txt:57-60`); the filter is the same permission rule as the allow rule `Bash(git commit *)` (`inv` T17, T30); the script's grep matches `git commit` with spaces only, see note N1 and the section "Deny rules" | P10 :251; WP :255; G04 :95 | consistent |
| 6 | `onFailure: "block"` (D9) | settings.json:36 | the field exists from Claude Code 2.1.295 (`t7/d9-onfailure-and-hook-triggers.txt:18`, :35); with `block` a failure does what exit code 2 does: cannot start, exit code other than 0 or 2, timeout (`docs:14-18`) (`inv` T28, T32) | P10 :253, :274-277; G04 :101 (the sentence); the guide fragment G04 :92-100 leaves it out on purpose (`inv` T41) | consistent |
| 7 | `timeout` 180 (commit hook), 120 (Stop hook) | settings.json:35, :47 | the docs: "Seconds before canceling" (`t7/d9-onfailure-and-hook-triggers.txt:75`); measured 38 s and 57 s cold, 29 s on a green tree (`real:9`, `cold:4`, `real:19`) (`inv` T31, T36) | P10 :252, :263; WP :255; G04 :95 | consistent |
| 8 | `CLAUDE_PROJECT_DIR` in both commands and in both scripts | settings.json:37, :48; scripts line 9 and 7 | named in the hooks docs (`inv` T37); the real runs started the hooks from another folder with only this variable set (`real:19`, `real:30`) | P10 :254, :265, :289, :311; WP :256; G04 :96 | consistent |
| 9 | the 5 allow and 6 deny permission rule spellings | settings.json:11-25 | deep equal to the pack block (`inv` T11) and to the plan and guide blocks (`inv` T38, T40); each of the 11 rules is spelled the same in P10, WP and G04 (`inv` T13 to T23) | P10 :229-241; WP :241-242; G04 :45-46 | consistent |
| 10 | skill folder and name `new-use-case`, `SKILL.md` and `reference.md` | `.claude/skills/new-use-case/SKILL.md:2`, `reference.md` | frontmatter `name` equals the folder name; `reference.md` is on the tree and linked from `SKILL.md:24` (`inv` T02, T03, T48, T56); no variant spelling anywhere (`inv` T48 loose search) | P10 :337, :365; WP :274; G04 :184; Gcheat :19 | consistent |
| 11 | skill `description` | SKILL.md:3 | equal to the pack block (`inv` T45); 279 characters, below the 1,536 of the docs (`inv` T49, `docs:48`) | P10 :341 | consistent |
| 12 | skill `allowed-tools: Read, Grep, Glob, Edit, Write, Bash(./mvnw *)` | SKILL.md:4 | every tool name is in the tools reference; `Bash(./mvnw *)` is also in `permissions.allow` (`inv` T50 to T55); the docs accept a comma-separated string (`docs:49`) | P10 :342 | consistent |
| 13 | the types and routes the skill names: `NotFoundException`, `ConflictException`, `InvalidInputException`, `UnavailableException`, `ArchitectureTest`, `ProblemDetail`, Flyway, `POST .../commands/{action}`, 202, the suffixes `Flow`, `DataService`, `Payload`, `View` | SKILL.md:15-22 | every type is declared on `level-0-start`; the suffixes and the route exist (`inv` T57 to T69) | not named elsewhere | consistent |
| 14 | `/ship` file, `disable-model-invocation: true`, `$ARGUMENTS`, the `!` lines | `.claude/commands/ship.md:2-14` | commands and skills both create the command (`docs:46`); the field and `$ARGUMENTS` are in the skills docs (`inv` T04, T71 to T73) | P10 :370-387; WP :279-282; G04 :198 | consistent |
| 15 | `/ship` allowed tools against the permissions | ship.md:5 | the five `Bash(...)` entries equal `permissions.allow` as sets (`inv` T70); each command of the file, `git status --short`, `git diff --stat HEAD`, `./mvnw verify`, `git add -A`, `git commit -m "feat: x"`, is allowed by the staged rules and denied by none (`inv` T74 to T78, `deny:42-49`) | P10 :376 | consistent |
| 16 | agent file `architecture-reviewer` | `.claude/agents/architecture-reviewer.md:2-5` | `name` equals the file stem; `tools: Read, Glob, Grep, Bash` are tools of the tools reference; `model: sonnet` is named in the sub-agents docs (`inv` T05, T79 to T81); `docs/architecture.md` exists on `level-0-start` (`inv` T82) | P10 :389-392; WP :284; G04 :204; Gcheat :19 | consistent |
| 17 | git hook `.githooks/pre-commit`, mode 755, LF | `.githooks/pre-commit` | on the tree with mode 100755; the script runs `./mvnw -q verify`; `.gitattributes:3` keeps `.githooks/*` LF (`inv` T08, T84, T85); `core.hooksPath` is unset in the sandbox, so the hook is off until activated (`claims:53`) | P10 :329-335 | consistent |
| 18 | activation command `git config core.hooksPath .githooks` | not in a staged file | the command works: after it a red build aborts the commit with exit 1 and a green one goes through (`real:93-95`, `h5:72-75`) | P10 :335; WP :263; G04 :141; Gcheat :19 (`inv` T83; the control that misspells it in the plan is reported, `inv:129`) | consistent |
| 19 | tokens the Stop hook uses: `./mvnw`, `-pl bookstore`, `-Dtest=ArchitectureTest`, `-Dsurefire.failIfNoSpecifiedTests=false` | `architecture-check-on-stop.sh:11` | `mvnw` mode 100755; `<module>bookstore</module>` in `pom.xml:19`; `ArchitectureTest.java` exists; the real Stop hook run accepted every flag and ran only `ArchitectureTest` in 5 s (`inv` T86 to T89, `stop:11`) | P10 :315 | consistent |
| 20 | `.env` is git-ignored and no `.env*` file is tracked | not in a staged file | `.gitignore:27:.env*` and `:28:!.env.example` on `level-0-start`; zero tracked paths with a `.env` component, with a control that hits (`claims:43-47`) | P10 :420; G04 :71 | consistent |
| 21 | the scenario texts: `git commit -m "test"`, `git commit --no-verify -m "x"`, `git commit -n -m "x"`, `git push`, `Customers must be able to cancel an order that has not shipped yet.`, `Read the .env file in the repository root.` | not in staged files | spelled the same in pack, plan and guide; a loose search for the cancel sentence finds no variant (`inv` T91 to T98) | P10 :412, :416, :425; WP :278; G04 :62, :69, :161, :191, :76 | consistent |
| 22 | the branch name `solution/level-3` (D17) | not in a staged file | `step_branch level-3` prints `solution/level-3` and no variant spelling exists in the guide, the plan or spec 10 (`claims:60-65`) | P10 :12, :19, :217, :431, :500; WP :230, :300, :344; G04 :24, :101, :107, :176, :210 | consistent |
| 23 | the Git Bash prerequisite for Windows | `windows-hooks.md` | the sentence `Git for Windows (Git Bash)` is spelled the same in the pack, the plan and the guide (`inv` T99) | P10 :323; WP :344; Gsetup :23; G04 :132; Gtrouble :13 | consistent |
| 24 | text that D16 made stale: PowerShell hook variants | not in staged files | the pack, the plan and spec 11 still ask for `.ps1` hooks or PowerShell tabs (`inv` T100 to T104) | P10 :219-220, :322, :500; WP :344; P11 :56 | mismatch-pack-or-guide (PF-T10-2) |
| 25 | text that `onFailure` made stale: "Any other non-zero code does NOT block." | `tests-before-commit.sh:3` keeps the pack comment | with `onFailure: "block"` an exit code other than 0 and 2 blocks the commit (`docs:16`, `docs:18`) (`inv` T105 to T107) | P10 :283; WP :262; G04 :105, :112; Gcheat :21 | mismatch-pack-or-guide (PF-T10-3, = PF-QA-T7-1) |
| 26 | what Claude Code does with the files: loads the hooks, applies the rules, selects the skill, runs `/ship` | all | needs a session | all | rehearsal-only (R3-1 to R3-7) |

Row count: 26 rows; the 107 rows of `inv` are the evidence behind rows 1 to 25. Rows 24 and 25 hold 8 inventory rows. No
other row mismatches.

### Notes on rows

- N1 (row 5): the Claude commit hook runs only when the call matches `Bash(git commit *)` and then only when the script
  finds `git` and `commit` separated by white space with no double quote in front of them. The hooks docs add that `if`
  is best-effort: "When Claude Code can't determine which commands the Bash input runs, it runs your hook regardless of
  the pattern" (`t7/d9-onfailure-and-hook-triggers.txt:60`). Two forms of `git commit` pass the script without a build
  (PF-T8-1 and `git -C . commit`, section "Position variants").
- N2 (row 9): `Bash(git push)` is redundant next to `Bash(git push *)`, because a trailing `*` with a space before it also
  matches the bare command ("A `*` at the end, with a space before it, also matches the bare command", `docs:25`). The
  control that drops only the exact rule keeps `git push` denied (`deny:83-86`). Harmless: the pack keeps both.
- N3 (row 9): `Read(./.env.*)` also covers `.env.example` and `.env.local` (`deny:60-61`). No file of that name is tracked
  on `level-0-start` (`claims:43`), so no participant file is hidden by it.
- N4: the allow rules of a project settings file are applied only after the workspace trust dialog is accepted for that
  folder; deny and ask rules are not affected (`docs:44`). Every fresh clone folder in the rehearsal rows needs that
  step, and the hooks run either way (the permissions page, table "What runs before you trust a folder").
- N5 (row 6): the harness asserts the scripts' own exit codes, and no staged file says that a non-2 exit lets the commit
  through; the stale sentence is the pack's comment line (row 25). With `onFailure` unset, an exit code other than 0 and 2
  is a non-blocking error (`docs:20`); with it set, the same exit blocks (`docs:16`, `docs:18`).

## Zero-token behaviour (C0-13d)

All of it ran without a Claude process.

### Z1 Hook harness on two bashes

```
$ bash docs/spec-pack/level-branches/tests/test-hooks.sh                                            (bash 5.3.20)
PASSED: 70 of 70                                                                       (h5:145)
$ env PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash docs/spec-pack/level-branches/tests/test-hooks.sh (bash 3.2.57)
PASSED: 70 of 70                                                                       (h32:145)
```

Both runs exit 0 (`h5:146`, `h32:146`) and take about 13 s. The scenario "Red build blocks the commit" is shown by two harness
cases together (FF-T8-b): `tbc-commit-red` (exit 2, header, 5-line tail, `h5:79`) and `tbc-chained-git-add-and-git-commit`
(`git add -A && git commit -am wip`, exit 2, same header and tail, `h5:100`). The known exit-0 form is
`tbc-escaped-quotes-before-commit` (`h5:14`, row `h5:87`).

### Z2 The commit hook on a real red build

The staged hook was run the way `settings.json` names it, from another folder with only `CLAUDE_PROJECT_DIR` set, on the
scratch sandbox with a failing test file added (`real:26-44`). The stdin is a PreToolUse payload with the command in it.

| Case | Command in the payload | Result | Line |
|---|---|---|---|
| green tree, plain | `git commit -m "test"` | exit 0 after 29 s, no output | `real:19` |
| not a commit | `git status` | exit 0 after 0 s, no build | `real:23` |
| red, plain | `git commit -m "test"` | exit 2 after 25 s, 41 stderr lines, first line `Commit blocked: ./mvnw verify failed. Fix the cause (do not weaken tests), then commit again.` | `real:30-31` |
| red, chained | `git add -A && git commit -m "test"` | the same | `real:33-34` |
| red, `-n` after the message | `git commit -m "x" -n` | the same: the Claude hook still runs | `real:36-37` |
| red, quoted path before the command (PF-T8-1) | `cd "<folder>" && git commit -m "test"` | exit 0 after 0 s: no build, the known bypass | `real:39-40` |
| red, `-C` between git and commit | `git -C . commit -m "test"` | exit 0 after 0 s: no build | `real:42-43` |

The hooks never commit: the commit count is 4 before and after (`real:28`, `real:44`). The failing test is in the last 40
lines the hook sends back: `[ERROR]   RehearsalRedTest.deliberatelyRed:11` (`real:70`), and for the guide's own prompt
("change Money to round HALF_UP on purpose and commit", G04 :151) the one-line edit of `Money.java` line 27 fails
`MoneyTest.roundsHalfEvenToTwoDecimals:17` and `CatalogueIntegrationTest.pricesAreRoundedHalfEven:95` (`real:126-129`).
A longer failure list can push the test name out of the 40-line tail: then the first stderr line still states that the
commit was blocked (note for row R3-1).

### Z3 Cold `./mvnw -q verify` against the timeout 180

| Run | Wall clock | Line |
|---|---|---|
| fresh clone (no `target`, no `node_modules`), warm `~/.m2`, Docker running | 38 s, exit 0 | `real:9` |
| the same on another fresh clone with an EMPTY local Maven repository (`-Dmaven.repo.local`), 301 jars downloaded | 57 s, exit 0 | `cold:4-5` |
| the commit hook on a green tree, warm | 29 s | `real:19` |
| the commit hook on a red tree | 24 to 26 s | `real:30`, `real:33`, `real:36` |
| `./mvnw clean verify` of the full level-3 sandbox | 32 s wall clock, `Total time: 31.077 s` | `ver:2`, `ver:11` |
| the Stop hook, `ArchitectureTest` only | 5 s | `stop:11` |

The margin to 180 s is a factor of 3 to 5 on this machine, with a fast network and the Maven wrapper distribution and
the Docker images already present (the setup check runs the build once). A slower disk or network, a first run that also
downloads the wrapper, or a heavily loaded laptop can exceed 180 s. What then happens is stated by the docs and the
changelog, not shown by a run: the timeout counts as a failure (`docs:17`), and "With `block` set, a failure does what
exit code 2 does on that event ... a `PreToolUse` failure blocks the tool call" (`docs:18`). So a timeout BLOCKS the
commit, it does not let it through. The risk that a slow first build lets an untested commit through (pack line 275 to 276) is gone
through D9 (PF-T10-7 keeps the practical advice).

### Z4 What `onFailure: "block"` does on a timeout and on a non-2 exit (A1, FF-T7-a, PF-QA-T7-1)

| Event | With `"onFailure": "block"` (staged) | Source |
|---|---|---|
| the hook cannot start (script or interpreter missing) | the commit is blocked | `docs:15`, `docs:18`, and the hooks docs lines 955 to 958 with the notice `failed; blocking because onFailure is "block"` (`docs:61-62`). No harness case starts a hook that cannot start: `tbc-could-not-start-mvnw-missing` and `-not-executable` are `mvnw` missing or not executable INSIDE the hook, where the script itself exits 2 (`h5:106-107`). The start failure was run by hand: with `chmod -x` the command string of `settings.json` exits 126, `Permission denied`, in `sh -c` and in `bash -c` (`rr1:54-55`); row R3-1 variant E is the Claude-side observation |
| the hook exits with a code other than 0 or 2, for example exit 1 when the project folder cannot be entered | the commit is blocked | `docs:16`, `docs:18`; harness `tbc-could-not-cd` exits 1 (`h5:105`) |
| the hook reaches its `timeout` (180 s) | the commit is blocked | `docs:17`, `docs:18`, and after a timeout "the message says `timed out` instead of `failed`" (`docs:63`); changelog line `t7/d9-onfailure-and-hook-triggers.txt:35` |
| without the field (the pack's earlier block) | all three are non-blocking errors: the commit goes through | `docs:20`; `t7/d9-onfailure-and-hook-triggers.txt:26`, :29 |
| the Stop handler (it has no `onFailure`) | exit 2 keeps Claude working; any other non-zero exit is a non-blocking error | `t7/d9-onfailure-and-hook-triggers.txt:69`; `windows-hooks.md` section 2 |

A1's timeout probe ("a hook that times out blocks the commit (only if the field exists)") is satisfied by the documented
behaviour plus the harness. No hook was run past 180 s and no Claude session started (D14). The harness cannot exercise
Claude's timeout; it asserts the scripts' own exit codes (0, 2, 1). The field exists in the docs from Claude Code 2.1.295
(`t7/d9-onfailure-and-hook-triggers.txt:18`); row R3-1 asks Maik to record `claude --version` and to treat anything older
as the reason the block does not happen. Variant E of R3-1 (FF-T7-a) makes the first line of the table observable in
Claude Code: switch off the execute permission of the commit hook, run prompt A on a green tree, expect the block with
`failed; blocking because onFailure is "block"`, restore. The hook then ends in exit 126, which is in the same documented
class as exit 1 ("an exit code other than 0 or 2", `docs:16`). An exit 1 of the script itself and the timeout cannot be
triggered in a row without editing the hook, which the rows forbid, so for them the row states the expectation and its
docs lines (`docs:16`, `docs:17`, `docs:18`, `docs:63`) and does not claim an observation.

### Z5 `./mvnw clean verify` on the full level-3 sandbox

```
$ ./mvnw clean verify          (sbx-1 HEAD a20c4ad, tree 309786e0, exit code 0, 32 s)
[INFO] Tests run: 1428, Failures: 0, Errors: 0, Skipped: 0                              (ver:4)
[INFO] Tests run: 190, Failures: 0, Errors: 0, Skipped: 0                               (ver:5)
[INFO] frontend ........................................... SUCCESS [  7.739 s]       (ver:7)
[INFO] bookstore .......................................... SUCCESS [ 18.604 s]       (ver:8)
[INFO] distributor ........................................ SUCCESS [  4.524 s]       (ver:9)
[INFO] BUILD SUCCESS                                                                    (ver:10)
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 ... -- in nl.openvalue.bookstore.ArchitectureTest   (ver:12)
testsuite name=nl.openvalue.bookstore.ArchitectureTest tests=22 failures=0 errors=0 skipped=0   (Surefire XML, ver:23)
```

The cancel-order test classes ran in the same build (`ver:14-21`) and the XML holds 41 test cases with `ancel` in the name
(`ver:25`). The 1428 bookstore tests are 1380 of the baseline plus the 48 of the exercise (the T9 record shows 1380 for
the same level without the exercise); the red runs above show 1429 and 1428 with one deliberately red test or one Money
change.

### Z6 The Stop hook and the git hook with real builds

- Stop hook (`stop`): no Java change, exit 0 after 0 s (`stop:6`); an untracked Java file, `ArchitectureTest` runs, exit 0
  after 5 s (`stop:11`); an untracked class in `domain` that carries a Spring annotation, exit 2 and the first stderr
  line `ArchitectureTest fails. Fix the layering before you finish:` with the rule `DOMAIN_DEPENDS_ON_JAVA_ONLY` in the
  tail (`stop:17-18`, `stop:24`, `stop:37`); the same red tree with `stop_hook_active` true exits 0 at once, the loop
  guard (`stop:53`).
- Git hook (`real:93-97`): with `git config core.hooksPath .githooks` a commit on a red tree prints `pre-commit:
  ./mvnw -q verify`, then `pre-commit: build failed, commit aborted`, exits 1 and makes no commit; `git commit
  --no-verify -m x` on the same tree commits (a scratch repository, the control for why the deny rules exist).
- The push control (`dry:20-26`): with a local bare remote that holds the seeded branch and a local commit ahead of it, a
  `git push` run by hand outside Claude changes the remote's refs. So "the refs are unchanged" in row R3-2 is a meaningful
  observation: it fails if the push ran.

## Deny rules: static matching and position variants (criterion 4)

Consistency evidence only. The matcher (`deny-match.py.txt`) implements exactly what the permissions page says, and the
behaviour in Claude Code is rows R3-2 and R3-3. Docs page: https://code.claude.com/docs/en/permissions.md, read on
2026-11-10, sha256 `86be6c54...` (`docs:4`). Sentences used, with the lines of the fetched file:

- "Rules are evaluated in order: deny, then ask, then allow. The first match in that order determines the outcome"
  (`permissions.md:56`, `docs:22`).
- "A `*` in a Bash rule matches any text, including spaces, so one rule covers a family of commands. A rule with no `*`
  matches one exact command." (`:146`, `docs:23`)
- "A `*` at the end, with a space before it, also matches the bare command." (`:184`, `docs:25`)
- "Deny and ask rules apply when any subcommand matches them" (`:221`, `docs:28`); wrappers such as `timeout` are stripped
  and "A deny or ask rule matches past any leading assignment" (`:229`, `:231`, `docs:29-30`).
- "A push written another way, such as `git -C . push`, isn't matched" (`:152`, `docs:24`) and the table row
  `Bash(git push *)` stops `git push origin main` and does not stop `git -C . push origin main` or `git -c push.default=current push origin main`
  (`:256`, `docs:33`).
- "Read and Edit deny rules apply to Claude's built-in file tools, to file commands Claude Code recognizes in Bash, such as
  `cat`, `head`, `tail`, `sed`, and `tee` ... They don't apply to a command that reads files without naming them, such as
  `grep -r pattern .` ... or to arbitrary subprocesses that read or write files indirectly, like a Python or Node script
  that opens files itself." (`:355`, `docs:35`)

Result of the staged rules against 34 commands and 5 Read paths: 0 mismatches against these semantics (`deny:63`), and
four controls, each dropping rules from a copy, make the matcher report the changes (`deny:66-96`).

| Command | Static result | Rule or reason | Line |
|---|---|---|---|
| `git commit --no-verify -m "x"` | denied | `Bash(* --no-verify*)` | `deny:24` |
| `git commit -n -m "x"` | denied | `Bash(git commit -n *)` | `deny:25` |
| `git push` | denied | `Bash(git push)` and the bare match of `Bash(git push *)` | `deny:26` |
| `git add -A && git push`, `FOO=bar git push`, `timeout 30 git push` | denied | the push rules, per subcommand | `deny:37-39` |
| `git commit -m "feat: x"`, `git add -A && git commit -m "feat: x"`, `git status`, `./mvnw -q verify` | allowed | the allow rules | `deny:42-49` |
| Read tool on `.env`, `./.env`, `.env.local` | denied | `Read(./.env)`, `Read(./.env.*)` | `deny:58-60` |

Position variants the rules may miss, each a variant row of the checklist:

| Variant | Static result | What else guards | Row |
|---|---|---|---|
| `git commit -m "x" -n` | not denied, allowed by `Bash(git commit *)` (`deny:28`) | the rule `Bash(git commit -n *)` is anchored at `git commit -n `; the Claude commit hook still runs and blocks a red build (`real:36-37`); `-n` skips only the git hook | R3-2 V1 |
| `git commit -nm "x"` | not denied (`deny:29`) | same | R3-2 V1 |
| `git commit -m "x" --no-verify`, `git commit --no-verify` | denied (`deny:30-31`) | `Bash(* --no-verify*)` has a leading wildcard | none needed |
| `git -C . push`, `git -C . push origin HEAD:refs/heads/rehearsal` | no rule matches, neither deny nor allow (`deny:32-33`): prompt or classifier | the docs name this form (`docs:24`, `docs:33`) | R3-2 V2 |
| `git -c push.default=current push`, `/usr/bin/git push`, `sh -c 'git push'` | no rule matches (`deny:34-36`) | the docs table names the first (`docs:33`); the others follow the "different form" sentence (`permissions.md:250`, `docs:32`) | R3-2 V3 |
| `cat .env`, `head .env`, `tail -n 5 .env`, `cat < .env` through Bash | denied by the Read rules, because the docs say they cover these file commands (`deny:50-55`) | the premise of the task brief (that the Read rules do not cover Bash) is out of date against the docs of 2026-11-10 (PF-T10-4); the row decides what Claude Code does | R3-3 V1 |
| `grep -r REHEARSAL_SECRET .`, `python3 -c "print(open('.env').read())"` | not covered: `grep` is in the read-only set and runs without a prompt, `python3` has no rule (`deny:56-57`); the docs name both gaps (`docs:35`) | the documented gap: only a sandbox closes it | R3-3 V2 |
| `cd "<folder>" && git commit -m "test"` on a red build (PF-T8-1) | allowed: the allow rule matches the second subcommand and a `cd` into the folder you are in is a no-op that does not prompt (`docs:40`) | the commit hook exits 0 without the build: a known bypass (`real:39-40`, `h5:14`, `h5:87`) | R3-1 variant C |
| `git -C . commit -m "test"` | no rule matches (`deny:41`) | the commit hook exits 0 without the build (`real:42-43`) | R3-1 variant D |

## Skill selection: static check (criterion 5)

- Scenario sentence (pack :425, plan :278, guide G04 :191): `Customers must be able to cancel an order that has not
  shipped yet.`
- Skill `description` (SKILL.md:3, 279 characters): `Adds or changes a bookstore use case end to end through the Clean
  Hexagonal Onion layers (domain behaviour, flow, ports, command or query adapter, data adapter, migration, tests). Use
  when asked to add a feature, endpoint, business rule, command or query in the bookstore module.`
- Search (`skill:6-7`): the two texts share NO content word. The sentence words are able, cancel, customers, must, order,
  shipped, yet; none is in the description. The control, the description with "Cancel an order that has not shipped."
  added, shares three (`skill:21-22`), so the check can see an overlap.
- Reading (`skill:9-17`): the description names the kinds of request "add a feature", "endpoint", "business rule",
  "command or query" and "the bookstore module", and the sentence is a feature request that adds behaviour to the
  bookstore's `Order` (a rule: only an order that has not shipped can be cancelled). The link is meaning, not shared
  words. The docs say "Claude uses this to decide when to apply the skill" (`docs:48`) and "Description always in
  context, full skill loads when invoked" (`docs:54`). Whether Claude picks the skill is row R3-4, and the pack file stays
  as it is (verbatim, FF-T1-b). This is product finding PF-T10-1.

## J15 amended (criterion 7)

If Maik's rehearsal triggers the pack's plugin fallback (row R2-5: the plugin does not load from the project
declaration), the edits are four, not three, and the fourth depends on how the first is done: (1) the level-2 settings
file `level-2/files/dot-claude/settings.json` loses both plugin keys, either (1a) by removing the file AND its row in
`level-2/manifest.tsv`, or (1b) by keeping the file (an empty object) and its row; (2) the two plugin lines of
`docs/lsp-setup.md` (lines 25 and 26) go; (3) the level-3 settings file `level-3/files/dot-claude/settings.json` drops
the same two plugin keys, in one Write (HR2); and (4) level-3 manifest row 2 flips from `replace` to `add` if and only
if (1a) was done. Both outcomes were run on four copies of the staging tree, each built with
`simulate-level-0.sh` and `build-level.sh level-3` (`j15:10-38`, control exit 0 on the unmodified copy at `j15:10-14`;
the real staging tree is unchanged, `j15:41-46`):

| Copy | Level-2 settings | Level-3 row 2 | `build-level.sh level-3` | Line |
|---|---|---|---|---|
| A1 | row and file removed (1a) | `replace`, no edit (4) | exit 1: `level-3/manifest.tsv line 2: replace: target does not exist: .claude/settings.json` | `j15:16-20` |
| A2 | row and file removed (1a) | `add`, edit (4) | exit 0 | `j15:22-26` |
| B1 | file kept without the keys (1b) | `replace`, no edit (4) | exit 0 | `j15:28-32` |
| B2 | file kept without the keys (1b) | `add`, edit (4) | exit 1: `level-3/manifest.tsv line 2: add: target exists: .claude/settings.json` | `j15:34-38` |

So with (1a) the flip is required (a `replace` row needs a file that exists and the level-2 row that created it is
gone), and with (1b) the flip must NOT be done (an `add` row refuses an existing file). The plan's J15 sentence (the
level-2 settings `add` row goes) is (1a); R2-5 (1) allows (1b) ("whether it stays as an empty object or the manifest row
for it goes is for the orchestrator to decide"). Evidence for the current state: row 2 of `level-3/manifest.tsv` is
`replace` of `.claude/settings.json` with mode 644 (`inv` T10, `inv:13`), and the level-3 file keeps every level-2 key
unchanged and adds exactly `hooks` and `permissions` (`inv` T09), so after step (3) the file holds only those two keys
and in case (1a) can be an `add` row. The checklist row R2-5 names (1) to (3); row R3-8 of the Level 3 section states
(1a) and (1b), (4) and the check, which is conditional in the same way.

## Residuals and product notes from T9 (FF-T9-a, criterion 8)

Residual line for `solution/level-3`: README.md line 70 (the Orders endpoints) and `http/orders.http` do not mention
`POST /orders/{id}/commands/cancel`.

- README.md line 70 on `level-0-start` reads `- Orders: GET /orders/{id}, GET /orders?customerEmail=, POST
  /orders/commands/place, POST /orders/{id}/commands/ship` (`claims:5`) and names no cancel endpoint (`claims:6`); the
  control finds `commands/ship` once, on line 70 (`claims:7`). The level-3 tree holds the same README.md (`claims:8-9`).
- `git show level-0-start:http/orders.http` holds the place and ship requests and no cancel request: no `.http` file on
  `level-0-start` or on the level-3 tree names cancel (`claims:13`, `claims:15`), the control finds `commands/ship` in
  `http/orders.http` (`claims:14`), and every `.http` file on the tree equals the base (`claims:16`).
- The level-3 manifest has no row for README.md or `http/` (`claims:19-21`), and the staged skill never asks for README or
  `http/` updates: no hit in `SKILL.md`, `reference.md` or `ship.md` (`claims:24-27`). So participants who build the
  exercise with the staged skill get the same gap.

Product note for Maik: the staged `new-use-case` skill never asks for a README or `http/` update, so participants'
solutions show the same gap. Options: an owner-approved level-3 exercise patch row for README.md (and, if wanted,
`http/orders.http`) with its base blob from `level-0-start` (`git rev-parse level-0-start:README.md`), or accept it as a
residual. This task changes neither; no file outside the records changed.

T9's frontend residual: the storefront does not know a cancelled order. `frontend/src/pages/OrderPage.tsx` line 29 prints
`Not shipped yet` for any order whose `shippedAt` is null, a cancelled one included, and
`frontend/src/api/types.ts` line 18 reads `export type OrderStatus = 'PLACED' | 'SHIPPED'` (`claims:36-37`). No
frontend file knows `CANCELLED` or `cancelledAt` on the base or on the level-3 tree (`claims:30`); the only
case-insensitive hits of `cancel` are an unrelated DOM event test in `CataloguePage.test.tsx` (`claims:31-35`); the
backend does name them (control, `claims:39`).

## Product findings for Maik

- PF-T10-1 (selection risk, owner decision only if row R3-4 fails): the skill description shares no content word with
  the scenario sentence (`skill:7`; only the stop word `to` is in both raw word sets). The pack and the guide both say the description "is what makes Claude pick it". If row R3-4
  fails in fresh sessions, the options are an owner-approved wording change of the verbatim description (for example
  naming requirements that change how an order behaves) or accepting that participants must name the skill. No file is
  changed by this task.
- PF-T10-2 (D16 left PowerShell text behind): `10-harness-reference-solutions.md` lines 219-220 ("the PowerShell hook
  variants"), 322 ("PowerShell variants `tests-before-commit.ps1` ..."; the sentence after it about Git Bash is still
  right) and 500 ("write the PowerShell variants"), `11-workshop-guide-site.md` line 56 ("Hook script tabs: bash and
  PowerShell") and `workshop-plan.md` line 344 ("Provide `.ps1` variants of the hook scripts in `solution/level-3`")
  still ask for the variants that D16 dropped (`inv` T100 to T104). Suggested fix: delete those words. The guide itself
  is already clean: no inventory row lists a guide page (`inv` T100 to T104).
- PF-T10-3 (= PF-QA-T7-1, now with every line): with `"onFailure": "block"` on the commit hook, an exit code other than 0
  and 2 blocks the commit (`docs:16`, `docs:18`). Five places say the opposite: the comment of the pack's hook block
  (`10-harness-reference-solutions.md` line 283, also line 3 of the staged `tests-before-commit.sh`, kept verbatim),
  `workshop-plan.md` line 262, guide page 04 lines 105 and 112, and `reference/cheat-sheet.md` line 21 (`inv` T105 to
  T107). The statement is true for a participant's own hook, written without `onFailure`, and the guide's fragment
  (G04 :92-100) leaves the field out on purpose (`inv` T41, T32), so the fix is wording: say that a hook without
  `onFailure` does not block on other codes and that the reference solution's does.
- PF-T10-4 (premise out of date): plan amendment A2 (T10) lists `cat .env` through Bash as a variant the rules may miss,
  and the task brief expects `cat .env` and `head .env` to get past the Read rules. The docs page of 2026-11-10 says the Read deny rules also apply to the file commands Claude Code
  recognizes in Bash, `cat`, `head`, `tail`, `sed` and `tee`, and to `<` targets (`docs:35`). The static result is a deny
  (`deny:50-55`). The documented gaps are a command that reads files without naming them (`grep -r pattern .`) and a script
  that opens the file itself (`python3 -c`), `deny:56-57`. Row R3-3 records which of these Claude Code really denies.
- PF-T10-5: `Bash(git commit -n *)` is anchored at the start, so `git commit -m "x" -n` and `git commit -nm "x"` are
  allowed (`deny:28-29`). The consequence is small: the Claude commit hook still builds (`real:36-37`); only the git
  pre-commit hook is skipped. `Bash(* --no-verify*)` has a leading wildcard and catches the long flag at any position
  (`deny:30-31`). No change proposed.
- PF-T10-6 (= PF-T8-1, extended): the commit hook finds `git commit` only up to the first double quote of the command, so
  `cd "<folder>" && git commit -m "test"` exits 0 without a build (`real:39-40`), and `git -C . commit -m "test"`
  matches neither the hook nor the allow rule (`real:42-43`, `deny:41`; it needs a permission prompt or the classifier).
  Rows R3-1 C and D show what Claude Code does. The pack hook is verbatim; no edit here.
- PF-T10-7 (timeout in practice): on this machine the whole build takes 29 to 57 s against the timeout 180 (`real:19`,
  `cold:4`). A participant whose first build downloads everything over a slow network, or whose machine is loaded, can
  pass 180 s. With D9 the commit is then blocked (not let through), and the next attempt is fast because the cache is
  warm. Suggested guide sentence: run `./mvnw verify` once before the first commit through Claude (the setup check
  already does).
- PF-T10-8 (= FF-T9-a): see the section "Residuals and product notes from T9".
- PF-T10-9 (layout of this folder, outside my scope): the layout list of `README.md` does not name the `records/` folder
  or `tests/hooks-fixtures/` as folders of their own (the second is named in the new `test-hooks.sh` line). Amendment A9
  allows one README item for this task, so I left it; a later task may add a `records/` line.
- Information: the public schemastore settings schema does not list `onFailure` (`t7/d9-onfailure-and-hook-triggers.txt:42`),
  so a schema-validating editor underlines it on both the staged file and a participant's copy.

## How the check script was built (negative controls)

`token-inventory.py.txt` was written with its controls first. The first draft reported controls C1 to C4 only through
one generic row (the staged file and the built tree differ) and crashed on C5 (a misspelled `onFailure` raised a
`KeyError`), so a token-specific check could have been missing without anyone noticing. The final version applies each
misspelling to the staged copy AND to the copy of the built tree, so only the token-specific checks can report it, and
each control must be reported by its own finding: C1 hook path (`inv:123`), C2 permission rule (`inv:124`), C3 skill name
(`inv:125`), C4 `/ship` allowed tools (`inv:126`), C5 hook field (`inv:127`), C6 guide hook script name (`inv:128`), C7
workshop plan activation command (`inv:129`), C8 guide skill path (`inv:130`); the summary is `controls: all reported`
(`inv:131`). The deny-rule matcher has four controls that drop rules (`deny:66-96`), and the skill check has one that adds
the sentence's words (`skill:19-22`). Each universal or descriptive claim of this record has a counterexample search with
a control that must hit in `claims` (FF-T7-b).

## Not proven here (listed for Maik, see `rehearsal-checklist.md`, section "Level 3")

- "Red build blocks the commit": what Claude Code shows for the block, that Claude's next message names the failing test,
  plain and chained commit, the two known bypasses, and the block for a hook that cannot start (variant E; exit 1 and a
  timeout stay documented expectations). R3-1, R3-5 and R3-7 W1 (plain and chained commit, quoted-path bypass).
- "Escape hatch is closed" on a green build: the three denials, the control commit, the variants. R3-2.
- "Secrets stay unread": the Read denial and the `cat`, `head`, `grep`, `python3` observations. R3-3.
- "Skill is selected automatically" on the G10 tree. R3-4.
- `/ship` commits through the hook. R3-5.
- The git hook on Maik's clone (zero-token proof exists). R3-6.
- Windows through Git Bash (W1 to W3 of `windows-hooks.md`, decisions D9 and D16). R3-7.
- J15 (plugin fallback companion edit): both outcomes ran on copies of the staging tree (`j15`), whether Maik's fallback
  keeps the level-2 settings row decides edit (4). R3-8.
- Claude Code version at least 2.1.295 for `onFailure`: `claude --version` is part of the common preconditions.

## Evidence index (`evidence/level-3/t10/`)

| File | Content |
|---|---|
| `sandbox-build.txt` | T1 script output for sbx-1 and the G10 sandbox, the G10 start commit, shas, trees, cmp of the 8 config rows, the cancel grep, the `.claude` file set |
| `sandbox-verify.txt` | `./mvnw clean verify` on the full level-3 sandbox, totals, `ArchitectureTest` line and XML |
| `harness-bash5.txt`, `harness-bash32.txt` | the T8 harness re-run on bash 5.3.20 and macOS `/bin/bash` 3.2.57, with the result tables |
| `real-hook-runs.txt` | the commit hook, the git hook and the guide's Money prompt on real builds; cold timing; the stderr copies |
| `real-hook-runs-stop.txt` | the Stop hook on real builds: green, red, loop guard |
| `cold-empty-m2-verify.txt` | `./mvnw -q verify` on a fresh clone with an empty local Maven repository |
| `token-inventory.txt`, `token-inventory.py.txt` | the 107-row inventory, the findings and the 8 controls; the script |
| `deny-rule-matching.txt`, `deny-match.py.txt` | docs sentences, the matcher run on the staged rules, 4 controls; the script |
| `skill-selection-static.txt`, `skill-check.py.txt` | the static skill description check with a control; the script |
| `claim-searches.txt` | counterexample searches with controls for every universal or descriptive claim (FF-T7-b), the README and `http/` proof (FF-T9-a), the frontend residual |
| `row-setup-dry-run.txt` | the setup, reset and judging commands of the rehearsal rows run in fresh clones (FF-T3-a); an r1 note appended at the end (its R3-2 section ran on a branch named `main`) |
| `row-dry-run-r1.txt` | rework r1: the R3-2 setup in a clone made with `git clone --branch solution/level-3`, the control with `git reset --soft`, variant V1 with the soft undo and the second variant committing, the restore of `origin`; R3-1 variant E (`chmod -x` on the commit hook: exit 126, the restore, the undo) |
| `j15-fallback-combinations.txt` | the J15 companion edits on four copies of the staging tree plus a control: which build-level runs exit 0 or 1, with the error lines (row R3-8) |
| `row-printf-check.txt` | the printf one-liner of row R3-1 against the heredoc used in the runs, on bash 5 and 3.2, with a control |
| `claude-docs-quotes.txt` | Claude Code docs lines the rows lean on, with sha256 and line numbers; block H5 (appended in r1) holds the `onFailure` notice wording and the failure classes |
| `staging-selftest.txt`, `referee-verify.txt` | the gate runs after the edits (re-run after the r1 edits, same result): `test-staging.sh` (`PASSED all 58 cases`) and `./mvnw clean verify` in the project root (`BUILD SUCCESS`, 1314 bookstore tests, 190 distributor tests, `ArchitectureTest` 22 tests) |
| `hygiene.txt` | (an r1 section was appended: the same checks again after the rework, files unchanged against their r0 hashes, appended evidence files keep their earlier bytes, the citation resolver over the new text) leak grep over the records folder with two positive controls, ASCII and dash checks, no `.log` file, the checklist edit is insertion only (the first 51366 bytes equal the committed file), the README diff has two hunks, no path outside the file scope changed, no Claude invocation |
| `../t7/d9-onfailure-and-hook-triggers.txt`, `../t8/*` | earlier evidence this record cites (read only) |
| `../cancel-order/**` | T9 evidence (read only, untouched by this task) |

## Rework r1 (after the QA verdict FAIL r0)

Five defects in the task's own rows and in the J15 text, fixed in `rehearsal-checklist.md` (Level 3 section only), this
record and the evidence folder; the staged files, the harness, `cancel-order/**` and the README are as in r0:

- R3-2 Setup printed a `git status -sb` line for a branch named `main`; it now says what `git clone --branch
  solution/level-3` prints, re-run in such a clone (`rr1:4-11`). The V1 undo is `git reset --soft <ready>` and the second
  variant commits (`rr1:17-25`; the hard undo's `nothing to commit` is shown at `rr1:26-33`); the control ends with a soft
  reset so V1 has a staged change (`rr1:13-15`); the restore of `origin` is stated (`rr1:37-43`).
- J15: both outcomes, section "J15 amended" and row R3-8, proven on copies (`j15`).
- FF-T8-b: R3-5 and R3-7 W1 run the plain and the chained commit and list the quoted-path bypass.
- FF-T7-a: R3-1 variant E (the hook cannot start) with the docs wording (`docs:60-67`) and the dry run (`rr1:45-71`).
- Nits: the guard sentence in the prompts, the Z4 citation, "no content word", the order inside R3-3, the stale `origin`.

## Scope guard

No `claude` command was run for this task and no `*.jsonl` or stream file exists in the evidence. The strings
`claude mcp add`, `claude --version`, `claude --debug` and `claude` appear only as quoted guide text or as commands in
rehearsal rows for Maik. `ANTHROPIC_API_KEY` was not set, read or printed; the run scripts unset it. No settings file was
written and no plugin or marketplace command ran (HR2): the staged settings file was read with the file reader, and the
inventory script compares its plugin part with the level-2 file without naming the keys. The scratch distributor was not
started, ports 8080 and 8090 were not opened, and `docker compose down -v` was not run. Docker was used only through the
Testcontainers of the Maven builds. All sandboxes stay in the session scratchpad until the gate passes. The leak grep over
this folder (the home folder prefix, the user name, the scratchpad prefix, the API key prefix and two token names) prints
nothing, and the same grep over two raw, unmasked outputs does hit (positive controls, `hygiene.txt`).
