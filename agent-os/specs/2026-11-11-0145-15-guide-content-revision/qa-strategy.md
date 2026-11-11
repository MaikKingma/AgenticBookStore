---
agent: qa-lead
task: qa-15-guide-content-revision
status: in-progress
spec: (not yet written) agent-os/specs/<ts>-15-guide-content-revision/ ; pack docs/spec-pack/specs/15-guide-content-revision.md (D19)
assignment: strategy (delivered); next: review <task-id> per verifier verdict
updated: 2026-11-11T01:45:30Z
activity: ""
---

## TL;DR

QA strategy for spec 15 delivered (section "QA strategy" below). Tiers: T1 critical (it rewrites the oracle: structure
ban, deny-list, coverage retargets, sweep keys, fixtures), T5 critical (new rehype plugin over every table in both builds,
new built rule, frozen config pin edit, A4 removal with no CI browser step), T2/T3/T4 elevated, T6 standard. qaModel sonnet
for every task (owner policy); the critical tasks are compensated by mutant tables with expected messages in the directive
and lens 4 on every criterion in my reviews. Gate 1 findings G-1..G-9 (one is a real sequencing conflict: G-1, the T1 prose
ban vs the Level 2 demo text that only T3 removes). Untestable criteria U1..U12 with rewrites; criteria to add A1..A12.
Checker baseline for T1 = spec 14 `checker-baseline-hashes.txt`, verified 99/99 OK on this tree (7ded994) by me.
memory: written in worktree (two existing topic files extended, no new lines).

## QA strategy

### Inputs read

Orchestrator ledger `run-15-guide-content-revision.md` (Scope & conventions, Shape draft T1..T6 + C, Decisions log), pack spec
`docs/spec-pack/specs/15-guide-content-revision.md`, res-1..res-4 outboxes, spec 14 `plan.md` (A4, R5, R6, FF) and
`qa-strategy.md` (C0), fix-11 `plan.md` (A1/A2: a pack wording fix that had to add `SWEEP_EXCLUDED` keys), checker sources
(`rules/structure.mjs` admonition parser and `commonErrors`, `rules/content.mjs` DENY, `rules/coverage.mjs`,
`coverage-manifest.test.mjs` floors/sweep/SU-21 test, `task-list-labels.test.mjs` config pin, `check-built.mjs`,
`src/plugins/rehype-task-list-labels.mjs`), `guide/docs/index.md`, `00-setup-check.md`, `05-level-4...mdx`,
`reference/architecture.md`, `workshop-plan.md` 10-60, 140-205, 340-345, the spec 14 rig folder in the session scratchpad.

Own checks run (read-only): `shasum -a 256 -c agent-os/specs/2026-11-10-1610-14-guide-branding/checker-baseline-hashes.txt`
from the worktree root: 99 lines OK, equal to `git ls-files guide/scripts | wc -l` = 99. So the spec 14 tip baseline is the
spec 15 base baseline. The spec 14 sweep script `SP/s14-guide-verify/s14-final-qa-r1-sweep.mjs:269` holds the A4 filter
(`expected = (v) => v.id === 'scrollable-region-focusable' && width === 390 && [...]`): that line is the "exclusion" to drop.

### Risk tiers and models

| id | tier | qaModel | why |
|---|---|---|---|
| T1 | critical | sonnet | rewrites the oracle every later task is graded by: removes a pinned structure rule and replaces it with a ban, adds deny-list terms, retargets 5 coverage entries, renumbers line-keyed `SWEEP_EXCLUDED`, edits fixtures and 4+ test files, touches 7 pages and the pack; a wrong retarget silently weakens coverage for the rest of the spec |
| T2 | elevated | sonnet | new content shapes (a whole rule file in a fence, an ASCII mock) mirrored into the pack, new sweep literals, byte-equality claim against a branch; mock fidelity cannot be checked by agents (D14) |
| T3 | elevated | sonnet | first raw `<details>` JSX on a page (MDX compile, rendered open/close in both builds, summary contrast that no pair covers), L2-17 retarget, pack 200-202/343/59 edits |
| T4 | elevated | sonnet | PF-T11-1 is a Track A blocker: a wrong pin (`OPENSPEC_VERSION` on `curl`) installs `latest` silently; PowerShell lines hit rule (c); SU-21 semantics change; truth claims about three remote branches. Lens 4 on the install criterion |
| T5 | critical | sonnet | new rehype plugin over every article table in both builds, new built rule (oracle), edit of a frozen config pin, a11y claim (A4 removal) whose only proof is a local rig (CI has no browser step) |
| T6 | standard | sonnet | short tips on four pages plus pack and coverage entries; pattern-following; the risk is semantic (filler, hidden required steps) |

Owner model policy is fixed (impl and verifiers sonnet). Compensation for T1 and T5: the directive names each mutant, its
edit and the expected failing test or rule message; verifiers report a mutant table (id `s15-T<N>-M<k>`, file, edit,
expected, observed message, restore hash); my reviews apply lens 4 to every criterion of T1 and T5. A critical task that
FAILs twice goes to escalation, not a third identical round.

### Audit of the shaping decisions

- Tech lead skipped: agree (one module, pack gives the split).
- "Coverage floors never go down; a removed entry is replaced by one pinning the replacement text": agree and tighten (A3):
  today every floor equals the live count (res-1, node count), so after each task `floor[page] === live count`, never only `>=`.
- WP:N row labels stay opaque ids: agree. Consequence: `SECTION_9_ROWS` keeps 74 rows and both `74` asserts stay unchanged
  (retargeted entries keep their `row`).
