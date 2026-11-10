# References

- `docs/spec-pack/specs/09-workshop-baseline.md` - Requirement "Demo data in local runs only" (scenario "Tests are
  unaffected"), "Human documentation" (README at most 120 lines, required parts), "Publishing main" (path list with
  D15's `guide/` and `.github/workflows/guide.yml`).
- `docs/spec-pack/orchestrator/decisions.md` - D10 (failed live model call answers 503 `assistant-unavailable`), D15
  (the publish script also strips the guide).
- `agent-os/specs/2026-11-09-1615-09-workshop-baseline/` - spec 09 cycle (the test, the README and the script).
- `agent-os/specs/2026-11-09-2034-fix-08-assistant-review/` - fix-08 cycle (the `assistant-unavailable` 503, T1; thinking
  disabled, T4).
- Code: `bookstore/src/test/java/nl/openvalue/bookstore/EmptyCatalogueAtStartIntegrationTest.java:32-42`,
  `bookstore/src/test/resources/application-test.yml`, `bookstore/src/main/resources/application.yml:14-18`,
  `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantCommands.java:60-80`, `README.md:87-103`,
  `http/assistant.http:1-10`, `scripts/publish-workshop-main.sh:7-18`, `scripts/test-publish-workshop-main.sh:42-53`,
  `.github/workflows/no-agent-config.yml`, `.github/workflows/ci.yml` (step running the script test).
