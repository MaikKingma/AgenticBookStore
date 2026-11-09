# Spec 12: Storefront (React + Vite, served by the bookstore)

> Modules: `frontend` (new), `bookstore` (backend tasks), root `pom.xml` · Depends on: 02, 03, 04, 08 · Capability: `storefront`

## Why
Participants should see the application they are harnessing, not only JSON. A small React storefront makes the
catalogue, stock, orders and the assistant visible in a browser, gives Level 0 a place where a broken discount is
obvious, and shows agents a second technology inside the same repository and the same referee. It replaces the
static chat page that spec 08 originally planned.

## What changes
- A new Maven module `frontend/`: a Vite `react-ts` project with Vitest and React Testing Library, built by
  frontend-maven-plugin with a locally installed Node.js. `./mvnw clean verify` installs Node.js and npm into
  `frontend/node/`, runs `npm ci`, `npm test` and `npm run build`.
- The Vite build output is packaged into the bookstore as static files under `/shop/`. One process, same origin,
  no CORS.
- Backend: `StorefrontConfig` in the root package of the bookstore redirects `/` to `/shop/` and serves the
  storefront's `index.html` for `/shop/`.
- Pages: catalogue (browse and search), book detail with stock level, basket, checkout, order status, order
  history by e-mail, assistant chat.

## Out of scope
- Authentication, accounts, payments, addresses, discounts (Level 0), cancelling orders (Level 3).
- Any business rule in the browser. The storefront shows what the REST adapters return and sends what the command
  adapters accept. Where the UI mirrors a server limit for usability (quantity 1 to 10, at most 20 lines, 1000
  characters per chat message, no basket button for books that cannot be ordered), the server stays the authority
  and the UI shows the server's ProblemDetail when they disagree.
- Server-side rendering, a Node.js runtime in production, a separate frontend server.
- Changes to the REST paths of specs 02 to 08. They stay exactly as they are.

## Design notes

### Routes and serving
- Base path `/shop/` (Vite `base: '/shop/'`). The storefront uses hash routing (`/shop/#/books/<id>`), so deep links
  work without a server-side fallback and never collide with `/books`, `/inventory`, `/orders` and `/assistant`.
- `GET /` answers `302` to `/shop/`. `GET /shop` and `GET /shop/` serve `index.html` of the build.
- Java needed for this: one class `StorefrontConfig implements WebMvcConfigurer` in the root package
  `nl.openvalue.bookstore` (the architecture standard reserves the root package for `BookstoreApplication` and the
  cross-cutting configuration a spec explicitly puts there, as spec 04 does with the `Clock` bean; the standard names
  `StorefrontConfig` since the amendment of 2026-11-06). It registers
  view controllers: `/` redirects to `/shop/`, `/shop` and `/shop/` forward to `/shop/index.html`. The static files
  themselves are served by Spring Boot's default static resource handling from `classpath:/static/shop/`. No
  controller, no flow, no adapter package is touched. ArchUnit stays as it is.
- Without the frontend build output (for example `./mvnw -pl bookstore spring-boot:run` before any full build),
  `/shop/` answers 404 and the REST API works unchanged. After one `./mvnw verify`, the output is present.

### Build wiring
- Root `pom.xml` lists the modules in this order: `frontend`, `bookstore`, `distributor`.
- `frontend/pom.xml` (packaging `pom`, no Java): frontend-maven-plugin with the pinned Node.js (npm is the version
  bundled with it), executions `install-node-and-npm`, `npm ci`, `npm test` (`vitest run`), `npm run build`
  (output `frontend/dist/`). The `npm test` execution is skipped when Maven runs with `-DskipTests` (bind its
  `skip` parameter to `${skipTests}`, *verify at build time*), so the Windows CI job of spec 01 builds the
  storefront without running Vitest. The plugin's `installDirectory` is `frontend/` so Node.js lands in
  `frontend/node/`, which is git-ignored.
- `bookstore/pom.xml` adds a resource: directory `${project.basedir}/../frontend/dist`, target path `static/shop`,
  included only when it exists. Maven's reactor order guarantees it exists in a full build.
- Versions: `product/tech-stack.md` (React, Vite, Vitest, React Testing Library, Node.js, frontend-maven-plugin).
  `package.json` pins exact versions (no `^`), `package-lock.json` is committed.
