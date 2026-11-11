---
spec: 16-bonus-level-orchestration
pack: docs/spec-pack/specs/16-bonus-level-orchestration.md
decision: D20
base: factory
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-11T12:38:22Z
qa-lead: on
---

# Plan: spec 16 bonus level, orchestrate your own team

Shape: `shape.md` (scope, research, decisions D-S1 to D-S8, SA1, SA2, owner items Q1 and Q2). Doc citations and the
verified claims: `references.md`. QA strategy: `qa-strategy.md` (snapshot: tiers, findings G-1 to G-6, rewrites, the
common directive C0-1 to C0-14, task directives, qa-final focus). Branches: spec `sdd/16-bonus-level-orchestration/spec`
cut from `origin/factory` 8d5ccd8; tasks `sdd/16-bonus-level-orchestration/T<n>`, cut from the spec tip one at a time
and squash-merged back by the orchestrator after verifier PASS, QA lead ENDORSE, the ship gate and green checks. The
spec PR into `factory` is the owner's merge.

qaDirective per task = `qa-strategy.md` C0 plus that task's block under "Per-task qaDirectives", as amended by "Gate 1
resolutions" below, plus every `## QA feed-forward` entry. qaModel is sonnet for every task by owner policy; the QA
lead's opus meta-review compensates on the critical tier.

| id | module | dependsOn | batch | summary | qaTier | qaModel | pr | branch | verdict | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | guide | [] | 1 | built rule pins the 11 live routes, the exact sitemap and the prev/next chain | critical | sonnet | (pending) | sdd/16-bonus-level-orchestration/T1 | PASS r0 | ENDORSE r0 |
| T2 | guide | [T1] | 2 | register the bonus page as a stub with its pinned marker; every 11 -> 12 pin; plan stub section; routes 12; Status line | critical | sonnet | | | | |
| T3 | guide | [T2] | 3 | guards: page-to-plan pack membership, placeholder-only fragment, no minutes, no workflow script code | critical | sonnet | | | | |
| T4 | guide | [T3] | 4 | the bonus page content, its plan mirror and coverage, outline and cleared-when pins, claims table | critical | sonnet | | | | |

## Common to every task (C)

- Module `guide`. ASCII only, no em dash or en dash, plain English; no absolute home path in any committed file; no
  new npm dependency (`guide/package.json`, the lockfile and `docusaurus.offline.config.ts` hash-equal to the spec
  base); never skip, delete or weaken a test or rule; every pin change listed old -> new with its reason in the
  outbox; `SWEEP_EXCLUDED` byte-identical; node:test total at least the previous tip's (802 at the base) plus the
  task's new tests.
- Untouched: `main`, level branches, tags, the release, Pages settings, `.github/`, `guide/docusaurus.config.ts`,
  `guide/sidebars.ts`, `guide/src/**`, `guide/static/**`, the 11 existing pages under `guide/docs/`, the frozen
  `guide/scripts/lib/mdscan.mjs`, `guide/scripts/rules/coverage.mjs`, `guide/scripts/rules/plan.mjs`, and every pack
  file except `docs/spec-pack/workshop/workshop-plan.md` and (T2 only) `docs/spec-pack/specs/16-bonus-level-orchestration.md`.
- No new file under `.claude/agents/`, `.claude/workflows/`, and no `*.js` outside `guide/scripts`. No headless Claude
  Code (D14). Every `mktemp` with an explicit template under `$TMPDIR`.
- All local gates green on Node 26.13.0 (`npm ci`, `npm run check`, `npm run typecheck`, `npm run build`,
  `npm run check:built`, `npm run build:offline`, `node scripts/check-built.mjs build-offline`) and the referee
  `./mvnw clean verify` under 60 s.
- Implementers never commit, branch or push (the orchestrator owns git) and describe source mutants in the outbox
  instead of applying them to production files outside their scope.

## T1 Characterize the live site: routes, sitemap and the level flow

