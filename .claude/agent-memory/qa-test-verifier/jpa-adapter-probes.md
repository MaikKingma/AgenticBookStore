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
- Tie groups: with two tie rows, any column that differs between them matches the asserted order in one direction, so `asc` or `desc` substitution of that column always survives. Ask for three tie rows whose other columns (status, nullable timestamps) are non-monotone in the asserted order. Prove the data with a temporary witness class run on real code and on each substitution mutant. PostgreSQL sorts NULL first under `desc` and last under `asc`, so "the row that must come last has the higher timestamp" is false when the other row's value is NULL.
- With a `join fetch` of child rows, joined child columns (`join fetch o.lines l ... , l.isbn desc`) are substitution columns too. Check every varied child column inside the tie group, not only the parent columns. A witness with constant child data has no such column, but an implementer who "varies the line values" can line them up monotone by accident (ids 3/2/1 got isbn 317/201/188). Re-run the joined-column substitutions on every tie-data rework. The probe runner must restore from the CURRENT round's `.orig` copies: an earlier round's runner restores the old test file.
- An index matching the whole ORDER BY (`(email, placed_at desc, id desc)`) makes "tiebreak removed" and "tiebreak swapped for a column that is constant in the tie" survive: the index scan supplies the order. Prove the cause with a removal-plus-index-drop probe (it then fails in heap order) and report it as inspection.
- Arithmetic JPQL (`a - b <= c`, `order by a - b`): probe each operand with its default literal (`s.reserved` -> `0`, `s.reorderLevel` -> `3`) in the `where` AND in the `order by` separately; grep the probe log for the generated SQL (`(x.on_hand-0)`) to prove the mutant ran.
- Mapper helpers shared by insert and update (`toNewEntity` calls `copyInto`): to probe one path only, append the mutated setter calls after the shared call in that one method instead of editing the helper.
- A "save unchanged keeps version 0" test (save, clear, `save(findBy(...))`) also kills read-path swaps in `toDTO`, because the swapped DTO dirties the entity on the second save.
- `ddl-auto: validate` (active in the bookstore `@DataJpaTest` slices too) checks column names and types but not varchar length: `@Column(length = 64)` -> `255` survives every slice, while a renamed column fails with `Schema validation: missing column`. Only the migration plus an `information_schema` test and a 65-non-space-char insert pin the length. Run the rename as a control to prove validate is on.
- Update-only per-field loss when insert and update share `copyInto`: drop one setter from `copyInto` and re-add it in `toNewEntity` (insert still writes it, update keeps the old value). Needs an A -> B update test whose every field differs.
- Hand-written corrupt rows (residual claims): a temporary `@DataJpaTest` class in the scratchpad, installed, run with `clean`, then `rm`'d by one script that ends with `ls` showing it gone, turns "throws NPE on read" into evidence.

Related: [[domain-mutation-probes]], [[spring-runtime-probes]]
