---
name: docusaurus-marker-ban-and-site-pass
description: QA of the live-guide rendered pass (offline zip, dev console, print, patched rendered script) and Linux-only flaky node:test CLI tests; marker-ban wiring mutants live in docusaurus-guide-probes
metadata:
  type: feedback
---
Rule: a "turn this rule on for the real tree" change is probed with a scratch copy and a mutant list over rule AND wiring; the list and the reinsertion probe are in [[docusaurus-guide-probes]].
**Why:** extra traps from banning a required-presence rule: a mutant that bans only pages with empty anchors survives when every witness uses Level 1, so witness Level 2 to 4 as well; sentence-level negation rules ("must not claim X") are killed by pack-line equality of a hook fragment only, claim-style wording mutants pass the frozen checker.
**How to apply:** a deletion-only or word-swap task on a frozen checker: reverse the edits in memory and `Buffer.compare` with `git show <base>:<path>`; the reversed sha must equal the previous task's hash-file entry (superseded entries fail by design). A ban on literal text is guarded only by your own grep: show it red on a scratch copy with the paragraph restored while `check` and `check:built` stay green. Built scan: tag-stripped HTML plus JS chunks (Prism splits `install.ps1`; an unselected tab panel is in `textContent`, not `innerText`).

Rule: derive the rendered script from the previous one with an exact-replacement patcher, never hand-edit a 400-line file.
**Why:** a patcher that stops when any `from` string occurs other than once shows exactly what changed from the implementer's script and keeps the independent run honest.
**How to apply:**
- Console buckets strictly per engine: Chromium offline axe triple only, WebKit offline FF-5 pair plus the axe preload warning, Firefox nothing. Run the abort control (fetch to https://example.com) in its own phase; Firefox logs its CORS error late, so classify by URL.
- CI zip: `gh run download <run id> -n workshop-guide-offline -D <fresh dir>` (gh and lsof may be off the pinned PATH; lsof is in /usr/sbin), unzip, `diff -rq` against an unzip of your own `build-offline` zip; extract the CI copy into a path with spaces for the file:// matrix.
- Rig traps: `wait` with no PIDs also waits for background `docusaurus serve` children (use `wait $PIDS`); `innerText` applies `text-transform` (count admonition titles via `textContent`); an offline `context.route` handler that does not `abort()` hangs silently; the 1280 states phase may follow the first content link, so reload before a link-click check; read `h1` and `meta[name=description]` only after waiting for the new page; collapsed sidebar category needs a label click; click only visible copy buttons.
- Print: `page.pdf({format:'A4'})` page count = `/Type /Page[^s]` matches; hide the last section by `display:none` for "second page only for X"; also measure at 10 mm margins (can differ from margin-0). Headroom control: build the old text plus one restored word into a scratch tree; if it prints one page more while the final tree fits, the compensation edits are load-bearing. Run the same print script on the base build: the fixed count must differ there.
- Must-fail mutants for a rendered-only assertion (a Checkpoint retitled or added) pass the frozen checker and both builds; show checker exit 0 and the failing rendered line in one log.

Rule: a test that runs a CLI through `spawnSync` pipes and reads a long stderr is Linux-flaky when the CLI ends with `process.exit(n)`; prove a fix by reproducing red and green on Linux, not by repeating macOS runs.
**Why:** Node writes pipe output asynchronously on Linux, so a failing run that exits right after a long `console.error` loses the tail. macOS never truncated in 480+ runs; only Linux CI showed it.
**How to apply:**
- Container: `docker run ubuntu:24.04` with a Linux Node tarball mounted, `apt-get install -y libatomic1`, worktree `:ro`, copy `guide/` and `docs/spec-pack` inside. 8 workers x 25 to 40 runs give a measurable rate (old test red about 11 percent). Take the "before" file from `git show <sha>:<path>` and compare sha256.
- Must-fail for the witness: the capture helper mutated back to pipes run in the same container, plus deterministic macOS mutants (cut stderr at the CI byte, drop the last line, status 0, wrong count). Do not expect the witness to be the most sensitive test.
- Bash traps: `pids="$pids $(burn)"` with a backgrounded busy loop hangs (background with `> /dev/null 2>&1 &`, use `$!`); `\.` before a multi-byte char in `grep -E` fails in the C locale; foreground wait loops belong in a script file.
Related: [[docusaurus-guide-probes]], [[shell-traps]]
