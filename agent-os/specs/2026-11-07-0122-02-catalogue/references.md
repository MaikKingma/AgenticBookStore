# References for Spec 02 Catalogue

## Similar Implementations

None in the product packages: spec 02 is the first slice. Spec 01 code that spec 02 builds on:

- `bookstore/src/test/java/nl/openvalue/bookstore/ArchitectureTest.java` and `ArchitectureSelfTest.java`: rule declaration style (`@ArchTest public static final ArchRule`, anchored package constants, `allowEmptyShould(true)`), the `EXPECTED_VIOLATORS` map keyed by rule field name, `assertReportsExpectedViolators`, the rule-count assertion and the named-fixture list. T7 extends them in the same style.
- `bookstore/src/test/java/nl/openvalue/archfixtures/**`: fixture conventions (plain classes, javadoc "Fixture: ...", allowed suffix per package, `java..` types only).
- `bookstore/src/test/java/nl/openvalue/bookstore/PostgresTestConfig.java`, `IntegrationTestBase.java`, `DatabaseCleaner.java`: the shared container, the full-context base class with `RestTestClient` and the before-each cleaner.
- `bookstore/src/main/resources/db/migration/V0__baseline.sql`: the migration naming; `V1__create_catalogue.sql` follows it.

## Research used for shaping (session artifacts, not tracked)

The reports live under `.claude/comms/mailboxes/<id>/outbox/` of the factory session that shaped this spec. Their load-bearing findings are restated here so the spec stands on its own.

### Web adapters on Boot 4.1.2, Spring Framework 7, Jackson 3 (res-1, verified with a probe project)

- Plain `@ExceptionHandler` methods in a `@RestControllerAdvice` suffice; `ProblemDetail.setProperty("code", ...)` and `setProperty("details", List<Map<...>>)` serialise flattened at the top level, `Content-Type: application/problem+json`; returning a bare `ProblemDetail` or `ResponseEntity<ProblemDetail>` both work; use `HttpStatus.UNPROCESSABLE_CONTENT` and `status().isUnprocessableContent()`.
- `@WebMvcTest(BookCommands.class)` loads every `@ControllerAdvice` bean, so each advice is scoped with `@RestControllerAdvice(basePackageClasses = ...)`; unscoped twin advices let one win silently.
- Leave `spring.mvc.problemdetails.enabled` unset: when true, Boot's `ProblemDetailsExceptionHandler` beats an unordered advice for validation errors and the body loses `code` and `details`.
- `@Valid @RequestBody` on a record raises `MethodArgumentNotValidException`; read `getBindingResult().getFieldErrors()` (`getField()` is the component name, `authors[1]` for list elements, `getDefaultMessage()`); error order is unstable, so tests filter by field. Constraints on parameters raise `HandlerMethodValidationException` with an empty 400 (avoid them). Malformed JSON, bad UUIDs, 415 give empty bodies by default.
- Jackson 3 coerces "9.99" into a `BigDecimal` but an enum component rejects "cookbooks" and lowercase "fiction" with an empty 400, so payloads keep `String price` and `String genre`; `Integer publicationYear` with `@NotNull` (a primitive would default to 0). Views carry `String price` from `toPlainString()`; records and enums serialise by name.
- `@ResponseStatus(ACCEPTED)` on a void method gives 202 without a body, on a record 202 with JSON; a body-less POST works.
- Idioms: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.test.context.bean.override.mockito.MockitoBean`, MockMvc `jsonPath("$.code")` and `content().contentType("application/problem+json")`; `RestTestClient` `.post().uri(...).contentType(MediaType.APPLICATION_JSON).body(...).exchange().expectStatus().isEqualTo(422).expectHeader().contentType("application/problem+json").expectBody().jsonPath("$.code").isEqualTo(...)`, `.expectBody(CommandResult.class).returnResult().getResponseBody()`, `.expectBody().isEmpty()`.

### Data adapter on Boot 4.1.2, Hibernate 7.4, Spring Data JPA 4.1, PostgreSQL 18.6 (res-2, verified with a probe project)

- `ddl-auto=validate` checks table and column existence and the JDBC type family only (not length, precision, scale, nullability, unique, pk, fk); `int` for an `Integer` and `bigint` for a `Long` validate; so scale 2 and ISBN uniqueness are proven behaviourally.
- `@ElementCollection` + `@CollectionTable(name = "book_author", joinColumns = @JoinColumn(name = "book_id"))` + `@OrderColumn(name = "position")` + `@Column(name = "name", length = 120)` yields exactly `book_author(book_id, position, name)`; order is preserved on reload, positions 0, 1, 2.
- Optimistic locking: `ObjectOptimisticLockingFailureException` surfaces when the flush goes through the repository (`saveAndFlush` or `flush`); `TestEntityManager.flush()` throws the raw `jakarta.persistence.OptimisticLockException`. Recipe: save, flush, clear, load, bump `version` with `JdbcTemplate`, save a changed copy.
- Search: JPQL over the element collection works with `exists (select 1 from b.authors a where lower(a) like :p escape '\\')`; `order by b.title` with a trailing `Limit.of(100)` becomes `fetch first ? rows only`. Trap: `(:x is null or lower(b.title) like lower(concat('%', :x, '%')))` fails with `function lower(bytea) does not exist` when `x` is null; build the lower-cased, escaped pattern in Java and pass it (null stays null).
- `@DataJpaTest` + `@Import(PostgresTestConfig.class)` + `@AutoConfigureTestDatabase(replace = NONE)` runs Flyway, rolls back per test, applies the `test` profile, starts no web layer; `@Service` and `@Component` beans need `@Import`. `IntegrationTestBase` cleans before each test, never after, so a slice test that needs exact counts cleans first inside its transaction.
- A duplicate ISBN surfaces as `DataIntegrityViolationException` at flush; `Optional<BookJPA> findByIsbn(String)` is a derived query.
- Annotation packages: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`.

