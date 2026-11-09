---
spec: fix-08-assistant-review
source: docs/spec-pack/specs/08-bookstore-assistant.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-09T20:34:06Z
origin: fresh spec 08 review at a56dfbc (main session); decision D10 (Maik 2026-11-09); spec 08 residuals R1 and R22
gate1: 2026-11-09T20:36:30Z
qa-lead: on
base: factory
branch: sdd/fix-08-assistant-review/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
kind: bugfix
severity: S2
path: lite+research
fix-strategy: root-cause
regression-since: not-a-regression
closes: [R1 failed live call gives a generic 500, R22 failed question stays in memory, README answer language, integration tests online under exported SPRING_AI variables, live follow-up rejected for a thinking signature]
---

# Plan: a failed live model call answers 503 assistant-unavailable and leaves the conversation as it was

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
ROOT = the repository root. FE = `frontend/`. All commands run from ROOT unless they say `in FE`.
`<pr-base>` = `origin/sdd/fix-08-assistant-review/spec` at the moment the task branch was cut.

## Ground rules

- The pack specs win: `docs/spec-pack/specs/08-bookstore-assistant.md` (requirement "Failed model call", API table,
  configuration without temperature), `docs/spec-pack/specs/12-storefront.md` ("Errors"),
  `docs/spec-pack/orchestrator/decisions.md` D10, `docs/spec-pack/product/architecture-standard.md`. Bookstore Java
  goes through the `hexagonal-slice` skill (the `assistant` package is the inbound conversational adapter; steps 0, 7
  and 8 apply). No new dependency, no POM change, version pins as they are. Spring AI types only in `assistant`.
  Nothing under `docs/spec-pack/` is edited.
