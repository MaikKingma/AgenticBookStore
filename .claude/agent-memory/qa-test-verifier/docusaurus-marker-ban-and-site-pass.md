---
name: docusaurus-marker-ban-and-site-pass
description: Techniques and traps for QA of a checker rule turned on for the live guide (skeleton marker ban) and for the site-wide rendered pass over a Docusaurus guide (offline zip, dev console, print, own render script)
metadata:
  type: feedback
---

Rule: probe a "turn this rule on for the real tree" change with a scratch copy, a mutant list over rule AND wiring, and a per-page reinsertion probe.

**Why:** the rule file can sit at 100 % coverage while the wiring (option name, CLI default-root switch, exit code, call site) is the part that silently drops it. 24 mutants (warning instead of error, errors discarded, option off or flag-gated, ban not called, drop first or last page, wrong root `scripts/` `src/` `guide/docs/` top-level only, predicate substring or startsWith or spacing-tolerant, no sort, message without page, md-only, mdx-only) all died here, so use that list as the minimum.
**How to apply:**
- Scratch = `rsync guide` (no node_modules, builds) plus `docs/spec-pack`, symlinked node_modules; one runner with exact-occurrence guard, `node --test` per mutant, real CLI exit recorded.
- Reinsertion probe: put the token before `## Level cleared when` (or after the frontmatter), never at the end of the file (an appended comment breaks level-page structure rules and every row then reads UNEXPECTED). Keep byte copies OUTSIDE `docs/` (a `.qaorig` beside a page trips the "exactly the eleven pages" rule); restore from the text held in memory.
- With a marker back, `npm run check` fails first in node:test (live-guide self-tests); the ban's own stderr line shows only from `node scripts/check.mjs` directly. Quote both and say which stage named the page.
- A positional root (`node check.mjs <root>`) skips an opt-in rule by design: note it as an observation, the npm script pins no root.

Rule: derive the rendered script from the previous one with an exact-replacement patcher, never hand-edit a 400-line file.

**Why:** a patcher that stops when any `from` string occurs other than once shows exactly what changed from the implementer's script, and keeps the independent run honest (stricter console buckets, controls, extra pages).
**How to apply:**
- Console buckets strictly per engine: Chromium offline axe triple only, WebKit offline FF-5 pair plus the axe preload warning, Firefox nothing. Run the "abort control" (a fetch to https://example.com) in its own phase tag: Chromium logs `net::ERR_FAILED` and Firefox a CORS error for it, which would otherwise read as an unexpected offline error.
- Built meta tags are minified and unquoted (`name=description content="..."`): the regex needs both forms.
- CI zip: `gh run download <run id> -n workshop-guide-offline -D <fresh dir>` (gh lives in /opt/homebrew/bin, not on the pinned PATH; lsof in /usr/sbin), unzip, `diff -rq` against an unzip of your own `build-offline` zip: identical trees prove the CI artifact is the local build. Extract the CI copy into a path with spaces for the file:// matrix.
- Dev-mode driver traps: a collapsed sidebar category needs a click on its label before the link is visible; read `h1` and `meta[name=description]` only after waiting for the new page (they race and show the previous page); copy buttons inside hidden tab panels are not visible, click only the visible ones; assert counts after `waitFor`.
- Print: Playwright `page.pdf({format:'A4'})` page count = `/Type /Page[^s]` matches (equals `/Count`); hide the last section by `display:none` from its h2 to the end for the "second page only for X" proof; also measure at 10 mm margins, the count can differ from the margin-0 result (report as an observation when the card's check is margin-0).
- Must-fail mutants for a rendered-only assertion (a Checkpoint retitled, one added to a page that has none) pass the frozen checker and both builds; show the exit 0 of the checker and the failing rendered line in the same log.

Related: [[docusaurus-guide-probes]], [[shell-traps]]
