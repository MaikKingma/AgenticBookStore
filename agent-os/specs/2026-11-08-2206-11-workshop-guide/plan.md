---
spec: 11-workshop-guide
source: docs/spec-pack/specs/11-workshop-guide-site.md (factory 52c4f6b, includes D7)
status: done
accepted-by: orchestrator (autonomous)
created: 2026-11-08T21:07:46Z
gate1: 2026-11-08T21:07:46Z
gate2: 2026-11-10T14:09:31Z (first acceptance 2026-11-09T22:49:14Z; re-run after T13)
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
| T2 | `guide.yml`: PR build plus offline zip artifact, gated deploy and release, SHA pins, timeouts | root | [T1] | B2 | critical | opus | sonnet | PASS r1 | #73 | sdd/11-workshop-guide/T2 | ENDORSE |
| T3 | OpenValue brand in `custom.css` only, Montserrat with provenance and OFL, contrast check, no colour values elsewhere | guide | [T1, T2] | B3 | elevated | sonnet | sonnet | PASS r2 | #76 | sdd/11-workshop-guide/T3 | ENDORSE r2 |
| T4 | LevelProgress, 11 page skeletons, sidebar, fixed anchors, structure checks reading the workshop plan at check time | guide | [T3] | B4 | critical | opus | sonnet | PASS r2 | #79 | sdd/11-workshop-guide/T4 | ENDORSE r2 |
| T4a | accessible names for task-list checkboxes (local rehype plugin) and a `check:built` guard | guide | [T4] | B4a | elevated | sonnet | sonnet | PASS r1 | #83 | sdd/11-workshop-guide/T4a | ENDORSE |
| T5 | pack-anchored coverage manifest and content rules | guide | [T4a] | B5 | critical | opus | sonnet | FAIL r4, human override (A7) | #87 | sdd/11-workshop-guide/T5 | AMEND r4, override: human |
| T5b | rule (c) on the built pages: `check:built` checks every pre or code line inside a Windows PowerShell tab panel against the pack command list | guide | [T5] | B5b | critical | opus | opus | PASS r3 | #100 | sdd/11-workshop-guide/T5b | ENDORSE r3 |
| T5c | frozen self-tests read a skeleton snapshot instead of the live docs tree (A10) | guide | [T5b] | B5c | critical | opus | opus | PASS r1 | #110 | sdd/11-workshop-guide/T5c | ENDORSE |
| T6 | index, setup check, wrap-up pages | guide | [T5c] | B6 | elevated | sonnet | sonnet | PASS r1 | #112 | sdd/11-workshop-guide/T6 | ENDORSE |
| T7 | Level 0 and Level 1 pages | guide | [T5] | B7 | elevated | sonnet | sonnet | PASS r1 | #114 | sdd/11-workshop-guide/T7 | ENDORSE |
| T8 | Level 2 page | guide | [T5] | B8 | elevated | sonnet | sonnet | PASS r1 | #115 | sdd/11-workshop-guide/T8 | ENDORSE |
| T9 | Level 3 page | guide | [T5] | B9 | elevated | sonnet | sonnet | PASS r1 | #118 | sdd/11-workshop-guide/T9 | ENDORSE |
| T10 | Level 4 page | guide | [T5] | B10 | elevated | sonnet | sonnet | PASS r1 | #119 | sdd/11-workshop-guide/T10 | ENDORSE |
| T11 | reference pages, marker ban, site-wide pass in both builds and three engines | guide | [T6, T7, T8, T9, T10] | B11 | elevated | sonnet | sonnet | PASS r1, red CI check reworked, PASS r2 | #124 | sdd/11-workshop-guide/T11 | ENDORSE r2 |
| T12 | D9: Git Bash prerequisite on the setup and Level 3 pages, the commit hook's onFailure behaviour, cheat sheet print fit (A14) | guide | [T11] | B12 | elevated | sonnet | sonnet | PASS r1, REJECT r1 reworked, PASS r2 | #129 | sdd/11-workshop-guide/T12 | ENDORSE r2 |
| T13 | D16 (F2): no PowerShell hook text on the Level 3 and troubleshooting pages; Git Bash wording on wrap-up; JDK 21 floor on the setup page (A15) | guide | [T12] | B13 | elevated | sonnet | sonnet | PASS r1 | #156 | sdd/11-workshop-guide/T13 | ENDORSE |

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

From the T2 meta-review (qa-lead ENDORSE r1):

- FF-9 (C0-1, T3 and every later rendered check): rendered tooling lives in the scratchpad folder
  `s11-guide-verify/` with its own Playwright and axe-core install, versions quoted; `res-1-pw` is not acceptable
  from T3 on.
- FF-10 (T3 verifier, first guide PR gated by guide.yml): bind CI evidence as T2 did (`headRefOid`,
  `gh run list --commit <full sha>`, run JSON saved unfiltered); the `check` step shows the contrast pair lines and
  the FF-1 guard; build green, deploy and release skipped; `git diff --name-only <pr-base>...HEAD -- .github` is empty
  (guide.yml stays blob b2848628).
- FF-11 (C0-3/C0-4, every task changing `guide/**`): local gates stay mandatory; a CI run is extra evidence, not a
  substitute.
- FF-12 (final brief): `docs/spec-pack/**`-only changes on factory do not trigger guide.yml; qa-final runs
  `npm run check` against the merged pack itself; the path filter is a Gate 2 residual for the owner.
- Gate 2 owner notes: O1 the `workshop-2026` tag stays on the first deploy commit, later pushes replace the zip;
  O2 GitHub compares the gate value case-insensitively (`TRUE` opens it too).

From the T3 meta-review (qa-lead ENDORSE r2, after one rework round for unpinned contrast pairs and rule wiring):

- FF-13 (C0-6, verifiers of T4, T5, T11): T3's part of the checker does not regress. (a) `rules/contrast.mjs`,
  `rules/brand.mjs`, `rules/fonts.mjs`, `rules/colors.mjs`, `rules/zero-tests.mjs`, `lib/site.mjs` keep the T3
  sha256 baseline in the QA ledger unless the task card names a change (quote `shasum -a 256`); (b)
  `rules/contrast-pairs.mjs` and `contrast-pairs.test.mjs` only gain lines and `MINIMUM_PAIR_COUNT` is never lowered;
  (c) an edited T1 or T3 test file only gains lines unless a criterion justifies a deletion; (d) a change to
  `lib/text.mjs` `withoutBlockComments` re-runs Z-1, Z-2 and the W-d witness; (e) a change to `check.mjs` or
  `lib/site.mjs` re-runs wiring mutants W-1 to W-13. Implementation directive for T4, T5, T11: every new rule gets at
  least one test that runs it through `runChecks`.
- FF-14 (C0-6, every verifier and qa-final): the guard line `ok tests (N test files, M test cases)` is a lower bound;
  C0-6 and FF-2 use only node:test's `tests N`. A drop in M from `/*` inside test strings is not a finding; a guard
  failure on a file with real test cases is.
- C0-12 freeze: the checker freezes at T5's gated baseline (copied into the ledger at the checkpoint after T5), not
  at T3.

From the T4 meta-review (qa-lead ENDORSE r2, after one rework round for box fences, H1 forms, unpinned anchors and
untested rules):

- FF-15 (T5 criterion, A5): insertion-only tests in `structure.test.mjs` for a fenced block under the Start branch
  line and a setext heading with `=` and `==`; the T5 verifier shows mutants M-07g4 and M-15k killed.
- FF-16 (T5 implementer and verifier): rule (d) does not flag the contractual anchor `feature-briefs`, with an
  exemption no wider than that. Pass: the L4 heading `{/* #feature-briefs */}`, the link
  `./05-level-4-spec-driven-development.mdx#feature-briefs`, `#feature-briefs` on L4. Fail: `level-4-feature-briefs.md`,
  `feature-briefs.md`, prose `feature-briefs` without `#`, `#workshop-plan`, `#spec-pack`. Mutants: an exemption for
  any `#`-prefixed term, an exemption matching `feature-briefs` anywhere. The built-text deny-list grep (C0-8a) reads
  text content only, or counts the `id` and `href` hits exactly.
- FF-17 (T4a and T5 verifiers): `rules/structure.mjs`, `rules/plan.mjs`, `lib/structure.mjs`,
  `lib/structure-fixtures.mjs` keep the T4 r2 hashes unless the card names a change; the three T4 test files only
  gain lines; a `check.mjs` change re-runs W-1..W-13 and SW-1..SW-11; the test-count floor is 258.
- FF-18 (T6 to T11 verifiers, qa-final): every page has exactly one rendered `h1` in both builds, equal to the
  frontmatter title.
- FF-19 (content implementers): anchors as `## Heading {/* #id */}`; any line starting with `#` outside a fenced block
  fails the checker, so rephrase; after the freeze a checker false positive is a question, never a checker edit.
- FF-20 (T11 and qa-final; T6 to T10 under the same expectation): the full axe ruleset reports 0 violations once T4a
  has landed.
- FF-21 (T4a and T5 implementers): never edit a file outside the fileScope, not even temporarily, including
  `docs/spec-pack/**`; pack edits and mutation probes of pack files are the verifier's job.
