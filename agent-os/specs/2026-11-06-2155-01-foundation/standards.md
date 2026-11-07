# Standards for Spec 01 Foundation

The following standards apply to this work.

---

## architecture (pointer standard, `agent-os/standards/architecture.md`)

The architecture standard for this repository is `docs/spec-pack/product/architecture-standard.md`. Read that file; it wins over every skill in `.claude/skills/`.

- Bookstore code is written through the `hexagonal-slice` skill, which pulls in `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing`.
- The referee is `./mvnw clean verify` from the repo root.

### Key points of `docs/spec-pack/product/architecture-standard.md` for this spec

Read the full standard; it is not copied here because it is tracked in this repository. The parts spec 01 encodes:

- Section 1, the layers: root package `nl.openvalue.bookstore` holds `BookstoreApplication` and only spec-mandated cross-cutting configuration; exactly the top-level packages `domain`, `domaininteraction`, `command`, `query`, `process`, `assistant`, `data`, `acl`.
- Section 2, dependency rules (enforced by ArchUnit): (1) `domain` may only be accessed by `domaininteraction`; (2) `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data`, `acl`; (3) no outer adapter may be accessed by any other layer; (4) `domain` depends on `java..` only; (5) `jakarta.persistence..` and `org.springframework.data..` only in `data`; (6) `org.springframework.web.client..` only in `acl`; (7) `org.springframework.ai..` only in `assistant`; (8) `@RestController` classes only in `command`, `query` and `assistant`; (9) documented exception: `process` may reference `..domain..*Event` classes, listed as a named `ignoreDependency` with a comment.
- Section 3, naming rules (top-level classes only, patterns anchored at `nl.openvalue..`): `command` ends with `Commands`, `Payload`, `CommandResult`, `ExceptionHandler`; `query` with `Queries`, `View`, `ExceptionHandler`; `process` with `EventProcessor`, `Delegate`; `assistant` with `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel`; `data` with `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository`; `acl` with `AppServiceImpl`, `Properties`; `domaininteraction` with `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception`.
- Section 5, testing standard: `@DataJpaTest` and `@SpringBootTest` on Testcontainers PostgreSQL with `@ServiceConnection` and one shared container; `*IntegrationTest` naming; ArchUnit `@ArchTest` in `ArchitectureTest`; `./mvnw verify` under 60 seconds; no in-memory database; Docker missing is an environment error, never a skip.
