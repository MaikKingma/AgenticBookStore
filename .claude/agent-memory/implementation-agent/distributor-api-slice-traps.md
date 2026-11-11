---
name: distributor-api-slice-traps
description: Traps and techniques for distributor @WebMvcTest slices and the QA C0 greps that scan product files (spec 05 T4)
metadata:
  type: reference
---
- QA C0-5d greps `distributor/src` (comments included) for `ProblemDetail`, `ResponseEntityExceptionHandler`,
  `UNPROCESSABLE_ENTITY`, `jakarta.validation`: keep those tokens out of Javadoc too.
- `@WebMvcTest(X.class)` with a nested `@TestConfiguration` (catalogue bean, fixed `Clock`) plus `@Import(RestockService.class)`
  loads the `@RestControllerAdvice` but not the MCP scanner: no `Registered tools:` line, no BeanPostProcessorChecker triple.
  Each slice class still adds one `Started` line.
- Jackson 3 default `ACCEPT_FLOAT_AS_INT` silently truncates `500.7` into an `Integer`/`int` body field. Bind numeric body fields
  that need range or wholeness checks as `BigDecimal` and check in the controller.
- 405 never reaches a scoped `@RestControllerAdvice` (null handler); use an unscoped advice with a request-path guard that
  rethrows the same exception outside `/api/`. Rethrowing the same instance is silent (a different one logs `Failure in
  @ExceptionHandler`); the rethrow path makes `DefaultHandlerExceptionResolver` log one expected WARN `Resolved [...]` line.
- Command payload coercion in the slice (Boot 4 / Jackson 3, a String record component): a JSON number or boolean is read as its
  text (`12.5` -> "12.5", `true` -> "true"); an object (`{}`) gives 400 with an EMPTY body and no content type
  (`HttpMessageNotReadableException`, not the problem-detail advice). Pin these as literals.
- Unstubbed `@MockitoBean` flow methods return null, so a 202 with `CommandResult(null)` passes silently: stub the id and assert
  `$.id` in every 202 test or a "null id" mutant survives.
