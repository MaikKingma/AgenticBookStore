---
spec: 15-guide-content-revision
pack: docs/spec-pack/specs/15-guide-content-revision.md
decision: D19
base: factory
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-11T01:45:37Z
qa-lead: on
---

# Plan: spec 15 guide content revision

Shape: `shape.md` (scope, research summary, shaping decisions D-S1 to D-S11). QA strategy: `qa-strategy.md` (snapshot of
the QA lead's strategy: tiers, Gate 1 findings G-1 to G-9, rewrites U1 to U12, added criteria A1 to A12, common
directive C0-1 to C0-17, task directives, qa-final focus). Branches: spec `sdd/15-guide-content-revision/spec`, tasks
`sdd/15-guide-content-revision/T<n>`, each task PR squash-merged into the spec branch; the spec PR into `factory` is the
owner's merge.

| id | module | dependsOn | batch | summary | qaTier | qaModel | pr | branch | verdict | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | guide | [] | 1 | remove every sync moment, rebalance the timetable, ban sync moments in the checker | critical | sonnet | #171 | sdd/15-guide-content-revision/T1 | PASS r1 | ENDORSE |
| T2 | guide | [T1] | 2 | Level 1 step 3 rule file spelled out, step 5 `/context` text mock, Level 1 creativity tip (A1) | elevated | sonnet | | | | |
| T3 | guide | [T1] | 2 | Level 2 step 5 debugging section in `<details>`, demo text removed, Level 2 creativity tip (A1) | elevated | sonnet | | | | |
| T4 | guide | [T1] | 2 | Level 4 prepared setup and brownfield order per track, pinned OpenSpec install, Node sentence, Level 4 creativity tip (A1) | elevated | sonnet | | | | |
| T5 | guide | [] | 1 | wide tables in focusable labelled regions, A4 exclusion dropped, full 390 px sweep | critical | sonnet | | | | |
| T6 | guide | [T1] | 2 | Level 3 creativity tip (A1) | standard | sonnet | | | | |

Amendment A1 (owner request: parallelise the updates per level): T5 runs beside T1 and T2, T3, T4, T6 run beside each
other after T1, each in its own git worktree (own build dirs, own ports), never two agents in one worktree. Task PRs
merge into the spec branch one at a time; a later branch merges the spec tip and re-proves its gates before its merge.
qaDirective per task = `qa-strategy.md` C0 plus that task's block under "Task directives" plus every
`## QA feed-forward` entry below. qaModel is sonnet for every task by owner policy (see Decisions); the QA lead's opus
meta-review compensates on the critical tasks.

## Common to every task (C)

- ASCII only, no em dashes, plain English; no "facilitator" on guide pages (say "Maik and Tom walk around"); no local
  absolute home paths in committed files; no new npm dependency.
- Never skip, disable, delete or weaken a test or check. Where a rule, fixture, hash or coverage entry pins changed
  text, update it in the same task with the reason so it still guards the same thing; the outbox lists every pin
  changed (old -> new) and every edited `guide/scripts/**` file with its reason.
- Coverage: after the task every per-page floor in `coverage-manifest.test.mjs` equals the live entry count and never
  goes below its value at the task's base (a removed entry is replaced by one pinning the replacement text);
  `anchored N/N` stays complete; `WP:N` row labels stay opaque ids; `SWEEP_EXCLUDED` `plan:<line>` keys are renumbered
  when plan lines shift (old -> new list, reasons unchanged), and a new key needs a reason naming the pin that covers
  the literal plus a deletion mutant of that pin (A3, A4).
- Every new command line on a page exists as a whole line or inline code span in one of the five SOURCE_FILES (added
  to `docs/spec-pack/workshop/workshop-plan.md` when new) and is pinned by a coverage entry.
- Untouched: level branches, `main`, tags, every pack file other than `workshop-plan.md` (notably `progress.md`,
  `decisions.md`, `roadmap.md`), spec 14 branding, `guide/scripts/fixtures/skeleton-docs/**` hash-equal (G-9) unless a
  task says otherwise.
- Agents never run Claude Code, never install plugins or marketplaces, never execute install scripts; framework CLIs
  only in a throwaway sandbox (`mktemp -d "$TMPDIR/<prefix>.XXXXXX"`, isolated HOME, `OPENSPEC_TELEMETRY=0`).
