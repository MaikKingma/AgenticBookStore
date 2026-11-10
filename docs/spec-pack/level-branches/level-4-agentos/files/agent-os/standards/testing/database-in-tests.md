# Database in Tests

PostgreSQL through Testcontainers, never an embedded database.

- No H2, no in-memory database anywhere. Every test that needs a database runs on PostgreSQL in Docker.
- Docker must be running. A missing Docker daemon is an environment error, not a reason to skip a test.
- One shared container: `PostgresTestConfig` (`@ServiceConnection`, image `postgres:18.6`).
- Data adapter tests: `@DataJpaTest` with `@Import(PostgresTestConfig.class)` and `@AutoConfigureTestDatabase(replace = NONE)`.
- Full-application tests extend `IntegrationTestBase`: random port, PostgreSQL, no transaction rollback.
- Never use `@DirtiesContext`: closing one context stops the shared container under every other cached context.
- The test profile (`application-test.yml`) loads the schema migrations only, so tests start with an empty catalogue and never see the demo data.
- Never add an `application.yml` to the test resources: it would shadow the main one.
