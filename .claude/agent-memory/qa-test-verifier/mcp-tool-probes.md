---
name: mcp-tool-probes
description: Probing Spring AI @McpTool servers (unit plus integration class per run, required-parameter and output-cap traps) and in-process STDIO tests in a shared Surefire fork (order runs, logging-leak and stdout-noise probes)
metadata:
  type: feedback
---

Rule: run every MCP tool mutant against BOTH the unit class and the SDK-client integration class in one Maven call
(`-Dtest=<Unit>,<Integration>`, about 4 s with `clean`), and attribute kills per class from the Surefire summary.

**Why:** some card criteria live only at one level (get_title has no unit test; annotations, yml and schema only
exist over the wire). A single-class run reports false survivors or misses that the integration test does not catch
a trim removal.
**How to apply:**
- Annotation and yml mutants need the live context: removing `protocol: STREAMABLE` gives 404 on `/mcp` (SSE transport), removing `annotations = @McpAnnotations(...)` falls back to the defaults (readOnly false, destructive true, idempotent false, openWorld true; confirm with `javap -v` of `McpTool$McpAnnotations`).
- Spring AI validates `@McpToolParam` requiredness at runtime (`ToolInputValidator ... required property 'x' not found`, tool result isError). `required = false` on a required param is therefore a testable mutant: probe it, and a test needs `tool.inputSchema()` (a `Map<String,Object>`, read `getOrDefault("required", ...)`) or a call without the argument.
- Output caps ("text under N characters", trim loop): compute the fixture's prefix lengths first (a small `python3 -I` replay of the JSON). Uniform items step by a constant, so `>=` vs `>` and `N-1` survive unless a fixture lands exactly on N and N-1. Prove the gap with a temporary witness test that pads the last item with the same mapper as oracle until the text is exactly N (expect one item fewer) and N-1 (expect all).
- A thrown exception from an `@McpTool` method surfaces as an isError result whose text repeats the message twice on two lines; an exact single-line text assertion kills "throw instead of error result".
- A test that calls a tool without a required argument makes the server log one `ToolInputValidator` WARN per referee. That line is on purpose, not a defect. Classify it and record its exact count in the warning inventory, the same way as the scanner's BeanPostProcessorChecker lines.
- Rework rounds: load the r1 generator's PROBES list by path (`importlib.util.spec_from_file_location`) into an r2 generator that reads fresh `r2` `.orig` copies. That way the mutants are byte-identical by construction. Re-run the full list, not just the targeted survivors: new tests shift line numbers and add kills, and the whole list runs in about 6 s per probe.

Rule: an in-process test that swaps `System.out` and re-initialises Boot logging can leak into later test classes
while every test stays green. So probe its harness lines in the class order where a class FOLLOWS it
(`-Dsurefire.runOrder=reversealphabetical` when it sorts last), and read the console `Started .* in` count as the
kill signal, not the test result.

**Why:** with `System.setOut(original)` or the system-property restore removed, the module stays 177/0/0/0, but the
console Started count drops to 0. Leaked `CONSOLE_LOG_*` properties or a dead capture stream silence every later
context. In alphabetical order the class ran last and the same mutants showed nothing.
**How to apply:**
- Put three real-code runs at the top of the probe manifest: `-Dtest=<Class>` alone, alphabetical and reverse. Have the runner print the `Running` class order, the console Started count, the count of the known stderr status line and the Started count in the test's target log file per run. A changed status-line count (4 to 8) also reveals a leak.
- Logging-prep probes: remove the pre-start `LoggingSystem.cleanUp()` (must fail in the order where cached contexts come FIRST: log lines reach the swapped stdout), remove the property restore and remove the re-initialisation. A re-init can be an equivalent mutant, because Boot cleans up logging on context close. Report it as informational.
- Unfiltered-stdout assertion: insert test-side `System.out.println` noise after `context.close()` and before stdout is restored. Use four variants: text, an empty line, a JSON-RPC line without an id, and a duplicate id. Each must fail at its own assertion (parse, parse, `containsExactly` ids, `containsExactly` ids). Make `awaitLines` a no-op to show that the `hasSize >= 2` guard is the line that fails on zero lines.
- A deadline shortened to 1 ms survives when the wait loop checks the count before the deadline. Prove the deadline with a transport-off probe instead: testcase `time=` of about 5.1 s in the Surefire XML and the fail message carrying the captured text.
- Profile settings with a shared effect (an empty console pattern and a console threshold of OFF each silence the console on their own) can only be killed together. Probe each alone as informational and pin the verbatim bytes by inspection plus the hash baseline.
- Which stream a framework status line went to: an awk over the class's Surefire XML that tracks the `<system-out>` and `<system-err>` sections gives the proof without the implementer's source extracts.
Related: [[env-switch-mutant-runner]], [[spring-runtime-probes]], [[shell-traps]]
