---
name: readme-and-live-jar-probes
description: Verifying a README plus packaging task - fact checks that catch overclaims, a budgeted three-run jar start script, a piped STDIO driver, claude mcp add checks without running it
metadata:
  type: feedback
---

Rule: check every README sentence that makes a claim about data or behaviour against the code AND the data files, not only
commands and paths. Prose overclaims ("do not match real editions", "never") survive command checks.

**Why:** a README stated that no EAN matched a real edition; one seed EAN (the shared Pride and Prejudice ISBN) is a real
Penguin edition. Found by `grep -o -E '"ean": "978[0-8]...'` on the seed plus a WebFetch of openlibrary `/isbn/<ean>.json`.
**How to apply:**
- Start proof: one script in `.claude/comms/tmp/` run through `with-budget.sh 120`: per run preflight `lsof`, a python
  `time.time()` stamp before `java -jar ... &`, a 50 ms curl poll of `/actuator/health`, the `Started ... (process running for Y)`
  line, `lsof` LISTEN, `/actuator/info` 200 and `/actuator/env` 404, curl MCP `initialize` with
  `Accept: application/json, text/event-stream`, `kill`, `wait` (143), teardown `lsof` exit 1. macOS `date` has no `%N`.
- `spring-boot:run` forks: kill the mvn PID, then re-check the port and kill a leftover java PID only if you started it.
- Real-jar STDIO: a `python3 -I` driver with a reader thread, stdin held open until both replies arrive, then close stdin
  (the server exits by itself); stdout and stderr to separate files; use a fresh subdirectory of the scratchpad as cwd so an
  implementer's older log file cannot pass for yours. Check stdout lines strictly (no empty lines, ids exactly 1 and 2).
- `claude mcp add` is never run; compare README lines to `claude mcp add --help` (usage and the two example shapes) and prove
  the URL by curl and the jar arguments by the piped run.
- Rework re-check with no saved copy of the first-round file (untracked file, only a hash): rebuild it by swapping the reworked
  lines for the old text (known from the earlier verdict quote, tried over every line-wrap) and compare sha256; a match proves
  the rest of the file is byte-identical. Save a copy next time (`cp` to the scratchpad) along with the hash.
- `localhost` tries `::1` first; a server bound to 127.0.0.1 only answers after curl falls back (`curl -v` shows both Trying lines).
- A literal grep over a path that does not exist yet (`http/distributor.http` before T8) exits 2 with a warning and prints
  no matches: say so rather than calling it a clean empty result.

- `.http` request collections: a `python3 -I` driver parses the file's own bytes (split on `###`, comments, request line, headers,
  body by line range, emulate the `client.global.set(...)` handler), sends with http.client, and oracles every body from the seed
  JSON plus today's Amsterdam date. Prove it bites with 4-5 mutated COPIES in the scratchpad (swap an EAN against its comment,
  quantity 0 to 1, drop the handler, change a query); never edit the repo file. A comment-vs-seed rule must only fire when the
  comment actually names a title (a first run FAILed on a comment that makes no title claim: driver defect, keep both outputs).
  A mutant with an unresolved `{{var}}` makes Tomcat log "Invalid character found in the request target": attribute it.

- Headless Claude Code proxy (never `claude mcp add`): run `claude -p ... --mcp-config <scratch json> --strict-mcp-config
  --allowedTools mcp__<server>__<tool> --output-format stream-json --verbose` from a script that `cd`s to the scratchpad (the
  project's CLAUDE.md and settings stay out). Judge the `system/init` event (`mcp_servers[].status`, `tools`, `apiKeySource`),
  then the `tool_use` and its `tool_result`. A 429 `model_requires_usage_credits` on the default model kills the turn but
  init still proves the connection; a retry with only `--model <alias>` gets the tool call (report it as a deviation).
  The server log line `Client initialize request - Protocol: ... name=claude-code, version=...` proves the negotiated version.
- After a Claude Code HTTP session, SIGTERM on a Boot MCP server hangs 30 s in graceful shutdown (open SSE GETs) while the
  port is already free: wait for the PID (up to 35 s) before the next jar, never kill -9 it as "stuck".
- `clean test` order runs delete `target/<app>.jar`: rebuild (`clean verify`) before live runs from the jar.

Related: [[stdio-shared-fork-probes]], [[shell-traps]]
