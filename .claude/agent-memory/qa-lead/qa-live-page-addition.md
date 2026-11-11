---
name: qa-live-page-addition
description: Strategy traps when a spec adds a page to the live Docusaurus guide (spec 16 bonus page): default-dir built-rule collisions, routes and pagination pins, page-count pin greps, one-letter literals in the sweep, minutes regexes, claim closure against the research set, pointers to files that only some participant branches carry; read before the strategy of any guide page addition
metadata:
  type: feedback
---

Complements [[qa-docs-checker-specs]] (checker-as-oracle traps). Found at spec 16 strategy time.

1. **A new built rule "on for the default build dir only" breaks the CLI fixture tests.** `navbar.test.mjs` and
   `level-progress.test.mjs` copy `scripts/` beside a tiny fixture build (no sitemap, fake routes) and run
   `check-built.mjs` with no argument expecting exit 0. Any rule that needs the real site's pages fails there.
   **How to apply:** before Gate 1 grep `withCopiedScripts` callers and either put those test files in the task's
   fileScope (fixtures gain the pages; count pins become listed retargets) or require a second, pinned predicate.
   Once those fixtures derive from the rule's frozen list, the next list change MUST edit them (an indexed lookup
   per route crashes on the new one, page counts move): write them into that task's fileScope unconditionally.
2. **Routes and pagination pins need same-count and prefix witnesses.** Extra and missing locs leave a
   count-plus-subset comparator alive: add a replaced loc at the same count, a duplicate, a trailing-slash loc; for
   prev/next add a prev-side swap, an extra prev on the first page and an href with an extra path segment
   (`endsWith`). Prove the frozen list by importing it from the rule and diffing against a read-only live curl.
3. **Page-count pin greps:** `11`/`eleven` pins hide as `ELEVEN` constants, `skeletons.length, 11`,
   `pending skeleton pages: 11`, `11 error(s)`. Dry-run `git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])'` and
   write the NOT-a-pin list (manifest ids `XX-11`, `S11:` labels, `11. Risks`) into the ledger so the verifier
   classifies every hit.
4. **One-letter literals are free in the plan-to-manifest sweep.** The sweep accepts a literal that is a substring of
   any entry snippet in its section, so a key literal such as `s` is "covered" by almost any snippet. Ask the content
   verifier to list literals under 4 characters and name the entry that holds each in its sentence context.
5. **"No minutes" regexes:** `\d+\s*(min|mins|minutes)\b` passes `1 minute`, `30-minute`, `an hour`, `ten minutes`.
   Write the witness list at strategy time and apply the rule to the plan section that mirrors the page too.
6. **Claim closure:** the lead's page outline can state behaviour outside the researcher's verified quote set
   (permissions or hooks applying to subagents, usage counting). Diff every outline sentence against the claim ids
   at strategy time; each gap is a Gate 1 finding (quote it or drop it).
7. **Pointers to repo files "on your branch":** spec 09 strips `.claude/` from `main` and `level-0-start`, so an
   agent file exists only on `solution/level-3` and `start/level-4-*`. Check every pointer with
   `git grep -c <word> <ref> -- <path>` over each participant ref before accepting the wording.
8. **A script-code ban scoped to spans and fences:** the only live prose that matches (`Break the agent (25 min)`)
   sits in the plan, not on a page, so the live run cannot kill a "whole text" mutant; prose-negative witnesses
   must do it.
9. **Comment openers in test literals corrupt the zero-test count.** `rules/zero-tests.mjs` strips block comments
   textually, so a literal `'{/* unclosed'` in a test file hides every `test(` up to the next `*/` (spec 16 T2:
   `ok tests` fell 715 -> 711 while tests were added). Direct implementers to build `/*` and `*/` from parts and
   verifiers to compare the guard count with a plain `^[ \t]*(test|it)\s*\(` count per edited file.
10. **"Before the element" witnesses need one reason and adjacency.** A fixture helper that adds a blank line hides
    the line directly above (an `index < start - 1` mutant survives); an admonition witness WITH a body is killed by
    the body line, not the `:::` line. Name raw-text witnesses (no separator, empty admonition) at strategy time.
