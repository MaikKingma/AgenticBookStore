---
name: qa-warnings-and-logging
description: Warning inventory and log-evidence traps in directives: warnings vs task sequencing, per-context counts, once-per-JVM notices, base-referee multiset diff, logback initialised once per JVM, run-order dependence, STDIO empty-pattern line, README command claims
applies-to:
  - bookstore/src/test/**
  - bookstore/src/main/resources/logback*
  - "README*.md"
metadata:
  type: feedback
---

Siblings: [[qa-directive-traps]] (grep syntax), [[qa-probe-design-traps]].

1. **Framework warnings vs task sequencing.** "Green with a framework warning = FAIL" collides with skeleton-first specs: the first task's referee prints warnings later tasks fix. At strategy time give every expected warning an owning criterion, else verifiers wave them through as "environmental". Known on this stack: Mockito inline-mock self-attach on JDK 25 (fixed by a Surefire `-javaagent` argLine), JPA open-in-view, Flyway "No migrations found", Maven `prefixes.txt` 401 warnings (from the pinned flyway-parent POM). Put a warning-inventory item in C0: every warning quoted and classified as fixable here (FAIL), owned by a named criterion, or unowned (flag it).
   - A negative-path test makes the server log a WARN (Hibernate `HHH000247 ... SQLState: 23505`, Spring AI `ToolInputValidator ... required property 'query' not found`): pre-classify as accepted only at an exact count with its key, so a second, real one cannot hide behind it.
   - Warnings emitted once per context (Spring AI's three BeanPostProcessorChecker lines per MCP-scanning context) scale with context count: write the expected count as a multiple of a per-context marker (`Registered tools: 4`), never "unchanged afterwards".
   - Inventory grep `warn|deprecat|self-attach` (Mockito's line has no "warn"). In a final brief never write an allow-list from one test class's log (a full referee prints ~130 hits): the bar is a base referee in a throwaway worktree plus a normalised multiset diff (timestamps, PIDs, threads, `.m2` path): 0 added at the tip.

2. **Boot initialises logback once per JVM** (`LogbackLoggingSystem.initialize` returns early, Boot 4.1.2). An in-process `SpringApplicationBuilder` test (STDIO profile) in the shared Surefire fork keeps the console appender of an earlier cached context, and logback writes through the swapped `System.out`: the outcome depends on class order. Demand green runs with `-Dsurefire.runOrder=alphabetical` and `reversealphabetical` plus the class alone, and a non-empty precondition on any "every captured line" assertion. Surefire XML `<system-out>` attributes Started lines but never recovers silenced ones.
   - Surefire sorts by fully qualified path, so a top-level `nl.x.HealthEndpointTest` (upper-case `H`) sorts BEFORE lower-case subpackages and runs LAST under `reversealphabetical`. Dry-run the order from a previous log's `Running` lines; to make one class start a shared context write `-Dtest='A,B' -Dsurefire.runOrder=alphabetical` with A first by FQN.
   - The Spring AI STDIO recipe's empty `logging.pattern.console` makes logback print `|-ERROR in ch.qos.logback.classic.PatternLayout("") - Empty or null pattern.` on stderr at every logging init (`logging.threshold.console: OFF` filters events only). Pre-classify it (exact text, count = STDIO starts proven by the Started lines, stderr only) or have the spec drop the empty pattern; the exact count also guards the test's logging preparation (a leaked property doubles it, a skipped `cleanUp()` zeroes it).

3. **README criterion "every command was run".** Exclude commands writing the user's tool configuration (`claude mcp add`): check them against `--help` plus the live-proven URL and arguments; prove the client connection with a headless `claude -p --mcp-config <tmp> --strict-mcp-config --output-format stream-json` run (init event: server connected, tool list). Open SSE streams hold Boot's graceful shutdown for 30 s: stop scripts wait for the PID, not the port. README statements about the data (real vs invented, counts) get a census script over the seed file.