- CI (spec 01 workflow): the Linux `verify` job runs the frontend tests and build inside `./mvnw -B verify`; the
  Windows `windows-build` job builds the storefront with `-DskipTests`. One change to the workflow: cache
  `frontend/node` and `~/.npm` with `actions/cache` to keep both jobs fast (backend task 2 below).
- The 60 second budget of the architecture standard now includes the frontend. Measure after this spec; over budget
  is an open question, never a reason to drop tests.

### Development
- `frontend/node/npm run dev` (run in `frontend/`, using the npm that the Maven build installed, so no global
  Node.js is needed) starts Vite on port 5173 with `server.proxy` for `/books`, `/inventory`, `/orders` and
  `/assistant` to `http://127.0.0.1:8080` (the bookstore binds to 127.0.0.1, and Node.js may resolve `localhost`
  to `::1`), against a running bookstore.
- `npm test` runs Vitest with the `jsdom` environment and React Testing Library. API calls are mocked at `fetch`
  level in tests; no test calls the bookstore.

### Structure (`frontend/src/`)
```
main.tsx, App.tsx (HashRouter, routes)
api/            client.ts (fetch wrapper, same-origin paths, ProblemDetail parsing), books.ts, inventory.ts, orders.ts, assistant.ts
pages/          CataloguePage, BookDetailPage, BasketPage, CheckoutPage, OrderPage, OrdersPage, AssistantPage
components/     SearchForm, BookCard, StockBadge, BasketSummary, ProblemDetailAlert, ChatMessageList
state/          basket.tsx (React context, sessionStorage), conversation.ts (assistant conversation id)
```
The storefront keeps UI state only: the basket before an order is placed, the last search, the assistant
conversation id. Quantities, totals and availability shown before checkout are presentation; the bookstore decides
at `POST /orders/commands/place`.

### Endpoints used (unchanged from specs 02 to 08)
| Page | Calls |
|---|---|
| Catalogue | `GET /books?q=&title=&author=&genre=` (`BookView[]` with `available`, `inStock`) |
| Book detail | `GET /books/{id}` (`BookDetailView`), `GET /inventory/{bookId}` (`StockView`) |
| Checkout | `POST /orders/commands/place` (`PlaceOrderPayload`), then `GET /orders/{id}` |
| Order status | `GET /orders/{id}` (`OrderView`) |
| Order history | `GET /orders?customerEmail=` (`OrderSummaryView[]`) |
| Assistant | `POST /assistant/chat` (`ChatPayload`, `AssistantReplyView`) |

No new endpoint is needed at the time of writing. If implementation finds one missing, it is added to this spec as
a backend task in the right inbound adapter (`query` for reads, `command` for writes), following the architecture
standard and the `hexagonal-slice` skill, before any frontend code depends on it; existing paths are never changed.

### Errors
Every non-2xx response is parsed as RFC 9457 `ProblemDetail` and shown by `ProblemDetailAlert` with `title`,
`detail` and the `details` list (for example the shortages of `insufficient-stock` or the `{field, message}` pairs of
`validation-failed`). The assistant page shows `503 assistant-offline` as "The assistant is offline", `503
assistant-unavailable` as "The assistant is unavailable right now, try again later" (decision D10) and `429` as
"Too many requests, try again in a minute".

### Visual design (decision D7, Maik 2026-11-08)
The shop is called **OpenBook**. It looks modern and uses the OpenValue 2026 brand colours (theme `OPENVALUE2026` of
the OpenValue slides template).

- Name: the header shows the wordmark "OpenBook" (text, "Open" in navy, "Book" in blue, Montserrat bold) and the
  document title is "OpenBook". No OpenValue logo in the shop. The footer may say "OpenBook, the J-Fall 2026 workshop
  shop by OpenValue".
- Colour tokens, as CSS custom properties in one place (`src/styles/tokens.css`):

  | Token | Value | Use |
  |---|---|---|
  | `--ob-navy` | `#1D252D` | body text, header background, headings |
  | `--ob-blue` | `#3D7DC9` | accents: wordmark, focus ring, active nav, highlights, large headings only (4.2:1 on white, so never body-size text) |
  | `--ob-teal` | `#014046` | primary buttons (white text), links, in-stock badge |
  | `--ob-sage` | `#809FA2` | decorative only: icons, dividers (never text) |
  | `--ob-mist` | `#CBD9DA` | borders, out-of-stock badge background (navy text) |
  | `--ob-snow` | `#F1F5F6` | page background |
  | `--ob-white` | `#FFFFFF` | cards, inputs |

  Every text and background pair meets WCAG 2.2 AA (4.5:1 for normal text). Errors use the `ProblemDetailAlert` with
  a red that also meets AA on its background.
