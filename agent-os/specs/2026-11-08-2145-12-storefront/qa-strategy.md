---
agent: qa-lead
task: qa-12-storefront
status: in-progress
spec: docs/spec-pack/specs/12-storefront.md (agent-os spec folder not yet written; task table = lead-1 strategy-12-storefront.md "Task breakdown" plus the orchestrator amendments OQ-1 (a), OQ-3, OQ-7, T10 live isolation, T3 CI evidence, record file, linear order)
updated: 2026-11-08T20:44:26Z
activity: ""
assignment: strategy (delivered)
---

# QA ledger: spec 12 storefront (OpenBook)

## TL;DR

Strategy delivered. Tiers: T5 (API client, the cross-module contract every page's error display rests on) and T8 (checkout payload,
server figures shown verbatim, the Level 0 teaching surface) are critical on opus; T10 is critical on opus because its live run
shares the machine with spec 06 (the compose project `bookstore` resolves to the same `bookstore-postgres-1` container from every
checkout, so one `docker compose down` loses spec 06's database) and its verdict rests on judging 14 screenshots; T1, T2, T3, T4,
T6, T7, T9 are elevated on sonnet. Fourteen Gate 1 findings (section 2): one conflict that must be struck before T10 (GQ1: the
lead card's "`docker compose down` afterwards"), two untestable criteria with rewrites (GQ2 contrast pairs "the stylesheets use",
GQ8 "responsive / visible focus / reduced motion" beyond structure), one spec-literal conflict that needs an asserted exemption
(GQ3: wordmark "Book" blue on white is 4.22:1), a missing guard test for the shared Vitest hooks plus a console trap (GQ4), the
clean-tree wording (GQ5), the base-count source (GQ6), test-case additions for reachable server answers (GQ7), and wording or
pinning items (GQ9 to GQ14). Every task has a T<n>-F falsifier list and a must-fail probe list up front (section 4), because
rework in this repo came from missing falsifiers. memory: written in worktree.

## 1. QA strategy: tiers, models, review depth

| Task | Module | qaTier | qaModel | Review depth (QA lead) | Why |
|---|---|---|---|---|---|
| T1 | bookstore | elevated | sonnet | all lenses | build wiring that can silently drop `application.yml` and Flyway migrations; serving contract of `/`, `/shop`, `/shop/` |
| T2 | frontend | elevated | sonnet | all lenses | new module, exact pins, lockfile natives, shared test hooks every later task relies on |
| T3 | root | elevated | sonnet | all lenses, CI-evidence rules | reactor order and CI caches; evidence is real GitHub Actions runs (memory: qa-ci-config-specs) |
| T4 | frontend | elevated | sonnet | all lenses, lens 4 on the contrast checker | the two guard tests (contrast, style contract) police every later task; a vacuous guard passes everything after it |
| T5 | frontend | critical | opus | all lenses, lens 4 on every criterion | cross-module wire contract and error normalisation used by all seven pages |
| T6 | frontend | elevated | sonnet | all lenses | router and shell for every page, catalogue pass-through and the alert component |
| T7 | frontend | elevated | sonnet | all lenses, lens 4 on estimate and refusals | the only money arithmetic in the browser, client caps, persistence |
| T8 | frontend | critical | opus | all lenses, lens 4 on every criterion | order payload contract, server totals shown verbatim (Level 0 discount must be visible), basket kept or cleared correctly |
| T9 | frontend | elevated | sonnet | all lenses | spec 08 contract is text only; keyed messages and caps |
| T10 | root | critical | opus | all lenses | shared live environment (data-loss hazard to spec 06), clean-state referee, DoD evidence, screenshot judgment |

qa-final: opus (orchestrator policy).

## 2. Gate 1 findings (orchestrator decides before Gate 1)

Each finding is a rewrite or an addition inside the task's own fileScope (tests, test cases, wording); none changes product scope
except where marked.

- **GQ1 (T10 AC3, conflict, must strike before T10).** The lead card says "`docker compose down` afterwards". `bookstore/compose.yaml`
  gives the project name `bookstore` in every checkout, so the container is `bookstore-postgres-1` everywhere, and spec 06 uses it
  now. Any `docker compose` command in any checkout acts on spec 06's database, and `spring-boot:run` (compose support,
  `lifecycle-management: start-only`) would attach the walk-through to it. Rewrite: "No `docker compose` command at all. Start a
  throwaway `docker run --name s12-t10-<role>-pg -p 127.0.0.1:<free port>:5432 ... postgres:18.6`; run `java -jar
  bookstore/target/<jar>` with `--spring.docker.compose.enabled=false` and an explicit `spring.datasource.url/username/password`
  for that container (and `--server.port` only if 8080 is busy); teardown `docker rm -f <name>`. Evidence: `docker inspect -f
  '{{.State.Status}} {{.State.StartedAt}}' bookstore-postgres-1` identical before and after; the bookstore log has no Docker Compose
  line and shows Flyway migrating the throwaway database; ports free before and after."
- **GQ2 (T4 AC2 b, untestable as written).** "Every text pair the stylesheets use" cannot be checked: the auto-extraction sees only
  rules that declare `color` and a background together, and the inherited pairs are a hand list nobody can check against the CSS.
  Rewrite: "Every rule in `src/**/*.css` that declares `color` either declares `background` or `background-color` in the same rule,
  or carries a comment `/* ob-on: --ob-<token>[, --ob-<token>] */` naming the surface(s) it sits on. The contrast test parses both
  forms and FAILS on a `color` declaration with neither (a fixture string proves that failure). The explicit list stays for the
  root and body defaults." From then on a new page's CSS cannot add an unchecked pair.
- **GQ3 (T4 AC2, T6 AC2, spec literal vs OQ-1 (a)).** Wordmark "Book" in `--ob-blue` on the white header is 4.22:1, below the
  spec's literal "each pair at least 4.5:1". WCAG 2.2 SC 1.4.3 exempts logotypes, so "meets WCAG 2.2 AA" holds, but the test must
  carry the exemption explicitly, never by structure (for example a wordmark colour declared without a background so the extractor
  misses it). Rewrite: "The contrast test holds a logotype exemption list with exactly one entry, blue on the header surface,
  asserted at least 3:1; every CSS rule with `color: var(--ob-blue)` has a selector in that list (T4: zero or one such rule;
  from T6: exactly one, the wordmark's)." Judgment call stays logged for Maik.
- **GQ4 (T2 fileScope and AC4, missing guard).** The `afterEach` hooks in `setupTests.ts` are shared test infrastructure for about
  60 later tests; a "remove the hook and see" inspection leaves them unguarded (memory: qa-directive-traps 9). Add
  `frontend/src/setupTests.test.ts` to T2: test 1 dirties sessionStorage, `window.location.hash`, a stubbed global (`fetch`), a
  `vi.spyOn` spy and a rendered tree; test 2 in the same file asserts each one is reset and `document.body` is empty. Also add to
  AC4: "`console.error` and `console.warn` fail the test (spy installed in `beforeEach`, `expect(spy).not.toHaveBeenCalled()` in
  `afterEach`)". Then React key and act warnings and jsdom `Not implemented:` errors fail tests instead of passing as stderr noise,
  which makes "React keys are index-based" (T6 AC5, repeated field) and console hygiene testable.
- **GQ5 (T2 AC6, T10 AC2, wording).** "`git status` clean" after a probe cannot be met before the gate (the tree holds the
  uncommitted work; implementers have read-only git). Rewrite: "`git status --porcelain --untracked-files=all` plus `shasum -a 256`
  of every probe-touched file identical before the first probe and after the last restore."
- **GQ6 (T1 AC6, base).** "Started count equal to the pre-task count" needs a base. The implementer states its own pre-change run;
  the T1 verifier runs the referee once on a `git archive <pr-base>` extraction in the scratchpad. Its Started lines, per-module
  totals, warning inventory and non-ASCII list become the spec 12 baselines in this ledger (frontend tasks never change them).
- **GQ7 (T5 to T9, test-case additions for reachable answers).** Marked [add] in the falsifier lists, each a server answer that
  res-1's status table shows reachable or a known defect class: T5 a second code per status for 404 (`order-not-found`) and 422
  (`invalid-email`) and an e-mail with `+`; T6 a malformed percent escape in the hash; T7 inventory 404 on the detail page; T8 409
  `book-not-orderable` at checkout (same status, empty details), 422 `quantity-out-of-range` as the 422 witness, a Boot-JSON 400
  on the order page, header badge back to empty after checkout; T9 400 `validation-failed` for `message`. If an item is not
  accepted, verifiers report it as informational, never as a FAIL.
