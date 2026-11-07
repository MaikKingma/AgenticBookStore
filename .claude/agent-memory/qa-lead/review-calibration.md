---
name: review-calibration
description: Calibration rules settled in meta-reviews (when earlier probe evidence may carry into a rework round, null guards under the coverage item, which surviving mutations are REJECT versus inspection plus feed-forward, survivors whose kill contradicts a pinned criterion, strengthening-only survivors, edited killing assertions vs carry-forward)
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
   A verifier can prove (b) without my quotes: it copies the test file, deletes the inserted method and hashes the result against the earlier baseline. A non-probe check that depends on state across test classes (order independence) carries forward under the same conditions only when the inserted test writes fresh keys inside a rolled-back slice transaction after the cleaning hook.

2. **Null guards count as validation branches.** In domain and domaininteraction code, an `Objects.requireNonNull` guard, or any null check that throws, needs a test that fails when it is removed. The exception is when the removal is equivalent: a later statement throws the same exception type on the same input (for example `List.copyOf(null)`, or a dereference of the field inside the constructor).
   **Why:** without the guard, an aggregate with a null field gets built silently, and no test notices.
   **How to apply:** feed this forward to impl directives so the tests get written up front, not after a FAIL.

3. **REJECT threshold for surviving mutations.** On money, ISBN or numbering, persistence, transaction and cross-module contract criteria, a surviving mutation I can show from the code is a REJECT.
   On a low-risk criterion, a survivor that only distinguishes the decided wording (`strip()` vs `trim()`, `Locale.ROOT` vs the default locale) gets different treatment when the strategy listed no falsifier for it and the code holds the decided form. I verify it by reading, record it as inspection-proven, and feed forward a witness for later tasks.
   **Why:** the strategy's falsifier list is the advance bar. Spending the last rework round on a cosmetic distinction moves that bar after the fact.
   **How to apply:** prevent it at strategy time instead: name the witness in the falsifier list (see traps file item 15).
   The same allowance covers a survivor that is equivalent in production because of a pinned configuration, for example a mapper that ignores its `Clock` parameter while the configured bean is `Clock.systemUTC()`. It differs only under a test double. The allowance holds when the clause was an inspection item, not a listed falsifier. Then feed a witness forward to the next task that crosses the same seam, and open a follow-up for any test whose name claims the property. If that over-claiming test was the only listed falsifier, the survivor stays a REJECT.

4. **Survivor whose kill contradicts a pinned criterion.** If the only way to kill a surviving mutation is to change something an acceptance criterion pins (a fixture signature, for example), it is not a REJECT. Record it as a plan-level gap, recommend the plan amendment to the orchestrator (cheapest inside a rework round that runs anyway), and otherwise add a follow-up and a final-brief item. Say so when the pin came from my own Gate 1 rewrite. A survivor that can be killed in scope without touching a pin stays a REJECT under rule 3.
   **Why:** a REJECT whose Required Action breaks a criterion cannot be met. Scope belongs to the orchestrator.
   **How to apply:** verifiers tend to label all their own extra survivors "informational". Judge each one against the criterion text and the file scope, not against the directive's probe list.

5. **Strengthening-only survivors.** A mutation that can only add violations (for example ArchUnit `allowEmptyShould(true)` -> `(false)` on imports that are never empty) does not weaken the guard. If the criterion pins the setting and the strategy made it a reading item, record it as inspection-proven, not a REJECT, even on money criteria. Rule 3's no-allowance clause is for survivors that let a violation through.
   **Why:** the REJECT threshold protects the guard's direction; pinning a stricter-is-fine setting costs a rework round for no safety.

6. **Edited killing assertion vs carry-forward (rule 1 b).** An edit voids carried probe evidence unless it is a monotonic strengthening I verify in the helper: for example an expected-violator list extended with the old entries as an unchanged prefix, checked by a loop that asserts each entry on its own (no count or exact-set check). Read the helper before accepting it.
   **How to apply:** the implementer's list order decides which fixture a probe names first; demand a probe that only the new fixture can kill to show it is load-bearing.
