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
- Vite's dev server binds `localhost`, possibly IPv6 only: `curl 127.0.0.1:5173` fails until `server.host: '127.0.0.1'`.
- `rg` is not on the Bash PATH and BSD grep has no `-P`. Byte-level ASCII check (in a script): `env LC_ALL=C grep -rnI
  $'[\x80-\xff]' <paths>`. The Write tool stores U+2003 as a plain space: use a committed non-ASCII file as positive control.
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
  say so. Run the whole list before handing off.
- Must-fail probes for a shared setup file or `src/api`: one script that deletes/replaces a line per run, restores from a copy
  and compares `shasum`/sha256 per probe (`python3 -I` runner, `vitest run src/api`).
- A mocked `fetch` that rejects on an unmatched request is swallowed by a client that maps rejections to its own error: record
  unmatched requests and check them in `onTestFinished` (a throw there fails the test); take the registrar as a parameter.
  Plain `Response`, `Headers`, `ReadableStream` work under jsdom.
- Order pass-through fixtures: at least three elements, and per attribute the middle value strictly above or below both
  neighbours; numeric strings must hold under string AND numeric comparison ("12.50" sorts before "6.99" as text). When no
  permutation works, change one price (brute force found `7.99, 12.50, 3.50` with quantities 2, 3, 1; a data-only double may
  differ from the basket it answers). The rule covers EVERY array under a `toEqual` (nested `authors`, sent `lines`, a helper's
  call log). One UUID per entity across all test files; a 404 or 409 detail must not reuse a live id.
- Hash router tests: setting `window.location.hash` fires `hashchange` later, so await a `findBy*` right after; set the hash
  before `render` for a start route. A stale async answer needs a body-read witness (`vi.spyOn(response, 'text')`, `waitFor`)
  plus `await act(async () => { for (...) await Promise.resolve() })`; probe it by forcing `if (true)` in the guard.
  Unknown-route rows: unknown first segment WITH a second (`#/basket/x`), prefix `#xbasket`, `#/basket?x=1`, double-encoded id
  (`a%2541` gives `a%41`). A genre or enum shown on a card needs one fixture with an underscore (`SCIENCE_FICTION`).
- jsdom "Not implemented: requestSubmit()" goes to raw stderr, not the console trap, so a handler missing `preventDefault`
  stays green: witness with `document.addEventListener('submit', (e) => seen.push(e.defaultPrevented))`, remove it in
  `onTestFinished`, expect `[true]`.
- A synchronous App test that now renders a fetching page: install `mockFetch` routes with `respond: new Promise<Response>(() =>
  {})`, or the late answer logs an act warning. A hook throwing outside its provider logs `console.error`: `mockClear()` after
  the `toThrow`.
- "Sent as typed" fields: `type="email"` trims edge whitespace in `value`; use `type="text"` with `inputMode="email"` (keep
  `noValidate` if the card names it). Controlled `type="number"` inputs reset after a refused `fireEvent.change`.
- Send path calling an id helper: make the send `async` with try/catch around `await chat({ id: helper(), ... })`, so a throwing
  helper (`crypto.randomUUID` absent on plain-http non-localhost) becomes the generic alert. A reply with edge whitespace
  cannot be awaited by `findByText`: await the author label, compare `li.textContent` byte-exact. A counter is checked with
  `toHaveAccessibleDescription`.
- Live checks after a rebuild: the jar serves `index.html` with `Last-Modified` 1980, so a REUSED Playwright session keeps the
  old build (old CSS hash); open a new session per build. `overflow-wrap: anywhere` inherited from `.card` makes `th`/`td`
  min-content 1 char, and `overflow-wrap: normal` alone still wraps headings at spaces: add `white-space: nowrap` for short
  columns.
- "Existing test file unmodified, 0 deletions": add imports as NEW lines (never widen an `import { a }` line) and append to the
  last `describe` by replacing its closing lines with themselves plus the new block. Shared in-flight state belongs in the
  root provider with a `useRef` guard; only a same-tick hook test (`act(() => { place(); place() })`) kills a state-only guard.
- Worktree Bash guard refuses `cd <dir> && ...`, inline `for` and escaped spaces in paths: use a script in
  `.claude/comms/tmp` or a relative path from the worktree root. No `Grep` tool: grep via Bash.
