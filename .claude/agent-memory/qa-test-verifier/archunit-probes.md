---
name: archunit-probes
description: Probes that prove the bookstore ArchitectureTest rules are non-vacuous on the production import, plus grep traps when reviewing package-info and rule files
metadata:
  type: feedback
---

Rule: prove that every rule's package string resolves on the PRODUCTION import, not only on fixtures.

**Why:** `allowEmptyShould(true)` hides a mistyped package. Fixtures (T5) prove the rules bite on `archfixtures`, but they do not prove that the production constants match the real packages.
**How to apply:**
- Cheapest full proof for the naming-rule packages: temporarily drop `.doNotHaveSimpleName("package-info")` from the naming helper. maven-compiler-plugin emits an empty synthetic `package-info.class` per package, so each naming rule must then fail, naming only its own package-info. That proves each string resolves and that no two patterns overlap (`..domain..` vs `..domaininteraction..`).
- Rule 8 FQN: put a real `@RestController` class in `data`, named `*Repository` so that no naming rule fires. Rule 9: put an `*Event` record in domain, used by a `process` class through a parameter and a method call, plus a non-Event field in another `process` class. Only the second must be reported (check with `grep -c` on the log).
- Use your own probe class names (`QaProbe*`) so that impl leftovers cannot satisfy the probe.

Grep traps:
- The directive grep `grep -l '@\|^import '` on package-info hits Javadoc `{@code ...}`. Back it with a refined grep (`'^\s*@|^\s*import |@[A-Z]'`) and with `javap -v` on the `.class`: `attributes: 1` (SourceFile only) means no annotations.
- The impl verifier and the QA verifier share the session scratchpad and the W7 prefix. Check `ls` first and use a role infix (`t4-qa-*`) so you never overwrite the impl's evidence.
- Suffix set equality: script-parse the section 3 table and the test, then prove the checker can fail on a mutated scratch copy.

Related: [[spring-runtime-probes]], [[scaffold-verification-techniques]], [[shell-traps]]
