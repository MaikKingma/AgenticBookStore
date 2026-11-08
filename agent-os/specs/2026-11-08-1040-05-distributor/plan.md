---
spec: 05-distributor
source: docs/spec-pack/specs/05-distributor-service.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-08T10:59:42Z
qa-lead: on
base: factory
spec-branch: sdd/05-distributor/spec
modules: [root, bookstore, distributor]
---

# Plan: Spec 05 Distributor service

Restates `docs/spec-pack/specs/05-distributor-service.md` (source of truth). Shaping notes and decisions D-a to D-e and L1 to L7 are in `shape.md`, standards in `standards.md`, research summary in `references.md`. The full task cards (risks, seed approach, startup proof) come from the tech-lead strategy; the QA strategy (tiers, Gate 1 findings G1 to G18, common directive C0 and per-task directives T<n>-1..k, qa-final focus) is in the QA lead's ledger. Both live in the cycle's comms and are passed to every agent by path: `.claude/comms/mailboxes/lead-1/outbox/strategy-05-distributor.md` (section 4) and `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-05-distributor.md` (section "QA strategy"). Where a G finding and a card below differ, the finding wins.

Path abbreviations: M = `distributor/src/main/java/nl/openvalue/distributor/`, R = `distributor/src/main/resources/`, T = `distributor/src/test/java/nl/openvalue/distributor/`.

## Summary

Turn the bare `distributor` skeleton into the fictional book wholesaler: an in-memory catalogue of about 150 titles from `titles.json` (120 public-domain classics, 30 fictional new releases with `releasedDaysAgo`), subject labels from `subjects.json`, 20 shared ISBNs in `bookstore-shared-isbns.txt`; search and restock services on a `Clock` (Europe/Amsterdam); REST under `/api` with the `{errorCode, message}` envelope; chaos knobs on `/api/**`; the Spring AI MCP server (Streamable HTTP, SYNC, `distributor-catalog` 1.0.0) at `/mcp` with four read-only tools; a STDIO profile; `distributor/target/distributor.jar`; `distributor/README.md`; `http/distributor.http`. No change to `bookstore/` or the root `pom.xml`. Referee `./mvnw clean verify` from the repo root, green after every task, budget 60 s (baseline 18.3 s: bookstore 667 tests, distributor 3).

## Ground rules for every task

