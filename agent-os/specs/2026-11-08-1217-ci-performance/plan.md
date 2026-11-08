---
spec: ci-performance
source: Maik's brief of 2026-11-08 (no pack spec; shape.md is the spec)
status: in-progress
accepted-by: orchestrator (autonomous)
created: 2026-11-08T12:43:10Z
gate1: 2026-11-08T12:43:10Z
qa-lead: on
base: factory
branch: sdd/ci-performance/spec
module-vocabulary: [root, bookstore, distributor]
---

# Plan: ci-performance

shape.md holds goals, constraints, findings, decisions and follow-ups; measurements.md holds the numbers.
QA directives live in the QA lead's ledger `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-ci-performance.md`
(section "QA strategy": common block C0 plus T1-1..T1-6, T2-1..T2-7, T3-0..T3-6, qa-final focus).

## Ground rules

- The only product files this spec changes are `.github/workflows/ci.yml` and `docs/spec-pack/specs/01-foundation.md`.
  No pom, `.mvn/` or source change. The local referee `./mvnw clean verify` behaves exactly as before.
- Never skip, disable, delete or weaken a test; no `continue-on-error`, `-fn`, `--fail-never`,
  `maven.test.failure.ignore` or `testFailureIgnore`. The `verify` job fails whenever a test fails.
- Workflow ASCII only, no tabs; docs plain English, no em dashes; no secrets.
- Every `uses:` is pinned to a full 40-character commit SHA with a trailing `# vX.Y.Z` comment; the implementer
  records the `gh api repos/<o>/<r>/releases/latest` output with a timestamp in its outbox.

## Verification model (CI tasks)

Evidence is real GitHub Actions runs. For each task the orchestrator commits the implementation, pushes the task branch
and opens the task PR into `sdd/ci-performance/spec` before the verifier runs; rework rounds are added commits on the
same branch. Verifiers read runs with `gh` and never push, rerun, cancel or write git; reruns and throwaway branches are
done by the orchestrator. The local referee runs once per task to prove the local build is unchanged.

## Tasks

| id | summary | module | dependsOn | batch | qaTier | qaModel | impl model | status | pr | branch | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | triggers, concurrency, SHA pins and timeouts remove duplicate and superseded runs | root | [] | B1 | elevated | sonnet | sonnet | PASS r1 | #47 | sdd/ci-performance/T1 | ENDORSE |
| T2 | full test report on failure (`-fae`, annotations, report artifact; summary unverified per A3) | root | [T1] | B2 | critical | opus | sonnet | PASS r3 (A3) | #48 | sdd/ci-performance/T2 | ENDORSE r3 |
| T3 | Docker images: Ryuk off and background pre-pull of `postgres:18.6`, measured | root | [T2] | B3 | elevated | sonnet | sonnet | pending | | | |

Batches are sequential because all three tasks edit `ci.yml`.

### T1: triggers, concurrency, SHA pins and timeouts

- fileScope: `.github/workflows/ci.yml`, `docs/spec-pack/specs/01-foundation.md`
- qaDirective: ledger C0 plus T1-1 to T1-6.
- Acceptance criteria:
  1. `on:` keeps `push` and `pull_request`; `push.branches` is exactly `main`, `factory`, `sdd/*/spec` (no `tags`,
     `branches-ignore` or `paths` keys, so tag pushes no longer trigger: reading check); `pull_request` has no branch or
     path filter.
  2. Workflow-level `concurrency`: `group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.sha }}`
     and `cancel-in-progress: ${{ github.event_name == 'pull_request' }}` (push runs are grouped per sha and never
     cancelled: reading check plus the T2 spec-branch witness).
  3. Every `uses:` pinned to a full commit SHA with a `# vX.Y.Z` comment, each SHA verified to be that release tag's
     commit: actions/checkout v7.0.1, actions/setup-java v6.2.0, or the newest stable release at implementation time
     as recorded in the outbox.
  4. `timeout-minutes`: `verify` 15, `windows-build` 10.
  5. Job names, runners, Temurin 25, `cache: maven`, both `run:` commands and `permissions: contents: read` unchanged;
     no job-level `permissions` or `env`.
  6. Spec 01 "Requirement: Continuous integration" text states the trigger scope (push to `main`, `factory` and the
     `sdd/*/spec` integration branches; every pull request) and adds one sentence that the commands may carry
     additional flags and environment settings that do not skip or weaken tests (for example `-fae`). The two
     scenarios and everything outside that requirement are unchanged; plain English, no em dashes, ASCII.
  7. The workflow parses and GitHub accepts it; ASCII only, no tabs; no pom or `.mvn/` change; local referee green.
  8. Real-runner evidence: no `push` run exists for branch `sdd/ci-performance/T1`; the PR head sha has exactly one
     CI run (event `pull_request`), both jobs green; a pull_request run superseded by a newer push to the same PR
     concluded `cancelled` with the concurrency cancellation annotation; runs per task-PR push before 2, after 1.

