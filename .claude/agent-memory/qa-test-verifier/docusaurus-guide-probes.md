---
name: docusaurus-guide-probes
description: Techniques and traps for QA of the Docusaurus guide site and its node:test check harness (guide/): one probe runner with byte-copy restore, config evaluation, template regeneration, anti-vacuity gaps
metadata:
  type: feedback
---

Rule: probe the Node harness with one fs-based runner script, not shell edits.

**Why:** a runner that edits in place, runs, records the exit code and restores from `cp -p` byte copies keeps probes honest and fast (a `npm run check` takes about 1 s, a Docusaurus build about 2 s here), and hash equality with the snap-before file proves the restore.
**How to apply:**
- Write the runner in `.claude/comms/tmp/` as an ASCII `.mjs` (build forbidden characters with `String.fromCodePoint`). Ops: write, append, replaceOnce with an exact-occurrence guard. After each probe restore touched files, delete created ones, and at the end compare all product hashes with the snapshot. Node `cpSync` does not preserve mtime: finish with `touch -r <orig copy> <file>`, because evidence-timing proofs rely on mtimes.
- Mutants run `node --test "scripts/**/*.test.mjs"` through `spawnSync` with the glob unexpanded; collect `✖` lines for the kill reason. Check for equivalent survivors (removing a `sort()` is unobservable on APFS).
- Evaluate configs: Node 26 type stripping cannot import `docusaurus.offline.config.ts` natively because of its extensionless `'./docusaurus.config'` import (`ERR_MODULE_NOT_FOUND`; `createRequire` does not help). Copy both to the scratchpad, symlink `node_modules`, rewrite that one import to `.ts`, import and flatten-diff.
- Anti-vacuity gaps in any `node --test` harness: a glob matching nothing exits 0 (`tests 0`), so deleting every test file keeps the check green; emptied files still count as passing. Also probe a literal-text rule against code blocks and inline code (false positives), and a NUL-byte file (a common binary skip bypasses the text rules). `npm` refuses every script when `package.json` is invalid JSON, so that probe never reaches the rule under test. Never run `npm --prefix guide exec -- node ...` (npm fetched node@27 alpha and ran 0 tests): run node from a script that `cd`s into guide/.
- Offline matcher trap: WebKit's pageerror `String(e)` is `XMLHttpRequest cannot load file: //...` with no `Error: ` prefix and a space after `file:`. Match `^(Error: )?XMLHttpRequest cannot load file:.* due to access control checks\.$`. Playwright Firefox `context.route` never sees `file://` requests, so "aborted 0" proves nothing alone: after measuring, `fetch('https://example.com/x')` must move the abort counter 0 -> 1 in every engine. The hash route `#/no-such-page` rendering `Page Not Found` proves the client router works from file://.

Rule: for a checker with pure rule modules, mutate the WIRING (`runChecks`, the site-rules glue), not only the rule functions.

