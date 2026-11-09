# Standards

- `docs/spec-pack/specs/08-bookstore-assistant.md`: requirement "Failed model call" (decision D10), the API table row
  for `POST /assistant/chat`, the configuration without temperature, offline mode, the shared-key guardrails, and
  "No test calls the real Anthropic API".
- `docs/spec-pack/specs/12-storefront.md` "Errors": the assistant page texts for `assistant-offline`,
  `assistant-unavailable` (D10) and 429; the test plan (`AssistantPage.test.tsx`).
- `docs/spec-pack/orchestrator/decisions.md` D10.
- `agent-os/standards/architecture.md` (pointer to `docs/spec-pack/product/architecture-standard.md`): failures are
  translated into the four use-case exceptions at the use-case boundary (section 4); `UnavailableException` is the
  503; Spring AI only in `assistant` (rule 7); the `assistant` naming suffixes (section 3); the testing standard
  (section 5: web slice for `assistant`, end-to-end `*IntegrationTest`).
- `docs/spec-pack/product/tech-stack.md`: the pinned versions; no new Maven or npm dependency.
- `.claude/skills/hexagonal-slice` (steps 0, 7 and 8 for the assistant adapter) and `layer-testing` (web slice tests
  with `@WebMvcTest`, end-to-end tests with `IntegrationTestBase`).
- `CLAUDE.md` rules: referee `./mvnw clean verify` within 60 seconds, ASCII and plain English without em dashes,
  never weaken a test or an ArchUnit rule, never `ANTHROPIC_API_KEY`, never `--no-verify`, never force-push.