### T2: full test report on failure

- fileScope: `.github/workflows/ci.yml`
- qaDirective: ledger C0 plus T2-1 to T2-7.
- Acceptance criteria:
  1. `verify` runs `./mvnw -B -fae verify`; `windows-build` runs `mvnw.cmd -B -fae -DskipTests verify`; nothing else
     in either command changes.
  2. In `verify`, after the build step, a mikepenz/action-junit-report step (SHA-pinned, v6.7.1 or newer stable) with
     `if: ${{ !cancelled() }}` reads `**/target/surefire-reports/TEST-*.xml` and publishes every failed test with its
     message as an annotation; `annotate_only: true` (no check run, no extra permission; workflow permissions stay
     `contents: read`). The job conclusion is failure whenever a test fails. (Narrowed by A3: the step may also write
     a job summary, but its content is not a gated criterion and is reported as unverified by QA.)
  3. After it, an actions/upload-artifact step (SHA-pinned, v7.0.2 or newer stable) with `if: failure()` uploads
     `**/target/surefire-reports/**` of all modules, `retention-days: 14`, artifact name containing the job id and
     `${{ github.run_attempt }}`.
  4. Green task PR run: both jobs green, the report step ran and succeeded, its per-module log lines name both modules
     with totals equal to the Surefire console totals; zero artifacts. (Narrowed by A3: no job summary evidence.)
  5. Deliberate-failure proof on throwaway branch `exp/ci-performance-fail-proof` (one failing assertion in a
     bookstore test and one in a distributor test, a draft PR into `sdd/ci-performance/spec`, never merged, closed and
     the branch deleted afterwards): `verify` red; the Maven log shows the bookstore assertion failure, distributor
     tests running after it, a reactor summary with both modules FAILURE and none SKIPPED, BUILD FAILURE; both failing
     tests with their messages in the annotations API (A3: not in the summary); the artifact holds both modules' reports with
     `<failure` elements and expires 14 days after creation; the orchestrator reruns the failed jobs once and attempt 2
     uploads an artifact under a different name. Run id recorded.
  6. Evidence path for "full report on failure" (replaced by A3): `-fae` behaviour in the Maven log (both modules
     report, none skipped), `gh api repos/MaikKingma/AgenticBookStore/check-runs/<job_id>/annotations` showing every
     failed test with its message, the uploaded Surefire artifact holding all modules' reports, and the job concluding
     failure. Job summary content is not evidence for any criterion and is neither rendered nor inferred.
  7. Numbers: on the green run the report step duration and the verify job duration are recorded next to the baseline
     median 58 s (range 52 to 62; build step 50 s, 47 to 55, per the C0-8 script on the five warm baseline runs, see
     Amendment A1); the report step takes at most 10 s.
  8. YAML parses, ASCII only, no pom change, nothing about Docker or Ryuk changes.

### T3: Docker images, Ryuk off and background pre-pull

- fileScope: `.github/workflows/ci.yml`
- qaDirective: ledger C0 plus T3-0 to T3-6.
- Design decided by the experiment (measurements.md, run 42480752749): no image cache, no mirror.
- Acceptance criteria:
  1. `verify` job `env` sets `TESTCONTAINERS_RYUK_DISABLED: 'true'` (a string), only under `jobs.verify`, with a
     comment that this is safe on ephemeral runners only.
  2. The first step of `verify` (before checkout) starts a background `docker pull postgres:18.6` (exactly the
     string in `PostgresTestConfig`) writing start and end timestamps to a log file; the last step of `verify` has
     `if: always()` and prints that log and the pulled image digest.
  3. No image cache, no `docker save`/`load`, no registry mirror, no image-name change; the decision cites the
     experiment numbers in measurements.md.
  4. Real-runner evidence over at least 3 runs or attempts on the head sha: in every run the verify log has 0
     `Pulling docker image: postgres` lines, 0 `testcontainers/ryuk` lines, exactly 1 `Ryuk has been disabled` line and
     the container started once; in every run the background pull's end timestamp precedes the first Testcontainers log
     line; per-module totals equal the T2 green run's, Skipped 0; the median `Time elapsed` of the test class that starts
     the container is lower than the baseline median of the same metric computed by the same script on the 5 baseline
     runs. Job duration, build step and Maven Total time are reported with medians and ranges against the baseline, not
     gated.
  5. Nothing outside the workflow changes; the local build still starts Ryuk.

## QA feed-forward

