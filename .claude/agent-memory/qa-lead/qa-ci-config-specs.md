---
name: qa-ci-config-specs
description: QA strategy and evidence traps for CI-config specs whose evidence is real GitHub Actions runs (unreadable job summaries, signed-in channel at Gate 1, baselines recomputed by script, same-suite controls, timing noise, log-grep anchors, warning greps, hang and timeout paths, red/green probe pairs, Linux-only pipe truncation by process.exit); read before the strategy or final brief of any workflow or CI spec
metadata:
  type: feedback
---

Split out of [[qa-directive-traps]] (item 24 there). Write these into the directive of any task or spec whose evidence is a GitHub Actions run.

Job summaries are not readable from the CLI. `gh run view --log` lacks them, and `.../jobs/<id>/summary_raw` answers 404. There is no REST endpoint for them. They are also hidden from logged-out viewers in general: a positive control on the action's own public run showed none. So anonymous playwright-cli rendering cannot work (my ci-performance G1 premise was false and cost two BLOCKED rounds).
   Any criterion on summary content needs a signed-in channel decided at Gate 1, before the cycle starts: either the owner connects Claude in Chrome (`/chrome`), or a human capture is planned (copied text plus a screenshot that shows the URL). Add `gh api .../check-runs/<job_id>/annotations` for per-test messages. If no channel is certain, offer at Gate 1 the agent-checkable form the orchestrator chose later (ci-performance A3): gate on the action's per-module log lines equal to the console totals, the annotations API, the artifact and the red job; mark the summary unverified.
   These are not substitutes for summary content (they are only for the narrowed form):
   - the action's step log lines or its `summary` outputs printed to the log (an intermediate value, not the published summary);
   - a check run with `checks: write` (a different surface). Job-duration noise (verify 41 to 62 s) exceeds single-digit-second gains, so "faster" criteria gate on mechanism (log line counts per run) plus a low-noise per-run metric, with baselines recomputed by the same script. Pin checks resolve tags through `repos/<o>/<r>/commits/<tag>`, because `git/ref/tags` returns the tag object sha for annotated tags. A cancelled run is a concurrency cancel only if its annotation says so (else it may be manual).
   Never write a baseline number into a strategy or criterion from research prose: run the same-script computation first
   (ci-performance G3 quoted a minimum as the median). The harness refuses `rg -F` on `${{ }}` strings: direct byte-compares
   into a `python3 -I` script with a one-space negative control. The warning grep for CI logs must include Maven/JVM lines
   (`\[WARNING\]|WARNING: | WARN `) compared by count with a baseline job log, not only `##[warning]` and annotations.
   Ask for `--json jobs` evidence saved unfiltered (steps included), or step durations have no file behind them.
   Greps for a `run:` step's output must anchor on the Actions timestamp prefix (`Z <text>`): the step header echoes the script source, so a plain count is inflated. A "runs in the background" claim is proven by the step's run-JSON duration (0 s) against the work's own duration, not by reading `&`.
   Before fixing a baseline run set for a timing gate, check that the runs are the same suite (per-module `Tests run:` totals). In ci-performance, four different suites were mixed: a per-class metric survives that, but job and Maven totals are not like-for-like. Add a same-suite control run (the previous task's green run).
   Research counts of log events (cold caches, pulls) need the exact text the tool prints and a restatable run set: setup-java prints `maven cache is not found`, not `Cache not found`. A figure missing either is "not recomputed" and supports no before/after claim; check this at strategy time, not at qa-final.
   Hang path: a job timeout, a manual cancel and a supersede all make `cancelled()` true; only a step `timeout-minutes` below the job's ends as failure. Gate it on the step-timeout annotation plus the absence of the job-timeout one. A hung test writes no `TEST-*.xml`, so the junit report shows all passed: never gate a hang on the report. Red/green probe pairs: green differs from the gated head only by the probe test. A red recipe that lowers the job timeout for speed must never be re-run literally on the fix (job 4 < step 10 cancels by design): in the final brief, name the green-after run as the "must not reproduce" re-run and gate on `git diff --exit-code <green head> <tip> -- <workflow>`.
   Bookkeeping commits after a workflow task's verdict keep the evidence when three things hold: the diff lists only
   bookkeeping paths, the workflow blob is unchanged, and the new head's runs are green. A `pull_request` path filter is
   evaluated over the whole PR diff, so those commits re-trigger the workflow. A CI run stands in for the local gates
   only when the task changes no file those gates read; otherwise local gates stay mandatory (spec 11 T2).
   Linux-only red on a CI step that is green on macOS: suspect a Node CLI that calls `process.exit` right after
   `console.error`/`console.log`. Over a Linux pipe the queued tail is lost (exit status kept), macOS never loses it, so
   local loops and one green CI run prove nothing. Directive: reproduce in a Linux container on the CI Node version,
   N runs before and after (spec 11 T11: 23/200 vs 0/320), plus a witness on the real pipe that dies on a tail cut. A
   test-side fix reads the child's output through files (`stdio: ['ignore', fd, fd]`); the real fix is
   `process.exitCode = 1`, pinned by a static no-`process.exit(` test. A red Linux log missing an error line is no
   proof the error is absent.
