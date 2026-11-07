---
name: jpa-postgres-persistence-traps
description: Hibernate 7 / Spring Data / PostgreSQL 18 persistence traps that change data-adapter and test design (Instant rounding, Limit with join fetch, element collection version bumps, deadlocks from unsorted multi-row updates) plus the statement-counting probe technique
metadata:
  type: reference
---

Probed on Boot 4.1.2 / Hibernate 7.4.13 / postgres:18.6 in a scratchpad project with the repo's real migrations
copied in (technique: research-agent memory `boot4-verification-techniques.md`).

- **Instant precision**: `timestamptz` keeps microseconds and ROUNDS the rest (`.999999999` reloads as the next
  second). macOS JDK clock gives micros, Linux gives nanos, so equality-after-reload tests fail only on CI.
  Rule: truncate to `ChronoUnit.MICROS` in the domain; bracket assertions with `before.truncatedTo(MICROS)`.
- **Spring Data `Limit` + `join fetch`**: silently ignored (no SQL limit, no warning, all rows returned). Without the
  fetch the same `Limit` emits `fetch first ? rows only`. Hibernate 7 dedups join-fetched parents itself.
- **`@ElementCollection` + `@OrderColumn`**: in-place `clear()+addAll()` with identical values bumps the owner
  `@Version` and loads the collection. Update mappers must not touch immutable collections.
- **Multi-row optimistic updates**: two transactions updating the same rows in opposite order deadlock (about 1 s,
  `CannotAcquireLockException`). Sort the writes by key so the loser fails fast with
  `ObjectOptimisticLockingFailureException`.
- **Probe technique**: `spring.jpa.properties.hibernate.generate_statistics=true`, then
  `emf.unwrap(SessionFactory.class).getStatistics()` (`clear()`, then read `getPrepareStatementCount`,
  `getEntityInsertCount`, `getEntityUpdateCount`, `getCollectionUpdateCount`). Statement counts prove N+1, the
  absence of writes before a failure, and "no write to the child table".

Related: [[spring-tx-event-traps]]
