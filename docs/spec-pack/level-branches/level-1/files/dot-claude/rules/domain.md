---
paths:
  - "bookstore/src/main/java/**/domain/**"
---
# Domain layer
- Aggregates: private constructor, an intention-revealing static factory for creation, `restore(...)` for rehydration.
- Change state through behaviour methods named after the business action. No setters.
- Check invariants inside the aggregate and throw an unchecked exception from the `domain` package.
- Register domain events in the aggregate; flows publish them after saving via `pullDomainEvents()`.
- Value objects are records that validate in the compact constructor.
- Domain tests are plain JUnit and AssertJ, without a Spring context.
