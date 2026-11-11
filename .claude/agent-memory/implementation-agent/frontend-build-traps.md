---
name: frontend-build-traps
description: Non-obvious traps for the frontend module (Maven-installed Node, Vite 8, Vitest 5, jsdom) and the test techniques that held, incl. QA grep traps in test sources
metadata:
  type: feedback
---
- **Vitest cleanup**: `globals: false` has no RTL auto cleanup: call `cleanup()` in `afterEach` from setupTests. Order in one
  `afterEach`: cleanup, read console spy calls, `restoreAllMocks`, `expect` last (a failing assertion cannot skip the resets).
  Keep spies out of module-level `let` (QA greps `^(export )?(let|var) `): use `vi.mocked(console.error)` guarded by
  `vi.isMockFunction`. QA asks a guard test per afterEach statement (`setupTests.test.ts`: dirty test, then clean-state test).
- **Shell**: no `rg` or Grep tool (grep via Bash, no `-P`); the worktree guard refuses `cd <dir> && ...` and inline `for` loops
  (script in `.claude/comms/tmp`). ASCII check: `env LC_ALL=C grep -rnI $'[\x80-\xff]' <paths>`; the Write tool stores U+2003 as a
  plain space, so use a committed non-ASCII file as positive control.
- **CSS in Vitest 5**: every `*.css` import becomes an empty string (`?raw`, `?inline`, glob too; `?raw` still works for html and
  other files). Read stylesheets with `node:fs`; start the test with `/// <reference types="node" />` (tsconfig has only
  `vite/client` types) and use `import.meta.dirname` (jsdom makes `import.meta.url` non-file).
- **CSS contract tests** (`styleContract.test.ts`): read the effective cascade (top-level rules, source order, last declaration
  wins; comments and media queries excluded), assert in helpers that throw, run them on the real file and, via
  `expect(() => helper(css)).toThrow()`, on one-mutation variants of a passing fixture (needs a non-colour property: `color: red`
  trips the colour-literal scan). DOM element arrays: `toEqual` is `isEqualNode`; use `toBe`.
- **QA greps over test sources are literal** and also hit titles, comments and prose: `https?://` (write `${'https'}://host`),
  `.fails(`, module-level `const x = [`/`new`/`const X: T = [` (keep fixtures inside `it`/`describe`, use `as const` or
  `Object.freeze`), `fontsource`, `document title`, limits `-w 10|20|1000` (also date examples like "10:00:00" and production
  comments: name limits in words), `Number(` and `parseFloat` (use unary `+x`), `reserved`, `isbn`, `onHand` (fixtures for a
  page that reads only some fields may omit them). A pinned font licence text can trip them: strip only the URL schemes and
  say so. Run the whole C0-6 list before handing off.
- **Mocked fetch** that rejects on an unmatched request is swallowed by a client that maps rejections to its own error: record
  unmatched requests and check them in `onTestFinished` (a throw there fails the test). Plain `Response`, `Headers`,
  `ReadableStream` work under jsdom. A synchronous App test that renders a fetching page: route `mockFetch` with a
  never-resolving `respond`, or the late answer logs an act warning. A hook throwing outside its provider logs `console.error`:
  `mockClear()` after the `toThrow`.
- **Order pass-through fixtures (C0-16, see QA standards)**: at least three elements, per attribute the middle value strictly
  above or below both neighbours; numeric strings must hold under string AND numeric comparison ("12.50" sorts before "6.99" as
  text); keep `inStock = available > 0`. When no permutation works, change one price (brute force). The rule covers EVERY array
  under a `toEqual` (nested `authors`, sent `lines`, a helper's call log). One UUID per entity across test files.
- **Hash router tests**: setting `window.location.hash` fires `hashchange` later, so await a `findBy*` right after (no act
  warning); set the hash before `render` for a start route. A stale async answer needs a body-read witness
  (`vi.spyOn(response, 'text')`, `waitFor`, not a timer) plus `await act(async () => { for (...) await Promise.resolve() })`
  before the absence assertion. Unknown-route rows: unknown first segment WITH a second (`#/basket/x`), prefix `#xbasket`,
  `#/basket?x=1`, double-encoded id (`a%2541` gives `a%41`), else the final `return notFound` never runs. A genre or enum shown
  on a card needs one fixture with an underscore (`SCIENCE_FICTION`).
- **jsdom "Not implemented: requestSubmit()"** goes to raw stderr, not the console trap, so a handler missing `preventDefault`
  stays green: witness with `document.addEventListener('submit', (e) => seen.push(e.defaultPrevented))`, remove it in
  `onTestFinished`, expect `[true]`.
- **"Sent as typed" fields**: `type="email"` trims edge whitespace in `value`; use `type="text"` with `inputMode="email"` (keep
  `noValidate` if the card names it). Controlled `type="number"` inputs reset after a refused `fireEvent.change`. An id helper
  in the send path (`crypto.randomUUID` is absent on plain-http non-localhost) needs try/catch inside an `async` send
  (`await chat({ id: helper(), ... })`), so a throw becomes the generic alert, not a stuck pending state. `findByText`
  normalises whitespace: await the author label, compare `li.textContent` byte-exact.
