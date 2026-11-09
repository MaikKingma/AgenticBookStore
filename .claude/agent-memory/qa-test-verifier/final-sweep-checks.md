---
name: final-sweep-checks
description: qa-final (PR readiness) traps - base branch moving, CI run ids of task branches, mvnw in a scratch clone, bugfix negative-control ladder, warning inventory, JaCoCo from the CLI without a POM change, heartbeat stamp checks
metadata:
  type: feedback
---

Rule: verify every PR-readiness fact against the live remote yourself. Never copy a sha or run id from the brief.

**Why:** the owner can commit straight to `factory` (agent config, for example) while a spec cycle runs. The spec branch then no longer contains its base, and `gh pr view` still reports MERGEABLE/CLEAN with a stale `baseRefOid`. Briefs can also name CI run ids that belong to the last task branch (`headBranch: sdd/<slug>/T<n>`), not to the spec tip.
**How to apply:**
- Base check: `git ls-remote origin refs/heads/<base> refs/heads/<spec>` first, then `git merge-base --is-ancestor`. Exit 1 means "base moved, rebase required", which is a FAIL. CLAUDE.md forbids force-push, so the remedy to name is a merge of the base into the spec branch.
- CI: `gh run list --commit <tip sha> --json databaseId,event,headSha,conclusion` gives the real tip runs. In the `pull_request` run log, the line `HEAD is now at <sha> Merge <tip> into <base>` shows which base the merged tree was built on. Quote it as risk evidence, never as a replacement for the ancestor check.
- In a scratch clone, run `./mvnw` from a script that `cd`s into the clone. Calling `<clone>/mvnw` from the project root would resolve the main checkout's `.mvn`.
- The session scratchpad is shared across specs: a previous final sweep's `final-qa-clone` blocks `git clone`. Move it aside (`mv` to `spec<NN>-final-qa-clone`), never delete it.

Rule (bugfix-cycle qa-final): run the per-task negative control in throwaway worktrees as a three-step ladder, then prove the cross-task paths in a scratch worktree at the tip.
**Why:** a regression test that turns green when the OTHER task's fix lands does not pin its own defect. That gap only shows when the fixes are applied one at a time, and a single base-vs-tip run cannot see it.
**How to apply:**
- `git worktree add --detach .claude/worktrees/<name> <base>`, then `git -C <wt> checkout <tip> -- <test files>` (only there). Steps: base, then `cp` the other task's production file from the clone, then `cp` your own. Expect red, red, green, and `shasum` the files in every run. Run `clean test` from a script that `cd`s into the worktree.
- The IT `problem()` helper asserts the status before it reads the body, but RestTestClient prints the response to the log. `grep '"status":400' <log>` gives the field and message for the failure shape.
- Scratch IT methods: `place(...).expectBody(JSON_OBJECT).returnResult()` with `getStatus()` / `getResponseBody()` records a record-only scenario without asserting it. A `System.out.println("TAG " + problem)` line per method makes the log quotable. Remove the worktree with `--force`.
- Simpler bugfix ladder when the brief allows it: one detached worktree per task AT THE TIP, revert only that task's production files with `git -C <wt> show <base>:<path> > <file>`, run the union of all tasks' regression classes (exact red set = own tests only, other tasks green = cross control), restore with `show HEAD:`, porcelain empty.
- Surefire `runOrder=reversealphabetical` sorts by fully qualified name, and an upper-case class in the root package (`...distributor.HealthEndpointTest`) sorts BEFORE lower-case subpackages, so reverse runs it last. To prove a shared context starts from a given class, run `-Dtest=A,B -Dsurefire.runOrder=alphabetical` and count `Started`.
- Surefire `-Dtest=A#m1+B#m2` runs only class A: `+` joins methods of ONE class. Count `Tests run` before trusting a recipe; use a comma between classes.
- Per-task hash baselines into one integrity check: a python script reads the gate files in task order, keeps only `^[0-9a-f]{64}  ` lines (some files also hold porcelain and bare paths), drops agreed paths, keeps the LAST line per path and prints every supersede (old -> new hash, by which gate); then `shasum -a 256 -c` in the `git archive <tip>` extraction and a `diff` of the sorted path union against the spec diff's name list proves "reconciled by name".
- Jars from `spring-boot:repackage` carry the reproducible 1980 mtime: cite the `Building jar` log line of the run that built them, never `stat`.
- A directive that says a mutated control "fails at check X" is about order: the driver's first FAIL can be an earlier witness (a log check) for the same cause. Quote the first FAIL and the X lines both.
- Warning inventory (against a reference referee log, or a base referee in a throwaway worktree): a case-insensitive `warn` grep hits about 130 lines per referee here (app WARN logs that ACL, data and web tests provoke on purpose, Spring AI BeanPostProcessorChecker, npm `install-scripts` printed as `[ERROR]`), far beyond a directive's short "pre-existing" list. Prove ownership instead of arguing: `git reset --hard <base>` in the worktree, run the base referee, grep both logs per module section, classify by category in a python script and `diff` the sorted multisets after normalising timestamps, PIDs, thread names (`[...-exec-N]`), `Time elapsed: X s`, logback `HH:MM:SS,mmm` prefixes and the `.m2` path; "0 added at TIP" closes it. Without the thread and elapsed-time filters every run differs. Also normalise UUIDs (InventoryFlow WARN lines carry a random book id per run and show up as added AND removed) and count green Surefire summary lines separately (an `error` grep matches them through `Errors: 0`). A base referee needs no worktree: `git archive <base>` into the scratchpad, then a script that `cd`s there and runs `./mvnw -f <dir>/pom.xml clean verify`.
- Directive expected counts are a dry run, not truth: recount each one from its source (`git diff --name-only --diff-filter=AM` for a hash-union size) and report an off-by-one as a calibration note with the binding property shown, never as a product finding.

