# Shape: Spec 05 Distributor service

Source of truth: `docs/spec-pack/specs/05-distributor-service.md`. This file records the shaping notes and the engineering decisions the pack spec leaves open. `plan.md` holds the task cards.

## Scope

- Add to the existing `distributor` Maven module (plain Spring Boot 4.1.2 app, packages `api`, `mcp`, `catalog` under `nl.openvalue.distributor`): an in-memory catalogue of about 150 titles loaded from `titles.json`, a REST API under `/api` with its own error envelope, chaos knobs (default off), an MCP server (Spring AI 2.0.2, Streamable HTTP, SYNC) at `/mcp` with four read-only tools, a STDIO profile, and the executable jar `distributor/target/distributor.jar`.
- The bookstore onion architecture, its ArchUnit rules and the bookstore skills do not apply to the distributor ("someone else's system").
- Allowed paths: `distributor/**` and `http/distributor.http`. `bookstore/**` and the root `pom.xml` are not touched (the root already imports `spring-ai-bom` 2.0.2).
- Out of scope (pack spec): persistence, authentication, UI, MCP write tools, the `list_new_releases` tool (Lab 3 stretch exercise).

## Decisions

Decided by the orchestrator from the pack spec, tech-stack and the research; cheap and reversible engineering calls.

- D-a No `spring-boot-starter-validation`. The tech-stack lists only `web`, `actuator` and the MCP server starter for the distributor. Request checks are done by hand and answered with the envelope.
- D-b 400 envelope `INVALID_REQUEST` (the spec names no code) for: `size` below 1 or above 50, a negative or non-numeric `page` or `size`, an unknown `availability`, a malformed body, a missing, non-numeric or out-of-range (1 to 500) `quantity`, a blank `ean`. Request checks run before business checks (an OUT_OF_PRINT title with quantity 0 gives 400). REST rejects instead of clamping because a client computing offsets from the size it asked for would skip items. The MCP `limit` clamps to 1..20 (default 10), as the pack scenario requires.
- D-c The `Clock` bean is `Clock.system(ZoneId.of("Europe/Amsterdam"))`. Every date calculation (`releasedDaysAgo`, delivery dates) uses `LocalDate.now(clock)`. Tests use fixed clocks; zone falsifiers pin the zone.
- D-d Pack task 6 (manual Claude Code smoke test on two machines) is not an implementation task. The orchestrator runs a headless single-machine proxy in the final sweep (`claude -p` with a temporary `--mcp-config` file, no change to the user's Claude Code configuration) and records the Claude Code version in the spec PR. The two-machine run is a manual check for the owner.
- D-e No Origin or Host validation on `/mcp` and no MCP capability trimming. The tech-stack's posture is the loopback bind; the tools are read-only and the data is fictional.
- L1 `POST /api/restock-orders` with an unknown EAN gives 404 `TITLE_NOT_FOUND` (spec 06 maps it to `TitleNotFoundAtDistributorException`).
- L2 `GET /api/restock-orders/{id}` with an unknown id gives 404 `RESTOCK_ORDER_NOT_FOUND`. The GET body equals the 201 body (five keys). `restockOrderId` is a random UUID string. `retailerReference` is optional, stored and not echoed. A restock never changes `warehouseQuantity`; CONFIRMED does not depend on the quantity compared with stock.
- L3 Subject labels live in `distributor/src/main/resources/catalog/subjects.json` (array of `{code, label}`, sorted by code). `GET /api/subjects` and `list_subjects` return it in code order.
- L4 `search_titles` enforces the 4000-character cap in code: drop trailing items until the serialised text is under 4000.
- L5 The seed loader uses Boot's `JsonMapper` with a strict per-call reader (`FAIL_ON_UNKNOWN_PROPERTIES`), not a custom mapper.
- L6 The 20 shared ISBNs are IN_STOCK classics with fixed release dates and only AUTHOR contributors.
- L7 The STDIO profile carries the spec's five settings verbatim plus `logging.threshold.console: "OFF"` (silences a logback stderr error caused by the empty console pattern; stdout stays JSON-RPC only).
- Search defaults: page 0, size 20; sorted by `titleName` case-insensitively, then `ean`; a blank `q` or `subject` is ignored; the subject prefix is compared case-insensitively against any of a title's codes; filters combine with AND.
- Known limit, accepted: unmapped `/api/...` paths fall to Boot's default 404 body, not the envelope (no scenario covers it).

## MCP recipe (research)

`spring-ai-starter-mcp-server-webmvc` (version from the BOM, MCP Java SDK 2.1.0, Jackson 3 only), a `@Component` with `@McpTool` methods, endpoint `/mcp` by default. Rules: set `protocol: STREAMABLE` explicitly (otherwise SSE starts); optional parameters are boxed types; tool errors are a `CallToolResult` with `isError(true)` (a thrown exception duplicates the message); every tool carries `@McpAnnotations(readOnlyHint = true, destructiveHint = false)`. Test client: `HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port).endpoint("/mcp")` with `McpClient.sync`, available through the server starter. The webmvc starter honours `spring.ai.mcp.server.stdio=true`, so no launcher module is needed.

## Scenario-to-test map (planned)

| Pack scenario | Primary test | Supporting tests |
|---|---|---|
| Subject prefix | `TitlesApiTest.subjectPrefixReturnsAtMostSizeItemsAndTotalCountsAllMatches` | `TitleCatalogTest.totalCountsAllMatchesNotJustThePage`, `TitleCatalogTest.subjectIsAPrefixNotAContainsMatch`, `SeedDataTest.atLeastSixScienceFictionTitles` |
| Unknown EAN | `TitlesApiTest.unknownEanIs404WithTitleNotFoundEnvelope` | `SeedDataTest.scenarioUnknownEanIsAbsent` |
| In-stock title (Friday to Tuesday) | `RestockOrdersApiTest.inStockTitleOnFridayIsConfirmedForNextTuesday` | `RestockServiceTest` weekday cases, `RestockServiceTest.deliveryDateUsesTheClockZone` |
| Out of print | `RestockOrdersApiTest.outOfPrintTitleIs422NotOrderable` | `RestockServiceTest.outOfPrintIsNotOrderableAndStoresNothing` |
| Automated MCP round trip | `McpServerIntegrationTest.automatedRoundTripListsFourToolsAndFindsPrideAndPrejudice` | `McpStdioModeTest.stdioListsTheFourAnnotatedTools` |
| Output stays small | `McpServerIntegrationTest.searchTitlesOutputStaysSmall` | `CatalogMcpToolsTest.limitIsCappedAtTwenty`, `CatalogMcpToolsTest.resultTextIsTrimmedUnderTheCap` |
| Manual smoke test (Claude Code) | not automated: headless proxy in the final sweep plus the owner's two-machine check | `McpServerIntegrationTest`, `McpStdioModeTest` |

The implementers' outbox artifacts record the final test method names; qa-final checks the map against them.
