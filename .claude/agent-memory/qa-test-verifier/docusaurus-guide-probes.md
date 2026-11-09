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

Rule: re-verify a pinned table with generated per-line removal probes plus a "lower the pin with the removal" control.

**Why:** a count pin alone is satisfied by swapping or editing an entry; a name pin alone misses entries nobody listed. A probes file that reads the table source and emits one remove-this-line probe per entry (53 probes ran in 8 s) shows both layers; the control (remove an entry and lower the minimum in the test) must still fail through the by-name test.
**How to apply:** also mutate entries' fg/bg/kind (not only remove them): non-pinned entries survive content edits, report that as an observation. After a probe run `diff -r` against the snapshot can show `Only in ...: components`: the runner's `mkdirSync` leaves an empty dir after the file is removed, `rmdir` it, re-check, then run the final gates.
- Regex block-comment stripping (`/\/\*[\s\S]*?\*\//`) over a test file undercounts when a string literal holds `/*` (a title `scripts/**` swallows tests up to the next `*/`): compare the guard's count with a naive line count per file. Direction is safe (never a vacuous pass) but the printed "N test cases" is only a lower bound.

Rule: for a Markdown/MDX structure checker, probe the syntax variants the renderer accepts, then confirm survivors in the built HTML.

**Why:** a rule written for the common form misses equivalent forms that render the same. A column-0 `^#` H1 rule passed `   # Foo` and setext `Foo` / `===`, and Docusaurus then made "Foo" the page `<h1>` in place of the frontmatter title. A box parser that attached "any fenced block to the current label" accepted a bogus extra catch-up block, which rendered.
**How to apply:**
- Witness set: ATX with 1 to 3 leading spaces, setext `===`, four-colon `::::type Title`, a second fenced block per label, a fence under a label that should have none, text after a label that should be bare, CRLF, the marker inside a fence. Check each survivor in a probe build (`docusaurus build --out-dir <SP dir>`) before calling it a defect: `check:built` caught the four-colon form, so that one was covered by the gate chain.
- Declarative tables (anchor ids, header values): one removal mutant per entry. The value edits died against an independent fixture copy, but the anchor-id removals mostly survived, because fixtures that contain every anchor never show a missing requirement.
- Explicit heading ids: when the auto slug equals the id (`## Stretch {/* #stretch */}`), removing the id keeps a link-probe build green. A build-level "remove one anchor" probe needs an id whose slug differs (`I am far behind` -> `#far-behind`).
- The offline config (hash router) renders no route at build time. A component that throws on bad props, and a broken anchor, both pass `build:offline`. Only the Pages build guards them.
- Built HTML is minified (`aria-current=step` without quotes), and Prism splits code into token spans, so grep for a word, not a whole command line.
- Never run `npm --prefix guide exec -- node ...`: npm resolves `node` as a registry package (it fetched node@27 alpha) and runs 0 tests. Run node from a script that `cd`s into guide/.
- Summaries: `grep ... | paste - -` pairs each result with the NEXT probe header. Have the runner print `id exit` on one line instead.
- On `file://`, axe injection triggers Docusaurus preload console errors in Chromium and WebKit. Tag console messages with the current step and replay fresh loads and a real hover and click before blaming the app.
- Markdown H1 rules (ATX plus setext) never see a JSX `<h1>` in MDX: it passes and renders a second H1 next to the title. Probe it and route it to the rendered "exactly one H1" check.
- Mutants for a reworked "one fenced block per box line" rule: first-block-wins vs last-block-wins is equivalent while a count check exists; also try a label-specific exemption per label (tests often cover 3 of 4 labels) and a quantifier tightening (`=+` to `={3,}`, tests rarely use a single `=`). A `!flags[index - 1]` mutant on a setext rule is equivalent: a flagged `===` line always has a flagged predecessor.

Rule: on a re-verification, prove carry-forward by tree ids and extracted artifacts, not by zip digests.

**Why:** two CI runs over byte-identical inputs give the offline zip a different `digest` (zip timestamps), so the digest alone suggests a change that is not there.
**How to apply:** `git rev-parse <r1>:guide/docs` against `HEAD:guide/docs` (and src, static) for rendered inputs; `gh run download <id> -n <name> -D <fresh dir>` for each run, `unzip` each into its own fresh dir, then `diff -rq` the two trees. Re-adapt mutant strings that the rework changed before re-running a mutant file (a stale `from` string shows as SETUP ERROR, not as a kill).

Related: [[shell-traps]], [[scaffold-verification-techniques]]
