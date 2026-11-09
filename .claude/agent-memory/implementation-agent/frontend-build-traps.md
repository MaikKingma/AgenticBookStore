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
- `rg` is not on the Bash tool PATH here and BSD grep has no `-P`. Byte-level ASCII check, in a script: `env LC_ALL=C grep
  -rnI $'[\x80-\xff]' <paths>`. The Write tool stores U+2003 as a plain space, so a Written control file proves nothing; use
  a committed file known to hold non-ASCII as the positive control.
- C0-6 greps are literal and also hit test titles and comments: `fontsource` (6e) and `document title` (6d, the dot is a
  wildcard) in an `it(...)` title fail the gate. Run the whole C0-6 list before handing off.
- Must-fail probes for a shared setup file: one script with a loop that deletes one line per run via `grep -vF`, restores from
  a copy and compares `shasum` (see the T2 outbox for the pattern).
- Vitest 5 turns every `*.css` import into an empty string, the `?raw`, `?inline` and glob forms too (plugin
  `vitest:css-empty-post` matches `.css?`). To read stylesheets in a test use `node:fs`; tsconfig.app.json has
  `types: ["vite/client"]` only, so start the test with `/// <reference types="node" />` (no config change). Use
  `import.meta.dirname`, not `import.meta.url` (jsdom makes it a non-file URL). `?raw` works for html and other files.
- Test sources are grepped for `https?://`, `.fails(`, module-level `const x = [`/`new`: write URL fixtures as
  `${'https'}://host`, keep fixtures inside `it`/`describe`, and use `as const` for module-level tables.
- A pinned font licence text can trip the QA greps (`reserved`, `https?://`): strip only the URL schemes and say so.
- A mocked `fetch` that rejects on an unmatched request is swallowed by a client that maps rejections to its own error, so
  the test still passes. Record unmatched requests in the mock and check them in `onTestFinished` (a throw there fails the
  test; verified); take injection of the registrar as a parameter so a plain test can prove the wiring. Plain `Response`,
  `Headers` and `ReadableStream` work under the jsdom environment. Probe runners: a `python3 -I` script that replaces text,
  runs `vitest run src/api`, restores from a copy and compares sha256 per probe.
- Order pass-through fixtures (C0-16): at least three elements, and for every attribute the middle value strictly above
  or below both neighbours (never ascending or descending). Numeric strings need this under string AND numeric
  comparison ("12.50" sorts before "6.99" as text). Keep `inStock = available > 0`. A python sweep of sort mutants per
  attribute and direction proves it. The rule covers EVERY array under a `toEqual`, not only lists named "in order":
  arrays nested in a view (`authors`), sent payloads (`lines`), and a test helper's call log. One UUID per entity across
  all test files (a not-found id is its own literal; a 404 or 409 detail must not reuse a live order or book id).
- Rule of the plan: the QA verifier asks for a guard test per afterEach statement (`setupTests.test.ts`: a dirty test, then
  a test asserting clean state). The assertion at the very end of the afterEach itself is only killed by a probe in another
  test file, not by the guard.
