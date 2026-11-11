---
name: domain-mutation-probes
description: Mutation-probe techniques for bookstore domain value objects and aggregates (checksum falsifiers, null guards, shared constructors), flow-layer probes (port order, translators, mapper hand-offs), JSON view mappers, wrapped-cause detail pins and test-only rework rounds
metadata:
  type: feedback
---
Rule: a "rejects X" test only falsifies a clause if the input is rejected by THAT clause alone. Check what else would reject it under the mutation.
**Why:** an ISBN test with a non-ASCII digit at the check-digit position kills the Unicode-aware variant but survives the natural removal `allMatch(Character::isDigit)`: the `charAt - '0'` checksum arithmetic rejects it by accident.
**How to apply:**
- Checksum-guarded format: build the witness so the naive checksum still matches: replace digit d at weight w with char c where `w * ((c - '0') - d) % 10 == 0` (U+0661 for the `5` at index 10 of 9780141439518).
- Two factories sharing one private validating constructor: apply "factory B skips check K" as a constructor `boolean` flag; only B's test should fail.
- Null guards (`requireNonNull`) are validation branches; only guards whose removal is not masked by a later NPE (`List.copyOf(null)`, `Year.now(null)`) need a pin. Name the unmasked ones by file:line.
- Rework rounds: keep a sha256 list of the fileScope from round 1; if only test files changed, cite round 1 logs and re-probe the Required Actions plus every probe whose only killer was an edited test. A new assertion after an existing one needs a mutant that passes the old line and breaks only the new one (failure at the new line number). `cmp` production against the earlier `.orig` copies first; see [[scaffold-verification-techniques]].
- Pass-through `Clock` parameters: probe "use `Clock.systemUTC()` instead". Witness: fixed clock in the past plus a year only the real clock accepts.
- Oracle trap: flow tests computing the expected message via production code die inside the oracle under mutation. Prove the flow assertion bites with a temporary oracle-free witness (exact type, code, `never().save`), then `rm` it.
- Stored parameter hard-coded on one read path (reorder level in `lowStock()`): probe every other branch reading it (the reset in `receive`); witness above and below the default.
- Exception payload witnesses: duplicate-X data A, B, A cannot tell "the duplicate" from "the first element"; probe `get(0)` and `get(size - 1)`, witness B, A, C, A, B.
- State cleared next to an existing `if` (`receive` clears a pending restock beside `if (available() > level) resetSignal()`): probe "move the clear into the if" (and else). Give the implementer a witness ending strictly BELOW the level, not at it (at the level leaves `>=` alive, costing a second round). Run as a temporary witness class (snapshot, real code, mutant, `rm`).
Flow-layer probes (mocked port and publisher):
- "Returns the port result in order": a stub in neither id nor available order can still be ascending in another column, and a constant column makes a stable sort a no-op. Probe a sort by each DTO column; prove survivors with a stub monotonic in no column. Lists the flow builds itself (error details, events) with under three rows survive about half the column sorts: prove with a temporary three-row witness.
- Probe lists often name a mutant for one command only: run the counterpart in the other command too.
- A new line in a shared verify helper (`verifyNoInteractions(newPort)`): add a stray harmless port call as the first statement under test; every helper caller must fail at the helper's line, rejection tests without the check survive.
- "No port call while X" probes die on unstubbed Mockito mocks only by NPE, never reaching `verifyNoInteractions`: add stray-call mutants on the guarded path (bare `findById(id)`, `save(stored)`, `publishEvent(item)`), each must fail at its own verify line.
- Probe the details `message` separately from the field.
Mapper hand-offs into validating factories (`Money.of(amount, code)`, `OrderStatus.valueOf`, `new Quantity`):
- "Equivalent because the domain rejects it anyway" is wrong when the mutant drops the argument. Probe "drop the argument" (hard-coded `"EUR"`, first-line currency for all lines) and the lenient parse ("SHIPPED else PLACED"); only a rejected value (USD) asserted as the domain exception kills them.
- Write the per-field pass-through inventory FIRST: plain fields need two accepted values, hand-offs a rejected one. Fixtures reusing one e-mail, id or instant let "replace by the test's literal" survive.
- Aggregate built in a translator from an external DTO: move EACH value-object construction out of the translator separately; "whole call outside" is killed by any one rejected argument.
- "Override replaces a default": when every override exceeds the default, "keep the higher" survives; needs an override below the default and a zero override ("zero counts as absent"). Run in round 0.
- Before round 0, build an input x relation matrix (null, above/below/equal to the replaced value, zero, bound just outside, element position, duplicates); each empty cell is a mutant family (clamp `min(500.00)`, `limit(MAX)`, `distinct()`, `dropWhile(blank)`).
View mappers and wrapped-cause pins:
- A view record copying an optional component: probe a guard on every DERIVED flag (`pending == null || !stock.lowStock() ? null : ...`), in both polarities and at the boundary (`<`, `<=`, `>=`, `!=`); witnesses strictly above, at and below the limit.
- Null-key forms: `jsonPath("$", hasKey(k))` plus `value(nullValue())`, never `doesNotExist()`. `@JsonInclude` NON_NULL, NON_EMPTY, NON_ABSENT, NON_DEFAULT all die on `hasKey` (run the class-level one once under the full referee). A list view needs first, last, next, previous element and null-filter mutants; "pending only on the middle item" kills all five.
- A wrapped-cause message is pinned by `isEqualTo(prefix + domainMessage)` plus a cause-message assertion, not `startsWith` or `isExactlyInstanceOf`. Probe the suffix (`getMessage`, `toString`, class name, appended text, upper-cased, substring, dropped) and the cause (dropped, `getCause()`, wrapped, same type other message).
Batch runner for a use-case slice: keep a pristine sandbox and a probe sandbox; a `python3 -I` mutator replaces OLD exactly once (exit 1 on 0 or 2 hits); run the same fixed class list with `clean test -pl <module>` per probe (a changed total means a compile or selection problem); restore by byte copy and `diff -rq` the `src` trees. `if (false)` replacements keep the edit compilable. Add per mirror site (DTO ctor, domain mapper, JPA mapper both ways, view) one "drops the new field" probe, per shared helper one "other caller's lambda swapped", and the enum pin (add a fourth value); an allow-list rewrite of a two-value rule is equivalent on the current enum.
Related: [[env-switch-mutant-runner]], [[shell-traps]], [[spring-runtime-probes]], [[scaffold-verification-techniques]]
