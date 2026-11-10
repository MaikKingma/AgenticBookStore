---
name: consistency-check-techniques
description: Zero-model proof techniques for staged config files (glob tests, token resolution on a factory sha, sandbox builds) and their traps
metadata:
  type: reference
---

Techniques that held for the spec 10 consistency-check tasks (D14, no Claude runs):

- **Glob test for rule `paths`**: never use `fnmatch` (its `*` crosses `/`). Cross-check three implementations: a small
  regex translator, `pathlib.PurePosixPath.full_match` (Python 3.13) and `git ls-files ':(glob)<pattern>'` on the
  sandbox. Add widened and narrowed negative controls per rule. Test-tree folders named like the layer
  (`archfixtures/domain`) are the trap that a missing `main` anchor would match.
- **Resolve tokens on a recorded sha**: `git grep -n -e <token> <sha> -- <paths>` (read-only). A pattern that starts
  with `-` (for example `-pl bookstore`) needs `-e`, else git reports an unknown switch. `git grep -F ""` is an error:
  list files with `git ls-tree -r --name-only` instead.
- **Prove a Maven goal resolves without starting the app**: `./mvnw -pl <module> spring-boot:help -Dgoal=run`.
- **Sandbox**: build with the T1 scripts from the worktree (`simulate-level-0.sh`, `build-level.sh`), `TMPDIR` and
  `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir` in the scratchpad, `mktemp -d "$TMPDIR/sbx.XXXXXX"`. Full verify of the
  repository took about 30 seconds with a warm `~/.m2` and Docker running.
- Bash tool in a worktree agent: paths with spaces must be double-quoted (backslash escapes make the guard refuse the
  command); multi-step logic goes into a script under `.claude/comms/tmp/`.
- **MCP server proof without Claude**: start the built jar with `--server.port=<free>` (port from `python3 -I` bind to
  0, never the app's default), curl `initialize` (read the `Mcp-Session-Id` header), `notifications/initialized`, then
  `tools/list` with the header; STREAMABLE answers arrive as JSON or as `data:` SSE lines, strip `data:`. Never start the
  jar without the port flag, its default port is the shared one. Stop only the recorded pid.
- **Public sources for external tokens**: marketplace JSON, formulae.brew.sh API, AUR RPC, upstream wrapper script at a
  tag and the Claude Code docs pages are all readable with curl (tags stripped with sed); they settle plugin id,
  formula, package and Java floor without installing anything.
- Script traps: an evidence script must never `cat "$RAW"` into a file that is also `$RAW` (it filled 300 GB in
  seconds; delete the file at once); the worktree guard refuses `find`/`ls` with `$HOME` on the command line, put it in
  a script file; macOS `sed -i` needs `-i ''`, and `-i.bak` drops a stray `.bak` into the worktree; a leak-grep pattern
  quoted verbatim inside a record makes the leak grep hit the record.
- **Completeness and quote fidelity are separate checks**: a token inventory of every staged file (backtick spans, JSON
  keys and values, URLs, identifiers) diffed against the record, and a quote pass (quoted strings of 12+ characters)
  against pack, plan, guide and fetched pages; add mutated-quote controls.
- The `grep` of this shell (ugrep) has no `-P` and rejects `.{0,200}` context patterns; print context with `python3 -I`.
- `origin/factory` can move while a task runs (shared clone): pin the sha the sandbox was built from in every script.
- **Insertion-only proof** for long one-line rows: old tokens must be a greedy subsequence of the new tokens (difflib
  reports "replace" for insertions); keep a mutant that changes one number; wrap old words instead of rewording.
- **Public docs as evidence**: append `.md` to a docs URL (`/docs/en/hooks.md`) for the markdown form and quote by line
  number; curly apostrophes become `?` in ASCII evidence. Check the schemastore schema too (it may lack a field).
- **Universal and descriptive claims**: `git grep` each for a counterexample on the recorded tag plus a control that
  must hit (macOS `git grep -E` has no `\w`/`\b`; use `-P`). Split the file into units (sentences, cells), give every
  unit a source tag and resolve each tag with a script; prefer prescriptive wording and name the real exceptions.
- **Token inventory with controls that mean something**: a misspelling control must land in the staged copy AND in the
  in-memory copy of the built tree (overlay dict), else the generic "staged differs from tree" row reports it and a
  missing token-specific check hides; assert each control is reported by its own finding text. Spelling search must
  tolerate hard-wrapped pack prose (turn spaces in the needle into `\s+`, map the match start to a line) or `Git for
  Windows (Git Bash)` is "missing". Loose-variant regexes flag headings ("New use case") as misspellings: anchor them.
- **Hooks without Claude**: run the staged PreToolUse hook as settings name it, from another folder, with only
  `CLAUDE_PROJECT_DIR` set and a one-line JSON payload on stdin; a real red build takes ~25 s and gives the true stderr.
  Plant the red test as an untracked file so `git reset --hard && git clean -fdx` removes it. Fresh clone of a sandbox
  replays row setup commands; a local bare remote plus a by-hand `git push` proves "refs unchanged" can fail.
- **Rehearsal-row dry-run traps** (QA FAIL r0 of spec 10 T10): dry-run each row in a clone made with the row's own
  recipe from a source repo that really has the branch name (`git clone --branch solution/level-3` prints that name in
  `git status -sb`; a stand-in on `main` gives a false literal). Run the row to the NEXT step: `git reset --hard
  <ready>` drops a staged change (use `--soft`), a control commit must be undone before the next variant, and the
  clone's `origin` keeps pointing at the scratch bare repo after `reset --hard && clean -fdx` (record and restore the
  url). Observable "hook cannot start": `chmod -x` the hook, `sh -c` of the settings command exits 126 `Permission
  denied`, status shows a mode change that `git add -A` would stage, `git reset --hard` restores the bit. Cite a
  hook-failure row from the docs block that prints the notice (`failed; blocking because onFailure is "block"`).
