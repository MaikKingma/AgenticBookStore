---
name: qa-harness-staging-specs
description: Strategy traps for specs that stage agent config (CLAUDE.md, .claude/, hooks) under docs/ and verify it with headless Claude Code sessions; ignore rules, eol attributes, re-run budget, isolation evidence, no-run consistency checks, rehearsal-row reset and judging
metadata:
  type: feedback
---

Traps found while writing the spec 10 strategy (harness reference solutions staged under
`docs/spec-pack/level-branches/`). Apply to any spec that stages files for other branches or proves behaviour with
`claude -p` runs. Harness probe techniques themselves live in the senior-research-agent memory; do not copy them.

1. **Root ignore patterns bite at any depth.** The root `.gitignore` has `notes/`, `*.log`, `target/`, `.env*`,
   `dist/`: a planned `S/notes/x.md` or an evidence `*-debug.log` is silently never committed. At strategy time run
   `git check-ignore -v --no-index` on every planned fileScope folder and evidence name, and put
   "`git status --porcelain --ignored -- S` shows no `!!`" into the common block.
   **Why:** the task passes locally and the file is missing from the PR.

2. **"CRLF kept" is undefined in git.** `text eol=crlf` stores LF blobs. Write the criterion as `git ls-files --eol`
   on a fresh clone (`i/lf w/crlf attr/text eol=crlf`) plus `od -c` of the worktree bytes. A renamed folder
   (`dot-githooks`) no longer matches the root rule `.githooks/*`: check `git check-attr` on staged paths.

3. **Re-run budget for headless scenario tasks.** Verifiers re-run every zero-token check (sandbox build and verify,
   bash harnesses, `claude -p "/context"`, `/mcp` init, generator inits, validators) and audit expensive sessions
   from jsonl, transcript and kept sandboxes; one capped spot re-run (`--max-budget-usd`, `--max-turns`) per task.
   Demand an implDirective to keep the run sandboxes until the gate, or the audit has nothing to judge.

4. **Isolation and user-scope evidence per run.** Init event (`apiKeySource`, skills, agents, mcp_servers, plugins)
   plus sha256 of the user settings and plugin registry files before and after. A probe once wrote user settings
   through `claude plugin marketplace add`; ban `claude plugin`, `claude mcp add`, `--bare` in every script.

5. **Honest N-of-M.** A model-behaviour scenario (skill selection, Level 0 task success) is reported with all runs
   started; a run is replaceable only for a quoted infrastructure error. Judge "verify green" outside Claude.

6. **Judge a scenario claim by stream order.** "LSP reported the error" fails if a Bash `./mvnw` call precedes the
   diagnostic; "permission denied" needs the `permission_denied` event, because `result.permission_denials` also
   lists hook blocks; a `--settings` stand-in file must hold only what it stands in for.

7. **Time-dependent paths in staged docs.** Exemplary paths and patch bases are checked against the simulated base
   at a recorded sha, and listed for re-check at the real cut (later specs may add or change them). Verbatim staged
   text naming a not-yet-existing path is no FAIL of the staging task (verbatim is the criterion); route it as a
   product finding and make the scenario task that exercises the text record the absence and the re-check.

8. **"Refs unchanged" in a shared repo.** Parallel spec runs move `sdd/*` refs and worktree HEADs during any
   verifier run, so a whole-repo `for-each-ref` before/after diff is never clean. Write the check as "no ref the
   scripts can name exists" (the branch and tag names from the step library) plus the suite's own ref checks on
   TMPDIR repos; then a moved `sdd/<other spec>` ref is attributable, not a finding.

9. **When agents may not run Claude Code at all** (owner decision mid-cycle). Re-plan verification three ways:
   (a) zero-token behaviour still executed (bash harnesses, curl JSON-RPC `initialize`/`tools/list` for MCP, generator
   inits, validators, glob tests of rule `paths`, `./mvnw verify`); (b) static consistency by the verifier's OWN
   token inventory (paths, commands, skills, hooks, settings keys, server and tool names, ports) resolved on the built
   tree and spelled the same in spec, guide sources and code, with a misspelling negative control; (c) owner rehearsal
   rows with exact setup, prompt, expected observation and fallback. Isolation checks that read `~/.claude` become
   n/a (a "no claude invocation" grep replaces them).
   **Why:** without (a) verifiers drift into pure reading; without (c) Gate 2 reads unproven scenarios as passed.
   **How to apply:** static matching of permission rules is consistency, not behaviour; the rehearsal rows must
   carry position variants a prefix rule misses (`git commit -m x -n`, `git -C . push`, `cat .env` via Bash).

10. **Rehearsal rows must produce the state they premise.** Verifiers check row wording and tokens, not whether the
   row's setup, reset and judge commands give "fresh" runs. On one clone, `git restore . && git clean -fd` keeps the
   ignored `target/` (a stale `target/classes/db/migration/V5__*.sql` reaches the next run's Flyway classpath) and
   any commit (a CLAUDE.md asking for "small commits" invites them); working-tree-only judging then misreads a
   committed run. Require a recorded start sha, `git reset --hard <start> && git clean -fdx` (or a fresh clone),
   judging against `<start>`, and `./mvnw clean verify`. Put this in C0-13e at strategy time (spec 10 T3 r1).
   A third witness lives outside the repo: Claude Code's per-project auto memory, keyed by the clone path, survives
   any reset in the same folder; require a new clone folder per run or a `/memory` check before each run. I missed it
   in r1, found it in the bounded r2 and could only route it as a follow-up: list all three witnesses up front.
