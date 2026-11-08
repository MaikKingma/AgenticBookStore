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
- `rg` is not on the Bash tool PATH here; use `grep -rnP '[^\x00-\x7F]'` for the byte-level ASCII check.
- Must-fail probes for a shared setup file: one script with a loop that deletes one line per run via `grep -vF`, restores from
  a copy and compares `shasum` (see the T2 outbox for the pattern).
- Rule of the plan: the QA verifier asks for a guard test per afterEach statement (`setupTests.test.ts`: a dirty test, then
  a test asserting clean state). The assertion at the very end of the afterEach itself is only killed by a probe in another
  test file, not by the guard.
