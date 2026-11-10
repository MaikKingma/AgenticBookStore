---
name: consistency-check-techniques
description: Zero-model proof techniques for staged config and generated trees (globs, token resolution on a sha, sandboxes, hooks, installer isolation, Spec Kit init) and their traps
metadata:
  type: reference
---

Techniques that held for the spec 10 consistency-check tasks (D14, no Claude runs):

- **Glob test for rule `paths`**: never `fnmatch` (its `*` crosses `/`). Cross-check a small regex translator,
  `pathlib.PurePosixPath.full_match` (Python 3.13) and `git ls-files ':(glob)<pattern>'` on the sandbox, plus widened
  and narrowed negative controls per rule. Test-tree folders named like the layer (`archfixtures/domain`) are the trap
  a missing `main` anchor would match.
- **Resolve tokens on a recorded sha**: `git grep -n -e <token> <sha> -- <paths>` (read-only). A pattern starting with
  `-` needs `-e`; `git grep -F ""` is an error (use `git ls-tree -r --name-only`). macOS `git grep -E` has no `\w`/`\b`
  (use `-P`). `origin/factory` can move mid-task (shared clone): pin the sha in every script.
- **Maven goal resolves without starting the app**: `./mvnw -pl <module> spring-boot:help -Dgoal=run`.
- **Sandbox**: build with the T1 scripts (`simulate-level-0.sh`, `build-level.sh`), `TMPDIR` and
  `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir` in the scratchpad, `mktemp -d "$TMPDIR/sbx.XXXXXX"`. Full verify ~30 s warm.
- **Shell traps**: paths with spaces double-quoted (backslash escapes make the guard refuse); multi-step logic in a script
  under `.claude/comms/tmp/`; the guard refuses `find`/`ls` with `$HOME` inline. An evidence script must never
  `cat "$RAW"` into a file that is also `$RAW` (filled 300 GB in seconds). macOS `sed -i` needs `-i ''` (`-i.bak` leaves
  a `.bak`). `grep` here (ugrep) has no `-P` and rejects `.{0,200}`: print context with `python3 -I`.
- **MCP server proof without Claude**: start the built jar with `--server.port=<free>` (port from a `python3 -I` bind to
  0; never the default, it is shared), curl `initialize` (read `Mcp-Session-Id`), `notifications/initialized`, then
  `tools/list` with the header; STREAMABLE answers are JSON or `data:` SSE lines. Stop only the recorded pid.
- **Public sources for external tokens**: marketplace JSON, formulae.brew.sh API, AUR RPC, upstream wrapper script at a
  tag and Claude Code docs pages (append `.md`, quote by line; curly apostrophes become `?`) settle plugin id, formula,
  package and Java floor without installing anything. Check the schemastore schema too (it may lack a field).
- **Completeness and quote fidelity are separate checks**: a token inventory of every staged file (backtick spans, JSON
  keys and values, URLs, identifiers) diffed against the record, and a quote pass (strings of 12+ characters) against
  pack, plan, guide and fetched pages, with mutated-quote controls.
- **Token inventory controls that mean something**: a misspelling control must land in the staged copy AND the in-memory
  copy of the built tree (overlay dict), else the generic "staged differs from tree" row reports it and a missing
  token-specific check hides; assert each control by its own finding text. Spelling search tolerates hard-wrapped prose
  (spaces in the needle become `\s+`). Loose-variant regexes flag headings ("New use case"): anchor them.
- **Insertion-only proof** for long one-line rows: old tokens must be a greedy subsequence of the new tokens (difflib
  reports "replace" for insertions); keep a mutant that changes one number; wrap old words instead of rewording.
- **Universal and descriptive claims**: `git grep` each for a counterexample on the recorded tag plus a control that must
  hit. Split the file into units (sentences, cells), give each a source tag, resolve tags with a script; prefer
  prescriptive wording and name the real exceptions.
- **Hooks without Claude**: run the staged PreToolUse hook as settings name it, from another folder, with only
  `CLAUDE_PROJECT_DIR` set and a one-line JSON payload on stdin; a real red build takes ~25 s. Plant the red test as an
  untracked file so `git reset --hard && git clean -fdx` removes it. A fresh clone of a sandbox replays row setup; a
  local bare remote plus a by-hand `git push` proves "refs unchanged" can fail.
