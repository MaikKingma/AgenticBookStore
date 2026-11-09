---
name: qa-git-script-specs
description: Falsifiers and safety rules for specs whose product is a bash script that rewrites git refs (publish or strip scripts, force pushes, guards): commit-tree determinism, atomic and lease witnesses, reuse precedence, refusal and look-alike witnesses, scratch-repo recipes, the script-interpreter trap in bash tests; read before the strategy or review of any git-script task
metadata:
  type: feedback
---

Written at the spec 09 strategy (publish-workshop-main.sh), confirmed by the T4 review. See also
[[qa-directive-traps]] and [[review-calibration]].

1. **commit-tree is deterministic.** Equal tree, parents, message, author and committer identity and dates give the
   same SHA. A hermetic script test that pins GIT_*_DATE lets an "always create a new commit" mutant pass every
   idempotency assertion. Demand different committer dates between the two runs (or unpinned dates plus a 1 s
   sleep) and a must-fail probe "always commit-tree", plus a control with the date step removed (the mutant must
   then survive). Also a witness that a local branch with the same tree but a parent is not reused.
2. **Push flags survive sequential tests.** Dropping `--atomic` or replacing leases by `--force` passes any test
   without a concurrent writer. Witnesses (verified in spec 09 T4): an `update` hook in the bare remote rejecting
   the tag (atomic: main stays); a PATH shim `git` that moves the remote ref before forwarding `push` (lease:
   `stale info`, nothing moved); the shim also records the push argv. A no-op rerun cannot test a lease: git marks
   an up-to-date ref before the CAS check, so test the existing-ref lease on a run that really moves the ref.
   "No network without --push": a broken origin URL must not fail the default mode; `--remote` honoured: a broken
   origin plus a good named remote; an empty `--remote ''` must be a usage error, not a fallback to origin.
3. **Reuse precedence.** When a script can reuse a baseline from the remote or from a local branch, ask for a
   witness where both are valid baselines with different SHAs (second machine: default mode first, then `--push`);
   otherwise a swapped order that force-moves the public ref survives. Put it in the falsifier list at strategy time.
4. **Path matching.** Look-alike witnesses for every listed path (`.claude-foo/`, `CLAUDE.md.example`,
   `docs/specs-notes.md`, nested `x/specs/y`, `docs/spec-pack-old/`) kill prefix-grep mutants; a guard run from a
   subdirectory kills a missing cd to the top level (pathspecs are cwd-relative, the guard then passes silently).
   Root-only lists miss nested agent files (Claude Code reads nested CLAUDE.md): price it at Gate 1.
5. **Refusals need each shape.** Clean-tree: tracked, staged, untracked non-ignored (and an ignored file must not
   refuse), and untracked under `status.showUntrackedFiles=no` (kills a dropped `--untracked-files=normal`). Branch:
   detached HEAD at the branch's commit and a `name-x` branch. Remote equality: ahead, behind and diverged (kills an
   `is-ancestor` check). Every refusal asserts refs unchanged.
6. **Recipe gap before the gate.** Nothing is committed before the QA gate, so a clone of the task branch lacks the
   script under test. Recipe: clone into SP, rename the branch, `cp -p` the uncommitted files, commit in the scratch
   clone, bare repo in SP as origin. Git writes only inside scratch repos, never the project repo or its worktrees
   (default mode of such a script moves the shared repo's local refs). Fresh-clone builds install node and npm: give
   them their own budget, not the referee's.
7. **Script-interpreter trap.** `/bin/bash test.sh` proves nothing about bash 3.2 when the test calls
   `bash "$SCRIPT"`: that resolves bash from PATH (Homebrew 5 on this Mac). Require `"$BASH" "$SCRIPT"` in the test
   and, as evidence, a logging PATH shim for bash with 0 calls plus bash-4 mutants (`${x,,}`, empty `"${a[@]}"`
   under `set -u`) that die. shellcheck passes both mutants, and a summary line printing `$BASH` names the test's
   interpreter, not the script's. CI on ubuntu is bash 5 only: 3.2 stays a manual check.
8. **Tooling.** Linux run in `ubuntu:24.04` plus apt git. Hermetic proof: a hostile HOME .gitconfig (gpgSign with a
   failing program, other default branch, failing hooksPath, useConfigOnly, showUntrackedFiles no) must keep the
   test green. shellcheck and actionlint are not installed on this machine: docker images, digests recorded;
   actionlint on an existing workflow may carry base findings, so phrase the criterion as "adds no finding".
