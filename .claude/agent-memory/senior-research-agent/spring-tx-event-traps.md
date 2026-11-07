---
name: spring-tx-event-traps
description: Spring 7 transactional event listener traps that change how after-commit and rollback tests must be built (listener order, swallowed after-commit exceptions, test bean registration)
metadata:
  type: reference
---

Verified on Spring Framework 7.0.10 / Boot 4.1.2 with a scratchpad probe project (technique: see the
research-agent memory `boot4-verification-techniques.md`, run the repo `mvnw -o -f <scratch pom>`).

- A plain `@EventListener` that throws aborts the multicaster loop. Unordered method listeners all have order
  `Ordered.LOWEST_PRECEDENCE`, so if the thrower runs first, `@TransactionalEventListener` adapters never register
  and AFTER_ROLLBACK stays silent too: a "no after-commit event" assertion passes for the wrong reason.
  Rule: for a "published then rolled back" proof use a `BEFORE_COMMIT` listener that throws on a toggle, and record
  AFTER_ROLLBACK as proof of publication. Order-independent.
- Exceptions thrown by AFTER_COMMIT listeners run in `afterCompletion` and are logged and swallowed; never assert
  inside a listener, record and assert in the test thread.
- AFTER_COMMIT runs synchronously on the caller thread before the proxy returns, with the finished transaction
  still bound (`isActualTransactionActive()` true): no polling needed in tests; listener data access needs
  `REQUIRES_NEW`.
- A test-sources `@Component` in the app package is component-scanned into every full context; with scan plus
  `@Import`, one bean survives under the scanned name. Register test listeners as stereotype-free classes via
  `@Import` on the shared integration base to keep them explicit.
- Port adapters using `saveAndFlush` (this repo's spec 02 pattern) fail before publish, so "fail the port" cannot
  prove a rollback after publish.

Related: [[spring-mvc-validation-traps]]
