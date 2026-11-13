---
name: spring-boot-env-logging-traps
description: Boot 4.1.2 traps for no-context yml/EPP tests and for a second in-process app (SpringApplicationBuilder) in the shared Surefire fork - logback init once per JVM, system properties, main class
metadata:
  type: reference
---
## yml and EnvironmentPostProcessor tests
- No-context tests: `new StandardEnvironment()`, remove `SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME` and
  `SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME`, pass variables only via a `MapPropertySource`, else an exported variable turns tests
  red (QA proves with a keyed referee run).
- `new YamlPropertySourceLoader().load(name, new ClassPathResource(name))` gives `EnumerablePropertySource`s: "no key X" walks
  `getPropertyNames()` with a positive control. `Binder.get(env).bind(name, Type.class)` resolves `${VAR:default}`;
  `environment.getProperty` suffices for raw strings (`${VAR:}` gives `""`).
- EPPs load from `org.springframework.boot.EnvironmentPostProcessor` and the deprecated `...boot.env.` key, but a class
  implementing the new interface registered under the old key fails startup.
- `addFirst` vs `addLast`: assert the last property source holds the value. Boot adds `applicationInfo` AFTER every EPP: match
  config files by `startsWith("Config resource")`.
- `@TestPropertySource` on the shared IT base is visible to EPPs: an EPP adding a source only when a property is unset
  (AssistantModeConfig) then skips. Check such tests before pinning.

## Second Boot app in the shared fork
Sources: `spring-boot-4.1.2-sources.jar`, `org/springframework/boot/logging/`.
- `LogbackLoggingSystem.initialize`/`beforeInitialize` return early when the LoggerContext carries the "initialized" marker:
  an in-process app started after a cached context keeps the old console config. Fix: `LoggingSystem.get(cl).cleanUp()` first.
- `LoggingSystemProperties` writes `CONSOLE_LOG_*` into System properties only when absent; `LogFile` writes `LOG_FILE`/`LOG_PATH`
  unconditionally; a console-OFF profile that runs first silences later contexts. Fix: copy `System.getProperties()` before the
  start, restore after.
- After close, logback stays configured for that profile: re-initialize with a `StandardEnvironment` plus `application.yml` via
  `YamlPropertySourceLoader`, then `cleanUp`, `beforeInitialize`, `initialize(new LoggingInitializationContext(env), null, null)`.
- Prove both class orders (`-Dsurefire.runOrder=alphabetical` and `reversealphabetical`) and check JUnit method order.
- `logging.threshold.console: OFF` does NOT silence logback's `|-ERROR in PatternLayout("")` status line.
- `SpringApplicationBuilder(App.class)` takes the main class from the stack (`ForkedBooter`): add `.main(App.class)` for
  `Started App`. `.properties(...)` loses to profile YAML: pass overrides as `.run("--logging.file.name=x")`.