- Local gates: in `guide/`: `npm ci`, `npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`,
  `npm run build:offline`; the rendered rig (Playwright + axe-core in a scratchpad folder; Chromium, Firefox, WebKit;
  Pages build via `docusaurus serve` on a free port and the offline build over `file://`; light and dark; 1280 and
  390 px) for the pages the task touches; the referee
  `env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify` from the
  repo root (60 s budget). Checker regression: node:test total at least the base's (713 at 7ded994), no lost titles
  except listed renames.

## Tasks

### T1 Remove sync moments (pack item 1)
- dependsOn: []
- fileScope: `guide/docs/index.md`, `guide/docs/01-level-0-break-the-agent.mdx`,
  `guide/docs/02-level-1-context-engineering.mdx`, `guide/docs/03-level-2-give-your-agent-eyes.mdx`,
  `guide/docs/04-level-3-behavioral-control.mdx`, `guide/docs/05-level-4-spec-driven-development.mdx` (Sync moment
  boxes only), `guide/README.md`, `docs/spec-pack/workshop/workshop-plan.md`, `guide/scripts/rules/structure.mjs`,
  `guide/scripts/structure.test.mjs`, `guide/scripts/structure-check.test.mjs`, `guide/scripts/lib/structure-fixtures.mjs`,
  `guide/scripts/rules/content.mjs`, `guide/scripts/content.test.mjs`, `guide/scripts/check-content.test.mjs`,
  `guide/scripts/coverage.mjs`, `guide/scripts/rules/coverage.mjs`, `guide/scripts/coverage.test.mjs`,
  `guide/scripts/coverage-manifest.test.mjs`, `guide/scripts/built.test.mjs` (sample title only, if needed).
