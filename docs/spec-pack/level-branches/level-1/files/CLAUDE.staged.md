# Agentic Bookstore

Spring Boot 4.1 / Java 25 online bookstore (module `bookstore`, PostgreSQL in Docker), a fake book distributor
(module `distributor`) and a React storefront (module `frontend`, built by Maven, served at `/shop/`).

## Commands
- Build and test everything: `./mvnw verify` (Windows: `mvnw.cmd verify`). Docker must be running (Testcontainers).
  Always use the wrapper, never `mvn`.
- One test class: `./mvnw -pl bookstore test -Dtest=BookTest`
- Run: `./mvnw -pl bookstore spring-boot:run` (port 8080), `./mvnw -pl distributor spring-boot:run` (port 8090)

## Definition of done
- `./mvnw verify` is green, including `ArchitectureTest`. Show the last lines of the output.
  Never say tests pass without having run them in this session.
- Every new behaviour has a test in the layer where the behaviour lives.

## Rules that are not obvious from a quick look
- Read docs/architecture.md before adding or moving classes. Inbound adapters (command, query, process, assistant)
  only call flows in `domaininteraction`. Only flows touch domain objects.
- `domain` is plain Java: no Spring, JPA, Jackson or Lombok imports.
- Money is always `Money` (BigDecimal, scale 2, HALF_EVEN). Never double or float.
- Schema changes only through a new Flyway migration in `bookstore/src/main/resources/db/migration`.
  Never edit an existing migration.
- No Lombok, no MapStruct: records and small hand-written mappers.
- The distributor is an external system. Bookstore code reaches it only through the `DistributorAppService` port.

## Working agreements
- Stay inside the scope of the request. Propose unrelated refactors instead of doing them.
- Small commits, conventional commit messages (feat:, fix:, test:, refactor:, docs:).
