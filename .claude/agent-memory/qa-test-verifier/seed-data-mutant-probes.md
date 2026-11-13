---
name: seed-data-mutant-probes
description: Seed data mutants (JSON, repeatable SQL) - identity-asserting generator, count guards, growth diffs, ON CONFLICT
metadata:
  type: feedback
---
Rule: build seed-data mutants with one `python3 -I` generator that loads the `.orig`, mutates the parsed objects and re-serialises in the file's own layout, and first asserts that re-serialising the UNMUTATED data is byte-identical to the original.
**Why:** without the identity assert, a formatting difference (spacing, key order, trailing newline) makes every probe differ in many places, so a kill can come from layout instead of the intended value.
**How to apply:**
- One-object-per-line arrays: `"[\n" + ",\n".join(json.dumps(o) for o in items) + "\n]\n"` reproduces the file; non-ASCII probes need `ensure_ascii=False` and a UTF-8 write, or the probe silently tests nothing.
- Count guards: probe each count at minimum minus one (must fail) AND all counts at exactly the minimum in one must-pass probe (kills `>` for `>=`). Reduce counts by flipping extras to a neutral value that keeps every other invariant (IN_STOCK needs quantity above 0; do not empty a list or drop the last use of a labelled code).
- SQL seed with a repeatable Flyway migration (idempotent `INSERT ... ON CONFLICT DO NOTHING`): build an independent census as a `python3 -I` SQL parser (distributor files as oracle) plus ~50 one-rule negative-control copies (each must exit 1 with its own `FAIL <rule>`; anchor on a unique neighbour string, titles and ISBNs recur in order lines).
- "Developer edits survive a rerun" tests are only as strong as the columns they edit: probe `DO UPDATE SET <col> = excluded.<col>` once per table and per column the test does NOT touch. Delete-then-reinsert cannot tell DO NOTHING from DO UPDATE. A target-less `ON CONFLICT DO NOTHING` swallows every unique violation, so a "duplicate" mutant dies on a different assertion (an FK violation in `@BeforeAll`): read the failing message. A price with one decimal (`23.5`) is equivalent (`numeric(10,2)` pads).
- Seed GROWTH (rows appended to existing statements): prove "old rows unchanged" with a `python3 -I` diff script (old non-comment lines a byte-exact ordered subsequence of the new file; only the last row of each grown VALUES list may gain a comma; new-only lines directly before that statement's `ON CONFLICT`; untouched statements compared as one byte tail) plus ~12 one-change controls. A paired genre swap (old row to X, new row to Y) survives the census and only this script kills it. Derive per-statement new-row counts in the script and assert them (prose counts drift).
- `-Dtest=A+B` runs ZERO tests in this Surefire (BUILD SUCCESS, no `Running` line): use `A,B`. It writes no `timestamp` attribute in the XML: take class order from the log's `Running` lines. Hand-timed `date +%s` around the referee runs about 12 s above Maven's "Total time": quote both. `pg_dump` 18 prints a random `\restrict <token>` line per dump: filter `^\\(un)?restrict ` before hashing two dumps for a "restart leaves data unchanged" proof.
Related: [[env-switch-mutant-runner]], [[integration-test-probes]]
