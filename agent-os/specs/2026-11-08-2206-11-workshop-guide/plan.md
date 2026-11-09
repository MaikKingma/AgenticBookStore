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
| T2 | `guide.yml`: PR build plus offline zip artifact, gated deploy and release, SHA pins, timeouts | root | [T1] | B2 | critical | opus | sonnet | PASS r1 | #73 | sdd/11-workshop-guide/T2 | ENDORSE |
| T3 | OpenValue brand in `custom.css` only, Montserrat with provenance and OFL, contrast check, no colour values elsewhere | guide | [T1, T2] | B3 | elevated | sonnet | sonnet | PASS r2 | #76 | sdd/11-workshop-guide/T3 | ENDORSE r2 |
| T4 | LevelProgress, 11 page skeletons, sidebar, fixed anchors, structure checks reading the workshop plan at check time | guide | [T3] | B4 | critical | opus | sonnet | PASS r2 | #79 | sdd/11-workshop-guide/T4 | ENDORSE r2 |
| T4a | accessible names for task-list checkboxes (local rehype plugin) and a `check:built` guard | guide | [T4] | B4a | elevated | sonnet | sonnet | PASS r1 | #83 | sdd/11-workshop-guide/T4a | ENDORSE |
| T5 | pack-anchored coverage manifest and content rules | guide | [T4a] | B5 | critical | opus | sonnet | FAIL r4, human override (A7) | #87 | sdd/11-workshop-guide/T5 | AMEND r4, override: human |
| T5b | rule (c) on the built pages: `check:built` checks every pre or code line inside a Windows PowerShell tab panel against the pack command list | guide | [T5] | B5b | critical | opus | opus | verifying | #100 | sdd/11-workshop-guide/T5b | |
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

## Decision on the Stop hook (M-2, orchestrator, 2026-11-09)

T9 does not print the optional Stop hook script; it describes the Stop hook as optional and links `solution/level-3`.
T9's AC2 (quoted script lines anchored by manifest entries) then holds for the printed bash hook script only.
