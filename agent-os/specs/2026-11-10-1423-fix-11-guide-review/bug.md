---
bug: fix-11-guide-review
severity: S3
path: lite
status: open
closes: [review S1 Level 0 and 1 catch-up on main, review S2 no run command for the app, review S3 generic catch-up form]
---
# Bug - the guide's Level 0 and 1 catch-up does not reset to the start, and no page says how to start the app

## Report
- Observed (fresh read-only review of the guide merged as 0bcbcfe, spec `docs/spec-pack/specs/11-workshop-guide-site.md`,
  decisions D9, D16, D17):
  - S1: `guide/docs/01-level-0-break-the-agent.mdx:14,19` and `guide/docs/02-level-1-context-engineering.mdx:15,19,29`
    start Levels 0 and 1 from `main` and catch up with `git stash -u && git switch main`. D17 freezes `level-0-start`
    (a tag on b668073, the same commit as `main`, not a branch) as the Level 0 start and the base of
    `solution/level-1`. A participant whose agent committed during Level 0 stays on that commit after the catch-up.
  - S2: `guide/docs/00-setup-check.md:122-148` says to set `BOOKSTORE_AI_API_KEY` "in the terminal where you run the
    app", but no page gives the run command `./mvnw -pl bookstore spring-boot:run` (README.md "Run", storefront at
    http://localhost:8080/shop/). Level 0 says the storefront is where you see the result.
  - S3: `guide/docs/reference/troubleshooting.md:106-110` and `guide/docs/index.md:52-56` give one generic catch-up
    form (`git stash -u && git switch solution/level-N`, `git switch <branch>`), wrong for Level 0 and 1
    (`level-0-start`) and Level 4 (`start/level-4-<track>`).
- Expected: Level 0 and 1 catch-up commands land on `level-0-start` from any Level 0 state, also when re-run; the
  setup page says how to start the app in the terminal that holds the key, and Level 0 points at it; the far-behind
  help names the command of every level.
- Environment: factory 8e7b279, guide as deployed from 0bcbcfe.
- First seen / last known good: spec 11 review 2026-11-10 -> regression window: not a regression (written that way;
  D17 came after the page grammar was frozen).
- Frequency: always.
- Impact: participants who fall behind in Levels 0 or 1 start Level 1 on a tree that still holds the Level 0 agent's
  commits; participants cannot see the storefront without guessing the run command. S3: degraded, a workaround
  exists (README).
- Related: D17 (`docs/spec-pack/orchestrator/decisions.md`), spec 11 folder
  `agent-os/specs/2026-11-08-2206-11-workshop-guide/`.

## Reproduction
- Status: reproduced (orchestrator, 2026-11-10T14:15Z, throwaway clone under `$TMPDIR/f11-repro.*`, script
  `f11-repro.sh` in the session scratchpad).
- Recipe: clone, `git switch main`; simulate Level 0 (the agent commits a new file, then a tracked file is modified
  and an untracked file added); run the guide's catch-up `git stash -u && git switch main`.
- Evidence:
  - A: `Already on 'main'` / `Your branch is ahead of 'origin/main' by 1 commit.`, exit 0; HEAD 20ec10a is not the
    tag b668073; the agent's `bookstore/Discount.java` is still present.
  - B: `git switch level-0-start` -> `fatal: a branch is expected, got tag 'level-0-start'`, exit 128.
  - C: `git stash -u && git switch -C level-1 level-0-start` from the same state -> `Switched to a new branch
    'level-1'`, HEAD equals the tag; re-run after more Level 0 work -> `Reset branch 'level-1'`, HEAD equals the tag.
    `-c` instead of `-C` fails on a re-run: `fatal: a branch named 'level-1' already exists`, exit 128.
  - S2: `grep -rn "spring-boot:run" guide/docs` finds only the distributor command (Level 2, troubleshooting); no
    page holds `./mvnw -pl bookstore spring-boot:run` or `localhost:8080/shop`.
  - S3: `grep -n "git switch" guide/docs/reference/troubleshooting.md guide/docs/index.md` shows only the generic
    forms.
- Regression-test layer: guide self-test (node:test, `guide/scripts/*.test.mjs`) reading the live docs, as
  `guide/scripts/live-guide.test.mjs` does, plus the structure rule of `npm run check`.
