# Standards for Spec 02 Catalogue

The following standards apply to this work.

---

## architecture (pointer standard, `agent-os/standards/architecture.md`)

The architecture standard for this repository is `docs/spec-pack/product/architecture-standard.md`. Read that file; it wins over every skill in `.claude/skills/`. Bookstore code is written through the `hexagonal-slice` skill, which pulls in `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing`. The referee is `./mvnw clean verify` from the repo root.

### Parts of the standard spec 02 encodes

- Section 1: the root package holds `BookstoreApplication` and only spec-mandated cross-cutting configuration (the `Clock` bean, created here and reused by spec 04); spec 02 fills `domain`, `domaininteraction`, `command`, `query` and `data`.
- Section 2: rule 1 (`domain` only from `domaininteraction`), rule 2 (`domaininteraction` only from the six adapters), rule 3 (adapters not accessed by other layers, so the two exception handlers are deliberately duplicated), rule 4 (`domain` depends on `java..` only: `BigDecimal`, `Currency`, `UUID`, `Clock` are allowed), rule 5 (`jakarta.persistence..` and `org.springframework.data..` only in `data`), rule 8 (`@RestController` only in `command`, `query`, `assistant`).
- Section 3: naming suffixes per layer (`Commands`, `Payload`, `CommandResult`, `ExceptionHandler`; `Queries`, `View`, `ExceptionHandler`; `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository`; `Flow`, `DataService`, `DTO`, `DomainMapper`, `Exception`); nested port exceptions are exempt; `domain` and the root package have no naming rule.
- Section 4: aggregates with private constructors, intention-revealing factories and `restore`; value objects as validated records (`Isbn` with checksum, `Money` with scale 2 HALF_EVEN, EUR, never `double` or `float`); flows `@Service @Transactional` with JDK-typed parameters that create the value objects themselves; the four use-case exceptions with `code`, `title`, `detail`, `details`; ports speak DTOs and JDK types with nested port exceptions; DTOs as records with a constructor from the aggregate and a `*DomainMapper` back; command routes `POST /{resource}/commands/{action}` and `POST /{resource}/{id}/commands/{action}` returning 202 (with `CommandResult(id)` on creation); query routes `GET /{resource}` and `GET /{resource}/{id}` returning views; RFC 9457 `ProblemDetail` per adapter with `code` and `details`, bean validation to 400 `validation-failed`; data adapter with Flyway migrations only and `ddl-auto=validate`.
- Section 5: domain unit tests, flow tests with mocked ports, `@WebMvcTest` with `@MockitoBean`, `@DataJpaTest` on Testcontainers PostgreSQL with `@AutoConfigureTestDatabase(replace = NONE)` and one shared container, `*IntegrationTest` on the full context, ArchUnit; `./mvnw verify` under 60 seconds; no in-memory database; every scenario maps to at least one test with a behaviour-describing name.

## Factory rules carried into every task (CLAUDE.md and the run's ground rules)

- Never skip, disable, delete or weaken a test or an ArchUnit rule; never `--no-verify`; never force-push.
- The app reads `BOOKSTORE_AI_API_KEY`; never `ANTHROPIC_API_KEY`; no key committed.
- Docs, comments and specs in plain English with no em dashes; product code and config never reference `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`.
- Versions only from `docs/spec-pack/product/tech-stack.md`; nothing new pinned in spec 02.
- No discount concept anywhere in `bookstore/src`.
