---
agent: lead-1
task: lead-1
status: completed
spec: docs/spec-pack/specs/11-workshop-guide-site.md (agent-os spec folder not yet written; shaping brief inline from the orchestrator)
updated: 2026-11-08T21:20:00Z
---

# Strategy: spec 11, workshop guide site

## TL;DR

Build the guide checker first, then the content. Five infrastructure tasks come before any page body:
- T1: scaffold the site and the check harness.
- T2: the CI workflow.
- T3: the brand and the contrast check.
- T4: the LevelProgress component, skeletons of all eleven pages, and the page-structure checks.
- T5: a coverage manifest anchored to the pack.

Then five content tasks (T6 to T10) fill the skeletons. T11 writes the reference pages, bans the skeleton marker and does the site-wide pass.

Rules that hold the plan together:
- `npm run check` is the executable spec. It is dependency free (Node built-ins and `node:test`) and runs in CI before the build.
- At check time it reads `docs/spec-pack/workshop/workshop-plan.md`, so the "Level cleared when" lists, Goals and minutes cannot drift. The guide only ever lives on `factory`, where the pack exists: progress.md says the spec PR merges after `spec-10-done`, so after spec 09 has published `main`.
- A skeleton marker on each unfinished page exempts it from body coverage. This keeps every merge green while the coverage rules are live from T5 on.
- Every manifest snippet must exist in the pack and also on the page, so the manifest cannot grade invented content.
- The verifier's manual pass covers what a script cannot judge: whether restated prose is faithful, the rendered layout, offline use in three browser engines, and axe contrast.

11 tasks, one module each (T2 is `root`, all others are `guide`), run strictly one at a time.

memory: written in worktree

## 1. Problem Analysis

**What is being built.** A Docusaurus 3.10.2 docs-only site in `guide/`:
- Eleven pages from a fixed information architecture, and one static React component.
- The OpenValue 2026 brand in one stylesheet, and self-hosted Montserrat.
- A contrast script, and a three-job workflow with a public deploy gate held by the owner.

The content restates three pack files (workshop plan sections 3 to 10, SDD guide Part 2, feature briefs) and strips facilitator-only material. Much participant-facing material exists only after specs 09 and 10, so it becomes a follow-up rather than being invented.

**UX.**
- Participants read the guide on a second screen at 720 px, possibly offline from a zip, under time pressure.
- What matters is the fixed page grammar: indicator, at-a-glance box, steps, checkpoints, and "Level cleared when" last. Commands must be copyable (code blocks with copy buttons). Catch-up must be one command.
- Accessibility: AA contrast in both colour modes. The current level must be signalled by more than colour (`aria-current="step"`, a bold visible label).

**Architecture.**
- Two configs and two builds (settled). The browser-router build is the only build that proves links and rendering. The hash build is only a repackaging of it.
- Brand tokens live once in `custom.css`, and components consume them only through `var(--...)`.
- The npm script names form a contract between `package.json` and `guide.yml` (API-first). T1 fixes the five names, so the workflow never has to change when later tasks add checks.

**Testing.** No unit-testable product logic exists apart from the component guard and the checker itself. So the test layers are:
1. `node:test` self-tests for every checker rule, a passing and a failing case each, built from in-memory strings. This guards against a vacuous checker.
2. `npm run check`, run on real pages.
3. `npm run build` (broken links, anchors and render errors) and `npm run check:built` (an admonition left as literal text).
4. The verifier's Playwright and axe pass, which is tooling only and never committed.
5. Semantic review against the res-3 `source line -> item -> page` table.

**Performance.** About 5 s for `npm ci`, 3 s and 1 s for the two builds, and a 286 KB zip. Nothing to optimise. The 60 s Maven referee is unaffected because `guide/` is not a module, but it is re-run per task as a guard.

**Security and data integrity.**
- The deploy and the release are public side effects. They are gated on `push && vars.GUIDE_DEPLOY == 'true'`, which fails closed.
- `contents: write` is granted only on the release job. Actions are SHA-pinned. No secrets.
- Content risk: the guide must never tell anyone to set `ANTHROPIC_API_KEY`.
- No new npm dependency. The template's 25 audit advisories are noise: never run `npm audit fix`, because it would change dependencies.

**Maintainability.**
- The pack stays the source of truth. Reading it at check time turns any later pack change into a red guide build on the next guide change, or at the pre-merge sync with `factory`.
- The manifest and the checker are the regression suite for the follow-up round after spec 10.
- Trap: a check that duplicates colour values or bullet text would rot. That is why the contrast check reads `custom.css` by variable name and the list check reads the plan.

**Constraints that shape the plan.**
- One implementation agent (sonnet) per task, with a verifier able to check everything in one session. So L0 and L1 share a task, while L2, L3 and L4 get one task each.
- Each task must be completable without later tasks. This is why skeletons exist (anchors and links resolve from T4 on) and why a marker gates coverage.
- Agents run in separate worktrees. The research artifacts live under `.claude/comms/` (git-ignored). The orchestrator should copy res-1, res-2 and res-3 into the spec folder (for example `references.md`) so agents in other worktrees can read them by repo path.

## 2. Recommended Approaches

### A. Checker-first walking skeleton, with a pack-anchored manifest (recommended)

Philosophy: acceptance-test-driven documentation. The checker and the manifest are written before the pages they judge. Skeleton pages make the site structurally complete from T4 on, and content tasks only fill bodies.

- Pros:
  - Every acceptance criterion of the spec's "Level pages" scenario is machine-checked from T4 on.
  - Coverage of literals (commands, prompts, paths, URLs) is mechanical, and "never invented" Windows commands are enforced.
  - Drift detection outlives the cycle and serves the follow-up round.
  - Verifiers spend their time on semantic fidelity instead of counting bullets.
- Cons: five infrastructure tasks before the first content body, and one more component of the system (the checker) to keep correct.
- Excels when content must stay synchronised with a living source and many verifiers must apply the same rules.
- Risks:
  - Self-grading. Mitigation: T5 authors the manifest before the content, and content tasks may not edit it.
  - Over-strict rules blocking legitimate content. Mitigation: escalate as a question; never weaken a rule silently.
  - Parser brittleness on Markdown. Mitigation: a strict page grammar defined in T4.

### B. Content-first, verifier-only checks

Write the pages in 5 or 6 tasks. Each verifier checks the conventions by eye with grep and the res-3 table, and one late task adds a light structural script.

- Pros: fewer tasks, and content is visible early.
- Cons:
  - "Identical to the workshop plan" is checked by eye eleven times.
  - Drift after the cycle goes undetected, and nothing guards the follow-up edits.
  - Verifier rework rounds are likely: missed bullets, wrong branches, space-form admonitions that render as plain text with no error.
