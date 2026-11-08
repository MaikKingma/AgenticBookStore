# Spec 01 Foundation: Shaping Notes

Restates `docs/spec-pack/specs/01-foundation.md`, which is the source of truth for scope, requirements, scenarios, tests, tasks and definition of done. Nothing here adds, drops or changes a requirement, scenario, path, name or version of the pack spec.

## Scope

- A Maven multi-module repository with wrapper, Java 25, Spring Boot 4.1.2, Spring AI BOM 2.0.2 managed in the root (modules `bookstore` and `distributor`; `frontend` arrives with spec 12).
- Two runnable Spring Boot applications: `bookstore` (port 8080) and `distributor` (port 8090), both binding to `127.0.0.1`.
- The eight layer packages of the bookstore, each with a one-line `package-info.java` Javadoc.
- `ArchitectureTest` that encodes every rule from the architecture standard, plus `ArchitectureSelfTest` proving the rules bite on fixtures in `nl.openvalue.archfixtures`.
- PostgreSQL only: `compose.yaml` with Spring Boot Docker Compose support for local runs, Testcontainers PostgreSQL with `@ServiceConnection` for every test that needs a database, Flyway (`V0__baseline.sql`), `ddl-auto=validate`, Actuator health. No H2, no embedded database anywhere.
- GitHub Actions CI on Linux (`verify`, full tests with Docker) and Windows (`windows-build`, compile-only with `-DskipTests`).

Out of scope: any domain model or endpoint (specs 02 to 08); agent configuration of any kind in the participant start state.

## Decisions

- Task grouping: the pack's eight numbered tasks map to six implementation tasks (T1 skeleton, T2 CI, T3 bookstore runtime and test setup, T6 distributor configuration and tests, T4 packages and ArchitectureTest, T5 fixtures and ArchitectureSelfTest). Pack tasks 7 (push, confirm CI green) and 8 (measure the verify time) are orchestrator work. Execution order: T1, T2, T3, T6, T4, T5, one task at a time.
- CI lands as T2 so that every later task PR into the spec branch gets checks. The workflow triggers on push to every branch and on every pull request, without branch filters.
- Health `db` component: the scenario "Compose-managed database" requires `GET /actuator/health` to report `db` UP. Spring Boot 4.1.2 only shows components with `management.endpoint.health.show-components=always` (verified in research), so the bookstore `application.yml` carries that property in addition to the properties the pack spec lists. The scenario text is unchanged.
- Rule 3 of the architecture standard (no outer adapter may be accessed by any other layer) is encoded as six named rules, one per outer adapter, so that the self-test proves each one on its own fixture. Rules 1 to 3 use `layeredArchitecture()` with `consideringAllDependencies()`, because `ignoreDependency` (rule 9, the `process` to `..domain..*Event` exception) exists only there, and because the root package may hold only `BookstoreApplication` and spec-mandated configuration that touches no layer. Rule 4 is an allowlist (`java..` and the domain package). Every rule has `allowEmptyShould(true)`.
- Rule 7 fixture (`org.springframework.ai..` only in `assistant`): a stub type in package `org.springframework.ai.archfixtures` under bookstore test sources, because Spring AI is not on the spec 01 classpath. Test-only, no dependency change.
- One Testcontainers PostgreSQL per JVM: `PostgresTestConfig` returns a static singleton container from its `@ServiceConnection` bean. `IntegrationTestBase` carries `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@AutoConfigureRestTestClient`, `@Import(PostgresTestConfig.class)` and the `DatabaseCleaner`; `BookstoreApplicationTests` and the bookstore health test extend it, so the suite starts one container and one context.
- Health tests use `RestTestClient` (Boot 4), not `TestRestTemplate`.
- Root POM coordinates `nl.openvalue:agenticbookstore-parent:0.0.1-SNAPSHOT` (no spec names them).
- The missing-migration scenario is verified by the T3 QA verifier as an uncommitted probe in the working tree, restored afterwards, never committed.
- Traps carried into every task card: the token `h2` is banned in the definition-of-done grep scope even in comments; code, comments, POMs, YAML and CI never reference `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`, because spec 09 strips them from `main`; Initializr writes tabs, which T1 converts to spaces per `.editorconfig`.
- Versions are exactly the pins of `product/tech-stack.md` (Java 25, Spring Boot 4.1.2, Spring AI BOM 2.0.2, ArchUnit 1.5.1 `archunit-junit6`, Testcontainers 2.0.5 as managed by Boot, `postgres:18.6`). Both modules are generated with start.spring.io through its API with curl; starter POMs are never hand-written.

## Context

- **Visuals:** None.
- **References:** No existing code (the repository holds only docs and agent configuration before this spec). Reference material is the research under `.claude/comms/mailboxes/res-1..res-4/outbox/` and the lead strategy under `.claude/comms/mailboxes/lead-1/outbox/strategy-01-foundation.md` (gitignored session state; summarised in `references.md`).
- **Product alignment:** mission (a brownfield codebase with a test-enforced architecture, `./mvnw verify` green and under 60 seconds), roadmap (spec 01 is the referee every workshop level relies on), tech stack (pins and Initializr generation).

## Standards Applied

- `architecture` (pointer to `docs/spec-pack/product/architecture-standard.md`): sections 1 to 3 define the packages, dependency rules and naming rules `ArchitectureTest` encodes; section 5 defines the testing standard (Testcontainers PostgreSQL, no in-memory database, ArchUnit `@ArchTest`, `*IntegrationTest` naming).
