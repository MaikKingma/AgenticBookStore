---
name: docusaurus-guide-traps
description: Non-obvious traps for the guide/ Docusaurus site, its node:test checker (MDX scanner, built-HTML rules, probes), printable A4 pages and Node CLI pipe truncation
metadata:
  type: reference
---
No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and its research file.

## Running and tooling
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`. Long runs and probes go in a
  script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n` (no `sleep`: `run_in_background`; the guard
  refuses `bash -c 'cd ...'`). `docusaurus serve` needs the site dir: `serve "<root>/guide" --port N --no-open`.
- `node --test "scripts/**/*.test.mjs"` (quoted glob) exits 0 with "tests 0" on an unmatched glob and counts an emptied file as
  one pass (a zero-test guard must read the `check` script's glob and count registrations). `node scripts/check.mjs` alone
  separates page defects from self-test noise; `npm run check` output is 60 KB, grep `ok `/`FAIL`/`coverage:`. An "every text
  file" rule must exempt `package-lock.json`.
- Docusaurus strips numeric file prefixes from routes (`00-setup-check.md` is `/setup-check`); built pages are
  `build/<route>.html`, never `route/index.html`.
- `npm exec node scripts/x.mjs` is NOT a way to run node (it fetches `node@alpha`): run the pinned node binary on a script file.
  A `import.meta.url === file://${argv[1]}` main guard fails on a path with a space: use `pathToFileURL`.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`; a bare `[data-theme='dark']` loses. Set `--ifm-link-decoration` per
  `.markdown a`. `.navbar` needs `color` (hamburger is currentColor) and scoped `--ifm-menu-*` vars. An empty Prism theme
  (`{plain: {}, styles: []}`) lets CSS own code colours. Body background is on `html`.
- Bare `--ifm-color-primary` is painted as text by Infima (TOC hover/active): primary itself must be the AA shade per mode.
- With `future.v4`, `## Title {#id}` fails in MDX; use `## Title {/* #id */}`. `- [ ]` items render as bare disabled checkboxes
  and axe reports `label`: a local rehype plugin (`ariaLabel` property) in the preset's `docs.rehypePlugins` held.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels (a colon label is mandatory). Tracks
  page = numbered list with the fence nested 3-space, one command per fence, no admonition in a tab; level page = h3 steps;
  double-backtick span for `` !`cmd` ``; bold never inside a manifest snippet. Manifest `text` snippets are case-sensitive and
  match page source; a nested fence still counts for `kind: command`. Anchor report-line regexes ("table of contents" matches
  `/content/`).

## Checker design (rules, self-tests, mutants)
- Self-tests calling only rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule describing the whole guide is gated behind an
  option set in `main()` for the default root.
- A frozen "live guide" test comparing CLI stdout to in-process `runChecks` forbids a new `ok` line from an opt-in rule: print
  nothing on pass, `FAIL <rule>` plus errors on fail. Same for `check-built` (`built-powershell.test.mjs`): print an ok line
  only when its count is above 0. Self-tests needing a page state read `scripts/fixtures/skeleton-docs/`.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir symlink vs the main() guard): `realpathSync`
  the temp dir and assert stdout too.
- Frozen files: `shasum -a 256` before the first edit. Mutants: run on a scratchpad COPY of `guide/` + `docs/spec-pack` with
  `node_modules` symlinked, via a node runner (edit, spawn `node --test` or `scripts/check.mjs`, restore by hash). Count only
  NEW failing titles versus the baseline as kills. Call helper scripts by absolute path, never `~`, `$VAR` args or `$?`.
- "Survives the old gate, dies against the new" proof: one runner, two variants (new tests vs the prior gate's test file swapped
  in from `git show <sha>:path` saved by a script); the same mutant list gives SURVIVED then KILLED. In a git worktree the Bash
  guard refuses `git show ... > file` inline: put every git read in a script file. A fixture-page line number: count the blank
  line after frontmatter (a 4-line head puts the first box on line 6).
- Loading `docusaurus.config.ts` from node:test prints MODULE_TYPELESS (breaks the 0-warning baseline): pin config on source text.
- A "no body H1" rule must cover ATX with 0-3 spaces of indent and setext; a box parser must keep all fenced blocks per entry.
- `coverage-manifest.test.mjs` reads the SKELETON snapshot pages (rule b is silent there): a test pinning live page text must read
  `readPages(join(GUIDE, 'docs'))`. `SWEEP_EXCLUDED` keys and the `plan:<n>` witnesses move on ANY plan edit above them, also an
  inserted agenda sentence: remap old -> new by a script (read the actual list from the failing deepEqual diff), reasons untouched.
- A sweep literal is covered by SUBSTRING: a snippet holding `/plan-product` covers the literal `/plan`, so its `SWEEP_EXCLUDED` key
  goes stale (delete it with a comment naming the covering entry). `plan:<n>` also hides in assertions (`includes('plan:58 ...')`):
  grep `plan:[0-9]`, not only keys. A whole PowerShell line newly in the plan breaks a test that used it as "only the SDD guide
  states it" (check-content scratch-pack witness): retarget it to another sdd-only line.
