---
name: tool-adapter-probes
description: Probing a Spring AI @Tool class with mocked flows - mutant families that matter, the parse-catch trap, NPE-masked kills, clean-run cost
metadata:
  type: feedback
---

Rule: for a `@Tool` class with a curated error helper, list EVERY `catch` clause (including ones inside private parse
helpers) and prove each is entered by a test, with one mutant that changes only the catch type.

**Why:** a length guard in front of `UUID.fromString` made every malformed-id test row skip the `catch (IllegalArgumentException)`;
the type-swap mutant stayed green. The fix is a row that passes the guard and fails the parser: a 36-character non-UUID
(`zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz`, a last-digit `g`). Padded ids and 32-hex ids only exercise the guard.
**How to apply:**
- Probe the guard and the catch separately: `length < 36` survives as equivalent (the JDK rejects 37+ characters and the
  catch gives the same text), which is the tell that the catch is entered only when the guard is weakened.
- Mocked-flow search tests with an unstubbed extra row die with the generic "technical reason" text when a cap mutant
  pulls the 11th book (Mockito returns null, NPE, helper rewraps). The kill is real but not by the asserting line; report it
  as an observation and suggest stubbing the extra row (`lenient().when(...)`, strict stubs would flag the correct tool).
  Re-verify by the Failures/Errors split in the probe row (Errors 0, `containsExactly` and `verify(never())` lines named).
- A long probe batch can stall when the laptop sleeps (log mtimes jump by an hour); each row restores its own file, so
  results stay valid, but keep heartbeating from `date -u`.
- Families that paid off (about 160 mutants, one `python3 -I` generator plus a cp-install runner, about 4.4 s per
  `clean test -Dtest=<Class>` run, 12 minutes in the background): cap +-1 and eager stock lookup, sort-before and
  sort-after-limit on every column, inStock at every site (`>=0`, `>1`, `onHand`, `!lowStock`, `|| pending`, status), comparator
  tiebreaks (id, isbn, price, author, case-insensitive, descending), clamp bounds, null limit, blank-genre guard
  (`isEmpty`, `trim().isEmpty`), helper catch order, suffix hard-coded or sorted, log level and throwable.
- Equivalent survivors to expect and reason once: `stock.bookId()` for `book.id()` (stock is named by the book id),
  `strip()` on domain-normalised strings, `toString()` for `toPlainString()` on scale-2 money, stackless exception variants.
- Prove a Required Action before sending it: patch the test copy from the `.orig` (python3 -I, assert exact-once), install
  test plus survivor mutant, run, restore both by `cp -p`, print both hashes next to snap-before.
- Spring AI default exception processor returns only the message: assert texts through `callback.call` plus
  `DefaultToolExecutionExceptionProcessor.builder().build().process(e)` on the thrown `ToolExecutionException`.
- A `catch (RuntimeException e)` C0 grep prints exactly one line in the D5 helper; WARN events kept off the console
  (`additive=false` on the logger plus a `ListAppender`) leave no line in the referee log, so the C0-6 "one WARN per call"
  accounting is 0 console lines by design.

Related: [[env-switch-mutant-runner]], [[shell-traps]]