- Excels for one-off documents with no living source. That does not describe this guide.

### C. Generate content from the pack at build time

A prebuild script or remark plugin extracts plan sections into MDX.

- Pros: perfect synchronisation for the extracted parts.
- Cons:
  - The pack is facilitator-shaped. The spec asks for restructuring and stripping, which generation cannot do faithfully.
  - Custom plugin code becomes part of the build.
  - Source pages become unreadable.
  - It contradicts "a checklist copied from the workshop plan".
- Use it only if the plan becomes participant-shaped, which it will not. Rejected.

**Methodology for A:**
- ATDD: checker plus manifest first, with self-tests per rule.
- Incremental delivery behind a skeleton marker, which acts as a per-page feature flag for coverage.
- Progressive rollout gated by `GUIDE_DEPLOY`, which only the owner flips.
- No new spike: res-1 already proved the configs on the pinned versions in three engines.

## 3. Strategic Implementation Plan

### Ground rules (apply to every task; put them in the plan, not only in cards)

- **G1. Toolchain.**
  - Use Node v26.13.0 (`node --version` evidence in the outbox) and `npm ci` in `guide/`.
  - `npm install` is used only by T1 to create the lockfile.
  - Never run `npm audit fix` and never add a dependency.
  - Playwright and axe-core are verifier tooling, installed in the scratchpad only. res-1's script is a starting point if it is still present: `<scratchpad>/res-1-pw/res-1-check.mjs`.
- **G2. Standard gates from T1 on.** All exit 0:
  - in `guide/`: `npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`, `npm run build:offline`;
  - at the repo root: `./mvnw clean verify` (green, unchanged).
- **G3. Plain ASCII.** Every text file the task changes passes a byte-level `[^\x00-\x7F]` grep (font binaries excluded).
  - No em dashes and never the word "lab".
  - Trap: the Write and Edit tools decode a typed backslash-u escape into the literal character. The checker builds forbidden characters from code points (`String.fromCodePoint(0x2014)`).
- **G4. MDX safety.**
  - Placeholders such as `<branch>`, `<key on screen>`, `{...}`, `$ARGUMENTS` appear in prose only inside inline code.
  - No raw `<img>`. Markdown image syntax only, and no images are expected.
  - YAML titles with a colon are quoted.
  - Admonitions use the bracket form (`:::info[Sync moment]`, `:::tip[Checkpoint]`).
  - Keep `markdown.format` at its default (`mdx`), so the `.md` pages can import Tabs.
- **G5. No fact without a pack source.**
  - Each content outbox carries a `page section -> pack file:lines` table.
  - Gaps go to the outbox's follow-up list and are never invented: missing Windows variants, anything that exists only after specs 09 or 10, outputs the pack does not state.
  - Solution branches are linked (`https://github.com/MaikKingma/AgenticBookStore/tree/solution/level-N`), and their file texts are not printed. The one exception is the bash hook scripts from spec 10 lines 273-314.
  - Claude Code behaviour claims are restated as the plan states them, never embellished (res-3 C14).
- **G6. Participant pages never cite internal paths or specs.** No `spec-pack`, `workshop-plan`, `sdd-framework-guide`, `feature-briefs`, `spec NN`, `.claude/comms`, and no "facilitator". The plan itself says "Detailed track instructions: `sdd-framework-guide.md`": do not copy that line.
- **G7. The manifest and the checker belong to T4 and T5.**
  - A content task that finds a manifest defect sends a `question` and does not edit it.
  - No rule is weakened to reach green (CLAUDE.md rule).
- **G8. Git is read-only for agents.**
  - Falsifier probes are uncommitted edits in the task worktree, reverted with Edit, and recorded with their exit codes.
  - The PR diff never contains `guide/build*`, `guide/node_modules` or `guide/.docusaurus`.
- **G9. Evidence.** Every "passes" claim in an outbox comes with the command and its output tail. Every rendered claim comes with the Playwright script path and its log.

### Page grammar (contract for T4 skeletons, the checker and all content tasks)

Level page (`0N-level-N-*.mdx`), in order:

1. Frontmatter with `title: "Level N: <name>"` (exact names from spec 11 lines 46-48). Optional `sidebar_label`, `sidebar_position`.
2. Imports: `LevelProgress`, plus `Tabs` and `TabItem` when used.
3. `<LevelProgress current={N} />`.
4. `:::note[At a glance]` with these lines in order:
   - `**Goal:**` the plan's Goal sentence, first letter may be capitalised;
   - `**Start branch:**`;
   - `**Catch-up:**` followed by a fenced `bash` block of exactly `git stash -u && git switch <branch>`;
   - `**Time:** <plan minutes> minutes`;
   - `**Compare:**` `solution/level-N` (L1 to L3 only).
5. While unfinished, the marker `{/* guide-skeleton */}`.
6. Body sections. These H2 anchors are contractual so other pages can link to them before the body exists:
   - L2: `## Part A: LSP {#part-a-lsp}`, `## Part B: MCP {#part-b-mcp}`, `## Stretch {#stretch}`;
   - L3: `{#part-a-permissions}`, `{#part-b-hooks}`, `{#optional-stop-hook}`, `{#part-c-skill-and-command}`, `{#stretch}`;
   - L4: `{#tracks}`, `{#feature-briefs}`.
7. `## Level cleared when` followed by a Markdown task list (`- [ ] ...`). The items equal the plan's bullets after whitespace normalisation, in the same order and count. Nothing follows the list.

Header values (settled):

| Level | Start branch | Catch-up branch | Time | Compare |
|---|---|---|---|---|
| L0 | `main` (tag `level-0-start`) | `main` | 25 | (none) |
| L1 | `main` | `main` | 35 | `solution/level-1` |
| L2 | your Level 1 result, or `solution/level-1` | `solution/level-1` | 20 | `solution/level-2` |
| L3 | your Level 2 result, or `solution/level-2` | `solution/level-2` | 30 | `solution/level-3` |
| L4 | `start/level-4-<track>` | `start/level-4-<track>` | 45 | (none) |

Other pages:
- Frontmatter title, plus the marker while unfinished. `index.md` has `slug: /` and the first `sidebar_position`.
- `reference/_category_.json` sets the label "Reference" after Wrap-up.
- Troubleshooting anchors are contractual: `{#windows-hooks}`, `{#jdtls-not-starting}`, `{#usage-limits}`, `{#mcp-not-connecting}`, `{#anthropic-api-key}`, `{#offline-copy-blank-page}`, `{#docker-not-running}`, `{#far-behind}`.

