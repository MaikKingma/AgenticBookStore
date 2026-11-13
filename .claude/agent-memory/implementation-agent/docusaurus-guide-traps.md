---
name: docusaurus-guide-traps
description: Non-obvious traps for the guide/ Docusaurus site, its node:test checker (MDX scanner, built-HTML rules, page registration, mutants), A4 pages and CLI pipe truncation
metadata:
  type: reference
---
No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and research file.

## Running and tooling
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`. Long runs and probes go in a
  script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n`. `docusaurus serve` needs the site dir:
  `serve "<root>/guide" --port N --no-open`. `npm exec node x.mjs` fetches `node@alpha`: run the pinned node on a script file.
- `node --test "scripts/**/*.test.mjs"` exits 0 with "tests 0" on an unmatched glob. `node scripts/check.mjs` alone separates
  page defects from self-test noise; `npm run check` output is 60 KB: grep `ok `/`FAIL`/`coverage:`.
- Routes strip numeric file prefixes (`00-setup-check.md` is `/setup-check`); built pages are `build/<route>.html`.
- A `import.meta.url === file://${argv[1]}` main guard fails on a path with a space: use `pathToFileURL`.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`. `.navbar` needs `color`. An empty Prism theme lets CSS own code colours.
  Bare `--ifm-color-primary` is painted as text: it must be the AA shade per mode.
- With `future.v4`, `## Title {#id}` fails in MDX: use `## Title {/* #id */}`. `- [ ]` items render as unlabelled disabled
  checkboxes (axe `label`): a local rehype plugin (`ariaLabel`) in `docs.rehypePlugins` held.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels. Tracks page = numbered list with the
  fence nested 3-space, one command per fence, no admonition in a tab; double-backtick span for `` !`cmd` ``. Manifest `text`
  snippets are case-sensitive and match page source. Anchor report-line regexes ("table of contents" matches `/content/`).

## Adding a page (what it touches)
- Beyond the page: `rules/structure.mjs` OTHER_PAGES, the skeleton snapshot fixture (`scripts/fixtures/skeleton-docs/docs`),
  `rules/routes-built.mjs` frozen list, `coverage.mjs` PAGE_FILES plus an entry, every page-count pin. Find pins with
  `git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])' -- guide/scripts guide/README.md`; ids, dates, `T11` are not pins.
- Coverage error messages cut the snippet at 80 characters plus `...`. A fixture test using the new route as its "extra unknown
  loc" must pick another. Name chain positions in `routes-built.test.mjs` instead of renumbering.
- A guide page with literals mirrored in the plan: the coverage sweep needs one entry per plan span and fence line, snippet word
  for word in page prose (not the at-a-glance box) and the plan section. A sweep literal is covered by SUBSTRING (`/plan-product`
  covers `/plan`, so a `SWEEP_EXCLUDED` key goes stale). Pin a new plan fence with ONE whole-block `kind: 'command'` entry.
  `SWEEP_EXCLUDED` keys and `plan:<n>` witnesses move on ANY plan edit above them: remap by script.
  `scanMarkdown().blocks[].body` keeps the list-item indent: dedent by the fence indent.
- `coverage-manifest.test.mjs` reads the SKELETON pages: a test pinning live page text uses `readPages(join(GUIDE, 'docs'))`.

## Checker design (rules, self-tests, mutants)
- Self-tests calling only rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule wired for the default build dir only breaks tests
  running the CLI on a fixture (`withCopiedScripts` in `navbar.test.mjs`, `level-progress.test.mjs`).
- A frozen test comparing CLI stdout to `runChecks` forbids a new `ok` line from an opt-in rule: print nothing on pass, `FAIL
  <rule>` plus errors on fail.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir symlink vs main() guard): `realpathSync`
  the temp dir and assert stdout too.
- Mutants: run on a scratchpad COPY of `guide/` + `docs/spec-pack` with `node_modules` symlinked, via a node runner (string-replace
  one production file in memory, spawn `node --test`, restore in `finally`, compare sha256; `shasum -a 256` frozen files before
  the first edit). Count the `from` string to prove the mutant applied. Count only NEW failing titles vs the baseline as kills.
  A mutant equivalent in the all-green case needs a test on the partial-failure return value. Never `~`, `$VAR` or `$?` in
  helper-script calls. "Survives the old gate, dies against the new": same runner, old tests from `git show <sha>:path` saved by
  a script (the worktree guard refuses `git show ... > file` inline).
- The zero-test guard blanks block comments before counting `test(`: a test string holding an unclosed MDX comment opener hides
  tests. Build such literals from parts (`'{/' + '*'`); compare guard vs plain count per edited file (`rules/zero-tests.mjs`).

## MDX scanner (scripts/lib/mdscan.mjs)
- Scan fences and comments in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is). A comment opener in an
  inline code span must not start a comment. Keep ONE span recogniser (`scan.spans`) for all consumers.
- A multi-line span is safe only inside one paragraph: stop at every block start (list marker, ATX, setext, `>`, `|`, `<`, `{`,
  `:::`, fence); bound table-row spans to their cell. Ending early is tolerated, joining is a fail-open. Also `\<Tag>`, `\{/*`.
- Coverage rules: leave the at-a-glance box, "Level cleared when" checklist and frontmatter out; count per pack section AND page.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes: use the quote-aware `scripts/lib/html.mjs`. It drops a trailing slash of an
  unquoted value last in the tag: read hrefs from the raw tag like `hrefOf` in `rules/navbar-built.mjs`. `<li role=tab>` has no
  `</li>`, `.token-line` is a `div` with `<br/>`: read a pre per `.token-line`, not `textContent`.
- "Invisible characters" is `/\p{Default_Ignorable_Code_Point}/gu`, never a hand list. A fence info string without a space keeps
  its suffix (`powershell{1}`): match with `(?![a-z0-9])`.
- Probe builds: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink node_modules, `npm --prefix <copy>
  run build` (~2 s warm). The offline build is client rendered: `index.html` holds no table.
- Playwright: import by absolute path from a scratch install. Theme: `newContext({colorScheme})`, wait for `dataset.theme`. A
  `<details>` flips `open` first: `locator.waitFor({state: 'visible'})` before `isVisible()`.

## Rig: axe, focus rings
- axe `color-contrast` marks a label `incomplete` when its fill is a pseudo-element or a negative z-index span; a symbol-only
  node is `incomplete` even with aria-hidden: draw it as CSS `content: '\2713'` in an empty aria-hidden span.
- A focus ring on a `clip-path` element is clipped: clip sibling layer spans only.
- axe `scrollable-region-focusable` fires on any `.markdown table` wider than 390 px; held: rehype plugin
  `rehype-table-regions.mjs` wrapping each table in `div tabindex=0 role=region aria-label` (unique per page).

## Edit/Write, A4 pages, CLI pipes
- Edit/Write turn a backslash-u escape (NBSP, also in a regex class) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)`; grep edited files for non-ASCII. An Edit deleting text at a list line end can fuse bullets: read
  `git diff -U0`.
- Printable A4 (`page.pdf({format: 'A4'})`, margin 0): 1123 px per page minus 32 px bottom padding of `.container`. Measure at
  viewport `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, `documentElement.scrollHeight` (6 px over made a second
  page). Count PDF pages from `/Count N` of the `/Type /Pages` object.
- A spawnSync test of a CLI ending `process.exit(1)` after ~33 KB of stderr lost the tail on Linux CI only: use
  `process.exitCode = 1`, or `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`.
