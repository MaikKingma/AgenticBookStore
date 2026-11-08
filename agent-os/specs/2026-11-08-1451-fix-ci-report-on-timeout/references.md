# References

- Workflow: `.github/workflows/ci.yml` (`verify` job: timeout line 21, `Build and test` lines 36-37, `Test report`
  condition line 39, `Upload surefire reports` condition line 49, `always()` log step line 57).
- Spec 01: `docs/spec-pack/specs/01-foundation.md`, "Requirement: Continuous integration".
- Introducing cycle: `agent-os/specs/2026-11-08-1217-ci-performance/` (T1 PR #47 timeouts, T2 PR #48 report and upload,
  plan.md amendment A3 on job summaries); spec PR #46 merged as a7047c3.
- Precedent for real-runner proof with throwaway draft PRs: ci-performance T2 fail-proof branch
  `exp/ci-performance-fail-proof`.
- Reproduction: draft PR #54, branch `exp/fix-ci-report-on-timeout-red`, run 42491917218, job 128765932512.
- GitHub docs: Expressions, status check functions
  (https://docs.github.com/en/actions/reference/workflows-and-actions/expressions#status-check-functions); Workflow
  syntax, `jobs.<job_id>.timeout-minutes` and `jobs.<job_id>.steps[*].timeout-minutes`; Canceling a workflow
  (https://docs.github.com/en/actions/managing-workflow-runs/canceling-a-workflow).
- Runner source, actions/runner v2.338.2: `src/Runner.Worker/StepsRunner.cs` (step timeout is Failed, job cancel is
  Canceled), `src/Runner.Sdk/ProcessInvoker.cs` (SIGINT, SIGTERM, kill sequence).
- Research report (comms, not committed): `.claude/comms/mailboxes/res-1/outbox/research-github-timeout-status.md`.
