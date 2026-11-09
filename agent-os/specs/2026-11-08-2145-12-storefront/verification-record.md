# Verification record: 12-storefront (OpenBook), task T10

Written by the T10 implementation agent (impl-t10) and updated by the T11 implementation agent (impl-t11). This record
holds measurements and observations. T10 changed no product file. T11 fixed findings F1 and F2 (section 13) and retook the
14 screenshots; the lines that the fix made stale are updated in place and say "after T11". Plain English, ASCII, no em
dashes.

- Branch: `sdd/12-storefront/T10`, HEAD `eba9b1d2a110b4485be105942e30e0c0897bfaa4` (merge of origin/factory into the spec
  branch, so spec 06 is included). `origin/factory` is `deedc6c5000bf4aeaceba57ed5997d24d14ea2d6`.
- Run on 2026-11-09 between 05:19Z and 05:35Z (local time CET, +01:00 in Maven's "Finished at").
- SP means the session scratchpad directory of this run. Evidence files are `SP/s12-t10-impl-*` (named below). Helper scripts
  are `.claude/comms/tmp/s12-t10-impl-*.sh` and `.js`.
- Browser tool: `playwright-cli` (skill `playwright-cli`), Chromium, one session `s12t10`, viewport set to 1280 x 800 and
  390 x 844, device pixel ratio 1 (screenshots are 1280 x 800 and 390 x 844 pixels). Note: `playwright-cli` itself is a Node
  program from a global Node install. It is the browser tool, not part of the build. The referee, the probes and the dev
  server all ran with the Node-free PATH described in section 1.

## 1. Referee measurement (criteria 1 and 2)

Clean state, script `s12-t10-impl-referee.sh cold`: `rm -rf frontend/node frontend/node_modules frontend/dist` and every
`target/` directory. Evidence `SP/s12-t10-impl-clean-state.txt`:

```
## target dirs left after clean:
## frontend contents after clean:
README.md index.html package-lock.json package.json pom.xml src tsconfig.app.json tsconfig.json tsconfig.node.json vite.config.ts
## which node with PATH=/usr/bin:/bin:/usr/sbin:/sbin:
(exit 1)
JAVA_HOME=<sdkman current>
26.13.0
```

`which node` with that PATH prints nothing (exit 1). The last line is the Node archive version kept in `~/.m2`
(`com/github/eirslett/node/26.13.0`). The archive in `~/.m2` and the npm cache were NOT deleted for the cold run, so the
cold run shows "Installing node ... Unpacking .../node-26.13.0-darwin-arm64.tar.gz" from the local archive and `npm ci`
from the local npm cache.

Command (inside the script, with `bash ~/.claude/scripts/with-budget.sh 900`):
`env PATH=/usr/bin:/bin:/usr/sbin:/sbin JAVA_HOME=<as set> ./mvnw clean verify`

| Run | Evidence file | Reactor | Total time | Result |
|---|---|---|---|---|
| Cold (after clean state) | `SP/s12-t10-impl-referee-cold.log` | frontend 7.489 s, bookstore 12.500 s, distributor 4.896 s | `Total time:  25.133 s` | BUILD SUCCESS |
| Warm (second run) | `SP/s12-t10-impl-referee-warm.log` | frontend 4.440 s, bookstore 12.207 s, distributor 4.257 s | `Total time:  21.101 s` | BUILD SUCCESS |
| Final (after all probes and the no-dist jar) | `SP/s12-t10-impl-final-referee.log` | frontend 4.478 s, bookstore 12.220 s, distributor 4.189 s | `Total time:  21.081 s` | BUILD SUCCESS |

Budget figure (warm): 21.101 s, under the 60 s budget. OQ4 (budget) is closed as under budget; nothing was trimmed.

Quoted lines of the cold log: `[INFO] Installing node version v26.13.0` (line 26), `[INFO] Running 'npm test'` (line 47),
`[INFO]  Test Files  21 passed (21)` (line 56), `[INFO]       Tests  579 passed | 2 expected fail (581)` (line 57),
`[INFO] Copying 6 resources from ../frontend/dist to target/classes/static/shop` (line 95). The warm log says
`[INFO] Node v26.13.0 is already installed.` (line 27).

Counts (final referee log, the same numbers in the cold and warm logs):

| Item | Observed | Baseline in plan.md |
|---|---|---|
| Vitest | 21 files, 581 tests (579 passed + 2 expected fail, the two `it.fails` of `setupTests.test.ts`) | 21 files, 581 tests (FF-T9) |
| Bookstore Java | `Tests run: 936, Failures: 0, Errors: 0, Skipped: 0` | 689 at FF-T1; 936 after the factory merge brought spec 06 tests in |
| Distributor Java | `Tests run: 190, Failures: 0, Errors: 0, Skipped: 0` | 190 |
| ArchitectureTest | 22 tests | 21 at FF-T1 (spec 06 added one) |
| ArchitectureSelfTest | 58 tests | 40 at FF-T1 (spec 06 added 18) |
| StorefrontConfigTest | 5 tests | n/a |
| Spring `Started ... seconds` lines | 15 | 15 |
| `Creating container for image: postgres:18.6` | 1 | 1 |
| Warning inventory (`warn\|deprecat\|self-attach`) | 102 lines | 34 at FF-T3 |
| Error-pattern inventory (`ERROR \|\[ERROR\]\|npm notice\|(!)\|stderr \|`) | 9 lines | 9 at FF-T3 |

The warning inventory grew from 34 to 102 lines. The cause is the factory merge: 67 of the lines are `WARN` log lines of
`DistributorAppServiceImpl` printed by the spec 06 acl tests (for example "Distributor answered status 503 for EAN ..."),
and 1 is a `WARN` of `DefaultHandlerExceptionResolver` printed by `BookCommandsTest` (a JSON parse error resolved to a 400),
68 in total at `eba9b1d`, which is the growth of 68 lines (34 to 102). The frontend part is unchanged: exactly four `[ERROR] npm warn install-scripts ... fsevents@2.3.3` lines (OQ-8,
accepted). The error-pattern count is 9 as in FF-T3. No Vitest `stderr |` block, no React `Warning:` line.

Red-test probe (criterion 2). Uncommitted file `frontend/src/s12t10Probe.test.ts` with
`expect(1 + 1).toBe(3)`, then `./mvnw verify` (no `-fae`, no `clean`, same Node-free PATH, script `s12-t10-impl-mvn.sh
probe-red verify`). Evidence `SP/s12-t10-impl-probe-red.log`, `exit=1`:

```
[ERROR] - 3
[ERROR] + 2
[ERROR]  > src/s12t10Probe.test.ts:5:19
[INFO]  Test Files  1 failed | 21 passed (22)
[INFO]       Tests  1 failed | 579 passed | 2 expected fail (582)
[INFO] Reactor Summary for agenticbookstore-parent 0.0.1-SNAPSHOT:
[INFO] bookstore .......................................... SKIPPED
[INFO] distributor ........................................ SKIPPED
[INFO] agenticbookstore-parent ............................ SUCCESS [  0.005 s]
[ERROR] frontend ........................................... FAILURE [  3.914 s]
[ERROR] BUILD FAILURE
[ERROR] ... frontend-maven-plugin:2.0.2:npm (npm test) on project frontend: Failed to run task: 'npm test' failed.
```

(The failure arrow in the real log is a Unicode character; it is shown here as `>`.) The build failed in `frontend` before
the bookstore was built, and bookstore and distributor are SKIPPED. The probe file was removed with `rm`.

`-DskipTests verify` probe (script `s12-t10-impl-mvn.sh probe-skiptests -DskipTests verify`, run after
`rm -rf bookstore/target/classes/static/shop` so the result is fresh). Evidence `SP/s12-t10-impl-probe-skiptests.log`:
`[INFO] --- frontend:2.0.2:npm (npm test) @ frontend ---` followed by `[INFO] Skipping execution.`; the count of `RUN  v`
lines is 0; `BUILD SUCCESS` in 3.656 s; `bookstore/target/classes/static/shop/index.html` exists again, 403 bytes at T10 (441 bytes after T11, which added the icon
link), modified 06:32 (after the removal at 06:32 and after the preceding red probe).

Snapshot equality (GQ5). `SP/s12-t10-impl-snap-before.txt` (taken before the first probe) holds `git status --porcelain
--untracked-files=all` (empty) and `shasum -a 256` of the 358 tracked files under `frontend bookstore distributor pom.xml
.github`. `SP/s12-t10-impl-snap-final.txt` (after the last probe and the cleanup) is identical except for the 14 new
untracked PNG files of the screenshot deliverable. `diff` output is exactly those 14 `??` lines and nothing else.
`git status` shows no change to any tracked file.

## 2. Isolation (GQ1) and the live session

- No `docker compose` command was run at any point in this task. The container `bookstore-postgres-1` does not exist on this
  machine: `docker inspect -f '{{.State.Status}} {{.State.StartedAt}}' bookstore-postgres-1` answers `error: no such
  object: bookstore-postgres-1` both before (`SP/s12-t10-impl-docker-before.txt`, 05:19:40Z) and after
  (`SP/s12-t10-impl-docker-after.txt`, 05:34:31Z). So there was no shared container to disturb. During the session other
  checkouts started their own Testcontainers containers (`testcontainers-ryuk-...`, one anonymous Postgres); they are not
  mine and were not touched.
- Ports 8080, 5173 and 55432 were free before (`lsof -nP -iTCP:<port> -sTCP:LISTEN` empty) and free after.
- Throwaway database: `docker run --name s12-t10-impl-pg -e POSTGRES_DB=bookstore -e POSTGRES_USER=bookstore -e
  POSTGRES_PASSWORD=bookstore -p 127.0.0.1:55432:5432 -d postgres:18.6`. Removed at the end with `docker rm -f
  s12-t10-impl-pg`; `docker ps -a --filter name=s12-t10` is empty afterwards.
- Bookstore start (script `s12-t10-impl-bookstore.sh`): `java -jar bookstore/target/bookstore-0.0.1-SNAPSHOT.jar
  --spring.docker.compose.enabled=false --spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/bookstore
  --spring.datasource.username=bookstore --spring.datasource.password=bookstore` on 8080 (free). Log
  `SP/s12-t10-impl-bookstore-live.log`: `Started BookstoreApplication in 2.265 seconds`; zero lines match `docker compose`
  (case-insensitive); Flyway on the throwaway database: `Database: jdbc:postgresql://127.0.0.1:55432/bookstore (PostgreSQL
  18.6)`, `Migrating schema "public" to version "0 - baseline"` through version 3, `Successfully applied 4 migrations to
  schema "public", now at version v3`. Zero `ERROR` and zero `WARN` lines in the whole live log. No assistant key was set;
  `ANTHROPIC_API_KEY` was never set or read.
- Walk-through data was created only through the REST command endpoints (script `s12-t10-impl-seed.sh`, output
  `SP/s12-t10-impl-seed.txt`), all answers `202`:

| Book | Genre | Price | Stock received | Role in the walk-through |
|---|---|---|---|---|
| A Tale of Two Cities (id 0ae352c8-59ef-46fa-a1f5-e8746aa83364) | fiction | 12.50 | 5 | trailing zero, main order |
| Wuthering Heights (e314a1ed-4113-4cd4-a4ec-7d70fd1031d5) | fiction | 8.00 | 2 | low stock, provokes the 409 at 3 |
| Frankenstein (57192aca-fc60-4484-819c-2c8a0854701d) | science-fiction | 8.99 | 3 | second genre |
| Deep Learning (6c3fcbfd-b6fe-4c8f-9871-9ac2b02a7d4e), three authors | technology | 49.95 | 10 | third genre, long author list |
| Pride and Prejudice (4bf93b17-8e73-41a0-b932-6c147f21beb1) | fiction | 9.99 | none | out of stock |

  Two earlier orders for `reader@example.com` were also placed through `POST /orders/commands/place` (b65170a0-bda6-4c7e-b1e2-03cdc0a01284 before the
  browser order, 18ab61d7-943a-437e-bc08-464c7d41544f after it) so the history has three rows in a known order.

## 3. Live HTTP checks (criterion 3)

Script `s12-t10-impl-curl.sh`, evidence `SP/s12-t10-impl-curl-live.txt`, against the jar on `http://127.0.0.1:8080` (this
checkout's own throwaway instance):

| Request | Status and header lines |
|---|---|
| `curl -i /` | `HTTP/1.1 302` and the literal `Location: http://127.0.0.1:8080/shop/` (absolute, resolves to `/shop/` on the same origin; GQ10) |
| `curl -i /shop/` | `HTTP/1.1 200`, `Content-Type: text/html;charset=UTF-8`, `Content-Length: 441` after T11 (403 at T10, before the icon link was added); body holds `<title>OpenBook</title>`, `<link rel="icon" href="data:," />` and `<div id="root"></div>` |
| `curl -i /shop` | `HTTP/1.1 200`, `Content-Type: text/html;charset=UTF-8` |
| `/shop/assets/index-CgCUFTxD.js` (after T11; `index-BWOTWKFB.js` at T10) | `HTTP/1.1 200`, `Content-Type: text/javascript`, `Content-Length: 242127` at T10 (the file in `frontend/dist/assets` after T11 is also 242127 bytes) |
| `/shop/assets/montserrat-latin-400-normal-BLhwKU8k.woff2` | `HTTP/1.1 200`, `Content-Type: font/woff2`, `Content-Length: 18780` |
| `/shop/missing.js` | `HTTP/1.1 404`, `Content-Type: application/json`, body is the Boot error JSON |
| `/books` | `HTTP/1.1 200`, `Content-Type: application/json` (`[]` on the empty database; later the five books) |
| `/actuator/health` | `HTTP/1.1 200`, body `{"components":{"db":{"status":"UP"},...},"status":"UP"}` |

Dev mode (script `s12-t10-impl-dev.sh`, evidence `SP/s12-t10-impl-dev-check.txt` and `SP/s12-t10-impl-dev-server.log`).
Command, run in `frontend/`: `PATH="<project-root>/frontend/node:/usr/bin:/bin:/usr/sbin:/sbin" node/npm run dev`
(the documented form of `frontend/README.md`). Output: `VITE v8.3.6  ready in 108 ms` and `Local:
http://127.0.0.1:5173/shop/`. `curl -i http://127.0.0.1:5173/shop/` gave `HTTP/1.1 200 OK`, `Content-Type: text/html`, the
body has `<title>OpenBook</title>`, `<div id="root"></div>` and the Vite client scripts. `/books` through 5173 gave `HTTP/1.1
200 OK`, `content-type: application/json`, 1157 bytes; `cmp` of that body with `/books` fetched from 127.0.0.1:8080 right
afterwards printed `cmp: identical` (both 1157 bytes). Port 8080 was this task's own bookstore instance. The dev server
was stopped (`5173 free`).

## 4. Jar without the frontend (GQ14)

After the live session: `rm -rf frontend/dist`, then (script `s12-t10-impl-mvn.sh nodist-package -q -pl bookstore
-DskipTests clean package`, exit 0). `jar tf bookstore/target/bookstore-0.0.1-SNAPSHOT.jar` written to
`SP/s12-t10-impl-nodist-jar-list.txt`: `grep -c "static/shop"` prints `0` and `grep -n "static/"` prints nothing. For
comparison the full referee jar lists `BOOT-INF/classes/static/shop/`, `BOOT-INF/classes/static/shop/index.html` and the
assets (after T11: `index-CgCUFTxD.js` and `index-Dn8vvogM.css`, three `montserrat-latin-*-normal-*.woff2`; at T10 the names
were `index-BWOTWKFB.js` and `index-BK3v9Bi3.css`, the CSS hash changes because `global.css` changed). A short run of the no-dist jar
(same isolation rules, `SP/s12-t10-impl-curl-nodist.txt`): `/shop/` `HTTP/1.1 404` with the Boot error JSON `{"timestamp":
...,"status":404,"error":"Not Found","path":"/shop/"}`; `/books` `HTTP/1.1 200` with the five books; `/actuator/health`
`HTTP/1.1 200`; `/` still answers `302` with `Location: http://127.0.0.1:8080/shop/` (so a build without the frontend
redirects to a 404 page; this matches the design note, which only promises that `/shop/` is 404 and the REST API works).
The jar was stopped, the final referee (section 1, "Final") rebuilt everything.

## 5. Walk-through results (criterion 4)

Browser walk-through at 1280 x 800 unless stated. Evidence files are `SP/s12-t10-impl-*.json` (one per script).

| # | Step | Observed | Result |
|---|---|---|---|
| 1 | Catalogue listing | five cards in server order: A Tale of Two Cities 12.50 EUR In stock, Deep Learning 49.95 EUR In stock (authors "Ian Goodfellow, Yoshua Bengio, Aaron Courville"), Frankenstein 8.99 EUR In stock, Pride and Prejudice 9.99 EUR Out of stock, Wuthering Heights 8.00 EUR In stock; footer text "OpenBook, the J-Fall 2026 workshop shop by OpenValue"; title "OpenBook" | pass |
| 2 | Search "austen" + genre FICTION | request `http://127.0.0.1:8080/books?q=austen&genre=FICTION`; one card "Pride and Prejudice"; no alert. A search with no match shows "No books match your search." | pass |
| 3 | Deep link, fresh load of `/shop/#/books/0ae352c8-...` (from about:blank) | requests `/books/<id>` and `/inventory/<id>`; shows ISBN 9780141439600, Authors "Charles Dickens" (list), Genre FICTION, Published 1859, Price 12.50 EUR, "In stock4 available" (badge "In stock" plus "4 available"), enabled "Add to basket" | pass |
| 4 | Out of stock detail (Pride and Prejudice) | "Out of stock", "Add to basket" disabled | pass |
| 5 | Add twice, no reload (FF-T7 item 7) | header badge 0 -> 1 -> 2 on the detail page; a `window.__marker` set before the clicks was still set (same document); `openbook.basket` = one line, quantity 2, price "12.50"; text "Added to your basket. View basket" | pass |
| 6 | Add a second book through hash navigation | badge 3 (items, sum of quantities); marker still set | pass |
| 7 | Basket quantity changes (input) | set 3: line total "37.50 EUR", estimated total "45.50 EUR", badge 4; tries 0, 11, empty, 2.5: input stays 3, stored quantity stays 3, message "Quantity must be between 1 and 10."; 10 accepted ("125.00 EUR"), 1 accepted ("12.50 EUR"); then Tale 3 and Wuthering Heights 3: "37.50 EUR", "24.00 EUR", estimated "61.50 EUR", badge 6 | pass |
| 8 | Basket survives a reload (FF-T7 item 7) | after `page.reload()` the marker is gone (real reload), `openbook.basket` still Tale x3 and Wuthering x3, badge 6, estimated total 61.50 EUR | pass |
| 9 | Checkout page | lists "A Tale of Two Cities, 3 x 12.50 EUR: 37.50 EUR", "Wuthering Heights, 3 x 8.00 EUR: 24.00 EUR", "Estimated total: 61.50 EUR", text "The bookstore confirms the total when the order is placed.", field "E-mail address", button "Confirm order" | pass |
| 10 | Insufficient stock (FF-T8 item 6) | `POST /orders/commands/place` with `{"customerEmail":"reader@example.com","lines":[{Tale,3},{Wuthering,3}]}` answered `409`, `code` `insufficient-stock`, `details` `[{"requested":"3","available":"2","bookId":"e314a1ed-..."}]`. Alert text, quoted: "Insufficient stock / Not enough stock for 1 of the requested books / Book Wuthering Heights: requested 3, available 2" (basket title, not the id). Basket text in `sessionStorage` byte-identical before and after, badge still 6, e-mail field keeps its text, "Confirm order" enabled again | pass |
| 11 | Checkout success (FF-T7, FF-T8) | after setting Wuthering to 1: request body `{"customerEmail":"reader@example.com","lines":[{"bookId":"0ae352c8-...","quantity":3},{"bookId":"e314a1ed-...","quantity":1}]}`, answer `202` `{"id":"1929ad29-d61f-4c20-9030-57675ee8aba3"}`; no reload (marker kept); hash `#/orders/1929ad29-...`; badge "0"; `openbook.basket` is `null` | pass |
| 12 | Order page equals `GET /orders/<id>` (FF-T8 item 6) | page: Status PLACED, Placed "2026-11-09 05:26:15 UTC", Shipped "Not shipped yet", E-mail reader@example.com, Total 45.50 EUR, lines "A Tale of Two Cities 9780141439600 3 12.50 EUR 37.50 EUR" and "Wuthering Heights 9780141439563 1 8.00 EUR 8.00 EUR". API (fetched by the browser): `placedAt` "2026-11-09T05:26:15.609332Z" (cut to `YYYY-MM-DD HH:MM:SS UTC` equals the page), `total` "45.50", line totals "37.50" and "8.00" | pass |
| 13 | Orders history | e-mail `reader@example.com`, request `/orders?customerEmail=reader%40example.com` answered 200; three rows in this order: 18ab61d7-... (3 items, 108.89 EUR, 05:26:32 UTC), 1929ad29-... (4 items, 45.50 EUR, 05:26:15 UTC), b65170a0-... (2 items, 21.49 EUR, 05:22:54 UTC); the list of link ids equals the id order of the API answer (`sameOrderAsApi` true); each row links to its order page | pass |
| 14 | Sticky header (FF-T6 item 9), phone catalogue | `SP/s12-t10-impl-sticky-390.json`: before scrolling `getBoundingClientRect().top` 0; after `scrollY` 600 the header `top` is 0 and `getComputedStyle(header).position` is `sticky` (document height 1954 px at 390 wide) | pass |
| 15 | Browser console and HTTP errors per page | script `s12-t10-impl-pw-sweep.js`, evidence `SP/s12-t10-impl-sweep-1280.json` and `...-390.json`: for all seven routes at both widths zero console errors, zero warnings, zero page errors, zero HTTP answers of 400 or more. Separately, at T10: the very first page load of the browser session logged one `404` for `/favicon.ico` (finding F2, fixed by T11: a brand-new browser session after T11 logs 0 console errors and sends no `/favicon.ico` request, section 13; the same sweep after T11 is in section 13 too). The deliberate error paths logged one console error each: the `409` of row 10 and the `404` of `POST /assistant/chat` (rows 16 and 17) | pass (F2 fixed by T11) |
| 16 | Assistant page, spec 08 absent (FF-T9) | h1 "Assistant", text "Ask about books, authors and what is in stock.", empty `Conversation` list, "0 / 1000 characters", Send disabled; 1001 characters (`fill`): "1001 / 1000 characters, too long" and Send disabled; 1000 characters: "1000 / 1000 characters" and Send enabled | pass |
| 17 | One send with spec 08 absent (GQ9, FF-T9) | message "Do you have anything by Austen?"; `POST /assistant/chat` request body `{"conversationId":"b7bc3fd0-4054-4eb7-b272-6bc71c1baa8d","message":"Do you have anything by Austen?"}`; status `404`, `Content-Type: application/json`, body `{"timestamp":"2026-11-09T05:27:23.119Z","status":404,"error":"Not Found","path":"/assistant/chat"}`; alert text exactly `Not Found`; the field keeps its text (31 characters), Send enabled again; no text names an API key variable | recorded (round trip pending, section 10) |
| 18 | Conversation id | `openbook.conversationId` = `b7bc3fd0-4054-4eb7-b272-6bc71c1baa8d`, matches `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$` (lower-case v4) and equals the request body id; after `page.reload()` a second send ("Second message after reload") posted the same `conversationId`; session keys at the end: `openbook.conversationId`, `openbook.lastSearch` | pass |

Not walked live (covered only by the Vitest suites): the unknown-genre 422 (the genre field is a select with fixed
options), the 400 `validation-failed` for the e-mail, the 21st distinct book, a DISCONTINUED book, the 429 and 503 answers.

## 6. GQ8 live checks

Script `s12-t10-impl-pw-gq8.js` (reloads the page, then measures; one file per page and width,
`SP/s12-t10-impl-gq8-<page>-<1280|390>.json`). "Tab" is one Tab press from a freshly loaded page; the first stop is the
"OpenBook" wordmark link on every page.

| Page | scrollWidth at 390 (innerWidth 390) | scrollWidth at 1280 | grid tracks | outline after one Tab (390 and 1280) | transition and reduced motion |
|---|---|---|---|---|---|
| Catalogue | 390 | 1280 | 390: 1 track (`358px`); 1280: 3 tracks (`357.328px 357.328px 357.344px`) | `outlineStyle` solid, 3px, `rgb(61, 125, 201)` | `button.button.button-primary` `background-color 0.15s`; with `prefers-reduced-motion: reduce` emulated the same element reports `none 0s` |
| Book detail | 390 | 1280 | no grid | solid 3px, same colour | same button, `0.15s`, reduced `none 0s` |
| Basket | 390 | 1280 | no grid | solid 3px | `button.button-secondary` and `a.button-primary`, both `0.15s`, reduced `none 0s` |
| Checkout | 390 | 1280 | no grid | solid 3px | button-primary `0.15s`, reduced `none 0s` |
| Order | 390 | 1280 | no grid | solid 3px | no element declares a transition on this page (the check is vacuous here) |
| Orders | 390 | 1280 | no grid | solid 3px | button-primary `0.15s`, reduced `none 0s` |
| Assistant | 390 (and 390 with 1000 `x` characters without spaces, and after a failed send) | 1280 | no grid | solid 3px; textarea `solid 3px`; Send (enabled, via Tab) `BUTTON:Send:solid 3px rgb(61, 125, 201)` | button-primary `0.15s`, reduced `none 0s` |

Every focusable element reached by the following Tab presses on every page also reported an outline that is not `none`
(the next eight stops are listed in each json file; the only `none` is the Tab that wraps to the document body).

Order page at 390 at T10 (`SP/s12-t10-impl-tablescroll-390.json`): document `scrollWidth` 390; the `.table-scroll` wrapper has
`overflow-x: auto`, `clientWidth` 316 and `scrollWidth` 316, so it did NOT scroll (finding F1). After T11 the same wrapper
has `clientWidth` 316 and `scrollWidth` 561, so it scrolls, and the document `scrollWidth` is still 390 (finding F1 is
fixed by T11, section 13).

## 7. Screenshots (criterion 5, Definition of done)

Files in `agent-os/specs/2026-11-08-2145-12-storefront/visuals/`, exactly 14. All 14 were retaken by T11 (section 13)
after the fix, so the descriptions below are the T11 versions. Taken with Playwright `page.screenshot` through
`playwright-cli run-code` (script `s12-t11-impl-pw-shots.js`) at device pixel ratio 1 (viewport only), on a fresh build of
the T11 tip with the T10 seed data (new ids, same books and prices) and the order of 3 x A Tale of Two Cities (12.50) and
1 x Wuthering Heights (8.00). `sips -g pixelWidth -g pixelHeight` (script `s12-t11-impl-sips.sh`, `SP/s12-t11-impl-sips.txt`):
every `*-desktop.png` is 1280 x 800 and every `*-phone.png` is 390 x 844, and `ls` prints 14 files. The files were written
at 09:2x local on 2026-11-09, after the last change of `frontend/src` and `frontend/index.html` (the CSS comment edited
afterwards does not reach the built CSS). I opened every file. Descriptions:

| File | What it shows |
|---|---|
| `catalogue-desktop.png` | white sticky header with "Open" in navy and "Book" in blue, nav Catalogue (underlined blue), Basket with badge 4, Orders, Assistant; snow background, "Catalogue" heading, a white rounded search card (Search, Title, Author, Genre with "Any genre", teal "Search" button), then a three column grid of white cards with soft shadow: A Tale of Two Cities (Charles Dickens, FICTION, 12.50 EUR), Deep Learning (three authors, TECHNOLOGY, 49.95 EUR), Frankenstein (Mary Shelley, SCIENCE_FICTION, 8.99 EUR), each with a teal "In stock" badge; the second row starts with Pride and Prejudice and Wuthering Heights (cut off by the viewport) |
| `catalogue-phone.png` | same content in one column; the header wraps to three rows (wordmark; Catalogue, Basket with badge 4 and Orders; "Assistant" on its own row), search card with stacked fields, the first book card (A Tale of Two Cities) starting at the bottom edge |
| `book-detail-desktop.png` | "A Tale of Two Cities" heading, "Back to the catalogue" link, white card with ISBN, Authors (bullet list), Genre, Published 1859, Price 12.50 EUR, Availability "In stock" badge plus "1 available" and "Low stock" (the order of the task took the other copies), teal "Add to basket" button; dark navy footer with the OpenBook text, snow band below the footer |
| `book-detail-phone.png` | same card in one column, Availability "In stock", "1 available" and "Low stock"; the footer starts at the bottom edge |
| `basket-desktop.png` | "Basket" with two white line cards (A Tale of Two Cities 12.50 EUR each, quantity 3, line total 37.50 EUR; Wuthering Heights 8.00 EUR each, quantity 1, line total 8.00 EUR), outlined "Remove" buttons, "Estimated total: 45.50 EUR" and the estimate sentence; Basket nav item active with badge 4 |
| `basket-phone.png` | the same two line cards in one column, "Estimated total: 45.50 EUR" at the bottom edge, badge 4 |
| `checkout-desktop.png` | "Checkout", back link, plain lines "A Tale of Two Cities, 3 x 12.50 EUR: 37.50 EUR" and "Wuthering Heights, 1 x 8.00 EUR: 8.00 EUR", "Estimated total: 45.50 EUR", a narrow white form card with the e-mail "reader@example.com" (focus ring visible) and a teal "Confirm order" button; footer visible |
| `checkout-phone.png` | the same page in one column, the form card at full width, footer text wrapped to two lines |
| `order-desktop.png` | "Order" with a white card: Order number 57c4e1b6-5c4a-49b4-9668-65060cc6975d, Status PLACED, Placed "2026-11-09 08:21:28 UTC", Shipped "Not shipped yet", E-mail, Total 45.50 EUR, then an Items table with five readable columns (Title, ISBN, Quantity, Unit price, Line total) and two lines; Orders nav item active, badge 4 |
| `order-phone.png` | the order card in one column, scrolled down so that the Items table is in the viewport, and the table wrapper scrolled sideways to its right end (it is 561 px wide in a 316 px wrapper): the column headings "Quantity", "Unit price" and "Line total" are whole words on one line, and the rows read "3 | 12.50 EUR | 37.50 EUR" and "1 | 8.00 EUR | 8.00 EUR" (finding F1 is fixed); the Title and ISBN columns are to the left of the visible part; the footer is below the card |
| `orders-desktop.png` | "Orders", form card with "reader@example.com" and a dark navy "Show orders" button (hover state, the pointer rests on it), then two white cards, each a link "Order <id>" followed by "PLACED | date UTC | Items: n | total EUR", newest first (45.50 EUR, 4 items; 21.49 EUR, 2 items) |
| `orders-phone.png` | the same two cards in one column, text wrapped across lines, no horizontal overflow |
| `assistant-desktop.png` | empty state: "Assistant", "Ask about books, authors and what is in stock.", a white card with the label "Your message", an empty textarea, "0 / 1000 characters" and a faded teal disabled "Send" button; no conversation messages (spec 08 is absent) |
| `assistant-phone.png` | the same empty state in one column; footer wrapped to two lines |

Every file shows the "OpenBook" wordmark in the header, live data (except the assistant, which has no data by design) and no
error alert. The phone files are single column with no horizontal overflow (section 6).
Judgment items for Maik (not decided here): "modern", "generous whitespace", "prominent search bar".

## 8. Continuous integration cache evidence (FF-T3)

Save run: push run `42530533668` on `a2cb46a0985237967ac5b64abaea9c3e00da289f` (the T3 merge). Restore run: a later push
run, `42566395829` on `eba9b1d2a110b4485be105942e30e0c0897bfaa4` (status success). Fetched with `gh run view <id> --log --job
<job id>` (script `s12-t10-impl-ci.sh`, logs `SP/s12-t10-impl-ci-*.log`).

Saves (run 42530533668, first run: the keys did not exist yet):

| Job | Line (Actions timestamp) |
|---|---|
| verify (job 128883537232) | `2026-11-08T22:30:13.8806946Z Cache not found for input keys: node-Linux-5deaeb98...`; `2026-11-08T22:30:14.2430632Z Cache not found for input keys: npm-Linux-cde33fc8..., npm-Linux-` |
| verify | `2026-11-08T22:31:21.3964679Z Cache saved with key: npm-Linux-cde33fc83ee0f8085821b6700b2600b33ef9eeeece8c789138c13f906aae2775` |
| verify | `2026-11-08T22:31:23.2856250Z Cache saved with key: node-Linux-5deaeb98984c2f4719ede12b52a55575b445d13f25bc25f10aae622b24d6d0f8` |
| windows-build (job 128883537125) | `2026-11-08T22:31:10.4775467Z Cache saved with key: npm-Windows-27fa23e3054ad7a91f48a72eff7a25980e8153e15eb7334fc563bb39e807726c` |
| windows-build | `2026-11-08T22:31:12.3283509Z Cache saved with key: node-Windows-16b8ff95ed0cf6c9e7b8c9c7f6b3653980d06eebd2d690be9f82cac19f19b852` |

Restores (run 42566395829, same keys because the lock file and `frontend/pom.xml` did not change):

| Job | Line (Actions timestamp) |
|---|---|
| verify (job 128993703619), step "Cache frontend Node.js" | `2026-11-09T05:17:10.9799034Z Cache hit for: node-Linux-5deaeb98984c2f4719ede12b52a55575b445d13f25bc25f10aae622b24d6d0f8` and `2026-11-09T05:17:12.0782059Z Cache restored from key: node-Linux-5deaeb98...` |
| verify, step "Cache npm packages" | `2026-11-09T05:17:12.4610193Z Cache hit for: npm-Linux-cde33fc83ee0f8085821b6700b2600b33ef9eeeece8c789138c13f906aae2775` and `2026-11-09T05:17:13.0405513Z Cache restored from key: npm-Linux-cde33fc8...` |
| windows-build (job 128993703495), step "Cache frontend Node.js" | `2026-11-09T05:17:13.7886242Z Cache hit for: node-Windows-16b8ff95ed0cf6c9e7b8c9c7f6b3653980d06eebd2d690be9f82cac19f19b852` and `2026-11-09T05:17:15.1008541Z Cache restored from key: node-Windows-16b8ff95...` |
| windows-build, step "Cache npm packages" | `2026-11-09T05:17:15.4989336Z Cache hit for: npm-Windows-27fa23e3054ad7a91f48a72eff7a25980e8153e15eb7334fc563bb39e807726c` and `2026-11-09T05:17:16.6122843Z Cache restored from key: npm-Windows-27fa23e3...` |

The Windows npm restore is included. Both jobs of both runs concluded `success`.

## 9. Definition of done, line by line

| Definition of done line | Evidence |
|---|---|
| OpenBook branding: screenshots of the seven pages at desktop and phone width committed in the spec folder's `visuals/` and linked from the spec PR | 14 files, section 7 (the orchestrator links them from the spec PR; committing is the orchestrator's step) |
| `./mvnw clean verify` green from the repo root, including the frontend tests and build; time recorded | section 1: BUILD SUCCESS three times, cold 25.133 s, warm 21.101 s, final 21.081 s; Vitest 21 files and 581 tests; bookstore 936; distributor 190 |
| `GET /` redirects to `/shop/`; every page of this spec works in a browser against the running bookstore | section 3 (`302`, `Location: http://127.0.0.1:8080/shop/`), section 5 rows 1 to 18; the assistant page loads and behaves per its tests, its live round trip is pending spec 08 (section 10) |
| REST paths of specs 02 to 08 unchanged (diff of the `*Commands` and `*Queries` classes empty) | `git diff origin/factory --stat -- "*Commands.java" "*Queries.java"` prints nothing (origin/factory is `deedc6c`); also `git diff HEAD --stat -- frontend bookstore pom.xml .github` is empty for this task |
| No global Node.js was needed | `env PATH=/usr/bin:/bin:/usr/sbin:/sbin which node` prints nothing (exit 1); the referee, the probes and `npm run dev` ran with that PATH (plus the local `frontend/node`) |
| `grep -ri "cors" bookstore/src frontend/src` returns nothing | run literally from the repo root: no output |

Further greps, run literally from the repo root (`SP/s12-t10-impl-greps.txt`): clock, random and locale
(`\bDate\b|performance\.now|Math\.random|setTimeout|setInterval|useFakeTimers|toLocale|Intl\.`) 0 lines; module `let`/`var` 0;
`parseFloat` 0; inline style colours 0; `document.title` 0; `innerHTML` family 0; stripped paths and secrets
(`docs/spec-pack|agent-os|\.claude|CLAUDE\.md|ANTHROPIC_API_KEY` in frontend, bookstore/src, poms, .github) 0; caret or
tilde pins in `frontend/package.json` 0; `@ts-ignore` and `eslint-disable` 0; Java `@Disabled`/`@DirtiesContext`/assume
forms 0; `grep -rn PagePending frontend/src` 0. The skip/only/todo/fails grep prints exactly the two accepted
`it.fails` lines: `frontend/src/setupTests.test.ts:56` and `:64` (A2). The CDN grep
(`googleapis|gstatic|jsdelivr|unpkg|fontsource|cdnjs|https?://` in `frontend/src` and `frontend/index.html`) prints only
two lines of `frontend/src/assets/fonts/OFL.txt` (licence text, exempt by A4).

Non-ASCII baseline, in the Bash tool: `rg -n "[^\x00-\x7F]" frontend/src frontend/index.html frontend/README.md
frontend/pom.xml frontend/package.json frontend/vite.config.ts frontend/.npmrc pom.xml bookstore/pom.xml .github bookstore/src`
prints exactly the baseline of FF-T1 and nothing else:
`bookstore/src/test/java/nl/openvalue/bookstore/domain/IsbnTest.java:67` and `:74` (the Arabic-Indic digit test data).
Positive control: a file holding one U+2003 (made by `printf` with the bytes e2 80 83, `SP/s12-t10-impl-nonascii-control.txt`)
gives `rg -c "[^\x00-\x7F]"` `1`. `rg` skips binaries (woff2, png). This record itself is plain ASCII.

## 10. Manual checks pending spec 08 (never faked)

- The live assistant round trip (`/shop/#/assistant` against a running spec 08 endpoint: a user message and a `reply` shown,
  the same conversation id reused) is pending the manual check after spec 08 merges (GQ9). Observed so far, with the
  endpoint absent: the page loads with its empty state, the counter and the length limit behave, one send posts
  `{conversationId, message}` and the server answers `404` (Boot error JSON), shown as the alert "Not Found".
- The 429 and 503 body shapes (OQ-4) are only checked against the T9 fixtures; re-check them live after spec 08: 503
  `assistant-offline` shown as "The assistant is offline", 429 shown as "Too many requests, try again in a minute", and a
  400 `validation-failed` for `message`. Also re-check that spec 08's `AssistantExceptionHandler` stays package-scoped (it must
  not turn the static 404 of `/shop/missing.js` into a ProblemDetail).