Tabs:
- `groupId="os"` with labels `macOS/Linux` and `Windows PowerShell`. Use it only where the pack states the Windows form.
- `groupId="jdtls"` with labels `macOS`, `Arch Linux`, `Other Linux`, `Windows (WSL)`.
- `groupId="track"` with labels `Track A: OpenSpec`, `Track B: Spec Kit`, `Track C: Agent OS`.
- Hook script tabs (bash and PowerShell) wait for spec 10. This cycle shows bash only, plus a pointer to `reference/troubleshooting#windows-hooks` (follow-up F2).

Observation card (L0): a fenced `markdown` block that has a copy button.
- Header `| Failure mode | Evidence | Column |`, at least three rows, the hint "(didn't know, didn't care, didn't check)", and a line "What went right:".
- The instruction to save it as `notes/level-0.md`, which git ignores.

### npm script contract (fixed in T1; `guide.yml` depends on exactly these names)

| Script | Does | Notes |
|---|---|---|
| `typecheck` | `tsc` | tsconfig excludes `build`, `build-offline`, `.docusaurus` |
| `check` | `node:test` self-tests, then `node scripts/check.mjs` (all source rules, contrast) | Never needs a build; prints one line per rule and the pending skeleton pages |
| `build` | `docusaurus build` to `build/` | THE gate |
| `check:built` | Scans `build/**/*.html` for literal `:::` | Fails if `build/` is missing |
| `build:offline` | `docusaurus build --config docusaurus.offline.config.ts --out-dir build-offline` | Runs only after the gate |

### Phases and checkpoints

- **Phase 1, foundation (T1, T2).**
  - Success: the template is pruned to docs-only; both builds pass; the check harness runs with self-tests; the CI workflow runs green on its own PR with deploy and release skipped.
  - Checkpoint: the T2 PR run is the first proof that CI and the local runs behave the same.
  - Course correction: if Node 26.13.0 or `zip` behaves differently in CI, fix it before any content exists.
- **Phase 2, brand (T3).**
  - Success: contrast pairs pass in both modes (or dark mode is off by rule); fonts load offline in three engines; axe finds no violations on the index.
  - Checkpoint: the dark-mode decision is recorded here and nowhere later.
- **Phase 3, executable spec (T4, T5).**
  - Success: all eleven pages exist as skeletons; the "Level page structure" scenario passes mechanically; the manifest is anchored N/N; pending entries are listed per page.
  - Checkpoint: the QA lead reviews the manifest against res-3 section 9 before content starts. That is the cheapest moment to fix a coverage gap.
- **Phase 4, content (T6 to T10).**
  - Success per task: the marker is removed from the task's pages, coverage for those pages is green, and the semantic review shows no invented facts and no facilitator-only content.
  - Checkpoint: after T7 (the first level pages), the QA lead checks for cross-task patterns, such as recurring MDX traps or paraphrase drift, and amends the remaining cards.
- **Phase 5, closure (T11).**
  - Success: reference pages are done; the marker is banned (0 pending); the site-wide pass passes in both builds and three engines; the follow-up list is complete.
- **After the cycle, not part of it.** The follow-up round (F1 to F13 below) runs after `spec-10-done` and before the spec PR merges (the PR waits for that tag anyway). The owner's deploy gate comes last.

## 4. Quality Gates

**What the script checks versus what the verifier checks by hand**

| Check | `npm run check` (CI) | Verifier pass |
|---|---|---|
| Titles, `LevelProgress` first with N, header box (Goal and minutes from the plan, branches from the table), checklist identical and last | yes (T4) | rendered H1 and indicator highlight in the browser |
| Admonition bracket syntax, sync phrase, checkpoint shape (code block plus "Expected") | yes (T4) | rendered admonitions; `check:built` catches literal `:::` |
| Literal coverage (commands in fenced blocks, prompts, paths, URLs), anchored to the pack | yes (T5) | prose steps against the res-3 table rows marked `manualRows` |
| Windows tab lines exist in the pack | yes (T5) | (none) |
| No em dash, ASCII, no "lab", deny-list, `ANTHROPIC_API_KEY` rule, no raw `<img>` | yes (T1, T5) | spot-read |
| No colour literal outside `custom.css`; brand assertions; contrast of declared pairs | yes (T3) | axe color-contrast on rendered pages, both modes, 720 and 1280 px (catches Infima defaults not declared) |
| Dependency allowlist, exact Docusaurus pins | yes (T1) | (none) |
| Broken links and anchors | `npm run build` | probes in T1 |
| Offline use, 720 px, copy buttons, tabs, fonts loaded | (none) | Playwright: Pages build via `npx docusaurus serve`; offline via `file://` (Chromium per task; Chromium, Firefox and WebKit in T1, T3, T11); network blocked offline |
| Facts restated faithfully, no facilitator content, no printed solution texts | deny-list only | semantic review per page with the author's source table |
| External URLs alive | (none) | `curl -sIL` status 200 (T6) |

**UX and accessibility.**
- The indicator is a `nav` with an accessible name and an `ol`, with exactly one `aria-current="step"`. The current state is not conveyed by colour alone.
- There is no horizontal page scroll at 720 px, and code blocks scroll on their own.
- The WebKit `file://` first load is sometimes blank (res-1 finding 13). A verifier may reload once and must record it. The troubleshooting page says how to recover.

**Code quality.**
- The checker is split into small modules (structure, coverage, hygiene, contrast) behind one entry point. Each rule is a pure function from text to errors, with self-tests. Anti-vacuity guards: exactly five plan lists parsed, at least N contrast pairs evaluated, all IA files found.
- `LevelProgress` is typed `current: 0 | 1 | 2 | 3 | 4`. MDX props are not type-checked, so it also throws at runtime on any other value, which the browser build turns into a failure.

**Documentation.**
- `guide/README.md` covers commands, the two-build rule, the checker, and the owner hand-over steps (enable Pages with source GitHub Actions; allow `factory` in the `github-pages` environment if it is restricted; set `GUIDE_DEPLOY=true`).
- The workflow header comment names the variable.
- No ADR is needed: the decisions are recorded in the spec folder and in D7.

**Security.**
- The gate expression appears exactly three times (deploy job, release job, Pages upload step).
- The verifier never dispatches the workflow, never sets the variable, never enables Pages and never creates a release. Evidence is read-only `gh api` calls.

## 5. Agent Coordination

- **Instances:**
  - `impl-t1` to `impl-t11`, sonnet, opus only for rework;
  - `qa-t1` to `qa-t11`, one verifier per task;
  - `qa-lead-1`, persistent across the cycle, which keeps the ledger `outbox/qa-ledger-11-workshop-guide.md`.
