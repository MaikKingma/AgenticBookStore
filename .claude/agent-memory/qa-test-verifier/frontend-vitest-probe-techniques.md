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

- CSS contract tests (hand-rolled parser plus fixture strings): probe three families, not one. (1) value mutants (token hex, contrast formula terms, threshold) die on the calibration literals; (2) rule-deletion mutants (delete a whole class rule) survive when only colour pairs are scanned, so list which classes a test really names before believing "pinned class names"; (3) new-file mutants (a probe `.css` under a new `src/components/` dir; the runner must create and `rmdir` it) prove the scan covers files added later. Binary fixtures (woff2) are probed by overwriting with HTML bytes and restoring from the `.orig`.
- Fixture strings in a test file trip the repo's own literal greps (for example `style={{ color: 'red' }}` against the inline-style grep, an absolute URL against the CDN grep). Run every C0 grep over the test files too and expect fixtures to be built from parts.
- Vitest 5 empties every CSS import including `?raw`; tests read CSS with `node:fs`. A `it.fails` guard is probed by deleting the trap's final `expect` (both fixtures report "Expect test to fail" and the recorder test fails).
- Rework rounds re-run every global grep over the whole tree, not only the one the last FAIL named: a reworded or new test title can trip a different literal grep (a title naming `@fontsource/...` hit the case-insensitive CDN grep while the inline-style grep was clean).
- Class-contract tests ("each contract class has a rule"): probe with a regex rename of every class in turn (`\.name(?![\w-])` to `.namezz`, all occurrences) plus a rename of only the base rule when a longer class shares the prefix; deleting only a base block survives whenever `.x a` or `.x:hover` rules remain (the check matches any selector), so report that as a design limit, not a defect. A hard-coded class list can shrink silently unless its length is asserted.
- Re-run the referee after any extra probe round: the "referee last" rule is about the newest restore, so a late probe batch costs one more 25 s run.

Related: [[scaffold-verification-techniques]], [[shell-traps]]
