---
name: restclient-acl-probes
description: RestClient anti-corruption adapters - element-level malformed bodies, float coercion, log escaping
metadata:
  type: feedback
---
Rule: besides the planned mutants, feed the adapter element-level malformed 2xx bodies through a temporary QA-only test class; per-key "required" checks and a `catch (RestClientException)` do not cover them.
**Why:** a mutation list derived from the code only removes checks that exist; the defects sat where no check exists: a `null` element in a list of nested records gave a raw NullPointerException from the public method, and Jackson 3 coerces a JSON float into a `Long`/`Integer` component by default (`ACCEPT_FLOAT_AS_INT`), so cents `9.99` silently became price `0.09`. Neither is a RestClientException.
**How to apply:**
- Witness list per payload: `[null]` element in every nested list, null value of every nested key the translation reads, a float in EVERY integer component of EVERY response payload in round 0 (title cents and the restock answer's `quantity` alike), a string in a number component, a top-level JSON array, a 3xx with a body.
- A null-element Required Action asks for the null AFTER a valid element (`[valid, null]`): a one-element `[null]` cannot tell `anyMatch` from `allMatch` or `getFirst() == null`. Probe those three plus `getLast() == null` on every new element check (first/last/middle positions kill all four).
- Exact numeric conversion (`intValueExact()` / `toBigIntegerExact()` behind a catch): probe `intValue()`, `toBigInteger()`, `setScale(0, HALF_UP)`, `CEILING`, catch removed, reason literal changed; witness an int-overflow value too (the WARN reason can mislead while the exception is right).
- Install with `cp` into the test package, `clean test -Dtest=<QaClass>`, copy `target/surefire-reports/TEST-<QaClass>.xml` to the scratchpad (testcase-level `<system-out>` maps observations to cases; the next `clean` wipes it), `rm` the class; every later run needs `clean`.
- Parameterized matrix over (method x failure): Surefire prints only `[n]`: write the index map into the verdict. `javap -p` of nested classes in zsh: single-quote `'pkg.Outer$Inner'`.
- Identity-field check (answer ean vs requested id): op set and witness rows in [[env-switch-mutant-runner]]. Killers confirmed: trailing space kills `stripTrailing` and `startsWith`; an EAN with add-on (`<id>90000`) kills `startsWith` and `contains`; leading-space plus last-digit cases alone let both live. "Check moved after X" needs a real reordering (hoist the later call into a local). `lines()` equals `split("\\R")` only for LF/CR/CRLF. After a lead rejects for surviving mutants, rerun the whole set.
- Characterization test over existing behaviour (contract test, production unchanged): the recorded red "missing contract file" proves only that the test reads the file. Judge by the mutant ladder plus one control: put the PRE-task test (`git show HEAD~1:<test>`) back, apply the filter-removed mutant, show it survives; then the new test is the sole killer. Data probes (relabel one role in the contract copy) prove the test reads that file, not an inline body. Role-filter siblings (EDITOR kept as author) not killed by TRANSLATOR/ILLUSTRATOR payloads: follow-up, not FAIL, when the criterion names only those roles.
Related: [[env-switch-mutant-runner]], [[readme-and-live-jar-probes]], [[shell-traps]]