- Gate 2 owner note: O2, the offline (hash router) build renders nothing at build time, so the Pages build that runs
  first is the guard for broken anchors and bad `current` values.

From the T4a meta-review (qa-lead ENDORSE r1):

- FF-22 (T5 criterion, A6): one insertion-only test in `task-list-labels.test.mjs`: a task item " Parent" with a
  nested `ol` holding a task item " Child" yields the labels `['Parent', 'Child']`; the T5 verifier shows mutant P14
  (`ol` removed from `isList`) killed.
- FF-17 extension (T5 verifier): `check-built.mjs`, `lib/html.mjs`, `rules/built.mjs`, `built-checkbox.test.mjs`,
  `task-list-labels.test.mjs` keep their T4a r1 hashes (QA ledger) unless T5's card names a change; the two new test
  files only gain lines; `built.test.mjs` unchanged; test-count floor 289 with the T4a titles present; touching the
  built rule re-runs the T4a strip probe and mutants R01-R24, L01-L03. The C0-12 freeze baseline at T5 includes all
  T4a script files.
- FF-23 (T5 to T11 verifiers, qa-final): quote the `check:built` tail on the Pages build; with the current pages it
  reads `ok   built: 12 html files, no literal ":::", 14 checkboxes, all named` (the counts follow the pages; from
  T6 on `guide/src/plugins/rehype-task-list-labels.mjs` and `guide/docusaurus.config.ts` keep their T4a r1 hashes
  unless T11's card names a config change).
- FF-24 (T6 to T11 implementers and verifiers, qa-final): task-list items are plain Markdown text, inline code,
  emphasis and links only, never a JSX or HTML element; verifiers check on every page in both builds that the
  checkbox count equals the task items in the source and every `aria-label` equals its item's visible text
  (whitespace collapsed); a mismatch is a FAIL of the task that added the item, fixed by rewording, not by editing
  the plugin.

From the T5 meta-reviews (AMEND r3, checkpoint manifest review, AMEND r4) and the human decision A7:

- FF-25 (T6 to T11 verifiers, qa-final): rendered cross-check of rules (b) and (c) on the Pages build. N(x) = x with
  every whitespace character, every backtick and every `|` deleted. (a) For each non-skeleton page of the task, every
  manifest entry of that page (ENTRIES imported read-only from the frozen `guide/scripts/coverage.mjs`; `pageSnippet`
  when present): N(snippet) is a substring of N(the article's textContent, hidden tab panels included); for `kind:
  command`, of N(the textContent of one single `pre`). (b) Every line of every `pre` inside a tab panel whose tab label
  contains "PowerShell" (the n-th `[role=tabpanel]` of a tabs container belongs to its n-th `[role=tab]`; a line is the
  textContent of one `.token-line`, or of one newline-split line of a raw `pre`; trimmed, non-empty) is in
  `packCommandLines` of the five pack files (imported read-only from the frozen `rules/content.mjs`). Quote: entries
  checked = the page's entry count, missing 0; Windows lines checked M, outside the allow-list 0. An entry the checker
  counts but the build lacks, or a Windows line outside the allow-list, is a FAIL of the task. Mandatory by hand until
  T5b merges (A7); afterwards (b) is also enforced by `check:built`.
- FF-26 (T6 implementer and verifier): the built `<meta name="description">` of every page holds no `{/` or `*/`; a
  page whose first paragraph would hold a comment gets a `description` frontmatter.
- FF-27 (T6 to T11 implDirective, checker-safe authoring after the freeze): `Tabs`/`TabItem` tags alone on their
  lines; a literal tag only in inline code (never `\<` or `{'<...>'}`); no lone backtick in prose (write "the backtick
  key"); keep every manifest snippet contiguous in the source (FF-28); a checker false positive is a `question`, never
  a checker edit (FF-19).
- FF-28 (T6 to T11 implDirective, manifest-forced wording; the manifest is frozen, so fix the page, never the
  manifest). General: run `npm run check` before removing the marker and copy the `pending entries <page>` ids into the
  outbox; each entry is met by its exact snippet (`pageSnippet` when present) in the page's Markdown source:
  case-sensitive, whitespace free, but Markdown inside the snippet breaks it (a link around part of it, emphasis,
  backticks added or dropped, `<kbd>`, a table cell boundary). `kind: command` entries sit in a fenced block (a tab is
  fine). Frontmatter, the at a glance box, comments and the "Level cleared when" checklist never count: repeat in the
  body what the body needs. Per task:
  - T6: agenda as a pipe table, Time directly before Block, `| 1:55 | Break | 15 | |` with Break unformatted; block
    names verbatim (IX-01, IX-02, IX-04, IX-06 to IX-10), "240 minutes". Setup: "Use WSL and install as on Linux
    there." and "Native Windows is not verified yet." in the Windows (WSL) jdtls tab; "Pro/Max/Team/Enterprise";
    "Level 2 Part A" (a link wraps all four words or none); "JDT LS release"; "Node.js is not needed";
    `Get-Command jdtls` in a fenced block (the Windows PowerShell tab is fine); no "under a minute". Wrap-up:
    "Introduction to Model Context Protocol" (the pack's "Introduction to MCP" alone fails) and the other four titles in
    their case; "context, behaviour, structure, verification"; "the guide stays online"; "revoked tonight"; install
    lines in fenced blocks; Windows only as stated: `irm https://openspec.dev/cli/install.ps1 | iex` may sit in a
    Windows PowerShell tab, Spec Kit's Windows form only as the text `--script ps` (a full `specify init ... --script
    ps` line in a PowerShell tab fails rule c), no PowerShell tab for uv or Agent OS.
  - T7: L0 task blockquote verbatim; "`Esc` twice" with backticks (no `<kbd>`); the check-question phrases (L0-09 to
    L0-15) verbatim; `frontend/` in the body. L1: "never double" (no backticks on double); "Read docs/architecture.md
    before adding or moving classes." (no backticks on the path in that sentence); "schema changes only via a" (then
    free); "without showing output"; the three `paths: [...]` lines exactly, in inline code or a fenced block;
    `/compact focus on <topic>` in inline code; "use a subagent to find every place that formats money"; "1M-token
    window"; "Level 0 task in a fresh session"; "the rule is loaded" (spec 10 wording).
  - T8: the five prompts verbatim with their backticks (also inside a code block); "Windows: inside WSL";
    `Executable not found in $PATH: "jdtls"` and `mcp__distributor__*` in inline code; the real `CatalogMcpTools.java`
    path; "connected with 4 tools"; "two minutes" with "watch the demo" (no "facilitator").
  - T9: the permissions JSON as its own fenced block equal to WP:239-244 (merged with the hooks into one object it
    fails L3-02); the hooks fragment as WP:252-258; spec 10 lines 273-295 in one fenced block, unmodified, as the
    compare-with reference; `git commit -m "test"` and `git commit --no-verify -m "x"` in fenced blocks framed as what to
    ask Claude to run (in your own terminal `--no-verify` skips the git hook and no deny rule applies); "Any other
    non-zero exit code would NOT block" (case as written); "Frontmatter: `name`" and "a precise `description`";
    `` !`git status --short` `` as a double-backtick span; the Stop hook script not printed (M-2 decision), described
    as optional with a link.
  - T10: the common start as one body block copied from SDD:95-97 with its comments (`claude # new session`, literal
    `start/level-4-<track>`), outside the box and the track tabs; the three track branches in the body (track table);
    every track command in a fenced block in its tab, Track A `/clear` and Track C `/plan` included;
    `/speckit-checklist` and `/speckit-analyze` as text; the OpenSpec path with its `.../spec.md`; "Use the existing
    onion layers, see docs/architecture.md and the constitution." without backticks; briefs verbatim with their
    backticks (bold route labels may stay); the FB:41 hint verbatim and nothing else of FB:40-45; "built from
    `solution/level-3`".
  - T11: cheat sheet tokens contiguous ("`Esc` twice", "openspec validate --strict", `/compact focus on <topic>` in
    inline code); troubleshooting "Git for Windows (Git Bash) or WSL2", "Agent OS needs bash (WSL or Git Bash)",
    "switch to that key"; the STDIO fallback commands with `<absolute path>` in fenced blocks; the bare
    `ANTHROPIC_API_KEY` only in prose.
- FF-29 (T6 to T11 verifiers, manifest items): (a) quote `ok coverage (anchored 304/304, 15 manual rows, N entries
  pending)` with N = 251, 195, 167, 133, 68, 0 after T6, T7, T8, T9, T10, T11, and no pending line for the task's
  pages. (b) For each pinned short snippet on the task's pages, the page line where it sits in its pack step. (c) Tick
  the task's manual rows with page lines: T6 WP:56, WP:329, S11:34; T7 WP:117-119, 162-166, 111-113, 140-147; T8
  WP:218-221, 191-194; T9 WP:287-290, the Stop hook row (labelled S10:273-295, item S10:297-314: absent per M-2) and the
  prose checkpoints row (labelled S10:402-404, item S10:410-417); T10 WP:317-321; T11 S11:38 and AR. (d) Semantic items
  the manifest leaves out: T7 the Level 0 start premise "No `CLAUDE.md`, no `.claude/`, no MCP" (WP:89) and the money
  question with "(`double` is a finding.)" (WP:108), present or omitted with a stated reason; T9 the stretch subagent's
  "Bash only for `git diff` and `git status`" (WP:285); T10 the WP:303-307 table with its Weight column, and FB:42-45
  absent ("great debrief story", "verified buyer", "scenario-to-test mapping"). (e) C0-13 facilitator-range greps use
  facilitator-only phrases, never a manifest snippet: WP:334-347 e.g. "spare Max account", "Console spend limit", "MCP
  spec 2026-07-28"; FB:40-45 as in (d). (f) T11 AR counted trace: 8 package rows (AS:32-41), the nine dependency rules
  (AS:45-55, rule 9 the process exception), 7 suffix rows (AS:65-73), the onion text block (AS:19-30),
  `ArchitectureTest` as the referee (AS:158): each present or omitted with a reason; no "exactly these packages" or
  enforced-package-list claim.
- FF-30 (T6 to T11 implDirective, extends FF-27; the A7 authoring rules): (a) an inline code span never runs over a
  line break (any page); (b) never start a non-table line with `|`, and a table always has a delimiter row; (c) in a
  Windows PowerShell tab use a fence or a plain `<pre>`, never `<pre><code>` or an expression inside `<pre>`; (d) no
  lone backtick in prose.
- Merge order (A7): no content task (T6 to T11) PR merges before T5b has merged.

From the T5b reviews (verifier FAIL r1 and r2, PASS r3; QA lead ENDORSE r3) and the orchestrator's rework rulings
(A9 below):

- FF-31 (T6 to T11 implDirective; extends FF-27 and FF-30; bars residuals R-1 to R-5): (a) every `TabItem` has a
  plain string label `label="..."` made only of ASCII letters, digits, spaces and `/ ( ) . + -`; never `label={...}`,
  a character reference (`&...;`), a backslash escape or markup inside a label; a tab that holds Windows commands has
  the literal word `PowerShell` in its label. (b) A Windows command (PowerShell or cmd syntax) outside a PowerShell
  tab goes in a fence whose language word is `powershell`; never `batch`, `posh`, `dos` or another near name. (c) Fence
  metadata goes after a space: ```` ```powershell {1} ```` or ```` ```powershell title="x" ````, never
  `powershell{1}`. (d) No hand-written `class` or `className` on `pre`, `code` or `span`; a command in prose is a
  backtick span, never a `<code>` element. (e) A checker false positive is a `question`, never a checker edit.
- FF-32 (T6 to T11 verifiers and qa-final): (a) from the T5c merge: `shasum -a 256 -c
  agent-os/specs/2026-11-08-2206-11-workshop-guide/t5c-freeze-baseline-hashes.txt` prints 89 OK and
  `git ls-files guide/scripts | wc -l` prints 89; this file replaces `t5b-freeze-baseline-hashes.txt` (75 files, valid
  from the T5b merge to the T5c merge) as the C0-12 baseline (T11: its diff against this file is limited per FF-34);
  any FAILED entry or extra file is a FAIL. FF-2 floor: node:test total at least 496 and every T5c title present
  (`comm -23` against the T5c title list empty). T6 to T10 are verified only on a tree that contains the T5c merge. (b) Quote both `check:built` lines (`ok   built: N html files, no literal ":::", C
  checkboxes, all named` and `ok   built: M Windows PowerShell lines in tab panels and PowerShell code blocks, all
  pack command lines`); M equals the verifier's own FF-25 (b) count plus the lines of `powershell` blocks outside
  PowerShell tabs; M > 0 from T6 on and never drops below the previous task's M without a stated removal. (c) The
  summed `git grep -c '<TabItem' -- guide/docs` equals the summed `git grep -cE '<TabItem[^>]*label="[A-Za-z0-9 ./()+-]+"' -- guide/docs`,
  and `git grep -nE 'label=\{|label="[^"]*[&\\]' -- guide/docs` prints nothing. (d)
  `git grep -nE '<(pre|code|span)[^>]*class(Name)?[[:space:]]*=' -- guide/docs` prints nothing. (e)
  `git grep -nE '^[[:space:]]*(```+|~~~+)[A-Za-z0-9_-]+[^[:space:]A-Za-z0-9_-]|^[[:space:]]*(```+|~~~+)(batch|posh|dos)([^A-Za-z0-9]|$)' -- guide/docs`
  prints nothing. Run each grep against a control that must match on a scratch copy, never on the task tree; keep
  the patterns in single quotes. (f) Read the task's pages: every Windows command sits in a PowerShell tab or a
  `powershell` fence.
- FF-34 (T11 card amendment, from the T5c meta-review): the marker ban (a) reads only `<root>/docs/**` through the
  same predicate as the skeleton exemption (`SKELETON_MARKER`, `rules/structure.mjs`), never a file-tree or repo grep
  (`guide/scripts/**` holds 17 marker mentions by design); (b) `guide/scripts/fixtures/skeleton-docs/**` keeps its 11
  markers and stays hash-equal to the T5c baseline (editing it is a FAIL); (c) the ban is a `runChecks` option that the
  CLI turns on for its default root (the T4 `structure` opt-in pattern), so every `runChecks`-level snapshot test keeps
  its asserting lines verbatim; the only asserting lines T11 may change are in the two CLI copy tests
  (`check-content.test.mjs` about 267-279, `structure-check.test.mjs` about 152-162), which change to the ban's exact
  outcome from all three cwds (exit 1, ban errors naming the sorted list of the 11 `docs/...` files, no other error);
  plus one new test that the finished live guide passes every rule from the three cwds (exit 0, the ok lines, 0
  pending); (d) the two live variants stay verbatim and green (a design that needs them edited is a question first);
  (e) no escape hatch flag, the `check` script pin stays; (f) the verifier shows a marker reinserted on one live page
  makes `npm run check` exit 1 naming that page, the ban and the exemption treat `{/*guide-skeleton*/}` alike, the real
  guide exits 0 with the 11 fixture markers present, and the diff against the T5c baseline is limited to the ban's rule
  and wiring files, the two copy tests and new tests.
- FF-35 (from the T6 meta-review; refines C0-13 for T7 to T11 verifiers, qa-final and the implementers' source
  tables): (a) before marking a clause embellished or invented, search the whole pack (`docs/spec-pack/**`: the three
  workshop files, spec 11, the other specs, the product docs) and the res-3 inventory, and cite every source found; an
  instruction that follows from a cited pack mechanism is faithful (T6: the app reads `BOOKSTORE_AI_API_KEY` at
  startup, S08:29); a claim about Claude Code behaviour is never derived this way (C14). (b) A participant-directed
  fact on a facilitator-range line may appear when a participant line or spec 11 states the same fact (cite both); a
  fact found only in a facilitator range is a finding; the facilitator-only phrase greps stay (M-4).
- FF-36 (T11 implementer and verifier; T7 to T10 verifiers for their own cross-page links): the T6 pages use the
  skeleton troubleshooting headings as link texts ("Docker is not running", "jdtls not starting", "Usage limits",
  "Offline copy shows a blank page", "the `ANTHROPIC_API_KEY` pitfall") and link `#part-a-lsp` and the five
  `#level-cleared-when`. T11 keeps the eight troubleshooting heading texts as in the skeleton (a reword is a question
  first); T11 and qa-final verifiers check on the rendered Pages build that every article link into
  `reference/troubleshooting` has text equal to its target heading (case-insensitive, backticks and the hash-link
  character stripped), listed per link; T7 to T10 keep the bare `## Level cleared when` heading exactly as the skeleton
  and the checker pin it (Docusaurus derives the id `level-cleared-when`; corrected 2026-11-09 during T7, the skeleton
  never carried a comment anchor there) and the L2 page keeps `{/* #part-a-lsp */}`. T7 to T10 verify on a tree that contains the T5c merge; node:test floor 496.
  Process: when the orchestrator merges the spec tip into a task branch before spawning its verifier, the verifier
  is told that the implementation outbox predates that merge.
From the T7 meta-review (qa-lead ENDORSE r1; T7 page hashes in `t7-hashes.txt`):

- FF-37 (all later verifiers, ruling on FF-5): on offline `file://`, inside the axe phase only, axe-core's CSSOM
  preload produces per axe run exactly one Chromium triple (`Access to XMLHttpRequest at 'file:...' ... blocked by
  CORS policy`, `Failed to load resource: net::ERR_FAILED`, `Couldn't load preload assets:`), and in WebKit one
  `Couldn't load preload assets:` warning plus one FF-5 pair. These are not a finding when (a) console messages are
  tagged by phase and the load, DOM, click, hover and navigation phases show nothing beyond the FF-5 WebKit pair;
  (b) counts are quoted per text and per axe run and equal one per text per run; (c) none of these texts appears in
  Firefox, on the Pages build (axe phase included) or in dev mode; (d) full-axe evidence includes the Pages build in
  all three engines. Any other text, a higher count, or one of these texts outside the axe phase is a finding.
- FF-38 (T8 to T11 verifiers, qa-final): the frozen checker checks Checkpoint shape, not count. Each level page gets
  a rendered assertion: the number of tip admonitions titled "Checkpoint", each with exactly one `pre` and one line
  starting `Expected`, equals the page's count: L0 0, L1 1, L2 2 (diagnostics, `/mcp`), L3 3 (red commit exit 2,
  `--no-verify` denied, `.env` read denied), L4 as the T10 source table justifies; backed by one must-fail mutant (a
  Checkpoint retitled) that the checker passes and the rendered assertion fails. T11 and qa-final assert the full
  per-page list.
- FF-39 (T8 to T10 verifiers): a solution-text scan: no trimmed line over 20 characters from the level's spec 10
  section appears in the built page unless the same line is in the level's own source. T8: S10:123-216 against WP
  section 7 (the plugin-declaration lines S10:140-163 must be absent, F4). T9: S10:217-420 against WP section 8
  (S10:273-295 exempt as the compare-with quote; S10:297-314 must be absent, M-2). T10: S10:421-484 against WP
  section 9, SDD:93-137 and FB:6-41. Every scan quotes the checked count, the hits and a control that hits; also
  `solution/level-4` is 0 in the source and built text of every level page.
- FF-40 (T8 to T10): C0-6 for content tasks: "`guide/scripts` tree id equals 0a958db4" replaces the "N equals the
  `test(`/`it(` grep count" clause (FF-2 at least 496 with `comm` empty both ways and the throwaway red probe stay).
  FF-36 target check: on the Pages build `#level-cleared-when` is a single `h2` with text "Level cleared when"
  followed directly by the task list; T8 also checks that `#part-a-lsp` is on the "Part A: LSP" `h2` and that the
  setup page link lands there.
From the T8 meta-review (qa-lead ENDORSE r1; T8 page hash in `t8-hashes.txt`):

- FF-41 (T9, T10, T11 verifiers and qa-final, built meta descriptions): (a) quote the built
  `<meta name="description">` of each page in the task; on a level page it reads `Goal: ` plus the pinned Goal
  sentence with no words dropped, on other pages the page's first sentence; it never contains `{/` or `*/`; T11 and
  qa-final list all eleven pages. (b) The L2 fix per A11: the only change to the L2 page is one added frontmatter
  line; the T11 verifier shows `git diff` on that file is exactly that line, the file with the line removed hashes to
  the `t8-hashes.txt` value, and the built L2 description equals the Goal sentence. Author rule (T9 to T11): add a
  `description:` only where (a) fails; ASCII, quoted, the source sentence verbatim.
- FF-42 (T9, T10 verifiers, whole-line commands): every fenced command line equals a pack command line after trim, or
  is listed with its source as a forced form; a line that only contains a pack command is a finding; backed by one
  must-fail mutant (a token appended to a fenced command) that the checker passes and this check fails.
- FF-43 (T9, T10 verifiers; qa-final on the merge result, names the page relies on): check every repo path or class
  name the page names, prompts included, with `git ls-files` or `git grep` on HEAD; if absent there, check
  `origin/factory` and say which spec creates it; a name absent from both must be a spec 10 solution-branch artifact
  the page presents as one (for T9: `.githooks/`, `tests-before-commit.sh`); otherwise it is a finding. qa-final
  checks `DistributorAppServiceImpl` and `contracts/distributor/README.md:16` on the spec PR merge result.
- Process note: only the FF-38 retitle mutant needs a build; other page mutants need only the frozen
  `node scripts/check.mjs`.
- Gate 2 owner note from T7: Level 1 step 2 says "Try both" (import and path) before the checkpoint expects
  `docs/architecture.md` not to be loaded; a participant who kept the `@` import sees it loaded. The page follows the
  pack.
- Gate 2 owner note from T6: setup page line 90, "The plugin does not ship the binary" (WP:59 wording), comes before
  the page first names the `jdtls-lsp` plugin (line 119); no later task's file scope covers this page. The T6 page
  hashes are in `t6-hashes.txt`.
- FF-33 (T6 to T11 verifiers and qa-final; orchestrator, for the T5 r4 case OA1b-f, a raw `<img>` hidden from source
  rule (f) behind an unclosed comment opener): the built article HTML of every page of the task holds no `<img`
  element (no images are expected, ground rule G4); quote the count (0).
- Gate 2 residuals from T5b: F-T5b-1 (test-only: an exhaustive Default_Ignorable loop in the label test and pins for
  the C05 and C06 shapes; the code already catches all of them; after the cycle or with any future change to
  `rules/built-powershell.mjs`); R-1 visible spaces in a label, R-2 look-alike letters in a label, R-3 hidden markup
  in a JSX label, R-4 a language class on a `span` inside `pre`, R-5 `batch`/`posh` fences and an inline
  `<code className="language-powershell">` outside tabs (all barred for authors by FF-31, checked by FF-32). The offline
  build has no page HTML to check (code text lives in JS chunks); the Pages build in CI is the guard.

- FF-44 (T11 implementer and verifier, amendment A12): the only change to `guide/docs/04-level-3-behavioral-control.mdx`
  is one line added after line 130 (the `tests-before-commit.ps1` sentence):
  `[Windows hooks](./reference/troubleshooting.md#windows-hooks)`. The verifier shows that `git diff` on the file is
  exactly that line, and that the file with the line removed hashes to `t9-hashes.txt`. In both builds a click on the
  link lands on the `h2` "Windows hooks" (check the click target, not only the href). The troubleshooting Windows hooks
  section matches L3:130 (the `.ps1` name, "not verified yet" until F2), states the WP:344 advice (Git Bash or WSL2,
  `.ps1` variants on `solution/level-3`) and invents no hook-command text.
- FF-45 (T10, T11 verifiers): a fenced prompt that is not pack text counts as a forced form only if it restates the
  scenario's WHEN clause and adds nothing; its Expected line is the THEN clause word for word. List it with its scenario
  lines and kill a reworded copy with a must-fail mutant. A precondition no page provides is stated as a precondition
  and becomes a Gate 2 owner note; never invent a setup step for it.
- FF-46 (qa-final): re-run the content verifiers' scratch-layer scripts on the merge result (T7 g14, T8 ff39, T9
  `qa-t9-s11-sem.mjs` and `qa-t9-s11-ff39.mjs`, plus T10's); before re-running, update every assertion that pins a
  decision rather than pack text (T9 SEM-06 "no windows-hooks link" is superseded by A12) and quote the change. The
  cheat sheet's names for Level 3 files match the Level 3 page.
- Process note from T9 (orchestrator): before approving a departure from a link the task card names, check the
  skeleton; all 18 contractual anchors have existed since T4.
- Gate 2 owner notes from T9: (R3) the `.env` checkpoint needs a `.env` file in the repository root and the pack never
  says who creates one (`.env*` is git-ignored); (QA lead, untested reading) the deny rule is `Read(./.env)`, so a
  Claude that falls back to Bash `cat .env` may hit a permission prompt rather than a deny; confirm in the spec 10
  rehearsal; (R4) L3 line 55 "Two deny rules guard the escape hatches" also calls the `.env` rule an escape hatch,
  which the pack uses only for the `--no-verify` path (WP:266-268); loose wording, not a false claim. The T9 page hash
  is in `t9-hashes.txt`.

- FF-47 (T11 verifier and qa-final; replaces the FF-32 (c) sum over `guide/docs`; orchestrator ruling from T10): the
  three `track` group labels are exempt from the FF-31 (a) label alphabet as exactly these strings, not as "colon
  allowed": `Track A: OpenSpec`, `Track B: Spec Kit`, `Track C: Agent OS` (page grammar strategy.md:198, frozen
  `TAB_GROUPS.track`, rule (g) exact equality). The total `<TabItem` count equals the plain-alphabet count plus
  `git grep -cE '<TabItem[^>]*label="(Track A: OpenSpec|Track B: Spec Kit|Track C: Agent OS)"' -- guide/docs`, which
  is exactly 3, all on the L4 page; the `label=\{|label="[^"]*[&\\]` grep prints nothing. Authoring rule: no label
  outside the `track` group contains a colon.
