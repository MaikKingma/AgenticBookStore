---
name: bash-script-traps
description: Traps and techniques for the repo's bash tooling (level-branches, publish script) and hermetic bash tests: errexit in || contexts, bash 3.2 vs 5, git apply --3way, EXIT traps, PATH shims, bash mutants
metadata:
  type: reference
---
See also `.claude/agent-memory/senior-research-agent/shell-script-git-research-techniques.md` (three-shell matrix, hermetic
env vars, forced identity failure).

## Shell versions and runners
- `bash` on PATH here is Homebrew bash 5; macOS `/bin/bash` is 3.2. Run a script under test with `"$BASH" "$SCRIPT"`, never
  `bash "$SCRIPT"`, or a "green on 3.2" claim is false: prove it with a PATH shim `bash` that logs invocations (expect 0) and
  print `$BASH_VERSION`. 3.2 run: `PATH=/bin:/usr/bin:/usr/sbin:/sbin /bin/bash <budget> /bin/bash <test>`.
- Under `set -u` on 3.2, `"${arr[@]}"` on an EMPTY array is "unbound variable": use index loops. Bash-4 mutants to check the
  guard: `${x,,}` and an empty `"${a[@]}"`. A Linux bash 5 run needs no new image: `postgres:18.6` plus `apt-get install -y git`.
- Worktree guard refuses `bash -c '...'`, `git -C <other dir>` and inline loops: use `.claude/comms/tmp/<id>-*.sh`. Evidence files
  from a script: pipe through a `mask()` sed (sandbox, base, scratchpad, worktree paths, longest first), grep for home paths.

## errexit, traps, restores
- `f || die` turns errexit OFF inside `f` and everything it calls: end every step with `|| return 1`. Test runner per case:
  `set +e; ( set -e; fn ) > out 2>&1; rc=$?; set -e` (inside `if ( ... )` errexit is ignored).
- An EXIT trap inherits `set -e`: its first failing command ends the trap. Start with `local rc=$?; set +e`, guard each step
  with `|| record/echo`, end with `exit "$rc"`. A surviving mutant deleting `set +e` is NOT equivalent while an `echo >&2` in the
  trap is unguarded (fd 2 closed makes errexit fire).
- A restore with `cp -p` onto a file the failed write never touched (mode 444) fails: compare first (`cmp -s` plus
  `ls -ld | cut -c1-10`), restore only rows whose write started. Every compare/restore kind needs its own failing fixture
  (a mode-only row; an rm shim for removing an added file, not only a cp shim).
- Failures no fixture can cause are forced with a PATH shim: `#!/bin/sh`, `pat='<glob>'`, `case "$*" in $pat) exit 1;; esac`,
  `exec <real> "$@"`. Make the glob specific (`-p */row-0 *` hits the restore, not the backup).

## git specifics
- `git apply --3way` (git 2.50) tries a plain apply first, implies `--index`, exits non-zero on conflicts leaving markers and
  unmerged entries: back up the index file and targets, restore both on failure. A 3-way-clean fixture needs the target edit
  inside the hunk context but two unchanged lines away from the change.
- Pin git flags that override config (`status --untracked-files=normal`): the test sets the config it overrides
  (`status.showUntrackedFiles no`), or dropping the flag survives.
- `--force-with-lease=<ref>:<sha>` works when the sha is not local; read remote values with `git ls-remote` (no fetch).
  Atomic witness: an `update` hook in the bare remote rejecting the tag. Lease witness: a PATH shim `git` that moves remote
  main on `push` and logs the push argv.
- Date-varied idempotency: set `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE` per run, an hour apart, or an "always commit-tree"
  mutant produces the same id and survives. Precedence between two valid candidates needs a case where both exist and differ,
  in BOTH date orders, compared against the fixed expected id, not a live ref the mutant may have moved.

## Mutants, tests, portability
- Mutants on copies: `perl -0pe` (same on BSD and GNU) into a scratch copy with its own TMPDIR. A mutant can abort in setup
  (`set -e`): an EXIT trap printing "aborted" keeps that legible; a negative control must not abort either (re-adding a file
  with identical content leaves nothing to commit). A passing self-test case proves nothing until a mutant of the guarded line
  turns it red: assert the exact refusal message and the manifest line.
- Prefix-mutant look-alikes (`"$p*"` pathspec): names extending the whole path (`guide.yml.disabled`); `guide.yaml` survives.
  shellcheck SC2016 fires on `printf '%s' '[ "$1" = x ]'`: write hook files with a quoted heredoc; `ci.yml` already has an
  SC2016 info, compare against base before claiming a new finding.
- `IFS=$TAB read` collapses empty TSV fields: count tabs and refuse leading, trailing or doubled tabs first.
- macOS `mktemp -d` WITHOUT a template ignores TMPDIR: always `mktemp -d "${TMPDIR:-/tmp}/name.XXXXXX"`.
- macOS `cmp -n N` falsely reports "EOF on <first>" when the file is exactly N bytes: use `head -c N new | cmp -s - old`.
  macOS grep has no `-P`: non-ASCII check `env LC_ALL=C grep -c -E '[^ -~]' <file>`.
