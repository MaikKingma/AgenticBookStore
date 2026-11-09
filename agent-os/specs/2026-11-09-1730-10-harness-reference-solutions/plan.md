---
spec: 10-harness-reference-solutions
source: docs/spec-pack/specs/10-harness-reference-solutions.md
status: in-progress
phase: prepare
gate1: 2026-11-09T17:42:47Z
accepted-by: orchestrator (autonomous)
qa-lead: on
review: agent
base: factory
spec-branch: sdd/10-harness-reference-solutions/spec
---

# Plan: Spec 10 harness reference solutions (prepare phase)

Scope and decisions: `shape.md`. `S` = `docs/spec-pack/level-branches/`. QA strategy, the common C0 block and the
full per-task qaDirectives: `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-10-harness-reference-solutions.md`
(comms, not committed; the summaries below are the record).

Every task runs alone, in order T1 to T13 (batch = position; one worktree, one task at a time). Sandboxes live in the
session scratchpad, are built only with the T1 scripts, and are kept on disk until the task's gate passes. Headless
Claude Code runs follow J7 (`--setting-sources project,local`, `ENABLE_CLAUDEAI_MCP_SERVERS=false`, `--settings` for
the allow list, own `COMPOSE_PROJECT_NAME` and ports, no user-scope writes, never `ANTHROPIC_API_KEY`). Evidence
files are `.txt`, `.jsonl` or `.md`, never `.log` (root `.gitignore` ignores `*.log` and `notes/`).

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | impl model | pr | branch | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | staging layout, scripts, self-test, branch-cut tool | root | [] | 1 | critical | opus | opus | #135 | sdd/10-harness-reference-solutions/T1 | ENDORSE (r3) |
| T2 | Level 1 files staged verbatim | root | [T1] | 2 | standard | sonnet | sonnet | #139 | sdd/10-harness-reference-solutions/T2 | ENDORSE (r0) |
| T3 | Level 1 scenarios run and recorded | root | [T2] | 3 | elevated | sonnet | sonnet | | | |
| T4 | Level 2 config and `docs/lsp-setup.md` | root | [T2] | 4 | standard | sonnet | sonnet | | | |
| T5 | Level 2 multi-role ACL contract test from MCP data | bookstore | [T4] | 5 | elevated | sonnet | sonnet | | | |
| T6 | Level 2 scenarios and macOS LSP rehearsal | root | [T5] | 6 | elevated | sonnet | sonnet | | | |
| T7 | Level 3 settings, skill, reference, command, subagent | root | [T4] | 7 | elevated | sonnet | sonnet | | | |
| T8 | Level 3 hooks (sh, ps1, git hook), harness, Windows note | root | [T7] | 8 | critical | opus | opus | | | |
| T9 | Level 3 cancel order built with the skill, committed via `/ship` | bookstore | [T8] | 9 | critical | opus | opus | | | |
| T10 | Level 3 scenarios run and recorded | root | [T9] | 10 | elevated | sonnet | sonnet | | | |
| T11 | Level 4 OpenSpec track | root | [T10] | 11 | standard | sonnet | sonnet | | | |
| T12 | Level 4 Spec Kit track | root | [T10] | 12 | elevated | sonnet | sonnet | | | |
| T13 | Level 4 Agent OS track | root | [T10] | 13 | elevated | sonnet | sonnet | | | |

### T1 Staging infrastructure and branch-cut tooling
fileScope: `S/README.md`, `S/.gitattributes`, `S/scripts/**`, `S/tests/test-staging.sh`, `S/tests/fixtures/**`.
1. `S/README.md` documents the layout (one folder per step: `level-1`, `level-2`, `level-3`, `level-4-openspec`,
   `level-4-speckit`, `level-4-agentos`, each with `manifest.tsv`, `files/`, optional `patches/`), the path encoding
   rule (a leading dot of any component becomes `dot-`; `CLAUDE.md` is staged as `CLAUDE.staged.md`; no staged
   component may start with a dot or be named `CLAUDE.md`, `CLAUDE.local.md` or `AGENTS.md`), the manifest columns
   (op `add|replace|patch`, group `config|exercise`, staged path, target path, mode `644|755`, base blob sha for
   `patch`), the chain (level-2 = level-0 + level-1 + level-2; each level-4 track = level-3 + track), and the exact
   cut and verify procedure from `level-0-start`. Plain English, no em dashes.
