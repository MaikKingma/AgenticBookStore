---
spec: fix-11-guide-review
source: docs/spec-pack/specs/11-workshop-guide-site.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-10T14:22:57Z
origin: fresh read-only review of the guide merged as 0bcbcfe (main session); decisions D9, D16, D17
gate1: 2026-11-10T14:25:05Z
qa-lead: on
base: factory
branch: sdd/fix-11-guide-review/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
kind: bugfix
severity: S3
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [review S1 Level 0 and 1 catch-up on main, review S2 no run command for the app, review S3 generic catch-up form]
---

# Plan: Levels 0 and 1 catch up at the tag level-0-start, the setup page starts the app, far-behind names every level

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
ROOT = the repository root (the worktree root named in the task prompt). All commands run from ROOT. `<pr-base>` =
`origin/sdd/fix-11-guide-review/spec` at the moment the task branch was cut. `SCRATCH` = the session scratchpad named
in the task prompt.

## Ground rules

- The pack and D17 win: `docs/spec-pack/specs/11-workshop-guide-site.md`, `docs/spec-pack/orchestrator/decisions.md`
  D9, D16, D17. Nothing under `docs/spec-pack/` is edited. `progress.md` and `decisions.md` are the main session's.
- Never skip, disable, delete or weaken a test or a check of `guide/scripts/`. Never `--no-verify`. Pre-existing test
  and check files change only where decision D2 or D3 names the change. No new PowerShell code line anywhere (the
  pack command list of content rule (c) stays untouched); if a criterion seems to need one, block and ask.
- Never push, never touch `main`, the tag `level-0-start`, other tags, Pages settings or repository variables.
  Implementers and verifiers do not git commit, branch or stash in ROOT; the orchestrator owns git.
- Throwaway clones only under an explicit template: `mktemp -d "$TMPDIR/f11-<role>-XXXXXX"`; commits there with
  `git -c user.name=t -c user.email=t@t`. Never in ROOT.
- Secrets: never set, read or write `ANTHROPIC_API_KEY`; never read the real `BOOKSTORE_AI_API_KEY`; no page tells
  anyone to set `ANTHROPIC_API_KEY`. No `claude` CLI, no plugin or marketplace commands.
- ASCII only in every file touched; docs in plain English without em dashes.
- Guide gates (from `guide/`, via `npm --prefix guide run <script>` from ROOT): `check`, `typecheck`, `build`,
  `check:built`, `build:offline`, all exit 0. Base on 8e7b279 (orchestrator, 2026-11-10T14:20Z, Node v24.20.0):
  `npm run check` 514 node:test cases pass, check.mjs all `ok`, `structure (11 pages ...)`,
  `coverage (anchored 304/304, 15 manual rows, 0 entries pending)`, `content (11 pages ...)`, 0 pending.
- Referee: `env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify`
  from ROOT, green within 60 s. Base on 8e7b279: 36.8 s; Vitest 689 passed + 2 expected fail (691); bookstore 1379;
  distributor 190. Maven runs set `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<SCRATCH>/jtmp`. Never run
  `spring-boot:run` or `docker compose` against the shared database.
- Evidence files in SCRATCH carry the role and task prefix (`f11-t1-impl-*`, `f11-t1-qa-*`).
- After 3 failures on one item the item is blocked and the cycle stops with a needs-decision.

## Path ruling (reference.md section 6)

Lite path (the default) - all hold:
- [x] reproduced in 1a.2: S1 by the orchestrator in a throwaway clone (bug.md Reproduction A to C); S2 and S3 by grep
- [x] root cause named at file:line and confirmed: `guide/scripts/rules/structure.mjs:20-35,208` (bug.md Root cause)
- [x] one obvious fix design readable from the code: decisions D1 to D5 below; the tag form was proven in the clone
- [x] blast radius enumerable by grep now: bug.md Blast radius (each item task T1 or deferred)
- [x] no already-wrong persisted data or produced artifacts beyond the deployed guide, which the factory push redeploys
- [x] not S1; not money, numbering, auth or concurrency
Bundled owner decisions: none (the nit "Git Bash or WSL2" is not folded in: coverage entry TS-03 pins that text, so
it is not trivial; residual).
Open question the code does not settle in minutes: none.
-> path: lite (no lead, no research fan-out; one task; QA lead registered at Phase 2/3).

## Decisions (orchestrator, within authority, cheap and reversible)

