# Standards for Spec 03 Inventory

The following standards apply to this work.

---

## architecture (pointer standard, `agent-os/standards/architecture.md`)

The architecture standard for this repository is `docs/spec-pack/product/architecture-standard.md`. Read that file; it wins over every skill in `.claude/skills/`. Bookstore code is written through the `hexagonal-slice` skill, which pulls in `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing`. The referee is `./mvnw clean verify` from the repo root.

### Parts of the standard spec 03 encodes

- Section 1: the root package keeps `BookstoreApplication` and the spec 02 `Clock` bean (`ClockConfig`), which spec 03 reuses; spec 03 extends `domain`, `domaininteraction`, `command`, `query` and `data`; `process` stays empty until spec 07.
- Section 2: rule 1 (`domain` only from `domaininteraction`: the adapters read `available` and `lowStock` from `StockItemDTO`, never from `StockItem`), rule 2, rule 3 (the query handler gains its own 400 method instead of sharing the command handler's), rule 4 (`domain` depends on `java..` only: `UUID`, `Instant`, `Clock`, `List` are allowed; the event record carries JDK types only), rule 5 (`jakarta.persistence..` and `org.springframework.data..` only in `data`), rule 8 (`@RestController` only in `command`, `query`, `assistant`), rule 9 (the documented `process` to `*Event` exception stays unused in spec 03).
- Section 3: naming suffixes per layer (`Commands`, `Payload`; `Queries`, `View`, `ExceptionHandler`; `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository`; `Flow`, `DataService`, `DTO`, `DomainMapper`); nested classes (the port's `StockItemNotFoundException`, the controller's `InvalidQueryParameterException`) are exempt; `domain` and the root package have no naming rule.
- Section 4: the aggregate with a private constructor, `openFor` and `restore` factories, behaviour methods `receive`, `reserve`, `ship`, `correct`, invariants throwing domain exceptions, domain events registered in an internal list and returned by `pullDomainEvents()`, events as records with JDK-typed fields; `Quantity` as a validated record; the flow `@Service @Transactional` with JDK-typed parameters creating the value objects itself and publishing pulled events with `ApplicationEventPublisher` after saving, inside the same transaction; the four use-case exceptions with `code`, `title`, `detail`, `details`, domain exceptions translated at the use-case boundary; the port speaking DTOs and JDK types with a nested port exception; the DTO as a record with a constructor from the aggregate and a `*DomainMapper` back; command routes `POST /inventory/{bookId}/commands/{action}` returning 202; query routes `GET /inventory` and `GET /inventory/{bookId}` returning views; RFC 9457 `ProblemDetail` per adapter with `code` and `details`, bean validation to 400 `validation-failed`; data adapter with a new Flyway migration only and `ddl-auto=validate`.
- Section 5: domain unit tests, flow tests with mocked ports and a mocked publisher, `@WebMvcTest` with `@MockitoBean`, `@DataJpaTest` on Testcontainers PostgreSQL with `@AutoConfigureTestDatabase(replace = NONE)` and one shared container, `*IntegrationTest` on the full context with the test after-commit listener the spec asks for, ArchUnit unchanged; `./mvnw verify` under 60 seconds; no in-memory database; every scenario maps to at least one test with a behaviour-describing name.

## Factory rules carried into every task (CLAUDE.md and the run's ground rules)

- Never skip, disable, delete or weaken a test or an ArchUnit rule; existing tests are extended, or have their pinned shapes updated deliberately at the named sites; never `--no-verify`; never force-push.
- The app reads `BOOKSTORE_AI_API_KEY`; never `ANTHROPIC_API_KEY`; no key committed.
- Docs, comments and specs in plain English with no em dashes; product code and config never reference `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`; no literal non-ASCII characters in the Java files spec 03 writes (non-ASCII test data is built from code points).
- Versions only from `docs/spec-pack/product/tech-stack.md`; nothing new pinned in spec 03; no POM change.
- Time only through the injected `Clock`; never `Instant.now()` or `Clock.system*` in `domain` and `domaininteraction`.