2. `S/scripts/simulate-level-0.sh <ref> <out-dir>` builds a scratch git repo whose single commit is the tree of
   `<ref>` minus the spec 09 strip list, using only read-only git on the source repo (`git archive`). If `<ref>`
   contains `scripts/publish-workshop-main.sh` and it answers `--list-paths` (one path per line, exit 0), that list is
   used; otherwise the built-in copy of the list in `docs/spec-pack/specs/09-workshop-baseline.md`. The self-test
   covers both branches with a fake publish script and diffs the built-in list against the spec 09 text.
3. `S/scripts/apply-level.sh [--group config|exercise] <step> <target-repo>` applies one step: `add` refuses an
   existing target, `replace` requires one, `patch` applies with `git apply --3way` and refuses when the base blob is
   missing from the target repo or differs and the 3-way merge is not clean; modes from the manifest; decodes paths;
   refuses absolute or `..` targets and encoded paths that break the rule; atomic (on any error the target tree is
   unchanged); leaves changes uncommitted; exits non-zero naming the failing manifest line.
4. `S/scripts/build-level.sh <step> <target-repo>` applies the chain and commits one commit per step with a fixed
   message per step.
5. `S/scripts/cut-level-branches.sh <level-0-ref>` creates LOCAL branches `solution/level-1` to `solution/level-3` and
   `start/level-4-openspec`, `start/level-4-speckit`, `start/level-4-agentos`, one commit per step on its parent,
   dry-run by default (prints every command), `--execute` to create them; never pushes, never touches tags, refuses
   when a target branch exists.
6. `S/tests/test-staging.sh` (bash, offline, temp repos, exit 0) covers every success and refusal path of 2 to 5,
   mode 755 kept, eol per `S/.gitattributes` (LF for `*.sh` and `dot-githooks/*`, CRLF for `*.ps1`, checked with
   `git ls-files --eol` on a fresh clone and `od -c`), and no ref written in the source repo.
7. Factory referee `./mvnw clean verify` green; nothing outside `S` changes.
qaDirective (summary): run the self-test; 14 script mutants each turn it red; ref safety (for-each-ref, worktrees,
tags, bare remote unchanged); chain counts 1/2/3/4 and branch tree equals build-level tree; README commands run as
written.

### T2 Level 1 files
fileScope: `S/level-1/**`. The five files of the pack's `solution/level-1` section staged verbatim (`CLAUDE.md`,
`.claude/rules/domain.md`, `.claude/rules/data-and-migrations.md`, `.claude/rules/testing.md`,
`CLAUDE.local.md.example`). Criteria: each equals its pack fenced block; `CLAUDE.md` at most 60 lines; build-level
`level-1` on a simulated level-0 of `origin/factory` adds exactly these five paths; `CLAUDE.local.md` stays ignored on
the target; `./mvnw clean verify` green in that sandbox.
qaDirective: own extractor plus `cmp` with a negative control; five `A` rows; check-ignore; verify.

### T3 Level 1 scenarios
fileScope: `S/records/level-1.md`, `S/records/evidence/level-1/**`. "Rules load on demand" (`/context` at start lists
`CLAUDE.md` and neither `docs/architecture.md` nor `domain.md`; an InstructionsLoaded hook passed with `--settings`
logs `domain.md` with `load_reason` `path_glob_match` and a trigger under `/domain/` after an edit) and "Level 0 task
with context" (three fresh `--model sonnet` runs of the literal Level 0 prompt, workshop-plan lines 97 and 98, in
separate sandboxes). Per run, judged outside Claude: verify result (tail quoted), a `Money`-typed discount in a
`domain` file (file and line), a new `V{n}__` migration and no edited migration (diff), ArchitectureTest result.
Verdict on "at least two of three", every attempt reported. The record states `docs/architecture.md` is absent on the
simulated base and that the runs are repeated on the real `level-0-start` in the resumed run.
qaDirective: re-run `/context`; audit the three sessions from kept sandboxes; one capped spot re-run of the
InstructionsLoaded run; re-verify one run yourself.