- From T1 r1 (QA lead ledger, "Feed-forward directives"): FF-1 baseline verify 58 s / build step 50 s, quote own
  same-script values; FF-2 warning-line inventory against baseline job 128712764531 (T3: only `Ryuk has been disabled`
  may be added); FF-3 byte-compare every `${{` string in a `python3 -I` script with a negative control; FF-4 save the
  unfiltered `gh run view --json jobs` output as evidence; FF-5 (qa-final) residual: same-sha pushes to two filtered
  branches share one push group.
- From T2 r2 and r3: FF-6 (T3, qa-final) a green verify job prints exactly 2 plain `No annotations found for JUnit
  Test Report | <module>` lines; FF-7a (T3, qa-final, replaces FF-7 per A3) quote the report step's per-module log lines
  equal to the console totals plus the annotations API, never summary evidence, summary stated as UNVERIFIED by QA;
  FF-8 (qa-final residuals) annotations point at `.class` paths, `github.job` collides across future matrix legs;
  FF-9 (T3) compare totals with T2 green run 42483525266, byte-compare the report and upload steps against e31cf21.

## Amendments

- A1 2026-11-08T13:03Z, orchestrator (QA lead ruling F-B on T1 r1): T2 AC7 baseline reference corrected from 52 s
  (research prose, a different run set) to 58 s, the same-script median over the five warm baseline runs. No pass rule
  changes (the gate is report step at most 10 s). Gate re-run self-accepted: a measurement reference fix inside the
  spec's authority.
- A2 2026-11-08T13:28Z, orchestrator (qa-t2 r1 BLOCKED): GitHub hides job summaries from logged-out viewers (positive
  control on the action's own public run also showed none; `summary_raw` 404). T2 AC6 evidence path extended: the
  summary may be rendered with the claude-in-chrome skill in the owner's signed-in browser (read-only page views of this
  repository's run pages only) when anonymous rendering shows no summary; the annotations cross-check stays. If neither
  works, a human capture remains the fallback. Gate re-run self-accepted: evidence method only, criteria unchanged.
  WITHDRAWN by A3.
- A3 2026-11-08, orchestrator decision (made by the main-session orchestrator, to whom the owner delegated all plan and
  gate approvals; this is not a QA lead endorsement), resolving the T2 needs-decision (option d): T2 is narrowed. The
  job-summary half of AC2, AC4 and AC5(e), and the summary evidence path of AC6, are dropped as gated criteria,
  because GitHub exposes job summaries to no API or CLI and no agent can verify them. The gated evidence for "full
  report on failure" is: `-fae` behaviour (both modules report, none skipped), the annotations API showing every
  failure, the uploaded Surefire artifact holding all reports, and the job failing red. The report action keeps its
  summary output (no workflow change, so the r1 and r2 evidence on head e31cf21 and fail-proof 35b8243 carries); the
  spec PR body describes it as unverified by QA for the owner's end-result review. No `checks: write` (least privilege
  kept). Amendment A2 (Claude in Chrome route) is withdrawn. qa-final must not require job-summary evidence (QA lead
  FF-7 and the summary part of qa-final focus 2 are superseded). Consequence: T2 gate re-run as verifier round 3
  (`mode: resume`, narrowed criteria) plus QA lead meta-review; no implementation rework.

## Decisions log (orchestrator, self-accepted)

- 2026-11-08 Tech lead skipped: single module, CI configuration only, three sequential tasks; the Docker topic went to
  a senior researcher and was settled by an experiment.
- 2026-11-08 Image strategy: no cache (warm restore plus load median 7 s against pull median 5 s; cold run +9 s), no
  mirror (hosted runners not rate limited; probe shows 100 per hour per runner address, none consumed), Ryuk off plus
  background pre-pull (paired build-step saving median 7 s).
- 2026-11-08 Push filter `main`, `factory`, `sdd/*/spec`: removes the push/pull_request duplicate and tag runs, keeps
  both triggers, and seeds the spec-branch Maven cache that task PRs restore. Accepted cost (QA lead G7): with an open
  spec PR each spec-branch push runs twice. Reversible; spec 01 wording updated in T1.
- 2026-11-08 Report in annotate-only mode (least privilege, works on fork PRs); summary evidence via the rendered public
  run page (QA lead G1).
- 2026-11-08 Rejected: Maven `-T 2`, Surefire parallelism, extra wrapper cache, image mirror, Dependabot (follow-up).
- 2026-11-08 Gate 1 self-accepted: QA lead findings G1 to G8 folded into the criteria above (G1 T2 AC6, G2 T3 AC4,
  G3 T2 AC7, G4 reading checks plus T2 witness, G5 T1 AC6, G6 T2 AC5, G7 decision above, G8 T3 AC4 per run).
