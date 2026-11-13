---
name: tool-adapter-probes
description: Spring AI @Tool classes, ChatClient wiring and scripted chat-model doubles - catch, memory, stub-server mutants
applies-to:
  - bookstore/src/main/java/**/assistant/**
  - bookstore/src/test/java/**/assistant/**
metadata:
  type: feedback
---
Rule: for a `@Tool` class with a curated error helper, list EVERY `catch` clause (including those in private parse helpers) and prove each is entered by a test, with one mutant that changes only the catch type.
**Why:** a length guard in front of `UUID.fromString` made every malformed-id test row skip the `catch (IllegalArgumentException)`; the type-swap mutant stayed green. The fix is a row passing the guard and failing the parser: a 36-character non-UUID (`zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz`, a last-digit `g`); padded and 32-hex ids only exercise the guard.
**How to apply:**
- Probe guard and catch separately: `length < 36` survives as equivalent (the JDK rejects 37+ chars and the catch gives the same text), the tell that the catch is entered only when the guard is weakened.
- Mocked-flow search tests with an unstubbed extra row die with the generic "technical reason" text when a cap mutant pulls the 11th book (Mockito null, NPE, helper rewraps): the kill is real but not by the asserting line: report as observation, suggest stubbing the extra row (`lenient().when(...)`). Re-verify by the Failures/Errors split in the probe row.
- Prove a Required Action before sending it: patch the test copy from `.orig` (python3 -I, exact-once), install test plus survivor mutant, run, restore both by `cp -p`, print hashes beside snap-before.
- Spring AI's default exception processor returns only the message: assert texts through `callback.call` plus `DefaultToolExecutionExceptionProcessor.builder().build().process(e)` on the thrown `ToolExecutionException`.
- Families that paid off (one `python3 -I` generator plus cp-install runner, about 4 s per probe): cap +-1 and eager stock lookup, sort-before and sort-after-limit on every column, inStock at every site (`>=0`, `>1`, `onHand`, `!lowStock`, `|| pending`, status), comparator tiebreaks, clamp bounds, null limit, blank-genre guard (`isEmpty`, `trim().isEmpty`), helper catch order, suffix hard-coded or sorted, log level and throwable.

ChatClient wiring and scripted chat-model doubles (Spring AI 2.0.2):
- `MessageWindowChatMemory` moves its eviction cut forward to the next USER message, so with only complete Q/A turns a window of N+1 behaves like N (exact-history tests kill N-1 only). Witness: a scripted step throwing after the memory advisor stored the USER message (a failed call), then N/2 more turns; the dangling question shows in N+1.
- `ToolCallingChatOptions.builder().build()` has null tool callbacks when the client has no tools: a double streaming `getToolCallbacks()` NPEs and a "defaultTools dropped" mutant is killed only by that NPE (check the kill reason). Once the double is null-safe, the tools-dropped mutant must die on the tool-set assertion (Spring AI adds `No ToolCallback found`).
- Stub-server contract tests (real model client against a JDK `HttpServer`): probe the factory's `defaultOptions(...)` line with a perl one-liner generator over `.fixed` (removed, `thinkingAdaptive()`, `thinkingEnabled(1024)`, `.model(..)`, `.maxTokens(512)`, `.temperature(0.2)`, `.topP(0.9)`, empty builder), `clean` run each, `cp` restore plus hash. A guard test on model, max_tokens and sampling passes on the fix by design: only these mutants show it bites. Check the stub keys its response off the parsed request body, not a test flag.
Related: [[env-switch-mutant-runner]], [[prompt-text-probes]], [[shell-traps]]