- acceptanceCriteria:
  1. No sync moment remains in the guide: the five `:::info[Sync moment]` boxes, the welcome page timetable cells,
     `index.md` line 17 ("Each level opens with a short live demo") and line 49, and the mention in `guide/README.md`
     are removed or reworded. `grep -rli 'sync moment'` over `guide/docs`, `guide/build` and `guide/build-offline`
     (HTML and JS) prints nothing, with a positive control on the base build (U2); no rendered page has an admonition
     title containing "sync moment" (11 pages, DOM check).
  2. The welcome page says that Maik and Tom walk around (one sentence, pinned by a new IX coverage entry) and nothing
     describes a facilitator demo (U1).
  3. Timetable per D-S1 in `index.md` and `workshop-plan.md` (timetable rows, level sections, Part headings): Level 0
     solo 22, Level 1 solo 32, Level 2 solo 18 (LSP 10, MCP 8), Level 3 solo 28 (permissions 4, hooks 14, skill and
     command 10), Level 4 solo 42; every level total, debrief, the break and the 240 minutes unchanged and every other
     agenda cell byte-identical (proved by an arithmetic script); five IX entries pin the new Format cells (A2).
  4. `workshop-plan.md` drops every sync moment the same way (lines named by res-1: 15, 29-34, the per-level sync
     blocks and Part minutes). The Level 2 demo text (step 5, plan 200-202 and 343, `reference/troubleshooting.md`
     line 50, entry L2-17) is NOT changed by T1 (it is T3's).
  5. Structure rule: the "exactly one Sync moment per finished level page" rule and `SYNC_PHRASE` are replaced by a
     ban: an admonition of any type whose title contains "sync moment" in any case and spacing fails on any page,
     skeletons included (A11). Witnesses and mutants per `qa-strategy.md` T1 block (M1 to M5); the structure test
     samples that used the Sync moment box are retitled and still kill their rules (M8 to M10).
  6. Content deny-list gains `sync moment`, `maik demos` and `live demo` with line-break and double-space witnesses
     (M6, M7). Each removed guide sentence, re-inserted verbatim on its page one at a time, fails `npm run check`
     (A10).
  7. Coverage: L0-01, L1-01, L2-01, L3-01 and L4-05 are retargeted to the replacement sentences on page and plan
     (each proved by deleting the new sentence); floors equal live counts; the four `SWEEP_EXCLUDED` keys of deleted
     plan lines are removed and the rest renumbered with reasons unchanged; no new key.
  8. All local gates green; rendered check of the 7 touched pages per C.

### T2 Level 1 rule file and `/context` mock (pack items 3, 4)
- dependsOn: [T1]
- fileScope: `guide/docs/02-level-1-context-engineering.mdx`, `docs/spec-pack/workshop/workshop-plan.md`,
  `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`, `guide/scripts/level-1-rule.test.mjs`
  (new, A5).
- acceptanceCriteria:
  1. Step 3 names the exact path `.claude/rules/domain.md` and shows its complete content in a fenced block whose
     lines are byte-equal to `git show origin/solution/level-1:.claude/rules/domain.md` and to the staged pack copy
     under `docs/spec-pack/level-branches/level-1/`; it names the other two rule files (`data-and-migrations.md`,
     `testing.md`). A committed node:test (`level-1-rule.test.mjs`) extracts the block from the page and compares it
     with the staged pack copy (A5).
  2. One sentence explains the header (U5): it is YAML frontmatter, `paths` lists globs relative to the project root,
     and the rule loads only when Claude reads or edits a matching file, consistent with
     https://code.claude.com/docs/en/memory.
  3. Step 5 shows an ASCII text mock of `/context` output in a fenced block, preceded or followed by a sentence saying
     it is illustrative (pinned; deleting it fails a check). The mock is internally consistent (the category numbers
     add up to the total, the grid matches the shares) and its category labels trace to the official docs (U3).
  4. 3 to 5 annotations of at most 30 words each cover: messages and tool results growing, free space shrinking, when
     to run `/compact focus on <topic>`, when to run `/clear` (U4).
  5. The Level 1 checkpoint that checks a loaded rule names `/memory` (the documented place that lists loaded rules,
     G-7); `workshop-plan.md` mirrors steps 3 and 5 (lines 148-151 and the context-rot step); new sweep literals are
     covered; the L1 floor equals the live count.
  6. All local gates green; rendered check of the Level 1 page per C (the code block's rendered lines equal the file
     lines).

### T3 Level 2 debugging section (pack item 5)
- dependsOn: [T1, T2]
- fileScope: `guide/docs/03-level-2-give-your-agent-eyes.mdx`, `guide/docs/reference/troubleshooting.md`,
  `docs/spec-pack/workshop/workshop-plan.md`, `guide/scripts/rules/content.mjs`, `guide/scripts/content.test.mjs`,
  `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`.
- acceptanceCriteria:
  1. "Still stuck after two minutes: watch the demo and continue with Part B" is gone; the guide and the plan carry no
     demo instruction (plan line 340 may keep the word "demo" only if it is not a demo step; the verifier quotes it).
     Content deny-list gains `watch the demo`; re-inserting the old sentence fails `npm run check` (A1).
  2. Part A step 5 holds exactly one `<details>` with a `<summary>` title, closed by default (no `open` attribute),
     content hidden until click or Enter (U7). Inside: a ready-to-paste diagnosis prompt in one fenced block (ASCII, no
     placeholders, read-only, names the checks; U6) and the checks: `jdtls` on the PATH of the shell that starts
     `claude`, Java 21, the plugin enabled and its Errors tab in `/plugin`, start a new session after installing, the
     server starts when Claude first touches a `.java` file. Every item traces to
     https://code.claude.com/docs/en/plugins/code-intelligence or .../plugins/troubleshooting.
  3. The Part B fallback ("continue with Part B") stays visible outside the `<details>` (A9).
  4. `reference/troubleshooting.md` line 50 and `workshop-plan.md` lines 200-202 and 343 are rewritten the same way;
     `workshop-plan.md` line 59 says Java 21 (D-S5); a JDK grep over the guide and the plan shows no 17 for jdtls.
  5. L2-17 is retargeted to the Part B fallback sentence and the prompt gets an entry of kind `command` (both proved by
     deletion); floors equal live counts; sweep keys renumbered.
  6. All local gates green; rendered: click and Enter open the details in both builds and three engines, axe 0
     violations closed and open in light and dark, the summary's contrast computed and at least 4.5:1.

### T4 Level 4 getting-started order and OpenSpec install (pack items 6, 7)
- dependsOn: [T1, T3]
- fileScope: `guide/docs/05-level-4-spec-driven-development.mdx`, `guide/docs/00-setup-check.md`,
  `guide/docs/reference/cheat-sheet.md` (only if it shows the OpenSpec install), `docs/spec-pack/workshop/workshop-plan.md`,
  `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`, `guide/scripts/content.test.mjs` and
  `guide/scripts/check-content.test.mjs` (only for new PowerShell-line witnesses).
- acceptanceCriteria:
  1. Each track says what its start branch already holds, every claim mapped to a path in
     `git ls-tree -r origin/start/level-4-<track>` at the `git ls-remote` sha (U9): OpenSpec initialised
     (`openspec/config.yaml`, one spec under `openspec/specs/`) without the CLI; Spec Kit `.specify/` and a written
     constitution; Agent OS product docs plus standards written for this repo and indexed (the word "discovered" is
     gone, A7). The page and plan line 19 no longer claim an installed framework where none is (G-4, A7).
  2. Each track gains a short "For your own brownfield project" note (at most 8 lines and 120 words, U8) with the
     framework's documented first steps as an ordered command list: OpenSpec `openspec init`, project context in
     `openspec/config.yaml`, then the `/opsx` flow; Spec Kit `specify init` in the existing repo, then
     `/speckit-constitution` before `/speckit-specify`; Agent OS `/discover-standards`, `/inject-standards`,
     `/plan-product`, then `/shape-spec` (D-S6). Each note cites the doc page in `references.md`. The existing track
     commands stay unchanged (they match the docs).
  3. The setup page and the Level 4 OpenSpec track give the pinned install route (U10): bash
     `curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v1.16.0 sh`, PowerShell
     `$env:OPENSPEC_VERSION = "v1.16.0"` then `irm https://openspec.dev/cli/install.ps1 | iex`, the npm alternative
     `npm install -g @fission-ai/openspec@1.16.0` (Node.js 22.12 or later), and the check `openspec --version`. The
     wrong form with the variable on `curl` is absent; the version equals `tech-stack.md`. Evidence: release v1.16.0
     assets, the downloaded (never executed) script sources honouring `OPENSPEC_VERSION`, `npm view`.
  4. The Node.js sentence on `00-setup-check.md` line 21 and `workshop-plan.md` line 47 reads the same in both: Node.js
     is not needed for the build, only for Track A when OpenSpec is installed with npm (22.12 or later); SU-21 stays
     and a new SU entry pins the qualification (A12).
  5. Every new command line exists in `workshop-plan.md`; PowerShell lines pass rule (c) with mutants; bash lines are
     pinned by coverage entries; floors equal live counts; sweep keys renumbered.
  6. All local gates green; rendered check of the Level 4 and setup pages per C.

### T5 Wide tables in focusable regions (pack item 8)
- dependsOn: [T4]
- fileScope: `guide/src/plugins/rehype-table-regions.mjs` (new), `guide/docusaurus.config.ts`,
  `guide/src/css/custom.css` (table region block only), `guide/scripts/task-list-labels.test.mjs` (plugin-list pin),
  `guide/scripts/rules/table-regions-built.mjs` (new), `guide/scripts/table-regions.test.mjs` (new),
  `guide/scripts/rules/built.mjs`, `guide/scripts/check-built.mjs`, `guide/scripts/lib/site.mjs` (only if the rule
  needs it), `guide/README.md` (one section), `agent-os/specs/2026-11-11-0145-15-guide-content-revision/visuals/**`.
- acceptanceCriteria:
  1. A rehype plugin (no `node_modules` import, G-8) wraps every table in page content in a `div` with `tabindex="0"`,
     `role="region"` and an `aria-label` unique per page, taken from the nearest preceding heading (suffixed when a
     heading has several tables); the table keeps its own semantics (no role on the table). Witnesses W1 to W10 of the
     `qa-strategy.md` T5 block (MDX heading ids, inline code or `<kbd>` in headings, tables inside TabItem or details,
     idempotence, uniqueness).
  2. The plugin-list pin in `task-list-labels.test.mjs` is updated to the new list with the reason; dropping either
     plugin fails it.
  3. A built rule `table-regions-built.mjs` runs in `check:built` and asserts every article table sits in such a
     region with a unique name; node:test fixtures with mutants (no tabindex, no role, no label, duplicate label,
     table outside a region) plus a CLI test that kills the wiring mutant; the rule is also proven on
     `build-offline` with a table count equal to an independent count (A6).
  4. Focus ring: computed outline not `none`, uses the existing focus variable, at least 3:1 against its background,
     not clipped, at 1280 and 390 px, both modes, three engines (U11); keyboard: focusing the region and pressing
     ArrowRight increases `scrollLeft` on the three overflowing tables at 390 px; at 1280 px table cell rects equal the
     T4-gate build (A8).
  5. Spec 14's A4 exclusion is gone: the rig has no exclusion filter; a plugin-off control build shows the old hits;
     the full sweep (11 pages x 2 modes x 1280 and 390 px x 3 engines x 2 builds = 264 runs) gives axe 0 violations on
     the full rule set, no horizontal page scroll, console clean per the spec 11 accepted list.
  6. A sweep record in `visuals/sweep-record.md` with the counts; `guide/README.md` describes the table regions.
  7. All local gates green.

### T6 Creativity tips (pack item 2)
- dependsOn: [T5]
- fileScope: `guide/docs/02-level-1-context-engineering.mdx`, `guide/docs/03-level-2-give-your-agent-eyes.mdx`,
  `guide/docs/04-level-3-behavioral-control.mdx`, `guide/docs/05-level-4-spec-driven-development.mdx`,
  `docs/spec-pack/workshop/workshop-plan.md`, `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`.
- acceptanceCriteria:
  1. Each of the Level 1 to Level 4 pages holds at least one `:::tip[Make it yours]` (U12): at most 60 words, invites
     participants to invent their own rule, skill, hook, guardrail or command, names a concrete artefact tied to that
     level's content, in the spirit of "the best harness has a creative engineer tweaking it continuously"; nothing
     refers to it; not in the at-a-glance box or a checklist; never a new required step.
  2. Checklists and step headings byte-identical to the T5 gate; the word diff is limited to the tips.
  3. Each tip is mirrored in the level's section of `workshop-plan.md` and pinned by one coverage entry (proved by
     deletion); floors equal live counts; sweep keys renumbered.
  4. All local gates green; rendered: tip count per page at least 1, axe 0 violations in both modes.

