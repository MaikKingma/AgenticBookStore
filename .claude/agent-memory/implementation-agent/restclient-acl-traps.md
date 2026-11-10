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
- Log-forging tests: split captured output with `split("\\R")` (Java's `\R` includes U+2028, U+2029, U+0085, VT);
  `String.lines()` does not. Write U+2028/U+2029 in source as the escape, but the Edit tool turns a typed
  escape for those two into the raw char: `rg -n '[^\x00-\x7F]'` after every edit, fix by line with `perl -CSD`.
  Expected escaped text in source is `"\\u2028"` (the doubled backslash is not a unicode escape).
- "Additions only" on a `Stream.of(...)` source: appending after the last element rewrites its closing `)));`
  line (1 deletion in numstat). Insert new rows before the last element instead.
- Capturing a distributor title with curl: REST `GET /api/titles/{ean}` is the contract bytes as is (no trailing newline).
  MCP `/mcp` needs `Accept: application/json, text/event-stream`; the `tools/call` answer is SSE
  (`grep '^data:' | sed 's/^data://' | jq -j '.result.content[0].text'` gives the identical payload, `cmp` proves it).
  The session id comes from the `Mcp-Session-Id` response header of `initialize`. macOS grep has no `-P`.
- Missing-key variants: load the committed contract file into a `LinkedHashMap` with a Jackson 3
  `TypeReference` (a raw `Map.class` read gives an unchecked warning), change one key, write it back.
