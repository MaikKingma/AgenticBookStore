---
name: final-sweep-checks
description: qa-final (PR readiness) traps in this repo - base branch moving under the spec branch, CI run ids that belong to task branches, running mvnw inside a scratch clone
metadata:
  type: feedback
---

Rule: verify every PR-readiness fact against the live remote yourself. Never copy a sha or run id from the brief.

**Why:** the owner can commit straight to `factory` (agent config, for example) while a spec cycle runs. The spec branch then no longer contains its base, and `gh pr view` still reports MERGEABLE/CLEAN with a stale `baseRefOid`. Briefs can also name CI run ids that belong to the last task branch (`headBranch: sdd/<slug>/T<n>`), not to the spec tip.
**How to apply:**
- Base check: `git ls-remote origin refs/heads/<base> refs/heads/<spec>` first, then `git merge-base --is-ancestor`. Exit 1 means "base moved, rebase required", which is a FAIL. CLAUDE.md forbids force-push, so the remedy to name is a merge of the base into the spec branch.
- CI: `gh run list --commit <tip sha> --json databaseId,event,headSha,conclusion` gives the real tip runs. In the `pull_request` run log, the line `HEAD is now at <sha> Merge <tip> into <base>` shows which base the merged tree was built on. Quote it as risk evidence, never as a replacement for the ancestor check.
- If the base commit adds a file that is untracked in the main checkout, compare `git hash-object <file>` to the base blob, and warn that a merge in the main checkout may refuse to overwrite it.
- In a scratch clone, run `./mvnw` from a script that `cd`s into the clone. Calling `<clone>/mvnw` from the project root would resolve the main checkout's `.mvn`.
- The session scratchpad is shared across specs: a previous final sweep's `final-qa-clone` blocks `git clone`. Move it aside (`mv` to `spec<NN>-final-qa-clone`), never delete it.
- Surefire-vs-source class lists: ArchitectureTest holds `@ArchTest` fields and no `@Test`, so list `@(Test|ArchTest)` holders. A tr_TR run proves its argLine took effect when the PostgreSQL detail line prints in Turkish (`Ayrıntı:`).
- Writing your own memory changes the main checkout's `git status`. Quote the end status first, then write memory, and report the delta.

Related: [[shell-traps]], [[spring-runtime-probes]], [[scaffold-verification-techniques]]
