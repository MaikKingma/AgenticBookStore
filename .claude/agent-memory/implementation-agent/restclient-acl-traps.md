---
name: restclient-acl-traps
description: Traps and techniques for the bookstore acl adapter on Spring RestClient (Boot 4.1.2) - two constructors need @Autowired, path variable encoding, OutputCapture formats, loopback HttpServer timeout tests
metadata:
  type: reference
---
- A `@Component` with a public constructor for Spring and a package-private one for contract tests fails at startup with "No
  default constructor found": put `@Autowired` on the public one. Only an `ApplicationContextRunner` (or full context) test
  catches it; MockRestServiceServer tests never build the bean.
- `restClient.get().uri("/api/titles/{ean}", isbn)` on `RestClient.builder()` encodes a `/` in the value as `%2F`
  (`requestTo(".../978%2F0141439518")` matches). String concatenation keeps the slash.
- Error bodies: `e.getResponseBodyAs(Record.class)` inside `try/catch (RestClientException)` returns null for an empty body and
  throws for html or malformed JSON. Catch `RestClientResponseException` before `RestClientException`.
- Loopback timeout test: `HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)` with its own
  executor; `@AfterEach` `server.stop(0)` and `executor.shutdownNow()` so the sleeping handler is interrupted. Measure with
  `System.nanoTime()`.
- Jackson's default float-to-int coercion turns `9.99` into a `Long`/`Integer` component as 9, silently. Read EVERY integer-typed
  response component as `BigDecimal` (money: `toBigIntegerExact()`, counts: `intValueExact()`; ArithmeticException = unusable
  body) and give each one a float test in round 0, not only the price.
- Required-key checks stop at top-level keys; QA also feeds null list elements and null fields the translation reads
  (`contributors: [null]`, an AUTHOR without `name`): validate those too, or an NPE leaves the public method. Test the broken
  element alone, first, last and in the middle (parameterized): a one-element list cannot kill `anyMatch`->`allMatch`.
- Log-forging tests: split captured output with `split("\\R")` (Java's `\R` includes U+2028, U+2029, U+0085, VT); `String.lines()`
  does not. The Edit tool turns a typed U+2028/U+2029 escape into the raw char: grep for non-ASCII after every edit, fix by line
  with `perl -CSD`. Expected escaped text in source is `"\\u2028"`.
