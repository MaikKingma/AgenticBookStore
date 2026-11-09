# References

- `agent-os/specs/2026-11-08-2145-12-storefront/plan.md`: the spec 12 cycle, its QA feed-forward (the literal C0-6
  grep set for frontend files, fixture rules, the console trap of `setupTests.ts`) and its decisions.
- `agent-os/specs/2026-11-09-1010-fix-12-storefront-review/`: the previous lite bugfix of the storefront (plan
  amendment A1 on the red set of new UI elements).
- `frontend/src/genres.ts`, `frontend/src/api/types.ts` (`Genre`, `BookView`, `BookDetailView`): the genre names.
- `frontend/src/components/SearchForm.tsx`, `frontend/src/components/BookCard.tsx`,
  `frontend/src/pages/BookDetailPage.tsx`: the three places a genre is shown.
- `frontend/src/pages/CataloguePage.test.tsx` (`LISTING`, `genreSelect()`, `mockFetch` routes) and
  `frontend/src/pages/BookDetailPage.test.tsx` (`bookView()`, `routes()`): the fixtures the T1 tests reuse.
- `frontend/src/components/ProblemDetailAlert.tsx` and its test: the unknown-genre alert, which keeps the server list.
- `frontend/src/App.tsx` and `frontend/src/App.test.tsx` ("the shell"): header, main and footer landmarks.
- `frontend/src/styles/global.css` (`.page`, `.site-header`, `.site-footer`) and
  `frontend/src/styles/styleContract.test.ts` (`rulesOf`, `ruleFor`, `declared`): the CSS contract pattern.
