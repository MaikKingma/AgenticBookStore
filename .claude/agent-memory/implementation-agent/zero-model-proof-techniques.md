---
name: zero-model-proof-techniques
description: Proving staged config, generated trees and hand-written Agent OS files with zero model runs (glob tests, git grep on a sha, hooks, installer isolation, trace tables) and installer traps
applies-to:
  - .claude/**
  - agent-os/**
metadata:
  type: reference
---
- **Glob test for rule `paths`**: never `fnmatch` (`*` crosses `/`). Cross-check a small regex translator,
  `pathlib.PurePosixPath.full_match` (Python 3.13) and `git ls-files ':(glob)<pattern>'` on the sandbox, with widened and narrowed
  negative controls. Test folders named like the layer (`archfixtures/domain`) trap a missing `main` anchor.
- **Tokens on a recorded sha**: `git grep -n -e <token> <sha> -- <paths>` (read-only; patterns starting with `-` need `-e`;
  `git grep -F ""` errors, use `git ls-tree -r --name-only`). macOS `git grep -E` has no `\w`/`\b`: use `-P`. `origin/factory`
  can move mid-task: pin the sha. `./mvnw -pl <module> spring-boot:help -Dgoal=run` resolves a goal without starting the app.
- **Shell**: multi-step logic in `.claude/comms/tmp/` scripts. Never `cat "$RAW"` into a file that is also `$RAW` (filled 300 GB).
  `grep` has no `-P`: use `python3 -I`. `mask` BEFORE any `cut`; evidence must not match its own greps. `git status --short`
  folds untracked folders: `--untracked-files=all`.
- **Completeness vs quote fidelity** are separate checks: a token inventory of every staged file diffed against the record, and
  a quote pass (strings of 12+ chars) against pack, plan, guide and fetched pages, with mutated-quote controls. A misspelling
  control must land in the staged copy AND the in-memory built tree, else the generic "staged differs" row hides a missing
  token check; assert each control by its own finding text. Search tolerates hard wraps (needle spaces become `\s+`).
- **Hooks without Claude**: run the staged PreToolUse hook as settings name it, from another folder, with only
  `CLAUDE_PROJECT_DIR` set and a one-line JSON payload on stdin. Plant the red test as an untracked file so `git reset --hard &&
  git clean -fdx` removes it. "Cannot start": `chmod -x`, `sh -c` exits 126.
- **Generator or installer under isolation** (`npx`, `uvx`): HOME, every `XDG_*`, `npm_config_cache`, `UV_CACHE_DIR`,
  `UV_TOOL_DIR` in scratch; `env -i HOME=<h> PATH=<python 3.11+ dir>:/opt/homebrew/bin:/usr/bin:/bin ...` (else uv downloads
  Python). List files before and after; a second plain run shows what the user's own run writes. The npm package ships no `docs/`.
- **Trace table with needles in both texts**: one TSV row per range of a written file (written lines, source and lines, needles).
  The checker fails when a needle is missing from either side or a content line is in no row. A `LINE` needle fits verbatim
  tables. Write it red first with an empty trace; wrapped source prose breaks a needle across lines: one-line needles only.
- **Universal claims** need counterexample searches on two trees (`git archive` of the tag, built sandbox), each with a control.
- **Agent OS installer** (`project-install.sh`) copies only `profiles/<p>/standards/**`; the default profile ships
  `global/tech-stack.md`, so no standards install and `/plan-product` step 4 finds none. It writes `.claude/commands/agent-os/*.md`
  and `agent-os/standards/index.yml` only; bare `mktemp` ignores TMPDIR on macOS: run with a `mktemp` shim on PATH.
  `--commands-only` rebuilds `index.yml` from the files (stage with the builder's trailing blank line). A clone of the start
  branch prompts "Existing standards folder detected"; a `solution/level-3` clone avoids it.
- Commands in `.claude/commands/<subdir>/` are `/<subdir>:<name>` (`/agent-os:plan-product`); pack and guide write the short form.
