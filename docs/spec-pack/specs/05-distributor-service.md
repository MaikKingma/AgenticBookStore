# Spec 05: Distributor service (REST API + our own MCP server)

> Module: `distributor` · Depends on: 01 · Capability: `distributor-catalog`

## Why
The bookstore needs an external system to integrate with, and the workshop needs an MCP server that participants
can read, run and connect to Claude Code. The distributor is both: a fictional book wholesaler with a REST API (used
by the bookstore's ACL in spec 06) and an MCP server (used by the coding agent in Level 2).

Its model is **deliberately foreign** to the bookstore's (EAN instead of ISBN, cents instead of Money, subject
codes instead of genres, contributors with roles). That is what makes the anti-corruption layer meaningful.

The distributor does NOT follow the onion architecture: it is "someone else's system". Keep it a plain, small
Spring app (`api`, `mcp`, `catalog` packages). The bookstore's ArchUnit rules do not apply here.

## What changes
- In-memory catalogue of about 150 titles loaded from `src/main/resources/catalog/titles.json` at startup.
- REST API under `/api` for titles, subjects and restock orders.
- MCP server (Spring AI, Streamable HTTP) at `/mcp` with read-only tools.
- Optional STDIO mode as a fallback transport.
- Executable jar `distributor/target/distributor.jar` for the offline USB kit.

## Out of scope
- Persistence (in-memory is fine; restock orders are lost on restart), authentication, a UI.
- Write tools in MCP. Restock orders are REST-only: agents get read-only tools by default.
- `list_new_releases` MCP tool: this is the Level 2 stretch exercise. Do not implement it (the seed data does contain
  release dates so the exercise works).

## Distributor model (JSON shape on the wire)
```json
{
  "ean": "9780141439518",
  "titleName": "Pride and Prejudice",
  "contributors": [ { "name": "Jane Austen", "role": "AUTHOR" } ],
  "subjectCodes": ["FIC004000"],
  "format": "PAPERBACK",
  "listPriceCents": 999,
  "currency": "EUR",
  "availability": "IN_STOCK",
  "warehouseQuantity": 240,
  "releaseDate": "1813-01-28"
}
```
- `role`: AUTHOR, EDITOR, TRANSLATOR, ILLUSTRATOR. `format`: HARDCOVER, PAPERBACK, EBOOK.
- `availability`: IN_STOCK, BACKORDER, OUT_OF_PRINT.
- `subjectCodes`: BISAC-style codes; use these families so spec 06 can map them: `FIC` (fiction; `FIC028000`
  science fiction, `FIC009000` fantasy, `FIC022000` mystery, `FIC027000` romance), `HIS`, `SCI`, `COM`, `JUV`, `POE`.
- Errors use the distributor's own envelope, not ProblemDetail: `{ "errorCode": "TITLE_NOT_FOUND", "message": "..." }`.

## Seed data (`titles.json`)
- About 120 public-domain classics (authors who died before 1956) and about 30 fictional "new releases" by
  fictional authors. No living or recently deceased real people.
- EANs are valid ISBN-13s (checksum correct). They do not need to match real editions; the README states the data
  is fictional demo data.
- Seed entries for new releases use `releasedDaysAgo` (0 to 60) instead of a fixed date; the loader converts it to
  `releaseDate` relative to the start date, so the data never ages.
- At least 10 titles have TRANSLATOR or ILLUSTRATOR contributors (Level 2 contract-test exercise), at least 5 are
  BACKORDER, at least 5 are OUT_OF_PRINT, at least 3 have only an EBOOK format.
- 20 titles are "shared" with the bookstore seed (spec 09): all IN_STOCK, all with AUTHOR-only contributors, one of
  them 9780141439518 (Pride and Prejudice). Their ISBNs are listed one per line in
  `src/main/resources/catalog/bookstore-shared-isbns.txt`; spec 09 seeds exactly these into the bookstore, so
  restocking works in demos. This file is the single source of truth for the shared ISBNs.

## REST API
| Method and path | Response |
|---|---|
| `GET /api/titles?q=&subject=&availability=&page=0&size=20` | `{ "items": [Title], "page", "size", "total" }`; `q` matches title or contributor name, case-insensitive; `subject` matches a code prefix (`FIC` matches `FIC028000`); size max 50 |
| `GET /api/titles/{ean}` | `Title` or 404 envelope `TITLE_NOT_FOUND` |
| `GET /api/subjects` | `[ { "code": "FIC028000", "label": "Fiction / Science Fiction" } ]` |
| `POST /api/restock-orders` body `{ "ean", "quantity", "retailerReference" }` | 201 `{ "restockOrderId", "ean", "quantity", "status", "expectedDeliveryDate" }`; status CONFIRMED (IN_STOCK, delivery in 2 working days) or BACKORDERED (delivery in 14 days); 422 envelope `NOT_ORDERABLE` for OUT_OF_PRINT; 400 for quantity outside 1 to 500 |
| `GET /api/restock-orders/{id}` | the restock order or 404 |

A `java.time.Clock` bean is used for every date calculation (delivery dates, `releasedDaysAgo`), so tests can fix
the date.

Demo knobs (default off): `distributor.chaos.latency-ms` adds latency to every `/api` call,
`distributor.chaos.failure-rate` (0.0 to 1.0) returns 503 envelopes `TEMPORARILY_UNAVAILABLE`.

