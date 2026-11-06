# Architecture Standard: Clean Hexagonal Onion (with a dash of DDD)

> Status: normative. Every spec in this pack builds on this file.
> Use it as: the OpenSpec `config.yaml` context, the Spec Kit constitution, or an Agent OS standard
> (`agent-os/standards/architecture/clean-hexagonal-onion.md`). See `workshop/sdd-framework-guide.md`.

This standard describes the *structure* Maik Kingma designed for his earlier "Clean Hexagonal Onion with a
Dash of DDD in Spring" workshops. It is applied here to a new, original domain (an online bookstore).
No code is reused from earlier workshop repositories: only the layering, naming and dependency rules.

## 1. The layers

Root package: `nl.openvalue.bookstore` (bookstore module) and `nl.openvalue.distributor` (distributor module).
The bookstore module has exactly these top-level packages under the root:

```
                 +-----------------------------------------------------------+
  inbound   ---> | command | query | process | assistant                     |  outer adapters
                 +-----------------------------------------------------------+
                 |                 domaininteraction                         |  flows, ports, DTOs
                 |   +-------------------------------------------------+     |
                 |   |                    domain                       |     |  aggregates, VOs, events
                 |   +-------------------------------------------------+     |
                 +-----------------------------------------------------------+
  outbound  <--- | data | acl                                                |  outer adapters
                 +-----------------------------------------------------------+
```

| Package | Role | Typical classes (naming is enforced) |
|---|---|---|
| `domain` | Aggregates, value objects, domain events, domain exceptions. Pure Java. | `Book`, `Isbn`, `Money`, `Order`, `OrderLine`, `StockRunningLowEvent`, `InsufficientStockException` |
| `domaininteraction` | Use-case orchestration ("flows"), the ports to the outside world, DTOs that cross the boundary, mappers DTO to domain. | `CatalogueFlow`, `BookDataService` (port), `DistributorAppService` (port), `BookDTO`, `BookDomainMapper` |
| `command` | Inbound REST adapter for state-changing requests. | `BookCommands`, `AddBookPayload` |
| `query` | Inbound REST adapter for read requests. | `BookQueries`, `BookView` |
| `process` | Inbound event-driven adapter: reacts to domain events after commit. | `EventProcessor`, `RequestRestockDelegate` |
| `assistant` | Inbound conversational adapter (Spring AI). The only place where Spring AI types appear. | `AssistantCommands`, `BookstoreTools`, `AssistantConfig`, `OfflineChatModel` |
| `data` | Outbound persistence adapter. Implements `*DataService` ports. | `BookDataServiceImpl`, `BookJPA`, `BookJPAMapper`, `BookRepository` |
| `acl` | Outbound anti-corruption layer to external systems. Implements `*AppService` ports. | `DistributorAppServiceImpl` (+ private payload records) |

## 2. Dependency rules (enforced by ArchUnit, never by convention alone)

1. `domain` may only be accessed by `domaininteraction`.
2. `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data`, `acl`.
3. No outer adapter (`command`, `query`, `process`, `assistant`, `data`, `acl`) may be accessed by any other layer.
4. `domain` depends on `java..` only. No Spring, no Jakarta Persistence, no Jackson, no Lombok, no Spring AI.
5. `jakarta.persistence..` and `org.springframework.data..` are only allowed in `data`.
6. `org.springframework.web.client..` (`RestClient`) is only allowed in `acl`.
7. `org.springframework.ai..` is only allowed in `assistant`.
8. `@RestController` classes only live in `command`, `query` and `assistant`.
9. Documented exception (keep it small and explicit): `process` may reference domain *event* classes
   (`..domain..*Event`), because the event type is the contract of the listener. The ArchUnit test lists this as a
   named `ignoreDependency` with a comment explaining why. Adding a new exception requires a spec change.

## 3. Naming rules (enforced by ArchUnit)

Naming rules apply to top-level classes only (`package-info`, nested, anonymous and synthetic classes are excluded).
Prefer top-level records for nested JSON shapes so they carry a proper suffix (`OrderLinePayload`, `OrderLineView`,
`PendingRestockView`); private nested payload records inside an `*AppServiceImpl` are the exception.
All layer patterns are anchored at the company root (`nl.openvalue..domain..`, `nl.openvalue..query..`), so framework
packages such as `org.springframework.data.domain` never match.

| Package | Class names must end with |
|---|---|
| `command` | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` |
| `query` | `Queries`, `View`, `ExceptionHandler` |
| `process` | `EventProcessor`, `Delegate` |
| `assistant` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` |
| `data` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` |
| `acl` | `AppServiceImpl`, `Properties` (payload records are private nested records) |
| `domaininteraction` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` |

## 4. Building blocks and how they behave

### Aggregates (domain)
- Constructor is private. New instances come from an intention-revealing static factory
  (`Book.addToCatalogue(...)`, `Order.place(...)`). Rehydration from storage uses a separate static `restore(...)`
  factory that skips creation rules but still validates invariants.
- State changes happen through behaviour methods named after the domain (`receive`, `reserve`, `ship`),
  never through setters.
- Invariants are checked inside the aggregate and violations throw a domain exception (unchecked, in `domain`).
- Aggregates register domain events in an internal list; `pullDomainEvents()` returns and clears them.
- Domain events are records with JDK-typed fields only (`UUID bookId`, `int available`, `Instant occurredAt`), so the
  `process` layer depends on the event class and nothing else from `domain`.

### Value objects (domain)
- Java `record`s with validation in the compact constructor: `Isbn` (ISBN-13 with checksum), `Money`
  (`BigDecimal`, scale 2, `RoundingMode.HALF_EVEN`, currency EUR), `Quantity`.