### T4 Level 2 configuration and LSP note
fileScope: `S/level-2/**` (config rows). `.mcp.json` and `.claude/settings.json` (plugin declaration) verbatim;
`docs/lsp-setup.md` written from the pack bullets: at most 40 lines, every bullet covered, ASCII, no em dashes, no
`ENABLE_LSP_TOOL`, JDK floor "JDK 21 or later" (J8). Verify green on a level-2 config sandbox.

### T5 Level 2 exercise: multi-role contributor ACL contract test
fileScope: `S/level-2/**` (exercise rows: patch of `DistributorAppServiceImplTest.java`, new contract JSON under
`bookstore/src/test/resources/contracts/distributor/`, patch of that folder's README), `S/records/evidence/level-2/acl/**`.
In a level-2 sandbox with the distributor on a free port, headless Claude Code with the project `.mcp.json`
(`DISTRIBUTOR_MCP_URL`) calls `get_title` for 9789900010395 (or another historical multi-role title); the payload
becomes the contract file, byte-identical to the MCP tool result and to REST `GET /api/titles/{ean}`; one new
`MockRestServiceServer` test asserts the exact author list and the absence of every non-author; the README's
"AUTHOR-only" sentence is corrected; verify green; factory unchanged outside `S`.
qaDirective: zero-token byte identity via curl; mutants (filter removed, role checks) and a data probe must fail the test.

### T6 Level 2 scenarios and macOS LSP rehearsal
fileScope: `S/records/level-2.md`, `S/records/evidence/level-2/**` (except `acl/`). `brew install jdtls` if missing
(J5). "MCP is available" (init event: `distributor` connected from the project, exactly `search_titles`, `get_title`,
`list_subjects`, `check_availability`), "ACL contract test from real MCP data" (verify, test name), "Type error is
reported and fixed", "Find references across layers" (`Money.of`, LSP tool hits in at least two layers, no Grep),
headless with `--plugin-dir <marketplace copy>/plugins/jdtls-lsp`. Record the jdtls version and Java floor, the
first-index time with a proven cold cache (unique sandbox folder), and the multi-module result (references across
`bookstore` packages resolve; no diagnostics on an unchanged tree). "Plugin loads from the project declaration" is
recorded as interactive-only with the res-1 evidence and listed as a manual check for Maik. If LSP fails, the task
blocks with a question (pack rule).

### T7 Level 3 configuration
fileScope: `S/level-3/**` (config rows except hooks). `.claude/settings.json` = pack level-2 keys plus the level-3
`permissions` and `hooks` (replace op, list order as in the pack); `new-use-case/SKILL.md`, `commands/ship.md`,
`agents/architecture-reviewer.md` verbatim; `new-use-case/reference.md` under 80 lines, one row per layer (package,
allowed dependencies, suffixes, test type, one exemplary existing class path), every path present on the level-0 tree,
rules traced to the architecture standard; the assistant row marked "re-check at cut" (spec 08). Verify green.

### T8 Level 3 hooks, harness, Windows note
fileScope: `S/level-3/files/dot-claude/hooks/**`, `S/level-3/files/dot-githooks/**`, their manifest rows,
`S/tests/test-hooks.sh`, `S/tests/hooks-fixtures/**`, `S/records/windows-hooks.md`. The two `.sh` hooks and
`.githooks/pre-commit` verbatim (755, LF); `tests-before-commit.ps1` and `architecture-check-on-stop.ps1` with
identical behaviour (CRLF); the harness runs every hook against stub `mvnw` (green, red, long output) and stdin
fixtures (git commit, other command, escaped quotes, `stop_hook_active` true with and without spaces, Java change, no
Java change) asserting exit code, stub argv, stderr first line and tail length; the `.ps1` cases run under `pwsh` in
the official PowerShell container image pinned by digest; sh and ps1 result tables identical. The Windows note states
what is known and what needs a Windows machine (manual check for Maik) and lists J6 as owner decisions.

### T9 Level 3 exercise: cancel order
fileScope: `S/level-3/**` (exercise rows under `bookstore/`), `S/records/evidence/level-3/cancel-order/**`. A fresh
headless session on a level-3 sandbox receives "Customers must be able to cancel an order that has not shipped yet."
and calls the `new-use-case` skill before any edit (evidence). The result is completed test-first (a recorded red run
per layer failing on an assertion) to the reference: `OrderStatus.CANCELLED`, `Order.cancel(Clock)` recording
`cancelledAt`, `StockItem.release(Quantity)`, `OrderFlow.cancelOrder`, `POST /orders/{id}/commands/cancel` (202;
404 `order-not-found`; 409 `order-already-shipped`; 409 `order-already-cancelled`), ship after cancel 409
`order-already-cancelled`, no domain event, V5 `cancelled_at`, backend views mirror `shippedAt` (sites listed; exact
field pins extended, never removed), tests per layer plus one integration test per scenario; committed in the sandbox
through headless `/ship` with the commit hook green; staged files equal that commit's blobs. Verify and ArchitectureTest
green; `grep -ri cancel bookstore/src/main/java` on factory stays empty. Frontend display of CANCELLED is a residual.

### T10 Level 3 scenarios
fileScope: `S/records/level-3.md`, `S/records/evidence/level-3/**` (except `cancel-order/`). "Red build blocks the
commit", "Escape hatch is closed" (on a green build: `git commit --no-verify -m x`, `git commit -n -m x`, `git push`
each a `permission_denied` event; push against a bare remote whose refs stay unchanged), "Secrets stay unread"
(`.env` read denied, plus the `cat .env` observation), "Skill is selected automatically" (fresh session on a level-3
sandbox built with `--group config` for level 3, `grep -ri cancel` empty). Windows runs: manual check for Maik.

