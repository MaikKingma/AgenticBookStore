---
agent: qa-lead
task: qa-11-workshop-guide
status: in-progress
spec: docs/spec-pack/specs/11-workshop-guide-site.md (agent-os spec folder not yet written; tasks from lead-1 strategy-11-workshop-guide.md)
assignment: strategy
updated: 2026-11-08T21:02:31Z
activity: ""
---

# QA ledger: spec 11, workshop guide site

## TL;DR

Strategy delivered (assignment `strategy`). Tiers: T2, T4, T5 critical (opus); T1, T3, T6 to T11 elevated (sonnet).
The checker (T1, T3, T4, T5) is the oracle for every later task, so the bar there is mutation probes on the checker
itself, not only input probes. Content tasks (T6 to T10) are gated by a frozen-checker hash chain, an independent
literal sweep and a semantic source-table review. 14 Gate 1 findings (untestable or ambiguous criteria) with rewrites
below; the most important: T1 creates `guide/build-offline/` before T2 adds its ignore line (G-1), and the admonition
contrast pairs depend on "types used" by later tasks (G-6).
memory: written in worktree

## QA strategy

### Risk tiers and models

| id | tier | qaModel | why |
|---|---|---|---|
| T1 | elevated | sonnet | new pattern: harness, configs, hygiene rules every later task relies on |
| T2 | critical | opus | public side effects (Pages deploy, a public release and tag) behind a gate no in-cycle run can exercise; evidence is a real CI run |
| T3 | elevated | sonnet | user-facing brand, contrast checker module, three engines |
| T4 | critical | opus | the structure checker is the oracle for the "Level page structure" scenario and for T6 to T11 (cross-task contract); parser brittleness |
| T5 | critical | opus | the coverage manifest is the oracle for "Complete level coverage"; self-grading and silent-exclusion risk |
| T6 | elevated | sonnet | user-facing content, external URLs, OS tabs |
| T7 | elevated | sonnet | content; solution-text leakage risk |
| T8 | elevated | sonnet | content |
| T9 | elevated | sonnet | content; permissions and hook JSON that participants copy as a safety net (fidelity = security-relevant) |
| T10 | elevated | sonnet | content; three track tabs, verbatim briefs |
| T11 | elevated | sonnet | marker ban plus the site-wide rendered matrix (3 engines x 2 builds) |

### C0: common directive for every verifier of this spec (in addition to the card, G1 to G9 and the task block)

C0-1 Scratch and evidence. SP = the session scratchpad. Every evidence file is named `s11-tN-qa-r<k>-*` and cited by
absolute path; helper scripts go to `.claude/comms/tmp/qa-tN-s11-*.sh` (spec 12 runs in the same session and shares SP).
Rendered tooling lives in `SP/s11-guide-verify/` with its own Playwright and axe-core install; never modify `res-1-pw/`
or any spec 12 folder. Before changing the shared `check-site.mjs`, copy it to `s11-tN-check-site.mjs` and quote the
`diff` against the previous task's copy. Every round writes its own `s11-tN-qa-r<k>-hashes.txt` with repo-relative
`shasum -a 256` lines only, one per fileScope file present (docs-only rounds included).

C0-2 Snapshot and restore. Nothing is committed before the gate: never ask for or claim an empty `git status`. At
snap-before record `git status --porcelain` plus the hashes of every probe-touched file, and `cp` each one to
`SP/s11-tN-qa-<File>.orig`. Restore every probe by `cp` from the `.orig` copy, then `shasum` and quote the match before
the next probe. Snap-after must equal snap-before. Pack files a probe touches (workshop-plan.md, sdd-framework-guide.md,
level-4-feature-briefs.md, spec 10) get the same treatment, and their hashes before and after are quoted.

C0-3 Toolchain. Every npm and node command runs as `PATH=<pinned bin>:/usr/bin:/bin ...` with
`<pinned bin>` = `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/node-v26.13.0-darwin-arm64/bin`.
Quote `node --version` (v26.13.0) and `npm --version`. Run `npm ci` in `guide/` yourself (never `npm install`, never
`npm audit fix`). Every long command goes through `bash ~/.claude/scripts/with-budget.sh <s> ...` (npm ci 300,
each build 300, referee 600; report the referee duration against the 60 s budget and note spec 12 docker contention
if it is slow).

