# Spec 01: Foundation (multi-module build, architecture referee, CI)

> Repository: `MaikKingma/AgenticBookStore` · Modules: root, `bookstore`, `distributor` (the `frontend` module is added by spec 12) · Depends on: nothing
> Read first: `product/architecture-standard.md`, `product/tech-stack.md`

## Why
Every later spec and every workshop lab relies on two things: a build that is fast and identical on every laptop,
and an architecture test that fails loudly when an agent puts code in the wrong layer. This spec creates both
before any business code exists.

## What changes
- A Maven multi-module repository with wrapper, Java 25, Spring Boot 4.1.2, Spring AI BOM 2.0.2 managed in the root.
- Two runnable Spring Boot applications: `bookstore` (port 8080) and `distributor` (port 8090), both binding to `127.0.0.1`.
- The layer packages of the bookstore, each with a one-line `package-info.java` Javadoc.
- `ArchitectureTest` that encodes every rule from the architecture standard, plus a self-test proving the rules bite.
- PostgreSQL only: `compose.yaml` with Spring Boot Docker Compose support for local runs, Testcontainers PostgreSQL
  with `@ServiceConnection` for every test that needs a database, Flyway, `ddl-auto=validate`, Actuator health.
  No H2, no embedded database anywhere.
- GitHub Actions CI on Linux and Windows.

## Out of scope
- Any domain model or endpoint (specs 02 to 08).
- Agent configuration of any kind (`CLAUDE.md`, `.claude/`, `AGENTS.md`). The participant start state must not have it.

## Design notes
```
AgenticBookStore/
├── pom.xml                      # parent: spring-boot-starter-parent 4.1.2, packaging pom, modules (spec 12 adds frontend)
├── mvnw, mvnw.cmd, .mvn/wrapper/
├── .gitattributes               # *.sh, mvnw, .githooks/*: eol=lf ; *.cmd, *.ps1: eol=crlf
├── .gitignore                   # target/, notes/, .idea/, *.iml, .env*, CLAUDE.local.md, node_modules/, dist/, frontend/node/ (never a bare `data/`: it would match the data layer package)
├── .editorconfig
├── .github/workflows/ci.yml
├── bookstore/
│   ├── pom.xml
│   ├── compose.yaml             # PostgreSQL for local runs (Spring Boot Docker Compose support)
│   └── src/main/java/nl/openvalue/bookstore/
│       ├── BookstoreApplication.java
│       ├── domain/ domaininteraction/ command/ query/ process/ assistant/ data/ acl/   (package-info.java each)
│   └── src/main/resources/application.yml, db/migration/ (V0__baseline.sql: empty comment-only file)
│   └── src/test/java/nl/openvalue/bookstore/ArchitectureTest.java, PostgresTestConfig.java
│   └── src/test/java/nl/openvalue/archfixtures/...   (deliberately violating classes, test-only)
└── distributor/
    ├── pom.xml
    └── src/main/java/nl/openvalue/distributor/DistributorApplication.java
```
- Generate each module from start.spring.io (see tech stack) and then move common configuration to the root POM.
- Root POM properties: `java.version=25`, `archunit.version=1.5.1`, `spring-ai.version=2.0.2`;
  `dependencyManagement` imports `spring-ai-bom`.
- `bookstore/compose.yaml`: one service `postgres` from the image pinned in `product/tech-stack.md`, database, user
  and password `bookstore`, port 5432 published on 127.0.0.1 only, no volume (every `docker compose down` gives a
  clean, freshly migrated and seeded database, so no participant gets stuck on stale local state).
- `bookstore/application.yml`: no datasource URL. Spring Boot Docker Compose support (`spring-boot-docker-compose`)
  starts the compose file on `spring-boot:run` and derives the datasource from the running container
  (`spring.docker.compose.lifecycle-management=start-only`, so the database survives application restarts;
  `spring.docker.compose.file=./compose.yaml` relative to the module, *verify at build time* that `spring-boot:run`
  from the repo root with `-pl bookstore` resolves it). Further: `spring.jpa.hibernate.ddl-auto=validate`,
  `spring.jpa.open-in-view=false`, Flyway on, `server.address=127.0.0.1`, `server.port=8080`,
  `management.endpoints.web.exposure.include=health,info`, `management.endpoint.health.show-components=always`
  (so the health response lists `db`, as the Compose scenario below expects; details stay hidden). Running the jar
  without Docker Compose support
  (for example `java -jar`) uses `SPRING_DATASOURCE_URL`, documented in the README (spec 09).