- **GQ8 (T10 AC4 and AC5, untestable "mobile-first, responsive, visible focus, reduced motion" beyond structure).** Add live,
  falsifiable checks in the browser: at 390 px on every page `document.documentElement.scrollWidth <= 390`; the book grid's
  computed `grid-template-columns` has one track at 390 px and at least two at 1280 px; after one Tab press
  `getComputedStyle(document.activeElement).outlineStyle !== 'none'`; with reduced motion emulated, an element that declares a
  transition reports computed `transition-duration` `0s`. "Modern", "generous whitespace" and "prominent" stay Maik's judgment
  at Gate 2 from the screenshots (untestable by design, never a verifier guess).
- **GQ9 (DoD "every page works in a browser", assistant).** Cannot be met before spec 08. T10 records "live assistant round trip
  pending the manual check after spec 08 merges" plus what was observed (page loads, empty state, a sent message shows the alert
  for the absent endpoint's 404). Never ENDORSE a faked round trip. Gate 2 residual.
- **GQ10 (spec wording, informational).** Scenario "Root redirects" says `Location: /shop/`; a real server sends the absolute
  `http://127.0.0.1:8080/shop/` (res-1 probe). T1 resolves the header against the base URL, which is right; T10 records the literal.
  Propose spec wording at finalize: "a `Location` that resolves to `/shop/` on the same origin". Also record that the spec's Tests
  line names MockMvc while T1 uses `RestTestClient` on the shared real server (stronger: the forward really happens).
- **GQ11 (T7 AC6, pin before T7).** "Number of items or lines, state which" must be decided in the card: recommendation items (sum
  of quantities, the server's `itemCount` meaning), with the witness "same book added twice shows 2".
- **GQ12 (lead gates G5, G6, wording).** G5 misses `Date.parse`, `Date.UTC`, a `Date` type and locale formatting; G6 misses
  module-level `const` holding a mutable object. Rewrite G5 as `grep -rnE '\bDate\b|performance\.now|Math\.random|setTimeout|
  setInterval|useFakeTimers|toLocale|Intl\.' frontend/src` empty (instants and money are strings, no locale formatting); G6 as
  `^(export )?(let|var) ` empty plus every module-level `const` with an `[`, `{` or `new` initializer read and shown frozen (`as
  const`, `Object.freeze`, never mutated). Accepted, these become gate-proven in every task.
- **GQ13 (T8 AC4 vs spec "newest first", informational).** The server sorts `placedAt desc, id desc` (res-1); the client claim is
  pass-through, so the T8 fixture is in server order that matches no client sort. The matrix records "newest first" as owned by
  spec 04's `OrderQueriesTest`; spec 12 proves no client re-sort.
- **GQ14 (T3, T10, untraced design note).** "Packaged into the bookstore" needs `jar tf` evidence of
  `BOOT-INF/classes/static/shop/index.html` (T3, T10). The design note "without the frontend build output `/shop/` answers 404 and
  the REST API works unchanged" has no criterion: add a T10 live check on a jar built by `./mvnw -pl bookstore -DskipTests clean
  package` after `rm -rf frontend/dist` (`/shop/` 404, `/books` 200, `jar tf` lists no `static/shop/`).

## 3. Common directive C0 (prepended to every task's qaDirective)

```
C0. Ground rules for spec 12-storefront (every task). SP abbreviates the session scratchpad
/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad
and is always written out in full. <n> is the task number. Evidence files are SP/s12-t<n>-qa-... ; helper scripts are
<project-root>/.claude/comms/tmp/s12-t<n>-qa-*.sh. FE = <project-root>/frontend. No global Node.js: every node, npm or npx call
runs inside a script with the literal prefix PATH="<FE>/node:/usr/bin:/bin:/usr/sbin:/sbin" (the path holds spaces: quote it).
1. Referee: preflight `docker info --format '{{.ServerVersion}}'` (quote). From the repo root:
   `bash ~/.claude/scripts/with-budget.sh 600 ./mvnw clean verify > SP/s12-t<n>-qa-referee.log 2>&1`. Report BUILD SUCCESS, the
   reactor summary (from T3: frontend, bookstore, distributor in this order), the `Total time:` line (budget 60 s; over budget is
   an open question to the orchestrator, never a FAIL and never a reason to trim), per-class counts from
   bookstore/target/surefire-reports and distributor/target/surefire-reports, and the Vitest `Test Files` and `Tests` lines (from
   T3 in the referee log; T2 from its `-f frontend/pom.xml` run). Skipped 0 in Java; Vitest skipped and todo 0. Bookstore and
   distributor totals: T1 = base + the StorefrontConfigTest methods; T2 to T10 = T1's verified totals exactly. Vitest total =
   previous gate's total + this task's added tests (show both). The referee runs LAST, after your last probe restore and after the
   impl finished: prove it with `stat -f '%Sm %N'` of the impl artifact, the newest changed product file, your snap-after file,
   one Surefire XML and (from T2) frontend/dist/index.html; never with frontmatter timestamps. Then write repo-relative
   `shasum -a 256 <file>` lines (only such lines) for every fileScope file, binaries included (woff2, png), to
   SP/s12-t<n>-qa-hashes.txt; every round writes its own SP/s12-t<n>-qa-r<k>-hashes.txt, docs-only rounds included.
2. Per-test evidence: once per round run Vitest from FE through a script with
   `vitest run --reporter=verbose --reporter=json --outputFile=SP/s12-t<n>-qa-vitest.json` and cite per-file test counts and
   names from the JSON (numFailedTests 0, numPendingTests 0, numTodoTests 0).
3. Docker or network unavailable: BLOCKED with the output. Never skip or weaken: `grep -rnE '\.(skip|only|todo|fails)\(|\b(xit|
   xdescribe|xtest)\(' frontend/src` empty; `grep -rnE '^\s*@(Disabled|DirtiesContext|ArchIgnore)|assume(True|False|That)'
   bookstore/src/test` empty; never `-DskipTests` for the referee.
4. Probes (production mutations, temporary files): uncommitted, one at a time. Before the first, `cp` every fileScope file and
   every probe-touched file byte for byte to SP as `s12-t<n>-qa-<File>.orig`, and write `git status --porcelain
   --untracked-files=all` plus `shasum -a 256 <every probe-touched file>` to SP/s12-t<n>-qa-snap-before.txt; after the last
   restore write the same to SP/s12-t<n>-qa-snap-after.txt; both identical ("clean" means equal to the snapshot, never an empty
   status). Git is read-only: restore by `cp` from the `.orig` copy, then `shasum -a 256` must equal its snap-before line; quote
   that line per probe. Frontend probes run one test file: `bash ~/.claude/scripts/with-budget.sh 180 bash <script running
   vitest run <test file>> > SP/s12-t<n>-qa-<probe-id>.log 2>&1`. Java probes run `./mvnw -pl bookstore clean test
   -Dtest=<Class>` (always `clean`). Quote the failing assertion line (the `expected ...` / `AssertionError` text plus the test
   name) that names the CURRENT probe's reason. Vitest transpiles without type-checking, so a probe must change runtime
   behaviour; an import error, a syntax error, a compile error or a failure naming another reason is no evidence. A survivor of a
   must-fail probe is a FAIL with a Required Action naming the missing assertion; informational probes are reported only. Never
   edit a test to make a probe pass or fail. Your own extra survivors are judged against the criterion text.
5. Scope: `git diff --name-only <pr-base>...HEAD` plus `git status --porcelain --untracked-files=all` list only fileScope files
   (agent files under .claude/agent-memory/ and agent-os/specs/ are reported, not a product FAIL). Always empty diffs:
   bookstore/src/main except T1's StorefrontConfig.java, every `*Commands.java` and `*Queries.java`, ArchitectureTest.java,
   ArchitectureSelfTest.java, IntegrationTestBase.java, bookstore/src/main/resources, docs/spec-pack/orchestrator/progress.md.
   Frontend files of earlier tasks change only where this task's fileScope names them; earlier test files gain lines only, except
   a removal the card names (the T2 placeholder test): write `git diff -U0 -- frontend/src` to an SP file and list every
   `^-[^-]` line in a test file with its reason.
6. Global greps, run literally from the repo root and quoted with output even when the diff does not touch the path:
   a. Clock, random, locale (GQ12): `grep -rnE '\bDate\b|performance\.now|Math\.random|setTimeout|setInterval|useFakeTimers|
      toLocale|Intl\.' frontend/src` empty.
   b. Module state (GQ12): `grep -rnE '^(export )?(let|var) ' frontend/src` empty; `grep -rnE '^(export )?const
      [A-Za-z0-9_]+(: [^=]+)? = (\[|\{|new )' frontend/src`: read every hit and show it frozen (`as const`, `Object.freeze`, never
      mutated) or FAIL.
   c. No business rule: `grep -rn parseFloat frontend/src` empty; `grep -rnE 'Number\(|parseInt|toFixed|Math\.(round|floor|ceil|
      trunc)' frontend/src` only in src/state/estimate.ts, the quantity input handler (name file:line) and test files;
      `grep -rnE 'onHand|reserved|reorderLevel' frontend/src` only in src/api/types.ts and test fixtures; `grep -rni isbn
      frontend/src`: display and types only (no length, digit or checksum logic); limits: `grep -rnwE '10|20|1000'
      frontend/src --include=*.ts --include=*.tsx` outside tests and src/limits.ts: read every hit, no mirrored limit literal.
   d. Styling and injection: `grep -rnE 'style=\{\{[^}]*(color|background|border|shadow|fill|stroke)' frontend/src` empty;
      `grep -rn 'document.title' frontend/src` empty; `grep -rnE 'dangerouslySetInnerHTML|innerHTML|outerHTML|
      insertAdjacentHTML' frontend/src` empty.
   e. Definition of done, literal: `grep -ri "cors" bookstore/src frontend/src` empty (fixture data avoids the substring).
      No CDN: `grep -rniE "googleapis|gstatic|jsdelivr|unpkg|fontsource|cdnjs|https?://" frontend/src frontend/index.html`
      empty except `http://www.w3.org/` namespace lines (listed).
   f. Stripped paths and secrets: `grep -rnE 'docs/spec-pack|agent-os|\.claude|CLAUDE\.md|ANTHROPIC_API_KEY' frontend
      bookstore/src bookstore/pom.xml pom.xml .github --exclude-dir=node --exclude-dir=node_modules --exclude-dir=dist` empty
      (the root .gitignore keeps its .claude entries and is not scanned); `grep -rnE 'API_KEY|apiKey|api[-_ ]key' frontend/src`
      only in *.test.* fixtures, never in production code.
   g. Non-ASCII, byte level, in the Bash tool: `rg -n '[^\x00-\x7F]' frontend/src frontend/index.html frontend/README.md
      frontend/pom.xml frontend/package.json frontend/vite.config.ts frontend/.npmrc pom.xml bookstore/pom.xml .github
      bookstore/src` prints exactly the T1 baseline lines (recorded in this ledger after T1) and nothing else; on every fileScope
      text file empty; rg skips binaries (woff2, png): say so. Positive control: Write SP/s12-t<n>-qa-nonascii-control.txt with
      one U+2003 character, `rg -c '[^\x00-\x7F]'` on it prints 1. Non-ASCII test data is built from code points
      (`String.fromCodePoint(0x2003)`); never type an escape into a Write or Edit parameter. Em dashes are covered by this grep.
   h. Pins: `grep -nE '"[\^~]' frontend/package.json` empty.
   i. TypeScript: `grep -rnE '@ts-(ignore|expect-error|nocheck)|eslint-disable' frontend/src` empty; `grep -rnE ':\s*any\b|as
      any\b|<any>' frontend/src` only under frontend/src/test/ (listed).
   j. Queries: `grep -rnE 'getByTestId|data-testid' frontend/src` listed with a reason (role and label queries first);
      informational.
7. Warning inventory: `grep -n -i -E "warn|deprecat|self-attach" SP/s12-t<n>-qa-referee.log`, `grep -n -E " ERROR |\[ERROR\]|npm
   notice|\(!\)|stderr \|" SP/s12-t<n>-qa-referee.log`; compare per key and count with the previous gate's referee log (T1: the
   base log of T1-2). (a) Lines present in the previous log at the same count are carried. New lines: (b) accepted at an exact
   count with exact text: `npm warn install-scripts ... fsevents@2.3.3` logged as `[ERROR]` on macOS (OQ-8), `npm notice`
   update hints; (c) fixable in scope, a FAIL: any Vitest `stderr |` block, React `Warning:` (keys, act), jsdom `Not
   implemented:` (for example `window.scrollTo`), Vite `(!)` warnings, TypeScript errors, `npm warn` caused by this task's files;
   (d) unowned (for example `npm warn deprecated` from a pinned package), flagged to the orchestrator. Never "environmental"
   without a machine-local source shown.
