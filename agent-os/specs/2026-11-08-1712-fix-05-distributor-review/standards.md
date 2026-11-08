# Standards

- `docs/spec-pack/specs/05-distributor-service.md`: the distributor is "someone else's system", a plain small Spring
  app (`api`, `mcp`, `catalog`); the bookstore's onion architecture, skills and ArchUnit rules do not apply.
- `docs/spec-pack/product/tech-stack.md`: distributor dependencies are `web`, `actuator` and the MCP server starter
  only (no validation starter, no new dependency; spec 05 decision D-a: request checks by hand, answered with the
  envelope).
- `agent-os/standards/architecture.md` (pointer to `docs/spec-pack/product/architecture-standard.md`): applies to the
  bookstore; read only for the general testing rules (never weaken a test, the referee).
- `CLAUDE.md` rules: referee `./mvnw clean verify` within 60 seconds, ASCII and plain English without em dashes,
  never `ANTHROPIC_API_KEY`, never `--no-verify`, never force-push.
