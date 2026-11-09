---
title: Architecture
sidebar_position: 2
---

The bookstore is built as a Clean Hexagonal Onion with a dash of DDD. This page is the short version for participants: the layers, which layer may use which, how classes are named, and the test that checks it all.

The full team version is the file `docs/architecture.md` in the workshop repository, see [docs/architecture.md on main](https://github.com/MaikKingma/AgenticBookStore/blob/main/docs/architecture.md). That file is part of the workshop baseline that is published on `main` before the workshop.

## The onion

The root package of the bookstore module is `nl.openvalue.bookstore`. Inbound adapters call the flows, the flows work on the domain, outbound adapters implement the ports of the flows.

```text
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

## Packages

| Package | Role | Typical classes |
|---|---|---|
| `domain` | Aggregates, value objects, domain events, domain exceptions. Pure Java. | `Book`, `Isbn`, `Money`, `Order`, `OrderLine`, `StockRunningLowEvent`, `InsufficientStockException` |
| `domaininteraction` | Use-case orchestration ("flows"), the ports to the outside world, DTOs that cross the boundary, mappers DTO to domain. | `CatalogueFlow`, `BookDataService` (port), `DistributorAppService` (port), `BookDTO`, `BookDomainMapper` |
| `command` | Inbound REST adapter for state-changing requests. | `BookCommands`, `AddBookPayload` |
| `query` | Inbound REST adapter for read requests. | `BookQueries`, `BookView` |
| `process` | Inbound event-driven adapter: reacts to domain events after commit. | `EventProcessor`, `RequestRestockDelegate` |
| `assistant` | Inbound conversational adapter (Spring AI). The only place where Spring AI types appear. | `AssistantCommands`, `BookstoreTools`, `AssistantConfig`, `OfflineChatModel` |
| `data` | Outbound persistence adapter. Implements `*DataService` ports. | `BookDataServiceImpl`, `BookJPA`, `BookJPAMapper`, `BookRepository` |
| `acl` | Outbound anti-corruption layer to external systems. Implements `*AppService` ports. | `DistributorAppServiceImpl` (+ private payload records) |

## Dependency rules

ArchUnit enforces these nine rules, never convention alone.

1. `domain` may only be accessed by `domaininteraction`.
2. `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data`, `acl`.
3. No outer adapter (`command`, `query`, `process`, `assistant`, `data`, `acl`) may be accessed by any other layer.
4. `domain` depends on `java..` only. No Spring, no Jakarta Persistence, no Jackson, no Lombok, no Spring AI.
5. `jakarta.persistence..` and `org.springframework.data..` are only allowed in `data`.
6. `org.springframework.web.client..` (`RestClient`) is only allowed in `acl`.
7. `org.springframework.ai..` is only allowed in `assistant`.
8. `@RestController` classes only live in `command`, `query` and `assistant`.
9. Documented exception, kept small and explicit: `process` may reference domain event classes (`..domain..*Event`), because the event type is the contract of the listener. The ArchUnit test lists this as a named `ignoreDependency` with a comment explaining why. Adding a new exception requires a spec change.

## Naming

The naming rules apply to top-level classes only. Nested, anonymous and synthetic classes and `package-info` are excluded. Prefer top-level records for nested JSON shapes, so they carry a proper suffix.

| Package | Class names must end with |
|---|---|
| `command` | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` |
| `query` | `Queries`, `View`, `ExceptionHandler` |
| `process` | `EventProcessor`, `Delegate` |
| `assistant` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` |
| `data` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` |
| `acl` | `AppServiceImpl`, `Properties` (payload records are private nested records) |
| `domaininteraction` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` |

## The referee

The layer, dependency and naming rules are checked by ArchUnit in `ArchitectureTest`. `./mvnw verify` runs it together with all other tests, so a violation turns the build red. When it fails, fix the design, never the rule: do not weaken or delete an `ArchitectureTest` rule to make a build pass.