- Typography: Montserrat (the brand font), self-hosted as woff2 files (latin subset, weights 400, 600 and 700) with
  its SIL Open Font License file committed next to them, loaded with `@font-face` and `font-display: swap`. Fallback
  stack `Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`. No font CDN at runtime: the shop must
  work on conference wifi. If the font files cannot be obtained at build time, use the fallback stack and say so in
  the PR.
- Look: mobile-first and responsive (one column on a phone, a grid of book cards from tablet width), generous
  whitespace, cards with rounded corners and a soft shadow, a sticky header with the wordmark, navigation and a
  basket count badge, a prominent search bar on the catalogue page, clear in-stock and out-of-stock badges, visible
  keyboard focus, and `prefers-reduced-motion` respected for any transition.
- Genre labels (decision D11, Maik 2026-11-09): the shop never shows a raw enum name. The genre select and the book
  card show a readable label in sentence case, one per `Genre` value: Fiction, Science fiction, Fantasy, Mystery,
  Romance, History, Science, Technology, Children, Poetry. The select still sends the enum name (`SCIENCE_FICTION`)
  as the `genre` parameter. The unknown-genre alert keeps the list the bookstore sends.
- Footer (decision D11): the footer sits at the bottom of the browser window when the page content is shorter than
  the window, and below the content when it is longer. It never covers content.
- Plain CSS only (one global stylesheet plus the tokens, or CSS modules, both built into Vite). No UI library, no CSS
  framework and no new npm dependency: `product/tech-stack.md` pins every frontend package.

## Requirements

### Requirement: Storefront is served by the bookstore
The bookstore SHALL serve the built storefront under `/shop/` from the same process and origin as the REST API, and `/` SHALL redirect to `/shop/`.

#### Scenario: Root redirects
- **GIVEN** the bookstore built with `./mvnw verify`
- **WHEN** a client calls `GET /`
- **THEN** the response is 302 with `Location: /shop/`

#### Scenario: Storefront page
- **WHEN** a client calls `GET /shop/`
- **THEN** the response is 200 with `text/html` and the body contains the storefront's root element

#### Scenario: REST paths are untouched
- **WHEN** a client calls `GET /books` and `GET /actuator/health`
- **THEN** both answer exactly as before this spec (200 JSON, no redirect)

#### Scenario: Deep link
- **WHEN** a browser opens `/shop/#/books/<id>` of an existing book
- **THEN** the book detail page renders (hash routing, no server round trip for the route)

### Requirement: Browse and search the catalogue
The storefront SHALL list the catalogue and SHALL search it with the same criteria the query adapter offers.

#### Scenario: Catalogue listing
- **GIVEN** `GET /books` returns three books
- **WHEN** the catalogue page renders
- **THEN** three book cards show title, authors, the genre label ("Science fiction", not `SCIENCE_FICTION`), price with "EUR" and an in-stock badge from `inStock`

#### Scenario: Search
- **GIVEN** the user enters "austen" in the search field and selects genre FICTION
- **WHEN** the user submits the form
- **THEN** the storefront calls `GET /books?q=austen&genre=FICTION` and renders the result

#### Scenario: Unknown genre error
- **GIVEN** `GET /books?genre=cookbooks` answers 422 with `code` `unknown-genre` and `details` listing the valid genres
- **WHEN** the result arrives
- **THEN** the alert shows the ProblemDetail title and the list of valid genres

### Requirement: Book detail with stock level
The storefront SHALL show one book with its stock level.

#### Scenario: Detail page
- **GIVEN** `GET /books/{id}` returns a book and `GET /inventory/{bookId}` returns `available 7, lowStock false`
- **WHEN** the detail page renders
- **THEN** it shows ISBN, authors as a list, publication year, price, "7 available" and an "Add to basket" button

#### Scenario: Out of stock
- **GIVEN** `GET /inventory/{bookId}` returns `available 0`
- **WHEN** the detail page renders
- **THEN** it shows "Out of stock" and the "Add to basket" button is disabled

