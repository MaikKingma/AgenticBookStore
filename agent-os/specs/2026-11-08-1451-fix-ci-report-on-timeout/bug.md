---
bug: fix-ci-report-on-timeout
severity: S3
path: lite+research
status: fixed
closes: [ci-performance review finding (PR #46, merged as a7047c3)]
---
# Bug: a hung test leaves CI without the test report and without the Surefire reports artifact

## Report
- Observed: when a test hangs and the `verify` job hits `timeout-minutes: 15`, GitHub ends the job as cancelled. The
  JUnit report step (`if: ${{ !cancelled() }}`, `.github/workflows/ci.yml:39`) and the Surefire reports upload
  (`if: failure()`, `.github/workflows/ci.yml:49`) are both skipped, so there are no annotations and no reports
  artifact. Only the `if: always()` pre-pull log step runs. The job still ends red, so nothing is masked.
- Expected: on a timed-out verify job the Surefire reports that exist are uploaded and the report step annotates what
  it can. A run the user cancels manually, or a pull_request run superseded by `concurrency` (`cancel-in-progress`),
  does not have to produce them. A green run still uploads no artifact.
- Environment: GitHub-hosted `ubuntu-latest`, workflow `CI` on `factory` at 19d3000.
- First seen / last known good: found by the read-only review of the ci-performance spec (PR #46). Regression window:
  not a regression of older behaviour; the report and upload steps were introduced by ci-performance T2 (PR #48).
- Frequency: always, whenever a hang reaches the job timeout.
- Impact: the "full report on failure" goal of ci-performance fails exactly in the hung-test case; a workaround
  exists (re-run locally). S3.
- Related: `agent-os/specs/2026-11-08-1217-ci-performance/` (T2), PR #46, PR #48.

## Reproduction
- Status: reproduced on a real runner.
- Recipe: throwaway branch `exp/fix-ci-report-on-timeout-red` = `origin/factory` 19d3000 plus one commit 9319df2 that
  adds a distributor test `ZzHungTestProbeTest` sleeping 30 minutes and lowers the `verify` job timeout from 15 to 4
  minutes (nothing else changes); draft PR #54 into `factory`, never merged.
- Evidence: run 42491917218, `verify` job 128765932512 (`gh run view 42491917218 --json jobs`): job `cancelled`;
  `Build and test` `cancelled`; `Test report` `skipped`; `Upload surefire reports` `skipped`; `Show Postgres pre-pull
  log and image digest` `success`. Annotations (`gh api .../check-runs/128765932512/annotations`): "The job has
  exceeded the maximum execution time of 4m0s" and "The operation was canceled.". Artifacts
  (`gh api .../actions/runs/42491917218/artifacts`): `total_count` 0. The log shows every bookstore test class and the
  distributor classes `HealthEndpointTest` and `DistributorApplicationTests` finishing before
  `Running nl.openvalue.distributor.ZzHungTestProbeTest`, so their `TEST-*.xml` reports existed on the runner and were
  lost.
- Regression-test layer: real GitHub Actions runs (a workflow file has no unit layer). The regression evidence is the
  red-before and green-after run pair of task T1, recorded by run id; the file-swap ceremony of the bugfix templates
  does not apply to a workflow file.
- Test file: the throwaway probe `distributor/src/test/java/nl/openvalue/distributor/ZzHungTestProbeTest.java`, only
  on `exp/*` branches, never merged.

## Root cause
- Causal chain: no report and no artifact after a hang -> the report and upload conditions (`ci.yml:39`, `ci.yml:49`)
  are false -> `cancelled()` is true because GitHub delivers a job timeout to the runner as a cancellation -> **root
  cause: the `Build and test` step (`.github/workflows/ci.yml:36-37`) has no step-level `timeout-minutes`, so the only
  thing that stops a hang is the job timeout at `ci.yml:21`, which cancels the job** -> the ci-performance gates proved
  the failure path with failing assertions only, never a hang.
- Root cause (one sentence): because `Build and test` (`ci.yml:36-37`) has no step timeout below the job's
  `timeout-minutes: 15` (`ci.yml:21`), a hung test is ended by the job timeout, which GitHub reports as a cancellation,
  so `cancelled()` is true and both report conditions (`!cancelled()`, `failure()`) are false.
- Contributing factors: no expression distinguishes a job timeout from a manual cancel or a concurrency supersede
  (all three reach the runner as the same cancel), so the conditions alone cannot express the expected behaviour.
- Introduced by: ci-performance T1 (job timeouts, PR #47) together with T2 (report and upload steps, PR #48); intended
  a full report on test failure and a bounded job.
- Semantics (research res-1, runner v2.338.2 source and GitHub docs): a step that hits its own `timeout-minutes`
  while the job token has not fired is logged "has timed out after N minutes" and set to Failed
  (`src/Runner.Worker/StepsRunner.cs` lines 322-337), and a failed step makes the job failure (lines 274-279), so
  `!cancelled()` and `failure()` are true afterwards; a job timeout, a manual cancel and a concurrency supersede set the
  job to Canceled (lines 149-155), so `cancelled()` is true and only `always()` steps run. On a step timeout the
  runner sends SIGINT, waits 7.5 s, SIGTERM, waits 2.5 s, then kills the shell (`src/Runner.Sdk/ProcessInvoker.cs`).

## Blast radius
- [x] Same pattern elsewhere: `grep -nE "timeout-minutes|cancelled\(\)|failure\(\)|always\(\)|success\(\)"
  .github/workflows/*.yml` hits `ci.yml:21` (verify job timeout), `:39`, `:49`, `:57` (always, unaffected) and `:64`
  (`windows-build` job timeout). `ci.yml` is the only workflow. `windows-build` runs no tests and has no report or
  upload step: deferred, nothing to report there.
- [x] Other callers: none; the report and upload steps exist only in `verify`. A hang in a step other than
  `Build and test` (checkout, setup-java) still ends as a job timeout: deferred, no test reports exist at that point.
- [x] Persisted data already wrong: none (CI configuration).
- [x] Artifacts already produced: none to remediate; past timed-out runs simply have no artifact.
- [x] Docs, standards, ADRs or tests encoding the wrong assumption: ci-performance `plan.md` T2 criteria cover the
  failure path only; not changed (historical record). Spec 01 "Requirement: Continuous integration" is unaffected.
- [x] Sibling environments: none (no other workflow, no self-hosted runner).

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause: step-level `timeout-minutes` on `Build and test` below the job timeout | yes | one added key plus a comment; a hang ends the step as failed, the existing conditions then run report and upload; manual cancel and supersede still skip both; green runs unchanged |
| (b) revert | no | reverting ci-performance T1/T2 removes the timeouts or the report, the opposite of the goal |
| (c) conditions `always()` / `always() && !success()` | viable but weaker | covers the job timeout but also uploads on every manual cancel and supersede, and the job stays "cancelled", not failed |
| (d) `-Dsurefire.timeout` in the CI command | partial | covers hung Surefire forks only, makes the CI command diverge further from the local referee |
Chosen: (a), value 10 minutes (the build step takes about 30 to 60 s; 10 leaves 5 minutes of the 15-minute job for the
setup steps, the up to 15 s kill sequence, the report and the upload). The job timeout stays 15 as the backstop for
hangs in other steps.

## Prevention
- Gap: the ci-performance QA strategy proved "full report on failure" with failing assertions only; no criterion
  exercised the timeout path, and cancellation semantics were not checked against the status functions.
- Action: follow-up (QA lead standards proposal, recorded at Gate 2): any CI change that adds failure-path steps or
  timeouts proves the hang path on a real runner. No task in this cycle (lite path).

## Verification
- Red-before run 42491917218 on the pre-fix workflow (job cancelled, report and upload skipped, 0 artifacts) and the
  green-after run of task T1 on the fixed workflow (job failure, report and upload ran, artifact with the reports).
- One normal green run on the fixed workflow uploads no artifact (the T1 task PR run).
- Blast-radius items closed or deferred as above.
- `./mvnw clean verify` green and unchanged.