## Decisions

- Model policy (owner, spawn prompt): research, implementation, per-task verifiers and memory curators on sonnet; the
  QA lead and qa-final on opus. The QA strategy rates T1 and T5 critical; the owner policy wins and the opus meta-review
  compensates.
- No tech lead (one module; the pack suggests the split). Research: four sonnet research agents.
- Gate 1 self-accepted by the orchestrator (owner delegated): every task has summary, criteria, file scope, module,
  dependsOn and QA fields; the QA lead's untestable criteria U1 to U12 are rewritten above, A1 to A12 added, G-1 fixed
  (the `watch the demo` ban moves to T3 with its file scope).
- Item 9 (RA-1 to RA-4) is not in scope: the files it concerns are not edited by this spec.

## QA feed-forward

From the T1 meta-review (qa-lead ENDORSE r1; full text in the QA ledger section "QA feed-forward"):

- FF-T1-1 (T6, under A2): (a) a ban witness on a finished Level 2 fixture page with contractual anchors (kills M2c);
  (b) one witness asserting the exact `line N:` of the ban message (kills M1f); (c) the title regex becomes
  `/sync[\s_-]*moment/i`: `Sync-moment`, `sync_moment`, `Sync - moment` fail; `In sync`, `A moment`, `Sync` pass. The
  verifier shows each new witness survives against the T1-gate files and dies against the new ones.
