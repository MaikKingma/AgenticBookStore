---
name: clean-hexagonal-onion
description: The Clean Hexagonal Onion structure of the bookstore module. Covers which package a class belongs in, which layer may depend on which, the framework types each layer may use and the class-name suffixes ArchUnit enforces. Use before creating, moving or renaming any Java class in the bookstore module, when deciding where code goes, or when ArchitectureTest fails.
---

# Clean Hexagonal Onion

The source of truth is `docs/spec-pack/product/architecture-standard.md`. If this skill and the standard disagree,
the standard wins. Stop and report the difference instead of picking one yourself.

Scope: the `bookstore` module, root package `nl.openvalue.bookstore`. The `distributor` module is a plain Spring
app (`api`, `mcp`, `catalog` packages, see spec 05) and these rules do not apply there.

## The layers

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

There are exactly these eight top-level packages under the root. A new top-level package needs a spec change: stop
and ask.

## Where does this class go?

Walk this list top to bottom and take the first match.

| If the class... | Package |
|---|---|
| holds a business rule, an invariant, a state change, a domain event or a domain exception | `domain` |
| orchestrates a use case, is a port interface, a DTO, a DTO-to-domain mapper or a use-case exception | `domaininteraction` |
| belongs to the assistant (spec 08): touches Spring AI, or is its endpoint, payload, view, tools, config, properties, rate limiter or exception handler | `assistant` |
| is an HTTP endpoint that changes state, or its request body | `command` |
| is an HTTP endpoint that reads, or its response shape | `query` |
| reacts to a domain event after commit | `process` |
| is a JPA entity, a Spring Data repository or implements a `*DataService` port | `data` |
| calls an external system (the distributor REST API) and implements an `*AppService` port | `acl` |

Nothing fits? Stop and ask. Do not create `util`, `common`, `shared`, `config`, `ports` or `adapters` packages.
The root package itself only holds `BookstoreApplication` and the cross-cutting configuration a spec explicitly puts
there (the `Clock` bean `ClockConfig`, created in spec 02; spec 12: `StorefrontConfig`), as section 1 of the standard says. Configuration of
one adapter stays in that adapter (`AssistantConfig` in `assistant`).

## Dependency rules

These are enforced by `ArchitectureTest`, never by convention alone.

1. `domain` may only be accessed by `domaininteraction`.
2. `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data`, `acl`.
3. No outer adapter (`command`, `query`, `process`, `assistant`, `data`, `acl`) may be accessed by any other layer.
4. `domain` depends on `java..` only: no Spring, no Jakarta Persistence, no Jackson, no Lombok, no Spring AI.
5. `jakarta.persistence..` and `org.springframework.data..` only in `data`.
6. `org.springframework.web.client..` (`RestClient`) only in `acl`.
7. `org.springframework.ai..` only in `assistant`.
8. `@RestController` classes only in `command`, `query` and `assistant`.
9. The one documented exception: `process` may reference domain event classes (`..domain..*Event`), because the event
   type is the listener's contract. It is a named `ignoreDependency` with a comment. Any new exception needs a spec
   change.

Framework types allowed in `domaininteraction`: `@Service`, `@Transactional`, `ApplicationEventPublisher` and the
SLF4J `Logger`. Nothing else.

## Naming suffixes

These apply to top-level classes only. Patterns are anchored at `nl.openvalue..`.

| Package | Class names must end with |
|---|---|
| `domain` | free, named after the domain (`Book`, `Isbn`, `StockRunningLowEvent`, `InsufficientStockException`) |
| `domaininteraction` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` |
| `command` | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` |
| `query` | `Queries`, `View`, `ExceptionHandler` |
| `process` | `EventProcessor`, `Delegate` |
| `assistant` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` |
| `data` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` |
| `acl` | `AppServiceImpl`, `Properties` (external payloads are private nested records) |

Nested JSON shapes are top-level records with a proper suffix (`OrderLinePayload`, `OrderLineView`).

## What crosses which boundary

| Boundary | Carries | Never carries |
|---|---|---|
| adapter to flow (method parameters) | JDK types, DTOs | domain types, payloads, JPA entities |
| flow to adapter (return values) | DTOs, JDK types | domain types |
| port (`*DataService`, `*AppService`) | DTOs, JDK types | domain types, JPA entities, external payloads |
| HTTP in | `*Payload` records | DTOs, domain types |
| HTTP out | `*View` records, `CommandResult`, `ProblemDetail` | DTOs, domain types |
| exceptions reaching adapters | the four use-case exceptions and port exceptions | domain exceptions |

## When ArchitectureTest fails

Fix the design, never the rule. Never weaken or remove a rule in `ArchitectureTest`. Add a rule only when the spec
you implement asks for it (spec 02: the money rule; spec 06: the acl rule), with its fixture in `ArchitectureSelfTest`.
The only `ignoreDependency` is the named `process` to `..domain..*Event` exception of the standard (spec 07); never
add another.

| Symptom | Fix |
|---|---|
| an adapter imports a domain class | the flow takes JDK types or DTOs and returns a DTO; the adapter maps to `*Payload` or `*View` |
| `data` or `acl` imports a domain class | the port speaks DTOs; mapping to domain happens in a `*DomainMapper` in `domaininteraction` |
| an adapter catches a domain exception | the flow translates it into a use-case exception (see the `ddd-building-blocks` skill) |
| a JPA or Jackson annotation in `domain` | move persistence shape to a `*JPA` class in `data`; JSON shape to a `*Payload` or `*View` |
| a class has the wrong suffix | rename it to the suffix of its layer, or move it to the layer its name says |
| one adapter calls another adapter | route through a flow; adapters only talk to `domaininteraction` |

## Not in this codebase

Lombok, MapStruct, setters on aggregates, `double` or `float` for money, and the package names `ports` and
`adapters`.

Related skills: `ddd-building-blocks` (how to model), `hexagonal-slice` (build a feature end to end),
`layer-testing` (which test where).
