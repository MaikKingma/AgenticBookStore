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
- Provenance: `npx --yes create-docusaurus@3.10.2 <scratch dir> classic --typescript --skip-install --package-manager npm` works non-interactively; diff `package.json`, `custom.css`, `tsconfig.json` against the guide.
- Prove a flag is load-bearing with a negative control: `future.v4: false` makes the space-form `:::info Title` render as a real admonition, `true` leaves a literal `<p>:::`.
- Anti-vacuity gaps to look for in any `node --test` harness: `node --test` with a glob matching nothing exits 0 (`tests 0`), so deleting every test file keeps the check green; emptied files still count as passing "tests". Also probe a literal-text rule against code blocks and inline code (false positives), and a NUL-byte file (a common binary skip bypasses the text rules).
- `npm` refuses every script when `package.json` is invalid JSON, so an invalid-JSON probe never reaches the rule under test.
- Render: Firefox in dev mode floods `console.timeStamp` messages (type info, React profiling marks); filter on type error and warning. WebKit on `file://` logs XHR prefetch errors per chunk (documented upstream, res-1 finding 9 in the spec's research folder), not an app diagnostic. Pages mode: `npx docusaurus serve --port 33NN --no-open` from `guide/` in a script, kill server PID and the listener PID, show `lsof` empty.
- Offline matcher trap: WebKit's pageerror `String(e)` is `XMLHttpRequest cannot load file: //...` with no `Error: ` prefix and a space after `file:`. Match `^(Error: )?XMLHttpRequest cannot load file:.* due to access control checks\.$`. Playwright Firefox `context.route` never sees `file://` requests, so "aborted 0" proves nothing alone: after measuring, `fetch('https://example.com/x')` must move the abort counter 0 -> 1 in every engine. The hash route `#/no-such-page` rendering `Page Not Found` proves the client router works from file://.
- Bash tool guard: a one-line `for ... tail -c1 ...` over repo files was refused as a git-like compound; put loops in a script file.

Rule: for a checker with pure rule modules, mutate the WIRING (`runChecks`, the site-rules glue), not only the rule functions.

**Why:** a rule module can be at 100 % line coverage with every unit mutant killed while the pipeline can still drop it: discarding a rule's errors in `check.mjs`, not printing its report lines, passing `darkMode` wrong all survived 136 green tests. Deleting an entry of a declarative table (required contrast pairs) survives too unless a test pins the table.
**How to apply:**
- Probe set per task: one mutant per `report(...)` call, per `errors:` spread, per `lines.push(...)`, plus "remove one entry of every pair/manifest table". Each in its own `node --test` run; survivors become Required Actions with the failing assertion sketched.
- Run real-file probes in three layers (`npm run check`, `node --test`, `node scripts/check.mjs` separately): node:test fixtures that read the real CSS fail first and mask whether the checker itself catches the defect.
- `node --test --experimental-test-coverage` gives line/branch per file without a dependency; quote it but never use it as the verdict.
- Rendered checks: a hover read right after `locator.hover()` returns the pre-transition colour (transparent) in light mode; wait ~450 ms before `getComputedStyle`. Probe pages: copy an `.mdx` into `guide/docs/`, build with `docusaurus build --out-dir <abs scratch dir>` (absolute path works, also with `--config docusaurus.offline.config.ts`), restore the tree, serve with `docusaurus serve --dir <dir> --port N`. WebKit `file://` woff2 responses have status 0, so assert `FontFace.status`, not response codes. A negative control = delete one woff2 in a copy, expect that face `error`.
- Audit text colours by walking text nodes and compositing the effective background (alpha over ancestors); it catches what a variable-level checker cannot (Infima overrides, hover overlays).
- Third-party font provenance: fetch the README URLs into a fresh dir and compare sha256 with the "source" column; no tool needed to prove upstream integrity.

Rule: re-verify a pinned table with generated per-line removal probes plus a "lower the pin with the removal" control.

**Why:** a count pin alone is satisfied by swapping or editing an entry; a name pin alone misses entries nobody listed. A probes file that reads the table source and emits one remove-this-line probe per entry (53 probes ran in 8 s) shows both layers; the control (remove an entry and lower the minimum in the test) must still fail through the by-name test.
**How to apply:** also mutate entries' fg/bg/kind (not only remove them): non-pinned entries survive content edits, report that as an observation. After a probe run `diff -r` against the snapshot can show `Only in ...: components`: the runner's `mkdirSync` leaves an empty dir after the file is removed, `rmdir` it, re-check, then run the final gates.
- Regex block-comment stripping (`/\/\*[\s\S]*?\*\//`) over a test file undercounts when a string literal holds `/*` (a title `scripts/**` swallows tests up to the next `*/`): compare the guard's count with a naive line count per file. Direction is safe (never a vacuous pass) but the printed "N test cases" is only a lower bound.

Rule: for a Markdown/MDX structure checker, probe the syntax variants the renderer accepts, then confirm survivors in the built HTML.

**Why:** a rule written for the common form misses equivalent forms that render the same. A column-0 `^#` H1 rule passed `   # Foo` and setext `Foo` / `===`, and Docusaurus then made "Foo" the page `<h1>` in place of the frontmatter title. A box parser that attached "any fenced block to the current label" accepted a bogus extra catch-up block, which rendered.
**How to apply:**
- Witness set: ATX with 1 to 3 leading spaces, setext `===`, four-colon `::::type Title`, a second fenced block per label, a fence under a label that should have none, text after a label that should be bare, CRLF, the marker inside a fence. Check each survivor in a probe build (`docusaurus build --out-dir <SP dir>`) before calling it a defect: `check:built` caught the four-colon form, so that one was covered by the gate chain.
- Declarative tables (anchor ids, header values): one removal mutant per entry. The value edits died against an independent fixture copy, but the anchor-id removals mostly survived, because fixtures that contain every anchor never show a missing requirement.
- Explicit heading ids: when the auto slug equals the id (`## Stretch {/* #stretch */}`), removing the id keeps a link-probe build green. A build-level "remove one anchor" probe needs an id whose slug differs (`I am far behind` -> `#far-behind`).
- The offline config (hash router) renders no route at build time. A component that throws on bad props, and a broken anchor, both pass `build:offline`. Only the Pages build guards them.
- Built HTML is minified (`aria-current=step` without quotes), and Prism splits code into token spans, so grep for a word, not a whole command line.
- Never run `npm --prefix guide exec -- node ...`: npm resolves `node` as a registry package (it fetched node@27 alpha) and runs 0 tests. Run node from a script that `cd`s into guide/.
- Summaries: `grep ... | paste - -` pairs each result with the NEXT probe header. Have the runner print `id exit` on one line instead.
- On `file://`, axe injection triggers Docusaurus preload console errors in Chromium and WebKit. Tag console messages with the current step and replay fresh loads and a real hover and click before blaming the app.
- Markdown H1 rules (ATX plus setext) never see a JSX `<h1>` in MDX: it passes and renders a second H1 next to the title. Probe it and route it to the rendered "exactly one H1" check.
- Mutants for a reworked "one fenced block per box line" rule: first-block-wins vs last-block-wins is equivalent while a count check exists; also try a label-specific exemption per label (tests often cover 3 of 4 labels) and a quantifier tightening (`=+` to `={3,}`, tests rarely use a single `=`). A `!flags[index - 1]` mutant on a setext rule is equivalent: a flagged `===` line always has a flagged predecessor.

Rule: on a re-verification, prove carry-forward by tree ids and extracted artifacts, not by zip digests.

**Why:** two CI runs over byte-identical inputs give the offline zip a different `digest` (zip timestamps), so the digest alone suggests a change that is not there.
**How to apply:** `git rev-parse <r1>:guide/docs` against `HEAD:guide/docs` (and src, static) for rendered inputs; `gh run download <id> -n <name> -D <fresh dir>` for each run, `unzip` each into its own fresh dir, then `diff -rq` the two trees. Re-adapt mutant strings that the rework changed before re-running a mutant file (a stale `from` string shows as SETUP ERROR, not as a kill).

Rule: for a rehype/remark plugin or built-HTML guard, probe through the real pipeline in a scratch copy, and keep controls structural.

**Why:** hast fixtures are hand-built, so they cannot show what MDX really emits. A probe page showed `<kbd>Ctrl</kbd>` (an `mdxJsxTextElement`, no `element` type) is invisible to a text walk over `type === 'element'` nodes, while a nested ordered list and a loose item were handled. Unit mutants never see that.
**How to apply:**
- Scratch copy: `rsync -a --exclude node_modules --exclude build --exclude build-offline --exclude .docusaurus guide <dir>/`, symlink `node_modules`, drop in an extra docs page, build with `docusaurus build`, read attributes back with `grep -o`. A probe image needs a remote URL (a missing local file fails the build).
- Negative controls as copies: delete the registration line (Pages and offline), and for "offline inherits via spread" clone the presets in a copy of the offline config with the plugin list emptied. Run the same rendered script in modes real/neg/negoffline so the violations appear only where intended.
- Console: tag messages by phase (load, DOM reads, axe). axe on `file://` makes Chromium and WebKit log XHR/CORS errors in the axe phase only, and the WebKit offline XHR pair at load (about 200 per page) is identical in the no-plugin control, so compare with the control, not with zero.
- Equivalent survivors seen for a text-walk plugin: `find(isCheckbox)` vs first child, and counting a closing `</input>` tag. A real survivor class: branches only reachable by `ol` or by an unreferenced default path (`check-built` default dir).
- Entity-encoded blank names (`aria-label="&#32;"`) pass a scanner that does not decode entities; report as a documented limit, not a defect, when the card's wording is "non-empty".

Rule: verify a pack-anchored coverage manifest with your own section-aware sweep, not the implementer's sweep test.

**Why:** a self-test that checks "every pack literal is a substring of some snippet" is section- and page-blind: short literals (`/clear`, `claude`, `name`) count as covered by snippets of other pages, and "drop the entry + lower the page floor + unpin it" stays green. An own sweep (fence tracker, span extractor, heading spans, same source, same section, same page) found real missing entries.
**How to apply:**
- Report three buckets per literal: carried in its section, only elsewhere, nowhere; then a page-aware pass (in-section literal carried only by cheat-sheet entries). Judge each leftover.
- Comment-blanking scanners: an unclosed opener inside inline code (`` `{/*` ``) blanks the rest of the page, so later Tabs vanish from tab and PowerShell rules. Confirm with a probe build (page renders, checker green, control red).
- Regex rules over prose: try the natural sentence on the page where the token is allowed ("Never set ANTHROPIC_API_KEY" hit a `set\s+KEY` form). Also try exotic forms (`set -gx`, `setx /M`, `SetEnvironmentVariable`, YAML key).
- A mutant whose `tests N` drops below the baseline (266 of 391) broke a module's syntax: invalid evidence, redo it. Survivors often expose a mis-asserted test (`/not in/` also matched another rule's message).
- In a checker whose ok-detail prints only without errors, "anchored N/N" can never show N-1: a mutant on that count is equivalent.
- After a comment scanner learns inline code, probe the TAG scanner the same way: an inline `` `</TabItem>` `` inside a PowerShell tab ends the item for a regex tag walker, so a following bash fence escapes the PowerShell-line rule while the build renders it in that tab. Only the closing-item form is fail-open; inline `<Tabs>`/`</Tabs>` give errors (fail-closed).
- A rule moved to "code only" (fenced lines + inline spans) loses JSX code: `<code>`/`<pre>` forms that the whole-text version caught now pass. Re-run the old witnesses in JSX code after such a rework.
- Sweeps that strip `# comment` from fence lines never see comment text: entries sourced from a fence comment (`/status`, `/model sonnet`) are guarded only by floors and pins (drop + lower floor survives).
- Span-scanner mutant families: closer of a longer run, carry reset on blank line, carried line without closer (needs a span over 3 lines), carry length fixed to 1 (double-backtick span over a line break), escaped backtick, span across a blank line. Read `--experimental-test-coverage` uncovered lines first: they name the unexecuted branch. Re-scanning `rest` after a split often makes the longer-run closer mutant equivalent.
- Classify survivors by direction: fail-open mutants (hide content from a rule) are required pins; fail-closed ones (false positives) are recommended.
- A span scanner whose "paragraph" only ends at blank or fence lines makes false spans across lines where MDX starts a new block: list items, ATX headings, table rows, JSX flow lines (`</TabItem>`). Witness: lone backticks on both sides of a comment or a tag. A rule that moves from whole text to "markup minus spans" inherits this as fail-open (a real `<img>` between two list items passed); confirm with a build.
- Literal-tag forms MDX renders as text: `\</TabItem>`, `{'</TabItem>'}`; missed spans: `\\` before a backtick (escaped backslash, the span still opens), a ``` line with a backtick in its info string inside a span. Probe every tag reader with them.
- Docusaurus copies the raw first paragraph, comment text included, into `<meta name=description>`: grep the article body, not the whole HTML, before saying hidden text renders.
- "Ending a paragraph too early is fail-closed" is false for a tag reader and a comment blanker: a missed span that holds `</TabItem>` ends the item, one that holds an unclosed `{/*` blanks the rest. Witnesses where CommonMark continues but a block-start list ends: a `2. ` line, an empty `*`/`+`/`1.` line, a line starting with inline JSX or `{expr}` then text, a `|` paragraph with no delimiter row, `- >` continued by `  >`. Build each; a silent unmatched `</TabItem>` makes them pass.
- Span oracle without a build: `mdast-util-from-markdown` + `micromark-extension-gfm` + `mdast-util-gfm` in guide/node_modules give CommonMark inlineCode nodes; compare collapsed values with the scanner's spans per file (import by absolute path from a scratch script).

Rule: for a built-HTML tab reader (tab label -> panel lines), probe the Tabs props Docusaurus really emits differently.

**Why:** `<Tabs lazy>` renders only the selected panel (theme-classic TabItem returns null), so a container has more tabs than panels. A fail-closed "count mismatch" error that tests pin only for more panels than tabs lets a `!==` -> `<` mutant survive, and on a real lazy build it maps the shown PowerShell panel to tab 0 and passes.
**How to apply:**
- Probe page set in a scratch copy (one page per probe, `docusaurus build --out-dir`), checked by the repo's real CLI: label case/extra words, `values` prop, value-only item, `default`, `queryString`, `lazy` (selected and not), nested Tabs, details, MDX component wrapper, table cell, admonition, `<pre><code>`, `<pre>{expr}`, `<br/>` in pre, showLineNumbers, title + `{1}`, magic comment, entities and real NBSP. Run the source rule on the same text to see which escape both.
- Combined escapes: prefix a probe with an A7-style unclosed `{/*` span opener; then a `powershell` fence outside tabs, a soft-hyphen label and a `token-line` span plus stray text in a raw pre all pass both rules.
- Fixture provenance: CSS-module class suffixes (`tabList_xxxx`) depend on the build path, so byte equality fails; compare articles with `name_[A-Za-z0-9_-]{4}` normalised.
- The offline (hash router) build writes only index.html; panel text lives in a JS chunk, so check:built on it is vacuous by construction.
- Invisible-character label stripping: a closed code-point list (U+00AD, U+200B-D, U+2060, U+FEFF) misses 17 of 23 Default_Ignorable characters (U+200E/F bidi marks, U+034F, U+2061-4, U+2066-9, tag chars). Enumerate them at unit level, confirm one with a real build (Docusaurus keeps a raw U+200E in the tab), and offer `/\p{Default_Ignorable_Code_Point}/gu` (Node 26 supports it) as the fix.
- Verifying a `\p{Default_Ignorable_Code_Point}` fix: loop over all 0..0x10FFFF (skip surrogates; 4174 hits) raw, as `&#x..;` and `&#..;`. Pin-check with narrowing mutants: `[\p{Cf}\p{Mn}]` and "minus Hangul fillers" survive when the test list holds only Cf/Mn code points (the 4 Lo fillers U+115F, U+1160, U+3164, U+FFA0 and the Cn reserved ones are DI too). Classify look-alikes separately: NBSP, hair space and Cyrillic letters are not DI, so they are residuals, not misses.
- Language-class detection on `pre`: probe `<pre><code className="language-powershell">` (class on code only, raw JSX keeps it there), a fence info string without a space (`powershell{1}` gives class `language-powershell{1}`), class order and extra classes in raw JSX, `<CodeBlock language=...>`, upper-case fences (Docusaurus writes the lower-case class on the pre).
- Tests that filter errors through a message-shape helper (`offending(errors)`) without an `errors.length` check silently ignore errors of another shape; a reverted test line can stay green for the wrong reason. Run the reverted line under the new code to see it.

Rule: when self-tests move from a live tree to a committed snapshot, derive the moved set empirically and run every probe in scratch copies.

**Why:** a static list of "tests that read the tree" is a guess; a snapshot probe (strip the markers from the fixture) and the base red probe (base scripts, live markers stripped) give the dependent set by name, and they should be identical. A kill matrix over that set then shows each moved test still guards.
**How to apply:**
- Scratch copy = `rsync -a --exclude node_modules --exclude build* guide <dir>/`, symlink `node_modules`, `rsync` `docs/spec-pack` next to it (the checker finds the pack relative to itself); `diff -rq` the copy's scripts against the worktree first. Mutants, docs-state probes (one marker removed at a time, all removed, the next task's pages via `git show <sha>:<path>`) and throwaway tests all run there, so the worktree is never edited.
- CLI-vs-runChecks consistency tests are state-dependent: exit-code and stderr mutants only show when the live tree has errors, so run the CLI mutant set in a clean and an error state; a mutant of a constant both sides share (the pack path) is equivalent for them by construction and must be killed elsewhere.
- Probe a rule-violating body on a live page too: it must reach only `check.mjs`, never a self-test.

Rule: for a content-page task, bind the pages to the frozen oracle with page mutants in a scratch copy plus a rendered cross-check.

**Why:** a green `npm run check` on real pages does not show that each rule binds those pages; only one exact edit per rule on the real page text does, and the kill line must name the rule.
**How to apply:**
- Scratch = `rsync guide` (no node_modules, builds) plus `docs/spec-pack`, symlinked node_modules; one runner applies a single `replaceOnce` per mutant, runs `node scripts/check.mjs` (about 1 s), prints the non-ok lines, restores. A reinserted skeleton marker exits 0 (pending only) until the marker ban exists: not a survivor. Break one contractual anchor in the copy: `npm run build` exits 1 with "broken anchors".
- Rendered cross-check imports `ENTRIES` and `packCommandLines` read-only. Tab panel n of a `.tabs-container` pairs with tab n; copy buttons must equal the `pre` count with hidden panels. A `pre`'s `textContent` has no newlines (join `.token-line`s to test a fenced card's rows); offline hash-router hrefs are `#/route#anchor` (split on `#`, anchor is part 3). Leakage rule ("no spec line longer than N chars in the page"): also run an 8-word shingle overlap, minus the permitted source section, with an appended-line control that must hit. Docusaurus heading hash-links add one U+200B per heading to article text, so a non-ASCII text sweep counts them: not page content.
- Dev mode: spawn `docusaurus start --port N --no-open --host localhost` from the script, drive the flow, kill the child, show `lsof` empty. `gh run view <id> --json jobs,headSha,event,conclusion > file` binds CI evidence to the head sha. Solution-text scan (line vs built text): compare with backticks stripped and with tags removed WITHOUT inserting a space (a tag-to-space text turns `x`. into "x ." and hides a wrapped-tail hit); a control that appends the forbidden source lines must hit; extract prompts from the pack by quoted strings but drop one-word quotes that belong to an error string. Docusaurus can garble the built meta description from an unchanged `**Goal:** ...: ...` line (L2: "Goal the code through ..."): report it, it is not a comment leak.

Related: [[shell-traps]], [[scaffold-verification-techniques]]