- **Channel rows to enable per task** (in addition to the default orchestrator rows):
  - `{from: impl-tN, to: qa-tN}` (handoff);
  - `{from: qa-tN, to: impl-tN}` (verdict);
  - `{from: qa-lead-1, to: impl-tN}` and `{from: qa-lead-1, to: qa-tN}` (META verdicts).
  - No impl-to-impl rows: tasks are sequential, and cross-task facts flow through the orchestrator and the ledger.
- **Handoffs and artifacts:**
  - impl to qa: `impl-tN/outbox/task-tN.md`, with changes, gate outputs, probe table, source table (content tasks) and follow-ups found.
  - qa to orchestrator and impl: `qa-tN/outbox/task-tN-verdict.md`, including the Playwright script path in the scratchpad.
  - qa-lead: ledger entry per task, plus pattern notes that the orchestrator folds into the next card.
- **Reusable verifier tooling.** `qa-t1` creates `<scratchpad>/guide-verify/check-site.mjs` (serve the Pages build, open the offline build, run the 720 px, copy-button, tabs, fonts and axe checks). Later verifiers reuse and extend it, and the orchestrator passes the path in each assignment. It is never committed.
- **Parallel workstreams.** None in execution. In parallel the orchestrator can prepare the follow-up list in the spec folder and copy the research into `references.md`. Spec 12 runs in another worktree: only the root `.gitignore` (T2) can conflict, as one added line in the Docusaurus section.
- **Mandatory QA lead checkpoints:** after T5 (manifest review against res-3 section 9), after T7 (patterns), and the Gate 2 site-wide review after T11.

## 6. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| The hash build hides broken links and crashing pages (exit 0) | The browser build is the gate and runs first in the same job; QA probes broken links only with `npm run build` |
| The `future.v4` space-form admonition renders as plain text with no error | A source rule bans `:::\w+ ` text; `check:built` bans literal `:::` |
| Infima derived defaults break contrast: navbar link hover is primary (teal on navy is about 1.3:1); admonition palettes are off-brand in dark mode | T3 declares navbar hover and active colours and the admonition variables for every type used; axe on the rendered pages catches anything undeclared |
| Spec D7 tension: "current level in blue" versus "every pair 4.5:1" and "current level filled teal" | Current step filled teal with a white label (light), mist with a navy label (dark); blue only as a non-text accent (3:1 rule) or large bold text in the non-text list. Confirm with the owner (Q1); not blocking |
| Dark-mode inactive steps with navy labels on a navy page fail | Inactive steps sit on snow or mist fill (as the light-mode text allows) with sage outline and navy label in both modes; the contrast pairs prove it |
| Sage on white is 2.8:1 | Never use sage for text or as the only cue |
| The checker becomes vacuous (parses nothing and passes) | Anti-vacuity guards plus mutation probes in every infrastructure card |
| The manifest is self-graded or invented | Authored in T5 before content; every snippet anchored to the pack; content tasks cannot edit it |
| A hex false positive on Markdown anchors (`#add-book-reviews`) | The colour rule is scoped to code files and inline styles; a self-test covers the anchor case |
| `tsc` with `allowJs` scans the build output | tsconfig excludes `build`, `build-offline`, `.docusaurus`; typecheck runs after both builds in T1 |
| The res-2 draft zips the folder, not its contents | T2 zips with step `working-directory: guide/build-offline` and `zip -r -q ../../workshop-guide-offline.zip .`; `unzip -l` evidence |
| The OFL licence does not travel with the zip (the fonts are bundled) | The licence goes to `guide/static/fonts/OFL.txt`, so both builds ship it; provenance file next to the fonts points to it |
| Google Fonts serves one variable woff2 for all three weights | Acceptable if `document.fonts` resolves 400, 600 and 700; the provenance records exactly what was downloaded |
| Pack drift after the cycle (spec 10 may edit the plan) | Check-time reading flags it on the next guide change and at the pre-merge sync with `factory` |
| Research artifacts are git-ignored and per worktree | The orchestrator copies res-1, res-2 and res-3 into the spec folder before T1 |
| Agents print solution answers or invent Windows commands | G5; the Windows-tab anchoring rule; semantic review |
| WebKit `file://` first load sometimes blank | Troubleshooting entry; reload allowed once in QA; real-Safari follow-up F10 |

## Task breakdown

Execution order: T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11. Every task also meets ground rules G1 to G9 and the standard gates (G2).

### T1. Scaffold the docs-only site and the check harness
- **module:** guide
- **dependsOn:** []
- **summary:**
  - Generate the official `create-docusaurus@3.10.2` classic TypeScript template into `guide/` with npm.
  - Prune it to docs-only: no `blog/`, `src/pages/`, `HomepageFeatures`, tutorial docs, template images, logo, favicon or footer. Delete the template's `guide/.gitignore` (the root file is the single source).
  - Write `docusaurus.config.ts` and `docusaurus.offline.config.ts` per res-1, the five-script contract, a minimal `docs/index.md` (`slug: /`), `scripts/check.mjs` with self-tests, and the first hygiene rules.
  - Write `guide/README.md` for maintainers.
- **acceptanceCriteria:**
  1. `guide/package.json`:
     - the six `@docusaurus/*` packages are exactly `3.10.2`;
     - the dependency and devDependency key sets equal the template's (res-1 finding 10), nothing more;
     - `guide/package-lock.json` is committed with lockfileVersion 3.
  2. After `npm ci` on Node v26.13.0, `typecheck`, `check`, `build`, `check:built` and `build:offline` all exit 0.
  3. Config (by grep):
     - `docusaurus.config.ts`: `url: 'https://maikkingma.github.io'`, `baseUrl: '/AgenticBookStore/'`, `trailingSlash: false`, `onBrokenLinks: 'throw'`, `onBrokenAnchors: 'throw'`, `markdown.hooks.onBrokenMarkdownLinks: 'throw'`, `blog: false`, docs `routeBasePath: '/'`, `future.v4: true` kept, no search plugin;
     - `docusaurus.offline.config.ts` spreads it and overrides only `baseUrl: '/'` and `future.experimental_router: 'hash'`.
  4. Falsifier probes (uncommitted, reverted, exit codes recorded):
     - `[x](/nowhere)` in index makes `npm run build` exit non-zero;
     - `./index.md#no-such-heading` makes it exit non-zero;
     - the same probe with `npm run build:offline` exits 0. Recorded only, to document why `build` is the gate.
  5. Hygiene rules, each with a passing and a failing `node:test` self-test: ASCII only (with a dedicated em-dash message), no word `lab` or `labs` (case-insensitive, word boundary), and the dependency allowlist. Probes that make `npm run check` fail:
     - an em dash built from code point 0x2014 in index;
     - "Lab 1" in index;
     - an extra dependency key.
  6. `check:built`:
     - fails on a build containing `<p>:::` (probe: `:::info Sync moment` space form);
     - fails when `build/` is missing;
     - passes otherwise.
  7. `build-offline/index.html` opened from `file://` in Chromium shows the index title. The Pages build served by `npx docusaurus serve` renders at `/AgenticBookStore/`.
  8. tsconfig excludes `build`, `build-offline`, `.docusaurus`. `npm run typecheck` exits 0 with both build folders present.
  9. The PR diff has no path under `guide/build`, `guide/build-offline`, `guide/node_modules`, `guide/.docusaurus`.
  10. `./mvnw clean verify` green. No file outside `guide/` changed.
