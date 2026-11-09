---
name: integration-test-techniques
description: RestTestClient and AssertJ techniques for *IntegrationTest classes (type-sensitive JSON asserts, exact key sets, probe-killing data, no unchecked warnings)
metadata:
  type: reference
---

- `RestTestClient` `jsonPath(..).isEqualTo(expected)` converts the actual value to the expected value's class before
  comparing. So `isEqualTo("9.99")` also passes when the price is a JSON number. When the type matters (string prices),
  read the body with `expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})` and compare with AssertJ.
  Jackson gives a `String` for a JSON string and a `Double` or `Integer` for a number.
- Map bodies give exact view shapes for free: `assertThat(view).isEqualTo(Map.of(...))` checks the key set and the
  values together. `List.equals` works across list types, so a `List.of(Map.of(...))` literal can be compared
  directly with a deserialised `details` array.
- Avoid raw-type unchecked notes in the referee log: use `extractingByKey("details", InstanceOfAssertFactories.LIST)`
  with `.singleElement(InstanceOfAssertFactories.MAP)`. Write `.extracting(d -> (Object) ((Map<?, ?>) d).get("field"))`;
  the `(Object)` cast is what lets `containsExactly(String...)` compile.
- Choose data that the expected production probes break: insert rows out of title order AND out of ISBN order, so
  a sort-by-ISBN mutation fails. Every filter in a search URL needs its own decoy: for each criterion, one row
  that matches all the other criteria but not this one (same-author other-genre row for genre, same-genre
  other-author row for author). QA probes each criterion by passing null for it. Open-in-view is off, so any get or change after an add goes through lazy authors and kills a
  removed flow `@Transactional`.
- The full context is cached across `IntegrationTestBase` subclasses: a new subclass adds no Started line. A
  `@MockitoSpyBean` on a port and a plain class in the base `@Import` keep it that way.
- The base cleans BEFORE each test, not after: a `PER_CLASS` subclass's non-static `@BeforeAll` runs on the previous
  class's leftover rows, so any row it inserts needs keys (ISBN, id) that no other test uses.
- Verifier inspection greps (`assert|fail\(`, forbidden annotations) also match Javadoc and comments: keep those
  tokens out of the prose of the file they grep.
- Order claims over a whole list with fixed "added first / added last" rows: with three rows the insertion order is
  then always monotone, so use at least four. Columns no use case changes (reserved, reorder level) are set by
  `jdbcTemplate.update(...)` with an `isEqualTo(1)` row-count check.

- MockMvc `jsonPath("$.details").value(List.of(...))` converts the actual array to the expected value's class and then
  reports `but was:<null>`. Wrap it: `.value(equalTo(List.of(Map.of(...))))` (Hamcrest, no conversion).
- Payload records: `@Valid List<@NotNull X>` logs Hibernate Validator warning HV000271 (container-level `@Valid`
  deprecated). Write `List<@Valid @NotNull X>`; field paths stay `lines[1]` and `lines[1].bookId`.

- Order claims over books whose ids the app generates: seed `book`, `book_author` and `stock_item` by SQL with fixed
  ids (first hex digit 0 to 7), so the id column can be made non-monotone too; keep the pack scenarios on HTTP adds.
  Set instants (`placed_at`) by `jdbcTemplate.update(..., OffsetDateTime.ofInstant(i, ZoneOffset.UTC), id)` so views
  can be compared with literal ISO strings and newest-first never depends on request timing.
- Root-order claims over a fetch-joined query (orders with lines): QA also sorts by each child column (book id,
  isbn, title, price, quantity) asc and desc over the flat joined rows, where the root keeps its first appearance,
  ties enumerated. Root-level columns alone are not enough. Give each listed root its own books, and put the
  smallest and largest child value of every column in the middle root. Tabulate with a script before running.
- Verifier greps `^\s*@` on integration classes allow only `@Test`/`@Autowired`: a generic varargs helper needs
  `@SafeVarargs` (or logs an unchecked note), so pass `List<Map<String, Object>>` instead.

- QA greps `LocalDate(Time)?\.now\(` over `bookstore/src/test` and allows only the base's default-answer line. A
  test that needs "today by the application Clock" autowires its own `Clock` and writes
  `LocalDate.ofInstant(clock.instant(), clock.getZone())`. Stub the shared `@MockitoBean` default in the base
  `@BeforeEach` with `doAnswer(...).when(mock)`; it causes no strict-stubbing failure in tests that never call it.
- Null-valued JSON key pins: wrap the unchanged `Map.of(...)` literal in one `withNoPending...(Map)` helper that copies
  into a `LinkedHashMap` and puts the null, so existing pin lines keep their figures. `Map.equals` treats a missing key
  as different from a null value, so `isEqualTo` is itself a presence check. In slices,
  `jsonPath("$.k").value(nullValue())` fails on a missing path, and `jsonPath("$", hasKey("k"))` pins presence.
- `OutputCaptureExtension` on a full-context IT: `CapturedOutput.getOut()` joins the class-level capture with the test's
  own, so a test also sees the context start-up and every EARLIER test of the class. Assert per unique token (a fresh
  book id, a literal only that test throws), never a bare `containsOnlyOnce(" WARN ")`. "Exactly one WARN naming X"
  without filtering: `containsPattern(" WARN .*" + x)` plus `doesNotContainPattern("(?s) WARN [^\\n]*" + x + ".* WARN [^\\n]*" + x)`.
- Spring's after-commit listener failure logs `ERROR ... TransactionSynchronization.afterCompletion threw exception`,
  then a BLANK line, then `java.lang.X: <message>`: join them with `\\R+`, not `\\R`. Logged per failing listener call.
- Re-stubbing the shared mock replaces the base default; for "fail first, then the default", rebuild the default answer
  locally (`doThrow(e).doAnswer(this::defaultConfirmation)`), since the base's lambda cannot be chained.

Related: [[flow-test-techniques]], [[jpa-slice-traps]].