C0-4 Gates. All exit 0 with the output tail quoted: `npm run check`, `npm run typecheck`, `npm run build`,
`npm run check:built`, `npm run build:offline` in `guide/`, and `./mvnw clean verify` at the root (Tests run totals and
BUILD SUCCESS). Run `typecheck` once more after both builds exist.

C0-5 Warning inventory. Grep `warn|deprecat|experimental|error` (case-insensitive) over the logs of npm ci, check,
typecheck, both builds and check:built. Pre-classified as accepted, each at its exact text and a count the T1 verdict
fixes as baseline: Node 26 `ExperimentalWarning: localStorage is not available` (per build); npm 11.20 notice that
install scripts did not run for `@swc/core`, `core-js`, `fsevents`; the npm audit summary line of `npm ci`; the
sitemap plugin's disabled-in-hash-router warning in `build:offline`. Every other line is classified as fixable here
(FAIL), owned by a named later criterion, or unowned (flag it). A count above the baseline is a finding.

C0-6 Checker tests. Quote the node:test summary (`# tests N`, `# pass N`, `# fail 0`). N must equal the number of
`test(`/`it(` declarations you count by grep in `guide/scripts/**` (dry-run command quoted). One throwaway failing
test file in the discovery pattern the package uses must make `npm run check` exit non-zero (proves discovery and that
a self-test failure is not masked by `;` chaining); remove it and re-hash.

C0-7 Probes must fail for the stated reason. For every falsifier, quote the checker's or the build's failure line
(rule id or message, file, line). A probe that fails for another reason (an MDX compile error, a syntax error in the
checker, a missing import) is no evidence. A mutation probe on checker code (a rule function returning no errors, a
flipped comparison) is applied by you, never by the implementer, and each one is its own run with its own log.

C0-8 Rendered checks. Pages build: `npx docusaurus serve --port 33NN --no-open` (NN = task number, qa-final 3399)
after an `lsof -iTCP:33NN -sTCP:LISTEN` preflight; stop it by PID and show the port free afterwards. Offline build:
`file://` with every non-`file:` request aborted by `context.route`, and the count of aborted and failed requests
recorded (expected 0). Viewport 720 px: `document.documentElement.scrollWidth <= clientWidth` per page, plus one long
`pre` whose own scrollWidth exceeds its clientWidth. Copy buttons equal code blocks (count both in the DOM, hidden
tab panels included). WebKit `file://`: one reload allowed per page, recorded with the page name. Save the script and
its log under C0-1 names.

C0-8a Built-output checks. Grep the built HTML text (not only the source) for the hygiene items: non-ASCII in page
text content, the G6 deny-list, the word lab, raw `<branch>`-style placeholders in `<p>` text.

C0-9 Hard limits. Never dispatch a workflow, set a variable, enable Pages, create a release or a `gh-pages` ref, push,
or run any git write. Never add a dependency.

C0-10 Scope. Product files changed = `git diff --name-only <pr-base>...HEAD` plus the working tree, minus `.claude/`
and `agent-os/`; must be a subset of the task's fileScope. Quote the list.

C0-11 Evidence timing. Prove the gates ran after the implementer finished with `stat` mtimes (impl artifact, last
product file, your gate logs), not frontmatter timestamps.

C0-12 Frozen oracle. From T6 on, `shasum -a 256` of every `guide/scripts/**` file must equal the last gated baseline
(T5's, or T11's own diff limited to the marker ban). Any difference in a content task is a FAIL (G7).

C0-13 Semantic review (content tasks). Build your own `page section -> pack file:lines` table for every H2 section and
every checkpoint; mark each step and claim faithful, embellished or invented. Every `Expected` line of a checkpoint
must be a pack-stated output (res-3 section 5 "stated" rows); the res-3 "Gaps" rows must not appear as outputs.
Claude Code behaviour claims (res-3 C14) are restated as the plan states them, never extended. Facilitator ranges
(WP:81-83, 124-126, 171-173, 334-347; FB:40-45 except the FB:41 hint; SDD Part 1) are absent from the built text:
grep three distinctive phrases per range.

