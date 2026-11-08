# Distributor

A small fictional book wholesaler. It keeps an in-memory catalogue of about 150 titles and offers it over a
REST API and over MCP (Model Context Protocol), so a bookstore, a script or an AI client can search titles,
check stock and place restock orders.

All data is fictional demo data. The EANs are valid ISBN-13 numbers, but apart from the Pride and Prejudice
example they are invented. The new releases and their authors are fictional, and no restock order is ever
shipped. Orders live in memory and are gone when the process stops.

## Run

Needs Java 25. From the repository root:

    ./mvnw -pl distributor spring-boot:run

or build once and run the jar:

    ./mvnw clean verify
    java -jar distributor/target/distributor.jar

The service listens on `http://127.0.0.1:8090`. Health check: `http://127.0.0.1:8090/actuator/health`.

## REST API

| Method and path | Purpose |
|---|---|
| `GET /api/titles` | Search. Optional `q`, `subject` (code prefix), `availability`, `page` (default 0), `size` (default 20, 1 to 50) |
| `GET /api/titles/{ean}` | One title |
| `GET /api/subjects` | Subject codes and labels |
| `POST /api/restock-orders` | Body `{"ean": "...", "quantity": 10, "retailerReference": "..."}`, quantity 1 to 500 |
| `GET /api/restock-orders/{id}` | A placed order |

Every error is JSON with exactly `errorCode` and `message`:

| Status | errorCode | When |
|---|---|---|
| 400 | `INVALID_REQUEST` | A bad parameter or a missing or malformed body |
| 404 | `TITLE_NOT_FOUND` | Unknown EAN |
| 404 | `RESTOCK_ORDER_NOT_FOUND` | Unknown order id |
| 422 | `NOT_ORDERABLE` | The title is out of print |
| 503 | `TEMPORARILY_UNAVAILABLE` | Only when the chaos knobs below are on |

## MCP (HTTP)

The MCP endpoint is `http://localhost:8090/mcp` (Streamable HTTP). It offers four read-only tools:
`search_titles`, `get_title`, `list_subjects` and `check_availability`. Add it to Claude Code with:

    claude mcp add --transport http distributor http://localhost:8090/mcp

If `localhost` resolves to IPv6 on your machine, use `http://127.0.0.1:8090/mcp` instead.

## MCP (STDIO)

For clients that start the server themselves, build the jar and register it with an absolute path:

    claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio

In this mode there is no web server and stdout carries only the protocol. Logs go to `distributor-stdio.log`
in the working directory of the process. At start you may see one line on stderr,
`|-ERROR in ch.qos.logback.classic.PatternLayout("") - Empty or null pattern.`. It is expected and harmless.

## Chaos knobs

Two settings make `/api/**` misbehave on purpose, for resilience demos. Both are off by default.

- `distributor.chaos.latency-ms`: milliseconds every API call waits first (default 0).
- `distributor.chaos.failure-rate`: share of API calls answered with 503, 0.0 to 1.0 (default 0.0).

Pass them as arguments, for example:

    java -jar distributor/target/distributor.jar --distributor.chaos.latency-ms=300 --distributor.chaos.failure-rate=0.2

With Maven: `./mvnw -pl distributor spring-boot:run -Dspring-boot.run.arguments="--distributor.chaos.failure-rate=0.2"`.
Health and MCP calls are not affected.

## Security note

The server binds to 127.0.0.1 only, so it is reachable from your own machine only. The MCP endpoint has no
authentication. Do not expose it to a network.
