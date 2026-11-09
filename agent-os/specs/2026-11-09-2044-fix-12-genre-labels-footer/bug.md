---
bug: fix-12-genre-labels-footer
severity: S4
path: lite
status: fixed
closes: [D11 genre labels, D11 footer at the bottom of the window]
---
# Bug: the storefront shows raw genre enum names, and the footer floats under short content

## Report
- Observed (Maik, live walk of `/shop/` on 2026-11-09, with screenshot): the genre select of the catalogue lists
  `FICTION`, `SCIENCE_FICTION`, ...; the book cards print the genre as `SCIENCE_FICTION`. On a short page the footer is
  not at the bottom of the browser window; it sits right under the content with empty window space below it.
- Expected (pack spec `docs/spec-pack/specs/12-storefront.md`, Visual design bullets "Genre labels" and "Footer",
  catalogue scenario; `docs/spec-pack/orchestrator/decisions.md` D11): the shop never shows a raw enum name. The
  select and the card show the sentence-case label (Fiction, Science fiction, Fantasy, Mystery, Romance, History,
  Science, Technology, Children, Poetry); the select still sends the enum name as the `genre` parameter; the
  unknown-genre alert keeps the list the bookstore sends. The footer sits at the bottom of the window when the content
  is shorter than the window, below the content when it is longer, and never covers content.
- Environment: factory at e16a69b, the built storefront served by the bookstore under `/shop/`, any browser.
- First seen / last known good: the behaviour has existed since spec 12; D11 (e16a69b) changed the requirement.
  Regression window: not a regression.
- Frequency: always.
- Impact: cosmetic, no flow blocked, no data affected. S4.
- Related: D11, spec 12 cycle `agent-os/specs/2026-11-08-2145-12-storefront/`.

## Reproduction
- Status: reproduced (deterministic, by reading the code and by the existing tests that pin the old behaviour).
- Recipe A (genre): `frontend/src/components/SearchForm.tsx:58-60` renders `<option value={genre}>{genre}</option>`
  for every entry of `GENRES` (`frontend/src/genres.ts:6-17`, enum names only). `frontend/src/components/BookCard.tsx:15`
  renders `{book.genre}`; `frontend/src/pages/BookDetailPage.tsx:53` renders `{book.genre}` as well. The existing tests
  assert exactly this and pass on e16a69b: `CataloguePage.test.tsx:118-127` (card text `SCIENCE_FICTION`),
  `CataloguePage.test.tsx:196-214` (option texts `FICTION` ... `POETRY`), `BookDetailPage.test.tsx:83` (`TECHNOLOGY`)
  and `:96` (`SCIENCE_FICTION`).
- Recipe B (footer): `frontend/src/App.tsx:68-75` renders `SiteHeader`, `<main className="page">` and `SiteFooter` as
  plain siblings inside `#root`. `frontend/src/styles/global.css` has no `min-height`, no `100vh`/`100dvh` and no flex
  column anywhere (grep of `frontend/src/styles` and `frontend/index.html` for `min-height|100vh|dvh|#root|flex-direction`
  prints nothing); `.site-footer` (`global.css:142-148`) only has `margin-top: 3rem`, and `.page` (`global.css:59-64`)
  does not grow. So the footer follows the content directly and ends wherever the content ends.
- Evidence: base referee on e16a69b (orchestrator, 2026-11-09): `./mvnw clean verify` BUILD SUCCESS in 26.6 s; Vitest
  22 files, 598 passed + 2 expected fail (600); bookstore 1314; distributor 190. Log:
  scratchpad `f12g-orch-base-referee.log`.
- Regression-test layer: component layer (Vitest and Testing Library) for the labels, per the spec 12 test plan
  (`CataloguePage.test.tsx`, the detail page test, a unit test of `genres.ts`). jsdom computes no layout, so the footer
  is pinned at the source level: the CSS contract style of `frontend/src/styles/styleContract.test.ts` (rules read from
  disk) plus a DOM test of the shell element in `frontend/src/App.test.tsx`.