#### Scenario: Discontinued book
- **GIVEN** the book has status DISCONTINUED
- **WHEN** the detail page renders
- **THEN** it shows "Discontinued" and the "Add to basket" button is disabled

### Requirement: Basket
The storefront SHALL keep a basket in the browser until the order is placed, with line quantities between 1 and 10 and at most 20 lines (the limits of spec 04).

#### Scenario: Add and change quantity
- **GIVEN** an empty basket
- **WHEN** the user adds book A twice and sets the quantity of A to 3
- **THEN** the basket has one line for A with quantity 3 and a line total of three times the price

#### Scenario: Quantity bounds
- **WHEN** the user tries to set a quantity of 0 or 11
- **THEN** the input refuses it and the quantity stays within 1 to 10 (the server rule of spec 04); a 21st distinct book is refused with a message

#### Scenario: Basket survives a reload
- **GIVEN** a basket with two lines
- **WHEN** the page is reloaded
- **THEN** the basket still has the two lines (`sessionStorage`)

### Requirement: Place an order and see its status
The storefront SHALL place the basket as one order with the customer's e-mail address and SHALL show the order.

#### Scenario: Successful checkout
- **GIVEN** a basket with 2 x A and 1 x B and the e-mail "reader@example.com"
- **WHEN** the user confirms the checkout
- **THEN** the storefront posts `PlaceOrderPayload { customerEmail: "reader@example.com", lines: [{bookId: A, quantity: 2}, {bookId: B, quantity: 1}] }`, and after the 202 with `CommandResult { id }` it empties the basket and navigates to the order page, which shows status PLACED, the lines with unit price and line total, and the total

#### Scenario: Insufficient stock
- **GIVEN** the server answers 409 with `code` `insufficient-stock` and `details` `[{bookId, requested, available}]`
- **WHEN** the user confirms the checkout
- **THEN** the basket is kept and the alert lists each shortage with requested and available quantities

#### Scenario: Validation error
- **GIVEN** the server answers 400 with `code` `validation-failed` and `details` `[{field, message}]` (for example `field: "customerEmail"`)
- **WHEN** the user confirms the checkout
- **THEN** the alert shows the message next to the field name

#### Scenario: Order history
- **GIVEN** the user enters "reader@example.com" on the orders page
- **WHEN** the storefront calls `GET /orders?customerEmail=reader@example.com`
- **THEN** it lists the orders newest first with status, date, item count and total, each linking to its order page

### Requirement: Assistant chat
The storefront SHALL provide the chat page for `POST /assistant/chat` with one conversation id per browser session.

#### Scenario: Chat round trip
- **GIVEN** a conversation id in `sessionStorage`
- **WHEN** the user sends "Do you have anything by Austen?"
- **THEN** the storefront posts `ChatPayload { conversationId, message }`, shows the user message and the `reply` of `AssistantReplyView`, and reuses the same conversation id for the next message

#### Scenario: Assistant offline
- **GIVEN** the server answers 503 with `code` `assistant-offline`
- **WHEN** the user sends a message
- **THEN** the page shows "The assistant is offline" and the input stays usable

#### Scenario: Message length
- **WHEN** the user types more than 1000 characters
- **THEN** the send button is disabled and a counter shows the limit (the server limit of spec 08)

### Requirement: OpenBook branding
The storefront SHALL present itself as OpenBook in the OpenValue 2026 colours described under "Visual design".

#### Scenario: Name and title
- **GIVEN** the storefront is open on any page
- **THEN** the header shows the "OpenBook" wordmark and the document title is "OpenBook"

#### Scenario: Brand tokens in one place
- **GIVEN** the frontend source
- **THEN** `src/styles/tokens.css` defines the seven `--ob-*` colour tokens with the values of the table, and no other
  stylesheet hard-codes a colour value outside it (a grep for `#` colour literals outside `tokens.css` finds none)

#### Scenario: Readable contrast
- **GIVEN** the text and background pairs the stylesheets use
- **THEN** each pair has a contrast ratio of at least 4.5:1 (checked by a small Vitest test over the pairs, computed
  with the WCAG relative luminance formula)

### Requirement: Frontend is part of the referee
`./mvnw clean verify` SHALL install the pinned Node.js locally, run the frontend tests and build the storefront, and SHALL fail when a frontend test fails.

