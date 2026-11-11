# QA strategy: spec 16 bonus level orchestration

Snapshot of the QA lead's strategy (qa-lead, opus, 2026-11-11), copied verbatim from its ledger
`.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-16-bonus-level-orchestration.md` (section "QA strategy") at Gate 1.
The Gate 1 findings G-1 to G-6 and the added criteria are resolved in `plan.md` (section "Gate 1 resolutions"); where
this snapshot offers options, `plan.md` names the one taken. The ledger stays the live record of reviews.

## QA strategy

### Inputs read

Pack spec 16; lead strategy `lead-1/outbox/strategy-16-bonus-level-orchestration.md` (task breakdown, outline,
gates, risks); res-6 doc claims; res-4 gates and sweep rig; res-3 placement; `guide/scripts/check-built.mjs`;
the CLI fixture tests of `navbar.test.mjs`, `level-progress.test.mjs`, `table-regions.test.mjs`;
`fixtures/skeleton-docs.mjs` and README; dry-run greps listed under "Dry runs". Orchestrator amendments SA1 (T5
dropped: no edit to any existing live page) and SA2 (`references.md` written by the orchestrator from T4's outbox
`## Claims table`) applied.

### Dry runs done at strategy time (read-only)

- `git grep -n -i -P "eleven|(?<![0-9.])11(?![0-9])|on 10 skeleton" -- guide/scripts guide/README.md` on the base:
  the page-count pins are `check-content.test.mjs:50,51,253,283,285,287,288`, `live-guide.test.mjs:34,36`,
  `skeleton-ban.test.mjs:20,163,171,174,175,176`, `structure-check.test.mjs:27,135,167,169,170,189`,
  `structure.test.mjs:27,397`, `coverage-manifest.test.mjs:214`, `rules/structure.mjs:422,426`, comments
  `check.mjs:61`, `check-content.test.mjs:278`, `structure-check.test.mjs:159`, `fixtures/skeleton-docs.mjs:12`,
  `fixtures/skeleton-docs/README.md:3`. NOT pins (must stay byte-identical): `README.md:160` (fonts), 
  `built-powershell.test.mjs:70`, `fixtures/built-powershell/README.md:14`, `content.test.mjs:387,570`,
  `check-content.test.mjs:340,343`, `mdscan.test.mjs:233`, every `XX-11` manifest id and `S11:` row label in
  `coverage.mjs` and `coverage-manifest.test.mjs`, `coverage.mjs:70` (`11. Risks and mitigations`),
  `rules/skeleton-ban.mjs:1`, `skeleton-ban.test.mjs:13`, `live-guide.test.mjs:12`, `rules/structure.mjs:18`.
- `git grep -n -P "export\s+const\s+meta|\b(agent|pipeline|parallel|phase)\s*\(" -- guide/docs` prints nothing;
  over the plan it hits `workshop-plan.md:114` (`## 4. Level 0: Break the agent (25 min)`, a heading, not a span): a
  whole-text mutant of the T3 script ban still passes every live page, so only the prose-negative witnesses kill it.
- `git grep -c architecture <ref> -- .claude/agents/architecture-reviewer.md` over `origin/main`, `level-0-start`,
  `origin/solution/level-3`, `origin/start/level-4-{agentos,openspec,speckit}`: present on the last four only (G-6).
- `check-built.mjs` with no argument sets `{navbar, levels}`; the default-dir CLI fixture tests listed in G-1 exist.

### Risk tiers and models

| Task | Tier | qaModel | Why critical |
|---|---|---|---|
| T1 | critical | sonnet | Oracle for HR4 "every live URL resolves, prev/next flow". A vacuous or subset comparison makes every later green meaningless. Touches the shared `check-built.mjs` entry point (G-1). |
| T2 | critical | sonnet | Changes the live flow (Level 4 Next), registers the page in frozen rules, retargets about 25 pins, edits the frozen fixture folder and the plan. The marker box is the safety valve for behind participants. |
| T3 | critical | sonnet | The no-solutions and pack-membership oracle T4 is graded by; regex MDX scanners failed open four rounds in spec 11. |
| T4 | critical | sonnet | Participant-facing text on a live site, HR1 claims against docs that drift, HR2 no-solutions judgment, coverage and sweep edits. |

### Testability check and proposed rewrites

