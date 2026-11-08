# Spec 02: Catalogue (books, ISBN, money, search)

> Module: `bookstore` · Depends on: 01 · Capability: `catalogue`

## Why
The catalogue is the heart of the bookstore and the area participants touch first: the Level 0 and Level 1 task
("add discounts") lands here. It must show every onion layer once, cleanly, so an agent that reads the code can
learn the pattern, and an agent that does not read it will visibly break it.

## What changes
- Domain: `Book` aggregate, value objects `BookId`, `Isbn`, `Money`, `Title`, `AuthorName`, enum `Genre`, enum `BookStatus`.
- Domain interaction: `CatalogueFlow`, port `BookDataService`, `BookDTO`, `BookDomainMapper`.
- Command adapter: add a book, change its price, discontinue it.
- Query adapter: search the catalogue, get one book.
- Data adapter: JPA entities and Flyway migration `V1__create_catalogue.sql`.

## Out of scope
- **Discounts of any kind.** Do not add a discount, percentage or promotion concept. Discounts are the Level 0 task.
- Stock levels (spec 03), authors as an aggregate (authors are value objects on a book), images, pagination beyond a result cap.

## Domain model
| Type | Kind | Rules |
|---|---|---|
| `BookId` | record(UUID) | Generated in the domain: `BookId.newId()` |
| `Isbn` | record(String value) | Accepts ISBN-13 with or without hyphens or spaces; normalises to 13 digits; validates the checksum; prefix 978 or 979; otherwise `InvalidIsbnException` |
| `Money` | record(BigDecimal amount, Currency currency) | Scale 2, `RoundingMode.HALF_EVEN`; currency EUR only for now; not negative; `of("12.99")`, `plus`, `times(int)`, `isGreaterThan`; `toString()` gives `12.99 EUR` |
| `Title` | record(String) | Trimmed, 1 to 200 characters |
| `AuthorName` | record(String) | Trimmed, 1 to 120 characters |
| `Genre` | enum | FICTION, SCIENCE_FICTION, FANTASY, MYSTERY, ROMANCE, HISTORY, SCIENCE, TECHNOLOGY, CHILDREN, POETRY; `Genre.parse(String)` accepts any case and `-` or `_` |
| `BookStatus` | enum | AVAILABLE, DISCONTINUED |
| `Book` | aggregate | Fields: id, isbn, title, authors (1 to 5, ordered), genre, price, publicationYear, status. Factory `Book.addToCatalogue(isbn, title, authors, genre, price, publicationYear)`; `Book.restore(...)`; behaviour `changePrice(Money)`, `discontinue()` |

Invariants: price greater than 0.00 and at most 500.00 EUR; publication year between 1450 and next year;
a discontinued book cannot change price (`BookDiscontinuedException`); discontinuing twice is a no-op.

## API
| Method and path | Body | Success | Errors |
|---|---|---|---|
| `POST /books/commands/add` | `AddBookPayload { isbn, title, authors[], genre, price, publicationYear }` | 202 + `CommandResult { id }` | 400 validation, 409 ISBN exists, 422 invalid ISBN |
| `POST /books/{id}/commands/change-price` | `ChangePricePayload { price }` | 202 | 404, 409 discontinued, 422 price out of range |
| `POST /books/{id}/commands/discontinue` | none | 202 | 404 |
| `GET /books?q=&title=&author=&genre=` | | 200 `BookView[]` | 422 unknown genre |
| `GET /books/{id}` | | 200 `BookDetailView` | 404 |

`BookView { id, isbn, title, authors (joined with ", "), genre, price ("12.99"), currency ("EUR"), status }`.
`BookDetailView` adds `authors` as an array and `publicationYear`.
Search: `q` is free text matching title OR any author name; `title` and `author` match only that field; all matches
are case-insensitive "contains"; `genre` is an exact match; the given criteria are combined with AND; sorted by title,
at most 100 results. The query adapter passes `genre` as a string; the flow parses it (unknown genre: 
`InvalidInputException`, code `unknown-genre`, `details` listing the valid genres). The same rule applies to the genre
in `AddBookPayload`. Discontinued books are included and marked by status.
Prices are serialised as strings to avoid floating-point surprises in clients.

## Persistence
`V1__create_catalogue.sql`: table `book` (`id uuid pk`, `isbn varchar(13) unique not null`, `title varchar(200)`,
`genre varchar(32)`, `price_amount numeric(10,2)`, `price_currency varchar(3)`, `publication_year int`,
`status varchar(16)`, `version bigint`), table `book_author` (`book_id uuid fk`, `position int`, `name varchar(120)`,
pk `(book_id, position)`). `BookJPA` uses `@Version` for optimistic locking; authors as `@ElementCollection` with `@OrderColumn`.

## Requirements

