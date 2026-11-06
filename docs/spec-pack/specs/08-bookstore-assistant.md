# Spec 08: Bookstore assistant (Spring AI chat with tools)

> Module: `bookstore` · Depends on: 02, 03, 04, 07 · Capability: `assistant`

## Why
Participants build *with* an AI agent; the app should also contain an AI feature, so they see the other side: a
model behind a port-like boundary, tools that call our flows, a system prompt as product code, and guardrails
around a shared API key. The assistant is an **inbound adapter** in the onion: it talks to flows like a controller does.

## What changes
- Package `assistant`: `AssistantConfig` (ChatClient bean), `BookstoreTools` (`@Tool` methods),
  `AssistantCommands` (REST), `ChatPayload`, `AssistantReplyView`, `BookSuggestionView`, `AvailabilityView`.
- System prompt as a resource: `src/main/resources/prompts/assistant-system.st`.
- Chat memory per conversation (`MessageWindowChatMemory`, 20 messages, in-memory repository).
- Offline mode when no API key is configured, and a rate limit that protects the shared workshop key.
- A one-page chat UI at `/` (static `index.html`, vanilla JS, no build step).

## Out of scope
- Placing orders or changing anything through the assistant (read-only tools only).
- Customer data in tool results (no order or e-mail tools).
- RAG, embeddings, vector stores. MCP client to the distributor (that is Lab 4 brief C).

## Configuration
```yaml
spring:
  ai:
    anthropic:
      api-key: ${BOOKSTORE_AI_API_KEY:}
      chat:
        model: ${BOOKSTORE_AI_MODEL:<current Haiku model id from docs.claude.com/en/docs/about-claude/models>}
        max-tokens: 1024
        temperature: 0.2
bookstore:
  assistant:
    max-requests-per-minute: 20
    max-message-length: 1000
# bound to an `AssistantProperties` record (the `assistant` naming rule allows `Properties`)
```
- Dependency: `org.springframework.ai:spring-ai-starter-model-anthropic` (version from the Spring AI BOM 2.0.2).
- The env var is `BOOKSTORE_AI_API_KEY`, never `ANTHROPIC_API_KEY` (see tech stack: Claude Code would pick that up).
- Spring AI 2.0 property names have no `.options` segment.

## Tools (`BookstoreTools`)
| Tool method | Description given to the model | Calls | Returns |
|---|---|---|---|
| `searchCatalogue(query, genre?)` | "Search the bookstore catalogue by title or author, optionally filtered by genre. Use this before mentioning any book." | `CatalogueFlow.search` with the free-text `q` criterion (spec 02) + `InventoryFlow` | up to 10 `BookSuggestionView { id, title, authors, genre, price, inStock }` |
| `getBookDetails(bookId)` | "Get details of one book from the catalogue by its id." | `CatalogueFlow.findById` | `BookSuggestionView` plus publicationYear |
| `checkAvailability(bookId)` | "Check how many copies are available and whether a restock is on its way." | `InventoryFlow.findByBookId` | `AvailabilityView { available, pendingRestockExpected }` |
| `recommendByGenre(genre, limit)` | "Recommend in-stock books of a genre, newest publication first. limit max 5." | flows | up to 5 `BookSuggestionView` |

Tools call flows only (ArchUnit already forbids anything else). Tool exceptions become short error strings for the
model, never stack traces.

