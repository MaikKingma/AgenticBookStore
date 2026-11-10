# Standards for spec 09

- `agent-os/standards/architecture.md`, which points to `docs/spec-pack/product/architecture-standard.md` (layers,
  dependency rules, naming, testing per layer, the process exception). `docs/architecture.md` rewrites it for the team.
- `docs/spec-pack/product/tech-stack.md` (version pins: Java 25, Spring Boot 4.1.2, Flyway as managed by Boot,
  `postgres:18.6`, SHA-pinned GitHub Actions).
- `CLAUDE.md`: referee `./mvnw clean verify` from the repo root, 60 s budget; never skip, disable, delete or weaken a
  test or an ArchUnit rule; never `--no-verify`; never force-push a GitHub branch; docs plain English with no em
  dashes; product files ASCII only; `BOOKSTORE_AI_API_KEY` only, never set, read or write `ANTHROPIC_API_KEY`.
- Skills for bookstore code and tests: `hexagonal-slice`, `layer-testing` (T1 test placement and Testcontainers use).
- Existing migrations in `bookstore/src/main/resources/db/migration` are never edited.
