# Spec 01: Foundation (multi-module build, architecture referee, CI)

> Repository: `agentic-bookstore` · Modules: root, `bookstore`, `distributor` · Depends on: nothing
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
- H2 + Flyway + `ddl-auto=validate` wiring, Actuator health.
- GitHub Actions CI on Linux and Windows.

## Out of scope
- Any domain model or endpoint (specs 02 to 08).
- Agent configuration of any kind (`CLAUDE.md`, `.claude/`, `AGENTS.md`). The participant start state must not have it.

## Design notes
```
agentic-bookstore/
├── pom.xml                      # parent: spring-boot-starter-parent 4.1.2, packaging pom, modules
├── mvnw, mvnw.cmd, .mvn/wrapper/
├── .gitattributes               # *.sh, mvnw, .githooks/*: eol=lf ; *.cmd, *.ps1: eol=crlf
├── .gitignore                   # target/, data/, notes/, .idea/, *.iml, .env, CLAUDE.local.md
├── .editorconfig
├── .github/workflows/ci.yml
├── bookstore/
│   ├── pom.xml
│   └── src/main/java/nl/openvalue/bookstore/
│       ├── BookstoreApplication.java
│       ├── domain/ domaininteraction/ command/ query/ process/ assistant/ data/ acl/   (package-info.java each)
│   └── src/main/resources/application.yml, db/migration/ (V0__baseline.sql: empty comment-only file)
│   └── src/test/java/nl/openvalue/bookstore/ArchitectureTest.java
│   └── src/test/java/nl/openvalue/archfixtures/...   (deliberately violating classes, test-only)
└── distributor/
    ├── pom.xml
    └── src/main/java/nl/openvalue/distributor/DistributorApplication.java
```
- Generate each module from start.spring.io (see tech stack) and then move common configuration to the root POM.
- Root POM properties: `java.version=25`, `archunit.version=1.5.1`, `spring-ai.version=2.0.2`;
  `dependencyManagement` imports `spring-ai-bom`.
- `bookstore/application.yml`: datasource `jdbc:h2:mem:bookstore;DB_CLOSE_DELAY=-1` (in-memory on purpose: every restart is a
  clean, freshly migrated and seeded database, so no participant gets stuck on stale local state), `spring.jpa.hibernate.ddl-auto=validate`,
  `spring.jpa.open-in-view=false`, Flyway on, `server.address=127.0.0.1`, `server.port=8080`,
  `management.endpoints.web.exposure.include=health,info`.
- Tests use the main `application.yml` (do NOT add `src/test/resources/application.yml`: it would shadow the main
  file and silently drop `ddl-auto=validate`). Test-only overrides go in `src/test/resources/application-test.yml`
  (for example Flyway locations without `db/seed`, see spec 09), activated for every test via Surefire
  `systemPropertyVariables` `spring.profiles.active=test`.
- Test database hygiene: all `@SpringBootTest` contexts share one in-memory database, and test-managed
  `@Transactional` rollback is not allowed (AFTER_COMMIT listeners must fire). Provide `IntegrationTestBase` in test
  sources with a `DatabaseCleaner` that truncates every application table (not `flyway_schema_history`) in
  `@BeforeEach`. Every `*IntegrationTest` extends it.
- `package-info.java` Javadoc is one sentence per layer, e.g. "Outbound adapter: persistence. Implements the
  `*DataService` ports from domaininteraction." Keep it this short: the workshop needs the agent to *miss* context.

## Requirements

### Requirement: Reproducible build
The repository SHALL build and test with `./mvnw verify` (or `mvnw.cmd verify` on Windows) using only Java 25 and network access to Maven Central, without a local Maven installation, Docker or Node.

#### Scenario: Clean clone builds
- **GIVEN** a fresh clone on a machine with only Temurin 25 and git
- **WHEN** the developer runs `./mvnw verify`
- **THEN** both modules compile, all tests pass and the build finishes with BUILD SUCCESS

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

### Requirement: Schema is migration-managed
The bookstore SHALL run Flyway on startup and Hibernate SHALL only validate the schema.

#### Scenario: Missing migration is detected
- **GIVEN** a JPA entity whose table has no migration (verified in a throwaway branch, not committed)
- **WHEN** the application context starts in a test
- **THEN** startup fails with a schema validation error

### Requirement: Continuous integration
The repository SHALL have a GitHub Actions workflow that runs `./mvnw -B verify` on `ubuntu-latest` and `windows-latest` with Temurin 25 and Maven dependency caching, on push and pull request.

#### Scenario: Windows line endings do not break scripts
- **GIVEN** the repository is checked out on Windows
- **WHEN** CI runs `mvnw.cmd -B verify`
- **THEN** the build passes, and `.gitattributes` keeps `*.sh` and `mvnw` with LF endings

## Tests (required)
- `ArchitectureTest` (production rules) and `ArchitectureSelfTest` (rules against fixtures).
- `BookstoreApplicationTests` and `DistributorApplicationTests`: context loads.
- A health endpoint test per module (`@SpringBootTest(webEnvironment = RANDOM_PORT)`).

## Tasks
1. Generate both modules from start.spring.io, create root POM, wrapper, `.gitattributes`, `.gitignore`, `.editorconfig`.
2. Configure H2, Flyway (`V0__baseline.sql`), `ddl-auto=validate`, Actuator, `server.address`.
3. Create the eight layer packages with `package-info.java`.
4. Write fixture classes in `nl.openvalue.archfixtures` (one violation per rule, one naming violation per package).
   Rules use root-anchored patterns (`nl.openvalue..domain..`) so they apply to both production and fixture packages,
   and naming rules select top-level classes only (`areTopLevelClasses()`, excluding `package-info`).
5. Write `ArchitectureTest` with one named rule per dependency and naming rule; expose the rules as `public static final`
   fields so `ArchitectureSelfTest` can reuse them.
6. Write `ArchitectureSelfTest`, context and health tests, `IntegrationTestBase` and `DatabaseCleaner`.
7. Add CI workflow; push and confirm both OS jobs are green.
8. Measure `./mvnw -q verify` time.

## Definition of done
- `./mvnw verify` green locally and in CI on both operating systems.
- `ArchitectureSelfTest` proves every rule fails on its fixture.
- No `CLAUDE.md`, `.claude/`, `AGENTS.md`, `.mcp.json` on the branch that will become `main` (they may exist on `factory`).
