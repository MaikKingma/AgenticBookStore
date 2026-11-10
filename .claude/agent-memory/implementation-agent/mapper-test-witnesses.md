---
name: mapper-test-witnesses
description: What DomainMapper, DTO-convenience-constructor and flow-translator tests need so QA mutation probes die on the first round (validating hand-offs and single-literal pass-throughs)
metadata:
  type: feedback
---

Before handing a mapper or DTO test class to QA, walk every field the code under test hands on and give it the witness its kind
needs (missing witnesses cost QA rework rounds while the production code was correct; see qa-test-verifier memory
`domain-mutation-probes.md`). Related: [[flow-test-techniques]].

- Hand-off into a validating factory (`Money.of(amount, code)`, `new EmailAddress(...)`, `OrderStatus.valueOf(...)`, any `new XxxId`
  that checks): one test feeds a value that factory rejects and asserts the exception at the mapper layer (a "USD" price, a
  "BOGUS" status). Otherwise a mutant that drops the argument or parses leniently survives.
- Plain pass-through (id, timestamps, e-mail, book id): at least two distinct accepted values across the class, each asserted by
  literal; one shared fixture constant lets "replace by the test's own literal" mutants survive.
- "Which element" derivations (`currency()` of the first line): a fixture where first, middle and last differ.
- A flow that builds an aggregate from a port DTO inside the exception translator: every value-object constructor in that lambda
  (`new Title`, each `new AuthorName`, `Genre.parse`, `Money.of`) needs its own rejected DTO value asserted as the translated
  422, or a "built outside the translator" mutant survives. Price literals need a second accepted value with cents (12.00 and
  12 both save 12.00), and an id used in an exception message needs a second value too.
- Either-or hand-offs (an optional override against a default) need every relation on both sides: override below and above
  the default, equal amount made distinguishable through another field, zero, negative and null, plus both ends of each domain
  range for both inputs. Build a sweep table input x relation before the first QA round.
- Rules "X happens regardless of threshold" need a case strictly below the threshold, not only at it: `> level` and `>= level`
  guards are twin mutants. A length rule on a value kept as given needs a witness of max chars plus a trailing space, or a
  `strip().length()` mutant survives.
- An `isBlank()` check needs a key of only U+2003 (built as `Character.toString(0x2003)`, file stays ASCII) asserted as blank:
  spaces and `"\t\n"` are also stripped by `trim()`, so a `trim().isEmpty()` mutant survives them.
