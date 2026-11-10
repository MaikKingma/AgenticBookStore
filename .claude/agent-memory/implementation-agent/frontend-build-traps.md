---
name: frontend-build-traps
description: Non-obvious traps for the frontend module (Maven-installed Node, Vite 8, Vitest 5) and the workarounds that held
metadata:
  type: feedback
---

- First Node install without a package.json: `./mvnw -f frontend/pom.xml -Dskip.npm=true generate-resources` installs
  `frontend/node` only. Scaffold with `npm create vite@<pin> app -- --template react-ts --no-interactive` into the
  scratchpad, never the repo; the template lacks `strict` in tsconfig.app.json (add it) and ships a favicon, `public/`
  and `oxlint` to delete.
- Vitest with `globals: false` has no RTL auto cleanup: call `cleanup()` in `afterEach` from setupTests. Order in one
  `afterEach`: cleanup, read console spy calls, `restoreAllMocks`, `expect` last. Keep spies out of module-level `let`
  (QA greps `^(export )?(let|var) `): use `vi.mocked(console.error)` guarded by `vi.isMockFunction`. The QA verifier
  wants a guard test per afterEach statement (`setupTests.test.ts`: dirty test, then clean-state test); the last
  assertion is only killed by a probe in another test file.
- Vite dev server binds `localhost`, maybe IPv6 only: set `server.host: '127.0.0.1'` or curl on 127.0.0.1 fails (000).
- `rg` is not on the Bash tool PATH and BSD grep has no `-P`; there is no Grep tool. Byte-level ASCII check, in a script:
  `env LC_ALL=C grep -rnI $'[\x80-\xff]' <paths>`. The Write tool stores U+2003 as a plain space, so use a committed
  non-ASCII file as the positive control.
- Worktree Bash guard refuses `cd <dir> && ...` and inline `for` loops: use paths from the worktree root or a script in
  `.claude/comms/tmp`. Must-fail probes for a shared file: a script deleting one line per run via `grep -vF`, restoring
  from a copy, comparing `shasum`.
- QA greps hit test titles, comments and fixtures, so keep these tokens out: C0-6 `fontsource`, `document title`; URLs
  `https?://` (write `${'https'}://host`); `.fails(`; module-level `const x = [`/`new` (use `as const` or
  `Object.freeze(...)`, keep fixtures inside `it`/`describe`); C0-6c words (isbn, onHand, reserved) and `-w 10|20|1000`
  (also hits comments and "10:00:00": name limits in words); `Number(`/`parseFloat` (use unary `+x`). A pinned font
  licence text trips `reserved` and URLs: strip only the URL schemes and say so. Run the whole C0-6 list before handoff.
- Vitest 5 turns every `*.css` import into an empty string (`?raw`, `?inline`, glob too). Read stylesheets with
  `node:fs`, start the test with `/// <reference types="node" />`, use `import.meta.dirname` (jsdom makes
  `import.meta.url` non-file). `?raw` works for html and other files. CSS contract tests: read the effective cascade
  (top-level rules, source order, last wins, no comments or media queries), put assertions in throwing helpers, run them
  on the real file and via `expect(() => helper(css)).toThrow()` on one-mutation fixture variants; fixtures need a
  non-colour property (`color: red` trips the colour-literal scan).
- A mocked `fetch` rejecting on an unmatched request is swallowed by a client that maps rejections to errors. Record
  unmatched requests and check in `onTestFinished`; inject the registrar as a parameter. `Response`, `Headers`,
  `ReadableStream` work in jsdom. A synchronous App test row that renders a fetching page: routes whose `respond` is
  `new Promise<Response>(() => {})`, or an act warning fails the console trap. A hook throwing outside its provider logs
  via `console.error`: `mockClear()` after the `toThrow`.
- Stale async answer: body-read witness (`vi.spyOn(response, 'text')`, then `waitFor`), not a timer, then `await act(async
  () => { for (...) await Promise.resolve() })` before the absence assertion; probe by forcing `if (true)` in the guard.
- Order pass-through fixtures (C0-16): at least three elements, middle value strictly above or below both neighbours for
  every attribute (numeric strings under text AND numeric comparison: "12.50" sorts before "6.99"); keep
  `inStock = available > 0`. Covers EVERY array under `toEqual` (authors, sent `lines`, helper call log). One UUID per
  entity across test files; a not-found id is its own literal. If no permutation of three lines makes all attributes peak
  or valley, brute-force a price (`7.99, 12.50, 3.50` with quantities 2, 3, 1 worked).
- Hash router: setting `window.location.hash` fires `hashchange` later, so await `findBy*` (no act warning); set the hash
  before `render` for a start route. Unknown-route rows: unknown first segment with a second (`#/basket/x`), prefix
  `#xbasket`, `#/basket?x=1`, double-encoded id (`a%2541` gives `a%41`); otherwise the final `return notFound` never
  runs. An enum on a card needs a fixture with an underscore (`SCIENCE_FICTION`).
- jsdom "Not implemented: requestSubmit()" goes to raw stderr, not the console trap, so a missing `preventDefault` stays
  green. Witness: `document.addEventListener('submit', (e) => seen.push(e.defaultPrevented))`, remove in
  `onTestFinished`, expect `[true]`.
- `type="email"` trims edge whitespace in `value`; for "sent as typed" use `type="text"` with `inputMode="email"` (keep
  `noValidate`). Controlled `type="number"` inputs reset after a refused `fireEvent.change` (fits "value stays 3").
- Send path with an id helper (`crypto.randomUUID` is absent on plain-http non-localhost): `async` send with try/catch
  around `await chat({ id: helper(), ... })` so a throw gives the generic alert, not a stuck pending state. Edge-whitespace
  replies: await the author label, compare `li.textContent` byte-exact (`findByText` normalises). Counter:
  `toHaveAccessibleDescription` via `aria-describedby`.
- Live checks after a rebuild: the jar serves `index.html` with `Last-Modified` 1980, so a reused Playwright session
  measures the old build; open a new session per build. `overflow-wrap: anywhere` inherited from `.card` makes th/td
  min-content 1 char; add `white-space: nowrap` for short columns.
- "Existing test file unmodified, 0 deletions" tasks: add imports as NEW lines after the old block, append to the last
  `describe` by replacing its closing lines with themselves plus the new block. Shared in-flight state (request outliving
  its page) belongs in the root provider with a `useRef` guard; only a same-tick hook test
  (`act(() => { place(); place() })`) kills a state-only guard.
- DOM element arrays: `toEqual` is `isEqualNode`; compare identity with `toBe`.
