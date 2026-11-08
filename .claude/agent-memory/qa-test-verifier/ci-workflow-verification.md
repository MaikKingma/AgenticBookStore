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

Related: [[final-sweep-checks]], [[shell-traps]]
