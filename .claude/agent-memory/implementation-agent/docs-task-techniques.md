---
name: docs-task-techniques
description: Techniques for README and architecture doc tasks whose claims a QA census checks (live proofs, two-way scripts, traps)
metadata:
  type: feedback
---

For a prose task the tests are a claim census plus gate greps. What held:

- **Two-way scripts with a negative control.** Parse the `@RestController` sources (class `@RequestMapping` plus method
  mapping) and the README list, compare both ways, then re-run on a README copy with one line removed and expect a
  difference. Same for `ArchitectureTest` constants and `classNames(...)` suffix lists against the architecture doc
  (`python3 -I`, parametrize the doc path). Quoted enum names such as `HALF_EVEN` look like rule constants: allowlist them. A
  count-sentence census reads the seed SQL (parse the INSERT VALUES rows) with copies altered one number at a time.
- **Fake an "older database" for a reset claim.** Start the jar with `--spring.flyway.locations=classpath:db/migration` on its
  own database, add a book through the API, stop it, start again with the default locations: the seed fails on the FK to
  `book_author`, exit 1.
- **Do not write "no volume" about the compose reset.** The `postgres:18.6` image declares an anonymous volume at
  `/var/lib/postgresql`; say "removes the database container". Check with `docker inspect --format '{{json .Config.Volumes}}'`.
- **Background jars and Vite:** start each from a script file with `run_in_background`, stop by the PID that
  `lsof -nP -iTCP:<port> -sTCP:LISTEN` shows; the exit-143 notifications that follow are the kill, not a failure.
- **`rg` works in the Bash tool, not in script files.** Run gate greps as separate Bash calls with a positive control file in
  the scratchpad (exit 0) next to each empty result (exit 1). `rg -ni 'a|b'`, never `-E`. Its exit 1 shows only as "no
  output": add `--stats` so "0 matches, N files searched" is the quotable proof.
- **A rate limit behind the offline check is not reachable offline.** To prove a 429 live, start a second instance with a
  made-up key and `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.1:<port>` pointing at a local stub that answers a minimal
  Messages JSON; the stub's request count shows rejected calls never reach the model.
- **Port 8080 may belong to someone else:** preflight with `lsof`, never kill it, run the jar with `--server.port=<free>`.
- **Worktree Bash guard refuses paths with backslash-escaped spaces.** Use relative paths from the worktree root
  (`python3 -I <abs scratchpad script> . README.md http`, `bash .claude/comms/tmp/x.sh`); the scratchpad and Read/Edit/Write
  absolute paths are fine.
- Keep a `README.md` under a line limit by unwrapping lines and inlining small code blocks, not by dropping sections.
