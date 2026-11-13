---
name: web-mvc-slice-traps
description: @WebMvcTest slice traps in bookstore and distributor - nested contexts, ProblemDetail shape, 405 and advice scope, Jackson 3 coercion, view controller order, QA C0 greps on product files
applies-to:
  - bookstore/src/main/java/nl/openvalue/bookstore/command/**
  - bookstore/src/main/java/nl/openvalue/bookstore/query/**
  - distributor/**/api/**
  - "*ControllerTest.java"
metadata:
  type: reference
---
- A `@Nested` class with `@NestedTestConfiguration(OVERRIDE)` and its own `@WebMvcTest` gets its own context (one more Started
  line). Spring injects the ENCLOSING instance from the nested context, so outer `@MockitoBean` fields fail with "No bean override
  instance found". Fix: declare mocks on the class (`@MockitoBean(types = {A.class, B.class})`) and `@Autowired` the fields. The
  outer `@BeforeEach` also runs for nested tests; `when(...)` calls do not count for `verifyNoInteractions`.
- `@WebMvcTest(X.class)` plus a nested `@TestConfiguration` and `@Import(Service.class)` loads the `@RestControllerAdvice` but not
  the MCP scanner: no `Registered tools:` line. Each slice class adds one `Started` line.
- `ProblemDetail` from an `@ExceptionHandler` serialises without `type` when `about:blank`, `instance` = request path, `title` =
  reason phrase when unset. `ErrorResponseException` (429 etc.) needs an advice method returning `exception.getBody()`.
- 405 never reaches a scoped `@RestControllerAdvice`: use an unscoped advice with a request-path guard rethrowing the same
  exception outside `/api/` (a different instance logs `Failure in @ExceptionHandler`; the rethrow logs one expected WARN).
- Jackson 3 `ACCEPT_FLOAT_AS_INT` silently truncates `500.7` into an int field: bind numeric fields needing wholeness checks as
  `BigDecimal`. A String record component reads a JSON number or boolean as its text (`12.5` -> "12.5"); an object gives 400 with
  an EMPTY body and no content type. Pin these as literals.
- Unstubbed `@MockitoBean` flow methods return null, so a 202 with `CommandResult(null)` passes: stub the id, assert `$.id`.
- Mock a ChatClient chain explicitly (`advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())`), not deep stubs.
- `ViewControllerRegistry` order is 1; controllers 0, actuator earlier: an extra `addRedirectViewController("/books", ...)` is an
  equivalent mutant (observable only with `registry.setOrder(-1000)`). Tell the verifier before it probes. `RestTestClient` does
  not follow redirects; read `EntityExchangeResult.getUrl()` to resolve an absolute `Location`.
- Boot parent pom `<resources>` uses `${basedir}/src/main/resources`; the effective pom prints the relative form: copy `${basedir}`.
- QA C0-5d greps `distributor/src` (comments too) for `ProblemDetail`, `ResponseEntityExceptionHandler`, `UNPROCESSABLE_ENTITY`,
  `jakarta.validation`: keep those tokens out of Javadoc.
