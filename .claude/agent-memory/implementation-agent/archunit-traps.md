---
name: archunit-traps
description: Non-obvious ArchUnit 1.5.1 behaviours met while extending ArchitectureTest and ArchitectureSelfTest in the bookstore module
metadata:
  type: reference
---

- `LayeredArchitecture` is mutable: a shared instance collects every `whereLayer(...)` clause (eight rules on one
  instance give eight `where layer '` clauses each). That is why `ArchitectureTest.layers()` builds a fresh one;
  `ArchitectureSelfTest.everyLayeredRuleStatesExactlyOneLayerConstraint` pins it.
- A layered rule's `getDescription()` spans several lines (one `layer '...'` line per layer, then the `where layer`
  clause). To print it, dump with a temporary test and grep the log with `-A10`, not a single-line grep.
- Primitive field types (`int`) create no dependency; array types do (`long[]` is `[J`, package `java.lang`), so
  narrowing a `java..` allowlist reports array fields.
- "Additions only" diffs on `Map.ofEntries(...)`: appending after the last entry rewrites its `)));` line. Insert a
  new entry between existing ones instead.
- `haveRawParameterTypes(Class...)` matches the exact parameter list; for "any parameter is X" use a
  `DescribedPredicate<List<JavaClass>>` with `anyMatch` (see `ArchitectureTest.includeDoubleOrFloat`).
- A `CompositeArchRule` prints each half's package list separately (`... and no code units ...`). A set of the
  packages parsed from the whole description hides a package dropped from one half only; pin each half's bracket
  (QA expects it, see `moneyRuleDescriptionNamesTheFourLayersInEachHalf`). Likewise, a violator fixture whose
  floating-point parameter sits last lets a last-parameter-only predicate pass: put it in the middle.
- Parameter fixtures for an "any parameter" predicate need every position: only, first, middle, last (QA kills
  `skip(1)` and `size() > 1 &&` with a single-parameter and a first-parameter fixture). Kill a predicate mutant
  with a real violator fixture, never by reflecting on the private helper (the orchestrator rejects that).
- `ArchitectureSelfTest.assertReportsExpectedViolators` stops at the first missing violator, so the order of an
  `EXPECTED_VIOLATORS` list decides which fixture a probe names. Put the fixture that uniquely kills a mutant
  before the ones that die with it.

Related: [[maven-build-traps]].
