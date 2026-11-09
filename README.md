# AgenticBookStore

## Assistant

The bookstore has an AI assistant. It answers questions about the catalogue in the language of the customer and uses the bookstore's own data to do it. It only reads: it cannot add books, change stock or place orders.

### Chat endpoint

Send a message with `POST /assistant/chat`. The body has two fields: `conversationId`, a UUID that you choose (use the same one to continue a conversation), and `message`, the text of your question. With the bookstore running on port 8080:

```bash
curl -i -X POST http://localhost:8080/assistant/chat -H "Content-Type: application/json" -d '{"conversationId":"123e4567-e89b-12d3-a456-426614174000","message":"Which science fiction books do you have?"}'
```

The reply is JSON with the same conversation id and the answer:

```json
{"conversationId":"123e4567-e89b-12d3-a456-426614174000","reply":"..."}
```

The assistant remembers the last 20 messages of each conversation. A new `conversationId` starts a new conversation.

### The key

The assistant needs an API key for the model. The application reads it from the environment variable `BOOKSTORE_AI_API_KEY`. Set it in the shell where you start the bookstore.

bash:

```bash
export BOOKSTORE_AI_API_KEY="replace-with-your-workshop-key"
```

PowerShell:

```powershell
$env:BOOKSTORE_AI_API_KEY = "replace-with-your-workshop-key"
```

> **Warning:** never set `ANTHROPIC_API_KEY` for the bookstore: Claude Code offers to use that variable instead of your own subscription, so your Claude Code usage would be billed to the workshop key.

By default the model is `claude-haiku-5-5`. To use another model, set the optional variable `BOOKSTORE_AI_MODEL` to its name before you start the bookstore.

### Without a key

Without `BOOKSTORE_AI_API_KEY` the bookstore still starts. The rest of the bookstore is not affected, and `POST /assistant/chat` answers `503 Service Unavailable` with the message "Assistant offline: set BOOKSTORE_AI_API_KEY". Set the key and restart the bookstore to bring the assistant online.

### Limits

- A message can be at most 1000 characters. A longer message, a blank message or a missing `conversationId` is answered with `400 Bad Request`.
- The bookstore accepts at most 20 chat requests per minute per running instance. Above that it answers `429 Too Many Requests` until the oldest of those requests is a minute old. The limit protects the shared workshop key.

### What the assistant can do

The assistant has four read-only tools. It uses them to look things up instead of guessing:

- search the catalogue by title or author, optionally for one genre (at most 10 results);
- show the details of one book;
- check how many copies of a book are available and, when a restock is on its way, the date it is expected;
- recommend in-stock books of a genre, newest first (at most 5).

### The chat page

The storefront has a chat page for the assistant. Open `http://localhost:8080/shop/`, then choose Assistant in the navigation (or go straight to `http://localhost:8080/shop/#/assistant`).
