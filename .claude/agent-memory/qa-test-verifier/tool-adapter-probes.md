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
- Memory advisor order 200 vs tool-calling advisor 300: `.order(ToolCallingAdvisor.DEFAULT_ORDER + 1)` puts TOOL messages
  into memory, a cheap advisor-order mutant. 2.0.2 has no default conversation id and no PromptChatMemoryAdvisor.

Related: [[env-switch-mutant-runner]], [[shell-traps]]
