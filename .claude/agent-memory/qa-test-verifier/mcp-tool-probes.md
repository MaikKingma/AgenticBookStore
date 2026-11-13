---
name: mcp-tool-probes
description: Spring AI @McpTool servers (unit plus SDK IT per run) and in-process STDIO tests in a shared Surefire fork
metadata:
  type: feedback
---
Rule: run every MCP tool mutant against BOTH the unit class and the SDK-client integration class in one Maven call (`-Dtest=<Unit>,<Integration>`, about 4 s with `clean`), and attribute kills per class from the Surefire summary.
**Why:** some criteria live at one level only (a tool with no unit test; annotations, yml and schema exist only over the wire); a single-class run reports false survivors.
**How to apply:**
- Annotation and yml mutants need the live context: removing `protocol: STREAMABLE` gives 404 on `/mcp` (SSE transport); removing `annotations = @McpAnnotations(...)` falls back to defaults (readOnly false, destructive true, idempotent false, openWorld true; confirm with `javap -v` of `McpTool$McpAnnotations`).
- Spring AI validates `@McpToolParam` requiredness at runtime (`ToolInputValidator ... required property 'x' not found`, isError result): `required = false` on a required param is a testable mutant; a test needs `tool.inputSchema()` (read `getOrDefault("required", ...)`) or a call without the argument.
- Output caps ("text under N characters", trim loop): compute the fixture's prefix lengths first (a `python3 -I` replay). Uniform items step by a constant, so `>=` vs `>` and `N-1` survive unless a fixture lands exactly on N and N-1: prove with a temporary witness padding the last item with the same mapper as oracle to exactly N (one item fewer) and N-1 (all).

Rule: an in-process test that swaps `System.out` and re-initialises Boot logging can leak into later classes while every test stays green: probe its harness lines in an order where a class FOLLOWS it (`-Dsurefire.runOrder=reversealphabetical` when it sorts last) and read the console `Started .* in` count as the kill signal.
**Why:** with `System.setOut(original)` or the system-property restore removed, the module stays green but the console Started count drops to 0 (leaked `CONSOLE_LOG_*` properties or a dead capture stream silence every later context); in alphabetical order the class ran last and the same mutants showed nothing.
**How to apply:**
- Three real-code runs at the top of the manifest: `-Dtest=<Class>` alone, alphabetical, reverse; the runner prints the `Running` order, the console Started count, the count of the known stderr status line and the Started count in the test's target log file (a status-line count change, 4 to 8, also reveals a leak).
- Logging-prep probes: remove the pre-start `LoggingSystem.cleanUp()` (must fail in the order where cached contexts come FIRST), the property restore, the re-initialisation (can be equivalent since Boot cleans logging on context close: informational).
- Unfiltered-stdout assertion: insert `System.out.println` noise after `context.close()` and before stdout is restored: text, empty line, JSON-RPC line without an id, duplicate id; each must fail at its own assertion (parse, parse, `containsExactly` ids x2). Make `awaitLines` a no-op to show the `hasSize >= 2` guard is the failing line on zero lines.
Related: [[env-switch-mutant-runner]], [[spring-runtime-probes]], [[shell-traps]]
