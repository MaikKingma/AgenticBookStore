# References: ci-performance

- Current workflow: `.github/workflows/ci.yml` (factory at 05be36c).
- Spec 01 CI requirement: `docs/spec-pack/specs/01-foundation.md`, "Requirement: Continuous integration".
- Test infrastructure the image step must match: `bookstore/src/test/java/nl/openvalue/bookstore/PostgresTestConfig.java`
  (image `postgres:18.6`).
- Baseline runs (warm, factory and pull_request): 42474529317, 42478926587, 42476894930, 42476879143, 42473951976;
  duplicate pair 42478894392 (push) and 42478908987 (pull_request) on one commit.
- Image experiment: run 42480752749 on throwaway branch `exp/ci-performance-images` (commit 59c7517, never merged,
  branch and cache entry deleted), attempt 1 cold, attempts 2 to 5 warm. See `measurements.md`.
- Actions (verified 2026-11-08, re-verified by T1/T2 at implementation): actions/checkout v7.0.1, actions/setup-java
  v6.2.0, actions/upload-artifact v7.0.2, mikepenz/action-junit-report v6.7.1.
- Docs: GitHub Actions workflow syntax (on.push.branches, concurrency, permissions), caching dependencies (cache
  scope and restrictions), security hardening for GitHub Actions (SHA pinning), Testcontainers configuration
  (`TESTCONTAINERS_RYUK_DISABLED`), Docker Hub usage limits, Maven CLI (`--fail-at-end`).
