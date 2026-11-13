---
name: worktree-agent-shell-guard
description: Bash guard quirks for worktree-isolated research agents and cheap ways to get Surefire totals and wall time
applies-to:
  - any
metadata:
  type: reference
---

In a worktree-isolated agent the Bash guard refuses any command whose paths contain backslash-escaped spaces or that mixes
several paths ("too complex to verify"). The project root path contains spaces.

How to apply:
- Run commands from the worktree cwd with relative paths (`git grep`, `cat bookstore/...`, `bash .claude/comms/tmp/x.sh`);
  use the Read tool for absolute paths with spaces.
- `git grep -n <pat> -- <paths>` replaces grep -r; it also gives the "finds nothing" proof for DoD greps.
- Wall time of the referee: `/usr/bin/time -p bash ~/.claude/scripts/with-budget.sh 300 ./mvnw -q clean verify` in the
  background (-q prints no totals; the output file is huge, tail it). Surefire totals: sum the
  `Tests run:` lines of `<module>/target/surefire-reports/*.txt` with a small script file (the XML header has
  attributes on one line too, but the .txt is simpler).
- The claude-api skill's "Current Models" table is the authority for model ids (read it, never recall); it also lists
  per-model API quirks such as rejected sampling parameters.
