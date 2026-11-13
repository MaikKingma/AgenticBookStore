---
name: qa-test-infra-guards
description: Guarding shared test infrastructure and failure devices: cleaning hooks needing a guard test, Vitest console trap guard, shared integration base context key, rollback and after-commit listener probes, boundary-probe wording
metadata:
  type: feedback
---

Siblings: [[qa-probe-design-traps]], [[qa-directive-traps]].

1. **Shared test-infrastructure hooks need a guard test, not a code-review fallback.** A "report whether removing `@BeforeEach` fails a test, else inspection" item leaves the hook unguarded, and once accepted at strategy time I cannot REJECT later. Demand a guard: a `@TestInstance(PER_CLASS)` subclass seeds a row in `@BeforeAll` and its test asserts the row is gone. FK-linked tables truncated in ONE statement need no CASCADE, and CASCADE masks a partial table list: require an unlinked table.
   - A Vitest console trap (`expect(calls).toEqual([])` in `afterEach`) is the same: deleting the expect, or collecting after `restoreAllMocks`, survives every committed test. Vitest flips `test.fails` AFTER `afterEach` and runs `onTestFailed` before the flip, so the guard is `it.fails` per console method with an `onTestFailed` capture of the messages, plus a next test asserting the trap message. Write it WITH a named exemption from the C0 `.fails(` grep.
   - The trap does NOT see jsdom's virtual console: `Not implemented:` (a submit without `preventDefault`) goes to raw stderr and the test passes. Guard that class with a `Not implemented` grep on a stderr-capturing log, plus pins such as `defaultPrevented` from a `document` submit listener.

2. **Shared test base changes.** Adding an `@Import` or `@MockitoSpyBean` to the integration base changes the context key; if any full-context class does not extend the base, the Started count jumps by one: check `extends` on every `@SpringBootTest` class at strategy time.

3. **Test-only failure devices** (rollback thrower, spy `doThrow`) need their own non-vacuity probe: swap in the weak device (plain `@EventListener` thrower without `@Order`) or break the `@Transactional` boundary, and the scenario must fail.
   - Never write the boundary probe as "remove the class-level `@Transactional`": it also keeps the session open for lazy loads, so every request dies with LazyInitializationException before any write and cannot tell "rolled back" from "never written". Write `@Transactional(noRollbackFor = RuntimeException.class)`; a `REQUIRES_NEW` on one save works only when its rows reference already committed data. Under that probe a BEFORE_COMMIT listener thrower still rolls back, so the device scenario needs its own probes (toggle ignored, plain `@EventListener`, recorder on AFTER_COMPLETION).
   - After-commit listener feeding a REQUIRES_NEW delegate: a business failure before publish (409) proves no phase; the must-fail set is REQUIRED delegate and AFTER_ROLLBACK (killed by the visible side effect) plus AFTER_COMPLETION (killed by publish-then-rollback via `failBeforeCommit`). A plain `@EventListener` probe blocks on the outer transaction's row lock (exit 124): informational, with a budget. `NOT_SUPPORTED` and `fallbackExecution = true` survive every IT; only reflection pins kill them.
