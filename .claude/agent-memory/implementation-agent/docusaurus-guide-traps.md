---
name: docusaurus-guide-traps
description: Non-obvious traps for the guide/ Docusaurus site, its node:test checker (MDX scanner, built-HTML rules, page registration, mutants), printable A4 pages and CLI pipe truncation
metadata:
  type: reference
---
No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and its research file.

## Running and tooling
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`. Long runs and probes go in a
  script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n` (use `run_in_background`, not `sleep`).
  `docusaurus serve` needs the site dir: `serve "<root>/guide" --port N --no-open`.
- `node --test "scripts/**/*.test.mjs"` (quoted glob) exits 0 with "tests 0" on an unmatched glob and counts an emptied file as
  one pass (a zero-test guard must read the `check` script's glob and count registrations). `node scripts/check.mjs` alone
  separates page defects from self-test noise; `npm run check` output is 60 KB, grep `ok `/`FAIL`/`coverage:`. An "every text
  file" rule must exempt `package-lock.json`.
- Docusaurus strips numeric file prefixes from routes (`00-setup-check.md` is `/setup-check`); built pages are
  `build/<route>.html`, never `route/index.html`.
- `npm exec node scripts/x.mjs` fetches `node@alpha`: run the pinned node binary on a script file. A
  `import.meta.url === file://${argv[1]}` main guard fails on a path with a space: use `pathToFileURL`.
- Docusaurus builds the page `<meta description>` from the first paragraph, even inside an admonition.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`; a bare `[data-theme='dark']` loses. `.navbar` needs `color`
  (hamburger is currentColor). An empty Prism theme (`{plain: {}, styles: []}`) lets CSS own code colours. Body background is
  on `html`. Bare `--ifm-color-primary` is painted as text: primary itself must be the AA shade per mode.
- With `future.v4`, `## Title {#id}` fails in MDX; use `## Title {/* #id */}`. `- [ ]` items render as bare disabled checkboxes
  and axe reports `label`: a local rehype plugin (`ariaLabel` property) in the preset's `docs.rehypePlugins` held.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels (a colon label is mandatory). Tracks
  page = numbered list with the fence nested 3-space, one command per fence, no admonition in a tab; double-backtick span for
  `` !`cmd` ``; bold never inside a manifest snippet. Manifest `text` snippets are case-sensitive and match page source.
  Anchor report-line regexes ("table of contents" matches `/content/`).

## Adding a page (what it touches)
- Beyond the page: `rules/structure.mjs` OTHER_PAGES, the skeleton snapshot fixture (`scripts/fixtures/skeleton-docs/docs`),
  `rules/routes-built.mjs` frozen list, `coverage.mjs` PAGE_FILES plus an entry, and every page-count pin. Find pins with
  `git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])' -- guide/scripts guide/README.md`; classify each hit (ids like `L4-11`,
  dates, `T11` are not pins; derived counts move by the number of new pages with entries).
- Coverage error messages cut the snippet at 80 characters plus `...`: match that form. A fixture test that used the new
  route as its "extra unknown loc" must pick another route. Name chain positions in `routes-built.test.mjs`
  (`const [HOME, ..., WRAP_UP] = LIST.keys()`) instead of renumbering.
- `git diff -U0` slides a blank-line-bounded insertion to its lowest position: same text, other hunk header; tell the QA lead.
- MDX rule shape that held: before the first element allow only blank lines and whole-line one-line MDX comments, judge inside
  the admonition's content lines, strip code spans before looking for a link, skip fenced lines.

## Checker design (rules, self-tests, mutants)
- Self-tests calling only rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule describing the whole guide is gated behind an
  option set in `main()` for the default root. A rule wired for the default build dir only breaks every test that runs the CLI
  with no argument on a fixture (`withCopiedScripts` in `navbar.test.mjs`, `level-progress.test.mjs`): extend their fixtures.
- A frozen "live guide" test comparing CLI stdout to in-process `runChecks` forbids a new `ok` line from an opt-in rule: print
  nothing on pass, `FAIL <rule>` plus errors on fail. Same for `check-built`: print an ok line only when its count is above 0.
  Self-tests needing a page state read `scripts/fixtures/skeleton-docs/`.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir symlink vs the main() guard):
  `realpathSync` the temp dir and assert stdout too.
- Frozen files: `shasum -a 256` before the first edit. Mutants: run on a scratchpad COPY of `guide/` + `docs/spec-pack` with
  `node_modules` symlinked, via a node runner script (string-replace one production file in memory, write, spawn
  `node --test` or `scripts/check.mjs`, restore in `finally`, compare sha256). Check each mutant applied (count the `from`
  string: `if (boxes.length !== 1) {` can occur twice). Count only NEW failing titles versus the baseline as kills. Write
  `endsWith`/`includes` mutants so the good path still passes. A mutant equivalent in the all-green case (a count printed from a
  constant) needs a test on the partial-failure return value. Call helper scripts by absolute path, never `~`, `$VAR` or `$?`.
- "Survives the old gate, dies against the new": one runner, two variants (new tests vs the prior gate's test file from
  `git show <sha>:path` saved by a script; the worktree guard refuses `git show ... > file` inline).
