# AgenticBookStore

## The app

An online bookstore built as a clean hexagonal onion: a Spring Boot REST backend on PostgreSQL, the OpenBook storefront
(React) and a small distributor service. When a book's stock runs low, the bookstore asks the distributor to restock it.
The [workshop guide site](https://maikkingma.github.io/AgenticBookStore/) is on GitHub Pages.

## Prerequisites

- Java 25 (the Maven wrapper `./mvnw` does the rest) and no Node.js: the build installs its own into `frontend/node/`.
- Docker, running: the tests and the local run of the bookstore start PostgreSQL in a container.

## Run

Build once from the repository root (this also builds the storefront), then start the two services in two terminals:

    ./mvnw verify
    ./mvnw -pl bookstore spring-boot:run
    ./mvnw -pl distributor spring-boot:run

The bookstore starts PostgreSQL through Docker Compose (`bookstore/compose.yaml`, port 5432) and serves the storefront
at http://localhost:8080/shop/. The database keeps running when the bookstore stops. The distributor listens on port
8090; the bookstore needs it to import titles and to restock. Both services listen on 127.0.0.1 only.

### The jar against an existing PostgreSQL

`./mvnw verify` also builds `bookstore/target/bookstore-0.0.1-SNAPSHOT.jar`. To run it against your own PostgreSQL, set the
datasource (its URL, user and password) and start it from the repository root. The migrations create the schema.

    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bookstore \
    SPRING_DATASOURCE_USERNAME=bookstore SPRING_DATASOURCE_PASSWORD=bookstore \
    java -jar bookstore/target/bookstore-0.0.1-SNAPSHOT.jar

### Reset the database

    docker compose -f bookstore/compose.yaml down

This removes the database container, so the next start creates a fresh database that is migrated and seeded again. Use it
too when the bookstore fails at startup because an older local database already holds a book that the demo data adds.

## Modules

| Folder | What it is |
|---|---|
| `bookstore/` | The application: REST API, domain, PostgreSQL with Flyway migrations, the assistant, the demo data |
| `distributor/` | A fictional book distributor with a REST API and MCP on port 8090, see `distributor/README.md` |
| `frontend/` | The OpenBook storefront (React, TypeScript, Vite), see `frontend/README.md` |
| `http/` | Request files in the JetBrains HTTP Client format, for the bookstore and the distributor |

## Architecture

The bookstore has eight packages. Dependencies only point inwards, and ArchUnit tests enforce it.

    inbound  --->  command | query | process | assistant
                       domaininteraction   (flows, ports, DTOs)
                           domain          (aggregates, value objects, events)
    outbound <---  data | acl

[docs/architecture.md](docs/architecture.md) has the rules, the suffixes, the testing per layer and the restock trade-off.

## Endpoints

Bookstore, port 8080. Commands answer 202 and return `{"id": "..."}` when they create something. Errors are RFC 9457 problem details.

- Books: `GET /books`, `GET /books/{id}`, `POST /books/commands/add`, `POST /books/commands/import-from-distributor`,
  `POST /books/{id}/commands/change-price`, `POST /books/{id}/commands/discontinue`
- Inventory: `GET /inventory/{bookId}`, `GET /inventory?lowStock=true`, `POST /inventory/{bookId}/commands/receive`,
  `POST /inventory/{bookId}/commands/correct`, `POST /inventory/{bookId}/commands/request-restock`
- Orders: `GET /orders/{id}`, `GET /orders?customerEmail=`, `POST /orders/commands/place`, `POST /orders/{id}/commands/ship`
- Assistant: `POST /assistant/chat`
- Other: `GET /actuator/health`, `GET /actuator/info`, and `/` redirects to `/shop/`

Distributor, port 8090: `GET /api/titles`, `GET /api/titles/{ean}`, `GET /api/subjects`, `POST /api/restock-orders`,
`GET /api/restock-orders/{id}`, MCP on `/mcp`, `GET /actuator/health` and `GET /actuator/info`.

## Storefront

For hot reload, start the bookstore and run the Vite dev server from `frontend/` with the Maven-installed Node.js on the PATH:

    PATH="$PWD/node:$PATH" node/npm run dev

Then open http://127.0.0.1:5173/shop/. The dev server proxies `/books`, `/inventory`, `/orders` and `/assistant` to
http://127.0.0.1:8080. Windows commands are in `frontend/README.md`.

## Assistant

The assistant answers questions about the catalogue in the language of the customer, with read-only tools. It is part
of the shop at http://localhost:8080/shop/, and `POST /assistant/chat` takes a `conversationId` (a UUID you choose) and
a `message`. The reply holds that `conversationId` and a `reply`; the last 20 messages of a conversation are kept.

The key comes only from `BOOKSTORE_AI_API_KEY`, set in the shell where you start the bookstore. In bash that is
`export BOOKSTORE_AI_API_KEY="..."`, in PowerShell `$env:BOOKSTORE_AI_API_KEY = "..."`. Warning: never set
`ANTHROPIC_API_KEY` for the bookstore. The app does not read it, and Claude Code offers to use that variable instead of
your own subscription, so your usage there would be billed to the workshop key.

The model is `claude-haiku-5-5` unless `BOOKSTORE_AI_MODEL` names another one. `application.yml` sets the base URL to
https://api.anthropic.com (a Spring environment variable can still override it), and no temperature or other sampling setting
is configured. Without a key the bookstore still starts, in offline mode, and `POST /assistant/chat` answers 503 with the
code `assistant-offline`. With a key set, a failed live model call (for example a wrong key, a spend or rate limit at
Anthropic, or a timeout) answers 503 with the code `assistant-unavailable`. A message has at most 1000 UTF-16 code units (an
emoji counts as two). A longer, blank or missing message, or a missing `conversationId`, answers 400. At most 20 chat
requests a minute per running instance reach the model; the rest answer 429.

## Tests

`./mvnw verify` needs Docker: tests use PostgreSQL in Testcontainers, never an in-memory database. Maven activates the
`test` profile; in an IDE add `-Dspring.profiles.active=test`, so no demo data loads and the assistant stays offline.

## Demo data

A local run starts with 103 books, their stock and 6 orders for 3 customers. Every run of the bookstore loads this data
(Maven or jar), and the tests never do. Seven books are at or below their reorder level, three of them sold out.

- 20 books are also in the distributor catalogue. Their titles and authors are real classics; the stock figures are invented.
- The other 83 books, their authors, the customers, their e-mail addresses and the orders are invented.
- Sense and Sensibility (9789900010029) is left out on purpose, because `http/inventory.http` adds it.

Run the files in `http/` in this order on one database: `catalogue.http`, `inventory.http`, `orders.http`,
`distributor.http`, `demo-flow.http` (which needs the distributor). Run each once, except `distributor.http`.
