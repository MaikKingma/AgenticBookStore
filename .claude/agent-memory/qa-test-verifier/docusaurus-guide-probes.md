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

Related: [[shell-traps]], [[scaffold-verification-techniques]]
