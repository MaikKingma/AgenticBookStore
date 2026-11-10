# Standards

- `docs/spec-pack/product/architecture-standard.md` (via `agent-os/standards/architecture.md`) - section 5 testing per
  layer: the seed guard is an end-to-end integration test on PostgreSQL in Testcontainers; tests never see the demo
  data.
- `.claude/skills/layer-testing` - integration tests extend `IntegrationTestBase`; never weaken a test to get green.
- `CLAUDE.md` rules - referee `./mvnw clean verify` within 60 s; never skip, disable, delete or weaken a test; docs in
  plain English, no em dashes; never `ANTHROPIC_API_KEY`.
- Spec 09 "Publishing main" - the path list lives once, in `scripts/publish-workshop-main.sh`; the script and its test
  run on macOS /bin/bash 3.2 with BSD tools and on Linux bash 5 with GNU tools.
