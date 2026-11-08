---
name: spring-ai-mcp-traps
description: Spring AI 2.0.x MCP server (webmvc, @McpTool) traps found by source reading and a scratch round trip; plus how to verify MCP behaviour fast
metadata:
  type: reference
---

Traps (Spring AI 2.0.2, MCP Java SDK 2.1.0; re-check on a BOM bump):
- `spring.ai.mcp.server.protocol` MUST be set: the field default says STREAMABLE but the
  @ConditionalOnProperty conditions use matchIfMissing=true for SSE, so an absent property starts SSE.
- A RuntimeException thrown from an @McpTool method becomes isError=true with the message twice
  (message + root-cause message; root cause is the exception itself). Return a CallToolResult with
  isError(true) for a clean tool error.
- Optional @McpToolParam(required=false) params must be boxed: a primitive int with a missing
  argument fails with an NPE error result. No default-value attribute exists.
- @McpAnnotations defaults are readOnlyHint=false, destructiveHint=true: set them for read-only tools.
- Tool results are serialized by Spring AI's own JsonHelper (default Jackson 3 mapper), not Boot's.
- Tool arguments are validated against the generated JSON Schema by the MCP SDK before binding: `limit: 2.7` or
  `"3"` is a tool error, `1e1` passes. `spring.jackson.*` changes do not reach this path.
- Raw curl round trip works: initialize, read `Mcp-Session-Id` header, send notifications/initialized, tools/call;
  Accept must list both application/json and text/event-stream.
- The MCP client classes (HttpClientStreamableHttpTransport, McpClient) are in mcp-core, which the
  server starter brings in, so tests need no extra dependency. The BOM does not manage io.modelcontextprotocol.sdk.

Verification technique:
- Sources jars: `./mvnw -q dependency:get -Dartifact=<g:a:v>:jar:sources` run from the worktree root
  (the Bash guard refuses a quoted absolute mvnw path containing spaces).
- Scratch Boot project outside the repo, built with `./mvnw -f <scratch>/pom.xml clean verify`
  from the worktree root. A SpringBootTest with `spring.config.name=does-not-exist` shows what
  activates when a property is absent.
