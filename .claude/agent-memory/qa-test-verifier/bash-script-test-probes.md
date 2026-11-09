---
name: bash-script-test-probes
description: Verifying bash scripts and their bash tests (git publish scripts) - interpreter trap, mutant generator, trace and lease witnesses, CI startup errors
metadata:
  type: feedback
---

Rule: before you accept a "green on macOS /bin/bash 3.2" claim, find out which bash runs the script under test, not only which bash runs the test.

**Why:** a test that calls `bash "$SCRIPT"` resolves bash from PATH. Here that is Homebrew 5.x, so `/bin/bash test.sh` ran the script under bash 5. Bash-4-only mutants (`${x,,}`, an empty `"${a[@]}"` under `set -u`) passed the test, CI (ubuntu, bash 5) and shellcheck.
**How to apply:**
- Prove the interpreter with a logging `bash` shim first on `PATH=<shim>:/bin:/usr/bin:/usr/sbin:/sbin`. The shim echoes `$BASH_VERSION` to a log and then runs `exec /bin/bash "$@"`. Count the invocations.
- Ask for `"$BASH" "$SCRIPT"` in the test. Show that the fix bites: run the same bash-4 mutants with the fixed test copy under the default PATH.
- Mutants: write a `python3 -I` generator that makes exact replacements from a `.orig` copy (abort unless each old string occurs once). Give every mutant its own directory with the script, a copy of the test (it finds the script via `dirname $0`) and its own `TMPDIR`. Run 5 parallel batches of about 20 s per run.
- A summary line that prints `$BASH` names the test's interpreter, not the script's: a test copy with the fix reverted still printed `/bin/bash 3.2`. Prove the script's interpreter with the shim (exec the PATH bash, expect 0 calls), not with the summary.
- Rework rounds: for each fixed gap, build a "fix reverted" test variant next to the mutant it targets (reverted `"$BASH"` + bash-4 mutant, removed new refusal + M5c). The variant must let the mutant survive; that attributes the kill to the new test lines.
- Never let two batches share a probe directory. The runner `rm -rf`s the TMPDIR and clobbers the other batch's run. Copy the directory under a new id instead.
- Idempotency of `commit-tree` (same tree, message and dates give the same SHA): prove the date-variation witness with a negative control. A test copy with pinned dates must let the "always commit" mutant survive.
- Push witnesses: an `update` hook in the bare remote that rejects the tag (atomic push). A PATH `git` shim that moves the remote ref right before `push` (lease). `GIT_TRACE=1` greps for `built-in: git push|ls-remote` (exact argv, plus "no network in default mode", with the `--push` trace as positive control).
- A hostile-config positive control with a failing `reference-transaction` hook breaks even `git init`. That still proves the config bites.
- actionlint has no `-color=never`; use `-no-color`. The image bundles shellcheck, so `run:` blocks are linted too.
- A run with only some jobs created and conclusion `failure`: the jobs API shows nothing. Download the public run page and grep the annotation (`Internal server error. Correlation ID`). Compare with other branches' runs in the same window before you blame the workflow file.

Related: [[shell-traps]], [[ci-workflow-verification]], [[env-switch-mutant-runner]]
