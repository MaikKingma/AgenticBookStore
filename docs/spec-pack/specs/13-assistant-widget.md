# Spec 13: Assistant chat widget with streamed answers (after the first release)

> Modules: `bookstore` (assistant), `frontend` · Depends on: 08, 12, and the first release (spec 09 `main`,
> spec 10 level branches, spec 11 guide) · Capability: `assistant`, `storefront` · Decision D13

## Status
Deferred by Maik on 2026-11-09: optional, built after the first release. Until then specs 08 and 12 stand as they
are (the assistant page at `#/assistant` and `POST /assistant/chat`). Do not start this spec before the first
release is published and tagged; then it is a normal SDD cycle on `factory`.

## Why
The assistant should feel like a classic chat assistant: a launcher in the corner of every page instead of a separate
page, and answers that appear while the model writes them.

## What changes
- Bookstore, package `assistant`: `POST /assistant/chat/stream` sends the reply as Server-Sent Events.
  `POST /assistant/chat` (JSON) stays unchanged for the `.http` files, tests and workshop exercises.
- Storefront: the assistant page and its navigation link go away. A round launcher button with a chat icon, fixed in
  the lower right corner of every page, opens a chat panel. The conversation (id and messages) lives in a React
  context backed by `sessionStorage` (`state/conversation.tsx`), so it survives navigation and a reload.
- New component `AssistantWidget`; `AssistantPage` is removed.

## Out of scope
- Any change to the tools, the system prompt, the memory window, the rate limit or the offline mode of spec 08.
- Placing orders through the assistant.

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /assistant/chat/stream` (decision D13) | `ChatPayload { conversationId (UUID), message }` | 200 `text/event-stream`: one or more `delta` events (data `{"text": "..."}`, the reply in order), then one `done` event (data `{"conversationId": "..."}`) | Before the stream starts, exactly the errors of `POST /assistant/chat` as ProblemDetail (400, 429, 503 `assistant-offline`, 503 `assistant-unavailable` when the call fails before the first chunk). A failure after the first chunk ends the stream with one `error` event (data `{"code": "assistant-unavailable", "title": "Assistant unavailable", "detail": "..."}`) and no `done` event |

The storefront calls `POST /assistant/chat/stream` (`ChatPayload`, Server-Sent Events) instead of `POST /assistant/chat`.

## Requirements

### Requirement: Streamed answers
`POST /assistant/chat/stream` SHALL stream the reply as Server-Sent Events with the same system prompt, tools,
conversation memory, validation, rate limit, offline mode and failure rules as `POST /assistant/chat`. Use Spring
AI's `ChatClient` streaming (`stream()`); the tool loop must still run, *verify at build time* that tool calls work
while streaming in Spring AI 2.0.2. The concatenated `delta` texts are the reply that the memory stores. The SSE
event and data shapes are records with an `assistant` suffix (for example `AssistantStreamEventView`). Spring AI and
Reactor types stay inside `assistant`.

#### Scenario: Streamed round trip
- **GIVEN** a `ScriptedChatModel` that streams the reply "Pride and Prejudice is in stock." in three chunks
- **WHEN** a client posts "Do you have anything by Austen?" to `/assistant/chat/stream`
- **THEN** the response is `text/event-stream` with three `delta` events whose texts join to that reply, then one `done` event with the conversation id

#### Scenario: Streamed follow-up uses memory
- **GIVEN** conversation X received a streamed answer about "Frankenstein"
- **WHEN** conversation X streams "Is it in stock?"
- **THEN** the model receives the earlier question and the full streamed answer of X, and nothing of conversation Y

#### Scenario: Failure mid-stream
- **GIVEN** a `ScriptedChatModel` that streams one chunk and then throws
- **WHEN** a client posts to `/assistant/chat/stream`
- **THEN** the stream holds one `delta` event, then one `error` event with `code` `assistant-unavailable` and no `done` event, and the conversation memory holds no entry for that question (decision D10)

#### Scenario: Errors before the stream
- **GIVEN** no key is configured, or 20 requests in the current minute, or a message longer than 1000 characters
- **WHEN** a client posts to `/assistant/chat/stream`
- **THEN** the response is the same ProblemDetail as `POST /assistant/chat` gives (503 `assistant-offline`, 429, 400), not an event stream, and the model is not called

### Requirement: Assistant chat widget
The storefront SHALL offer the assistant as a classic chat widget: a round launcher button with a chat icon, fixed in
the lower right corner of every page, opens a chat panel above it. There is no assistant page and no assistant link
in the navigation; `#/assistant` is not a route. The widget streams answers from `POST /assistant/chat/stream`, with
one conversation id per browser session.

