# Flows, Ports and DTOs

Flows (`CatalogueFlow`, `InventoryFlow`, `OrderFlow`) in `domaininteraction`:

- `@Service` and `@Transactional`. Public methods are the use cases and the transaction boundary.
- A flow loads DTOs through ports, maps them to the domain with a `*DomainMapper`, calls domain behaviour, maps back to a DTO and saves through the port.
- After saving, publish the pulled domain events with `ApplicationEventPublisher`, inside the same transaction.
- Parameters and results are JDK types or DTOs, because callers are adapters that may not touch `domain`.
- `@Service`, `@Transactional`, `ApplicationEventPublisher` and the SLF4J `Logger` are the only framework types allowed in `domaininteraction`.

Ports:

- An interface: `*DataService` for persistence, `*AppService` for external systems.
- Ports speak DTOs and JDK types, never domain types, JPA entities or external payloads.
- An exception that belongs to one port is nested in it (`DistributorAppService.DistributorUnavailableException`) and extends a use-case exception.

DTOs:

- A record named `*DTO` with JDK-typed components. A DTO that mirrors an aggregate has a convenience constructor from it (`new BookDTO(book)`); the way back is a `*DomainMapper`.

Use-case exceptions (unchecked, in `domaininteraction`):

| Exception | HTTP |
|---|---|
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `InvalidInputException` | 422 |
| `UnavailableException` | 503 |

- Each has a stable kebab-case `code` (`isbn-already-exists`), a short `title`, a `detail` message and optional `details`.
- Flows translate domain exceptions into these, so no domain exception reaches an adapter.
