---
name: ci-workflow-verification
description: Verifying GitHub Actions workflow tasks (ci-performance style) with read-only gh - techniques and traps
metadata:
  type: feedback
---

Rule: for CI-config tasks the evidence is real runs plus the file; script every gh call and recompute baselines yourself.

**Why:** a ledger/plan figure ("baseline median 52 s") turned out to be the minimum of the five baseline runs (median 58 s); copying it would have carried a wrong gate number forward.
**How to apply:**
- Put gh/jq/python in a script under `.claude/comms/tmp/qa-<id>-cip-<slug>.sh`; in `gh --jq` object construction write `.databaseId`, a bare `databaseId` fails with "function not defined".
- Cancellation proof: `gh api repos/<r>/check-runs/<job_id>/annotations` holds "Canceling since a higher priority waiting request for <workflow>-<pr> exists"; the group name in it proves the group expression for PR runs.
- `gh run view <id> --json jobs` gives step startedAt/completedAt; `Image:`/`Version:` lines are in the "Set up job" log (baseline job logs may label the step `UNKNOWN STEP`).
- Tags: GitHub docs sentence "the workflow won't run for events affecting the undefined Git ref"; glob cheat sheet says `*` does not match `/` (docs page is long, WebFetch it with offset).
- Trap: the Bash safety check refuses `rg -F` of strings containing `${{` (misreads as rm risk). Do not script around it; read-compare via the ruby YAML dump and say so in the verdict.
- PR-run log line `HEAD is now at <sha> Merge <head> into <base>` proves the merged tree.
- Job summaries (`$GITHUB_STEP_SUMMARY`, action-junit-report `job_summary`) are NOT rendered to logged-out viewers, even on public repos: the run page shows only status, annotations and artifacts; `runs/<r>/jobs/<j>/summary_raw` answers a 9-byte 404; check-run `output.summary` is null. Positive control: any public run whose workflow sets `job_summary_text` (for example mikepenz/action-junit-report CI). A plan that demands anonymous summary evidence is BLOCKED; ask for a signed-in human capture. A signed-in path via the `claude-in-chrome` skill only works when the extension is connected in that session; the Skill call says so at once ("not set up"). Do not swap in WebFetch, which is anonymous. Return BLOCKED and quote the message. Precedent: the orchestrator then narrowed the criterion (summary content not gated); the verdict must still say the summary is UNVERIFIED by QA and that nothing was inferred from the action's log lines.
- Trap: `rg -c` omits files with zero matches, so a "count is 0" check prints nothing; add `--include-zero` to quote the 0.
- The annotations API and the rendered page show action-junit-report failures (title `Class.method`, message `expected ... but was ...`); `gh run download -n <name> -D <fresh dir>` plus an ElementTree sum of `tests`/`failures` proves artifact completeness per module.
- playwright-cli: the Bash guard refuses `playwright-cli eval`; use `run-code --filename=<js in .claude/comms/tmp>` and redirect `--raw` output to the scratchpad. It writes `.playwright-cli/` into the cwd (the worktree); move it to the scratchpad before finishing.

- Log-count traps: a `grep -c` on a string that is also in a `run:` script (for example `postgres pull start`) counts the echoed script source in the "Run" group header; anchor on the Actions timestamp (`Z <string> 20`). Run every mechanism regex on a baseline log first as a positive control (a "0 pulls" claim means nothing if the pattern never matches).
- Container-start metric: find the class that starts the container as the `Starting <Class> using Java` line before the first `Creating container for image`, then its Surefire `Time elapsed: X s -- in ...<Class>`; compute baseline and candidate with one script and compare per-run ranges, not only medians. Image `Version:` sits on the line after `Image:` in the "Set up job" log (step label can be `UNKNOWN STEP` in old logs).
- Warning inventory diff: strip the log timestamp and PID before comparing `[WARNING]|WARNING: | WARN ` lines, else every line looks added and removed.
- `python3 -I` ignores a `#!/usr/bin/env python3 -I` shebang; run `python3 -I script.py`. The Write tool, not a Bash heredoc, for script content.
- A stamp written into `updated:` ahead of `date -u` is a trap; take the time from `date -u` right before each bump.
- Cold Maven cache scan: setup-java prints `maven cache is not found` (and `maven-wrapper cache is not found`); a hit is `Cache restored from key: setup-java-Linux-x64-maven-<hash>`. A grep for `Cache not found` never matches and reports 0 cold runs; check the miss pattern on the spec branch's first push run first.
- Referee on committed bytes without a clone or worktree: in a script, `git -C <wt> archive <sha> | tar -x -C <SP>/<prefix>-tree`, then `cd` there and run the budgeted `./mvnw`. `find -type f` counts one less than `git ls-tree -r` because `agent-os/product` is a symlink (mode 120000), not a missing file.
- Timeout kinds via the annotations API: job timeout gives "The job has exceeded the maximum execution time of Nm0s" (job `cancelled`, `!cancelled()`/`failure()` steps skipped); step timeout gives "The action '<step>' has timed out after N minutes." (step and job `failure`). A hung test writes no `TEST-*.xml`, so the junit report still shows all-passed; the annotation is the only pointer.
- Probe-pair proof: `git diff <base> <probe>` in full (not only --stat), parent via `git log --format="%H %P"`, and the PR log line `HEAD is now at <x> Merge <probe> into <base>`. Compare SHAs in someone else's evidence character by character (a doubled digit slipped through once).
- Trap: `gh api repos/<r>/actions/jobs/<id>/logs > f` refuses Maven-coloured logs ("the response contains terminal escape sequences") and leaves a 0-byte file. Add `--allow-escape-sequences` and `wc -l` the file before grepping it.
- Trap: `gh cache list --limit 100` returning exactly 100 entries means the list was cut off, so a "no probe cache" claim needs `--limit 1000 --json ref --jq` with a `length` total and a positive-control ref pattern.
- Byte guards on a workflow (no `continue-on-error`/`|| true`/`-fn`, pins, ASCII): `python3 -I` on `git show <sha>:<file> > <SP>/x.yml`, plus one control copy per injected token written by a second script. Every guard must bite on its own control.

Related: [[final-sweep-checks]], [[shell-traps]]
