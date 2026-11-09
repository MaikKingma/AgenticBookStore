# AgenticBookStore

An online bookstore built as a clean hexagonal onion: a Spring Boot REST backend on PostgreSQL, the OpenBook storefront
(React) and a small distributor service that supplies the stock. Customers search the catalogue and place orders, and
the bookstore asks the distributor to restock a book when its stock runs low.

## Prerequisites

- Java 25 (the Maven wrapper `./mvnw` does the rest).
- Docker, running: the tests and the local run of the bookstore start PostgreSQL in a container.
- No Node.js: the build installs its own into `frontend/node/`.

## Run

Build once from the repository root (this also builds the storefront into the bookstore), then start the bookstore and,
in a second terminal, the distributor:

    ./mvnw verify
    ./mvnw -pl bookstore spring-boot:run
    ./mvnw -pl distributor spring-boot:run

The bookstore starts PostgreSQL through Docker Compose (`bookstore/compose.yaml`, port 5432) and serves the storefront
at http://localhost:8080/shop/. The database keeps running when the bookstore stops. The distributor listens on port
8090; the bookstore needs it to import titles and to restock. Both services listen on 127.0.0.1 only.

### The jar against an existing PostgreSQL

`./mvnw verify` also builds `bookstore/target/bookstore-0.0.1-SNAPSHOT.jar` and `distributor/target/distributor.jar`. To run
the bookstore jar against a PostgreSQL you already have, set the datasource (your own URL, user and password) and start it
from the repository root. The migrations create the schema in that database, and the demo data is loaded too.

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
| `bookstore/` | The application: REST API, domain, PostgreSQL with Flyway migrations, the demo data |
| `distributor/` | A fictional book distributor with a REST API and MCP on port 8090, see `distributor/README.md` |
| `frontend/` | The OpenBook storefront (React, TypeScript, Vite), see `frontend/README.md` |
| `http/` | Request files in the JetBrains HTTP Client format, for the bookstore and the distributor |
| `guide/` | The workshop guide site, a Docusaurus project built on its own, outside the Maven build |

## Architecture

The bookstore has eight packages. Dependencies only point inwards, and ArchUnit tests enforce it.

    inbound  --->  command | query | process | assistant
                       domaininteraction   (flows, ports, DTOs)
                           domain          (aggregates, value objects, events)
    outbound <---  data | acl

[docs/architecture.md](docs/architecture.md) has the rules, the naming suffixes, the testing per layer and the trade-off
of the synchronous restock.

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

The bookstore serves the built storefront at `/shop/`. To work on it with hot reload, start the bookstore and run the
Vite dev server from `frontend/`, with the Node.js that Maven installed on the PATH:

    PATH="$PWD/node:$PATH" node/npm run dev

Then open http://127.0.0.1:5173/shop/. The dev server proxies `/books`, `/inventory`, `/orders` and `/assistant` to
http://127.0.0.1:8080. Windows commands are in `frontend/README.md`.

## Assistant

The chat assistant calls a language model with the key in the environment variable `BOOKSTORE_AI_API_KEY`, which you set
before you start the bookstore. Without a key the bookstore starts in offline mode, and `POST /assistant/chat` answers
503 with the error code `assistant-offline`. A message has at most 1000 characters and the bookstore answers at most 20
chat requests a minute (429 beyond that). Set the key under this name only: do not use `ANTHROPIC_API_KEY`, which this
app does not read and which other tools on your machine pick up.

## Tests

`./mvnw verify` runs all tests and needs Docker: the database tests use PostgreSQL in a Testcontainers container, and
there is no in-memory database. Maven activates the `test` profile. When you run a test from your IDE, add
`-Dspring.profiles.active=test` to the run configuration, so the demo data stays out and the assistant stays offline.

## Demo data

A local run starts with 40 books, their stock and 6 orders for 3 customers. Every run of the bookstore loads this data
(Maven or jar), and the tests never do. Seven books are at or below their reorder level, three of them sold out.

- 20 books are also in the distributor catalogue. Their titles and authors are real classics; the stock figures are invented.
- The other 20 books, their authors, the customers, their e-mail addresses and the orders are invented.
- Sense and Sensibility (9789900010029) is left out on purpose, because `http/inventory.http` adds it.

Run the files in `http/` on the demo data, in this order on one database: `catalogue.http`, `inventory.http`,
`orders.http`, `distributor.http` and `demo-flow.http`. Run each file once per database, except `distributor.http`, which
can run again. `demo-flow.http` walks from a search to a restock and needs the distributor.
