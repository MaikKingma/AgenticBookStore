---
spec: 11-workshop-guide
source: docs/spec-pack/specs/11-workshop-guide-site.md (factory 52c4f6b, includes D7)
status: in-progress
accepted-by: orchestrator (autonomous)
created: 2026-11-08T21:07:46Z
gate1: 2026-11-08T21:07:46Z
qa-lead: on
base: factory
branch: sdd/11-workshop-guide/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
---

# Plan: 11-workshop-guide

`shape.md` holds scope, decisions and follow-ups. The task cards (summary, acceptance criteria, fileScope, falsifier
probes, verifier risks) are the T1 to T11 sections of `strategy.md`, together with its ground rules G1 to G9, the page
grammar and the npm script contract; they apply as written, amended by the Gate 1 criteria amendments below, which win
where they differ. QA directives: `qa-strategy.md` (snapshot at Gate 1; common verifier block C0-1 to C0-13 plus the
task directives); the live ledger is `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-11-workshop-guide.md`.

## Ground rules (all tasks)

- G1 to G9 of `strategy.md` apply to every task. In short: Node v26.13.0, `npm ci`, no new dependency, no
  `npm audit fix`; standard gates `npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`,
  `npm run build:offline` in `guide/` and `./mvnw clean verify` at the root all exit 0; plain ASCII, no em dashes,
  never "lab"; MDX safety (bracket admonitions, placeholders only in inline code, no raw `<img>`); no fact without a
  pack source, gaps become follow-ups; no internal paths on participant pages; agents never write git.
- Owner's public deploy gate: no agent enables Pages, runs or dispatches a deploy, creates a release, a tag or a
  `gh-pages` branch, or sets a repository variable.
- Implementation agents are sonnet (opus from rework round 2); verifiers per the qaModel column; qa-lead and qa-final
  opus. Never fable.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | impl model | status | pr | branch | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | scaffold docs-only 3.10.2 TS site, two configs, five npm scripts, check harness with self-tests, hygiene rules, README, root ignore for `guide/build-offline/` | guide | [] | B1 | elevated | sonnet | sonnet | pending | | | |
| T2 | `guide.yml`: PR build plus offline zip artifact, gated deploy and release, SHA pins, timeouts | root | [T1] | B2 | critical | opus | sonnet | pending | | | |
| T3 | OpenValue brand in `custom.css` only, Montserrat with provenance and OFL, contrast check, no colour values elsewhere | guide | [T1, T2] | B3 | elevated | sonnet | sonnet | pending | | | |
| T4 | LevelProgress, 11 page skeletons, sidebar, fixed anchors, structure checks reading the workshop plan at check time | guide | [T3] | B4 | critical | opus | sonnet | pending | | | |
| T5 | pack-anchored coverage manifest and content rules | guide | [T4] | B5 | critical | opus | sonnet | pending | | | |
| T6 | index, setup check, wrap-up pages | guide | [T5] | B6 | elevated | sonnet | sonnet | pending | | | |
| T7 | Level 0 and Level 1 pages | guide | [T5] | B7 | elevated | sonnet | sonnet | pending | | | |
| T8 | Level 2 page | guide | [T5] | B8 | elevated | sonnet | sonnet | pending | | | |
| T9 | Level 3 page | guide | [T5] | B9 | elevated | sonnet | sonnet | pending | | | |
| T10 | Level 4 page | guide | [T5] | B10 | elevated | sonnet | sonnet | pending | | | |
| T11 | reference pages, marker ban, site-wide pass in both builds and three engines | guide | [T6, T7, T8, T9, T10] | B11 | elevated | sonnet | sonnet | pending | | | |

Batches are sequential: T6 to T10 share the module `guide` (one build output, one lockfile), and every task branch is
cut from the spec branch tip after the previous task's PR is merged.

Per task: fileScope as in the `strategy.md` card (T1 additionally owns the root `.gitignore` line, see G-1);
qaDirective = `qa-strategy.md` C0 plus the task's directive block plus every `## QA feed-forward` entry so far.

## Gate 1 criteria amendments (QA lead findings, all accepted by the orchestrator)

The rewrites in `qa-strategy.md` section "Gate 1 findings" (G-1 to G-14) replace the card wording they name, with
these choices where the finding offered two:

- G-1: the root `.gitignore` line `guide/build-offline/` moves into T1's fileScope; T2 only verifies it is present.
- G-6: T3 declares contrast pairs for every Infima admonition type (note, tip, info, warning, danger, caution as an
  alias), so later pages may use any of them.
- G-2 applies to every task: "no product file outside the fileScope (diff minus `.claude/` and `agent-os/`)".
- G-3, G-4, G-5, G-7 to G-14 apply as written.

Not testable in this cycle (Gate 2 residuals, owner): the push path of "Deployed automatically" and the DoD items
"site live", "offline zip available", "every command executed once on a clean machine" (F1, F12). In-cycle evidence
is the static gate, the skipped deploy and release jobs on PR runs, and the absence of every public side effect.

## Verification model for T2 (workflow)

Evidence is real GitHub Actions runs bound to the full PR head sha: the orchestrator commits the implementation,
pushes the task branch and opens the task PR into `sdd/11-workshop-guide/spec` before the verifier runs; rework
rounds are added commits. Verifiers read runs with `gh` and never push, rerun, cancel, dispatch or write git.

## QA feed-forward

(none yet)

## Amendments

(none yet)
