---
name: consistency-check-techniques
description: Zero-model proof techniques for staged config and generated trees (globs, token resolution on a sha, sandboxes, hooks, installer isolation, Spec Kit init) and their traps
metadata:
  type: reference
---
Techniques that held for consistency-check tasks with no Claude runs:

- **Glob test for rule `paths`**: never `fnmatch` (its `*` crosses `/`). Cross-check a small regex translator,
  `pathlib.PurePosixPath.full_match` (Python 3.13) and `git ls-files ':(glob)<pattern>'` on the sandbox, plus widened and
  narrowed negative controls per rule. Test-tree folders named like the layer (`archfixtures/domain`) are the trap a missing
  `main` anchor would match.
- **Resolve tokens on a recorded sha**: `git grep -n -e <token> <sha> -- <paths>` (read-only). A pattern starting with `-`
  needs `-e`; `git grep -F ""` is an error (use `git ls-tree -r --name-only`). macOS `git grep -E` has no `\w`/`\b` (use `-P`).
  `origin/factory` can move mid-task (shared clone): pin the sha in every script. A Maven goal resolves without starting the
  app: `./mvnw -pl <module> spring-boot:help -Dgoal=run`.
- **Shell traps**: multi-step logic in a script under `.claude/comms/tmp/`. Never `cat "$RAW"` into a file that is also
  `$RAW` (filled 300 GB). `grep` has no `-P`: print context with `python3 -I`. `mask` BEFORE any `cut`; evidence must not
  match its own greps. `git status --short` folds untracked folders: use `--untracked-files=all`.
- **Completeness and quote fidelity are separate checks**: a token inventory of every staged file diffed against the record,
  and a quote pass (strings of 12+ characters) against pack, plan, guide and fetched pages, with mutated-quote controls. A
  misspelling control must land in the staged copy AND the in-memory copy of the built tree, else the generic "staged differs
  from tree" row reports it and hides a missing token-specific check; assert each control by its own finding text. Spelling
  search tolerates hard-wrapped prose (needle spaces become `\s+`).
- **Hooks without Claude**: run the staged PreToolUse hook as settings name it, from another folder, with only
  `CLAUDE_PROJECT_DIR` set and a one-line JSON payload on stdin. Plant the red test as an untracked file so `git reset --hard &&
  git clean -fdx` removes it. "Hook cannot start": `chmod -x` the hook, `sh -c` of the settings command exits 126.
- **Generator or installer under isolation** (`npx`, `uvx`): HOME, every `XDG_*`, `npm_config_cache`, `UV_CACHE_DIR`,
  `UV_TOOL_DIR` in the scratch folder; `env -i HOME=<h> PATH=<python 3.11+ dir>:/opt/homebrew/bin:/usr/bin:/bin ...` keeps
  tokens out (without Python on PATH uv downloads one). List files before and after; a second plain run (HOME and XDG only)
  shows what the user's own run writes. The npm package ships no `docs/`: curl from the tag.
