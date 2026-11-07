---
name: review-calibration
description: Calibration rules settled in meta-reviews (when earlier probe evidence may carry into a rework round, null guards under the coverage item, which surviving mutations are REJECT versus inspection plus feed-forward)
metadata:
  type: feedback
---

Rules I settled while meta-reviewing verifier verdicts in this repo's SDD cycle. Apply them in every review so verdicts stay consistent across tasks and specs. See also [[qa-directive-traps]].

1. **Carrying probe evidence into a rework round.** A verifier may skip re-running the earlier round's mutation probes only if all three of these hold:
   - (a) the sha256 of every production file equals the earlier baseline;
   - (b) the test diff is insertion-only, which I check by finding every earlier quoted asserting line verbatim at its recorded or shifted line (the shift equals the inserted block);
   - (c) the earlier probe logs are still on disk.

   **Why:** a mutant's kill depends only on the production bytes and on the killing test. Re-running is wasted budget, but an edited killing test silently voids the old evidence.
   **How to apply:** this is why I quote asserting lines into the ledger every round. If any of (a) to (c) fails, CHALLENGE for a re-run.

2. **Null guards count as validation branches.** In domain and domaininteraction code, an `Objects.requireNonNull` guard, or any null check that throws, needs a test that fails when it is removed. The exception is when the removal is equivalent: a later statement throws the same exception type on the same input (for example `List.copyOf(null)`, or a dereference of the field inside the constructor).
   **Why:** without the guard, an aggregate with a null field gets built silently, and no test notices.
   **How to apply:** feed this forward to impl directives so the tests get written up front, not after a FAIL.

3. **REJECT threshold for surviving mutations.** On money, ISBN or numbering, persistence, transaction and cross-module contract criteria, a surviving mutation I can show from the code is a REJECT.
   On a low-risk criterion, a survivor that only distinguishes the decided wording (`strip()` vs `trim()`, `Locale.ROOT` vs the default locale) gets different treatment when the strategy listed no falsifier for it and the code holds the decided form. I verify it by reading, record it as inspection-proven, and feed forward a witness for later tasks.
   **Why:** the strategy's falsifier list is the advance bar. Spending the last rework round on a cosmetic distinction moves that bar after the fact.
   **How to apply:** prevent it at strategy time instead: name the witness in the falsifier list (see traps file item 15).
