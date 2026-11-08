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
| T1 | scaffold docs-only 3.10.2 TS site, two configs, five npm scripts, check harness with self-tests, hygiene rules, README, root ignore for `guide/build-offline/` | guide | [] | B1 | elevated | sonnet | sonnet | PASS r1 | #71 | sdd/11-workshop-guide/T1 | ENDORSE |
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

From the T1 meta-review (qa-lead ENDORSE r1; full wording in the QA ledger review log):

- FF-1 (T3 Required Action, see A2): `npm run check` exits non-zero when node:test executes no real test case;
  must-fail witnesses (a) every `scripts/**/*.test.mjs` deleted, (b) every test file emptied, (c) the `check` glob
  matching nothing, each in its own run with the guard's message quoted; a self-test pins the guard; the `&&` chain
  and the C0-6 throwaway probe stay; no dependency added. The T3 verifier runs (a) to (c) plus a guard-disabling mutant.
- FF-2 (C0-6, verifiers of T3, T4, T5, T11): the node:test total is at least the last gated total (46 after T1) and
  the earlier test titles are a subset of the current ones unless the task's criteria remove a test (`comm -23`
  output quoted, empty).
- FF-3 (C0-1, every verifier and qa-final): evidence files are named `s11-tN-qa-r<k>-*`, the hash file
  `s11-tN-qa-r<k>-hashes.txt`.
- FF-4 (C0-5 warning baseline, a higher count is a finding): `npm ci` uuid@8.3.2 deprecation 1 line, install-scripts
  notice 6 lines, audit summary 1 line; `build` localStorage ExperimentalWarning 2 lines; `build:offline` sitemap
  disabled 1 line; `check`, `typecheck`, `check:built` 0.
- FF-5 (C0-8 console): accepted only in WebKit on offline `file://`, counted per page: "Cross origin requests are only
  supported for HTTP." and the matching "XMLHttpRequest cannot load file:... due to access control checks." page
  error. Anything else, either message in Chromium or Firefox or on the Pages build, is a finding; dev-mode console
  stays clean.
- FF-6 (T4 and every content task): `check:built` flags any literal `:::` in built HTML, code included. A page that
  needs `:::` in code sends a question and never edits the rule. If T4 narrows the rule, B04b and B05b still fail and
  X01, X02 become passing self-tests.
- FF-7 (T6): end `guide/docs/index.md` with a newline. `guide/package.json:21` (two keys on one line) is a follow-up,
  no later task owns that file.
- FF-8 (T4, recommended): a `docs/**` file with a NUL byte is an error instead of being skipped (R3).

## Amendments

- A1 (2026-11-08, orchestrator, during T1): the lab rule (G-4) also skips `guide/package-lock.json`. The lockfile is
  generated, not participant content, and contains the transitive package name `postcss-lab-function` of
  `@docusaurus/preset-classic`, so G-4 read literally cannot pass with the lockfile AC1 requires. The ASCII rule still
  scans the lockfile; self-tests pin both facts. Consequence: none for pages; cheap and reversible.
- A2 (2026-11-08, orchestrator, after the T1 meta-review): FF-1 is an added T3 acceptance criterion (zero-test guard
  for `npm run check`); T3's fileScope already holds `guide/scripts/**`. Consequence: T3 is slightly larger; the
  gate re-run is self-accepted (an added test criterion inside a task's scope).
