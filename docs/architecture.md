# Architecture: Clean Hexagonal Onion

The bookstore is built as an onion with a little domain-driven design. This page is the rulebook for the `bookstore`
module: which package a class belongs in, who may call whom, how classes are named and how each layer is tested. The
rules are enforced by `ArchitectureTest` (in `bookstore/src/test/java/nl/openvalue/bookstore/`), so a violation breaks
the build. The rule names below are the constant names in that file.

The `distributor` module is a small separate service (a fictional book distributor). It is not part of the onion.

## 1. The layers

The root package is `nl.openvalue.bookstore`. It holds `BookstoreApplication` and the configuration that belongs to no
layer (`ClockConfig`, `StorefrontConfig`). Configuration of one adapter stays in that adapter. Under the root there
are exactly eight packages:

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

| Package | Role | Examples |
|---|---|---|
| `domain` | Aggregates, value objects, domain events, domain exceptions. Pure Java. | `Book`, `Isbn`, `Money`, `Order`, `OrderLine`, `StockItem`, `StockRunningLowEvent`, `InsufficientStockException` |
| `domaininteraction` | Use cases ("flows"), the ports to the outside world, the DTOs that cross them, the mappers from DTO to domain. | `CatalogueFlow`, `BookDataService` (port), `DistributorAppService` (port), `BookDTO`, `BookDomainMapper` |
| `command` | Inbound REST adapter for requests that change state. | `BookCommands`, `AddBookPayload` |
| `query` | Inbound REST adapter for requests that read state. | `BookQueries`, `BookView` |
| `process` | Inbound event adapter: reacts to domain events after the commit. | `EventProcessor`, `RequestRestockDelegate` |
| `assistant` | Inbound conversational adapter. The only place where Spring AI types may appear. | see the suffixes in section 3 |
| `data` | Outbound persistence adapter. Implements the `*DataService` ports. | `BookDataServiceImpl`, `BookJPA`, `BookJPAMapper`, `BookRepository` |
| `acl` | Outbound anti-corruption layer towards the distributor. Implements the `*AppService` ports. | `DistributorAppServiceImpl`, `DistributorProperties` |

Read the diagram from the outside in: a request enters through an inbound adapter, which calls a flow. The flow loads
data through a port, lets the domain decide, and saves through a port. The domain knows nothing about HTTP, SQL or the
distributor.

## 2. Dependency rules

Every rule is an `@ArchTest` constant in `ArchitectureTest`.

| # | Rule | Constant in `ArchitectureTest` |
|---|---|---|
| 1 | `domain` may only be accessed by `domaininteraction` (with the one exception of rule 9). | `DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION` |
| 2 | `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data` and `acl`. | `DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS` |
| 3 | No outer adapter may be accessed by any other layer, one rule per adapter. | `COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS`, `QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS`, `PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS`, `ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS`, `DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS`, `ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS` |
| 4 | `domain` depends on `java..` and itself only: no Spring, no Jakarta Persistence, no Jackson, no Spring AI. | `DOMAIN_DEPENDS_ON_JAVA_ONLY` |
| 5 | `jakarta.persistence..` and `org.springframework.data..` only in `data`. | `PERSISTENCE_TYPES_ONLY_IN_DATA` |
| 6 | `org.springframework.web.client..` (`RestClient`) only in `acl`. | `REST_CLIENT_ONLY_IN_ACL` |
| 7 | `org.springframework.ai..` only in `assistant`. | `SPRING_AI_ONLY_IN_ASSISTANT` |
| 8 | `@RestController` classes only in `command`, `query` and `assistant`. | `REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT` |
| 9 | The documented process exception, see section 5. | part of rule 1 |

Two more rules sit next to these:

- `APP_SERVICE_IMPL_NESTED_CLASSES_ONLY_IN_ACL`: the private payload records nested in an `*AppServiceImpl` describe
  the distributor's JSON and never leave `acl`.
- `NO_DOUBLE_OR_FLOAT_FOR_MONEY`: in `domain`, `domaininteraction`, `command` and `query`, no field and no method or
  constructor parameter has the type `double` or `float`, boxed or not. A price is `Money` in the domain and a
  `BigDecimal` plus a currency code outside it.

The layer patterns are anchored at the company root (`nl.openvalue..domain..`), so framework packages such as
`org.springframework.data.domain` never match.

## 3. Naming rules

The top-level classes of a package must end with one of the suffixes below. Nested, anonymous and synthetic classes and
`package-info` are not checked. Prefer a top-level record for a nested JSON shape (`OrderLinePayload`,
`PendingRestockView`) so that it carries a proper suffix.