- D1 (commands): Level 0 catch-up `git stash -u && git switch -C level-0 level-0-start`; Level 1 catch-up
  `git stash -u && git switch -C level-1 level-0-start`. `-C` creates the participant's own branch at the tag, or
  resets it on a re-run (`-c` fails on a re-run, bug.md Reproduction C); `git stash -u` keeps uncommitted work
  recoverable and leaves ignored files such as `notes/level-0.md` alone. Level 0 `Start branch` stays
  `` `main` (tag `level-0-start`) `` (pack wording, same commit); Level 1 `Start branch` becomes `` `level-0-start` ``
  and its prose stops saying "Start on `main` again" (it says to start from the tag, the catch-up gives a clean
  branch there even after Level 0 commits). Levels 2 to 4 are unchanged.
- D2 (grammar): `guide/scripts/rules/structure.mjs` LEVELS carries each level's full expected catch-up command
  (exact equality of a single fenced bash line stays, as does every other at a glance check); Levels 0 and 1 get the
  D1 commands and Level 1 the new start; Levels 2 to 4 keep byte-identical expected commands. The fixture builder
  `guide/scripts/lib/structure-fixtures.mjs`, the skeleton fixtures
  `guide/scripts/fixtures/skeleton-docs/docs/01-level-0-break-the-agent.mdx` and `02-level-1-context-engineering.mdx`
  and `guide/scripts/structure.test.mjs` change only in the Level 0/1 catch-up and start literals (and the test titles
  that quote the old template); every existing test case and every negative case stays, retargeted literal for
  literal. `built-powershell.test.mjs` and its captured fixture `real-level-0-at-a-glance.html` stay unchanged.
- D3 (far-behind): `guide/docs/reference/troubleshooting.md` `## I am far behind {/* #far-behind */}` lists the
  catch-up of every level, one fenced bash block per level (Level 0, 1, 2, 3, 4), each byte-equal to that level
  page's at a glance block, and no generic `solution/level-N` block. Coverage entry TS-11 in
  `guide/scripts/coverage.mjs` gets `pageSnippet: 'git stash -u && git switch solution/level-1'` (the manifest's
  documented field for "what the page carries when it differs from the pack"); its snippet, source and section stay,
  so it stays anchored in the pack. `guide/docs/index.md` drops the generic `git switch <branch>` block and says that
  each level page names its own catch-up, with a link to `./reference/troubleshooting.md#far-behind`.
- D4 (start the app): `guide/docs/00-setup-check.md` gets `## Start the app` right after `## Set the workshop key`
  and before `## Choose the model`: in the same terminal that holds the key, from the repository root, a plain fenced
  bash block of exactly `./mvnw -pl bookstore spring-boot:run` (the guide shows `./mvnw` commands as plain bash
  blocks, like `./mvnw verify`; no PowerShell tab, so no new PowerShell line), Docker must run (it starts PostgreSQL),
  open `http://localhost:8080/shop/`, stop with Ctrl+C. Wording from README "Run". The key section's sentence links to
  `#start-the-app`. `guide/docs/01-level-0-break-the-agent.mdx` (the storefront sentence) links to
  `./00-setup-check.md#start-the-app`. The setup page names no other key variable than today.
- D5: models per the main session (2026-11-10): sonnet implementer and verifier; opus QA lead and qa-final. Never
  fable.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | status | verdict | qa-lead | pr | branch |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Levels 0 and 1 catch up at `level-0-start`, far-behind and index name every level's command, the setup page starts the app | guide | [] | 1 | elevated | sonnet | done | PASS r0 | ENDORSE | #159 | sdd/fix-11-guide-review/T1 |
| T2 | the pack's Level 1 start (line 132) and far-behind row (line 347) name the D1 commands; the setup link and same-terminal sentence are pinned (A1, FF-2) | guide | [T1] | 2 | elevated | sonnet | in-progress | | | | sdd/fix-11-guide-review/T2 |

### T2: the pack agrees with the guide on the Level 0/1 catch-up; FF-2 pins

- fileScope: `docs/spec-pack/workshop/workshop-plan.md` (line 132 and line 347 only, A1), `guide/scripts/level-start.test.mjs`,
  and `guide/scripts/coverage.mjs` (TS-11 only, and only if the anchor needs it). Any other file that pins the
  text of those two pack lines (for example a self-test that reads the real pack) is not in scope: block and ask.
- Wording (D6, orchestrator): line 132 becomes
  `**Start:** the tag `level-0-start` again (clean): `git stash -u && git switch -C level-1 level-0-start`. Catch-up / compare: `solution/level-1`.`
  (the "Catch-up / compare" phrase and its branch stay); line 347 becomes a far-behind row that names, at the start
  of each level, Level 0 `git stash -u && git switch -C level-0 level-0-start`, Level 1
  `git stash -u && git switch -C level-1 level-0-start`, Levels 2 and 3 `git stash -u && git switch solution/level-N`
  (N one lower than the level) and Level 4 `git stash -u && git switch start/level-4-<track>`. Keeping the generic
  Levels 2 and 3 form keeps TS-11's pack snippet anchored unchanged; TS-11 keeps its `pageSnippet` because the page
  shows explicit commands. Exact wording may differ only in connecting words; every command literal is as above.
