---
name: frontend-vitest-probe-techniques
description: Verifying the Maven-installed Node frontend module (Vitest, console trap, shared setup hooks) without a global Node, and traps that hide stderr or survivors
metadata:
  type: feedback
---

Rule: run Vitest with the verbose reporter yourself; the Maven `npm test` run hides what the verbose run shows.

**Why:** the default reporter under Maven printed no `stderr |` block for a passing test, while `--reporter=verbose` printed one (a guard test that logged on purpose through a call-through `vi.spyOn(console, ...)`). A green Maven log proves nothing about console hygiene.
**How to apply:**
- A script in `.claude/comms/tmp/` with `export PATH="<FE>/node:/usr/bin:/bin:..."` then `exec node/node node_modules/vitest/vitest.mjs run --reporter=verbose --reporter=json --outputFile=<SP file> <test files>`; call it as `bash with-budget.sh 180 bash <script> > log 2>&1` (no `cd`, no env prefixes with paths that hold spaces; the worktree guard refuses compound commands).
- Count `grep -c 'stderr |'`, and `grep -cE 'Warning:|act\(|Not implemented'`. A spy without `mockImplementation` always passes through to the real console.
- The JSON outputFile is overwritten by every run; copy it before an extra witness run if you cite it.
- Probe runner: one `python3 -I` file holding a dict of probes (list of file/old/new edits plus test files), asserts each `old` occurs exactly once, restores from a `.orig` copy after each run and prints sha equality. `vitest` transpiles only, so probes must change runtime behaviour.
- Guard-of-the-guard gap: a console trap whose `expect` lives in `afterEach` cannot be asserted by a committed test inside a small file scope. Probe by deleting that `expect` AND adding `console.error('probe')` to a test: it stays green (survivor). Report it as a ledger note and keep a `console.error`/`console.warn` probe in every later task's list.
- Dev-server check: start `node/npm run dev` in a script (subshell, `&`, write the pid), sleep, `curl`, kill the `lsof -tiTCP:<port> -sTCP:LISTEN` pid plus the wrapper pid, then `lsof` again; the same command with `PATH=/usr/bin:/bin` gives `env: node: No such file or directory` exit 127.
- `rm -rf frontend/node_modules` before the first `-f frontend/pom.xml clean verify` makes `npm ci` run from scratch (log shows `added N packages`); `clean` deletes only `dist`, and a failed build leaves no `dist`, so rebuild with `-DskipTests` before testing that `clean` removes it.

Related: [[scaffold-verification-techniques]], [[shell-traps]]
