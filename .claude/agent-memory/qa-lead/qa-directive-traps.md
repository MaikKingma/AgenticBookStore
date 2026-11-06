---
name: qa-directive-traps
description: Recurring traps when writing verifier directives in this repo's SDD cycle - pre-gate "clean tree" semantics, the path-reference ban vs .gitignore, probes failing for the wrong reason, ArchUnit vacuity guards, framework-warning ownership, hand-written frontmatter timestamps, scope arguments replacing global greps
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

5. **Framework warnings vs task sequencing.** The verifier rule "green with a framework warning = FAIL" collides with skeleton-first specs: the first task's referee prints warnings that later tasks fix. At strategy time, give every expected warning an owning acceptance criterion. Known ones on this stack: Mockito inline-mock self-attach on JDK 25 (dynamic-agent and CDS warnings, fixed by a Surefire `-javaagent` argLine), JPA open-in-view, Flyway "No migrations found". Also the Maven `prefixes.txt` 401 warnings, which come from repositories declared in the pinned flyway-parent POM and are not machine-local. Put a warning-inventory item in C0: every warning quoted and classified as fixable here (FAIL), owned by a named criterion, or unowned (flag it).
   **Why:** otherwise verifiers wave warnings through as "environmental" or "owned elsewhere", and no task ever fixes the unowned ones.
   The inventory grep must be `warn|deprecat|self-attach`. Mockito's line "Mockito is currently self-attaching ..." contains no "warn" and slips through a plain `warn` grep.

6. **Frontmatter timestamps are hand-written.** Agents' `updated`/`created` values have run minutes ahead of the real file write. Prove "the referee ran after the impl finished" with `stat` mtimes (impl artifact, last product file, Surefire XML), and direct verifiers to do the same.
   **Why:** the lens-2 ordering check and stale-agent detection both silently trust a value nobody took from the clock.

7. **Scope argument instead of a directive item.** Verifiers skip global greps as "untouched by this diff". Accept that only with a quoted scope proof plus my own cheap confirmation, and direct them to execute every global item literally.

**How to apply:** at strategy time put these into the common directive block. In reviews, CHALLENGE any probe or clean-tree claim that lacks the snapshot or the quoted reason line.