## MCP server
Dependency `org.springframework.ai:spring-ai-starter-mcp-server-webmvc`. Configuration:
```yaml
server:
  port: 8090
  address: 127.0.0.1
spring:
  ai:
    mcp:
      server:
        name: distributor-catalog
        version: 1.0.0
        type: SYNC
        protocol: STREAMABLE        # always set: SSE is deprecated
        instructions: >
          Read-only catalogue of the book distributor. Use search_titles to find titles, get_title for details,
          check_availability before suggesting a title to stock. Prices are in cents.
```
Tools (class `nl.openvalue.distributor.mcp.CatalogMcpTools`, Spring AI 2.0 `@McpTool` / `@McpToolParam`
annotations, package `org.springframework.ai.mcp.annotation`; fall back to `@Tool` + `MethodToolCallbackProvider`
if the annotation scanner does not pick them up):

| Tool | Parameters | Returns |
|---|---|---|
| `search_titles` | `query` (required), `subject` (optional code prefix), `limit` (optional, default 10, max 20) | compact list: ean, titleName, authors (AUTHOR role only, joined), subjectCodes, availability, listPriceCents |
| `get_title` | `ean` (required) | the full Title JSON, or a tool error "No title with EAN ..." |
| `list_subjects` | none | subject codes and labels |
| `check_availability` | `ean` (required) | availability, warehouseQuantity, expected delivery for a restock of 10 |

Tool descriptions must say when to use the tool and what the units are (cents, EAN = ISBN-13). Keep tool output
small: results are capped, and no tool result's text content is longer than about 4 000 characters.

STDIO fallback: profile `stdio` sets `spring.ai.mcp.server.stdio=true`, `spring.main.web-application-type=none`,
`spring.main.banner-mode=off`, an empty `logging.pattern.console` and `logging.file.name=distributor-stdio.log`.
*Verify at build time* that the webmvc starter honours `stdio=true`; if it does not, add a small
`distributor-stdio` launcher module with `spring-ai-starter-mcp-server` instead.

## Requirements

### Requirement: Search titles over REST
The distributor SHALL return paged titles matching the query, subject prefix and availability filters.

#### Scenario: Subject prefix
- **WHEN** a client calls `GET /api/titles?subject=FIC028&size=5`
- **THEN** at most 5 science-fiction titles are returned and `total` counts all matches

#### Scenario: Unknown EAN
- **WHEN** a client calls `GET /api/titles/9780000000002`
- **THEN** the response is 404 with `{ "errorCode": "TITLE_NOT_FOUND" }`

### Requirement: Restock orders
The distributor SHALL accept restock orders for orderable titles and compute an expected delivery date.

#### Scenario: In-stock title
- **GIVEN** an IN_STOCK title
- **GIVEN** the `Clock` fixed on a Friday
- **WHEN** a restock order for 10 units is posted
- **THEN** the response is 201 with status CONFIRMED and an expected delivery date on the next Tuesday (2 working days)

#### Scenario: Out of print
- **WHEN** a restock order is posted for an OUT_OF_PRINT title
- **THEN** the response is 422 with errorCode `NOT_ORDERABLE`

### Requirement: MCP tools are discoverable and callable
The distributor SHALL expose the four read-only tools over MCP Streamable HTTP at `/mcp`.

#### Scenario: Automated MCP round trip
- **GIVEN** the distributor running on a random port in a `@SpringBootTest`
- **WHEN** a test MCP client, built programmatically with the MCP Java SDK's Streamable HTTP client transport against the `@LocalServerPort` (no client starter, no fixed port), lists tools and calls `search_titles` with query "austen"
- **THEN** exactly the four tools are listed and the result contains "Pride and Prejudice"

#### Scenario: Output stays small
- **WHEN** `search_titles` is called with query "a" and limit 50
- **THEN** at most 20 results are returned and the tool result's text content is under 4 000 characters

### Requirement: Claude Code can connect
The MCP server SHALL work with the Claude Code version used on the workshop day.

#### Scenario: Manual smoke test (run on two machines, record the Claude Code version in the PR)
- **GIVEN** the distributor running with `./mvnw -pl distributor spring-boot:run`
- **WHEN** the tester runs `claude mcp add --transport http distributor http://localhost:8090/mcp` and opens `/mcp` in Claude Code
- **THEN** the server shows as connected with 4 tools, and the prompt "Use the distributor to find books by Mary Shelley" results in a call to `mcp__distributor__search_titles`

Note: the current MCP specification is 2026-07-28, while Spring AI 2.0.2 ships a Java SDK that tracks 2025-11-25.
The smoke test is the proof that version negotiation works; if it fails, use the STDIO fallback and report it.

## Tests (required)
- `TitleCatalogTest` (search, prefix, paging), `RestockServiceTest` (working-day calculation, status rules),
  `SeedDataTest` (every EAN has a valid checksum; the counts listed under seed data hold; every ISBN in
  `bookstore-shared-isbns.txt` exists, is IN_STOCK and has only AUTHOR contributors).
- `TitlesApiTest`, `RestockOrdersApiTest` (`@WebMvcTest`).
- `McpServerIntegrationTest` (MCP client round trip, output size).

## Tasks
1. Domain records, JSON loader with `releasedDaysAgo` conversion, `titles.json` seed with the constraints above.
2. Search and restock services, REST controllers, error envelope advice, chaos knobs.
3. MCP tools class, configuration, MCP integration test.
4. STDIO profile and its verification.
5. `finalName` `distributor`, `http/distributor.http`, short `distributor/README.md` (run, REST, MCP, Claude Code command).
6. Manual Claude Code smoke test on two machines.

## Definition of done
- `./mvnw verify` green; MCP round-trip test green; Claude Code smoke test recorded.
- `java -jar distributor/target/distributor.jar` starts in under 10 seconds.
