---
spec: fix-12-genre-labels-footer
source: docs/spec-pack/specs/12-storefront.md
status: in-progress
accepted-by: orchestrator (autonomous; Maik delegated plan and gate approvals, approves only the end result)
created: 2026-11-09T20:44:09Z
origin: Maik's live walk of the storefront at /shop/ on 2026-11-09 (screenshot), decision D11 at e16a69b
gate1: 2026-11-09T20:47:20Z
qa-lead: on
base: factory
branch: sdd/fix-12-genre-labels-footer/spec
module-vocabulary: [root, bookstore, distributor, frontend, guide]
kind: bugfix
severity: S4
path: lite
fix-strategy: root-cause
regression-since: not-a-regression
closes: [D11 genre labels, D11 footer at the bottom of the window]
---

# Plan: readable genre labels everywhere in the shop, and a footer at the bottom of the window

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.
ROOT = the repository root. FE = `frontend/`. All commands run from ROOT unless they say `in FE`. Vitest runs in FE
with the pinned Node from `frontend/node` (installed by the referee).

## Ground rules

- The pack spec `docs/spec-pack/specs/12-storefront.md` (Visual design bullets "Genre labels" and "Footer", the
  catalogue and search scenarios, the unknown-genre scenario), `docs/spec-pack/orchestrator/decisions.md` D11 and
  `docs/spec-pack/product/tech-stack.md` win. Plain CSS only, no UI library, no new npm dependency.
- Never skip, disable, delete or weaken a test or an ArchUnit rule. Never `--no-verify`. Never set, read or write
  `ANTHROPIC_API_KEY`. ASCII only in every file touched; docs and comments plain English without em dashes.
- Pre-existing tests pass unmodified, with exactly the carve-out of D3 (T1 only): the listed assertions that pin the
  pre-D11 raw enum names are replaced by assertions of the D11 labels, each at least as strong. New tests are
  appended in new `describe` blocks, never edited into old tests.
- Referee: `./mvnw clean verify` from ROOT, green, within 60 seconds. Base referee on e16a69b (orchestrator,
  2026-11-09): 26.6 s; Vitest 22 files, 598 passed + 2 expected fail (600); bookstore 1314; distributor 190.
- Implementers do not git commit, branch or stash; the orchestrator owns git. Implementers never mutate production
  files outside their fileScope to self-probe: describe each mutant in the outbox, the verifier runs it.
- Evidence files in the shared scratchpad carry the cycle, role and task prefix (`f12g-t1-impl-*`, `f12g-t2-qa-*`).
- Frontend hygiene from the spec 12 cycle (its plan.md QA feed-forward): run the literal C0-6 grep set before handoff
  (titles, comments and fixtures are scanned), no colour literal outside `tokens.css`, every `color` rule follows the
  ob-on convention of `contrast.test.ts`, no console error or warning (the `setupTests.ts` trap), and every array in a
  `toEqual` follows the pass-through fixture rules.

## Path ruling (reference.md section 6)

Lite path (the default) - all hold:
- [x] reproduced in 1a.2: both defects deterministically by reading (bug.md Reproduction), the genre one also by the
  existing tests that pin the raw names and pass on e16a69b
- [x] root cause named at file:line and confirmed: `frontend/src/genres.ts:6-17` (no label map; consumers
  `SearchForm.tsx:59`, `BookCard.tsx:15`, `BookDetailPage.tsx:53`); `frontend/src/App.tsx:68-75` with
  `frontend/src/styles/global.css` (no shell layout)
- [x] one obvious fix design readable from the code: a typed label map in `genres.ts`; a shell element with a
  sticky-footer flex column
- [x] blast radius enumerable by grep now: bug.md Blast radius (three genre sites, all in T1; the alert deferred by D11)
- [x] no already-wrong persisted data or produced artifacts
- [x] not S1; not money, numbering, auth or concurrency
Bundled owner decisions in the report: D11 is the owner decision and is already written into the pack spec; it is the
expected behaviour of both tasks, not an extra task.
Open question the code does not settle in minutes: none.
-> path: lite (no lead, no research fan-out; two tasks; QA lead registered at Phase 2/3, every task elevated).

## Decisions (orchestrator, within authority, cheap and reversible)

- D1: models per the main session: sonnet for both implementers and per-task verifiers (no critical task), opus for
  the QA lead and qa-final. Never fable.
- D2 (T1): the book detail page is in scope. D11 says "the shop never shows a raw enum name"; the card and the select
  are named examples, and `BookDetailPage.tsx:53` prints the same wire value.