| Package | Class names must end with | Constant in `ArchitectureTest` |
|---|---|---|
| `command` | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` | `COMMAND_CLASS_NAMES` |
| `query` | `Queries`, `View`, `ExceptionHandler` | `QUERY_CLASS_NAMES` |
| `process` | `EventProcessor`, `Delegate` | `PROCESS_CLASS_NAMES` |
| `assistant` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` | `ASSISTANT_CLASS_NAMES` |
| `data` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` | `DATA_CLASS_NAMES` |
| `acl` | `AppServiceImpl`, `Properties` | `ACL_CLASS_NAMES` |
| `domaininteraction` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` | `DOMAININTERACTION_CLASS_NAMES` |

The `domain` package has no suffix rule: name aggregates, value objects, events and exceptions after the business.

## 4. Building blocks

### Aggregates and value objects (domain)

- An aggregate (`Book`, `Order`, `StockItem`) has a private constructor. New instances come from an intention-revealing
  static factory (`Book.addToCatalogue(...)`, `Order.place(...)`). Loading from storage uses a separate static
  `restore(...)` that skips the creation rules but still checks the invariants.
- State changes through behaviour methods named after the business (`receive`, `reserve`, `ship`), never through
  setters. A violated invariant throws an unchecked domain exception that lives in `domain`.
- An aggregate collects its domain events in a list. `pullDomainEvents()` returns them and clears the list.
- A domain event is a record with JDK types only (`UUID bookId`, `int available`, `Instant occurredAt`), so the
  `process` layer needs nothing else from `domain`.
- Value objects are records that validate in the compact constructor: `Isbn` (ISBN-13 with checksum), `Money`
  (`BigDecimal`, scale 2, rounding `HALF_EVEN`, currency EUR), `Quantity`. Never use `double` or `float` for money.

### Flows, ports and DTOs (domaininteraction)

- A flow (`CatalogueFlow`, `InventoryFlow`, `OrderFlow`) is a `@Service` and `@Transactional`: its public methods are
  the use cases and the transaction boundary. A flow loads DTOs through ports, maps them to the domain with a
  `*DomainMapper`, calls domain behaviour, maps back to a DTO and saves through the port.
- After saving, a flow publishes the pulled domain events with `ApplicationEventPublisher`, inside the same
  transaction. A listener bound to the commit therefore only ever sees events of saved data.
- Flow parameters and results are JDK types or DTOs, because their callers are adapters that may not touch `domain`.
- `@Service`, `@Transactional`, `ApplicationEventPublisher` and the SLF4J `Logger` are the only framework types
  allowed in `domaininteraction`.
- A port is an interface: `*DataService` for persistence, `*AppService` for external systems. Ports speak DTOs and JDK
  types, never domain types, JPA entities or external payloads. Exceptions that belong to one port are nested in it
  (`DistributorAppService.DistributorUnavailableException`) and extend a use-case exception.
- A DTO is a record named `*DTO` with JDK-typed components (a price is `BigDecimal amount` plus `String currency`).
  It has a convenience constructor from its aggregate; the way back is a `*DomainMapper`.
- Four unchecked use-case exceptions map to HTTP: `NotFoundException` (404), `ConflictException` (409),
  `InvalidInputException` (422) and `UnavailableException` (503). Each has a stable kebab-case `code`
  (`isbn-already-exists`), a short `title`, a `detail` message and optional `details`. Flows translate domain
  exceptions into them, so no domain exception reaches an adapter.

### Inbound adapters

- `command`: `POST /{resource}/commands/{action}` for collection level commands and
  `POST /{resource}/{id}/commands/{action}` for commands on one instance. Request bodies are `*Payload` records with
  Jakarta Validation. Success is `202 Accepted`; a command that creates something answers with `CommandResult(id)`.
- `query`: `GET /{resource}` and `GET /{resource}/{id}`. Responses are `*View` records shaped for the reader, never
  DTOs.
- Errors become RFC 9457 `ProblemDetail` in one `@RestControllerAdvice` per adapter package (`*ExceptionHandler`): the
  four use-case exceptions map to 404, 409, 422 and 503 with the exception's title and detail and the properties
  `code` and `details`; a bean validation failure maps to 400 with `code` `validation-failed` and `details` as a list
  of `{field, message}`.
- `process`: `EventProcessor` has `@TransactionalEventListener(phase = AFTER_COMMIT)` methods that hand the event to a
  `*Delegate`. A delegate runs in `REQUIRES_NEW` and calls one flow method.
- `assistant`: the chat adapter. It is the only package that may use Spring AI.

### Outbound adapters

- `data`: a `*DataServiceImpl` implements the port with a Spring Data `*Repository` and a `*JPAMapper`. The schema
  changes only through new Flyway migrations (`V{n}__{description}.sql` in `db/migration`); an existing migration is
  never edited. JPA runs with `ddl-auto: validate`, so Hibernate never changes the schema. The demo data is a
  repeatable migration in `db/seed`, which only the default profile loads (see the README).
- `acl`: an `*AppServiceImpl` implements the port with Spring `RestClient`. External payloads are private nested
  records. Every external failure becomes a port exception. The base URL comes from `distributor.base-url`.

## 5. The documented process exception

Rule 1 has exactly one exception. In `DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION` a single `ignoreDependency` lets
classes in `process` reference classes in `domain` whose simple name ends with `Event`:

