# Inbound Adapters

`command`:

- `POST /{resource}/commands/{action}` for collection level commands, `POST /{resource}/{id}/commands/{action}` for commands on one instance.
- Request bodies are `*Payload` records with Jakarta Validation.
- Success is `202 Accepted`. A command that creates something answers with `CommandResult(id)`.

`query`:

- `GET /{resource}` and `GET /{resource}/{id}`.
- Responses are `*View` records shaped for the reader, never DTOs.

Errors:

- One `@RestControllerAdvice` per adapter package (`*ExceptionHandler`) turns exceptions into RFC 9457 `ProblemDetail`.
- The four use-case exceptions map to 404, 409, 422 and 503 with the exception's title and detail and the properties `code` and `details`.
- A bean validation failure maps to 400 with `code` `validation-failed` and `details` as a list of `{field, message}`.

`process`:

- `EventProcessor` has `@TransactionalEventListener(phase = AFTER_COMMIT)` methods that hand the event to a `*Delegate`.
- A delegate runs in `REQUIRES_NEW` and calls one flow method.

`assistant`:

- The only package that may use Spring AI. `AssistantCommands` serves `POST /assistant/chat`.
- `BookstoreTools` gives the model read-only access to the bookstore through `CatalogueFlow` and `InventoryFlow`, like any other inbound adapter.