- **fileScope:** `guide/package.json`, `guide/package-lock.json`, `guide/tsconfig.json`, `guide/docusaurus.config.ts`, `guide/docusaurus.offline.config.ts`, `guide/sidebars.ts`, `guide/docs/index.md`, `guide/scripts/**`, `guide/README.md`, template files under `guide/` (deletions); `guide/src/css/custom.css` stays as generated (T3 owns it)
- **verifier risks:**
  - The template generator pulling a different version, or prompting.
  - Leftover template links (navbar blog, footer, tutorial sidebar) that only fail in some builds.
  - `future.v4` removed to "fix" admonitions.
  - `allowJs` scanning the build output.
  - A self-test that cannot fail.
  - A literal em dash typed into the checker source (escape-decoding trap).
  - `node --test` discovering zero tests (require test count > 0 in the output).
  - Node version not 26.13.0.

### T2. CI workflow and ignore rule
- **module:** root
- **dependsOn:** [T1] (needs the script contract and the lockfile; `setup-node` caching fails without `guide/package-lock.json`)
- **summary:**
  - `.github/workflows/guide.yml` from the res-2 draft and the settled decisions. Build steps in order: `npm ci`, `npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`, `npm run build:offline`, zip the folder contents, upload the offline zip artifact, gated Pages artifact upload. Gated deploy and release jobs.
  - Root `.gitignore` gets `guide/build-offline/`.
- **acceptanceCriteria:**
  1. actionlint 1.7.12 (pinned digest from the brief) exits 0 with no findings on `guide.yml`.
  2. Actions and secrets:
     - every `uses:` is a 40-hex SHA plus a `# vX.Y.Z` comment, exactly the six briefed pins;
     - no `configure-pages`, `workflow_dispatch`, `pull_request_target` or `secrets.`.
  3. Triggers:
     - `push` on branches `[factory]` with paths `guide/**` and `.github/workflows/guide.yml`;
     - `pull_request` with the same paths.
  4. Permissions: top level `contents: read`; `pages: write` and `id-token: write` only on deploy; `contents: write` only on release.
  5. The gate:
     - `github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true'` appears exactly on the deploy job, the release job and the Pages-artifact step (grep count 3);
     - the header comment names `GUIDE_DEPLOY` and the three owner hand-over steps.
  6. Timeouts and concurrency:
     - `timeout-minutes` on every job, and long steps lower than their job;
     - top-level concurrency equal to ci.yml's;
     - deploy uses group `pages` with `cancel-in-progress: false`.
  7. `setup-node` with `node-version: '26.13.0'`, `cache: npm`, `cache-dependency-path: guide/package-lock.json`.
  8. Steps run in the order above in one job. No `continue-on-error` and no `if: always()` on check or build steps.
  9. `unzip -l` on the zip shows `index.html` at the root, with no `build-offline/` prefix. Evidence: the PR run's artifact via `gh run download`, or the identical zip command run locally from a script file.
  10. The task PR's own run: build job green; deploy and release skipped. Read-only `gh api` shows Pages 404, no release `workshop-2026`, no `gh-pages` ref.
  11. After both builds, `git status --porcelain` lists nothing under `guide/build-offline`. ci.yml is unchanged. ASCII only. `./mvnw clean verify` green.
- **fileScope:** `.github/workflows/guide.yml`, `.gitignore`
- **verifier risks:**
  - The draft zipping the folder instead of its contents.
  - `defaults.run.working-directory` assumed to apply to `uses:` steps (the Pages `path` must stay `guide/build`).
  - The gate written as a boolean (`== true`).
  - The release job given checkout or extra permissions.
  - A conflict with spec 12's `.gitignore` edits.
  - The verifier must never dispatch, set variables or enable Pages.

