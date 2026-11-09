---
spec: 06-distributor-acl
source: docs/spec-pack/specs/06-distributor-acl.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-08T20:32:05Z
qa-lead: on
base: factory
spec-branch: sdd/06-distributor-acl/spec
modules: [root, bookstore, distributor]
---

# Plan: Spec 06 Distributor anti-corruption layer

Restates `docs/spec-pack/specs/06-distributor-acl.md` (source of truth). Shaping decisions D-a to D-j are in `shape.md`, standards in `standards.md`, research summary in `references.md`. The full task cards (file scope, criteria, named tests, risks) are section 7 of the tech-lead strategy; the QA strategy (tiers, Gate 1 findings G0 to G15, common directive C0 and per-task directives, qa-final focus) is in the QA lead's ledger. Both live in the cycle's comms and are passed to every agent by path: `.claude/comms/mailboxes/lead-1/outbox/strategy-06-distributor-acl.md` (section 7) and `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-06-distributor-acl.md` (section "QA strategy"). Where a G finding and a card differ, the finding wins; G1 and G9 are already folded into the cards below.

Path abbreviations: B = `bookstore/src/main/java/nl/openvalue/bookstore/`, T = `bookstore/src/test/java/nl/openvalue/bookstore/`, F = `bookstore/src/test/java/nl/openvalue/archfixtures/`, C = `bookstore/src/test/resources/contracts/distributor/`.

## Summary

Add the outbound anti-corruption layer to the distributor (spec 05): the port `DistributorAppService` (JDK types and DTOs, three nested exceptions), `DistributorAppServiceImpl` with Spring `RestClient` and `DistributorProperties` (base URL, 2 s connect, 5 s read), contract tests with `MockRestServiceServer` over JSON captured from the real distributor, the use case `CatalogueFlow.importFromDistributor` and `POST /books/commands/import-from-distributor` (202, 404, 409, 422, 503), one ArchUnit rule keeping `*AppServiceImpl` nested classes inside `acl`, and `DistributorImportIntegrationTest`. Referee `./mvnw clean verify` from the repo root, green after every task, budget 60 s (baseline about 22 s: bookstore 684 tests, distributor 190, 10 Started lines in the bookstore).

## Ground rules for every task

- The pack spec decides what; `docs/spec-pack/product/architecture-standard.md`, `tech-stack.md` and `docs/spec-pack/orchestrator/decisions.md` win. Bookstore code goes through the `hexagonal-slice` skill (with `clean-hexagonal-onion`, `ddd-building-blocks`, `layer-testing`); the standard and the spec win over the skills. A contradiction is reported as blocked, never resolved by the implementer.
- Only the task's fileScope is edited, never anything else, not even temporarily. Never the distributor module.
- Never skip, disable, delete or weaken a test or an ArchUnit rule; never edit an existing ArchitectureTest rule; never add `ignoreDependency`; no `@Disabled`, `assume*`, `@DirtiesContext`; never `--no-verify`.
- Versions only from tech-stack.md; the only new dependency is `spring-boot-starter-restclient` (T3, no version). No WireMock, no MockServer, no `@RestClientTest`. No test calls the real distributor (live runs against a started distributor are evidence steps of T2, T7 and qa-final, never tests).
- Time only through a fixed `Clock` in tests; never the wall clock.
- Lessons from earlier reviews, binding: a payload constraint repeats a domain rule only where the spec names 400 (where a 422 domain code exists, the flow owns it); no mutable shared state in singletons (only final fields, unmodifiable tables); watch numeric coercion of request bodies (JSON numbers or booleans into String fields); every error status the spec names has a test asserting the ProblemDetail `code`; every distributor failure becomes one of the three port exceptions.
- Falsifiers first: two values per pass-through field, both sides of every boundary, order witnesses with at least three rows not monotone in any column, authors in non-alphabetical order.
- Plain English, no em dashes; every new or changed file is ASCII (`rg -n '[^\x00-\x7F]' <file>` empty). Product files and test code never reference `docs/spec-pack`, `agent-os`, `.claude`, `CLAUDE.md`. Never `ANTHROPIC_API_KEY`.
- Git is read-only for every agent; the orchestrator owns commits, branches and PRs.
- Every implementer records its final test method names in its outbox as the task's rows of the scenario-to-test map.
- Scratch and evidence files: session scratchpad `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/` with prefix `s06-T<n>-<role>-`. Long runs through `bash ~/.claude/scripts/with-budget.sh <s> ...`. One live distributor at a time (port 8090; check health first, use another port and say why if 8090 is taken).