- "Modern", "generous whitespace" and "prominent search bar" (D7) stay Maik's judgment from the screenshots (GQ8).

## 11. Findings and open questions for the orchestrator

- OQ4 (budget): closed, warm referee 21.101 s against the 60 s budget (cold 25.133 s).
- F1 (visual defect, product code): FIXED BY T11 (CSS addition in `global.css`, evidence in section 13: at 390 px the
  wrapper scrolls, `clientWidth` 316 and `scrollWidth` 561, every word has one client rect, every table heading is 40.5 px
  tall as at 1280). T10 observation as it was: on the order page at 390 px width the Items table does not scroll
  inside `.table-scroll`; it squeezes to the 316 px container (`overflow-wrap: anywhere` on `th` and `td`, `table-layout:
  auto`), so headings and numbers break inside words ("Qu / ant / ity", "12.5 / 0 / EUR", "Wuthe / ring / Height / s").
  The document does not overflow (`scrollWidth` 390), so the GQ8 overflow check passes, but FF-T8's expectation "while
  `.table-scroll` scrolls" does not hold (`scrollWidth` 316 equals `clientWidth` 316). Evidence:
  `SP/s12-t10-impl-tablescroll-390.json`, `SP/s12-t10-impl-shot-order-fullpage-390.png`. T10 changed no product file.