Rule (re-running earlier verifiers' scratch scripts on a final head): copy each to a new name, edit, `diff` old vs new, and compare the new tail with the old log (same kill table, same counts).
**Why:** three silent traps. A copy keeps its CLI guard `if (process.argv[1].endsWith('<old name>'))`, so the renamed copy runs nothing and exits 0. A script whose BASE is `origin/<spec branch>` now compares the head with itself (after the merges the spec ref IS the head): pin the task PR base sha (`git rev-parse <task merge>^1`). A script that reads files and runs `git -C R` breaks on a `git archive` tree: add `GW = process.env.QA_GIT_ROOT ?? R` for git and keep files from the fresh tree.
**How to apply:** grep every copy for `argv[1].endsWith`, `origin/`, `HEAD:`, ports, `slice(` and line-number literals before running. To widen a site-wide render for new per-cell checks, copy it and only add code; quote "lines removed: 0" from the diff. Independent read-only parts (CI/API, source greps, scenario probes) fit `fork` subagents with their own evidence prefix; keep everything that needs the reserved port in the main thread.

## Coverage from the CLI and impl heartbeat stamps

Rule: measure coverage from the command line; never add JaCoCo to a POM (plans forbid POM changes).

**Why:** the bookstore POM has no JaCoCo and its surefire config sets no `argLine`, so `prepare-agent` can inject the agent cleanly. Java 25 needs JaCoCo 0.8.14 or later.
**How to apply:**
- `./mvnw -pl bookstore org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.14:report -l <scratch>/jacoco.log`, then grep `bookstore/target/site/jacoco/jacoco.csv` for the changed classes (columns: instr missed/covered, branch missed/covered, line missed/covered).
- Run the plain `./mvnw -pl bookstore verify` too, as the gate run. The referee's `clean` removes the report.
- When a stat-based order proof pins the referee's Surefire XML and jar (qa-final), run JaCoCo on a `git archive <tip>` extraction in the scratchpad (`-pl bookstore -am`, script that `cd`s there) so the worktree target stays the referee's.
- For a constraint-only change (annotations on a record), line coverage is vacuous: prove each kept or removed annotation with a probe instead.

Rule: treat an impl artifact's `updated` as a claim; compare it with the file's mtime.

**Why:** an impl artifact once carried an `updated` stamp minutes in the future (estimated, not taken from `date -u`). It passes the literal heartbeat gate, but it hides a dead agent from the 15-minute rule.
**How to apply:** `stat -f "%Sm" -t "%Y-%m-%dT%H:%M:%S%z" <artifact>` (local +0100, so convert it) against `date -u`. A future stamp is a process note in the verdict, not a FAIL, unless the gate's stated conditions fail.
The same trap hits the verifier's own artifact: class-level probe batches and referees finish far faster than guessed, so take every `updated` from a fresh `date -u` call right before the bump, never from a guessed elapsed time.
Related: [[shell-traps]], [[integration-test-probes]], [[spring-runtime-probes]], [[scaffold-verification-techniques]], [[readme-and-live-jar-probes]]