- D3 (T1): the pre-existing assertions that pin the old raw names are updated, because D11 changed the requirement
  they encode: `CataloguePage.test.tsx` comment at line 10, the expected genres of the listing test (lines 118-120,
  asserted at 127) and the option texts of "offers Any genre and the ten genre names in declaration order" (lines
  196-214; its title may say "labels"); `BookDetailPage.test.tsx` line 83 (`TECHNOLOGY`) and the test at lines 90-99
  ("the genre as sent", `SCIENCE_FICTION`). Each replacement asserts the exact label and, where the old one asserted a
  raw name, also that the raw name is absent. No other pre-existing test line changes; the search tests that select
  an option by its value (`'FICTION'`, `'POETRY'`, `'MYSTERY'`, `'ROMANCE'`) stay as they are.
- D4 (T1): a genre value the label map does not know (the bookstore added one before the shop was rebuilt) is shown as
  sent, never blank and never a crash.
- D5 (T2): the shell is an element of its own, `<div className="app-shell">` in `App.tsx` around header, main and
  footer, rather than a `#root` rule, so a DOM test can pin it. `min-height: 100vh` is declared first and
  `min-height: 100dvh` after it in the same rule (the dynamic unit for mobile toolbars, the static one as the
  fallback). The main grows; the footer stays in the normal flow (never fixed, absolute or sticky), so it can never
  cover content.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | status | verdict | qa-lead | pr | branch |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | the select, the card and the detail page show the sentence-case genre label; the select still sends the enum name | frontend | [] | 1 | elevated | sonnet | done | PASS r0 | ENDORSE | #133 | sdd/fix-12-genre-labels-footer/T1 |
| T2 | the footer sits at the bottom of the window on a short page and below the content on a long one | frontend | [T1] | 2 | elevated | sonnet | done | PASS r0 | ENDORSE | #137 | sdd/fix-12-genre-labels-footer/T2 |

T2 depends on T1 only for ordering (same module, one task at a time).

### T1: readable genre labels in the select, on the card and on the detail page

- fileScope: `frontend/src/genres.ts`, `frontend/src/genres.test.ts` (new), `frontend/src/components/SearchForm.tsx`,
  `frontend/src/components/BookCard.tsx`, `frontend/src/pages/BookDetailPage.tsx`,
  `frontend/src/pages/CataloguePage.test.tsx`, `frontend/src/pages/BookDetailPage.test.tsx`
- Acceptance criteria:
  1. Regression test `frontend/src/pages/CataloguePage.test.tsx` > "offers the genre labels as option text and the
     enum names as option values" (new `describe` block appended, for example "CataloguePage genre labels") exists at
     the component layer (per the spec 12 test plan: Vitest and Testing Library), is named for the correct behaviour
     (not the bug id), and asserts the expected behaviour stated in bug.md Reproduction: the option texts of the Genre
     select are exactly `Any genre`, `Fiction`, `Science fiction`, `Fantasy`, `Mystery`, `Romance`, `History`,
     `Science`, `Technology`, `Children`, `Poetry` in that order, and the option values are exactly `''`, `FICTION`,
     `SCIENCE_FICTION`, `FANTASY`, `MYSTERY`, `ROMANCE`, `HISTORY`, `SCIENCE`, `TECHNOLOGY`, `CHILDREN`, `POETRY`.
  2. Regression test `frontend/src/pages/CataloguePage.test.tsx` > "shows the genre label on every card and no enum
     name": the three cards of the listing show `Fantasy`, `Science fiction` and `Fiction` in card order, and no card's
     text contains `FANTASY`, `SCIENCE_FICTION` or `FICTION` (case-sensitive).
  3. Regression test `frontend/src/pages/CataloguePage.test.tsx` > "sends the enum name of the genre chosen by its
     label": the visitor picks the option by its label text `Science fiction` (`user.selectOptions` with the label)
     and searches; the request paths are exactly `['/books', '/books?genre=SCIENCE_FICTION']`.
  4. Regression test `frontend/src/pages/BookDetailPage.test.tsx` > "shows the genre label, not the enum name" (new
     `describe` block appended): for `SCIENCE_FICTION` the page shows `Science fiction` and no element text equals
     `SCIENCE_FICTION`; for `TECHNOLOGY` it shows `Technology`.
  5. Unit test `frontend/src/genres.test.ts` (new): the label map has exactly one entry per `GENRES` value and no
     other, the labels in `GENRES` order are exactly the ten labels of criterion 1, and a value the map does not know
     is returned as given (D4). Green after only (the module does not export a label map before the fix).
  6. The pre-existing assertions listed in plan.md D3, and only those, are updated to the D11 labels; each replacement
     is at least as strong as the line it replaces (exact text, plus absence of the raw name where a raw name was
     asserted). `git diff <pr-base> -- frontend/src/pages/CataloguePage.test.tsx frontend/src/pages/BookDetailPage.test.tsx`
     shows deletions only on the D3 lines. The unknown-genre test (`CataloguePage.test.tsx` "shows the title, the
     detail and the genre list of the server for an unknown genre") and `ProblemDetailAlert.test.tsx` pass unmodified:
     the alert keeps the server's list.
  7. Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside +
     SHA-256; never `git checkout --`, `git restore` or `git stash`), `vitest run src/pages/CataloguePage.test.tsx
     src/pages/BookDetailPage.test.tsx` (in FE) FAILS on exactly the tests of criteria 1 to 4 and the D3-updated
     tests, with `expected [ 'Any genre', 'FICTION', ... ] to deeply equal [ 'Any genre', 'Fiction', ... ]` (or the
     equivalent list mismatch) for criterion 1, `Unable to find an element with the text: Science fiction` (or the
     equivalent missing-label failure) for criteria 2 and 4, and user-event's `Value "Science fiction" not found in
     options` (no option carries that label) for criterion 3 - not with a compile, import, type or fixture error; no
     other pre-existing test fails; with the fix in place it PASSES. Both outputs are recorded verbatim in the outbox
     artifact under `## Red evidence` and `## Green evidence`.
  8. The fix sits at the root cause: the ten labels live in one place, `genres.ts`, as a map keyed by the `Genre` type
     so that a missing genre is a type error; `SearchForm.tsx`, `BookCard.tsx` and `BookDetailPage.tsx` read it. No
     component derives a label by string transformation (no `replace`, `toLowerCase` or `split` on a genre).
     `ProblemDetailAlert.tsx` is unchanged. The comments in `genres.ts` and `BookCard.tsx` that say the genre is shown
     as sent are updated in plain English.
  9. No behaviour change outside the root cause: the full Vitest run is green (600 baseline, 2 of them the
     expected-fail `it.fails`, plus the new tests); every other pre-existing test passes unmodified; no console error
     or warning; no new npm dependency; `npm run build` (tsc and Vite) green; the C0-6 grep set is clean.
  10. `./mvnw clean verify` is green within 60 seconds.

