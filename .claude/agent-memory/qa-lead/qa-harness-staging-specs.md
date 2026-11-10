---
name: qa-harness-staging-specs
description: Strategy traps for specs that stage agent config (CLAUDE.md, .claude/, hooks) under docs/ and verify it with headless Claude Code sessions; ignore rules, eol attributes, re-run budget, isolation evidence, no-run consistency checks, rehearsal-row reset and judging, lifecycle premises (LSP starts on first edit), exercise reference tests, staged doc rows, locale-bound parity twins, gate-regex input forms (chained commands) and row literals measured with a variant command
metadata:
  type: feedback
---

Traps found while writing the spec 10 strategy (harness reference solutions staged under `docs/spec-pack/level-branches/`). Apply to any spec that
stages files for other branches or proves behaviour with `claude -p` runs. Harness probe techniques themselves live in the senior-research-agent
memory; do not copy them.

1. **Root ignore patterns bite at any depth.** The root `.gitignore` has `notes/`, `*.log`, `target/`, `.env*`, `dist/`: a planned `S/notes/x.md` or
   an evidence `*-debug.log` is silently never committed (the task passes locally, the file is missing from the PR). At strategy time run `git
   check-ignore -v --no-index` on every planned fileScope folder and evidence name and put "`git status --porcelain --ignored -- S` shows no `!!`"
   into the common block.
2. **"CRLF kept" is undefined in git.** Check `git ls-files --eol` on a fresh clone plus `od -c`; a renamed folder (`dot-githooks`) no longer matches
   the root rule `.githooks/*`: check `git check-attr` on staged paths.
3. **Re-run budget for headless scenario tasks.** Verifiers re-run every zero-token check (sandbox build and verify, bash harnesses, `claude -p
   "/context"`, `/mcp` init, generator inits, validators) and audit expensive sessions from jsonl, transcript and kept sandboxes; one capped spot
   re-run (`--max-budget-usd`, `--max-turns`) per task. Demand an implDirective to keep the run sandboxes until the gate, or the audit has nothing to
   judge.
4. **Isolation and user-scope evidence per run.** Init event (`apiKeySource`, skills, agents, mcp_servers, plugins) plus sha256 of the user settings
   and plugin registry files before and after. A probe once wrote user settings through `claude plugin marketplace add`: ban `claude plugin`, `claude
   mcp add`, `--bare` in every script.
5. **Honest N-of-M.** Report all runs started; replace one only for a quoted infra error; judge verify outside Claude.
6. **Judge a scenario claim by stream order.** "LSP reported the error" fails if `./mvnw` precedes the diagnostic; "permission denied" needs the
   `permission_denied` event (`result.permission_denials` also lists hook blocks).
7. **Time-dependent paths in staged docs.** Exemplary paths and patch bases are checked against the simulated base at a recorded sha and listed for
   re-check at the real cut. Verbatim staged text naming a not-yet-existing path is no FAIL of the staging task (verbatim is the criterion): route it
   as a product finding and make the scenario task exercising the text record the absence and the re-check.
8. **"Refs unchanged" in a shared repo.** Parallel spec runs move `sdd/*` refs and worktree HEADs during any verifier run, so a whole-repo
   `for-each-ref` before/after diff is never clean. Write "no ref the scripts can name exists" (branch and tag names from the step library) plus the
   suite's own ref checks on TMPDIR repos; a moved `sdd/<other spec>` ref is then attributable, not a finding. Once the base tag is
   published, compare built trees on the REAL tag: a simulate from a later source drifts by source-only changes (spec 10 T14: one `.gitignore` line).
9. **When agents may not run Claude Code at all** (owner decision mid-cycle), re-plan verification three ways: (a) zero-token behaviour still executed
   (bash harnesses, curl JSON-RPC `initialize`/`tools/list` for MCP, generator inits, validators, glob tests of rule `paths`, `./mvnw verify`); (b)
   static consistency by the verifier's OWN token inventory (paths, commands, skills, hooks, settings keys, server and tool names, ports) resolved on
   the built tree and spelled the same in spec, guide sources and code, with a misspelling negative control; (c) owner rehearsal rows with exact
   setup, prompt, expected observation and fallback. Isolation checks reading `~/.claude` become n/a (a "no claude invocation" grep replaces them).
   **Why:** without (a) verifiers drift into pure reading; without (c) Gate 2 reads unproven scenarios as passed. **How to apply:** static matching of
   permission rules is consistency, not behaviour; rehearsal rows must carry position variants a prefix rule misses (`git commit -m x -n`, `git -C .
   push`, `cat .env` via Bash).
