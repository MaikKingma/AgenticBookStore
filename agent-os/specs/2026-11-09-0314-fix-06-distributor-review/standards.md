# Standards

- `docs/spec-pack/specs/06-distributor-acl.md`: the translation table (`ean` -> isbn), "The system SHALL add a
  distributor title to the catalogue using translated data", the error mapping (5xx, timeout, refused connection and
  unusable answers -> `DistributorUnavailableException`, 503 `distributor-unavailable`).
- `agent-os/standards/architecture.md` (pointer to `docs/spec-pack/product/architecture-standard.md`): the onion
  layers and naming rules; `acl` holds only `AppServiceImpl` and `Properties` classes, so helpers stay private; the
  flow translates domain exceptions and passes port exceptions through.
- `.claude/skills/hexagonal-slice`, `clean-hexagonal-onion`, `ddd-building-blocks`, `layer-testing`: the per-layer
  test recipe (acl contract tests with `MockRestServiceServer`, flow tests with mocked ports, end-to-end integration
  tests on PostgreSQL).
- `CLAUDE.md` rules: referee `./mvnw clean verify` within 60 seconds, ASCII and plain English without em dashes,
  never weaken a test or an ArchUnit rule, never `ANTHROPIC_API_KEY`, never `--no-verify`, never force-push.