### Task directives (qaDirective, in addition to all of C0)

**T1 (elevated, sonnet).**
1. Config by evaluation, not grep alone: import both configs with Node 26 type stripping from a script in SP run with
   cwd `guide/`, deep-diff the resolved objects and quote the diff; expected only `baseUrl` and
   `future.experimental_router`. If the import cannot run, say why and fall back to grep plus the probes below.
2. Link probes (card 4) plus: a link to a missing `./nope.md` must make `npm run build:offline` exit non-zero too
   (this proves `markdown.hooks.onBrokenMarkdownLinks: 'throw'` survives the spread; the default `warn` would let the
   hash build pass).
3. `future.v4` live: the card-6 space-form probe must render as literal `<p>:::` (quote the HTML line), which only
   happens with v4 on.
4. check:built mutants: put the space-form probe in a nested page (`docs/reference/probe.md`), not only index (kills an
   index-only scan); `build/` with zero `.html` files must fail (anti-vacuity; see Gate 1 G-3).
5. Hygiene rule witnesses (each a run of `npm run check`, quote the message):
   - non-ASCII: em dash from code point 0x2014 (dedicated message), U+00A0 and U+2019 (generic message), each in index;
     an em dash at the last byte of a file without a final newline; an em dash in `README.md`, `sidebars.ts`, one
     `scripts/*.mjs` file and a nested `docs/x/y.md` (proves the walker covers every scanned path);
   - lab: `Lab 1`, `LABS`, `lab.`, `lab-1` fail; `label`, `collaborate`, `syllabus`, `laboratory` pass;
   - dependencies: an extra devDependency key fails (kills a dependencies-only check); a removed template key fails;
     `^3.10.2` on one `@docusaurus/*` package fails (exact pin).
6. Checker mutants: for each hygiene rule, make the rule return no errors; `node --test` must fail. Check the checker
   source itself with `rg -n '[^\x00-\x7F]' guide/scripts` (prints nothing) and `od -c` on the line that builds the
   em dash.
7. Rendered: Pages build GET `/AgenticBookStore/` 200 with the index title; offline `file://` index title in Chromium.
8. `git status --porcelain` after both builds: nothing under `guide/build`, `guide/build-offline`, `guide/node_modules`,
   `guide/.docusaurus` (see G-1). `git check-ignore -v` for one file in each.
9. Template leftovers: rendered navbar and footer have no blog, tutorial or external template link (list hrefs).

**T2 (critical, opus).** Read the qa-ci-config traps first: evidence is the task PR's real run.
1. Bind to the head: `gh pr view <n> --json headRefOid`, then `gh run list --commit <full 40-char sha>`; every
   workflow run for that sha (guide.yml build, ci.yml verify and windows-build) is green. Save `gh run view <id> --json
   jobs,headSha,event,conclusion` unfiltered. Jobs `deploy` and `release` have conclusion `skipped`; no run with event
   `push` exists for the head sha (the push filter names only `factory`).
2. Step order from the run JSON `steps` array, not from reading the YAML: npm ci, check, typecheck, build, check:built,
   build:offline, zip, upload zip, Pages upload (skipped). Quote setup-node's resolved version line (26.13.0) and the
   `check` step's rule lines (anchor log greps on the Actions timestamp prefix `Z `).
3. Artifact: `gh run download <id> -n workshop-guide-offline -D SP/s11-t2-qa-artifact`; `unzip -l` the inner zip:
   `index.html` at the root, no `build-offline/` prefix, `assets/` present; record size.
4. Gate expression by a `python3 -I` byte-compare script (the harness refuses `rg -F` on `${{ }}`): exactly 3
   occurrences of `github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true'`, on the deploy job `if`, the release job
   `if` and the Pages-upload step `if`. Negative controls on SP copies: `== true` unquoted, `||` for `&&`, and one
   occurrence removed, each makes the script report a mismatch.
