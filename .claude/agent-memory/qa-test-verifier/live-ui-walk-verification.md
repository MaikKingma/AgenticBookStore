---
name: live-ui-walk-verification
description: Re-verifying a recorded live UI walk-through (throwaway DB, jar, dev proxy, playwright-cli walk, GQ8-style layout checks, screenshots) and the traps that skew its evidence
metadata:
  type: feedback
---

Rule: reproduce a recorded walk with your own data and scripts, seeded to the same figures, and compare value by value; never cite the implementer's JSON as evidence.

**Why:** seeding the same books, prices and stock through the REST commands made every money string, 409 body and order total byte-comparable with the record, while ids and times differ by design.
**How to apply:**
- One control script with subcommands (`docker-state`, `ports`, `pg-start`, `jar-start`, `jar-stop`, `pg-stop`) writes a dated SP file per call; take `docker-state`/`ports` before and after the session. Use a different throwaway port than the implementer.
- playwright-cli: one `async page => {...}` file per phase via `run-code --filename`, `--raw` redirected to SP; attach `console`/`pageerror`/`response`/`request` listeners inside and `off` them in `finally`. Fill number inputs with `locator.fill` (fires `onChange`). A `window.__marker` proves "no reload".
- Favicon 404s fire no page `response` event; attribute a bare "Failed to load resource: 404" with `m.location().url`. A new context in the same browser does not re-request the favicon, so "first load of a fresh session" needs a brand-new `-s=<name>` session.
- Blur-then-Tab starts from the last focused element (after a form submit the first stop is the next link, not the wordmark); say which start point a "first Tab stop" figure used.
- Table-squeeze check at phone width: `.table-scroll` `clientWidth == scrollWidth` plus `th` heights several line-heights tall proves words break instead of scrolling, even when the document `scrollWidth` passes.
- Negative control for a CSS fix: `git archive <pr-base>` into the scratchpad, `./mvnw -DskipTests package` there (8 s with Node cached), copy the jar out, run it on a second port against the SAME throwaway DB (`--server.port=8081`), and drive both builds with one script that takes the origin from `page.url().split('/shop/')[0]` (`URL` is not defined inside `run-code`). One brand-new `-s=<name>` session per build keeps the favicon check a true first load.
- `playwright-cli` drops `.playwright-cli/` (page snapshots, console log) into the cwd, here the repo root, as untracked files that break a before/after porcelain snapshot; delete them and `rmdir` before the final snapshot.
- Dev-mode console check (the jar serves a production React build, which strips React dev warnings): the Vite proxy target is fixed at 127.0.0.1:8080, so start a SECOND tip jar on 8080 against the same throwaway DB and Vite from the `git archive` extraction with `--port <free> --strictPort`; derive the dev journey from the jar journey with a string-replace script and switch to books that still have stock (the first walk used it up). Dev consoles show only `debug`/`info` lines (`[vite] connecting`, React DevTools hint); count `error`/`warning` types, not all messages.
- Background children started inside `with-budget.sh` keep running after it exits (Vite stayed on its port): write pid files and stop them with an explicit `stop` subcommand plus an `lsof -tiTCP:<port>` sweep.
- Spec scenario "a red frontend test fails the build": drop a failing `*.test.ts` into the tip extraction (never the repo), `./mvnw verify` there, expect `frontend FAILURE` with `bookstore`/`distributor SKIPPED`, then delete the file.
- Any walk that provokes a 4xx on purpose (insufficient stock 409) logs Chromium's `error: Failed to load resource:
  the server responded with a status of 409 (Conflict)` with the API URL as location, in jar and dev mode alike. It is
  the browser's network log, not an app or React diagnostic: quote it, attribute it by its location URL (not a script)
  plus an empty `console.` grep over production `src`, and let the lead judge it.
- Cache-policy fixes: the Tomcat access log is the discriminator. A hashed asset with the jar's 1980 `Last-Modified`
  is served from the heuristic cache on a second `goto` (no access-log line) and answers `If-Modified-Since: now` with
  304, while a `no-cache` entry document shows a fresh `200` line per navigation. The asset 304 doubles as an
  in-run control that the date-validator branch is live on the same server.
- Dev-mode console check WITHOUT a backend (change touches no server code): start Vite alone on a free `--port --strictPort` and mock `/books`, `/inventory` with `page.route` inside `run-code`. Any uncaught exception in a route handler (`new URL` is undefined there) kills the playwright daemon with "Session closed" and no message: parse the href by hand and wrap `route.fulfill` in try/catch, `unrouteAll` in `finally`. The catalogue remembers the last search in sessionStorage, so clear it before a second listing visit or the "first" page shows the searched subset. Select cards with `li:has(article)`, not `getByRole('listitem')` (header nav items count).
- Layout (sticky-footer) measurement with no backend: a `run-code` template with `__W__ __H__ __PHASE__ __PORT__` placeholders filled by `sed` in a driver script, one brand-new `-s=` session per phase and size, `setViewportSize` inside, route-mocked API (12 books for a long page, empty `#/basket` for a short one). Measure `footer.getBoundingClientRect().bottom - innerHeight`, `document.scrollingElement.scrollHeight === innerHeight`, last `article` bottom against footer top in document coordinates, header top after `scrollTo`. The pre-fix baseline can share the one Vite dev server: copy the HEAD bytes over the two production files, walk, copy the fixed bytes back (HMR serves the current file to a fresh session), then compare sha256 and porcelain. Run playwright from a scratch cwd (a driver script that `cd`s), never the repo. A strict-mode locator error on a duplicated link text (`Catalogue` vs `Browse the catalogue`) aborts the walk and leaves later measurements missing: use `exact: true` and check that every named measurement key is present.
- Library walk instead of the CLI daemon: a node `.cjs` file can `require` the `playwright` package bundled under the
  global `@playwright/cli/node_modules/` and launch chromium itself: one `browser.newContext({viewport})` per phase and
  size, listeners per page, `CHECK PASS|FAIL` lines, screenshots to SP, exit code = fails. Run it from a script that
  `cd`s to a scratch dir. End every walk with a control context that calls `console.warn('<tag>')` and assert exactly
  one recorded hit: a jar walk with zero console messages is otherwise indistinguishable from a detached listener.
- A fresh throwaway DB has no books (no Flyway seed): seed through `POST /books/commands/add` (answers 202 with the
  id, lower-case hyphen genres such as `science-fiction` accepted) with valid ISBN-13 check digits, then `GET /books`.
Related: [[readme-and-live-jar-probes]], [[ci-workflow-verification]], [[shell-traps]]
