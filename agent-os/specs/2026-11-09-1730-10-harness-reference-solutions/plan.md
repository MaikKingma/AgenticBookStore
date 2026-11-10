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
session scratchpad, are built only with the T1 scripts, and are kept on disk until the task's gate passes. No agent
runs Claude Code for this spec (amendment A2, decision D14); CLI-only tools run under HR1 isolation, with their own
`COMPOSE_PROJECT_NAME` and ports where the app starts, never `ANTHROPIC_API_KEY`. Evidence
files are `.txt`, `.jsonl` or `.md`, never `.log` (root `.gitignore` ignores `*.log` and `notes/`).

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | impl model | pr | branch | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | staging layout, scripts, self-test, branch-cut tool | root | [] | 1 | critical | opus | opus | #135 | sdd/10-harness-reference-solutions/T1 | ENDORSE (r3) |
| T2 | Level 1 files staged verbatim | root | [T1] | 2 | standard | sonnet | sonnet | #139 | sdd/10-harness-reference-solutions/T2 | ENDORSE (r0) |
| T3 | Level 1 consistency check and rehearsal rows (A2) | root | [T2] | 3 | elevated | sonnet | sonnet | #144 | sdd/10-harness-reference-solutions/T3 | ENDORSE (r2) |
| T4 | Level 2 config and `docs/lsp-setup.md` | root | [T2] | 4 | standard | sonnet | sonnet | #142 | sdd/10-harness-reference-solutions/T4 | ENDORSE (r0) |
| T5 | Level 2 multi-role ACL contract test from MCP data (built by TDD, A2) | bookstore | [T4] | 5 | elevated | sonnet | sonnet | #148 | sdd/10-harness-reference-solutions/T5 | ENDORSE (r2) |
| T6 | Level 2 consistency check and LSP rehearsal rows (A2) | root | [T5] | 6 | elevated | sonnet | sonnet | #150 | sdd/10-harness-reference-solutions/T6 | ENDORSE (r2) |
| T7 | Level 3 settings, skill, reference, command, subagent | root | [T4] | 7 | elevated | sonnet | sonnet | #154 | sdd/10-harness-reference-solutions/T7 | ENDORSE (r2) |
| T8 | Level 3 hooks (sh, git hook), harness, Git Bash note (ps1 dropped, A6) | root | [T7] | 8 | critical | opus | opus | #155 | sdd/10-harness-reference-solutions/T8 | ENDORSE (r4) |
| T9 | Level 3 cancel order built by TDD (A2) | bookstore | [T8] | 9 | critical | opus | opus | | | |
| T10 | Level 3 consistency check and rehearsal rows (A2) | root | [T9] | 10 | elevated | sonnet | sonnet | | | |
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
  (`git ls-tree level-0-start -- <path>` non-empty; the seed path is on line 10 of the rule, corrected from "line 5"
  after the T3 r1 meta-review); a Level 0 run that tries to read either file and finds it
  missing is a side effect of the base, not a run failure; the verifier quotes such reads. qa-final re-runs the
  five-block `cmp` of Level 1 on the final bytes.
- FF-T4-a (from T4; T6 and T7): the rehearsal row for "Plugin loads from the project declaration" states that the
  pack fallback (remove both plugin keys) also removes the plugin lines of `docs/lsp-setup.md` (lines 25 and 26) and
  the same two keys from the level-3 settings file staged by T7.
- FF-D14 (from the T4 meta-review): C0-13 (consistency-check block) is prepended to the qaDirectives of T3, T5, T6,
  T9, T10, T12, T13 with the per-task deltas in the QA ledger ("QA strategy changes under D14"); C0-7, C0-12, P1, P4,
  the spot re-runs and every stream or transcript check lapse; C0-11 is reduced to one status per scenario and no
  claimed Claude result.
- FF-T3-a (from T3; QA directives of T6, T10, T11, T12, T13): C0-13e also checks that every rehearsal row's setup,
  reset and judging commands produce the state the scenario assumes: a recorded start commit,
  `git reset --hard <start> && git clean -fdx` (or a fresh clone) between repeated runs, judging against `<start>`
  with commits included, and `./mvnw clean verify` wherever a verify result is judged; the witnesses are stale
  git-ignored output (`target/classes/db/migration`) and commits made by a run.
