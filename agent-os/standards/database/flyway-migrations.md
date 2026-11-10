# Flyway Migrations

The schema changes only through a new Flyway migration. Never edit an existing migration.

- Folder: `bookstore/src/main/resources/db/migration`. File name: `V{n}__{description}.sql`, plain SQL. The next number is the highest existing `V{n}__` plus one.
- Hibernate only validates (`spring.jpa.hibernate.ddl-auto=validate`): a new column or table without a migration fails at startup.
- Money columns: `numeric(10,2)` plus a `varchar(3)` currency column.
- Demo data is the repeatable migration `db/seed/R__seed_demo_data.sql`, loaded only by the default profile. Do not add test data to it.
- The Flyway locations in `application.yml` are a single comma separated value: `classpath:db/migration,classpath:db/seed`. The test profile replaces it with the schema location only.
