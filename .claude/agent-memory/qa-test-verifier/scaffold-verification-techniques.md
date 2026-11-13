---
name: scaffold-verification-techniques
description: Generated scaffolding and static .http files - regenerate for provenance, snapshot-safe probes, rework carry-forward
metadata:
  type: feedback
---
Rule: prove provenance of generated files by regenerating, not by reading the implementer's claims.
**Why:** a reindent (tabs to spaces) turns every POM/Java line into a diff hunk, which can hide a real change.
**How to apply:**
- Regenerate with the recorded curl into a fresh scratch dir, `diff -r -x target gen/<mod> <mod>`, then `diff -r -w` excluding the deleted wrapper/metadata files (`-x .mvn -x mvnw -x mvnw.cmd -x HELP.md -x .gitignore -x .gitattributes`); the `-w` run leaves structural hunks to map against the deviation list. Wrapper byte-equality: `shasum -a 256` root vs both generated copies.
- Give every "prints nothing" grep a control (the tab grep `grep -rlP "\t"` on the generated tree must hit; the h2 path grep with a token that must match, e.g. `postgresql`).
- Probes: a snapshot script in `.claude/comms/tmp/` writes porcelain status, the sha256 of every task file (probe paths as ABSENT) and a `find` of the src trees; `cmp` the before, after-restore and after-referee files.
- Rework rounds: record the file-scope sha256 at the end of every round; identical production hashes let old probe logs carry forward. A changed killing test file voids carry-forward: re-run the whole mutant index with a copy of the runner under the new round's log prefix. Reuse old mutant files only when production is hash-equal and live = `.orig` = both snaps. Two shifts: (1) a rework calling a handler directly turns a "method removed" mutant into a compile error (not a kill): add an annotation-only variant; (2) racy probes (removing `synchronized` or a sort) can flip from killed to surviving with byte-identical tests: report each round's per-run tally and apply the pre-declared three-run rule again.
- Prove an uncommitted test file only gained a method: `cp` to scratch, delete the new block, `shasum`; for a pure insertion `sed '<a>,<b>d' <file> | shasum -a 256` (include the blank separator). Equal to the old hash means no assertion was weakened. Several hunks: a `python3 -I` script runs `diff old new`, accepts only `NaM[,K]` headers (any `c`/`d` header or `<` line means not insertion-only), drops those lines and hashes the rest. A pure edit works the same: reverse it in the copy, the hash must equal the old one.
- Static request collections (`http/*.http`): one `python3 -I` script checks everything from the file's own bytes (bodies cut with `sed -n 'a,b p'`, key sets vs the payload records read from the Java source via regex on `public record X(...)` with annotations stripped and top-level commas split, JSON types, ISBN-13 checksum with an invalid-digit control, difference from the sibling collection); compare handler and header lines to the sibling by `sed -n 'Np' | shasum`. Combination inventory: grep the integration tests for each literal to find input combinations no HTTP test exercises. Look up test line numbers with `grep -n` (counting by eye drifts by one).
Related: [[http-collection-replay-probes]], [[shell-traps]]
