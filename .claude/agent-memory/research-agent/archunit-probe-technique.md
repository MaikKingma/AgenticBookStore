---
name: archunit-probe-technique
description: How to verify ArchUnit API shapes and violation text without touching the repo
applies-to:
  - bookstore/src/test/**/ArchitectureTest.java
  - bookstore/src/test/**/ArchitectureSelfTest.java
metadata:
  type: reference
---

Compile a tiny probe with plain `javac` against `~/.m2/.../archunit/1.5.1/archunit-1.5.1.jar` plus `slf4j-api` (any 2.0.x). No Maven needed.
Put fixture classes in a throwaway package such as `nl.openvalue.fx.<layer>` so the repo's `nl.openvalue..domain..` patterns match.
Print `rule.evaluate(classes).getFailureReport().getDetails()` to see the exact text spec 01's `ArchitectureSelfTest` origin regex sees.
Built-ins cover most needs: `CompositeArchRule.of(a).and(b)`, `noCodeUnits().should().haveRawParameterTypes(DescribedPredicate)`.
See [[boot4-verification-techniques]] for the scratchpad probe-project approach on the Spring side.