8. Contexts and container, counted in the bookstore section of the referee log (between the `Building bookstore` and the next
   module header; quote line numbers): exactly one `Creating container for image: postgres:18.6`; Spring `Started .* in .*
   seconds` lines equal to the T1-2 base count (StorefrontConfigTest must not start a context of its own).
9. Coverage: no coverage tool is pinned; never add one. Changed-code inventory: every exported function, component, reducer
   action, conditional render, error branch and early return in the changed production files mapped to the test (file:line)
   that executes it and the assertion that fails when it is removed; list every branch no test executes.
10. Traceability matrix in the verdict: acceptance criterion -> test (file:line) -> the quoted asserting line(s) verbatim; spec
    scenario -> test; inspection criteria -> command -> quoted output. Quote asserting lines verbatim (the QA lead compares them
    across rework rounds).
11. Evidence files: first `ls -lT SP/s12-t<n>-qa-*` (if a name exists from before this round, use a distinct name and say so);
    cite by absolute path, each with an mtime inside this round. Never cite an impl file or an implementer's probe log as your
    evidence.
12. UI hygiene: the Vitest run has zero `stderr |` blocks and zero `Warning:`, `act(`, `Not implemented` lines (quote the greps
    with count 0); quote the console trap of setupTests (GQ4) when accepted. Browser E2E only in T10 (and qa-final); elsewhere
    E2E preflight and budget are N/A: say so. Live runs only where an item asks, with port preflight `lsof -nP -iTCP:<port>
    -sTCP:LISTEN` and teardown. Never run any `docker compose` command and never touch the container bookstore-postgres-1 (GQ1).
13. Shell: one simple command per Bash call; where an item shows a pipe, write the first command's output to an SP file and grep
    that file; run every `rg` item in the Bash tool itself; scripts in .claude/comms/tmp run with `bash` or `python3 -I`.
14. Assertion form: contract values are literals (paths, codes, titles, money strings such as "24.00", instants, statuses, key
    sets). Request bodies: `JSON.parse(init.body)` compared with `toEqual` against a literal (exact key set; numbers stay numbers,
    `quantity: 2` never "2"). URLs: exact strings; e-mail parameters through `new URL(url, 'http://x').searchParams.get(...)`
    equal to a literal. Lists in order: `getAllByRole(...).map((e) => e.textContent)` `toEqual` a literal array; never `toContain`
    alone for an order or completeness claim. Every absence assertion (`queryByText('25.00')` is null, a button missing) runs
    AFTER an awaited presence assertion on the same render, otherwise it is vacuous. Queries by role and label. `fetch` answered
    with real `Response` objects carrying the server's content type (`application/problem+json` for ProblemDetails,
    `application/json` for views and Boot error JSON). No sleeps; `findBy*` or `waitFor` only; pending states through a promise
    the test resolves by hand, never timers.
15. Falsifiers: each item of the task's T<n>-F list must exist as a test or an assertion; a missing one under a PASS is a REJECT
    with the named test as Required Action (announced here, so it is not a moving bar). Items marked [add] apply only when the
    orchestrator accepted GQ7 (or the finding named); otherwise report them as informational.
16. Ordered data: every order or pass-through-order claim uses at least three elements sorted in no attribute in either
    direction (every attribute the code holds for the element: id, title, author, price, genre, date, status, quantity, total),
    exempt attributes named per test. UUID literals are lower-case hyphenated, distinct per entity, first hex digit 0 to 7, and
    id order differs from the asserted order.