- The 429 (`AssistantRateLimiter`, Spring's `ErrorResponseException`) and the `assistant-offline` 503 stay exactly as
  they are, in the bookstore and in the storefront.
- Never skip, disable, delete or weaken a test or an ArchUnit rule. Never `--no-verify`. Pre-existing test files
  change only where a criterion names the change (setup lines); new tests are appended.
- Secrets: never set, read or write `ANTHROPIC_API_KEY`; never export a real key; no test or probe calls the real
  Anthropic API. Every run with exported SPRING_AI variables sets them only in the child shell of that one command,
  with dummy values (`SPRING_AI_ANTHROPIC_API_KEY=sk-ant-dummy-not-a-key`), and also exports
  `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.1:9` so that a call that escapes fails locally.
- Every `mktemp` uses an explicit template under `$TMPDIR` or the scratchpad. No `docker compose down -v`; never touch
  the shared `bookstore-postgres-1`. Never run `claude plugin` commands.
- ASCII only in every file touched; docs and comments in plain English without em dashes.
- Referee: `./mvnw clean verify` from ROOT, green, within 60 seconds. Base referee on 22d8702 (orchestrator,
  2026-11-09T20:33Z): 34.3 s; Vitest 22 files, 598 passed + 2 expected fail (600); bookstore 1314; distributor 190.
- Implementers do not git commit, branch or stash; the orchestrator owns git. Implementers never mutate production
  files outside their fileScope to self-probe: describe each mutant in the outbox, the verifier runs it.
- Evidence files in the shared scratchpad carry the role and task prefix (`f08-t1-impl-*`, `f08-t2-qa-*`).
- After 3 failures on one item the item is blocked and the cycle stops with a needs-decision.

## Path ruling (reference.md section 6)

Lite path (the default) - all hold:
- [x] reproduced in 1a.2: by reading `AssistantCommands.java:44-48` and `AssistantExceptionHandler` (no handler for
  a chat failure), confirmed by the spec 08 T3 failed-call witness (R22: the advisor stores the question before the
  call); T2 by reading `AssistantPage.tsx:18-26`; T3a by `README.md:5` vs `assistant-system.st:6`; T3b by the
  property ranking (OS environment above config data) and `AssistantModeConfig.java:27-33`
- [x] root cause named at file:line and confirmed: bug.md Root cause
- [x] one obvious fix design readable from the code: snapshot and restore the conversation memory around the one chat
  call and translate its failure into `UnavailableException`; one more `shownProblem` branch; one README sentence; a
  `@TestPropertySource` on the shared integration test base
- [x] blast radius enumerable by grep now: bug.md Blast radius (one chat call, one client page; R17 deferred)
- [x] no already-wrong persisted data or produced artifacts (memory is in-process only)
- [x] not S1; not money, numbering or auth posture (the API key is not handled by the fix); the memory lock for
  concurrent requests stays deferred by the main session
Bundled owner decisions: D10 storefront text -> own task T2; README and test isolation -> own task T3.
Open question the code does not settle in minutes: none.
-> path: lite (no lead, no research fan-out; three tasks; QA lead registered at Phase 2/3). The main session set T1
to qaTier critical with opus implementer and verifier; T2 and T3 are elevated with sonnet.

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (T1): the catch sits in `AssistantCommands.chat` around the one chat call, the use-case boundary of the
  assistant (the architecture standard translates failures at the use-case boundary; the assistant adapter has no
  flow). It catches `RuntimeException`, restores the conversation memory to the snapshot taken right before the call,
  and throws `UnavailableException` with code `assistant-unavailable`, title "Assistant unavailable", detail "The
  assistant is unavailable right now, try again later" (the spec 12 text; it names no key and no internal message),
  the failure kept as the cause. `AssistantExceptionHandler` answers it as it answers every `UnavailableException`;
  it is not changed. `Error`s are not caught.
- D2 (T1): the memory is restored by snapshot, not by removing the last message: the advisor's `add` can evict the
  oldest message of a full window of 20, and a failure can come after a tool round. `AssistantCommands` receives the
  `ChatMemory` bean in its constructor. That changes the setup of three pre-existing test sites (the two
  `@WebMvcTest` bean sets in `AssistantCommandsTest` and the constructor call in `AssistantCommandsLengthTest`); those
  setup lines are the only permitted change to pre-existing test files. Because the constructor call no longer
  compiles against the unfixed controller, the negative control restores `AssistantCommandsLengthTest.java` from the
  PR base together with the production file (the new tests compile either way).
- D3 (T1): the failure is logged once at WARN by `AssistantCommands`, with the cause attached, so the facilitator still
  sees why the live call failed (today Boot logs the unhandled exception; a handled 503 would otherwise be silent).
- D4 (T2): `assistant-unavailable` is recognised by its code, like `assistant-offline`; the 429 check stays first.
  The page shows only the spec 12 text, as it does for offline (server title and detail are not shown).
- D5 (T3b; superseded in part by amendment A4: the base pins the blank key and a loopback base URL, not the model):
  offline mode is pinned with `@TestPropertySource(properties = {"spring.ai.model.chat=none",
  "spring.ai.anthropic.api-key="})` on `IntegrationTestBase`: inlined test properties outrank OS environment
  variables, and a subclass's own `@TestPropertySource` (`AssistantIntegrationTest`: `scripted`) still wins over the
  inherited value. `application-test.yml` stays (it also covers contexts outside the base); only its comment is made
  true. `AssistantConfigContextTest` (R17) is deferred: it cannot reach the API.
- D7 (T4, amendment A2): the assistant's chat client runs with thinking disabled
  (`defaultOptions(AnthropicChatOptions.builder().thinkingDisabled())` in `AssistantConfig`; the property cannot bind).
  It removes the signed thinking blocks Spring AI 2.0.2 replays reordered, in memory and in the tool loop, needs no
  beta header, and leaves all of max-tokens 1024 to the answer. Product-behaviour change beyond spec 08, reported.
- D8 (T5, amendment A3): the `searchCatalogue` description keeps the spec 08 sentence unchanged and appends the
  one-title-or-one-author rule (the smallest deviation from the spec 08 tool table text, reported to the main session);
  the follow-up rule, the returned fields and the genre list go into the system prompt next to the spec 08 rules. The
  spec 02 search contract and the memory window stay as they are (keeping tool calls in memory would be a larger
  change with the replay risks of A2).
- D6: models per the main session: opus for T1 (implementer and verifier), the QA lead and qa-final; sonnet for T2
  and T3. Never fable.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | status | verdict | qa-lead | pr | branch |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | a failed chat call answers 503 `assistant-unavailable` and leaves the conversation memory as it was | bookstore | [] | 1 | critical | opus | done | PASS r0 | ENDORSE | #134 | sdd/fix-08-assistant-review/T1 |
| T4 | the assistant's chat client runs with thinking disabled, so a follow-up turn replays no thinking block (A2) | bookstore | [T1] | 2 | critical | sonnet | done | PASS r0 | ENDORSE | #138 | sdd/fix-08-assistant-review/T4 |
| T2 | the assistant page shows "The assistant is unavailable right now, try again later" for `assistant-unavailable` | frontend | [] | 3 | elevated | sonnet | done | FAIL r0, PASS r1 | ENDORSE | #141 | sdd/fix-08-assistant-review/T2 |
| T3 | README matches the prompt's answer language; integration tests stay offline whatever is exported | bookstore | [] | 4 | elevated | sonnet | done | PASS r0 | ENDORSE | #143 | sdd/fix-08-assistant-review/T3 |
| T5 | the assistant finds a book again on a follow-up, names only what the tools return and lists the genres (A3) | bookstore | [T3] | 5 | elevated | sonnet | pending | | | | |

T1, T4, T3 and T5 share the bookstore module and run one after the other; the batches fix the order T1, T4, T2, T3,
T5 (amendments A2 and A3). T5 depends on T3 for ordering only. T4 depends on T1 because its red evidence names T1's 503 shape. T4 is critical with a sonnet
verifier: the main session's model rule (opus only for T1, the QA lead and qa-final) wins over the tier mapping.

### T1: a failed chat call answers 503 assistant-unavailable and leaves the memory as it was

- fileScope: `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantCommands.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantCommandsTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantCommandsLengthTest.java` (constructor call only),
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/ScriptedChatModel.java` (additive: a failing step),
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/ScriptedChatModelTest.java` (tests of that step),
  `bookstore/src/test/java/nl/openvalue/bookstore/AssistantIntegrationTest.java`
- Acceptance criteria:
  1. Regression test `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantCommandsTest.java` > a new
     test (for example `failedChatCallAnswersAssistantUnavailable`, parameterised) exists at the web slice layer (per
     architecture standard section 5 and the layer-testing skill), is named for the correct behaviour (not the bug
     id), and asserts the expected behaviour stated in bug.md Reproduction: with the mocked chat call throwing, for
     each of at least three unrelated exception types (`org.springframework.ai.retry.NonTransientAiException`,
     `org.springframework.ai.retry.TransientAiException`, `java.lang.IllegalStateException`), `POST /assistant/chat`
     with a valid payload answers 503, content type `application/problem+json`, `status` 503, `code`
     `assistant-unavailable`, `title` "Assistant unavailable", `detail` "The assistant is unavailable right now, try
     again later", `details` an empty list; the whole body contains neither the cause's message (a witness message
     that holds `sk-ant-dummy-not-a-key` and the word `upstream`) nor the cause's class name.
  2. Regression test `bookstore/src/test/java/nl/openvalue/bookstore/AssistantIntegrationTest.java` > a new test (for
     example `failedModelCallLeavesNoEntryForTheQuestion`) exists at the end-to-end integration layer, is named for
     the correct behaviour, and asserts, with `ScriptedChatModel` scripted to throw, for three conversations: (a) a
     new conversation: 503 `assistant-unavailable` and `ChatMemory.get(id)` is empty; (b) a conversation with one
     answered turn: 503 and the memory holds exactly the two earlier messages (type and text, in order); (c) a
     conversation holding a full window of 20 messages: 503 and the memory holds exactly those 20 messages (none
     evicted). A fourth conversation with one answered turn is unchanged by all three failures.
  3. A follow-up in conversation (b) after the failure answers 200, and the model receives the earlier turn and the
     new question only: the failed question text appears in no prompt after the failure
     (`ScriptedChatModel.received()`), and afterwards the memory holds the two earlier messages plus the new question
     and its answer.
  4. A failure after a tool round (the scripted model first requests `searchCatalogue`, then throws) also answers 503
     `assistant-unavailable` and leaves that conversation's memory exactly as before the request.
  5. The failure is logged once at WARN by `AssistantCommands` with the cause attached: a slice test with
     `OutputCaptureExtension` (or a log appender) finds exactly one WARN line from `AssistantCommands` for one failed
     request and the cause's class name in that log output.
  6. Unchanged behaviour: the 429 (`ErrorResponseException`, `TOO_MANY_REQUESTS`) and the `assistant-offline` 503 keep
     their existing tests green and unmodified; a rate-limited request still does not call the chat client; a
     successful turn still stores the question and the answer; the ProblemDetail of a failed call has the same shape
     (status, title, detail, code, details) as the offline one.
  7. Negative control: on the pre-fix code (`AssistantCommands.java` and the setup-only `AssistantCommandsLengthTest.java`
     restored from `git show <pr-base>:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or
     `git stash`), `./mvnw -pl bookstore test -Dtest='AssistantCommandsTest,AssistantIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false`
     FAILS on exactly the new tests of criteria 1 to 5 (no pre-existing test red) with: for criterion 1 the unhandled
     exception escaping MockMvc (`jakarta.servlet.ServletException: Request processing failed` wrapping the scripted
     exception, the slice form of Boot's 500); for criteria 2 and 4 `expected: 503 but was: 500` or, where the status
     is not asserted first, a memory holding the failed question where it must not; for criterion 3 the failed
     question in the follow-up prompt; for criterion 5 no WARN line - not with a compile, import or context error. With
     the fix in place it PASSES. Both outputs are recorded verbatim in the outbox artifact under `## Red evidence` and
     `## Green evidence`.
  8. The fix sits at the root cause: the catch, the snapshot and the restore are in `AssistantCommands.chat` around
     the one chat call; `AssistantExceptionHandler`, `AssistantConfig` and `OfflineChatModel` are unchanged; no new
     top-level class (naming rule); ArchUnit green unmodified.
  9. No behaviour change outside the root cause: `./mvnw -pl bookstore test` green (1314 + the new tests); the
     production diff is confined to `AssistantCommands.java`; the pre-existing test files differ from `<pr-base>` only
     by appended tests, the `ChatMemory` entry in the two `@WebMvcTest` bean sets of `AssistantCommandsTest` and the
     constructor argument in `AssistantCommandsLengthTest` (`git diff <pr-base> -- <file>` quoted in the outbox); no
     test method, assertion, literal or display name of a pre-existing test changes.
  10. `./mvnw clean verify` is green within 60 seconds.
- Fix design (readable from the code, the implementer confirms it): `AssistantCommands` takes the `ChatMemory` bean;
  after `rateLimiter.acquire()` it snapshots `chatMemory.get(id)` (a copy), calls the chat client, and on
  `RuntimeException` clears the conversation and adds the snapshot back (when not empty), logs WARN with the cause,
  and throws the `assistant-unavailable` `UnavailableException` with the cause. The class comment names the new step.

### T4: the assistant's chat client runs with thinking disabled (amendment A2)

- fileScope: `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantConfig.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantAnthropicContractTest.java` (new)
- Acceptance criteria:
  1. Regression test `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantAnthropicContractTest.java` >
     a follow-up turn test (for example `followUpTurnReplaysNoThinkingBlock`) exists at the HTTP contract layer (the
     real `AnthropicChatModel` against a local stub server; per bug.md A2 Reproduction), is named for the correct
     behaviour (not the bug id), and asserts: the `ChatClient` comes from the production factory
     `AssistantConfig.assistantChatClient(...)` (same package; `BookstoreTools` with mocked flows, the real
     `assistantChatMemory()`), never from a copy of its builder chain; the model is built by hand with a literal
     dummy key, base URL `http://127.0.0.1:<port>` of a JDK `com.sun.net.httpserver.HttpServer` on port 0, the model
     and max-tokens of `application.yml` (`claude-haiku-5-5`, 1024). The stub answers like the real API: a request
     without `"thinking":{"type":"disabled"}` gets `[thinking + signature, text]`, a request with it gets `[text]`.
     Two turns in one conversation: every captured request carries `thinking.type == "disabled"`; the turn-2 request
     carries the turn-1 answer text and no `thinking` block and no assistant message other than the turn-1 answer.
  2. Tool-use variant (for example `toolRoundReplaysNoThinkingBlock`): the stub answers turn 1 without the disabled
     setting with `[thinking, text, tool_use searchCatalogue]`, then `[thinking, text]`; with the setting
     `[text, tool_use]`, then `[text]`. At least 3 captured requests (turn 1, the tool loop, turn 2); every request
     carries `thinking.type == "disabled"`; no request holds a thinking block; the tool result reaches the model.
  3. Every request still carries the model and `max_tokens` of the configuration and no sampling parameter
     (`temperature`, `top_p`, `top_k`).
  4. Negative control: on the pre-fix code (`AssistantConfig.java` restored from `git show <pr-base>:<file>` via
     copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), `./mvnw -pl bookstore test
     -Dtest=AssistantAnthropicContractTest -Dsurefire.failIfNoSpecifiedTests=false` FAILS on the tests of criteria
     1 and 2 with a request lacking `thinking.type == "disabled"` and, where asserted, a turn-2 body holding an
     assistant message `[text, thinking]` (the reorder shape of bug.md A2) - not with a compile, bind or context
     error. With the fix in place it PASSES. Outputs verbatim under `## Red evidence` and `## Green evidence`.
  5. The fix sits at the root cause: `AssistantConfig.assistantChatClient` sets
     `defaultOptions(AnthropicChatOptions.builder().thinkingDisabled())` (or the equivalent 2.0.2 API res-1 verified);
     the class comment says why in plain English (the pinned Spring AI replays thinking blocks reordered, the API
     then rejects the follow-up); no property, no beta header, no memory decorator, no change to `AssistantCommands`.
  6. No behaviour change outside the root cause: the default options reach `ScriptedChatModel` and `OfflineChatModel`
     harmlessly; `AssistantIntegrationTest`, `ScriptedChatModelTest`, `AssistantCommandsTest`,
     `AssistantModeIntegrationTest`, `AssistantConfigTest` and `AssistantConfigContextTest` pass unmodified, and the
     tool tests still see their tool names; `./mvnw -pl bookstore test` green; ArchUnit green unmodified; no POM
     change.
  7. The outbox states whether spec 08 residual R2 (thinking tokens counting toward max-tokens 1024) is closed.
  8. `./mvnw clean verify` is green within 60 seconds.
- Live re-check (not a criterion of the task; the main session runs it with the owner's key after the merge): the
  two-message conversation answers 200 twice.

### T2: the assistant page shows the unavailable text

- fileScope: `frontend/src/pages/AssistantPage.tsx`, `frontend/src/pages/AssistantPage.test.tsx`
- Acceptance criteria:
  1. Regression test `frontend/src/pages/AssistantPage.test.tsx` > "shows that the assistant is unavailable right now
     when it answers 503 assistant-unavailable" (new tests appended) exists at the component layer (per spec 12 test
     plan: Vitest and Testing Library), is named for the correct behaviour (not the bug id), and asserts the expected
     behaviour: a 503 `application/problem+json` with `code` `assistant-unavailable`, `title` "Assistant unavailable"
     and a server detail shows the alert text "The assistant is unavailable right now, try again later"; the server
     title and the server detail are not shown (asserted after the awaited presence of the text).
  2. The input stays usable: after that answer the message field is enabled and still holds the typed message, the
     Send button is enabled, nothing joins the conversation list, and a second send posts again and shows the reply
     of a 200 (the alert is gone).
  3. Recognised by the code, never by the status: a 500 or 502 with `code` `assistant-unavailable` shows the same text;
     a 503 with another code (for example `distributor-unavailable`) or without a code shows the server's own title as
     before; a 429 whose body has `code` `assistant-unavailable` still shows "Too many requests, try again in a minute".
  4. Unchanged: the existing `assistant-offline` and 429 tests stay green and unmodified (`git diff <pr-base> --numstat
     -- frontend/src/pages/AssistantPage.test.tsx` shows 0 deletions); ProblemDetail parsing (`api/client.ts`,
     `pages/problem.ts`, `ProblemDetailAlert`) is unchanged; no style change and no colour literal (tokens.css only).
  5. Negative control: on the pre-fix code (`AssistantPage.tsx` restored from `git show <pr-base>:<file>` via copy-aside
     + SHA-256; never `git checkout --`, `git restore` or `git stash`), `npx vitest run src/pages/AssistantPage.test.tsx`
     (in FE, pinned Node from `frontend/node`) FAILS on the tests of criteria 1 and 3 (the `assistant-unavailable`
     cases) with `Unable to find an element with the text: The assistant is unavailable right now, try again later`
     (or the equivalent missing-text failure) - not with a compile, import, type or fixture error - and on no
     pre-existing test; tests of criterion 2 and the other-code cases of criterion 3 may pass before. With the fix in
     place it PASSES. Both outputs are recorded verbatim under `## Red evidence` and `## Green evidence`.
  6. The fix sits at the root cause: one branch in `shownProblem` (`AssistantPage.tsx`), its comment updated to name
     three failures with a text of their own; no other production file changes.
  7. No behaviour change outside the root cause: all 600 baseline Vitest tests pass (2 of them the expected-fail
     `it.fails`) plus the new ones; no console error or warning (the setupTests trap); `npm run build` (tsc and Vite)
     green; no new npm dependency.
  8. `./mvnw clean verify` is green within 60 seconds.

### T3: README matches the prompt; integration tests stay offline whatever is exported

- fileScope: `README.md`, `bookstore/src/test/java/nl/openvalue/bookstore/IntegrationTestBase.java`,
  `bookstore/src/test/resources/application-test.yml` (comment only),
  `bookstore/src/test/java/nl/openvalue/bookstore/IntegrationTestOfflineTest.java` (new)
- Acceptance criteria:
  1. `README.md` says the assistant answers in the language of the customer (matching
     `bookstore/src/main/resources/prompts/assistant-system.st:6`) instead of "in plain English"; `git diff <pr-base> --
     README.md` changes only that sentence; ASCII, no em dash. `git grep -n -i "plain English" -- README.md` prints
     nothing.
  2. (amended A4) Regression test `bookstore/src/test/java/nl/openvalue/bookstore/IntegrationTestOfflineTest.java`
     (extends `IntegrationTestBase`, no own property source) exists at the end-to-end integration layer and asserts:
     the environment resolves `spring.ai.anthropic.api-key` to the empty string and `spring.ai.anthropic.base-url` to
     `http://127.0.0.1:9`, both from a property source that precedes `systemEnvironment` in the environment's property
     sources; `spring.ai.model.chat` resolves to `none` from `AssistantModeConfig`'s own source (the production
     offline switch still runs); the only chat model is `OfflineChatModel` and there is no `AnthropicChatModel` bean.
  3. (amended A4) `IntegrationTestBase` carries the pin as a test property source (`spring.ai.anthropic.api-key=` and
     `spring.ai.anthropic.base-url=http://127.0.0.1:9`; NOT `spring.ai.model.chat`, plan D5 as amended) and its class
     comment says why (environment variables outrank `application-test.yml`; a blank key keeps the production offline
     switch in charge; the loopback base URL means no request can leave the machine even when a developer exports a
     model choice); `AssistantIntegrationTest` still runs on `ScriptedChatModel`; `AssistantModeIntegrationTest`
     passes unmodified, `offlineSwitchComesAfterTheConfigurationFiles` included; the `application-test.yml` comment no
     longer claims more than the file does.
  4. (amended A4) Exported proof, child shell only, dummy values: (a) `SPRING_AI_ANTHROPIC_API_KEY=sk-ant-dummy-not-a-key
     SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.2:9 ./mvnw -pl bookstore test
     -Dtest='IntegrationTestOfflineTest,AssistantModeIntegrationTest,AssistantIntegrationTest'
     -Dsurefire.failIfNoSpecifiedTests=false` is green with the fix (offline, scripted; the pinned base URL wins over
     the exported one), and its log holds zero `Failed to connect` lines; (b) information, not a green criterion: the
     same run with `SPRING_AI_MODEL_CHAT=anthropic` added: report which tests fail and prove from the log that every
     attempted connection went to `127.0.0.1:9` (count of `Failed to connect to /127.0.0.1:9` lines; zero lines naming
     any other host). An explicitly exported model choice is honoured by design (spec 08 residual R17); the pin only
     guarantees that nothing reaches the API.
  5. (amended A4) Negative control: with `IntegrationTestBase.java` restored from `git show <pr-base>:<file>` via
     copy-aside + SHA-256 (never `git checkout --`, `git restore` or `git stash`), (a) plain `./mvnw -pl bookstore test
     -Dtest=IntegrationTestOfflineTest -Dsurefire.failIfNoSpecifiedTests=false` FAILS on the property-source or
     base-URL assertion of criterion 2; (b) with the variables of criterion 4a exported and
     `-Dtest='IntegrationTestOfflineTest,AssistantModeIntegrationTest#startsOfflineWithoutAKey'` (bean and environment
     checks only; no test in this selection posts to the chat endpoint) FAILS with the key resolving to the dummy
     value, `spring.ai.model.chat` not `none`, or an `AnthropicChatModel` bean where none is expected - not with a
     compile or context error. With the fix in place both PASS. Outputs verbatim under `## Red evidence` and
     `## Green evidence`.
  6. No behaviour change outside the root cause: `./mvnw -pl bookstore test` green; pre-existing test files unchanged
     except `IntegrationTestBase.java` (annotation and comment only) and the `application-test.yml` comment; no
     production file changes.
  7. `./mvnw clean verify` is green within 60 seconds, plain and with only `SPRING_AI_ANTHROPIC_API_KEY=sk-ant-dummy-not-a-key
     SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.2:9` exported in the child shell (amended A4; `SPRING_AI_MODEL_CHAT`
     stays out of the full run because of the deferred `AssistantConfigContextTest`, R17); the exported run's log holds
     zero `Failed to connect` lines.

### T5: the assistant finds a book again on a follow-up, names only what the tools return and lists the genres (amendment A3)

- fileScope: `bookstore/src/main/resources/prompts/assistant-system.st`,
  `bookstore/src/main/java/nl/openvalue/bookstore/assistant/BookstoreTools.java` (the `searchCatalogue` description
  and its `@ToolParam` descriptions only), `bookstore/src/test/java/nl/openvalue/bookstore/assistant/AssistantSystemPromptTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/assistant/BookstoreToolsTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/AssistantIntegrationTest.java` (appended test only),
  one new test class if a genre check needs access the `assistant` test package lacks (name it in the outbox)
- Acceptance criteria:
  1. Regression test `AssistantSystemPromptTest` > new tests pin these statements of `assistant-system.st` (red on the
     current prompt): (a) what the tools return, by name: title, authors, genre, price in EUR, publication year, the
     number of copies in stock and whether a restock is on its way, and that the catalogue holds nothing else (no
     descriptions, summaries or reviews), so the assistant offers nothing else; (b) on a follow-up about a book named
     earlier in the conversation, look the book up again by its title (a title-only search) before checking stock or
     a restock; (c) the 10 genres of spec 02 in readable form (for example "science fiction", not
     `SCIENCE_FICTION`), in the declaration order of `domain/Genre.java`, and that the assistant can list them when
     asked. A test proves the listed genres equal the `Genre` constants, so the two cannot drift.
  2. The spec 08 prompt rules stay, each still pinned by the existing tests unmodified: assistant of an online
     bookstore; only books in our catalogue; always use the tools before naming a book, never invent titles, authors
     or prices; prices in EUR; cannot place or change orders and points to the shop; answer in the language of the
     customer; at most 120 words.
  3. Regression test `BookstoreToolsTest` > a new test pins the `searchCatalogue` tool description: it starts with the
     spec 08 text unchanged ("Search the bookstore catalogue by title or author, optionally filtered by genre. Use
     this before mentioning any book.") and adds that the query is one title or one author name (or a fragment of
     one), never a title and an author together (plan D8); the query parameter's `@ToolParam` description says the
     same. Red on the current code. The other three tools' descriptions are unchanged (existing tests unmodified).
  4. Guard test (green before and after, recorded as such) `AssistantIntegrationTest` > a follow-up scenario with
     `ScriptedChatModel`: turn 1 asks for an author and the scripted model calls `searchCatalogue` with the author;
     turn 2 ("Is it in stock, and is more on its way?") the scripted model calls `searchCatalogue` with the title only,
     then `checkAvailability` with the returned id, and replies with the availability; the test asserts the flows are
     called with those arguments, the reply carries the stock figure, and turn 2's prompt holds the turn-1 question and
     answer. A second assertion documents the spec 02 contract the rule rests on: a combined "title author" query
     returns no book while the title-only query returns it (through the real tool and flow, no mock of the search).
  5. Negative control: on the pre-fix code (`assistant-system.st` and `BookstoreTools.java` restored from
     `git show <pr-base>:<file>` via copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`),
     `./mvnw -pl bookstore test -Dtest='AssistantSystemPromptTest,BookstoreToolsTest' -Dsurefire.failIfNoSpecifiedTests=false`
     FAILS on exactly the new tests of criteria 1 and 3 with a missing-statement assertion (the expected sentence or
     genre not found in the prompt or description) - not with a compile, resource or context error; no pre-existing
     test red. With the fix in place it PASSES. Outputs verbatim under `## Red evidence` and `## Green evidence`.
  6. No behaviour change outside the root cause: the catalogue search of spec 02 (`CatalogueFlow`, `q` criterion) is
     unchanged; no other production file changes; every pre-existing test passes unmodified; the prompt stays plain
     English, ASCII, no em dash, and short enough that the 120-word answer rule and the 1024 max-tokens are not at risk
     (state the prompt's word count in the outbox); ArchUnit green unmodified.
  7. `./mvnw clean verify` is green within 60 seconds.
- Live check (not a criterion; the main session runs it with the owner's key after the T5 merge and before qa-final):
  the three findings no longer occur.

## QA feed-forward

From the QA lead's T1 r0 review (ENDORSE; ledger `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-fix-08-assistant-review.md`):
- FF-T4-1 (T4): the regression test takes the `ChatClient` from `AssistantConfig.assistantChatClient(...)`, never a
  copy of its builder chain. Must-fail probe: delete the thinking-disabled line, the test goes red.
- FF-T4-2 (T4): stub `HttpServer` on 127.0.0.1 port 0; `thinking.type == "disabled"` on every captured request; the
  test proves it saw something (at least 3 requests with the tool round; turn 2 carries the turn-1 answer). Literal
  dummy key; never ANTHROPIC_API_KEY.
- FF-T4-3 (T4, live re-check): with T1 in place the unfixed defect shows as 503 `assistant-unavailable` on turn 2
  with the memory restored; after T4 turn 2 answers 200.
- FF-T4-4 (T4): the default options also reach `ScriptedChatModel` and `OfflineChatModel`; the verifier runs
  `AssistantIntegrationTest`, `ScriptedChatModelTest`, `AssistantCommandsTest` and `AssistantModeIntegrationTest`
  unmodified and green and checks the tool tests still see their tool names.
- FF-T4-5 (T4): the T4 record says whether residual R2 is closed.
- FF-T2-1 (T2): the server `detail` in the fixture differs from the spec 12 text and the server title is asserted
  absent. Must-fail probe: render `problem.detail` for `assistant-unavailable`.
- FF-T3-1 (T3): the exported proof turns the 5 `AssistantModeIntegrationTest` failures seen by qa-t1 green, its log
  holds zero `Failed to connect to /127.0.0.1:9` lines, and offline assertions check the code `assistant-offline`,
  not only the status (with T1 an escaped call also answers 503).
- qa-final Q1: in `AssistantCommands.java`, `git grep -n -E "catch \((Throwable|Exception|Error)[ )]"` prints nothing
  and `catch (RuntimeException failure)` appears exactly once. Q2: the cause is passed (by reading, unless a test
  pins it). Q3: `AssistantCommands.java` at the spec tip has the hash of the T1 merge, or T1's branch-level negative
  control is re-run.
From the QA lead's T4 r0 review (ENDORSE):
- FF-T3-2 (T3): the bookstore baseline is 1336; `AssistantAnthropicContractTest` (3 tests, no Spring context, literal
  key, loopback base URL) runs in every T3 run, plain and with the variables exported, stays 3/3 green, and T3 does
  not touch it.
- FF-LIVE (the main session's keyed re-check after the T4 merge, default model): (a) "Hello, what can you do?" then
  "And what genres do you have?" in one conversation answers 200 twice; (b) a first turn that makes the model search
  the catalogue (for example "Anything by Austen?") and a follow-up answer 200 twice (tool-loop reorder); (c) the
  server log has no `BadRequestException` and no `Invalid signature`. With T1 merged, a 503 `assistant-unavailable`
  on turn 2 means T4 is not working.
- qa-final Q4: at the spec tip `git grep -c "thinkingDisabled()" -- bookstore/src/main` is 1; `AssistantConfig.java`
  has the hash of the T4 merge or the T4 negative control is re-run; the contract test is 3/3 in the final module
  run. Q5: the FF-LIVE result is quoted in the final verdict or marked pending; qa-final never makes a real call.
- Residuals for Gate 2: R-d live acceptance of thinking disabled is docs-based until FF-LIVE runs; R-e thinking is
  off for every model while the model is overridable through `BOOKSTORE_AI_MODEL` (checked for Haiku 5.5 only);
  R-f the auto-configured `ChatClient.Builder` is not exercised offline (R17 deferred; FF-LIVE closes it); R-g the
  Spring AI 2.0.2 replay reorder stays latent in the library, guarded by the contract test (keep it through upgrades).
From the QA lead's T2 r1 review (ENDORSE):
- FF-T3-3 (T3, and T5): no `frontend/` file changes; `AssistantPage.tsx` (db0f0dc2...bf44) and
  `AssistantPage.test.tsx` (67167522...735f) keep their hashes after the T2 merge; the Vitest baseline in every later
  referee run is 605 passed + 2 expected fail.
- qa-final Q6: walk `#/assistant` in Vite dev mode in a fresh browser context with the console captured: happy path,
  then a `page.route` answering 503 `application/problem+json` with the exact unavailable body of
  `AssistantIntegrationTest`; the alert reads exactly "The assistant is unavailable right now, try again later", the
  server title and detail are absent, the typed message stays, Send is enabled, a second send clears the alert; console
  at most the deliberate 503 line, zero React warnings. A real-server variant only with
  `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.1:9` and a dummy key in a child shell.
- qa-final Q7: both T2 hashes unchanged at the spec tip, or T2's negative control re-run at branch level;
  `git grep -c "assistant-unavailable" <tip> -- frontend/src/pages/AssistantPage.tsx` is 2.
- Residual R-h: the real T1 answer reaching the page in a browser is unproven until Q6 runs.

From the QA lead's T3 r0 review (ENDORSE):
- FF-T5-T3-1 (T5): T5 does not touch `IntegrationTestBase.java` (73d25c10...97e2), `IntegrationTestOfflineTest.java`
  (dd8a2443...0442) or `application-test.yml` (375f393d...a97961); module baseline 1340 plus T5's new tests;
  `IntegrationTestOfflineTest` 4/4 and `AssistantAnthropicContractTest` 3/3 in every T5 run; Vitest 605 + 2 expected
  fail.
- FF-T5-T3-2 (T5): `README.md:5` relies on `assistant-system.st` "Answer in the language of the customer.", pinned by
  `AssistantSystemPromptTest.java:28`; that sentence and its assertion stay unmodified; after T5
  `git grep -c "language of the customer" -- README.md bookstore/src/main/resources/prompts/assistant-system.st`
  prints 1 for each file.
- FF-T5-T3-3 (T5): no T5 run needs exported variables; if a verifier exports anything, only the dummy key plus
  `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.2:9`; never `ANTHROPIC_*`, never `SPRING_AI_MODEL_CHAT` in a full run.
- qa-final Q8: both T3 test-file hashes unchanged at the spec tip, or T3's negative control (a) re-run at branch level;
  no "plain English" in README; "language of the customer" once each in README and the prompt. Q9: one referee run
  with only the dummy key and `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.2:9` exported: green under 60 s, same
  totals as the plain run, 0 `Failed to connect` lines, `IntegrationTestOfflineTest` 4/4, the `env` line quoted. Q5 is
  answered by A3 (7 of 7 answered 200).
- Residuals: R-d and R-f closed (A3); R-i an exported `SPRING_AI_MODEL_CHAT=anthropic` makes 7 tests in 2 classes fail
  locally against `127.0.0.1:9` (4 of them bean or environment checks), nothing leaves the machine; R-j
  `AssistantConfigContextTest` still reads the process environment (deferred, R17).

For T5 (orchestrator, from earlier lessons):
- FF-T5-1: pin prompt statements by meaning-bearing phrases, not whole-file equality, so later wording edits fail only
  where a rule is lost; one assertion per rule; genre check derived from `Genre.values()`.
- FF-T3-2 applies to T5 as well: `AssistantAnthropicContractTest` stays 3/3 and untouched.

- Optional follow-up FU-1 (not in this cycle's criteria): a test asserting `hasCauseReference` on the
  `UnavailableException` and an `isNotNull()` before `AssistantCommandsTest.java:465` reads the throwable.

## Amendments

- A1 (2026-11-09, orchestrator, on impl-t1's question; self-accepted, cheap and reversible): (1) T1 criterion 1:
  `org.springframework.ai.retry.*` is not on the Spring AI 2.0.2 classpath (no spring-ai-retry module; adding it is a
  POM change). The witness types are `com.anthropic.errors.AnthropicIoException` (connection or timeout),
  `com.anthropic.errors.AnthropicRetryableException`, `java.lang.IllegalStateException` and
  `java.io.UncheckedIOException` (two outside the Anthropic hierarchy, so a catch of `AnthropicException` alone
  dies). (2) D1 refined: the four use-case exceptions (`NotFoundException`, `ConflictException`,
  `InvalidInputException`, `UnavailableException`) thrown from the chat call are not a failed model call: they pass
  through unchanged (the existing `useCaseExceptionsMapToTheirStatus` rows stay green and unmodified), after the
  memory restore, without the WARN line. Every other `RuntimeException` is restored, logged once at WARN and
  translated to `assistant-unavailable`. New T1 criterion 6a: an integration test with `ScriptedChatModel` throwing
  a use-case exception (for example `NotFoundException`) answers its own status and code and leaves that
  conversation's memory exactly as before; a slice test finds no WARN line from `AssistantCommands` for a
  pass-through. Consequence: none outside T1.
- A2 (2026-11-09, orchestrator, on the main session's report from the owner's live-key check; self-accepted): new
  defect, S2: every second message of a conversation fails live with a 400 for a thinking-block signature (bug.md
  "Amendment A2"). Path stays lite+research: one researcher, res-1 (sonnet), confirmed the root cause offline against
  a local stub. New task T4 (bookstore, critical tier, sonnet implementer and verifier per the main session's model
  rule), ordered right after T1: T1, T4, T2, T3. Decision D7: thinking disabled on the assistant's chat client
  (`AssistantConfig`), not a memory decorator and not the `drop_block` beta header. D7 changes product behaviour
  beyond spec 08 "Configuration" (which names model and max-tokens only): the assistant no longer uses extended
  thinking; recorded here and reported to the main session, not put to Maik. The live re-check with the owner's key
  is the main session's, after the merge. Consequence: the spec PR closes one more finding; T4's contract test is the
  first test of the real model client.
- A3 (2026-11-09, orchestrator, on the main session's report; owner's decision): (1) T4 live evidence (FF-LIVE a, b,
  c): the main session re-checked cc6d658 with the owner's key: 7 of 7 messages answered 200 (1.4 to 3.8 s),
  including follow-ups in three conversations (172a2b6a hello then genres; 7e5d5bb7 Austen, stock, price; 04519ed6
  science fiction under 15 euro, then newest); no `BadRequestException`; thinking disabled is accepted by Haiku 5.5.
  Closes residuals R-d and R-f. Transcript posted by the main session on PR #111. (2) New task T5 (S3, bookstore,
  elevated, sonnet), three answer-quality defects from the same check against spec 08 "Grounded answers" and the
  system prompt rules (bug.md "Amendment A3"). Decision D8. After T5 merges into the spec branch the orchestrator
  tells the main session and pauses before qa-final for its live check. Consequence: one more task PR; qa-final
  waits for the main session.
- A4 (2026-11-09, orchestrator, on impl-t3's question; self-accepted, cheap and reversible, reported to the main
  session): pinning `spring.ai.model.chat=none` in `IntegrationTestBase` (D5 as first written) makes
  `AssistantModeConfig` skip in every shared integration context, so the pre-existing
  `AssistantModeIntegrationTest.offlineSwitchComesAfterTheConfigurationFiles` (the integration witness of the
  production offline switch, spec 08 residual R18) turns red and the switch is no longer exercised by any full-boot
  test. Editing that test would weaken spec 08's offline-mode evidence. D5 amended: the base pins
  `spring.ai.anthropic.api-key=` (blank, so an exported key never reaches a test context and the production switch
  decides offline mode for real) and `spring.ai.anthropic.base-url=http://127.0.0.1:9` (so no request can leave the
  machine, whatever model a developer exports). T3 criteria 2 to 5 and 7 amended accordingly. Deviation from the
  main session's example mechanism: an exported `SPRING_AI_MODEL_CHAT=anthropic` still switches the shared context
  to the Anthropic model (by design, R17); those tests then fail locally against the loopback address instead of
  staying offline, and nothing reaches the API. Consequence: no pre-existing test changes.
- A5 (2026-11-10, orchestrator, on impl-t5's report before QA; self-accepted, cheap and reversible): (1) T5 criterion 3
  necessarily changes one pre-existing assertion: `BookstoreToolsTest.toolsCarryTheSpecDescriptions` compared the
  whole `searchCatalogue` description with `isEqualTo`, which an appended rule cannot satisfy; it becomes `startsWith`
  the unchanged spec 08 sentence (the other three tools stay `isEqualTo`). To keep the assertion strength, the new
  test asserts the whole new description and the whole `query` `@ToolParam` description with `isEqualTo` (not only
  `contains`). This is the single permitted change to a pre-existing test in T5. (2) New T5 criterion 1(d): listing
  the genres in readable form ("science fiction") invites the model to pass that form as a tool argument, which
  `Genre.parse` rejects (it accepts case, `-` and `_`, not a space). The prompt therefore says how to pass a genre to
  a tool (capitals with underscores, for example SCIENCE_FICTION), pinned by a prompt test that also proves the named
  form is accepted by the tools (`Genre.parse` or the tool's genre path) and the readable form with a space is not.
  Consequence: one more prompt sentence; T5 goes back to its implementer before QA (not a QA rework round).
