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
- **Shell traps**: multi-step logic in a script under `.claude/comms/tmp/`; the guard refuses `find`/`ls` with `$HOME` inline.
  An evidence script must never `cat "$RAW"` into a file that is also `$RAW` (filled 300 GB). `grep` here has no `-P`: print
  context with `python3 -I`.
- **MCP server proof without Claude**: start the built jar with `--server.port=<free>` (port from a `python3 -I` bind to 0;
  never the shared default), curl `initialize` (read `Mcp-Session-Id`), `notifications/initialized`, then `tools/list` with the
  header; answers are JSON or `data:` SSE lines. Stop only the recorded pid.
- **Completeness and quote fidelity are separate checks**: a token inventory of every staged file (backtick spans, JSON keys and
  values, URLs, identifiers) diffed against the record, and a quote pass (strings of 12+ characters) against pack, plan, guide
  and fetched pages, with mutated-quote controls. A misspelling control must land in the staged copy AND the in-memory copy of
  the built tree, else the generic "staged differs from tree" row reports it and a missing token-specific check hides; assert
  each control by its own finding text. Spelling search tolerates hard-wrapped prose (needle spaces become `\s+`); anchor
  loose-variant regexes (they flag headings).
- **Insertion-only proof** for long one-line rows: old tokens must be a greedy subsequence of the new tokens (difflib reports
  "replace" for insertions). **Universal claims**: `git grep` a counterexample on the recorded tag plus a control that must hit.
- **Hooks without Claude**: run the staged PreToolUse hook as settings name it, from another folder, with only
  `CLAUDE_PROJECT_DIR` set and a one-line JSON payload on stdin. Plant the red test as an untracked file so `git reset --hard &&
  git clean -fdx` removes it. "Hook cannot start": `chmod -x` the hook, `sh -c` of the settings command exits 126.
- **Rehearsal-row dry-runs**: use a clone made with the row's own recipe from a source repo that really has the branch name
  (`git clone --branch solution/level-3`; a stand-in on `main` gives a false literal; label stand-in files "not a Claude
  result"). Run the row to the NEXT step: `git reset --hard <ready>` drops a staged change (use `--soft`), undo a control commit
  before the next variant, restore the clone's `origin` url after `reset --hard && clean -fdx`. A local bare remote plus a by-hand
  `git push` proves "refs unchanged" can fail.
- **Generator or installer under isolation** (`npx`, `uvx`): HOME, every `XDG_*`, `npm_config_cache`, `UV_CACHE_DIR`,
  `UV_TOOL_DIR` in the scratch folder; `env -i HOME=<h> PATH=<python 3.11+ dir>:/opt/homebrew/bin:/usr/bin:/bin ...` keeps
  tokens out (without Python on PATH uv downloads one). List files before and after, summarize by area. A second plain run
  (HOME and XDG only) shows what the user's own run writes (npm `$HOME/.npm`, `$XDG_CONFIG_HOME/openspec/config.json` unless
  `OPENSPEC_TELEMETRY=0`, `uv tool install` into `$XDG_DATA_HOME/uv/tools`). The npm package ships no `docs/`: curl from the tag.
- **Evidence scripts**: `mask` BEFORE any `cut`; evidence must not match its own greps (build leak and branch-name patterns
  from split strings, print counts for controls). Plain `git status --short` folds untracked folders: count with
  `--untracked-files=all`. A regenerated evidence log may differ from the QA hash when inputs moved: keep a pre-edit copy.
