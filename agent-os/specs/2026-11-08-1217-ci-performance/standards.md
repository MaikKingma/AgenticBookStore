# Standards: ci-performance

- `CLAUDE.md` rules: never skip, disable, delete or weaken a test; never `--no-verify`; never force-push; never mark
  anything done without the command output that proves it; secrets only `BOOKSTORE_AI_API_KEY`, never
  `ANTHROPIC_API_KEY`; docs plain English with no em dashes. Referee `./mvnw clean verify`, 60 s budget.
- `docs/spec-pack/specs/01-foundation.md`, "Requirement: Continuous integration" and its two scenarios: the contract
  this spec keeps (T1 adjusts the wording only where the brief allows).
- `docs/spec-pack/product/tech-stack.md`: Java 25 (Temurin), Spring Boot 4.1.2, Testcontainers 2.0.5, `postgres:18.6`.
  GitHub Actions versions are not pinned there; this spec records them in `plan.md`.
- `agent-os/standards/architecture.md`: not applicable (no application code changes).
- GitHub security hardening guide for third-party actions: pin actions to a full-length commit SHA.
