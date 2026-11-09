---
bug: fix-08-assistant-review
severity: S2
path: lite+research
status: open
closes: [spec 08 residual R1, spec 08 residual R22, README answer language, integration tests online under exported SPRING_AI variables, live follow-up rejected for a thinking signature (A2)]
---
# Bug: a failed live model call answers a generic 500 and leaves the question in the conversation

## Report
- Observed (fresh spec 08 review at a56dfbc, main session): with a key configured, anything the chat call throws
  (authentication, spend or rate limit at Anthropic, an Anthropic 5xx, a timeout, a connection failure) reaches
  Spring's default 500, and the unanswered question stays in the `MessageWindowChatMemory` of that conversation. The
  storefront shows the generic 500. The README says the assistant answers "in plain English", but the system prompt
  says "Answer in the language of the customer". Exporting `SPRING_AI_ANTHROPIC_API_KEY` or `SPRING_AI_MODEL_CHAT`
  in the shell puts the integration test context online, because environment variables outrank
  `application-test.yml`.
- Expected (decision D10, Maik 2026-11-09; spec 08 "Failed model call", spec 12 "Errors"): 503 ProblemDetail, code
  `assistant-unavailable`, title "Assistant unavailable", a detail that names no key and no internal message; the
  question does not stay in the conversation memory; the storefront shows "The assistant is unavailable right now,
  try again later" and the input stays usable. The README matches the prompt. No test can reach the API, whatever is
  exported.
- Environment: `factory` at 22d8702 (spec 08 merged as a56dfbc plus D10).
- First seen / last known good: spec 08 review; regression window: not a regression (the behaviour was unspecified
  until D10; spec 08 QA residuals R1 and R22 named it).
- Frequency: always, on every failed live call.
- Impact: S3. The assistant is unusable while the provider fails anyway; the generic 500 hides the reason and the
  orphaned question is sent again with the next one (two consecutive user turns). Workaround: a new conversation.
- Related: decisions.md D10; spec 08 QA ledger residuals R1, R2, R17, R22; spec 08 plan
  `agent-os/specs/2026-11-09-1104-08-bookstore-assistant/plan.md`.

## Reproduction
- Status: reproduced (by reading, confirmed by the spec 08 T3 failed-call witness recorded as R22).
- Recipe T1: a context with a chat model that throws on `call` (a `ScriptedChatModel` scripted to fail, or the
  web-slice `ChatClient` mock throwing); `POST /assistant/chat` with a valid payload. Today: no handler of
  `AssistantExceptionHandler` matches the exception, so the answer is Boot's 500; `MessageChatMemoryAdvisor` has
  already added the user message to the conversation before the model was called, and nothing removes it.
- Recipe T2: `AssistantPage` receives `503` with `code` `assistant-unavailable`; `shownProblem`
  (`frontend/src/pages/AssistantPage.tsx:18-26`) maps only 429 and `assistant-offline`, so the page shows the
  server's title and detail instead of the spec 12 text.
- Recipe T3a: `README.md:5` versus `bookstore/src/main/resources/prompts/assistant-system.st:6`.
- Recipe T3b: run `AssistantModeIntegrationTest` with `SPRING_AI_ANTHROPIC_API_KEY=sk-ant-dummy-not-a-key` exported
  in the child shell only: `AssistantModeConfig` sees a non-blank key, the Anthropic chat model is created and the
  offline assertions fail. With `SPRING_AI_MODEL_CHAT=anthropic` exported the mode switch is skipped.
- Regression-test layers: T1 web slice (`AssistantCommandsTest`, `@WebMvcTest`) for the 503 and end-to-end
  integration (`AssistantIntegrationTest`, `ScriptedChatModel`) for the memory, per architecture standard section 5
  and the layer-testing skill; T2 component test (`AssistantPage.test.tsx`, Vitest and Testing Library); T3b
  end-to-end integration (`IntegrationTestBase` subclasses) run with the two variables exported.