- **Rehearsal-row dry-run traps**: dry-run each row in a clone made with the row's own recipe from a source repo that
  really has the branch name (`git clone --branch solution/level-3`; a stand-in on `main` gives a false literal, and for
  a Spec Kit row use `git branch start/level-4-speckit HEAD`; label stand-in files "not a Claude result"). Run the row to
  the NEXT step: `git reset --hard <ready>` drops a staged change (use `--soft`), undo a control commit before the next
  variant, and restore the clone's `origin` url after `reset --hard && clean -fdx`. "Hook cannot start" is observable:
  `chmod -x` the hook, `sh -c` of the settings command exits 126, status shows a mode change `git add -A` would stage.
  Cite a hook-failure row from the docs block that prints `failed; blocking because onFailure is "block"`.
- When a level file merges a pack block into an earlier staged file, build the expected text from the earlier file plus
  the pack lines and compare bytes, one mutant per key and list; match pack headings by exact line.
- **Generator or installer under isolation** (`npx`, `uvx`): HOME, every `XDG_*`, `npm_config_cache`, `UV_CACHE_DIR`,
  `UV_TOOL_DIR` in the scratch folder; `env -i HOME=<h> PATH=<python 3.11+ dir>:/opt/homebrew/bin:/usr/bin:/bin ...`
  keeps tokens out (without Python on PATH uv downloads one). List files before and after, summarize by area (a cache
  holds thousands). A second plain run (HOME and XDG only) shows what the user's own run writes: npm `$HOME/.npm`,
  OpenSpec `$XDG_CONFIG_HOME/openspec/config.json` unless `OPENSPEC_TELEMETRY=0`, `uv tool install` writes
  `$XDG_DATA_HOME/uv/tools/<name>`, `$XDG_DATA_HOME/../bin`, `$XDG_CACHE_HOME/uv` (`uv tool uninstall` keeps the cache;
  `uv cache clean <pkg>` removes only that package). The npm package ships no `docs/`: curl the file from the tag.
  OpenSpec 1.16.0: bare `validate --strict` exits 1 without a terminal; `archive` writes a placeholder Purpose and
  `validate --all --strict` then fails; staged `.gitkeep` encodes as `dot-gitkeep`.
- **Spec Kit 1.3.1** (`uvx --from specify-cli==1.3.1 specify init ... < /dev/null`, network only for PyPI): init is not
  byte-deterministic (`installed_at`/`updated_at` in `.specify/integrations/*.manifest.json` and
  `.specify/workflows/workflow-registry.json`, which lacks a final newline). It needs a `claude` program on PATH or
  `--ignore-agent-tools` (else exit 1, nothing written). `/speckit-constitution` amends an existing constitution, so a
  fresh-vs-staged comparison first restores the template (`git show <start>:.specify/templates/constitution-template.md`).
  Hand-written constitution check: `python3 -I` with a trace TSV (fragment, source file, line, source fragment) plus
  mutants (placeholder back, key term removed, forbidden word allowed, untraced rule, shifted line), red first on the
  template. Generated `tasks-template.md` calls tests OPTIONAL; the Level 3 allow list has no Spec Kit script entry.
- **Staging a generated tree**: commit it in sandbox 1, stage from `git diff-tree` of that commit, prove with a fresh
  sandbox 2 whose tree id equals sandbox 1's.
- **Evidence scripts**: `mask` BEFORE any `cut`; evidence must not match its own greps (build leak and branch-name
  patterns from split strings, describe them in prose, print counts for controls); a leak pattern quoted in a record
  makes the leak grep hit the record. ASCII check ignoring tabs: `LC_ALL=C grep -E '[^[:print:][:space:]]'`; emoji in
  `git grep` output through `sed 's/[^ -~]/?/g'`. Plain `git status --short` folds untracked folders: count with
  `--untracked-files=all`. A regenerated evidence log may differ from the QA hash when inputs moved: keep a pre-edit copy.
