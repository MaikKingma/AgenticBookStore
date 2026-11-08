---
name: boot-logging-shared-fork
description: Traps when a test starts a second Spring Boot app in process (SpringApplicationBuilder) in the shared Surefire fork - logback init once per JVM, only-if-absent logging system properties, empty console pattern error, main class in the Started line
metadata:
  type: reference
---

Boot 4.1.2, logback. Verified on the sources jar (`spring-boot-4.1.2-sources.jar`, `org/springframework/boot/logging/`).

- `LogbackLoggingSystem.initialize` and `beforeInitialize` return early when the LoggerContext carries the
  "initialized" marker. So an in-process app started after a cached test context keeps the old console config.
  Fix: call `LoggingSystem.get(cl).cleanUp()` before the start (this removes the marker).
- `LoggingSystemProperties` writes `CONSOLE_LOG_THRESHOLD`, `CONSOLE_LOG_PATTERN` and the rest into System properties
  only when they are absent, and `LogFile` writes `LOG_FILE` and `LOG_PATH` unconditionally. Logback falls back to
  those values for later contexts whose environment leaves them null. So a profile with the console OFF that runs
  first silences every later context's console, and the G1 Started-line count drops. Fix: copy
  `System.getProperties()` before the start and restore it exactly afterwards.
- After the close, logback is still configured for that profile. Re-initialize it for the tests that follow: a
  `StandardEnvironment` plus `application.yml` via `YamlPropertySourceLoader`, then `cleanUp`, `beforeInitialize`
  and `initialize(new LoggingInitializationContext(env), null, null)`.
- Prove both class orders: `-Dsurefire.runOrder=alphabetical` and `reversealphabetical`. Also check JUnit's method
  order. A method that never reads stdout and runs first hides the trap, because closing its context clears the marker.
- `logging.threshold.console: OFF` does NOT silence `|-ERROR in PatternLayout("") - Empty or null pattern.`. Boot
  always builds the console encoder, and `SystemStatusListener` prints the status to `System.err`, which is the
  referee console when the app runs in process.
- `SpringApplicationBuilder(App.class)` takes the main class from the stack (the Surefire `ForkedBooter`), so the log
  says `Started ForkedBooter`. Add `.main(App.class)` to get `Started App`.
- Default properties (`.properties(...)`) lose to profile YAML. To override a profile value in a test, pass it as a
  command-line argument (`.run("--logging.file.name=target/x.log")`).

In-process STDIO recipe and transport traps: res-2's artifact for spec 05 (`research-mcp-stdio.md`).