- fileScope: `guide/scripts/rules/routes-built.mjs` (new), `guide/scripts/routes-built.test.mjs` (new),
  `guide/scripts/rules/built.mjs`, `guide/scripts/check-built.mjs`, `guide/scripts/navbar.test.mjs`,
  `guide/scripts/level-progress.test.mjs`, `guide/README.md`.
- Acceptance criteria:
  1. `routes-built.mjs` holds a frozen list of the 11 live `<loc>` values verbatim
     (`https://maikkingma.github.io/AgenticBookStore/`, then `setup-check`, `level-0-break-the-agent`,
     `level-1-context-engineering`, `level-2-give-your-agent-eyes`, `level-3-behavioral-control`,
     `level-4-spec-driven-development`, `wrap-up`, `reference/cheat-sheet`, `reference/architecture`,
     `reference/troubleshooting` under that base) and fails when a route's build file is missing (`index.html`,
     `<route>.html`, `reference/<name>.html`) or when the `<loc>` values of `build/sitemap.xml` are not exactly that
     list. Witnesses in `routes-built.test.mjs`: route file deleted (top level, nested `reference/troubleshooting.html`
     and `index.html`), extra loc, missing loc, one loc replaced by another at the same count, a duplicated loc, a
     trailing-slash loc, sitemap absent.
  2. The rule pins the full prev/next chain in the built HTML of every listed page: Welcome -> Setup check -> Level 0
     to Level 4 -> Wrap-up -> Cheat sheet -> Architecture -> Troubleshooting (prev and next hrefs, first page no prev,
     last page no next). Witnesses: a swapped next href, a swapped prev href, a missing pagination nav, an extra next
     on Troubleshooting, an extra prev on Welcome, a next href with an extra path segment.
  3. (G-1, option A) Wired in `check-built.mjs` like `navbar` and `levels`: it runs when no build dir is given, so
     the copied-scripts fixture builds of `navbar.test.mjs` and `level-progress.test.mjs` gain a sitemap, the 11
     route files and the pagination chain; every count or message change in those two files is a listed retarget
     (old -> new, reason), no assertion is removed. `node scripts/check-built.mjs build-offline` and every fixture
     build with an explicit dir print no routes line. New CLI tests in `routes-built.test.mjs`: no argument on a
     real-shaped fixture prints the ok line; no argument with the sitemap removed exits 1 with the routes message; an
     explicit dir prints no routes line. Ok line: `ok   routes: 11 routes in the sitemap and the build, prev/next chain
     of 11 pages`.
  4. `npm run check:built` on a fresh `npm run build` of the tip prints the ok line; the outbox quotes a read-only
     `curl` of the live sitemap whose locs equal the frozen list. No file under `guide/docs` changes.
  5. `guide/README.md` "Rules in place" gains one bullet describing the rule and that a page addition must extend the
     list with a reason.

## T2 Register the bonus page with its marker

- fileScope: `guide/docs/06-bonus-level-orchestration.mdx` (new), `docs/spec-pack/workshop/workshop-plan.md`,
  `docs/spec-pack/specs/16-bonus-level-orchestration.md` (Status line only), `guide/scripts/rules/structure.mjs`,
  `guide/scripts/lib/structure-fixtures.mjs`, `guide/scripts/structure.test.mjs`,
  `guide/scripts/structure-check.test.mjs`, `guide/scripts/check-content.test.mjs`,
  `guide/scripts/live-guide.test.mjs`, `guide/scripts/skeleton-ban.test.mjs`,
  `guide/scripts/fixtures/skeleton-docs/docs/06-bonus-level-orchestration.mdx` (new),
  `guide/scripts/fixtures/skeleton-docs/README.md`, `guide/scripts/fixtures/skeleton-docs.mjs` (comment),
  `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`, `guide/scripts/check.mjs` (comment only),
  `guide/scripts/rules/routes-built.mjs`, `guide/scripts/routes-built.test.mjs`, `guide/scripts/navbar.test.mjs`
  and `guide/scripts/level-progress.test.mjs` (required, A1: their fixtures follow the 12-route list; listed
  retargets), `guide/README.md`.