## System prompt (`assistant-system.st`, product-owned text)
Must state: you are the assistant of an online bookstore; only talk about books in our catalogue; always use the
tools before naming a book, never invent titles, authors or prices; prices are in EUR; you cannot place, change or
point the customer to the shop for orders; answer in the language of the customer; at most 120 words.

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /assistant/chat` | `ChatPayload { conversationId (UUID), message }` | 200 `AssistantReplyView { conversationId, reply }` | 400 validation or message too long, 429 rate limit (`AssistantRateLimiter` throws Spring's `ErrorResponseException` with `TOO_MANY_REQUESTS`), 503 assistant offline (`UnavailableException`, code `assistant-offline`) |

## Requirements

### Requirement: Grounded answers
The assistant SHALL answer questions about the catalogue using the tools and SHALL not name books that the tools did not return.

#### Scenario: Tool call is executed against our flows
- **GIVEN** a `ScriptedChatModel` test double that first requests the tool `searchCatalogue` with query "austen" and then replies with the tool result's first title
- **WHEN** a client posts "Do you have anything by Austen?"
- **THEN** `CatalogueFlow.search` is called with "austen" and the reply contains "Pride and Prejudice"

#### Scenario: Manual live check (facilitator, with the workshop key)
- **GIVEN** the seeded catalogue and `BOOKSTORE_AI_API_KEY` set
- **WHEN** the facilitator asks "Recommend two science fiction books under 15 euro" in the chat UI
- **THEN** the answer names only seeded books with correct prices (record the transcript in the PR)

### Requirement: Conversation memory
The assistant SHALL remember the last 20 messages per conversation id and SHALL keep conversations separate.

#### Scenario: Follow-up question
- **GIVEN** conversation X asked about "Frankenstein"
- **WHEN** conversation X asks "Is it in stock?"
- **THEN** the model receives the earlier messages of X and none of conversation Y

### Requirement: Offline mode
The application SHALL start and pass all tests without an API key; the chat endpoint SHALL then answer 503.

#### Scenario: No key configured
- **GIVEN** `BOOKSTORE_AI_API_KEY` is empty
- **WHEN** the application starts and a client posts to `/assistant/chat`
- **THEN** startup succeeds and the response is 503 with a ProblemDetail "Assistant offline: set BOOKSTORE_AI_API_KEY"

Implementation note: Spring AI's Anthropic auto-configuration may refuse an empty key at startup. Choose a mechanism
(for example `spring.ai.model.chat=none` unless a key is present, set by an `EnvironmentPostProcessor`, plus an
`OfflineChatModel` bean) and verify it at build time. The behaviour above is what matters. Class names must satisfy
the `assistant` naming rule (the post-processor can be `AssistantModeConfig`).

### Requirement: Shared-key guardrails
The assistant SHALL reject messages longer than 1000 characters and SHALL limit requests to 20 per minute per application instance.

#### Scenario: Rate limit
- **GIVEN** 20 chat requests in the current minute
- **WHEN** a 21st request arrives
- **THEN** the response is 429 and the model is not called

## Tests (required)
- `BookstoreToolsTest` (mocked flows; caps and error strings).
- `AssistantCommandsTest` (`@WebMvcTest`: validation, 429, 503).
- `AssistantIntegrationTest` with `ScriptedChatModel` (a `ChatModel` implementation in test sources that returns
  scripted responses, including an `AssistantMessage` with a tool call) covering grounding and memory scenarios.
  In Spring AI 2.0 `ChatModel.call()` no longer executes tools; the tool loop runs in ChatClient's auto-registered
  tool-calling advisor, so a scripted tool-call response exercises the real `BookstoreTools` path. *Verify this at
  build time*; if the loop turns out to live in the model, let `ScriptedChatModel` execute the tool calls through a
  `ToolCallingManager`.
- ArchUnit: Spring AI only in `assistant` (rule already exists, now non-empty); `assistant` naming rule.
- No test calls the real Anthropic API.

## Tasks
1. Dependency and configuration; offline mechanism and its test.
2. View records and `BookstoreTools` with tests.
3. System prompt resource; `AssistantConfig` with `ChatClient.Builder`, `defaultSystem`, `defaultTools`,
   `MessageChatMemoryAdvisor`; conversation id via `ChatMemory.CONVERSATION_ID` advisor param.
4. `AssistantCommands`, `AssistantRateLimiter` (simple sliding window), `AssistantExceptionHandler`.
5. `ScriptedChatModel`, integration tests.
6. `static/index.html` chat page (conversation id in `sessionStorage`, plain fetch to `/assistant/chat`).
7. Manual live check with the workshop key; README section "Assistant".

## Definition of done
- `./mvnw verify` green without any key; manual live check transcript recorded.
- `grep -r ANTHROPIC_API_KEY bookstore distributor README.md` finds only the warning in the README.
