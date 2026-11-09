---
name: tool-adapter-probes
description: Probing Spring AI @Tool classes, ChatClient wiring and scripted chat-model doubles - mutant families, parse-catch trap, NPE-masked kills, memory-window snap trap
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

ChatClient wiring and scripted chat-model doubles (Spring AI 2.0.2):
- `MessageWindowChatMemory` moves its eviction cut forward to the next USER message, so with only complete Q/A turns a
  window of N+1 behaves like N (exact-history tests kill N-1 only). Witness: a scripted step that throws after the memory
  advisor stored the USER message (a failed model call), then N/2 more turns; the dangling question shows in N+1.
- `ToolCallingChatOptions.builder().build()` has null tool callbacks when the client has no tools; a double that streams
  `getToolCallbacks()` NPEs, and a "defaultTools dropped" mutant is then killed only by that NPE. Check the kill reason.
  Once the double is null-safe, its guard-removal mutant may die by the NPE in a no-tools test (the NPE is the
  defect), while the tools-dropped mutant must die on the tool-set assertion (Spring AI adds `No ToolCallback found`).
- Removing a bean method breaks compilation of tests that call it (`-Dtest` still compiles every test); drop only the
  `@Bean` to let the auto-configured bean take over, then pin the origin with `getFactoryBeanName()`.
- Memory advisor order 200 vs tool-calling advisor 300: `.order(ToolCallingAdvisor.DEFAULT_ORDER + 1)` puts TOOL messages
  into memory, a cheap advisor-order mutant. 2.0.2 has no default conversation id and no PromptChatMemoryAdvisor.
- Snapshot-and-restore around a chat call (catch at the controller boundary): one family per restore spelling (none,
  remove-last, clear-only, skip-when-empty), one per use-case type dropped from the pass-through multi-catch, log
  level/twice/no-throwable, detail or title from the cause. Remove-last dies only on a full-window conversation (tool
  messages never reach memory, so a tool-round case cannot see it). Dropping the cause from the translated exception
  survives by design (the handler never reads it; the WARN log carries it): report as advisory, not a gap of an AC.
- Runs with exported dummy `SPRING_AI_*` vars: set `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.1:9` and grep the log for
  `127.0.0.1:9` / `Connection refused` to prove escaped calls stayed local. To show a red there predates the task, rerun
  that class on the pre-fix bytes in the same env (script swap, `clean`, `cp -p` restore), then a plain referee.

Related: [[env-switch-mutant-runner]], [[shell-traps]]
