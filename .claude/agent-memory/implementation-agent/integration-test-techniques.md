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
- MockMvc `jsonPath("$.details").value(List.of(...))` converts the actual array to the expected value's class and then
  reports `but was:<null>`. Wrap it: `.value(equalTo(List.of(Map.of(...))))` (Hamcrest, no conversion).
- Payload records: `@Valid List<@NotNull X>` logs Hibernate Validator warning HV000271 (container-level `@Valid`
  deprecated). Write `List<@Valid @NotNull X>`; field paths stay `lines[1]` and `lines[1].bookId`.
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
- A forked full context off `IntegrationTestBase` (own `@TestPropertySource` plus `@Import(PlainTestDouble.class)` plus a
  `@MockitoSpyBean` on a transactional flow) just works: the subclass annotations add to the base's, the spy on the
  proxied flow keeps its transactions, and it costs one extra Started line. Drive the Spring AI tool loop from
  `ScriptedChatModel` and read the tool result from the received prompt's `ToolResponseMessage.responseData()` (raw
  JSON of the tool's return value, parse it with Jackson 3 `JsonMapper.readValue(json, TypeReference)`).
Related: [[flow-test-techniques]], [[jpa-slice-traps]].
