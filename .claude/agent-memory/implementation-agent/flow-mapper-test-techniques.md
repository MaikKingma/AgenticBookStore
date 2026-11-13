---
name: flow-mapper-test-techniques
description: Mockito and AssertJ techniques for *FlowTest classes and DomainMapper/DTO tests so QA mutation probes die in round one (strict stubs, clock witnesses, event capture, ordering, validating hand-offs)
metadata:
  type: reference
---
Walk every field the code under test hands on and give it the witness its kind needs before handing over (missing witnesses cost
QA rework rounds while production code was correct).

## Flow tests (mocked ports)
- `MockitoExtension` strict stubs fail a never-called stub. For a precedence test (stub says "duplicate", expect an earlier 422
  and `verify(port, never())`) mark that one stub `lenient()`.
- An `Optional` port method returns `Optional.empty()` unstubbed: verify the exact argument (`verify(port).findByIsbn("<norm>")`)
  so a raw-argument mutation fails on the verify.
- Assert translated messages exactly: compute the domain exception message in the test (`catchThrowable(() -> new Title(bad))`).
- Clock witnesses: a second flow on `Clock.fixed(2000-...)` with publication year 2027 kills `Clock.systemUTC()`, one call site
  per test: give every `toDomain(dto, clock)` call site its own witness.
- Events: `ArgumentCaptor<Object>` so `verify(events).publishEvent(captor.capture())` binds the `Object` overload; `inOrder(port,
  events)` pins save before publish. A criterion checked by a `publishEvent\(` grep needs explicit calls, not `events::publishEvent`.
- Not-found pass-through tests call the flow with input that would fail validation (`receive(id, -1)`).
- Order and "which element" tests: make the stubbed order monotonic in NO column (id, every int field, derived values,
  booleans), asc or desc. Three rows, reported row in the middle; the asserted list itself (shortage `details`, events) needs
  three entries. Constant columns let a stable sort survive: vary them. Add save-never/no-events checks to read-side rejections.
- Log asserts: `@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})` works in a unit test; `containsOnlyOnce(" WARN
  ").containsPattern(" WARN .*" + id)` pins level and id (QA forbids filtering output). Put the id only in the log arguments.
- Self-probe before handing over: script in `.claude/comms/tmp/` builds each mutant with `perl -0pe` from a `cp -p` original, runs
  `-Dtest=<FlowTest>`, restores, compares `shasum`; flag "MUTANT NOT APPLIED" (`cmp -s`). Cross-line mutants need `/s`.

## Mapper and DTO tests
- Hand-off into a validating factory (`Money.of`, `new EmailAddress`, `OrderStatus.valueOf`, any checking `new XxxId`): one test
  feeds a value it rejects and asserts the exception at the mapper layer, else a drop-the-argument mutant survives.
- Plain pass-through: at least two distinct accepted values per class, asserted by literal. "Which element" derivations: first,
  middle and last differ.
- Aggregate built from a port DTO inside the exception translator: each value-object constructor in the lambda needs its own
  rejected DTO value asserted as the translated 422. Price literals need a second value with cents; an id in an exception
  message needs a second value.
- Either-or hand-offs: override below and above the default, equal amount made distinguishable by another field, zero, negative,
  null, both ends of each range. Build an input x relation sweep table before round one.
- "Regardless of threshold" needs a case strictly below it (`>`/`>=` twins). A length rule on a value kept as given needs max chars
  plus a trailing space. `isBlank()` needs a key of only U+2003 (`Character.toString(0x2003)`): `trim().isEmpty()` mutants survive
  spaces and `"\t\n"`.
