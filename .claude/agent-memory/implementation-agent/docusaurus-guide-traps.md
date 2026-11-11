---
name: docusaurus-guide-traps
description: Non-obvious traps for the guide/ Docusaurus site, its node:test checker (MDX scanner, built-HTML rules, probes), printable A4 pages and Node CLI pipe truncation
metadata:
  type: reference
---

No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and its research file.

## Running and tooling
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`. Long runs and probes go in a
  script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n`. No `sleep` (blocked): use
  `run_in_background`. A worktree `bash -c 'cd ...'` is refused by the guard; use a script file.
- `docusaurus serve` needs the site dir as argument (`serve "<root>/guide" --port N --no-open`). `lsof` is in /usr/sbin.
- `node --test "scripts/**/*.test.mjs"` (quoted glob) exits 0 with "tests 0" on an unmatched glob and counts an emptied file as
  one pass. `node scripts/check.mjs` alone separates page defects from self-test noise; `npm run check` output is 60 KB, grep
  `ok `/`FAIL`/`coverage:`. A "every text file" rule must exempt `package-lock.json`.
- Docusaurus strips numeric file prefixes from routes (`00-setup-check.md` is `/setup-check`); built pages are
  `build/<route>.html`, never `route/index.html`.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`; a bare `[data-theme='dark']` loses. Set `--ifm-link-decoration` per
  `.markdown a`. `.navbar` needs `color` (hamburger is currentColor) and scoped `--ifm-menu-*` vars. An empty Prism theme
  (`{plain: {}, styles: []}`) lets CSS own code colours. Body background is on `html`.
- With `future.v4`, `## Title {#id}` fails in MDX; use `## Title {/* #id */}`. `- [ ]` items render as bare disabled checkboxes
  and axe reports `label`: a local rehype plugin (`ariaLabel` property) in the preset's `docs.rehypePlugins` held.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels (a colon label is mandatory). Tracks
  page = numbered list with the fence nested 3-space, one command per fence, no admonition in a tab; level page = h3 steps; the
  double-backtick span for `` !`cmd` ``; bold never inside a manifest snippet.
- Manifest `text` snippets are case-sensitive and match page source; a fence nested in a list item still counts for
  `kind: command`. A solution-text scan flags wrapped tails of pack lines: judge each hit against the plan. Anchor report-line
  regexes ("table of contents" matches `/content/`).

## Checker design (rules, self-tests, mutants)
- Self-tests calling only rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule describing the whole guide is gated behind an
  option set in `main()` for the default root.
- A frozen "live guide" test comparing CLI stdout to in-process `runChecks` forbids a new `ok` line from an opt-in rule: print
  nothing on pass, `FAIL <rule>` plus errors on fail. Self-tests needing a page state read `scripts/fixtures/skeleton-docs/`.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir symlink vs the main() guard): `realpathSync`
  the temp dir and assert stdout too.
- Frozen files: `shasum -a 256` before the first edit; apply mutants via a node runner (edit, spawn `node --test`, restore, sha
  check). Call helper scripts by absolute path, never `~`, `$VAR` args or `$?`.
- Loading `docusaurus.config.ts` from node:test prints MODULE_TYPELESS (breaks the 0-warning baseline): pin config on source text.
- A "no body H1" rule must cover ATX with 0-3 spaces of indent and setext; a box parser must keep all fenced blocks per entry.

## MDX scanner (scripts/lib/mdscan.mjs)
- Scan fences and comments in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is). A comment opener
  inside an inline code span must not start a comment: skip spans, also across a line break in one paragraph, never past a blank
  line or fence. Keep ONE span recogniser (`scan.spans`) for comment blanking, `markupOf`, `codeOf` and the pack allow-list.
- A multi-line span is safe only inside one paragraph: stop at every block start (list marker, ATX, setext, `>`, `|` row, `<`,
  `{`, `:::`, fence); bound table-row spans to their cell. Ending early is tolerated, joining is a fail-open. Also `\<Tag>`,
  `\{/*`, `{'...'}` and a backslash before a backtick.
- Coverage rules: leave the at-a-glance box, "Level cleared when" checklist and frontmatter out; fold whitespace around
  `[ ] { }`, strip blockquote markers. Read command-shaped rules on `codeOf` only. Count coverage per pack section AND per page.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes (`<input type=checkbox disabled/>`): use the quote-aware `scripts/lib/html.mjs`.
  `<li role=tab>` has no `</li>`, `</p>` is dropped before a block, `.token-line` is a `div` with a trailing `<br/>`, the
  PowerShell panel is the hidden second `[role=tabpanel]`. A tree builder needs pop-to-matching end tags plus "new li closes the
  open li". Read a pre per `.token-line`, not `textContent`.
- "Invisible characters" is `/\p{Default_Ignorable_Code_Point}/gu`, never a hand list. A fence info string without a space keeps
  its suffix (`powershell{1}`); match names with `(?![a-z0-9])`, not `$`.
