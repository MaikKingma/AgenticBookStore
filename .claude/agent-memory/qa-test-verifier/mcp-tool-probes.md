---
name: mcp-tool-probes
description: Probing Spring AI @McpTool servers in the distributor - one run per probe over unit plus integration class, required-parameter and output-cap boundary traps
metadata:
  type: feedback
---

Rule: run every MCP tool mutant against BOTH the unit class and the SDK-client integration class in one Maven call
(`-Dtest=<Unit>,<Integration>`, about 4 s with `clean`), and attribute kills per class from the Surefire summary.

**Why:** some card criteria live only at one level (get_title has no unit test; annotations, yml and schema only
exist over the wire). A single-class run reports false survivors or misses that the integration test does not catch
a trim removal.
**How to apply:**
- Annotation and yml mutants need the live context: removing `protocol: STREAMABLE` gives 404 on `/mcp` (SSE
  transport), removing `annotations = @McpAnnotations(...)` falls back to the defaults (readOnly false, destructive
  true, idempotent false, openWorld true; confirm with `javap -v` of `McpTool$McpAnnotations`).
- Spring AI validates `@McpToolParam` requiredness at runtime (`ToolInputValidator ... required property 'x' not
  found`, tool result isError). `required = false` on a required param is therefore a testable mutant: probe it, and
  a test needs `tool.inputSchema()` (a `Map<String,Object>`, read `getOrDefault("required", ...)`) or a call
  without the argument.
- Output caps ("text under N characters", trim loop): compute the fixture's prefix lengths first (a small
  `python3 -I` replay of the JSON). Uniform items step by a constant, so `>=` vs `>` and `N-1` survive unless a
  fixture lands exactly on N and N-1. Prove the gap with a temporary witness test that pads the last item with the
  same mapper as oracle until the text is exactly N (expect one item fewer) and N-1 (expect all).
- A thrown exception from an `@McpTool` method surfaces as an isError result whose text repeats the message twice
  on two lines; an exact single-line text assertion kills "throw instead of error result".
- A test that calls a tool without a required argument makes the server log one `ToolInputValidator` WARN per
  referee. That line is on purpose, not a defect. Classify it and record its exact count in the warning inventory,
  the same way as the scanner's BeanPostProcessorChecker lines.
- Rework rounds: load the r1 generator's PROBES list by path (`importlib.util.spec_from_file_location`) into an
  r2 generator that reads fresh `r2` `.orig` copies. That way the mutants are byte-identical by construction.
  Re-run the full list, not just the targeted survivors: new tests shift line numbers and add kills, and the whole
  list runs in about 6 s per probe.
- The runner also greps C0-5 f/g/h with each mutant installed and logs the counts, so every survivor carries its
  FF3 gate classification without extra runs.
Related: [[env-switch-mutant-runner]], [[spring-runtime-probes]], [[shell-traps]]