10. **Rehearsal rows must produce the state they premise.** Verifiers check row wording and tokens, not whether the setup, reset and judge commands
    give "fresh" runs. On one clone, `git restore . && git clean -fd` keeps the ignored `target/` (a stale `target/classes/db/migration/V5__*.sql`
    reaches the next run's Flyway classpath) and any commit (a CLAUDE.md asking for "small commits" invites them); working-tree-only judging then
    misreads a committed run. Require a recorded start sha, `git reset --hard <start> && git clean -fdx` (or a fresh clone), judging against `<start>`
    and `./mvnw clean verify` (C0-13e). A third witness lives outside the repo: Claude Code's per-project auto memory, keyed by the clone path,
    survives any reset in the same folder: require a new clone folder per run or a `/memory` check before each run. List all three witnesses up front
    (missing the third in r1 left only a follow-up in the bounded round).
11. **Staged exercise reference tests over existing production (characterization tests).** A "red" from a missing contract file proves nothing; the
    binding proof is a role-mutant ladder plus a control showing the pre-task suite lets the main mutant live. At strategy time: (a) for an allow-list
    rule (`"AUTHOR".equals`) list the deny-list of values the data happens to hold as a falsifier and a witness value outside that set (EDITOR), and
    check the seed for a title carrying it BEFORE the card pins "one contract file" (no historical title had EDITOR, so the twin could only go to rule
    4); (b) when the task adds a row to a participant-facing provenance doc, direct the verifier to re-check every section claim for the new row
    ("captured on that day unless the table says otherwise", run-naming headings, column headers, exhaustive enumerations): verifiers rate such
    contradictions as nits.
    Judge a heading qualifier ("run 1 unless noted, no flags") by the file's own convention: a sibling heading whose noted row carries the same kind
    of deviation (a chaos flag) shows "unless noted" covers the whole parenthesis. A rework replacing an evidence file with a fresh raw run re-leaks
    sandbox paths: mask while writing, re-grep. The nit-rating also hits NEW authored files: a universal claim about the code ("every end to end test
    is named `*IntegrationTest`") falls to one `git grep` on the base; a prescriptive rule from the standard is fine, the descriptive "every" is not:
    direct verifiers to counterexample-search every "every/all/only/never". Scope labels are no claims: a heading over imperative bullets each
    carrying its own trigger, or a column whose cells mix layer and library terms, is read by the file's convention. A rule reworded to match the code
    can contradict the file's declared source of truth: route a product finding against the source, never reword back. Demand a per-round hash file of
    changed files. Mirror sites reach past the module: grep the sibling endpoint repo-wide (README lists, `http/*.http`).
12. **Lifecycle premises in rehearsal rows.** Rows state "added here" facts about WHEN Claude Code starts something (the first references request is
    the LSP cold start) and nobody checks them. Claude Code's docs say the language server starts the first time Claude EDITS a file of the plugin's
    extension (code-intelligence page, "Confirm the server starts"); a headless probe saw the same. A read-only first request may meet no server and
    fail as "text search". At strategy time require every row's lifecycle premise (LSP, MCP, plugin, hook, skill load) to be sourced in an evidence
    file with a branch for the other behaviour, and the measurement to start at the event that really starts the component.
    When I write such a branch as an RA, dry-run it through the NEXT row of the suggested order: a branch pulling an edit forward leaves the tree
    changed under a later "unchanged tree" premise, and a "started but not ready" first answer (empty while indexing) needs its own stop event and
    counting answer. A component started BY an edit has a second "not ready" form: no report under that edit (no `Found N` line), so the stopwatch has
    no stop event and a "not running" failure label misjudges a running server. List both forms (empty answer, missing report) and the "never started"
    case in every timing branch.
13. **Parity twins of shell tools are locale-bound.** A twin of `grep -E '[[:space:]]'` written as `[ \t\v\f\r]` agrees only in the C locale; in UTF-8
    (participant default) grep also matches U+3000 raw in a JSON string. Name the UTF-8 witness or require the claim scoped. Probe with
    `/usr/bin/grep` (interactive `grep` can be a shell function). Any rework of a staged file changes blob ids in evidence: put the evidence refresh
    into the RA list.
14. **Gate regexes need the caller's real input form.** A hook grep `"command"...:"[^"]*git[[:space:]]+commit` was probed for every whitespace member
    over three rounds, yet every positive fixture started with `git`, so the deleted prefix (`"git`, a start-anchored hook) passed 68/68 and lets `git
    add -A && git commit` (how Claude usually commits) through. At strategy time list the chained form as a must-block case and a word-internal
    `gitcommit` as must-pass, and probe each quantified part widened AND deleted. Found in a bounded round with the task open: REJECT, closed
    test-only RA (protected property; not the narrowing loop of [[review-calibration]] rule 23). Bound "insert these lines after line N" as `diff
    --minimal` plus a head/block/tail split check.
15. **Row literals measured with another command.** Dry runs count with `git status --porcelain -uall`, rows print plain `git status --short`, which
    folds a new untracked folder into one line (29 files -> 10 lines). In every row review run the printed command verbatim and compare with the
    literal; reject premises measured by a variant.
    Rework bounds on evidence: line-cited files stay insertion only; an uncited log may be regenerated if declared and every check re-run (grep
    `<file>:[0-9]` first). A cited file regenerated without a copy: revert the declared difference in memory and hash; equal = byte proof. A census
    check with a hard-coded expected list is circular.
    Rows copied into a checklist: direct a field-by-field carry incl. observations and "not a failure" clauses.