5. Structure by parsing, not grep: a node script (cwd `guide/`, `js-yaml` from the installed tree, read-only) prints
   per job: permissions, `if`, timeout-minutes, steps with their timeout-minutes, `continue-on-error`, `if`. Check:
   top-level `contents: read`; `pages: write` and `id-token: write` only on deploy; `contents: write` only on release;
   release job has no checkout; Pages upload `path: guide/build` (not affected by `defaults.run`); every job has a
   timeout; npm ci, every `npm run` step and the zip step have a step timeout below their job's; no
   `continue-on-error`, no `always()` anywhere in build.
6. Pins: each `uses:` SHA equals `gh api repos/<o>/<r>/commits/<tag> --jq .sha` for its `# vX.Y.Z` comment; exactly the
   six briefed actions; checkout and upload-artifact identical to ci.yml's lines.
7. actionlint 1.7.12 by digest on guide.yml: exit 0, no output. Also run it on ci.yml and show only the known SC2016.
8. Side effects absent (read-only): `gh api repos/MaikKingma/AgenticBookStore/pages` 404,
   `.../releases/tags/workshop-2026` 404, `.../git/ref/heads/gh-pages` 404, `gh variable list` without
   `GUIDE_DEPLOY`, `.../environments` listed and recorded.
9. Warnings in the CI log: `##[warning]`, the annotations API per job, and `WARN|warn|deprecat` counts compared with
   the local C0-5 baseline; Node 20/24 action-runtime deprecation notes are recorded, not waved.
10. `.gitignore`: `git check-ignore -v guide/build-offline/index.html` names the root rule; the diff is one added line;
    no other ignore rule changed (spec 12 conflict check).
11. Residual by design: the push path (deploy and release on `factory`) cannot run in this cycle; say so in the
    verdict; it is F12. No PASS may claim the deploy works.

**T3 (elevated, sonnet).**
1. Contrast arithmetic: recompute every printed pair with your own script (WCAG 2.2 relative luminance) from the
   resolved hex values; all ratios agree to 2 decimals. Self-test witnesses that must exist (add them as probes if
   absent): white on black 21.00; `#767676` on white passes 4.5 and `#777777` on white fails; one pair within 0.05 of
   3.0 on each side; a 3-digit hex; mixed-case hex; a `var()` chain two levels deep.
2. Contrast mutants (each must make `node --test` fail): drop the `+ 0.05`; swap the R and B coefficients (the blue
   `#3D7DC9` on white witness, 4.2, must catch it); `>` for `>=` at the threshold; resolve `var()` one level only;
   read dark values from `:root` (and light from the dark block).
3. Real-file probes (`npm run check` must fail): set one dark-only text colour to navy; delete one declared variable;
   remove one required pair; add `color: #fff` in a new CSS module; `style={{color: 'white'}}` in an MDX page
   (see G-5); and the four dark-mode switch cases (off plus dark block, on without dark block fail; the other two
   pass). A `disableSwitch: true` inside a comment must not count as set.
4. Colour-literal false positives that must pass: `[x](#add-book-reviews)`, `[x](#bad)`, `[x](#cafe)`, `{#deadbeef}`
   heading id in a doc.
5. Rendered, per engine (Chromium, Firefox, WebKit) and per build (Pages, offline with network aborted):
   `await document.fonts.load('<w> 16px Montserrat')` for 400, 600, 700, then every Montserrat FontFace has status
   `loaded` (never use `document.fonts.check()`: it returns true when no face matches). Negative control: in an SP copy
   of `build-offline/`, delete one woff2 and show that face's status `error`.
6. Rendered colours: computed background of `pre` is snow or mist; navbar background navy with white title; hover the
   navbar link (`locator.hover()`) and compute the hover text/background ratio yourself (>= 4.5); walk all text nodes
   on the index and fail on blue `rgb(61, 125, 201)` text below 24 px (or below 18.66 px bold) and on any sage
   `rgb(128, 159, 162)` text.
7. axe color-contrast zero violations on the index at 720 and 1280 px, light and (if kept) dark, plus once at 720 px
   with the mobile sidebar menu open.
8. The T3 PR's real CI run shows the contrast pair lines in the `check` step log (proves "checked by a script that the
   CI build runs"); the run's offline zip artifact lists `fonts/OFL.txt` (`unzip -l`).
9. Provenance: your `shasum -a 256` of each woff2 matches the provenance file; quote the URL lines.

