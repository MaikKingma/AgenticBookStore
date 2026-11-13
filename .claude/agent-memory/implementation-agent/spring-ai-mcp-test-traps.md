---
name: spring-ai-mcp-test-traps
description: Testing Spring AI 2.0.2 @Tool beans and MCP tools (mcp-core 2.1.0) - callbacks without a context, log recorder, scripted ChatClient, stub AnthropicChatModel, MCP client request traps
applies-to:
  - bookstore/src/main/java/nl/openvalue/bookstore/assistant/**
  - distributor/**/mcp/**
  - bookstore/src/test/java/nl/openvalue/bookstore/assistant/**
metadata:
  type: reference
---
## Spring AI tools and ChatClient
- No context needed: `MethodToolCallbackProvider.builder().toolObjects(tools).build().getToolCallbacks()` gives `ToolCallback`s
  (`inputSchema()`, `call(json)`). A failing tool throws `ToolExecutionException`; `DefaultToolExecutionExceptionProcessor.builder()
  .build().process(e)` is what the model receives. Missing JSON arguments and `null` arrive as `null`: assert both.
- Log asserts without console capture: logback `ListAppender` on `LoggerFactory.getLogger(Type.class)`, level `TRACE` (`Level.ALL`
  is deprecated: javac warning, a QA FAIL), `setAdditive(false)`, restore both in `@AfterEach`. Throwable:
  `((ThrowableProxy) event.getThrowableProxy()).getThrowable()`.
- `UUID.fromString` is lenient for non-36-char input (`"1-2-3-4-5"` parses): check length 36 first. Jackson 3
  (`tools.jackson.databind.json.JsonMapper`) keeps null record components. A lazy stream leaves unused strict stubs: stub through
  one `thenAnswer` over a map.
- The whole ChatClient (tool loop, memory advisor) runs without a context: `ChatClient.builder(scriptedModel)` into the real
  `AssistantConfig` bean methods. `Prompt` keeps the caller's list by reference: a recording double must `List.copyOf`.
  `ToolCallingChatOptions.getToolCallbacks()` is null without tools. `defaultSystem(Resource)` reads with the platform charset:
  pass `UTF_8`.
- `MessageWindowChatMemory` cuts forward to the next USER message: a window witness needs an odd count (a first call that throws
  leaves its question unanswered).
- No `spring-ai-retry` here (`NonTransientAiException` does not exist). Live failures are `com.anthropic.errors.AnthropicException`
  subclasses (`AnthropicIoException` takes a `(String)`, HTTP ones such as `RateLimitException` only a builder). Check a plan's
  named types with `dependency:build-classpath` plus `javap`.
- Real `AnthropicChatModel` offline: `AnthropicChatModel.builder().options(AnthropicChatOptions.builder().apiKey(dummy)
  .baseUrl("http://127.0.0.1:<port>").model(..).maxTokens(..).build()).build()` against a JDK `HttpServer` on port 0. The stub
  response needs `id`, `type`, `role`, `model`, `content`, `stop_reason`, `stop_sequence`, `usage`. `defaultOptions(..)` takes the
  options BUILDER. Use `SoftAssertions`; never assert a whole request body string.

## MCP client tests
- `new McpSchema.CallToolRequest(name, Map)` and no-arg `builder()` are `@Deprecated` (javac warning, a QA FAIL): use
  `CallToolRequest.builder(name).arguments(map).build()`.
- `CallToolResult.isError()` and `ToolAnnotations` hints are `Boolean`, may be null: assert `isNotEqualTo(Boolean.TRUE)`.
- The scanner picked a class up when the log shows `McpServerAutoConfiguration : Registered tools: <n>`.
- `@McpToolParam.required()` defaults true and is enforced: pin it in the IT. `Tool.inputSchema()` is a Map: read
  `getOrDefault("required", List.of())`. A missing argument gives `isError` true plus one `ToolInputValidator` WARN: name it in
  the outbox (QA inventories warnings).
- A length-cap check needs fixtures hitting the cap exactly: grow a field one ASCII char at a time with the production mapper as
  oracle to land on N and N-1.
- More server-side traps: `agent-os/specs/2026-11-08-1040-05-distributor/shape.md` "MCP recipe".