- Launcher: a `button` with the accessible name "Open the assistant" and an inline SVG chat icon (no icon library,
  no new npm dependency). When the panel is open it reads "Close the assistant".
- Panel: `role="dialog"` with the label "OpenBook assistant", a header with a close button, the message list, the
  input with its length counter and the send button. On a phone the panel takes the full width; from tablet width
  it is a card of about 24rem wide. Escape and the close button close it; focus moves to the input on open and back
  to the launcher on close.
- The conversation (id and messages) lives in a React context backed by `sessionStorage`, so it survives navigation
  between pages and a reload, and starts fresh in a new browser session.
- Streaming: the storefront reads the event stream with `fetch` and a stream reader (`EventSource` cannot post);
  each `delta` is appended to the assistant message as it arrives. While an answer streams, the send button is
  disabled and the message list announces "The assistant is typing" once through a polite live region, not every
  chunk. The finished answer is announced once.
- The launcher and the open panel never hide the footer text or the last content for good: the page leaves room at
  the bottom for the launcher (decision D11 still holds).
- Colours come from the tokens and every pair meets WCAG 2.2 AA (the launcher is teal with a white icon).

#### Scenario: Open and close
- **GIVEN** any page of the storefront
- **WHEN** the user activates "Open the assistant", then presses Escape
- **THEN** the dialog "OpenBook assistant" opens with focus in the input, then closes with focus back on the launcher

#### Scenario: Streamed chat round trip
- **GIVEN** a conversation id in `sessionStorage` and a server that streams three `delta` events and a `done` event
- **WHEN** the user sends "Do you have anything by Austen?"
- **THEN** the storefront posts `ChatPayload { conversationId, message }` to `/assistant/chat/stream`, shows the user message, shows the assistant message growing with each chunk, and reuses the same conversation id for the next message

#### Scenario: Conversation survives navigation
- **GIVEN** a conversation with one answered question in the widget
- **WHEN** the user navigates from the catalogue to a book detail page and opens the widget
- **THEN** the question and the answer are still shown

#### Scenario: Assistant offline
- **GIVEN** the server answers 503 with `code` `assistant-offline`
- **WHEN** the user sends a message
- **THEN** the widget shows "The assistant is offline" and the input stays usable

#### Scenario: Failure mid-stream
- **GIVEN** the server streams one `delta` event and then an `error` event with `code` `assistant-unavailable`
- **WHEN** the user sends a message
- **THEN** the widget keeps the partial text, marks the answer as incomplete and shows "The assistant is unavailable right now, try again later"

#### Scenario: Message length
- **WHEN** the user types more than 1000 characters
- **THEN** the send button is disabled and a counter shows the limit (the server limit of spec 08)

## Tests (required)
- Bookstore: `AssistantCommandsTest` covers the stream endpoint's errors before the stream; `ScriptedChatModel` also
  implements `stream()` with scripted chunks, so the streaming scenarios run offline; an integration test per
  streaming scenario above. No test calls the real Anthropic API.
- Frontend: `AssistantWidget.test.tsx`: open and close with focus, streamed round trip, conversation id reuse,
  survives navigation, 503 and 429 messages, failure mid-stream, length limit. `AssistantPage.test.tsx` goes with the
  page.
- `ArchitectureTest` unchanged and green (Spring AI and Reactor types only in `assistant`).

## Tasks
1. Stream endpoint, event records and `ScriptedChatModel.stream()`, with tests (bookstore).
2. Conversation context with `sessionStorage` and the streaming API client, with tests (frontend).
3. `AssistantWidget`, removal of the assistant page, route and navigation link, with tests (frontend).
4. Docs: README assistant section, the guide pages that mention the assistant page, `http/assistant.http` example
   for the stream endpoint.
5. Manual live check with the workshop key: a streamed answer and a streamed follow-up in the widget; screenshots
   of the open widget at desktop and phone width.

## Definition of done
- `./mvnw clean verify` green from the repo root without any key; time recorded.
- Live check transcript and widget screenshots in the spec PR.
- `POST /assistant/chat` behaves exactly as before (its tests unchanged and green).
- The released `main` is updated only through the publish procedure of spec 09, after Maik's approval.
