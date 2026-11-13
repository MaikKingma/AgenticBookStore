---
name: bash-script-qa-probes
description: Bash tooling and self-test verification - copy-based mutants, xtrace coverage, rollback witnesses
metadata:
  type: feedback
---
Rule: for a bash self-test, mutate COPIES of the script folder in the scratchpad, never the repo, and measure coverage with xtrace before calling the probe list complete.
**Why:** the ledger's named mutants all died, but the coverage trace showed refusal lines that no case reached. One mutant on such a line silently let agent config through with exit 0. A suite that kills only the named mutants does not cover "every refusal path".
**How to apply:**
- Use a python `-I` generator with id -> (file, old, new) and an exact-once check. Per mutant: `cp -Rp` the snapshot into `SP/mut/<id>/<same relative path>` (keep sibling files the test reads, for example a spec file), run the suite with TMPDIR in SP, and record the `not ok` lines. Run with `xargs -P 6` (30 mutants in a few minutes). Add a CONTROL id that changes nothing: a green CONTROL from a copy proves that the test finds its scripts relative to itself.
- Coverage without kcov: a `BASH_ENV` file that opens `exec {fd}>>trace.$$.txt`, sets `BASH_XTRACEFD`, `PS4='+${BASH_SOURCE[0]}:${LINENO}: '` and `set -x`. Every `bash script` the test starts inherits it (needs bash 4.1 or later on PATH). Parse `+...:/scripts/<name>.sh:<line>:`. Multi-line commands and heredoc terminators show up as false "missed" lines: read the missed list by hand.
- From the never-reached die/line_die lines, build a second "gap" batch (remove each check). Each survivor needs a witness, the same input on the original and on the mutant copy, to tell harmful (silent success, line number lost) apart from equivalent (refused later by accident).
- Run the README "as written" in a scratch clone of the worktree (`git clone <worktree>`, then point `refs/remotes/origin/<base>` at the real sha), with the staging folder copied in and FIXTURE step folders. Use a scratch bare remote for fetch and push steps. Keep HOME real for Maven and isolate git only with `GIT_CONFIG_GLOBAL` and `GIT_CONFIG_NOSYSTEM`.
- Rollback under `set -e`: a failing command inside an EXIT-trap rollback aborts the rest of the handler silently (index restore, folder cleanup, backup removal, the "rolled back" line). Witness on the UNMUTATED script: an earlier row that stages (patch via `--3way`), then a write onto a `chmod 444` file. Check `git status`, empty folders and leftover backup folders. A mutant leaving a temp folder behind is a hint to look for this.
- "Deleting `set +e` in the trap is equivalent, every command is guarded" is usually false: the `echo ... >&2` status lines are unguarded. Witness: run the failing case with stderr closed (`2>&-`); the mutant aborts the trap and leaks its backup/temp folder (or turns exit 0 into 1). Judge claimed-equivalent mutants with a witness, never by reading.
Related: [[shell-traps]], [[env-switch-mutant-runner]], [[readme-and-live-jar-probes]], [[bash-script-test-probes]]
