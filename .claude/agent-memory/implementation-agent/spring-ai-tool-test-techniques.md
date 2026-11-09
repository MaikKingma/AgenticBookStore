---
name: spring-ai-tool-test-techniques
description: Testing Spring AI 2.0.2 @Tool beans without a Spring context - callbacks, error processor, log recorder, JDK UUID leniency
metadata:
  type: reference
---

- No context needed: `MethodToolCallbackProvider.builder().toolObjects(tools).build().getToolCallbacks()` gives
  `ToolCallback`s (name, description, `inputSchema()` JSON, `call(json)` returning JSON). A failing tool makes `call`
  throw `ToolExecutionException`; `DefaultToolExecutionExceptionProcessor.builder().build().process(e)` is exactly what
  the model receives (the message only). Missing JSON arguments and `null` arrive as `null`; assert both.
- A tool exception for the model: a private static `RuntimeException` built with `super(msg, null, false, false)` carries
  a one-line text with no stack trace. Catch it first in the helper (before the catch-all) so it is not rewrapped.
- Log assertions without console capture: logback `ListAppender` on `(ch.qos.logback.classic.Logger)
  LoggerFactory.getLogger(Type.class)`, set the level to `TRACE` (`Level.ALL` is deprecated: javac warning, a QA
  FAIL), `setAdditive(false)` so expected WARN traces stay out of the build log, restore level and additive in
  `@AfterEach`. Throwable check: `((ThrowableProxy) event.getThrowableProxy()).getThrowable()` is the instance.
- `UUID.fromString` is lenient for non-36-char input (`"1-2-3-4-5"` parses). A strict id check needs a length 36 test
  first; keep that string in the malformed-id witnesses.
- Jackson 3 (`tools.jackson.databind.json.JsonMapper`) keeps null record components in Spring AI's tool JSON, so
  "key present with null" holds without extra config.
- Stream laziness: `sorted(..).map(lookup).filter(..).limit(n)` stops looking stock up after n hits, but a Mockito
  strict-stub-per-book then fails on unused stubs: stub through one `thenAnswer` over a map instead.