- FF-38 full list (T11, qa-final): Checkpoint counts L0 0, L1 1, L2 2, L3 3, L4 0 (the T10 justification, ruled valid
  by qa-t10 and the QA lead), with one must-fail mutant that adds or retitles a checkpoint.
- FF-46 extension (qa-final): also re-run T10's `qa-t10-s11-sem.mjs`, `qa-t10-s11-mutants.mjs` and
  `qa-t10-s11-ff39.mjs`; base sha 82227c4 and port 3310 are literals in them: re-point them to the spec PR base and
  port 3399 and quote the edit. Re-check the assertions that pin decisions or page conventions rather than pack text:
  SEM-18 (0 checkpoints), SEM-02 (the track-label ruling), SEM-06 (the one forced form), SEM-22 ("5 minutes"),
  SEM-11 (the "built from" count). `t10-hashes.txt` is 1 OK at T11 and on the merge result.
- FF-48 (T11 implementer and verifier): Level 4 command names on the cheat sheet match the L4 page (`/speckit-specify`
  with a dash, `openspec validate --strict`, the `/opsx:` forms, `/inject-standards testing`). No reference page has
  an OpenSpec install line, except the SDD:146-151 "after the workshop" table rows quoted word for word (F8).
- Gate 2 owner notes from T10: the pack does not say where the OpenSpec CLI comes from (F8; the page states no install
  step); `docs/architecture.md`, `.mcp.json`, `/ship` and the `start/level-4-*` branches with their framework files
  exist only after specs 09 and 10 (the page names them as the pack does); "use your own change name" for briefs B
  and C is card text, not pack text; the sync moment sits after the briefs (WP section 9 order); Level 4 has 0
  checkpoints; optional wording: Track B step 2 says "Answer up to five questions" before the `/speckit-clarify`
  fence, while SDD:119 gives the command first. The T10 page hash is in `t10-hashes.txt`.

