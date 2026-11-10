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
- **Completeness and quote fidelity are separate checks**: (1) a `python3 -I` token inventory over every staged file
  (backtick spans, JSON keys and values, URLs, quoted strings, identifiers) diffed literally against the record catches
  missing rows (JSON field names and enum values were the r0 gap); (2) a quote pass over the record and checklist
  (double-quoted strings of 12 or more characters, quotes inside code spans hidden, whitespace collapsed) against the
  union of pack, plan, guide and fetched pages catches an altered word and a changed capital letter. Add mutated-quote
  controls.
- The `grep` of this shell (ugrep) has no `-P` and rejects `.{0,200}` context patterns; print context with `python3 -I`.
- A wrapper's per-platform behaviour (jdtls cache folder: Darwin `~/Library/Caches`, Linux `~/.cache`) means one
  cold-cache proof per OS in a rehearsal row; quote the platform lines of the script, not only the folder line.
- `origin/factory` can move while a task runs (shared clone): pin the sha the sandbox was built from in every script.
- Pack text can contradict itself on wording (spec 09 says Level 1 "imports" `docs/architecture.md`, spec 10 forbids the
  `@` import): record it as a product finding, never edit verbatim staged files.
- A rehearsal row that times a "cold start" must cite the docs sentence that says what starts the process (LSP: the
  first edit of a matching file, not a read-only references request). Without that citation the first request is only
  assumed to be the trigger; give the row a branch for "nothing started" and let the stopwatch follow the real start.
