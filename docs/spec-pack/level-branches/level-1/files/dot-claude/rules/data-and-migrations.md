---
paths:
  - "bookstore/src/main/java/**/data/**"
  - "bookstore/src/main/resources/db/**"
---
# Data adapter and migrations
- `*DataServiceImpl` implements a `*DataService` port and speaks DTOs. JPA entities (`*JPA`) never leave `data`.
- Next migration number: the highest `V{n}__` file in `db/migration` plus one. File name `V{n}__{snake_case_description}.sql`.
- Hibernate only validates (`ddl-auto=validate`): every new column or table needs a migration, or startup fails.
- Seed data lives in `db/seed/R__seed_demo_data.sql` (repeatable, idempotent) and is not loaded in tests. Do not add
  test data there.
- Use `numeric(10,2)` plus a `varchar(3)` currency column for money.