### ArchUnit 1.5.1 and the spec 01 conventions (res-3, probes against the jar)

- One `ArchRule` for the money rule: `CompositeArchRule.of(noFields().that().areDeclaredInClassesThat().resideInAnyPackage(...).should().haveRawType(double.class).orShould().haveRawType(float.class)).and(noCodeUnits().that()...should().haveRawParameterTypes(<DescribedPredicate<List<JavaClass>> "include double or float">)).allowEmptyShould(true)`; `codeUnits` covers constructors (record canonical constructors included), `methods()` does not; the rule ignores `Double`, `double[]`, `List<Double>`; lambdas over `double` count as code units.
- Violation details start `Field <fqn.price> has raw type double`, `Method <fqn.m(double)> has raw parameter types ...`, `Constructor <fqn.<init>(float)> ...`, matching the self-test's origin regex.
- Self-test edits for a new rule: the rule field, one `EXPECTED_VIOLATORS` entry keyed by the field name with fixture FQNs, one `@Test` calling `assertReportsExpectedViolators`, `hasSize(20)` to `hasSize(21)`; fixture import is automatic via the map.
- `ClockConfig` in the root package trips no rule (layered rules only report edges touching the eight layers; no naming rule on the root); all three `IntegrationTestBase` subclasses need the bean once `CatalogueFlow` exists.
- `DatabaseCleaner` truncates every public base table except `flyway_schema_history` with CASCADE; the cleaner test has no empty-schema assertion; `BookstoreApplicationTests`' `version = '0'` check survives V1. Surefire sets `spring.profiles.active=test`.

### Tech-lead strategy (lead-1)

Approach A (inside-out layer tasks reordered for green-after-every-merge) with decisions D1 to D35, the ProblemDetail catalogue, the scenario-to-test map and the quality gates, restated in `shape.md` and `plan.md`. Forward check against specs 03, 04, 06, 08 and 12 for the signatures they consume (`CatalogueFlow.addBook`, `search`, `findById`, `BookDataService.BookNotFoundException`, `Genre.parse`, `BookView`).