- Part split L2 10/8, L3 4/14/10: agree. Nothing pins Part minutes or the Format column: A2 adds IX entries for the five
  Format cells, the arithmetic is a verifier census.
- Demo text in T3, sync moments in T1: agree, but it creates G-1.
- plan:173 "projector machine", plan:340 "for demos", spec 11 pack lines, README.md:15, spec 10:168 JDK 17: owner residuals; T1/T3
  must leave them byte-identical (verified by `git diff -U0` hunks).
- res-3 (a) Agent OS order per the site (discover, inject, plan-product, shape): agree, residual "pack bullet order differs".
  (b) Track C says "written for this repo and indexed": agree. (c) `/shape-spec` typed form unchanged: agree. (e) install
  script never executed: accepted with the evidence rewrite U10.
- res-4: rehype plugin over all article tables, frozen pin updated with reason: agree, with the MDX witnesses W3..W5 (T5).
  RA-1..RA-4 out (item 9 condition false: spec 15 edits neither `brand.test.mjs` nor `level-progress.test.mjs`): agree,
  they stay owner residuals.
- JDK 21 in T3 (plan:59): agree.

### Gate 1 findings (contradictions, sequencing, under-guarded criteria)

- **G-1 (T1 vs T3, sequencing).** T1 adds the prose deny-list, but the "watch the demo" sentences (`03-level-2...mdx:107`,
  `reference/troubleshooting.md:50`) stay until T3. A T1 deny term `watch the demo` turns `npm run check` red at T1. Rewrite:
  T1 bans `sync moment`, `maik demos` (or `demos this now`) and `live demo` (index.md:17 goes in T1); T3 adds `watch the demo`
  after removing both sentences. T3's fileScope gains `guide/scripts/rules/content.mjs` and `guide/scripts/content.test.mjs`
  (DENY row plus its term-loop entry only). T1 summary says "index.md timetable/line 49": line 17 ("Each level opens with a
  short live demo") is T1 too (Decisions log res-1 lists 17/25-30/49).
- **G-2 (all plan-editing tasks: the literal sweep grows).** `coverage-manifest.test.mjs` sweeps every inline code span and every
  fenced line of plan sections 3 to 9: a new fence (T2 rule file, T2 mock if mirrored, T3 prompt, T4 install lines and
  brownfield commands) or a new inline span adds sweep literals that fail the equality test. Fix-11 A2 needed an amendment for
  exactly this. Rule for every task: each new literal is covered by a manifest entry of its own section and page (one
  `kind: 'command'` entry whose snippet is the whole block covers every line of that block); a new `SWEEP_EXCLUDED` key is
  allowed only with a reason naming the pin that holds the literal on its page, plus a deletion mutant of that pin; renumbered
  keys keep their literal and reason byte-identical.
- **G-3 (T4, SU-21).** The new Node sentence can keep the words "Node.js is not needed", so SU-21 passes unchanged while pinning
  nothing of the correction. Rewrite: SU-21 stays (it still guards "the setup page does not demand Node for the build"), and
  a new SU entry pins the qualification (Node.js 22.12 or later only for the npm route); the base sentence alone must fail the
  new entry, and the existing negative ("You need Node.js 22 installed.") still fails SU-21.
- **G-4 (T4, wrong-claim residue).** `05-level-4...mdx:32` ("framework installed") and `workshop-plan.md:19` ("Prepared start
  branches ... remove the install and init time") are false for Track A, whose branch ships no CLI. A7: T4 corrects both or
  they are logged as residuals; the Level 4 page must not claim an installed `openspec` CLI.
- **G-5 (T5, offline build unchecked).** `check-built.mjs` gates site rules to the default `build/` dir; CI runs no built rule
  on `build-offline`. The plugin reaches the offline build only through the config spread. A6: the verifier runs the
  table-region rule on `build-offline` too (CLI with an explicit dir if the rule is ungated, or a one-off import of the rule
  function), with the same table count.
- **G-6 (T1..T4 rendered runs, A4 still present).** Until T5 the A4 hit is pre-existing. No s15 rig script may contain an
  exclusion filter; T1..T4 report every violation and classify `scrollable-region-focusable` only if it is identical to the
  base hit (page, engine, target selector) on exactly level-4, reference/architecture, reference/cheat-sheet. Any other page,
  or a new node, is a finding. From T5 on: zero, unclassified.
- **G-7 (T2, Level 1 checklist premise).** Plan and page checklist say "`/context` shows the path-scoped rules loading only
  for matching files"; the docs say `/memory` lists loaded rule files and are silent on `/context` (res-2). Not T2's scope
  (checklists are pinned to the plan); owner residual / rehearsal row. T2's mock must not add a second unverified claim as fact
  (a rule file under "Memory files" is fine only under the "illustrative" sentence).
- **G-8 (T5, plugin dependency).** "No new npm dependency" plus the existing plugin's comment "Node built-ins only, no
  unist-util-visit": the new plugin imports nothing from `node_modules` (grep its import lines), `package.json` and the lockfile
  hash-equal to the spec base. A hand-rolled walk is exactly where MDX nodes get dropped: W3..W5.
- **G-9 (T1, fixtures).** `guide/scripts/fixtures/skeleton-docs/**` hold no sync text (res-1, res-4); the ban covers skeletons.
  They stay hash-equal unless a listed reason; `lib/structure-fixtures.mjs` loses the plan fixture line 145 and the
  non-skeleton box branch 220-222 (no dead `if (!skeleton) {}` left).

### Untestable or under-specified criteria, with testable rewrites

- **U1 (spec item 1, "facilitators walk around; no facilitator demo step").** Rewrite: no page under `guide/docs` contains a
  sentence that tells participants to watch or look up for a demo (deny terms `sync moment`, `maik demos`, `live demo` from T1,
  `watch the demo` from T3); `index.md` states once that Maik and Tom walk around and you can put your hand up, pinned by an IX
  entry to its plan twin; no page uses "facilitator".
- **U2 (DoD, "built pages show no sync moment").** Rewrite: `grep -rli 'sync moment' build build-offline` prints nothing
  (HTML and JS chunks), with the positive control that the same grep for `Level cleared when` prints files; and on the 11 pages
  in the rig no admonition title (`.theme-admonition` heading text) matches `/sync\s*moment/i`.
- **U3 (T2, "faithful text mock").** No doc shows `/context` output (res-2), so fidelity is unverifiable by agents (D14).
  Rewrite: the page states in one sentence that the grid is an illustration, not a screenshot, and that symbols, names and
  numbers differ (pinned by an L1 entry); the mock is ASCII in a `text` or language-less fence; its numbers are internally
  consistent (cells per symbol sum to the grid size, percentages sum to 100 within 0.1, used = sum of non-free categories,
  stated total equals the window); every category label maps to a docs quote or is covered by the illustrative sentence; owner
  rehearsal row "compare the mock with a real `/context` (macOS)".
- **U4 (T2, "short annotations on what to watch for").** Rewrite: 3 to 5 annotations, each at most 30 words, which together
  say: messages and tool results grow each turn; free space shrinks; `/clear` between unrelated tasks; `/compact focus on <topic>`
  when the same task drifts; each claim traced to a docs quote (context-window, commands pages).
- **U5 (T2, "one sentence on what the header does and when the rule loads").** Rewrite: one sentence (or two at most) that says
  the header is YAML frontmatter, `paths` lists globs relative to the repository root, and the rule loads when Claude reads or
  edits a matching file, not at session start; each clause matches the memory-page quote (res-2 A1); no claim the docs do not
  make (for example "loads when you open the folder").
- **U6 (T3, "ready-to-paste prompt").** Rewrite: one fenced block (copy button), ASCII, no `<placeholder>` the participant must
  edit, read-only wording ("without changing anything"), asks for `which jdtls`, `java -version`, the plugin's enabled state in
  `.claude/settings.json` and `~/.claude/settings.json`, and the most likely cause; a coverage entry of kind `command` pins it
  in a fence.
- **U7 (T3, "collapsible section").** Rewrite: built HTML has exactly one `details` in Part A step 5 with no `open` attribute;
  in the rig the prompt is not visible before the summary is clicked and visible after, and Enter on the focused summary opens
  it, in both builds and three engines; the Part B fallback sentence sits outside the `details` and is visible while closed.
- **U8 (T4, "short note ... with the documented first steps in docs order").** Rewrite: per track a note of at most 8 lines
  and 120 words whose ordered command list equals: OpenSpec `openspec init`, (optional) `openspec/config.yaml` `context:`,
  `/opsx:explore`, `/opsx:propose`, `/opsx:apply`, `/opsx:archive`; Spec Kit `specify init --here --force --integration claude`,
  `/speckit-constitution`, `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks` (`/speckit-analyze` allowed
  after tasks), `/speckit-implement` (`/speckit-converge` allowed last); Agent OS `/discover-standards`, `/inject-standards`,
  `/plan-product`, `/shape-spec` in plan mode (`/index-standards` only "after hand edits"). Each step cites a doc URL at the
  pinned version in the spec folder (OpenSpec v1.16.0, Spec Kit v1.3.1, Agent OS main with read date).
- **U9 (T4, "what the start branch already holds").** Rewrite: every claim maps to a path listed by `git ls-tree -r --name-only
  origin/start/level-4-<track>` whose ref equals `git ls-remote origin refs/heads/start/level-4-<track>`; Track C says "written
  for this repo and indexed", never "discovered".
- **U10 (spec item 7, "a working, pinned install route").** Agents may not run the install script (classifier refusal; owner
  constraint). Rewrite: (a) the release `v1.16.0` exists with the platform assets and `SHA256SUMS` (`gh release view`); (b) the
  downloaded (never executed) `install.sh` and `install.ps1` read `OPENSPEC_VERSION` on the `sh` / `$env:` side and verify the
  checksum (quote the lines); (c) `npm view @fission-ai/openspec@1.16.0 version engines` gives `1.16.0` and `>=22.12.0`;
  (d) optional, if the harness allows: npm route installed into a `mktemp` prefix with `HOME` set to it and
  `OPENSPEC_TELEMETRY=0`, `openspec --version` prints `1.16.0` (a refusal is recorded once, never retried, not a FAIL);
  (e) residual: the script routes are untested on participant machines (rehearsal, macOS only per D8).
- **U11 (T5, "visible focus ring").** Rewrite: on keyboard focus of each region, computed `outline-style` is not `none`, its
  colour equals the resolved existing focus variable in that mode, contrast against the page background at least 3:1
  (computed), the ring band is unclipped in a screenshot crop, at 1280 and 390 px, both modes, three engines.
- **U12 (T6, "not filler", "short", "where it fits", "not a required step").** Rewrite: per level page 1..4 at least one tip
  admonition with one fixed title (proposal `:::tip[Make it yours]`, never "Checkpoint"); each at most 60 words; each names a
  concrete harness artefact (rule, skill, hook, permission or guardrail, command) and ties it to that level's own content (a
  table tip -> the step it builds on); no step, checkpoint or checklist item refers to it (checklist sections byte-identical to
  the T5 gate); not inside the at a glance box; mirrored in the plan level section and pinned by one L<n> entry each.

### Criteria to add

- A1 (T3): `watch the demo` deny term and its term-loop row (G-1); fileScope `rules/content.mjs`, `content.test.mjs`.
- A2 (T1): five IX coverage entries, one per new Format cell (`Solo 22, debrief 3`, `Solo 32, debrief 3`,
  `Solo 18 (LSP 10, MCP 8), debrief 2`, `Solo 28 (permissions 4, hooks 14, skill and command 10), debrief 2`,
  `Solo 42, debrief 3`), section `2. Agenda`; plus one IX entry for the walk-around sentence on index.md. IX floor rises to
  the live count.
- A3 (T1..T6): after each task every per-page floor equals the live entry count (not only `>=`), the manifest total never
  drops, `MANUAL_ROWS` stays 15 (no removed entry turns into a manual row).
- A4 (T1..T6): no new `SWEEP_EXCLUDED` literal without a reason naming its pin plus a deletion mutant of that pin (G-2);
  deleted keys only for literals gone from the pack (T1: the four `plan:182`/`plan:183` keys).
- A5 (T2, recommended): a committed node:test that extracts the Level 1 page's `.claude/rules/domain.md` fenced block and
  asserts byte-equality with the staged pack file `docs/spec-pack/level-branches/level-1/files/dot-claude/rules/domain.md`
  (byte-identical to `origin/solution/level-1` per res-2). Without it the equality is verifier-only and a later edit of page and
  plan together stays green. If the orchestrator declines: verifier `cmp` against both sources is mandatory.
- A6 (T5): the table-region rule proven on `build-offline` as well (G-5).
- A7 (T4): the Level 4 page and plan:19 claim no installed `openspec` CLI on `start/level-4-openspec` (G-4).
- A8 (T5): keyboard scroll (focus the region, ArrowRight increases `scrollLeft` at 390 px on the three overflowing tables);
  tables keep their table role (`getByRole('table')` count equals the table count); at 1280 px every table's cell rects equal
  the T4-gate build within 1 px (no visual regression).
- A9 (T3): the Part B fallback stays outside `<details>`; `<details>` closed by default.
- A10 (T1): every removed guide sentence (5 boxes, index.md:17, index.md:49), re-inserted verbatim on its page one at a time,
  makes `npm run check` fail (structure ban and/or deny term, quoted).
- A11 (T1): the ban scope is "an admonition of any type whose title contains `sync moment` in any case and spacing, on any page
  (finished, skeleton, non-level)", implemented in `commonErrors`; `SYNC_PHRASE` and the exactly-1 rule are gone.
- A12 (T4): new SU entry for the Node qualification (G-3).

### C0: common directive for every verifier of this spec (in addition to the task card and its block)

C0-1 Scratch and evidence. SP = the session scratchpad named in your environment. Evidence `SP/s15-t<N>-qa-r<k>-*`
(implementers `s15-t<N>-impl-*`, qa-final `s15-final-qa-r<k>-*`), cited by absolute path. Helper scripts
`.claude/comms/tmp/s15-t<N>-qa-*.sh|.mjs|.py`. Every round writes `s15-t<N>-qa-r<k>-hashes.txt`: repo-relative
`shasum -a 256` lines only, one per fileScope file present. Every temporary directory: `mktemp -d "$TMPDIR/s15-t<N>-<role>-XXXXXX"`
inside a script file.

C0-2 Snapshot and restore. Never claim an empty `git status`. At snap-before record `git status --porcelain`, hash every
probe-touched file and `cp` it to `SP/s15-t<N>-qa-<File>.orig`; restore every probe by `cp`, then `shasum` and quote the match
before the next probe; snap-after equals snap-before. No git write of any kind; base bytes via `git show <base>:<path>`.

C0-3 Toolchain. Every npm/node command with `PATH=<SP>/node-v26.13.0-darwin-arm64/bin:/usr/bin:/bin`; quote `node --version`
and `npm --version`. `npm ci` in `guide/` yourself; never `npm install` in `guide/` or `npm audit fix`. Budgets via
`bash ~/.claude/scripts/with-budget.sh` (npm ci 300, each build 300, referee 600, a full sweep 1500).

C0-4 Gates, all exit 0, tails quoted: in `guide/` `npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`,
`npm run build:offline`; `typecheck` again after both builds exist; referee from the repo root
`env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify` with
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<SP>/jtmp` (Tests run totals, BUILD SUCCESS). Never `spring-boot:run` or `docker compose`.

C0-5 Warning inventory. Grep `warn|deprecat|experimental|error` (case-insensitive) over the npm ci, check, typecheck, both
builds and check:built logs. Spec 14 accepted counts: npm ci uuid@8.3.2 deprecation 1, install-scripts notice 6, audit summary 1;
`build` localStorage ExperimentalWarning 2; `build:offline` sitemap disabled 1; check, typecheck, check:built 0. The T1 verifier
re-records these on the spec base first (from `git archive` of the base, or the first run before any probe if the impl diff
touches no build input); any other line is fixable here (FAIL), owned by a named criterion, or unowned (flag it).

C0-6 Checker no-regression. Quote node:test `# tests N`, `# pass N`, `# fail 0`; N at least the previous gate's (spec base 713).
Titles: previous gate's titles minus current (`comm -23`) is empty except renames listed old -> new. `diff` the `check.mjs`
output against the previous gate: only the lines the task names may differ; `coverage (anchored N/N, 15 manual rows, 0 entries
pending)` with N never lower than the previous gate. One throwaway failing test file in the discovery pattern makes
`npm run check` exit non-zero (T1, T5; any task adding a test file); remove it and re-hash.

C0-7 Scripts regime. Every `guide/scripts/**` file outside the task's fileScope hash-equal to the previous gate (T1: spec 14
`agent-os/specs/2026-11-10-1610-14-guide-branding/checker-baseline-hashes.txt`, verified 99/99 on 7ded994; later tasks: the
previous task's `-hashes.txt` of its endorsed round). Inside the fileScope each changed file listed with its reason. Edited test
files insertion-only except retargets listed old -> new. `rules/coverage.mjs`, `lib/mdscan.mjs`, `rules/plan.mjs` stay
hash-equal across the whole spec unless a criterion names them.

C0-8 Pin retargets (spec requirement "still guards the same thing"). For every rule, fixture, hash, coverage entry, floor or
sweep key the task changes: an old -> new table in the outbox, and per item the mutant that proves the new pin guards the same
thing, run by you, each failing with the quoted message: a retargeted entry -> delete its new sentence from the page (fails
rule b with the id) and from the plan section (fails rule a); a floor -> equals the live count (script prints counts per page);
a renumbered sweep key -> `sed -n '<new>p' workshop-plan.md` contains the literal, reason byte-identical to the base reason; a
deleted sweep key -> its literal is no longer at any swept line of its section; a new entry -> length >= 8 and not already
satisfied by unrelated text (count its occurrences on the page and in the section). Whole-line equality for `kind: 'command'`
entries (the checker matches substrings).

C0-9 Pack and page together. A table: every changed participant sentence of `workshop-plan.md` with its page twin (and the
reverse). `git diff -U0 <pr-base> -- docs/spec-pack` touches only `workshop/workshop-plan.md` (no level branch file, progress.md,
decisions.md, roadmap.md, other pack file), hunks quoted. Every fenced line and inline code span the task adds to a page that is
a command (shell or slash) exists as a whole trimmed line or whole inline span in one of the five SOURCE_FILES (script over the
page diff; list non-command spans such as a prompt, a mock line or a path with their classification).

C0-10 Hygiene. `git grep --no-index -n -P '[^\x00-\x7F]' -- <fileScope text files> docs/spec-pack/workshop/workshop-plan.md`
prints nothing, with a positive control file that prints; no em dash; no "facilitator" on any page (replacement text says "Maik
and Tom walk around"); `git grep -n '/Users/' -- guide docs/spec-pack/workshop` prints nothing; plain English.

C0-11 Rendered checks, for the pages the task touches. Rig: reuse `SP/s14-guide-verify/` (quote Playwright and axe-core
versions; if absent, install the same versions into `SP/s15-guide-verify/`); new scripts `s15-*.mjs` derived by copy from
`s14-final-qa-r1-sweep.mjs` with the `diff` quoted; never edit an `s11-*` or `s14-*` file. No s15 script holds an exclusion
filter (the s14 line 269 `expected` filter is deleted in every copy; `grep -n scrollable SP/s15-*.mjs` shows no filter).
3 engines x Pages build (`docusaurus serve --port <P> --no-open`, P = 3360+N for verifiers, 3369 qa-final, 3370+N implementers,
`lsof -iTCP:<P> -sTCP:LISTEN` preflight, stop by PID, port free after) and offline build over `file://` from a path with a space
(non-file requests aborted and counted, expected 0, plus one control fetch) x light/dark x 1280/390 px. Brand-new context per
build and mode; assert `documentElement.dataset.theme` before measuring. axe full default rule set: `violations` and
`incomplete` listed per run. T1..T4: G-6 classification only for the base A4 hit; T5, T6: 0 violations. Console: only spec 11
FF-5 (WebKit offline CORS pair) and FF-37 (axe preload triple on `file://`, axe phase only).

C0-12 Scope. Product files changed (`git diff --name-only <pr-base>...HEAD` plus working tree, minus `.claude/` and
`agent-os/`; T5 also the spec folder `visuals/`) are a subset of the fileScope. `guide/package.json`,
`guide/package-lock.json`, `guide/docusaurus.offline.config.ts` hash-equal to the spec base in every task.

C0-13 Timing by `stat` mtimes (impl artifact, last product file, your gate logs), never frontmatter.

C0-14 Hard limits. No push, no git write, no workflow dispatch. Never run Claude Code (D14), never install or enable a plugin
or marketplace. Framework CLIs only in a `mktemp -d` sandbox with `HOME` set to it and `OPENSPEC_TELEMETRY=0`; never execute
`install.sh` or `install.ps1` (download to read is fine).

C0-15 Probes fail for the stated reason. Quote the failing assertion or rule message (rule, file, line) per probe; a syntax,
import, MDX compile or build error is no evidence unless the criterion is "the build fails". Mutants are applied by you, one per
run with its own log, ids `s15-T<N>-M<k>`.

C0-16 Built text. From T1 on: `grep -rli 'sync moment' guide/build guide/build-offline` prints nothing; positive control
`grep -rli 'Level cleared when' guide/build` prints files.

C0-17 Decisions. Report each Decisions-log item and each Gate 1 finding (G, U, A ids) the task implements as met or
disclosed, with the evidence line.

### Task directives (qaDirective, in addition to all of C0)

**T1 (critical, sonnet). Sync moments out, timetable rebalanced, oracle updated. Lens 4 on every criterion.**
1. Census: `git grep -n -i -E 'sync[[:space:]]+moment|demos this now|maik demos|live demo|look up, maik' -- guide/docs guide/README.md docs/spec-pack/workshop/workshop-plan.md`
   prints nothing (control: the same grep on `git show <base>` content prints the 5 boxes, index.md:17/49, README:202 and the 5
   plan paragraphs + plan:15). The Level 2 demo sentences (03:107, troubleshooting:50, plan 200-202, 343) are still present
   (T3's scope, quote them). plan:173 and plan:340 byte-identical.
2. Structure ban (A11), node:test witnesses, each giving exactly the ban error: `:::info[Sync moment]` on a finished level page,
   on a skeleton level page, on wrap-up, on index; with the old phrase body and with another body; `:::note[Sync moment]`;
   `:::tip[sync moment]`; `:::info[Sync  Moment]`; `:::info[Sync moment: hooks]`; nested inside `::::tip[Outer]`; control
   `:::info[Heads up]` passes. Mutants (each must fail a test): M1 ban removed; M2 ban moved from `commonErrors` to the
   finished-level branch (killed by wrap-up and skeleton witnesses); M3 `box.type === 'info'` kept (note witness); M4 exact title
   equality (case and suffix witnesses); M5 case-sensitive match (`sync moment` witness).
3. Deny-list: rows `sync moment` (`/sync\s+moment/gi`), `maik demos` or `demos this now`, `live demo`, each in the term loop.
   Witnesses: `Sync\nmoment` and `Sync  moment` fail (kill a single-space regex, M6); near misses pass: `demo data`, `in sync`,
   `a moment`. M7: each new DENY row removed alone makes a test fail.
4. End to end (A10): re-insert, one at a time, each removed guide text verbatim from the base (the 5 boxes, index.md:17, the
   index.md:49 bullet) on its page: `npm run check` exits 1; quote the ban and the deny messages for the Level 1 box (both must
   appear) and the deny message for the two index sentences. Restore by `cp`, hash.
5. Retitled samples still test their rule: space-form test still exactly one error (M8: space-form rule removed -> that test
   fails); unclosed test on wrap-up (M9: unclosed rule removed -> fails); `built.test.mjs` SPACE_FORM retitled (M10: the
   `<p>:::` check in `rules/built.mjs` removed -> fails). `SYNC_PHRASE` absent from `guide/scripts`.
6. Coverage (C0-8, A2, A3): L0-01, L1-01, L2-01, L3-01, L4-05 table old -> new snippet, each new snippet >= 20 chars, exactly
   once in its plan level section and once on its page outside box and checklist; five deletion mutants on the page (rule b with
   the id) and five on the plan (rule a). Row labels unchanged, `SECTION_9_ROWS` 74. Floors equal live counts; manifest total
   >= 304 + new; IX entries for the five Format cells and the walk-around sentence (A2), deletion mutant for one of them.
7. Sweep keys: the four `plan:182`/`plan:183` keys deleted (literals gone), every other key renumbered old -> new list,
   reasons byte-identical (`diff` of the reason strings), equality and stale-key tests green. No new key (A4).
8. Timetable: index.md and plan Format cells exactly the res-1 "after" values; a script prints per row solo + debrief = level
   total (25, 35, 20, 30, 45) and Part sums (10 + 8 = 18; 4 + 14 + 10 = 28); plan Part headings `(10 min)`, `(8 min)`,
   `(4 min)`, `(14 min)`, `(10 min)`; every other agenda cell, `240`, `1:55 | Break`, start times and every `**Time:**` line
   byte-identical to the base.
9. Fixtures (G-9): `lib/structure-fixtures.mjs` diff limited to plan line 145 and the box branch; skeleton docs hash-equal.
   README:202 example retitled.
10. Rendered: index and the five level pages (C0-11), no admonition title matching `/sync\s*moment/i` in the DOM; C0-16.

**T2 (elevated, sonnet). Level 1 rule file and `/context` mock.**
1. Byte equality: extract the page's `.claude/rules/domain.md` fence body and `cmp` it with `git show
   origin/solution/level-1:.claude/rules/domain.md` and with the staged pack file (A5); same for the plan's copy if mirrored.
   Rendered: the code block's `.token-line` texts joined by newline equal the file lines (MDX ate nothing: `---`, `#`, backticks).
   If A5 is adopted: mutants one glob character changed on the page, and page plus plan changed together, each fail the test.
2. Path `.claude/rules/domain.md` on the page (content.test.mjs:45 pin green); the other two files named with their globs
   (L1-14..L1-20 green). U5 sentence check with the docs quote.
3. Mock (U3, U4): ASCII, fence `text` or none; consistency script output quoted; illustrative sentence pinned by an entry
   (deletion mutant); annotation count and word counts; `/clear` and `/compact focus on <topic>` still satisfy L1-27, L1-28.
4. Pack: plan 148-151 and step 5 changed with the page (C0-9). G-2: every new plan literal (each fence line, each new span) is
   covered: quote `sweep(ENTRIES)` before and after; new entries and floor L1 = live count.
5. Checkpoint after step 3 still holds a fence and an `Expected` line; its wording uses `/memory` as the certain route (G-7).
6. Rendered on Level 1 (C0-11): both code blocks scroll inside `pre` at 390 px, no page horizontal scroll.

**T3 (elevated, sonnet). Level 2 debugging section, demo text out.**
1. Demo text gone: `git grep -n -i demo -- guide/docs` prints nothing; plan `git grep -n -i -E 'facilitator demo|watch the'`
   prints nothing; plan:340 ("for demos") the only expected plan hit of `demo`, listed.
2. A1: `watch the demo` DENY row in the term loop; re-inserting 03:107's base sentence on the page and troubleshooting:50's on
   its page each make `npm run check` fail with the deny message.
3. `<details>` (U6, U7, A9): one `details` in Part A step 5 (between `#part-a-lsp` and `#part-b-mcp`), no `open`; content
   checks: `which jdtls` in the shell that starts `claude`, `java -version` 21 or later, `/plugin` Installed (enabled) and Errors
   tab, a new session after installing, the server starts on the first `.java` edit (refers back to step 4); each traced to the
   res-2 doc quotes (code-intelligence, plugins troubleshooting). No new command outside the pack (`/reload-plugins`,
   `claude plugin list` absent unless added to the plan).
4. Rendered (C0-11) on Level 2 and troubleshooting: details closed then opened by click and by Enter on the focused summary,
   both builds, three engines; axe with details closed and open, both modes; summary and body text contrast computed per mode
   (no checker pair covers `details`, res-4) >= 4.5; the troubleshooting link to the Level 2 section resolves (click, h2/anchor
   reached) in both builds.
5. L2-17 retarget to the Part B fallback sentence (old guard "give up and go to Part B"); deletion mutant fails L2-17; L2-14,
   L2-15 (`claude --debug` in a fence), L2-16 still green; new prompt entry kind `command` (U6), floors = live counts, sweep keys.
6. JDK: `git grep -n -E 'JDK 1[0-9]|Java 1[0-9]' -- guide/docs docs/spec-pack/workshop/workshop-plan.md` prints nothing;
   plan:59 says 21; spec 10:168 untouched (residual).
7. Plan 200-202 mirrors the details content and the fallback (C0-9 table); plan 343 risk row says no demo, keeps `which jdtls`
   (TS-02).

**T4 (elevated, sonnet). Level 4 start-branch truth, brownfield notes, OpenSpec install, Node sentence. Lens 4 on the install.**
1. Install lines, whole-line equality on the setup page and in Track A: `curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v1.16.0 sh`;
   PowerShell `$env:OPENSPEC_VERSION = "v1.16.0"` and `irm https://openspec.dev/cli/install.ps1 | iex` in a Windows PowerShell
   tab; `npm install -g @fission-ai/openspec@1.16.0` with Node 22.12+; `openspec --version`. `git grep -n 'OPENSPEC_VERSION=v1.16.0 curl'`
   over guide and plan prints nothing. `1.16.0` equals the pin in `docs/spec-pack/product/tech-stack.md` (quote the line).
2. Evidence U10 (a) to (e); downloaded scripts kept in `SP`, quoted lines, never executed.
3. Rule (c): mutants on the page only: `v1.16.0` -> `v1.15.0` in the PowerShell line, and `irm` line with a trailing `-Verbose`,
   each make `npm run check` fail with the rule (c) message; `check:built` reports the PowerShell line count up by the new lines.
   Bash lines: coverage entries kind `command` pin them; mutant dropping `OPENSPEC_VERSION=v1.16.0 ` from the page fails its entry.
4. Node sentence (G-3, A12): setup page:21 and plan:47 identical; SU-21 test negative still fails; the base sentence alone fails
   the new SU entry; renames listed.
5. Start-branch truth (U9) table claim -> path; "discovered from the codebase" gone; G-4/A7 lines corrected or residual.
6. Brownfield notes (U8): per track the ordered command list extracted from the rendered tab equals the expected list (script);
   word and line counts; doc URLs in the spec folder; any new `Tabs groupId="track"` carries exactly the three labels; new
   commands (for example the short `specify init --here --force --integration claude`) added to the plan (C0-9).
7. Track commands unchanged: per track the command list of the step sections before and after equal (diff quoted), except the
   added install step in Track A.
8. Coverage: new SU and L4 entries, floors = live counts, sweep keys renumbered and new literals covered (G-2).
9. Rendered (C0-11): setup page and Level 4, every tab of the os and track groups opened in both builds; PowerShell tab
   lines visible and selectable.

**T5 (critical, sonnet). Table regions, A4 dropped. Lens 4 on every criterion.**
1. Plugin hast fixture witnesses (node:test), each with the mutant it kills: W1 two tables under one heading -> distinct labels
   (constant label); W2 table before any heading -> non-empty fallback label (crash or empty); W3 heading `## Tracks {/* #tracks */}`
   -> label exactly `Tracks` (walk that stringifies MDX expressions); W4 heading with inline code, emphasis and an inline JSX
   element (`<kbd>`) -> their text kept (element-only text walk); W5 a table inside an `mdxJsxFlowElement` (TabItem, details)
   -> wrapped (visitor not descending into JSX); W6 plugin run twice -> one wrapper (double wrap); W7 H2 then H3 then table -> the
   H3 text (first-heading lookup); W8 a heading after the table does not name it; W9 wrapper `tabIndex` 0, `role` region,
   `aria-label` set, the table carries no role (role on the table); W10 the wrapper's only child is the table, table order kept.
   G-8: no `import` from `node_modules` in the plugin.
2. Built rule fixtures, each failing with its message: table without wrapper; `tabindex` missing; `tabindex="-1"`; `role`
   missing or `group`; `aria-label` missing, empty, whitespace; duplicate labels on one page; one region around two tables;
   wrapper as grand-ancestor only; same label on two different pages passes; zero HTML files is an error. Mutants: rule returns
   []; uniqueness check removed; trim dropped from the label check; parent check widened to any ancestor; the rule's call removed
   from `scanBuilt` or `check-built.mjs` (a CLI test must kill it, spec 14 G-7); `npm run check` with `guide/build` moved aside
   keeps the same test count. Real tail line quoted with table count N and page count M; N equals an independent count of
   `<table` in article content of `build/**/*.html` (script); A6 the same N on `build-offline`.
3. Config pin (`task-list-labels.test.mjs`): both import lines and `rehypePlugins: [rehypeTaskListLabels, rehypeTableRegions],`
   pinned; mutants dropping either plugin each fail it; offline spread pin unchanged; old -> new title list.
4. CSS: wrapper rule in `custom.css` with `overflow-x: auto`, inner table not double-scrolling, focus ring on the existing focus
   variable via `:focus-visible`, no colour literal outside the token block; contrast, brand, colors checks green and
   `MINIMUM_PAIR_COUNT` unchanged unless a new nontext pair is added (then bumped, README Brand lists it).
5. Rig control first: a build with the plugin unregistered (C0-2 restore) swept at 390 px axe-only on the three pages and three
   engines shows `scrollable-region-focusable` exactly as A4 recorded; then the task build shows none.
6. Full sweep (C0-11): 11 pages x 2 modes x 2 widths x 3 engines x 2 builds = 264 runs, axe full default rule set, 0 violations,
   no filter, `incomplete` dispositioned; `landmark-unique` and `region` clean.
7. Per table, 390 and 1280 px, both builds: parent is the region (`getByRole('region', {name})` count 1 per label); Tab reaches
   each region in document order; U11 focus ring; A8 keyboard scroll, table role kept, 1280 px cell rects equal the T4-gate build
   within 1 px; at 390 px region `scrollWidth > clientWidth` on the three wide tables, no page horizontal scroll.
8. Sweep record in the spec folder `visuals/`: your counts equal it; ASCII; no `/Users/`.
9. `guide/README.md` describes the plugin and the rule as the code does.

**T6 (standard, sonnet). Creativity tips.**
1. U12 per tip: title, word count, artefact named, level content it builds on, location (not in box, not in checklist).
2. Checklist sections and every step heading byte-identical to the T5 gate; no other text changed on the four pages except the
   tips (word diff quoted).
3. Plan mirrors each tip in its level section (C0-9); one L<n> entry per tip, deletion mutant on one tip; floors = live counts;
   sweep keys renumbered, new literals covered (G-2).
4. Rendered (C0-11) on Levels 1..4: tip count per page by title in the DOM >= 1; 0 axe violations both modes.

### qa-final focus

1. Integrity: per-task hash baselines vs committed tip bytes (`git show <tip>:<f>`); union of baseline paths equals the spec
   diff's product files; record the new `checker-baseline-hashes.txt` (every `guide/scripts/**` file) in the spec folder.
2. Checker end state vs base: node:test titles lost = union of rename lists; totals monotone from 713; `anchored N/N` with N =
   304 + additions; manual rows 15; floors = live counts; `SWEEP_EXCLUDED` literal set (line numbers stripped) = base set minus the
   four T1 literals plus approved additions only; `SYNC_PHRASE` and the exactly-1 rule gone, ban and deny rows present.
3. Sync moments end to end on the tip: C0-16 both builds; DOM titles on 11 pages; T1 M-reinsertion of the Level 1 box fails
   `npm run check` with both messages; `watch the demo` re-insertion fails.
4. Pack and guide parity over the whole spec: sentence table, plan diff hunks vs base, whole-line command census of every page
   command added in the spec against the five SOURCE_FILES, timetable arithmetic in both files, JDK 21 everywhere, Node sentence
   identical in setup page and plan.
5. Full 264-run sweep on the tip with no filter, plus the plugin-off control; table regions (focus, keyboard scroll, role kept);
   `<details>` open/close in both builds; tips per level; Level 1 code blocks equal the branch file.
6. External URLs new in the spec, one request each: `https://openspec.dev/cli/install.sh`, `.../install.ps1` (download, never
   run), the v1.16.0 release assets list, the cited docs pages (code.claude.com memory, context-window, commands,
   plugins/code-intelligence, plugins/troubleshooting; OpenSpec v1.16.0 docs; Spec Kit v1.3.1 docs; Agent OS pages);
   `npm view @fission-ai/openspec@1.16.0`.
7. Start-branch claims re-checked against `git ls-remote` shas.
8. CI artifact: the offline zip of the spec PR's `guide.yml` run (bound by `gh run list --commit <full sha>`), unzipped into a
   path with a space, rig on it (tables, details, no sync moment, install tabs).
9. Hygiene over product files of the spec diff: ASCII, no em dash, no "facilitator" on pages, no `/Users/`.
10. CI green on the spec PR head, PR readiness (base ancestor, no gitlinks).

### Residuals expected at Gate 2 (owner)

R-a `/context` mock not compared with real output (rehearsal row, D14). R-b install scripts never executed by agents; Windows
PowerShell route untested (rehearsal, macOS only per D8). R-c Level 2 debugging steps not exercised live (D14). R-d Level 1
checklist relies on `/context` listing path-scoped rules (docs silent, G-7). R-e Agent OS typed command form (PF-T13-1), Spec Kit
"short path" wording, pack bullet order for Agent OS. R-f spec 11 pack text, README.md:15, plan:173, plan:340, spec 10:168. R-g
RA-1..RA-4 (spec 14). R-h rendered checks are local only (spec 14 S3). R-i wrap-up page keeps the unpinned `@latest` routes
(post-workshop advice, consistent with sdd:149).

## Review log
(none yet)

## Traceability matrices
(per task, filled at each review)

## Cross-task patterns
(none yet)

## Open questions
(none)

## Resume point
Strategy delivered (handoff sent to orchestrator). Next assignment expected: `review T1` after the T1 verifier verdict. Before
it: read this ledger's C0, the T1 block and G-1/G-2; baseline = spec 14 `checker-baseline-hashes.txt` (99/99 on 7ded994).
