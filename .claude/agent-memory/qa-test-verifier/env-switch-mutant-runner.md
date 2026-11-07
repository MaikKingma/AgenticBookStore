---
name: env-switch-mutant-runner
description: Run many mutation probes cheaply with one schema-mutant file per production class selected by an env variable, plus a runner script that restores by cp and compares hashes
metadata:
  type: feedback
---

Rule: when a probe list has 20+ mutants on a few small classes, write one "mutant schema" copy of each class in the scratchpad that switches on `System.getenv("QA_PROBE")` (unset = original behaviour), and drive it with a runner script in `.claude/comms/tmp/`.

**Why:** one Edit per mutant costs a tool call per probe plus a restore. A WebMvcTest-sized run is about 4 s with `clean`, so 50 probes finish in about 4 minutes in the background, and the env variable reaches the forked Surefire JVM unchanged.
**How to apply:**
- Per probe the script does: `cp` mutant over the production file, `QA_PROBE=<id> bash with-budget.sh 300 ./mvnw -pl <module> clean test -Dtest=<Class>`, `cp` the FF7 `.orig` back, `shasum` all touched files and compare with the snap-before, then append one result line (id, rc, hash flag, summary line, failing test lines).
- When a later round reuses round 1 `.orig` copies, make the runner abort unless every `.orig` is hash-equal to the live production file. A stale `.orig` would otherwise silently restore old bytes.
- Prove the schema first with a CONTROL run (all mutants in place, variable unset): same green count as the real code. A killed probe then also proves the variable reaches the JVM.
- Compare hashes only against the `^[0-9a-f]{64}` lines of the snapshot; the `git status` lines in the same file also match a filename grep and gave false mismatches.
- Parameterized tests: grep the failing `[n]` index per probe (for example `grep -oE 'With400\(String\)\[[0-9]\]'`) to show which input killed which mutant.
- Do not edit a running script (bash reads it incrementally); stop the background task with TaskStop, fix, restart.
- Keep two or three plain Edit probes for things a schema cannot express (annotation attribute removed, `@ExceptionHandler` removed).
- Run the final referee after the last probe, even if the last probe restored identical bytes; the order proof stays clean.
- Take the `.orig` copies and restore with `cp -p`: product mtimes stay the implementer's, so the C0 order proof (impl artifact, newest product file, snap-after, Surefire XML) still reads correctly.
- Probe target classes get mutant bytecode in `target/classes`; run `javap` inspections only after the referee rebuild, never between probes.
- FF8 "one sort probe per distinct permutation": first list, for each column and both directions, which row permutation its sort gives. Probe one representative per distinct result. Then add a reversal probe for any non-identity permutation no column gives (for 3 rows there are 5). Use stable `Comparator` sorts on the view record in the schema. A column that is constant because of a domain invariant (for example a single-currency `Money`) is exempt; cite the invariant's file:line.
- Alternative when mutants touch annotations, `@Query` strings or SQL migrations (no env switch possible): write a `python3 -I` generator in the scratchpad that builds one full mutant file per probe from the `.orig` by exact replacement and aborts unless each old string occurs exactly once. A positional-args filter lets you add probes later without rewriting files the running batch may read. The runner then installs `mut-<id>--<File>` files and saves a `diff` per probe.
- Watch the result file with a Monitor `until`-style script that prints new lines and exits at the expected count. Background probe runs then report as they land.

Related: [[domain-mutation-probes]], [[integration-test-probes]], [[shell-traps]]