From the T11 meta-review (qa-lead ENDORSE r1; hashes in `t11-hashes.txt`, checker freeze in
`t11-freeze-baseline-hashes.txt`):

- O-1 ruling (QA lead): the two "pair up" sentences on the troubleshooting page stay; the WP:334-347 risk table is the
  page's designated source (S11:38, res-3 section 3 and 2.11), not a facilitator-only range for this page.
- FF-49 (T12 verifier and qa-final; freeze; values as updated by the T11 r2 meta-review):
  `shasum -a 256 -c t11-freeze-baseline-hashes.txt` (file sha256 b62c68ca963e63014d102854e76602036aa2d68d98fd5abb716319c3101867e8)
  prints 92 OK, `git ls-files guide/scripts | wc -l` prints 92, `git rev-parse HEAD:guide/scripts` prints
  b4886ba38f649593c357c8fe5834283036f9254c; any FAILED entry, extra file or tree change is a FAIL. node:test floor 514
  (`ok tests (23 test files, 457 test cases)`) with every T11 r2 title present. Hash chain: `t6`
  (except the setup page in T12), `t7`, `t10`, `t11` (except L3 and the cheat sheet in T12). `t11-freeze` replaces
  `t5c-freeze` and `t11-hashes.txt` replaces `t8`/`t9` for L2 and L3 from the T11 merge on.