#### Scenario: One build
- **GIVEN** a machine without a global Node.js
- **WHEN** `./mvnw clean verify` runs
- **THEN** `frontend/node/` contains the pinned Node.js, `npm test` and `npm run build` ran, and `bookstore/target/classes/static/shop/index.html` exists

#### Scenario: Red frontend test fails the build
- **GIVEN** a deliberately failing Vitest test (in a throwaway branch, not committed)
- **WHEN** `./mvnw verify` runs
- **THEN** the build fails in the `frontend` module before the bookstore is packaged

#### Scenario: No business rule in the browser
- **WHEN** a reviewer greps `frontend/src` for price arithmetic beyond line and basket totals, stock arithmetic, or ISBN validation
- **THEN** nothing is found; totals shown before checkout are labelled as estimates, the order page shows the server's `total`, and the mirrored limits (1 to 10, 20 lines, 1000 characters) are constants with a comment pointing at the spec that owns them

## Tests (required)
Frontend (Vitest + React Testing Library, `fetch` mocked):
- `CataloguePage.test.tsx`: listing, search query string, unknown genre alert.
- `BookDetailPage.test.tsx`: stock level, out of stock, discontinued.
- `basket.test.tsx`: add, merge, quantity bounds, sessionStorage persistence, clear after order.
- `CheckoutPage.test.tsx`: payload shape, navigation after 202, insufficient-stock details, validation details.
- `OrderPage.test.tsx` and `OrdersPage.test.tsx`: rendering of `OrderView` and `OrderSummaryView[]`.
- `AssistantPage.test.tsx`: round trip, conversation id reuse, 503 and 429 messages, length limit.
- `client.test.ts`: ProblemDetail parsing for 400, 404, 409, 422, 503.

Bookstore:
- `StorefrontConfigTest` (`@WebMvcTest` importing `StorefrontConfig`, or `@SpringBootTest` with MockMvc): `/`
  answers 302 to `/shop/`; `/shop/` serves `index.html` when a test resource `static/shop/index.html` is present;
  `/books` and `/actuator/health` are not redirected.
- `ArchitectureTest` unchanged and green: `StorefrontConfig` lives in the root package and touches no layer.

## Tasks
Backend tasks (bookstore module, through the `hexagonal-slice` skill where it applies; `StorefrontConfig` is
configuration in the root package, so steps 0 and 8 of the skill apply and steps 1 to 7 do not):
1. `StorefrontConfig` with its test; resource mapping of `../frontend/dist` to `static/shop` in `bookstore/pom.xml`.
2. Root `pom.xml`: add the `frontend` module first in the module list; CI workflow: cache `frontend/node` and
   `~/.npm`.

Frontend tasks (follow this spec and `product/tech-stack.md`):
3. `frontend/pom.xml` with frontend-maven-plugin (pinned Node.js and npm, `npm ci`, `npm test`, `npm run build`);
   `.gitignore` entries `frontend/node/`, `node_modules/`, `dist/`.
4. Scaffold with the Vite `react-ts` template (`create-vite`, run once with the Maven-installed npm; record the
   create-vite version in the PR), then set every dependency to the exact versions of `product/tech-stack.md`
   (React, Vite, Vitest, React Testing Library packages); add jsdom at its current stable version and pin it exactly
   in `package.json`; `vite.config.ts` with `base: '/shop/'`, the dev proxy and the test environment.
5. API client with ProblemDetail parsing and its tests.
6. Catalogue and book detail pages with tests.
7. Basket state, basket page, checkout page, order page, orders page with tests.
8. Assistant page with tests.
9. `./mvnw clean verify` from a clean clone without a global Node.js; record the time in
   `docs/spec-pack/orchestrator/progress.md`; manual walk through every page against the running bookstore.

## Definition of done
- OpenBook branding: screenshots of the catalogue, book detail, basket, checkout, order, orders and assistant pages
  (desktop and phone width) are committed in the spec folder's `visuals/` and linked from the spec PR.
- `./mvnw clean verify` green from the repo root, including the frontend tests and build; time recorded.
- `GET /` redirects to `/shop/`; every page of this spec works in a browser against the running bookstore.
- The REST paths of specs 02 to 08 are byte-for-byte unchanged (diff of the `*Commands` and `*Queries` classes is
  empty, unless this spec gained a backend task for a missing endpoint; then only that addition appears).
- No global Node.js was needed (`which node` on the build machine may be empty).
- `grep -ri "cors" bookstore/src frontend/src` returns nothing.
