# Spec 06: Distributor anti-corruption layer (import titles, request restock)

> Module: `bookstore` · Depends on: 03, 05 · Capability: `distributor-integration`

## Why
The bookstore must talk to the distributor without letting the distributor's model (EANs, cents, subject codes,
contributor roles, error envelopes) leak into the domain. The `acl` layer translates at the border. For agents this
is a classic trap: the shortest path is to call the REST API from a controller with the distributor's JSON.

## What changes
- Port `DistributorAppService` in `domaininteraction` with DTOs `DistributorTitleDTO`, `RestockConfirmationDTO`
  and nested exceptions `TitleNotFoundAtDistributorException`, `TitleNotOrderableException`,
  `DistributorUnavailableException`.
- `DistributorAppServiceImpl` in `acl` using `RestClient`.
- Use case `CatalogueFlow.importFromDistributor(String isbn, BigDecimal priceOverride)` (override nullable; the flow
  creates `Isbn` and `Money`, because the command adapter may not touch domain types) and command
  `POST /books/commands/import-from-distributor`.
- Port method `requestRestock` (used by spec 07; implemented and contract-tested here).

## Out of scope
- The restock process itself (spec 07). Searching the distributor from the bookstore API (the agent uses MCP for that).
- Contract tests for titles with TRANSLATOR or ILLUSTRATOR contributors: deliberately left for the Level 2 MCP exercise.
  Do map roles correctly (authors only), just do not add a dedicated test for multi-role titles.

## Port (domaininteraction)
```java
public interface DistributorAppService {
    Optional<DistributorTitleDTO> findTitle(String isbn);
    RestockConfirmationDTO requestRestock(String isbn, int quantity, String retailerReference);
    // nested exceptions: TitleNotFoundAtDistributorException extends NotFoundException,
    // TitleNotOrderableException extends InvalidInputException (422),
    // DistributorUnavailableException extends UnavailableException
}
public record DistributorTitleDTO(String isbn, String title, List<String> authors, String genre,
                                  BigDecimal recommendedPrice, String currency, int publicationYear,
                                  boolean orderable) {}
public record RestockConfirmationDTO(String restockOrderId, int quantity, LocalDate expectedDeliveryDate,
                                     boolean backordered) {}
```
The port uses JDK types only, because `acl` implements it and may not access `domain` (architecture standard,
section 4). `genre` carries the name of one of our `Genre` constants; the flow parses it with `Genre.parse`, and a
test asserts that every genre the ACL can produce parses.

## Translation rules (acl)
| Distributor | Bookstore |
|---|---|
| `ean` | isbn (13-digit string) |
| `titleName` | title |
| `contributors` with role AUTHOR, in order | authors (other roles are dropped) |
| `listPriceCents` + `currency` | `BigDecimal` (cents / 100, scale 2) + currency; non-EUR currency: `TitleNotOrderableException` |
| first `subjectCodes` entry, longest-prefix match | genre name: `FIC028` SCIENCE_FICTION, `FIC009` FANTASY, `FIC022` MYSTERY, `FIC027` ROMANCE, other `FIC` FICTION, `HIS` HISTORY, `SCI` SCIENCE, `COM` TECHNOLOGY, `JUV` CHILDREN, `POE` POETRY; anything else FICTION and a WARN log |
| `availability` OUT_OF_PRINT | `orderable = false` |
| `releaseDate` year | publicationYear for the import |
| 404 `TITLE_NOT_FOUND` | `Optional.empty()` from `findTitle`, `TitleNotFoundAtDistributorException` from `requestRestock` |
| 422 `NOT_ORDERABLE` | `TitleNotOrderableException` |
| 5xx, timeout, connection refused | `DistributorUnavailableException` |

