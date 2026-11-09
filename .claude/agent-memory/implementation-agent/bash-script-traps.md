---
name: bash-script-traps
description: Traps when writing and testing the bash tooling of this repo (docs/spec-pack/level-branches): errexit lost in || contexts, bash 3.2 on macOS, git apply --3way, fixture rules
metadata:
  type: reference
---

- `f || die` turns errexit OFF inside `f` and everything it calls. A helper used that way must end every step with
  `|| return 1`, or a failing first command lets the next ones run (seen: apply failed, commit still ran).
- Test runner per case: `set +e; ( set -e; fn ) > out 2>&1; rc=$?; set -e`. Inside `if ( ... )` or `( ... ) || x`
  errexit is ignored even with `set -e` in the subshell.
- `bash` on PATH here is Homebrew bash 5; macOS `/bin/bash` is 3.2. Prove 3.2 with
  `PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash <budget> /bin/bash <test>` (Apple git lives in /usr/bin).
- bash 3.2 with `set -u`: `"${arr[@]}"` on an EMPTY array is "unbound variable"; index loops avoid it.
- Splitting TSV with `IFS=$TAB read` collapses empty fields (tab is IFS whitespace): count tabs and refuse
  leading, trailing or doubled tabs before reading.
- `git apply --3way` (git 2.50) tries a plain apply first, implies `--index`, and exits non-zero on conflicts while
  leaving markers and unmerged entries: back up the index file and the targets, restore both on failure.
- A 3-way-clean fixture needs the target edit inside the hunk context but two unchanged lines away from the change;
  adjacent-line edits conflict.
- `git ls-files --eol` columns are `i/lf w/crlf attr/text eol=crlf <path>`; a gitattributes pattern with a slash is
  anchored to its folder, so a nested folder needs `**/dot-githooks/*`.
- The level-branches self-test takes about 45 s per run, so a 20-mutant probe needs more than one 600 s budget: run
  each mutant on its own copy of S, 7 in parallel (`cmd &` then `wait`), or split the list across runs.
- macOS `mktemp -d` WITHOUT a template ignores TMPDIR and writes to /var/folders (an HR1 breach for agents told to
  stay in the scratchpad). Always `mktemp -d "${TMPDIR:-/tmp}/name.XXXXXX"`, README recipes included; the self-test
  case `case_temp_folders_use_a_template_under_tmpdir` enforces it.
- A self-test case that passes on the real code proves nothing until a mutant of the guarded line turns it red:
  assert the exact refusal message and the manifest line, or an accidental failure (cp, mkdir) kills the case for you.
- An EXIT trap inherits `set -e`: its first failing command ends the trap (rollback stops halfway, backup leaks). Start
  every trap with `local rc=$?; set +e`, guard each step with `|| record/echo`, end with `exit "$rc"` (status kept on
  bash 3.2 and 5). A surviving mutant that deletes `set +e` is NOT equivalent while any `echo ... >&2` in the trap is
  unguarded: with fd 2 closed, errexit fires on that echo and the rest of the trap is skipped (QA refuted the claim).
- A restore with `cp -p` onto a file the failed write never touched (mode 444) fails: compare first (`cmp -s` plus
  `ls -ld | cut -c1-10`) and only restore rows whose write started.
- Every compare and every restore kind in a rollback needs its own failing fixture, or its mutant survives: a mode-only
  row (equal content, other mode) for the mode half of the compare; an rm shim for removing an added file, not only
  a cp shim for restoring a changed one.
- Failures no fixture can cause (one git subcommand, a restore cp, a cleanup rm) are forced with a PATH shim:
  `#!/bin/sh`, `pat='<glob>'`, `case "$*" in $pat) exit 1;; esac`, `exec <real> "$@"`. A pattern from a variable is
  not word-split, so globs with spaces work. Make the glob specific (`-p */row-0 *` hits the restore, not the backup).
- Unknown-option tests: give one argument too few (`--dry-run level-1`), else removing the `-*) usage` arm is masked
  by the argument-count check. Append new self-test cases at the end so QA's case numbers stay stable.