17. Pass-through inventory: per rendered or sent field, list the distinct values the tests feed. A field fed only its natural
    default (currency `EUR`, status `PLACED` or `AVAILABLE`, `shippedAt` null, details `[]`, quantity 1, `available` 0,
    `inStock` true, the single code of a status) needs one other value asserted as a literal. Every String sent to the server
    (e-mail, chat message, search terms) needs one value with edge whitespace sent byte-exact, unless the card decides a trim
    and a test pins it.
18. Errors: for every page and endpoint the task touches, list each status the spec and res-1's status table name and the test
    covering it. ProblemDetail rendering keys on `code`, never on `title` text or on the status alone (except 429, keyed on the
    status by decision); non-ProblemDetail bodies render through the same alert.
```

## 4. Per-task qaDirectives (C0 plus the block)

### T1 (elevated, sonnet): C0 plus

```
T1-1 Inspection: `grep -n '^import\|@' StorefrontConfig.java` shows only org.springframework.context.annotation.Configuration
     and org.springframework.web.servlet.config.annotation types, `@Configuration(proxyBeanMethods = false)`, no @Controller,
     @RestController, @RequestMapping; quote the three registrations. pom: write the `<resources>` block of
     ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/4.1.2/spring-boot-starter-parent-4.1.2.pom and the
     first two entries of bookstore/pom.xml to SP files and compare whitespace-normalised (identical); the third entry has
     directory `${project.basedir}/../frontend/dist`, targetPath `static/shop`, no `<filtering>`; `git diff -- bookstore/pom.xml`
     shows only the block and its plain-words comment; `./mvnw -q -pl bookstore help:effective-pom
     -Doutput=SP/s12-t1-qa-effective-pom.xml` lists exactly three resources.
T1-2 Base (GQ6): `git archive <pr-base>` extracted into SP/s12-t1-qa-base/, referee run there (budget 600) to
     SP/s12-t1-qa-base-referee.log: base per-module totals, Started lines, warning inventory, the bookstore non-ASCII list and
     `grep -ri cors bookstore/src` (base, empty). These are the spec 12 baselines; the QA lead records them in the ledger.
T1-F Falsifiers: rootRedirectsToShop asserts status 302 exactly (not is3xxRedirection) and the Location resolved against the base
     URL equals `<base>/shop/` (trailing slash), proving RestTestClient did not follow the redirect; shopWithSlashServesIndexHtml
     and shopWithoutSlashServesIndexHtml assert 200, a `text/html` content type and a body containing `id="root"`;
     booksIsNotRedirected asserts 200, a JSON content type and no Location header; healthIsNotRedirected asserts 200, `UP` and no
     Location. The class extends IntegrationTestBase and declares none of @SpringBootTest, @Import, @TestPropertySource,
     @ActiveProfiles, @MockitoBean, @DirtiesContext. No application*.yml under bookstore/src/test/resources.
T1-P Must-fail probes (`./mvnw -pl bookstore clean test -Dtest=StorefrontConfigTest`): P1 redirect target "/shop" (no slash);
     P2 "/" registered as `forward:/shop/index.html` instead of a redirect; P3 the "/shop" registration removed; P4 the "/shop/"
     registration removed; P5 `.setStatusCode(HttpStatus.MOVED_PERMANENTLY)` on the redirect; P6 an added
     `registry.addRedirectViewController("/books", "/shop/")`; P7 `@Configuration` removed with the class kept (proves the bean is
     picked up by scanning, not imported by the test).
T1-R Resource probes (verifier's own; frontend/ does not exist on this branch, remove the whole directory afterwards and prove it
     with the snapshots): create frontend/dist/probe.txt holding `@project.version@` and frontend/dist/index.html; `./mvnw -q -pl
     bookstore clean process-resources`: bookstore/target/classes/static/shop/probe.txt holds the token unreplaced (Read it),
     `cmp bookstore/src/main/resources/application.yml bookstore/target/classes/application.yml` identical, the db/migration file
     count in target equals the source count. Then without frontend/dist: the log shows `skip non existing resourceDirectory` and
     target/classes has no static/shop.
T1-G Gates: ArchitectureTest and ArchitectureSelfTest counts equal the base; Started count equal to the base (C0-8);
     `grep -ri cors bookstore/src` empty before (base) and after.
```

### T2 (elevated, sonnet): C0 plus

```
T2-1 Inspection: frontend/pom.xml packaging pom, parent agenticbookstore-parent, plugin 2.0.2, installDirectory and
     workingDirectory `${project.basedir}`, nodeVersion v26.13.0, no npmVersion, no `<skip>` element; executions (quote ids and
     phases) install-node-and-npm and `npm ci` at generate-resources in that order, `npm test` at phase `test`, `npm run build` at
     prepare-package; clean-plugin fileset `dist`. package.json key sets exact (two dependencies, the twelve dev dependencies of
     the card, no oxlint, no `lint` script), versions exact, scripts exact; .npmrc `save-exact=true`. A python3 -I script compares
     each pin with `packages["node_modules/<name>"].version` in package-lock.json (all equal; lockfileVersion quoted).
T2-2 Lockfile natives: a python3 -I script lists every package entry with an `os` or `cpu` field, grouped by family; every family
     with a darwin-arm64 member also has linux-x64 and win32-x64 members (quote the table). The real falsifier is CI in T3.
T2-3 Builds: `./mvnw -f frontend/pom.xml clean verify` log shows the execution order, `RUN v5.1.0`, the Vitest summary and the
     vite build; `frontend/node/node --version` prints v26.13.0; every `src=` and `href=` value in frontend/dist/index.html starts
     with `/shop/` (quote them). `-DskipTests` run: `Skipping execution.` attributed to the npm-test execution (quote both lines),
     `RUN v` count 0, dist rebuilt (mtime after the run start). Red probe: uncommitted src/s12-t2-qa-probe.test.ts with
     `expect(1).toBe(2)`: BUILD FAILURE naming the npm-test execution and the AssertionError quoted; removed; snapshots equal
     (GQ5). `./mvnw -f frontend/pom.xml clean` removes dist (ls). `npm ci` from scratch (node_modules deleted first) in the
     `-f` build log. Root referee per C0-1 unchanged (frontend not in the reactor yet; root pom.xml diff empty).
T2-4 Stray files: after the builds `git status --porcelain --untracked-files=all frontend` lists only fileScope files (no
     tsconfig*.tsbuildinfo); `git check-ignore -v frontend/node frontend/node_modules frontend/dist` quotes the rules.
T2-5 Config: vite.config.ts `base: '/shop/'`; proxy keys exactly /books, /inventory, /orders, /assistant, each target
     `http://127.0.0.1:8080` (never localhost); port 5173; test.environment jsdom, setupFiles, include exactly
     `['src/**/*.test.{ts,tsx}']`; tsconfig.app.json `strict: true` and tsBuildInfoFile under node_modules/.tmp.
T2-6 Guard (GQ4) [add]: setupTests.test.ts as described in GQ4. Must-fail probes, one per afterEach statement removed: cleanup,
     vi.unstubAllGlobals, vi.restoreAllMocks, sessionStorage.clear, the hash reset; each makes the guard's second test fail.
     Console trap probe: a `console.error('probe')` added to App.test.tsx makes it fail.
T2-7 Smoke: App.test.tsx finds the placeholder inside getByRole('main'); probe: App returns null, the test fails.
T2-8 Template removal: no src/assets/react.svg, App.css, index.css, public/vite.svg, oxlint config; `grep -c oxlint
     frontend/package-lock.json` prints 0; the create-vite version is in the outbox.
T2-9 README: every command matches package.json; the verifier runs the documented dev command in a script (port 5173 free first):
     `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:5173/shop/` gives 200, then the server is stopped and the port is
     free again; the same command without the PATH prefix (PATH=/usr/bin:/bin) fails with `env: node` (quote). README ASCII.
T2-10 fsevents `[ERROR]` lines quoted with their count and classified C0-7 (b).
```

### T3 (elevated, sonnet): C0 plus

```
T3-1 Diff: `git diff <pr-base>...HEAD -- pom.xml` inserts only `<module>frontend</module>` before bookstore (order frontend,
     bookstore, distributor); the ci.yml diff is insertion-only (`grep -E '^-[^-]'` on it empty), exactly two cache steps per job
     between setup-java and the Maven step (quote line numbers). A python3 -I script byte-compares each key, path and restore-key
     against the card literals, with a one-space negative control that must fail; the node cache has no restore-keys; Windows npm
     path `~\AppData\Local\npm-cache`. Pin: `gh api repos/actions/cache/commits/v6.1.0 --jq .sha` equals
     55cc8345863c7cc4c66a329aec7e433d2d1c52a9.