- FF-50 (T12 and later, after the `origin/factory` merge): decision D9 shifts S10 line numbers (old to new): 253-272
  +1; 273-316 +6; 317 becomes 323-326; 318-477 +9; 478-480 become 487-490; 481-486 +10; 487 becomes 497-498; 488 and
  later +11. Re-point every range: the T9 scan S10:217-420 becomes 217-429 (exemption 273-295 becomes 279-301, the
  absent Stop hook range 297-314 becomes 303-320); the T10 scan S10:421-484 becomes 430-494. Manifest entries anchor by
  section heading and survive; only their labels go stale. On the merge commit, `git diff <spec tip before merge>
  <merge> -- guide` prints nothing.
- FF-51 (T12 card, A14): (a) insertion-only on the setup page and L3: with the added lines removed the files hash to
  the `t6` and `t11` values; changing an existing line is a question first; the WP:252-258 fragment and the hook
  script stay byte for byte. (b) Sources S10:323-326 (Git Bash) and S10:253, 274-277 (`onFailure`); the wording says
  no more than "times out (180 s) or fails to run"; nothing says or implies that a non-zero exit other than 2 blocks
  (L3:103, L3:110 and cheat-sheet:33 quoted together with the new sentence). (c) No new fence, tab or command;
  `"onFailure": "block"` only as inline code; no `PowerShell(...)` text, no "D9", "decision" or "spec 10"; M stays 4,
  `<TabItem` stays 15, the manifest stays 304/304 with 0 pending. (d) Re-run the FF-39 scan for L3 with the FF-50
  ranges; hits on the D9 lines are listed as allowed, any other hit is a finding. (e) Owner note, not a T12 finding:
  setup and L3 say "Git for Windows (Git Bash)" (S10), troubleshooting, L4 and wrap-up say "Git Bash or WSL2"
  (WP:344); T12 does not edit those pages. (f) Rendered checks in both builds and three engines, 720 px, full axe 0,
  built descriptions unchanged. Must-fail mutant: L3 plus a line saying "any non-zero exit blocks the commit" passes
  the frozen checker and fails the (b) check.
- FF-52 (orchestrator decision): the L3 page says that the reference solution on `solution/level-3` sets
  `"onFailure": "block"` on the commit hook, so a hook that times out (180 s) or fails to run blocks the commit; it
  does not tell participants to change the WP:252-258 fragment (that would need a workshop plan change). Follow-up
  F14: spec 10 verifies that the field exists in the hooks schema; if it does not, the setup and L3 lines change and
  the gap goes into the guide.
- FF-53 (qa-final): run FF-46 with the FF-50 re-pointing; re-run the T11 scratch layer (`qa-t11-s11-render.mjs`
  through `qa-t11-s11-render.sh`, `-static.mjs`, `-ff38mut.sh`, `-reinsert.mjs`, `-mutants.mjs`) with ports and base
  sha re-pointed and the edits quoted; print the cheat sheet at margin 0 and at 10 mm; quote every final PR run with
  its attempts.
- Gate 2 owner notes from T11: O-5 the two CLI copy tests keep titles that say "passes" while asserting exit 1 (FF-2
  forbids renames in the cycle; rename after it); O-6 an explicit root skips the ban by design (FF-34 (c), pinned);
  the first CI run on PR #124 failed at startup with 0 jobs and was re-run without a code change.
