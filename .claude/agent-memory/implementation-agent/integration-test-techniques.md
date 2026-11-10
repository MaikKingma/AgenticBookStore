---
name: integration-test-techniques
description: RestTestClient and AssertJ techniques for *IntegrationTest classes (type-sensitive JSON asserts, exact key sets, probe-killing data, no unchecked warnings)
metadata:
  type: reference
---

- `RestTestClient` `jsonPath(..).isEqualTo(expected)` converts the actual value to the expected value's class, so
  `isEqualTo("9.99")` also passes for a JSON number. When type matters, read
  `expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})` and compare with AssertJ (Jackson gives
  `String`, `Double` or `Integer`).
- Map bodies give exact view shapes: `assertThat(view).isEqualTo(Map.of(...))` checks key set and values together;
  `List.equals` works across list types. Null-valued key pins: wrap the `Map.of(...)` literal in a
  `withNoPending...(Map)` helper copying into a `LinkedHashMap` plus the null; `Map.equals` treats a missing key as
  different from null, so `isEqualTo` is a presence check. In slices `jsonPath("$.k").value(nullValue())` fails on a
  missing path; `jsonPath("$", hasKey("k"))` pins presence.
- No raw-type unchecked notes: `extractingByKey("details", InstanceOfAssertFactories.LIST)` with
  `.singleElement(InstanceOfAssertFactories.MAP)`; `.extracting(d -> (Object) ((Map<?, ?>) d).get("field"))` (the
  `(Object)` cast lets `containsExactly(String...)` compile). A generic varargs helper needs `@SafeVarargs`, which QA's
  `^\s*@` grep rejects (only `@Test`/`@Autowired`): pass `List<Map<String, Object>>` instead.
- Probe-killing data: insert rows out of title order AND ISBN order. Every search filter needs its own decoy row that
  matches all other criteria but not this one (QA probes each by passing null). Open-in-view is off, so a get after an
  add goes through lazy authors and kills a removed flow `@Transactional`.
- The full context is cached across `IntegrationTestBase` subclasses: no extra Started line. `@MockitoSpyBean` on a port
  and a plain class in the base `@Import` keep it so. A forked context (own `@TestPropertySource`,
  `@Import(PlainTestDouble.class)`, `@MockitoSpyBean` on a transactional flow) also works and costs one extra Started
  line. Drive the Spring AI tool loop from `ScriptedChatModel`; read the tool result from the received prompt's
  `ToolResponseMessage.responseData()` (raw JSON, parse with Jackson 3 `JsonMapper.readValue(json, TypeReference)`).
- The base cleans BEFORE each test: a `PER_CLASS` non-static `@BeforeAll` runs on the previous class's leftover rows, so
  its rows need keys (ISBN, id) no other test uses.
- Verifier greps (`assert|fail\(`, forbidden annotations) also match Javadoc and comments: keep those tokens out of prose.
- Order claims over a list with fixed first/last rows need at least four rows (three are always monotone). Columns no use
  case changes (reserved, reorder level): `jdbcTemplate.update(...)` with an `isEqualTo(1)` row-count check. For ids the
  app generates, seed `book`, `book_author`, `stock_item` by SQL with fixed ids (first hex digit 0 to 7) so ids are
  non-monotone too; set instants via `jdbcTemplate.update(..., OffsetDateTime.ofInstant(i, ZoneOffset.UTC), id)` so views
  compare with literal ISO strings. Fetch-joined roots (orders with lines): QA sorts by each child column asc and desc
  over the flat rows, root keeping first appearance; give each root its own books, put the smallest and largest child
  value of every column in the middle root, tabulate with a script first.
- MockMvc `jsonPath("$.details").value(List.of(...))` reports `but was:<null>`: use `.value(equalTo(List.of(Map.of(..))))`.
  Payload records: `List<@Valid @NotNull X>`, not `@Valid List<@NotNull X>` (HV000271 warning); paths stay `lines[1]`.
- QA greps `LocalDate(Time)?\.now\(` over `bookstore/src/test` (only the base's default-answer line allowed): autowire
  `Clock` and use `LocalDate.ofInstant(clock.instant(), clock.getZone())`. Stub the shared `@MockitoBean` default in the
  base `@BeforeEach` with `doAnswer(...).when(mock)`. Re-stubbing replaces it; for "fail first, then default" rebuild the
  answer locally (`doThrow(e).doAnswer(this::defaultConfirmation)`).
- `OutputCaptureExtension` on a full-context IT: `getOut()` includes context start-up and every EARLIER test of the
  class. Assert per unique token (fresh book id, literal only that test throws), never a bare
  `containsOnlyOnce(" WARN ")`. "Exactly one WARN naming X": `containsPattern(" WARN .*" + x)` plus
  `doesNotContainPattern("(?s) WARN [^\\n]*" + x + ".* WARN [^\\n]*" + x)`. Spring's after-commit listener failure logs
  `ERROR ... afterCompletion threw exception`, a BLANK line, then `java.lang.X: <message>`: join with `\\R+`.
- JDBC witnesses for nullable column groups: `queryForObject(sql, (rs, n) -> Arrays.<Object>asList(rs.getString(1),
  rs.getObject(2, Integer.class), ...), id)` gives nulls for SQL NULL, so "all cleared" is
  `isEqualTo(Arrays.asList(null, null, null, null))`. "Row unchanged after rejection": `queryForMap("select * ...")`
  before and after. Run every "unchanged" claim from each start of the flag it could flip
  (`@ValueSource(booleans = {true, false})`), else a reset-with-rollback-suppressed mutant survives (QA rule C0-17).
- "Never two threads inside" witness for a synchronized method: a test `Clock` whose `instant()` bumps an `AtomicInteger`,
  records the max, then `CountDownLatch.await(50, ms)` on a latch nobody opens; two threads released from one start latch.
- Bulk data for a result-cap test: 200 HTTP adds cost about 2.7 s and the add opens the stock item. Title prefix plus
  `%03d`; insertion order `(step * 73) % 200 + 1`; ISBN body `900000000 + (n * 137) % 997` plus computed check digit.
  Assert the ISBNs in title order are neither sorted nor reverse sorted.

Related: [[flow-test-techniques]], [[jpa-slice-traps]].
