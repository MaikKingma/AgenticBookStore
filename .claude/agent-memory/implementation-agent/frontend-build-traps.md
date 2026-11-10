---
name: frontend-build-traps
description: Non-obvious traps for the frontend module (Maven-installed Node, Vite 8, Vitest 5) and the workarounds that held
metadata:
  type: feedback
---

- First Node install without a package.json: `./mvnw -f frontend/pom.xml -Dskip.npm=true generate-resources` installs
  `frontend/node` only (the npm executions log `Skipping execution.`). Scaffold with `npm create vite@<pin> app -- --template
  react-ts --no-interactive` into the scratchpad, never into the repo; the template has no `strict` in tsconfig.app.json
  (add it) and ships a favicon, `public/` and `oxlint` to delete.
- Vitest with `globals: false` does not register RTL auto cleanup: call `cleanup()` in `afterEach` from setupTests.
  Order inside one `afterEach`: cleanup, read the console spy calls, `restoreAllMocks`, then `expect` last, so a failing
  assertion cannot skip the resets. Keep spies out of module-level `let` (QA greps `^(export )?(let|var) `): fetch them
  back with `vi.mocked(console.error)` and guard with `vi.isMockFunction`.
- Vite's dev server binds `localhost`, which can be IPv6 only: `curl http://127.0.0.1:5173/` fails with code 000 until
  `server.host: '127.0.0.1'` is set.
- `rg` and a `Grep` tool are absent (grep through Bash), BSD grep has no `-P`, and the worktree Bash guard refuses
  `cd <dir> && ...` and inline `for` loops (use a script in `.claude/comms/tmp`). Byte-level ASCII check, in a script: `env LC_ALL=C grep
  -rnI $'[\x80-\xff]' <paths>`. The Write tool stores U+2003 as a plain space, so a Written control file proves nothing; use
  a committed file known to hold non-ASCII as the positive control.
- Must-fail probes for a shared setup file: one script with a loop that deletes one line per run via `grep -vF`, restores from
  a copy and compares `shasum` (see the T2 outbox for the pattern).
- Vitest 5 turns every `*.css` import into an empty string, the `?raw`, `?inline` and glob forms too (plugin
  `vitest:css-empty-post` matches `.css?`). To read stylesheets in a test use `node:fs`; tsconfig.app.json has
  `types: ["vite/client"]` only, so start the test with `/// <reference types="node" />` (no config change). Use
  `import.meta.dirname`, not `import.meta.url` (jsdom makes it a non-file URL). `?raw` works for html and other files.
- A mocked `fetch` that rejects on an unmatched request is swallowed by a client that maps rejections to its own error, so
  the test still passes. Record unmatched requests in the mock and check them in `onTestFinished` (a throw there fails the
  test; verified); take injection of the registrar as a parameter so a plain test can prove the wiring. Plain `Response`,
  `Headers` and `ReadableStream` work under the jsdom environment. Probe runners: a `python3 -I` script that replaces text,
  runs `vitest run src/api`, restores from a copy and compares sha256 per probe.
- Hash router tests: assigning `window.location.hash` fires `hashchange` in a later task, so await a `findBy*` right after
  (RTL turns the act environment off while it polls, no act warning); set the hash before `render` for a start route.
  A stale async answer needs a body-read witness (`vi.spyOn(response, 'text')`, then `waitFor`), not a timer, before the
  absence assertion. C0-6b also hits a module-level `const X: T = [`: wrap with `Object.freeze(...)` or use `as const`.
- jsdom "Not implemented: HTMLFormElement's requestSubmit()" goes to raw stderr, NOT through the console trap, so a form
  handler missing `preventDefault` stays green. Witness it in the test: `document.addEventListener('submit', (e) =>
  seen.push(e.defaultPrevented))` (runs after React's root listener), `onTestFinished` removes it, expect `[true]`.
- Router rows an unknown-route list needs: unknown first segment WITH a second segment (`#/basket/x`, `#/foo/bar`), a
  prefix like `#xbasket`, `#/basket?x=1`, and a double-encoded id decode row (`a%2541` gives `a%41`); the final
  `return notFound` after a valid id is otherwise never executed. A genre or enum shown on a card needs one fixture with an
  underscore (`SCIENCE_FICTION`) to pin "shown as sent".
- A synchronous App test row that now renders a real fetching page: install `mockFetch` routes whose `respond` is
  `new Promise<Response>(() => {})`, or the answer lands after the test and logs an act warning (console trap fails it).
  A hook that throws outside its provider logs through `console.error`: `vi.mocked(console.error).mockClear()` after the
  `toThrow` assertion. A stale-answer test needs `await act(async () => { for (...) await Promise.resolve() })` after the
  body-read witness; probe it by forcing `if (true)` in the guard (both success and failure branch).
- "Sent as typed" text fields (e-mail): `type="email"` makes the DOM (jsdom and browsers) trim edge whitespace in `value`,
  so edge-whitespace witnesses never reach the payload. Use `type="text"` with `inputMode="email"`; keep `noValidate` on
  the form anyway when the card names it. Controlled `type="number"` inputs reset to the state value after a refused
  `fireEvent.change` (React restores it), which is what a "value stays 3" test needs.
- Form pages that call an id helper inside the send path: make the send an `async` function with try/catch around
  `await chat({ id: helper(), ... })`, so a throwing helper (`crypto.randomUUID` is absent on plain-http non-localhost
  origins) becomes the generic alert instead of a stuck pending state. A reply with edge whitespace cannot be awaited with
  `findByText` (it normalises); await the author label instead and compare `li.textContent` byte-exact. A counter is
  checked with `toHaveAccessibleDescription` when the textarea points at it with `aria-describedby`.
- Live checks after a rebuild: the jar serves `index.html` with `Last-Modified` 1980, so a REUSED Playwright session can keep
  the old `index.html` (old CSS hash) and measure the old build. Open a new session per build. `overflow-wrap: anywhere`
  inherited from `.card` makes `th`/`td` min-content 1 char: `overflow-wrap: normal` alone still wraps headings at spaces
  (taller on a phone); add `white-space: nowrap` for the short columns. A style test needs a non-colour property in CSS
  fixtures (`color: red` trips the colour-literal scan).
- "Existing test file unmodified, 0 deletions" tasks: add imports as NEW lines after the old import block (never widen an
  existing `import { a }` line) and append to the last `describe` by replacing its closing lines with themselves plus the
  new block; fixtures for a page that only reads some fields can omit the C0-6c words (isbn, onHand, reserved). Shared
  in-flight state (a request that outlives its page) belongs in the root provider with a `useRef` guard next to the
  state: only a same-tick hook test (`act(() => { place(); place() })`) kills a state-only guard.
- CSS source-level contract tests (styleContract.test.ts): read the effective cascade (top-level rules only, source order, last
  declaration wins; comments and media queries excluded) and put the real assertions in helpers that throw; run the helper on
  the real file and, via `expect(() => helper(css)).toThrow()`, on one-mutation variants of a passing fixture. `Number(` and
  `parseFloat` hit the C0-6c greps: use unary `+x`. DOM element arrays: `toEqual` is `isEqualNode`, compare identity with `toBe`.
