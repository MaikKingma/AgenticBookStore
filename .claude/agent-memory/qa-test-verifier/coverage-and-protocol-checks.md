---
name: coverage-and-protocol-checks
description: Measuring JaCoCo coverage in the bookstore without a POM change, and checking impl heartbeat stamps against file mtimes
metadata:
  type: feedback
---

Rule: measure coverage from the command line; never add JaCoCo to a POM (plans forbid POM changes).

**Why:** the bookstore POM has no JaCoCo and its surefire config sets no `argLine`, so `prepare-agent` can inject the agent cleanly. Java 25 needs JaCoCo 0.8.14 or later.
**How to apply:**
- `./mvnw -pl bookstore org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.14:report -l <scratch>/jacoco.log`, then grep `bookstore/target/site/jacoco/jacoco.csv` for the changed classes (columns: instr missed/covered, branch missed/covered, line missed/covered).
- Run the plain `./mvnw -pl bookstore verify` too, as the gate run. The referee's `clean` removes the report.
- For a constraint-only change (annotations on a record), line coverage is vacuous: prove each kept or removed annotation with a probe instead.

Rule: treat an impl artifact's `updated` as a claim; compare it with the file's mtime.

**Why:** an impl artifact once carried an `updated` stamp minutes in the future (estimated, not taken from `date -u`). It passes the literal heartbeat gate, but it hides a dead agent from the 15-minute rule.
**How to apply:** `stat -f "%Sm" -t "%Y-%m-%dT%H:%M:%S%z" <artifact>` (local +0100, so convert it) against `date -u`. A future stamp is a process note in the verdict, not a FAIL, unless the gate's stated conditions fail. The same trap hits the verifier's own artifact: take every `updated` from a fresh `date -u` call, never from a guessed elapsed time.

Related: [[shell-traps]], [[integration-test-probes]]
