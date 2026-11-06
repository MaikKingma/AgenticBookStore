# References for Spec 01 Foundation

## Similar Implementations

None. The repository holds no code before this spec.

## Research used for shaping (session artifacts, not tracked)

The reports live under `.claude/comms/mailboxes/<id>/outbox/` of the factory session that shaped this spec. Their load-bearing findings are restated here so the spec stands on its own.

### Initializr generation (res-1)

- start.spring.io accepts `bootVersion=4.1.2`, `javaVersion=25`, `type=maven-project`, `packaging=jar`, `groupId=nl.openvalue`, `artifactId`, `name`, `baseDir` and `packageName` per module, `dependencies=` with the ids from `product/tech-stack.md`.
- Boot 4 starter names: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, plus `org.flywaydb:flyway-database-postgresql`, runtime `spring-boot-docker-compose` (optional) and `org.postgresql:postgresql`; test scope `spring-boot-starter-*-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`. The module POMs carry no versions.
- Generated per module: `.gitattributes`, `.gitignore`, `HELP.md`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `application.properties`, `*ApplicationTests`; bookstore also `compose.yaml`, `TestcontainersConfiguration`, `TestBookstoreApplication`, all with `postgres:latest`, which must become `postgres:18.6`.

### ArchUnit 1.5.1 (res-2)

- `com.tngtech.archunit:archunit-junit6:1.5.1` exists. Imports: `com.tngtech.archunit.junit.AnalyzeClasses`, `com.tngtech.archunit.junit.ArchTest`, `com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests`, `com.tngtech.archunit.lang.ArchRule`.
- `ignoreDependency` exists only on `layeredArchitecture()` and `onionArchitecture()`; `onlyBeAccessed()` misses type-only dependencies; `allowEmptyShould(true)` maps to optional layers for layered rules.
- Naming: `.that().resideInAPackage(..).and().areTopLevelClasses().and().doNotHaveSimpleName("package-info").should().haveSimpleNameEndingWith(a).orShould()...`.
- Self-test: `rule.evaluate(classes).hasViolation()`, `getFailureReport().getDetails()`; `DoNotIncludeTests` excludes `target/test-classes`; `packages = "nl.openvalue.bookstore"` never imports the sibling `nl.openvalue.archfixtures`.

### Testcontainers 2.0.5 with Boot 4.1.2 (res-3)

- Container class `org.testcontainers.postgresql.PostgreSQLContainer` (non-generic); `new PostgreSQLContainer("postgres:18.6")`.
- A plain `@Bean` starts one container per Spring context; a static singleton returned from the bean gives one per JVM.
- `GET /actuator/health` shows no components unless `management.endpoint.health.show-components=always`; with it, `components.db.status` is `UP`.
- Health test client in Boot 4: `RestTestClient` with `@AutoConfigureRestTestClient`.
- Flyway 12.4.0 accepts a comment-only `V0__baseline.sql` and records `version='0'`, `description='baseline'`, `success=true`.
- `DatabaseCleaner`: query `information_schema.tables` for base tables in `public`, exclude `flyway_schema_history`, `TRUNCATE ... RESTART IDENTITY CASCADE`; no-op without tables.
- Docker down: tests error with "Could not find a valid Docker environment", `Skipped: 0`.
- `spring.docker.compose.skip.in-tests` defaults to `true`; `./mvnw -pl bookstore spring-boot:run` from the repo root runs the app with the module basedir as working directory, so `./compose.yaml` resolves.

### GitHub Actions (res-4)

- `actions/checkout@v7`, `actions/setup-java@v6` with `distribution: temurin`, `java-version: '25'`, `cache: maven`.
- `on: push` and `on: pull_request` without filters fire on every branch and every PR; check names equal job names.
- ubuntu-latest has Docker; windows-latest runs Windows containers only; the Windows step runs with `shell: cmd`.
- `mvnw` must be committed with mode 100755.
