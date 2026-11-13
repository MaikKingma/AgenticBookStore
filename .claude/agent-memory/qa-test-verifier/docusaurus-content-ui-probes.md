---
name: docusaurus-content-ui-probes
description: guide/ content pages vs the pack oracle, plus component, CSS, palette and focus-ring probes and rigs
applies-to:
  - guide/docs/**
  - guide/src/**
  - guide/docusaurus.config.ts
metadata:
  type: feedback
---
Rule: for content-page tasks bind pages to the frozen oracle with page mutants in a scratch copy plus a rendered cross-check.
**Why:** a green `npm run check` does not show each rule binds the real pages; only one exact edit per rule on the real text does, and the kill line must name the rule.
**How to apply:**
- Scratch = `rsync guide` plus `docs/spec-pack`, symlinked node_modules; one `replaceOnce` per mutant, `node scripts/check.mjs`, restore. A reinserted skeleton marker exits 0 until a marker ban exists; a broken contractual anchor makes `npm run build` exit 1 "broken anchors".
- Own `semChecks(sourceText)` layer (pack-line equality, whole-line fenced commands, checkpoint lines, no PowerShell tokens in any fence): checker survivors it kills are residuals for qa-final. Confirm a kill is for the stated reason.
- Pack coverage manifests: own section-aware sweep (three buckets per literal: in its section, only elsewhere, nowhere); an implementer's substring self-test is section- and page-blind. One mutant per pinned literal in a `test()` asserting several.
- Rendered cross-check: tab panel n pairs with tab n; `pre.textContent` has no newlines (join `.token-line`s); offline hrefs are `#/route#anchor`. Leakage rule: 8-word shingle overlap with an appended-line control. A command participants must run: re-fetch the official page (`curl -sL <url>.md`) and quote the line.
- Page with a plan mirror and literal floor: own oracle via `mdast-util-from-markdown` + gfm for spans and fenced lines on the page AND the plan section; set-diff both ways plus "every page literal sits in some manifest snippet". Sensitivity control: point the rig's source dir at a scratch guide (mutated page) while the browser renders the real build; the literal check must FAIL.
- Carry-forward: `git rev-parse <r1>:guide/docs` vs `HEAD:guide/docs`; CI zips differ by timestamp, so `gh run download` each, unzip, `diff -rq`; `gh run view <id> --json jobs,headSha,event,conclusion` binds CI evidence to the head sha.
- Dev-mode SPA walk: after a click wait for the h1 text to change (the read right after `waitForURL` returns the old h1); pagination link names are glued ("PreviousBonus level"): use `a.pagination-nav__link--prev`.

Rule: for component, navbar, palette, focus-ring and table-region tasks, split mutants by who can see them and give every CSS or keyboard claim a negative control.
**Why:** `colors.mjs` exempts `custom.css`, so 6 of 6 CSS mutants passed `npm run check`; TSX mutants (`<=` for `<`, aria-hidden dropped) pass hand-written fixtures and die only in a real build plus `check:built`; the palette sweep reads custom.css once, so an in-place mutant poisons concurrent runs.
**How to apply:**
- Rule/CSS mutants in place; TSX mutants and palette batches in a scratch copy (tar minus node_modules/build, `docs` symlinked beside `guide/`), `npm run build` then `check-built.mjs`, about 5 s each. A mutant inserted at a comment inside the dark block makes a nested `@media`: anchor on a top-level comment. Classify survivors: equivalent, contract-neutral, rendered-only (rig only).
- Rig console classifier: keep 700 chars of message (end-anchored regexes miss a 200-char cut); classify by engine, build and phase. Offline: Chromium logs the axe triple only, WebKit the CORS pair plus XHR pageerror per chunk, Firefox nothing (its CORS error comes late: classify the abort-control fetch by URL). Firefox dev logs thousands of `timeStamp` events; the React DevTools info line is the positive control in Chromium and Firefox only. Dev mode: one `docusaurus start --port N --no-open --host localhost` per task, `lsof` free afterwards. The colour toggle cycles system -> light -> dark.
- WebKit swallows the first key after `page.screenshot()` or Tab: ArrowRight check before any screenshot, 350 ms after Tab, `scrollLeft = 0` plus 1500 ms before ring shots; never allow "one repeat". `<details>` animates (~900 ms between presses); hover reads need ~450 ms. Firefox ignores `scroll-margin` for visible elements. Sample ring pixels from a decoded screenshot PNG, not computed style. A cell-rect compare vs a base build needs a sensitivity control (`padding-top:30px` moved every cell; `margin-bottom` collapsing moved none).
Related: [[docusaurus-guide-probes]], [[docusaurus-markdown-html-probes]], [[live-ui-walk-verification]]
