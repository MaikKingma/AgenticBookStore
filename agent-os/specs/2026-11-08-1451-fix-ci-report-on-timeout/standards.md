# Standards

- `docs/spec-pack/specs/01-foundation.md`, "Requirement: Continuous integration": two jobs, Temurin 25, Maven cache,
  triggers, commands that may carry additional flags and settings that do not skip or weaken tests.
- `docs/spec-pack/product/tech-stack.md`: GitHub Actions CI, Maven wrapper.
- `CLAUDE.md` (factory): referee `./mvnw clean verify`; never weaken a test to get green; no `--no-verify`, no
  force-push; plain English, no em dashes.
- ci-performance decisions carried as standing rules: `permissions: contents: read`, every action pinned by full SHA
  with a version comment, annotate-only report, job summaries never gated (plan.md A3 of
  `agent-os/specs/2026-11-08-1217-ci-performance/`).
- `agent-os/standards/architecture.md`: not applicable (no bookstore code).
