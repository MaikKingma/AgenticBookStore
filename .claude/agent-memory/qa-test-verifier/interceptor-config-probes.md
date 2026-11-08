---
name: interceptor-config-probes
description: Probe families for a Spring MVC HandlerInterceptor plus a validated @ConfigurationProperties record under @WebMvcTest, and which survivors to expect
metadata:
  type: feedback
---

Rule: probe an interceptor feature by family (comparison, order, bounds, path pattern, wiring, advice) and add a temporary witness test for every survivor before calling it a gap.

**Why:** the unit test with fake clock/random/sleeper kills the logic mutants, but the production wiring (real sleeper, registered path set, advice content type) survives unless a slice test measures it.
**How to apply:**
- Path pattern probes: `/**`, `/api/*`, no `addPathPatterns`, `/api/titles/**`, and an enumerated list that omits one controller. Only a slice test with a non-`/api` 404 path (plus a prefix lookalike such as `/apiary`) and a two-segment `/api` path kills the first three; the enumerated list survives unless a test goes through every controller.
- A `/**` interceptor surfaces in MockMvc as `ServletException: Request processing failed: <ChaosException>` on an unmapped path (the scoped advice does not apply), not as a status: the kill reason to quote.
- Record validation: probe each check removed and each bound shifted one step on both sides (`<` vs `<=`, `-0.02`, `1.01`) and a NaN-accepting rewrite; `@DefaultValue` on primitive components is behaviour-equivalent when removed.
- Real-sleep wiring: mutants `Thread.sleep(0L)`, sleep body removed and sleeper `millis -> { }` all survive a fake-sleeper unit test. Witness: slice with `latency-ms=200`, time one call with `System.nanoTime()`, assert `>= 200` (lower bound, not flaky); green on real code, red on all three.
- Advice content type: a preset `.contentType(...)` skips Accept negotiation, so only a request with `Accept: application/problem+json` kills its removal; also probe the handler written without the shared `envelope(...)` helper.
- Removing `Thread.sleep(millis)` alone is a compile error (unreachable catch): remove the whole try block or replace the argument.
- Slice context faults show as `ApplicationContext failure threshold (1) exceeded` on every later test: quote the first cause (`grep -o "No qualifying bean of type ..."`), not the repeated lines.
- Waiting for a background batch: a foreground `bash <script>` with a bounded `until grep -q DONE` loop (under 600 s) works; Monitor tools do not keep a subagent alive.

Related: [[env-switch-mutant-runner]], [[spring-runtime-probes]]
