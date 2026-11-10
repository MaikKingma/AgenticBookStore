---
name: spring-environment-tests
description: Testing yml settings and EnvironmentPostProcessors without a context, so results never depend on the shell; Boot 4.1.2 EPP key behaviour
metadata:
  type: reference
---

- No-context yml or EPP tests: `new StandardEnvironment()`, then remove `SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME` and
  `SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME`, and pass variables only through a `MapPropertySource`. Otherwise an exported
  variable (a workshop key in the facilitator's shell) turns the tests red. QA proves it with a keyed referee run.
- Load yml with `new YamlPropertySourceLoader().load(name, new ClassPathResource(name))`; the sources are
  `EnumerablePropertySource`, so "no key named X" checks walk `getPropertyNames()`. Add a positive control (a known
  key is among the names) so the check cannot pass on an unread file.
- `Binder.get(environment).bind(name, Type.class)` resolves `${VAR:default}` placeholders and binds records by
  constructor; `environment.getProperty` is enough for raw strings (a `${VAR:}` gives `""`, not null).
- Boot 4.1.2 loads EPPs from `org.springframework.boot.EnvironmentPostProcessor` and still from the deprecated
  `org.springframework.boot.env.EnvironmentPostProcessor` key, but a class implementing the new interface registered
  under the old key is not assignable to it: startup fails (source: `SpringFactoriesEnvironmentPostProcessorsFactory`).
- `addFirst` vs `addLast` in an EPP test: assert the last property source holds the added value, and the source-name list is
  unchanged where nothing may be added.
- Full-context EPP order check: Boot adds an `applicationInfo` source AFTER every EPP, so "after every source whose
  name contains application" can never hold. Match the config files by `startsWith("Config resource")` instead
  (names look like `Config resource 'class path resource [application.yml]' via location ...`).

- Pinning a property in `@TestPropertySource` on the shared IT base is visible to EPPs (inlined test properties are in the
  environment before they run): an EPP that adds a source only when a property is unset (AssistantModeConfig) then skips,
  so a full-context test that expects that source goes red. Check such tests before pinning (T3 of fix-08).

Related: [[maven-build-traps]].