### T11 Level 4 OpenSpec track
fileScope: `S/level-4-openspec/**`, the OpenSpec section of `S/records/level-4.md`. On a full level-3 sandbox:
`npx -y @fission-ai/openspec@1.16.0 init --tools claude`, `openspec/config.yaml` with the pack block, seed
`openspec/specs/ordering/spec.md` from spec 04's place and ship requirements, `openspec validate --all --strict
--no-interactive` exit 0 (plus a negative control); generated files staged as they are; contradictions with
`CLAUDE.md` recorded; tracked files unchanged; verify green.

### T12 Level 4 Spec Kit track
fileScope: `S/level-4-speckit/**`, the Spec Kit section of `S/records/level-4.md`. `uvx --from specify-cli==1.3.1
specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation"`, headless
`/speckit-constitution` with the pack's five principles, constitution reviewed and staged with the generated files;
`.claude/settings.json`, `CLAUDE.md`, hooks and plugin keys byte-identical to level 3; verify green.

### T13 Level 4 Agent OS track
fileScope: `S/level-4-agentos/**`, the Agent OS section of `S/records/level-4.md`. Agent OS cloned into a scratch
folder (commit recorded; `~/agent-os` untouched), `project-install.sh` in the sandbox, headless `/plan-product` with
the pack's `mission.md` and `tech-stack.md` inline, `/discover-standards` (architecture, testing, persistence), standards
reviewed against the architecture standard, `/index-standards`; everything staged; verify green. "Rehearsed in time"
for all three tracks is a manual check for Maik.

## QA feed-forward

- FF-T1-a (from T1; impl and QA directives of every task with a bash script or test harness, starting with T8):
  failure-injection tests up front (a shim first on PATH failing one exact call; one test per "could not X" branch
  asserting that message present and the success message absent); mode-only and content-only changes as separate
  rows; a shim pattern never catches a cleanup call another assertion of the same run checks; run the harness on
  bash 5 and on macOS `/bin/bash` 3.2 (`env PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash`) and quote both; an
  "equivalent mutant" claim needs an input run on original and mutant, never an argument.
- FF-T1-b (from T1; QA directives of all remaining tasks): a survivor that twins an already killed class (same
  reachability, same harm) is in scope and can FAIL; a survivor reachable only in an abnormal environment (closed or
  failing descriptors, concurrent writers, double faults) with harm outside the criterion's protected property is info
  plus a follow-up; verbatim pack code (T8 hooks, T7 skill files) is never a rework target, a defect there is a
  product finding for the orchestrator.
- FF-T1-c (from T1; every rework round): re-mutate only the production lines the round changed; earlier results for
  unchanged lines carry over when production is hash-equal to the last baseline, the test diff is insertion-only
  (proved by deleting the added lines from a copy and comparing with the earlier original) and the earlier logs are on
  disk; re-run the batch whose killing cases gained assertions; an escalated round gets a closed Required Actions list
  and named FAIL bounds.
- FF-T2-a (from T2; T3 directive and record): the T3 record states that `docs/architecture.md` (named by
  `CLAUDE.md`) and `db/seed/R__seed_demo_data.sql` (named by `.claude/rules/data-and-migrations.md` line 5) are absent
  on the simulated base and arrive with spec 09, and lists both as resumed-run checks
  (`git ls-tree level-0-start -- <path>` non-empty); a Level 0 run that tries to read either file and finds it
  missing is a side effect of the base, not a run failure; the verifier quotes such reads. qa-final re-runs the
  five-block `cmp` of Level 1 on the final bytes.
- Follow-ups from T1 for qa-final: FU-T1-1 (closed-stderr test of the three exit handlers; qa-final greps that each
  keeps `set +e`); F13 (README cut-procedure prose carries no push line, reading check); J12/F14 (backup removed on an
  incomplete rollback: residual for Maik).

## Amendments

- A1 (2026-11-09, orchestrator, from Maik's decision D9 on factory f5a0e17, merged into the spec branch as 726adf4):
  T7's `.claude/settings.json` follows the updated pack block, so the commit hook handler carries
  `"onFailure": "block"`. T7 verifies at build time that the field exists in the hooks schema of the Claude Code
  version in use (2.1.319 here; docs and a headless probe under HR1); if it does not, the pack's earlier block is
  staged without it, the gap is recorded in `S/records/level-3.md` as a note for the guide and reported.
  T8's Windows note states that Windows participants need Git for Windows (Git Bash) and that no `PowerShell(...)`
  handlers or rules are added; the `.ps1` variants stay as the pack says. T10 adds one probe: a hook that times out
  blocks the commit (only if the field exists). Gate 1 re-run and self-accepted: consequences are confined to T7, T8
  and T10 text, no new task.
- HR1 (2026-11-09, coordinator): no probe, scenario run or rehearsal changes anything outside the worktree and the
  session scratchpad; every headless `claude` and framework tool runs with an isolated `HOME` and XDG dirs under the
  scratchpad; every `mktemp` call (scripts, tests, README recipes, verifier runs) uses an explicit template under
  `$TMPDIR` or the scratchpad; every `./mvnw` run sets `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<scratch dir>` (the macOS
  JVM ignores TMPDIR); sandboxes use free ports; J10 allows sandbox Maven builds to read the shared `~/.m2` cache and Docker.
- HR2 (2026-11-09, coordinator, Maik's global plugin guard): each staged settings file that declares the plugin (T4,
  T7) is written once, in one Write of its final content, so Maik sees a single prompt; no workaround of the guard;
  plugin scenarios use the isolated HOME and `--plugin-dir`; a denied `claude plugin install` in the isolated HOME
  turns that scenario into a manual check.