## Task table

| id | summary | module | dependsOn | batch | qaTier | qaModel | implModel | status | verdict | qa-lead | rounds | pr | commit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Port `DistributorAppService`, two DTO records, three nested exceptions | bookstore | [] | 1 | standard | sonnet | sonnet | done (branch `sdd/06-distributor-acl/T1`) | PASS (r0) | ENDORSE | 0 | #67 | 37dbd0e |
| T2 | Contract JSON captured live from the distributor, README, `http/distributor.http` additions | bookstore | [] | 2 | elevated | sonnet | sonnet | done (branch `sdd/06-distributor-acl/T2`) | PASS (r0) | ENDORSE | 0 | #69 | 62079f8 |
| T3 | `DistributorAppServiceImpl`, `DistributorProperties`, restclient starter, base-class mock, contract and wiring tests | bookstore | [T1, T2] | 3 | critical | opus | opus | done (branch `sdd/06-distributor-acl/T3`) | PASS (r2) | ENDORSE | 2 | #75 | 8ee41d4 |
| T4 | `CatalogueFlow.importFromDistributor` and `CatalogueFlowTest` | bookstore | [T1, T3] | 4 | critical | opus | opus | done (branch `sdd/06-distributor-acl/T4`) | PASS (r2) | ENDORSE | 2 | #78 | ab52051 |
| T5 | `ImportBookPayload`, `POST /books/commands/import-from-distributor`, `BookCommandsTest` | bookstore | [T4] | 5 | elevated | sonnet | sonnet | done (branch `sdd/06-distributor-acl/T5`) | PASS (r0) | ENDORSE | 0 | #80 | d6cd48a |
| T6 | ArchUnit rule for nested `*AppServiceImpl` classes, fixtures, self-test registry | bookstore | [T3] | 6 | critical | opus | opus | done (branch `sdd/06-distributor-acl/T6`) | PASS (r1) | ENDORSE | 1 | #82 | 3bd96d2 |
| T7 | `DistributorImportIntegrationTest` and import examples in `http/catalogue.http` | bookstore | [T5] | 7 | elevated | sonnet | sonnet | done (branch `sdd/06-distributor-acl/T7`) | PASS (r0) | ENDORSE | 0 | #84 | 1dd327f |
| T8 | Test-only follow-ups FU-T3-1 to 3 in `DistributorAppServiceImplTest` and FU-T5-1 to 3 in `BookCommandsTest` plus one comment fix in `http/catalogue.http` (Amendments A1, A2, A4) | bookstore | [T3, T5, T7] | 8 | elevated | sonnet | sonnet | done (branch `sdd/06-distributor-acl/T8`) | PASS (r0) | ENDORSE | 0 | #86 | 5e82a14 |

Execution and merge order, one task at a time: T1, T2, T3, T4, T5, T6, T7, T8. Pack task mapping: pack 1 = T1; pack 2 = T2; pack 3 = T3; pack 4 = T4 + T5; pack 5 = T6 + T7.

qaDirective of every task: C0 plus the task's T<n> section of the QA ledger, plus every `## QA feed-forward` entry recorded before the task starts.

## Tasks

### T1: Port, DTOs and nested exceptions

