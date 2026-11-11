---
name: web-slice-nested-contexts
description: Two web slice contexts in one test class (@Nested with its own @WebMvcTest), ProblemDetail JSON shape, ErrorResponseException and mocked ChatClient chains
metadata:
  type: reference
---
- A `@Nested` class with `@NestedTestConfiguration(OVERRIDE)` and its own `@WebMvcTest` gets its own context (one more Started
  line). Spring injects the ENCLOSING instance from the nested context, so outer `@MockitoBean` fields fail with "No bean override
  instance found for BeanOverrideHandler". Fix: declare the mocks on the class (`@MockitoBean(types = {A.class, B.class})`) and
  `@Autowired` the fields; the nested class declares its own type-level mocks, and the outer fields resolve to its beans.
- The outer `@BeforeEach` also runs for nested tests, on those nested beans. `when(mock.x())` stubbing calls do not count for
  `verifyNoInteractions`.
- A `ProblemDetail` returned from an `@ExceptionHandler` serialises without `type` when it is `about:blank`, with `instance` =
  request path, and `title` = reason phrase when unset. Exact body maps: title, status, detail, instance, plus custom properties.
- `ErrorResponseException` (Spring's 429 etc.) needs an advice method returning `exception.getBody()`; otherwise
  `DefaultHandlerExceptionResolver` sends Boot's error JSON (empty body in MockMvc).
- Mock a ChatClient chain explicitly (request spec and call spec mocks, `advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())`),
  not deep stubs; check advisor params by applying the captured consumer to a small recording `AdvisorSpec`.
