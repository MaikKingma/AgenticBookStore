---
name: flow-test-techniques
description: Mockito and AssertJ techniques for *FlowTest classes with mocked ports (strict stubs, precedence tests, exact translation checks, clock witnesses)
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
- A new `@Service` that needs a bean nobody provides yet breaks every full-context test; land the config class in the
  same task (spec 02 T3: `CatalogueFlow` needs the `Clock` bean from `ClockConfig`).
