---
name: archunit-probes
description: Non-vacuity probes and fixture self-test review for the bookstore ArchUnit rules
applies-to:
  - bookstore/src/test/java/**/ArchitectureTest.java
  - bookstore/src/test/java/**/*Arch*
  - bookstore/src/main/java/**
metadata:
  type: feedback
---
Rule: prove every rule's package string resolves on the PRODUCTION import, not only on fixtures.
**Why:** `allowEmptyShould(true)` hides a mistyped package; fixtures prove the rules bite on `archfixtures`, not that the production constants match real packages.
**How to apply:**
- Cheapest full proof for naming-rule packages: temporarily drop `.doNotHaveSimpleName("package-info")` from the naming helper. maven-compiler-plugin emits an empty synthetic `package-info.class` per package, so each naming rule must then fail naming only its own package-info: each string resolves and no two patterns overlap (`..domain..` vs `..domaininteraction..`).

Self-test (fixture) review:
- Dump probe: a temporary `QaT5ProbeDetailsTest`-style class in `nl.openvalue.bookstore` evaluates every `@ArchTest` field on the fixture import and writes every detail to a scratch file: quoted evidence for the expectation map, extra violators and detail counts that containment assertions hide.
- Containment-only assertions (`anyMatch(contains(fqn))`) cannot see a rule reporting TOO MUCH. Shared-LayeredArchitecture mutation: in ArchUnit 1.5.1 `whereLayer` constraints pile up on a shared instance while `ignoreDependency` returns a copy, so rules 2 and 3 start reporting the rule-9 positive control and the self-test stays green; fix with a control asserted absent from every rule.
- Field-only completeness (`getDeclaredFields` + `@ArchTest`) misses a rule declared as an `@ArchTest static void m(JavaClasses)` method (ArchitectureTest runs 21 tests, self-test green). Probe both halves of a two-package rule (each package string removed separately) and a rename that keeps the count, so the size line and the set-equality line both fail.
- Expected survivors, not defects: `isEquivalentTo` to `isAssignableTo` on primitives; `allowEmptyShould(true)` to `false` (only stricter).

Composite and parameter-predicate rules (money rule style):
- Sweep parameter position and arity: last-only (`skip(Math.max(0, size()-1))`), skip-first, skip-first-unless-lone, multi-parameter-only (`size() > 1 &&`), `allMatch`; drop each package from ONE half only (all halves x packages; a SET comparison of the description hides a per-half drop). Kills: a count of the full package bracket per half, and one fixture per position (lone, first, middle, last).
- Judge survivors against the criterion text, not the implementer's list: a survivor on a money criterion killable inside the file scope is a FAIL, not informational. Propose the kill as a FIXTURE, not a reflective test of a private helper (rejected: couples the self-test to implementation); a fixture outside the fileScope needs a plan amendment.
Enclosing-class walk predicates ("nested at any depth in a class named *X"):
- Sweep EVERY walk position: suffix on direct-only, middle-only, top-only. Mutants: direct-only, top-only, direct-or-top (killed only by a middle-only fixture), skip-top, depth cap N (at or above the deepest fixture is inherent, informational), start-at-self, `contains`. Suffix only on the top-level class lets top-only survive; the killer is a nested `...X` inside a top-level class without the suffix, with a member-anchored witness.
Related: [[spring-runtime-probes]], [[scaffold-verification-techniques]], [[shell-traps]]