- module: bookstore; dependsOn: []; implModel sonnet; qaTier standard (sonnet).
- fileScope: `B/domaininteraction/DistributorAppService.java`, `B/domaininteraction/DistributorTitleDTO.java`, `B/domaininteraction/RestockConfirmationDTO.java`, `T/domaininteraction/UseCaseExceptionsTest.java`.
- Acceptance criteria:
  1. `DistributorAppService` declares exactly `Optional<DistributorTitleDTO> findTitle(String isbn)` and `RestockConfirmationDTO requestRestock(String isbn, int quantity, String retailerReference)`.
  2. `DistributorTitleDTO` and `RestockConfirmationDTO` records have exactly the spec's components, names, types and order. JDK types only, no Spring import.
  3. Nested `final class` exceptions in the interface (`BookDataService` style): `TitleNotFoundAtDistributorException extends NotFoundException` (`title-not-found-at-distributor`, "Title not found at distributor"), `TitleNotOrderableException extends InvalidInputException` (`title-not-orderable`, "Title not orderable"), `DistributorUnavailableException extends UnavailableException` (`distributor-unavailable`, "Distributor unavailable"); the detail names the ISBN where known.
  4. `UseCaseExceptionsTest` asserts per exception: superclass, `code()`, `title()`, detail, `details()` empty and non-null.
  5. Javadoc on `DistributorTitleDTO`: genre is the name of a bookstore genre constant; recommended price has scale 2; currency is EUR because the port rejects other currencies.

### T2: Capture the contract files from the running distributor

- module: bookstore (the root file `http/distributor.http` rides along); dependsOn: []; implModel sonnet; qaTier elevated (sonnet).
- fileScope: `C/**` (JSON files and `README.md`), `http/distributor.http` (append requests only; existing requests unchanged).
- Procedure: start the distributor (`./mvnw -pl distributor spring-boot:run` or the jar), check health, send each request as written in `http/distributor.http` (curl is fine), store each body verbatim; restart once with `--distributor.chaos.failure-rate=1.0` for the 503 body; stop the distributor afterwards.
- Files (strategy section 7, t2): 14 all-AUTHOR titles (Pride and Prejudice 9780141439518; genre rows The Last Man 9789900010050, Connecticut Yankee 9789900010791, A Study in Scarlet 9789900010159, Sense and Sensibility 9789900010029, Frankenstein 9789900010043, Utopia 9789900010616, On the Origin of Species 9789900011163, Passages from the Life of a Philosopher 9789900011187, Little Women 9789900010821, The Raven 9789900010210; Pamela 9789900010678 OUT_OF_PRINT; Bleak House 9789900010272 BACKORDER; two-author 9789900020080); error bodies title-not-found 404, unknown-path 404, chaos 503; restock request/response pairs CONFIRMED, BACKORDERED, NOT_ORDERABLE, TITLE_NOT_FOUND, INVALID_REQUEST; derived files (exactly one change each) USD currency, unmapped subject `["XYZ000000", "FIC028000"]`, round price 1200, authors reversed.
- Acceptance criteria:
  1. Every file in `C/` is listed in `README.md` with source request (method, path, body), distributor run flags and capture date; derived files name their source file and the single change.
  2. No captured title has a contributor whose role is not AUTHOR.
  3. Every restock request body has all three keys (`ean`, `quantity`, `retailerReference`).
  4. README lists the literal values later tests assert (restock order ids, expected delivery dates, release years, prices).
  5. README and http comments contain no agent or pack paths; ASCII only; plain English.

### T3: DistributorAppServiceImpl, DistributorProperties, starter, base-class mock

