# Shape: ci-performance

## Origin

Maik, 2026-11-08: "The CI is pulling images each time it runs, like postgres. Improve the CI setup: build caching, run
all tests instead of failing at the first error to get a full report. There are a lot of tips and tricks to get better
performing CI." There is no pack spec for this work; this folder is the spec.

## Goals

1. Avoid re-pulling Docker images every run, or prove with numbers that a cache is not faster than a pull on
   GitHub-hosted runners and choose the faster option. Weigh Docker Hub anonymous pull rate limits.
2. A full test report on failure: the reactor continues past a failing module, every Surefire result is visible in the
   job summary, and the reports are uploaded as an artifact on failure. The job still fails when any test fails.
   (Job summary part not gated and UNVERIFIED by QA, plan.md Amendment A3: GitHub exposes job summaries to no API
   or CLI. The gated evidence is `-fae`, the annotations API, the uploaded artifact and the red job.)
3. Other proven CI practices where they measurably help or are clearly correct: sensible cache keys, concurrency groups
   that cancel superseded runs, no duplicate work, pinned action versions, timeouts. Parallelism only if deterministic.

Every change is backed by before and after numbers from real runs (`measurements.md`).

## Constraints

- Spec 01 "Requirement: Continuous integration" still holds: one workflow, jobs `verify` (ubuntu-latest, full
  `./mvnw -B verify` with Testcontainers PostgreSQL) and `windows-build` (windows-latest, compile only with
  `-DskipTests`), on push and pull request, Temurin 25, Maven dependency caching. Additive flags and steps are fine.
  Where the wording must change, T1 updates it in `docs/spec-pack/specs/01-foundation.md`.
- The local referee `./mvnw clean verify` behaves exactly as before: no pom or `.mvn/` change; CI-only flags and env vars.
- Never skip, disable, delete or weaken a test; no `continue-on-error`, `-fn` or test-failure-ignore settings.
- Versions of Java, Spring Boot, Testcontainers and postgres stay at the pins in `docs/spec-pack/product/tech-stack.md`.
- Workflow files ASCII only; docs plain English with no em dashes; no secrets.
- No speculative config for spec 11 (`guide.yml`) or spec 12 (frontend build); their caching follow-ups are listed below.

## Findings that shaped the plan (research artifacts are in the orchestrator comms, summarised in measurements.md)

- Images cost about 4 to 6 s of a 52 s verify job (research prose; the same-script baseline over the five warm runs
  is a 58 s median with a 52 s minimum, plan.md Amendment A1). The experiment (run 42480752749) showed a warm tarball cache
  (restore plus `docker load`, median 7 s) is slower than a plain pull (median 5 s), and a cold cache run adds about 9 s.
  Cache scope is per branch, and this repo cuts a new branch per task, so many runs would be cold. Decision: no image
  cache. Ryuk off plus a background pre-pull of `postgres:18.6` saved a median of 7 s on the build step.
- Docker Hub answered `ratelimit-limit: 100;w=3600` with `ratelimit-remaining: 100` on every runner, and GitHub
  documents that hosted-runner pulls are not rate limited. No mirror (a mirror adds an eviction failure mode).
- `actions/setup-java` v6 with `cache: maven` already caches both `~/.m2/repository` and `~/.m2/wrapper/dists` and sets
  `-ntp`. No extra cache step is needed. The real cache problem is scope: 28 of 44 push runs were cold (a research figure, not recomputed: its run set
  cannot be restated and its grep text does not match setup-java's `maven cache is not found` line; this cycle had 1
  cold verify job of 18, measurements.md "After"), because task
  branches cannot see sibling caches and nothing seeds the spec branch. Pushing to `sdd/*/spec` seeds it.
- One task PR caused about 5 runs, 2 of them pure duplicates (push plus pull_request on one commit). Tag pushes ran
  full CI too.
- Only Surefire runs (no Failsafe). Bookstore and distributor are independent modules, so `-fae` tests both.
- Rejected: Maven `-T 2` (at most 4 to 7 s, interleaves module logs against the reporting goal), Surefire parallelism
  (unsafe: `DatabaseCleaner` truncates tables per test, shared recorders), image mirror, extra wrapper cache,
  Dependabot (new configuration, owner decision, follow-up).

## Decisions (self-accepted by the orchestrator, see plan.md)

- Push trigger limited to `main`, `factory` and `sdd/*/spec`; pull_request unfiltered. Both triggers stay. Trade-off:
  pushes to a branch without a PR get no CI, and each push to a spec branch with an open spec PR runs twice (push run
  seeds the cache, PR run checks the merge). Tag pushes no longer run CI.
- Concurrency per PR number (cancel superseded pull_request runs) or per sha (push runs, never cancelled).
- Actions pinned by full commit SHA with a version comment: this public repo is shipped to workshop participants,
  and GitHub's hardening guide names a full SHA as the only immutable reference.
- Report: mikepenz/action-junit-report in annotate-only mode, so no `checks: write` permission is needed and fork PRs
  work; artifacts with `actions/upload-artifact` on failure, 14 days.
- Ryuk disabled on the `verify` job only (ephemeral runners), background pre-pull of the pinned image.

## Follow-ups (not in this spec)

- Spec 11 (`guide.yml`): `actions/setup-node` with `cache: npm` and `cache-dependency-path: guide/package-lock.json`;
  the same concurrency group pattern and SHA pinning; for the Pages deploy job use the `pages` concurrency group without
  cancelling an in-progress deploy; push filter matching this workflow.
- Spec 12 (frontend via frontend-maven-plugin): cache the Node.js download (`frontend/node/`, keyed on the Node.js pin
  26.13.0 and the plugin version) and the npm cache (`~/.npm` on Linux, `%LocalAppData%\npm-cache` on Windows, keyed on
  `frontend/package-lock.json`) in both jobs, or set the plugin's download root to a cached directory; `npm ci` keeps
  the lockfile authoritative. Re-measure Maven `-T 2` once the reactor has three modules.
- Dependabot (or GitHub's workflow lockfile mechanism) to keep the SHA pins current: owner decision.
- Spec 09: the push filter names `sdd/*/spec`, which is factory tooling; spec 09 may drop that entry when it publishes
  `main`.
