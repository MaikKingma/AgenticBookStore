---
paths:
  - "**/src/test/**"
---
# Tests
- One test class per production class, same package, suffix `Test`; end-to-end tests end with `IntegrationTest`.
- Web adapters: `@WebMvcTest` with `@MockitoBean` flows. Data adapters: `@DataJpaTest` on the shared Testcontainers
  PostgreSQL (`@Import(PostgresTestConfig.class)`, never an embedded database). ACL: `MockRestServiceServer`.
- Never call the real distributor or a real LLM. The assistant uses `ScriptedChatModel`.
- Name tests after behaviour: `rejectsOrderWhenStockIsInsufficient`.
- Never weaken or delete an `ArchitectureTest` rule to make a build pass.
