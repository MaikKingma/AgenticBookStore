# Clean Hexagonal Onion

`docs/architecture.md` is the rulebook for the `bookstore` module. `ArchitectureTest` is the referee.

- Root package `nl.openvalue.bookstore`. Under it exactly eight packages: `domain`, `domaininteraction`, `command`, `query`, `process`, `assistant`, `data`, `acl`.
- Inbound adapters: `command`, `query`, `process`, `assistant`. Outbound adapters: `data`, `acl`.
- `domain`: aggregates, value objects, domain events, domain exceptions. Pure Java.
- `domaininteraction`: flows, ports, DTOs, DTO to domain mappers.
- A request enters an inbound adapter, which calls a flow. The flow loads data through a port, lets the domain decide, saves through a port.
- Only `BookstoreApplication` and configuration that belongs to no layer (`ClockConfig`, `StorefrontConfig`) sit in the root package. Configuration of one adapter stays in that adapter.
- The `distributor` module is a separate service, not part of the onion. The bookstore reaches it only through the `DistributorAppService` port.
- Never weaken or delete an `ArchitectureTest` rule to make a build pass.
