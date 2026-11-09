# Shape: 12-storefront (OpenBook)

## Source

The pack spec `docs/spec-pack/specs/12-storefront.md` is the source of truth, as of factory f6ce0d7 (it includes
decision D7: the shop is called OpenBook, OpenValue 2026 colours, Montserrat, screenshots in the Definition of done).
This folder restates it as tasks; it never changes requirements, scenarios, paths, names or versions.

## Scope

- New Maven module `frontend/` (Vite react-ts, Vitest, React Testing Library), built by frontend-maven-plugin 2.0.2
  with Node.js 26.13.0 installed into `frontend/node/`. No global Node.js.
- The bookstore serves the build under `/shop/` (one `StorefrontConfig` in the root package, a resource mapping in
  `bookstore/pom.xml`); `/` redirects to `/shop/`.
- Seven pages: catalogue, book detail, basket, checkout, order, order history, assistant.
- OpenBook branding (D7): tokens in `src/styles/tokens.css`, self-hosted Montserrat, contrast test, screenshots.
- Root `pom.xml` module list and the CI cache for `frontend/node` and the npm cache.

Out of scope: everything the pack spec lists as out of scope, any change to `*Commands`/`*Queries` classes or other
bookstore packages (specs 06 to 08 change those in parallel), ArchitectureTest.

## Context of this run

- Runs in parallel with specs 06 to 08 in an isolated worktree. Spec 08 (assistant) is not built: the assistant page
  is built against the spec 08 contract text (`POST /assistant/chat`, `ChatPayload`, `AssistantReplyView`, 503
  `assistant-offline`, 429). Frontend tests mock `fetch`. The live walk-through of the assistant page is a manual
  check after spec 08 merges and is never faked.
- The main session merges the spec 12 PR as soon as it is ready (before spec 08); `spec-12-done` is tagged only after
  spec 08 is merged and the assistant page is walked live (progress.md, 00c515b).
- Pack task 9 says "record the time in progress.md": the main session owns that file in this run. The time goes into
  `verification-record.md` in this folder and into the spec PR body; the main session copies it.

## Research and strategy (summaries; full artifacts snapshotted here)

- Wire contract (research res-1): money is a decimal string, instants ISO-8601 UTC strings, `BookView.authors` one
  joined string, `BookDetailView.authors` an array, `OrderSummaryView.itemCount` a number, place answers 202 `{id}`,
  `GET /orders` newest first server-side. ProblemDetail carries top-level `code` and `details` (array of flat string
  objects): `insufficient-stock` `[{bookId, requested, available}]` with string values, `unknown-genre` `[{genre}]`,
  `validation-failed` `[{field, message}]` (fields may repeat). Framework errors (non-UUID path, malformed JSON, 405,
  static 404, probably the spec 08 429) are plain Boot JSON without `code`.
- Build wiring (research res-2): all pins exist and install together; the plugin skips an `npm` execution bound to
  phase `test` under `-DskipTests` by itself; a bare `<resources>` block drops the Spring Boot parent's entries.
- Strategy: `strategy.md` (tech lead, approach A: infrastructure, two foundations, pages, verification).
- QA strategy: `qa-strategy.md` (QA lead: tiers, common directive C0, per-task blocks with falsifier and probe lists,
  Gate 1 findings GQ1 to GQ14, traceability plan).

## Decisions (self-accepted by the orchestrator, logged in plan.md)

1. No react-router: it is not in the tech stack. An in-house `HashRouter` in `src/router.tsx` keeps the spec's name.
2. oxlint from the create-vite template is removed (not in the tech stack, not run by the referee).
3. Resources: the two Spring Boot parent resource entries are re-declared, plus an unfiltered entry for
   `../frontend/dist` with target path `static/shop` (spec wording; probe-verified).
4. No Maven dependency from the bookstore to the frontend (it would break `./mvnw -pl bookstore spring-boot:run`
   before a full build). Under `-fae` (CI) the bookstore still builds after a red frontend test; the build still fails.
5. `npm test` is skipped under `-DskipTests` by binding the execution to phase `test` (the plugin's own handling,
   verified) instead of the spec's literal "bind skip to ${skipTests}", which the spec marks verify at build time.
6. Header (D7 inconsistency, OQ-1): the token table puts navy on the header background while the wordmark's "Open" is
   navy. Chosen: a light header (white surface, navy bottom border), wordmark exactly as specified ("Open" navy,
   "Book" blue), navy carries the footer. The wordmark's blue on white (4.22:1) is a logotype, exempt under WCAG 2.2
   SC 1.4.3, and the contrast test carries that one exemption explicitly (GQ3). Flagged for Maik.
7. Basket badge counts items (sum of quantities, the server's `itemCount` meaning) (GQ11).
8. Live runs never use `docker compose`: the compose project `bookstore` is shared by every checkout and holds spec
   06's database. T10 runs the jar against a throwaway Postgres container (GQ1).
9. All QA Gate 1 findings GQ1 to GQ14 accepted.

## Follow-ups for the main session

- OQ-2: the spec's `frontend/node/npm run dev` fails without a global Node.js; it needs `frontend/node` on PATH.
  `frontend/README.md` documents the PATH form; the pack spec and the spec 11 guide should use it.
- OQ-4: spec 08 body shapes (429, 503 title and detail, 400 for an over-long message) are spec text only; re-check
  them in the live assistant walk-through. Spec 08's exception handler must stay package-scoped, or static 404s turn
  into ProblemDetails.
- GQ10: real servers send an absolute `Location`; propose the spec wording "a `Location` that resolves to `/shop/` on
  the same origin".