- FF-T1-2 (T3, inside its `content.mjs` / `content.test.mjs` scope): (a) the near miss `Maik demonstrates` must pass
  (kills M12d); (b) the `sync moment` deny row becomes `/sync[\s_-]*moment/gi`: `Sync-moment`, `sync_moment`,
  `Syncmoment` fail; `in sync`, `a moment`, `sync the moment`, `asynchronous` pass.
- FF-T1-3 (T4): the Level 4 page's "About 35 minutes" (three times) and "A fits every track in 35 minutes" mirror
  `sdd-framework-guide.md:104,117,129` and `level-4-feature-briefs.md:41`, which this spec may not edit: keep the 35
  and add one sentence (mirrored in the plan, pinned by a coverage entry) saying what the rest of the 42-minute solo
  block is for, or report it as an owner residual. Check that the L4-05 sentence does not read as part of Brief C.
- FF-T1-4 (every later task, from A1): C0-18 ports and rig folders per task (verifiers 31N0-31N9, implementers
  32N0-32N9, qa-final 3190-3199, rig in `SP/s15-t<N>-qa-rig/`); C0-19 "previous gate" means the spec tip the task was
  cut from or last merged; C0-20 after merging the spec tip, re-prove the gates, the pin audit against the merged plan
  and manifest, the plan diff, the built grep, the rendered check, and that files the task does not own equal the spec
  tip; C0-21 the A4 axe hit counts as pre-existing only while T5 is not in the task's base, after that the bar is 0.