- module: bookstore; dependsOn: [T1, T2]; implModel opus; qaTier critical (opus).
- fileScope: `bookstore/pom.xml`, `B/acl/DistributorAppServiceImpl.java`, `B/acl/DistributorProperties.java`, `T/acl/DistributorAppServiceImplTest.java`, `T/acl/DistributorAppServiceImplWiringTest.java`, `T/IntegrationTestBase.java`.
- Acceptance criteria: strategy section 7 t3 criteria 1 to 8 and its named tests, plus QA G3 to G8, G15. In short:
  1. pom: `spring-boot-starter-restclient` without a version; nothing else added.
  2. `DistributorProperties` is a `@ConfigurationProperties("distributor")` record (base-url, connect-timeout, read-timeout) with `@DefaultValue` `http://localhost:8090`, `2s`, `5s`, registered by `@EnableConfigurationProperties` on the impl; `BookstoreApplication` unchanged.
  3. Public constructor `(RestClient.Builder, DistributorProperties)` sets base URL and the timeout request factory; package-private constructor `(RestClient)`; only final fields, unmodifiable genre table.
  4. Private nested payload records with reference-typed components; translation exactly per the spec table and D-a; genre from the first subject code by longest-prefix match (`startsWith`), anything else (including missing or empty codes) FICTION with one WARN naming the code.
  5. Only the three port exceptions or `Optional.empty()` leave a public method; error bodies parsed in a guarded block; the path variable goes through a URI template; a WARN with the status or exception class (not the body) when a failure becomes `DistributorUnavailableException`.
  6. `IntegrationTestBase` gains `@MockitoBean protected DistributorAppService distributorAppService;` plus one Javadoc sentence; nothing else changes there.
  7. `DistributorAppServiceImplTest` (`MockRestServiceServer` bound to a `RestClient.builder()`, contract files from `C/`): every translation row, two values per pass-through, the ten genre rows each parsed by `Genre.parse`, table completeness against `Genre.values()`, the derived files, restock CONFIRMED and BACKORDERED with strict request matching, 404 and 422 mappings for both methods, and every failure class of D-a for both methods (one case per required key, null and empty subject codes as two cases, malformed 404 and 422 bodies).
  8. `DistributorAppServiceImplWiringTest` (`ApplicationContextRunner`): one `DistributorAppService` bean, defaults and overrides bound; loopback `HttpServer` test per G3 (handler returns a valid title after 2 s; connect 5 s, read 250 ms; elapsed between 200 and 1800 ms; positive control without sleep returns the full Pride and Prejudice DTO); never port 8090.

### T4: Import use case in CatalogueFlow

- module: bookstore; dependsOn: [T1, T3]; implModel opus; qaTier critical (opus).
- fileScope: `B/domaininteraction/CatalogueFlow.java`, `T/domaininteraction/CatalogueFlowTest.java`, plus every other test file that calls `new CatalogueFlow(` (constructor update only; list them in the outbox).
- Acceptance criteria:
  1. The constructor gains `DistributorAppService`; `importFromDistributor(String isbn, BigDecimal priceOverride)` returns `UUID`; Javadoc states the order below and that the remote call runs inside the use-case transaction.
  2. Order (D-f): build `Isbn` and, when non-null, the override `Money` inside the domain-exception translator; then `findByIsbn(normalised)` (present: 409, port not called); then `findTitle(normalised)` (empty: `TitleNotFoundAtDistributorException`); `orderable == false`: `TitleNotOrderableException`; then `Book.addToCatalogue` with the DTO's title, authors, `Genre.parse(genre)`, the override or `Money.of(recommendedPrice, currency)`, the publication year and the flow's clock; then the same private path as `addBook` (save book, open the empty stock item). Port exceptions pass through unchanged.
  3. `CatalogueFlowTest` (mocked ports): strategy section 7 t4 tests plus G2 (both failures in one case for every order claim; authors not alphabetical). The year boundary uses a clock in the year 2000 (G1): year 2001 accepted, year 2002 gives 422 `publication-year-out-of-range`.

### T5: ImportBookPayload and the command endpoint

- module: bookstore; dependsOn: [T4]; implModel sonnet; qaTier elevated (sonnet).
- fileScope: `B/command/ImportBookPayload.java`, `B/command/BookCommands.java`, `T/command/BookCommandsTest.java`.
- Acceptance criteria:
  1. `ImportBookPayload(@NotBlank String isbn, @Pattern(regexp = "-?\\d+(\\.\\d+)?") String price)`; no `@NotBlank` on price.
  2. `POST /books/commands/import-from-distributor` returns 202 with `CommandResult(id)`; null-safe `new BigDecimal(price)`; `CommandExceptionHandler` unchanged.
  3. `BookCommandsTest`: strategy section 7 t5 tests plus G12 (price "-5", isbn "abc", a hyphenated isbn and one with a leading space reach the flow; pinned results for a JSON number and a boolean sent as price or isbn, never a 500); 404, 409, 422, 503 each with ProblemDetail `code` and title from the real nested exception; 400 `validation-failed` with the field.

