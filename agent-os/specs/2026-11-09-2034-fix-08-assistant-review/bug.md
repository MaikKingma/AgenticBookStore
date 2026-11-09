---
bug: fix-08-assistant-review
severity: S3
path: lite
status: open
closes: [spec 08 residual R1, spec 08 residual R22, README answer language, integration tests online under exported SPRING_AI variables]
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
- Action: T1 adds a failing-model test double and tests; T3 pins offline mode in `IntegrationTestBase` with a test
  property source (outranks the environment). Follow-up for Maik: a standards line "a test configuration that must
  hold whatever the shell exports belongs in `@TestPropertySource`, not in `application-test.yml`".

## Verification
- Regression tests red on unfixed -> green on fixed (impl `## Red evidence`, QA `## Negative control`)
- Blast-radius items closed or deferred
- `./mvnw clean verify` green within 60 seconds, plain and with a dummy `SPRING_AI_ANTHROPIC_API_KEY` exported in the
  child shell; the `IntegrationTestBase` subclasses green with both dummy SPRING_AI variables exported
  (`SPRING_AI_MODEL_CHAT` stays out of the full referee because of the deferred `AssistantConfigContextTest`, R17)