- Commands: `./mvnw -pl bookstore test -Dtest=<Class>[#method] -Dsurefire.failIfNoSpecifiedTests=false`; in
  `frontend/`, `npx vitest run src/pages/AssistantPage.test.tsx` with the pinned Node from `frontend/node`.

## Root cause
- Causal chain T1: failed live call -> the exception leaves `ChatClient.call().content()` ->
  **root cause (`bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantCommands.java:44-48`)**: the
  chat call at the use-case boundary of the assistant is neither translated into a use-case exception nor undone in
  the conversation memory, while `MessageChatMemoryAdvisor` (wired at `AssistantConfig.java:47`) stores the user
  message before the model answers -> `AssistantExceptionHandler` has no matching handler, Boot answers 500 and the
  question stays in memory -> missed because spec 08 left the live failure unspecified (R1, R22) and every test runs
  offline.
- Root cause (one sentence, T1): `AssistantCommands.chat` calls the chat client without catching its failure, so no
  `UnavailableException` is raised and the memory entry the advisor wrote before the call is never removed.
- Root cause T2: `frontend/src/pages/AssistantPage.tsx:18-26` has no branch for the code `assistant-unavailable`.
- Root cause T3a: `README.md:5` states a fixed answer language the prompt does not have.
- Root cause T3b: `bookstore/src/test/resources/application-test.yml:3-8` pins the key in a config file, which ranks
  below OS environment variables; `IntegrationTestBase.java:45-48` has no test property source that outranks them.
- Contributing factors: no test double for a failing model existed; the offline check covers only the no-key case.
- Introduced by: spec 08 (a56dfbc); intended: offline mode without a key; a live failure was out of its scope.

## Blast radius
- [x] Same pattern elsewhere: `git grep -n "chatClient" bookstore/src/main` hits only `AssistantCommands.java` and
  `AssistantConfig.java`; there is one chat call. -> task T1.
- [x] Other callers: `POST /assistant/chat` is the only entry; the storefront `AssistantPage` is its only client.
  -> tasks T1 and T2.
- [x] Persisted data already wrong: none. The chat memory is in memory only (`InMemoryChatMemoryRepository`) and
  is lost on restart. -> none.
- [x] Artifacts already produced: none.
- [x] Docs, tests that encode the wrong assumption: `README.md:5` (task T3); `application-test.yml` comment claims
  "no test can send a request to the real API" (task T3 makes it true).
- [x] Sibling test contexts: `AssistantConfigContextTest` builds an `ApplicationContextRunner` that reads the process
  environment (spec 08 residual R17): an exported `SPRING_AI_MODEL_CHAT` turns it red, but it holds no Anthropic
  auto-configuration and cannot reach the API. -> deferred: changing it means editing a pre-existing test's
  environment; reported as a follow-up. `@WebMvcTest` and `@DataJpaTest` slices load no Spring AI
  auto-configuration. -> none.
- [x] Deferred by the main session (not fixed here, listed in the report): R2 thinking tokens count toward
  max-tokens 1024; no tool-call ceiling; the chat memory has no lock for concurrent requests on one conversation id
  (the restore of T1 does not add one); no full-boot test that switches online with a key.

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | catch at the boundary, restore the memory snapshot, translate to `UnavailableException` |
| (b) revert | no | nothing to revert; the behaviour was never specified |
| (c) guarded workaround + follow-up | no | a generic `RuntimeException` handler in the advice would hide the memory defect |
Chosen: (a), because the boundary is the one place that knows both the conversation id and the failure.

## Prevention
- Gap: spec 08 left the live failure unspecified; offline-by-construction tests never exercise a failing model; the
  test configuration relied on a file that environment variables outrank.
- Action: T1 adds a failing-model test double and tests; T3 pins a blank key and the loopback base URL
  `http://127.0.0.1:9` in `IntegrationTestBase` with a test property source (outranks the environment), so the
  production offline switch decides offline mode and no request can leave the machine (plan.md amendment A4). Follow-up for Maik: a standards line "a test configuration that must
  hold whatever the shell exports belongs in `@TestPropertySource`, not in `application-test.yml`".

