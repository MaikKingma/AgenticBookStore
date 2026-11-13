---
name: jpa-adapter-probes
description: Spring Data JPA adapters on Hibernate 7 and AFTER_COMMIT listeners - join dedup, tiebreaks, flush, locale
metadata:
  type: feedback
---
Rule: a "join instead of exists" probe is only killed by a test whose SQL row limit cuts into duplicated rows.
**Why:** Hibernate 7 removes duplicate root entities from `select b ... join b.authors a` in memory, so an exact-list "each book once" test at limit 100 stays green; yet `fetch first ? rows only` counts joined rows, so a limited search returns fewer books.
**How to apply:**
- Witness: one book with two matching child rows plus two more matching books, `limit 2`, assert the exact two titles; an unconditional `left join` multiplies rows for every search, so the free-text path dies on the same witness. Prove the mutated query ran: `@DataJpaTest` logs SQL (grep `join book_author`).
- Case-insensitive search via `toLowerCase(Locale.ROOT)`: the `toLowerCase()` probe dies only under `-DargLine="-Duser.language=tr -Duser.country=TR"` AND an input with upper-case `I` (PostgreSQL `lower` gives `i`, Java tr gives `ı`); the bookstore surefire config sets no argLine, so `-DargLine` works; print `Locale.getDefault()` in the witness. Dropping `lower(col)`: the search fragment must differ in case from the stored text there (`rEjUd` vs "Prejudice" is all lower case at that position).
- Order-by tiebreak (drop the second key): PostgreSQL's small in-memory sort keeps heap order on ties, so the probe dies only when the tie row with the HIGHER key was inserted first. Also probe by column substitution (`, s.bookId` -> `, s.onHand`, `, s.reserved`). With two tie rows any differing column matches the asserted order in one direction (so asc/desc substitution survives): ask for three tie rows whose other columns (status, nullable timestamps) are non-monotone, proven by a temporary witness on real code and each substitution. PostgreSQL sorts NULL first under `desc`, last under `asc`. After any tie-data rework re-run the first-key probes (reordering ids can make the whole list equal id order).
- With `join fetch`, joined child columns (`l.isbn desc`) are substitution columns too: vary them non-monotone in the tie group (ids 3/2/1 got isbn 317/201/188 by accident) and re-run on every tie-data rework; the runner must restore from the CURRENT round's `.orig`.
- An index matching the whole ORDER BY (`(email, placed_at desc, id desc)`) makes "tiebreak removed" and "swapped for a tie-constant column" survive (the index supplies order): prove with a removal-plus-index-drop probe (then red in heap order), report as inspection.
- `ddl-auto: validate` (active in `@DataJpaTest` slices) checks column names and types, not varchar length: `@Column(length = 64)` -> `255` survives every slice, a renamed column fails `Schema validation: missing column` (run it as the control). Only the migration plus an `information_schema` test and a 65-char insert pin the length.

Rule: probe the listener phase, the delegate propagation and the catch placement end to end; expect two IT-equivalent mutants that only an annotation pin in the unit test kills.
**Why:** the flow carries a class-level `@Transactional`, so some delegate mutants behave exactly like real code in every IT.
**How to apply:**
- One IT class plus the two unit classes with `clean` takes about 9 s per probe (generator-plus-runner from [[env-switch-mutant-runner]]). IT-equivalent (unit pin must exist): delegate `Propagation.NOT_SUPPORTED` and listener `fallbackExecution = true`.
- Plain `@EventListener` with the REQUIRES_NEW delegate hangs on the row lock the publishing transaction holds: run last with a short budget (exit 124 is the expected informational result), then check `pgrep -fl surefire` and `docker ps` are empty.
- `noRollbackFor = RuntimeException.class` on the delegate still rolls back (the flow's inner REQUIRED marks rollback-only): Spring logs `UnexpectedRollbackException` on the afterCompletion ERROR line; a test pinning the exception type on the line after the ERROR kills it, one only counting the ERROR line does not.
- `@TransactionalEventListener(condition = ...)` with a record payload: `#root.args[0].available()`; test both sides of the split. Attribute each new afterCompletion ERROR, dispatcherServlet ERROR and flow WARN line by the nearest preceding `Running nl...` line, then by a literal only one test throws.
Related: [[domain-mutation-probes]], [[spring-runtime-probes]]
