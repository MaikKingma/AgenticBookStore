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

- `rg` here is a zsh shell function, not a binary on PATH. Inside `bash script.sh` it gives "rg: command not found" (exit 127), which can pass for an empty result. Run every `rg` check directly in the Bash tool, never from a script. Keep greps that scripts run on ugrep `grep`.

- The Edit tool trims trailing whitespace in `old_string` and `new_string`. A probe that ends in `, ` (for example `super("code", `) restores without the space, and a restore using the same trimmed string then fails with "String to replace not found". Anchor probe edits on text that does not end in a space (include the next token), and after the last restore compare the file hashes with the before-snapshot.

Related: [[scaffold-verification-techniques]], [[final-sweep-checks]]
