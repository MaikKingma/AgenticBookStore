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
  `npm run check` output is 60 KB, so grep the `ok `/`FAIL`/`coverage:` lines.
- Template leftovers to delete after create-docusaurus: blog/, src/pages, src/components, static/img, static/.nojekyll, docs
  tutorials, the template's .gitignore. Rename the package and run `npm install --package-lock-only`.
- A word-boundary scan over `package-lock.json` hits `postcss-lab-function`; a "every text file" rule must exempt the lockfile.
- A README provenance line cannot hold a font CDN host when a rule bans those strings: source fonts from the upstream GitHub
  tag (static woff2), subset with a scratch-venv fonttools.
- Docusaurus strips numeric file prefixes from routes (`00-setup-check.md` is `/setup-check`). Built pages are
  `build/<route>.html`, never `route/index.html`.

## Theming and MDX
- Infima dark variables sit on `html[data-theme='dark']`; a bare `[data-theme='dark']` loses. Set `--ifm-link-decoration` per
  `.markdown a`. `.navbar` needs `color` (hamburger is currentColor) and scoped `--ifm-menu-*` vars for the mobile menu. An
  empty Prism theme (`{plain: {}, styles: []}`) lets CSS own code colours. Body background is on `html`.
- With `future.v4`, `## Title {#id}` fails in MDX; use `## Title {/* #id */}`. `- [ ]` items render as bare disabled
  checkboxes and axe reports `label` on `.task-list-item > input`; fix that held: a local rehype plugin (`ariaLabel` property)
  in the preset's `docs.rehypePlugins`.
- Tab sets: frozen `TAB_GROUPS` in `rules/content.mjs` forces `groupId` and exact labels (a colon label is mandatory). Recipes
  that went green first time: tracks page = numbered list, fence nested 3-space in each item, one command per fence, no
  admonition in a tab; level page = h3 steps instead of nested list fences, sync and Checkpoint boxes as the L2 page, a
  `Before you move on` h2, the double-backtick span for `` !`cmd` ``, bold never inside a manifest snippet.
- Manifest `text` snippets are case-sensitive and match page source (imperative as in the pack: "cross out everything"). A fence
  nested in a list item still counts for `kind: command`. A solution-text scan flags wrapped tails of pack lines: judge each
  hit against the plan section.
- Pack facts: `Get-Command jdtls` is an inline span, the setup `$env:...` line is a fenced line with a trailing `# comment`,
  `mvnw.cmd verify` sits inside a fence. Anchor report-line regexes: "table of contents" matches `/content/`.

## Checker design (rules, self-tests, mutants)
- Self-tests that only call rule functions miss wiring (a dropped `errors` spread stays green): pin the pair table by name plus a
  minimum count and test each rule through `runChecks` on a temp tree. A rule describing the whole guide cannot run by default
  (older tests build minimal trees): gate it behind an option set in `main()` for the default root.
- A frozen "live guide" test comparing CLI stdout line by line to in-process `runChecks` forbids a new `ok` line from an opt-in
  rule: print nothing on pass, `FAIL <rule>` plus errors on fail. If probes remove all markers, pin CLI output equal to
  `runChecks` plus pending lines derived from the live markers. Self-tests needing a known page state read
  `scripts/fixtures/skeleton-docs/`, never live docs.
- A copied `check.mjs` run from `mkdtemp` prints nothing and exits 0 on macOS (tmpdir is a symlink, the main() guard compares argv
  to the module URL): `realpathSync` the temp dir and assert stdout too.
- Frozen-file discipline: `shasum -a 256` every frozen file before the first edit; apply mutants through a node runner (edit,
  spawn `node --test`, restore, sha check). Call helper scripts by absolute path, never `~`, `$VAR` args or `$?`. A mutant search
  text holding `${...}` must be a plain quoted string. When a rule edit changes a line other mutants search for, adapt and log.
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
  deeper `>`, `|` row, GFM header row and pipe-less rows below it, `<`, `{`, `:::`, fence) and bound same-line spans in table
  rows to their cell. Ending early is tolerated, joining is a fail-open. Also `\<Tag>`, `\{/*`, `{'...'}` and a backslash before
  a backtick open or escape. Fences after `>`/list markers need container-aware body lines. Check refactors against the pack
  with a git-object compare; each redundant guard survives its mutant, so drop it.