- FF-T1-5 (qa-final): IX-11 to IX-15 match a Format cell anywhere in the agenda, not per row; qa-final re-runs the
  per-row arithmetic census from a final-prefixed copy of `s15-t1-qa-timetable.mjs`.

## Amendments

- A1 (2026-11-11, owner request "parallelise the updates per level", relayed mid-run; gate re-run self-accepted).
  - Task order and dependencies: T5 depends on nothing and runs in parallel with T1. T2, T3, T4 and T6 depend only on
    T1 and run in parallel with each other once T1 is merged into the spec branch. Each parallel task works in its own
    git worktree (separate `guide/build`, `guide/build-offline`, `node_modules`, `target`, and distinct local ports);
    two agents never share a worktree.
  - Creativity tips (pack item 2) move into the level tasks: T2 adds the Level 1 tip, T3 the Level 2 tip, T4 the Level 4
    tip, and T6 becomes the Level 3 tip only. The T6 criteria above apply per tip to the task that adds it (one
    `:::tip[Make it yours]` per level, at most 60 words, mirrored in the plan, pinned by one coverage entry, never a
    step or checklist item); the T6 fileScope shrinks to `guide/docs/04-level-3-behavioral-control.mdx`,
    `docs/spec-pack/workshop/workshop-plan.md`, `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`.
    T6 criterion 2 ("checklists and step headings byte-identical") applies to every level task's tip edit.
  - T5: "the T4-gate build" in criterion 4 (A8) reads "the T5 base build" (the spec tip T5 was cut from); its rendered
    sweep covers its own branch, and qa-final repeats the full unfiltered sweep on the final tip.
  - Merge discipline: task PRs merge into the spec branch one at a time in the order they pass. Every later open task
    branch then merges the spec tip (merge commit, never rebase); conflicts in `workshop-plan.md`, `coverage.mjs` and
    `coverage-manifest.test.mjs` are resolved by that task's implementer (floors equal live counts, `SWEEP_EXCLUDED`
    keys recomputed against the merged plan), and the task's gates and a delta verification run again before its merge.
- A2 (2026-11-11, orchestrator, from the T1 meta-review; gate re-run self-accepted, test-only plus one regex line).
  T6's fileScope gains `guide/scripts/structure.test.mjs` (insertion-only) and `guide/scripts/rules/structure.mjs`
  limited to the `SYNC_MOMENT_TITLE` line, for FF-T1-1. T3 carries FF-T1-2 inside its existing scope; T4 carries FF-T1-3.

## Residuals for the owner (expected)

- The `/context` mock is illustrative and not compared with real output (D14); a rehearsal row can confirm it.
- The OpenSpec install scripts were never run by an agent; the PowerShell route is untested; rehearsal item.
- The LSP debugging steps were not run live.
- Agent OS command form (`/shape-spec` or `/agent-os:shape-spec`, PF-T13-1) and the order difference to the pack
  spec bullet (D-S6) stay open; the start branch's standards were written, not discovered (D-S7, PF-T13-4).
- Pack text outside this spec's scope still mentions sync moments or demos or JDK 17: spec 11 (lines 7, 54, 151),
  `docs/spec-pack/README.md` line 15, spec 10 line 168, plan line 173 (distributor on the projector machine, a break
  action, unchanged).
- RA-1 to RA-4 from spec 14 stay open. Rendered checks run in a local rig, not in CI.