| Task | Criterion | Finding | Proposed testable rewrite (within scope) |
|---|---|---|---|
| T1 | 3 | Untestable as written (G-1): turning the rule on for the default dir breaks `navbar.test.mjs:295` and `level-progress.test.mjs:518` (both outside the fileScope). | "Wired in `check-built.mjs`; runs when no build dir is given [option A: and the fixture builds of `navbar.test.mjs` and `level-progress.test.mjs` gain a sitemap, the 11 route files and the pagination chain, both files added to the fileScope, every count change a listed retarget] [option B: and when <real-site predicate> holds; the predicate has a CLI witness each way and a test that the real tree satisfies it]. `node scripts/check-built.mjs build-offline` and every fixture build with an explicit dir print no routes line. New CLI tests in `routes-built.test.mjs`: no argument on a fixture shaped like the real build prints the ok line; no argument with the sitemap removed exits 1 with the routes message; an explicit dir prints no routes line." |
| T1 | 1 | Witness set misses a same-count swap and a duplicate, so a count-plus-subset comparator survives. | Add witnesses: one loc replaced by another (count 11), one loc duplicated (12 entries, set of 11), a loc with a trailing slash. |
| T1 | 2 | Witness set has no prev-side or first-page witness. | Add: a swapped prev href, an extra prev on Welcome, a next href with an extra path segment (`/AgenticBookStore/x/wrap-up`, kills `endsWith`). |
| T2 | 3 | Pin list incomplete; proof grep case-sensitive (G-3). | "`git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])|on 10 skeleton'` over `guide/scripts` and `guide/README.md`: every hit is either retargeted (listed old -> new) or on the ledger's NOT-a-pin list, unchanged." The retarget list adds `check-content.test.mjs:283` and `skeleton-ban.test.mjs:176`. |
| T2 | 4 | Self-contradictory (G-2). | "`git diff --stat <pr-base> -- guide/scripts/fixtures/skeleton-docs/docs` shows exactly one added file and nothing else; `fixtures/skeleton-docs/README.md` and the `skeleton-docs.mjs:12` comment change only by the sentence(s) that say the bonus page was added later, with the reason; the README keeps 'Never edit these files' for the 05e58a2 files." |
| T2 | 2 | "Not the first body element" is undefined (the fixture copy carries `{/* guide-skeleton */}`; MDX `import` lines and comments do not render). | "The outbox states what may precede the box (blank lines, MDX comments) and each allowed precedent has a passing witness; a paragraph, a heading, an import line and another admonition before the box each fail." |
| T3 | 4 | Regex does not falsify "no time slot" (G-4). | `/\b\d+\s*-?\s*(min|mins|minute|minutes|hour|hours)\b/i` plus `/\b(an?|half an?|one|two|three|ten|fifteen|twenty|thirty|forty|forty-five|sixty|ninety)\s*-?\s*(minute|minutes|hour|hours)\b/i`, applied to the bonus page AND the plan's `Bonus level` section; witnesses `10 min`, `10min`, `10 mins`, `1 minute`, `30-minute`, `an hour`, `ten minutes`, one in the plan section; negatives `2.1.154`, `claude --version`. |
| T3 | 2 | A finished line can leak as an inline span (`` `tools: Read, Grep` ``) and the rule reads fences only. | Missing criterion 2b: "an inline span on the bonus page of the form `<key>: <value>` whose key is a subagent frontmatter key has a single angle-bracket placeholder value" (witness `` `tools: Read, Grep` `` fails, `` `tools: <tools>` `` passes, a key name alone such as `` `tools` `` passes). |
| T4 | 1 | Outline is checked by reading only; a later edit can drop Part D or the cleared-when heading silently (shape rule vs count rule). | Missing criterion 1b in `bonus-page.test.mjs`: the H2 sequence equals exactly `Part A: ...`, `Part B: ...`, `Part C: ...`, `Part D: ...`, `Before you move on`, `Bonus cleared when` (exact strings pinned), the marker box is the first element, one `:::tip[Make it yours]`, and nothing follows the cleared-when list. Witnesses: Part D heading removed, two Parts swapped, a paragraph after the list. |
| T4 | 4 | "Every sentence stating Claude Code behaviour" is judged by the implementer; G-5 shows the outline already holds unquoted behaviour. | Keep, plus: "the verifier enumerates the behaviour sentences independently from the page text and diffs its list against the claims table; every row's quote is re-fetched and found with `grep -F` in the raw `.md` page; no new id without a verbatim quote and URL:line". |
| T4 | 5 | Pointer wording false for some participants (G-6). | "The page names `.claude/agents/architecture-reviewer.md` only as an example that exists on the `start/level-4-<track>` and `solution/level-3` branches (verified by `git ls-tree` per ref in the outbox), never as 'on your branch' unconditionally." |
| T4 | 6 | "Rendered check" is fine; add the two pages whose built HTML changed in T2 (Level 4 and Wrap-up, pagination) only at T2 and qa-final. | none for T4. |

