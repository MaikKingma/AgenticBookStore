---
name: flow-test-techniques
description: Mockito and AssertJ techniques for *FlowTest classes with mocked ports (strict stubs, precedence tests, exact translation checks, clock witnesses, event capture, pass-through ordering)
metadata:
  type: reference
---

- `@ExtendWith(MockitoExtension.class)` uses strict stubs: a stub that is never called fails the test with
  `UnnecessaryStubbingException`. For a precedence test (stub the port to say "duplicate", expect an earlier 422
  and `verify(port, never())`), mark that one stub `lenient()`; everywhere else keep strict stubs.
- An `Optional`-returning port method on a Mockito mock returns `Optional.empty()` without a stub; verify the exact
  argument (`verify(port).findByIsbn("<normalised>")`) instead of stubbing it, so a raw-argument mutation fails on
  the verify rather than on a strict-stub argument mismatch.
- Assert translated messages exactly by computing the domain exception's message in the test
  (`catchThrowable(() -> new Title(bad)).getMessage()`) instead of `isNotBlank()` alone.
- Clock witnesses: a second flow on `Clock.fixed(2000-...)` with publication year 2027 kills `Clock.systemUTC()` at
  each call site, but only one site per test. Give every `toDomain(dto, clock)` call site its own witness
  (add, change price and discontinue each have one in `CatalogueFlowTest`).
- Watch a behavioural red, not only a compile red: add a stub method that returns a neutral value first, run, then
  implement.
- Published events: capture with `ArgumentCaptor<Object>`, so `verify(events).publishEvent(captor.capture())` binds
  the `publishEvent(Object)` overload and a mutant that publishes the whole pulled list as one event fails on record
  equality. Put the port and the publisher in one `inOrder(port, events)` to pin save before publish.
- Not-found pass-through tests call the flow with input that would fail validation (`receive(id, -1)`), so a value
  object built before the load turns the expected `isSameAs` 404 into a 422.
- Order pass-through tests (`containsExactly` on a port list): make the stubbed order monotonic in NO column, asc or
  desc (id, every int field, derived values like available, booleans), else a re-sorting mutant survives. Also add
  the save-never/no-events check to read-side rejection tests (QA counts them as rejections).
- A new `@Service` that needs a bean nobody provides yet breaks every full-context test; land the config class in the
  same task (spec 02 T3: `CatalogueFlow` needs the `Clock` bean from `ClockConfig`).
- When a criterion is checked by a token grep (`publishEvent\(`, `orders\.save\(`), write explicit calls, not method
  references: `events::publishEvent` hides from the grep. `inOrder.verify(events).publishEvent(any(Object.class))`
  binds the `Object` overload without a captor.
- Multi-aggregate flows (spec 04 `OrderFlow`): give every "which element is reported" or "in X order" test three rows
  where the reported row is in the middle and no stock column is monotonic in book-id order; QA probes sort by each.
  This includes the asserted list itself: a two-entry shortage `details` list or two published stock events is always
  sorted in every column, so it needs three entries too. Tabulate each asserted column before reporting.
  QA counts every attribute of the entity the flow holds in scope (isbn, title, price of the book), not only the
  asserted record's fields. A shared fixture list (e.g. `STORED_LINES`) is asserted in several orders (book-id order
  for stock, stored order for the saved DTO): one changed literal can fix one order and make another monotone, so
  tabulate every order the constant appears in. Constant columns (every reorder level 3) also let a sort survive,
  because Java sorts are stable: vary them unless validation or an asserted literal pins them. Before-values derived
  from asserted after-values and quantities can be pinned; show another test kills that sort instead.