**T4 (critical, opus).** Lens 4 depth on every criterion.
1. Plan-parser falsifiers on the real pages (each must make `npm run check` fail, quote the message):
   - reword one word that sits on the continuation line of a wrapped plan bullet (L0 "output or the wrong file", L1
     bullet 3) and of a wrapped Goal (L2 and L4 goals wrap in the plan): kills first-line-only parsing;
   - drop the backticks around `CLAUDE.md` in an L1 item: kills markup-stripping normalisation;
   - `- [x]` for `- [ ]` (see G-11); an extra item; the last item removed; an item duplicated;
   - after the list: a heading, an admonition, a `{/* */}` comment, a second list after a blank line;
   - Goal without its final period; Goal truncated to its first plan line;
   - L2 Time set to 8 (the Part A minutes): kills "any `(N min)` in the section"; L2 parses 20 from
     `(LSP + MCP) (20 min)`;
   - at-a-glance lines out of order (Time before Catch-up); Compare added to L0; Compare removed from L3; L3 Compare
     `solution/level-2`; L1 Start branch changed (see G-8);
   - `LevelProgress` inside the admonition; a paragraph before it; `current={2}` on L3 (card).
2. Plan-file falsifiers (edit the pack under C0-2, restore by cp, hashes quoted): a sixth `**Level cleared when**`
   block makes the count guard fail; one word changed in the SDD guide's L4 list fails with a message naming the SDD
   guide; the plan renamed away fails with a message containing the path. Note that the plan's line 13 quotes
   "Level cleared when" inside a table: the parser must not count it.
3. cwd independence (G-9): `npm --prefix guide run check` from the repo root exits 0 on the real pages.
4. Marker scope: with the marker present, a swapped checklist item still fails; with it present, a missing sync
   moment passes; without it, zero and two sync moments both fail.
5. Admonition rule witnesses: `:::info Sync moment` and `:::tip Checkpoint` fail; a Checkpoint without a fenced block
   fails; one without a line starting `Expected` fails; the sync phrase misspelled fails.
6. H1 rule: a body `# Foo` fails; a fenced bash block holding `# comment` lines passes (the setup page needs this).
7. Files and routes: a twelfth `docs/extra.md` makes the file rule fail; the build emits exactly the eleven route
   HTML files (list `build/**/*.html` minus `404.html`); rendered sidebar order read from the DOM in both builds.
8. Anchors: one temporary probe page linking every contractual anchor (13 level-page anchors and 8 troubleshooting
   anchors) builds; removing one anchor makes `npm run build` fail.
9. Component: `current={5}`, `current={-1}` and `current={1.5}` each make `npm run build` fail (kills a missing lower
   bound, `> 5` and a range-only check); `current={4}` builds. Rendered on all five pages in both builds: `nav` with an
   accessible name, `ol` of five, exactly one `aria-current="step"` on N, its label font-weight >= 600 and the others
   lower, current fill teal (light) or mist (dark) by computed style, label contrast >= 4.5 computed by you; axe on one
   level page in each mode.
10. Checker mutants (each `node --test` must fail): the checklist comparator ignores order; the count guard removed;
    the Compare-level set widened to 0 to 4; the marker exemption applied to structure rules.

**T5 (critical, opus).** Lens 4 depth on every criterion. Mandatory qa-lead checkpoint after this task.
1. Independent literal sweep (the main guard against silent exclusion): with your own script, extract every inline
   code span and every fenced-block line from WP lines 43-323, SDD 91-142 and FB 6-36; drop the facilitator ranges of
   res-3 section 3; diff the result against the manifest snippets and `manualRows`. Every leftover literal is listed
   with your judgement (missing entry = finding). Then tick every res-3 section 9 row.
2. Section scoping: an entry whose snippet exists in the pack but in another section (an L1 command attributed to the
   L3 section) must fail rule (a). An entry whose page snippet is on another page must fail rule (b).
3. Rule (b) gaming witnesses on a non-skeleton probe page: a command snippet only in prose inline code fails; only
   inside `{/* */}` or `<!-- -->` fails; inside a fenced block inside a `<TabItem>` passes.