- F2 (informational): FIXED BY T11 (`<link rel="icon" href="data:," />` in `frontend/index.html`, evidence in section 13:
  a brand-new browser session, first load of `/shop/`, sends no `/favicon.ico` request and logs 0 console errors). T10
  observation as it was: the first page load of a fresh browser session logs `Failed to load resource: the server responded
  with a status of 404 () @ http://127.0.0.1:8080/favicon.ico`; `frontend/index.html` has no favicon link and the bookstore
  has no `/favicon.ico`. Later loads did not log it again.
- F3 (judgment): on the short pages (book detail, checkout, order, orders, assistant) the dark footer ends above the
  bottom of the 800 px viewport and a snow band shows below it (see `book-detail-desktop.png`).
- F4 (judgment): at 390 px the header wraps to three rows and the sticky header takes about 140 px of the 844 px viewport.
- F5 (by design, A7): the catalogue restores the last search from `sessionStorage` (`openbook.lastSearch`), so after a
  search without a match the catalogue stays empty on reload until the key is cleared. I cleared it by hand once before the
  phone pass.
- F6 (informational): a build without the frontend still redirects `/` to `/shop/`, which then answers 404 (section 4).
- F7 (informational): the shared container `bookstore-postgres-1` does not exist on this machine, so the "identical before
  and after" evidence reads "absent before and after". Other checkouts' Testcontainers were running during the session.
