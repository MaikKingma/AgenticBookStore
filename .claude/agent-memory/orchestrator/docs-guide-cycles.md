---
name: docs-guide-cycles
description: Traps of docs, guide-site and pack-wording cycles (checker design, frozen checkers, truth tables, wording sweeps, contrast, live-site pages, claim verification)
metadata:
  type: project
---

## Checkers
- Content checkers that model MDX with regexes loop (spec 11 T5: every round found a new bypass). Decide at shaping
  whether a source-level oracle is worth it; prefer checking the built HTML for security-relevant rules and give the
  QA lead a closed construct list as the bar from round 1.
- Freeze the checker by a hash baseline once content tasks start; bind each page by a hash file per task, so later
  one-line amendments prove themselves by removal hashes.
- The frozen guide checker often pins the defect itself (structure rule LEVELS, coverage pageSnippet, the pack-literal
  sweep in `coverage-manifest.test.mjs`): grep `guide/scripts` before planning and name each check change in the plan.
  A pack edit adds literals the sweep flags: allow `SWEEP_EXCLUDED` keys in the task scope from the start.
- Before touching a frozen checker on a coordinator brief, grep what it really pins: the brief may rest on a premise
  from a decision's wording that never existed in code.
- A Node CLI self-test that reads a long failing stderr over a pipe is flaky on Linux only (`process.exit` drops the
  tail): capture through files, reproduce in a Linux container before any ruling.

## Wording and truth
- Blast radius of a wording bug: grep ALL of `docs/spec-pack/` (product, specs, workshop) for the old wording up front.
- Doc-heavy tasks narrow slowly under QA lead REJECTs for false universal or descriptive sentences. Put "per-sentence
  truth table with a git grep counterexample per claim" into the implDirective of round 0.
- A "line N reads X" criterion deletes other sentences on that line: name the sentence.
- A literal ban in a directive needs replacement wording; trim-for-fit needs a list of qualifiers that must survive.
- Rewriting a census test can silently drop an all-rows check: ask the lead to diff old clauses, not counts.
- Seed or catalogue amendments: count the enum against the pack before planning (spec 09 D12 left no room for the
  required rows); a contradiction with the pack is a stop with options.
- Rehearsal rows: start commit, `reset --hard <start>` plus `clean -fdx`, judge against start, fresh clone folder for
  auto memory, sourced start triggers with a branch.

## Guide site
- A brand hex below AA collides with pinned 4.5 text pairs; relaxing a pinned check is an owner question with two
  options (answer was: AA shade for text, exact hex for non-text only). Keep the other tasks running meanwhile.
- A sweep at a new width can surface a pre-existing axe hit: prove it on the base build, exclude by amendment, log a
  residual.
- Live-site additions: characterize first (a built rule freezing live routes, sitemap, prev/next), register the page as
  a stub, add guards, then write content; a new page takes a fractional `sidebar_position` so no URL moves.
- Verify Claude Code doc claims by curl of the raw `.md` pages, never WebFetch summaries; a claims table (sentence, id,
  file:line, quote) in the content task's outbox, copied by me into `references.md`.
- Keep the QA lead's beginner read even on a fast path: it caught a mode-dependent instruction (approval prompts differ
  in auto mode) that the verifier passed.