- Tests use the main `application.yml` (do NOT add `src/test/resources/application.yml`: it would shadow the main
  file and silently drop `ddl-auto=validate`). Test-only overrides go in `src/test/resources/application-test.yml`
  (for example Flyway locations without `db/seed`, see spec 09), activated for every test via Surefire
  `systemPropertyVariables` `spring.profiles.active=test`.
- Test database: `PostgresTestConfig` (`@TestConfiguration(proxyBeanMethods = false)`) declares one
  `@Bean @ServiceConnection PostgreSQLContainer` from the pinned image. Every `@DataJpaTest` (with
  `@AutoConfigureTestDatabase(replace = NONE)`) and every `@SpringBootTest` imports it, so Spring's test context
  cache starts the container once per JVM and reuses it. Docker Compose support is off in tests (Boot's default
  `spring.docker.compose.skip.in-tests=true`; keep it).
- Test database hygiene: all `@SpringBootTest` contexts share that one container, and test-managed
  `@Transactional` rollback is not allowed (AFTER_COMMIT listeners must fire). Provide `IntegrationTestBase` in test
  sources with a `DatabaseCleaner` that truncates every application table (not `flyway_schema_history`) in
  `@BeforeEach`. Every `*IntegrationTest` extends it.
- `package-info.java` Javadoc is one sentence per layer, e.g. "Outbound adapter: persistence. Implements the
  `*DataService` ports from domaininteraction." Keep it this short: the workshop needs the agent to *miss* context.

## Requirements

### Requirement: Reproducible build
The repository SHALL build and test with `./mvnw verify` (or `mvnw.cmd verify` on Windows) using only Java 25, a running Docker daemon and network access to Maven Central, Docker Hub and, from spec 12 on, the Node.js download site and the npm registry, without a local Maven installation or a local Node.js installation.

#### Scenario: Clean clone builds
- **GIVEN** a fresh clone on a machine with only Temurin 25, git and Docker running
- **WHEN** the developer runs `./mvnw verify`
- **THEN** both modules compile, all tests pass on a Testcontainers PostgreSQL and the build finishes with BUILD SUCCESS

#### Scenario: Docker is not running
- **GIVEN** the Docker daemon is stopped
- **WHEN** the developer runs `./mvnw verify`
- **THEN** the database tests fail with a message that names Docker as the cause (no test is skipped silently)

#### Scenario: Build is fast enough for hooks
- **GIVEN** dependencies are already in the local Maven repository
- **WHEN** `./mvnw -q verify` runs on a 2024 laptop
- **THEN** it completes in under 60 seconds (measure and record the time in the PR description)

### Requirement: Layer dependency rules are enforced
The bookstore module SHALL contain an `ArchitectureTest` (ArchUnit, `@AnalyzeClasses(packages = "nl.openvalue.bookstore", importOptions = DoNotIncludeTests)`) that encodes every dependency rule in section 2 of the architecture standard as a separate, named `@ArchTest` rule.

#### Scenario: Rules pass on a compliant codebase
- **GIVEN** the empty layer packages
- **WHEN** the tests run
- **THEN** every rule passes (rules use `allowEmptyShould(true)` where a layer has no classes yet)

#### Scenario: Rules fail on a violation
- **GIVEN** the test-only package `nl.openvalue.archfixtures` with classes that mirror the bookstore layers and contain one violation per rule (for example a `domain` class importing `jakarta.persistence.Entity`, a `command` class calling a `data` class)
- **WHEN** `ArchitectureSelfTest` evaluates each production rule against the fixture classes with `ClassFileImporter`
- **THEN** each rule reports at least one violation, proving none of the rules is vacuous

### Requirement: Naming rules are enforced
`ArchitectureTest` SHALL enforce the class-name suffixes per package from section 3 of the architecture standard.

#### Scenario: Wrongly named class is rejected
- **GIVEN** a fixture class `BookController` in a `command` fixture package
- **WHEN** the naming rule for `command` is evaluated against the fixtures
- **THEN** the rule reports that classes in `command` must end with `Commands`, `Payload`, `CommandResult` or `ExceptionHandler`

### Requirement: Applications start
Both applications SHALL start with `./mvnw -pl <module> spring-boot:run` and expose `/actuator/health` with status UP, bound to 127.0.0.1.

#### Scenario: Health endpoint
- **WHEN** the bookstore is started and `GET http://localhost:8080/actuator/health` is called
- **THEN** the response is 200 and `$.status` is `UP`

