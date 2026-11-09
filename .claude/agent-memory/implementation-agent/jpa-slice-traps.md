---
name: jpa-slice-traps
description: Non-obvious behaviours of the bookstore JPA slice tests (Hibernate 7, Spring Data JPA 4.1, PostgreSQL) that decide whether a data-adapter mutation is killable
metadata:
  type: reference
---

- A `JdbcTemplate` read inside a `@DataJpaTest` shares the test transaction's connection but never triggers a
  Hibernate auto-flush. Reading `version` with JDBC right after `save` is what kills a `saveAndFlush` to `save`
  mutation; reading through the repository would flush and hide it.
- Clear the persistence context (`TestEntityManager.clear()`) before a round-trip `findById`, or the port returns
  the cached instance and proves nothing about the stored row. For a stale-update test, clear before the load and
  never between the JDBC version bump and the save.
- After a constraint violation at flush, the PostgreSQL transaction is aborted: the test must not touch the
  database again. Hibernate logs `HHH000247` and the constraint name at WARN for that deliberate failure; it is
  not a mapping or query warning (QA inventories it as such).
- In a Java text block, JPQL `escape '\\'` becomes `escape '\'`, which is correct for PostgreSQL with
  standard-conforming strings. Escape the backslash in Java before `%` and `_`.
- An unconditional clear-and-add on an `@ElementCollection` dirties the owner and bumps `@Version` even when the
  values are equal; a "save unchanged keeps version 0" test pins the only-when-different rule.
- Hibernate 7 removes duplicate root entities in memory, so a `join` over a collection without `distinct` passes
  an exact-list "each book once" test. Only a limit smaller than the joined row count exposes it (SQL
  `fetch first n rows` counts joined rows): pair a multi-author match with a small limit to pin `exists`.
- To pin a tiebreak column in `order by`, insert the tie row that must come last first: a small in-memory
  PostgreSQL sort keeps heap (insertion) order for ties, so a dropped tiebreak then fails. Use fixed UUIDs with a
  first hex digit 0 to 7 (PostgreSQL compares `uuid` unsigned, `UUID.compareTo` signed). Hibernate 7.4 accepts
  arithmetic (`s.a - s.b`) in a JPQL `order by`. A two-row tie cannot defeat a wrong tiebreak column in both
  directions (`x asc` or `x desc` always matches one of the two orders): use a tie group of three or more rows
  whose every other column runs in neither direction in the asserted order. PostgreSQL sorts NULL first under
  `desc` and last under `asc`, so a nullable column needs its NULL in the middle row, never at an end. "Every
  other column" includes joined collection columns: with `join fetch o.lines l` a mutant can order by `l.isbn` or
  `l.title`, so tabulate each order and line column in asserted order before reporting. Check every exact-list
  test that has a tie, not just the one about ordering.
- `JdbcTemplate.queryForMap` returns a `timestamptz` column as `java.sql.Timestamp`; `toInstant()` keeps the
  microseconds, so a literal `Instant.parse("...123456Z")` compares exactly. `uuid` comes back as `UUID`, `numeric`
  as `BigDecimal` with the column scale, so `Map.of(...)` rows compare with `containsExactly`.
- Migration shape as a slice test: `information_schema.columns` (`data_type`, `character_maximum_length` as Integer,
  `is_nullable` "YES", `column_default` null) plus `pg_constraint` with `contype <> 'n'` (PG 18 lists NOT NULL as
  `n`). `queryForMap` gives a `date` as `java.sql.Date` (`toLocalDate()`). An over-long varchar insert is a
  `DataIntegrityViolationException` naming `character varying(n)` and logs one HHH000247 (22001) for QA's inventory.
- A find-then-insert `save` that skips the update branch fails with `DuplicateKeyException`
  (`NonUniqueObjectException`) when the entity is already managed, not with an optimistic-lock error.

Pointer: verified Hibernate and Spring Data facts for spec 02 live in the spec folder's `references.md`.
Related: [[maven-build-traps]].
