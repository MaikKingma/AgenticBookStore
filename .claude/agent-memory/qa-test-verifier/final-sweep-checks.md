---
name: final-sweep-checks
description: qa-final traps - moving base, CI run ids, bugfix control ladder, warning inventory, JaCoCo CLI, heartbeat stamps
metadata:
  type: feedback
---
Rule: verify every PR-readiness fact against the live remote yourself. Never copy a sha or run id from the brief.
**Why:** the owner can commit straight to `factory` while a spec cycle runs: the spec branch then lacks its base, and `gh pr view` still reports MERGEABLE/CLEAN with a stale `baseRefOid`. Briefs can name CI run ids belonging to the last task branch (`headBranch: sdd/<slug>/T<n>`), not the spec tip.
**How to apply:**
- Base check: `git ls-remote origin refs/heads/<base> refs/heads/<spec>` first, then `git merge-base --is-ancestor`; exit 1 = "base moved, rebase required", a FAIL (force-push is forbidden, so name a merge of base into the spec branch).
- CI: `gh run list --commit <tip sha> --json databaseId,event,headSha,conclusion` gives the real tip runs. In the `pull_request` run log `HEAD is now at <sha> Merge <tip> into <base>` shows the base the merged tree was built on: risk evidence, never a replacement for the ancestor check.
- In a scratch clone run `./mvnw` from a script that `cd`s into the clone (`<clone>/mvnw` from the project root resolves the main checkout's `.mvn`).

Rule (bugfix-cycle qa-final): run the per-task negative control in throwaway worktrees as a three-step ladder, then prove cross-task paths in a scratch worktree at the tip.
**Why:** a regression test that turns green when the OTHER task's fix lands does not pin its own defect; only applying fixes one at a time shows it.
**How to apply:**
- `git worktree add --detach .claude/worktrees/<name> <base>`, `git -C <wt> checkout <tip> -- <test files>`. Steps: base, then `cp` the other task's production file, then your own: expect red, red, green; `shasum` in every run; `clean test` from a script that `cd`s there. Remove worktrees with `--force`, then `rmdir` the `.claude/worktrees` dir `worktree add` created.
- Simpler ladder: one detached worktree per task AT THE TIP, revert only that task's production files via `git -C <wt> show <base>:<path> > <file>`, run the union of all regression classes (exact red set = own tests only; other tasks green = cross control), restore with `show HEAD:`, porcelain empty.
- Warning inventory against a base referee: a case-insensitive `warn` grep hits about 130 lines per referee (WARN logs provoked on purpose, BeanPostProcessorChecker, npm `[ERROR]` install-scripts). Prove ownership: run the base referee on `git archive <base>` in the scratchpad (`./mvnw -f <dir>/pom.xml clean verify` from a script that `cd`s there), classify per module in python, `diff` sorted multisets after normalising timestamps, PIDs, thread names, `Time elapsed`, logback prefixes, UUIDs and the `.m2` path; "0 added at TIP" closes it. JVM-once notices (`sun.misc.Unsafe`) print in the first test taking a new library path: attribute by the `Running <class>` line and a control with the base production file. Repackaged jars carry a 1980 mtime: cite the `Building jar` line, never `stat`.

Rule (re-running earlier verifiers' scratch scripts on a final head): copy to a new name, edit, `diff` old vs new, compare the new tail with the old log (same kill table, same counts).
**Why:** a copy keeps its CLI guard `if (process.argv[1].endsWith('<old name>'))` and runs nothing (exit 0); a script whose BASE is `origin/<spec branch>` compares the head with itself after merges (pin the task PR base sha, `git rev-parse <task merge>^1`); a script running `git -C R` breaks on a `git archive` tree (add `GW = process.env.QA_GIT_ROOT ?? R`).
**How to apply:**
- Grep every copy for `argv[1].endsWith`, `origin/`, `HEAD:`, ports, `slice(`, line-number literals. After a deletion re-points a pin, scope each new negative token to the touched section.
- A script importing a rule from the working tree silently changes meaning when a later task extends it: re-run against the rule as the earlier task merged it (`git show <task merge>:<rule>` plus relative imports in the same layout), assert old list = new list minus the addition.

Rule: measure coverage from the command line; never add JaCoCo to a POM (plans forbid POM changes).
**Why:** the bookstore POM has no JaCoCo and its surefire config sets no `argLine`, so `prepare-agent` injects cleanly; Java 25 needs JaCoCo 0.8.14 or later.
**How to apply:** `./mvnw -pl bookstore org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.14:report -l <scratch>/jacoco.log`, grep `bookstore/target/site/jacoco/jacoco.csv` for the changed classes; also run plain `./mvnw -pl bookstore verify` as the gate (the referee's `clean` removes the report). When a stat-based order proof pins the referee's Surefire XML and jar, run JaCoCo on a `git archive <tip>` extraction (`-pl bookstore -am`). For a constraint-only change (annotations on a record) line coverage is vacuous: prove each annotation with a probe.

Rule: treat an impl artifact's `updated` as a claim; compare with the file's mtime (`stat -f "%Sm" -t "%Y-%m-%dT%H:%M:%S%z"`, convert local time) against `date -u`.
**Why:** an artifact once carried an `updated` minutes in the future (estimated, not from `date -u`); it passes the literal heartbeat gate but hides a dead agent from the 15-minute rule. A future stamp is a process note, not a FAIL, unless the gate's stated conditions fail; the same trap hits your own artifact: take every `updated` from a fresh `date -u` right before the bump.
Related: [[shell-traps]], [[integration-test-probes]], [[readme-and-live-jar-probes]]
