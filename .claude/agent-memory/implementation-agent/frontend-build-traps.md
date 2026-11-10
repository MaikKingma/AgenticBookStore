---
name: frontend-build-traps
description: Non-obvious traps for the frontend module (Maven-installed Node, Vite 8, Vitest 5) and the workarounds that held
metadata:
  type: feedback
---

- First Node install without a package.json: `./mvnw -f frontend/pom.xml -Dskip.npm=true generate-resources` installs
  `frontend/node` only. Scaffold with `npm create vite@<pin> app -- --template react-ts --no-interactive` into the scratchpad,
  never the repo; the template lacks `strict` in tsconfig.app.json (add it) and ships a favicon, `public/`, `oxlint` to delete.
- Vitest with `globals: false` has no RTL auto cleanup: call `cleanup()` in `afterEach` from setupTests. Order in one
  `afterEach`: cleanup, read console spy calls, `restoreAllMocks`, `expect` last. Keep spies out of module-level `let` (QA greps
  `^(export )?(let|var) `): use `vi.mocked(console.error)` guarded by `vi.isMockFunction`. QA asks a guard test per afterEach
  statement (`setupTests.test.ts`: dirty test, then clean-state test); the final assertion needs a probe in another file.
- Vite's dev server binds `localhost`, possibly IPv6 only: `curl 127.0.0.1:5173` fails (000) until `server.host: '127.0.0.1'`.
- `rg` is not on the Bash PATH, BSD grep has no `-P`, and there is no Grep tool (grep via Bash). Byte-level ASCII check (in a
  script): `env LC_ALL=C grep -rnI $'[\x80-\xff]' <paths>`. The Write tool stores U+2003 as a plain space: use a committed
  non-ASCII file as positive control.
- Vitest 5 turns every `*.css` import into an empty string (`?raw`, `?inline`, glob too; `?raw` still works for html and other
  files). Read stylesheets with `node:fs`; start the test with `/// <reference types="node" />` (tsconfig has only
  `vite/client` types) and use `import.meta.dirname` (jsdom makes `import.meta.url` non-file).
- CSS source-level contract tests (`styleContract.test.ts`): read the effective cascade (top-level rules only, source order,
  last declaration wins; comments and media queries excluded), put the assertions in helpers that throw, run the helper on the
  real file and, via `expect(() => helper(css)).toThrow()`, on one-mutation variants of a passing fixture. A fixture needs a
  non-colour property (`color: red` trips the colour-literal scan). DOM element arrays: `toEqual` is `isEqualNode`; compare
  identity with `toBe`.
- QA greps over test sources are literal and also hit titles, comments and prose: `https?://` (write `${'https'}://host`),
  `.fails(`, module-level `const x = [`/`new`/`const X: T = [` (keep fixtures inside `it`/`describe`, use `as const` or
  `Object.freeze`), `fontsource`, `document title`, limits `-w 10|20|1000` (also date examples like "10:00:00" and production
  comments: name limits in words), `Number(` and `parseFloat` (use unary `+x`), `reserved`, `isbn`, `onHand` (fixtures for a
  page that reads only some fields may omit them). A pinned font licence text can trip them: strip only the URL schemes and
  say so. Run the whole C0-6 list before handing off.
- Must-fail probes for a shared setup file or `src/api`: one script that deletes/replaces a line per run, restores from a copy
  and compares `shasum` per probe.
- A mocked `fetch` rejecting on an unmatched request is swallowed by a client that maps rejections to its own error: record
  unmatched requests and check them in `onTestFinished`. Plain `Response`, `Headers`, `ReadableStream` work under jsdom.
- Order pass-through fixtures (rule C0-16, see QA standards): at least three elements, middle value strictly above or below both
  neighbours per attribute, numeric strings ordered under string AND numeric comparison; covers EVERY array under a `toEqual`.
- Hash router tests: setting `window.location.hash` fires `hashchange` later, so await a `findBy*` right after; set the hash
  before `render` for a start route. A stale async answer needs a body-read witness (`vi.spyOn(response, 'text')`, `waitFor`)
  plus `await act(async () => { for (...) await Promise.resolve() })` before the absence assertion. Unknown-route rows need an
  unknown first segment WITH a second (`#/basket/x`), `#xbasket`, `#/basket?x=1`, `a%2541`, else `return notFound` never runs.
- jsdom "Not implemented: requestSubmit()" goes to raw stderr, so a handler missing `preventDefault` stays green: witness with
  `document.addEventListener('submit', (e) => seen.push(e.defaultPrevented))`, remove it in `onTestFinished`, expect `[true]`.
- A synchronous App test that renders a fetching page: `mockFetch` routes with a never-resolving `respond`, or the late answer
  logs an act warning. A hook throwing outside its provider logs `console.error`: `mockClear()` after the `toThrow`.
- "Sent as typed" fields: `type="email"` trims edge whitespace in `value`; use `type="text"` with `inputMode="email"` (keep
  `noValidate` if the card names it). Controlled `type="number"` inputs reset after a refused `fireEvent.change`.
- `crypto.randomUUID` is absent on plain-http non-localhost: wrap `await chat({ id: helper(), ... })` in try/catch so a throw
  becomes the generic alert. `findByText` normalises whitespace: await the author label, compare `li.textContent` byte-exact.
- Live checks after a rebuild: the jar serves `index.html` with `Last-Modified` 1980, so a REUSED Playwright session keeps the
  old build; open a new session per build. `overflow-wrap: anywhere` from `.card` makes `th`/`td` min-content 1 char: add
  `white-space: nowrap` for short columns.
- "Existing test file unmodified": add imports as NEW lines and append to the last `describe` by replacing its closing lines
  with themselves plus the new block. Only a same-tick hook test (`act(() => { place(); place() })`) kills a state-only guard.
