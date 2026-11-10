---
name: new-use-case
description: Adds or changes a bookstore use case end to end through the Clean Hexagonal Onion layers (domain behaviour, flow, ports, command or query adapter, data adapter, migration, tests). Use when asked to add a feature, endpoint, business rule, command or query in the bookstore module.
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(./mvnw *)
---
# New use case

Work through these steps in order. Do not skip tests.

1. Understand: restate the use case in one sentence and list its business rules. Find the closest existing use
   case and read it end to end (command or query class, flow method, domain behaviour, tests). Mirror its shape.
2. Domain first: add behaviour to the aggregate (no setters), invariants as domain exceptions, a domain event if
   something else must react. Write the domain test first.
3. Flow: add a method to the right `*Flow`: load through the port, map to domain, call the behaviour, save,
   publish pulled events. Translate domain exceptions into `NotFoundException`, `ConflictException`,
   `InvalidInputException` or `UnavailableException`. Unit test it with mocked ports.
4. Ports and data: extend a `*DataService` only if needed; implement it in `data`; add a new Flyway migration if the
   schema changes.
5. Adapter: a command (`POST .../commands/{action}`, `*Payload` with validation, 202) or a query (`GET`, `*View`).
   The adapter's exception handler already maps the four use-case exceptions to ProblemDetail. Add a web slice test.
6. End to end: one integration test per acceptance scenario.
7. Verify: run `./mvnw verify` and paste the summary. If `ArchitectureTest` fails, fix the design, never the rule.

Layer cheat sheet and naming rules: [reference.md](reference.md)
