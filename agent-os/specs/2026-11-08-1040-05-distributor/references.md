# References: Spec 05 Distributor service

## Research summary (2026-11-08)

- MCP server (senior research): the `@McpTool` recipe works on Boot 4.1.2 and Spring AI 2.0.2, proven by a throwaway project with a Streamable HTTP round trip and a `java -jar` start under 1 s bound to 127.0.0.1. MCP Java SDK 2.1.0 (`mcp`, `mcp-core`, `mcp-json-jackson3`), protocol 2025-11-25 negotiated. `/mcp` is the default endpoint (`spring.ai.mcp.server.streamable-http.mcp-endpoint`). The annotation scanner is on by default. Traps: missing `protocol` starts SSE; primitive optional parameters fail on null; a thrown exception duplicates the error text; the default tool annotations say destructive. Manual POSTs to `/mcp` need `Accept: application/json, text/event-stream` and must echo `Mcp-Session-Id`. `@WebMvcTest` slices load no MCP beans.
- STDIO: the webmvc auto-configurations are guarded by `McpServerStdioDisabledCondition`; with `spring.ai.mcp.server.stdio=true` the common auto-configuration creates `StdioServerTransportProvider` and no web server starts. In-process test: swap `System.in`/`System.out` before starting the context, send `initialize`, assert `serverInfo`. Traps: empty `logging.pattern.console` logs a logback error to stderr; `logging.file.name` is relative to the working directory; quote `"off"` in YAML.
- Baseline: `@WebMvcTest` is `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`; ProblemDetail is not used by default; Jackson 3 reads records with `LocalDate` and enums without extra modules; a `@RestControllerAdvice` scoped with `basePackageClasses` renders the envelope for 400, 404, 422 (`HttpStatus.UNPROCESSABLE_CONTENT`) and 503; `@ConfigurationPropertiesScan` is not honoured in slices (use `@EnableConfigurationProperties` on the `WebMvcConfigurer`); `<finalName>distributor</finalName>` gives `distributor/target/distributor.jar` with no root pom change; working days skip Saturday and Sunday only.

## Reference code

- `http/catalogue.http`, `http/inventory.http`, `http/orders.http`: house style for `http/distributor.http`.
- `distributor/src/test/java/nl/openvalue/distributor/HealthEndpointTest.java`: the RANDOM_PORT context the MCP integration test shares.
- Spec 06 (`docs/spec-pack/specs/06-distributor-acl.md`): consumer of the REST wire format (EAN 9780141439518, 999 cents, `FIC004000`, 404 `TITLE_NOT_FOUND`, 422 `NOT_ORDERABLE`, 503 envelope).
