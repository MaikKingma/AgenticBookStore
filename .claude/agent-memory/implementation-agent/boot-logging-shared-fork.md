---
name: boot-logging-shared-fork
description: Traps when a test starts a second Spring Boot app in process (SpringApplicationBuilder) in the shared Surefire fork - logback init once per JVM, only-if-absent logging system properties, empty console pattern error, main class in the Started line
metadata:
  type: reference
---

Boot 4.1.2, logback (read in `spring-boot-4.1.2-sources.jar`, `org/springframework/boot/logging/`).

- `LogbackLoggingSystem.initialize`/`beforeInitialize` return early when the LoggerContext carries the "initialized" marker, so an
  in-process app started after a cached test context keeps the old console config. Fix: `LoggingSystem.get(cl).cleanUp()` before
  the start (removes the marker).
- `LoggingSystemProperties` writes `CONSOLE_LOG_THRESHOLD`, `CONSOLE_LOG_PATTERN` etc. into System properties only when absent,
  and `LogFile` writes `LOG_FILE`/`LOG_PATH` unconditionally; logback falls back to them for later contexts. So a profile with the
  console OFF that runs first silences every later context's console. Fix: copy `System.getProperties()` before the start and
  restore it exactly afterwards.
- After the close, logback is still configured for that profile. Re-initialize it for later tests: a `StandardEnvironment` plus
  `application.yml` via `YamlPropertySourceLoader`, then `cleanUp`, `beforeInitialize` and
  `initialize(new LoggingInitializationContext(env), null, null)`.
- Prove both class orders (`-Dsurefire.runOrder=alphabetical` and `reversealphabetical`) and check JUnit's method order: a method
  that never reads stdout and runs first hides the trap, because closing its context clears the marker.
- `logging.threshold.console: OFF` does NOT silence `|-ERROR in PatternLayout("") - Empty or null pattern.`: Boot always builds
  the console encoder and `SystemStatusListener` prints to `System.err`, the referee console when the app runs in process.
- `SpringApplicationBuilder(App.class)` takes the main class from the stack (Surefire `ForkedBooter`): add `.main(App.class)` to
  get `Started App`. Default properties (`.properties(...)`) lose to profile YAML: pass overrides as command-line arguments
  (`.run("--logging.file.name=target/x.log")`).
