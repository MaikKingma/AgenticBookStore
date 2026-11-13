---
name: bash-script-test-probes
description: Bash and git publish scripts - interpreter trap, mutant dirs, push witnesses, look-alike path probes
applies-to:
  - scripts/**
  - "*.sh"
metadata:
  type: feedback
---
Rule: before accepting "green on macOS /bin/bash 3.2", find out which bash runs the script under test, not only the test.
**Why:** a test calling `bash "$SCRIPT"` resolves bash from PATH (Homebrew 5.x here), so `/bin/bash test.sh` ran the script under bash 5; bash-4-only mutants (`${x,,}`, an empty `"${a[@]}"` under `set -u`) passed the test, CI (ubuntu, bash 5) and shellcheck.
**How to apply:**
- Prove the interpreter with a logging `bash` shim first on `PATH=<shim>:/bin:/usr/bin:/usr/sbin:/sbin` (echo `$BASH_VERSION` to a log, then `exec /bin/bash "$@"`) and count invocations. A summary line printing `$BASH` names the test's interpreter, not the script's (a test copy with the fix reverted still printed `/bin/bash 3.2`): exec the PATH bash, expect 0 shim calls. Ask for `"$BASH" "$SCRIPT"` in the test and show the fix bites with the bash-4 mutants under the default PATH.
- Mutants: a `python3 -I` generator with exact replacements from `.orig` (abort unless each old string occurs once); each mutant gets its own directory with the script, a test copy (it finds the script via `dirname $0`) and its own `TMPDIR`; 5 parallel batches; never let two batches share a probe directory (the runner `rm -rf`s TMPDIR). Map each look-alike assertion to its killing mutant by number; one identity mutant (unmutated copy) proves the harness is green.
- Rework rounds: for each fixed gap, a "fix reverted" test variant next to the mutant it targets (reverted `"$BASH"` + bash-4 mutant, removed new refusal + its mutant); the mutant must survive the variant, attributing the kill to the new test lines.
- Push witnesses: an `update` hook in the bare remote that rejects the tag (atomic push); a PATH `git` shim that moves the remote ref right before `push` (lease); `GIT_TRACE=1` greps for `built-in: git push|ls-remote` (exact argv, "no network in default mode", `--push` trace as positive control). A hostile-config positive control with a failing `reference-transaction` hook breaks even `git init`; that still proves the config bites.
- Precedence ("remote baseline wins over a local one"): besides the order swap, date-preference mutants (prefer newer / older by `%ct`); one date order kills the swap and one date mutant but not the other: both orders (local newer and older than remote) are needed. Also tag-first, tag-agrees and object-id-order (`[[ a < b ]]`: two cases with different SHAs pin it). A setup check ("dated before", "differs from") gets its own variant probe (flip the date, alias the commit), else fixture drift silently turns the case vacuous. Prove with a QA-only test variant in SP, never the project file.
Related: [[bash-script-qa-probes]], [[ci-workflow-verification]], [[shell-traps]]
