---
name: integration-test-techniques
description: RestTestClient and AssertJ techniques for *IntegrationTest classes (type-sensitive JSON asserts, exact key sets, probe-killing data, no unchecked warnings)
metadata:
  type: reference
---
- `RestTestClient` `jsonPath(..).isEqualTo(expected)` converts the actual value to the expected value's class, so
  `isEqualTo("9.99")` also passes for a JSON number. When the type matters, read the body with
  `expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})` and use AssertJ (String for JSON string, Double or
  Integer for numbers). MockMvc `jsonPath("$.details").value(List.of(...))` converts too and reports `but was:<null>`: wrap in
  `.value(equalTo(List.of(Map.of(...))))` (Hamcrest).
- Map bodies give exact view shapes: `assertThat(view).isEqualTo(Map.of(...))` checks key set and values together.
  `List.equals` works across list types. Null-valued key pins: wrap the `Map.of(...)` literal in a helper copying into a
  `LinkedHashMap` and putting the null; `Map.equals` treats a missing key as different from null, so `isEqualTo` is a presence
  check. In slices `jsonPath("$.k").value(nullValue())` fails on a missing path; `jsonPath("$", hasKey("k"))` pins presence.
- No raw-type unchecked notes: `extractingByKey("details", InstanceOfAssertFactories.LIST)` with
  `.singleElement(InstanceOfAssertFactories.MAP)`; `.extracting(d -> (Object) ((Map<?, ?>) d).get("field"))` (the `(Object)`
  cast lets `containsExactly(String...)` compile). Verifier greps `^\s*@` allow only `@Test`/`@Autowired` (no `@SafeVarargs`
  helpers: pass a `List`). Payload records: `@Valid List<@NotNull X>` logs HV000271: write `List<@Valid @NotNull X>`.
- Probe-breaking data: insert rows out of title order AND out of ISBN order (kills sort-by-ISBN). Every search filter needs its
  own decoy matching all other criteria but not this one; QA probes each criterion by passing null. Open-in-view is off, so a
  get/change after an add goes through lazy authors and kills a removed flow `@Transactional`.
- Order claims over a list: at least four rows (three with fixed "first/last" rows is always monotone). Columns no use case
  changes (reserved, reorder level) are set by `jdbcTemplate.update(...)` with an `isEqualTo(1)` row-count check. For
  app-generated ids seed `book`, `book_author`, `stock_item` by SQL with fixed ids (first hex digit 0 to 7) so ids can be
  non-monotone; set instants with `OffsetDateTime.ofInstant(i, ZoneOffset.UTC)` to compare literal ISO strings.
- The full context is cached across `IntegrationTestBase` subclasses (no new Started line); `@MockitoSpyBean` on a port and a
  plain class in the base `@Import` keep it so. A forked context (own `@TestPropertySource`, `@Import`, `@MockitoSpyBean` on a
  transactional flow) costs one extra Started line. Drive the Spring AI tool loop from `ScriptedChatModel` and read the tool
  result from the prompt's `ToolResponseMessage.responseData()` (raw JSON; Jackson 3 `JsonMapper.readValue(json, TypeReference)`).
- The base cleans BEFORE each test: a `PER_CLASS` subclass's non-static `@BeforeAll` runs on the previous class's leftover rows,
  so its rows need keys (ISBN, id) no other test uses.
- Verifier greps (`assert|fail\(`, forbidden annotations) also match Javadoc and comments: keep those tokens out of prose. QA greps
  `LocalDate(Time)?\.now\(` over `bookstore/src/test` (only the base's default-answer line is allowed): autowire the `Clock` and
  use `LocalDate.ofInstant(clock.instant(), clock.getZone())`. Stub the shared `@MockitoBean` default in the base `@BeforeEach`
  with `doAnswer(...).when(mock)`; re-stubbing replaces it, so for "fail first, then default" rebuild locally
  (`doThrow(e).doAnswer(this::defaultConfirmation)`).
- `OutputCaptureExtension` on a full-context IT: `getOut()` joins the class-level capture with start-up and EARLIER tests of
  the class. Assert per unique token (fresh book id, a literal only that test throws), never a bare
  `containsOnlyOnce(" WARN ")`. "Exactly one WARN naming X": `containsPattern(" WARN .*" + x)` plus
  `doesNotContainPattern("(?s) WARN [^\\n]*" + x + ".* WARN [^\\n]*" + x)`. An after-commit listener failure logs `ERROR ...
  afterCompletion threw exception`, a BLANK line, then `java.lang.X: <message>`: join with `\\R+`.
- JDBC witnesses for nullable column groups: `queryForObject(sql, (rs, n) -> Arrays.<Object>asList(rs.getString(1),
  rs.getObject(2, Integer.class), ...), id)` gives nulls for SQL NULL, so "all cleared" is `isEqualTo(Arrays.asList(null, ...))`.
  "Row unchanged after a rejected command": `queryForMap("select * ...")` before and after. Run every "unchanged" claim from each
  start of the flag it could flip (`@ValueSource(booleans = {true, false})`), else a reset-with-rollback-suppressed mutant
  survives (QA rule C0-17).
