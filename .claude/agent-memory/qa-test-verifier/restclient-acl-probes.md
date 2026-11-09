---
name: restclient-acl-probes
description: Probing a RestClient anti-corruption adapter (MockRestServiceServer contract tests, ApplicationContextRunner wiring) - element-level malformed bodies, Jackson float-to-int coercion, temporary QA test class, matrix index mapping
metadata:
  type: feedback
---

Rule: besides the planned mutants, feed the adapter element-level malformed 2xx bodies through a temporary QA-only
test class. Per-key "required" checks and a `catch (RestClientException)` do not cover them.

**Why:** a mutation list derived from the code only removes checks that exist. The defects sat where no check exists:
a `null` element in a list of nested records gave a raw NullPointerException from the public method, and Jackson 3
coerces a JSON float into a `Long`/`Integer` component by default (`ACCEPT_FLOAT_AS_INT`), so cents `9.99` silently
became price `0.09`. Neither is a RestClientException, so the adapter's catch never sees it.
**How to apply:**
- Witness list per payload: `[null]` element in every nested list, null value of every nested key the translation reads, a float in every integer component, a string in a number component, a top-level JSON array, a 3xx with a body.
- Run the float witness on EVERY integer component of EVERY response payload in round 0 (title cents and the restock answer's `quantity` alike); checking only the money field leaves the twin for a later round.
- When a Required Action asks for a null-element test, ask for the null AFTER a valid element (`[valid, null]`). A one-element `[null]` witness cannot tell `anyMatch` from `allMatch` or `getFirst() == null`; probe those three mutants on every new element check, and prove a survivor real by running the mutant against a `[valid, null]` body. Also probe the mirror `getLast() == null`; a first/last/middle position set kills all four.
- Exact numeric conversion (`BigDecimal.intValueExact()` / `toBigIntegerExact()` behind a catch): probe `intValue()`, `toBigInteger()` (truncation), `setScale(0, HALF_UP)` and `CEILING`, catch removed, reason literal changed. Witness an int-overflow value too: `intValueExact` throws there as well, so the WARN reason can mislead while the exception is right.
- Build the bodies with the test class's own `variant(file, map -> ...)` helper (package-private static), so they stay "a committed file with one change". `List.of(null)` throws; use `new ArrayList<>()` plus `add(null)`, or `Arrays.asList((Object) null)`.
- Install with `cp` into the test package, run `clean test -Dtest=<QaClass>`, copy `target/surefire-reports/TEST-<QaClass>.xml` to the scratchpad (testcase-level `<system-out>` maps each printed observation to its case; the next `clean` wipes it), then `rm` the class. Every later Maven run needs `clean`, or the removed class's bytecode still runs.
- Parameterized matrix over (method x failure): Surefire prints only `[n]`. Write the index map (row order x method count) into the verdict so each kill line reads as a named row.
- `javap -p` of nested classes in zsh: single-quote `'pkg.Outer$Inner'`, or `$Inner` expands to nothing and javap prints the outer class again.
- Two constructors (public for Spring, package-private for tests) need `@Autowired` on the public one; removing it gives "No default constructor found" in every ApplicationContextRunner test, which is the falsifier to quote.
- Identity-field-equals-request check (answer ean vs requested id): use the full op set and witness rows in [[env-switch-mutant-runner]] (identity-equality bullets). Killers confirmed here: trailing space kills `stripTrailing` and `startsWith`; an EAN with add-on (`<id>90000`) kills `startsWith` and `contains`; leading-space plus last-digit cases alone let both live (an implementer mutant note can be wrong, run it). "Check moved after X" mutants need a real reordering (hoist the later call into a local), not a text swap. `lines()` equals `split("\\R")` only for LF/CR/CRLF. After a lead rejects for surviving mutants, rerun the whole set (exec the old generator with a renamed prefix).
- Log-escaping helper (CR/LF/control chars before logging foreign text): probe CR-only, LF-only, CRLF, U+2028 and U+2029 kept, no ISO controls, hex case and width (`%04x`, `%X`; U+2028 prints the same at any width, only a leading-zero char like TAB or VT kills width), a missing call site per value, null-safety, a second WARN. The `Character.isISOControl` set boundary mutants (`c<0x20||c==0x85`, `c<=0x1E`, no U+007F, no NUL, 0x80/0x9F bounds) survive unless rows pin them; they cannot split a line under `\R`, so report them advisory. `(c<0x20||(c>=0x7F&&c<=0x9F))` is equivalent: keep it as the control that proves the survivor harness works.
- Caused-exception constructors: a mutant that puts the cause into `details`/message survives unless a test asserts `details()` is empty. Prove "problem body unchanged" with a temporary test that builds the ProblemDetail from the real ACL exception and from the cause-less constructor through both advice classes and compares JSON.
- Equivalent survivors worth naming, not failing: removing an explicit `contentType(APPLICATION_JSON)` on a POST (the JSON converter sets it), hard-coding a currency that a guard already restricts to one value.
Related: [[env-switch-mutant-runner]], [[readme-and-live-jar-probes]], [[shell-traps]]