4. Rule (c) witnesses (see G-12): a pack line with an appended `; Remove-Item -Recurse x` fails (kills substring
   matching); `Remove-Item -Recurse foo` fails (card); the pack's `$env:BOOKSTORE_AI_API_KEY="<key on screen>"` passes.
5. Rule (d) witnesses: `Facilitator`, `spec 10`, `Spec 09`, `.claude/comms` fail; `.claude/rules/domain.md`, `your
   spec`, `spec artefacts` pass.
6. Rule (e) witnesses on the setup page: `export ANTHROPIC_API_KEY=x`, `ANTHROPIC_API_KEY=x claude`,
   `$env:ANTHROPIC_API_KEY="x"`, `setx ANTHROPIC_API_KEY x`, `"ANTHROPIC_API_KEY": "x"` (settings `env` form) each fail;
   the bare token in the setup warning passes; the bare token on L2 fails.
7. Rule (f) `<img`, `<IMG` fail. Rule (g): unknown label, unknown groupId, `Tabs` without groupId, a group missing one
   of its labels each fail. Rule (h): two card rows fail; a rendered table without the fenced `markdown` block fails;
   a missing `notes/level-0.md` instruction fails.
8. Snippet quality: list every snippet shorter than 8 characters and every snippet that matches more than one pack
   section; judge each (a trivially matching snippet grades nothing).
9. Checker mutants (each `node --test` must fail): rule (a) searches the whole file; rule (b) ignores `kind`; rule
   (c) uses `includes`; the deny-list made case-sensitive.
10. Quote the "anchored N/N" line and the pending list per page; N equals the manifest entry count you count.

**T6 (elevated, sonnet).** In addition to C0-12 and C0-13:
1. Coverage: the check shows IX, SU and WU non-pending and green, all other pages still pending.
2. URLs: each external URL returns 200 for a GET following redirects (`curl -sL -o /dev/null -w '%{http_code}'`);
   HEAD refusals are recorded, not failures (see G-13).
3. Tabs only where the pack states both forms: list every TabItem with its pack line; no Windows tab on the clone
   line.
4. No participant email mentioned; no "under a minute" promise beyond the plan's wording; `<key on screen>` only in
   code.
5. Wrap-up links to each `#level-cleared-when` resolve (the build proves it; quote one rendered href per level).

**T7 (elevated, sonnet).**
1. Solution leakage, mechanical (see G-14): no trimmed line of spec 10 lines 25-109 longer than 20 characters occurs
   in the built L0 or L1 page text, unless the same line also occurs in WP sections 4 and 5.
2. L0 task text: byte-compare the blockquote with WP:97-98 after whitespace normalisation.
3. Expected findings absent: `ddl-auto=validate`, "`mvn` instead of", "all tests pass" do not occur on L0.
4. Observation card: copy button on the fenced `markdown` block (rendered), header and hint text exact.
5. Exactly one sync moment per page, rendered as an info admonition titled "Sync moment".

**T8 (elevated, sonnet).**
1. `git ls-files distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java` prints the path; the
   four tool names on the page match the `@McpTool` names in that file.
2. Every checkpoint `Expected` traced to WP:198, 208, 210, 219 or S10:174, 202, 209.
3. `$PATH`, quotes and the `claude --debug` error text render as code, not as broken MDX (rendered text compare).
4. The plugin-declaration sentence (WP:193) is absent or marked as pending (F4); no `.mcp.json` catch-up behaviour
   is described.

**T9 (elevated, sonnet).**
1. Permissions and hooks JSON: line-by-line equality with WP:236-245 and WP:250-259 after trailing-whitespace trim;
   if both sides parse as JSON, also compare the parsed objects. A participant copies these as a safety net: any
   difference in a deny rule is a FAIL.
2. Quoted bash script lines equal spec 10 lines 273-295 (and 297-314 for the optional Stop hook) line by line; each
   is anchored by a manifest entry.
3. No PowerShell script text: no `.ps1` body, `param(`, `$LASTEXITCODE`, `Write-Error`; only the pointer to
   `reference/troubleshooting#windows-hooks`.
4. The Stop hook is presented as optional; the JSON fragment is not presented as a complete file.
5. `$ARGUMENTS` and `` !`git status --short` `` render as code (rendered text compare).

