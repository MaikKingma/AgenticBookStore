---
name: round0-directive-traps
description: Witness, falsifier and criteria rules to put into implDirective/qaDirective at round 0 so code tasks pass without test-only rework; pointers to the plans that hold the detail
applies-to:
  - any
metadata:
  type: project
---

Rework rounds in this repo were almost always test-only (a surviving mutant, a missing falsifier), never production
defects. The cheapest fix is naming the witness family up front.

## Process
- Paste the QA lead's must-fail probe or falsifier list into the implDirective of round 0.
- Feed-forward only lands if copied into plan.md `## QA feed-forward` AND the next task prompt; ledger-only items were
  skipped by verifiers.
- The lead's test-only follow-ups after an ENDORSE are cheapest as one tail task before qa-final (spec 06 T8), or
  routed into the next task by an insertion-only scope amendment; on the last task, an owner residual.
- Fixture-only amendments inside a task, and aligning stub literals without touching assertions, are within authority:
  log under `## Amendments`, self-accept the gate re-run.
- When a lead Required Action literal fails the lead's own rule, a stronger literal within the rule is within authority.
- Implementers never mutate production files outside their fileScope to probe: "describe each mutant in the outbox;
  the verifier runs it". If a task's plan-defined control IS a production-file probe, carve it out explicitly.
- Reject verifier proposals for reflective tests of private helpers; ask for a fixture instead.
- Criteria I add beyond the pack spec stay source-level and inside the task's file scope (spec 01: a "no
  package-info.class" criterion was impossible without a POM change).
- Before planning a defence-in-depth check, grep every fixture that feeds it (fix-06: one helper fed about twenty tests).

## Witness families
- **Ordering:** three or more rows per order claim; every in-scope attribute non-monotone in every asserted order
  (stored and sorted, including fixture isbn, joined columns, NULL position); a tabulation table in the outbox (spec 04
  plan.md A1, A3).
- **Pass-through and validation:** two values per pass-through field, a rejected witness per validating hand-off,
  every pass-through field asserted, unpinned required params pinned.
- **Caps and optionals:** name both sides of every cap; an optional value vs its default below, equal, above and zero.
- **Malformed bodies:** probe per list element (null after a valid one) and per numeric type (float in an integer
  field), not only per key (spec 06 ledger SP-1/SP-2).
- **Exact equality and escapes:** leading and trailing pad, front and end extension, proper prefix, proper suffix, null;
  mutants `a.op(b)` and `b.op(a)` for startsWith/endsWith/contains plus the strip family; escape sets named by a JDK
  predicate need every `\R` member plus its boundaries; a new constructor that must leave a field untouched asserts it
  (fix-06 plan.md FF2, FF4, FF5).
- **Class-chain walks:** every chain position plus exact-name and letter-case controls.
- **Composite ArchUnit rules:** pin each half's description (same package list in both halves lets one-half drops
  survive); a "contains any double/float parameter" predicate needs fixtures with it alone, first, middle and last.
- **Searches over a collection:** a limit-counts-books test (Hibernate drops duplicate entities in memory, so a `join`
  instead of `exists` only shows with a small `Limit`).
- **Shared in-flight state:** every reset branch has a test that sends again.
- **Seeded facts in docs:** a seed census for every README data claim.

## Bugfix criteria
- A guard test that asserts a new message or a defect value cannot be "green before and after": write "green after only".
- "No double rounding" needs sub-ulp witnesses (`500.00000000000001`), not merely distinct doubles.
- New UI elements: "red set = the named tests plus any test of a new element; no pre-existing test red".
- A requirement change that old tests pin is a listed carve-out of exact lines, each replaced by a stronger assertion,
  never a blanket "tests may change".
- A thread-safety defect has no deterministic behavioural test: a package-private seam plus a structural test, with the
  negative control restoring only the root-cause file.

## Frontend
- Run the QA lead's literal grep set before handoff (titles, comments and fixtures are scanned too); build banned-form
  fixtures from string parts; tabulate every array inside a pass-through `toEqual` (3+ elements, no attribute sorted
  either way, one UUID per entity). Licence files are exempted from a grep, never edited.
