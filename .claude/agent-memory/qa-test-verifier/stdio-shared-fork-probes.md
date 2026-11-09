---
name: stdio-shared-fork-probes
description: Probing an in-process STDIO (swapped System.in/out) Spring Boot test in a shared Surefire fork - order runs, logging-leak probes that stay green, noise probes for an unfiltered stdout assertion
metadata:
  type: feedback
---

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
Related: [[mcp-tool-probes]], [[env-switch-mutant-runner]], [[spring-runtime-probes]]