- No decision is open that blocks T10.

## 12. Scope

T10 (as it was): only `agent-os/specs/2026-11-08-2145-12-storefront/verification-record.md` and the 14 files
`agent-os/specs/2026-11-08-2145-12-storefront/visuals/*.png` were created. `git status --porcelain --untracked-files=all`
lists exactly those 15 paths. Nothing is running: the bookstore jar, the dev server and the browser session are stopped,
the Postgres container is removed, ports 8080, 5173 and 55432 are free. Scratch output of the browser tool
(`.playwright-cli/`) was removed. The `target/` directories and `frontend/dist` hold the output of the final referee run.

T11: the product diff is `frontend/src/styles/global.css` (16 added lines, no removed line, `git diff -U0` shows only `+`
lines), `frontend/index.html` (1 added line) and `frontend/src/indexHtml.test.ts` (one inserted test); new file
`frontend/src/styles/orderTable.test.ts`; this record and the 14 PNG files. `OrderPage.tsx`, `.card` and every other
existing line of `global.css` are unchanged. Nothing is running after T11: the bookstore jars, the browser sessions and the
throwaway Postgres container are stopped and removed, scratch output of the browser tool (`.playwright-cli/`) was removed.

## 13. T11 fix: order table at 390 px and the favicon (findings F1 and F2)

