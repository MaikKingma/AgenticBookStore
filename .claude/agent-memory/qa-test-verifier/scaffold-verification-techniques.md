---
name: scaffold-verification-techniques
description: Techniques for verifying generated scaffolding (start.spring.io modules, wrapper, POMs) and for snapshot-safe probes in the bookstore repo
metadata:
  type: feedback
---

Rule: prove provenance of generated files by regenerating, not by reading the impl's claims.

**Why:** a reindent (tabs to spaces) turns every POM/Java line into a diff hunk, which can hide a real change.
**How to apply:**
- Regenerate with the recorded curl into a fresh scratch dir, then `diff -r -x target gen/<mod> <mod>`, then again with `diff -r -w` excluding the deleted wrapper/metadata files (`-x .mvn -x mvnw -x mvnw.cmd -x HELP.md -x .gitignore -x .gitattributes`). The `-w` run leaves only structural hunks to map against the deviation list.
- Wrapper byte-equality: `shasum -a 256` root vs both generated copies.
- Give every "prints nothing" grep a control: run the tab grep (`grep -rlP "\t"`) against the generated tree (must hit), and the h2 path grep with a token that must match (e.g. `postgresql`) to prove the paths resolve.
- Single JUnit version: compare dependency:tree versions with `help:evaluate -Dexpression=junit-jupiter.version` (the Boot-managed value).
- BOM import: grep the effective POM for `org.springframework.ai` entries and tally versions (`| sort | uniq -c`); one version line means the import is in place.
- Probes: a snapshot script in `.claude/comms/tmp/` writes porcelain status, the sha256 of every task file (and probe paths, recorded as ABSENT) and a `find` of the src trees to scratch; `cmp` the before, after-restore and after-referee files.
- Rework rounds: record the file-scope sha256 in the scratchpad at the end of every round. Next round, identical production hashes let the old probe logs carry forward. To prove an uncommitted test file only gained a method, `cp` it to scratch, delete the new block with Edit, and `shasum`. If it equals the old hash, no existing assertion was weakened (a git diff is not possible on an untracked file). The same works for a pure edit (for example reordered input): reverse the edit in the copy and the hash must equal the old one.
- An ArchUnit engine spike counts only as Failures 1 / Errors 0 with an AssertionError raised from `ArchUnitTestDescriptor`, never as 0 tests. Use different rules from the impl's spike.

- Workflow YAML (no actionlint installed): write your own `ruby -ryaml` checker (Psych loads `on` as `true`; assert `on` values are `nil` for unfiltered triggers), then prove it can fail by running it on a scratchpad copy mutated with sed (add `branches:`, drop a flag). Check pinned action majors exist with `gh api repos/<owner>/<action>/releases/latest --jq .tag_name`.

Related: [[shell-traps]]
