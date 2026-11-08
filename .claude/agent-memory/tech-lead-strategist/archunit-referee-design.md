---
name: archunit-referee-design
description: Rationale and anti-vacuity technique for the bookstore ArchitectureTest and ArchitectureSelfTest; use when a spec adds or touches an ArchUnit rule (specs 02, 06, 07) or when planning referee work
metadata:
  type: project
---

Decided in the spec 01 strategy (planned files: bookstore/src/test/java/nl/openvalue/bookstore/ArchitectureTest.java
and ArchitectureSelfTest.java; verify they exist before citing them).

Rule granularity: standard rule 3 is six named rules, one per outer adapter (20 rules total in spec 01:
13 dependency plus 7 naming). **Why:** a single combined rule is satisfied by one fixture, so five adapters could stay
unproven while the self-test looks green. **How to apply:** any rule over a set of packages gets one rule per member
when each member needs its own proof.

Anti-vacuity guards (all rules use `allowEmptyShould(true)`, so silence is the failure mode):
- engine discovery: Surefire XML must show the exact rule count for ArchitectureTest (JUnit 6 engine risk);
- production import sanity: re-import with the packages/importOptions read from `@AnalyzeClasses`, assert
  BookstoreApplication present and test classes absent;
- completeness map: reflected `@ArchTest` fields must equal the self-test's rule-to-violator map keys, so every new
  rule (money rule spec 02, acl rule spec 06) must bring a fixture;
- a root-level fixture (`nl.openvalue.archfixtures.RootLevelConfig`) keeps `consideringAllDependencies()` honest;
  without it, switching to `consideringOnlyDependenciesInLayers()` would pass every self-test.

Isolating fixtures for a rule with several halves or packages (technique from the spec 02 money rule, reuse for the
spec 06 acl rule): one plain-class fixture per aspect (fields half, method half, constructor half, each primitive,
each package), every one the only violator of its aspect, plus one control in a realistic out-of-scope package
asserted with `noneMatch`. **Why:** origin matching passes as soon as any detail names the class, so a fixture that
violates two halves hides a dropped half; records count twice (field plus canonical constructor). Fixture names carry
their package's suffix so they never double as naming violators. Description pins (`getDescription()` contains the
packages, "may not be accessed by any layer") catch widenings that a violator list cannot.

Registry trap: `everyArchitectureRuleHasAnExpectedViolator` pins the rule count (`hasSize(N)`); a task adding a rule
must bump it, and the card should say so, or QA reads the edit as weakening an existing test.

Traps to name in task cards: one fresh `LayeredArchitecture` per rule (an `ignoreDependency` leaks otherwise; only rule 1
carries the process to `*Event` exception); never `onlyBeAccessed()`; never `DoNotIncludeTests` on the fixture importer;
fixtures stay a sibling package of `nl.openvalue.bookstore` (Spring scanning) and never match Surefire includes.

Skill vs standard: the `clean-hexagonal-onion` skill dates the Event exception to spec 07; the standard (section 2
item 9) puts it in spec 01's scope. The standard wins; spec 07 only verifies it. Related: [[test-context-cost-model]].
