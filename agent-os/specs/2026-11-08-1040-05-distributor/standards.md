# Standards: Spec 05 Distributor service

- `docs/spec-pack/specs/05-distributor-service.md` decides what. `docs/spec-pack/product/tech-stack.md` and `docs/spec-pack/orchestrator/decisions.md` win over everything else (versions only from tech-stack: Spring Boot 4.1.2, Spring AI 2.0.2 BOM, `spring-ai-starter-mcp-server-webmvc`, `spring.ai.mcp.server.protocol=STREAMABLE`; both services bind to 127.0.0.1).
- `agent-os/standards/architecture.md` (the bookstore Clean Hexagonal Onion) does NOT apply to the distributor: the pack spec makes it a plain, small Spring app with packages `api`, `mcp`, `catalog`. The bookstore ArchUnit rules and skills do not apply either.
- Repo rules from `CLAUDE.md`: referee `./mvnw clean verify` green, budget 60 s; never skip, disable, delete or weaken a test; never `--no-verify`; never force-push; plain English without em dashes; product files ASCII only; never set, read or write `ANTHROPIC_API_KEY`; product code and config never reference `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md`.
- No custom ObjectMapper (tech-stack "JSON" row): use Boot's `JsonMapper` (Jackson 3, `tools.jackson.*`).
