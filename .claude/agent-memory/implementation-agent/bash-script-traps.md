---
name: bash-script-traps
description: Traps when writing and testing the bash tooling of this repo (docs/spec-pack/level-branches): errexit lost in || contexts, bash 3.2 on macOS, git apply --3way, rollback and EXIT-trap fixtures
metadata:
  type: reference
---

- `f || die` turns errexit OFF inside `f` and everything it calls. A helper used that way must end every step with
  `|| return 1`, or a failing first command lets the next ones run (apply failed, commit still ran).
- Test runner per case: `set +e; ( set -e; fn ) > out 2>&1; rc=$?; set -e`. Inside `if ( ... )` or `( ... ) || x`
  errexit is ignored even with `set -e` in the subshell.
- `bash` on PATH here is Homebrew bash 5; macOS `/bin/bash` is 3.2. Prove 3.2 with
  `PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash <budget> /bin/bash <test>` (Apple git lives in /usr/bin). Under `set -u` on 3.2,
  `"${arr[@]}"` on an EMPTY array is "unbound variable"; index loops avoid it.
- Splitting TSV with `IFS=$TAB read` collapses empty fields (tab is IFS whitespace): count tabs and refuse leading, trailing or
  doubled tabs before reading.
- `git apply --3way` (git 2.50) tries a plain apply first, implies `--index`, and exits non-zero on conflicts while leaving
  markers and unmerged entries: back up the index file and the targets, restore both on failure. A 3-way-clean fixture needs the
  target edit inside the hunk context but two unchanged lines away from the change.
- macOS `mktemp -d` WITHOUT a template ignores TMPDIR and writes to /var/folders (an HR1 breach for agents told to stay in the
  scratchpad). Always `mktemp -d "${TMPDIR:-/tmp}/name.XXXXXX"`, README recipes included.
- A self-test case that passes on the real code proves nothing until a mutant of the guarded line turns it red: assert the exact
  refusal message and the manifest line, or an accidental failure (cp, mkdir) kills the case for you.
- An EXIT trap inherits `set -e`: its first failing command ends the trap (rollback stops halfway, backup leaks). Start every
  trap with `local rc=$?; set +e`, guard each step with `|| record/echo`, end with `exit "$rc"`. A surviving mutant that deletes
  `set +e` is NOT equivalent while any `echo ... >&2` in the trap is unguarded: with fd 2 closed, errexit fires on that echo.
- A restore with `cp -p` onto a file the failed write never touched (mode 444) fails: compare first (`cmp -s` plus
  `ls -ld | cut -c1-10`) and only restore rows whose write started. Every compare and restore kind needs its own failing
  fixture: a mode-only row (equal content, other mode); an rm shim for removing an added file, not only a cp shim.
- Failures no fixture can cause (one git subcommand, a restore cp, a cleanup rm) are forced with a PATH shim: `#!/bin/sh`,
  `pat='<glob>'`, `case "$*" in $pat) exit 1;; esac`, `exec <real> "$@"`. A pattern from a variable is not word-split. Make the
  glob specific (`-p */row-0 *` hits the restore, not the backup).
- Worktree-isolated Bash refuses `git -C <other dir>` and redirects into the worktree from another checkout ("too complex"):
  use a scratchpad script. Evidence files written from a script: pipe output through a `mask()` sed (sandbox, base, scratchpad,
  worktree paths, longest first), then grep the evidence folder for home-directory paths.
- macOS `cmp -n N` falsely reports "EOF on <first>" when that file is exactly N bytes: prefix-check with
  `head -c N new | cmp -s - old`. Plain `diff` may shift an insert hunk up a line when the line above repeats as the last
  inserted line (`diff --minimal` does not): pin the position with head/sed/tail splits and `cmp`.
- macOS grep has no `-P`: a non-ASCII check is `env LC_ALL=C grep -c -E '[^ -~]' <file>` (exit 1 and 0 = clean).
