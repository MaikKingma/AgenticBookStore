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

Related: [[flow-test-techniques]], [[jpa-slice-traps]].
