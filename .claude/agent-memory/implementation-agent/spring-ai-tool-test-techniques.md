---
name: spring-ai-tool-test-techniques
description: Testing Spring AI 2.0.2 @Tool beans without a Spring context - callbacks, error processor, log recorder, JDK UUID leniency, scripted ChatClient, real AnthropicChatModel against a stub
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
  LoggerFactory.getLogger(Type.class)`, level `TRACE` (`Level.ALL` is deprecated: javac warning, a QA FAIL),
  `setAdditive(false)` so expected WARN traces stay out of the build log, restore both in `@AfterEach`. The throwable:
  `((ThrowableProxy) event.getThrowableProxy()).getThrowable()`.
- `UUID.fromString` is lenient for non-36-char input (`"1-2-3-4-5"` parses): a strict id check needs a length-36 test first;
  keep that string in the malformed-id witnesses. Jackson 3 (`tools.jackson.databind.json.JsonMapper`) keeps null record
  components in the tool JSON. A lazy stream (`sorted(..).map(lookup).filter(..).limit(n)`) leaves unused strict stubs per
  book: stub through one `thenAnswer` over a map.
- The whole ChatClient (tool loop, memory advisor) runs without a context: `ChatClient.builder(scriptedModel)` passed to the
  real `AssistantConfig` bean methods. `Prompt` keeps the caller's list by reference, so a recording double must `List.copyOf`
  the messages. `ToolCallingChatOptions.getToolCallbacks()` is null for a ChatClient without tools: guard it.
  `ChatClient.Builder.defaultSystem(Resource)` reads with the platform charset; pass `UTF_8`.
- `MessageWindowChatMemory` moves its cut forward to the next USER message, so complete Q/A turns cannot tell window 20 from
  21. A window witness needs an odd count: a first call that throws leaves its question unanswered in memory.
- A bean method injecting `ChatClient.Builder` breaks every `ApplicationContextRunner` over that config: register a builder and
  the tools with `withBean(...)`.
- Spring AI 2.0.2 here has no `spring-ai-retry`: `NonTransientAiException`/`TransientAiException` do not exist. A live
  Anthropic failure is a `com.anthropic.errors.AnthropicException` subclass (`AnthropicIoException`, `AnthropicRetryableException`
  take a `(String)`, HTTP ones such as `RateLimitException` only a builder). Check a plan's named types with
  `dependency:build-classpath` plus `javap`.
- Real `AnthropicChatModel` without a key or network: `AnthropicChatModel.builder().options(AnthropicChatOptions.builder()
  .apiKey(dummy).baseUrl("http://127.0.0.1:<port>").model(..).maxTokens(..).build()).build()` against a JDK `HttpServer` on
  port 0 (sendResponseHeaders with the byte length). The stub's response needs `id`, `type`, `role`, `model`, `content`,
  `stop_reason`, `stop_sequence`, `usage`. `ChatClient.Builder.defaultOptions(..)` takes the options BUILDER (`.build()` does
  not compile) and merges with the model's defaults. Use `SoftAssertions`; do not assert on a whole request body string
  (failure output floods).
- `@Tool` description = `getToolDefinition().description()`; `@ToolParam(description=..)` shows under `inputSchema()`
  `properties.<name>.description`.