- Coverage rules: leave the at a glance box, the "Level cleared when" checklist and the frontmatter out of searched text; fold
  whitespace and the whitespace around `[ ] { }`, strip blockquote markers, undo the table escape. Read command-shaped rules on
  code only (`codeOf`) since "set"/"export" are English in a warning. Count coverage per pack section AND per page (else
  `/clear` is covered by the cheat sheet); a tiny snippet with `#` falsely covers `#`. Leftover reasons key `source:line literal`.

## Built HTML and rendering evidence
- Minified build HTML has unquoted attributes (`<input type=checkbox disabled/>`): use the quote-aware scanner
  `scripts/lib/html.mjs`, not `type="checkbox"` regexes. Tabs: `<li role=tab>` has no `</li>`, `</p>` is dropped before a block,
  `.token-line` is a `div` with a trailing `<br/>`, the PowerShell panel is the hidden second `[role=tabpanel]`. A tree builder
  needs pop-to-matching end tags plus "new li closes the open li of the same list". MDX `<pre>` over lines renders
  `<pre><p>a\nb</pre>`; only `<` is entity-encoded in code text. `textContent` of a pre joins rows: read `.token-line` per row.
- "Invisible characters" is a Unicode property (`/\p{Default_Ignorable_Code_Point}/gu`), never a hand list (the build keeps a raw
  U+200E in a tab label). A fence info string without a space keeps its suffix (`powershell{1}`), raw `<pre><code className>`
  puts the class on the code; match names with `(?![a-z0-9])`, not `$`.
- Probe builds without touching the repo: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink
  node_modules, `npm --prefix <copy> run build` (about 2 s warm). Capture fixtures as the `<article>` slice (real pages carry
  U+200B in hash links; slice a smaller fragment for ASCII checks).
- Playwright: import it by absolute path from another scratch install; the page title sits in `.markdown > header`; wait for
  `document.title` to update (react-helmet). In a rendered link check exclude `a.hash-link` (double hash in the offline build)
  and take the id after the last `#`.

## Edit/Write traps
- Edit/Write turn a backslash-u escape (NBSP, or inside a regex class) into the literal character, failing the ascii rule: use
  `String.fromCharCode(0xa0)` and `new RegExp` from char codes, rewrite strays with Write, and grep edited files for non-ASCII.
- An Edit anchor ending in a space mid-line can lose it; an Edit deleting text at a list line end can swallow the newline and
  fuse bullets. End anchors at a line end and read `git diff -U0` of edited tests/lists (deletions must equal the agreed edits).
  Even `sed -i` is refused by the worktree shell rules.

## Printable A4 pages
- Chromium `page.pdf({format: 'A4'})`, margin 0: 1123 px per page minus 32 px bottom padding of `.container`. Table rows cost 51
  px (one line), 78, 104; a bullet line 30 px; a 24-row table cannot share a page, a bullet list fits. Measure at viewport
  `794 - 2*margin_px` with `emulateMedia({media: 'print'})`, hide the section, read `documentElement.scrollHeight` (at most
  `1123 - 2*margin_px`, 1047 at 10 mm; 6 px over made a second page). Prose wraps near 85 chars, code near 70 per line.
- One restored word can cost a 26 px line; joining two one-line bullets regains about 4 px. Re-measure after every list edit.
- Count PDF pages without tools: the `/Count N` of the `/Type /Pages` object (regex on latin1 text).

## Node CLI pipe truncation (Linux CI only)
- A spawnSync test of a CLI that ends `process.exit(1)` after long `console.error` (about 33 KB) lost the stderr tail on Linux CI
  (cut at 19555 bytes, status still 1) and never on macOS: pipes are async on Linux and `process.exit()` drops queued output.
- Fix that held: `spawnSync(node, [script], {cwd, stdio: ['ignore', fdOut, fdErr]})` with two `openSync` files in a
  `mkdtempSync` dir (helper `runCliCaptured` in `guide/scripts/skeleton-ban.test.mjs`). Use it for any long-output exit(1) CLI.
- Repro locally: official linux-arm64 Node tarball in `ubuntu:24.04` (`apt-get install libatomic1`), worktree mounted read-only,
  parallel workers. `process.exitCode = 1` instead of `process.exit(1)` in the CLI would also fix it.
