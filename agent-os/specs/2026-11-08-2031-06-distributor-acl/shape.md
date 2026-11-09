# Shape: Spec 06 Distributor anti-corruption layer

Source of truth: `docs/spec-pack/specs/06-distributor-acl.md`. This file records the shaping notes and the engineering decisions the pack spec leaves open. `plan.md` holds the task cards.

## Scope

- Bookstore module only (plus the root files `http/distributor.http` and `http/catalogue.http`): the port `DistributorAppService` with its DTOs and nested exceptions in `domaininteraction`; `DistributorAppServiceImpl` and `DistributorProperties` in `acl` using Spring `RestClient`; the use case `CatalogueFlow.importFromDistributor(String isbn, BigDecimal priceOverride)`; the command `POST /books/commands/import-from-distributor` with `ImportBookPayload`; one new ArchUnit rule with fixtures; contract JSON files captured from the running distributor (spec 05); `DistributorImportIntegrationTest`.
- `requestRestock` is implemented and contract-tested here and used by spec 07.
- Out of scope (pack spec): the restock process (spec 07); searching the distributor from the bookstore API; contract tests for titles with TRANSLATOR or ILLUSTRATOR contributors (Level 2 MCP exercise, D6). Roles are still mapped correctly (authors only).
- The distributor module is not changed.

## Decisions

Decided by the orchestrator from the pack spec, the architecture standard, tech-stack and the research; cheap and reversible engineering calls.

- D-a Every distributor failure the translation table does not name becomes `DistributorUnavailableException`: 400 `INVALID_REQUEST`, any other 4xx (405, 415, 429), a 404 without the `TITLE_NOT_FOUND` code (for example Spring's default unknown-path body), a 422 whose code is not `NOT_ORDERABLE`, an unparseable error body, a malformed or empty 2xx body, a 2xx body missing a required key, an unparseable date, a restock status other than CONFIRMED or BACKORDERED, a wrong content type, any `RestClientException`. No `RestClientException` or Jackson exception leaves the adapter. Reason: the standard (section 4) says every external failure is translated into a port exception, and these are failures of the integration, not of the caller's input.
- D-b Codes and titles: `TitleNotFoundAtDistributorException` is `title-not-found-at-distributor` / "Title not found at distributor"; `TitleNotOrderableException` is `title-not-orderable` / "Title not orderable"; `DistributorUnavailableException` is `distributor-unavailable` / "Distributor unavailable" (fixed by the spec).
- D-c `DistributorProperties` is registered with `@EnableConfigurationProperties(DistributorProperties.class)` on `DistributorAppServiceImpl`. Adapter configuration stays in the adapter; `BookstoreApplication` is unchanged; no configuration class (the acl naming rule allows only `AppServiceImpl` and `Properties`).
- D-d `spring-boot-starter-restclient` is added to `bookstore/pom.xml` without a version (managed by Boot 4.1.2). No restclient test starter and no `@RestClientTest`: `MockRestServiceServer` comes from spring-test, and the slice would bypass the spec's constructor pattern.
- D-e `@MockitoBean DistributorAppService` lives in `IntegrationTestBase`, so every full-context test shares one cached context and none can reach a distributor. `DistributorImportIntegrationTest` stubs the inherited mock (the spec's "with `@MockitoBean DistributorAppService`" holds through inheritance). An `ApplicationContextRunner` wiring test proves the real adapter bean still starts and binds its properties.
- D-f Import order: build `Isbn` and (when given) the override `Money` first, so an invalid request gets the existing 422 code without a lookup or remote call; then the catalogue check (present: 409, distributor not called); then `findTitle` (empty: 404 `TitleNotFoundAtDistributorException`); `orderable == false`: 422 `TitleNotOrderableException`; then the same private add path as `addBook` (save book, open the empty stock item), so a unique-constraint race still gives 409. `ImportBookPayload.price` is a nullable String with the same pattern as `AddBookPayload.price` (sign admitted), so a negative override is the flow's 422 `price-out-of-range`, never a 400.
- D-g Contract files are captured live from the running distributor and stored verbatim. Variants the seed cannot produce (non-EUR currency, unmapped subject prefix, round price, authors in reversed order) are `derived-` files, each one captured file with exactly one change, documented in `contracts/distributor/README.md`. Only titles whose contributors are all AUTHOR are captured. Tests assert the restock date found in the captured file (the spec's 2026-11-17 is an example).
- D-h One test in `DistributorAppServiceImplWiringTest` runs the public constructor against a JDK `HttpServer` on an ephemeral loopback port (read timeout 250 ms, handler sleeps 2 s) to prove the timeout request factory is applied. It is not a contract test, adds no dependency and never touches port 8090 or a real distributor; without it, dropping the request factory survives every test.
- D-i The ArchUnit rule covers classes nested at any depth in an `*AppServiceImpl`; a depth-2 fixture record proves it (QA G9 option A).
- D-j `ArchitectureSelfTest`'s closed registry count goes from 21 to 22. That is the registry bump the closed design requires for a new rule, not a weakened assertion.

## Pack task mapping

Pack 1 = T1; pack 2 = T2; pack 3 = T3; pack 4 = T4 (flow) + T5 (command adapter); pack 5 = T6 (ArchUnit) + T7 (integration test, `http/catalogue.http`).