- Loading `docusaurus.config.ts` from node:test prints MODULE_TYPELESS (breaks the 0-warning baseline): pin config on source text.
- `coverage-manifest.test.mjs` reads the SKELETON snapshot pages: a test pinning live page text must read
  `readPages(join(GUIDE, 'docs'))`. `SWEEP_EXCLUDED` keys and `plan:<n>` witnesses move on ANY plan edit above them: remap
  old -> new by a script (list from the failing deepEqual diff); grep `plan:[0-9]`, also in assertions.
- A sweep literal is covered by SUBSTRING: a snippet holding `/plan-product` covers `/plan`, so its `SWEEP_EXCLUDED` key goes
  stale (delete it, comment the covering entry). A new plan fence is swept line by line: pin it with ONE whole-block
  `kind: 'command'` entry (folded both sides, no `#` in mock lines); a new inline span needs an entry whose snippet
  CONTAINS it. `scanMarkdown().blocks[].body` keeps the list-item indent: dedent by the opening fence's indent.

## MDX scanner (scripts/lib/mdscan.mjs)
- Scan fences and comments in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is). A comment opener
  inside an inline code span must not start a comment: skip spans, also across a line break in one paragraph, never past a blank
  line or fence. Keep ONE span recogniser (`scan.spans`) for all consumers.
- A multi-line span is safe only inside one paragraph: stop at every block start (list marker, ATX, setext, `>`, `|` row, `<`,
  `{`, `:::`, fence); bound table-row spans to their cell. Ending early is tolerated, joining is a fail-open. Also `\<Tag>`,
  `\{/*`, `{'...'}` and a backslash before a backtick.
- Coverage rules: leave the at-a-glance box, "Level cleared when" checklist and frontmatter out; read command-shaped rules on
  `codeOf` only; count coverage per pack section AND per page.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes (`<input type=checkbox disabled/>`): use the quote-aware `scripts/lib/html.mjs`.
  It drops a trailing slash of an unquoted value written last in the tag (`href=/Base/>` reads `/Base`): read hrefs from the raw
  tag like `hrefOf` in `rules/navbar-built.mjs` (not exported; copy it). Void `<img>` nests the next one.
  `<li role=tab>` has no `</li>`, `</p>` is dropped before a block, `.token-line` is a `div` with a trailing `<br/>`, the
  PowerShell panel is the hidden second `[role=tabpanel]`. Read a pre per `.token-line`, not `textContent`.
- `grep -o` with a long bounded repeat dies on "exceeds complexity limits": quote markup with a small node script instead.
- "Invisible characters" is `/\p{Default_Ignorable_Code_Point}/gu`, never a hand list (the build keeps a raw U+200E in a tab
  label). A fence info string without a space keeps its suffix (`powershell{1}`); match names with `(?![a-z0-9])`, not `$`.
- Probe builds: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink node_modules, `npm --prefix <copy>
  run build` (~2 s warm). Capture fixtures as the `<article>` slice (hash links carry U+200B).
- The offline build is client rendered: its `index.html` holds no table; count compiled MDX in the js chunks or the DOM in the rig.
- Playwright: import by absolute path from a scratch install; wait for `document.title` to update. Theme: set
  `newContext({colorScheme})` and wait for `dataset.theme` (a localStorage seed did not switch it). A `<details>` flips `open`
  first: wait `locator.waitFor({state: 'visible'})` before `isVisible()`.

## Rig: navbar, theme wrappers, breadcrumbs (axe, focus rings)
- axe `color-contrast` lists a label as `incomplete` when its fill is a pseudo-element or a negative z-index span under
  `isolation: isolate`; real background spans BEFORE a `position: relative` link pass. A symbol-only node is `incomplete`
  (nonBmp) even with aria-hidden: draw it as CSS `content: '\2713'` in an empty aria-hidden span. Compare in a scratch page.
- A focus ring on a `clip-path` element is clipped: clip only sibling layer spans, never the link. Prove rings with
  `page.screenshot({clip})` decoded on a canvas. A Tab-focused region sits under the sticky navbar: `scroll-margin-top`.
- axe `scrollable-region-focusable` fires on any `.markdown table` wider than 390 px; CSS cannot fix it. Held: rehype plugin
  `rehype-table-regions.mjs` wraps each table in `div tabindex=0 role=region aria-label` (label unique per page).
- Rig selector `.markdown a` also matches crumb and `.alert` links: exclude `:not(nav a):not(.alert a)`.

## Edit/Write, A4 pages, CLI pipes
- Edit/Write turn a backslash-u escape (NBSP, also inside a regex class) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)` / `new RegExp` from char codes, and grep edited files for non-ASCII. An Edit deleting text at a
  list line end can swallow the newline and fuse bullets: read `git diff -U0` of edited lists.
- Printable A4 (Chromium `page.pdf({format: 'A4'})`, margin 0): 1123 px per page minus 32 px bottom padding of `.container`.
  Measure at viewport `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, read `documentElement.scrollHeight` (6 px over
  made a second page); re-measure after every list edit. Count PDF pages from `/Count N` of the `/Type /Pages` object.
- A spawnSync test of a CLI ending `process.exit(1)` after ~33 KB of `console.error` lost the stderr tail on Linux CI only:
  use `process.exitCode = 1`, or file descriptors for stdio (helper `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`).
