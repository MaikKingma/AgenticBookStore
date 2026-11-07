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
- Never `@DirtiesContext`: closing a context stops the shared container under the other cached contexts.
- Gate in task cards: the bookstore Surefire output shows one "Creating container for image" line and one Spring
  "Started ... in" line per distinct context; review any new context deliberately.
- Over budget is an open question for the orchestrator, never a reason to trim tests (standard section 5).

Related: [[archunit-referee-design]], [[strategy-constraints]].