### T2: the footer at the bottom of the window

- fileScope: `frontend/src/App.tsx`, `frontend/src/App.test.tsx`, `frontend/src/styles/global.css`,
  `frontend/src/styles/styleContract.test.ts`
- Acceptance criteria:
  1. Regression test `frontend/src/styles/styleContract.test.ts` > "gives the shell a full-height flex column" (new
     `describe` block appended, for example "the footer at the bottom of the window") exists at the source level of
     the CSS contract (jsdom computes no layout; bug.md Reproduction), is named for the correct behaviour, and asserts
     on `global.css` as read from disk: a `.app-shell` rule declares `display: flex`, `flex-direction: column`,
     `min-height: 100vh` and `min-height: 100dvh`, the `100dvh` declaration after the `100vh` one (D5).
  2. Regression test `frontend/src/styles/styleContract.test.ts` > "lets the main of the shell grow": a rule whose
     selector targets the main of the shell (`.app-shell > main` or `.app-shell > .page`) declares a positive grow
     (`flex: 1 0 auto`, `flex: 1` or `flex-grow: 1`).
  3. Guard test (green before and after) `frontend/src/styles/styleContract.test.ts` > "keeps the footer in the normal
     flow": no rule for `.site-footer` declares `position: fixed`, `absolute` or `sticky`.
  4. Regression test `frontend/src/App.test.tsx` > "puts the header, the main and the footer in one shell element"
     (new `describe` block appended): the banner, main and contentinfo landmarks are, in that order, the only element
     children of one element with class `app-shell`, on the catalogue and again after opening `#/basket`.
  5. Negative control: on the pre-fix code (production files restored from `git show HEAD:<file>` via copy-aside +
     SHA-256; never `git checkout --`, `git restore` or `git stash`), `vitest run src/styles/styleContract.test.ts
     src/App.test.tsx` (in FE) FAILS on exactly the tests of criteria 1, 2 and 4, with a missing-rule assertion (for
     example `expected undefined to be 'flex'`, no `.app-shell` rule) for criteria 1 and 2 and a missing-element
     assertion (no element with class `app-shell`) for criterion 4 - not with a compile, import or fixture error; the
     criterion 3 guard and every pre-existing test pass; with the fix in place it PASSES. Both outputs are recorded
     verbatim in the outbox artifact under `## Red evidence` and `## Green evidence`.
  6. The fix sits at the root cause: `App.tsx` wraps `SiteHeader`, the `main` and `SiteFooter` in one
     `<div className="app-shell">` inside `BasketProvider`; `global.css` gets the shell rule and the growing-main rule
     with a plain-English comment; the header stays sticky ("has a sticky header" green); no layout code in
     TypeScript, no `position: fixed` footer, no new dependency.
  7. Colours: the new rules declare no colour, or follow the ob-on convention; no colour literal; `contrast.test.ts`
     passes unmodified.
  8. No behaviour change outside the root cause: the full Vitest run is green (T1's after-state totals plus the new
     tests); every pre-existing test passes unmodified (`git diff <pr-base> --numstat -- frontend/src/App.test.tsx
     frontend/src/styles/styleContract.test.ts` shows 0 deletions); no console error or warning; `npm run build`
     green; the C0-6 grep set is clean.
  9. `./mvnw clean verify` is green within 60 seconds.