T3-2 Local referee: quote the `Reactor Build Order` block and the reactor summary; bookstore/target/classes/static/shop/index.html
     and bookstore/target/classes/application.yml exist; `jar tf` of the bookstore jar (to an SP file) lists
     BOOT-INF/classes/static/shop/index.html, one static/shop/assets/*.js, BOOT-INF/classes/application.yml and
     BOOT-INF/classes/db/migration/ (GQ14); Total time recorded as the first referee time with the frontend.
T3-3 CI (the orchestrator pushes and opens the PR first): `gh run list --commit <full 40-char PR head sha> --json
     databaseId,event,status,conclusion,headSha`; `gh run view <id> --json jobs` saved unfiltered; each job log saved to SP.
     Greps anchored on the Actions timestamp prefix (`Z <text>`): Linux: the Vitest `Test Files` and `Tests` lines with counts
     equal to the local run, frontend SUCCESS in the reactor summary; Windows: `Skipping execution.` for npm-test, the vite build
     `built in` line, BUILD SUCCESS; both: `HEAD is now at <sha> Merge` (merged tree), the post-job cache steps' save lines for
     both keys quoted with the action's exact text, `Path Validation Error` count 0; `gh api repos/<o>/<r>/check-runs/<job
     id>/annotations` saved per job. Warning count per job (`\[WARNING\]|WARNING: | WARN |##\[warning\]|npm warn`) compared
     with a baseline: the last green run before T3 on the spec branch or factory, named by sha; new lines classified per C0-7.
T3-4 First-run cache misses quoted with the action's exact text (informational); cache-hit evidence is T10-8.
T3-5 C0-6f on both files empty; no other file changed.
```

### T4 (elevated, sonnet): C0 plus

```
T4-1 Tokens: a test reads tokens.css raw and asserts the seven `--ob-*` values literally as in the spec table, plus the extra
     error text, error background and shadow tokens exist. Probe P1: `--ob-blue` changed to #3D7DC8, the token test fails.
T4-2 Contrast calibration: expected ratios as literals to two decimals (`toBeCloseTo(x, 2)`): white on navy 15.50, navy on mist
     10.69, teal on white 11.52, blue on white 4.22, sage on white 2.84; symmetry ratio(a, b) equals ratio(b, a); the SAME
     predicate and threshold constant the pair assertions use returns false for blue on white and sage on white and true for navy
     on snow. Must-fail probes: P2 sRGB linearisation dropped (channel / 255 used directly); P3 R and B coefficients swapped;
     P4 threshold 4.5 changed to 4.0 (killed by the predicate on blue on white); P5 the +0.05 terms dropped; P6 ratio taken as
     L2/L1 without ordering. Informational: `>` versus `>=`, 0.03928 versus 0.04045 (equivalent on these colours).
T4-3 Pairs (GQ2): the extractor is unit-tested on fixture CSS strings: `background-color`, `background` shorthand with the var
     first and with extra values (`background: var(--ob-white) no-repeat`), a rule inside `@media`, a multi-selector rule, a
     commented-out declaration ignored, the `/* ob-on: */` form with two surfaces, and a `color` with neither background nor
     annotation that the coverage check rejects. Production assertion: the extracted plus annotated set contains, named
     literally, white on teal (primary button), white on teal (in-stock badge), navy on mist (out-of-stock badge), error text on
     error background. Probe P7: the extractor stops matching `background-color`; the fixture test and the superset assertion
     fail.
T4-4 Logotype (GQ3) [add]: one exemption entry asserted >= 3:1; every rule with `color: var(--ob-blue)` has a selector in the
     exemption list. Probe P8: `color: var(--ob-blue)` added to a second rule in global.css, the test fails.
T4-5 Focus ring: blue against every surface a focusable element sits on (read global.css: snow, white, navy if the footer has
     links) >= 3:1. Blue on mist is 2.91 and on teal 2.73: a focusable element on a mist or teal surface, or a negative
     outline-offset on the teal button, needs another ring colour; the verifier lists the surfaces it found and checks each is in
     the test.
T4-6 Style contract fixtures, each asserted flagged: `#abc`, `#abcd`, `#aabbcc`, `#aabbccdd` (upper and lower case), `rgb(`,
     `rgba(`, `hsl(`, `hsla(`, `oklch(`, `color-mix(` with a literal colour, `border: 1px solid red`, `box-shadow: 0 1px 2px
     black`, `color: navy`, an undefined `var(--ob-nope)`, a `transition` without a reduce block, a stylesheet set without
     `:focus-visible`; each asserted NOT flagged: `color: var(--ob-navy)`, `transparent`, `currentColor`, `inherit`, the TS string
     `'#/books/'`. Anti-vacuity: the scanned set contains src/styles/global.css, src/styles/fonts.css, src/main.tsx, src/App.tsx
     and the exclusion list is exactly the test itself and tokens.css. Must-fail probes: P9 `color: #fff` in a NEW file
     src/components/s12-t4-qa-probe.css; P10 `style={{ color: 'red' }}` in App.tsx; P11 `rgb(0, 0, 0)` in global.css. Reduced
     motion: when a transition or animation exists, the reduce block sets both to none (or a 0s duration) for `*` or for every
     selector that declares one (test).
T4-7 Fonts: `od -c -N 4` per woff2 shows `w O F 2`; OFL.txt present; fonts.css has three @font-face rules (400, 600, 700) with
     `font-display: swap` and relative url(); the body stack literal; after the build three woff2 files in frontend/dist/assets.
     Provenance: the verifier downloads each recorded URL into SP and compares sha256 with the recorded sums and the committed
     files. Fallback path: no @font-face and the outbox says so.
T4-8 index.html raw test asserts `<title>OpenBook</title>`, `lang="en"` and the root div; probe P12: title changed, the test
     fails. C0-6e CDN grep empty.
T4-9 global.css primitives quoted by selector: sticky header, card radius and shadow token, primary button white on teal, links
     teal, both badges, alert tokens, `:focus-visible` ring in blue, reduce block, a min-width media query switching the grid.
```

### T5 (critical, opus): C0 plus

```
T5-I Types: a field-by-field table types.ts versus res-1 section 1 (money and instants string, BookView.authors string,
     BookDetailView.authors string[], shippedAt string | null, counts number, details Array<Record<string, string>>, status and
     genre unions); no `any`.
T5-F Falsifiers (client.test.ts, real Response objects):
     a. ProblemDetail pass-through: title, detail, code and details asserted as the body's literals; a second code per status
        [add]: 404 book-not-found and order-not-found, 422 unknown-genre and invalid-email (kills a status-keyed code or title
        table).
     b. details: missing, null and an object all become []; insufficient-stock with two shortages of different string values;
        validation-failed with an indexed field `lines[1].quantity` and a repeated `customerEmail` supplied in an order that is
        not sorted, kept as given.
     c. Detection: problem+json without `code` is a ProblemDetail (title from `title`); application/json with `code` is a
        ProblemDetail; one content type with a parameter (`application/json;charset=UTF-8`) handled; Boot error JSON (400 and 429,
        no code) gives title from `error`; Boot JSON without `error` gives the status text; an empty body with an empty status
        text gives `Request failed (502)`; a text/html 502 body with status text "Bad Gateway" gives "Bad Gateway"; malformed JSON
        under application/json gives an ApiError, never a SyntaxError (`rejects` with the ApiError type).
     d. Problem.status is the HTTP status (Boot JSON 429 gives 429).
     e. Network: fetch rejecting with TypeError gives ApiError status 0 "Cannot reach the bookstore".
     f. 2xx: 200 JSON parsed; 202 `{id}` parsed; no ApiError on 202.
     g. Request shape: method literal; the string passed to fetch starts with `/` (never relative, never /shop/); exact Accept
        header value; POST Content-Type and body byte-exact for a payload holding an e-mail with edge whitespace and capitals and a
        chat message with a leading space and a newline (no trim, no case change).
     h. searchBooks: all four criteria passed in object key order genre, author, title, q give exactly
        '/books?q=<a>&title=<b>&author=<c>&genre=FICTION' with distinct values (alphabetical key order would differ); each
        criterion omitted when "" (one case per criterion); the decided whitespace-only rule pinned by a test; `q: 'pride &
        prejudice'` encodes the `&` (literal pinned); the spec literal '/books?q=austen&genre=FICTION'; no criteria gives
        '/books' with no trailing `?`.
     i. findOrders [add]: `reader+shop@example.com` read back through searchParams equals the literal (kills unencoded
        concatenation, where `+` becomes a space); path /orders.
     j. getBook, getStock, getOrder: exact paths for a UUID; an id `a/b?c` encodes to the literal `/books/a%2Fb%3Fc`.
     k. placeOrder POSTs /orders/commands/place, chat POSTs /assistant/chat, payload unchanged.
     l. fetchMock: a call to '/books?q=austen' when only '/books?q=austen&genre=FICTION' is registered fails with a message
        naming the request (asserted with `rejects.toThrow` or `toThrow`); a POST to a GET-registered path is unmatched too.
     m. No retry: a 503 answer gives exactly one fetch call.