- The pack spec decides what; `docs/spec-pack/product/tech-stack.md` and `docs/spec-pack/orchestrator/decisions.md` win. The bookstore architecture standard, ArchUnit rules and skills do not apply to the distributor. A contradiction is reported as blocked, never resolved by the implementer.
- Only the task's fileScope is edited, never anything else, not even temporarily. Never `bookstore/**`, never the root `pom.xml`, never `DistributorApplication.java`, `DistributorApplicationTests.java` or `HealthEndpointTest.java`.
- Never skip, disable, delete or weaken a test; no `@Disabled`, `assume*`, `@DirtiesContext`, `@MockitoBean` (slices use the real in-memory services; a plain `Mockito.spy` in a unit test is allowed); never `--no-verify`.
- Versions only from tech-stack.md; the only new dependency of the cycle is `spring-ai-starter-mcp-server-webmvc` (T3, version from the BOM). No validation starter, no `ProblemDetail`, no `ResponseEntityExceptionHandler`, no custom ObjectMapper (use Boot's injected `JsonMapper`).
- Time only through the `Clock` bean (`LocalDate.now(clock)`); every fixed clock in a test lies in a year before 2026 (G2, G8); zone falsifiers use an instant whose UTC and Amsterdam dates differ.
- Plain English, no em dashes; every new or changed file is ASCII (`rg -n '[^\x00-\x7F]' <file>` empty; never type a backslash-u escape into a Write or Edit parameter). Product files never reference `docs/spec-pack`, `agent-os`, `.claude`, `CLAUDE.md`. Never `ANTHROPIC_API_KEY`. No `list_new_releases`.
- Git is read-only for every agent; the orchestrator owns commits, branches and PRs. Never run `claude mcp add` or anything that writes the user's Claude Code configuration.
- Falsifier tests named in a card and in the QA lead's T<n>-2 list are required and written first. In a test-only rework round never edit a production file; describe the mutant the new test kills in the outbox (G18).
- Ordered claims use at least three rows sorted in no column, ties inserted opposite to the asserted order. Contract values are literals in tests.
- Every implementer records its final test method names in its outbox as the task's rows of the scenario-to-test map (`shape.md` holds the planned names).
- Scratch and evidence files: session scratchpad `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/` with prefix `s05-T<n>-<role>-`. Long runs through `bash ~/.claude/scripts/with-budget.sh <s> ...`. One live distributor at a time (port 8090; use 18090 if a foreign process holds it and say why).
- Context gate (G1): `Started .* in .* seconds` lines in the distributor section of the referee log: 2 after T1 to T3, 4 after T4, 5 from T5 on; from T6 on the STDIO context's line is quoted from its log under `distributor/target/`.

## Task table

| id | summary | module | dependsOn | batch | qaTier | qaModel | implModel | status | verdict | qa-lead | rounds | pr | commit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Catalogue model, seed loader, Clock bean, seed data (titles, subjects, shared ISBNs) | distributor | [] | 1 | critical | opus | opus | done (branch `sdd/05-distributor/T1`) | PASS (r1) | ENDORSE | 1 | #44 | 5827249 |
| T2 | Search and restock services | distributor | [T1] | 2 | elevated | sonnet | sonnet | done (branch `sdd/05-distributor/T2`) | PASS (r1) | ENDORSE | 1 | #45 | af7aaf1 |
| T3 | MCP server, four read-only tools, round-trip test | distributor | [T2] | 3 | critical | opus | opus | done (branch `sdd/05-distributor/T3`) | PASS (r2) | ENDORSE | 2 | #50 | a0737c9 |
| T4 | REST API under `/api` and the error envelope | distributor | [T2, T3] | 4 | critical | opus | sonnet | done (branch `sdd/05-distributor/T4`) | PASS (r2) | ENDORSE | 2 | #52 | 9afb516 |
| T5 | Chaos knobs on `/api/**` | distributor | [T4] | 5 | elevated | sonnet | sonnet | pending | | | | | |
| T6 | STDIO profile and its in-process verification | distributor | [T3, T5] | 6 | critical | opus | opus | pending | | | | | |
| T7 | finalName `distributor`, README, startup proof | distributor | [T5, T6] | 7 | elevated | sonnet | sonnet | pending | | | | | |
| T8 | `http/distributor.http`, replayed live | root | [T1, T4] | 8 | standard | sonnet | sonnet | pending | | | | | |

Execution and merge order, one task at a time: T1, T2, T3, T4, T5, T6, T7, T8 (T8 runs last so its live replay uses the final jar; the lead's batch B5 pairing with T5 only matters for parallel runs, which this cycle does not do). Pack task mapping: pack 1 = T1; pack 2 = T2 + T4 + T5; pack 3 = T3; pack 4 = T6; pack 5 = T7 + T8; pack 6 (manual Claude Code smoke on two machines) = not an implementation task (D-d): orchestrator headless proxy in the final sweep plus the owner's two-machine check.

qaDirective of every task: C0 plus the task's T<n>-1..k section of the QA ledger, plus every `## QA feed-forward` entry recorded before the task starts.

## Tasks

### T1: Catalogue model, seed loader, Clock and seed data

- module: distributor; dependsOn: []; implModel opus (about 150 factual entries under the no-living-people rule).
- fileScope: M `catalog/Title.java`, `catalog/Contributor.java`, `catalog/ContributorRole.java`, `catalog/Format.java`, `catalog/Availability.java`, `catalog/Subject.java`, `catalog/TitleSeed.java`, `catalog/CatalogLoader.java`, `catalog/TitleCatalog.java`, `catalog/CatalogConfig.java`; R `catalog/titles.json`, `catalog/subjects.json`, `catalog/bookstore-shared-isbns.txt`; T `catalog/SeedDataTest.java`, `catalog/CatalogLoaderTest.java`, `catalog/CatalogConfigTest.java`.
- acceptanceCriteria:
  - Wire record `Title(ean, titleName, contributors, subjectCodes, format, listPriceCents, currency, availability, warehouseQuantity, releaseDate)` with exactly the ten spec keys; `Contributor(name, role)`; enums `ContributorRole` (AUTHOR, EDITOR, TRANSLATOR, ILLUSTRATOR), `Format` (HARDCOVER, PAPERBACK, EBOOK), `Availability` (IN_STOCK, BACKORDER, OUT_OF_PRINT); `Subject(code, label)`; seed record `TitleSeed` with nullable `releaseDate` and `releasedDaysAgo` (exactly one set). `TitleCatalog` offers all titles, `findByEan` (Optional) and subjects (no search yet). `CatalogConfig` provides `Clock.system(ZoneId.of("Europe/Amsterdam"))` and the `TitleCatalog` bean built by `CatalogLoader`. `CatalogLoader` uses Boot's `JsonMapper` with a strict per-call reader (`FAIL_ON_UNKNOWN_PROPERTIES`), converts `releasedDaysAgo` to `LocalDate.now(clock).minusDays(n)`, and fails fast naming the EAN for: both dates, no date, `releasedDaysAgo` outside 0..60, duplicate EAN, unknown property.
  - Seed per the lead's seed approach: one title per line; P&P exactly as the spec example (`9780141439518`, `Jane Austen` AUTHOR, `FIC004000`, PAPERBACK, 999, EUR, IN_STOCK, 240, `1813-01-28`); other EANs `97899` + 7-digit serial + check digit; about 120 classics (contributors who died before 1956; the lead's deny list absent) and about 30 fictional new releases (`releasedDaysAgo` spread over 0..60 including 0 and 60); at least 10 titles with TRANSLATOR or ILLUSTRATOR, 5 BACKORDER, 5 OUT_OF_PRINT, 3 EBOOK, 6 science fiction (`FIC028*`); every spec 06 mapping key present on a FIRST subject code; `subjects.json` sorted by code, every label used and every used code labelled, all codes in families FIC, HIS, SCI, COM, JUV, POE; `bookstore-shared-isbns.txt` exactly 20 distinct ISBNs one per line (no header, no blank line, trailing newline), all IN_STOCK classics with only AUTHOR contributors, including 9780141439518; 9780000000002 absent; IN_STOCK iff `warehouseQuantity` above 0; every title importable by the bookstore (EUR, 1..50000 cents, 1..5 AUTHOR names up to 120 chars, title up to 200 chars, no surrounding whitespace, release year 1450 or later; classics 1450..1955); all three files ASCII.
  - `SeedDataTest` (plain JUnit, loader with `JsonMapper.builder().build()` and a fixed clock before 2026): `everyEanIsAValidIsbn13`, `isbnChecksumHelperRejectsAWrongCheckDigit` (G3: accepts 9780141439518, 9780000000002, 9780141439600; rejects 9780141439519, 4006381333931, 12 and 14 digits, a letter), `eansAreUnique`, `seedHasAbout150TitlesWithAbout120ClassicsAnd30NewReleases` (bands 140..160, 110..130, 25..35), `everyEntryHasExactlyOneOfReleaseDateOrReleasedDaysAgo`, `releasedDaysAgoIsBetween0And60`, `classicsAreReleasedBetween1450And1955`, `atLeastTenTitlesHaveATranslatorOrIllustrator`, `atLeastFiveTitlesAreBackorder`, `atLeastFiveTitlesAreOutOfPrint`, `atLeastThreeTitlesAreEbookOnly`, `sharedIsbnFileListsExactly20DistinctIsbnsOnePerLine`, `everySharedIsbnExistsIsInStockAndHasOnlyAuthorContributors`, `sharedIsbnsAreClassics`, `prideAndPrejudiceMatchesTheSpecExample` (ten literals), `everySubjectCodeHasALabelAndEveryLabelIsUsed`, `subjectFamiliesCoverTheSpec06Mapping` (first code, G6a), `everySubjectCodeIsInASpecFamily` (G6b), `atLeastSixScienceFictionTitles`, `scenarioUnknownEanIsAbsent`, `janeAustenAndMaryShelleyArePresent` (G6d: "Frankenstein" by exactly "Mary Shelley", a second Austen title), `availabilityAndWarehouseQuantityAgree`, `everyTitleIsImportableByTheBookstore` (G6c), `seedFilesAreAscii`. Line-ending neutral (G6e).
  - `CatalogLoaderTest`: `releasedDaysAgoIsConvertedRelativeToTheClock` (G2: `Clock.fixed(2000-06-15T10:00:00Z, Europe/Amsterdam)`, 5 days gives 2000-06-10), `releasedDaysAgoUsesTheClockZone` (2000-01-14T23:30:00Z, 0 days gives 2000-01-15), `entryWithBothDatesFailsNamingTheEan`, `entryWithNoDateFails`, `releasedDaysAgoAbove60OrBelow0Fails` (0 and 60 accepted, -1 and 61 rejected), `duplicateEanFails` (A, B, A), `unknownJsonPropertyFails` (G4: mapper built with the feature disabled), `subjectsAreLoadedSortedByCode` (G5d), `seedEntryIsMappedFieldByField` (G5e). Every rejection asserts the exception type and the EAN literal in the message (G5b).
  - `CatalogConfigTest.productionClockUsesEuropeAmsterdam` (zone and ticking within 5 s of real time, G6f).
  - `DistributorApplicationTests` and `HealthEndpointTest` stay green unedited. Outbox: table of every classic contributor with death year, list of every fictional new-release author (G7).

### T2: Search and restock services

- module: distributor; dependsOn: [T1]; implModel sonnet.
- fileScope: M `catalog/TitleCatalog.java` (edit), `catalog/TitlePage.java`, `catalog/RestockService.java`, `catalog/RestockOrder.java`, `catalog/RestockStatus.java`, `catalog/DeliveryQuote.java`, `catalog/TitleNotFoundException.java`, `catalog/NotOrderableException.java`; T `catalog/TitleCatalogTest.java`, `catalog/RestockServiceTest.java`.
- acceptanceCriteria:
  - `TitleCatalog.search(q, subjectPrefix, availability, page, size)` returns `TitlePage(items, page, size, total)`: `q` case-insensitive contains match on titleName or any contributor name; `subject` case-insensitive prefix on any of the title's codes; `availability` exact; blank `q`/`subject` ignored; AND; sorted by titleName case-insensitively then ean; `total` counts all matches.
  - `RestockService`: `Optional<DeliveryQuote> quote(Title)` (IN_STOCK -> CONFIRMED plus 2 working days, Saturday and Sunday skipped; BACKORDER -> BACKORDERED plus 14 calendar days; OUT_OF_PRINT -> empty); `placeOrder(ean, quantity, retailerReference)` (throws `TitleNotFoundException` or `NotOrderableException`; stores a `RestockOrder` with a random UUID string id in a `ConcurrentHashMap`; never changes `warehouseQuantity`; CONFIRMED does not depend on stock); `Optional<RestockOrder> findOrder(id)`; package-private `int orderCount()` (G10a). Dates via `LocalDate.now(clock)`.
  - `TitleCatalogTest` (fixture list): `qMatchesTitleCaseInsensitively`, `qIsAContainsMatch`, `qMatchesAnyContributorIncludingTranslators` (G9b), `blankQIsIgnored` and `blankSubjectIsIgnored` (null, "", "   "), `subjectMatchesACodePrefix`, `subjectIsAPrefixNotAContainsMatch`, `subjectPrefixIsCaseInsensitive`, `subjectMatchesAnyOfTheTitlesCodes` (G9a), `availabilityFiltersExactly`, `filtersCombineWithAnd` (G9d), `resultsAreSortedByTitleNameThenEan` (G9e), `pagingReturnsTheRequestedSlice` (G9f), `totalCountsAllMatchesNotJustThePage`, `pageBeyondTheEndIsEmptyWithTheFullTotal` (includes `Integer.MAX_VALUE`), `findByEanIsEmptyForAnUnknownEan`; TitlePage page and size asserted as non-default literals (G9g).
  - `RestockServiceTest` (dates per G8, November 2021): `inStockOrderOnFridayIsConfirmedForTuesday` (2021-11-05 -> 2021-11-09), `orderOnThursdayIsDeliveredOnMonday`, `orderOnSaturdayIsDeliveredOnTuesday`, `orderOnSundayIsDeliveredOnTuesday`, `orderOnMondayIsDeliveredOnWednesday`, `orderOnWednesdayIsDeliveredOnFriday`, `backorderIsBackorderedForFourteenCalendarDays` (-> 2021-11-19), `outOfPrintIsNotOrderableAndStoresNothing` and `unknownEanThrowsTitleNotFound` (orderCount 0), `deliveryDateUsesTheClockZone` (2021-11-04T23:30:00Z -> 2021-11-09), `placedOrderIsFoundById` (quantities 10 and 37, retailerReference literal and null, G10b-c), `unknownOrderIdIsEmpty`, `twoOrdersGetDistinctIds` (UUID form, G10d), `quoteForOutOfPrintIsEmpty`, `inStockOrderAboveTheWarehouseQuantityIsStillConfirmed` (G10e).

### T3: MCP server and tools

- module: distributor; dependsOn: [T2]; implModel opus.
- fileScope: `distributor/pom.xml`; R `application.yml`; M `mcp/CatalogMcpTools.java`, `mcp/TitleSummary.java`, `mcp/AvailabilityReport.java`; T `mcp/McpServerIntegrationTest.java`, `mcp/CatalogMcpToolsTest.java`.
- acceptanceCriteria:
  - `distributor/pom.xml` gains `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` without a version. `application.yml` gains the spec's `spring.ai.mcp.server` block verbatim (name `distributor-catalog`, version `1.0.0`, type SYNC, `protocol: STREAMABLE`, the instructions text); server port and address unchanged.
  - `@Component CatalogMcpTools` with exactly four `@McpTool` methods `search_titles` (`query` required, `subject` optional, `limit` optional boxed `Integer`, clamped 1..20, null -> 10), `get_title` (`ean`), `list_subjects`, `check_availability` (`ean`); each with `@McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false)`; descriptions say when to use the tool and the units (cents, EAN = ISBN-13). `search_titles` returns a JSON array of `TitleSummary(ean, titleName, authors, subjectCodes, availability, listPriceCents)` (authors = AUTHOR names in order joined with ", "), trimmed by dropping trailing items until the text is under 4000 characters. `get_title` returns the full Title JSON or a `CallToolResult` with `isError(true)` and the single line "No title with EAN <ean>". `check_availability` returns `AvailabilityReport(ean, availability, warehouseQuantity, expectedDeliveryDate)` from `RestockService.quote` (null date when not orderable; unknown EAN is the same tool error). No `@Tool`, no `MethodToolCallbackProvider`.
  - `McpServerIntegrationTest` (same annotations as `HealthEndpointTest` so both share one context; client `HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port).endpoint("/mcp")` + `McpClient.sync`, closed after each test; no client starter, no fixed port): `automatedRoundTripListsFourToolsAndFindsPrideAndPrejudice`, `searchTitlesOutputStaysSmall` (query "a", limit 50: 1..20 items, text under 4000), `everyToolIsReadOnlyAndNotDestructive`, `toolDescriptionsStateUnits` (G11), `getTitleForAnUnknownEanIsASingleLineToolError`, `getTitleReturnsTheFullTitle`, `checkAvailabilityReportsStockAndDelivery`, `listSubjectsReturnsCodesAndLabels`, `serverIdentifiesAsDistributorCatalog`; text of every tool under 4000 (G12c); exact key sets (G12d).
  - `CatalogMcpToolsTest` (unit, fixture catalogue, Boot-default `JsonMapper`, fixed clock in November 2021): `limitDefaultsToTen`, `limitIsCappedAtTwenty` (G12f), `limitBelowOneIsRaisedToOne`, `authorsListsOnlyAuthorRoleJoinedInOrder`, `resultTextIsTrimmedUnderTheCap` (G12e), `subjectNarrowsTheSearch` (G12a), `checkAvailabilityDoesNotPlaceAnOrder`, `checkAvailabilityForOutOfPrintHasNoDeliveryDate`, `checkAvailabilityForBackorderIsFourteenDaysOut`, `checkAvailabilityForInStockIsTwoWorkingDaysOut`, `checkAvailabilityForAnUnknownEanIsTheSingleLineToolError` (G12b).
  - Outbox: informal start time of the SNAPSHOT jar (early warning only).

### T4: REST API and error envelope

- module: distributor; dependsOn: [T2, T3]; implModel sonnet.
- fileScope: M `api/TitlesController.java`, `api/RestockOrdersController.java`, `api/RestockOrderRequest.java`, `api/RestockOrderResponse.java`, `api/ErrorEnvelope.java`, `api/ApiExceptionHandler.java`, `api/InvalidRequestException.java`, `api/RestockOrderNotFoundException.java`; T `api/TitlesApiTest.java`, `api/RestockOrdersApiTest.java`, `api/ApiTestFixtures.java`.
- acceptanceCriteria:
  - Endpoints per the pack REST table with D-b and L1, L2: `GET /api/titles` (page 0, size 20 defaults; 400 `INVALID_REQUEST` for size outside 1..50, negative or non-numeric page or size, unknown availability), `GET /api/titles/{ean}` (404 `TITLE_NOT_FOUND`), `GET /api/subjects` (code order), `POST /api/restock-orders` (201 with exactly `restockOrderId`, `ean`, `quantity`, `status`, `expectedDeliveryDate`; 422 `NOT_ORDERABLE`; 404 `TITLE_NOT_FOUND`; 400 `INVALID_REQUEST` for missing, non-numeric or out-of-range quantity, blank or missing ean, malformed body, checked before the business rules), `GET /api/restock-orders/{id}` (same five keys, or 404 `RESTOCK_ORDER_NOT_FOUND`). `ApiExceptionHandler` is `@RestControllerAdvice(basePackageClasses = TitlesController.class)`, does not extend `ResponseEntityExceptionHandler`, has no catch-all; 422 via `HttpStatus.UNPROCESSABLE_CONTENT`. Every error body is `application/json` with exactly `errorCode` and `message`.
  - `TitlesApiTest` (`@WebMvcTest(TitlesController.class)`, fixture catalogue bean in a nested `@TestConfiguration`): `subjectPrefixReturnsAtMostSizeItemsAndTotalCountsAllMatches`, `unknownEanIs404WithTitleNotFoundEnvelope`, `titleByEanHasTheWireShape`, `pageResponseHasExactlyItemsPageSizeTotal`, `defaultsArePageZeroSizeTwenty`, `qMatchesTitleOrContributorCaseInsensitively`, `availabilityFilterApplies`, `unknownAvailabilityIs400InvalidRequest`, `size50IsAccepted`, `size51Is400InvalidRequest`, `size0Is400`, `negativePageIs400`, `nonNumericPageIs400WithTheEnvelope`, `subjectsAreReturnedWithCodesAndLabelsInCodeOrder`, plus G13b `pageAndSizeArePassedThrough`, G13c bound pairs (size -1, 1; `nonNumericSizeIs400WithTheEnvelope`), G13e `hugePageIsEmpty`.
  - `RestockOrdersApiTest` (`@WebMvcTest(RestockOrdersController.class)`, `@Import(RestockService.class)`, `Clock.fixed(2021-11-05T10:00:00Z, Europe/Amsterdam)` per G8): `inStockTitleOnFridayIsConfirmedForNextTuesday` (2021-11-09), `backorderTitleIsBackorderedFourteenDaysOut` (2021-11-19), `outOfPrintTitleIs422NotOrderable`, `unknownEanIs404TitleNotFound`, `quantity0Is400InvalidRequest`, `quantity501Is400InvalidRequest`, `quantities1And500AreAccepted`, `missingQuantityIs400`, `nonNumericQuantityIs400WithTheEnvelope`, `malformedJsonIs400WithTheEnvelope`, `blankEanIs400`, `invalidQuantityIsRejectedBeforeOrderabilityIsChecked`, `placedOrderIsFetchedById`, `unknownRestockOrderIs404RestockOrderNotFound`, plus G13c (quantity -1, `missingEanIs400`, whitespace ean) and G13d (unknown EAN with quantity 501 gives 400). Every 4xx asserts content type, exact keys, literal code, non-blank message (G13f).

### T5: Chaos knobs

- module: distributor; dependsOn: [T4]; implModel sonnet.
- fileScope: M `api/ChaosProperties.java`, `api/ChaosInterceptor.java`, `api/ChaosWebConfig.java`, `api/TemporarilyUnavailableException.java`, `api/ApiExceptionHandler.java` (additions only); R `application.yml` (edit); T `api/ChaosInterceptorTest.java`, `api/ChaosApiTest.java`.
- acceptanceCriteria:
  - `ChaosProperties` record `@ConfigurationProperties("distributor.chaos")` (`latencyMs` default 0, `failureRate` default 0.0; rejects negative latency and a rate outside 0.0..1.0). `ChaosInterceptor` (properties, `RandomGenerator`, sleeper `LongConsumer`): sleeps first, then throws `TemporarilyUnavailableException` when `random.nextDouble() < failureRate`, no random draw at 0.0. `ChaosWebConfig` (`@Component implements WebMvcConfigurer`, `@EnableConfigurationProperties(ChaosProperties.class)`, `RandomGenerator.getDefault()`, `/api/**` only). The advice maps the exception to 503 `TEMPORARILY_UNAVAILABLE`. `application.yml` documents both keys with their defaults.
  - `ChaosInterceptorTest`: `failureRateZeroNeverFailsAndDoesNotDrawARandom`, `failureRateOneAlwaysFails`, `failsWhenTheRandomIsBelowTheRate`, `passesWhenTheRandomEqualsTheRate`, `latencyIsAppliedBeforeHandling`, `zeroLatencyDoesNotSleep`, `propertiesRejectAFailureRateOutsideZeroToOne`, `propertiesRejectANegativeLatency`, `latencyIsAppliedAlsoWhenTheCallFails` (G14c), bound pairs per G14d.
  - `ChaosApiTest` (`@WebMvcTest(TitlesController.class)`, `distributor.chaos.failure-rate=1.0`, `distributor.chaos.latency-ms=5`): `apiCallsReturn503TemporarilyUnavailableEnvelope` (also `/api/titles/{ean}`, G14a), `nonApiPathsAreNotAffected`, `chaosPropertiesBindFromTheDocumentedKeys` (G14b).
  - `TitlesApiTest` and `RestockOrdersApiTest` stay green unedited.

### T6: STDIO profile and its verification

- module: distributor; dependsOn: [T3, T5]; implModel opus (process-global streams and logback state in a shared Surefire fork, P1 in the QA ledger).
- fileScope: R `application-stdio.yml`; T `mcp/McpStdioModeTest.java`.
- acceptanceCriteria:
  - `application-stdio.yml` with the spec's five settings verbatim (`spring.ai.mcp.server.stdio: true`, `spring.main.web-application-type: none`, `spring.main.banner-mode: "off"`, `logging.pattern.console: ""`, `logging.file.name: distributor-stdio.log`) plus `logging.threshold.console: "OFF"` (L7).
  - `McpStdioModeTest` (in process: swap `System.in`/`System.out` before `SpringApplicationBuilder(DistributorApplication.class).profiles("stdio")`, log file under `target/`, 5 s deadline, restore and close in `finally`): `stdioProfileAnswersInitializeOnStdout`, `stdioProfileStartsNoWebServer`, `stdioListsTheFourAnnotatedTools`, `stdoutCarriesOnlyJsonRpcLines` (G15a: at least two lines, each JSON-RPC 2.0, ids exactly the ids sent, no filtering). All four hold in both class orders of the shared fork (G15b). No stray `distributor-stdio.log` outside `target/`.

### T7: Packaging, README and startup proof

- module: distributor; dependsOn: [T5, T6]; implModel sonnet.
- fileScope: `distributor/pom.xml`, `distributor/README.md`.
- acceptanceCriteria:
  - `<finalName>distributor</finalName>`; the referee produces `distributor/target/distributor.jar`.
  - `distributor/README.md` (plain English, ASCII, about 80 lines at most, no agent paths): what it is (fictional demo data), run (`./mvnw -pl distributor spring-boot:run`, `java -jar distributor/target/distributor.jar`), REST endpoints and error codes, MCP (`/mcp`, the four tools, `claude mcp add --transport http distributor http://localhost:8090/mcp`, fallback `http://127.0.0.1:8090/mcp`), STDIO (`claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio`, where `distributor-stdio.log` is written; G16c), chaos knobs, loopback note.
  - Every README command except the `claude mcp add` lines is run once with quoted output; the `claude mcp add` lines are checked against `claude mcp add --help` and their URL and arguments against live runs (G16a). Startup proof (G16b): three runs of the literal `java -jar distributor/target/distributor.jar`, "process running for" Y under 10 s and scripted launch-to-health-UP wall time under 10 s, all recorded; Y above 5 s flagged; `127.0.0.1:8090` LISTEN only; the real-jar STDIO run quotes stderr (G15c).

### T8: `http/distributor.http`

- module: root; dependsOn: [T1, T4]; implModel sonnet.
- fileScope: `http/distributor.http`.
- acceptanceCriteria:
  - House style of `http/catalogue.http`; first line "# JetBrains HTTP Client format: run the requests top to bottom against a locally running distributor."; requests: list, `q=austen`, `subject=FIC028&size=5`, P&P by EAN, 9780000000002 (404), subjects, restock P&P quantity 10 (capture `restockOrderId`), GET by id, a BACKORDER EAN, an OUT_OF_PRINT EAN (422), quantity 0 (400). REST only.
  - Every EAN exists in `titles.json` with the availability its comment claims (script evidence); ASCII, no em dashes; live replay of the file's own requests against one running distributor records each status (G17).

## QA feed-forward

From the QA lead's T1 r1 review (details in the QA ledger, "Feed-forward issued"):

- FF1 (T2): `TitleCatalog` tests pin that it keeps its own copy of the input lists and returns unmodifiable views.
- FF2 (T2, fallback T3): decide and pin the behaviour of `TitleCatalog.findByEan(null)` (today a `NullPointerException` from `Map.copyOf`); a missing `ean` must never surface as an unexplained NPE through REST or MCP.
- FF3 (every verifier): classify survivors against the C0 gate checks; one `Clock.systemUTC()` probe per Clock consumer.
- FF4 (T8 replay and qa-final): a new release's `releaseDate` equals today's Europe/Amsterdam date minus its `releasedDaysAgo` in a live run.

From the QA lead's T2 r1 review:

- FF5 (T3 verifier): if a tool-error text is built from `TitleNotFoundException.getMessage()`, a prefix probe on that message (`"No title with EAN "` -> `"No title for EAN "`) must fail at the G12b exact-text assertion in `CatalogMcpToolsTest`; if T3 builds its own string, the verifier says so and skips the probe.

From the QA lead's T3 r2 review (T4 to T8 and qa-final):

- FF6: the `ToolInputValidator` WARN "Tool (search_titles) input validation failed: ... required property 'query' not found" is accepted at exactly 1 in the distributor section (source: `McpServerIntegrationTest.searchTitlesWithoutQueryIsAToolError`); any other count, tool or property must be attributed to a named test or flagged.
- FF7: the three BeanPostProcessorChecker lines appear once per context that loads the MCP annotation scanner; expected count = 3 x the number of `Registered tools: 4` lines in the distributor section (T3 baseline 6 from 2 contexts); whether a slice loads the scanner is observed, not assumed.

From the QA lead's T4 r2 review:

- FF8 (T5 implementer and verifier): `ChaosApiTest` sends one `/api/...` request with `accept(MediaType.APPLICATION_PROBLEM_JSON)` and asserts status 503, content type exactly `application/json` and the `TEMPORARILY_UNAVAILABLE` envelope; the verifier's probe CT-rm (remove `.contentType(MediaType.APPLICATION_JSON)` from the advice's `envelope(...)` helper) must fail under `-Dtest=ChaosApiTest`. The T4 test files stay unedited.

From the QA lead's T5 r1 review:

- FF9 (qa-final): a live latency run with a health control (the production sleeper really delays `/api` calls and not `/actuator/health`), restock POST and GET under `failure-rate=1.0` (503 envelope), checked against the T5 hash baseline. The optional test-only hardening F-s05-8 stays a follow-up (orchestrator decision: the live qa-final check covers the clause).

## Amendments

(none)
