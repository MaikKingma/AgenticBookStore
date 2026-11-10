---
name: docusaurus-guide-traps
description: Non-obvious traps for the guide/ Docusaurus site, its node:test checker (MDX scanner, built-HTML rules, probes), printable A4 pages and Node CLI pipe truncation
metadata:
  type: reference
---

No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and its research file.

## Running and tooling
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`. Long runs and probes go in a
  script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n`. Never `sleep` in Bash (blocked): use
  `run_in_background`. A worktree `bash -c 'cd ...'` is refused by the guard; use a script file.
- `docusaurus serve` needs the site dir as an argument (`serve "<root>/guide" --port N --no-open`). `lsof` is in /usr/sbin.
- `node --test "scripts/**/*.test.mjs"` (quoted glob) finds tests under scripts/ and nested; chain it with `&&` before the
  checker. It exits 0 with "tests 0" on an unmatched glob and counts an emptied file as one pass; a zero-test guard inside
  `scripts/**` can only read the `check` script's glob and count registrations.
- Content pages: `node scripts/check.mjs` alone (after `node --test` is green) separates page defects from self-test noise;
  `npm run check` output is 60 KB, so grep the `ok `/`FAIL`/`coverage:` lines. A "every text file" rule must exempt
  `package-lock.json`.
- Docusaurus strips numeric file prefixes from routes (`00-setup-check.md` is `/setup-check`). Built pages are
  `build/<route>.html`, never `route/index.html`.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`; a bare `[data-theme='dark']` loses. Set `--ifm-link-decoration` per
  `.markdown a`. `.navbar` needs `color` (hamburger is currentColor) and scoped `--ifm-menu-*` vars for the mobile menu. An
  empty Prism theme (`{plain: {}, styles: []}`) lets CSS own code colours. Body background is on `html`.
- With `future.v4`, `## Title {#id}` fails in MDX; use `## Title {/* #id */}`. `- [ ]` items render as bare disabled
  checkboxes and axe reports `label` on `.task-list-item > input`; fix that held: a local rehype plugin (`ariaLabel` property)
  in the preset's `docs.rehypePlugins`.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels (a colon label is mandatory). Tracks
  page = numbered list with the fence nested 3-space, one command per fence, no admonition in a tab; level page = h3 steps
  instead of nested list fences; the double-backtick span for `` !`cmd` ``; bold never inside a manifest snippet.
- Manifest `text` snippets are case-sensitive and match page source. A fence nested in a list item still counts for
  `kind: command`. A solution-text scan flags wrapped tails of pack lines: judge each hit against the plan section. Anchor
  report-line regexes: "table of contents" matches `/content/`.

## Checker design (rules, self-tests, mutants)
- Self-tests that only call rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule describing the whole guide cannot run by default
  (older tests build minimal trees): gate it behind an option set in `main()` for the default root.
- A frozen "live guide" test comparing CLI stdout line by line to in-process `runChecks` forbids a new `ok` line from an opt-in
  rule: print nothing on pass, `FAIL <rule>` plus errors on fail. Self-tests needing a known page state read
  `scripts/fixtures/skeleton-docs/`, never live docs.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir is a symlink, the main() guard compares argv
  to the module URL): `realpathSync` the temp dir and assert stdout too.
- Frozen-file discipline: `shasum -a 256` every frozen file before the first edit; apply mutants through a node runner (edit,
  spawn `node --test`, restore, sha check). Call helper scripts by absolute path, never `~`, `$VAR` args or `$?`. A mutant search
  text holding `${...}` must be a plain quoted string.
- Loading `docusaurus.config.ts` from node:test prints MODULE_TYPELESS (breaks the 0-warning baseline): pin config wiring on the
  source text.
- A "no body H1" rule must cover ATX with 0 to 3 spaces of indent and setext, not only `# ` at column 0. A box parser that keeps
  only the last fenced block per entry hides a second: keep all and count.

## MDX scanner (scripts/lib/mdscan.mjs)
- Scan fences and comments in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is; JSX comments may have
  spaces in the braces). A comment opener inside an inline code span must not start a comment: skip spans, also across a line
  break inside one paragraph, never past a blank line or a fence.
- Keep ONE span recogniser (`scan.spans`); comment blanking, `markupOf`, `codeOf` and the pack allow-list all use it. Separate
  regexes drifted and a tag inside a span ended a tab early. Raw `<code>`/`<pre>` interiors count as code.
- A multi-line span is safe only inside one paragraph as MDX parses it: stop at every block start (list marker, ATX, setext,
  deeper `>`, `|` row, GFM header row, `<`, `{`, `:::`, fence) and bound same-line spans in table rows to their cell. Ending
  early is tolerated, joining is a fail-open. Also `\<Tag>`, `\{/*`, `{'...'}` and a backslash before a backtick open or
  escape. Fences after `>`/list markers need container-aware body lines.
- Coverage rules: leave the at a glance box, the "Level cleared when" checklist and the frontmatter out of searched text; fold
  whitespace and the whitespace around `[ ] { }`, strip blockquote markers, undo the table escape. Read command-shaped rules on
  code only (`codeOf`) since "set"/"export" are English in a warning. Count coverage per pack section AND per page (else
  `/clear` is covered by the cheat sheet); a tiny snippet with `#` falsely covers `#`.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes (`<input type=checkbox disabled/>`): use the quote-aware scanner
  `scripts/lib/html.mjs`, not `type="checkbox"` regexes. Tabs: `<li role=tab>` has no `</li>`, `</p>` is dropped before a block,
  `.token-line` is a `div` with a trailing `<br/>`, the PowerShell panel is the hidden second `[role=tabpanel]`. A tree builder
  needs pop-to-matching end tags plus "new li closes the open li of the same list". `textContent` of a pre joins rows: read
  `.token-line` per row.
- "Invisible characters" is a Unicode property (`/\p{Default_Ignorable_Code_Point}/gu`), never a hand list (the build keeps a raw
  U+200E in a tab label). A fence info string without a space keeps its suffix (`powershell{1}`); match names with
  `(?![a-z0-9])`, not `$`.
- Probe builds without touching the repo: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink
  node_modules, `npm --prefix <copy> run build` (about 2 s warm). Capture fixtures as the `<article>` slice (real pages carry
  U+200B in hash links).
- Playwright: import it by absolute path from another scratch install; wait for `document.title` to update (react-helmet).

## Edit/Write traps
- Edit/Write turn a backslash-u escape (NBSP, or inside a regex class) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)` and `new RegExp` from char codes, and grep edited files for non-ASCII.
- An Edit deleting text at a list line end can swallow the newline and fuse bullets: read `git diff -U0` of edited lists.

## Printable A4 pages
- Chromium `page.pdf({format: 'A4'})`, margin 0: 1123 px per page minus 32 px bottom padding of `.container`. Measure at viewport
  `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, hide the section, read `documentElement.scrollHeight` (6 px over
  made a second page); re-measure after every list edit. Count PDF pages from `/Count N` of the `/Type /Pages` object.

## Node CLI pipe truncation (Linux CI only)
- A spawnSync test of a CLI that ends `process.exit(1)` after long `console.error` (about 33 KB) lost the stderr tail on Linux CI
  and never on macOS: pipes are async on Linux and `process.exit()` drops queued output. Fix that held:
  `spawnSync(node, [script], {cwd, stdio: ['ignore', fdOut, fdErr]})` with two `openSync` files in a `mkdtempSync` dir
  (helper `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`). `process.exitCode = 1` in the CLI also fixes it.
