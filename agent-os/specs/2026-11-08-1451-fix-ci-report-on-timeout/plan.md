---
spec: fix-ci-report-on-timeout
source: ci-performance read-only review (PR #46, merged as a7047c3), bug report of 2026-11-08
status: in-progress
accepted-by: orchestrator (autonomous; owner delegated all plan and gate approvals)
created: 2026-11-08T14:59:00Z
origin: main-session bug report, lite bugfix cycle
gate1: 2026-11-08T14:59:00Z
qa-lead: on
base: factory
branch: sdd/fix-ci-report-on-timeout/spec
module-vocabulary: [root, bookstore, distributor]
kind: bugfix
severity: S3
path: lite+research
fix-strategy: root-cause
regression-since: not-a-regression
closes: [ci-performance review finding (PR #46)]
---

# Plan: fix CI report on a timed-out verify job

bug.md (same folder) holds the report, reproduction, root cause, blast radius, strategy and prevention.

## Ground rules

- The only product file this spec changes is `.github/workflows/ci.yml`. No pom, `.mvn/`, source or test change on
  the spec branch; the local referee `./mvnw clean verify` behaves exactly as before.
- Spec 01 "Requirement: Continuous integration" stays satisfied. The `verify` job fails whenever a test fails or hangs;
  no `continue-on-error`, no `|| true`, no `-fn`/`--fail-never`/`testFailureIgnore`.
- `permissions: contents: read` stays; every `uses:` stays pinned by full SHA; workflow ASCII only, no tabs; docs plain
  English, no em dashes; no secrets.
- Gated evidence must be readable by an agent: run and job conclusions, step conclusions, logs, the annotations API and
  the artifacts API. Job summaries are never a criterion (ci-performance plan.md A3).

## Verification model (CI task)

Evidence is real GitHub Actions runs. The orchestrator commits the implementation, pushes `sdd/fix-ci-report-on-timeout/T1`
and opens the task PR into the spec branch before the verifier runs. Throwaway probe branches (`exp/fix-ci-report-on-timeout-*`)
are created, pushed, PR'd as drafts, closed unmerged and deleted by the orchestrator; their only addition is the probe
test `distributor/src/test/java/nl/openvalue/distributor/ZzHungTestProbeTest.java` (a 30-minute `Thread.sleep`), plus,
on the red probe only, the verify job timeout lowered to 4 minutes for speed. Verifiers read runs with `gh` and never
push, rerun, cancel or write git.

## Lite path checklist (sdd-bugfix reference.md section 6)

- [x] reproduced in 1a.2 (run 42491917218, not intermittent)
- [x] root cause named at file:line (`ci.yml:36-37` with `ci.yml:21`) AND confirmed by the reproduction (job cancelled,
  both conditions skipped)
- [x] one obvious fix design readable from the code: step-level `timeout-minutes`; the mechanism (step timeout is Failed,
  job timeout is Canceled) is verified from runner source by res-1 and proven on a real runner by the T1 green-after
  probe; if that probe does not show it, the upgrade rule applies
- [x] blast radius enumerable by grep now (bug.md section Blast radius), every site listed with a disposition
- [x] no already-wrong persisted data or produced artifacts to remediate
- [x] not S1; not money/numbering/auth/concurrency
Bundled owner decisions in the report: none.
Open question the code does not settle in minutes: GitHub status-function semantics under job vs step timeout -> one
researcher res-1 (research-agent, sonnet), lead: null.
-> path: lite+research (no lead; one task; per-task QA on opus, QA lead meta-review on opus, qa-final on opus with the
QA lead's brief and final review, both gates).

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | impl model | status | pr | branch | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | a hung test ends `Build and test` as failed so the report and the Surefire artifact are produced | root | [] | B1 | elevated | opus | sonnet | PASS r1 | #56 | sdd/fix-ci-report-on-timeout/T1 | ENDORSE |

### T1: step timeout on Build and test

- fileScope: `.github/workflows/ci.yml`
- Acceptance criteria:
  1. In `jobs.verify`, the `Build and test` step gets `timeout-minutes: 10` and one comment line stating it must stay
     below the job `timeout-minutes` (15) so a hang ends this step as failed instead of cancelling the job, which lets
     the report and upload steps run. No other line of `ci.yml` changes: `git diff origin/sdd/fix-ci-report-on-timeout/spec
     -- .github/workflows/ci.yml` shows only added lines inside that step; the report condition `if: ${{ !cancelled() }}`,
     the upload condition `if: failure()`, the job timeouts 15 and 10, the `run:` commands, the pins, `permissions` and
     `env` are byte-identical. YAML parses, ASCII only, no tabs.
  2. Negative control (red-before, pre-fix workflow): run 42491917218 (draft PR #54, head 9319df2 = `origin/factory`
     19d3000 plus only the probe test and the verify timeout 15 -> 4): `verify` job 128765932512 concluded `cancelled`;
     `Build and test` `cancelled`; `Test report` `skipped`; `Upload surefire reports` `skipped`; annotation "The job has
     exceeded the maximum execution time of 4m0s"; artifacts `total_count` 0. Verified independently with
     `gh run view 42491917218 -R MaikKingma/AgenticBookStore --json jobs`,
     `gh api repos/MaikKingma/AgenticBookStore/check-runs/128765932512/annotations` and
     `gh api repos/MaikKingma/AgenticBookStore/actions/runs/42491917218/artifacts`, and `git diff 19d3000 9319df2 --stat`
     shows only the probe test and `ci.yml` (one line).
  3. Green-after (fixed workflow): throwaway branch `exp/fix-ci-report-on-timeout-green` = the T1 head plus only the
     probe test file (`git diff <T1 head> <green head> --stat` lists only that file; `ci.yml` byte-identical to the T1
     head, job timeout 15, step timeout 10), draft PR into the spec branch: `verify` concluded `failure` (not
     `cancelled`); `Build and test` concluded `failure` and its log or annotations carry the runner's "has timed out
     after 10 minutes" message; `Test report` concluded `success` and its log names the per-module report results;
     `Upload surefire reports` concluded `success`; the run has exactly one artifact, named
     `surefire-reports-verify-attempt-1`, and its zip (`gh run download` or the artifacts API) contains `TEST-*.xml`
     for the bookstore test classes and for the finished distributor classes (`HealthEndpointTest`,
     `DistributorApplicationTests`); the pre-pull log step concluded `success`. Run id recorded.
  4. Normal green run (fixed workflow, no probe): the T1 task PR's `pull_request` run on its head: `verify` and
     `windows-build` concluded `success`; `Test report` `success`; `Upload surefire reports` `skipped`; artifacts
     `total_count` 0; Maven console totals per module with Skipped 0.
  5. Cancellation case (reading check, not gated on a run): because the conditions are unchanged, a manual cancel or a
     concurrency supersede still makes `cancelled()` true and skips report and upload (bug.md section Root cause,
     res-1 semantics), which the expected behaviour allows.
  6. Local referee `./mvnw clean verify` green with the same per-module test totals as on `origin/factory`; no file
     outside `ci.yml` changed on the task branch; no `continue-on-error` or `|| true` anywhere in `ci.yml`;
     `permissions: contents: read`; every `uses:` still a 40-character SHA with its version comment.
- implDirective (adapted from the bugfix template; the file-swap ceremony does not apply to a workflow file):
  "Bugfix task on a workflow file. 1. Read bug.md sections Reproduction and Root cause. 2. Under `## Red evidence` in
  your outbox record the red-before run read-only (`gh run view 42491917218 -R MaikKingma/AgenticBookStore --json jobs`,
  the annotations and artifacts calls of AC2). 3. Apply the fix at the root cause (`ci.yml:36-37`, the `Build and test`
  step), nothing else. 4. Validate: YAML parse (`ruby -ryaml -e 'YAML.load_file(ARGV[0])' .github/workflows/ci.yml` or an
  equivalent `python3 -I` script), ASCII and no-tab checks, `git diff -- .github/workflows/ci.yml`, and the referee
  `bash ~/.claude/scripts/with-budget.sh 300 ./mvnw -B clean verify` with per-module totals. 5. Leave `## Green
  evidence` stating that the green-after and normal-green runs are produced by the orchestrator after your commit and
  verified by QA. Never run git write commands, never push, never touch a workflow run. If the change cannot be expressed
  within AC1, set status: blocked and ask."
- qaDirective: "Bugfix task on a workflow file. Beyond your standard gate, verify the negative control independently
  from the GitHub API, never from the implementer's evidence: (1) red-before run 42491917218 per AC2, including the
  diff proof that the probe changed only the test and the job timeout; (2) green-after run per AC3, including the diff
  proof that the green probe differs from the T1 head only by the probe test, the step conclusions, the timeout message,
  and the artifact contents (download it to your scratch dir and list the XML files); (3) normal green run per AC4.
  State whether the implementer's `## Red evidence` matches. FAIL the task if the green-after job concluded `cancelled`,
  if report or upload did not run there, if any artifact exists on the normal green run, if any line of `ci.yml` outside
  the `Build and test` step changed, or if a build step got `continue-on-error` or `|| true`. Prefix every evidence file
  in the shared scratchpad with `t1-qa-`. Never push, rerun, cancel or write git."
- leadDirective (evidence chain): red-before run 42491917218 shows `cancelled` plus both steps `skipped` plus 0
  artifacts on the pre-fix workflow, with a probe diff limited to the test and one timeout line; the verifier's
  `## Negative control` reproduces that from the API; the green-after run on a branch differing from the T1 head only by
  the probe test shows `failure`, the step timeout message, both steps run and the artifact with the reports; the fix
  sits at bug.md section Root cause (`ci.yml:36-37`); no other line of `ci.yml` and no test changed on the task branch.

## QA feed-forward

- T1 r1 (verifier PASS, QA lead ENDORSE; ledger `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-fix-ci-report-on-timeout.md`):
  evidence runs red-before 42491917218, green-after 42492864936, normal green 42492829810, supersede witness
  42492795854. qa-final reuses them and re-checks the spec-branch tip.

## Gate record

- Gate 1 self-accepted 2026-11-08T14:59Z by the orchestrator (owner delegated plan approval). Checklist:
  - [x] T1 has summary, acceptanceCriteria, fileScope, module (`root`), dependsOn (`[]`), qaTier (`elevated`, lite
    rule), qaModel (`opus`, per the run brief), qaDirective.
  - [x] Every criterion names its command or API call; no job-summary criterion; the cancellation case is a reading
    check because the brief does not require artifacts there.
  - [x] One batch; one module; no parallelism question.
  - [x] Consistent with spec 01's CI requirement, the tech stack (GitHub Actions, Maven wrapper), the ci-performance
    decisions (least privilege, SHA pins, annotate-only report) and the research res-1 semantics.
  - [x] No decision outside orchestrator authority: one reversible key in CI configuration, no new dependency, service,
    permission or product scope.
  - [x] Open questions: none (res-1's open questions are answered by the reproduction run and the T1 probes).
  - [x] Lite path checklist ticked above; QA lead strategy skipped per the lite rule, the directives above are the
    strategy; qa-lead registered at Phase 2/3.

## Amendments

- A1 2026-11-08T15:24Z, orchestrator (QA lead note N1 at T1 r1): T1 AC6 "no file outside `ci.yml` changed on the task
  branch" reads "no product file outside `ci.yml` changed on the task branch"; the spec-folder bookkeeping commit that
  records `pr:` and the QA lead verdict in this plan (sdd-cycle Phase 2/3 step 2i) and the task's
  `chore(agent-memory)` commit are process files, not product files. Gate re-run self-accepted: a wording
  clarification, no behaviour or evidence changes.

## Decisions log (orchestrator, self-accepted)

- 2026-11-08 Path lite+research: one research-agent (sonnet) for GitHub status semantics; no tech lead.
- 2026-11-08 Fix (a) step timeout over (c) `always()` conditions: keeps manual cancel and supersede cheap, makes a hung
  test end red as failure; (c) recorded as rejected in bug.md.
- 2026-11-08 Step timeout 10 minutes: the build step ran in about 40 s on the red probe; 10 leaves 5 minutes of the
  15-minute job for setup, the kill sequence, report and upload.
- 2026-11-08 The orchestrator writes the throwaway probe test itself (sdd-bugfix 1a.2 sanctions orchestrator probes;
  never merged, branches deleted).
- 2026-11-08 Green-after probe keeps the fixed `ci.yml` unchanged (real 10-minute step timeout) so the proof runs the
  exact workflow that merges; the red probe lowered the job timeout to 4 minutes, which changes only how long the
  same job-timeout path takes.