- FF-T3-b (from T3; same tasks): repeated "fresh session" runs also start with clean Claude Code auto memory for the
  project, which lives outside the repository and is keyed by the clone path; require a new clone folder per run or a
  `/memory` check before each run (a git reset in the same folder does not touch it).
- FU-T3-1 (from the T3 r2 meta-review, folded into T6): T6 also updates the Level 1 row R1-2 Setup in
  `S/records/rehearsal-checklist.md` so a new clone folder per run is the default (or a `/memory` check before each
  run is required), per FF-T3-b.
- Product findings from T3 for Maik: PF-T3-1 spec 09 line 49 says "Level 1 imports it from `CLAUDE.md`" while spec
  10, the workshop plan and the guide use a plain path; PF-T3-2 `mvnw.cmd verify` is named only in spec 10 and
  `CLAUDE.md` (Windows rehearsal row R1-3). Guide notes: workshop-plan line 114's reset (`git restore . && git
  clean -fd`) keeps `target/`; running Level 0 and Level 1 in one clone carries Level 0's auto memory into Level 1.
- FF-T5-a (from T5; QA directives of T6, T9 to T13 and any task that adds to an existing participant-facing doc):
  after the edit, every sentence of the touched section is still true for the added content, including provenance and
  summary claims ("every file below ..."), headings that name a run, column headers and complete lists; a claim the
  addition makes false is a FAIL in the task's own file.
- FF-T5-b (from T5; T9 and any allow-list rule): the realistic mutant of an allow-list filter is a deny-list of exactly
  the values the test data contains; demand a witness with a value outside the test's own set, or record the pin that
  prevents one.
- FF-T5-c (from T5; implDirective and qaDirective of T6 to T13 for every file created or replaced under
  `S/records/evidence/`): run scripts mask sandbox, scratchpad, worktree and home paths while writing (one placeholder
  such as `$SBX`); the verifier runs the C0-10 leak grep on every evidence file created or replaced in each round,
  with a positive control on the raw run.
- FU-T5-1 (from T5, decision J14, residual for Maik and Gate 2): an AUTHOR-or-EDITOR filter or a deny-list filter
  (`!TRANSLATOR && !ILLUSTRATOR`) survives the Level 2 reference test, because the only historical multi-role title has
  no EDITOR; killing it needs a second contract (for example 9789900020264) and test, beyond the pack's one small
  reference test; qa-final probes the deny-list mutant.
- FF-T6-a (from T6; QA directives of T7 to T13): any rehearsal row whose order, measurement or failure column depends
  on when Claude Code starts or loads something (LSP server, MCP server, plugin, hook, skill, settings reload) cites
  the documented trigger in an evidence file and gives a branch for when the trigger differs; verifiers cross-check
  it against res-1 and the fetched docs pages.
- FF-T6-b (from T6): evidence-file edits are insertion only, modulo trailing whitespace (the root `.editorconfig`
  trims it); verifiers prove insertion with a trailing-whitespace-insensitive compare.
- FF-T6-c (from T6; every rehearsal row with a branch or a timing): a branch that pulls in a step from another row
  states the tree state it leaves and how the next row's premise is restored; every measured time names its start
  event, stop event and what to record when the first answer is "started but not ready"; a retry rule says which
  answer the failure judgement uses; verifiers dry-run each branch through the next row in the suggested order.
- FU-T6-1 (from the T6 r2 meta-review, folded into T7, amendment A3): three sentences of the Level 2 rows: (1) an
  empty first `LSP` answer (failure (d)) is judged on the second answer, with clock times t0 to t3 and the cold time
  reported as a range that includes the wait, never t1-t0 alone; (2) in the "No server running yet" branch, R2-2
  prompt 1 is not sent again, R2-2 continues with prompt 2, then the tree is reset before R2-4 Measure 2 (b); (3) the
  warm comparison repeats the prompt that started the server and records which one; plus the optional extra source
  for N7 (the tools page sentence on LSP calls whose server cannot start) and the long-line nit.
- FF-T7-a (from T7; T8, T10, the guide hand-off): with `"onFailure": "block"`, Claude Code 2.1.295 or later blocks
  the commit when the commit hook cannot start, times out, or exits with any code other than 0 and 2; T8's harness
  still asserts the verbatim script's own exit codes, but no T8 file may say a non-2 exit lets the commit through;
  the Windows note says a missing Git Bash or interpreter blocks every `git commit` through Claude; the Stop handler
  has no `onFailure`, so its non-2 exits do not block; T10's red-build and escape-hatch rows expect a block for exit 1
  and for a timeout and check the amended J15.
- FF-T7-b (from T7; every remaining task writing non-verbatim files): every universal or descriptive claim about the
  code gets a `git grep` counterexample search on `level-0-start` with a control; a counterexample is a FAIL in the
  task's own file.
- FF-T7-c (from T7; rehearsal rows in T10 to T13): when a component is started by an edit, its "started but not
  ready" form is a missing report under that edit; the row says what to record, how it differs from "never
  started", and whether the follow-up prompt is still sent.
- FF-T7-d (from T7): a heading, column header or section label is a scope label, judged by the file's own cells;
  it is a finding only when an item under it prescribes something false, it contradicts the declared source, or the
  label itself states a fact a `git grep` falsifies.
- FF-T7-e (from T7): when an authored file names a source of truth, every departure from it follows the code or the
  evidence, is marked in the trace ("code only" or the evidence line), and is reported as a product finding against
  the source; the file is never reworded back to a false source. Verifiers write per-round hash files
  (`t<n>-qa-r<k>-hashes.txt`) over every fileScope file.
- FF-T8-a (from T8; T9, T10): a claim that two implementations behave the same is checked under participant
  defaults (UTF-8 locale, Git Bash) or limited in the text ("for ASCII input", "in the harness").
- FF-T8-b (from T8; T10, qa-final): wherever the T8 harness is cited as proof that a red build blocks Claude's
  commit, cite `tbc-chained-git-add-and-git-commit` next to `tbc-commit-red`; every commit-hook rehearsal row runs one
  plain and one chained commit (`git add -A && git commit -m x`) and lists PF-T8-1's quoted-path form
  (`cd "<folder>" && git commit`) as the known exit-0 bypass.
- FF-T8-c (from T8; verifiers of T9 to T13): probe each quantified part of a regex, glob or matcher widened and
  deleted, and include the input form the real caller sends as a case that must hold.
- Product findings from T8 for Maik: PF-T8-1 a quote before `git commit` in the command (`cd "<folder>" && git
  commit`) makes the pack commit hook exit 0 without the build; PF-T8-2 the docs and D9 say that without Git Bash the
  commit hook never fires (no gate at all), so Windows needs Git Bash; PF-T8-3 the Stop hook skips its check when
  `git ls-files` fails; J16 and J17 in the run ledger. S/README.md does not yet list `tests/test-hooks.sh` (qa-final).
- Product findings from T7 for Maik: PF-QA-T7-1 `onFailure: "block"` also blocks on unexpected exit codes, so the
  pack hook comment (pack line 283, "Any other non-zero code does NOT block") and guide
  `04-level-3-behavioral-control.mdx` line 105 are stale for the commit hook; the public schemastore settings schema
  lacks `onFailure` (additionalProperties false), so a schema-validating editor flags it; PF-QA-T7-3
  `docs/architecture.md` lines 65-66 on `main` and factory say a price is "a `BigDecimal` plus a currency code outside"
  the domain, false for payloads and views (decimal strings); the staged reference.md follows the code and marks the
  departure.
- Product findings from T6 for Maik: PF-T6-1 JDK floor 17 also in guide `00-setup-check.md` line 92; PF-T6-2 guide
  `03-level-2-give-your-agent-eyes.mdx` line 124 and `reference/cheat-sheet.md` line 14 use `claude mcp add` (literal
  URL); PF-T6-3 spec 10 lines 182-183 "Built in Claude Code" stale under D14; PF-QA-T6-1 spec 05 line 159 lacks
  `--scope project`; guide order: the workshop plan Part A and guide page 03 ask for references before the first edit,
  while the docs start the server at the first edit.
- Product findings from T4 for Maik: PF-T4-1 the JDK floor reads "JDK 17 or later" in spec 10 line 168 and in
  workshop-plan line 59 (J8 and `docs/lsp-setup.md`: 21); PF-T4-2 workshop-plan line 207 `claude mcp add` writes a
  literal URL while the staged `.mcp.json` uses `DISTRIBUTOR_MCP_URL` (name, transport and port agree).
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
- A2 (2026-11-09, orchestrator, from Maik's decision D14 on factory c35037f, merged into the spec branch; answers
  open question Q1): no agent runs Claude Code for spec 10 at all (no login, no setup-token, no real HOME, no
  headless session); J5 (`brew install jdtls`) and the headless parts of J7 lapse. Where a task text above says "run",
  "headless", "session", "transcript" or "rehearse", A2 wins:
  - Every scenario run becomes a careful consistency check, recorded in `S/records/level-<n>.md`: every path,
    command, skill, hook, agent, setting key, MCP server and tool name, env variable, port and plugin id a staged
    file or a scenario names exists on the built sandbox tree or on `origin/factory` at the recorded sha, and is
    spelled the same in pack spec 10, spec 11 and its guide sources (`origin/sdd/11-workshop-guide/spec`,
    read-only), the staged files and the code. A mismatch in the task's own files is a FAIL; one in pack or guide
    text is a product finding for Maik. Everything provable without a model still runs (harness, curl, generators,
    validators, glob tests, `./mvnw clean verify` per level).
  - Scenarios that need a Claude Code session, the macOS LSP rehearsal and "Rehearsed in time" become rows of one
    rehearsal checklist for Maik's rehearsal on 2026-11-10, `S/records/rehearsal-checklist.md`: per row the branch,
    setup, exact prompt or command, expected observation, what counts as failure and the pack fallback, checked word
    by word against the pack scenario. T3 creates the file with the Level 1 rows; T6, T10, T11, T12, T13 add their
    levels; T13 adds the closing list of directly written files.
  - T3: Level 1 consistency check (CLAUDE.md and rules against the code: commands, class names, packages, the
    `Money` rule, migration folder and naming, `PostgresTestConfig`, `ScriptedChatModel`, `@MockitoBean`,
    `MockRestServiceServer`, `DistributorAppService`); CLAUDE.md has no `@` import; a glob test proves each rule's
    `paths` matches only its intended folder (with a widened-glob control); FF-T2-a stays; rehearsal rows for "Rules
    load on demand" and "Level 0 task with context".
  - T5: the multi-role contract test is built directly through the normal TDD flow; the payload is captured with
    curl from a distributor jar on a free port, via REST `GET /api/titles/{ean}` and MCP `tools/call get_title`, and
    the contract file is byte-identical to both; the "built in Claude Code" provenance is dropped.
  - T6: Level 2 consistency check (`.mcp.json` server name, transport, URL and port against the distributor config and
    the guide; curl `initialize` plus `tools/list` shows exactly the four tools; the plugin id and marketplace repo
    against `docs/lsp-setup.md` and the guide; `Money.of` used in at least two layers; `Book.price()` exists as the
    guide's type-error step says); rehearsal rows for MCP, the LSP scenarios (first-index time, multi-module result,
    type error, find references) and "Plugin loads from the project declaration" (with FF-T4-a: the pack fallback also
    removes the plugin lines of `docs/lsp-setup.md` and the plugin keys of the level-3 settings file).
  - T7: the D9 `onFailure` field is checked against the official Claude Code documentation and settings schema, not
    by a run.
  - T9: cancel order is a normal critical TDD task through the `hexagonal-slice` skill; new classes follow the staged
    `reference.md` rows; the sandbox commit is made with `core.hooksPath .githooks` and its git hook run is green; the
    skill-selection and `/ship` provenance is dropped (both become rehearsal rows in T10).
  - T10: Level 3 consistency check (settings hook paths and modes, permission rule spelling, skill name, description
    and `allowed-tools`, `/ship` allowed tools against the permissions, the agent file, the git hook activation
    command in the guide); hook behaviour is cited from the T8 harness (re-run); a measured cold `./mvnw -q verify`
    time is compared with the hook timeout 180; rehearsal rows for red build, escape hatch (including the variants the
    deny rules may miss: `git commit -m "x" -n`, `git -C . push`, `cat .env` through Bash), secrets, skill selection,
    `/ship`, and the Windows runs.
  - T11: unchanged (CLI only, HR1 isolation), plus its rehearsal rows.
  - T12, T13: CLI-only init steps (`specify init`, the Agent OS install script) still run under HR1 isolation; the
    files a Claude Code step would generate (the Spec Kit constitution, the Agent OS `/plan-product` output, the
    discovered and indexed standards) are written directly from the pack's product docs, carry no placeholders, trace
    every statement to a pack line and never contradict the architecture standard; each is listed for Maik's review.
  - QA follows the QA lead's "QA strategy changes under D14" (C0-13 for every re-planned task); Gate 2 reads every
    rehearsal-only scenario as an unproven residual until Maik rehearses.
  Gate 1 re-run and self-accepted: the scope narrows, no task is added, the per-task file scopes are unchanged except
  that `S/records/rehearsal-checklist.md` joins the file scope of T3, T6, T10, T11, T12 and T13.
- A3 (2026-11-10, orchestrator, from the T6 r2 meta-review): FU-T6-1 is folded into T7, whose file scope gains
  `S/records/rehearsal-checklist.md` for the three Level 2 sentences only (and the two optional items); Maik rehearses
  on 2026-11-10, so the fix ships with the next task instead of T10. Gate 1 re-run and self-accepted: one docs-only
  addition to T7, no new task.
- A4 (2026-11-10, coordinator: `main` and tag `level-0-start` published at b668073): after T13 and a merge of
  `origin/factory` into the spec branch, PHASE cut: task T14 (orchestrator) runs `cut-level-branches.sh level-0-start`
  (dry run, then `--execute`), pushes the six new branches `solution/level-1` to `solution/level-3`,
  `start/level-4-openspec`, `start/level-4-speckit`, `start/level-4-agentos` (never touching `main` or
  `level-0-start`; SSH where workflow files are involved), a verifier runs `./mvnw clean verify` on each branch and
  checks each branch tree against build-level; then qa-final and Gate 2, stopping with the spec PR ready (merge:
  false). From T8 on, sandboxes are built from `level-0-start`. Gate 1 re-run and self-accepted.
- A5 (2026-11-10, coordinator: parallel Level 4 tracks): after T10 merges, T11, T12 and T13 run in parallel on their
  own task branches in separate git worktrees and merge one after the other; each writes its own record
  `S/records/level-4-<track>.md` (consistency check plus its rehearsal rows) instead of a section of one shared file;
  the Level 4 section of `S/records/rehearsal-checklist.md` (pointers plus the "Rehearsed in time" rows) is added only
  by the last track to merge, after merging the spec branch into it. Gate 1 re-run and self-accepted.
- J15 (amended after T7 QA, PF-QA-T7-2): if Maik's rehearsal triggers the pack's plugin fallback, the level-2
  `.claude/settings.json` add row and the two plugin lines of `docs/lsp-setup.md` go, the level-3 settings file drops
  the two plugin keys (one Write, HR2), AND level-3 manifest row 2 flips from `replace` to `add`, otherwise
  build-level.sh level-3 fails.
- A6 (2026-11-10, from Maik's decision D16 on factory 7ba87a9, "Skip the powershell stuff"): every PowerShell part is
  dropped from T8 and all later tasks: no `.ps1` hook variants, no PowerShell parity cases, fixtures, mutants or
  Windows PowerShell notes. Level 3 ships `tests-before-commit.sh`, `architecture-check-on-stop.sh` and
  `.githooks/pre-commit` only; Windows participants run them through Git Bash (D9). `S/tests/test-hooks.sh` keeps only
  the bash/sh cases (green on bash 5.3 and macOS /bin/bash 3.2); `S/records/windows-hooks.md` keeps only the Git Bash
  prerequisite, the D9/onFailure facts and the Git Bash checks for Maik. T8's QA findings RA6, RA7, RA-r3-1 to RA-r3-3,
  PF-QA-T8-4 and R-T8-1 are struck as moot; T8's last round is bounded to the removal (sh hooks and git hook
  unchanged, harness green, no other drift). Gate 1 re-run and self-accepted: the scope narrows.
- A7 (2026-11-10, orchestrator, from the T8 r3 meta-review, decision J18): one closed test-only round r4 for T8
  after the D16 removal round: two fixtures and two harness cases (`git add -A && git commit -am wip` must be blocked;
  `ls legitcommits` must not run the build), the evidence refreshed; hooks, pre-commit, manifest and the note stay
  byte-unchanged. Reason: r3 was redirected by D16 from the escalated defect round to the removal, the gap (no text
  before `git commit` in any fixture) lets the most common chained commit through a broken hook, and the fix is two
  proven cases; a later fix task would leave T10 citing an incomplete harness. Gate 1 re-run and self-accepted.
- HR2 (2026-11-09, coordinator, Maik's global plugin guard): each staged settings file that declares the plugin (T4,
  T7) is written once, in one Write of its final content, so Maik sees a single prompt; no workaround of the guard;
  plugin scenarios use the isolated HOME and `--plugin-dir`; a denied `claude plugin install` in the isolated HOME
  turns that scenario into a manual check.
