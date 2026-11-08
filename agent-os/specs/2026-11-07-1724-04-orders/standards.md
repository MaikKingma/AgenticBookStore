# Standards for Spec 04 Orders

The following standards apply to this work.

---

## architecture (pointer standard, `agent-os/standards/architecture.md`)

The architecture standard for this repository is `docs/spec-pack/product/architecture-standard.md`. Read that file; it wins over every skill in `.claude/skills/`. Bookstore code is written through the `hexagonal-slice` skill, which pulls in `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing`. The referee is `./mvnw clean verify` from the repo root.

### Parts of the standard spec 04 encodes

- Section 1: the root package keeps `BookstoreApplication` and the spec 02 `Clock` bean (`ClockConfig`), which `OrderFlow` reuses; spec 04 extends `domain`, `domaininteraction`, `command`, `query` and `data`; `process` stays empty until spec 07.
- Section 2: rule 1 (the adapters read totals and line totals from `OrderDTO` and `OrderLineDTO`, never from `Order`), rule 2, rule 3, rule 4 (`domain` depends on `java..` only; `OrderPlacedEvent` carries JDK types only), rule 5 (`jakarta.persistence..` and `org.springframework.data..` only in `data`), rule 8 (the two new controllers live in `command` and `query`).
- Section 3: naming suffixes per layer (`Commands`, `Payload`, `CommandResult`; `Queries`, `View`, `ExceptionHandler`; `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository`; `Flow`, `DataService`, `DTO`, `DomainMapper`, `Exception`); nested classes (`OrderDataService.OrderNotFoundException`, `OrderQueries.MissingQueryParameterException`) are exempt; `domain` has no naming rule.
- Section 4: the aggregate with a private constructor, `place` and `restore` factories, behaviour method `ship`, invariants throwing domain exceptions, the domain event registered in an internal list and returned by `pullDomainEvents()`; `EmailAddress` and `OrderId` as validated records; `Money` for every amount, never `double` or `float`; the flow `@Service @Transactional` with JDK-typed and DTO parameters, creating the value objects itself and publishing the pulled events of the order and of every changed stock item with `ApplicationEventPublisher` after saving, inside the same transaction; the four use-case exceptions with `code`, `title`, `detail`, `details`, domain exceptions translated at the use-case boundary; the port speaking DTOs and JDK types with a nested port exception; DTOs as records with a constructor from the aggregate and a `*DomainMapper` back; command routes `POST /orders/commands/place` (202 with `CommandResult`) and `POST /orders/{id}/commands/ship` (202); query routes `GET /orders/{id}` and `GET /orders` returning views; RFC 9457 `ProblemDetail` per adapter with `code` and `details`, bean validation to 400 `validation-failed`; data adapter with a new Flyway migration only and `ddl-auto=validate`.
- Section 5: domain unit tests, flow tests with mocked ports and a mocked publisher, `@WebMvcTest` with `@MockitoBean`, `@DataJpaTest` on Testcontainers PostgreSQL with `@AutoConfigureTestDatabase(replace = NONE)` and one shared container, `OrderIntegrationTest` on the full context with the order event recorder, ArchUnit unchanged (21 rules, 40 self-tests); `./mvnw verify` under 60 seconds; no in-memory database; every scenario maps to at least one test with a behaviour-describing name.

## Factory rules carried into every task (CLAUDE.md and the run's ground rules)

- Never skip, disable, delete or weaken a test or an ArchUnit rule; existing tests are extended only in the files a task names; never `--no-verify`; never force-push.
- The app reads `BOOKSTORE_AI_API_KEY`; never `ANTHROPIC_API_KEY`; no key committed.
- Docs, comments and specs in plain English with no em dashes; product code and config never reference `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`; no literal non-ASCII characters in the files spec 04 writes (non-ASCII test data is built from code points).
- Versions only from `docs/spec-pack/product/tech-stack.md`; nothing new pinned in spec 04; no POM change.
- Time only through the injected `Clock`; never `Instant.now()` or `Clock.system*` in `domain` and `domaininteraction`.
- Level 3 is reserved: `grep -ri "cancel" bookstore/src/main/java` returns nothing; no release, no CANCELLED status, no cancel endpoint.
