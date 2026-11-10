# Tech Stack

Pin these versions; do not "upgrade to latest".

## Frontend

- React 19.3.1 with TypeScript (the version the Vite `react-ts` template pins; do not pin it separately), in `frontend/`
- Vite 8.3.6, `base: '/shop/'`; the dev server runs on port 5173 and proxies `/books`, `/inventory`, `/orders`, `/assistant` to 8080
- Vitest 5.1.0 with React Testing Library (`@testing-library/react` 16.3.3, `@testing-library/jest-dom` 7.0.1, `@testing-library/user-event` 14.6.8), `jsdom` environment
- Node.js 26.13.0, installed into `frontend/node/` by frontend-maven-plugin 2.0.2; participants never install Node.js
- frontend-maven-plugin 2.0.2 (`com.github.eirslett:frontend-maven-plugin`) runs `npm ci`, `npm test` and `npm run build` inside `./mvnw verify`

## Backend

- Java 25 (Eclipse Temurin 25 LTS), `maven.compiler.release=25`
- Maven via the wrapper (`./mvnw`, `mvnw.cmd`), multi-module: `frontend`, `bookstore`, `distributor`; `./mvnw clean verify` also tests and builds the storefront
- Spring Boot 4.1.2; do not move to 4.2.x
- Spring AI 2.0.2 via `org.springframework.ai:spring-ai-bom:2.0.2`; avoid 2.1
- Chat model (bookstore assistant): `org.springframework.ai:spring-ai-starter-model-anthropic`, key from `BOOKSTORE_AI_API_KEY`; never name it `ANTHROPIC_API_KEY`
- MCP server (distributor): `org.springframework.ai:spring-ai-starter-mcp-server-webmvc`, `spring.ai.mcp.server.protocol=STREAMABLE`, endpoint `/mcp`
- Jackson 3 (Boot 4 default); no custom ObjectMapper unless a spec asks for it
- HTTP client (acl): Spring `RestClient`; no WebClient, no Feign, no RestTemplate
- Validation: Jakarta Validation (`spring-boot-starter-validation`), on `*Payload` records only
- Observability: Spring Boot Actuator (`health`, `info` only)

## Database

- PostgreSQL 18, image `postgres:18.6`: the only database, for local runs and tests; no H2, no embedded database anywhere
- Flyway 12.4.0 (managed by Boot): plain SQL, `V{n}__{description}.sql`; `spring.jpa.hibernate.ddl-auto=validate` everywhere
- PostgreSQL JDBC driver 42.7.14 (managed by Boot); do not pin it yourself
- Local database lifecycle: `bookstore/compose.yaml` and `org.springframework.boot:spring-boot-docker-compose` 4.1.2, `lifecycle-management=start-only`, off in tests
- Test database: Testcontainers 2.0.5 (`org.testcontainers:testcontainers-postgresql`, managed by the Boot BOM), one `@ServiceConnection` container shared by all tests

## Other

- Testing: JUnit (managed by Boot 4.1.2, JUnit 6 generation), AssertJ, Mockito and MockMvc (managed by Boot); use `@MockitoBean`, not the removed `@MockBean`
- ArchUnit 1.5.1, `com.tngtech.archunit:archunit-junit6` (test scope); not managed by Boot, so it is pinned
- HTTP contract tests: `MockRestServiceServer` (spring-test); no WireMock, no MockServer
- Docker must be running for tests; a test is never skipped because Docker is missing
- Ports: bookstore 8080 (`/` redirects to `/shop/`, `/books`, `/inventory`, `/orders`, `/assistant`, `/actuator/health`), distributor 8090 (`/api`, `/mcp`, `/actuator/health`), PostgreSQL 5432 on 127.0.0.1, Vite dev server 5173 (development only)
- Both services bind to `127.0.0.1` by default (`server.address`), because the MCP endpoint is unauthenticated
- Agent tooling (not dependencies of the code): Claude Code (latest stable on the day), Agent OS 3.0 (clone of `main`)