### T3. OpenValue brand, fonts and contrast check
- **module:** guide
- **dependsOn:** [T1, T2] (T2 is a process dependency: every guide PR from here on is gated by CI)
- **summary:**
  - `custom.css` holds the seven brand values as custom properties and maps them onto Infima for light and dark mode: primary plus six shades, text, headings, background, links, navy navbar with white text and explicit hover and active colours, code and inline code backgrounds, admonition variables for every type the guide uses, and `LevelProgress` tokens. These are the only colour values in the guide.
  - Montserrat latin 400, 600 and 700 woff2 in `src/css/fonts/` with `@font-face` (`font-display: swap`). Provenance (URL, sha256, date) recorded. OFL in `static/fonts/OFL.txt`.
  - A contrast module that reads `custom.css` by variable name: text pairs at least 4.5:1, non-text pairs at least 3:1. Brand assertions. A colour-literal rule.
  - Config: `colorMode`, navbar title "Crafting your Agentic Harness" (the plan's title), prism `additionalLanguages`.
- **acceptanceCriteria:**
  1. Brand assertions in `npm run check`:
     - light `--ifm-color-primary: #014046`; dark `#CBD9DA`;
     - all seven brand values present;
     - `--ifm-font-family-base` equals exactly `Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
  2. Colour-literal rule: no hex, `rgb()`, `hsl()` or other colour function in `guide/src` (except `custom.css`), `guide/docs` or `guide/*.ts`.
     - Self-tests include the `#add-book-reviews` anchor false positive.
     - Probe: `color: #fff` in a new CSS module fails.
  3. Contrast: `npm run check` prints each pair with its ratio.
     - Text pairs are at least 4.5 and non-text pairs at least 3.0, in both modes.
     - If dark mode is switched off, the light pairs pass and the script verifies `disableSwitch: true` and `respectPrefersColorScheme: false`. It also fails if a dark block exists while the switch is off, or if the switch is on and no dark block exists.
     - Required pairs: body text, headings, links, navbar text, navbar hover and active, inline code, code block text, admonition text for each admonition type used (note, info, tip at least), `LevelProgress` current label on current fill, and inactive label on inactive fill.
     - The script has no colour literal, resolves `var()`, and fails on a missing variable or fewer pairs than declared.
  4. No normal-size text pair uses blue `#3D7DC9`.
  5. Fonts:
     - referenced as `url('./fonts/...') format('woff2')`;
     - the provenance file lists URL and sha256 per file, and the verifier's `shasum -a 256` matches;
     - the OFL text is present in both `build/` and `build-offline/`;
     - the strings `fonts.googleapis`, `gstatic` and `@fontsource` appear neither in `guide/` (excluding `node_modules`) nor in the build output.
  6. Playwright, on the Pages build and on offline `file://` with network blocked, in Chromium, Firefox and WebKit:
     - `document.fonts` reports Montserrat 400, 600 and 700 loaded;
     - body font-family starts with Montserrat;
     - code is monospace.
  7. axe color-contrast: zero violations on the index in light mode, and in dark mode if kept, at 720 and 1280 px.
  8. Navbar shows the title, no logo and no blog link. A colour-mode toggle is present only if dark mode is kept. No dependency added.
- **fileScope:** `guide/src/css/custom.css`, `guide/src/css/fonts/**`, `guide/static/fonts/OFL.txt`, `guide/scripts/**`, `guide/docusaurus.config.ts` (themeConfig only), `guide/README.md`
- **verifier risks:**
  - Infima defaults not overridden: navbar hover in teal on navy; dark admonitions; footer if left.
  - Prism setting `--prism-background-color` inline, overriding a CSS variable.
  - A contrast script that hard-codes values (duplication).
  - The variable-font weight mapping.
  - The licence missing from the zip.
  - Blue used for small text.
  - A sage text pair.

### T4. LevelProgress, page skeletons and structure checks
- **module:** guide
- **dependsOn:** [T3] (the component consumes T3's tokens)
- **summary:**
  - `src/components/LevelProgress.tsx`, with an optional CSS module using `var(--...)` only. It is static: a `nav` with an accessible name and an `ol` of five steps, labels "Level 0" to "Level 4", and `aria-current="step"` with a visible bold label on the current step. It throws on a value outside 0 to 4.
  - Skeletons of all eleven IA pages, following the page grammar: final titles, at-a-glance boxes, final "Level cleared when" lists, contractual anchors, the marker, sidebar order, `reference/_category_.json`.
  - The structure module of the checker, which reads the plan at check time.
- **acceptanceCriteria:**
  1. Files and routes:
     - exactly the eleven IA files exist, plus `reference/_category_.json`;
     - routes: `/`, `/setup-check`, `/level-0-break-the-agent`, `/level-1-context-engineering`, `/level-2-give-your-agent-eyes`, `/level-3-behavioral-control`, `/level-4-spec-driven-development`, `/wrap-up`, `/reference/cheat-sheet`, `/reference/architecture`, `/reference/troubleshooting`;
     - the rendered sidebar order is Welcome, Setup check, Level 0 to 4, Wrap-up, Reference (Cheat sheet, Architecture, Troubleshooting).
  2. Each level page's title is the exact spec name. The rendered H1 equals it. There is no body `# ` H1.
  3. Checker: the first element after frontmatter and imports is `<LevelProgress current={N} />`, with N equal to the file's level.
     - Rendered: five steps with exactly one `aria-current="step"`, on N.
     - Probe: `current={7}` makes `npm run build` fail.
  4. Checker, at-a-glance box:
     - Goal text matches the plan's Goal (whitespace-normalised, first letter case-insensitive);
     - Time equals the plan heading minutes;
     - catch-up block is exactly `git stash -u && git switch <branch>` per the header table;
     - Compare appears only on L1 to L3, with the right branch.
  5. Checker, checklist:
     - `## Level cleared when` is followed by a task list equal to the plan's bullets (same order and count), and nothing follows it;
     - the plan is read from `../docs/spec-pack/workshop/workshop-plan.md`, with a clear failure message if it is missing;
     - exactly five non-empty plan lists are parsed (anti-vacuity);
     - the L4 list also equals the SDD guide's list.
  6. Checker, admonitions:
     - space-form admonitions fail;
     - `:::info[Sync moment]` must contain "Look up, Maik demos this now";
     - `:::tip[Checkpoint]` must contain a fenced block and a line starting with "Expected";
     - a non-skeleton level page has exactly one sync moment.
  7. All contractual anchors resolve: a probe link to `./reference/troubleshooting.md#anthropic-api-key` from index builds; a link to a missing anchor fails.
  8. The marker exempts only body-content rules (sync count, coverage), never structure. `npm run check` lists the pending pages.
  9. Self-tests for every rule. Mutation probes, each of which must make `npm run check` fail:
     - swap two L3 checklist items;
     - reword one L1 item;
     - add a paragraph after the checklist;
     - put `LevelProgress` below the box;
     - `current={2}` on the L3 page;
     - change the L2 catch-up branch;
     - change L0 Time to 30.
  10. The component passes the colour rule and `tsc`, renders in both builds, and causes no horizontal page scroll at 720 px.
- **fileScope:** `guide/src/components/**`, `guide/docs/**`, `guide/scripts/**`, `guide/sidebars.ts`
- **verifier risks:**
  - JSX first-element detection with blank lines and imports.
  - Plan bullets wrapped over continuation lines.
  - Inline code inside checklist items.
  - `{/* */}` comments and `{#id}` heading ids in `.md` files.
  - Number-prefix stripping in doc ids.
  - `index.md` sidebar position.
  - Dark-mode tokens of the component.
  - Throwing in the component only in development builds.

### T5. Pack-anchored coverage manifest and content rules
- **module:** guide
- **dependsOn:** [T4]
- **summary:** Author `guide/scripts/coverage.mjs` from res-3 sections 2, 5, 6 and 9 before any content exists. Each entry has `id`, `source` (plan, sdd, briefs, spec10, spec09), `section`, `page`, `kind` (`command` or `text`), `snippet` and an optional `pageSnippet`. Add the coverage module and the content rules:
  - (a) every snippet exists in its pack section;
  - (b) on non-skeleton pages, the page snippet exists, inside a fenced block when its kind is `command`;
  - (c) every line in a Windows PowerShell or PowerShell tab code block exists in the pack;
  - (d) a deny-list: `facilitator`, `expected findings`, `fallback kit`, `rehearsal`, `spec-pack`, `workshop-plan`, `sdd-framework-guide`, `feature-briefs`, `spec NN`, `.claude/comms`;
  - (e) `ANTHROPIC_API_KEY` never in an assignment, export or `$env:` form, and the token only on the setup and troubleshooting pages;
  - (f) no raw `<img`;
  - (g) tab `groupId` and label sets per the page grammar;
  - (h) the L0 observation card shape when the L0 page is non-skeleton.
- **acceptanceCriteria:**
  1. Every res-3 section 9 row is accounted for, either as manifest entries or in `manualRows` with a reason (a prose step). The verifier ticks each row.
  2. `npm run check` prints "anchored N/N".
     - Probe: an entry whose snippet is absent from the pack fails.
  3. With every content page still a skeleton, the check passes and prints the pending entries per page.
  4. Probes that must fail:
     - remove the marker from the L0 skeleton (it lists the missing L0 entries);
     - a PowerShell tab with `Remove-Item -Recurse foo`;
     - `export ANTHROPIC_API_KEY=x` on setup;
     - the word "facilitator" on a page;
     - `<img src="/x.png">`;
     - an unknown tab label.
  5. Self-tests per rule (pass and fail). Only `guide/scripts/**` changed.
- **fileScope:** `guide/scripts/**`
- **verifier risks:**
  - Snippet escaping (nested backticks in `` !`git status --short` ``; `$CLAUDE_PROJECT_DIR`).
  - The plan's `...` path for `CatalogMcpTools.java` needs a `pageSnippet` (the real path from res-3).
  - SDD Part 2 section parsing.
  - Whitespace across wrapped plan lines.
  - A deny-list false positive.
  - Entries silently excluded to make the check pass.

### T6. Index, setup check and wrap-up
- **module:** guide
- **dependsOn:** [T5]
- **summary:** Fill `index.md`, `00-setup-check.md` and `06-wrap-up.md` per res-3 sections 2.1, 2.2 and 2.8.
  - **Index:** welcome, agenda (with the Keynote and Break rows), the five levels with their Goals, how to use the guide.
  - **Setup:** prerequisites from workshop plan lines 45-47 and 59-66 and spec 09 lines 38-39 (no participant email exists); commands; `jdtls` tabs; `which jdtls` and `Get-Command jdtls` tabs; key export tabs; `/model sonnet`; an `ANTHROPIC_API_KEY` warning linking to `#anthropic-api-key`; checkpoints with pack-stated outputs only.
  - **Wrap-up:** who cleared which level (links to each `#level-cleared-when`), the recap map, the verified Academy, OpenCode and Agent SDK URLs (course title "Introduction to Model Context Protocol"), the key being revoked, and the take-home install table from SDD lines 146-152 with its pack-stated Windows variants.
- **acceptanceCriteria:**
  1. The marker is removed from the three pages, and coverage for IX, SU and WU is green.
  2. Every listed external URL returns 200 (`curl -sIL`).
  3. Tabs only where the pack states both forms. `git clone ... && cd AgenticBookStore` has no Windows tab (recorded as a follow-up).
  4. The source table in the outbox covers every section. The semantic review finds no invented fact and no facilitator content.
  5. Playwright on the three pages, in both builds: 720 px with no page scroll; copy buttons equal code blocks; tab sync by `groupId`.
- **fileScope:** `guide/docs/index.md`, `guide/docs/00-setup-check.md`, `guide/docs/06-wrap-up.md`
- **verifier risks:**
  - `<key on screen>` in prose.
  - The setup block copied with interactive `claude` lines inside a multi-command block.
  - Promising "under a minute" without the plan's wording (follow-up F11).
  - Inventing a Windows form.
  - Mentioning a participant email.

### T7. Level 0 and Level 1 pages
- **module:** guide
- **dependsOn:** [T5]
- **summary:** Fill L0 per workshop plan lines 85-122:
  - the task text verbatim as a blockquote, steps 1 to 5, the observation card, reset, and the column meanings (participant-useful part of the debrief); no expected findings.
  
  Fill L1 per lines 128-169:
  - steps 1 to 6, the `CLAUDE.md` content list, the three rule files with `paths:`, hierarchy, context rot, the `/context` checkpoint from the plan and spec 10 lines 113-115;
  - link `solution/level-1` without printing its files.
- **acceptanceCriteria:**
  1. The markers are removed, and coverage for L0 and L1 is green, including the observation card shape.
  2. Exactly one sync moment per page.
  3. No text of spec 10 lines 25-109 is printed. The verifier compares distinctive lines.
  4. Source table plus semantic review. Playwright standard.
- **fileScope:** `guide/docs/01-level-0-break-the-agent.mdx`, `guide/docs/02-level-1-context-engineering.mdx`
- **verifier risks:**
  - Printing answers.
  - The `~/.claude/CLAUDE.md` Windows path invented.
  - Claims about auto mode or `/rewind` embellished.
  - `<topic>` in prose.
  - The card as a rendered table only, with no copyable block.

### T8. Level 2 page
- **module:** guide
- **dependsOn:** [T5]
- **summary:** Fill Part A (LSP: binary check "Windows: inside WSL", plugin install with the marketplace fallback, scope note, prompts verbatim, the diagnostics checkpoint, the stuck path) and Part B (MCP: start the distributor, `claude mcp add`, `/mcp` checkpoint, `mcp__distributor__*`, the ACL contract test prompt, the real `CatalogMcpTools.java` path), plus the `list_new_releases(days)` stretch.
- **acceptanceCriteria:**
  1. The marker is removed and L2 coverage is green.
  2. Checkpoints use the pack outputs only: `Found N new diagnostic issues ...`, distributor connected (4 tools per spec 10), `mcp__distributor__*`.
  3. The real path `distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java` exists in the repo.
  4. Anchors from the page grammar are present.
  5. Source table, semantic review, Playwright standard.
- **fileScope:** `guide/docs/03-level-2-give-your-agent-eyes.mdx`
- **verifier risks:**
  - `$PATH` and quotes inside the error string in prose.
  - The plugin-declaration sentence that depends on spec 10 (follow-up F4).
  - `.mcp.json` behaviour for catch-up participants described without a source.
  - "facilitator demo" copied verbatim (deny-list).

### T9. Level 3 page
- **module:** guide
- **dependsOn:** [T5]
- **summary:** Fill:
  - Part A: the permissions JSON, rule order, modes.
  - Part B: the hook JSON; script requirements; the bash `tests-before-commit.sh` quoted from spec 10 lines 273-295 as a "compare with" reference; `git config core.hooksPath .githooks`; the HALF_UP test; why both.
  - The optional Stop hook (spec 10 lines 297-314 optional quote; say the solution branch has it and the exercise does not need it).
  - Part C: the skill, the cancel-order prompt, `/ship`.
  - The stretch subagent.
  - Checkpoints: red commit blocked with exit 2; `git commit --no-verify` denied; `.env` read denied.
  - PowerShell hook variants are a follow-up pointer to `#windows-hooks`.
- **acceptanceCriteria:**
  1. The marker is removed and L3 coverage is green.
  2. Quoted script lines are anchored to spec 10 by manifest entries.
  3. No PowerShell script text.
  4. JSON blocks equal the plan's.
  5. Source table, semantic review, Playwright standard.
- **fileScope:** `guide/docs/04-level-3-behavioral-control.mdx`
- **verifier risks:**
  - `$ARGUMENTS` and `` !`cmd` `` in MDX prose.
  - The JSON fragment presented as a complete file.
  - The Stop hook presented as required.
  - The `/ship` scope check from spec 10 (C4: the plan text governs).

### T10. Level 4 page
- **module:** guide
- **dependsOn:** [T5]
- **summary:** Fill:
  - the track table (workshop plan lines 303-307) and the common start (SDD lines 93-102);
  - Track A, B and C tabs with the steps and gotchas from SDD lines 104-137;
  - the sync moment;
  - feature briefs A, B and C verbatim with their questions, and Brief C's two routes (feature briefs lines 6-36);
  - the time-fit hint from feature briefs line 41; "use your own change name" for briefs B and C.
- **acceptanceCriteria:**
  1. The marker is removed and L4 coverage is green, including all track commands in fenced blocks.
  2. Feature briefs lines 43-45 (facilitator notes) are absent.
  3. `solution/level-4-*` is not linked.
  4. Source table, semantic review, Playwright standard (track tabs switch, code inside tabs copyable).
- **fileScope:** `guide/docs/05-level-4-spec-driven-development.mdx`
- **verifier risks:**
  - `start/level-4-<track>` in prose.
  - Mixing `openspec` (terminal) and `/opsx:` (Claude Code).
  - Nested admonitions in tabs.
  - The OpenSpec CLI availability gap (follow-up F8, do not invent an install step on the branch).

### T11. Reference pages, marker ban and site-wide pass
- **module:** guide
- **dependsOn:** [T6, T7, T8, T9, T10]
- **summary:**
  - Cheat sheet: two tables, Claude Code commands and files, then the Level 4 commands.
  - Architecture: onion as a text block, packages, the nine rules with the process exception, naming suffixes, the ArchUnit referee; link to `docs/architecture.md` on `main`.
  - Troubleshooting: all contractual sections, including the Safari blank-page line, and the `ANTHROPIC_API_KEY` pitfall without an invented unset command.
  - Switch the checker so that any skeleton marker fails.
  - The site-wide verification.
- **acceptanceCriteria:**
  1. The checker fails on any marker (self-test plus probe). `npm run check` reports 0 pending and all coverage entries green.
  2. Cheat-sheet A4 print in Chromium is at most 2 pages, the second only for the Level 4 table, with the count recorded.
  3. Site-wide Playwright, all eleven pages, in the Pages build and in the offline zip extracted into a path with spaces:
     - Chromium, Firefox and WebKit (one WebKit reload allowed and recorded);
     - network blocked offline;
     - 720 px with no page scroll;
     - copy buttons equal code blocks;
     - axe color-contrast zero violations in both modes.
  4. The architecture page makes no "enforced package list" claim (res-3 C11).
  5. The follow-up list is complete in the outbox.
- **fileScope:** `guide/docs/reference/**`, `guide/scripts/**`
- **verifier risks:**
  - Regressions on earlier pages.
  - Over-long cheat-sheet rows that scroll at 720 px.
  - Troubleshooting inventing commands.
  - The marker ban implemented as a warning.

## Follow-ups (not tasks of this cycle; run after `spec-10-done`, before the spec PR merges)

- F1. Spec 11 task 6: run every guide command on a clean machine against the published `main`.
- F2. PowerShell hook scripts and the Windows hook command text (spec 10 task 4), then the bash and PowerShell hook tabs.
- F3. The `docs/lsp-setup.md` link on `solution/level-2`.
- F4. The plugin-declaration sentence (workshop plan line 193), after spec 10's re-verification.
- F5. Links to `docs/architecture.md` and the README on `main`.
- F6. `main`'s `.gitignore` contains `notes/`.
- F7. All solution and start branch links resolve (`curl`). The build does not check external links.
- F8. Real paths on `start/level-4-*` (SDD line 109), and whether the OpenSpec CLI is preinstalled.
- F9. Windows variants the pack does not state: `git restore . && git clean -fd`, `git clone ... && cd`, `git stash -u && git switch` in Windows PowerShell 5.1, `tree .claude`, `chmod +x`. These need the Windows story decision (res-3 C1).
- F10. A real Safari check, and Windows browsers opening the offline copy from `file://`.
- F11. The "under a minute" claim after spec 12's frontend build.
- F12. The owner's deploy gate: enable Pages, the environment branch policy, `GUIDE_DEPLOY=true`. The first deploy verifies extensionless URLs. The release zip.
- F13. The participant email does not exist; the setup page is the source until one is written.

## Open questions / decisions needed

- Q1 (owner, not blocking): D7 says "current level and navbar accent in blue (large or bold only)" and also "current level filled teal", while the contrast scenario demands 4.5:1 for every pair. The plan applies the strict reading: teal fill with a white label for the current step, and blue only as a non-text accent at 3:1. Confirm or overrule before T3.
- Q2 (owner, not blocking): the release is created public when the gate opens. Should `gh release create` use `--draft` (res-2)?
- Q3 (orchestrator): copy res-1, res-2 and res-3 into the spec folder before T1. `.claude/comms/` is git-ignored and local to one worktree.
- Q4 (orchestrator, not blocking): `guide.yml` paths stay `guide/**` plus the workflow file, as in the spec. Adding `docs/spec-pack/workshop/**` would re-run the drift check on pack edits, but would also redeploy on pack-only pushes. Recommendation: do not add it.
- Q5 (orchestrator): the OFL licence goes to `guide/static/fonts/OFL.txt`, so the zip carries it. The fonts stay in `src/css/fonts/` as settled. Flagged because spec 11 says only "with its SIL Open Font License file".
- Proposal for `agent-os/standards/`: "guides restate a source of truth; the checker reads that source at check time, and a manifest snippet must exist in both the source and the page." Recorded in my memory as a spec 11 decision until the owner adopts it.

## Resume point

Completed. The strategy and task breakdown are final. The handoff was sent to the orchestrator.
