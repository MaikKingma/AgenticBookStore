---
name: restclient-acl-traps
description: Traps and techniques for the bookstore acl adapter on Spring RestClient (Boot 4.1.2) - two constructors need @Autowired, path variable encoding, OutputCapture formats, loopback HttpServer timeout tests
metadata:
  type: reference
---

Framework facts behind these: res-3's `research-restclient-boot4.md` (spec 06 comms). Verified in spec 06 T3.

- A `@Component` with a public constructor for Spring and a package-private one for contract tests fails at
  startup with "No default constructor found". Put `@Autowired` on the public one. Only an
  `ApplicationContextRunner` (or full context) test catches it; MockRestServiceServer tests never build the bean.
- `restClient.get().uri("/api/titles/{ean}", isbn)` on `RestClient.builder()` encodes a `/` in the value as `%2F`
  (`requestTo(".../978%2F0141439518")` matches). String concatenation keeps the slash.
- Error bodies: parse with `e.getResponseBodyAs(Record.class)` inside `try/catch (RestClientException)`; it
  returns null for an empty body and throws for html or malformed JSON. Catch `RestClientResponseException`
  before `RestClientException`.
- OutputCaptureExtension: in a class-alone run logback prints `HH:mm:ss.SSS [main] WARN logger -- msg` (no DEBUG
  lines appeared); in the shared fork after a Boot context it prints Boot's pattern. `containsOnlyOnce(" WARN ")`
  works in both and in reverse class order.
- Loopback timeout test: `HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)` with
  its own executor; in `@AfterEach` call `server.stop(0)` and `executor.shutdownNow()` so the sleeping handler is
  interrupted instead of holding the test. Measure with `System.nanoTime()`.
- Jackson's default float-to-int coercion turns `9.99` into a `Long`/`Integer` component as 9, silently. Read EVERY
  integer-typed response component as `BigDecimal` (money: `toBigIntegerExact()`, counts: `intValueExact()`;
  ArithmeticException = unusable body) and give each one a float test in round 0, not only the price.
- Required-key checks stop at top-level keys; QA also feeds null list elements and null fields the translation reads
  (`contributors: [null]`, an AUTHOR without `name`). Validate those too, or an NPE leaves the public method. Test the
  broken element alone, first, last and in the middle (parameterized): a one-element list cannot kill
  `anyMatch`->`allMatch` or a getFirst/getLast-only check.
- Missing-key variants: load the committed contract file into a `LinkedHashMap` with a Jackson 3
  `TypeReference` (a raw `Map.class` read gives an unchecked warning), change one key, write it back.