- Acceptance criteria:
  1. Regression test `guide/scripts/level-start.test.mjs` gains case (d) "the workshop plan names the same Level 0
     and 1 catch-up as the guide": the pack's Level 1 section `**Start:**` line holds the Level 1 at a glance
     command read from the live Level 1 page and does not say `` `main` again ``; the pack's `Someone is far behind`
     row holds the Level 0, Level 1 and Level 4 at a glance commands read from the live level pages. The pack is
     located the way `guide/scripts/check.mjs` locates it. Case (c) gains the FF-2 pins: the `## Set the workshop key`
     section links `(#start-the-app)`, and the `## Start the app` section holds the same-terminal sentence (its exact
     words as on the page today).
  2. Negative control: with line 132 and line 347 restored from `git show HEAD:docs/spec-pack/workshop/workshop-plan.md`
     (copy-aside + SHA-256), `node --test guide/scripts/level-start.test.mjs` FAILS on exactly case (d) with an
     assertion naming the missing `git stash -u && git switch -C level-1 level-0-start` (or Level 0) in the pack -
     not with a read, import or syntax error; cases (a) to (c) stay green. With the fix in place all cases PASS.
     The FF-2 pins are proven by must-fail probes on the page (the verifier runs them): M10 the setup link made plain
     text, and the same-terminal sentence deleted, each makes case (c) FAIL. Outputs under `## Red evidence` and
     `## Green evidence`.
  3. Consistency: `git diff <pr-base> -- docs/spec-pack/` touches only lines 132 and 347 (`git diff -U0` hunks
     quoted); `npm --prefix guide run check` keeps `anchored 304/304, 15 manual rows, 0 entries pending`, and
     structure, content and plan checks `ok`; `content` rule (c) still passes (the new pack spans only add allowed
     lines); T1's other 11 files are unchanged (`git diff <pr-base> --stat`); if TS-11 changes, the verdict says why
     and that it is one entry.
  4. ASCII only, plain English, no em dash in every touched line.
  5. Gates green: `npm --prefix guide run check`, `typecheck`, `build`, `check:built`, `build:offline`, and the
     referee (same command and env as T1) within 60 s.

### T1: Levels 0 and 1 catch up at the tag, far-behind names every level, the setup page starts the app

- fileScope: `guide/docs/01-level-0-break-the-agent.mdx`, `guide/docs/02-level-1-context-engineering.mdx`,
  `guide/docs/00-setup-check.md`, `guide/docs/index.md`, `guide/docs/reference/troubleshooting.md`,
  `guide/scripts/level-start.test.mjs` (new), `guide/scripts/rules/structure.mjs`,
  `guide/scripts/lib/structure-fixtures.mjs`, `guide/scripts/structure.test.mjs`,
  `guide/scripts/fixtures/skeleton-docs/docs/01-level-0-break-the-agent.mdx`,
  `guide/scripts/fixtures/skeleton-docs/docs/02-level-1-context-engineering.mdx`, `guide/scripts/coverage.mjs`
  (TS-11 only), and `guide/scripts/coverage-manifest.test.mjs` only if a case there pins TS-11's page form (D3).
