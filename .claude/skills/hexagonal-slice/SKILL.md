---
name: hexagonal-slice
description: Step-by-step recipe to build one feature end to end through the bookstore layers, test first per layer. The steps run domain behaviour, use-case exceptions, port, flow, data or acl adapter, Flyway migration, command, query or process adapter, then integration tests. Use when implementing a spec task that adds or changes a use case, endpoint, business rule, persistence or external integration in the bookstore module.
---

# Hexagonal slice

Build features as vertical slices, inside out. Every step starts with its test. The rules behind each step live in
the other factory skills:

- `clean-hexagonal-onion`: where each class goes, dependency rules, suffixes.
- `ddd-building-blocks`: how aggregates, value objects, events and exceptions behave.
- `layer-testing`: which test type belongs to each layer.

The spec you are implementing (`docs/spec-pack/specs/<NN>-*.md`) decides *what*. These skills decide *how*. If the
spec and the architecture standard contradict each other, stop and ask.

## Step 0: frame the slice

1. Restate the use case in one sentence.
2. List its business rules and every acceptance scenario of the spec it covers.
3. Write the scenario-to-test map before any code. Every scenario gets at least one test; name the test after the
   behaviour (`rejectsOrderWhenStockIsInsufficient`).
4. Find the closest existing slice and read it end to end (inbound adapter, flow method, domain behaviour, outbound adapter, tests).
   Mirror its shape. In spec 01 and 02 there is nothing to mirror yet; then follow these steps literally.

## Step 1: domain

- Test first (`<Aggregate>Test`, pure JUnit and AssertJ, no Spring).
- Add or change value objects, then the aggregate's factory or behaviour method, then invariants with domain
  exceptions, then the domain event if something else must react.

## Step 2: use-case boundary

- Decide which of the four use-case exceptions each domain exception becomes, with the `code` the spec gives.
- New port exceptions are nested in the port interface and extend one of the four.

## Step 3: port

- Extend an existing `*DataService` or `*AppService` only with what this use case needs.
- Signatures use DTOs and JDK types only.

## Step 4: flow

- Test first (`<Capability>FlowTest`, Mockito mocks for every port and the publisher).
- Add one public method to the right `*Flow`: load through the port, map to domain, call the behaviour, translate
  domain exceptions, save the DTO, publish the pulled events.
- Test the happy path, every translated exception, and that events are published only after a successful save.

## Step 5: outbound adapter

**`data`**, when persistence changes:

- `<Aggregate>JPA` entity, `<Aggregate>Repository` (Spring Data), `<Aggregate>JPAMapper`, `<Aggregate>DataServiceImpl`.
- Schema changes only through a new Flyway file `src/main/resources/db/migration/V<next>__<description>.sql`.
  Never edit an existing migration. JPA runs with `ddl-auto=validate`, so entity and migration must match.
- Test: JPA slice test on PostgreSQL (see `layer-testing`).

**`acl`**, when the distributor is involved:

- `<System>AppServiceImpl` with Spring `RestClient`; external payloads as private nested records; base URL from
  configuration (`distributor.base-url`).
- Every failure (connection, timeout, 4xx, 5xx, unexpected body) becomes a port exception.
- Test: contract test with `MockRestServiceServer`. Never call the real distributor from a test.

## Step 6: inbound adapter

**`command`**:

- `POST /{resource}/commands/{action}` for collection-level commands, `POST /{resource}/{id}/commands/{action}`
  for instance commands.
- Body is a `<Action>Payload` record with Jakarta Validation annotations.
- Success is `202 Accepted`; creating an aggregate returns `CommandResult(id)`.

**`query`**:

- `GET /{resource}` and `GET /{resource}/{id}`.
- Responses are `<Thing>View` records shaped for the reader, never DTOs.

**`process`**:

- An `EventProcessor` method with `@TransactionalEventListener(phase = AFTER_COMMIT)` hands the event to a
  `<Action>Delegate`.
- The delegate runs `@Transactional(propagation = REQUIRES_NEW)` and calls exactly one flow method.

**All of them**:

- The package's `*ExceptionHandler` maps the four use-case exceptions to RFC 9457 `ProblemDetail`.
- It sets `title`, `detail` and the properties `code` and `details`.
- Bean validation failures become 400 with `code = validation-failed`.
- Extend the handler only when the spec adds a new case.
- Test: web slice test (`@WebMvcTest`, flow mocked) for status codes, body and ProblemDetail fields.

## Step 7: end to end

- One `*IntegrationTest` per acceptance scenario that crosses layers: full context, PostgreSQL, real HTTP through
  MockMvc or `RestTestClient`.
- For event-driven scenarios, assert the effect of the `process` listener after the commit.

## Step 8: verify and hand back

1. Run `./mvnw clean verify` from the repo root and keep the summary lines as evidence.
2. If `ArchitectureTest` fails, fix the design (see the table in `clean-hexagonal-onion`), never the rule.
3. Tick the spec's definition of done line by line, with evidence per line.
4. Report the scenario-to-test map from step 0 with the final test names.

## Typical file set for a command slice

```
domain/                <Aggregate>.java, <ValueObject>.java, <Rule>Exception.java, <Something>Event.java
domaininteraction/     <Capability>Flow.java, <Aggregate>DataService.java, <Aggregate>DTO.java,
                       <Aggregate>DomainMapper.java
command/               <Aggregate>Commands.java, <Action>Payload.java
query/                 <Aggregate>Queries.java, <Thing>View.java
data/                  <Aggregate>JPA.java, <Aggregate>JPAMapper.java, <Aggregate>Repository.java,
                       <Aggregate>DataServiceImpl.java
resources/db/migration V<next>__<description>.sql
tests                  <Aggregate>Test, <Capability>FlowTest, <Aggregate>CommandsTest,
                       <Aggregate>DataServiceImplTest, <UseCase>IntegrationTest
```

Leave out what the use case does not need. Do not add speculative methods, ports or endpoints.