**T10 (elevated, sonnet).**
1. Track tabs: three tabs with the grammar labels; clicking a track in one tab group switches the others on the page;
   the code inside every tab has a copy button (hidden panels counted).
2. Briefs A, B and C equal FB:6-36 after whitespace normalisation, questions included; the FB:41 hint present; the
   string `solution/level-4` absent from source and built text (see G-14); FB:43-45 phrases absent.
3. `openspec` terminal commands and `/opsx:` Claude Code commands appear in the context the SDD guide gives them
   (per step, with the SDD line).
4. No OpenSpec CLI install step invented on the track branch (F8).

**T11 (elevated, sonnet).**
1. Marker ban: the ban and the skeleton exemption call one shared predicate (read and quote it); reinserting the
   marker on one page fails; a spacing variant (`{/*guide-skeleton*/}`) is treated the same way by both.
2. `guide/scripts/**` diff against the T5 baseline (or the last gated one) contains only the ban and its tests.
3. Cheat sheet print: see G-10 rewrite.
4. Site-wide matrix on all eleven pages: Pages build and the T11 PR run's CI zip artifact extracted into a path with
   spaces; Chromium, Firefox, WebKit; network aborted offline; 720 px; copy buttons; axe color-contrast both modes at
   720 and 1280 px; LevelProgress on the five level pages; one result line per page x engine x build.
5. Troubleshooting: every command on the page traced to a pack line; no invented unset command; the eight anchors
   present; the Safari blank-page line present.
6. Architecture: no "enforced package list" claim; every rule and suffix traced to AS lines.
7. Regression: every earlier page still green in check and in the rendered matrix.

### Gate 1 findings: untestable or ambiguous criteria, with rewrites

- G-1 (T1 AC9 vs T2): T1 runs `npm run build:offline`, but `guide/build-offline/` is ignored only from T2 on, and T1
  deletes the template's `guide/.gitignore`. Rewrite: move the root `.gitignore` line `guide/build-offline/` into T1's
  fileScope (T2 then only verifies it), or T1 AC9 adds "the implementer removes `guide/build-offline/` before handoff
  and the verifier shows `git status --porcelain` without it".
- G-2 (T1 AC10, every task's "no file outside X changed"): the plan.md `pr:` bookkeeping commit breaks it. Rewrite:
  "no product file outside <fileScope> (diff minus `.claude/` and `agent-os/`)".
- G-3 (T1 AC6): add "fails when `build/` holds no `.html` file" (anti-vacuity), and "scans `build/**/*.html`
  recursively (a nested page is probed)".
- G-4 (T1 AC5): the scan scope is unstated, and the lab rule collides with the checker's own sources. Rewrite: "The
  ASCII rule scans every text file under `guide/` except `node_modules`, `build`, `build-offline`, `.docusaurus` and
  font binaries, `scripts/**` included. The lab rule scans the same set except `scripts/**`. The checker builds its
  forbidden characters from code points."
- G-5 (T3 AC2): "other colour function" leaves CSS named colours open, though the spec bans any hard-coded colour
  value. Rewrite: "no hex, colour function (`rgb`, `rgba`, `hsl`, `hsla`, `hwb`, `lab`, `lch`, `oklab`, `oklch`,
  `color`, `color-mix`) or CSS named colour in a property value of any `.css` file other than `custom.css`, nor in a
  `style` prop of `.tsx`/`.mdx`/`.md` files; `inherit`, `currentColor`, `transparent` allowed; Markdown anchors and
  heading ids are not colour values (self-tests)."
- G-6 (T3 AC3): "admonition text for each admonition type used" depends on later tasks: a `:::warning` added in T9
  would have no declared pair and no rule notices. Rewrite: "T3 declares contrast pairs for every Infima admonition
  type (note, tip, info, warning, danger, and caution as an alias)", or T4 adds a rule that the docs use only the
  types with declared pairs.
- G-7 (T3 AC6): `document.fonts` "reports loaded" is undefined and `document.fonts.check()` is vacuous. Rewrite:
  "after `document.fonts.load('<w> 16px Montserrat')` for 400, 600 and 700, each Montserrat FontFace has status
  `loaded`; a build copy with one woff2 deleted shows that face `error`".
