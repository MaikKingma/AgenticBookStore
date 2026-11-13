---
name: docs-task-techniques
description: README and architecture-doc tasks whose claims a QA census checks (two-way scripts, live proofs, rg traps) and replaying .http collection files live without an IDE
applies-to:
  - "README.md"
  - docs/**
  - http/*.http
metadata:
  type: reference
---
For a prose task the tests are a claim census plus gate greps. Guide-page traps live in [[docusaurus-guide-traps]].

- **Two-way scripts with a negative control**: parse `@RestController` sources (class plus method mapping) and the README list,
  compare both ways, re-run on a README copy with one line removed and expect a difference. Same for `ArchitectureTest` constants
  and `classNames(...)` suffix lists against the architecture doc (`python3 -I`, parametrize the doc path). Allowlist quoted enum
  names such as `HALF_EVEN`. A count-sentence census parses the seed SQL INSERT rows, with copies altered one number at a time.
- **Fake an "older database" for a reset claim**: start the jar with `--spring.flyway.locations=classpath:db/migration` on its own
  database, add a book, stop, start with default locations: the seed fails on the FK to `book_author`, exit 1.
- **Never write "no volume" about the compose reset**: `postgres:18.6` declares an anonymous volume at `/var/lib/postgresql`; say
  "removes the database container" (`docker inspect --format '{{json .Config.Volumes}}'`).
- **`rg` works in the Bash tool, not script files.** Run gate greps as separate calls with a positive control file in the
  scratchpad next to each empty result. `rg -ni 'a|b'`, never `-E`; add `--stats` so "0 matches, N files searched" is quotable.
- **A rate limit behind the offline check**: prove a 429 live with a made-up key and `SPRING_AI_ANTHROPIC_BASE_URL=http://127.0.0.1:<port>`
  on a local stub; its request count shows rejected calls never reach the model.
- Port 8080 may belong to someone else: preflight with `lsof` (`/usr/sbin/lsof` in a pinned PATH), never kill; use
  `--server.port=<free>`. `guide/node_modules` has no axe-core (the scratchpad rig carries it).
- Worktree Bash guard refuses backslash-escaped spaces: use relative paths from the worktree root. Keep a `README.md` under a
  line limit by unwrapping lines. A claims table is cheapest from a node script emitting a TSV for a `grep -nF` loop.

## .http replay (no IDE)
- Driver: python3 `-I` splits the file's bytes at `###`, substitutes `{{var}}` and `{{$uuid}}`, sends with urllib, runs each
  block's `> {% ... %}` handler through `node -e` with a shim (`client.global.set/get`, `client.test`, `client.assert`,
  `response.status/body`). Flag a capture of `undefined`, unresolved variables, a missing status line as FAIL.
- One line `# Expected status: 202 (distributor running); 503 (distributor stopped).` per block; a free-text tail breaks it.
- Run-mode blocks need three modes, each with a fresh container: distributor up, jar killed, and
  `--distributor.chaos.failure-rate=1.0`.
- Seeded state: new books start with a stock row at 0 (assert low-stock membership by id, never a count); a dip fires the
  automatic restock only when the signal is false, and `receive` resets it.
- Offline assistant path: `env -u BOOKSTORE_AI_API_KEY` (503 offline, 400 before offline, 429 only with a key).