- Never use `double` or `float` for money.

### Flows (domaininteraction)
- One flow per aggregate or business capability (`CatalogueFlow`, `InventoryFlow`, `OrderFlow`).
- Public methods are use cases. They load DTOs through ports, map to domain with a `*DomainMapper`, call domain
  behaviour, map back to a DTO, save through the port.
- Flows are annotated `@Service` and `@Transactional` (use-case boundary).
- After saving, a flow publishes the aggregate's pulled domain events with Spring's `ApplicationEventPublisher`,
  inside the same transaction, so `process` listeners run only after a successful commit.
- Flow method parameters and return types are JDK types or DTOs (`UUID bookId`, `String isbn`, `BigDecimal price`),
  because their callers are adapters. Flows create the domain value objects themselves.
- `@Service`, `@Transactional`, `ApplicationEventPublisher` and the SLF4J `Logger` are the only framework types allowed
  in `domaininteraction`.

### Use-case exceptions (domaininteraction)
- Four unchecked exceptions in `domaininteraction`: `NotFoundException` (404), `ConflictException` (409),
  `InvalidInputException` (422) and `UnavailableException` (503). Each has a stable kebab-case `code`
  (`isbn-already-exists`), a short human `title` ("ISBN already in catalogue"), a `detail` message and optional
  `details` (a list of key/value maps, e.g. the short items of an order or `{field, message}` for a bad field).
- Flows translate domain exceptions into these at the use-case boundary (domain exceptions never reach adapters,
  because adapters may not access `domain`). Port exceptions extend them directly.

### Ports (domaininteraction)
- `*DataService` interfaces for persistence, `*AppService` interfaces for external systems.
- Ports speak DTOs and JDK types (`String`, `UUID`, `BigDecimal`, `LocalDate`, `int`), never domain types, never JPA
  entities, never external payloads. Reason: `data` and `acl` implement the ports and may not access `domain`.
- Port-specific exceptions are nested in the port interface (e.g. `BookDataService.BookNotFoundException`) and extend
  one of the use-case exceptions below.

### DTOs (domaininteraction)
- Java `record`s named `*DTO` with JDK-typed components (a price is `BigDecimal amount` plus `String currency`).
  They carry state across the boundary. They never carry domain events or domain types.
- A DTO has a convenience constructor from its aggregate (`new BookDTO(book)`); the reverse is a `*DomainMapper`.

### Inbound adapters
- `command`: `POST /{resource}/commands/{action}` for collection-level commands and
  `POST /{resource}/{id}/commands/{action}` for instance commands. Request bodies are `*Payload` records with
  Jakarta Validation annotations. Success returns `202 Accepted`; a command that creates an aggregate returns a
  `CommandResult(id)` body.
- `query`: `GET /{resource}` and `GET /{resource}/{id}`. Responses are `*View` records shaped for the reader, never DTOs.
- Errors are translated to RFC 9457 `ProblemDetail` by a `@RestControllerAdvice` per adapter package
  (`*ExceptionHandler`): the four use-case exceptions to 404/409/422/503 with ProblemDetail `title` = exception title,
  `detail` = exception detail, and properties `code` and `details`; bean validation failures to 400 with
  `code = validation-failed` and `details = [{field, message}]`.
- `process`: an `EventProcessor` with `@TransactionalEventListener(phase = AFTER_COMMIT)` methods that hand the
  event to a `*Delegate`. A delegate runs `@Transactional(propagation = REQUIRES_NEW)` and calls exactly one flow method.
- `assistant`: see spec 08.

### Outbound adapters
- `data`: `*DataServiceImpl` implements the port with a Spring Data `*Repository` and a `*JPAMapper`.
  Schema changes only through new Flyway migrations (`V{n}__{description}.sql`); existing migrations are never edited.
  JPA runs with `ddl-auto=validate`.
- `acl`: `*AppServiceImpl` implements the port with Spring `RestClient`. External payloads are private nested
  records. Every external failure is translated into a port exception. Base URLs come from configuration
  (`distributor.base-url`).

## 5. Testing standard

| Layer | Test type | Tooling | Naming |
|---|---|---|---|
| `domain` | Pure unit tests, no Spring | JUnit, AssertJ | `BookTest` |
| `domaininteraction` | Unit tests with mocked ports | JUnit, Mockito, AssertJ | `CatalogueFlowTest` |
| `command`, `query`, `assistant` | Web slice tests with mocked flows | `@WebMvcTest`, `@MockitoBean`, MockMvc | `BookCommandsTest` |
| `data` | JPA slice tests on H2 | `@DataJpaTest` | `BookDataServiceImplTest` |
| `acl` | HTTP contract tests against a mocked server | `MockRestServiceServer` bound to `RestClient.Builder` | `DistributorAppServiceImplTest` |
| `process` | Unit tests for delegate and processor | JUnit, Mockito | `EventProcessorTest` |
| End to end | Full context, H2, real HTTP | `@SpringBootTest` + MockMvc or `RestTestClient` | `*IntegrationTest` |
| Architecture | Layer, dependency and naming rules | ArchUnit `@ArchTest` | `ArchitectureTest` |

- `./mvnw verify` runs everything and must stay under 60 seconds on a 2024 laptop (hooks run it).
- Every requirement scenario in a spec maps to at least one test. Test method names describe behaviour
  (`rejectsOrderWhenStockIsInsufficient`).
- No test may call the real distributor or a real LLM.

## 6. Things that are deliberately NOT in this standard
- Lombok (records and plain Java are enough on Java 25).
- MapStruct (mappers are hand-written, small and explicit).
- Hexagonal "ports" and "adapters" package names: this codebase uses the onion vocabulary above on purpose.