### Requirement: Add a book to the catalogue
The system SHALL add a book when all fields are valid and its ISBN is not yet in the catalogue, and return the new book id.

#### Scenario: Valid book is added
- **GIVEN** no book with ISBN 9780141439518 exists
- **WHEN** a client posts `AddBookPayload` with that ISBN, title "Pride and Prejudice", authors ["Jane Austen"], genre "fiction", price "9.99" and year 1813
- **THEN** the response is 202 with a `CommandResult` id, and `GET /books/{id}` returns the book with status AVAILABLE and price "9.99"

#### Scenario: Duplicate ISBN is rejected
- **GIVEN** a book with ISBN 9780141439518 exists
- **WHEN** a client adds another book with ISBN "978-0-14-143951-8"
- **THEN** the response is 409 with ProblemDetail title "ISBN already in catalogue" and `code` `isbn-already-exists`

#### Scenario: Invalid checksum is rejected
- **WHEN** a client adds a book with ISBN 9780141439519
- **THEN** the response is 422 with ProblemDetail `code` `invalid-isbn` and `details` `[{"field":"isbn", ...}]`

### Requirement: Money is exact
The system SHALL represent prices with `Money` (BigDecimal, scale 2, HALF_EVEN) in every layer that handles a price, and SHALL never use `double` or `float` for money.

#### Scenario: Rounding
- **WHEN** `Money.of("10.005")` is created
- **THEN** its amount is 10.00 (HALF_EVEN), and `Money.of("10.015")` is 10.02

#### Scenario: Architecture guards the type
- **GIVEN** an ArchUnit rule "no field or method parameter of type double or float in domain, domaininteraction, command, query"
- **WHEN** the architecture tests run
- **THEN** the rule passes on the codebase and fails on a fixture with a `double price` field

### Requirement: Change price
The system SHALL change the price of an available book and SHALL reject price changes for discontinued books.

#### Scenario: Price change on available book
- **GIVEN** an available book priced 9.99
- **WHEN** a client posts change-price with "12.50"
- **THEN** the response is 202 and the book's price is "12.50"

#### Scenario: Price change on discontinued book
- **GIVEN** a discontinued book
- **WHEN** a client posts change-price
- **THEN** the response is 409 and the price is unchanged

### Requirement: Search the catalogue
The system SHALL return books matching all given criteria, sorted by title, capped at 100.

#### Scenario: Search by author fragment and genre
- **GIVEN** books by "Jane Austen" (fiction) and "Mary Shelley" (science fiction)
- **WHEN** a client calls `GET /books?author=austen&genre=FICTION`
- **THEN** only the Austen books are returned, sorted by title

#### Scenario: Unknown genre
- **WHEN** a client calls `GET /books?genre=cookbooks`
- **THEN** the response is 422 with ProblemDetail `code` `unknown-genre` and `details` listing the valid genres

#### Scenario: Free-text search
- **WHEN** a client calls `GET /books?q=austen`
- **THEN** books whose title or author contains "austen" are returned

## Tests (required)
- Domain: `IsbnTest` (valid, hyphenated, bad checksum, wrong prefix), `MoneyTest` (rounding, arithmetic, negative),
  `BookTest` (factory validation, change price, discontinue rules), `GenreTest`.
- Flow: `CatalogueFlowTest` with mocked `BookDataService` (duplicate ISBN check, not found).
- Adapters: `BookCommandsTest`, `BookQueriesTest` (`@WebMvcTest`, mocked flow, ProblemDetail shapes),
  `BookDataServiceImplTest` (`@DataJpaTest`: save, find by ISBN, search, optimistic locking).
- End to end: `CatalogueIntegrationTest` covering every scenario above over HTTP.
- ArchUnit: the "no double/float for money" rule plus its fixture.

## Tasks
1. Value objects and `Book` aggregate with tests (test first).
2. `BookDTO`, `BookDomainMapper`, `BookDataService` port with nested `BookNotFoundException`, `IsbnAlreadyExistsException`.
3. `CatalogueFlow` use cases: `addBook`, `changePrice`, `discontinue`, `search`, `findById`.
4. Migration `V1`, `BookJPA`, `BookAuthorJPA` or element collection, `BookJPAMapper`, `BookRepository`, `BookDataServiceImpl`.
5. `BookCommands`, payload records with validation, `CommandResult`, `CommandExceptionHandler` (ProblemDetail).
6. `BookQueries`, `BookView`, `BookDetailView`, `QueryExceptionHandler`.
7. Money ArchUnit rule and fixture.
8. Integration test; `http/catalogue.http` with one request per endpoint.

## Definition of done
- All scenarios covered by tests; `./mvnw verify` green; ArchUnit green.
- No discount concept anywhere (`grep -ri discount bookstore/src` returns nothing).
