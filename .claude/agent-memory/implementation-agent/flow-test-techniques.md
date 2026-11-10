---
name: flow-test-techniques
description: Mockito and AssertJ techniques for *FlowTest classes with mocked ports (strict stubs, precedence tests, exact translation checks, clock witnesses, event capture, pass-through ordering)
metadata:
  type: reference
---

- `@ExtendWith(MockitoExtension.class)` uses strict stubs: a never-called stub fails with `UnnecessaryStubbingException`. For a
  precedence test (stub the port to say "duplicate", expect an earlier 422 and `verify(port, never())`), mark that one stub
  `lenient()`; everywhere else keep strict stubs.
- An `Optional`-returning port method on a Mockito mock returns `Optional.empty()` without a stub; verify the exact argument
  (`verify(port).findByIsbn("<normalised>")`) instead, so a raw-argument mutation fails on the verify rather than on a
  strict-stub argument mismatch.
- Assert translated messages exactly by computing the domain exception's message in the test
  (`catchThrowable(() -> new Title(bad)).getMessage()`) instead of `isNotBlank()` alone.
- Clock witnesses: a second flow on `Clock.fixed(2000-...)` with publication year 2027 kills `Clock.systemUTC()` at each call
  site, but only one site per test: give every `toDomain(dto, clock)` call site its own witness.
- Published events: capture with `ArgumentCaptor<Object>`, so `verify(events).publishEvent(captor.capture())` binds the
  `publishEvent(Object)` overload and a mutant publishing the whole pulled list as one event fails on record equality. Put the
  port and the publisher in one `inOrder(port, events)` to pin save before publish.
- Not-found pass-through tests call the flow with input that would fail validation (`receive(id, -1)`), so a value object built
  before the load turns the expected `isSameAs` 404 into a 422.
- A new `@Service` that needs a bean nobody provides yet breaks every full-context test; land the config class (e.g. `Clock`
  bean) in the same task.
- When a criterion is checked by a token grep (`publishEvent\(`, `orders\.save\(`), write explicit calls, not method references
  (`events::publishEvent` hides from the grep). `inOrder.verify(events).publishEvent(any(Object.class))` binds the `Object`
  overload without a captor.
- Order and "which element" tests (`containsExactly` on a port list): make the stubbed order monotonic in NO column, asc or
  desc (id, every int field, derived values, booleans), else a re-sorting mutant survives. Three rows, reported row in the
  middle; the asserted list itself (shortage `details`, stock events) needs three entries. QA counts every attribute of the
  entity in scope, not only the asserted record's fields. A shared fixture asserted in several orders: tabulate every order the
  constant appears in. Constant columns let a stable sort survive: vary them. Add save-never/no-events checks to read-side
  rejection tests.
- Self-probe the QA probe list before handing over: one script in `.claude/comms/tmp/` builds each mutant with `perl -0pe` from
  a `cp -p` original in the scratchpad, installs it by `cp`, runs `-Dtest=<FlowTest>`, restores and compares `shasum`; flag
  "MUTANT NOT APPLIED" when the regex misses (`cmp -s`). Mutants that cross lines need `/s`.
- An external-port DTO that carries its own key (distributor isbn) gets a stub value different from the request, so a flow that
  builds from the DTO key fails the saved-record assertion.
- Flow log assertions: `@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})` works in a plain unit test.
  Logback's unconfigured pattern and Boot's pattern both contain `" WARN "`, so
  `containsOnlyOnce(" WARN ").containsPattern(" WARN .*" + id)` pins level and id on one line without filtering the output (QA
  forbids filtering). Put the id only in the log arguments of a witness whose exception message lacks it.
- Two use cases sharing one implementation: a test-local `enum UseCase` holding `BiConsumer<Flow, UUID>` plus
  `@EnumSource`/`@MethodSource` runs every shared claim against both without duplicated bodies.
