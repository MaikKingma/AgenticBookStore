---
agent: lead-1
task: lead-1
status: completed
spec: docs/spec-pack/specs/12-storefront.md
updated: 2026-11-08T20:40:00Z
---

# Strategy: spec 12 storefront (OpenBook)

## TL;DR

Ten tasks, one module each, strictly linear on the frontend: T1 bookstore `StorefrontConfig` plus the pom resource
mapping (re-declare the two Boot parent entries plus the unfiltered dist entry); T2 frontend module that builds on
its own (`./mvnw -f frontend/pom.xml verify`); T3 root module list plus CI caches (first reactor build with the
frontend); T4 styling foundation (tokens.css, self-hosted Montserrat, global stylesheet, contrast test and a style
contract test that turns the colour-literal grep into a test); T5 API client; T6 shell, in-house `HashRouter`,
header wordmark, catalogue; T7 basket state plus book detail plus header badge; T8 basket, checkout, order, orders
pages; T9 assistant page; T10 root verification (timed referee without global Node, red-test probe, live walk-through,
14 screenshots in the spec folder `visuals/`, record file). Key decisions: no react-router (in-house `HashRouter`
on `useSyncExternalStore`, keeps the spec's name), oxlint removed, no bookstore-to-frontend Maven dependency
(it would break `./mvnw -pl bookstore spring-boot:run`), `StorefrontConfigTest` extends `IntegrationTestBase`
(no new context, real forward, real actuator), blue is never used for text except the wordmark so every text pair
meets 4.5:1 literally. One spec inconsistency needs an answer before T6: the header is navy but the wordmark's
"Open" is navy (invisible). memory: written in worktree.

## 1. Problem Analysis

**What is being built.** A React 19 + Vite 8 storefront in a new Maven module, built by frontend-maven-plugin with
a Maven-installed Node.js, packaged into the bookstore jar under `/shop/`, plus one root-package Spring config class.
Seven pages, OpenBook branding (D7), no business rules in the browser, all inside the 60 s referee.

**UX.** The shop is a teaching surface: Level 0 needs a broken discount to be obvious, so every server figure
(line totals, order total) must be shown verbatim, never recomputed. Errors must always render, including the
framework errors that are not ProblemDetails (plain Boot JSON for non-UUID paths, 405, the probable spec 08 429).
Pre-checkout totals are estimates and must say so. Accessibility: labelled inputs, `role="alert"` for problems,
visible focus, reduced motion, AA contrast; RTL queries by role and label enforce most of this for free.

**Architecture.** Frontend layers per the spec structure: `api/` (transport and error normalisation only),
`state/` (basket context, conversation id), `components/`, `pages/`, `App.tsx` (routes). Additions that do not
change listed names: `src/router.tsx` (in-house `HashRouter`, `Link`, `useRoute`, `navigate`), `src/limits.ts`
(the three mirrored limits), `src/state/estimate.ts` (the only money arithmetic), `src/format.ts` (instant
formatting), `src/styles/` (tokens, fonts, global), `src/assets/fonts/`, `src/test/` (fetch mock helpers). Backend:
one `@Configuration(proxyBeanMethods = false)` view-controller class in the root package (ClockConfig precedent),
passes ArchitectureTest unchanged (res-1 section 4).

**Testing.** Vitest + RTL with `fetch` mocked by real `Response` objects, so content type and body parsing are
exercised. No wall clock, no timers, no network. Witness data with distinct values so pass-through and ordering
claims are falsifiable. Caps tested on both sides. Bookstore: one test class on the shared full context.

**Performance.** Referee was 22 s; frontend module is about 3 s warm in the probe, estimate 5 to 10 s with about
60 tests. Over budget is OQ4, never a reason to trim tests.

**Security and data integrity.** Same origin, no CORS (and the word "cors" must not appear anywhere in
`bookstore/src` or `frontend/src`, including fixture data). No CDN at runtime (fonts self-hosted). Exact pins plus
committed lockfile plus `.npmrc save-exact=true`; no install scripts approved. Double-submit protection on checkout
and chat. Secrets: nothing in the frontend reads or names an API key.

**Maintainability.** The code ships to workshop participants on `main`, where `docs/spec-pack/`, `agent-os/`,
`.claude/` and `CLAUDE.md` are stripped: comments must never cite those paths. Limits cite the owning spec number
in words plus the server class or property that enforces the rule.

## 2. Recommended Approaches

### A. Linear vertical build on a styling and transport foundation (recommended)

Philosophy: infrastructure first (bookstore serving, frontend module, reactor and CI), then two foundations
(styles with their contract tests, API client with its parsing tests), then pages in the order their state
dependencies appear (catalogue, then basket state with its first consumer the detail page, then checkout and
orders, then assistant), then one verification task. TDD per page: write the spec scenario test with the witness
fixture first, watch it fail, implement.

- Pros: every page task starts with tokens, alert and client in place, so no restyling or re-plumbing; each task
  is reviewable in one sitting; every frontend task after T3 is proven by the root referee.
- Cons: ten tasks and ten QA rounds; the frontend tasks cannot overlap (same module).
- Excels when: tasks run one at a time anyway (they do here), and QA rework is the main cost driver.
- Risks: the contrast pair list drifts as pages add colours; mitigated by auto-extracting pairs from CSS rules.

### B. Big-bang frontend in two tasks (pack grouping, minimal splitting)

Pack tasks 5 to 8 as four tasks, styles folded into the first page task.

- Pros: fewer handoffs.
- Cons: 1500+ line diffs per task, styling decided ad hoc by the first page, the basket "Add to basket" button on
  the detail page would be a stub until task 7, and QA rounds get expensive.
- Risks: high rework, inconsistent styling, colour literals leaking before the guard test exists.

### C. Design-system-first spike

Time-boxed spike that builds a static style guide page with every component state, then the pages.

- Pros: visual consistency proven early; screenshots are easy.
- Cons: a page that ships but is not in the spec (or throwaway work); the spec forbids UI libraries, so the
  "system" is just CSS classes that approach A builds in T4 anyway.
- Risks: scope creep.

**Choice: A.** Methodology: TDD with scenario-first tests and named falsifiers; characterization-style probes for the
build wiring (observable Maven log lines and files as acceptance); uncommitted probes for "throwaway branch"
scenarios.

## 3. Strategic Implementation Plan

### Phase 1: serving and build wiring (T1, T2, T3)

- T1 and T2 are independent; T3 joins them (module list, CI caches, first reactor build with the frontend).
- Checkpoint after T3: `./mvnw clean verify` green with `Reactor Build Order` frontend, bookstore, distributor;
  `bookstore/target/classes/static/shop/index.html` and `application.yml` both present; CI green on Linux and
  Windows with the frontend; referee time noted (early budget signal for OQ4).
- Success: the empty scaffold is served at `/shop/` from the jar.

### Phase 2: foundations (T4, T5)

- T4 styles: tokens, fonts, global stylesheet primitives, contrast test, style contract test, document title.
- T5 API client and typed endpoint functions with exhaustive error parsing tests.
- Checkpoint: contrast test fails when a token is changed to a low-contrast value (falsifier run recorded); client
  tests cover every status the spec names plus the non-ProblemDetail shapes.

### Phase 3: pages (T6, T7, T8, T9)

- T6 shell and catalogue; T7 basket state and detail; T8 basket, checkout, order, orders; T9 assistant.
- Checkpoint per task: every spec scenario of the task maps to a named test; the style contract test still green;
  any new text/background combination appears in the contrast pairs.
- Success: all spec frontend tests exist with the names of the spec's Tests section.

### Phase 4: verification and evidence (T10)

- Timed referee from a clean state without global Node, red-test probe, `-DskipTests` probe, live API and dev-proxy
  checks, browser walk-through, 14 screenshots, record file.
- Success: every Definition of done line has evidence in the record file and the PR body.

### Decisions (binding for the implementers)

1. **Resources (T1).** Re-declare the two `spring-boot-starter-parent` 4.1.2 resource entries exactly (res-1
   section 6 XML) and add the third, unfiltered entry `${project.basedir}/../frontend/dist` with targetPath
   `static/shop`. Why: it is the spec's literal wording ("adds a resource ... target path"), and both the present and
   the missing-directory cases are verified by probe (res-1). Copy-resources (res-2) is the fallback only if the
   re-declared form misbehaves; its missing-directory case is not verified by both researchers. A pom comment
   explains in plain words why the parent entries are repeated.
2. **No Maven dependency from bookstore to frontend.** Why: with an edge, `./mvnw -pl bookstore spring-boot:run`
   before any full build would fail to resolve `nl.openvalue:frontend`, contradicting the spec ("`/shop/` answers
   404 and the REST API works unchanged"). Cost: under `-fae` (CI) the bookstore still builds after a red frontend
   test; the build still ends red. The spec scenario uses plain `./mvnw verify` (fail-fast), where the bookstore is
   skipped. Recorded as informational OQ.
3. **`npm test` skip under `-DskipTests`.** Bind the `npm test` execution to phase `test`; the plugin's own
   `skipTests` handling skips it (res-2, verified in source and probe). Do not add `<skip>${skipTests}</skip>`
   (`skip` is the `skip.npm` switch). The spec's "verify at build time" clause covers this; the PR states the
   verification.
4. **Module boundaries.** `frontend/pom.xml` builds standalone in T2 with `-f`; the root module list is the root task
   T3 (pack task 2 is exactly root pom plus CI). This keeps one module per task.
5. **StorefrontConfigTest** extends `IntegrationTestBase` (option B of res-1): real server, real forward to the test
   `index.html`, real actuator, zero new Spring contexts. `Location` is asserted by resolving it against the base
   URL (works whether Tomcat writes it relative or absolute); the literal header value is recorded live in T10.
6. **Router.** In-house `HashRouter` component in `src/router.tsx` (the spec's `App.tsx (HashRouter, routes)` keeps
   its name), built on `useSyncExternalStore` over `hashchange`; no module-level mutable state. react-router is not
   in the tech stack and the route table is seven fixed patterns. Not an open question.
7. **oxlint removed** (dependency, `lint` script, config file). Why: not in the tech stack, not run by the referee so
   it would rot, adds platform-native binaries to the lockfile; `tsc -b` in strict mode already gates the build. PR
   records the removal and the create-vite version.
8. **Styling.** One global stylesheet plus tokens plus fonts (no CSS modules): one place to grep, simple class names.
   Blue is used for the wordmark (logotype), focus ring, active-nav indicator, borders and accents only, never for
   text, so the literal "each pair at least 4.5:1" holds for every text pair; non-text pairs (focus ring) are checked
   at 3:1 in the same test as an extra.
9. **Fonts.** `src/assets/fonts/montserrat-latin-{400,600,700}-normal.woff2` plus `OFL.txt` in the same folder,
   `@font-face` in `src/styles/fonts.css` with relative `url()` so Vite emits `/shop/assets/...` hashed files.
   Downloaded once (curl) from a pinned source, checked for the `wOF2` magic bytes, source URL and SHA-256 per file in
   the PR (not in the repo, so the CDN grep stays clean). If the files cannot be obtained, the fallback stack only,
   said in the PR (spec rule).
10. **Dates.** Instants are formatted from the ISO string by a pure helper (`2026-06-15T10:00:00.123456Z` and
    `2025-12-24T00:00:00Z` both become `2026-06-15 10:00 UTC` style text in a `<time dateTime>`), no `Date`, no
    locale, no time zone dependency.
11. **Currency.** Book and order views carry `currency`; render it. `OrderSummaryView` has none: a constant `EUR`
    with a comment (spec 02 fixes EUR).

## 4. Quality Gates

Ground rules for every frontend task card (QA checks each):

- G1 Scenario map: every spec scenario of the task maps to a named test (listed in the card); spec test file names
  exactly as in the spec's Tests section.
- G2 Witness data: distinct values per field; ordering claims use server data that matches no client-side sort;
  pass-through claims use server figures a client recomputation would not produce.
- G3 Caps on both sides: quantity 0/1/10/11, lines 20/21, message 1000/1001.
- G4 Error coverage: every status the spec names has a test; `code` parsing tested; non-ProblemDetail bodies render.
- G5 No clock, no randomness in tests: `grep -rnE "Date\.now|new Date\(|setTimeout|setInterval|performance\.now|Math\.random" frontend/src`
  finds nothing (the conversation id uses `crypto.randomUUID`, which tests stub or pre-seed).
- G6 No shared mutable module state: `grep -rnE "^(export )?let " frontend/src` finds nothing.
- G7 No business rule: `parseFloat` nowhere; `Number(`/`parseInt` only in `state/estimate.ts` and the quantity
  input; no `isbn` logic beyond display and types; no `onHand`/`reserved` outside `api/` types; mirrored limits only
  in `src/limits.ts`, each with a comment naming the owning spec in words and the enforcing server class or property.
- G8 Styling: the style contract test (T4) stays green; new text/background combinations are covered by the contrast
  test; no inline `style` colour; no `document.title` writes.
- G9 Hygiene: `grep -ri cors bookstore/src frontend/src` empty; byte-level `grep -rnP "[^\x00-\x7F]"` over every
  written text file (not binaries, not `package-lock.json`) empty; no em dash; no `docs/spec-pack`, `agent-os`,
  `.claude` or `CLAUDE.md` mentions in `frontend/`, `bookstore/`, poms or CI; no `ANTHROPIC_API_KEY`.
- G10 Build: `tsc -b` strict passes (no `@ts-ignore`, no `any` in `src` outside test helpers), `package.json` has no
  `^` or `~`, lockfile committed, `./mvnw clean verify` green and its time noted in the task outbox.
- G11 Accessibility: inputs labelled, alerts `role="alert"`, chat replies in an `aria-live="polite"` region, every
  page has one `h1`, disabled buttons have a visible reason next to them.

Bookstore gates: ArchitectureTest file untouched (`git diff` empty for it); `*Commands`/`*Queries` and every layer
package untouched (`git diff --stat` against the spec branch base lists only `StorefrontConfig.java`,
`StorefrontConfigTest.java`, the test `index.html` and `bookstore/pom.xml`); Surefire shows the same number of Spring
"Started" lines as before T1 (no new context).

Documentation: `frontend/README.md` (plain English, ASCII) with the dev command including the PATH prefix for macOS,
Linux and Windows, the test and build commands, and a note that tokens live in `src/styles/tokens.css`. No ADR
needed: decisions are spec-level and recorded in this strategy and the PRs.

## 5. Agent Coordination

- Instances: one implementer per task in sequence (`impl-t1` ... `impl-t10`), one verifier per task (`qa-t1` ...
  `qa-t10`), `qa-lead` for the ledger. T10 needs the live-run slot (one live bookstore across worktrees; spec 06 runs
  in the main checkout) and a browser tool.
- Channels: `impl-tN -> qa-tN` (handoff), `qa-tN -> impl-tN` (verdict), everyone `-> orchestrator`. No peer channels
  between implementers are needed (linear plan; artifacts flow through merged code).
- Batches (topological, disjoint modules): B1 {T1 bookstore, T2 frontend} (may run in parallel if the orchestrator
  allows, else T1 then T2), B2 {T3 root}, B3 {T4}, B4 {T5}, B5 {T6}, B6 {T7}, B7 {T8}, B8 {T9}, B9 {T10}. T9 depends
  only on T7 (limits file, shell); it still runs after T8 because both are frontend.
- Handoff artifacts: each implementer's `outbox/task-tN.md` with scenario-to-test map, commands and output excerpts;
  T3 additionally links the CI run URLs; T10 writes the record file in the spec folder and the `visuals/` PNGs; the
  orchestrator copies the referee time to progress.md via the main session.
- Spec 08 hand-off: after spec 08 merges, the main session runs the live assistant walk-through and checks the 429 and
  503 body shapes against T9's fixtures (OQ-4).

## 6. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Bare `<resources>` drops `application.yml` and Flyway migrations | Three-entry block; T1 criteria check `target/classes` for `application.yml`, `db/migration` and the dist probe file; full-context tests fail otherwise |
| Lockfile lacks Linux or Windows native bindings (npm optional-dependency bug), `npm ci` fails in CI | Generate the lockfile from a clean state (no `node_modules`, no old lockfile); grep it for the linux-x64 and win32-x64 rolldown bindings; CI on both OSes in T3 is the falsifier |
| `frontend/node/npm run dev` fails without global Node (`env: node`) | README documents the PATH prefix; spec wording raised as OQ-2 |
| Vitest scans `frontend/node/**` | `test.include: ['src/**/*.test.{ts,tsx}']` |
| `tsc -b` writes `tsconfig.tsbuildinfo` into `frontend/` | Keep the template's `tsBuildInfoFile` under `node_modules/.tmp` |
| Number input with `maxLength`/clamping makes "refuse 0 or 11" untestable | Controlled input that ignores out-of-range values and shows a message; tests use `fireEvent.change` with exact values |
| Chat textarea `maxLength` makes the 1001-character scenario impossible | No `maxLength`; counter plus disabled send |
| Browser `type="email"` validation blocks the server's validation-failed path | `<form noValidate>` |
| `URLSearchParams` encodes `@` as `%40`, so a literal URL assertion fails for e-mails | Assert the parsed URL's `searchParams.get('customerEmail')`; the catalogue search keeps the exact `'/books?q=austen&genre=FICTION'` assertion |
| Relative API paths resolve under `/shop/` | Every API path starts with `/`; client test asserts the exact path |
| Colour literal for shadows or overlays sneaks in | Shadow and error colours are tokens; the style contract test scans all CSS and TS sources |
| Header navy plus wordmark "Open" in navy is invisible | OQ-1 before T6; default below |
| Live checks collide with other worktrees (port 8080, compose project `bookstore`) | T10 asks the orchestrator for the live slot; `docker compose down` afterwards |
| No browser tool for walk-through and screenshots | Preference order in T10 directive; escalate if none (OQ-3) |
| Referee over 60 s | Measure in T3 and T10; OQ4 if over; never trim tests |
| Write tool decodes backslash-u escapes in test data | Build such strings from code points; byte-level ASCII grep (G9) |
| Substring DoD grep `cors` matches fixture text (for example a "Scorsese" author) | Fixture data avoids the substring; G9 runs the exact DoD grep |

## Task breakdown

Every task: branch cut from the spec branch tip, Conventional Commits, `./mvnw clean verify` green with the time
noted in the outbox (except where stated), G9 hygiene greps clean.

### T1 (bookstore): StorefrontConfig, its test, resource mapping

- Summary: pack task 1. Root-package view controllers; `bookstore/pom.xml` resource mapping of `../frontend/dist`
  to `static/shop`.
- fileScope: `bookstore/src/main/java/nl/openvalue/bookstore/StorefrontConfig.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java`,
  `bookstore/src/test/resources/static/shop/index.html`, `bookstore/pom.xml`.
- module: bookstore. dependsOn: [].
- Acceptance criteria:
  1. `StorefrontConfig` is `@Configuration(proxyBeanMethods = false)`, implements `WebMvcConfigurer`, imports only
     Spring context and `web.servlet.config.annotation` types; registers redirect `/` to `/shop/` and forwards
     `/shop` and `/shop/` to `/shop/index.html`. No `@Controller`.
  2. `StorefrontConfigTest extends IntegrationTestBase` adds no context configuration and has one test per behaviour:
     `rootRedirectsToShop` (302, `Location` resolved against the base URL equals `<base>/shop/`),
     `shopWithSlashServesIndexHtml` and `shopWithoutSlashServesIndexHtml` (200, `text/html`, body contains
     `id="root"`), `booksIsNotRedirected` (200, JSON content type, no `Location`), `healthIsNotRedirected` (200,
     status `UP`, no `Location`).
  3. Test resource `static/shop/index.html` contains `<div id="root"></div>`.
  4. `bookstore/pom.xml` holds the two re-declared parent resource entries exactly plus the unfiltered dist entry,
     with a plain-words comment; no other pom change.
  5. Probe (uncommitted, deleted afterwards): a file `frontend/dist/probe.txt` containing `@project.version@`, then
     `./mvnw -q -pl bookstore process-resources`: `bookstore/target/classes/static/shop/probe.txt` exists with the
     token unreplaced, `application.yml` and `db/migration/` present. Without `frontend/dist` the log shows
     `skip non existing resourceDirectory`. Output excerpts in the outbox.
  6. ArchitectureTest untouched and green; Surefire "Started" line count equal to the pre-task count (state both).
  7. Baseline `grep -ri cors bookstore/src` before and after the change is empty (report the before result; if not
     empty before, raise it, do not edit other files).
- Directive and traps: follow `hexagonal-slice` steps 0 and 8 only (spec says 1 to 7 do not apply) and the
  `layer-testing` row "Root package config". `RestTestClient` must not follow redirects (the 302 assertion proves
  it). Do not put filtering on the dist resource (minified JS contains `@`). Never add an `application.yml` under
  test resources. Do not touch any other bookstore file.

### T2 (frontend): module pom, scaffold, exact pins, Vite and Vitest config

- Summary: pack tasks 3 and 4. A `frontend` module that installs Node, runs `npm ci`, `npm test`, `npm run build`,
  built standalone with `./mvnw -f frontend/pom.xml clean verify` (not yet in the root reactor).
- fileScope: `frontend/pom.xml`, `frontend/package.json`, `frontend/package-lock.json`, `frontend/.npmrc`,
  `frontend/index.html`, `frontend/vite.config.ts`, `frontend/tsconfig*.json`, `frontend/src/main.tsx`,
  `frontend/src/App.tsx`, `frontend/src/App.test.tsx`, `frontend/src/setupTests.ts`, `frontend/src/vite-env.d.ts`
  (if the template has it), `frontend/README.md`, removal of unused template files (assets, counter CSS, lint config).
  `.gitignore` already has the entries (verified by the orchestrator): no change.
- module: frontend. dependsOn: [].
- Acceptance criteria:
  1. `frontend/pom.xml`: packaging `pom`, parent `agenticbookstore-parent`, frontend-maven-plugin 2.0.2,
     `installDirectory` and `workingDirectory` `${project.basedir}`, `nodeVersion` `v26.13.0`, no `npmVersion`;
     executions in order: `install-node-and-npm` and `npm ci` (generate-resources), `npm test` (phase `test`),
     `npm run build` (prepare-package); clean plugin fileset deletes `dist`.
  2. `package.json`: exact pins (no `^`, `~`): react and react-dom 19.3.1; dev: vite 8.3.6, vitest 5.1.0,
     @testing-library/react 16.3.3, @testing-library/jest-dom 7.0.1, @testing-library/user-event 14.6.8,
     @testing-library/dom 10.4.2, jsdom 31.0.0, typescript 7.0.2, @vitejs/plugin-react 6.1.2, @types/node 24.19.2,
     @types/react 19.3.0, @types/react-dom 19.3.0. No other dependency; no oxlint. Scripts `test: vitest run`,
     `build: tsc -b && vite build`, `dev: vite`, `preview: vite preview`. `.npmrc` has `save-exact=true`.
  3. `vite.config.ts`: `base: '/shop/'`; dev server port 5173 with proxy for `/books`, `/inventory`, `/orders`,
     `/assistant` to `http://127.0.0.1:8080`; `test: { environment: 'jsdom', setupFiles: ['./src/setupTests.ts'],
     include: ['src/**/*.test.{ts,tsx}'] }`.
  4. `setupTests.ts` imports `@testing-library/jest-dom/vitest` and in `afterEach` runs `cleanup()`,
     `vi.unstubAllGlobals()`, `vi.restoreAllMocks()`, `sessionStorage.clear()` and resets `window.location.hash`.
  5. A minimal `App.tsx` renders a placeholder inside `<main>`; `App.test.tsx` has one smoke test (so the referee
     proves Vitest runs). `index.html` keeps `<div id="root"></div>`.
  6. Commands with output in the outbox: `./mvnw -f frontend/pom.xml clean verify` green and `frontend/node/node
     --version` prints `v26.13.0`; `frontend/dist/index.html` references `/shop/assets/`; `-DskipTests` run logs
     `Skipping execution.` for the npm-test execution and still builds `dist`; an uncommitted failing test gives
     BUILD FAILURE naming the npm-test execution (removed afterwards, `git status` clean).
  7. `package-lock.json` generated from a clean state and contains the rolldown native bindings for linux-x64 and
     win32-x64 (grep output in the outbox); `npm ci` passes from scratch.
  8. `frontend/README.md` documents `PATH="$PWD/node:$PATH" node/npm run dev` (run in `frontend/`), the Windows
     equivalent, and that the bookstore must run on 127.0.0.1:8080.
- Directive and traps: scaffold once with the Maven-installed npm, PATH prefixed with `frontend/node`
  (`npm create vite@9.3.4 ...` or the equivalent `npx create-vite@9.3.4 --template react-ts`), record the
  create-vite version in the outbox for the PR. Keep the template's `tsBuildInfoFile`. Do not approve install
  scripts; record the fsevents stderr lines that Maven prints as `[ERROR]` (cosmetic) in the outbox. Do not edit the
  root pom (T3). No global Node: never run `node` or `npm` without the PATH prefix.

### T3 (root): module list and CI caches

- Summary: pack task 2. Root `pom.xml` lists `frontend`, `bookstore`, `distributor`; CI caches `frontend/node` and
  the npm cache in both jobs.
- fileScope: `pom.xml`, `.github/workflows/ci.yml`.
- module: root. dependsOn: [T1, T2].
- Acceptance criteria:
  1. Module order exactly `frontend`, `bookstore`, `distributor`; `./mvnw clean verify` green, log shows that reactor
     order, `bookstore/target/classes/static/shop/index.html` and `bookstore/target/classes/application.yml` exist;
     time recorded (first referee time with the frontend).
  2. CI: in each job, after setup-java and before the Maven step, two `actions/cache@55cc8345863c7cc4c66a329aec7e433d2d1c52a9 # v6.1.0`
     steps: `frontend/node` keyed `node-${{ runner.os }}-${{ hashFiles('frontend/pom.xml') }}`; npm cache keyed
     `npm-${{ runner.os }}-${{ hashFiles('frontend/package-lock.json') }}` with restore-key `npm-${{ runner.os }}-`,
     path `~/.npm` on Linux and `~\AppData\Local\npm-cache` on Windows. Everything else unchanged: SHA pins, `-fae`,
     step timeout 10 within job timeout 15 on `verify`, `windows-build` command and timeout.
  3. CI evidence (orchestrator pushes the PR; the implementer or QA reads the logs): both jobs green on the PR run,
     the Linux log shows Vitest ran, the Windows log shows `Skipping execution.` for npm-test and a successful
     `vite build`; the post-job cache steps report saved caches with no "Path Validation Error". Cache-hit evidence is
     collected in T10 (a later run restoring the keys).
  4. No comment in either file cites stripped paths.
- Directive and traps: workflow-file pushes go over SSH (orchestrator). Do not add `restore-keys` to the node cache
  (a stale Node tree is re-extracted by the plugin anyway, but keep the key exact). Do not touch `bookstore/` or
  `frontend/`.

### T4 (frontend): styling foundation (OpenBook tokens, fonts, global stylesheet, contract tests)

- Summary: the D7 foundation every page task uses.
- fileScope: `frontend/src/styles/tokens.css`, `frontend/src/styles/fonts.css`, `frontend/src/styles/global.css`,
  `frontend/src/styles/contrast.test.ts`, `frontend/src/styles/styleContract.test.ts`,
  `frontend/src/assets/fonts/*.woff2`, `frontend/src/assets/fonts/OFL.txt`, `frontend/index.html`,
  `frontend/src/main.tsx` (stylesheet imports), `frontend/src/vite-env.d.ts` (raw import typing if needed).
- module: frontend. dependsOn: [T3].
- Acceptance criteria:
  1. `tokens.css` defines on `:root` exactly the seven tokens with the spec values (`--ob-navy #1D252D`,
     `--ob-blue #3D7DC9`, `--ob-teal #014046`, `--ob-sage #809FA2`, `--ob-mist #CBD9DA`, `--ob-snow #F1F5F6`,
     `--ob-white #FFFFFF`) plus extra tokens for the error text and error background and the shadow colour (all
     colour values of the app live here).
  2. `contrast.test.ts` reads `tokens.css` as raw text (Vite `?raw`), implements the WCAG 2.2 relative luminance and
     contrast formula, and asserts: (a) calibration: white on navy, blue on white rounds to 4.2, sage on white is below
     4.5 (proves the checker can fail); (b) every text pair is at least 4.5:1, where the pairs are the union of an
     explicit list (inherited combinations: navy on snow, navy on white, white on navy, white on teal, teal on white,
     teal on snow, navy on mist, error text on error background) and the pairs auto-extracted from every CSS rule in
     `src/styles/*.css` that declares both `color` and `background`/`background-color` with `var(--ob-*)`; (c) the
     focus ring colour against each surface it appears on is at least 3:1 (non-text); (d) anti-vacuity: at least 8
     explicit pairs and at least 1 auto-extracted pair.
  3. `styleContract.test.ts` scans every `src/**/*.{css,ts,tsx}` file (raw glob, excluding itself and
     `tokens.css`) and fails on hex colour literals, `rgb(`, `rgba(`, `hsl(` and named colour keywords in CSS
     colour properties; fails when a `var(--ob-...)` reference names an undefined token; fails when a stylesheet uses
     `transition` or `animation` without a `prefers-reduced-motion: reduce` block; asserts `:focus-visible` styling
     exists. Includes one fixture string per rule proving it fails.
  4. Fonts: three latin woff2 files (400, 600, 700) start with the `wOF2` magic bytes; `OFL.txt` next to them;
     `fonts.css` has three `@font-face` rules with `font-display: swap` and relative `url()`; body font stack
     `Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`. `dist/assets` contains the woff2 files
     after the build. If the files cannot be obtained: no `@font-face`, fallback stack only, outbox says so (PR note).
  5. `global.css` is mobile-first: base single column, a `min-width` media query switches the book grid to multiple
     columns; primitives for sticky header (`position: sticky`), cards (rounded corners, soft shadow via token),
     primary button (white on teal), links (teal), badges (in-stock white on teal, out-of-stock navy on mist), alert
     (error tokens), visible `:focus-visible` ring in blue, reduced-motion block.
  6. `index.html`: `<title>OpenBook</title>`, `lang="en"`, `<div id="root"></div>`, no external URLs; a test reads it
     raw and asserts the title and the root element.
  7. `grep -rniE "googleapis|gstatic|jsdelivr|unpkg|fontsource" frontend/src frontend/index.html` empty.
- Directive and traps: blue never as text colour (wordmark excepted, it is built in T6 as a logotype). Do not add
  CSS modules or any npm package. Font download: curl from a pinned, versioned URL into a scratch directory, verify
  bytes, then copy; SHA-256 and URL go in the outbox for the PR, not in the repo. Binary files are excluded from the
  ASCII grep. Vitest does not process CSS; raw imports are the way to read it.

### T5 (frontend): API client, ProblemDetail parsing, endpoint functions

- Summary: pack task 5.
- fileScope: `frontend/src/api/client.ts`, `frontend/src/api/types.ts`, `frontend/src/api/books.ts`,
  `frontend/src/api/inventory.ts`, `frontend/src/api/orders.ts`, `frontend/src/api/assistant.ts`,
  `frontend/src/api/client.test.ts`, `frontend/src/test/fetchMock.ts`.
- module: frontend. dependsOn: [T3].
- Acceptance criteria:
  1. Types follow the wire contract (res-1 section 1): money and instants are `string`, `BookView.authors` string,
     `BookDetailView.authors` string array, `shippedAt: string | null`, details `Array<Record<string, string>>`,
     status and genre unions.
  2. `client.ts` exposes `getJson` and `postJson` and an `ApiError` carrying a normalised `Problem`
     `{ status, title, detail?, code?, details }`. Rules: a non-2xx JSON body with `code` or content type
     `application/problem+json` maps its `title`, `detail`, `code`, `details` (missing or non-array details become
     `[]`); a JSON body without `code` (Boot error JSON) becomes `title` from `error`, else the status text, else
     `Request failed (<status>)`; a non-JSON or empty body likewise; a rejected `fetch` becomes status 0 "Cannot reach
     the bookstore". 202 bodies parse as `CommandResult`.
  3. `client.test.ts` uses real `Response` objects and covers: 400 validation-failed with an indexed field and a
     repeated field; 404 book-not-found with empty details; 409 insufficient-stock with two shortages of different
     values; 422 unknown-genre with ten `{genre}` objects; 503 assistant-offline; 400 and 429 as Boot error JSON (no
     `code`); a non-JSON 502 body; a network failure; a 200 JSON body; a 202 `{id}`; request shape (method, exact
     same-origin path starting with `/`, `Content-Type` and body for POST, `Accept` header).
  4. Endpoint functions: `searchBooks({q,title,author,genre})` builds the query in the order q, title, author, genre,
     omitting blank values (test: exact `'/books?q=austen&genre=FICTION'`, and `'/books'` for no criteria); `getBook`,
     `getStock`, `getOrder` encode the id segment; `findOrders(email)` (test asserts the parsed `customerEmail`
     parameter equals `reader@example.com`); `placeOrder`, `chat` post the payload unchanged.
  5. `fetchMock.ts` matches by method and path and fails the test on any unmatched request.
- Directive and traps: no retries, no caching, no module-level state. Do not validate UUIDs or e-mails client-side
  (the server is the authority; a hand-edited deep link shows the plain 400 as a generic alert). Key nothing on
  `title` text.

### T6 (frontend): shell, HashRouter, header wordmark, alert, catalogue

- Summary: pack task 6 (catalogue half) plus the app shell.
- fileScope: `frontend/src/router.tsx`, `frontend/src/router.test.tsx`, `frontend/src/App.tsx`,
  `frontend/src/App.test.tsx`, `frontend/src/main.tsx`, `frontend/src/components/ProblemDetailAlert.tsx`,
  `frontend/src/components/ProblemDetailAlert.test.tsx`, `frontend/src/components/SearchForm.tsx`,
  `frontend/src/components/BookCard.tsx`, `frontend/src/components/StockBadge.tsx`,
  `frontend/src/pages/CataloguePage.tsx`, `frontend/src/pages/CataloguePage.test.tsx`, `frontend/src/styles/global.css`
  (additions), `frontend/src/limits.ts` (genre list constant may live in `src/genres.ts` instead).
- module: frontend. dependsOn: [T4, T5].
- Acceptance criteria:
  1. `router.tsx`: `HashRouter`, `Link`, `useRoute`, `navigate`; `useSyncExternalStore` on `hashchange`; patterns
     `#/` (and empty hash), `#/books/:id`, `#/basket`, `#/checkout`, `#/orders`, `#/orders/:id`, `#/assistant`;
     unknown hash renders a "Page not found" view with a link home. `router.test.tsx` covers every pattern,
     `#/orders` versus `#/orders/:id`, decoding of params, unknown routes and re-render on `hashchange`.
  2. Header (sticky) shows the wordmark: a home link whose accessible name is "OpenBook", "Open" and "Book" in
     separate spans styled per OQ-1, Montserrat bold; navigation to Catalogue, Basket, Orders, Assistant with the
     active item marked (`aria-current="page"`). Footer may carry the spec's line. `App.test.tsx` asserts the
     wordmark on two different routes.
  3. `CataloguePage.test.tsx`: listing of three books whose fields all differ (title, authors string, genre, price
     with two different values, currency "EUR"), badge from `inStock` (one fixture has `available 3` with
     `inStock false` and must show out of stock, proving the badge reads `inStock`); search with "austen" and genre
     FICTION calls exactly `/books?q=austen&genre=FICTION` and renders the result; 422 unknown-genre shows the title
     and all ten genres from `details[i].genre`; a card links to `#/books/<id>`; empty result shows a message.
  4. `SearchForm` offers q, title, author and a genre select with an "Any genre" option plus the ten genre names
     (constant with a comment that it mirrors the bookstore `Genre` enum); the last search is kept in
     `sessionStorage` and restored when returning to the catalogue (one test).
  5. `ProblemDetailAlert.test.tsx`: renders title, detail, and details for `{field, message}` (repeated field shows
     both), `{genre}`, `{bookId, requested, available}`, and unknown shapes as key/value text; `role="alert"`; React
     keys are index-based.
- Directive and traps: wait for OQ-1 before styling the wordmark; default if unanswered: white header surface with a
  navy bottom border, navy text and nav, wordmark exactly "Open" navy and "Book" blue (the spec's wordmark wins over
  the token table's "header background" use; navy then carries the footer). Card has no basket button (cards link to
  the detail page). Remove the T2 placeholder test if superseded.

### T7 (frontend): basket state, estimates, limits, book detail, header badge

- Summary: pack task 6 (detail half) and the state part of pack task 7; basket state merges with its first consumer.
- fileScope: `frontend/src/state/basket.tsx`, `frontend/src/state/basket.test.tsx`,
  `frontend/src/state/estimate.ts`, `frontend/src/state/estimate.test.ts`, `frontend/src/limits.ts`,
  `frontend/src/pages/BookDetailPage.tsx`, `frontend/src/pages/BookDetailPage.test.tsx`,
  `frontend/src/components/StockBadge.tsx`, `frontend/src/App.tsx` (route and provider), `frontend/src/App.test.tsx`
  (deep link), header badge component or App additions, `frontend/src/styles/global.css` (additions).
- module: frontend. dependsOn: [T6].
- Acceptance criteria:
  1. `limits.ts`: `MAX_QUANTITY_PER_LINE = 10`, `MIN_QUANTITY_PER_LINE = 1`, `MAX_BASKET_LINES = 20`, each with a
     comment naming spec 04 (orders) in words and the server class that enforces it (`PlaceOrderPayload` for the line
     count, `OrderFlow` for the quantity), saying the server stays the authority.
  2. `basket.tsx`: context plus reducer, lines `{bookId, title, price, currency, quantity}` in insertion order,
     persisted to `sessionStorage` on every change, hydrated lazily; corrupt storage yields an empty basket.
     Actions: add (merge into existing line, +1), setQuantity, remove, clear. Refusals return a message and leave the
     state unchanged.
  3. `basket.test.tsx`: add A twice then set 3 gives one line, quantity 3, line total three times the price (price
     "7.99" gives "23.97"); setQuantity 0 and 11 refused, 1 and 10 accepted; add on a line at 10 refused with a
     message; 20 distinct lines accepted, 21st refused with a message, adding to an existing line at 20 lines still
     works; persistence: two lines survive unmount and a fresh provider, plus a pre-seeded storage case; clear empties
     state and storage.
  4. `estimate.ts` is the only money arithmetic: integer cents from the decimal string, line and basket totals,
     formatted back with two decimals; `estimate.test.ts` covers "7.99" x 3, "12.50" x 2, a basket total over two
     lines with different prices and quantities, and a price without cents digits beyond two ("10.00").
  5. `BookDetailPage.test.tsx`: shows ISBN, authors as a list (two authors), publication year, price with currency,
     "7 available" from `GET /inventory` while `BookDetailView.available` is 4 (proves the stock call is the source),
     enabled "Add to basket"; available 0 shows "Out of stock" and a disabled button; DISCONTINUED with available 5
     shows "Discontinued" and a disabled button; 404 shows the alert; clicking "Add to basket" adds the line and
     shows a confirmation; a refused add shows the basket message.
  6. Header shows a basket count badge (number of items or lines, state which in the outbox) that updates after an
     add; `App.test.tsx` deep link: hash `#/books/<id>` set before render shows the detail page.
- Directive and traps: no stock arithmetic (display `available` as given; out of stock is `available === 0`). No
  module-level state. Use `useReducer` init for hydration (no effect-based double render reading storage).

### T8 (frontend): basket page, checkout, order page, orders page

- Summary: pack task 7 (pages).
- fileScope: `frontend/src/components/BasketSummary.tsx`, `frontend/src/pages/BasketPage.tsx`,
  `frontend/src/pages/BasketPage.test.tsx`, `frontend/src/pages/CheckoutPage.tsx`,
  `frontend/src/pages/CheckoutPage.test.tsx`, `frontend/src/pages/OrderPage.tsx`,
  `frontend/src/pages/OrderPage.test.tsx`, `frontend/src/pages/OrdersPage.tsx`,
  `frontend/src/pages/OrdersPage.test.tsx`, `frontend/src/format.ts`, `frontend/src/format.test.ts`,
  `frontend/src/App.tsx` (routes), `frontend/src/components/ProblemDetailAlert.tsx` (optional book title lookup),
  `frontend/src/styles/global.css` (additions).
- module: frontend. dependsOn: [T7].
- Acceptance criteria:
  1. Basket page: lines with quantity inputs (label "Quantity for <title>"), remove buttons, line totals and an
     "Estimated total", a sentence that the bookstore confirms the total when the order is placed; empty basket
     message with a catalogue link. Test: quantity changes via `fireEvent.change` with 0, 1, 10, 11 (refusal message
     for 0 and 11, value stays), estimates for two lines with different prices and quantities.
  2. `CheckoutPage.test.tsx`: basket 2 x A, 1 x B and "reader@example.com": the posted body parses to exactly
     `{customerEmail, lines:[{bookId:A,quantity:2},{bookId:B,quantity:1}]}` (key sets checked); confirm disabled while
     pending; after 202 `{id}` the basket and storage are empty and the app (rendered through `App`) shows the order
     page with status PLACED, lines with unit price and line total and the total; 409 insufficient-stock keeps the
     basket and lists each shortage with requested and available (two shortages, different values, shown with book
     titles from the basket, falling back to the id); 400 validation-failed shows "customerEmail" next to its message
     (and both messages for a repeated field); a 422 shows the generic alert. Form has `noValidate`; the e-mail is
     sent as typed.
  3. `OrderPage.test.tsx`: renders `OrderView` verbatim: a line with unitPrice "12.50", quantity 2 and lineTotal
     "24.00" shows "24.00" (not "25.00"); total "31.50" while line totals sum to "32.00" shows "31.50 EUR" and not
     "32.00" (proves no recomputation); `placedAt` with microseconds and `shippedAt` null versus set; 404
     order-not-found alert.
  4. `OrdersPage.test.tsx`: three summaries whose server order matches no client sort (not by date, total, id, status
     or item count) render in server order with status, formatted date, item count and total with "EUR", each linking
     to `#/orders/<id>`; the request's parsed `customerEmail` equals the entered value; empty list message; 422
     invalid-email alert with its details.
  5. `format.ts` formats instants from the string only; `format.test.ts` covers fraction, no fraction, and that the
     output does not depend on `process.env.TZ` (run the assertion under two `TZ` stubs or prove no `Date` use).
- Directive and traps: the order page never sums anything; checkout navigates with `navigate('/orders/' + id)`;
  insufficient-stock values are strings, render them as given. Do not sort the orders.

### T9 (frontend): assistant page

- Summary: pack task 8, built against the spec 08 contract text.
- fileScope: `frontend/src/state/conversation.ts`, `frontend/src/state/conversation.test.ts`,
  `frontend/src/pages/AssistantPage.tsx`, `frontend/src/pages/AssistantPage.test.tsx`,
  `frontend/src/components/ChatMessageList.tsx`, `frontend/src/limits.ts` (one constant),
  `frontend/src/App.tsx` (route), `frontend/src/styles/global.css` (additions).
- module: frontend. dependsOn: [T7].
- Acceptance criteria:
  1. `limits.ts` gains `MAX_CHAT_MESSAGE_LENGTH = 1000` with a comment naming spec 08 (assistant) and the property
     `bookstore.assistant.max-message-length`.
  2. `conversation.ts`: reads the id from `sessionStorage`, otherwise creates one with `crypto.randomUUID()` and stores
     it; tests: existing id returned; missing id created once, stored, and the same id on the next call (stub
     `crypto.randomUUID` with a fixed value).
  3. `AssistantPage.test.tsx`: with a pre-seeded id, sending "Do you have anything by Austen?" posts exactly
     `{conversationId, message}`, shows the user message and the reply; a second message reuses the same id; 503 with
     `code` `assistant-offline` shows "The assistant is offline" and the input stays enabled and a following send
     succeeds; 429 with a Boot error JSON body and 429 with a ProblemDetail body both show "Too many requests, try
     again in a minute"; a 503 without that code shows the generic alert; 1000 characters keep send enabled and 1001
     disable it with a counter showing the limit (use paste or `fireEvent.change`, no `maxLength`); empty input keeps
     send disabled; send disabled while pending.
  4. Replies render in an `aria-live="polite"` list; messages are plain text (no HTML injection: a reply containing
     `<b>` renders as text).
- Directive and traps: key the 429 on status only, the offline message on `code` (body shapes unverified until spec
  08, OQ-4). Never mention an API key variable in UI text. The live round trip is a manual check after spec 08
  merges; never fake it.

### T10 (root): referee measurement, probes, live walk-through, screenshots, record

- Summary: pack task 9 plus the D7 screenshots DoD line.
- fileScope: `<spec-folder>/verification-record.md`, `<spec-folder>/visuals/*.png` (14 files). No source change.
  Uncommitted probes only.
- module: root. dependsOn: [T8, T9].
- Acceptance criteria:
  1. Clean-state referee without global Node: delete `frontend/node`, `frontend/node_modules`, `frontend/dist` and
     all `target/` directories (or use a fresh clone the orchestrator provides), then
     `PATH=/usr/bin:/bin:/usr/sbin:/sbin ./mvnw clean verify` green (JAVA_HOME as set); `PATH=/usr/bin:/bin:/usr/sbin:/sbin which node`
     prints nothing; record cold time; then a second warm run, record time (the budget figure); state over or under
     60 s (OQ4 if over).
  2. Red-test probe: an uncommitted failing Vitest test, `./mvnw verify` (no `-fae`) fails in `frontend`, the reactor
     summary shows bookstore and distributor SKIPPED; file removed, `git status` clean. `-DskipTests verify` probe:
     npm-test skipped, `bookstore/target/classes/static/shop/index.html` exists.
  3. Live checks (with the orchestrator's live slot, `docker compose down` afterwards): `curl -i /` shows 302 and the
     literal `Location`; `/shop/` 200 `text/html` with the root element; one `/shop/assets/*.js` and one `.woff2` 200;
     `/books` 200 JSON; `/actuator/health` 200; dev mode: `npm run dev` with the PATH prefix, `/shop/` on 5173 and
     `/books` proxied to JSON.
  4. Browser walk-through of catalogue, search, book detail (deep link `/shop/#/books/<id>`), add to basket, basket
     quantity change, checkout success, order page, insufficient stock (order more than available), orders history;
     assistant page loads (empty state); keyboard focus visible. Results table in the record.
  5. Screenshots: seven pages at desktop (1280 x 800) and phone (390 x 844) width, named `<page>-desktop.png` and
     `<page>-phone.png`, committed in `visuals/`; the assistant page may show the empty or offline state.
  6. Cache-hit evidence from a CI run after T3 (log lines restoring both keys), DoD greps (`cors`, Commands/Queries
     diff empty, ASCII), all recorded. The PR body carries the times and links the screenshots; the main session
     copies the time into progress.md.
- Directive and traps: browser tool preference: (1) a browser automation tool in your toolset, (2) headless Chrome
  driven over the DevTools protocol by a throwaway script run with `frontend/node` from the scratchpad (never
  committed, no npm package), (3) stop and ask the orchestrator (OQ-3). Do not edit progress.md. Do not leave the
  bookstore running.

## Open questions

- OQ-1 (needs an answer before T6): the token table puts navy on the header background, while the wordmark's "Open"
  is navy, which would be invisible on it. Options: (a) light header (white surface, navy border), wordmark exactly
  as specified; (b) navy header with "Open" in white and "Book" in blue (blue on navy is 3.7:1, acceptable for a
  logotype, but changes the specified wordmark colour). Recommendation and default: (a).
- OQ-2: the spec's `frontend/node/npm run dev` fails without global Node (`env: node`); it needs
  `PATH="$PWD/node:$PATH"` in front. T2 documents it in `frontend/README.md`; the pack spec and spec 11's guide
  should use the PATH form (main session's call).
- OQ-3: the browser walk-through and 14 screenshots need a browser tool. Who has one? If no implementer does, the
  main session or Maik performs those two items; T10 still does all curl and dev-proxy checks.
- OQ-4: spec 08 body shapes (429, 503 title and detail, 400 for message) are spec text only; T9 keys on status 429 and
  `code` `assistant-offline`. At spec 08 merge, re-check them in the manual assistant walk-through. Also for spec 08
  shaping: `AssistantExceptionHandler` must stay package-scoped, or it turns static 404s into ProblemDetails.
- OQ-5 (informational): `-DskipTests` uses the plugin's phase-based skip instead of the spec's "bind skip to
  ${skipTests}" wording (verified; the spec marks it verify at build time). Under `-fae` the bookstore still builds
  after a red frontend test; the build still fails. No Maven edge, for the reason in decision 2.
- OQ-6 (informational): the Windows npm cache path is `~\AppData\Local\npm-cache`, not `~/.npm`; T3 caches the native
  path per job, as the ci-performance follow-up said.
- OQ-7: T10 "clean clone": the orchestrator provides a fresh clone, or accepts the clean-state equivalent (delete
  build directories in the task worktree). Record file name `verification-record.md` in the spec folder is my
  proposal.
- OQ-8 (informational): fsevents install-script warnings appear as `[ERROR]` lines in a green Maven build; the plan
  does not approve install scripts. Accept the noise or approve `fsevents@2.3.3`; default: accept.
- OQ4 (budget): measured in T3 and T10.

## Skills used

agent-comms. Read `layer-testing` (root config row) for the T1 test style.

## Resume point

None (strategy complete). Memory: written in worktree (`storefront-design-decisions.md`, one bullet in
`strategy-constraints.md`).