T5-P Must-fail probes: P1 ProblemDetail detection by content type only; P2 by `code` only; P3 details hard-coded []; P4 code
     taken from a status table [add]; P5 title always the status text; P6 Boot JSON title ignores `error`; P7 the network catch
     removed; P8 searchBooks iterates object keys; P9 blank omission removed; P10 findOrders concatenates without encoding [add];
     P11 id not encoded; P12 Accept header dropped; P13 `res.ok` replaced by `status === 200`; P14 JSON.parse error not caught;
     P15 fetchMock matches the path without the query; P16 `.trim()` applied to the e-mail before stringify.
```

### T6 (elevated, sonnet): C0 plus

```
T6-F Falsifiers:
     a. Router: every pattern including the empty hash and `#/`; `#/orders` and `#/orders/<id>` both (matching order); the decided
        behaviour for `#/books/` (empty id) and for a trailing slash pinned; `#/books/a%20b` gives the param "a b"; [add] a
        malformed escape `#/books/%E0%A4%A` renders "Page not found" without a crash; an unknown route renders "Page not found"
        with a link whose href is `#/`; re-render on hashchange inside the same mounted tree (no remount); the hashchange listener
        removed on unmount (spy on removeEventListener).
     b. `navigate(path)` sets the hash; `Link` renders `href="#/..."`.
     c. Header: link with accessible name "OpenBook" and href `#/`, "Open" and "Book" in separate spans with distinct classes;
        App.test asserts the wordmark on two routes; navigation: exactly one `aria-current="page"` per route, asserted on at
        least two routes with different active items, Catalogue active on `#/` and NOT on `#/orders` (kills a `startsWith('/')`
        match); the decided active item for `#/orders/<id>` pinned. From T6 the GQ3 assertion is "exactly one blue text rule, the
        wordmark's".
     d. Listing: three books whose every field differs and whose order is sorted in no column (C0-16); prices "12.50", "7.99",
        "10.00" shown verbatim with the currency from the field, one book with currency "GBP" shown as GBP (decision 11); badge
        decorrelated both ways: `available 3, inStock false` shows out of stock and `available 0, inStock true` shows in stock;
        authors string shown as given ("Terry Pratchett, Neil Gaiman"); genre display pinned; each card links to `#/books/<id>`
        with distinct ids; no basket button on cards.
     e. Search: exactly '/books?q=austen&genre=FICTION'; the search response holds a book absent from the first listing and the
        first listing's books are gone afterwards (absence after awaited presence); genre select options "Any genre" plus the ten
        enum names in declaration order (literal array).
     f. 422 unknown-genre: the title from the body and the genres from `details` in the body's order, where that order differs
        from the client GENRES constant and holds one name the constant lacks (kills rendering the constant).
     g. Empty result message; a non-ProblemDetail error (status 0 or Boot JSON 400) renders through the alert.
     h. Last search: storage content asserted directly (key and JSON); after a remount the fields hold the values and the first
        request uses them (exact URL); corrupt stored search gives defaults without a crash.
     i. ProblemDetailAlert: title; detail; `{field, message}` with a repeated field shows both in given order; `{genre}`;
        `{bookId, requested, available}` values as given including "0"; an unknown shape `{foo: "bar"}` as key and value text;
        empty details render no list; `role="alert"`. With GQ4 accepted, a duplicate-key warning fails the repeated-field test.
T6-P Must-fail probes: P1 badge from `available > 0`; P2 badge from `inStock && available > 0`; P3 currency hard-coded "EUR";
     P4 router matches the /orders prefix before /orders/:id; P5 the subscribe function is a no-op; P6 decodeURIComponent removed;
     P7 the guard around decoding removed [add]; P8 the alert renders the GENRES constant instead of details; P9 aria-current by
     startsWith; P10 the last-search storage write removed; P11 price displayed through Number(); P12 genre dropped from the
     search request.
```

### T7 (elevated, sonnet): C0 plus

```
T7-F Falsifiers:
     a. limits.ts: values and comments quoted (spec 04 in words, PlaceOrderPayload for lines, OrderFlow for quantity, server stays
        the authority); imported where used; no literal elsewhere (C0-6c).
     b. Add A twice gives one line, quantity 2; setQuantity 3; line total "23.97" for "7.99"; insertion order with three lines
        B, A, C sorted in no column (id, title, price); merge keeps position (add A, B, then A gives [A x2, B]); removing the
        middle of three keeps the other two in order.
     c. Refusals start from a mid value: a line at 3, setQuantity 0 and 11 each refused with a message and the line stays 3
        (kills clamping); 1 and 10 accepted from 3; add on a line at 10 refused, stays 10, message; 20 distinct lines accepted, the
        21st refused with a message and storage still holds 20; adding to an existing line at 20 lines accepted (+1). Every refusal
        leaves the whole lines array and the storage value deep-equal to before.
     d. Persistence: after each change sessionStorage is read directly (no unmount) and equals the literal JSON; a fresh provider
        after unmount shows the two lines; storage pre-seeded by the test with a raw JSON literal under the documented key
        hydrates; invalid JSON and `{}` both give an empty basket; clear empties the state and the storage (pinned form);
        hydration through the useReducer initializer (quote; no effect reads storage). Stored line key set exact {bookId, title,
        price, currency, quantity}; quantity is a number.
     e. estimate.test: "7.99" x 3 = "23.97"; "12.50" x 2 = "25.00"; "10.00" x 1 = "10.00"; a two-line total with different prices
        and quantities; float witnesses "4.35" x 3 = "13.05" and "19.99" x 1 = "19.99" (a floor of price * 100 gives 13.02 and
        19.98); "0.10" x 3 plus "0.20" x 1 = "0.50"; "999.99" x 10 = "9999.90" (trailing zero kept).
     f. Detail page: ISBN; authors as list items in given non-alphabetical order (["Terry Pratchett", "Neil Gaiman"]);
        publication year; price verbatim with trailing zero and currency from the field; "7 available" from GET /inventory while
        the detail's `available` is 4; enabled button; available 0 shows "Out of stock" and a disabled button; DISCONTINUED with
        available 5 shows "Discontinued" and a disabled button; AVAILABLE with lowStock true and available 2 keeps the button
        enabled; book 404 shows the alert; [add] inventory 404 shows the alert and no enabled button; clicking "Add to basket" adds
        the line (storage asserted) and shows a confirmation; a refused add (line pre-seeded at 10) shows the basket message and the
        quantity stays 10; both requests use exact paths.
     g. Header badge (GQ11): the pinned semantics with the witness "same book added twice shows 2" (items); updates without a
        reload; a pre-seeded storage shows the hydrated count on first render.
     h. Deep link: hash `#/books/<id>` set before render shows the detail heading with the book title.
T7-P Must-fail probes: P1 setQuantity clamps instead of refusing; P2 line-count guard lets the 21st through; P3 merge appends a
     new line; P4 storage written only on unmount; P5 cents via Math.floor(Number(p) * 100); P6 detail shows the book's
     `available` instead of the stock call's; P7 disabled condition ignores DISCONTINUED; P8 out of stock decided by lowStock;
     P9 the corrupt-storage guard removed; P10 the cap at 10 checked after incrementing (allows 11); P11 remove deletes the wrong
     index. Informational: float sum with toFixed(2).
