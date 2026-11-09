---
name: final-sweep-checks
description: qa-final (PR readiness) traps in this repo - base branch moving under the spec branch, CI run ids that belong to task branches, running mvnw inside a scratch clone, bugfix negative-control ladder with cross controls in throwaway worktrees
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

Rule (bugfix-cycle qa-final): run the per-task negative control in throwaway worktrees as a three-step ladder, then prove the cross-task paths in a scratch worktree at the tip.
**Why:** a regression test that turns green when the OTHER task's fix lands does not pin its own defect. That gap only shows when the fixes are applied one at a time, and a single base-vs-tip run cannot see it.
**How to apply:**
- `git worktree add --detach .claude/worktrees/<name> <base>`, then `git -C <wt> checkout <tip> -- <test files>` (only there). Steps: base, then `cp` the other task's production file from the clone, then `cp` your own. Expect red, red, green, and `shasum` the files in every run. Run `clean test` from a script that `cd`s into the worktree.
- The IT `problem()` helper asserts the status before it reads the body, but RestTestClient prints the response to the log. `grep '"status":400' <log>` gives the field and message for the failure shape.
- Scratch IT methods: `place(...).expectBody(JSON_OBJECT).returnResult()` with `getStatus()` / `getResponseBody()` records a record-only scenario without asserting it. A `System.out.println("TAG " + problem)` line per method makes the log quotable. Remove the worktree with `--force`.

- Simpler bugfix ladder when the brief allows it: one detached worktree per task AT THE TIP, revert only that task's
  production files with `git -C <wt> show <base>:<path> > <file>`, run the union of all tasks' regression classes
  (exact red set = own tests only, other tasks green = cross control), restore with `show HEAD:`, porcelain empty.
- Surefire `runOrder=reversealphabetical` sorts by fully qualified name, and an upper-case class in the root package
  (`...distributor.HealthEndpointTest`) sorts BEFORE lower-case subpackages, so reverse runs it last. To prove a
  shared context starts from a given class, run `-Dtest=A,B -Dsurefire.runOrder=alphabetical` and count `Started`.

- Real-ACL end-to-end when IntegrationTestBase mocks the port: a scratch `@SpringBootTest(RANDOM_PORT)` class NOT
  extending the base, `@Import(PostgresTestConfig.class)`, a static JDK HttpServer started in a static field and wired
  by `@DynamicPropertySource`, a volatile body field per scenario and a hit counter asserted per call. Add a
  record-only method (prints status, row delta, stored row) to run per production state (factory, each fix alone).
  `ExchangeResult.getResponseBodyContent()` gives raw bytes for byte-identity checks.
- "Recipe does not reproduce" after fixture alignment: the recipe tests at tip are happy-path guards, so install the
  BASE bytes of the recipe tests at the tip (throwaway worktree) and show them red.
- Surefire `-Dtest=A#m1+B#m2` runs only class A: `+` joins methods of ONE class. Count `Tests run` before trusting a
  recipe; use a comma between classes.

Related: [[shell-traps]], [[spring-runtime-probes]], [[scaffold-verification-techniques]]
