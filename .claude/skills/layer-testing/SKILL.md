---
name: layer-testing
description: Which test to write for each bookstore layer and how. Covers domain unit tests, flow tests with mocked ports, web slice tests, JPA slice tests on PostgreSQL via Testcontainers, acl contract tests with MockRestServiceServer, process tests, end-to-end integration tests and ArchUnit. Use when writing or fixing tests, mapping spec scenarios to tests, or when ./mvnw clean verify fails.
---

# Layer testing

The source of truth is `docs/spec-pack/product/architecture-standard.md`, section 5, as amended for PostgreSQL
(decision D2 in `docs/spec-pack/orchestrator/decisions.md`). If they disagree with this skill, they win: stop and
report the difference.

## Test type per layer

| Layer | Test type | Tooling | Example name |
|---|---|---|---|
| `domain` | Pure unit tests, no Spring | JUnit, AssertJ | `StockItemTest` |
| `domaininteraction` | Unit tests with mocked ports | JUnit, Mockito, AssertJ | `InventoryFlowTest` |
| `command`, `query`, `assistant` | Web slice tests with mocked flows | `@WebMvcTest`, `@MockitoBean`, MockMvc | `InventoryCommandsTest` |
| `data` | JPA slice tests on PostgreSQL | `@DataJpaTest` + Testcontainers PostgreSQL | `StockItemDataServiceImplTest` |
| `acl` | HTTP contract tests against a mocked server | `MockRestServiceServer` bound to `RestClient.Builder` | `DistributorAppServiceImplTest` |
| `process` | Unit tests for processor and delegate | JUnit, Mockito | `EventProcessorTest` |
| End to end | Full context, PostgreSQL, real HTTP | `@SpringBootTest` + MockMvc or `RestTestClient` | `ReceiveStockIntegrationTest` |
| Architecture | Layer, dependency and naming rules | ArchUnit `@ArchTest` | `ArchitectureTest` |

## Rules for every test

- Every acceptance scenario of the spec maps to at least one test. Keep the map; it is evidence for the definition
  of done.
- Method names describe behaviour: `rejectsReservationWhenStockIsInsufficient`, not `testReserve2`.
- No test calls the real distributor or a real LLM. The assistant uses its offline chat model in tests (spec 08).
- Time is controlled through `Clock` (a fixed clock in tests), never `Thread.sleep` or `Instant.now()` assertions.
- Assert outcomes and messages that the spec defines (status, `code`, `details`), not incidental strings.
- One reason to fail per test. Prefer several small tests over one long scenario.

## Per layer, what to cover

**`domain`**:

- every factory, behaviour method and invariant
- the exception for every broken rule, including boundary values (0, 1, max, max + 1)
- events registered exactly when the spec says
- `pullDomainEvents()` clears the list

**`domaininteraction`**:

- happy path calls on the ports, in order
- every domain exception translated to the right use-case exception and `code`
- port exceptions passed through
- events published only after save

**Web slices**:

- status codes: 202 for commands, 200 for queries, `CommandResult` on create
- the `*View` JSON shape
- ProblemDetail `title`, `detail`, `code` and `details` for each use-case exception
- 400 with `code = validation-failed` and `{field, message}` details for bad payloads

**`data`**:

- save and load round trip
- every finder the port defines
- uniqueness and optimistic locking where the spec asks for them
- the mapping of every column

**`acl`**:

- request path, method and body sent to the distributor
- mapping of a good response
- every failure becomes a port exception: connection refused, timeout, 4xx, 5xx, malformed body

**`process`**:

- the processor hands the event to the delegate
- the delegate calls exactly one flow method with the event's data

**End to end**:

- one test per acceptance scenario that crosses layers
- for events, the effect after commit (for example a restock request recorded after stock runs low)

## PostgreSQL with Testcontainers

- There is no H2 and no embedded database. Tests need Docker running.
- Define the container once:
  - a shared test configuration with a `@Bean @ServiceConnection` for the Testcontainers PostgreSQL container
  - the PostgreSQL image version pinned in `product/tech-stack.md`
  - imported by `@DataJpaTest` and `@SpringBootTest` tests
  
  Spring's test context cache then reuses one container across test classes. Do not start a container per test
  class.
- If Spring tries to replace the datasource with an embedded one, add `@AutoConfigureTestDatabase(replace = NONE)`.
- Flyway runs every migration in tests, and `ddl-auto=validate` then catches drift between entities and migrations.
  A validate failure means fix the migration or the entity, never switch validation off.
- Keep tests independent of each other's data. Each test creates what it needs with unique keys, or cleans up in
  `@AfterEach`. Do not rely on test order.

## Speed budget

- The standard says `./mvnw clean verify` stays under 60 seconds, and the Lab 3 commit hook times out at 180
  seconds.
- Measure the time after each spec and record it.
- When it exceeds 60 seconds, report it as an open question instead of trimming tests.

## When verify fails

1. Read the first failure, not the last.
2. A failing test is information. Fix the production code, unless the test contradicts the spec. Then stop and ask;
   do not change the test on your own.
3. Never `@Disabled`, never delete a test, never weaken an assertion, never edit `ArchitectureTest`.
4. Docker not running is an environment problem: report it, do not work around it.
5. Run `./mvnw clean verify` again from the repo root before you claim green, and keep the summary lines as evidence.
