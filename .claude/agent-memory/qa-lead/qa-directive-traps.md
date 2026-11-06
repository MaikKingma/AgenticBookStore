---
name: qa-directive-traps
description: Recurring traps when writing verifier directives in this repo's SDD cycle - pre-gate "clean tree" semantics, the path-reference ban vs .gitignore, probes failing for the wrong reason, ArchUnit vacuity guards
metadata:
  type: feedback
---

Traps that recur across specs in this repo. Write them into every qaDirective that touches them.

1. **"Clean working tree" before the gate.** Nothing is committed before the QA gate, so the tree always holds the impl's uncommitted changes. Never ask for an empty `git status`. Ask for `git status --porcelain` plus `shasum -a 256` of the probe-touched files, identical before the first probe and after the last restore. Verifiers have read-only git: they restore with Edit or rm, never checkout or stash.
   **Why:** a literal "clean tree" criterion cannot be met, and a verifier ends up either failing wrongly or hand-waving it.

2. **Path-reference ban vs `.gitignore`.** The team rule "no file references docs/spec-pack, agent-os, .claude, CLAUDE.md" (spec 09 strips them from `main`) collides with the root `.gitignore`, which must keep its `.claude/...` ignore entries. Every grep gate needs that exemption spelled out.

3. **A probe must fail for the stated reason.** Make the verifier quote the failing assertion or exception line. Examples: a missing-migration entity without `@Id` fails as a mapping error, not as schema validation; a compile error is never a negative control.

4. **ArchUnit vacuity (allowEmptyShould everywhere).**
   - Every rule passes on empty packages, so the production run needs negative controls: a violating class in a real layer package, and a root-package class touching domain.
   - The self-test's production sanity check must read `@AnalyzeClasses` reflectively, because a hard-coded copy misses a package typo.
   - String-typed predicates (annotation FQN, package names) are silently vacuous when misspelled. Only a fixture-backed per-rule case catches that.

**How to apply:** at strategy time put these into the common directive block. In reviews, CHALLENGE any probe or clean-tree claim that lacks the snapshot or the quoted reason line.
