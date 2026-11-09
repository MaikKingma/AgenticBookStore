---
name: docusaurus-guide-traps
description: Non-obvious traps when working on the guide/ Docusaurus site and its node:test checker (pinned Node, serve, lockfile word scan, probes)
metadata:
  type: reference
---

- No stack skill exists for Node/Docusaurus; follow the spec folder's strategy.md and the res-1 research file in it.
- Run npm as `env PATH=<pinned node bin>:/usr/bin:/bin npm --prefix "<root>/guide" run <script>`; for long runs and
  probes put the call in a script under `.claude/comms/tmp/` that logs to the scratchpad and echoes `EXIT: n`.
- `docusaurus serve` needs the site dir as an argument (`serve "<root>/guide" --port N --no-open`) because the CWD
  is not guide/. `lsof` lives in /usr/sbin, so add it to PATH in scripts that preflight or stop a port.
- A word-boundary scan for a banned word over `package-lock.json` hits `postcss-lab-function` (a transitive
  dependency of preset-classic). A rule that scans "every text file" must exempt the lockfile.
- `node --test "scripts/**/*.test.mjs"` (quoted glob) discovers tests directly under scripts/ and nested; chain it
  with `&&` before the checker so a failing self-test is never masked.
- Template leftovers to delete after create-docusaurus: blog/, src/pages, src/components, static/img,
  static/.nojekyll, docs tutorials, the template's own .gitignore. The generated package name is derived from the
  path; rename it and run `npm install --package-lock-only` to refresh the lockfile name.
- Theming: Infima dark variables sit on `html[data-theme='dark']`, so a bare `[data-theme='dark']` block loses; use the
  `html[...]` form. Set `--ifm-link-decoration` per `.markdown a`, not globally (underlines the navbar). `.navbar`
  needs `color` set (hamburger icon is currentColor) and its mobile menu needs scoped `--ifm-menu-*` vars. An empty
  Prism theme (`{plain: {}, styles: []}`) lets `--prism-color` / `--prism-background-color` and `.token.*` rules in CSS
  own the code colours. Body background is on `html`, not `body`.
- `node --test` exits 0 with "tests 0" on an unmatched glob and counts an emptied file as one pass; a zero-test guard
  inside `scripts/**` can only read the `check` script's glob and count registrations (package.json is out of scope).
- A README provenance line cannot hold a font CDN host when a rule bans those strings in `guide/`: source fonts from the
  upstream GitHub tag (static woff2) and subset with a scratch-venv fonttools, never a CDN or npm package.
- Checker self-tests that only call rule functions miss the wiring: a dropped `errors` spread or `report(..., [], ...)` in
  `check.mjs`/`lib/site.mjs` stays green. Pin the pair table by name plus a minimum count, and test each rule through
  `runChecks` on a temp tree. A node mutation runner (edit, spawn `node --test`, restore, sha check) beats shell loops
  under the worktree Bash guard; call helper scripts by absolute path, never `~`, `$VAR` args or `$?`. In the
  runner's mutant table a search text holding `${...}` must be a plain quoted string: a String.raw template interpolates it.
  When a rule edit changes a line other mutants search for, adapt those mutants (same mutation) and log it, never drop silently.
- With `future.v4` the heading id form `## Title {#id}` does not compile in MDX (acorn error, also in `.md`); the working
  form is `## Title {/* #id */}`. Docusaurus renders `- [ ]` items as bare disabled checkboxes: axe reports `label`
  (not colour contrast) on `.task-list-item > input`. Fix that held: a local rehype plugin (hast walk, `ariaLabel`
  property) in the preset's `docs.rehypePlugins`; there is no built-in option, no swizzle needed.
- The Pages build HTML is minified with unquoted attributes (`<input type=checkbox disabled/>`), so a built-HTML rule
  needs a quote-aware tag scanner (`scripts/lib/html.mjs`), not a `type="checkbox"` regex. Loading
  `docusaurus.config.ts` from a node:test prints a MODULE_TYPELESS warning (breaks the 0-warning `check` baseline), so
  pin config wiring on the source text instead.
- A checker rule that describes the whole guide (page set, plan comparison) cannot run in `runChecks(root)` by default:
  older tests build minimal temp trees and may only gain lines. Gate it behind an option and set it in `main()` for the
  default root (`--structure` for explicit roots); test it through `runChecks(root, {structure: true, workshopDir})`.
- A "no body H1" rule must cover every form Markdown renders as the page H1: ATX with 0 to 3 spaces of indent and setext
  (`Foo` over `===`), not only `# ` at column 0; fenced lines and 4-space indent stay code. Likewise a box parser that keeps
  only the last fenced block per entry hides a second one: keep all of them and count.
- Page-coverage rules over MDX (manifest in `scripts/coverage.mjs`, scanner `scripts/lib/mdscan.mjs`): scan fences and comments
  in ONE pass (a fence inside `{/* */}` is not code, a comment inside a fence is; JSX comments may have spaces in the braces).
  Leave the at a glance box, the "Level cleared when" checklist and the frontmatter out of the searched text, or the Catch-up
  command and the checklist bullets satisfy entries. Compare after folding whitespace and the whitespace around `[ ] { }`
  (JSON lines break differently), strip blockquote markers, and undo the pack's table escape `\|`.
