---
name: spring-environment-tests
description: Testing yml settings and EnvironmentPostProcessors without a context, so results never depend on the shell; Boot 4.1.2 EPP key behaviour
metadata:
  type: reference
---

- No-context yml or EPP tests: `new StandardEnvironment()`, then remove `SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME` and
  `SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME`, and pass variables only through a `MapPropertySource`; otherwise an exported
  variable (a workshop key in the facilitator's shell) turns the tests red. QA proves it with a keyed referee run.
- Load yml with `new YamlPropertySourceLoader().load(name, new ClassPathResource(name))`; the sources are
  `EnumerablePropertySource`, so "no key named X" checks walk `getPropertyNames()`. Add a positive control (a known key is among
  the names) so the check cannot pass on an unread file. `Binder.get(environment).bind(name, Type.class)` resolves
  `${VAR:default}` and binds records by constructor; `environment.getProperty` suffices for raw strings (`${VAR:}` gives `""`).
- Boot 4.1.2 loads EPPs from `org.springframework.boot.EnvironmentPostProcessor` and still from the deprecated
  `org.springframework.boot.env.EnvironmentPostProcessor` key, but a class implementing the new interface registered under the
  old key is not assignable to it: startup fails (`SpringFactoriesEnvironmentPostProcessorsFactory`).
- `addFirst` vs `addLast` in an EPP test: assert the last property source holds the added value, and the source-name list is
  unchanged where nothing may be added. Full-context order check: Boot adds an `applicationInfo` source AFTER every EPP, so
  match the config files by `startsWith("Config resource")` instead of names containing application.
- Pinning a property in `@TestPropertySource` on the shared IT base is visible to EPPs: an EPP that adds a source only when a
  property is unset (AssistantModeConfig) then skips, so a full-context test expecting that source goes red. Check such tests
  before pinning.

Related: [[maven-build-traps]].
