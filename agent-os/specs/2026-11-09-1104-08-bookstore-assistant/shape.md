# Shape: Spec 08 Bookstore assistant

Source of truth: `docs/spec-pack/specs/08-bookstore-assistant.md`. This file records the scope as shaped and the decisions taken where the spec leaves room. It changes no requirement, scenario, path or name of the spec, except where a decision below says so and why.

## Scope

- Package `assistant` in the bookstore: `AssistantConfig` (ChatClient, chat memory, offline model), `AssistantModeConfig` (environment post-processor), `AssistantProperties`, `OfflineChatModel`, `BookstoreTools` (four read-only `@Tool` methods calling flows), `AssistantCommands` (`POST /assistant/chat`), `ChatPayload`, `AssistantReplyView`, `BookSuggestionView`, `BookDetailsView`, `AvailabilityView`, `AssistantRateLimiter`, `AssistantExceptionHandler`.
- System prompt resource `bookstore/src/main/resources/prompts/assistant-system.st`.
- Chat memory per conversation, 20 messages, in memory.
- Offline mode without a key (startup succeeds, 503 `assistant-offline`), 1000-character cap (400), 20 requests per minute per instance (429).
- README section "Assistant" with the single `ANTHROPIC_API_KEY` warning.
- Out of scope (spec): writes through the assistant, customer data in tool results, RAG, MCP client, any UI (the storefront of spec 12 is the UI), `http/assistant.http` (spec 09).

## Branch context

Spec 08 starts before spec 07 is merged. The spec branch `sdd/08-bookstore-assistant/spec` is cut from `origin/sdd/07-restock-process/spec` (9196b08), so `checkAvailability` uses the real pending restock. The spec PR targets `factory` and shows spec 07's commits until spec 07 merges; then `origin/factory` (which also brings spec 12's `frontend` module) is merged into the spec branch with a merge commit and the referee runs again. Spec 12's storefront already calls this endpoint; the HTTP contract stays exactly as the spec states.

## Decisions (orchestrator, self-accepted, logged in the run ledger)

- O1 Default model `${BOOKSTORE_AI_MODEL:claude-haiku-5-5}`, the current Haiku id from the claude-api reference (alias form).
- O2 No `temperature` is configured, although the spec's configuration block lists `temperature: 0.2`. The Messages API answers 400 to any request that carries `temperature`, `top_p` or `top_k` since 2026-10-21, so the spec value would break every live call. With the property unset, Spring AI 2.0.2 sends none. Cheap and reversible (one yml line); the owner's live check confirms it. Spec text suggestion for the owner: drop the temperature line.
- O3 `spring.ai.anthropic.base-url: https://api.anthropic.com` is set explicitly, because the Anthropic SDK otherwise reads `ANTHROPIC_BASE_URL` from the environment and could send the workshop key elsewhere. This only pins the vendor default.
- O4 Offline mechanism: `AssistantModeConfig implements org.springframework.boot.EnvironmentPostProcessor` (registered in `META-INF/spring.factories`) sets `spring.ai.model.chat=none` when the resolved key is blank and the property is unset; `OfflineChatModel` is the conditional `ChatModel` then; the controller checks `chatModel instanceof OfflineChatModel` per request. The test profile blanks the key (`application-test.yml`), so tests stay offline even when a facilitator shell exports `BOOKSTORE_AI_API_KEY`.
- O5 `ScriptedChatModel` (test sources) returns `ToolCallingChatOptions` from `getOptions()`; a negative-control test proves that default options drop the tools silently.
- O6 `recommendByGenre` composes existing flows inside the tool (`search` by genre, stock per book, `available > 0`, newest publication year first, ties by title then ISBN, limit clamped to 1 to 5, null as 5); no new flow method. `inStock` is `available > 0`, the same rule `BookQueries` uses (status ignored).
- O7 `AssistantExceptionHandler`, scoped to the `assistant` package, maps the four use-case exceptions with `code` and `details`, bean validation and the length cap to 400 `validation-failed`, and renders the limiter's `ErrorResponseException(TOO_MANY_REQUESTS)` as an RFC 9457 ProblemDetail (status 429, no invented `code`).
- O8 No tool-call ceiling (`spring.ai.tools.limits.*`) is configured; not in the spec. Residual for the owner.
- Tech-lead calls D1 to D11 accepted (strategy section 1), among them: request order validation, length, offline, rate limit, model (a rejected request takes no rate slot and writes no memory); book ids as `String` parsed inside the tools so a made-up id gets our own error text; curated tool error texts, a generic "<tool> failed for a technical reason." for anything unexpected, stack traces only in the log; `BookDetailsView` (the six suggestion fields plus `publicationYear`) as the return type of `getBookDetails`, reading the spec's "`BookSuggestionView` plus publicationYear" as its own top-level record; price as a plain decimal string, the expected restock date as `LocalDate.toString()` or null; the 429 detail names the limit; the limiter is a sliding window over accepted requests on the context `Clock`.
- System prompt reading: the spec's sentence "you cannot place, change or point the customer to the shop for orders" is read as "you cannot place or change orders; point the customer to the shop for orders" (the shop exists since spec 12). Product text; the owner may reword it.
- Live-model failures (invalid key, overload, timeout) keep Spring's default 500 without a stack trace; the spec names no mapping. Residual. `max-tokens: 1024` stays as the spec says; the live check watches for truncated answers.

## Scenario to test map (planned; final names in the task outboxes)

| Spec scenario | Test |
|---|---|
| Tool call is executed against our flows | `AssistantIntegrationTest.toolCallIsExecutedAgainstOurFlows` (T5); without a context `AssistantConfigTest.toolCallRunsAgainstTheFlows` (T3) |
| Manual live check | Owner's gate with the workshop key, transcript in the spec PR (not an agent task) |
| Follow-up question | `AssistantIntegrationTest.followUpQuestionSeesOnlyItsOwnConversation` (T5), `AssistantConfigTest.conversationsStayApart` and `memoryWindowKeepsTwentyMessages` (T3) |
| No key configured | `AssistantModeIntegrationTest.startsOfflineWithoutAKey` (T1) and `chatAnswers503WithoutAKey` (T4), `AssistantCommandsTest.Offline.offlineAnswers503` (T4) |
| Rate limit | `AssistantCommandsTest.twentyFirstRequestIs429AndNeverReachesTheModel` (T4), `AssistantRateLimiterTest` (T4) |
| Message longer than 1000 characters | `AssistantCommandsTest.messageOf1000CharactersIsAccepted` and `messageOf1001CharactersIs400` (T4) |
| ArchUnit: Spring AI only in `assistant`, naming rule | `ArchitectureTest` (22 rules, unchanged), `ArchitectureSelfTest` real `ChatClient` fixture (T1) and non-vacuity (T5) |
