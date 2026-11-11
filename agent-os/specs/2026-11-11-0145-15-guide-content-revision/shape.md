# Shape: spec 15 guide content revision

Pack spec: `docs/spec-pack/specs/15-guide-content-revision.md` (decision D19, Maik's review of the live guide on
2026-11-10). Base branch `factory` at 7ded994 (spec 14 merged as 2e31429). Module `guide`, plus
`docs/spec-pack/workshop/workshop-plan.md`, which changes together with the guide so the coverage map stays anchored.

## Scope

Items 1 to 8 of the pack spec. Item 9 (spec 14 test gaps RA-1 to RA-4) is conditional ("if they touch pages this spec
edits anyway"): RA-1 to RA-3 touch only `guide/scripts/brand.test.mjs` and RA-4 only
`guide/scripts/level-progress.test.mjs`; no task of this spec edits those files, so they stay owner residuals.

## Research used

Four research agents (sonnet, owner model policy); no tech lead (one module, the pack already suggests the split).
Findings in short:

- Sync moments (res-1): one `:::info[Sync moment]` box on each of the five level pages, the welcome page timetable
  and its lines 17 and 49, `guide/README.md`, and `workshop-plan.md` (timetable, level sections, Part minutes). The
  structure rule demands exactly one Sync moment box per finished level page; five coverage entries (L0-01, L1-01,
  L2-01, L3-01, L4-05) live only in those boxes; `coverage-manifest.test.mjs` pins per-page floors, the section 9 rows
  and `SWEEP_EXCLUDED` keys of the form `plan:<line>`. No check pins the sync and solo split; only level totals, the
  break and the 240 minutes are anchored. The content deny-list bans "facilitator" on guide pages.
- Level 1 and 2 (res-2): Claude Code rules live in `.claude/rules/*.md` with one frontmatter key, `paths` (YAML list
  of globs relative to the project root); a path-scoped rule loads when Claude reads or edits a matching file.
  `origin/solution/level-1` holds `.claude/rules/domain.md`, `data-and-migrations.md` and `testing.md` with block-list
  `paths`, byte-identical to the pack staging under `docs/spec-pack/level-branches/level-1/`. The official docs give no
  sample `/context` output, so the text mock is illustrative. LSP debugging per the official plugin docs: the binary on
  the PATH of the shell that starts `claude`, a new session after installing, the `/plugin` Errors tab, the server
  starts when Claude first touches a `.java` file; jdtls needs Java 21.
- Level 4 (res-3): the track commands and their order already match the official docs at the pinned versions
  (OpenSpec 1.16.0, Spec Kit 1.3.1, Agent OS 3.0). Documented brownfield first steps: OpenSpec `openspec init`, project
  context in `openspec/config.yaml`, then the `/opsx` flow; Spec Kit `specify init` in the existing repo, then
  `/speckit-constitution` before specify, plan, tasks, implement; Agent OS `/discover-standards`, `/inject-standards`,
  `/plan-product`, then `/shape-spec`. The OpenSpec start branch has config and one spec but no CLI. Pinned install:
  `curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v1.16.0 sh` (the variable must sit on `sh`), the
  PowerShell twin through `$env:OPENSPEC_VERSION`, or `npm install -g @fission-ai/openspec@1.16.0` (Node 22.12+).
- Checker mechanics (res-4): only PowerShell lines are matched against the pack automatically (five SOURCE_FILES);
  other commands are pinned by coverage entries. `<details>`, `:::tip` and language-less fences pass every check.
  Infima renders tables as `display:block; overflow:auto` with no focusable wrapper, hence axe
  `scrollable-region-focusable` at 390 px on level-4, reference/architecture and reference/cheat-sheet. Spec 14's A4
  exclusion lives only in spec 14's plan, sweep record and scratchpad rigs. The guide CI workflow runs on every PR
  touching `guide/**` (no branch filter) and has no browser step.

## Shaping decisions

- D-S1 Timetable: sync minutes go to solo time, totals, debriefs, break and 240 minutes unchanged. Level 0 solo 22,
  Level 1 solo 32, Level 2 solo 18 (LSP 10, MCP 8), Level 3 solo 28 (permissions 4, hooks 14, skill and command 10),
  Level 4 solo 42.
- D-S2 The guide says "Maik and Tom walk around" (the content rule bans "facilitator" on pages); no demo step.
- D-S3 Checker changes guard the same thing: the exactly-one Sync moment rule becomes a ban on any Sync moment
  admonition on every page; deny-list terms `sync moment`, `maik demos`, `live demo` (T1) and `watch the demo` (T3);
  retargeted coverage entries; per-page coverage floors never go down; `WP:N` row labels stay opaque ids;
  `SWEEP_EXCLUDED` keys are renumbered when plan lines shift, a new key only with a reason naming the pin that covers it.
- D-S4 The demo text of Level 2 (step 5, troubleshooting, plan) is removed in T3 together with its debugging section,
  so T1 cannot ban `watch the demo` (QA strategy G-1).
- D-S5 JDK: the Level 2 debugging section names Java 21 (upstream jdtls wrapper, guide setup page, staged
  `lsp-setup.md`); `workshop-plan.md` line 59 goes from 17 to 21. Spec 10 pack line 168 (17) is outside scope.
- D-S6 Agent OS brownfield order follows the official site (discover standards, inject, plan product, then shape),
  because D19 asks for "the order the frameworks' docs give"; `/index-standards` only after hand edits. The pack spec
  bullet lists plan product before index/inject; the difference is reported as a residual.
- D-S7 The Agent OS start branch's standards were written for this repo, not produced by `/discover-standards`
  (PF-T13-4): the guide says "written for this repo and indexed", which is true.
- D-S8 OpenSpec install: the install script with the version pin (no Node.js), the PowerShell twin, and npm as the
  alternative for people who have Node.js 22.12 or later. `npx` is not offered (it gives only `init`, not the later
  `openspec validate`). Agents never execute the install scripts; evidence is the release assets and the script source.
- D-S9 Wide tables: a second rehype plugin wraps every article table in a `div` with `tabindex="0"`, `role="region"`
  and an `aria-label` unique per page (from the preceding heading), so future tables are covered without page edits;
  a built rule pins it on both builds; the focus ring uses the existing focus variable. The spec 14 A4 exclusion is
  removed: the 390 px sweep runs with no exclusions.
- D-S10 Creativity tips: one short `:::tip[Make it yours]` per level from Level 1 to Level 4, mirrored in the plan and
  pinned by coverage entries; never a step or checklist item.
- D-S11 Tasks run one at a time in the order T1, T2, T3, T4, T5, T6 (all module `guide`; each shifts plan lines).

## Out of scope

Spec 14 branding, level branch content (spec 10), the storefront, RA-1 to RA-4 (above), `progress.md`,
`decisions.md`, `roadmap.md` and every pack file other than `workshop-plan.md`.
