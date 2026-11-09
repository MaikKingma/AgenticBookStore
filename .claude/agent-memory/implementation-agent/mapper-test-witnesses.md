---
name: mapper-test-witnesses
description: What DomainMapper, DTO-convenience-constructor and flow-translator tests need so QA mutation probes die on the first round (validating hand-offs and single-literal pass-throughs)
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

The same walk applies to a flow that builds an aggregate from a port DTO inside the exception translator: every value-object
constructor in that lambda (`new Title`, each `new AuthorName`, `Genre.parse`, `Money.of`) needs its own rejected DTO value
asserted as the translated 422, or a "built outside the translator" mutant survives. Price literals need a second accepted
value with cents (12.00 and 12 both save 12.00), and an id used in an exception message needs a second value too.

Either-or hand-offs (an optional override against a default) need every relation on both sides: override below and above
the default, equal amount made distinguishable through another field (default in a rejected currency), zero, negative and
null, plus both ends of each domain range for both inputs. QA mutants "keep the higher", "zero means none" and "use the
override only when it differs" survive an all-higher fixture. Build a sweep table input x relation before the first QA round.

Domain rules "X happens regardless of threshold" (a delivery clears the pending restock even when stock stays low) need a
case strictly below the threshold, not only at it: `> level` and `>= level` guards are twin mutants, and a fixture ending
at exactly the level kills only one (spec 07 T1 took a QA-lead REJECT for this). A length rule on a value kept as given
needs a witness of max chars plus a trailing space, or a `strip().length()` mutant survives.

An `isBlank()` check needs a key of only U+2003 (built as `Character.toString(0x2003)`, file stays ASCII) asserted as
blank: spaces and `"\t\n"` are also stripped by `trim()`, so a `trim().isEmpty()` mutant survives them (spec 08 T1).