- T11 rework (red Guide check on PR #124, run 42679728806): `check.mjs` ends a failing run with `process.exit(1)`
  right after `console.error`, so on Linux the tail of a long stderr read through a pipe can be lost; the T11 test
  `skeleton-ban.test.mjs` "one marker put back" lost its last line in 6 to 23 of 200 Linux runs. Fixed in the test only
  (output captured through files, asserting lines unchanged, one witness test; qa-t11 PASS r2, QA lead ENDORSE r2).
  Follow-up F15 (post-cycle): in `check.mjs:166` and `check-built.mjs:12` set `process.exitCode = 1` and return
  instead of `process.exit(1)`, pinned by a static test that no entry script under `guide/scripts` calls
  `process.exit(`, then re-baseline the freeze. Standards proposal for Gate 2: Node CLI entry points never call
  `process.exit` after writing output.
- FF-54 (T12 verifier and qa-final; interim until F15): a red `npm run check` or `check:built` on Linux may lose the
  tail of stderr, including the `check failed with N error(s)` line; a missing error line in a Linux log is never
  evidence that the error is absent. A scratch probe that asserts on the stderr of a failing run with more than about
  100 error lines reads the output through files (`stdio: ['ignore', fd, fd]`) or asserts on the exit status only. A
  Linux-only red on a test that is green locally is reproduced in a Linux container on the CI Node before any re-run
  or ruling; one green CI run is no evidence against a 5 to 20 percent race.
- Process note (all agents, from the T11 r2 meta-review): `updated` in an outbox is written from `date -u`, never as
  an estimate ahead of the clock; a future heartbeat hides a dead agent from the staleness rule.

From the T12 meta-reviews (qa-lead REJECT r1 on 738f7ac: RA-1 the L3 PowerShell sentence, RA-2 the cheat sheet's
"optional" Stop hook label; ENDORSE r2 on fdc0383; hashes in `t12-hashes.txt`, file sha256
b8dd5e4f493fcf5c2988107b3ead3cbea4ecb8aa4448e2117d9c18cffca0337a):

- FF-55 (T12 r2 verifier, delivered): diff L3 and the cheat sheet against the r1 bytes of 738f7ac; only L3:132 and the
  cheat-sheet lines in the implementer's table change; L3 minus lines 101-102 and 132-133 hashes to the `t11` value;
  FF-49 unchanged; the wording and static scratch checks follow the r2 sentences, with two must-fail copies (the r1 L3
  sentence restored; the cheat sheet without "optional") that fail the QA check while the frozen checker passes; 52
  tokens and a word trace against r1; print in four variants plus a control; rendered L3 and cheat sheet in both builds
  and three engines. Answered by qa-t12 PASS r2, QA lead ENDORSE r2.
- FF-56 (orchestrator, later directives; standards proposal at Gate 2): a directive that bans a literal gives the
  implementer replacement wording that keeps the referent; a trim-for-fit criterion lists what may not be dropped
  (qualifiers and labels such as optional and stretch, and pack facts, not only manifest tokens); every trim is graded
  meaning-neutral or not in an old -> new table; the verifier proves trims with a word-sequence trace against the
  previous bytes plus a control build without the compensating edits.
- FF-57 (qa-final, T12 layer, on the final head after the factory merge): (a) `shasum -a 256 -c t12-hashes.txt` gives
  3 OK; the rest of the chain is OK (`t6` index and wrap-up, `t7` 2, `t10` 1, `t11` architecture, troubleshooting and
  L2), the `t6` setup and `t11` L3 and cheat-sheet lines fail as expected; 11 pages bound. (b) Re-run the T12 scratch
  layer and quote each tail: `qa-t12-wording.mjs` (`WORDING OK`), `qa-t12-static.mjs` (`STATIC ALL OK`),
  `qa-t12-r2-joins.mjs` against 738f7ac (`JOINS ALL OK`), `qa-t12-cs.mjs` (52 tokens, 0 missing), `qa-t12-ff39.mjs`
  with the FF-50 ranges, `qa-t12-r2-mutants.sh` (same kill table). (c) Print with `qa-t12-r2-print.sh` in Chromium A4
  at margin 0 and 10 mm with a control: head full 2/2 and Level 4 hidden 1/1, control Level-4-hidden 10 mm = 2; quote
  the Chromium version and the headroom; two pages for the head is a FAIL. (d) `qa-t12-r2-sentences.sh` in three
  engines and both builds. (e) FF-53 and FF-54 apply unchanged.
- FF-58 (qa-final, after merging `origin/factory`: decisions D10 to D14, the D12 clarification and the spec 08 code;
  D13 deferred to spec 13): (a) on the merge,
  `git diff <tip> <merge> -- guide` prints nothing, FF-49 and FF-57 (a) hold. (b) Manifest sources unchanged: the diff
  over `docs/spec-pack/workshop` and spec 10 prints nothing; spec 09 changed but has 0 manifest entries
  (`grep -c "entries('spec09'" guide/scripts/coverage.mjs` = 0); `npm run check` still prints `anchored 304/304, 15
  manual rows, 0 entries pending`; if a source did change, re-derive the FF-50 ranges; D14 inserts 9 lines after
  spec 10 line 483 and edits the DoD, so T10's FF-39 range is S10:430-503 and ranges up to 483 stand. (c) decisions.md D9 still at
  lines 134-147; quote the lines the C0-13 rows cite. (d) D11 to D13 sweep over `guide/docs` and the built text for
  assistant, `/chat`, `/assistant/chat`, widget, stream, seed, book counts, results, temperature and storefront;
  allowed hits: L0:26, L4 Brief C (verbatim) and the architecture page's `assistant` rows. With D13 deferred the
  assistant page stays, so a page that mentions it or its nav link is not a finding; a chat widget, a streaming
  endpoint, a seeded-book count other than D12's (103 books: 100 available, 10 per genre, 3 discontinued), a search
  cap other than 200, or a temperature setting is a finding. (e) FF-43 on the merge:
  `AssistantCommands`, `BookstoreTools`, `AssistantConfig`, `OfflineChatModel` exist under
  `bookstore/src/main/java/**/assistant/`; the page's assistant suffix row equals the merged ArchitectureTest; its
  numbered rules match. (f) Referee on the merge: quote `Total time` and the counts (bookstore at least 1314,
  distributor 190, Vitest `682 passed | 2 expected fail` from factory ae2a5f2 on; fix-12 added tests, 598 before); over 60 s is never waived: re-run once on an idle machine
  and state the load; a second over-budget run is a needs-decision. (g) Guide, CI verify and windows-build on the merge
  commit and the final head, with run ids and attempts.
- Follow-up F16 (post-cycle, with F15, once the checker freeze lifts; from T12 O-4): manifest entries or a checker
  rule for setup:23 (Git Bash prerequisite), L3:101 (`onFailure`, "times out (180 s) or fails to run"), L3:132 (the
  PowerShell sentence) and the cheat-sheet labels "optional" (Stop hook) and "stretch" (review subagent), so removing
  or widening any of them fails `npm run check`; the must-fail copies R2-M1 to R2-M4 become the tests; re-baseline the
  freeze.
- Follow-up F14 extension (T12 O-3): spec 10 also settles whether `"onFailure": "block"` covers a non-2 exit (the
  reference script's `cd ... || exit 1`, next to its comment "Any other non-zero code does NOT block") or only a
  timeout or launch failure; the guide follows (L3:101, L3:105, L3:112, cheat-sheet:21); if it covers exit 1, spec 10
  changes first, then an sdd-bugfix on the guide.
- Follow-up F17 (post-cycle, D13): when the chat widget and streaming endpoint are built, re-check the architecture
  page's `assistant` rows against that change's ArchitectureTest.
- FF-59 (qa-final r3, from the T13 r1 meta-review, qa-lead ENDORSE; hashes in `t13-hashes.txt`, file sha256
  35dffe160b23...): the FF-49 chain reads `t13` 4 OK (setup, L3, wrap-up, troubleshooting), `t6` index, `t7` 2, `t10`
  1, `t11` architecture and L2, `t12` cheat sheet OK; expected FAILED lines exactly t6 setup and wrap-up, t11 L3,
  cheat sheet and troubleshooting, t12 setup and L3, each re-bound through the fb958b3 bytes. Re-run the qa-t13 layer
  (fidelity 4/4, the D16 grep with exactly 10 allowed hits, built scan with `.ps1` only in the OpenSpec installer,
  must-fail copy) and the earlier scratch layers that read a T13 page; an assertion pinning a line A15 deleted or
  replaced is a decision pin, updated and quoted (FF-46).
- Follow-up F16 extension (D16): nothing in the repo pins the absence of PowerShell hook text; add a checker rule or
  manifest test with F16 so a restored `.ps1` hook sentence fails `npm run check`.
- Follow-up F18 (owner, pack): WP:59 and S10:168 still say JDK 17 for `jdtls`; the guide's setup page says 21 from
  spec 10 finding PF-T6-1. Stale pack text after D16 (owner): S11:56 "Hook script tabs: bash and PowerShell.",
  S10:219, S10:322-326, S10:504, WP:344 ("Provide `.ps1` variants"). Not findings: L4:191 (verbatim SDD:136).
- D17 (factory 936028d): links to the frozen level branches (level-0-start, solution/level-1..3,
  start/level-4-openspec|speckit|agentos) may 404 until spec-10-done (R-G2 moves after spec-10-done); every other
  broken link is a FAIL.
- Gate 2 owner notes from T12: O-2 the L3 sentence says "hooks" where the pack says "handlers" (true of the settings
  fragment shown); O-5 the cheat sheet's one-page variant at 10 mm has 8 px of headroom; the D9 sentences have no repo
  pin until F16.

## Gate 2 (2026-11-09, self-accepted by the orchestrator)

- qa-final r1 at 921e18b (base factory 3b7e4f3): all 24 directive items green, no product finding; FAIL only because
  factory moved during the sweep. Factory ae2a5f2 merged into the spec branch (e7e7dea, no force-push).
- qa-final r2 at e7e7dea (base ae2a5f2, unmoved): PASS. Fresh-tree gates against the merged pack (coverage 304/304,
  15 manual rows, 0 pending; 514 tests; both check:built lines), referee 28.6 s (bookstore 1314, distributor 190, Vitest
  682 passed and 2 expected fail), D14 ranges re-derived, freeze 92 OK, 11 pages and 121 product files bound by hash,
  three byte-identical builds, rendered matrix 66 cells with 0 failures, CI attempt 1 green, PR readiness clean, deploy
  gate closed (Pages, release, gh-pages 404, no GUIDE_DEPLOY).
- QA lead final-review: ENDORSE; Gate 2 QA confidence high for the PR scope (guide source, frozen checker,
  `guide.yml`, one `.gitignore` line). Spec matrix S1 to S12 met (S7 push deploy and S9 hook tabs as residuals F12 and
  F2); S13 definition of done is an owner residual.
- Human overrides: only A7 (T5 merged after FAIL r4 and AMEND r4); its condition held (T5b and T5c merged before the
  first content task). The A7 temporary risk is still true at the head.
- Residuals with owners: F1 and F12 (push deploy, live site, release zip, clean-machine run: Maik and the
  orchestrator at the deploy gate); R-G2 external links to `solution/level-1..3` and `main` are checked by no gate (one
  `gh api` call per link at the deploy gate, after the spec 09 publish and spec 10); R-G3 `npm ci` reports 25
  vulnerabilities in the pinned Docusaurus 3.10.2 tree (static site built only on CI; Maik decides on a version bump
  after the release); FF-12 pack-only changes do not trigger the guide workflow (run `npm run check` by hand after a
  pack change); O1 and O2; F2; F14 with O-3; F15; F16; F-T5b-1; R-1 to R-5; F17 belongs to spec 13; the owner notes
  in the QA feed-forward section for the 2026-11-10 rehearsal.
- Standards proposals (QA lead ledger, `## Gate 2 QA assessment`): SP-1 literal bans with replacement wording and
  trim-for-fit qualifier lists; SP-2 Node CLI entry points set `process.exitCode`; SP-3 gate content and security rules
  on the built output, freeze checkers by hash; SP-4 spec-prefixed evidence names; SP-5 a base moved during qa-final is
  a FAIL, merge and re-run scoped by tree ids; SP-6 "no broken links" covers external links, checked at the deploy gate.
- The spec PR #68 is NOT merged by the orchestrator: it waits for `spec-10-done` and Maik's public deploy approval
  (the coordinator's rule for this run). The roadmap and progress files are not edited by this cycle.

### Gate 2 re-run (2026-11-10, self-accepted by the orchestrator after T13)

- Reopened for T13 (A15: decision D16, follow-up F2; JDK 21 floor; Git Bash wording) after merging factory bd838b6
  (D15, D16, spec 09, fix-08, fix-09) as 3fb3310; T13 merged as 7d0ca70 (#156, PASS r1, ENDORSE r1); factory 936028d
  (D17) merged as dcfa28a.
- qa-final r3 at dcfa28a (base factory 936028d, unmoved; scoped to the delta since fb958b3: T13 / A15 and the factory
  merges with D15 to D17): PASS. Fresh-tree gates (coverage 304/304, 15 manual rows, 0 pending; 514 tests; both
  check:built lines, 4 Windows lines all pack commands), referee 47.8 s (bookstore 1379, distributor 190, Vitest 689
  passed and 2 expected fail), freeze 92 OK, 11 pages bound by hash (`t13-hashes.txt` 4 OK, patch-id 7c0bd830), T13
  layer (fidelity 4/4, D16 grep 0 residual, must-fail copy), three A15 decision pins updated per FF-46, rendered matrix
  66 cells with 0 failures, 49 internal links 0 broken, 17 external URLs 200 (the 4 level-branch links are allowed by
  D17), CI attempt 1 green on T, PR readiness clean. Deploy gate armed by the owner (GUIDE_DEPLOY=true, Pages and
  github-pages at 13:15Z); nothing published yet.
- QA lead final-review r3: ENDORSE; Gate 2 QA confidence high for the PR scope (guide source, frozen checker,
  `guide.yml`, one `.gitignore` line). Spec matrix S1 to S12 met (S7 push path first runs at the merge; F2 closed by
  D16 / T13); S13 definition of done is an owner residual. The armed deploy gate is an owner approval (D17,
  progress.md:227), not a PR defect: merging #68 into factory runs deploy and release at once, with the
  `workshop-2026` tag on the merge commit.
- Residuals with owners (full table in the QA ledger `## Gate 2 QA assessment`): R-G2 level-branch links 404 until
  spec-10-done (D17), checked after it; F16 with the D16 extension (no repo pin for the absence of PowerShell hook
  text); F18 and the stale D16 pack text (S11:56, S10:219, S10:322-326, S10:504, WP:344); F1/F12 now at the merge
  (push deploy, live site, release zip); R-G3, FF-12, O1, O2, F14 with O-3, F15, F-T5b-1, R-1 to R-5, F17 unchanged.
  G-ARM (new): the merge publishes at once (deploy, release, `workshop-2026` tag on the merge commit); to hold it the
  owner unsets GUIDE_DEPLOY first. Right after the merge: the Guide push run has build, deploy and release green, the
  site answers 200, the release zip has `index.html` at its root.
- Standards proposals: SP-1 to SP-5 as before; SP-6 amended for D17 (one request per external URL; links to refs
  created later are checked right after they exist); SP-7 a final sweep quotes the deploy-gate state; SP-8 (agent-comms)
  `updated` is written from `date -u`.
- Under D17 #68 no longer waits for `spec-10-done`: the orchestrator does not merge it; the main session merges it with
  the owner's deploy approval in place and runs the post-merge checks; `spec-11-done` follows `spec-10-done` (R-G2).

## Amendments

- A1 (2026-11-08, orchestrator, during T1): the lab rule (G-4) also skips `guide/package-lock.json`. The lockfile is
  generated, not participant content, and contains the transitive package name `postcss-lab-function` of
  `@docusaurus/preset-classic`, so G-4 read literally cannot pass with the lockfile AC1 requires. The ASCII rule still
  scans the lockfile; self-tests pin both facts. Consequence: none for pages; cheap and reversible.
- A2 (2026-11-08, orchestrator, after the T1 meta-review): FF-1 is an added T3 acceptance criterion (zero-test guard
  for `npm run check`); T3's fileScope already holds `guide/scripts/**`. Consequence: T3 is slightly larger; the
  gate re-run is self-accepted (an added test criterion inside a task's scope).
- A3 (2026-11-09, orchestrator, during T4): the page grammar's contractual heading anchors use the MDX comment form
  `## Heading {/* #id */}` instead of `{#id}`, which does not compile under the template's `future.v4` (MDX acorn
  error, build exit 1). The ids are unchanged; the checker accepts only the working form and a test pins that `{#id}`
  fails. Consequence: T6 to T11 write anchors in the comment form; strategy.md's page grammar reads accordingly.
- A4 (2026-11-09, orchestrator, on the QA lead's T4 recommendation): new task T4a, run after T4 and before T5 (T5
  now depends on T4a). axe reports a critical WCAG `label` violation on every task-list checkbox, because Docusaurus
  renders `- [ ]` as a bare disabled input; the QA lead does not accept it as a residual and it would hit every later
  verifier. Card T4a (elevated, sonnet verifier, module guide):
  - fileScope: `guide/src/plugins/**`, `guide/docusaurus.config.ts`, `guide/scripts/**`.
  - AC1: a local rehype plugin (Node built-ins only, no new dependency; first check whether Docusaurus 3.10.2 already
    offers an option) gives every task-list checkbox an accessible name equal to its item text; the offline config
    inherits it by spreading the main config.
  - AC2: in both builds, `getByRole('checkbox', {name: <item text>, exact: true})` resolves exactly once for every
    checklist item.
  - AC3: the full axe ruleset reports 0 violations on the five level pages, both colour modes, both builds, three
    engines; negative control: a build copy without the plugin shows the violations.
  - AC4: `check:built` fails when a checkbox has no accessible name (self-tests and a strip probe).
  - AC5: plugin self-tests on a fixture, including an item with inline code.
  - AC6: `guide/docs/**` stays hash-equal to the T4 baseline; `package.json` and the lockfile stay equal to T1's;
    FF-13 and FF-17 hold.
  Consequence: one more task PR; the checker freeze baseline (C0-12) is taken at T5 after T4a.
- A5 (2026-11-09, orchestrator): FF-15 is an added T5 acceptance criterion (two insertion-only tests in
  `structure.test.mjs`: a fenced block under the Start branch line fails; a setext heading with `=` and `==` fails),
  and FF-16 constrains T5's rule (d). strategy.md's page grammar now prints the A3 anchor form.
- A6 (2026-11-09, orchestrator, after the T4a meta-review): FF-22 is an added T5 acceptance criterion (one nested
  `ol` fixture test in `task-list-labels.test.mjs`, insertion only). The plugin dropping the text of inline JSX
  elements from a checkbox label is accepted without a fix: no checklist item in the pack contains an HTML or JSX
  element, every level-page checklist item must equal a pack bullet, and FF-24 bars JSX in task items and checks
  label-equals-visible-text on every page. Consequence: none for the pages; the plugin stays as gated.
- A7 (2026-11-09, human decision relayed by the main session, options (b) plus (e) of the T5 needs-decision): T5
  failed its final verification (r4) after the escalated round; the QA lead upheld the FAIL (AMEND r4). The human
  overrides it: T5 merges with the r4 verifier hash file as the C0-12 freeze baseline (44 files under
  `guide/scripts/**`, copied into this folder as `t5-freeze-baseline-hashes.txt`; verify with `shasum -a 256 -c` from
  the repo root). T5b may change only the files its card names. Recorded temporary risk: "a multi-line inline code span holding a literal `</TabItem>` or
  `{/*`, and the paragraph-continuation cases from T5 r4 (OA1c, OA8c, OA3c, OA5c, OA4c, OA6c, OA6b, OA7c, OA1b, OA1b2,
  OA1b-f), are not caught by the source checker". Also open: RA-I (a `<pre>` line in a Windows tab that is part of a
  pack line or contains one is not pinned by a test; the code is correct today, the freeze hashes detect any change).
  Guards: (1) the authoring rules FF-27 and FF-30 in every content-task directive (no inline code span over a line
  break, no stray `|` lines, plain fences or `<pre>` in PowerShell tabs); (2) every content verifier checks the built
  pages by hand (FF-25) until T5b merges; (3) new task T5b (A8) enforces the Windows-line rule on built output in CI.
  CONDITION: T5b merges into the spec branch before ANY content task (T6 to T11) PR merges; content implementation and
  verification may run before, but no content task merges first. The MDX parser option (c) stays a logged follow-up for
  Maik.
- A8 (2026-11-09, same decision): new task T5b (critical; opus implementation, opus QA, QA lead review; module guide;
  depends on T5). Card:
  - fileScope: `guide/scripts/**` (new rule and test files; the T4a files `rules/built.mjs`, `check-built.mjs`,
    `lib/html.mjs` may change to wire the rule in). The existing tests of T1, T3, T4 and T4a stay untouched and green;
    T5's frozen source-checker files (r4 baseline) and the manifest stay byte-identical.
  - AC1: `npm run check:built` checks every line of every `pre` or `code` element inside a tab panel of the built Pages
    HTML whose tab label contains "PowerShell" (the n-th tab panel of a tabs container belongs to its n-th tab; a line
    is one `.token-line` or one newline-split line of a raw `pre`, trimmed, non-empty) against the pack command list
    (`packCommandLines` of the five pack files, imported read-only from `rules/content.mjs`), and fails on any line not
    in the list, naming file, tab and line.
  - AC2: self-tests use built HTML fixtures (minified, unquoted attributes, as Docusaurus emits them) and cover the 11
    T5 r4 cases as built output (each must fail), plus a hidden panel, nested tabs, a `pre` holding a pack line (passes),
    a raw `<pre>` (each line checked), and a non-PowerShell tab (not checked).
  - AC3: on the real guide build the rule passes and prints the number of Windows lines checked; a strip probe on a copy
    of the real build with one invented line fails.
  - AC4: CI runs it on every PR through the existing `npm run check:built` step (no workflow change).
  - AC5: no new dependency; `package.json` and the lockfile unchanged; no product file outside `guide/scripts/**`.

- A9 (2026-11-09, orchestrator, during T5b): A8 AC1 extended (raises the bar, closes known evasions of the
  structural guard): `check:built` also checks every PowerShell-family language code block (`powershell`, `pwsh`,
  `ps1`, `ps`, `cmd`, `bat`, class on the `pre` or its `code`, whole name or followed by a non-alphanumeric suffix)
  anywhere on the page; all text of a raw `pre` around token lines; tab labels cleaned of every Default_Ignorable code
  point and leftover character references. Two of T5b's own round-0 assertions were edited accordingly and the count
  line reads "in tab panels and PowerShell code blocks". The C0-12 freeze baseline after T5b is
  `t5b-freeze-baseline-hashes.txt` (75 files under `guide/scripts/**`).

- A10 (2026-11-09, orchestrator, raised by T6): five frozen self-tests read the live `guide/docs/**` tree and assume
  every page still carries the skeleton marker (`check-content.test.mjs` around lines 41, 97, 231, 263, 287 and
  `coverage-manifest.test.mjs` line 94), so `npm run check` fails as soon as a content task removes its markers. This
  is a defect of the frozen checker's tests, not of the pages. New task T5c (critical; opus implementation, opus QA,
  QA lead review; module guide; depends on T5b; must merge before any content task PR, like T5b). Card:
  - fileScope: `guide/scripts/**` only: a committed snapshot of the T4 skeleton pages as a fixture (for example
    `guide/scripts/fixtures/skeleton-docs/**`, byte-equal to the pages at spec tip 05e58a2), the tests that read the
    live tree switched to that snapshot, and a refreshed freeze baseline.
  - AC1: every self-test that depends on the marker state of the live docs tree reads the snapshot instead; each
    keeps its assertions and expected values (same intent, same strength); no rule source file changes.
  - AC2: a sweep of every test under `guide/scripts/**` for any other dependence on the live docs content (marker
    state, page bodies, pending counts) that T6 to T11 would break; each one found is moved to the snapshot the same
    way, or listed with a reason why it stays live.
  - AC3: with the T6 branch's three pages applied on top (`git show a1817fe:<path>` in a scratch copy), `npm run
    check` exits 0; with all eleven markers removed from a scratch copy, no self-test fails; the remaining failures are
    rule errors about the pages' missing content (coverage entries, and the structure and content rules that judge a
    finished but empty page, such as the sync moment count and the observation card), never a self-test. (Wording
    clarified by the orchestrator on 2026-11-09: the frozen rules correctly report empty finished pages.)
  - AC4: freeze: only the edited test files and the new fixture differ from `t5b-freeze-baseline-hashes.txt`; a new
    baseline `t5c-freeze-baseline-hashes.txt` replaces it for FF-32 (a).
  - AC5: no new dependency; no product file outside `guide/scripts/**`; no test is removed or weakened; node:test total
    unchanged or larger, every title kept.

- A11 (2026-11-09, orchestrator, on the QA lead's T8 recommendation): the built meta description of the Level 2 page
  drops the span between the two colons of its Goal line (the only Goal with a second colon). T11's fileScope gains
  exactly one added frontmatter line on `guide/docs/03-level-2-give-your-agent-eyes.mdx`:
  `description: "Goal: Let the agent see what you see: the code through a language server, and real external data through our own MCP server, instead of guessing."`
  (the WP:177-178 Goal verbatim). Checked under FF-41 (b). Consequence: one line; no other change to the L2 page.

- A12 (2026-11-09, orchestrator, on the QA lead's T9 meta-review): the T9 card (strategy.md:595) asked for a pointer to
  the troubleshooting anchor `#windows-hooks`; the T9 brief approved a prose pointer instead on the false premise that
  the anchor did not exist (it exists on the T4 skeleton, `guide/docs/reference/troubleshooting.md:8`). T11's fileScope
  gains exactly one added line on `guide/docs/04-level-3-behavioral-control.mdx`, after line 130:
  `[Windows hooks](./reference/troubleshooting.md#windows-hooks)`. Checked under FF-44. Consequence: one line; no other
  change to the L3 page (the line 55 wording, R4, stays an owner note).
  Revised 2026-11-09 (orchestrator, on the T11 implementer's finding that a single line renders inside the L3:130
  paragraph): exactly two added lines, one empty line and the unchanged link line, so the link is its own paragraph.
  FF-44's "exactly that line" reads "exactly these two lines"; with them removed the file hashes to `t9-hashes.txt`.
- A13 (2026-11-09, orchestrator, T11 card ruling): the cheat sheet's "two tables" in the card summary yields to spec 11
  ("one page, printable", S11:36) and card AC2 (at most 2 A4 pages, the second only for the Level 4 table): commands
  and files as one bullet list, the Level 4 commands as a table. Consequence: none outside the cheat sheet.
- A14 (2026-11-09, orchestrator, on decision D9 relayed by the coordinator, factory f5a0e17): new task T12 after T11
  and after `origin/factory` is merged into the spec branch. fileScope: `guide/docs/00-setup-check.md`,
  `guide/docs/04-level-3-behavioral-control.mdx`, `guide/docs/reference/cheat-sheet.md`. Acceptance criteria:
  (1) the setup check page states that Windows participants need Git for Windows (Git Bash), in prose (the pack states
  no check command, so no command and no manifest entry); (2) the Level 3 page states the same prerequisite (the `Bash`
  matcher, the `if` field and the `Bash(...)` rules apply only when Claude Code runs commands through Bash; no
  `PowerShell(...)` rules) and that the reference solution's commit hook sets `"onFailure": "block"`, so a hook that
  times out (180 s) or fails to run blocks the commit (FF-52); (3) the cheat sheet prints on at most 2 A4 pages at
  margin 0 and at 10 mm, and with the Level 4 section hidden on exactly 1 page at both margins, keeping all 52
  manifest tokens (the T11 O-4 finding); (4) FF-49 to FF-51 hold; the checker is not changed. Verified under FF-49 to
  FF-51, FF-35, FF-36, FF-38 (setup 1, L3 3), FF-41, FF-47.
- A15 (2026-11-10, orchestrator, on decision D16 relayed by the coordinator, factory bd838b6 merged into the spec
  branch as 3fb3310): new task T13 after T12 (follow-up F2). D16 drops every PowerShell hook. The guide never had a
  PowerShell hook tab; its four Windows PowerShell tab lines are pack commands, not hooks (setup `Get-Command jdtls`
  WP:54 and `$env:BOOKSTORE_AI_API_KEY="<key on screen>"` WP:75, troubleshooting `Get-Command jdtls` WP:54, wrap-up
  `irm https://openspec.dev/cli/install.ps1 | iex` SDD:149), so they stay and `check:built` keeps printing 4 Windows
  lines, which is the D16 state; the frozen checker (FF-49) does not change. fileScope:
  `guide/docs/04-level-3-behavioral-control.mdx`, `guide/docs/reference/troubleshooting.md`,
  `guide/docs/00-setup-check.md`, `guide/docs/06-wrap-up.md`. Acceptance criteria:
  (1) L3: the paragraph "On Windows, `solution/level-3` also holds a PowerShell variant of the hook ..." (L3:134) and
  one adjacent empty line are deleted; nothing else on L3 changes; L3:132 stays (D9 content, true under D16: it states
  that no PowerShell hooks or rules exist) and the `[Windows hooks]` link still resolves.
  (2) Troubleshooting: the paragraph "On Windows, `solution/level-3` also holds a PowerShell variant ..." (line 15)
  and one adjacent empty line are deleted; the `## Windows hooks {/* #windows-hooks */}` heading and lines 13 and 17
  (manifest tokens TS-03 and TS-04) stay byte for byte.
  (3) Setup: line 92 reads "The plugin does not ship the binary. `jdtls` needs JDK 21 or later, and you have 25."
  (only `17` becomes `21`; source spec 10 finding PF-T6-1: the upstream `jdtls` wrapper v1.63.0 refuses Java below 21;
  the pack's WP:59 and S10:168 still say 17, owner follow-up F18).
  (4) Wrap-up: line 124 reads "Agent OS needs bash (WSL or Git Bash)." (the TS-04 wording; one phrasing across pages).
  (5) `grep -rniE "ps1|powershell" guide/docs` lists only the four Windows PowerShell tab labels and fences with the
  four pack command lines above, the wrap-up `irm ... install.ps1 | iex` line and the L3:132 sentence; no `.ps1` hook
  name, no PowerShell hook sentence remains on any page; pages say nothing new about PowerShell.
  (6) All gates green with Node 26.13.0 (`npm ci`, `check` with anchored 304/304, 15 manual rows, 0 pending,
  node:test floor 514, `typecheck`, `build`, `check:built` with "4 Windows PowerShell lines", `build:offline`) and the
  referee; FF-49 (freeze 92 OK, `guide/scripts` tree b4886ba3) holds; `package.json` and the lockfile unchanged; no
  file outside the fileScope; ASCII only, no em dash; every other change is a deletion except the two one-line
  replacements in (3) and (4).
  Verified by an elevated sonnet verifier and the QA lead (opus); model policy from 2026-11-10: sonnet for
  implementation and verification, opus only for the QA lead and qa-final.

## Decision on the Stop hook (M-2, orchestrator, 2026-11-09)

T9 does not print the optional Stop hook script; it describes the Stop hook as optional and links `solution/level-3`.
T9's AC2 (quoted script lines anchored by manifest entries) then holds for the printed bash hook script only.
