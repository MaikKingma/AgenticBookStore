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
- Playwright for rendering evidence: import it by absolute path from another scratch install, do not copy it.
  Wait for `document.title` to update before asserting the title (react-helmet sets it after load).
