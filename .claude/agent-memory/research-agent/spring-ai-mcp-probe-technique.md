---
name: spring-ai-mcp-probe-technique
description: How to verify Spring AI 2.0.x MCP starter behaviour (stdio vs HTTP) cheaply, plus probe traps
metadata:
  type: reference
---

- Autoconfig conditions: `javap -v -p -cp <jar> <class>` on `spring-ai-autoconfigure-mcp-server-common` and `-webmvc` jars (Maven cache) shows `@Conditional*`; no sources jar needed. `McpServerStdioDisabledCondition` is the stdio switch.
- Probe: scratch Boot parent 4.1.2 + spring-ai-bom 2.0.2 + `spring-ai-starter-mcp-server-webmvc`, build with the repo `mvnw -f <probe pom>`; real-jar smoke via a bash script piping JSON-RPC lines into `java -jar` (hold stdin open with `(cat in; sleep 4) |`, EOF too early drops replies).
- Stdio test without touching surefire stdin: swap `System.in/out` with a pipe and capture stream BEFORE starting the context (provider captures them at bean creation).
- Probe trap: looping `ctx.getType()` over all bean definitions cost ~19 s and looked like a slow MCP shutdown; time inside the test before blaming the library.
