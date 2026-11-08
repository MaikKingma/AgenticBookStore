---
name: deterministic-regression-test-techniques
description: How to build deterministic red-before/green-after regression tests and like-for-like negative controls for clock, randomness and Spring test-context bugs
metadata:
  type: reference
---

Clock / midnight bugs:
- Thread-based probe clock (substituted for the production `Clock` bean in a throwaway tree only): one instant on
  thread `main` (JUnit), the next day on any other thread (Tomcat). Reproduces a test-vs-server date split
  deterministically. A bracket assertion (date before/after) stays red under this probe, so pick the probe that
  matches the fix being proven, or use a time-monotone probe for bracket fixes.
- Fixing without a new Spring context: a top-level `@TestConfiguration` with a `@Primary` fixed Clock (method name
  not `clock`, so no bean overriding) imported by EVERY class sharing the cached context. Count "Starting <Test>"
  log lines to prove sharing. `@TestBean(name=..., methodName="fqcn#m")` with a shared factory also shares (handler
  equality = name + factory method), but needs the field in every sharing class.

Randomness:
- `ThreadLocalRandom.current()` is one singleton for all threads, so identity tests cannot prove per-thread use;
  neither TLR nor `RandomGenerator.getDefault()` can be seeded or read. No deterministic black-box test exists.
  Use a package-private `Supplier<RandomGenerator>` seam plus `isInstanceOf(ThreadLocalRandom)`; the negative
  control is the literal pre-fix wiring line, kept compilable by retaining the old public constructor.
- A shared generator under contention is detectable only by direct `nextLong()` duplicate counting (very reliable at
  8 threads x 500k); boolean outcomes (draw < rate) are not. Measure rates in a single-file `java X.java` scratch.

Negative controls: prefer "put the pre-fix line back in the post-fix tree" over hand-written mutants; it proves the
test discriminates exactly the bug. For over-broad fixes, add a guard test (for example actuator default body) and
prove it red by disabling the guard.

Related: [[spring-mvc-validation-traps]], [[spring-tx-event-traps]]
