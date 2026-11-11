# Layer cheat sheet and naming rules

Source of truth: `docs/architecture.md` (read it for the reasons). `ArchitectureTest` enforces the layers and the
suffixes: if it fails, fix the design, never the rule. Put main code in `bookstore/src/main/java/nl/openvalue/bookstore/`
and tests under `bookstore/src/test/java/nl/openvalue/bookstore/`, in the package of the class they test.

| Package | Allowed dependencies | Class name suffixes | Test to write (example) | Exemplary class |
|---|---|---|---|---|
| `domain` | `java..` only: no Spring, Jakarta Persistence, Jackson or Spring AI. Only `domaininteraction` may use it (`process` may use classes ending in `Event`) | no suffix rule: name after the business | Pure unit test, JUnit and AssertJ (`OrderTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/domain/Order.java` |
| `domaininteraction` | `domain` and JDK types; framework only `@Service`, `@Transactional`, `ApplicationEventPublisher`, SLF4J `Logger` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` | Unit test with mocked ports, JUnit and Mockito (`OrderFlowTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/OrderFlow.java` |
| `command` | `domaininteraction` only, never `domain`, never another adapter | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` | Web slice test, `@WebMvcTest` with `@MockitoBean` flows (`OrderCommandsTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/command/OrderCommands.java` |
| `query` | `domaininteraction` only, never `domain`, never another adapter | `Queries`, `View`, `ExceptionHandler` | Web slice test, `@WebMvcTest` with `@MockitoBean` flows (`OrderQueriesTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/query/OrderQueries.java` |
| `process` | `domaininteraction`, plus `domain` classes ending in `Event` (the one documented exception) | `EventProcessor`, `Delegate` | Unit test of processor and delegate, JUnit and Mockito (`EventProcessorTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/process/EventProcessor.java` |
| `assistant` (re-check against `docs/architecture.md` at cut) | `domaininteraction` only; the only package that may use `org.springframework.ai..` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` | Web slice test with a mocked chat client, unit test of the tools with mocked flows (`AssistantCommandsTest`, `BookstoreToolsTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/assistant/BookstoreTools.java` |
| `data` | `domaininteraction` (ports, DTOs); the only package that may use `jakarta.persistence..` and `org.springframework.data..` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` | JPA slice test, `@DataJpaTest` on PostgreSQL with Testcontainers (`OrderDataServiceImplTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/data/OrderDataServiceImpl.java` |
| `acl` | `domaininteraction`; the only package that may use `org.springframework.web.client..` (`RestClient`) | `AppServiceImpl`, `Properties`; payload records are private nested records | HTTP contract test with `MockRestServiceServer` (`DistributorAppServiceImplTest`) | `bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java` |

The place-order chain, one existing class per step (copy its shape for a new use case):

- Command: `bookstore/src/main/java/nl/openvalue/bookstore/command/OrderCommands.java`, body
  `bookstore/src/main/java/nl/openvalue/bookstore/command/PlaceOrderPayload.java`
- Flow: `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/OrderFlow.java` (`placeOrder`)
- Aggregate: `bookstore/src/main/java/nl/openvalue/bookstore/domain/Order.java` (`Order.place`)
- Data adapter: `bookstore/src/main/java/nl/openvalue/bookstore/data/OrderDataServiceImpl.java`
- End to end test: `bookstore/src/test/java/nl/openvalue/bookstore/OrderIntegrationTest.java` (full context, PostgreSQL
  in Docker, real HTTP; name a new end to end test `*IntegrationTest`)
- Layer rules: `bookstore/src/test/java/nl/openvalue/bookstore/ArchitectureTest.java`

Rules that apply to every use case:

- Give an aggregate a private constructor, a static factory (`Order.place`), behaviour methods named after the business
  and no setters. Make an invariant throw an unchecked domain exception. Let an aggregate that raises events (`Order`,
  `StockItem`) release them through `pullDomainEvents()`.
- Write a use case that changes state in a flow as: load DTOs through a port, map to the domain with a `*DomainMapper`,
  call the behaviour, save, publish the events pulled (when the aggregate raises any). A read method returns the port's
  DTO. Translate domain exceptions in a flow into `NotFoundException` (404), `ConflictException` (409),
  `InvalidInputException` (422) or `UnavailableException` (503), each with a kebab-case `code`.
- Let a port speak DTOs and JDK types, never domain types or JPA entities. Write a command as
  `POST /{resource}/{id}/commands/{action}` (or without `{id}`) that answers 202, with a `*Payload` body when it takes
  input; the one existing exception is the assistant chat endpoint `POST /assistant/chat`, which answers 200 with a
  `*View`. Write a query as a `GET` that returns a `*View`, never a DTO.
- Hold money in an aggregate as `Money`, never as `double` or `float`. Carry a price in a DTO and in the data layer as a
  `BigDecimal` plus a currency, and in a payload or view as a decimal string.
- Change the schema only through a new Flyway migration `V{n}__{description}.sql` in
  `bookstore/src/main/resources/db/migration`. Never edit an existing one.
- Give every requirement at least one test and name tests after behaviour. Use no in-memory database: Docker must run.
