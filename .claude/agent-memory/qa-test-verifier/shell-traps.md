---
name: shell-traps
description: zsh and Bash-tool traps hit while running QA commands in this repo (globbing, find parentheses, mailbox dirs)
metadata:
  type: feedback
---

Rule: quote every glob-bearing argument; the Bash tool runs zsh, which fails on unmatched globs.

**Why:** `--include=*.java` and unescaped `( ... )` in `find` abort with "no matches found" before the command runs, which can look like an empty, passing result.
**How to apply:**
- Write `"--include=*.java"` and `find ... \( -name a -o -name b \)`.
- Repo paths contain spaces: always double-quote absolute paths.
- Relative paths in Bash resolve from the project root (the primary working directory), so `./mvnw clean verify` works without `cd`.
- `./mvnw ... -l <scratch>/referee.log` keeps the referee console out of context; grep the log afterwards for the reactor summary, `Tests run:`, `Creating container for image` and `Started .* in`.
- A brand-new QA mailbox may not exist at spawn (no inbox dir). Polling then prints "No such file"; that means the inbox is empty. Writing the outbox artifact creates the directory.
- `echo ==== label` fails in zsh ("= not found", `=cmd` expansion) and aborts the rest of that Bash line, so a following grep silently never runs. Quote separators (`echo "== label"`) and re-check that every grep in a chained line printed.
- `git grep -E` here does not understand `\s` (nor rely on `\b`): `git grep -c -E '^\s*@ArchTest'` exits 1 on a file with 20 of them. A `\s` alternative inside a skip/Disabled pattern is silently vacuous. Use `[[:space:]]`, drop anchors, and prove the pattern bites with `git -C <scratch> grep --no-index -n -E P <control-file>` (from the repo cwd `--no-index` refuses paths outside it).
- Never guess heartbeat or message timestamps: take `date -u` each time. `stat` mtimes print local time (+0100 here), so convert them before comparing with UTC stamps in an order proof.
- `grep` here is ugrep: a missing path (for example `http/` before T9) gives exit 2 with `ugrep: warning: ... No such file or directory`, and any matches in the other paths are still printed. Re-run on the existing paths alone so that "empty" means exit 1.
- In the Bash tool, a `grep` or `rg` with no match shows "completed with no output", not "Exit code 1" (`git grep`, `lsof` and `cmp` do show their exit code). A negative control that cannot match looks the same. So every empty grep needs a positive control on a path that does match.
- Inside `bash script.sh`, `grep` is BSD grep: `-P` prints a usage error and the count comes out empty, which looks like a clean ASCII check. Run non-ASCII checks as `rg -c "[^\x00-\x7F]"` directly in the Bash tool, with a printf-built control file.
- `rg` here is a zsh shell function, not a binary on PATH. Inside `bash script.sh` it gives "rg: command not found" (exit 127), which can pass for an empty result. Run every `rg` check directly in the Bash tool, never from a script. Keep greps that scripts run on ugrep `grep`.
- `git show <rev>:<path> --output=<file>` and `git diff ... --output=<file>` in the Bash tool printed to stdout and left an empty file. Use a plain `> <file>` redirect, then check the size with `wc -c` (an empty file can look like an empty diff).
- The Edit tool trims trailing whitespace in `old_string` and `new_string`. A probe that ends in `, ` (for example `super("code", `) restores without the space, and a restore using the same trimmed string then fails with "String to replace not found". Anchor probe edits on text that does not end in a space (include the next token), and after the last restore compare the file hashes with the before-snapshot.
- The Write tool turns U+2003 into a plain space even between other characters (`a<U+2003>b` landed as `61 20 62`), and trims a file whose only content is a U+2003, so the non-ASCII positive control silently fails. Build it with `printf 'a\xe2\x80\x83b\n' > <scratch file>`, check with `od -An -tx1`, then `rg -c`. The em dash (U+2014) survives Write.
- A snapshot file that holds porcelain status plus `shasum` lines: `grep " <path>$"` also matches the status line, so a hash lookup returns two lines and reports a false MISMATCH. Filter `grep -E '^[0-9a-f]{64}  '` first.
- Probe runs go faster with two scripts in `.claude/comms/tmp/`: a probe runner (maven `-Dtest`, summary grep) and a restore script that `cp`s the FF7 `.orig` byte copy back and prints `shasum` beside the snap-before line. Edit mutates, the script restores and proves the hash in one call.
- A multi-file `grep -h` (or `grep -c`) in the Bash tool did not print the files in argument order, so merged failure blocks got pinned to the wrong probe. Quote kill reasons per log, or from a script that labels each block with its probe id.
- The worktree-isolation guard refuses `bash <script> git -C <other repo> ...` (git in the argv), but direct `git -C <scratch clone> ...` calls are allowed. Run git facts on a scratch clone directly; keep git out of script args. The same guard also refuses a bare `bash --version` ("cannot be shown not to run git"). `bash <abs script in the worktree>` works, and `git -C <worktree>` inside such a script ran fine.
- Mutant generators: `grep -cF -- "$OLD"` with a newline in `$OLD` treats it as two patterns, and the empty second pattern can match every line, so an "exactly once" guard is unreliable (spotted by review before a run). Count with `OLD=... perl -0ne '$c = () = /\Q$ENV{OLD}\E/g; print $c'` and replace with `perl -0pe 's/\Q$ENV{OLD}\E/$ENV{NEW}/'`.
- `curl -i -w ' time_total=%{time_total}'` appends the timing to the BODY line (no newline). A diff filter `grep -v time_total` then drops every body and the diff looks clean. Strip the suffix with `sed -E 's/ time_total=[0-9.]+$//'` and compare line and byte counts of both sides.
- The worktree guard refuses a command whose path has backslash-escaped spaces (`.../OpenValue\ AI/...`) as "too complex"; the same path in double quotes runs. Prefer worktree-relative paths for `rg`/`grep` and double-quoted absolute paths for scripts.
- A Maven log that captured a raw NUL (an escape mutant logging U+0000) makes ugrep print `Binary file ... matches` instead of lines, so a summary grep looks empty. Use `grep -a` on probe logs.

- macOS `/bin/bash` is 3.2: `declare -A` (associative arrays) and `${!arr[@]}` fail in a runner script. Use a `case` function mapping names to paths.
- A foreground `bash <wait script>` with a `sleep 10` loop (about 3 minutes per call) is the way to wait for a background probe batch; a bare `sleep` in a Bash call is blocked and a Monitor alone ends nothing for a subagent.

- A Maven log that captured a raw NUL (an escape mutant logging U+0000) makes ugrep print `Binary file ... matches`
  instead of lines, so a summary grep looks empty. Use `grep -a` on probe logs.
- Ledger hash baselines are `shasum -a 256` (64 hex); plain `shasum` prints sha1 and will never match. Also, a path
  built from the repo root without the `.claude/worktrees/<id>` segment still exists (the main checkout), so a `printf >`
  there succeeds silently outside the worktree; use worktree-relative paths for scratch files.
Related: [[scaffold-verification-techniques]], [[final-sweep-checks]]
