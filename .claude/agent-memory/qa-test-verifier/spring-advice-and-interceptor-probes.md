---
name: spring-advice-and-interceptor-probes
description: @ControllerAdvice handlers, MVC interceptors, validated config records and config-precedence pins
applies-to:
  - bookstore/src/main/java/**/command/**
  - bookstore/src/main/java/**/query/**
  - bookstore/src/main/resources/application*
metadata:
  type: feedback
---
Rule: probe each advice handler's parts separately (status, title, code, `details`), and add a temporary web-slice witness for every survivor.
**Why:** tests that throw exceptions with empty details let `details` passed as `List.of()` survive for the 404 and 409 handlers; slice tests run with production defaults, so a handler hard-coding a default text survives everything.
**How to apply:**
- Sort "by A then B": a tiebreak probe drops only `thenComparing`; a test whose input already arrives in B order lets the field-only sort survive. Witness class fails on the mutant, passes on real code, then `rm` under the snapshot rule.
- `details`: witness builds the exception with the four-argument constructor (non-empty details) and asserts `$.details[0]...`; run on real code and each mutant.
- Forwarding handlers (`return exception.getBody()` for an `ErrorResponseException`, a details list built from a nested exception message that names a configured limit): probe `ProblemDetail.forStatusAndDetail(<status>, "<default text>")`; the witness passes a non-default exception through the handler (direct call or standalone MockMvc with the advice).
- Pass-through fix tasks: count the DISTINCT codes the tests feed each handler yourself, including sibling 503 (`UnavailableException`) handlers. Probe each with code and title hard-coded (title-only too, to prove `$.title` is load-bearing); a handler fed one code only survives.
- An advice made to extend `ResponseEntityExceptionHandler` while it handles `HttpMessageNotReadableException` itself fails context start ("Ambiguous @ExceptionHandler method mapped"): every slice test errors; quote that line.
- Unscoped path-guarded advice that rethrows (405/415): probes are guard -> always-API, guard removed per handler method, headers dropped, Accept collapsed to one type, content type removed / problem+json, status hard-coded, message without the method. The rethrow-same-exception claim is proven by one mutant throwing a NEW exception of the same type: the log must then show `Failure in @ExceptionHandler` (baseline 0). A guard on a handler type no non-API route can raise (415 outside /api in the distributor) survives and is unobservable over HTTP: say so, suggest a direct unit test, do not FAIL.
- Config-precedence pins (a test property source that must beat exported OS env vars): one swap runner taking `<id> <mutant|fixed> <plain|exp> <-Dtest>` gives control, falsifiers and exported proofs (about 10 s per run; `env VAR=... ./mvnw` inside the script). Falsifiers: each pinned property removed, an extra model pin added back, the pin changed to the real API host. A mistyped mutant path makes `cp` fail and the run silently a control run: abort on a failed `cp`. Prove "nothing leaves the machine" by counting `Failed to connect` lines per host.

Rule: probe an interceptor feature by family (comparison, order, bounds, path pattern, wiring, advice) and add a temporary witness test for every survivor before calling it a gap.
**Why:** a unit test with fake clock/random/sleeper kills the logic mutants, but production wiring (real sleeper, registered path set, advice content type) survives unless a slice test measures it.
**How to apply:**
- Path patterns: `/**`, `/api/*`, no `addPathPatterns`, `/api/titles/**`, an enumerated list omitting one controller. Only a slice test with a non-`/api` 404 path (plus a lookalike `/apiary`) and a two-segment `/api` path kills the first three; the enumerated list survives unless a test goes through every controller.
- Record validation: each check removed, each bound shifted one step on both sides (`<` vs `<=`, `-0.02`, `1.01`), a NaN-accepting rewrite; `@DefaultValue` on primitive components is equivalent when removed.
- Real-sleep wiring: `Thread.sleep(0L)`, sleep body removed and sleeper `millis -> { }` all survive a fake-sleeper unit test. Witness: slice with `latency-ms=200`, time one call with `System.nanoTime()`, assert `>= 200` (lower bound, not flaky). Removing only `Thread.sleep(millis)` is a compile error (unreachable catch): remove the whole try block or replace the argument.
Related: [[spring-runtime-probes]], [[env-switch-mutant-runner]]