Written by impl-t11 on 2026-11-09 (branch `sdd/12-storefront/T11`, base `fa6ea90`, the tip of `origin/sdd/12-storefront/spec`).
SP is the session scratchpad; evidence files are `SP/s12-t11-impl-*`, scripts `.claude/comms/tmp/s12-t11-impl-*`.

Root cause of F1. The order cells (`th`, `td` of `.order-lines`) inherit `overflow-wrap: anywhere` from `.card`
(`global.css`, the `.card` rule). That value lowers the minimum content width of every cell to one character, so the
`width: 100%` auto-layout table fits the 316 px wrapper by breaking words in the middle, and `.table-scroll` (which keeps
`overflow-x: auto`) never has anything to scroll.

Fix, both as additions after the `.order-lines th` rule (nothing removed, `.card` untouched):

1. `.order-lines th, .order-lines td { overflow-wrap: normal; }` stops the mid-word breaks.
2. `.order-lines th, .order-lines td:not(:first-child) { white-space: nowrap; }`. The first live measurement with the first
   rule alone showed the table at 482 px (the wrapper scrolled, no word was broken) but "Unit price" and "Line total" wrapped
   at their spaces, so every heading was 64.5 px tall against 40.5 px at 1280. The task asks for the same heading height on
   the phone, so the headings and the short value cells (ISBN, quantity, prices) stay on one line; the title column still
   wraps at its spaces. This second rule is an addition beyond the one the task named; it is covered by its own test.

