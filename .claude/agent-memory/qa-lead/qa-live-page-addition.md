---
name: qa-live-page-addition
description: Strategy traps when a spec adds a page to the live Docusaurus guide: default-dir built-rule collisions with CLI fixtures, route and pagination pins, page-count pin greps, one-letter sweep literals, minutes regexes, claim closure, branch-only pointers, comment openers in test literals, raw HTML code
applies-to:
  - guide/docs/**
  - guide/scripts/**
  - guide/docusaurus.config.ts
metadata:
  type: feedback
---

Complements [[qa-docs-checker-oracle]] and [[qa-docs-site-content]].

1. **A new built rule "on for the default build dir only" breaks the CLI fixture tests.** `navbar.test.mjs` and `level-progress.test.mjs` copy `scripts/` beside a tiny fixture build (no sitemap, fake routes) and run `check-built.mjs` with no argument expecting exit 0; a rule needing the real site's pages fails there. Before Gate 1 grep `withCopiedScripts` callers and put those test files in the task's fileScope (fixtures gain the pages; count pins become listed retargets), or require a second pinned predicate. Once fixtures derive from the rule's frozen list, the next list change MUST edit them: write them into that task's fileScope unconditionally.
2. **Routes and pagination pins need same-count and prefix witnesses.** Extra and missing locs leave a count-plus-subset comparator alive: add a replaced loc at the same count, a duplicate, a trailing-slash loc; for prev/next add a prev-side swap, an extra prev on the first page and an href with an extra path segment (`endsWith`). Prove the frozen list by importing it from the rule and diffing against a read-only live curl.
3. **Page-count pin greps:** `11`/`eleven` pins hide as `ELEVEN` constants, `skeletons.length, 11`, `pending skeleton pages: 11`, `11 error(s)`. Dry-run `git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])'` and write the NOT-a-pin list (manifest ids `XX-11`, `S11:` labels, `11. Risks`) into the ledger so the verifier classifies every hit.
6. **Claim closure:** the lead's page outline can state behaviour outside the researcher's verified quote set (permissions or hooks applying to subagents, usage counting). Diff every outline sentence against the claim ids at strategy time; each gap is a Gate 1 finding (quote it or drop it).
7. **Pointers to repo files "on your branch":** spec 09 strips `.claude/` from `main` and `level-0-start`, so an agent file exists only on `solution/level-3` and `start/level-4-*`. Check every pointer with `git grep -c <word> <ref> -- <path>` over each participant ref.
8. **Comment openers in test literals corrupt the zero-test count:** `rules/zero-tests.mjs` strips block comments textually, so a literal `'{/* unclosed'` in a test file hides every `test(` up to the next `*/` (`ok tests` fell 715 -> 711 while tests were added). Direct implementers to build `/*` and `*/` from parts and verifiers to compare the guard count with a plain `^[ \t]*(test|it)\s*\(` count per edited file.
9. **"Before the element" witnesses need one reason and adjacency:** a fixture helper that adds a blank line hides the line directly above (an `index < start - 1` mutant survives); an admonition witness WITH a body is killed by the body line, not the `:::` line. Name raw-text witnesses (no separator, empty admonition). A script-code ban scoped to spans and fences cannot be killed by the live run when the only matching prose sits in the plan: prose-negative witnesses must do it.
10. **Guards over "spans and fences" miss raw HTML code in MDX:** `<code>`, `<pre>` (any case, with attributes) render as code but are no Markdown span or fence; a guard frozen for the content task must read them (`HTML_CODE` of `lib/mdscan.mjs`) or the content task bans them by grep. `<kbd>`, `<samp>` and JSX code components stay for the rendered code-text cross-check. A key list tested by looping over the production table is a tautology: pin a typed second copy from the docs.
