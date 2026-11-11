# References for spec 10

- Pack spec: `docs/spec-pack/specs/10-harness-reference-solutions.md`.
- Strip list and publish script: `docs/spec-pack/specs/09-workshop-baseline.md`; spec 09's cycle on
  `origin/sdd/09-workshop-baseline/spec` (`scripts/publish-workshop-main.sh --list-paths` once it lands).
- Levels, branch and tag names: `docs/spec-pack/orchestrator/decisions.md` D6 and D8;
  `docs/spec-pack/workshop/workshop-plan.md` sections 4 to 9; `docs/spec-pack/workshop/sdd-framework-guide.md`.
- Guide that links the branches: `docs/spec-pack/specs/11-workshop-guide-site.md`.
- Exercise anchors: `bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java`,
  `bookstore/src/test/resources/contracts/distributor/`, the place-order chain (`OrderCommands`, `PlaceOrderPayload`,
  `OrderFlow`, `Order`, `OrderDataServiceImpl`, `OrderIntegrationTest`).
- Research reports of this cycle (comms outboxes, not committed): `research-claude-code-harness-verification.md`
  (res-1), `research-level-4-frameworks.md` (res-2), `research-exercise-codebase-facts.md` (res-3).
- Claude Code docs: https://code.claude.com/docs/en/settings-reference, /hooks, /memory, /skills, /slash-commands,
  /plugins/code-intelligence, /headless.
