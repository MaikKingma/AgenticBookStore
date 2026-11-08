---
name: mcp-test-client-traps
description: Traps when testing Spring AI MCP tools with the MCP Java SDK client (mcp-core 2.1.0) in this repo - deprecated request constructor, Boolean isError, scanner check, required-param schema, exact-cap fixtures
metadata:
  type: reference
---

- `new McpSchema.CallToolRequest(name, Map)` is `@Deprecated` in mcp-core 2.1.0; javac prints "uses or overrides a
  deprecated API" in the referee log, which the QA warning inventory classes as a fixable FAIL. Use
  `CallToolRequest.builder(name).arguments(map).build()` (the no-arg `builder()` is deprecated too).
- `CallToolResult.isError()` and the `ToolAnnotations` hints are `Boolean` and may be null on success: assert
  `isNotEqualTo(Boolean.TRUE)` for "not an error", never `isFalse()`.
- Quick check that the `@McpTool` scanner picked a class up: the context log line
  `McpServerAutoConfiguration : Registered tools: <n>`.
- `@McpToolParam.required()` defaults to `true` and the server enforces it at runtime, so QA treats requiredness
  as testable: pin it in the integration test. `Tool.inputSchema()` is a `Map<String, Object>` (no `required()`
  accessor): read `getOrDefault("required", List.of())`. A call missing a required argument returns `isError` true
  and logs one `ToolInputValidator` WARN line; name that line in the outbox, since QA inventories warnings.
- A length-cap comparison (`>=` vs `>`, off by one) needs fixtures that hit the cap exactly: grow one fixture
  field one ASCII character at a time, using the production mapper as the oracle, to land on N and N-1.
- Recipe and the other server-side traps (protocol STREAMABLE, boxed params, CallToolResult errors): the res-1
  research artifact of spec 05 and `agent-os/specs/2026-11-08-1040-05-distributor/shape.md` "MCP recipe".
