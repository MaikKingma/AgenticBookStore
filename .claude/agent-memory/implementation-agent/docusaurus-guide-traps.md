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
  under the worktree Bash guard; call helper scripts by absolute path, never `~`, `$VAR` args or `$?`.
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
- Playwright: the page title sits in `.markdown > header`, so "first element of the article" is the header's next sibling.
- Playwright for rendering evidence: import it by absolute path from another scratch install, do not copy it.
  Wait for `document.title` to update before asserting the title (react-helmet sets it after load).