Fix of F2: one `<link rel="icon" href="data:," />` in `frontend/index.html` (no URL, no colour literal). The built
`frontend/dist/index.html` and the live `/shop/` body carry it (`Content-Length: 441`, was 403).

Tests (jsdom has no layout, so the text is pinned):

- New `frontend/src/styles/orderTable.test.ts`, 8 tests: the effective `overflow-wrap` of the rule whose selector list is
  exactly `.order-lines th, .order-lines td` is `normal`; the effective `white-space` of the rule `.order-lines th,
  .order-lines td:not(:first-child)` is `nowrap`; `.table-scroll` keeps `overflow-x: auto`; the `.card` rule still has
  `overflow-wrap: anywhere`; four tests of the small stylesheet reader (selector list normalised, a narrower or longer list
  does not match, rules in a media query and comments are skipped, last declaration wins).
- `frontend/src/indexHtml.test.ts`: one inserted test, the icon link appears exactly once and is exactly `<link rel="icon"
  href="data:," />`; "loads nothing from another host" is untouched and green.
- Vitest: 22 files, 590 tests (588 passed + 2 expected fail) = 581 + 9 added (8 + 1). Only the 2 expected fails.
- Must-fail probes, script `s12-t11-impl-probes.py` (each probe edits one thing, runs both files, restores from a copy and
  compares sha256; every probe failed exactly the named test and every restore matched): `overflow-wrap` declaration
  removed, value `anywhere`, value `break-word`, selector narrowed to `th`, selector narrowed to `td`; `white-space`
  declaration removed, value `normal`, selector narrowed to `th`; `.table-scroll` `overflow-x` set to `visible`; `.card`
  `overflow-wrap` set to `normal`; icon link removed, href `/favicon.ico`, link twice, `rel="shortcut icon"`. Console probes
  (`console.error('probe')` and `console.warn('probe')` inside one test of each of the two files) each failed with
  "console.error or console.warn was called during the test". The style contract and contrast tests stayed green.

