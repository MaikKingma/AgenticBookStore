# Shape: spec 16 bonus level, orchestrate your own team

## Scope

Pack spec `docs/spec-pack/specs/16-bonus-level-orchestration.md` (decision D20), started by the owner on 2026-11-11
during the live workshop as a full SDD cycle. One new optional guide page "Bonus level: orchestrate your own team",
after Level 4 and before the Wrap-up, for participants who finish early: think through a feature's flow and its human
gates, define one research subagent and one QA subagent, coordinate them from the main session, and let Claude write
the orchestration as a dynamic workflow. An exercise without solutions. The workshop plan gains the bonus section so
the coverage map stays anchored.

Out of scope (pack): new level branches, solution content, changes to the bookstore or the storefront. Out of scope
(this cycle): any edit to an existing guide page (SA1), `.github/`, `package.json` and the lockfile.

## Constraints from the owner (2026-11-11)

- The workshop is live. Every existing page URL, anchor and the level flow keep working; the change is additive.
- Never touch `main`, a level branch, a tag, the `workshop-2026` release or GitHub Pages settings. Nothing lands on
  `factory` except through the owner's merge of the spec PR (a factory merge deploys the guide and rebuilds the release
  asset).
- HR1 verify the workflow feature and the subagent format against the official docs and cite them (`references.md`);
  stop before Gate 1 if the feature is not generally available or the docs do not settle command and syntax.
- HR2 no solutions anywhere. HR3 every command on the page is in the pack and passes the checks; frozen rules change
  only with a reason, never weakened. HR4 full gate proof on the final spec tip. HR5 the spec 16 Status line.
- Model policy: research, implementation and per-task verifiers on sonnet; tech lead, QA lead and qa-final on opus.

## Research (2026-11-11)

| id | topic | result |
|---|---|---|
| res-1 | workflow feature in the official docs | "Dynamic workflows" are documented, no preview label, all paid plans and API (Pro: `/config` toggle; admins can disable), since 2.1.154. Gap: how `agent()` uses a custom subagent file is undocumented; no mid-run input. |
| res-2 | subagent docs | file locations, format, fields, tool names, delegation and chaining; `/agents` no longer a wizard; agent teams experimental. |
| res-3 | page placement | new file at `sidebar_position: 7.5`, URL `/bonus-level-orchestration`, no level bar (component accepts Levels 0 to 4 only), an info box as the marker, every pin an addition or an exact 11 -> 12 bump; 11 live sitemap URLs. |
| res-4 | checks and gates | the pack is five files; only the PowerShell rule checks commands against the pack; the literal sweep needs a tuple and entries; gate recipe on Node 26.13.0 with a 288-run browser and axe sweep. |
| res-5 | content context | no required subagent experience after Level 4; `architecture-reviewer` ships on `solution/level-3` and `start/level-4-*`; the agenda has no slack (240 minutes). |
| res-6 | verbatim verification | 19 claims against raw pages: 15 verified, 2 verified with a gap (W5, S3), 0 refuted, 2 not found (W9, W10). See `references.md`. |

Strategy: tech lead lead-1 (opus), approach "characterize, guard, then add". QA strategy: qa-lead (opus), snapshot in
`qa-strategy.md`.

## Shaping decisions (self-accepted by the orchestrator, cheap and reversible)

- D-S1 HR1 GO. Dynamic workflows are generally available per the official docs and the command, location and syntax
  are documented, so the stop condition does not hold. The page states the 2.1.154 floor, the Pro `/config` "Dynamic
  workflows" row and that an organization admin can switch workflows off; it never claims that a workflow uses a
  `.claude/agents/` file by name; human gates sit between workflow runs; a participant without workflows has a clear
  end point (the bonus is done with Part C); no workflow script code.
- D-S2 Placement: `guide/docs/06-bonus-level-orchestration.mdx`, `sidebar_position: 7.5`, no level bar, a pinned
  `:::info[Optional bonus level]` box first on the page with a link to the Wrap-up for participants who are behind.
  No existing file name, position, URL or anchor changes. The frozen skeleton fixture gains one page (S1).
- D-S3 Workshop plan: a section `## Bonus level: orchestrate your own team (optional)` inserted between `## 9. Level 4`
  and `## 10. Wrap-up` after line 451, so no line-keyed coverage key moves; outside the level heading grammar, no
  `**Level cleared when**`, agenda table and total untouched.
- D-S4 No solutions (HR2): no workflow script code on the page or anywhere in the repo; agent-definition parts are
  explained as fields and questions; tool names are listed as documented options and the participant chooses; at most
  one frontmatter fragment whose every value is an angle-bracket placeholder, with no body text; no new agent file,
  workflow file or solution branch. The pack's "no reference agent files" holds.
- D-S5 The page may name `.claude/agents/architecture-reviewer.md` as an example of the format, conditionally: it
  exists on `solution/level-3` and the three `start/level-4-*` branches (frozen by D17), not on `main` or
  `level-0-start`. It is an architecture reviewer, neither the research agent nor the QA agent the exercise asks for.
- D-S6 Commands (HR3): every inline code span and fenced line on the bonus page appears verbatim in the plan's bonus
  section and is covered by coverage entries; a new check enforces this page-to-plan direction (the existing command
  checker covers PowerShell lines only); no PowerShell tab on the page.
- D-S7 Final gates run on Node 26.13.0 (the CI pin) and on nvm 24.20.0.
- D-S8 The spec 16 Status line is edited inside a task (T2) so QA covers it.
- SA1 The lead's T5 (one sentence each on the welcome and wrap-up pages) is dropped: the pack does not ask for it, the
  sidebar and the pagination make the page discoverable, and no existing live page changes during the workshop. Offered
  to the owner as an optional follow-up.
- SA2 `references.md` is written by the orchestrator: T4's implementer delivers the page's claims table in its outbox,
  the orchestrator copies it verbatim after T4's gate PASS, qa-final diffs the two.

## Owner items (not Gate 1 blockers; listed in the spec PR)

- Q1 The factory merge also rebuilds the release asset `workshop-2026` (`.github/workflows/guide.yml`), by design.
  The owner chooses the moment of the merge.
- Q2 No agent may run Claude Code (D14), so nobody in the cycle has run Parts B to D in a real session. Recommended:
  the owner runs them once (including the Pro `/config` row) before merging.