- Acceptance criteria:
  1. Page: frontmatter exactly `title: "Bonus level: orchestrate your own team"` and `sidebar_position: 7.5`; no
     `slug`, `id`, `pagination_*`, `sidebar_label` or `LevelProgress`; the body is only the
     `:::info[Optional bonus level]` box, saying the page is optional, for those who finish Level 4 early or at home,
     has no time slot and no solution branch, and linking `./06-wrap-up.md`. `build/bonus-level-orchestration.html`
     exists.
  2. `structure.mjs`: `OTHER_PAGES` gains the entry (title, position `7.5`, `anchors: []`, a marker flag); a marker
     check fails when the page has no `:::info[Optional bonus level]` box, has it with another type or title, does not
     have it as the first body element, has two, has no `./06-wrap-up.md` link inside it, or imports or renders
     `LevelProgress`. The outbox states what may precede the box (blank lines, MDX comments) and each allowed
     precedent has a passing witness; a paragraph, a heading, an import line and another admonition before the box
     each fail; a wrap-up link present only outside the box fails. One `structure.test.mjs` witness per failure mode
     plus position `8` instead of `7.5`; messages say "twelve pages".
  3. (G-3) Every page-count pin becomes 12 or twelve exactly, including `check-content.test.mjs:283` and
     `skeleton-ban.test.mjs:176` (old -> new list in the outbox). Proof: `git grep -n -i -P
     'eleven|(?<![0-9.])11(?![0-9])|on 10 skeleton'` over `guide/scripts` and `guide/README.md`: every hit is either
     retargeted or on the NOT-a-pin list of `qa-strategy.md` (Dry runs) and byte-identical.
  4. (G-2) Fixture: `git diff --stat <pr-base> -- guide/scripts/fixtures/skeleton-docs/docs` shows exactly one added
     file; `fixtures/skeleton-docs/README.md` and the `skeleton-docs.mjs` comment change only by the sentence(s) that
     say the bonus page was added later, with the reason; the README keeps "Never edit these files" for the original
     files. The fixture page holds no deny-listed phrase.
  5. Coverage: `PAGE_FILES.BO`; entry `BO-01` (kind text, source plan, section `Bonus level`) pins the marker
     sentence; a BO floor of 1; `npm run check` prints the anchored count of the base plus one, 15 manual rows, 0
     entries pending.
  6. Plan: heading `## Bonus level: orchestrate your own team (optional)` and one prose paragraph (no backticks, no
     minutes, no `**Level cleared when**`) inserted after line 451; `git diff -U0` of the plan is one insertion hunk
     after line 451; `SWEEP_EXCLUDED` byte-identical; `rules/plan.mjs` still finds five `**Level cleared when**`.
  7. Routes rule: the frozen list gains `https://maikkingma.github.io/AgenticBookStore/bonus-level-orchestration`, the
     chain becomes Level 4 -> bonus -> Wrap-up; the rule's diff is exactly those insertions and the count; every T1
     witness stays; ok line says 12.
  8. Spec 16 Status line replaced with: started by the owner on 2026-11-11 during the live workshop as a full SDD
     cycle; no "starts automatically" text left; nothing else in that file changes.
  9. `guide/README.md` documents the marker rule.

## T3 Guards: pack membership, no script code, placeholder-only fragment, no minutes

- fileScope: `guide/scripts/bonus-page.test.mjs` (new), `guide/scripts/rules/content.mjs`,
  `guide/scripts/content.test.mjs`, `guide/README.md`.
