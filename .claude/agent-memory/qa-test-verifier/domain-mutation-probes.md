---
name: domain-mutation-probes
description: Mutation-probe techniques for bookstore domain value objects and aggregates (checksum/ASCII falsifiers, shared validating constructors, null guards masked by later NPEs) and flow-layer probes (counterpart probes per command, port-order column substitution)
metadata:
  type: feedback
---

Rule: a "rejects X" test only falsifies a clause if the input is rejected by THAT clause alone. Check what else would reject it under the mutation.

**Why:** an ISBN test with a non-ASCII digit at the check-digit position kills the Unicode-aware variant (`isDigit` + `Character.digit`), but it survives the natural removal `allMatch(Character::isDigit)`. The `charAt - '0'` checksum arithmetic then rejects the input by accident, so production can still accept other non-ASCII ISBNs.
**How to apply:**
- For a checksum-guarded format, build the witness so that the naive checksum still matches. Replace digit d at weight w with a char c where `w * ((c - '0') - d) % 10 == 0`. Example: U+0661 for the `5` at index 10 (weight 1) of 9780141439518.
- Prove that the survivor is not equivalent: add a temporary QA test (snapshotted, then `rm`), show it fails under the mutation and passes on the real code, and quote both logs.
- Both factories sharing one private validating constructor: apply "factory B skips check K" as a constructor `boolean` flag (A passes true, B false). Only B's test should fail. That also proves A's path is still covered.
- Null guards (`requireNonNull`) are C0-8 validation branches. Only the guards whose removal is not masked by a later NPE (`List.copyOf(null)`, `price.isGreaterThan`, `Year.now(null)`) need a pinning test. Name the unmasked ones by file:line.
- Probes that add a moving part (mutating assignment order) are cheap extras: "assign the field before the checks" falsifies every "leaves X unchanged" clause at once.
- Extra probes must run before the referee, because the referee runs last (C0-1). Anything thought of after the referee gets reported "by reasoning, not run".
- Rework rounds: keep a sha256 list of the fileScope from round 1. If `diff` shows only test files changed, cite the round 1 probe logs instead of repeating them, and re-probe only the Required Actions. Also re-run every round 1 probe whose only killer was an edited test. When a rework adds an assertion after an existing one, prove the new line bites: use a mutant that passes the old line and breaks only the new one (for example catch, publish or save, then rethrow the same instance). The failure must then point at the new line number.
- Verify guard-pinning tests with one deletion per run, not the impl's combined run. A combined run cannot show which test depends on which guard, and the expected signature is "N run, 1 failure, that test only".

- Pass-through `Clock` parameters: always probe "use `Clock.systemUTC()` instead of the parameter". A test whose year is rejected by both the fixed clock and the real one (for example 2028 against a 2026 clock) survives. The witness is a fixed clock in the past plus a year only the real clock accepts.
- Parameterized per-class tests (several exception classes with no shared base): a one-class mutation must fail only index `[n]`. That proves the parametrisation binds every class.
- Oracle trap: flow tests that compute the expected message by calling production code (a `domainMessage(() -> mapper...)` helper) fail inside the oracle when that code is mutated, before the flow assertion runs. The kill is real, but prove the flow assertion bites with a temporary oracle-free witness (exact type, code, `never().save`), run it under the mutation and on restored code, then `rm` it.
- Short-circuit trap: a probe that breaks two halves at once (blank genre and blank text criteria) can be killed by the first half alone (the genre throws before `search` is verified). Split it into a probe for the second half so that half is shown pinned on its own.

- Parameter hard-coding on every read path: when the probe list pins a stored parameter against a hard-coded default on one path (for example reorder level in `lowStock()`), apply the same "hard-code the default" probe to every other branch that reads that parameter (for example the reset in `receive`). Tests often use the default value on the paths the list does not name, so the mutant survives there. Prove it is not equivalent with a temporary witness that tests both sides (level above and below the default).
- Probe edits on a shared helper (one guard used by the constructor and a behaviour method) blur which caller is pinned. Inline the mutated check in the one caller instead.
- Exception payload witnesses: a "duplicate X" test with data A, B, A asserting `id() == A` cannot tell "the duplicated element" from "the first element". Probe "report `list.get(0)`" every time; the witness needs the duplicate in a non-first, non-adjacent position (B, A, C, A). Also probe "report `list.get(size - 1)`": B, A, C, A ends on the duplicate, so it needs a case where the second copy is not last (A, C, A, B).
- Bound probes in a schema: change only the compared literal (keep the constant the exception reports), otherwise the accessor assertion (`max() == 10`) kills the mutant before the boundary input is ever exercised.

Flow-layer probes (mocked port and publisher):
- "Returns the port result in order" tests: a stub that is "in neither id nor available order" can still be ascending in on hand or reserved, and a column that is constant across rows turns a stable sort into a no-op. Probe a sort by each DTO column, not only the one named in the list. Prove a survivor with a witness stub that is monotonic in no column.
- Probe lists often name a mutant for one command only (toDomain outside the translator, value object before the load, save the loaded DTO). Run the counterpart in the other command too. It costs one run and closes the gap for the second command.
- A new line in a shared verify helper (for example `verifyNoInteractions(newPort)` in `verifyNothingLookedUpOrSaved()`): add a stray harmless port call (`newPort.findLowStock()`) as the first statement of the method under test. Expected signature: every helper caller fails at the helper's line number, plus the separately named tests at their own lines, and nothing else. This proves the line bites in each caller in one run. Callers that survive are the rejection tests without the check (list them for FF9).
- Swallowing probe on a new port call (`try { port.save(...) } catch (RuntimeException ignored) {}`) survives mocked flow tests by design. Run it as informational and name the integration test that owns atomicity.
- Probe the details `message` separately from the field (for example the title passed instead of the domain message). A test that asserts only the field lets that mutant survive.

Related: [[spring-runtime-probes]], [[scaffold-verification-techniques]]
