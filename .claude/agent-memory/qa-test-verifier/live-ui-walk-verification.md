---
name: live-ui-walk-verification
description: Re-verifying a recorded live UI walk-through (throwaway DB, jar, dev proxy, playwright-cli walk, GQ8-style layout checks, screenshots) and the traps that skew its evidence
metadata:
  type: feedback
---

Rule: reproduce a recorded walk with your own data and scripts, seeded to the same figures, and compare value by value; never cite the implementer's JSON as evidence.

**Why:** seeding the same books, prices and stock through the REST commands made every money string, 409 body and order total byte-comparable with the record, while ids and times differ by design.
**How to apply:**
- One control script with subcommands (`docker-state`, `ports`, `pg-start`, `jar-start`, `jar-stop`, `pg-stop`) writes a dated SP file per call; take `docker-state`/`ports` before and after the session. Use a different throwaway port than the implementer.
- playwright-cli: one `async page => {...}` file per phase via `run-code --filename`, `--raw` redirected to SP; attach `console`/`pageerror`/`response`/`request` listeners inside and `off` them in `finally`. Fill number inputs with `locator.fill` (fires `onChange`). A `window.__marker` proves "no reload".
- Favicon 404s fire no page `response` event; attribute a bare "Failed to load resource: 404" with `m.location().url`. A new context in the same browser does not re-request the favicon, so "first load of a fresh session" needs a brand-new `-s=<name>` session.
- Blur-then-Tab starts from the last focused element (after a form submit the first stop is the next link, not the wordmark); say which start point a "first Tab stop" figure used.
- Table-squeeze check at phone width: `.table-scroll` `clientWidth == scrollWidth` plus `th` heights several line-heights tall proves words break instead of scrolling, even when the document `scrollWidth` passes.
- Negative control for a CSS fix: `git archive <pr-base>` into the scratchpad, `./mvnw -DskipTests package` there (8 s with Node cached), copy the jar out, run it on a second port against the SAME throwaway DB (`--server.port=8081`), and drive both builds with one script that takes the origin from `page.url().split('/shop/')[0]` (`URL` is not defined inside `run-code`). One brand-new `-s=<name>` session per build keeps the favicon check a true first load.
- `playwright-cli` drops `.playwright-cli/` (page snapshots, console log) into the cwd, here the repo root, as untracked files that break a before/after porcelain snapshot; delete them and `rmdir` before the final snapshot.
- Before calling a leftover process yours, read `ps -o lstart,ppid`; an MCP server started weeks ago is not.
- Warning-inventory diffs against an earlier referee log: normalise Tomcat thread names (`[1-auto-1-exec-N]`) as well as timestamps and PIDs, else a reshuffle shows up as paired add/remove lines; then group the real additions by logger to attribute them (merged-in spec vs this task).
Related: [[readme-and-live-jar-probes]], [[ci-workflow-verification]], [[shell-traps]]
