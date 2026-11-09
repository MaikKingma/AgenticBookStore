---
name: seed-data-probes
description: Mutation probes for JSON/text seed files guarded by data tests (generator with a byte-identity check, CRLF must-pass, at-minimum must-pass), plus author-fact sourcing
metadata:
  type: feedback
---

Rule: build seed-data mutants with one `python3 -I` generator that loads the `.orig` copy, mutates the parsed objects and re-serialises in the file's own layout, and first asserts that re-serialising the UNMUTATED data is byte-identical to the original.

**Why:** without the identity assert, a formatting difference (spacing, key order, trailing newline) makes every data probe differ in many places, so a kill can come from layout instead of the intended value.
**How to apply:**
- One-object-per-line arrays: `"[\n" + ",\n".join(json.dumps(o) for o in items) + "\n]\n"` reproduced the file exactly; check yours with the assert before writing any probe.
- Non-ASCII probes need `ensure_ascii=False` and a UTF-8 write; the default escapes the character and the probe silently tests nothing.
- Count guards: probe each count at minimum minus one (must fail) AND all counts at exactly the minimum in one combined must-pass probe (kills `>` written for `>=`). Reduce counts by flipping extras to a neutral value that keeps every other invariant (IN_STOCK needs quantity above 0; removing a subject code must not empty the list or drop the last use of a labelled code).
- Line-ending neutrality: a must-pass probe with CRLF copies of every seed file; prove the mutant really holds `\r\n` with `od -c` before trusting the green run.
- Run the same runner (install mutant, `clean test -Dtest=...`, `cp -p` restore, hash check) as code probes: [[env-switch-mutant-runner]]. Add a CONTROL that installs the original bytes through the runner.
- Read the seed for analysis scripts from the `.orig` copies while probes run; the live file is a mutant half of the time.
- `Map.copyOf(...)` and `List.copyOf(...)` reject null lookups: `get(null)` / `contains(null)` throw NPE. An `Optional findBy(key)` over a copied map throws on a null key instead of returning empty; flag it for the callers that can pass null (tool parameters, optional query values).
Related: [[env-switch-mutant-runner]], [[shell-traps]]
