# Spec 02 Catalogue: Shaping Notes

Source of truth: `docs/spec-pack/specs/02-catalogue.md`. These notes restate it, record the engineering decisions the spec leaves open, and map every scenario to a test. Shaped autonomously by the orchestrator agent on 2026-11-07 with three research agents, one tech-lead strategist and the QA lead.

## Scope

In scope (the pack spec's "What changes"): the `Book` aggregate with `BookId`, `Isbn`, `Money`, `Title`, `AuthorName`, `Genre`, `BookStatus`; `CatalogueFlow`, the `BookDataService` port, `BookDTO`, `BookDomainMapper` and the four use-case exceptions; the command adapter (add, change price, discontinue), the query adapter (search, get one), the data adapter with `V1__create_catalogue.sql`; the money ArchUnit rule with fixture and self-test entry; `CatalogueIntegrationTest`; `http/catalogue.http`.

Out of scope: discounts of any kind (Lab 1), stock levels (spec 03), authors as an aggregate, images, pagination beyond the 100-result cap, domain events (none in spec 02), mapping a concurrent update to 409, ProblemDetail bodies for malformed JSON or bad UUIDs.

## Decisions

Decisions taken by the main session before this run (progress.md, 2026-11-07):

- `http/catalogue.http` lives in a repo-root `http/` folder (spec 09 lists `http/*.http` next to `README.md`).
- `Book.addToCatalogue(...)` and `Book.restore(...)` take a `java.time.Clock` in addition to the spec's parameters; `CatalogueFlow` passes its injected `Clock`; spec 02 creates the `Clock` bean (`Clock.systemUTC()`) in `ClockConfig` in the bookstore root package; spec 04 reuses it. Tests use a fixed clock. The domain depends on `java..` only.
- The money ArchUnit rule is one new `public static final ArchRule` field with `allowEmptyShould(true)`, a violator fixture under `nl.openvalue.archfixtures` with a `double price` field, an entry in the self-test's expected-violator map and the rule count 20 to 21.
- Spec 01 follow-ups go into this spec's QA strategy where a task touches their files; no task solely for them.
- Use-case exception shapes per the `ddd-building-blocks` table; the spec's codes are exact: `isbn-already-exists`, `invalid-isbn`, `unknown-genre`, `validation-failed` (400).

Decisions taken by the orchestrator during shaping (logged in the run ledger):

- Nine tasks in the order T1, T7, T2, T4, T3, T5, T6, T8, T9: inside out, test first per layer, reordered so `./mvnw clean verify` is green after every merge (the data adapter lands before the flow; the money rule right after the domain). `http/catalogue.http` is its own task because it belongs to the `root` module.
- Spec 01 follow-ups F10, F11, F12 (description half), F13 and F15 are folded into T7 as additive tests; F14, F9 and the origin half of F12 were resolved in the spec 01 review; F1 to F8 are deferred.
- Accepted defaults the spec does not cover: a concurrent update surfaces as HTTP 500 (`ObjectOptimisticLockingFailureException` is not mapped; no scenario, no ETag); malformed JSON, wrong JSON types and bad UUIDs give Boot's empty-body 400 (the standard requires ProblemDetail for the four use-case exceptions and bean validation only).

Engineering decisions (tech-lead strategy, adopted; the plan's task cards carry them as criteria):

| # | Decision |
|---|---|
| D1 | One domain exception per value object and one per aggregate invariant, nine in all, each `extends RuntimeException` directly: `InvalidIsbnException`, `InvalidMoneyException`, `InvalidTitleException`, `InvalidAuthorNameException`, `UnknownGenreException`, `PriceOutOfRangeException`, `PublicationYearOutOfRangeException`, `AuthorCountOutOfRangeException`, `BookDiscontinuedException`. |
| D2 | Exceptions carry facts (ids, values, bounds) in plain English messages, never HTTP concerns; the message becomes the ProblemDetail `detail`. |
| D3 | `Isbn`: remove `-` and spaces only; then 13 ASCII digits, prefix 978 or 979, weighted 1,3 checksum; `value()` is the normalised form so equality works across spellings. |
| D4 | `Money(BigDecimal, Currency)`: scale 2 HALF_EVEN in the compact constructor, EUR only, negative rejected after rounding; `of(String)`, `of(BigDecimal)`, `of(BigDecimal, String)`, `plus`, `times(int)`, `isGreaterThan`, `toString()` "12.99 EUR". |
| D5 | `Title` and `AuthorName`: `strip()`, then 1 to 200 (resp. 120) characters. |
| D6 | `Genre.parse`: `strip()`, `-` to `_`, `toUpperCase(Locale.ROOT)`, match by name; unknown, null or blank gives `UnknownGenreException`. |
| D7 | `BookId(UUID)` with `newId()`; `BookStatus { AVAILABLE, DISCONTINUED }`. |
| D8 | `Book`: final, private constructor, no setters, immutable author list; `addToCatalogue(Isbn, Title, List<AuthorName>, Genre, Money, int, Clock)`, `restore(BookId, Isbn, Title, List<AuthorName>, Genre, Money, int, BookStatus, Clock)`; both validate authors 1 to 5, price greater than 0.00 and at most 500.00, year 1450 to the clock's year plus one; the clock is not stored. |
| D9 | `changePrice`: discontinued check first (`BookDiscontinuedException`), then the range check; `discontinue()` idempotent. |
| D10 | No domain events in spec 02 (nothing reacts to a catalogue change; no speculative methods). |
| D11 | Four public, non-final use-case exceptions with `(code, title, detail)` and `(code, title, detail, details)` constructors; `details` never null, unmodifiable, empty by default; no common base class. |
| D12 | Port: `save(BookDTO)` (insert or update by id, throws `IsbnAlreadyExistsException`), `findById(UUID)` (throws `BookNotFoundException`), `Optional<BookDTO> findByIsbn(String)`, `search(q, title, author, genre, int limit)`. |
| D13 | Nested port exceptions: `BookNotFoundException` (`book-not-found`, "Book not found"), `IsbnAlreadyExistsException` (`isbn-already-exists`, "ISBN already in catalogue"). |
| D14 | `BookDTO(id, isbn, title, authors, genre, priceAmount, priceCurrency, publicationYear, status)` with `BookDTO(Book)`; no version (persistence concern). |
| D15 | `BookDomainMapper`: static `toDomain(BookDTO, Clock)` calling `restore` (domaininteraction allows no `@Component`). |
| D16 | `CatalogueFlow(BookDataService, Clock)`: `UUID addBook(String isbn, String title, List<String> authors, String genre, BigDecimal price, int publicationYear)`, `changePrice(UUID, BigDecimal)`, `discontinue(UUID)`, read-only `search(q, title, author, genre)` and `findById(UUID)`. |
| D17 | `addBook` validates and builds the aggregate first (422 before 409), then checks `findByIsbn` with the normalised ISBN, saves, returns the id. |
| D18 | `changePrice` and `discontinue` load, map, call the behaviour, save; no business `if` in the flow. |
| D19 | `search` strips criteria, blank means absent, parses the genre to its name, caps at 100 (`SEARCH_LIMIT`), returns DTOs; `findById` returns the DTO. |
| D20 | Translation of domain exceptions into use-case exceptions in private static helpers of the flow per the ProblemDetail catalogue in `plan.md`; `details` `[{field, message}]` for a bad field, `[{genre: NAME}]` per constant for an unknown genre. |
| D21 | `ClockConfig` (`@Configuration(proxyBeanMethods = false)`, `Clock.systemUTC()`), `ClockConfigTest` on the shared context. |
| D22 | Payloads: `AddBookPayload` with `@NotBlank`, `@NotEmpty List<@NotBlank String>`, `@Pattern("\\d+(\\.\\d+)?")` on `String price`, `@NotNull Integer publicationYear`; `ChangePricePayload` with the same price rule; no business ranges in annotations. |
| D23 | `BookCommands` with `@ResponseStatus(ACCEPTED)`, `CommandResult(UUID id)` on add, `new BigDecimal(price)` after validation. |
| D24 | `CommandExceptionHandler` `@RestControllerAdvice(basePackageClasses = BookCommands.class)`, plain `@ExceptionHandler` methods returning `ProblemDetail` with `code` and `details` properties; 400 `validation-failed` from `MethodArgumentNotValidException`; `spring.mvc.problemdetails.enabled` stays unset. |
| D25 | `BookQueries` with optional raw `@RequestParam` strings passed to the flow. |
| D26 | `BookView` and `BookDetailView` per the spec's field lists, `static from(BookDTO)`, `price` as `toPlainString()`, authors joined with ", " in `BookView`. |
| D27 | `QueryExceptionHandler` scoped to `BookQueries`, the same four mappings, no body-validation handler. |
| D28 | `V1__create_catalogue.sql` with every column `not null`, named constraints `pk_book`, `uq_book_isbn`, `pk_book_author`, `fk_book_author_book` (no cascade). |
| D29 | `BookJPA` with `@Id UUID`, `@ElementCollection` authors with `@OrderColumn(name = "position")`, plain `String` genre and status, `BigDecimal(10,2)` amount, `String` currency, `Integer` year, `@Version Long`. |
| D30 | `BookJPAMapper` static `toNewEntity`, `copyInto` (authors replaced only when different), `toDTO`. |
| D31 | `BookRepository`: `findByIsbn`, `@Query` search with `exists` subqueries, patterns as parameters with `escape '\\'`, `order by b.title, b.isbn`, trailing `Limit`. |
| D32 | `BookDataServiceImpl` (`@Component`): upsert on the managed entity with `saveAndFlush`, `DataIntegrityViolationException` on insert translated to `IsbnAlreadyExistsException`, Java-built lower-cased escaped patterns, DTO mapping inside the adapter. |
| D33 | Rule field `NO_DOUBLE_OR_FLOAT_FOR_MONEY`: `CompositeArchRule` of `noFields()...haveRawType(double/float)` and `noCodeUnits()...haveRawParameterTypes(include double or float)` over the four packages, `allowEmptyShould(true)`; covers constructors too (a strengthening of "method parameter"). |
| D34 | Self-test: map entry with four violators, origin assertion, control not reported, description assertion, count 21. |
| D35 | `http/catalogue.http`: JetBrains HTTP Client format, five requests (add with a response handler storing `bookId`, get, search, change price, discontinue). |

Interpretations recorded (contradiction check C1 to C7 found nothing requiring a stop):

- C1: the standard's section 1 and the onion skill still say "spec 04: the Clock bean"; the main session decided spec 02 creates it and spec 04 reuses it. Amending the parenthetical is a doc follow-up for the main session.
- C2: "represent prices with `Money` in every layer that handles a price" is read as the representation: `Money` in the domain, `BigDecimal` amount plus currency in DTOs and JPA (adapters may not access `domain`), strings on the wire, never `double` or `float`.
- C3 to C7: static mappers satisfy the standard's framework-type limits for `domaininteraction`; "Architecture guards the type" is an ArchUnit test, not an HTTP scenario; no events in spec 02; the element collection (no `BookAuthorJPA`) per the Persistence section; `not null` on every DDL column is an addition the domain requires.

## Context

- **Visuals:** None.
- **References:** No earlier slice exists; spec 01's `ArchitectureTest`, `ArchitectureSelfTest`, fixtures, `PostgresTestConfig`, `IntegrationTestBase`, `DatabaseCleaner` are the only code mirrored (see `references.md`).
- **Product alignment:** mission (a real domain with invariants, test-enforced architecture); roadmap item 02 (Lab 1 and 2 task area, discounts land here later); tech stack (Boot 4.1.2, Java 25, Jackson 3, Jakarta Validation, Flyway, Testcontainers 2.0.5, PostgreSQL 18.6; nothing new pinned).

## Standards Applied

- `architecture` (pointer to `docs/spec-pack/product/architecture-standard.md`): sections 1 to 5 in full; see `standards.md`.

## Scenario-to-test map (binding behaviour names; final method names are recorded in the task outbox artifacts)

| Spec scenario | Tests (owning task) |
|---|---|
| Valid book is added | `CatalogueIntegrationTest.validBookIsAddedAndCanBeRead` (T8); `CatalogueFlowTest.addsAValidBookAsAvailableAndReturnsItsId` (T3); `BookCommandsTest.addReturns202WithTheNewBookId` (T5); `BookQueriesTest.findByIdReturnsTheDetailView` (T6); `BookTest.addToCatalogueCreatesAnAvailableBookWithANewId` (T1); `GenreTest.parsesAnyCase` (T1) |
| Duplicate ISBN is rejected | `CatalogueIntegrationTest.duplicateIsbnIsRejected` (T8); `CatalogueFlowTest.rejectsADuplicateIsbn`, `checksDuplicatesWithTheNormalisedIsbn` (T3); `BookCommandsTest.addMapsAConflictTo409` (T5); `BookDataServiceImplTest.rejectsASecondBookWithTheSameIsbn`, `findsABookByIsbn` (T4); `IsbnTest.normalisesHyphensAndSpaces` (T1) |
| Invalid checksum is rejected | `CatalogueIntegrationTest.invalidChecksumIsRejected` (T8); `IsbnTest.rejectsABadChecksum` (T1); `CatalogueFlowTest.rejectsAnInvalidIsbnWithFieldDetails` (T3); `BookCommandsTest.addMapsInvalidInputTo422` (T5) |
| Rounding | `MoneyTest.roundsHalfEvenToTwoDecimals` (T1); `CatalogueIntegrationTest.pricesAreRoundedHalfEven` (T8) |
| Architecture guards the type | `ArchitectureTest.NO_DOUBLE_OR_FLOAT_FOR_MONEY` passes on production code (T7); `ArchitectureSelfTest.noDoubleOrFloatForMoney` names `archfixtures.domain.PricedBook` and the other three violators (T7) |
| Price change on available book | `CatalogueIntegrationTest.priceChangeOnAnAvailableBook` (T8); `BookTest.changePriceUpdatesTheAvailableBooksPrice` (T1); `CatalogueFlowTest.changesThePriceOfAnAvailableBook` (T3); `BookCommandsTest.changePriceReturns202WithoutBody` (T5) |
| Price change on discontinued book | `CatalogueIntegrationTest.priceChangeOnADiscontinuedBookIsRejectedAndThePriceIsUnchanged` (T8); `BookTest.changePriceOnADiscontinuedBookIsRejected` (T1); `CatalogueFlowTest.rejectsAPriceChangeOnADiscontinuedBook` (T3); `BookCommandsTest.changePriceMapsAConflictTo409` (T5) |
| Search by author fragment and genre | `CatalogueIntegrationTest.searchByAuthorFragmentAndGenre` (T8); `BookDataServiceImplTest.searchCombinesCriteriaWithAnd`, `searchWithoutCriteriaReturnsEveryBookSortedByTitle`, `searchSortsBooksWithTheSameTitleByIsbn` (T4); `CatalogueFlowTest.searchesWithNormalisedCriteriaTheCanonicalGenreAndALimitOf100` (T3); `BookQueriesTest.searchPassesTheQueryParametersToTheFlow` (T6) |
| Unknown genre | `CatalogueIntegrationTest.unknownGenreIsRejected` (T8); `GenreTest.rejectsAnUnknownGenre` (T1); `CatalogueFlowTest.rejectsAnUnknownGenreInSearchListingTheValidGenres` (T3); `BookQueriesTest.searchMapsUnknownGenreTo422` (T6) |
| Free-text search | `CatalogueIntegrationTest.freeTextSearchMatchesTitleOrAuthor` (T8); `BookDataServiceImplTest.searchMatchesTitleOrAuthorForFreeText` (T4) |

| Rule beyond the scenarios | Tests |
|---|---|
| ISBN: 979 prefix, wrong prefix, wrong length, non-digits, null, equality across spellings | `IsbnTest` (T1) |
| Money: arithmetic, comparison, toString, negative, non-EUR, unparsable, scale-insensitive equality | `MoneyTest` (T1) |
| Title 1 to 200 trimmed; AuthorName 1 to 120 trimmed | `TitleTest`, `AuthorNameTest` (T1) |
| Genre: hyphen or underscore, whitespace, null and blank, ten constants in order | `GenreTest` (T1) |
| Price greater than 0.00 and at most 500.00 | `BookTest` boundaries (T1); `CatalogueFlowTest.rejectsAPriceOutOfRangeOnAdd`, `rejectsAnOutOfRangePriceChange` (T3); `BookCommandsTest.changePriceMapsInvalidInputTo422` (T5) |
| Publication year 1450 to next year | `BookTest` boundaries (T1); `CatalogueFlowTest.rejectsAPublicationYearOutOfRange` (T3) |
| Authors 1 to 5, ordered | `BookTest` (T1); `CatalogueFlowTest.rejectsAnInvalidNumberOfAuthors`, `rejectsAnInvalidAuthorName`, `rejectsAnInvalidTitle` (T3); `BookDataServiceImplTest.keepsAuthorsInTheirOrder` (T4) |
| Discontinued check precedes the range check | `BookTest.discontinuedCheckPrecedesThePriceRange` (T1) |
| Discontinuing twice is a no-op | `BookTest.discontinuingTwiceIsANoOp` (T1); `CatalogueFlowTest.discontinuingADiscontinuedBookSucceeds` (T3); `CatalogueIntegrationTest.discontinuingTwiceIsANoOp` (T8) |
| restore keeps id and status, still validates | `BookTest.restoreKeepsIdAndStatus`, `restoreStillValidatesInvariantsForTheAuthorCount`, `...ForThePrice`, `...ForThePublicationYear` (T1); `BookDomainMapperTest.mapsAnAvailableDtoToTheDomainAndBackToAnEqualDto`, `mapsADiscontinuedDtoToTheDomainAndBackToAnEqualDto`, `stillAppliesTheBookInvariantsWhenRestoring` (T2) |
| Port exceptions carry the spec codes | `UseCaseExceptionsTest` (T2) |
| 404 on get, change-price, discontinue | `BookQueriesTest.findByIdMapsNotFoundTo404` (T6); `BookCommandsTest.changePriceMapsNotFoundTo404`, `discontinueMapsNotFoundTo404` (T5); `CatalogueFlowTest` pass-through (T3); `BookDataServiceImplTest.findByIdThrowsBookNotFoundForAnUnknownId` (T4); `CatalogueIntegrationTest.unknownBookIsNotFound` (T8) |
| 400 validation-failed | `BookCommandsTest.addRejectsMissingFieldsWith400`, `addRejectsABlankAuthorNameWith400`, `addRejectsANonDecimalPriceWith400`, `changePriceRejectsAMissingPriceWith400` (T5); `CatalogueIntegrationTest.incompleteBookIsRejectedWith400` (T8) |
| Genre in AddBookPayload follows the same rule | `CatalogueFlowTest.rejectsAnUnknownGenreOnAddListingTheValidGenres` (T3); `CatalogueIntegrationTest.unknownGenreOnAddIsRejected` (T8) |
| Blank criteria are absent; `?genre=` is not 422 | `CatalogueFlowTest.treatsBlankCriteriaAsAbsent` (T3) |
| Search: title only, author only, case-insensitive contains, genre exact, each book once, cap 100, `%` and `_` literal, no criteria | `BookDataServiceImplTest` (T4) |
| Save round trip, update and version, optimistic locking | `BookDataServiceImplTest.savesANewBookAndFindsItById`, `updatesAnExistingBookAndIncrementsItsVersion`, `rejectsAStaleUpdate` (T4) |
| Discontinued books are included and marked | `CatalogueIntegrationTest.discontinuedBooksAreIncludedInSearchWithTheirStatus` (T8) |
| Prices are strings with two decimals; view shapes | `BookQueriesTest.searchReturnsBookViewsWithJoinedAuthorsAndStringPrice`, `findByIdReturnsTheDetailView` (T6) |
| ProblemDetail shape for every use-case exception including 503 | `BookCommandsTest.mapsUnavailableTo503`, `BookQueriesTest.mapsUnavailableTo503` plus the 404, 409 and 422 cases (T5, T6) |
| Clock bean is UTC and wired | `ClockConfigTest.providesAUtcClock` (T3) |

## Checksum-verified test ISBNs

| ISBN | Use |
|---|---|
| 9780141439518 | Pride and Prejudice, Jane Austen, fiction (spec) |
| 978-0-14-143951-8 | the same ISBN hyphenated (spec duplicate scenario) |
| 9780141439519 | bad checksum (spec) |
| 9780141439587 | Emma, Jane Austen, fiction |
| 9780141439662 | Sense and Sensibility, Jane Austen, fiction |
| 9780141439471 | Frankenstein, Mary Shelley, science fiction (sent as "science-fiction") |
| 9780140296907 | Jane Austen: A Life, Claire Tomalin, history (title match only) |
| 9791090636071 | valid 979 prefix (`IsbnTest`) |
| 9771234567898 | valid checksum, wrong prefix 977 (`IsbnTest`) |

## Spec 01 follow-ups

Folded into T7: F10, F11, F12 (description half), F13, F15. Already resolved before this spec: F9, F14, the origin half of F12. Deferred (no spec 02 task edits their files): F1 (guard test for the cleaning hook in `IntegrationTestBase`), F2 (unlinked table in `DatabaseCleanerIntegrationTest`), F3 (effective `hibernate.hbm2ddl.auto` assertion), F4 (container survives a context close; covered for now by the one-container gate and the `@DirtiesContext` ban), F5, F6, F7 (distributor and health tests), F8 (`areMetaAnnotatedWith` for rule 8, needs its own fixture).
