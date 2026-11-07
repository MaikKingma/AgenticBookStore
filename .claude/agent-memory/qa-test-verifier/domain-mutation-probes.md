---
name: domain-mutation-probes
description: Mutation-probe techniques for bookstore domain value objects and aggregates (checksum/ASCII falsifiers, shared validating constructors, null guards masked by later NPEs)
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
- Rework rounds: keep a sha256 list of the fileScope from round 1. If `diff` shows only test files changed, cite the round 1 probe logs instead of repeating them, and re-probe only the Required Actions.
- Verify guard-pinning tests with one deletion per run, not the impl's combined run. A combined run cannot show which test depends on which guard, and the expected signature is "N run, 1 failure, that test only".

Related: [[spring-runtime-probes]], [[scaffold-verification-techniques]]
