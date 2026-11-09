---
name: linux-only-flaky-cli-tests
description: How to verify a fix for a node:test CLI test that is green on macOS and flaky on Linux CI (process.exit after console.error over spawnSync pipes); container repro, must-fail witness, load loops
metadata:
  type: feedback
---

Rule: a test that runs a CLI through `spawnSync` pipes and reads a long stderr is Linux-flaky when the CLI ends with `process.exit(n)`; prove a fix by reproducing red and green on Linux, not by repeating macOS runs.

**Why:** Node writes pipe output asynchronously on Linux, so a failing run that exits right after a long `console.error` loses the tail (status and stdout stay intact). macOS never truncated in 480+ runs, so any amount of local looping is vacuous; only the Linux CI showed it.
**How to apply:**
- Container: `docker run ubuntu:24.04` with a Linux Node tarball mounted (`-v <dir>/node-v..-linux-arm64:/node:ro`), `apt-get install -y libatomic1`, worktree mounted `:ro`, copy `guide/` and `docs/spec-pack` into the container so the checker finds the pack relative to itself. 8 parallel workers x 25 to 40 runs of the one test file give a measurable rate (old test red about 11 percent, pipe-capture mutant 8 percent).
- Take the "before" file from `git show <sha>:<path>`, not from the implementer's copy, and compare sha256.
- Must-fail for the new witness: a mutant of the capture helper back to pipes run in the same container (red in some runs), plus deterministic macOS mutants (cut stderr at the CI byte, drop the last line, status 0, wrong count in the CLI). Which test sees a given cut depends on where it falls, so do not expect the witness to be the more sensitive one.
- Bash traps: `pids="$pids $(burn)"` where burn backgrounds a busy loop hangs forever (the substitution waits for the background job's pipe); background with `> /dev/null 2>&1 &` and use `$!`. A `\.` before a multi-byte char in `grep -E` fails in the Ubuntu C locale: write the literal `ℹ`. A foreground wait loop belongs in a script file (`sleep` alone is blocked).
- Rule on the underlying `process.exit` in a frozen checker: pre-existing and failing-path only means follow-up (`process.exitCode = 1`), not a finding of the task that only fixed the test.

Related: [[docusaurus-guide-probes]], [[shell-traps]]
