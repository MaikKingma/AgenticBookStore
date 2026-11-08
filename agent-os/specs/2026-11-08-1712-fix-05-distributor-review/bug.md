---
bug: fix-05-distributor-review
severity: S3
path: lite+research
status: open
closes: [spec 05 review finding 1 (chaos random), finding 2 (fractional quantity), finding 3 (midnight flake), finding 4 (405/415 envelope)]
---
# Bug: four defects found in the read-only review of spec 05 (distributor)

The full research report, with every command and output excerpt, is
`.claude/comms/mailboxes/res-1/outbox/research-fix-05-designs.md` (res-1, senior-research-agent, opus). It is
gitignored comms state, so the facts that matter are restated here.

## Report
- **Finding 1, observed:** `ChaosWebConfig` hands one `RandomGenerator.getDefault()` (an `L32X64MixRandom`) to the
  singleton `ChaosInterceptor`, which draws from it on every Tomcat worker thread at the same time.
- **Finding 1, expected:** the failure draw is safe under concurrent requests, so the realised failure share follows
  `distributor.chaos.failure-rate`. Unit tests keep their injected fake random.
- **Finding 2, observed:** `POST /api/restock-orders` with `{"quantity": 500.7}` gives 201 with quantity 500, and
  `1.5` gives 201 with quantity 1.
- **Finding 2, expected:** a non-whole quantity is 400 `INVALID_REQUEST` (spec 05: 400 for a quantity outside 1 to
  500; spec 05 decision D-b: 400 `INVALID_REQUEST` for a missing, non-numeric or out-of-range quantity), never
  truncated. Global Jackson behaviour stays unchanged.
- **Finding 3, observed:** `McpServerIntegrationTest.checkAvailabilityReportsStockAndDelivery` computes `today` from
  the real clock before the tool calls. A run that crosses 00:00 Europe/Amsterdam expects delivery dates one day off.
- **Finding 3, expected:** the test is deterministic and the shared Spring context with `HealthEndpointTest` stays one
  context.
- **Finding 4, observed:** `PUT /api/restock-orders` (405) and `POST /api/restock-orders` without
  `Content-Type: application/json` (415) return Spring Boot's default error body.
- **Finding 4, expected:** spec 05: "Errors use the distributor's own envelope, not ProblemDetail". 405 and 415 keep
  their status and return `{ "errorCode", "message" }`. The accepted known limit stays: unknown `/api/...` paths answer
  with Boot's 404 body.