- Test files and command: `vitest run src/pages/CataloguePage.test.tsx src/pages/BookDetailPage.test.tsx
  src/genres.test.ts` (T1) and `vitest run src/styles/styleContract.test.ts src/App.test.tsx` (T2), in `frontend/`
  with the pinned Node from `frontend/node`.

## Root cause
- Causal chain A: a raw enum name on screen -> the components print the wire value (`SearchForm.tsx:59`,
  `BookCard.tsx:15`, `BookDetailPage.tsx:53`) -> **root cause: the storefront has no genre label mapping
  (`frontend/src/genres.ts:6-17` holds only the enum names)** -> the gates missed it because spec 12 named no label, so
  the tests pinned the wire value as the intended behaviour.
- Root cause A (one sentence): `genres.ts:6-17` offers only the enum names, so every view that shows a genre prints
  the value the bookstore sends.
- Causal chain B: the footer floats on a short page -> nothing makes the content area fill the window -> **root cause:
  the app has no shell layout (`frontend/src/App.tsx:68-75` renders header, main and footer as bare siblings and
  `frontend/src/styles/global.css` has no rule giving a shell `min-height: 100vh` with a growing main)** -> the gates
  missed it because spec 12 did not state where the footer sits and jsdom tests compute no layout.
- Root cause B (one sentence): without a full-height flex column around header, main and footer, the footer is laid
  out directly after the main content.
- Contributing factors: none beyond the missing requirement, which D11 now states.
- Introduced by: spec 12 (`agent-os/specs/2026-11-08-2145-12-storefront/`), which intended to show "every value as the
  bookstore sent it" (comment of `BookCard.tsx:5-6`).

## Blast radius
- [x] Same pattern elsewhere: `grep -rn "book.genre\|{genre}" frontend/src/pages frontend/src/components` hits
  `SearchForm.tsx:59`, `BookCard.tsx:15` and `BookDetailPage.tsx:53` -> all three in task T1 (D11: "the shop never
  shows a raw enum name", so the detail page is in scope, plan.md D2). `ProblemDetailAlert.tsx:13` shows the server's
  genre list -> deferred: D11 says the unknown-genre alert keeps the list the bookstore sends.
- [x] Other callers: `GENRES` is used only by `SearchForm.tsx`; the order, basket, checkout and assistant pages show
  no genre (grep above) -> nothing further.
- [x] Persisted data already wrong: none, display only.
- [x] Artifacts already produced: none.
- [x] Tests that encode the wrong assumption: `CataloguePage.test.tsx:10` (comment), `:118-127`, `:196-214`;
  `BookDetailPage.test.tsx:71-88` (`TECHNOLOGY`) and `:90-99` ("the genre as sent") -> updated to the D11 behaviour in
  T1 (plan.md D3); no other pre-existing test changes. Footer: no test encodes the floating footer -> nothing.
- [x] Sibling environments: the bookstore test copy `bookstore/src/test/resources/static/shop/index.html` holds no
  genre and no footer markup -> nothing.

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | one label map in `genres.ts` used by all three views; one shell element plus a sticky-footer rule |
| (b) revert | no | not a regression; the requirement is new (D11) |
| (c) guarded workaround + follow-up | no | nothing to guard; S4 |
Chosen: (a), because the cause is local, readable and owned by the frontend module.

## Prevention
- Gap: spec 12 named neither genre labels nor the footer position; jsdom tests cannot see layout.
- Action: the CSS contract test of T2 pins the sticky-footer rules at the source level. Follow-up for Maik (not a task):
  a live screenshot walk of the catalogue and of a short page is the only check of the real layout.

## Verification
- Regression tests red on unfixed -> green on fixed (impl `## Red evidence`, QA `## Negative control`)
- Blast-radius items closed or deferred
- `./mvnw clean verify` green within 60 seconds