Live checks. Isolation (GQ1): no `docker compose`; throwaway `docker run --name s12-t11-impl-pg ... postgres:18.6` on
`127.0.0.1:55433`, removed at the end; `java -jar` with `--spring.docker.compose.enabled=false` and an explicit datasource;
port 8080 was free before and after. Seed: the T10 data through the REST command endpoints (script `s12-t11-impl-seed.sh`);
the order of the task was placed through the page (3 x A Tale of Two Cities 12.50, 1 x Wuthering Heights 8.00; answer `202`,
Total 45.50 EUR). Each browser run used a new in-memory Playwright session (`playwright-cli -s=...`), because the server
sends `Last-Modified: Fri, 01 Feb 1980` for `index.html`, so a browser that has seen `/shop/` once can keep using the old
`index.html` (and so the old CSS name) after a rebuild; my first re-measurement in a reused session showed the old numbers
for that reason (observation only, nothing changed for it).

Order page, script `s12-t11-impl-pw-table.js`, evidence `SP/s12-t11-impl-table-tip.json` (T11 tip) and
`SP/s12-t11-impl-table-base.json` (negative control: `git archive origin/sdd/12-storefront/spec` extracted into
`SP/s12-t11-impl-base`, built with `./mvnw -DskipTests package`, run with the same script and seed):

