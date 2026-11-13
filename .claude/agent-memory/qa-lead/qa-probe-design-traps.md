---
name: qa-probe-design-traps
description: Designing must-fail mutation probes: failing for the stated reason, equivalent mutants, restore slips, clean builds and Surefire selector syntax, implementers editing production in rework, injected seams proven only by fakes
applies-to:
  - any
metadata:
  type: feedback
---

Siblings: [[qa-directive-traps]], [[qa-test-infra-guards]]. Judging rules for survivors: [[review-survivor-rules]].

1. **A probe must fail for the stated reason.** Make the verifier quote the failing assertion or exception line. A missing-migration entity without `@Id` fails as a mapping error, not schema validation; a compile error is never a negative control. Deleting a handler method a unit test calls directly is a compile error: write "advice live" probes as annotation removals (`@ExceptionHandler` dropped, method kept).
   - Before listing a must-fail probe check it is not an equivalent mutant: a view controller added on a path an annotated controller owns never runs (`ViewControllerRegistry` order 1 behind `RequestMappingHandlerMapping` order 0; a method mismatch answers 405 without fall-through). Write it as `registry.setOrder(-1)` plus the registration.

2. **Probes that remove a file need `clean`.** `./mvnw test` without `clean` keeps a moved migration or deleted probe class in `target/`, so the probe passes falsely or fails for the previous one's reason. Write `clean test -Dtest=...` into every probe item. A restore by `cp -p` needs it too: the restored source is older than its class file, so the incremental compile skips it and the run tests stale bytecode (check `Compiling N source files` in each probe log).
   - Surefire `-Dtest`: `+` joins methods of ONE class, classes need `,`; `A#m1+B#m2` silently runs only A; `A+B` runs ZERO tests with BUILD SUCCESS (grep my own directive for `+` before sending). Pin the expected `Tests run:` count of every selector.
   - A class with JUnit `@Nested` classes prints `Tests run: 0` for the outer class and the outer-plus-nested sum on the last nested class: pin per-class counts from the Surefire XML testcases and pre-explain the 0 line in a final brief.

3. **Implementers probing production in rework.** A test-only implementer whose round failed on a surviving mutation tends to re-create the mutant in the production file, even inside its own fileScope; integration-test implementers do it in round 0 on production outside their scope. Put into every test-only task's implDirective: "Never edit a file outside your fileScope, not even temporarily; mutation probes on production code are the verifier's job (describe the mutant in your outbox)." Judging rule: [[review-calibration]].
   Before feeding that sentence forward, read the task's implDirective: a test-only bugfix whose red run needs a production probe (a probe clock in a config class) has a plan-mandated implementer edit. Word it "except the probe(s) the plan names, SHA'd copy, one run each, restored byte-identical".

4. **Injected seams proven only with fakes.** A design that injects a sleeper, random or clock lets unit tests prove the logic with recording fakes while the production implementation (`Thread.sleep` body) and its wiring (`millis -> { }` in the config) fall to no test; a gate grep is no regression guard. Name one task falsifier per seam: call the production implementation directly (elapsed at least N ms) and time the wired path as the MINIMUM over several calls against the pinned value (a lower bound never flakes on correct code).