**Why:** a rule module can be at 100 % line coverage with every unit mutant killed while the pipeline can still drop it: discarding a rule's errors in `check.mjs`, not printing its report lines, passing `darkMode` wrong all survived 136 green tests. Deleting an entry of a declarative table (required contrast pairs) survives too unless a test pins the table.
**How to apply:**
- Probe set per task: one mutant per `report(...)` call, per `errors:` spread, per `lines.push(...)`, plus "remove one entry of every pair/manifest table" (a generated per-line removal probe, plus a "lower the pin with the removal" control that must still fail by name; also mutate entries' fg/bg/kind). Each in its own `node --test` run; survivors become Required Actions with the failing assertion sketched. The runner's `mkdirSync` leaves an empty dir after the file is removed: `rmdir` it before the final gates.
- Run real-file probes in three layers (`npm run check`, `node --test`, `node scripts/check.mjs` separately): node:test fixtures that read the real CSS fail first and mask whether the checker itself catches the defect.
- Rendered checks: a hover read right after `locator.hover()` returns the pre-transition colour in light mode; wait ~450 ms before `getComputedStyle`. Probe pages: copy an `.mdx` into `guide/docs/`, build with `docusaurus build --out-dir <abs scratch dir>` (also with `--config docusaurus.offline.config.ts`), restore the tree, serve with `docusaurus serve --dir <dir> --port N`. WebKit `file://` woff2 responses have status 0, so assert `FontFace.status`, not response codes; negative control = delete one woff2 in a copy.

Rule: for a Markdown/MDX structure checker, probe the syntax variants the renderer accepts, then confirm survivors in the built HTML.

**Why:** a rule written for the common form misses equivalent forms that render the same. A column-0 `^#` H1 rule passed `   # Foo` and setext `Foo` / `===`, and Docusaurus then made "Foo" the page `<h1>` in place of the frontmatter title. A box parser that attached "any fenced block to the current label" accepted a bogus extra block, which rendered.
**How to apply:**
- Witness set: ATX with 1 to 3 leading spaces, setext `===`, four-colon `::::type Title`, a second fenced block per label, a fence under a label that should have none, text after a bare label, CRLF, the marker inside a fence. Check each survivor in a probe build before calling it a defect (`check:built` caught the four-colon form). Markdown H1 rules never see a JSX `<h1>` in MDX (a second H1 renders): route it to the rendered "exactly one H1" check.
- Declarative tables (anchor ids, header values): one removal mutant per entry. When the auto slug equals an explicit heading id (`## Stretch {/* #stretch */}`), removing the id keeps a link-probe build green; use an id whose slug differs.
- The offline config (hash router) renders no route at build time: a component that throws on bad props, a broken anchor and an absolute route link all pass `build:offline`; only the Pages build guards them. A Markdown file link (`./no-such-page.md`) fails both builds, so probe both link forms. Built HTML is minified and Prism splits code into token spans: grep for a word, not a command line.

Rule: on a re-verification, prove carry-forward by tree ids and extracted artifacts, not by zip digests.

**Why:** two CI runs over byte-identical inputs give the offline zip a different `digest` (zip timestamps).
**How to apply:** `git rev-parse <r1>:guide/docs` against `HEAD:guide/docs` (and src, static); `gh run download <id> -n <name> -D <fresh dir>` per run, `unzip` each into its own dir, `diff -rq`. Re-adapt mutant strings that the rework changed (a stale `from` string shows as SETUP ERROR, not as a kill).

Rule: for a rehype/remark plugin or built-HTML guard, probe through the real pipeline in a scratch copy, and keep controls structural.

**Why:** hast fixtures are hand-built, so they cannot show what MDX really emits: `<kbd>Ctrl</kbd>` (an `mdxJsxTextElement`, no `element` type) is invisible to a text walk over `type === 'element'` nodes. Unit mutants never see that.
**How to apply:**
- Scratch copy: `rsync -a --exclude node_modules --exclude build --exclude build-offline --exclude .docusaurus guide <dir>/`, symlink `node_modules`, drop in an extra docs page, build, read attributes back with `grep -o`. A probe image needs a remote URL.
- Negative controls as copies: delete the registration line (Pages and offline); for "offline inherits via spread" clone the presets in a copy of the offline config with the plugin list emptied. Run the same rendered script in modes real/neg/negoffline.

Rule: verify a pack-anchored coverage manifest with your own section-aware sweep, not the implementer's sweep test.

**Why:** a self-test "every pack literal is a substring of some snippet" is section- and page-blind: short literals (`/clear`, `claude`, `name`) count as covered by other pages' snippets, and "drop the entry + lower the page floor + unpin it" stays green.
**How to apply:**
- Report three buckets per literal: carried in its section, only elsewhere, nowhere; then a page-aware pass. Judge each leftover. Regex rules over prose: try the natural sentence where the token is allowed ("Never set ANTHROPIC_API_KEY" hit a `set\s+KEY` form) and exotic forms (`set -gx`, `setx /M`, `SetEnvironmentVariable`, YAML key).
- Comment-blanking and tag scanners: an unclosed opener inside inline code (`` `{/*` ``) blanks the rest of the page, so later Tabs vanish from tab and PowerShell rules; an inline `` `</TabItem>` `` inside a PowerShell tab ends the item for a regex tag walker (fail-open; inline `<Tabs>`/`</Tabs>` give errors, fail-closed). A rule moved to "code only" loses JSX code (`<code>`/`<pre>`): re-run old witnesses in JSX code. Confirm each with a probe build (page renders, checker green, control red).
- Span-scanner mutant families: closer of a longer run, carry reset on blank line, carried line without closer, carry length fixed to 1, escaped backtick, span across a blank line. Read `--experimental-test-coverage` uncovered lines first. Classify survivors by direction: fail-open (hide content from a rule) are required pins, fail-closed (false positives) recommended.
- A paragraph that only ends at blank or fence lines makes false spans across list items, ATX headings, table rows, JSX flow lines. Witnesses where CommonMark continues but a block-start list ends: a `2. ` line, an empty `*`/`+`/`1.` line, a line starting with inline JSX or `{expr}`, a `|` paragraph with no delimiter row, `- >` continued by `  >`; lone backticks on both sides of a comment or tag. Literal-tag forms MDX renders as text (`\</TabItem>`, `{'</TabItem>'}`) and `\\` before a backtick are missed spans. Span oracle without a build: `mdast-util-from-markdown` + `micromark-extension-gfm` + `mdast-util-gfm` in guide/node_modules (import by absolute path from a scratch script).

Rule: for a built-HTML tab reader (tab label -> panel lines), probe the Tabs props Docusaurus really emits differently.

**Why:** `<Tabs lazy>` renders only the selected panel, so a container has more tabs than panels. A fail-closed "count mismatch" error pinned only for more panels than tabs lets a `!==` -> `<` mutant survive; on a real lazy build it maps the shown PowerShell panel to tab 0 and passes.
**How to apply:**
- Probe page set in a scratch copy (one page per probe): label case/extra words, `values` prop, value-only item, `default`, `queryString`, `lazy`, nested Tabs, details, MDX component wrapper, table cell, admonition, `<pre><code>`, `<pre>{expr}`, `<br/>` in pre, showLineNumbers, title + `{1}`, magic comment, entities and real NBSP; run the source rule on the same text. The offline build writes only index.html (panel text lives in a JS chunk), so check:built on it is vacuous.
- Invisible-character label stripping: a closed code-point list misses 17 of 23 Default_Ignorable characters (U+200E/F, U+034F, U+2061-4, U+2066-9, tag chars); offer `/\p{Default_Ignorable_Code_Point}/gu`. Verify such a fix over all 0..0x10FFFF (skip surrogates) raw, as `&#x..;` and `&#..;`; narrowing mutants (`[\p{Cf}\p{Mn}]`) survive when the test list holds only Cf/Mn. NBSP, hair space and Cyrillic look-alikes are not DI: residuals.

Rule: for a content-page task, bind the pages to the frozen oracle with page mutants in a scratch copy plus a rendered cross-check.

**Why:** a green `npm run check` on real pages does not show that each rule binds those pages; only one exact edit per rule on the real page text does, and the kill line must name the rule.
**How to apply:**
- Scratch = `rsync guide` plus `docs/spec-pack`, symlinked node_modules; one runner applies a single `replaceOnce` per mutant, runs `node scripts/check.mjs` (about 1 s), prints the non-ok lines, restores. A reinserted skeleton marker exits 0 (pending only) until the marker ban exists: not a survivor. Break one contractual anchor in the copy: `npm run build` exits 1 with "broken anchors".
- Rendered cross-check imports `ENTRIES` and `packCommandLines` read-only. Tab panel n pairs with tab n; copy buttons must equal the `pre` count with hidden panels. A `pre`'s `textContent` has no newlines (join `.token-line`s); offline hash-router hrefs are `#/route#anchor`. Leakage rule ("no spec line longer than N chars"): also run an 8-word shingle overlap with an appended-line control that must hit; compare with backticks stripped and tags removed WITHOUT inserting a space. Docusaurus heading hash-links add one U+200B per heading, and it can garble a built meta description from an unchanged `**Goal:** ...: ...` line: report, not a leak.
- Dev mode: spawn `docusaurus start --port N --no-open --host localhost` from the script, drive the flow, kill the child, show `lsof` empty. `gh run view <id> --json jobs,headSha,event,conclusion > file` binds CI evidence to the head sha.
- Two-layer kill table: run each page mutant through the frozen `check.mjs` AND an own `semChecks(sourceText)` module (pack line equality of JSON/script blocks, whole-line fenced commands, exact checkpoint commands plus Expected lines, script block count, no PowerShell tokens in any fence, pinned prose). Checker survivors that the own layer kills are unguarded gaps: report as residuals for qa-final (the checker is frozen). Check that a "killed" mutant dies for the stated reason (a fence inserted mid-paragraph made the checker report a body H1 via `#!`: accidental). A pack quote inside a list item is indented by the item: remove exactly that indent. Before calling a link to a skeleton anchor "dangling", grep the skeleton page; use a made-up anchor as the build-failure control.

Rule: for a pack-line edit pinned by a node:test case, probe each pinned literal separately and prove the sweep exclusions bite both ways.

**Why:** a case that asserts several things in one `test()` stops at the first failure, so only a per-literal mutant shows each assertion is live; and `coverage-manifest.test.mjs` SWEEP_EXCLUDED keys are checked by two tests (exact set, no stale key).
**How to apply:** one runner with a `files` map (pack, page, manifest test) copy-asides all of them and restores in `finally` with `utimesSync`; probes = base line only (132-only, 347-only), one command removed from a table row, a banned phrase re-added, a page link made plain text, a sentence replaced. A/B the exclusion keys: keys removed with the fixed pack -> sweep test fails; keys kept with the pre-fix pack -> "no stale exclusion" fails. Full `node scripts/check.mjs` stays green on the pre-fix pack (the structure rule only needs the pack to exist), so the self-test, not `check`, is the witness.

Rule: when a recipe says "commands copied from the BUILT pages", read them from `guide/build` with a script and feed them to `bash -c`, never retype them.
**Why:** the built HTML is minified: unquoted attributes (`id=far-behind`, `href=/AgenticBookStore/...`), no `</p>`, and code lines are `<div class=token-line>` with Prism spans. A reader written for quoted attributes and closed `<p>` finds nothing and looks like "no such anchor".
**How to apply:** scan `<(p|pre|h2)\b` openers; a `p` ends at the next `</p>|<p|<div|<pre|<h[1-6]|<ul`; split a `pre` on `<(span|div)[^>]*class="?token-line"?`; decode entities last. Match ids as `id="?x[\s">]`. Blast-radius greps on a page-grammar bug: also cover `docs/spec-pack/specs/` (the spec states the grammar), not only guide/ and the workshop plan.

Rule: for a navbar/theme-wrapper task, run three witness layers and fix the rig's console classifier before trusting a red run.

**Why:** a first rig run showed 16 (Chromium) and 7096 (WebKit) "unexpected" console lines that were only the spec 11 FF-37 / FF-5 shapes: the rig truncated messages to 200 chars, so end-anchored regexes (`due to access control checks\.$`) and the CORS tail missed them. WebKit offline also logs the pair `Cross origin requests are only supported for HTTP.` plus the XHR pageerror once per chunk (thousands per run).
**How to apply:**
- Keep full text (700 chars) in the capture; classify by shape per engine, build and phase (offline + axe phase = FF-37, WebKit offline = FF-5 pair, `control` phase = the abort-control fetch); print the unexpected remainder and require 0. Re-run all three engines after a classifier edit.
- `perl -pi` swaps on built HTML must target the element, not the filename: the `<head>` holds preload links with the same file names, so a first-match swap changed the preloads and the real `check:built` stayed green (a false survivor). Anchor on `name alt=""`. Run real-build controls in place with a `cp -p` backup and hash proof (build/ is untracked): href, swapped imgs, title target, rel token.
- `node --test --experimental-test-coverage` reports a low `check-built.mjs` figure when its tests spawn a COPY of scripts/ (different path); say so and rely on wiring mutants (one per branch, each killed) instead of the number.
- Dev-mode walk: Docusaurus colour toggle cycles system -> light -> dark, so a "click once, theme differs" assertion fails in a light-scheme context; click until it differs. The React DevTools info line proves the dev build and the console hook are live (positive control).
- Dev-mode check is one `docusaurus start --port 344x` per task, engines can share it; `lsof` both ports free afterwards.

Rule: for a component task (component + stylesheet + built rule), split the mutants by who can see them and run both test commands.

**Why:** `npm run check` is `node --test` THEN `node scripts/check.mjs`, and check.mjs does not run the tests: a runner that only calls check.mjs reported 6 of 6 known-killed mutants as survivors (0 failing titles). Hand-written fixtures cannot see a TSX mutant: `<=` for `<` in the component, a check mark without aria-hidden or after the label, aria-current on the li passed every node:test; only a real build plus `check:built` killed them.
**How to apply:**
- Rule/pair/css mutants: in place, byte copies, `node --test` plus check.mjs. TSX mutants and the bad-prop guard: a scratch copy (`cpSync` with a filter minus node_modules/build/.docusaurus, symlink `node_modules`, symlink the repo `docs/` beside `guide/` because check:built reads the pack), `npm run build` then `check-built.mjs` per mutant, about 5 s each. Quote the `RangeError` line of the guard from the build log, not the exit code.
- A mutant inserted at a comment that sits INSIDE the dark block makes a nested `@media` the pin cannot see (false survivor): anchor on a top-level comment. Classify survivors: equivalent (fallback `?.` never reached, a count printed only on success), contract-neutral, or rendered-only (hover colour, sr-only text, first-chevron clip: no CI pin, rig only).
- Rig traps: `locator.ariaSnapshot()` prints no `aria-current=step`; assert accessible names with exact `getByRole('link', {name})`. A "just outside the focus ring" pixel probe must stay inside the wrapped row gap (8px; ring offset+width = 5px): a probe at 7.5px read the crumb above in Firefox only. Firefox dev console logs thousands of `timeStamp` events (React tracks), WebKit dev logs nothing, so the React DevTools info line is a positive control in Chromium and Firefox only.

Rule: for a palette or brand task, run mutants on a throwaway COPY of guide/ and bind the rendered sweep to an own resolver.

**Why:** the sweep reads custom.css once at start, so an in-place mutant that overlaps a later engine run poisons its numbers; a copy (tar minus node_modules/build, `docs` symlinked beside it) lets a 140-mutant batch and three engine sweeps run at once. Rule-list pins hide behind pair coverage: dropping `--ifm-color-emphasis-100/200` or one admonition type from a rule's list AND mutating the CSS survived, while the CSS mutant alone is killed.
**How to apply:**
- CSS mutants edit the `html[data-theme='dark']` block by offset (same declaration exists in `:root`); each primary must-fail needs its own named message, and a teal restore is killed ONLY by the brand rule (teal passes contrast). Generated families: every list entry dropped, with and without the matching CSS change; each new pair removed with and without lowering the count pin.
- A node:test mutant runner counting `✖` lines must skip the summary line `✖ failing tests:`, or every kill shows one title too many. A mutant that also trips an existing aggregate test (`moduleErrors` deepEqual) fails two titles: say so.
- Independent sweep: own census (every element that owns text, ancestors composited, ignore U+200B-only anchors whose opacity is 0 or they read 1:1), axe `violations` with targets, `incomplete` re-measured from computed styles, hover/active/TOC-active states. A spec exemption (one axe rule on named pages) is proven by running the SAME script axe-only on a `git archive` build of the base (own `serve` port) and diffing the sorted violation lines.
- Screenshots: re-shoot with the same viewport rules and compare pixel by pixel with a small zlib PNG decoder (8-bit RGB/RGBA); 0 differing pixels binds committed shots to the final CSS better than mtimes. Outline-band and fill pixels read from the screenshot, not from computed style.
- Insertion-only proof: `diff base now` hunks must be `a` plus the one listed `c`; apply the reverse and compare sha256 with `git show <base>:<file>`.

Related: [[shell-traps]], [[scaffold-verification-techniques]]