- **Environment:** factory a8fceb1 (spec 05 merged as PR #43), Java 25.0.4, Spring Boot 4.1.2, Spring Framework
  7.0.10, Jackson 3.1.7.
- **Regression window:** not a regression. Finding 1 came in with 2ec1d24 (spec 05 T5, PR #53), findings 2 and 4 with
  63c8d4c (spec 05 T4, PR #52), finding 3 with 05beb23 (spec 05 T3, PR #50).
- **Frequency:** findings 2 and 4 always; finding 1 under concurrent `/api` calls with chaos on; finding 3 only on a
  run that crosses midnight Amsterdam.
- **Severity: S3 each.** A demo knob drifts, a malformed request is accepted with a changed value, an error body breaks
  the README contract, a test can flake. No persisted data, no money, numbering or auth.

## Reproduction
- Status: reproduced (all four), by res-1 in a throwaway worktree of a8fceb1.
- **Finding 1:** JDK 25 javadoc of `L32X64MixRandom`: "instances ... are *not* thread-safe". Stress probe outside the
  project, 8 threads x 500k `nextLong()` on one shared `getDefault()` instance: duplicates in 50 of 50 trials (threads
  replay the same stream); per-call `ThreadLocalRandom.current()`: 0 of 50. Through `preHandle` only a boolean is
  visible, so no deterministic behavioural test exists through the pre-fix interfaces.
- **Finding 2:** live app, `POST /api/restock-orders` `{"ean":"9780141439518","quantity":500.7}` -> 201 quantity 500;
  `1.5` -> 201 quantity 1; `0.5` -> 400 (truncated to 0). Jackson 3.1.7 `StdDeserializer._parseInteger`
  (`StdDeserializer.java:815-822`) returns `p.getValueAsInt()` for a float token while `ACCEPT_FLOAT_AS_INT` is on.
- **Finding 3:** probe clock in `CatalogConfig` (probe tree only) returning Thursday 2026-11-05T23:59:59.999
  Amsterdam on thread `main` and Friday 2026-11-06T00:00:00.001 on any other thread. The server reads the clock for
  `check_availability` on Tomcat threads. Result:
  `checkAvailabilityReportsStockAndDelivery:182 expected: {... "expectedDeliveryDate"="2026-11-09" ...} but was: {... "expectedDeliveryDate"="2026-11-10" ...}`.
- **Finding 4:** live app: `PUT /api/restock-orders` -> 405, `Allow: POST`, body
  `{"timestamp":...,"status":405,"error":"Method Not Allowed","path":"/api/restock-orders"}`; `DELETE /api/titles` ->
  405, `Allow: GET`, same Boot body; `POST /api/restock-orders` with `text/plain` or no content type -> 415,
  `Accept: application/json, application/yaml, application/*+json`, Boot body. MockMvc: 405 has `Handler: Type = null`,
  415 has handler `RestockOrdersController#place`.
- Regression-test layers: web slice (`@WebMvcTest`) for findings 2 and 4, unit for finding 1, the existing
  `@SpringBootTest` MCP round trip for finding 3 (the same layers spec 05 used for these behaviours).
- Command pattern: `./mvnw -pl distributor test -Dtest='<Class>[#method]' -Dsurefire.failIfNoSpecifiedTests=false`
  from the repo root.

## Root cause
- **Finding 1.** Causal chain: drifting failure share -> concurrent unsynchronised `nextDouble()` at
  `ChaosInterceptor.java:40` -> **root cause `distributor/src/main/java/nl/openvalue/distributor/api/ChaosWebConfig.java:26`**
  creates one non-thread-safe `L32X64MixRandom` for the singleton interceptor -> missed because the chaos tests are
  single-threaded and inject a fake random.
  One sentence: the production wiring shares a generator the JDK documents as not thread-safe across all request
  threads.
- **Finding 2.** Causal chain: 201 with a changed quantity -> the controller check at
  `RestockOrdersController.java:34-35` only sees the already truncated integer -> **root cause
  `distributor/src/main/java/nl/openvalue/distributor/api/RestockOrderRequest.java:7`** declares `Integer quantity`, so
  Jackson's default `ACCEPT_FLOAT_AS_INT` truncates the float token while reading the body -> missed because the tests
  covered missing, non-numeric and out-of-range values but no fraction.
- **Finding 3.** Causal chain: dates one day off -> the test reads the live clock once on the JUnit thread and the
  server reads it again per tool call on a Tomcat thread (`RestockService.java:36`) -> **root cause
  `distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java:171`** computes the
  expectation from the real `Clock.system(Europe/Amsterdam)` bean (`CatalogConfig.java:18`) -> missed because no run
  crossed midnight.
- **Finding 4.** Causal chain: Boot's body -> no `@ExceptionHandler` answers -> **root cause
  `distributor/src/main/java/nl/openvalue/distributor/api/ApiExceptionHandler.java:17`**: the advice is scoped with
  `basePackageClasses = TitlesController.class` and maps neither exception. 405 is thrown during handler lookup
  (`RequestMappingInfoHandlerMapping.java:260`) with a null handler, and Spring Framework 7 never consults a scoped
  advice for a null handler (`HandlerTypePredicate.test(null)` is false when selectors are set). 415 is thrown inside
  the handler call (no `consumes` on the mapping), where the scoped advice is asked but has no handler -> missed
  because spec 05 scenarios cover only the named error codes.
- Contributing factor (all four): the spec 05 review checklist had no concurrency, numeric coercion, wall-clock or
  "every error status" item.

## Blast radius
- [x] Same pattern elsewhere:
  - `grep -rn -E "RandomGenerator|java\.util\.Random|new Random\(|Math\.random|ThreadLocalRandom|SecureRandom|SplittableRandom" distributor/src`
    finds only `ChaosWebConfig`, `ChaosInterceptor` and `ChaosInterceptorTest`. `UUID.randomUUID()` is thread-safe.
    -> T1.
  - Request bodies: `RestockOrdersController.java:29` is the only `@RequestBody`. `page` and `size` are `int`
    `@RequestParam`s converted by Spring (`1.5` is already a 400 envelope). MCP `limit` is validated against the tool
    JSON schema by the MCP SDK (`2.7` rejected, `1e1` accepted). -> T1 for the body; the rest deferred: not affected.
  - Wall clock in tests:
    `grep -rn -E "LocalDate\.now\(|Instant\.now\(|Clock\.system|LocalDateTime\.now\(|ZonedDateTime\.now\(|System\.currentTimeMillis" distributor/src`
    finds the bug line and `CatalogConfigTest.java:19` (a deliberate real-clock check, not date-sensitive). -> T3;
    `CatalogConfigTest` deferred: correct as is.
  - Unmapped Spring MVC exceptions: 406 (`Accept: text/plain`) has an empty body; deferred, out of scope (not named in
    the review). `NoResourceFoundException` on unknown `/api/...` paths: accepted known limit of spec 05, kept.
    `MissingServletRequestParameterException`: unreachable (every parameter optional or defaulted).
- [x] Other callers: `RestockOrderRequest` has no other constructor callers; `ChaosInterceptor` is built only by
  `ChaosWebConfig` and `ChaosInterceptorTest`. No disposition needed.
- [x] Persisted data already wrong: none (in-memory, lost on restart).
- [x] Produced artifacts: none.
- [x] Docs that encode the wrong assumption: `distributor/README.md:34-42` "Every error is JSON" -> T2 adds the 405
  and 415 rows; `README.md:31` "quantity 1 to 500" -> T1 says "a whole number from 1 to 500".
  `http/distributor.http:74` mentions the range in a comment only; unchanged.
- [x] Sibling environments: none (no CI or seed difference).
- Noted, not part of this bug: `POST /mcp` with a bad `Accept` header returns a serialised `McpError` with a stack
  trace (Spring AI 2.0.2 transport). Follow-up.

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | four small, local changes, each verified in the probe tree |
| (b) revert | no | would remove spec 05 features |
| (c) guarded workaround + follow-up | no | nothing to guard |

Chosen: (a).
- F1: `ChaosInterceptor` gets a package-private `Supplier<RandomGenerator>` seam; the public `RandomGenerator`
  constructor stays and wraps the instance; `ChaosWebConfig` passes `ThreadLocalRandom::current`, so every draw uses
  the calling thread's generator.
- F2: `RestockOrderRequest.quantity` becomes `BigDecimal`; the controller rejects a missing, out-of-range or
  non-whole value with 400 `INVALID_REQUEST` "quantity must be a whole number between 1 and 500", after the ean check,
  then passes `intValueExact()`. Global Jackson config unchanged.
- F4: a new unscoped `@RestControllerAdvice` `ApiRequestMismatchHandler` maps 405 and 415 to the envelope for paths
  under `/api/` only, keeping status and the `Allow` / `Accept` headers; elsewhere it rethrows the same exception so
  Boot answers as today (`/actuator`, `/mcp`).
- F3: a shared top-level `@TestConfiguration` with a `@Primary` fixed Friday clock, imported by both
  `McpServerIntegrationTest` and `HealthEndpointTest` (one context, as today); the test asserts literal dates.

## Prevention
- Gap: the review-time checklist for distributor specs had no item for shared mutable state in singletons, numeric
  coercion of request bodies, wall-clock reads in tests, or "every error status uses the envelope".
- Action: follow-up, recorded for the main session (progress.md is theirs): add these four checks to the review brief
  of later specs (06, 07, 08). No standard changes in this cycle.

## Verification
- Regression tests red on unfixed, green on fixed (impl `## Red evidence`, QA `## Negative control`).
- Blast-radius items closed or deferred.
- `./mvnw clean verify` from the repo root green within 60 s.
