# Test Conventions

- Every requirement has at least one test, and every new behaviour has a test in the layer where it lives.
- Name tests after behaviour: `rejectsOrderWhenStockIsInsufficient`.
- No test calls the real distributor or a real language model. The distributor port is a Mockito mock in `IntegrationTestBase`; the assistant runs on a scripted chat model (`ScriptedChatModel`) or a mocked chat client.
- `./mvnw verify` runs everything, including the frontend tests and build, and should stay under 60 seconds.
- `ArchitectureTest` must stay green. Its rules are never weakened to make a build pass.
- Show the end of the `./mvnw verify` output as proof. Never say tests pass without having run them.
