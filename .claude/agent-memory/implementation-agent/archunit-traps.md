---
name: archunit-traps
description: Non-obvious ArchUnit 1.5.1 behaviours met while extending ArchitectureTest and ArchitectureSelfTest in the bookstore module
applies-to:
  - bookstore/src/test/**/ArchitectureTest.java
  - bookstore/src/test/**/ArchitectureSelfTest.java
metadata:
  type: reference
---
- `LayeredArchitecture` is mutable: a shared instance collects every `whereLayer(...)` clause (eight rules on one
  instance give eight `where layer '` clauses each). That is why `ArchitectureTest.layers()` builds a fresh one;
  `ArchitectureSelfTest.everyLayeredRuleStatesExactlyOneLayerConstraint` pins it.
- "Additions only" diffs on `Map.ofEntries(...)`: appending after the last entry rewrites its `)));` line: insert before it.
- `haveRawParameterTypes(Class...)` matches the exact parameter list; for "any parameter is X" use a
  `DescribedPredicate<List<JavaClass>>` with `anyMatch` (see `ArchitectureTest.includeDoubleOrFloat`).
- A `CompositeArchRule` prints each half's package list separately (`... and no code units ...`). A set of the
  packages parsed from the whole description hides a package dropped from one half only; pin each half's bracket
  (QA expects it, see `moneyRuleDescriptionNamesTheFourLayersInEachHalf`). Likewise, a violator fixture whose
  floating-point parameter sits last lets a last-parameter-only predicate pass: put it in the middle.
- Parameter fixtures for an "any parameter" predicate need every position: only, first, middle, last (QA kills
  `skip(1)` and `size() > 1 &&` with a single-parameter and a first-parameter fixture). Kill a predicate mutant
  with a real violator fixture, never by reflecting on the private helper (the orchestrator rejects that).
- Nested types: referencing `Outer.Nested` from another class gives ArchUnit dependencies only on `Outer$Nested`. The
  InnerClasses attribute creates no dependency on `Outer`, so a "never references the outer class" fixture holds
  (`javap -c -p` shows no `Outer;`). A nested type used from another package must be `public`.
- A walk predicate (enclosing-class chain, name suffix) needs a witness with the match on EACH walk position: direct
  only, middle only, top-level only, plus an exact-name witness and a letter-case control. Putting the suffix only on
  the top-level class lets "top-only" and "direct-or-top" walks survive, and QA probes both.