```

### T8 (critical, opus): C0 plus

```
T8-F Falsifiers:
     a. Basket page: label "Quantity for <title>" per line; `fireEvent.change` 0 and 11 refused with the message while value and
        state stay at the previous mid value; 1 and 10 accepted; a typed "4" is stored as the number 4; "Estimated total" next to
        the figure plus the sentence; estimates for two lines with different prices and quantities including the "4.35" witness;
        empty basket message with a link to `#/`; remove removes the right line.
     b. Checkout: basket A x2, B x1 where A's id sorts after B's; the posted body parses `toEqual({customerEmail:
        "reader@example.com", lines: [{bookId: A, quantity: 2}, {bookId: B, quantity: 1}]})` (order, numbers, no title, price or
        currency keys); a second case sends an e-mail with edge whitespace and capitals byte-exact; `noValidate` (a test submitting
        "not-an-email" reaches fetch); confirm disabled while pending and two clicks give one POST; after 202 `{id}` with an id
        distinct from every bookId: basket state and storage empty, `window.location.hash` equals `#/orders/<id>`, GET
        `/orders/<id>` exact, the order page shows PLACED, each line's unit price and line total, and the total; [add] the header
        badge shows the empty state afterwards.
     c. 409 insufficient-stock: basket state and storage unchanged (literal); each shortage with requested and available as
        given (strings, one available "0"), two shortages with different values in given order (not sorted by id or title), titles
        from the basket, one bookId not in the basket shown as its id.
     d. [add] 409 book-not-orderable with details []: generic alert with its title and detail, no shortage list, basket kept
        (kills status-keyed shortage rendering).
     e. 400 validation-failed: "customerEmail" next to its message; a repeated field shows both messages; an indexed field
        `lines[1].quantity` shown as decided (pinned).
     f. 422 [add witness]: quantity-out-of-range with details [{field: "lines", message: "The quantity 12 is not between 1 and
        10"}] shows the title and the message; basket kept.
     g. Status 0 shows the alert, keeps the basket and re-enables the button.
     h. Order page: a line "12.50" x 2 with lineTotal "24.00" shows "24.00" and not "25.00"; total "31.50" with line totals summing
        to "32.00" shows "31.50" and not "32.00"; currency from the field, one case "GBP"; SHIPPED with shippedAt set shows the
        formatted date, PLACED with null shows the pinned text (never "null"); placedAt with microseconds formatted; two lines in
        an order sorted by none of title, isbn, price; 404 order-not-found alert with its literal title; [add] a Boot-JSON 400 for a
        non-UUID deep link renders the generic alert.
     i. Orders page: three summaries in server order sorted in no column (id, status, placedAt, itemCount, total; one placedAt
        without fraction in the same second as a fractional one); DOM order equals server order (literal array of row texts);
        status, formatted date, item count and total with "EUR"; links `#/orders/<id>`; the request's customerEmail read through
        searchParams equals the entered value as typed; empty list message; 422 invalid-email alert with the details message;
        blank submit behaviour pinned.
     j. format.test: "2026-06-15T10:00:00.123456Z", "2025-12-24T00:00:00Z" and the year boundary "2025-12-31T23:30:00Z" (a zone
        shift would change the day), outputs literal; C0-6a (no Date) makes zone independence gate-proven.
     k. OrderPage and OrdersPage import nothing from estimate.ts (grep) and do no arithmetic (C0-6c).
T8-P Must-fail probes: P1 order page line total recomputed from unitPrice x quantity; P2 total recomputed from lines; P3 checkout
     sorts lines by bookId; P4 quantity sent as a string; P5 basket cleared on submit (before the answer); P6 basket not cleared
     after 202; P7 navigation with a bookId instead of the order id; P8 shortage rendering keyed on status 409 [add]; P9 the title
     fallback removed; P10 the pending guard removed; P11 orders sorted by placedAt; P12 e-mail trimmed before sending; P13
     currency hard-coded on the order page; P14 the format regex requires a fraction; P15 shippedAt null rendered as text.
```

### T9 (elevated, sonnet): C0 plus

```
T9-F Falsifiers:
     a. MAX_CHAT_MESSAGE_LENGTH = 1000 with its comment (spec 08 in words, `bookstore.assistant.max-message-length`) quoted.
     b. conversation.ts: an existing stored id (different from the stub's value) is returned and the `crypto.randomUUID` spy is
        called 0 times; a missing id is created once (spy count 1), stored under the key (raw storage read), and the next call
        returns the same id with the spy count still 1; after sessionStorage.clear a new id is created (no module cache).
     c. Page: with a pre-seeded id, "Do you have anything by Austen?" posts exactly `{conversationId, message}` (toEqual literal);
        the decided message trimming pinned with an edge-whitespace witness; the user message and the reply (distinct text) render
        in order; a second message posts the same seeded id; replies in an `aria-live="polite"` region; a reply "<b>x</b>"
        renders as text (textContent holds "<b>", no `b` element).
     d. Errors: 503 assistant-offline shows exactly "The assistant is offline", the input stays enabled and a following send
        succeeds (second POST, reply shown); 429 Boot error JSON and 429 ProblemDetail (some code) both show exactly "Too many
        requests, try again in a minute"; 503 without that code shows the generic alert with the body's title; [add] 400
        validation-failed `{field: "message"}` shows the alert and the input stays usable; status 0 shows the alert.
     e. Length: 1000 characters keep send enabled, 1001 disable it with a counter showing the limit (literal); empty input disabled;
        whitespace-only behaviour pinned; the textarea has no maxLength attribute (asserted null); input through fireEvent.change or
        paste.
     f. Send disabled while pending; a double click gives one POST.
     g. The verdict states that the 429 and 503 body shapes are spec 08 text only (OQ-4) and the live round trip is not part of
        this gate.
T9-P Must-fail probes: P1 randomUUID called on every call; P2 the id not stored; P3 offline keyed on status 503 only; P4 429 keyed
     on code; P5 limit check `>= 1000`; P6 limit check `> 1001`; P7 the input disabled after offline; P8 the pending guard removed;
     P9 a new conversation id per message.
```

### T10 (critical, opus): C0 plus

```
T10-1 Clean state (OQ-7), repeated by the verifier: delete frontend/node, frontend/node_modules, frontend/dist and every target/
      directory (`find . -name target -type d -prune` listed before and empty after); `PATH=/usr/bin:/bin:/usr/sbin:/sbin which
      node` prints nothing; the referee runs through a script with PATH=/usr/bin:/bin:/usr/sbin:/sbin and JAVA_HOME as set. The log
      shows Node installed or unpacked from the ~/.m2 archive (kept: the record says so), Vitest ran, the build ran,
      bookstore/target/classes/static/shop/index.html exists and the jar lists it. Cold time, then a warm second run time; compare
      with the record. Over 60 s: a type: question to the orchestrator (OQ4) with both numbers, never trimmed.
T10-2 Probes, repeated by the verifier: an uncommitted failing Vitest test with `./mvnw verify` (no -fae) fails in frontend and
      the reactor summary shows bookstore and distributor SKIPPED (quote); file removed, snapshots equal (GQ5). `-DskipTests
      verify`: `Skipping execution.` for npm-test, `RUN v` count 0, bookstore/target/classes/static/shop/index.html present with a
      fresh mtime.
T10-3 Isolation (GQ1), for the implementer's session (read the record and the outbox) and the verifier's own: `docker inspect -f
      '{{.State.Status}} {{.State.StartedAt}}' bookstore-postgres-1` saved before and after, identical; ports 8080, 5173 and the
      throwaway Postgres port free before and after; no `docker compose` command anywhere; throwaway container named
      s12-t10-<role>-pg, removed (`docker ps -a --filter name=s12-t10` empty after); the bookstore started with `java -jar`,
      `--spring.docker.compose.enabled=false` and the explicit datasource; its log has no Docker Compose line and shows Flyway on
      the throwaway database; walk-through data created through the REST command endpoints, including one book whose available
      stock is below a quantity the UI allows (for the 409).
T10-4 Live checks, re-run by the verifier: `curl -i /` gives 302 with the literal Location recorded and resolving to
      `<origin>/shop/`; `/shop/` and `/shop` 200 text/html with the root element; one /shop/assets/*.js and one .woff2 200 with
      their content types; `/shop/missing.js` 404; `/books` 200 JSON; `/actuator/health` 200 UP. Dev proxy: the documented dev
      command, `/shop/` on 5173 200, and `/books` through 5173 byte-identical (`cmp`) to `/books` from 127.0.0.1:8080 fetched at
      the same moment; if 8080 belonged to another checkout, the record says whose instance answered.
T10-5 [add, GQ14] Without dist: after the live session, rm -rf frontend/dist, `./mvnw -q -pl bookstore -DskipTests clean package`,
      `jar tf` lists no static/shop/, a short run of that jar answers `/shop/` 404 and `/books` 200 (same isolation rules). The
      final referee (C0-1) rebuilds everything afterwards.
T10-6 Walk-through: the record's table checked row by row; the verifier re-walks in the browser: deep link /shop/#/books/<id>, add,
      basket quantity change through the input, checkout success, order page whose line totals and total equal the strings of
      `GET /orders/<id>` (curl, compare), insufficient stock whose requested and available equal the 409 body, orders history in
      the order of `GET /orders?customerEmail=` ids, assistant page loads with its empty state and the record marks the live round
      trip pending spec 08 (GQ9; a sent message shows the alert for the absent endpoint, status quoted). GQ8 checks quoted per
      page: scrollWidth at 390, grid tracks at 390 and 1280, the focus outline after Tab, transition-duration 0s under reduced
      motion. Browser console errors 0 per page except the 4xx of deliberate error paths, listed.
T10-7 Screenshots: exactly 14 files visuals/{catalogue,book-detail,basket,checkout,order,orders,assistant}-{desktop,phone}.png
      and no others; `sips -g pixelWidth -g pixelHeight` per file gives 1280 or 390 times the device pixel ratio stated in the
      record; the verifier opens every PNG and confirms per file: the named page, the "OpenBook" wordmark in the header, live data
      (no error or empty state except the assistant), single column on phone files, no horizontal overflow; every PNG mtime is
      later than the newest frontend source change on the branch.
T10-8 Cache hits: from a CI run after T3 (named by sha), both jobs' restore lines for the node and npm keys, quoted with the
      action's exact text and anchored.
T10-9 DoD: C0-6e literal; `git diff --stat <spec-base>...HEAD -- '*Commands.java' '*Queries.java'` empty; the record is plain
      English ASCII without em dashes (C0-6g on it), holds every DoD line with an evidence pointer, the times, the kept ~/.m2 Node
      archive note, the GQ9 pending item; progress.md diff empty.
T10-10 Scope: only verification-record.md and the 14 PNGs; `git diff --stat <pr-base> -- frontend bookstore pom.xml .github`
      empty.
```

## 5. qa-final focus (seed for the final-brief)

1. Integrity: every task's hash baseline against the committed bytes at the spec tip, plus the sorted union of baseline paths
   equal to the product files of the spec diff (minus .claude/ and agent-os/); dry-run script handed over.
2. Referee on a `git archive <tip>` extraction with PATH limited to the system directories (no global Node); time against 60 s.
3. Cross-task journey through App with one fetch mock, as a scratch test in the extraction (source copied to SP): catalogue,
   detail, add twice, basket input to 3, checkout, order page, header badge empty; payload quantity is the number 3; ids flow.
4. Style guards at the tip: every `color` declaration across all CSS added by T6 to T9 is covered (GQ2); blue text only in the
   wordmark (GQ3); focus ring surfaces.
5. C0-6 greps on the whole product diff; storage keys of basket, last search and conversation id distinct (grep); setupTests
   guard green.
6. Spec scenario to test matrix complete; spec Tests-section file names exact.
7. Screenshots fresh against the last UI change on the tip; 14 files.
8. CI on the tip (`gh run list --commit <40-char sha>`): both jobs green, Linux Vitest counts equal local, Windows skip line,
   cache restore lines.
9. Bookstore unchanged since T1: Started and test counts equal the T1 baselines; ArchitectureTest diff empty.
10. Residuals for Gate 2: spec 08 live walk-through and 429/503 shapes (manual after spec 08).

## 6. Traceability plan (spec scenario -> task -> test)

| Spec scenario or DoD line | Task | Test or evidence |
|---|---|---|
| Root redirects | T1, T10 | StorefrontConfigTest.rootRedirectsToShop; T10-4 literal Location |
| Storefront page | T1, T10 | shopWithSlashServesIndexHtml, shopWithoutSlashServesIndexHtml; T10-4 |
| REST paths are untouched | T1, T10 | booksIsNotRedirected, healthIsNotRedirected; T10-9 Commands/Queries diff |
| Deep link | T7, T10 | App.test deep link; T10-6 browser |
| Catalogue listing | T6 | CataloguePage.test listing (T6-F d) |
| Search | T6, T5 | CataloguePage.test search (T6-F e); client.test searchBooks (T5-F h) |
| Unknown genre error | T6, T5 | CataloguePage.test 422 (T6-F f); client.test 422 |
| Detail page | T7 | BookDetailPage.test (T7-F f) |
| Out of stock | T7 | BookDetailPage.test available 0 |
| Discontinued book | T7 | BookDetailPage.test DISCONTINUED available 5 |
| Add and change quantity | T7, T8 | basket.test (T7-F b); BasketPage.test (T8-F a) |
| Quantity bounds, 21st book | T7, T8 | basket.test (T7-F c); BasketPage.test 0/1/10/11 |
| Basket survives a reload | T7 | basket.test persistence (T7-F d) |
| Successful checkout | T8 | CheckoutPage.test (T8-F b) through App |
| Insufficient stock | T8 | CheckoutPage.test 409 (T8-F c) |
| Validation error | T8 | CheckoutPage.test 400 (T8-F e) |
| Order history (newest first) | T8, spec 04 | OrdersPage.test server order (T8-F i); newest first owned by OrderQueriesTest (GQ13) |
| Chat round trip | T9 | AssistantPage.test, conversation.test (T9-F b, c) |
| Assistant offline | T9 | AssistantPage.test 503 (T9-F d) |
| Message length | T9 | AssistantPage.test 1000/1001 (T9-F e) |
| Name and title | T4, T6 | index.html raw test (T4-8); App.test wordmark on two routes (T6-F c); T10-7 screenshots |
| Brand tokens in one place | T4 | tokens test (T4-1); styleContract.test (T4-6) |
| Readable contrast | T4 | contrast.test (T4-2 to T4-5, GQ2, GQ3) |
| One build | T2, T3, T10 | T2-3; T3-2; T10-1 |
| Red frontend test fails the build | T2, T10 | T2-3 red probe; T10-2 |
| No business rule in the browser | all frontend | C0-6c, estimate.test, OrderPage.test verbatim totals |
| client.test 400, 404, 409, 422, 503 | T5 | T5-F a to c |
| DoD screenshots | T10 | T10-7 |
| DoD time recorded, no global Node | T10 | T10-1 |
| DoD `grep -ri cors` | all | C0-6e |
| Design note: no dist gives 404 | T10 | T10-5 (GQ14) |

## 7. Residual risks known at strategy time

- R1 Spec 08 shapes (429, 503 title and detail, 400 for `message`) are spec text only; manual check after spec 08 merges (OQ-4, GQ9).
- R2 CI's test report reads Surefire XML only: a red Vitest test shows only in the job log, not in the annotations or the summary.
  Candidate standards proposal at Gate 2.
- R3 `crypto.randomUUID` needs a secure context: fine on 127.0.0.1 and localhost; opening the shop over a LAN IP would break the
  assistant page. Workshop uses localhost.
- R4 Under `-fae` (CI) the bookstore still builds after a red frontend test; the build still ends red (OQ-5).
- R5 Visual judgment ("modern", "generous whitespace", "prominent search bar") only through the screenshots, by Maik (GQ8).
- R6 Wordmark contrast rests on the WCAG logotype exemption (OQ-1 judgment call, GQ3).
- R7 Stale search responses (a slow earlier answer overwriting a later one) are not tested; out of the spec's scenarios.
- R8 Browser back and forward rely on hashchange only (router test covers the event, not browser history).

## Review log

(none yet)

## Cross-task patterns

(none yet; baselines from T1-2 go here: base Started lines, per-module totals, warning inventory, bookstore non-ASCII list)

## Open questions

- GQ1 must be decided before T10 starts (it contradicts the lead card); GQ2 to GQ4, GQ7, GQ11, GQ12, GQ14 before the tasks they
  name start.

## Resume point

Strategy delivered (handoff sent to the orchestrator). Next assignment: `review t1` after the T1 verifier verdict. At that review,
record the T1-2 baselines under Cross-task patterns.
