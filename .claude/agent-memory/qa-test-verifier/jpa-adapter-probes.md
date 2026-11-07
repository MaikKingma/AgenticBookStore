---
name: jpa-adapter-probes
description: Mutation-probe traps for Spring Data JPA adapters on Hibernate 7 (join dedup hides row-limit bugs, flush observability, locale casing, proving the mutated SQL ran)
metadata:
  type: feedback
---

Rule: a "join instead of exists" probe is only killed by a test whose SQL row limit cuts into duplicated rows.

**Why:** Hibernate 7 removes duplicate root entities from `select b ... join b.authors a` results in memory. An exact-list "each book once" test at limit 100 stays green. The bug is real all the same: `fetch first ? rows only` counts joined rows, so a limited search returns fewer books.
**How to apply:**
- Witness: one book with two matching child rows plus two more matching books, `limit 2`, assert the exact two titles. An unconditional `left join` multiplies rows for every search, so the free-text path dies on the same witness.
- Prove that the mutated query really ran: `@DataJpaTest` logs SQL (`showSql` defaults to true), so grep the probe log for the join (`join book_author`).
- `JdbcTemplate` reads never trigger a Hibernate auto-flush. That makes `save` vs `saveAndFlush` observable through a JDBC version or column read. `em.find` by id does not flush either, so an insert `save` without a flush hides a unique violation.
- Case-insensitive search built with `toLowerCase(Locale.ROOT)`: the `toLowerCase()` probe only dies under `"-DargLine=-Duser.language=tr -Duser.country=TR"` AND with an input containing upper-case `I` (PostgreSQL `lower` gives `i`, Java tr gives dotless `ı`). The bookstore Surefire config sets no argLine, so `-DargLine` takes effect. Print `Locale.getDefault()` in the witness to prove it.
- Dropping `lower(col)` on a column: check that the test's search fragment differs in case from the stored text at that position (`rEjUd` vs "Prejudice" is all lower case there, so it is no witness).
- Moving a migration out of `db/migration` with `mv` keeps mtime and sha, so the snapshot cmp stays valid. Pair it with a `clean` run ([[spring-runtime-probes]]).

Related: [[domain-mutation-probes]], [[spring-runtime-probes]]