### T6: ArchUnit rule and fixtures

- module: bookstore; dependsOn: [T3]; implModel opus; qaTier critical (opus).
- fileScope: `T/ArchitectureTest.java` (additions only), `T/ArchitectureSelfTest.java`, `F/command/DistributorProxyCommands.java`, `F/acl/LeakyAppServiceImpl.java`, `F/command/TitleImportCommands.java`.
- Acceptance criteria:
  1. One new `@ArchTest public static final ArchRule`: no classes outside `nl.openvalue..acl..` depend on classes nested at any depth in a class whose simple name ends with `AppServiceImpl`; `allowEmptyShould(true)`; a described predicate; a one-line comment. `git diff` of `ArchitectureTest` shows additions only; no `ignoreDependency`.
  2. Fixtures: `DistributorProxyCommands` (a `@RestController` with a `RestClient` field and nothing from acl); `LeakyAppServiceImpl` with a non-private nested record (public; A3), a record nested two levels deep (G9), and a method using them; `TitleImportCommands` (a `@RestController` with a field of the nested record type and a second field of the depth-2 type, no `RestClient`, no reference to the outer class). One-line "Fixture: ..." Javadoc.
  3. `ArchitectureSelfTest`: `REST_CLIENT_ONLY_IN_ACL` lists `LeakyDelegate` (first) and `DistributorProxyCommands`; the new rule lists `TitleImportCommands`; `hasSize(21)` becomes 22 (D-j); only the three existing lines named by G11 change; a test method per new rule as for the others; a self-test that the violation names the depth-2 field.
  4. Controls (G10, matched on the class or field causing the violation): the new rule does not report `LeakyAppServiceImpl`, nor a class depending on a nested class of a class not ending with `AppServiceImpl`; the new rule does not report `DistributorProxyCommands`, and `REST_CLIENT_ONLY_IN_ACL` does not report `TitleImportCommands`; the full rule description is pinned with `isEqualTo`.
  5. Surefire shows 22 tests in `ArchitectureTest`; the production run is green with the real private records.

### T7: DistributorImportIntegrationTest and the catalogue http example

- module: bookstore (the root file `http/catalogue.http` rides along); dependsOn: [T5]; implModel sonnet; qaTier elevated (sonnet).
- fileScope: `T/DistributorImportIntegrationTest.java`, `http/catalogue.http`.
- Acceptance criteria:
  1. Extends `IntegrationTestBase`, no class-level context annotation, stubs the inherited `distributorAppService`.
  2. Import with the recommended price: 202; the book reads price "9.99", genre FICTION, authors ["Jane Austen"], every view field asserted as a literal; stock 0 through the existing stock read endpoint.
  3. Override "12.00" posted with a hyphenated ISBN: price "12.00"; the port receives the digits only (G13).
  4. Already in catalogue (set up through the add endpoint): 409 `isbn-already-exists`, `verifyNoInteractions(distributorAppService)`, exactly 1 book row and 1 stock row (G13).
  5. Distributor down (stub throws `DistributorUnavailableException`): 503, title "Distributor unavailable", code `distributor-unavailable`, zero book and stock rows; also 404 and 422 with their codes and zero rows.
  6. `http/catalogue.http`: an import without price (EAN 9789900010159) and one with a price override (EAN 9789900010050), file style kept, a comment that the distributor must be running; existing requests unchanged.
  7. No new Started line (still 10).

### T8: Test-only follow-ups to T3 (Amendment A1)

