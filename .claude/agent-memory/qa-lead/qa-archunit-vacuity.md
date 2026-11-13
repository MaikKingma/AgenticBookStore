---
name: qa-archunit-vacuity
description: ArchUnit rules and their self-tests: allowEmptyShould vacuity, reflective @AnalyzeClasses, origin-anchored failure matching, per-string and per-position witnesses (parameter predicates, nested-class walks), composite rules; read before any ArchUnit task strategy or review
metadata:
  type: feedback
---

Siblings: [[qa-directive-traps]], [[qa-probe-design-traps]].

- Every rule passes on empty packages (allowEmptyShould everywhere), so the production run needs negative controls: a violating class in a real layer package, and a root-package class touching domain.
- The self-test's production sanity check must read `@AnalyzeClasses` reflectively (a hard-coded copy misses a package typo). String-typed predicates (annotation FQN, package names) are silently vacuous when misspelled: only a fixture-backed per-rule case catches that.
- "At least one violator per rule" is too weak for a rule with several package strings (`jakarta.persistence..` plus `org.springframework.data..`): one violator per string. `areAnnotatedWith` matches direct annotations only; `areMetaAnnotatedWith` also matches direct ones: decide at strategy time.
- Never write "failure details contain the expected FQN": details name origin AND target, and fixture chains across layers make a designated violator the target of a neighbour rule, so a one-token layer swap survives. Ask for an origin-anchored match (first `<...>` token starts with the FQN) plus `getDescription()` pinned per rule (also catches shared-instance stacking and allowlist or suffix widening).
- Completeness by reflection over `@ArchTest` fields misses `@ArchTest` methods: add "declares no @ArchTest method". Layered descriptions render `where layer '<name>' may ...` and the layer definition lines also contain `layer '`: count or pin `where layer '`, never the bare token.
- A parameter predicate ("any parameter is double or float") needs four position witnesses, each its own fixture class (the origin regex accepts any member of a class): lone parameter (kills `size() > 1 &&`), first of several (kills `skip(1)`), middle of three (kills last-only and first-only), last (kills first-only). Keep multi-parameter fixtures too (a single-parameter fixture lets an exact-list `haveRawParameterTypes(double.class)` pass); include a method and a constructor. Pin all four in the Gate 1 fixture wording.
- "Not reported" controls must be origin-anchored too: a control class that is the TARGET of another fixture's violation fails `noneMatch(detail.contains(FQN))` on correct code. When one fixture carries several witnesses (depth-1 and depth-2 nested types), anchor by member (`^Field <FQN.field>`). The production import is a free control for widened predicates when real code already uses the excluded shape.
- A predicate walking the enclosing-class chain ("nested at any depth in a class named *X") needs a member-anchored witness per position of the matching class, not only per depth: direct enclosing only, middle only, top only, depth one past any cap you probe, a class named exactly `X` (kills a `.+X` regex), controls for letter case, a longer name and the class's own name. Each control asserts its field type first. A walk cap at or above the deepest fixture is unkillable: do not list it as must-fail.
