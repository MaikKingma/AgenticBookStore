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

Self-test (fixture) review techniques:
- Dump probe: a temporary `QaT5ProbeDetailsTest`-style class in `nl.openvalue.bookstore` evaluates every `@ArchTest` field on the fixture import and writes every detail to a scratch file. It turns "confirm the expectation map by reading each fixture" into quoted evidence, and it shows extra violators and detail counts, which containment assertions hide.
- Containment-only assertions (`anyMatch(contains(fqn))`) cannot see a rule that reports TOO MUCH. Shared-LayeredArchitecture mutation: in ArchUnit 1.5.1, `whereLayer` constraints pile up on a shared instance, while `ignoreDependency` returns a copy. So rules 2 and 3 start reporting the rule-9 positive control and the self-test stays green. Check with detail counts from the dump. The fix is a control asserted absent from every rule.
- Field-only completeness (`getDeclaredFields` + `@ArchTest`) misses a rule declared as an `@ArchTest static void m(JavaClasses)` method. Probe it: ArchitectureTest runs 21 tests and the self-test stays green.
- Probe both halves of a two-package rule (each package string removed separately) and a rename that keeps the count, so both the size line and the set-equality line are proven to fail.

Composite and parameter-predicate rules (money rule style):
- Beyond the directive's mutations, sweep the parameter position and arity: last-only (`skip(Math.max(0, size()-1))`), skip-first (`skip(1)`), skip-first-unless-lone (`skip(size() > 1 ? 1 : 0)`), multi-parameter-only (`size() > 1 &&`), `allMatch`. Also drop each package from ONE half only, for all halves × packages. A SET comparison of the description hides a per-half drop. Kills: a count of the full package bracket per half, and one fixture per position (lone, first, middle, last).
- Judge your own survivors against the criterion text, not the M list. A survivor on a money criterion that can be killed inside the file scope is a FAIL, not informational (qa-lead CP3/CP5 in its ledger's cross-task patterns). Prove the kill with a temporary probe before writing the Required Action.
- Propose the kill as a FIXTURE, not a reflective test of a private helper: the orchestrator rejected the reflective form (couples the self-test to an implementation detail) and amended the plan with fixtures instead. If the fixture is outside the fileScope, say it needs a plan amendment.
- `assertReportsExpectedViolators` stops at the first missing violator, so list order decides which fixture a probe names. When a probe must name a specific fixture, check the order, and grep the printed details to show the mutant is live.
- Expected survivors, not defects: `isEquivalentTo` to `isAssignableTo` on primitives (equivalent), and `allowEmptyShould(true)` to `false` (only stricter, and both imports are non-empty).

Enclosing-class walk predicates ("nested at any depth in a class named *X"):
- Sweep EVERY walk position, not just both ends: suffix on direct-only, middle-only, top-only. Mutants: direct-only, top-only, direct-or-top (killed only by a middle-only fixture), skip-top, depth cap N (a cap at or above the deepest fixture is an inherent survivor, informational), start-at-self, `contains`. Fixtures that put the suffix only on the top-level class let top-only survive; the killer is a nested `...X` inside a top-level class without the suffix, with a member-anchored witness.
- Suffix predicates: letter-variant controls (prefix only, other suffix, suffix plus more) do not cover a case-insensitive mutant (add `...Appserviceimpl`), nor a "needs a prefix" mutant (`matches(".+X")`; add a class named exactly `X`). Also probe "target must be in package P" when every positive fixture's target sits in P.
- Each control/witness fixture gets a vacuity probe: retype its field to `Object`; only its own test must fail.
- Prove a proposed kill before you write the Required Action: generator probes that add the fixture (real predicate green) and fixture plus mutant (red), with a temporary `QaT6Probe*Test` in the test package, deleted afterwards and covered by the snap diff.
- A rule-dump temp test that evaluates every `@ArchTest` field on the fixture import, filtered origin-anchored to the new fixtures, answers "which rules does each fixture trip" in one run. Calling the private predicate by reflection on the production import shows the should-set is non-empty.

Related: [[spring-runtime-probes]], [[scaffold-verification-techniques]], [[shell-traps]]
