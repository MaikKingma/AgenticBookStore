---
name: qa-git-script-specs
description: Falsifiers and safety rules for specs whose product is a bash script that rewrites git refs (publish or strip scripts, force pushes, guards): commit-tree determinism, atomic and lease witnesses, refusal and look-alike witnesses, scratch-repo recipes, portability runs; read before the strategy or review of any git-script task
metadata:
  type: feedback
---

Written at the spec 09 strategy (publish-workshop-main.sh). Items marked (unverified) are strategy-time techniques
not yet proven by a verifier run; update them after the first review. See also [[qa-directive-traps]].

1. **commit-tree is deterministic.** Equal tree, parents, message, author and committer identity and dates give the
   same SHA. A hermetic script test that pins GIT_*_DATE lets an "always create a new commit" mutant pass every
   idempotency assertion. Demand different committer dates between the two runs (or unpinned dates plus a 1 s
   sleep) and a must-fail probe "always commit-tree". Also a witness that a local branch with the same tree but a
   parent is not reused (kills reuse-on-tree-equality without the parentless check).
2. **Push flags survive sequential tests.** Dropping `--atomic` or replacing leases by `--force` passes any test
   without a concurrent writer. Witnesses (unverified): an `update` hook in the bare remote rejecting one ref
   (atomic: the other ref stays); a PATH shim `git` that moves the remote ref before forwarding `push` (lease:
   `stale info`, nothing moved); `GIT_TRACE=1` argv of the single push. "No network without --push": a broken
   origin URL must not fail the default mode; `--remote` honoured: a broken origin plus a good named remote.
3. **Path matching.** Look-alike witnesses for every listed path (`.claude-foo/`, `CLAUDE.md.example`,
   `docs/specs-notes.md`, nested `x/specs/y`, `docs/spec-pack-old/`) kill prefix-grep mutants; a guard run from a
   subdirectory kills a missing cd to the top level (pathspecs are cwd-relative, the guard then passes silently).
   Root-only lists miss nested agent files (Claude Code reads nested CLAUDE.md): price it at Gate 1.
4. **Refusals need each shape.** Clean-tree: tracked, staged, untracked non-ignored (and an ignored file must not
   refuse). Branch: detached HEAD at the branch's commit and a `name-x` branch. Remote equality: ahead, behind and
   diverged (kills an `is-ancestor` check). Every refusal asserts refs unchanged.
5. **Recipe gap before the gate.** Nothing is committed before the QA gate, so a clone of the task branch lacks the
   script under test. Recipe: clone into SP, rename the branch, `cp -p` the uncommitted files, commit in the scratch
   clone, bare repo in SP as origin. Git writes only inside scratch repos, never the project repo or its worktrees
   (default mode of such a script moves the shared repo's local refs). Fresh-clone builds install node and npm: give
   them their own budget, not the referee's.
6. **Portability and tooling.** Run the test under macOS /bin/bash 3.2 explicitly and bash 5 in a Linux container;
   a fixture with nothing to remove exercises empty arrays under `set -u`. Hermetic proof: a hostile HOME
   .gitconfig (gpgSign with a failing program, other default branch, failing hooksPath, useConfigOnly) must keep the
   test green. shellcheck and actionlint are not installed on this machine: docker images, digests recorded.