### Requirement: Local runs use PostgreSQL in Docker
The bookstore SHALL start its PostgreSQL through Docker Compose support when run with `spring-boot:run`, and SHALL not fall back to any in-memory database.

#### Scenario: Compose-managed database
- **GIVEN** Docker is running and no PostgreSQL container exists
- **WHEN** `./mvnw -pl bookstore spring-boot:run` starts
- **THEN** a `postgres` container from the pinned image is started, Flyway migrates it, and `GET /actuator/health` reports `db` UP

### Requirement: Schema is migration-managed
The bookstore SHALL run Flyway on startup and Hibernate SHALL only validate the schema.

#### Scenario: Missing migration is detected
- **GIVEN** a JPA entity whose table has no migration (verified in a throwaway branch, not committed)
- **WHEN** the application context starts in a test
- **THEN** startup fails with a schema validation error

### Requirement: Continuous integration
The repository SHALL have a GitHub Actions workflow with two jobs, both with Temurin 25 and Maven dependency caching. The workflow runs on every push to `main`, `factory` and the `sdd/*/spec` integration branches, and on every pull request; a feature branch is covered by its pull request. `verify` on `ubuntu-latest` runs `./mvnw -B verify` (Docker is available there, so the Testcontainers PostgreSQL runs); `windows-build` on `windows-latest` runs `mvnw.cmd -B -DskipTests verify`, which proves the wrapper, the line endings and the full build (including the frontend from spec 12) on Windows. The commands may carry additional flags and environment settings that do not skip or weaken tests (for example `-fae`). Database tests run on Linux only, because GitHub's Windows runners cannot run Linux containers (decision of 2026-11-06, `docs/spec-pack/orchestrator/progress.md`).

#### Scenario: Windows line endings do not break scripts
- **GIVEN** the repository is checked out on Windows
- **WHEN** CI runs `mvnw.cmd -B -DskipTests verify`
- **THEN** the build passes, and `.gitattributes` keeps `*.sh` and `mvnw` with LF endings

#### Scenario: Database tests run on Linux
- **WHEN** the `verify` job runs on `ubuntu-latest`
- **THEN** every test runs (none skipped) against the Testcontainers PostgreSQL and the job is green

## Tests (required)
- `ArchitectureTest` (production rules) and `ArchitectureSelfTest` (rules against fixtures).
- `BookstoreApplicationTests` (context loads against the Testcontainers PostgreSQL, Flyway ran, `V0` recorded in
  `flyway_schema_history`) and `DistributorApplicationTests`: context loads.
- A health endpoint test per module (`@SpringBootTest(webEnvironment = RANDOM_PORT)`).

## Tasks
1. Generate both modules from start.spring.io with curl (dependency ids per `product/tech-stack.md`: the bookstore
   gets `postgresql`, `docker-compose` and `testcontainers`, never `h2`), create root POM, wrapper, `.gitattributes`,
   `.gitignore`, `.editorconfig`.
2. Configure `compose.yaml`, Docker Compose support, Flyway (`V0__baseline.sql`), `ddl-auto=validate`, Actuator,
   `server.address`; `PostgresTestConfig` with the Testcontainers PostgreSQL `@ServiceConnection` bean.
3. Create the eight layer packages with `package-info.java`.
4. Write fixture classes in `nl.openvalue.archfixtures` (one violation per rule, one naming violation per package).
   Rules use root-anchored patterns (`nl.openvalue..domain..`) so they apply to both production and fixture packages,
   and naming rules select top-level classes only (`areTopLevelClasses()`, excluding `package-info`).
5. Write `ArchitectureTest` with one named rule per dependency and naming rule; expose the rules as `public static final`
   fields so `ArchitectureSelfTest` can reuse them.
6. Write `ArchitectureSelfTest`, context and health tests, `IntegrationTestBase` and `DatabaseCleaner`.
7. Add CI workflow (`verify` on Linux with tests, `windows-build` on Windows with `-DskipTests`); push and confirm both jobs are green.
8. Measure `./mvnw -q verify` time.

## Definition of done
- `./mvnw verify` green locally (Docker running) and in the Linux CI job; the Windows build job green.
- `grep -ri h2 bookstore/pom.xml bookstore/src pom.xml` returns nothing.
- The time of `./mvnw clean verify` is recorded in `docs/spec-pack/orchestrator/progress.md`.
- `ArchitectureSelfTest` proves every rule fails on its fixture.
- No `CLAUDE.md`, `.claude/`, `AGENTS.md`, `.mcp.json` on `main` as produced by the publish script of spec 09 (they may exist on `factory`).