## QA feed-forward

- FF1 (qa-lead, T1 r0; applies to T2): (1) must-fail probes on the CSS contract parser, one run each with a hash
  restore: `100dvh` before `100vh`; the `.app-shell` rule inside a comment; the rule inside
  `@media (min-width: 9999px)`; a later `.app-shell { display: block; }`; a later `.app-shell > main { flex: 0 0 auto; }`;
  `flex: 0 1 auto`, `flex: none` and `flex-grow: 0` fail while `flex: 1 0 auto` passes; the footer guard fails for
  `position: sticky` in a second `.site-footer` rule, in a grouped selector (`.x, .site-footer`), in a compound
  selector (`footer.site-footer`, `.app-shell > .site-footer`) and without a space (`position:fixed`). (2) DOM probes
  that must fail criterion 4: the footer outside the shell, an extra element child, main and footer swapped, the shell
  only on the catalogue route; the `#/basket` check runs after an awaited assertion that the basket page is shown.
  (3) Live measurement on the built app in a fresh browser context at 1280x800 and 390x844: on a short page (empty
  basket) the footer's bottom equals `innerHeight` within 1 px and `scrollHeight === innerHeight`; on a long page the
  footer starts at or below the last card and does not overlap it; after scrolling the header's top is 0. (4)
  `git diff <pr-base> --numstat -- frontend/src/styles/global.css frontend/src/App.tsx` with every deleted line quoted
  and justified (expected: only App.tsx lines re-indented inside the shell); no colour literal; `contrast.test.ts`
  unmodified; no console error or warning.
- Q1 (qa-lead, T1 r0; applies to qa-final): `git grep -n "GENRE_LABELS\[" -- frontend/src` hits only `genres.ts` and
  `genres.test.ts`, and `genreLabel(book.genre)` appears exactly once in each of `BookCard.tsx` and `BookDetailPage.tsx`.
- Q3 (qa-lead, T2 r0; applies to qa-final): on the spec tip, `frontend/src/styles/global.css` and `frontend/src/App.tsx`
  hash to the T2-reviewed values in the QA ledger; `git grep -n "app-shell" -- "frontend/src/styles/*.css"` prints
  exactly `.app-shell {` and `.app-shell > main {` in `global.css`; no other top-level `main` or `.page` rule sets
  layout; every `@media` block is quoted and none names `.app-shell`, `main` or `.page`.
- Q4 (qa-lead, T2 r0; applies to qa-final): on the built jar at `/shop/`, fresh browser context, 1280x800 and 390x844:
  computed styles of the shell (`display: flex`, column, `minHeight` equal to the window height) and of the main
  (`flexGrow` 1); short page footer bottom equals the window height within 1 px and `scrollHeight` equals it; long
  page footer at or below the last card; header top 0 after scrolling.

## Amendments

- Residuals for Maik (not criteria of this cycle): R1 (qa-lead, T1 r0) the D4 fallback is pinned at the `genreLabel`
  unit only; optional test-only follow-up: an unknown genre on a card and on the detail page shown as sent. R2 the title
  of the pre-existing listing test in `CataloguePage.test.tsx` ("every value as the bookstore sent it") is slightly
  stale; it is outside D3 and was left unchanged. Orchestrator decision: R1 not added to T1 (S4, the qa-final grep Q1
  rules out the direct map read on the merged code; cheap to add later). R3 (qa-lead, T2 r0) the source-level CSS
  reader does not see an override inside a media query or a later same-specificity `main.page` rule; Q3 and Q4 cover
  the merged code; optional test-only follow-up: an allow-list of the rules that may set layout on the shell, main or
  `.page`. R4 `.page { width: 100% }` keeps the main full width in the flex column and is pinned only by the Q3 hash.
