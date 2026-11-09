---
name: distributor-api-slice-traps
description: Traps and techniques for distributor @WebMvcTest slices and the QA C0 greps that scan product files (spec 05 T4)
metadata:
  type: reference
---

- QA C0-5d greps `distributor/src` (comments included) for `ProblemDetail`, `ResponseEntityExceptionHandler`,
  `UNPROCESSABLE_ENTITY`, `jakarta.validation`. Keep those tokens out of Javadoc too; a Javadoc "not a ProblemDetail"
  would fail the gate.
- `@WebMvcTest(X.class)` with a nested `@TestConfiguration` (catalogue bean, fixed `Clock`) plus `@Import(RestockService.class)`
  loads the `@RestControllerAdvice` but not the MCP scanner: no `Registered tools:` line, no BeanPostProcessorChecker triple.
  Each slice class still adds one `Started` line.
- Error bodies set `contentType(MediaType.APPLICATION_JSON)` on the `ResponseEntity`; MockMvc `content().contentType(APPLICATION_JSON)`
  then matches exactly (no charset suffix).
- An unhandled exception in a slice shows up as a thrown `ServletException` (test ERROR, not a 500 assertion), which
  is how a missing request check surfaces.
- Jackson 3 default `ACCEPT_FLOAT_AS_INT` silently truncates `500.7` into an `Integer`/`int` body field. Bind numeric
  body fields that need range or wholeness checks as `BigDecimal` and check in the controller (fix-05 T1); a guard test
  that asserts a reworded error message cannot be green on the pre-fix code, so say so in the evidence.
- 405 never reaches a scoped `@RestControllerAdvice` (null handler); use an unscoped advice with a request-path guard
  that rethrows the same exception outside `/api/` (fix-05 T2). Rethrowing the same instance is silent; only a different
  one logs `Failure in @ExceptionHandler`. The rethrow path makes `DefaultHandlerExceptionResolver` log one WARN
  `Resolved [...]` line in the module run, a new but expected line.
- Exact JSON asserts: read the body with a static `JsonMapper.builder().build()` into `Map<String, Object>`; ints come
  back as `Integer`, so `isEqualTo(7)` works and `Map.equals` pins the key set.
- Command payload coercion in the @WebMvcTest slice (Boot 4 / Jackson 3, a String record component): a JSON number or
  boolean is read as its text (`12.5` -> "12.5", `true` -> "true"); an object (`{}`) gives 400 with an EMPTY body and no
  content type (`HttpMessageNotReadableException`, not the problem-detail advice). Pin these as literals (spec 06 T5).
- Unstubbed `@MockitoBean` flow methods return null, so a 202 with `CommandResult(null)` passes silently: stub the id and
  assert `$.id` in every 202 test or a "null id" mutant survives.