- Test file: `guide/scripts/level-start.test.mjs` - run with `node --test guide/scripts/level-start.test.mjs` from the
  repository root.

## Root cause
- Causal chain: participant falls behind -> runs the at a glance catch-up -> `git switch main` keeps the Level 0
  commits -> **root cause: the page grammar pins Level 0 and 1 to `main`
  (`guide/scripts/rules/structure.mjs:20-35` LEVELS `catchUp: 'main'`, Level 1 `start: '`main`'`, and the fixed
  template `git stash -u && git switch ${spec.catchUp}` at `structure.mjs:208`, which cannot express a tag)** ->
  the pages were written to that grammar (`01-level-0-break-the-agent.mdx:19`, `02-level-1-context-engineering.mdx:14,19,28`)
  and the far-behind help copies the template (`troubleshooting.md:106-110`, `index.md:52-56`, coverage entry TS-11
  at `guide/scripts/coverage.mjs:457`). The checks passed because they enforce the defective contract.
- Root cause (one sentence): the frozen page grammar in `guide/scripts/rules/structure.mjs:20-35,208` encodes the
  pre-D17 start `main` and a `git switch <branch>` template that cannot target the tag `level-0-start`, so the pages,
  the far-behind help and the checks all agree on a catch-up that does not reset Level 0.
- S2 root cause: `guide/docs/00-setup-check.md:123-150` sets the key but no section of any page starts the app (the
  coverage manifest has no entry for README "Run", which is not a pack source).
- Contributing factors: D17 landed after the grammar freeze (spec 11 T13); the pack wording (`workshop-plan.md:132`
  "`main` again (clean)", `:347` far-behind row) still says `main` / `solution/level-N`.
- Introduced by: spec 11 (structure rule T3/T4, pages T5 to T13) - intended: one copyable catch-up per level.

## Blast radius
- [x] Same pattern elsewhere: `grep -rn "switch main\|solution/level-N\|git switch <" guide/` -> the two level pages,
  troubleshooting, index (task T1); structure rule, its fixtures and self-tests (task T1, named carve-out);
  `guide/scripts/fixtures/built-powershell/real-level-0-at-a-glance.html` and `built-powershell.test.mjs:243-251`
  -> deferred: a captured HTML fixture of the built PowerShell check, independent of the page grammar, stays as is.
- [x] Other callers: `guide/scripts/lib/structure-fixtures.mjs` and `guide/scripts/fixtures/skeleton-docs/docs/01-*`,
  `02-*` generate or hold the Level 0/1 at a glance box -> task T1.
- [x] Persisted data already wrong: none.
- [x] Artifacts already produced: the deployed guide on GitHub Pages and the offline zip -> regenerated by the guide
  workflow on the push to factory (approved, GUIDE_DEPLOY=true).
- [x] Docs or pack text encoding the wrong assumption: `docs/spec-pack/workshop/workshop-plan.md:132,347` -> task T2
  (plan.md amendment A1: the main session authorised exactly these two lines).
- [x] Pack product text (QA lead FF-4, T2 review): `docs/spec-pack/product/roadmap.md:62` "Catch-up rule for
  participants: `git stash -u && git switch solution/level-N`" -> deferred: outside amendment A1 (which allows only
  `workshop-plan.md` lines 132 and 347), and `roadmap.md` is the main session's; owner residual reported at hand-back.
  `docs/spec-pack/orchestrator/decisions.md:55` is decision history and needs nothing.
- [x] Sibling environments: none (no CI config or seed carries the catch-up).

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | grammar, pages and far-behind help change together; the checks stay exact |
| (b) revert | no | nothing to revert; D17 is newer than the grammar |
| (c) guarded workaround + follow-up | no | a docs-only patch fails the frozen structure rule |
Chosen: (a) - because the structure rule is the contract and must say what D17 says.

## Prevention
- Gap: a decision that renames a branch contract (D17) did not trigger a re-check of the page grammar that pins
  branch names, and the far-behind help had no check tying it to the level pages.
- Action: task T1 - the new self-test ties the far-behind help to the five at a glance commands, so a future rename
  cannot leave the help behind.

## Verification
- Regression test red on unfixed -> green on fixed (impl `## Red evidence`, QA `## Negative control`)
- Blast-radius items closed or deferred
- `npm --prefix guide run check`, typecheck, build, check:built, build:offline and `./mvnw clean verify` green