```
.ignoreDependency(resideInAPackage(PROCESS), resideInAPackage(DOMAIN).and(simpleNameEndingWith("Event")))
```

The reason: the event type is the contract of the listener, and an event carries JDK types only. Nothing else in
`domain` is reachable from `process`. The exception is small and explicit on purpose. Adding another one means
changing the rule and this section in the same change, after the team agreed.

## 6. Testing per layer

| Layer | Test type | Tooling | Example name |
|---|---|---|---|
| `domain` | Pure unit test, no Spring | JUnit, AssertJ | `BookTest` |
| `domaininteraction` | Unit test with mocked ports | JUnit, Mockito, AssertJ | `CatalogueFlowTest` |
| `command`, `query`, `assistant` | Web slice test with mocked flows | `@WebMvcTest`, `@MockitoBean` | `BookCommandsTest` |
| `data` | JPA slice test on PostgreSQL | `@DataJpaTest`, Testcontainers PostgreSQL, `@AutoConfigureTestDatabase(replace = NONE)` | `BookDataServiceImplTest` |
| `acl` | HTTP contract test against a mocked server | `MockRestServiceServer` bound to the `RestClient.Builder` | `DistributorAppServiceImplTest` |
| `process` | Unit test of processor and delegate | JUnit, Mockito | `EventProcessorTest` |
| End to end | Full context, PostgreSQL in Docker, real HTTP | `@SpringBootTest` with a random port and `RestTestClient` | `CatalogueIntegrationTest` |
| Architecture | Layer, dependency and naming rules | ArchUnit `@ArchTest` | `ArchitectureTest` |

- `./mvnw verify` runs all of it, including the frontend tests and build, and should stay under 60 seconds.
- There is no in-memory database. Every test that needs a database runs on PostgreSQL through Testcontainers, so
  Docker must be running. A missing Docker daemon is an environment error, not a reason to skip tests.
- The test profile (`spring.profiles.active=test`, set by Maven) loads the schema migrations only, so tests start with
  an empty catalogue and never see the demo data.
- Every requirement has at least one test, and test names describe behaviour (`rejectsOrderWhenStockIsInsufficient`).
- No test calls the real distributor or a real language model.

## 7. Trade-off: the synchronous restock

When an order or a stocktake correction leaves a book with available stock at or below its reorder level, the domain
registers a `StockRunningLowEvent`, once per dip. The flow publishes it inside the transaction, and after the
commit `EventProcessor.onStockRunningLow` hands it to `RequestRestockDelegate`. The delegate calls
`InventoryFlow.handleStockRunningLow`, which asks the distributor for a restock and stores the pending restock the
distributor confirmed.

That call is synchronous. It runs on the request thread, after the commit, in a new transaction. This design is
accepted on purpose. Its costs, also written down in the Javadoc of `RequestRestockDelegate`:

- **The response waits for the distributor.** The request that dipped the stock only answers once the distributor
  answered or timed out: up to 2 seconds to connect plus 5 seconds to read per dipping book (the defaults of
  `distributor.connect-timeout` and `distributor.read-timeout`). When one order dips several books, the calls run one
  after the other. The order itself is already committed and stays so.
- **Two database connections per request.** During the call the request holds the committed outer connection, which
  Spring releases only after every after-commit listener finished, and the connection of the new transaction. With the
  default pool of 10 connections, five such requests at once during a slow distributor fill the pool.
- **Failures never reach the caller.** A failure on this path (a data error, an optimistic lock failure, a pool
  timeout, a failed commit) is logged at ERROR, the order stays committed, and the low-stock signal stays set without
  a pending restock, so nothing retries it. The recovery is the manual `request-restock` command. Two expected
  answers are handled in the flow: a distributor that is unavailable is a warning and resets the signal, so the next
  reservation tries again; a title the distributor refuses (not found, or not orderable) is a warning and keeps the
  signal.
- **A phantom restock is possible.** If an optimistic lock failure or a failed commit happens after the distributor
  confirmed the restock, the distributor holds a restock order that the bookstore never recorded. This happens, for
  example, when another order reserves the same book while the call is in progress, because the save of the pending
  restock then loses on the optimistic lock. A manual restock request afterwards orders a second time.
- **A pending restock never expires.** The bookstore keeps a pending restock until stock is received, and the
  distributor keeps its restock orders in memory only, so they are gone when it restarts. After a distributor restart,
  a pending restock therefore blocks every further restock of that book until someone receives stock manually (the
  `receive` command clears it, whatever the quantity). While a restock is pending, a manual `request-restock` answers
  202 without calling the distributor.

Asynchronous execution or an outbox would remove these costs. Both are out of scope for now.

## 8. Deliberately not used

- Lombok: records and plain Java are enough.
- MapStruct: mappers are written by hand, small and explicit.
- "Ports" and "adapters" as package names: the code uses the onion vocabulary above on purpose.