### Common directive C0 (every verifier, T1 to T4; "in addition to all of C0", never a subset)

C0-1 Scratch and baselines. Every evidence file in the session scratchpad SP, named `s16-t<N>-qa-r<k>-<what>`,
cited by absolute path. At snap-before: `git status --porcelain`, repo-relative `shasum -a 256` of every fileScope
file (and every file a probe touches) into `s16-t<N>-qa-r<k>-hashes.txt`, and `cp` of each fileScope file to
`SP/s16-t<N>-qa-<File>.orig`. Restore probes by `cp` from `.orig`, then `shasum` against snap-before, quoted per
probe. Read-only git; never checkout, stash or reset.
C0-2 Node. `PATH=<SP>/node-v26.13.0-darwin-arm64/bin:/usr/bin:/bin`; quote `node --version` (v26.13.0) and
`npm --version`. If the tarball is gone, fetch node-v26.13.0-darwin-arm64 from nodejs.org/dist and verify against
SHASUMS256.txt (quote both hashes). Never `npm exec node`. Long runs in a script under
`.claude/comms/tmp/s16-t<N>-qa-*.sh`, each with `bash ~/.claude/scripts/with-budget.sh <s>`.
C0-3 Gates in `guide/`: `npm ci` (300), `npm run check` (180), `npm run typecheck` (180), `npm run build` (300),
`npm run check:built` (180), `npm run build:offline` (300), `node scripts/check-built.mjs build-offline` (60). Quote
exit codes and every `ok` line. node:test: `tests N`, `pass N`, `fail 0`, with N equal to the previous gate tip's N
plus the `test(` calls this task adds (count them from `git diff -U0` and quote both numbers; base 802). After the
gates, `git status --porcelain` shows no new tracked change (the lockfile untouched by `npm ci`).
C0-4 check:built census. Quote every ok line and compare with the previous gate tip's lines: only the changes this
task names may differ (T2: html files +1, level progress `other pages` +1, navbar pages +1, routes 11 -> 12).
C0-5 Referee from the repo root:
`env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify` with
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<SP>/jtmp`, budget 600; BUILD SUCCESS, Maven `Total time` under 60 s quoted, test
totals quoted. Over 60 s on a loaded machine: rerun once when idle and quote both, never wave it through.
C0-6 Scope and frozen files. `git diff --name-only <pr-base>...HEAD` plus `git status --porcelain`: product files
(minus `.claude/`, `agent-os/`) within the fileScope. Hash-equal to the PR base: `guide/package.json`,
`guide/package-lock.json`, `guide/docusaurus.config.ts`, `guide/docusaurus.offline.config.ts`, `guide/sidebars.ts`,
`guide/src/**`, `guide/static/**`, `.github/**`, `guide/scripts/lib/mdscan.mjs`, `guide/scripts/rules/coverage.mjs`,
`guide/scripts/rules/plan.mjs`, and the 11 existing pages under `guide/docs/` (T5 is dropped: no existing page
changes in any task). One command lists them with `git diff --stat <pr-base> -- <paths>` printing nothing.
C0-7 Pin discipline. For every edited existing test or rule file, list each `-` line of `git diff -U0` as a retarget
(old -> new, reason) or a comment change; any deleted assertion, deleted or skipped test, widened regex or
`>=` replacing `===` is a FAIL. Per-file test counts (base vs tip) quoted for every edited test file: none drops.
C0-8 `SWEEP_EXCLUDED` byte-identical: extract the block from `const SWEEP_EXCLUDED = {` to its closing `};` in
`git show <pr-base>:guide/scripts/coverage-manifest.test.mjs` and in the tip file into SP, `cmp` them, quote both
line counts (non-zero) and the `cmp` exit 0. Same for the short-snippet pin list (the block after line 632) except
where the task names an insertion.
C0-9 Hygiene. Run in the Bash tool: `git grep --no-index -n -P '[^\x00-\x7F]' -- <every changed file>` and over
`docs/spec-pack/workshop/workshop-plan.md` and `docs/spec-pack/specs/16-bonus-level-orchestration.md`: no output,
with a positive control file in SP that prints. No em dash (U+2014) and no en dash. Absolute home paths:
`git grep -n -E '/Users/|/home/|~/' -- <changed files>` classified (none allowed on the page or in the plan section).
C0-10 Probes and witnesses. Each must-fail probe quotes the failing assertion or checker line and fails for the
stated reason (never a syntax or import error). Each source mutant is applied to the real file, run, restored by
`cp`, `shasum`-checked. Declare an equivalent mutant with its reason instead of listing it as killed.
C0-11 Execute every directive item literally; "untouched by this diff" or "out of scope" needs a quoted scope proof
and is still a gap unless the item says otherwise. "Pre-existing", "flaky", "environmental" next to a PASS is a FAIL.
C0-12 Network: read-only `curl` only (no auth, no POST). Remote refs: `git ls-remote origin` restricted to `main`,
`solution/*`, `start/*`, `factory` and the tags `level-0-start` and `workshop-2026`, compared with the T1 baseline
file `s16-t1-qa-r0-remote-refs.txt` (T1 creates it): identical except `factory` when the orchestrator merged a task
PR (state it).
C0-13 Ports. Serve only the task's own build with `./node_modules/.bin/docusaurus serve --port 336<N> --no-open`
(in `guide/`, pinned PATH; never `npx` or `npm exec`); preflight `lsof -iTCP:<P> -sTCP:LISTEN`, stop by PID, confirm free. Every rendered check in a brand-new
browser context.
C0-14 Freshness. Prove the gates ran after the implementer's last edit with `stat` mtimes (impl outbox, last
product file, the gate logs), not frontmatter times.

### Per-task qaDirectives

#### T1 qaDirective (in addition to all of C0)

1. Green path on a real build: fresh `npm run build` of the PR tip, then `npm run check:built` prints exactly
   `ok   routes: 11 routes in the sitemap and the build, prev/next chain of 11 pages`. Quote the
   `<nav class="pagination-nav ...">` element of `build/level-4-spec-driven-development.html` and of
   `build/wrap-up.html` (the R13 markup the rule reads) and show the rule's selectors match it.
2. Frozen list provenance: import the frozen list from `rules/routes-built.mjs` with a one-line node script (do not
   retype it), sort it, and `diff` it against (a) the sorted `<loc>` values of `build/sitemap.xml` and (b) the
   sorted `<loc>` values of a fresh read-only `curl -sS https://maikkingma.github.io/AgenticBookStore/sitemap.xml`.
   Both diffs empty; a control diff against the list minus one loc prints one line.
3. Witnesses in `routes-built.test.mjs`, each run and quoted with its message: route file deleted (top level AND
   nested `reference/troubleshooting.html` AND `index.html`), extra loc, missing loc, one loc replaced by another at
   the same count, a duplicated loc, a trailing-slash loc, sitemap absent, swapped next href, swapped prev href,
   missing pagination nav, extra next on Troubleshooting, extra prev on Welcome, a next href with an extra path
   segment. Any missing witness is a Required Action (the last six beyond the card are this strategy's additions).
4. Source mutants on `rules/routes-built.mjs`, each must turn `node --test scripts/routes-built.test.mjs` red:
   M1 sitemap comparison as subset (frozen in build); M2 comparison by count only; M3 missing sitemap returns no
   error; M4 file-existence loop removed; M5 nested routes mapped flat (`reference-<name>.html`); M6 prev side not
   compared; M7 last-page next check removed; M8 href compared with `endsWith` or `includes`; M9 loc normalised by
   stripping a trailing slash. Quote the red test name per mutant.
5. Wiring (G-1 resolution as decided at Gate 1): CLI with no argument on a real-shaped fixture prints the ok line;
   no argument with the sitemap deleted exits 1 with the routes message; an explicit dir and `build-offline` print
   no `routes` line. If option B (predicate) was chosen: the predicate has a must-fail witness each way and a test
   that the real tree satisfies it. `navbar.test.mjs` and `level-progress.test.mjs` green; if they were edited
   (option A), C0-7 applies to every changed line.
6. No file under `guide/docs` changed (C0-6). README "Rules in place" gains exactly one bullet (quote it) that says
   a page addition must extend the list with a reason.
7. Record the remote-ref baseline for C0-12.

#### T2 qaDirective (in addition to all of C0)

1. Page bytes: frontmatter has exactly `title: "Bonus level: orchestrate your own team"` and
   `sidebar_position: 7.5`; no `slug`, `id`, `pagination_*`, `sidebar_label`, `LevelProgress`; the body is only the
   `:::info[Optional bonus level]` box. Read the box: it says optional, early or at home, no time slot, no solution
   branch, and links `./06-wrap-up.md`. No time words (T3's rewritten regex dry-run on the page prints nothing).
2. Marker rule witnesses (through the real `OTHER_PAGES` of `rules/structure.mjs`, never a test-local copy): no box;
   `:::note[Optional bonus level]` and `:::tip[...]`; a paragraph, a heading, an import line and another admonition
   before it (each fails) and an MDX comment before it (passes, if that is the stated rule); two boxes; the wrap-up
   link only after the box (link present on the page, outside the box: must fail); no link at all; a link to
   another page inside the box; `import LevelProgress` alone; `<LevelProgress current={4} />` alone; title variants
   `Optional Bonus Level` and `Optional bonus level extra`; position `8` and `7`. Mutants on `structure.mjs`: marker
   flag removed from the entry; title compared case-insensitively or by prefix; count check `>= 1`; link searched in
   the whole page; first-element check removed. Each killed, quoted.
3. Pins: run the dry-run grep from the ledger (`git grep -n -i -P 'eleven|(?<![0-9.])11(?![0-9])|on 10 skeleton'`
   over `guide/scripts` and `guide/README.md`) at the tip: every remaining hit is on the ledger's NOT-a-pin list and
   byte-identical; every pin on the ledger's pin list is retargeted (old -> new table). `skeleton-ban.test.mjs` list
   gains `docs/06-bonus-level-orchestration.mdx` in sorted position.
4. Fixture: `git diff --stat <pr-base> -- guide/scripts/fixtures/skeleton-docs/docs` shows one added file only;
   the fixture page carries the marker box and the skeleton marker and passes the content deny-list (no "spec 16",
   no "workshop-plan"). README and `skeleton-docs.mjs` comment edits limited to the added-later sentence.
5. Coverage: `PAGE_FILES.BO`; `BO-01` (kind text, source plan, section `Bonus level`) holds the marker sentence; BO
   floor 1; `anchored 345/345, 15 manual rows, 0 entries pending`. Probes: delete the marker sentence from the page
   (coverage error quoted); reword it in the plan only (anchoring error quoted); drop `BO-01` (floor test red).
6. Plan: `git diff -U0 <pr-base> -- docs/spec-pack/workshop/workshop-plan.md` has one hunk `@@ -451,0 +452,<n> @@`
   with only `+` lines; the added lines hold no backtick, no `**Level cleared when**`, no minutes;
   `rules/plan.mjs` still counts five `**Level cleared when**` (quote the plan test or a node one-liner using it);
   C0-8.
7. Routes: `git diff <T1 merge commit> -- guide/scripts/rules/routes-built.mjs guide/scripts/routes-built.test.mjs`
   quoted; the rule diff is the bonus loc, the bonus route, the chain edit Level 4 -> bonus -> Wrap-up and the
   count; every T1 witness still present and red on its mutation. Built: Level 4 next = bonus, bonus prev = Level 4,
   bonus next = Wrap-up, Wrap-up prev = bonus (quote the four hrefs from the built HTML); the built sidebar lists
   Level 4, the bonus, Wrap-up in that order (quote).
8. Rendered smoke (Chromium is enough here; qa-final runs the full matrix): bonus, Level 4 and Wrap-up pages on the
   Pages build at 1280 and 390 px, light and dark: axe 0 violations, no horizontal scroll, the info box is the
   first child of the article's markdown container; the offline build opens `#/bonus-level-orchestration`. Quote
   the built `<meta name="description">` of the bonus page and judge it as a participant would see it in a search
   result.
9. Spec 16 file: `git diff` shows only the Status line; `grep -c 'starts automatically'` prints 0.
10. README documents the marker rule (quote the bullet).

#### T3 qaDirective (in addition to all of C0)

1. Pack membership. Prove the page side and the plan side use the checker's own extraction (imports quoted; no
   copy of a scanner); C0-6 hash-equal for `lib/mdscan.mjs`, `rules/coverage.mjs`, `rules/plan.mjs`. Witnesses (each
   a page-only literal that must fail, plus the matching form that passes): a span in prose, in a heading, in a
   table cell (with `\|`), inside the marker box, inside a list item, inside a `<details>` or `TabItem`; a fenced
   line in a plain fence, a `~~~` fence, a four-backtick fence, a fence with a language and meta, a fence indented
   in a list item; a span that is a substring of a plan span (`/work` vs `/workflows`); a plan span with appended
   text (`claude --version && echo x`); a page fenced line with an appended `# note` when the plan has the bare line;
   a span present only in `## 9. Level 4` and one only in `## 10. Wrap-up` (the two neighbours: kills an
   off-by-one section bound); a plan with the bonus heading removed (must be an error, never a pass on zero lines).
   Mutants: whole-plan allow-list; `includes` instead of equality; page side comment-stripped; section ended at
   `###` instead of the next `##`. Live probe: append a page-only span to the real page (restore by `cp`): `npm run
   check` red with the message quoted.
2. Fragment rule: witnesses from the card plus: an empty value `tools:`; a quoted placeholder `"<tools>"`; two
   placeholders on one line; a key block without `---` lines; a YAML list item line; a `#` comment line; the second
   fragment in a different fence language; a fragment inside an admonition. Zero fragments passes (the stub). If
   Gate 1 accepts criterion 2b: `` `tools: Read, Grep` `` fails, `` `tools: <tools>` `` and `` `tools` `` pass.
3. Script-code ban on every page: the card's witnesses per token in a span and in a fence (8), `export const meta`
   in prose, span, fence and as a top-level MDX ESM line, `export  const  meta` (whitespace), `agent (x)` in a span
   (kills a dropped `\s*`); negatives that pass: "the agents (two of them)" and "an agent (the researcher)" in
   prose; the rule fires on a non-bonus page fixture (wrap-up) too. Mutants: rule applied to prose as well (killed
   only by the prose negatives; my dry run shows no live page hit, so the live run cannot kill it); rule limited to
   the bonus page; `\b` or `\s*` dropped. Live: run the rule on the 11 pages and the stub, 0 hits.
4. Minutes: the regex as decided at Gate 1 (G-4); the witnesses listed there, each quoted; applied to the plan
   bonus section if accepted.
5. Discovery: `bonus-page.test.mjs` runs inside `npm run check` (quote its test names in the node:test output).
   README lists each new rule (quote).

#### T4 qaDirective (in addition to all of C0)

1. Gate chain: `rules/content.mjs` hash-equal to the T3 merge; `git diff <T3 merge> -- guide/scripts/bonus-page.test.mjs`
   insertion-only (C0-7); the T3 live probe of directive T3-1 repeated on the content page (a page-only span fails).
2. Coverage and sweep: `SWEEPS[0].pages` gains exactly `['Bonus level', ['BO']]`; C0-8 for `SWEEP_EXCLUDED` and the
   short-snippet list (zero additions); BO floor equals the live BO count (probe: drop one BO entry, floor red);
   `anchored N/N, 15 manual rows, 0 entries pending`. List every page literal shorter than 4 characters (a key such
   as `s`): the sweep treats it as covered by ANY snippet in the section that contains that letter, so name the
   entry whose snippet holds it in its sentence context (`press `s`` form) or report it as unpinned.
3. `bonus-page.test.mjs`: cleared-when equality witnesses (two items swapped, one dropped, a page-only extra item,
   only the continuation line of a wrapped plan item reworded, backticks dropped from one item); `**Bonus cleared
   when**` exactly once in the plan; the anti-vacuity floor is exact (add one span to page and plan: red; remove
   one: red). If Gate 1 accepts 1b: the H2-sequence witnesses.
4. Claims (HR1): enumerate every sentence of the page that states how Claude Code behaves, independently, before
   reading the implementer's table; diff the two lists. Re-fetch every cited page as raw `.md`
   (`curl -sS https://code.claude.com/docs/en/<page>.md`), save to SP with sha256 and time, and `grep -nF` each quote:
   found, line quoted. Judge each sentence against its quote: exact, faithful, or over-claim (an over-claim is a
   FAIL). G-5 items need a quote or must be gone. Banned-claim grep over the page and the plan bonus section:
   `git grep -n -i -E 'ultracode|agent\(|Bash\(|windows|wsl|teammate|agent teams?|wizard|return|~/|/effort|depth|log\(|args'`,
   every hit classified. Every external link: `curl -sS -o /dev/null -w '%{http_code} %{url_effective}\n' <url>`
   without and with `-L`, 200 quoted; no `#fragment` on an external link unless the fragment id is found in the
   fetched HTML.
5. No solutions (HR2): at most one fenced fragment, placeholders only (T3 rule green); `git grep -n -E 'You are|You
   will|Your job'` over the page: no agent system-prompt text; no fenced block that could be saved as a whole agent
   file or a workflow script; the `architecture-reviewer` pointer per G-6 (quote `git ls-tree` for each participant
   ref); no new file under `.claude/agents/`, `.claude/workflows/` or any `*.js` outside `guide/scripts` (C0-6).
6. Rendered: bonus page at 1280 and 390 px, light and dark, Chromium, Firefox and WebKit, Pages and offline build
   (24 runs, rig copy of `s15-final-qa-sweep.mjs` with the bonus route, `diff` of the copy quoted): axe 0
   violations, theme asserted, no horizontal scroll, task-list checkboxes named, tables in named regions, console
   only the accepted lists. Rendered cross-check: the set and count of rendered `code` texts on the page equal the
   source spans and fenced lines (whitespace and backticks folded); no text from JSX attributes or ESM.
7. Beginner read: read the page top to bottom as a Java developer who never wrote an agent file. Report with line
   numbers: a term used before it is defined (subagent, frontmatter, main session, workflow), a step that cannot be
   done with what the page and Levels 0 to 4 gave, a missing stop point for Part D, any "orchestrator", "lab",
   minutes, or an instruction that writes outside the project (`~/.claude`).
8. Outbox claims table present in the `## Claims table` form SA2 needs (page sentence -> id -> URL:line), complete
   against item 4's list.

### qa-final focus (seed for the final brief)

1. HR4 recipe on the spec tip twice: Node 26.13.0 tarball AND nvm 24.20.0 (`npm ci`, `check`, `typecheck`, `build`,
   `check:built`, `build:offline`, `check-built.mjs build-offline`), lockfile hash unchanged after each `npm ci`.
2. Full sweep 12 routes x 2 modes x 2 widths x 3 engines x 2 builds = 288 runs, `0 FAIL` in all six logs, rig copy
   diff quoted, server stopped by PID.
3. Referee under 60 s on an idle machine.
4. Live sitemap re-curl: locs equal the routes rule's list minus the bonus loc (11); built sitemap equals the full
   list (12). Prev/next chain in the Pages build from the routes rule; in the offline build by a rendered click
   through Level 4 -> bonus -> Wrap-up.
5. Hashes: the 11 existing page files, `docusaurus.config.ts`, `sidebars.ts`, `src/**`, `static/**`, `package.json`,
   lockfile, offline config equal to the spec base AND to `origin/factory`; contractual anchors of `structure.mjs`
   (level anchors, eight troubleshooting anchors, `level-cleared-when`) present as ids in the built HTML.
6. Doc re-fetch: every quote of `references.md` found with `grep -F` in a fresh raw fetch; drift reported per id.
   `references.md` equals T4's outbox claims table (diff).
7. Content read of the whole page as a beginner, plus the behind-participant path: from Level 4, Next lands on the
   bonus, the first thing visible at 390 px is the box with the Wrap-up link (screenshot).
8. Remote refs unchanged (C0-12 baseline), no new branch, release `workshop-2026` assets untouched (read-only
   `gh release view workshop-2026 --json assets`), no `.github/` change.
9. Hash chain: every gated file at the tip equals its task gate's baseline; the union of baselines equals the spec
   diff's product files.
10. Pack audit (`res-4-pack-line-audit.mjs` copy) on the bonus page: 0 literals outside the plan.