- G-8 (T4 AC4): the Start branch value is not checked although the header table settles it. Add: "Start branch text
  equals the header table value".
- G-9 (T4 AC5): "read from `../docs/spec-pack/...`" is cwd-dependent. Rewrite: "resolved relative to the checker file
  (`import.meta.url`), so `npm --prefix guide run check` from the repo root also passes".
- G-10 (T11 AC2): "the second page only for the Level 4 table" has no check. Rewrite: "Chromium `page.pdf({format:
  'A4'})` of the cheat sheet has at most 2 pages, and with the Level 4 section hidden by an injected `display: none`
  it has exactly 1 page; both counts recorded".
- G-11 (T4 AC5): the checklist item form is open. Rewrite: "each item is `- [ ] ` followed by the plan bullet text
  with its Markdown kept (backticks, emphasis); `- [x]` fails".
- G-12 (T5 rule c): "exists in the pack" is undefined. Rewrite: "each trimmed line of a Windows PowerShell or
  PowerShell tab code block equals, as a whole, a trimmed pack fenced-block line (its trailing `# comment` stripped)
  or a pack inline code span; substring matches do not count".
- G-13 (T6 AC2): `curl -sIL` sends HEAD, which some hosts refuse. Rewrite: "returns 200 for a GET following redirects".
- G-14 (T7 AC3, T10 AC3): "compares distinctive lines" and "not linked" are open. Rewrite T7: "no trimmed line of
  spec 10 lines 25-109 longer than 20 characters occurs in the built page text unless it also occurs in WP sections 4
  and 5". Rewrite T10: "the string `solution/level-4` occurs neither in the page source nor in the built text".

Not testable in this cycle (Gate 2 residuals, owner): the spec's "Deployed automatically" push path and the DoD items
"site live, offline zip available, every command executed once on a clean machine" (F1, F12). In-cycle evidence is
the static gate, the skipped jobs on the PR runs and the absence of every public side effect.

### qa-final focus

1. Fresh tree: `git archive <tip>` extracted into SP; npm ci, all five gates and the referee there.
2. Integration integrity: each task's `s11-tN-qa-r<k>-hashes.txt` baseline against committed bytes at the tip; the
   union of baseline paths equals the product files of the spec diff.
3. CI on the tip: `gh run list --commit <full sha>`; guide.yml build and ci.yml green; deploy and release skipped;
   the tip run's zip artifact extracted into a path with spaces and run through the three-engine offline matrix.
4. Cross-page: every internal link and anchor (build), tab group sync across pages, LevelProgress on all five pages,
   axe both modes on all eleven pages, admonition types used vs declared contrast pairs.
5. Pack drift: if `factory` moved (spec 10 or 12 edits), merge first, then `npm run check` against the merged pack;
   `.gitignore` merged cleanly with spec 12's lines.
6. Side effects absent: Pages 404, no release `workshop-2026`, no `gh-pages`, no `GUIDE_DEPLOY`.
7. Workflow file hash equal to T2's gated hash; gate string count 3 at the tip.
8. Whole-site semantic sweep on built text: deny-list, facilitator phrases, `ANTHROPIC_API_KEY` forms, solution-text
   leakage, `solution/level-4`.
9. Follow-up list F1 to F13 plus every content outbox follow-up recorded in the spec folder.

### Follow-ups outside this cycle's QA scope

All of F1 to F13 run after `spec-10-done` and are not gated here. In-cycle QA still checks their negative side: no
invented Windows variant (F9, F2), no PowerShell hook text (F2), no plugin-declaration claim (F4), no "under a minute"
promise beyond the plan (F11), no participant-email claim (F13), no public side effect (F12), only Playwright WebKit
for Safari (F10, recorded as residual), branch links not resolved (F7, F3, F5, F8).

## Review log

(none yet)

## Traceability matrices

(none yet)

## Cross-task patterns

(none yet; checkpoints after T5 manifest review and after T7)

## Open questions

- none

## Resume point

Strategy delivered. Next assignment: `review T1` when the T1 verifier verdict arrives.
