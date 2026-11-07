---
name: jpa-adapter-probes
description: Mutation-probe traps for Spring Data JPA adapters on Hibernate 7 (join dedup hides row-limit bugs, flush observability, locale casing, order-by tiebreaks, arithmetic JPQL, proving the mutated SQL ran)
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
- Order-by tiebreak probes (drop the second key): PostgreSQL's small in-memory sort keeps heap (insertion) order on ties, so the probe only dies when the tie row with the HIGHER key was inserted first. Check the test's insertion order before predicting a survivor.
- Also probe the tiebreak position by column substitution (`, s.bookId` -> `, s.onHand`, `, s.reserved`), not only by removal: a tie pair ascending in id, on hand and reserved together lets a wrong tiebreak column pass. Fixing one exact-list test's tie by reordering ids can make its whole expected list equal the id order, so a first-key `order by s.bookId` then dies only in the dedicated order test: re-run the first-key probes after any tie-data rework.
- Arithmetic JPQL (`a - b <= c`, `order by a - b`): probe each operand with its default literal (`s.reserved` -> `0`, `s.reorderLevel` -> `3`) in the `where` AND in the `order by` separately; grep the probe log for the generated SQL (`(x.on_hand-0)`) to prove the mutant ran.
- Mapper helpers shared by insert and update (`toNewEntity` calls `copyInto`): to probe one path only, append the mutated setter calls after the shared call in that one method instead of editing the helper.
- A "save unchanged keeps version 0" test (save, clear, `save(findBy(...))`) also kills read-path swaps in `toDTO`, because the swapped DTO dirties the entity on the second save.

Related: [[domain-mutation-probes]], [[spring-runtime-probes]]
