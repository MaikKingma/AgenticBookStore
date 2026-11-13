---
name: domain-mutation-probes
description: Domain value objects, aggregates, flow ports and translators, view mappers, wrapped-cause pins
metadata:
  type: feedback
---
Rule: a "rejects X" test only falsifies a clause if the input is rejected by THAT clause alone; check what else would reject it under the mutation.
**Why:** an ISBN test with a non-ASCII digit at the check-digit position kills the Unicode-aware variant but survives the natural removal `allMatch(Character::isDigit)`: the `charAt - '0'` checksum arithmetic rejects it by accident.
**How to apply:**
- Checksum-guarded format: build the witness so the naive checksum still matches: replace digit d at weight w with char c where `w * ((c - '0') - d) % 10 == 0` (U+0661 for the `5` at index 10 of 9780141439518).
- Two factories sharing one private validating constructor: "factory B skips check K" as a constructor `boolean` flag; only B's test should fail.
- Rework rounds: keep a sha256 list of the fileScope from round 1; if only test files changed, cite round 1 logs and re-probe the Required Actions plus every probe whose only killer was an edited test. A new assertion after an existing one needs a mutant that passes the old line and breaks only the new one. `cmp` production against the earlier `.orig` copies first.
- Stored parameter hard-coded on one read path (reorder level in `lowStock()`): probe every other branch reading it (the reset in `receive`); witness above and below the default. State cleared next to an existing `if` (clear beside `if (available() > level) resetSignal()`): probe "move the clear into the if" (and else); the witness must end strictly BELOW the level, not at it (at it leaves `>=` alive).

Flow-layer probes (mocked port and publisher):
- "Returns the port result in order": a stub in neither id nor available order can still be ascending in another column, and a constant column makes a stable sort a no-op. Probe a sort by each DTO column; prove survivors with a stub monotonic in no column. Lists the flow builds itself (error details, events) with under three rows survive about half the column sorts: prove with a temporary three-row witness. Run the counterpart mutant in the other command too.
- A new line in a shared verify helper (`verifyNoInteractions(newPort)`): add a stray harmless port call as the first statement under test; every helper caller must fail at the helper's line. "No port call while X" probes die on unstubbed Mockito mocks only by NPE: add stray-call mutants (bare `findById(id)`, `save(stored)`, `publishEvent(item)`), each failing at its own verify line. Probe the details `message` separately from the field.
- Mapper hand-offs into validating factories (`Money.of(amount, code)`, `OrderStatus.valueOf`, `new Quantity`): "equivalent because the domain rejects it anyway" is wrong when the mutant drops the argument. Probe "drop the argument" (hard-coded `"EUR"`, first-line currency for all lines) and lenient parse ("SHIPPED else PLACED"); only a rejected value (USD) asserted as the domain exception kills them. Write the per-field pass-through inventory FIRST: plain fields need two accepted values, hand-offs a rejected one; fixtures reusing one e-mail, id or instant let "replace by the test's literal" survive. Move EACH value-object construction out of a translator separately.
- "Override replaces a default": when every override exceeds the default, "keep the higher" survives; needs an override below the default and a zero override. Before round 0 build an input x relation matrix (null, above/below/equal, zero, bound just outside, element position, duplicates); each empty cell is a mutant family (clamp `min(500.00)`, `limit(MAX)`, `distinct()`, `dropWhile(blank)`).

View mappers and wrapped-cause pins:
- A view record copying an optional component: probe a guard on every DERIVED flag (`pending == null || !stock.lowStock() ? null : ...`) in both polarities and at the boundary (`<`, `<=`, `>=`, `!=`); witnesses strictly above, at and below the limit.
- Null-key forms: `jsonPath("$", hasKey(k))` plus `value(nullValue())`, never `doesNotExist()`; `@JsonInclude` NON_NULL/NON_EMPTY/NON_ABSENT/NON_DEFAULT all die on `hasKey` (run the class-level one once under the full referee). A list view needs first, last, next, previous and null-filter mutants: "pending only on the middle item" kills all five.
- A wrapped-cause message is pinned by `isEqualTo(prefix + domainMessage)` plus a cause-message assertion, not `startsWith` or `isExactlyInstanceOf`. Probe the suffix (`getMessage`, `toString`, class name, appended text, upper-cased, substring, dropped) and the cause (dropped, `getCause()`, wrapped, same type other message).
Related: [[env-switch-mutant-runner]], [[spring-runtime-probes]], [[scaffold-verification-techniques]]