- Acceptance criteria:
  1. Pack membership: every inline code span and every trimmed fenced line of the live bonus page equals a whole
     inline span or whole fenced line of the plan's `Bonus level` section (not just any pack file), using the
     checker's own extraction; the frozen `lib/mdscan.mjs`, `rules/coverage.mjs`, `rules/plan.mjs` stay hash-equal. A
     missing bonus heading in the plan is an error. Witnesses on fixture strings: a span only on the page fails; a
     fenced line only on the page fails; a span present in another plan section but not the bonus section fails; a
     matching span passes.
  2. Fragment rule: at most one fenced block on the bonus page contains subagent frontmatter key lines; every key
     line's value is a single angle-bracket placeholder (`^<[^>]+>$`); the block holds no other line than `---` and key
     lines. Witnesses: a real value (`tools: Read, Grep`), a body line, two fragments; zero fragments passes.
  2b. (added) An inline span on the bonus page of the form `<key>: <value>` whose key is a subagent frontmatter key
     has a single angle-bracket placeholder value (`` `tools: Read, Grep` `` fails, `` `tools: <tools>` `` and
     `` `tools` `` pass).
  3. Script-code ban in `rules/content.mjs`, applied to every page: `export const meta` anywhere, and
     `/\b(agent|pipeline|parallel|phase)\s*\(/` inside any inline span or fenced line, fail with a message about "no
     workflow script code in the guide". `content.test.mjs` witnesses per token in a span and in a fence, plus prose
     negatives ("the agents (two of them)" passes). Today's 11 pages and the stub pass.
  4. (G-4) No time slot: `/\b\d+\s*-?\s*(min|mins|minute|minutes|hour|hours)\b/i` and
     `/\b(an?|half an?|one|two|three|ten|fifteen|twenty|thirty|forty|forty-five|sixty|ninety)\s*-?\s*(minute|minutes|hour|hours)\b/i`
     fail on the bonus page and on the plan's `Bonus level` section. Witnesses `10 min`, `10min`, `10 mins`,
     `1 minute`, `30-minute`, `an hour`, `ten minutes`, one in the plan section; negatives `2.1.154`,
     `claude --version`.
  5. `npm run check` green; the outbox shows each witness failing with its message, and `guide/README.md` lists the
     rules.

## T4 Bonus page content