- Pack facts for a "Windows line must come from the pack" rule: `Get-Command jdtls` is an inline span (S10), the setup
  `$env:...` line is a fenced line with a trailing `# comment`; `mvnw.cmd verify` sits inside a markdown fence of S10, so it is
  no inline span and no allowed PowerShell line. Regexes over report lines must be anchored: "table of contents" matches `/content/`.
- Frozen-file discipline: take `shasum -a 256` of every frozen checker file before the first edit, and apply mutants
  through a runner that restores bytes and compares the sha (scratchpad `s11-t5-impl-mutate.mjs` pattern). Even `sed -i` is
  refused by the worktree shell rules; use Edit.
- MDX scanner traps (T5 rework): a comment opener inside an inline code span (`` `{/*` ``) must not start a comment or every
  later rule is blind, so the scan skips spans (also across a line break inside one paragraph, never past a blank line or a
  fence). Words like "set"/"export" are English in a warning: read command-shaped rules (e) on code only (fenced lines + span
  interiors, see `codeOf`), the assignment/`$env:` forms on the whole text. An "independent sweep" of pack literals must
  count coverage per pack section AND per page, else `/clear` is "covered" by the cheat sheet; a tiny snippet containing `#`
  falsely covers the literal `#`. Reasons for the leftovers are keyed `source:line literal` (pack is frozen).
- Keep ONE inline code span recogniser: the scanner returns span offsets (`scan.spans`) and every reader (comment blanking,
  tag reading via `markupOf`, `codeOf`, the pack allow-list) uses them. Separate span regexes drifted, and a tag inside a span
  (`` `</TabItem>` ``) ended a tab early so a Windows fence escaped rule (c). Raw `<code>`/`<pre>` interiors count as code too.
- A multi-line span is only safe inside ONE paragraph as MDX parses it: stop at every block start (list marker, ATX, setext or
  break, deeper `>`, `|` row, a GFM header row above a delimiter row and the pipe-less rows below it, `<`, `{`, `:::`, fence by
  the main loop's own test) and bound same-line spans in table rows to their cell. Ending early is tolerated, joining is a
  fail-open. Also text in MDX: `\<Tag>`, `\{/*`, `{'...'}` strings (tags and comment openers) and `` \\` `` opens a span.
  Fences after `>`/list markers need container-aware body lines and an implicit end. Check every refactor against the pack
  with a git-object compare (`.claude/comms/tmp/impl-t5-r3-packspans.mjs`), and drop redundant guards: each one survives its mutant.
- Built Tabs HTML (3.10.2, minified): tab `<li role=tab>` without `</li>`, `</p>` dropped before a block, `.token-line` is a
  `div` with a trailing `<br/>`, the PowerShell panel is the hidden second `[role=tabpanel]` of a `.tabs-container`. A tree
  builder needs pop-to-matching end tags plus "new li closes the open li of the same ul/ol" (bounded by the nearest list, or
  tabs inside a list item break). MDX `<pre>` over lines renders `<pre><p>a\nb</pre>`; a blank line inside makes `<p>a<p>b`
  with no newline (text content joins). Only `<` is entity-encoded in code text, `&` and `>` stay raw.
- Evasion classes in built HTML (T5b r2): "invisible characters" means a Unicode property (`/\p{Default_Ignorable_Code_Point}/gu`),
  never a hand list (QA enumerates all of them; the build keeps a raw U+200E in a tab label). Language classes: a fence info string
  without a space keeps its suffix (`powershell{1}` builds `language-powershell{1}`), and raw `<pre><code className=...>` puts the
  class on the code, not the pre; match names with `(?![a-z0-9])`, not `$`, and read the pre's code elements too.
- Probe builds without touching the repo: copy guide/ minus node_modules/build/.docusaurus into the scratchpad, symlink
  node_modules, `npm --prefix <copy> run build` (about 2 s warm). Capture fixtures as the `<article>` slice (ASCII check:
  real pages carry U+200B in hash links, so slice a smaller fragment there).
- The Edit/Write tools turn a ` ` escape in the text into the literal character (ascii rule fails, and Edit cannot
  match it back): write `String.fromCharCode(0xa0)` instead, and fix a stray one by rewriting the file with Write.
  The same holds for any backslash-u escape inside a regex literal (a class of invisible characters): build it with
  `new RegExp` from `String.fromCharCode` codes, and grep every edited file for non-ASCII before the suite.
- An Edit whose old/new string ends in a space mid-line (`const x = `) can lose that space and silently change a
  committed line. End every Edit anchor at a line end, and read `git diff -U0 HEAD -- <test file>` before the gates
  when a test file "may only gain lines": the deletion count must equal the agreed edits.
- Content tasks: five frozen self-tests (check-content, coverage-manifest) pin the REAL docs tree as all-skeleton, so removing
  the first marker turns `npm run check` red while `node scripts/check.mjs` itself stays green. Run `node scripts/check.mjs`
  directly to separate page defects from that pin, and ask the orchestrator early; never edit the frozen tests.
- Docusaurus strips the numeric file prefix from routes (`00-setup-check.md` is `/setup-check`, `06-wrap-up.md` is `/wrap-up`).
- Playwright: the page title sits in `.markdown > header`, so "first element of the article" is the header's next sibling.
- Playwright for rendering evidence: import it by absolute path from another scratch install, do not copy it.
  Wait for `document.title` to update before asserting the title (react-helmet sets it after load).
