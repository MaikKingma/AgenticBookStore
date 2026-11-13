---
name: docusaurus-marker-ban-and-site-pass
description: Live-guide rendered pass (console buckets, CI zip, print count) and Linux-only flaky CLI tests
metadata:
  type: feedback
---
Rule: derive a rendered script from the previous one with an exact-replacement patcher, never hand-edit a 400-line file.
**Why:** a patcher that stops when any `from` string occurs other than once shows exactly what changed from the implementer's script and keeps the independent run honest.
**How to apply:**
- Console buckets strictly per engine (offline: Chromium axe triple only, WebKit CORS pair plus the axe preload warning, Firefox nothing). Run the abort control (fetch to https://example.com) in its own phase; Firefox logs its CORS error late, so classify by URL.
- CI zip: `gh run download <run id> -n workshop-guide-offline -D <fresh dir>` (gh and lsof may be off a pinned PATH; lsof is in /usr/sbin), unzip, `diff -rq` against your own `build-offline` zip; extract the CI copy into a path with spaces for the file:// matrix.
- Print: `page.pdf({format:'A4'})` page count = `/Type /Page[^s]` matches; hide the last section by `display:none` for "second page only for X"; also measure at 10 mm margins (can differ from margin-0). Headroom control: build the old text plus one restored word into a scratch tree; if it prints one page more while the final tree fits, the compensation edits are load-bearing. Run the same script on the base build: the fixed count must differ there.

Rule: a test that runs a CLI through `spawnSync` pipes and reads a long stderr is Linux-flaky when the CLI ends with `process.exit(n)`; prove a fix by reproducing red and green on Linux, not by repeating macOS runs.
**Why:** Node writes pipe output asynchronously on Linux, so a failing run that exits right after a long `console.error` loses the tail; macOS never truncated in 480+ runs.
**How to apply:**
- Container: `docker run ubuntu:24.04` with a Linux Node tarball mounted, `apt-get install -y libatomic1`, worktree `:ro`, copy `guide/` and `docs/spec-pack` inside. 8 workers x 25 to 40 runs give a measurable rate (old test red about 11 percent). Take the "before" file from `git show <sha>:<path>`, compare sha256.
Related: [[docusaurus-guide-probes]], [[shell-traps]]