Payload records are private nested records inside `DistributorAppServiceImpl`. The public constructor builds the
`RestClient` from the injected `RestClient.Builder` and `DistributorProperties` (base URL `distributor.base-url`,
default `http://localhost:8090`; connect timeout 2 s and read timeout 5 s via `distributor.connect-timeout`,
`distributor.read-timeout`). A package-private constructor takes a ready `RestClient`: unit tests build one from a
`RestClient.Builder` bound to `MockRestServiceServer`, so the timeout request factory never replaces the mock.
No separate configuration class is needed (and the `acl` naming rule allows only `AppServiceImpl` and `Properties`).

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /books/commands/import-from-distributor` | `ImportBookPayload { isbn, price? }` | 202 + `CommandResult { id }` | 404 unknown at distributor, 409 already in catalogue, 422 not orderable, 503 distributor unavailable |

## Requirements

### Requirement: Import a title from the distributor
The system SHALL add a distributor title to the catalogue using translated data, with the recommended price unless a price override is given, and open an empty stock item.

#### Scenario: Import with recommended price
- **GIVEN** the distributor has EAN 9780141439518 with listPriceCents 999 and subject FIC004000
- **WHEN** a client posts import-from-distributor with that ISBN and no price
- **THEN** the response is 202, the book exists with price "9.99", genre FICTION, authors ["Jane Austen"], and stock 0

#### Scenario: Price override
- **WHEN** the same import is posted with price "12.00"
- **THEN** the book's price is "12.00"

#### Scenario: Already in catalogue
- **GIVEN** the ISBN is already in the catalogue
- **WHEN** the import is posted
- **THEN** the response is 409 and the distributor is not called

#### Scenario: Distributor down
- **GIVEN** the distributor returns 503 or times out
- **WHEN** the import is posted
- **THEN** the response is 503 with ProblemDetail title "Distributor unavailable", `code` `distributor-unavailable`, and nothing is saved

### Requirement: Distributor model never leaks
No type from the distributor's wire format SHALL appear outside the `acl` package.

#### Scenario: Architecture rule
- **GIVEN** an ArchUnit rule "classes outside acl must not depend on nested classes of *AppServiceImpl" and the existing RestClient rule
- **WHEN** the architecture tests run
- **THEN** both rules pass; one fixture (a controller that uses `RestClient`) makes the RestClient rule fail, and a
  second fixture (a fixture `*AppServiceImpl` with a non-private nested record, referenced from a fixture controller)
  makes the nested-class rule fail

### Requirement: Restock requests are translated
`requestRestock` SHALL post a restock order and translate the distributor's response and errors.

#### Scenario: Confirmed restock
- **GIVEN** the distributor answers 201 with status CONFIRMED and expectedDeliveryDate 2026-11-17
- **WHEN** `requestRestock` is called for 10 units
- **THEN** a `RestockConfirmationDTO` with that date and `backordered = false` is returned

#### Scenario: Backordered restock
- **WHEN** the distributor answers status BACKORDERED
- **THEN** `backordered = true`

## Tests (required)
- `DistributorAppServiceImplTest` with `MockRestServiceServer` bound to the `RestClient.Builder`: every row of the
  translation table that the scenarios name, timeouts (simulate with `withException(new SocketTimeoutException())`),
  error envelopes. Request and response bodies come from JSON files under
  `src/test/resources/contracts/distributor/` that were captured from the real distributor API (spec 05) with
  `http/distributor.http`, so the contract matches reality. Capture only titles whose contributors are all AUTHOR
  (multi-role titles are the Level 2 exercise).
- `CatalogueFlowTest` additions (import with mocked port), `BookCommandsTest` additions.
- `DistributorImportIntegrationTest` (`@SpringBootTest` extending `IntegrationTestBase`, with
  `@MockitoBean DistributorAppService`: the HTTP contract is already covered by the unit contract tests).

## Tasks
1. Port, DTOs, exceptions in `domaininteraction`.
2. Capture contract JSON files from the running distributor.
3. `DistributorAppServiceImpl` with translation and error mapping; `@ConfigurationProperties` record
   `DistributorProperties` in `acl` (the `Properties` suffix is already allowed by the naming rule).
4. `importFromDistributor` use case, payload, endpoint, exception handler mappings (404, 409, 422, 503).
5. ArchUnit rule and fixture; integration test; `http/catalogue.http` import example.

## Definition of done
- Scenarios covered; `./mvnw verify` green; no distributor wire types outside `acl`.
