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

Related: [[scaffold-verification-techniques]], [[final-sweep-checks]]