| Measure | T11 tip, 390 x 844 | PR base `fa6ea90`, 390 x 844 | T11 tip, 1280 x 800 |
|---|---|---|---|
| document `scrollWidth` | 390 | 390 | 1280 |
| `.table-scroll` `clientWidth` / `scrollWidth` | 316 / 561 (scrolls) | 316 / 316 (never scrolls) | 1078 / 1078 (fits) |
| table width | 560.97 | 316 | 1078 |
| computed `overflow-wrap` of a cell | normal | anywhere | normal |
| words with a DOM Range of more than one client rect (of 26) | 0 | 17 ("Quantity" 3 rects, "12.50" 2, "9780141439600" 3, "Wuthering" 2, ...) | 0 |
| height of each of the 5 headings | 40.5 px | 112.5 px | 40.5 px |

So at 390 the words are whole, the wrapper scrolls, the document does not overflow and each heading is as tall as at 1280.
The base shows the defect: broken words, `scrollWidth` equal to `clientWidth`.

Favicon, script `s12-t11-impl-pw-favicon.js` (listeners registered, then the first load of `/shop/` in a brand-new session,
network idle plus 1.5 s), evidence `SP/s12-t11-impl-favicon-tip.json` and `SP/s12-t11-impl-favicon-base.json`:

| Build | `/favicon.ico` requests | Console errors | HTTP answers of 400 or more | Icon links in the DOM |
|---|---|---|---|---|
| T11 tip | none | 0 | none | `<link rel="icon" href="data:,">` |
| PR base | the browser's own request answered 404 (console error `Failed to load resource: the server responded with a status of 404 ()`, location `http://127.0.0.1:8080/favicon.ico`) | 1 | the 404 above | none |

(The browser's built-in favicon fetch is not listed by the page's `request` event, so the base run shows it only as the
console error with its location; the tip run shows neither.)

Regression sweep (GQ8), script `s12-t11-impl-pw-sweep.js`, evidence `SP/s12-t11-impl-sweep-tip.json`; all seven routes
(catalogue, book detail, basket, checkout, order, orders, assistant), each at 390 x 844 and 1280 x 800, a filled basket in
`sessionStorage` for the basket and checkout routes: document `scrollWidth` equals `innerWidth` on all 14 combinations;
catalogue grid 1 track at 390 and 3 tracks at 1280; the first Tab stop has `outline` solid 3px on all 14; transitions exist on
every page except the order page (1 element, 3 on the basket) and all report `0s` with `prefers-reduced-motion: reduce`
emulated; console warnings, console errors, page errors and HTTP answers of 400 or more: none on all 14 (the deliberate
error paths of rows 10, 16 and 17 were not repeated).

Referee after T11 (`./mvnw clean verify` with the Node-free PATH, script `s12-t11-impl-referee.sh`): `Total time:
24.278 s` (final run; two earlier runs of the same tree took 29.510 s and 31.491 s), BUILD SUCCESS, budget 60 s. Counts: Vitest 22 files, 590 tests (588 passed + 2 expected fail);
bookstore `Tests run: 936, Failures: 0, Errors: 0, Skipped: 0`; distributor `190`; Spring `Started` lines 15; one
`Creating container for image: postgres:18.6`; warning inventory 102 lines (the same as at T10, so T11 adds none).

Hygiene: the literal C0-6 grep set (6a to 6j, script `s12-t11-impl-greps.sh`, output `SP/s12-t11-impl-greps.txt`) prints the
same hits as at T10 and none from the T11 files (the first run of it flagged the word "ISBN" in a new CSS comment; the
comment was reworded). The skip/only/todo/fails grep prints only the two accepted `it.fails` lines. This record has no
non-ASCII byte and no em dash.
