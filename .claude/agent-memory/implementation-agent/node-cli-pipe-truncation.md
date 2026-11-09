---
name: node-cli-pipe-truncation
description: A node:test that spawns a CLI which ends with process.exit(1) and reads its long stderr over a pipe is flaky on Linux CI only; capture through files
metadata:
  type: reference
---

- Symptom: a spawnSync test of `guide/scripts/check.mjs` (exit 1, about 33 KB stderr, ends `process.exit(1)` after
  `console.error`) passed on macOS in 500+ runs and failed on the Linux CI runner: stderr cut at byte 19555, so the last
  lines (the asserted `skeleton:` line, `check failed with N error(s)`) were missing while status stayed 1.
- Cause: stdio pipes are asynchronous on Linux, `process.exit()` drops what is still queued. Not a shared temp path or a test order issue.
- Fix that held (test side, checker untouched): `spawnSync(node, [script], {cwd, stdio: ['ignore', fdOut, fdErr]})` with two
  `openSync` files in a `mkdtempSync` dir, read them back; file writes are synchronous. Helper `runCliCaptured` in
  `guide/scripts/skeleton-ban.test.mjs`. Use it for any CLI run whose output is long and ends in an exit(1).
- Reproduce a Linux-only race locally: official `node-v<ver>-linux-arm64.tar.xz` from nodejs.org, run it in `ubuntu:24.04` with
  `apt-get install libatomic1` and the worktree mounted read-only, several parallel workers (drivers were
  `.claude/comms/tmp/impl-t11-repro*.{mjs,sh}`, scratch only). Roughly 20 % of runs truncated before the fix, 0 of 800 after.
- Open follow-up (checker is frozen): `process.exitCode = 1` instead of `process.exit(1)` in `check.mjs` would also stop CI logs losing the error tail.
