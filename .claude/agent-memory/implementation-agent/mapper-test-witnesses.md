---
name: mapper-test-witnesses
description: What DomainMapper and DTO-convenience-constructor tests need so QA mutation probes die on the first round (validating hand-offs and single-literal pass-throughs)
metadata:
  type: feedback
---

Before handing a mapper or DTO test class to QA, walk every field the code under test hands on and give it the witness its kind needs.

- Hand-off into a validating factory (`Money.of(amount, code)`, `new EmailAddress(...)`, `OrderStatus.valueOf(...)`, any `new XxxId`
  that checks): one test feeds a value that factory rejects and asserts the exception at the mapper layer (a "USD" price, a
  "BOGUS" status). Otherwise a mutant that drops the argument or parses leniently survives (a lenient enum parse that maps
  anything else to the default is the classic one).
- Plain pass-through (id, timestamps, e-mail copied out of a value object, book id): feed at least two distinct accepted values
  across the class, each asserted by literal. If every test uses one shared fixture constant, "replace by the test's own literal"
  mutants survive.
- "Which element" derivations (`currency()` of the first line, and so on): a fixture where first, middle and last differ.

**Why:** spec 04 T2 needed two QA rework rounds only because these witnesses were missing; the production code was correct
every time (QA lesson FF-T2-2, kept in the qa-test-verifier memory `domain-mutation-probes.md`).

**How to apply:** at the refactor step of any task that adds a `*DomainMapper` or DTO convenience constructor, list the fields
the code hands on, tag each as hand-off or pass-through, and add the missing witnesses before reporting done. Related:
[[flow-test-techniques]].