## Amendment A2 (2026-11-09): every second message of a conversation fails live (task T4, S2)

### Report
- Observed (main session, owner's live-key check of spec 08 at a56dfbc, default model Haiku 5.5, 2026-11-09 21:36
  local): the first message of a conversation answers 200 with grounded answers; every second message in the same
  conversation fails in about 0.25 s, with or without tool use ("Hello, what can you do?" then "And what genres do
  you have?"). Log: `com.anthropic.errors.BadRequestException: 400 ... messages.1.content.1: Invalid signature in
  thinking block. The block is bound to a different conversation ... Content before this block was rewritten since
  it was created, first at messages.1.content.0.` (request ids req_011CgsziS4C1e2YPNbGRsTaz,
  req_011CgszjYwiWjrh8uoZh5e1m, req_011CgszjhdbCTcbHQNNZzR6w). Stack: AnthropicChatModel.call (spring-ai-anthropic
  2.0.2) <- ChatModelCallAdvisor <- ToolCallingAdvisor <- MessageChatMemoryAdvisor <- AssistantCommands.chat.
- Expected: spec 08 "Conversation memory", scenario "Follow-up question": the follow-up answers 200 with the earlier
  messages of the conversation.
- Severity: S2 (blocks the spec 08 conversation memory scenario live). Regression window: not a regression (since
  spec 08; offline tests use `ScriptedChatModel` and never see thinking blocks).
- With T1 in place the same defect shows as 503 `assistant-unavailable` on turn 2 (memory restored) instead of 500.

### Reproduction
- Status: reproduced offline by res-1 (`.claude/comms/mailboxes/res-1/outbox/research-t4-thinking-replay.md`) with the
  real `AnthropicChatModel`, `MessageChatMemoryAdvisor` and `MessageWindowChatMemory` against a local JDK
  `HttpServer` stub (no key, no real call): the turn-2 request body was `[user, assistant [text, thinking(SIG1)],
  assistant "<thinking text>", user]`, where the stub had answered `[thinking(SIG1), text]`.
- Regression-test layer: an HTTP contract test of the real chat client against a local stub server (the acl-style
  contract layer of architecture standard section 5 applied to the assistant's outbound model call; the offline
  `ScriptedChatModel` cannot see it). Test file:
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantAnthropicContractTest.java`, run with
  `./mvnw -pl bookstore test -Dtest=AssistantAnthropicContractTest -Dsurefire.failIfNoSpecifiedTests=false`.

### Root cause
- Causal chain: turn 2 fails with 400 -> the replayed assistant turn starts with text although the API answered with
  thinking first, so the thinking signature no longer matches its prefix -> spring-ai-anthropic 2.0.2 writes a
  stored assistant message as text, then thinking, then tool_use (`AnthropicChatModel.java:846-851`), and
  `MessageChatMemoryAdvisor.java:138-146` also stores the extra thinking `Generation` built at
  `AnthropicChatModel.java:1100-1110` as a plain assistant message; the same reorder happens inside one call in the
  tool loop when the model writes text before a tool_use -> **root cause in this repository
  (`bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantConfig.java:45-48`)**: the assistant's chat
  client leaves thinking unset, so Haiku 5.5 answers with signed adaptive-thinking blocks that Spring AI 2.0.2 cannot
  replay in order -> missed because every test runs on `ScriptedChatModel` or offline.
- Root cause (one sentence): `AssistantConfig.assistantChatClient` builds the chat client without a thinking option,
  so the model's default adaptive thinking produces signed thinking blocks that the pinned Spring AI version replays
  reordered and duplicated.
- Contributing factor (framework, not patched here): the replay order in `AnthropicChatModel` 2.0.2.

### Blast radius
- [x] Same pattern elsewhere: one chat client (`git grep -n "ChatClient" bookstore/src/main` hits only the
  assistant package). -> task T4.
- [x] Tool loop inside one call (same reorder on text before tool_use). -> task T4 (covered by the same option).
- [x] Persisted data: none (memory in process only). Produced artifacts: none.
- [x] Docs: spec 08 Configuration lists model and max-tokens only; the thinking setting is reported to the main
  session as a product-behaviour change (plan.md D7); `docs/spec-pack/` is not edited here.

### Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) strip thinking blocks from the replayed history (memory decorator) | partly | does not fix the in-call tool-loop reorder |
| (b) thinking disabled on the assistant's chat client | yes | no thinking blocks at all; frees max-tokens for the answer (R2); lower latency; product change beyond spec 08 |
| (c) `drop_block` beta header | no | depends on a beta header and keeps the duplicated thinking message |
Chosen: (b), in `AssistantConfig` (the property cannot bind `ThinkingConfigParam`; res-1 probe), because it removes the
cause for memory replay and tool loop alike without a beta header.

### Prevention
- Gap: no test drives the real Anthropic chat model; offline doubles cannot show provider-shape defects.
- Action: T4 adds the stub-server contract test. Follow-up for the main session: a standards line "an LLM adapter
  has one contract test of the real model client against a local stub server".

## Amendment A3 (2026-11-09): answer-quality defects from the live check (task T5, S3)

### Report
- Observed (main session, owner's live check of cc6d658, three conversations, all answers 200):
  1. Conversation 7e5d5bb7: turn 1 "Anything by Austen?" found Sense and Sensibility; turn 2 "Is it in stock, and is
     more on its way?" answered that the search returned no results and doubted its earlier answer; turn 3 found it
     again.
  2. Conversation 172a2b6a, turn 1: the assistant offered book "descriptions", which the catalogue does not have.
  3. "And what genres do you have?" answered that it cannot list genres.
- Expected: spec 08 "Grounded answers" (answers about the catalogue through the tools) and the system prompt rules:
  a follow-up finds the book again; the assistant offers only what the tools return; it can name the genres.
- Severity S3 (degraded answers; the customer can rephrase). Owner's decision: fix now, in this cycle.

### Reproduction and root cause
- Status: reproduced live by the main session (transcript on PR #111); mechanism confirmed by reading.
- Finding 1: `AssistantConfig` keeps only the questions and final answers in memory (`AssistantConfig.java`, the
  `MEMORY_MESSAGES` comment: "never tool calls or tool results"), so on turn 2 the model has no book id and searches
  again with a combined phrase; spec 02's `q` criterion matches title OR author by contains, so "title author" finds
  nothing. Root cause in the assistant layer: the `searchCatalogue` description (`BookstoreTools.java`) does not say
  that the query is one title or one author, and the system prompt (`assistant-system.st`) does not tell the model to
  look a book up again by title on a follow-up.
- Finding 2: `assistant-system.st` does not say what the tools return, so the model offers fields that do not exist.
- Finding 3: `assistant-system.st` does not name the genres and no tool lists them.
- Regression-test layer: prompt and tool-description statement tests (unit, `AssistantSystemPromptTest`,
  `BookstoreToolsTest`) plus a scripted end-to-end guard (`AssistantIntegrationTest`); model behaviour itself is
  live-only (spec 08 residual R5).

### Blast radius
- [x] One prompt, one search tool. -> task T5.
- [x] Spec 02 search contract: unchanged by decision (main session). -> none.
- [x] Memory window keeps final texts only: unchanged (plan D8). -> none.

### Fix strategy
Prompt and tool description texts (plan D8). Not chosen: keeping tool calls in memory (replay risk, see A2); changing
the spec 02 search (out of scope by the owner's decision).

## Verification
- Regression tests red on unfixed -> green on fixed (impl `## Red evidence`, QA `## Negative control`)
- Blast-radius items closed or deferred
- `./mvnw clean verify` green within 60 seconds, plain and with a dummy `SPRING_AI_ANTHROPIC_API_KEY` exported in the
  child shell; the `IntegrationTestBase` subclasses green with both dummy SPRING_AI variables exported
  (`SPRING_AI_MODEL_CHAT` stays out of the full referee because of the deferred `AssistantConfigContextTest`, R17)
