---
name: after-commit-listener-probes
description: Mutation probes for an AFTER_COMMIT listener plus REQUIRES_NEW delegate in the bookstore process layer, which mutants are IT-equivalent, and how to read the outcomes
metadata:
  type: feedback
---

Rule: probe the listener phase, the delegate propagation and the catch placement end to end, and expect two IT-equivalent mutants that only an annotation pin in the unit test kills.

**Why:** the flow carries a class-level `@Transactional`, so some delegate mutants behave exactly like the real code in every IT.
**How to apply:**
- One IT class plus the two unit classes with `clean` takes about 9 s per probe, so the generator-plus-runner pair from [[env-switch-mutant-runner]] fits (23 probes in about 7 minutes).
- IT-equivalent (the unit pin must exist): delegate `Propagation.NOT_SUPPORTED` (suspends the finished outer transaction; the flow then opens its own) and listener `fallbackExecution = true`.
- Plain `@EventListener` with the REQUIRES_NEW delegate hangs on the row lock the publishing transaction holds: run it last with a short budget (exit 124 is the expected informational result) and check `pgrep -fl surefire` and `docker ps` are empty afterwards.
- `noRollbackFor = RuntimeException.class` on the delegate still rolls back (the flow's inner REQUIRED marks rollback-only), so Spring logs `UnexpectedRollbackException` on the afterCompletion ERROR line. A test that pins the exception type on the line after the ERROR kills it; a test that only counts the ERROR line does not.
- `@TransactionalEventListener(condition = ...)` mutants with a record payload: use `#root.args[0].available()`; test both sides of the split (`< n` and `>= n`).
- Attribute each new afterCompletion ERROR, dispatcherServlet ERROR and flow WARN line by the nearest preceding `Running nl...` line in the bookstore section, then by a literal only one test throws (`grep -c` it in the test file).

Related: [[integration-test-probes]], [[spring-runtime-probes]]
