---
name: git-ci-traps
description: Git, gh and CI traps of the factory cycles (SSH push, check names, PR watch timing, moved base, parallel worktrees, merges into in-flight branches); verify commands before relying on them
applies-to:
  - any
metadata:
  type: project
---

## Push and PR checks
- **Workflow-file pushes over https are rejected** even when `gh auth status` shows the `workflow` scope (git's https
  credential is another token). Push over SSH (`git push <ssh-url> <branch>`); `gh pr create/merge` work fine.
- **Check names:** `verify` (ubuntu, full tests with Docker), `windows-build` (compile only), `publish-script-test`
  (since spec 09), `build` (guide). Every push and pull_request runs, so expect two runs per push; find the runs of a
  commit with `gh run view <id> --json headSha` (a pull_request run's headSha is the PR merge commit).
- **Watch timing:** checks can register 60 s after a push; `gh pr checks --watch` says "no checks reported" before
  that. Loop on that message and confirm every expected check name is listed before merging. A run that fails at
  startup with 0 jobs ("workflow file issue") is re-run, not reworked.
- **Worktree Bash guard:** refuses `cd X && git ...`, `$(...)` and any compound command naming git or gh; one plain git
  command per call (or `git -C <path>`).

## CI-config and CI-bug specs
- Real runs are the evidence: push and open the task PR before the verifier, and commit plan/memory before QA (evidence
  binds to the PR head sha; a later push invalidates it). Throwaway proof branches go through a draft PR into the spec
  branch, then close unmerged and delete.
- **Job summaries (`$GITHUB_STEP_SUMMARY`) are invisible to logged-out viewers and every API.** Never gate a criterion
  on them; gate on reactor lines, annotations, the uploaded report artifact and the red job (ci-performance plan.md A3).
- **Hang/timeout bugs:** prove with a throwaway probe pair, red on the base workflow, green on the exact gated workflow
  with only the probe test (fix-ci-report-on-timeout plan.md). Job timeout, manual cancel and supersede all set
  `cancelled()`; only a step timeout ends as failure.

## Moved base
- When `factory` moves, merge `origin/factory` into the spec branch (merge commit, never rebase or force-push), run
  every gate on the merge result BEFORE pushing it, then re-run only tip-dependent sweep items with a
  `git diff --name-status <old tip> <new tip>` proof; re-baseline changed gated files by patch-id, Java counts and
  warnings from a base referee on the merged factory commit.
- If it moves while qa-final runs, let the sweep finish, merge, then resume qa-final for only the tip-dependent items;
  re-check `git ls-remote origin factory` right before the readiness checklist.
- An untracked byte-identical copy of an incoming file blocks the merge: move it aside, merge, `cmp`.

## Parallel cycles and worktrees
- **Isolated Agent-tool worktree** starts on stripped `main`, not factory: cut with
  `git switch -c sdd/<slug>/spec --no-track origin/factory`. Comms live in that worktree's `.claude/comms/`.
- Merge `origin/factory` into the spec branch after the last task PR and BEFORE the final sweep (in long cycles also
  before a verification task), so qa-final reviews exactly what merges.
- Every factory merge brings agent-memory conflicts: union the `MEMORY.md` index lines yourself, let the owning agent
  type (sonnet) curate its topic files, run the memory guard after each merge, curators in parallel.
- **Spec cut from another unmerged spec branch:** merge factory in a scratch `git worktree add` on the local spec branch
  while a task branch is checked out in the main worktree; each open task branch merges the spec tip before its ship gate.
- **Parallel task tracks** (`git worktree add -b <task> <scratchpad>/wt-t<n>`): comms in the main worktree, distinct
  port ranges. Shared files (manifests, plan rows) conflict on every later merge: commit the task, merge the spec tip,
  the task's implementer resolves and re-runs gates, I commit the merge; hold a second merge until the first lands;
  commit plan.md row updates once at the end.
- The QA lead writes memory in the main worktree: copy its files into a track's memory commit only AFTER its handback
  (an earlier copy lost a line), then `git checkout --` them in the main worktree.
- **Spec tip into an in-flight task branch:** WIP-commit, merge, leave only the doc conflict to the implementer, commit
  the merge myself.
