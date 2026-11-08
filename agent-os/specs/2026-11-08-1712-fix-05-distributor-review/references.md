# References

- Pack spec: `docs/spec-pack/specs/05-distributor-service.md` (error envelope, restock quantity 1 to 500, `Clock` bean
  "so tests can fix the date", chaos knobs).
- Spec 06: `docs/spec-pack/specs/06-distributor-acl.md` lines 54-56 (the distributor error codes the bookstore ACL
  maps: 404 `TITLE_NOT_FOUND`, 422 `NOT_ORDERABLE`, 5xx and timeouts).
- Spec 05 folder: `agent-os/specs/2026-11-08-1040-05-distributor/` (`shape.md` decision D-b and the known limit for
  unmapped `/api/...` paths; `plan.md`).
- Research: `.claude/comms/mailboxes/res-1/outbox/research-fix-05-designs.md` (res-1, senior-research-agent, opus;
  reproduction evidence, framework source lines, probe recipes; gitignored comms state).
- Code: `distributor/src/main/java/nl/openvalue/distributor/api/ChaosWebConfig.java`, `ChaosInterceptor.java`,
  `RestockOrderRequest.java`, `RestockOrdersController.java`, `ApiExceptionHandler.java`, `ErrorEnvelope.java`;
  `catalog/CatalogConfig.java` (clock bean), `catalog/RestockService.java`; tests
  `distributor/src/test/java/nl/openvalue/distributor/api/ApiTestFixtures.java`, `ChaosInterceptorTest.java`,
  `RestockOrdersApiTest.java`, `TitlesApiTest.java`, `HealthEndpointTest.java`, `mcp/McpServerIntegrationTest.java`.
- Introducing commits: 2ec1d24 (spec 05 T5, chaos), 63c8d4c (spec 05 T4, REST and envelope), 05beb23 (spec 05 T3, MCP
  test).
- Precedent bugfix cycle: `agent-os/specs/2026-11-08-0932-fix-04-order-validation/`.