- A new plan fence (rule file, text mock) is swept line by line: pin it with ONE whole-block `kind: 'command'` entry (snippet is a
  template literal, folded on both sides) and keep `#` out of mock lines (the sweep strips whitespace + `#...`). A new inline span
  in the plan needs an entry whose snippet CONTAINS it: pin the sentence that holds it, not the bare span.
- `scanMarkdown().blocks[].body` keeps the indent of a list item that holds the fence (plan steps): dedent by the opening fence
  line's indent before comparing a block with a file. Page text pinned by a node:test of its own (byte equality, mock arithmetic)
  is read from the live page, never the skeleton snapshot.

## MDX scanner (scripts/lib/mdscan.mjs)
- Scan fences and comments in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is). A comment opener
  inside an inline code span must not start a comment: skip spans, also across a line break in one paragraph, never past a blank
  line or fence. Keep ONE span recogniser (`scan.spans`) for comment blanking, `markupOf`, `codeOf` and the pack allow-list.
- A multi-line span is safe only inside one paragraph: stop at every block start (list marker, ATX, setext, `>`, `|` row, `<`,
  `{`, `:::`, fence, GFM header row); bound table-row spans to their cell. Ending early is tolerated, joining is a fail-open.
  Also `\<Tag>`, `\{/*`, `{'...'}` and a backslash before a backtick.
- Coverage rules: leave the at-a-glance box, "Level cleared when" checklist and frontmatter out; fold whitespace around
  `[ ] { }`, strip blockquote markers. Read command-shaped rules on `codeOf` only. Count coverage per pack section AND per page.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes (`<input type=checkbox disabled/>`): use the quote-aware `scripts/lib/html.mjs`
  (it drops a trailing slash of an unquoted value before `>`: read the raw tag text; void `<img>` nests the next one).
  `<li role=tab>` has no `</li>`, `</p>` is dropped before a block, `.token-line` is a `div` with a trailing `<br/>`, the
  PowerShell panel is the hidden second `[role=tabpanel]`. Read a pre per `.token-line`, not `textContent`.
- "Invisible characters" is `/\p{Default_Ignorable_Code_Point}/gu`, never a hand list (the build keeps a raw U+200E in a tab
  label). A fence info string without a space keeps its suffix (`powershell{1}`); match names with `(?![a-z0-9])`, not `$`.
- Probe builds: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink node_modules, `npm --prefix <copy>
  run build` (~2 s warm). Capture fixtures as the `<article>` slice (hash links carry U+200B).
- The offline build is client rendered: its `index.html` holds no table, so a built-HTML rule sees nothing there. Count compiled
  MDX in the js chunks (`.jsx(s.table,` vs `className:"guide-table-region"`) and the DOM in the rig instead.
- Playwright: import by absolute path from a scratch install; wait for `document.title` to update (react-helmet). Theme: set
  `newContext({colorScheme})` and wait for `dataset.theme` (a localStorage seed did not switch it). A `<details>` (Docusaurus
  Details animates) flips `open` first: wait `locator.waitFor({state: 'visible'})` before `isVisible()`.

## Rig: navbar, theme wrappers, breadcrumbs (axe, focus rings)
- axe `color-contrast` lists a label as `incomplete` when its fill is a pseudo-element or a negative z-index span under
  `isolation: isolate`; real background spans BEFORE a `position: relative` link pass. A symbol-only node is `incomplete`
  (nonBmp) even with aria-hidden: draw it as CSS `content: '\2713'` in an empty aria-hidden span. Compare in a scratch page.
- A focus ring on a `clip-path` element is clipped: clip only sibling layer spans, never the link. Prove rings with
  `page.screenshot({clip})` decoded on a canvas. A Tab-focused region sits under the sticky navbar: `scroll-margin-top`.
- axe `scrollable-region-focusable` fires on any `.markdown table` wider than 390 px; CSS cannot fix it. Held: rehype plugin
  `rehype-table-regions.mjs` wraps each table in `div tabindex=0 role=region aria-label` (label = nearest heading, unique per
  page or `landmark-unique` fires); the wrapper scrolls, inner table `display: table`.
- Rig selector `.markdown a` also matches crumb links and `.alert` links: exclude with `:not(nav a):not(.alert a)`.

## Edit/Write, A4 pages, CLI pipes
- Edit/Write turn a backslash-u escape (NBSP, also inside a regex class) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)` / `new RegExp` from char codes, and grep edited files for non-ASCII. To keep a real check mark in
  TSX, `sed -i '' "s/^const X = .*\$/const X = '\\\\u2713';/"` then `od -c` the line. An Edit deleting text at a list line end
  can swallow the newline and fuse bullets: read `git diff -U0` of edited lists.
- Printable A4 (Chromium `page.pdf({format: 'A4'})`, margin 0): 1123 px per page minus 32 px bottom padding of `.container`.
  Measure at viewport `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, read `documentElement.scrollHeight` (6 px over
  made a second page); re-measure after every list edit. Count PDF pages from `/Count N` of the `/Type /Pages` object.
- A spawnSync test of a CLI ending `process.exit(1)` after ~33 KB of `console.error` lost the stderr tail on Linux CI only.
  Fix: `spawnSync(node, [script], {cwd, stdio: ['ignore', fdOut, fdErr]})` with two `openSync` files in a `mkdtempSync` dir
  (helper `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`); `process.exitCode = 1` also fixes it.