- module: bookstore; dependsOn: [T3]; implModel sonnet; qaTier elevated (sonnet).
- fileScope: `T/acl/DistributorAppServiceImplTest.java`, `T/command/BookCommandsTest.java` (A2) and `http/catalogue.http` (A4, one comment line only). No production change.
- Acceptance criteria (the QA lead's FU-T3-1 to FU-T3-3, ledger review "T3 r2"):
  1. FU-T3-1: a title body without `availability` (variant of a committed file) gives `orderable` true and no exception; the mutant `!title.availability().equals("OUT_OF_PRINT")` fails it.
  2. FU-T3-2: `subjectCodes: [null, "FIC028000"]` gives FICTION and exactly one WARN `No bookstore genre for subject code null of EAN 9789900010050, using FICTION`; the mutant `filter(Objects::nonNull).findFirst()` fails it.
  3. FU-T3-3: `listPriceCents: -999` gives `new BigDecimal("-9.99")`; the mutant `cents.abs()` fails it.
  4. FU-T5-1 (A2): new parameterized test posting price `"+5"`, `"12.00 "` and `"5 "`, plus a test whose price is `Character.toString(0x0661) + Character.toString(0x0662)` (the file stays ASCII); each gives `expectProblem(result, 400, "validation-failed", "Validation failed")`, `$.details.length()` 1, `$.details[0].field` `"price"` and `verifyNoInteractions(flow)`. Must-fail probes R4, R5, R12.
  5. FU-T5-2 (A2): captor rows `"12.345"` gives `new BigDecimal("12.345")` and `"123456.789"` gives `new BigDecimal("123456.789")`, each 202 and `isEqualTo`. Must-fail probes R7, R8, C5d.
  6. FU-T5-3 (A2): isbn pass-through rows `"ABC"` and `"978014143951X"` reach the flow unchanged (`isEqualTo(isbn)`). Must-fail probe C9c.
  7. A4: the comment above the appended import requests in `http/catalogue.http` ("The next two requests import a book", but the second one is a GET) says what the requests do; no request line changes.
  8. Existing tests unchanged (insertion only, new methods); production hashes unchanged; FF-4e and the referee green; the new totals and ACL WARN count stated exactly.

## QA feed-forward

From the QA lead's T1 r0 review (full text in the QA ledger, "Feed-forward issued"):

- FF-1 (C0-5c, every verifier and qa-final item 6): `rg -n '[^\x00-\x7F]' bookstore/src http` prints exactly the two pre-existing lines `bookstore/src/test/java/nl/openvalue/bookstore/domain/IsbnTest.java:67` and `:74` (spec 02 Arabic-Indic digit witnesses); any other line is a FAIL; the same grep on every fileScope file (JSON contract files and README included) is empty.
- FF-2 (C0-6 baseline): bookstore section also accepts exactly two Maven `Could not transfer metadata /.meta/prefixes.txt ... 401` warnings (repositories of the pinned flyway parent POM); HHH000247 exactly 4 with their paired WARN lines; dispatcherServlet ERROR exactly 5. Distributor section unchanged through spec 06: 6 BeanPostProcessorChecker WARNs, 1 ToolInputValidator WARN, 4 logback "Empty or null pattern." lines, 1 DefaultHandlerExceptionResolver WARN. Bookstore total after T1 is 690.
- FF-3 (T5, T7): the port exception details are fixed by T1: not found "The distributor has no title with ISBN <isbn>"; not orderable "The title with ISBN <isbn> cannot be ordered from the distributor"; unavailable is the detail the ACL passes. Detail assertions use exactly these strings.

From the QA lead's T2 r0 review:

- FF-4 (T3 implDirective and qaDirective): (a) every stubbed distributor answer is the committed file loaded from the classpath `contracts/distributor/<name>`, never a retyped copy; bodies no capture holds (G4 missing key, G5 `XYZFIC028`, malformed JSON, empty body) are built in the test and named as variants. (b) Capture-day values are asserted as the README literals, never computed: restockOrderId `7597c6da-0041-473d-861a-d4b84c7fb204` and `207e15be-cf0e-47da-90e8-60396068690f`; expectedDeliveryDate 2026-11-10 and 2026-11-22; publicationYear 2026 for 9789900020080, 1813 for Pride and Prejudice; the ACL test holds no clock. (c) Strict request matching uses the committed `restock-confirmed-request.json` and `restock-backordered-request.json` (the distributor does not echo retailerReference, so that matcher is the only check on it). (d) The D-a cases with a capture use it for both methods: `restock-invalid-request-response.json` with 400, `unknown-path-404.json` with 404, each `isExactlyInstanceOf(DistributorUnavailableException.class)`; G6's must-not-appear literal is the 503 file's message. (e) No contract file changes in T3; at the T3 gate `shasum -a 256 -c` of the T2 hash baseline `s06-T2-qa-hashes.txt` (scratchpad) shows 33 of 33 OK.
- FF-5 (qa-final): a later-day replay of `http/distributor.http` expects the README statuses and keys, not the capture-day ids and dates; the last appended GET needs a distributor started with `--distributor.chaos.failure-rate=1.0`; the contract bytes are proven by the T2 hash baseline.

From the QA lead's T3 r2 review (full text in the QA ledger, FF-6 to FF-8):

- FF-6 (T4 implDirective and qaDirective): in at least one success test the stubbed `DistributorTitleDTO` carries an isbn different from the requested ISBN (for example request `978-0-14-143951-8`, stub isbn `9789900010050`); the saved BookDTO isbn is `9780141439518`. Must-fail probe F17: the book is built from the DTO isbn instead of the normalised request ISBN.
- FF-7 (T4 to T8 C0 baselines): bookstore total after T3 is 792 (DistributorAppServiceImplTest 97, DistributorAppServiceImplWiringTest 5); ACL WARN baseline exactly 66 (`grep -c -E " WARN .*DistributorAppServiceImpl +:" <referee log>`); every later gate runs `shasum -a 256 -c` on the scratchpad file `s06-T3-qa-r2-hashes.txt` from the repo root (6 of 6 OK) unless T8 owns the change of the test file.
- FF-8 (qa-final): T3 hash integrity (or T8's baseline); the three FU-T3 must-fail probes on the tip; a live import of Pride and Prejudice whose stored isbn equals the request.

From the QA lead's T4 r2 review (full text in the QA ledger, FF-9 to FF-11):

- FF-9 (T5 to T8 C0 baselines): bookstore total after T4 is 829 (CatalogueFlowTest 70); ACL WARN stays exactly 66; every later gate also runs `shasum -a 256 -c` on the scratchpad file `s06-T4-qa-r2-hashes.txt` (2 of 2 OK). T7 probes that mutate `CatalogueFlow.java` restore it to sha `f8d925e4...`.
- FF-10 (T5 to T8 implDirective and qaDirective): value-relation sweep. Wherever code chooses or compares between two values (override vs recommended, given vs default, request vs stored), tests hold data below, equal and above, the zero value and both domain range ends, each asserted as a literal or as the exact 422; implementers table the sweep in the outbox (input x relation x test). For T5: price `"0"` and `"0.00"` reach the flow as `new BigDecimal("0")` and `new BigDecimal("0.00")` (captor literals), next to G12's `"-5"`.
- FF-11 (qa-final): T4 hash integrity; a live import of The Last Man with an override below its captured 11.99 (for example 7.50) stores 7.50 EUR; residual R7 accepted: the remote call widens the window between the duplicate check and the save, closed by the unique constraint and the adapter's 409 translation (no second check, by plan AC2; the lead card's wording differed).

From the QA lead's T5 r0 review (full text in the QA ledger, FF-12, FF-13):

- FF-12 (T6 to T8 C0 baselines and qa-final): bookstore total after T5 is 874 (BookCommandsTest 73); C0-6 (b) also accepts exactly 1 `DefaultHandlerExceptionResolver` WARN in the bookstore section (the `HttpMessageNotReadableException ... START_OBJECT` line from `importRejectsAnObjectPriceAsAnUnreadableBodyWith400`); ACL WARN stays 66; every later gate also runs `shasum -a 256 -c` on the scratchpad file `s06-T5-qa-hashes.txt` (3 of 3 OK) unless T8 owns the change of BookCommandsTest.
- FF-13 (qa-final): T5 hash integrity (or T8's baseline); live on the running app, POST import with price `{}` and with a malformed body gives 400 and never 500, body and content type recorded verbatim (residual R8); the FU-T5 must-fail probes on the tip.

From the QA lead's T6 r1 review (full text in the QA ledger, FF-14, FF-15):

- FF-14 (T7, T8 C0 baselines and qa-final): bookstore total after T6 is 893; ArchitectureTest 22/0/0/0 and ArchitectureSelfTest 58/0/0/0 at every later gate; `ArchitectureTest.java`, `ArchitectureSelfTest.java` and `archfixtures/` have empty diffs against each later PR base; every later gate also runs `shasum -a 256 -c` on the scratchpad file `s06-T6-qa-r1-hashes.txt` (5 of 5 OK).
- FF-15 (qa-final): T6 hash integrity; on the tip, the production leak probe P-prod (public `ErrorPayload` plus a field of its type in a command class fails the new rule with the field quoted; restore hash-checked) and the green control; the R4T and XCASE mutants still fail ArchitectureSelfTest.

From the QA lead's T7 r0 review (full text in the QA ledger, FF-16 to FF-18):

- FF-16 (T8 C0 baselines and qa-final): bookstore total after T7 is 920 (DistributorImportIntegrationTest 27); Started 10; ACL WARN 66; 1 DefaultHandlerExceptionResolver WARN in the bookstore section (T8 names any WARN its rows provoke on purpose and states the new count); `DistributorImportIntegrationTest.java` has an empty diff in T8; every later gate also runs `shasum -a 256 -c` on the scratchpad file `s06-T7-qa-hashes.txt` (T8 replaces the `http/catalogue.http` line, and the T3 and T5 test-file lines, with its own baseline).
- FF-17 (T8 implDirective): never edit a file outside the fileScope, not even temporarily; mutation probes on production code are the verifier's job; describe each mutant in the outbox instead.
- FF-18 (qa-final): T7 hash integrity; T7 survivors O4 and V6 are T4's F7 and F5 (re-run only if CatalogueFlow or CatalogueFlowTest changed); the `.http` replay needs an empty catalogue; a Testcontainers "Could not connect to Ryuk" start error is BLOCKED-with-diagnosis and one re-run.

From the QA lead's T8 r0 review (full text in the QA ledger, FF-19):

- FF-19 (qa-final): baselines on the tip: bookstore 931 (DistributorAppServiceImplTest 100, BookCommandsTest 81, CatalogueFlowTest 70, DistributorImportIntegrationTest 27, ArchitectureTest 22, ArchitectureSelfTest 58), distributor 190; ACL WARN exactly 67; 1 DefaultHandlerExceptionResolver WARN and 10 Started lines in the bookstore section. The T8 hash file `s06-T8-qa-hashes.txt` replaces the three lines T8 owns in the T3, T5 and T7 baselines; every other line stays OK (T2 33 included). If every hash holds, the follow-up probe evidence carries over.

## Amendments

- A1 (2026-11-08T22:46Z, orchestrator, self-accepted): new task T8, test-only, inside T3's test file, carrying the QA lead's follow-ups FU-T3-1 to FU-T3-3 from the T3 r2 ENDORSE. Consequence: one more task PR before qa-final; no production change, no requirement change. Within authority (tests only, cheap and reversible).
- A2 (2026-11-09T00:23Z, orchestrator, self-accepted): T8 also carries the QA lead's test-only follow-ups FU-T5-1 to FU-T5-3 from the T5 r0 ENDORSE; its fileScope gains `T/command/BookCommandsTest.java` and it now depends on T5. Consequence: none on requirements or production code. Within authority (tests only).
- A3 (2026-11-09T01:29Z, orchestrator, self-accepted, QA lead endorsed): T6 criterion 2 said "package-private nested record", which cannot compile with the `TitleImportCommands` clause (another package); the pack spec says "non-private" and wins. Wording corrected; the fixture records are public. No change to the bar (the guard does not depend on visibility).
- A4 (2026-11-09T02:03Z, orchestrator, self-accepted): T8 also fixes the comment wording above the appended import requests in `http/catalogue.http` (QA lead T7 r0 note: the comment says both requests import, the second is a GET); spec 09 publishes the file. Comment only; T8 now depends on T7.