- Probe builds: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink node_modules, `npm --prefix <copy>
  run build` (~2 s warm). Capture fixtures as the `<article>` slice (hash links carry U+200B).
- Playwright: import by absolute path from another scratch install; wait for `document.title` to update (react-helmet).

## Navbar, theme wrappers and the browser rig
- `lib/html.mjs` drops a trailing slash of an unquoted value before `>` (`href=/Base/>` reads `/Base`): read the raw tag text.
  Void `<img>` nests the next `<img>`: search descendants.
- A `display` on an image inside `ThemedImage` overrides hiding the other mode's file (both logos show before hydration): set no
  display on the img; probe with a no-JavaScript context (always light).
- Mobile menu is not in SSR HTML; on a docs page the sidebar opens the docs panel, navbar items sit behind
  `.navbar-sidebar__back`. Wait ~500 ms after `hover()` before reading link colour. WebKit needs `Alt+Tab` for links.
- Entry-point switch tests without `guide/build`: copy `scripts/` beside a fixture `build/` in a `realpathSync(mkdtemp)` dir.

## Chevron breadcrumb, axe and focus rings (LevelProgress)
- axe `color-contrast` lists a label as `incomplete` (bgOverlap) when its fill is a pseudo-element, or a real span with a
  negative z-index under `isolation: isolate`; it passes with real background spans BEFORE a `position: relative` link
  (z-index auto) or with bg + `clip-path` on a wrapper. A symbol-only node (the check mark) is `incomplete` (nonBmp) even
  with aria-hidden: draw it as CSS `content: '\2713'` in an empty aria-hidden span. Compare variants in a scratch page first.
- A focus ring on a `clip-path` element is clipped: clip only sibling layer spans, never the link. Prove it with
  `page.screenshot({clip})` decoded in-page on a canvas (`willReadFrequently`), sampling ring edges and corners.
- A new default-dir `check-built` rule breaks the older entry-point fixtures (builds with only `index.html`): the fixtures
  gain the pages, listed old -> new; keep the rule opt-in through an option only the default dir sets.
- Mutation sweeps over a copy: record the baseline's failing titles and count only NEW failing titles as kills (a mutant
  `from` string that no longer matched source left a pin test red at baseline and hid a survivor behind `status != 0`).

## Palette swaps (custom.css, brand rule) and sweeps
- `npm exec node scripts/x.mjs` is NOT a way to run node: it tries to fetch `node@alpha` from the registry. Run the pinned node
  binary on a script file. A `import.meta.url === file://${argv[1]}` main guard fails on a path with a space: use `pathToFileURL`.
- Bare `--ifm-color-primary` is painted as text by Infima (TOC link hover/active), so a restricted-contrast brand blue forces
  primary itself to be the AA shade per mode; surfaces under primary text must stay as light as snow (4.56:1) in light mode.
- Test mutants that edit custom.css via `replaceOnce` hit the FIRST declaration; an injected dark override must be a second
  `html[data-theme='dark']` block appended after the real one (`cssVariables` merges blocks, last wins).
- Rig selectors: `.markdown a` also matches the LevelProgress crumb links and links inside `.alert` (alert foreground colour);
  exclude with `:not(nav a):not(.alert a)`. The active sidebar entry of a category is `.menu__link--sublist` (no active bg).
- axe `scrollable-region-focusable` fires on any `.markdown table` wider than 390 px (block + overflow auto, not focusable);
  pre-existing, CSS cannot fix it (`overflow-wrap: anywhere` breaks words); needs `tabindex=0` from the rehype plugin.
- The `console.error` of a deliberate aborted control fetch (`Failed to load resource: net::ERR_FAILED`, Chromium) must be
  tagged as its own phase or it reads as a console finding.

## Edit/Write traps
- Edit/Write turn a backslash-u escape (NBSP) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)` / `new RegExp` from char codes, and grep edited files for non-ASCII. To keep a real `✓`
  in TSX, `sed -i '' "s/^const X = .*\$/const X = '\\\\u2713';/"` then `od -c` the line (CSS `\2713` is unaffected).
- An Edit deleting text at a list line end can swallow the newline and fuse bullets: read `git diff -U0` of edited lists.

## Printable A4 pages
- Chromium `page.pdf({format: 'A4'})`, margin 0: 1123 px per page minus 32 px bottom padding of `.container`. Measure at viewport
  `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, read `documentElement.scrollHeight` (6 px over made a second page);
  re-measure after every list edit. Count PDF pages from `/Count N` of the `/Type /Pages` object.

## Node CLI pipe truncation (Linux CI only)
- A spawnSync test of a CLI ending `process.exit(1)` after ~33 KB of `console.error` lost the stderr tail on Linux CI only (async
  pipes, `process.exit()` drops queued output). Fix: `spawnSync(node, [script], {cwd, stdio: ['ignore', fdOut, fdErr]})` with
  two `openSync` files (helper `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`); `process.exitCode = 1` also fixes it.