- fileScope: `guide/docs/06-bonus-level-orchestration.mdx`, `docs/spec-pack/workshop/workshop-plan.md`,
  `guide/scripts/coverage.mjs`, `guide/scripts/coverage-manifest.test.mjs`, `guide/scripts/bonus-page.test.mjs`.
  (`references.md` is the orchestrator's, SA2.)
- Acceptance criteria:
  1. Outline per the strategy (`shape.md` D-S1 to D-S5 and the page outline below): the marker box first, an intro,
     an `:::note[At a glance]` box, "What you bring", `## Part A: ...` to `## Part D: ...`, `:::tip[Make it yours]`,
     `## Before you move on`, and the page ends with `## Bonus cleared when` and its task list.
  1b. (added) `bonus-page.test.mjs` pins the exact H2 sequence (the four Part headings, `Before you move on`,
     `Bonus cleared when`, exact strings), the marker box first, exactly one `:::tip[Make it yours]`, and nothing after
     the cleared-when list. Witnesses: Part D heading removed, two Parts swapped, a paragraph after the list.
  2. The plan bonus section mirrors every span and fenced line of the page; `SWEEPS[0].pages` gains
     `['Bonus level', ['BO']]`; coverage entries cover every plan literal with zero new `SWEEP_EXCLUDED` keys and zero
     additions to the short-snippet list (snippets of 8 or more characters); the BO floor equals the live BO count;
     `anchored N/N, 15 manual rows, 0 entries pending`.
  3. `bonus-page.test.mjs` gains: the `## Bonus cleared when` items equal the plan's `**Bonus cleared when**` list in
     order and count; an anti-vacuity floor (the number of spans and fenced lines on the page at this tip, pinned
     exactly), so the T3 membership check can never pass on an emptied page.
  4. Claims (HR1, G-5): every sentence on the page that states Claude Code behaviour has a row in the outbox
     `## Claims table` (page sentence -> claim id -> URL and line); every quote is found with `grep -F` in a fresh raw
     `.md` fetch; a sentence without a verbatim quote (for example "permissions and hooks still apply to subagents",
     "a run counts toward your plan's usage", "read the script before you approve") gets a new id with its quote or is
     removed. No banned claim of `references.md` appears. Each `code.claude.com` link on the page answers 200 (curl in
     the outbox). Glob and Grep: either not named, or named with the documented macOS, Linux and WSL caveat (S4).
  5. No solutions (HR2): at most one placeholder fragment (T3 rule green), no agent system-prompt text, no script; (G-6)
     `.claude/agents/architecture-reviewer.md` appears only as an example that exists on the `solution/level-3` and
     `start/level-4-<track>` branches (outbox: `git ls-tree` per ref), described as an architecture reviewer, never as
     "on your branch" unconditionally.
  6. Deny-list, ASCII, colours and admonition rules green; the pack audit prints 0 page literals outside the plan;
     rendered check of the page (light and dark, 1280 and 390 px, Chromium, Firefox and WebKit, both builds), axe 0
     violations.

### Page outline and wording constraints (from the strategy, with Gate 1 resolutions)

1. Frontmatter only (title, position).
2. `:::info[Optional bonus level]` first: optional, for when you finish Level 4 early or continue at home; no time
   slot, no solution branch, you build your own; "Not finished with Level 4? Go to the Wrap-up" with the link.
3. Short intro: what you will have at the end (your flow on paper, two subagents of your own, one feature through the
   flow with you as the gate, and, if your account has it, the flow written as a workflow by Claude). One sentence on
   vocabulary: here "workflow" means Claude Code's dynamic workflows, not the Level 3 skill. Say "main session", never
   "orchestrator"; no "agent team".
4. `:::note[At a glance]`: goal; start from where you finished Level 4, no new branch; what you need: `claude --version`
   2.1.154 or later for Part D, and a plan with dynamic workflows; no fixed time.
5. What you bring from Levels 3 and 4 (no re-teaching; no unquoted behaviour claim, G-5).
6. Part A, think first: the steps of one feature (understand, research, plan, implement, verify, review), each marked
   "agent" or "human gate", starting from the participant's Level 4 track. Questions, no answers.
7. Part B, define two subagents: what a subagent is (S2, S9, S5); files in `.claude/agents/` (S1); the parts as a table
   of "part, what it does, question to answer" for `name`, `description`, `tools` (optional, inherits all when
   omitted; documented names to choose from), `model` (optional), the body as the instructions, and what it hands
   back; one research agent and one QA agent (behaviour: tests and scenarios); limit commands through permission rules,
   not the `tools` line; ask Claude to write the file from your answers or write it by hand (S7); the
   `architecture-reviewer` example per G-6; at most one placeholder fragment; link to the sub-agents docs.
8. Part C, coordinate with the main session: a small feature (for example the Level 4 brief you did not take); use the
   research agent, stop and read (your gate), run your track's flow, use the QA agent, commit through your own flow;
   check what each handed back and where you stepped in (S5 chaining).
9. Part D, let Claude write it as a workflow: check availability (2.1.154, the Pro `/config` row, an organization admin
   can switch workflows off, W6, W7); without workflows the bonus is done with Part C; otherwise ask in your own words
   for a workflow that runs one stage of your flow and describe the roles from Part B in the request (no claim about
   agent files, W9); `/workflows` to watch (W3); no input while it runs, so your gate sits between two runs (W8); save
   from `/workflows` to rerun as `/<name>` (W3, W4); workflows start from your main session (SA:438). No script code,
   no API names. Link to the workflows docs.
10. `:::tip[Make it yours]`: adapt the flow, add a gate, swap an agent role; the guide's refrain.
11. `## Before you move on`: one question (which gate did you keep human, and why).
12. `## Bonus cleared when`: your flow with at least one human gate written down; two subagent files of your own in
    `.claude/agents/` with a description, the tools you chose and what each hands back; one feature through the flow
    with both subagents and `./mvnw verify` green; if you have workflows: one run you read before it ran, and you can
    say where your gate sits.

## Gate 1 resolutions (orchestrator, 2026-11-11)

- G-1: option A (spec 14 precedent FF-T1-2): `navbar.test.mjs` and `level-progress.test.mjs` join T1's scope; their
  fixture builds gain the sitemap, route files and pagination; count changes are listed retargets.
- G-2, G-3, G-4: the QA lead's rewrites taken verbatim into T2.4, T2.3 and T3.4.
- G-5: T4.4 requires a verbatim quote per behaviour sentence or its removal.
- G-6: T4.5 makes the `architecture-reviewer` pointer conditional on the branches that carry it.
- Added criteria taken: T1 extra witnesses (in T1.1, T1.2), T2 precedent rule (in T2.2), T3 2b, T4 1b.

## QA feed-forward

Full wording in the QA lead's ledger (review log, T1 r0). Every later task's qaDirective includes these.

- FF-T1-1 (T2 scope and implementer): `guide/scripts/navbar.test.mjs` and `guide/scripts/level-progress.test.mjs` are
  REQUIRED edits in T2 (see A1). `level-progress.test.mjs` `fullBuild()` looks up a page per route of the list, so a
  12th route throws a TypeError unless `'bonus-level-orchestration.html': page('')` is added. Expected retargets:
  `navbar.test.mjs` `navbar: 11 pages` -> 12 and `built: 11 html files` -> 12; `level-progress.test.mjs`
  `none on the 6 other pages` -> 7 and `navbar: 11 pages` -> 12; the routes ok line 11 -> 12 in all three test files;
  `routes-built.test.mjs` typed `LIVE` list gains the bonus loc between Level 4 and Wrap-up and
  `{routes: 11, chainPages: 11}` -> 12.
- FF-T1-2 (C0-9 correction, T2 to T4 and qa-final): the non-ASCII grep over
  `docs/spec-pack/specs/16-bonus-level-orchestration.md` prints exactly one pre-existing line (line 3, a middle dot
  U+00B7 in the pack header); that line is the accepted baseline and must stay the only hit. The plan prints nothing.
- FF-T1-3 (T2 verifier): the rule changes in T2, so copy `.claude/comms/tmp/s16-t1-qa-mutants.mjs` to an `s16-t2-qa-`
  name (diff quoted), re-run all 41 mutants plus a new one (Level 4 next straight to Wrap-up must fail), and re-run the
  real-build replays R1 to R15 on the 12-page build.
- FF-T1-4 (T2 implementer): add a dated reason line for the bonus page to the rule's header comment in the T1 form
  `- 2026-11-11 (spec 16, T2): ...`.
- FF-T1-5 (qa-final): right before hand-back, re-run the live chain check on the 11 live pages (0 errors) and re-curl
  the live sitemap; it must equal the T1 list.
- Optional (T3 owns README edits): `guide/README.md:24` still describes `check:built` as a `:::` scan only (predates
  spec 16); T3 may correct it while adding its rule bullets.

## Amendments

- A1 (2026-11-11, orchestrator, from FF-T1-1): T2's scope entry for `guide/scripts/navbar.test.mjs` and
  `guide/scripts/level-progress.test.mjs` changes from "only if their T1 fixtures must follow" to required; the edits
  are listed retargets plus the `fullBuild()` page for the bonus route. Test-only, inside T2, no criterion weakened;
  Gate 1 re-run self-accepted.

## Decisions

- Gate 1 self-accepted by the orchestrator: every task has summary, criteria, fileScope, module, dependsOn, qaTier,
  qaModel and qaDirective; every criterion the QA lead flagged is rewritten; batches follow dependsOn; HR1 is settled by
  the official docs (`references.md`); no decision outside the orchestrator's authority. Owner items Q1 and Q2
  (`shape.md`) concern the owner's merge, not the cycle.
