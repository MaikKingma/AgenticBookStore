---
name: test-context-cost-model
description: How the bookstore test suite stays inside the 60 s verify budget (one container, one Spring context); use when a spec adds integration, slice or context tests
metadata:
  type: project
---

Rule: one PostgreSQL container per test JVM and one Spring context for all bookstore full-context tests.
**Why:** the 60 s `./mvnw clean verify` budget spans all twelve specs; distinct context keys (MOCK vs RANDOM_PORT vs
`@DataJpaTest`, different `@Import`, `@MockitoBean` sets, properties) each start a context, and a plain
`@Bean new PostgreSQLContainer(...)` starts one container per context (measured in spec 01 research).

**How to apply:**
- `PostgresTestConfig` returns a `private static final` container from its `@Bean @ServiceConnection` method.
- `IntegrationTestBase` (RANDOM_PORT, `@AutoConfigureRestTestClient`, `@Import(PostgresTestConfig.class)`) is the one
  canonical context; context-load and health tests extend it too. Spec 07 adds a `@MockitoBean` there, which keeps
  sharing because it is on the base, never per subclass.
- Test-only beans for full-context tests (event recorders, spies) go on `IntegrationTestBase`: a plain class added
  to its `@Import` (never a stereotype: a test-sources `@Component` is component-scanned into every full context and
  the `@Import` is ignored), `@MockitoSpyBean` or `@MockitoBean` fields on the base, state reset in the base
  `@BeforeEach`. Every subclass then shares one key.
- A port `@MockitoBean` on the base replaces the real adapter bean in every full-context test, so none of them proves
  the adapter still wires (starter present, properties registered). Pair it with an `ApplicationContextRunner` test
  (auto-config plus the adapter as user configuration): sub-second, no new cached context (spec 06 strategy).
- A default answer for a base `@MockitoBean` goes in the base `@BeforeEach` (mocks reset after every test);
  relative dates come from the context `Clock`, captured once per test in a field so stub and assertion agree.
  Subclasses override with `doThrow/doAnswer(...).when(mock)`.
- Never `@DirtiesContext`: closing a context stops the shared container under the other cached contexts.
- Gate in task cards: the bookstore Surefire output shows one "Creating container for image" line and one Spring
  "Started ... in" line per distinct context; review any new context deliberately.
- Deliberate extra contexts per slice: one `@DataJpaTest` (shares the static container) and one `@WebMvcTest` per
  controller (no container). Name the expected "Started" count in the gate. Lever if the budget gets tight: web slice
  tests listing the same controllers and the same `@MockitoBean` set share one context.
- Shared-database residue: `IntegrationTestBase` cleans before each test, never after, so committed rows can be there
  when a JPA slice runs. Slice tests that need exact counts or fixed unique keys run `DatabaseCleaner` in
  `@BeforeEach`; inside the slice's transaction the truncate is rolled back with the test.
- Stateful singletons (rate limiter, chat memory, scripted doubles) leak across test methods and classes of one
  cached context. Isolate by test-controlled inputs: a mutable clock bean advanced in `@BeforeEach`, fresh ids per
  test, a double's `reset()`; raise a limit in a context where it is not under test. Never a production reset method.
- External API keys: the test profile sets the key property explicitly empty, so `verify` stays offline even when a
  developer exports the real key (spec 08 strategy, D1). A forked context for an external double is named in the base
  Javadoc as the one exception.
- Over budget is an open question for the orchestrator, never a reason to trim tests (standard section 5).

Related: [[archunit-referee-design]], [[strategy-constraints]].