- Acceptance criteria:
  1. Regression test `guide/scripts/level-start.test.mjs` exists at the guide self-test layer (node:test reading the
     live `guide/docs`, like `live-guide.test.mjs`; it reuses the scanners in `guide/scripts/lib/` and adds no new
     general MDX parser), is named for the correct behaviour (not the bug id), and holds at least these cases:
     (a) "Levels 0 and 1 catch up on an own branch at the tag level-0-start": the at a glance catch-up block of the
     Level 0 page is exactly `git stash -u && git switch -C level-0 level-0-start`, of the Level 1 page exactly
     `git stash -u && git switch -C level-1 level-0-start`, and the Level 1 Start branch reads `` `level-0-start` ``;
     (b) "the far-behind help gives the catch-up of every level": the `#far-behind` section of troubleshooting (up to
     the next H2) holds a fenced bash block byte-equal to each of the five at a glance catch-up commands read from
     the five level pages, and no fenced line with `solution/level-N` or `<branch>`; `index.md` holds no
     `git switch <branch>` line and links to `./reference/troubleshooting.md#far-behind`;
     (c) "the setup page starts the app in the terminal that holds the key": `00-setup-check.md` has
     `## Start the app` after `## Set the workshop key` and before `## Choose the model`, with a fenced bash block of
     exactly `./mvnw -pl bookstore spring-boot:run` and the text `http://localhost:8080/shop/`, and the Level 0 page
     links to `./00-setup-check.md#start-the-app`.
  2. Negative control: on the pre-fix docs (the five `guide/docs/` files restored from `git show HEAD:<file>` via
     copy-aside + SHA-256; never `git checkout --`, `git restore` or `git stash`), with the new test and the fixed
     check files in place, `node --test guide/scripts/level-start.test.mjs` FAILS on all three cases with these
     shapes - (a) an assertion that shows `git stash -u && git switch main` against
     `git stash -u && git switch -C level-0 level-0-start` (or the Level 1 equivalent); (b) a missing
     `git stash -u && git switch -C level-0 level-0-start` block in the far-behind section; (c) a missing
     `## Start the app` section - not with an import, syntax or file-not-found error; and
     `node guide/scripts/check.mjs` exits 1 with the structure error
     `the Catch-up must be a fenced bash block of exactly "git stash -u && git switch -C level-0 level-0-start"` for
     `docs/01-level-0-break-the-agent.mdx` (and the Level 1 equivalent). With the fix in place all three cases
     PASS and `check.mjs` exits 0. Both runs are recorded verbatim under `## Red evidence` and `## Green evidence`.
  3. The commands work: in a throwaway clone (`mktemp -d "$TMPDIR/f11-t1-impl-XXXXXX"`, `git clone` of ROOT, then
     `git switch main`), after a simulated Level 0 (one commit of a new file, one modified tracked file, one untracked
     file, and an ignored `notes/level-0.md`), the Level 1 command exactly as the page shows it exits 0, leaves
     HEAD equal to `git rev-parse level-0-start^{commit}`, the branch `level-1`, an empty `git status --porcelain`,
     the stash holding the dirty work, and `notes/level-0.md` in place; a second run after more Level 0 work does the
     same (`Reset branch 'level-1'`); the Level 0 command from a fresh clone and from the dirty state lands on the
     tag on branch `level-0`. Output recorded under `## Command proof`.
  4. Not a weakening (D2, D3): for every changed line of `structure.mjs`, `structure-fixtures.mjs`,
     `structure.test.mjs`, the two skeleton fixtures, `coverage.mjs` and (if touched) `coverage-manifest.test.mjs`,
     the outbox lists old -> new; Levels 2 to 4 expected values are byte-identical to `<pr-base>`; the node:test
     count only grows (base 514, every base test title still present or renamed only where it quoted the old
     template, listed); every negative catch-up case of `structure.test.mjs` still fails on its mutant (extra
     command, missing stash, inline code instead of a block, a second block); the coverage line stays
     `anchored 304/304, 15 manual rows, 0 entries pending`.
  5. Pages: plain English, ASCII, no em dash; no new PowerShell line; no page names `ANTHROPIC_API_KEY` outside the
     setup and troubleshooting pages' existing warnings, and none tells anyone to set it; the Level 1 prose no longer
     says to start on `main`; nothing outside fileScope changes.
  6. Gates green: `npm --prefix guide run check`, `typecheck`, `build`, `check:built`, `build:offline`, and the
     referee `env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify`
     within 60 s (counts as the base).

## QA feed-forward

- FF-1 (T1 r0, for later tasks): `node --test guide/scripts/level-start.test.mjs` passes 3/3 or more;
  `npm --prefix guide run check` keeps `anchored 304/304, 15 manual rows, 0 entries pending` (or states a new count
  with its reason); T1's other files are unchanged unless the task names them; a TS-11 change is one entry, and the
  verdict says whether `pageSnippet` is still needed when the pack snippet equals a page command.
- FF-2 (T1 r0, adopted for T2): pin in `level-start.test.mjs` case (c) that the `## Set the workshop key` section
  links `(#start-the-app)` and that the Start the app section says to use the same terminal that holds the key;
  must-fail probes: the link turned into plain text (M10), the sentence deleted.
- qa-final items: the far-behind labels (Level 0 to Level 4) match their commands; the same-terminal sentence is
  present; reflog-only recovery of committed work after `-C` is a Gate 2 residual accepted by D1.

## Amendments

- A1 (2026-11-10T14:50Z, main session decision, applied by the orchestrator): the pack and the guide must agree.
  The main session authorised editing `docs/spec-pack/workshop/workshop-plan.md` lines 132 and 347 only, to match
  the new catch-up commands and `level-0-start`, keeping every coverage anchor and hash binding consistent. This
  lifts the Ground rule "Nothing under `docs/spec-pack/` is edited" for exactly those two lines; `progress.md` and
  `decisions.md` stay untouched. Consequence: new task T2 (below), cut after T1 merged; the bug.md Blast radius
  item "Docs or pack text" moves from deferred to task T2. The QA lead's FF-2 is folded into T2. Gate 1 re-run:
  self-accepted by the orchestrator (owner-delegated).
