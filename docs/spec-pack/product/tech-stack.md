# Tech Stack

> Versions verified against vendor release pages on 2026-11-06. Pin them; do not "upgrade to latest" during the
> build week. Re-check only the items marked *verify at build time*.

## Runtime and build

| Item | Version / choice | Notes |
|---|---|---|
| Java | **25** (Eclipse Temurin 25 LTS) | `maven.compiler.release=25`. Boot 4.1.x supports Java 17 up to 27. |
| Build | **Maven** via wrapper (`./mvnw`, `mvnw.cmd`), multi-module: `frontend`, `bookstore`, `distributor` | Maven chosen over the start.spring.io default (Gradle) because the hooks, docs and workshop levels all assume `./mvnw`. One referee: `./mvnw clean verify` also tests and builds the storefront. |
| Spring Boot | **4.1.2** | Do not move to 4.2.x (RC at the time of writing). |
| Spring AI | **2.0.2** via `org.springframework.ai:spring-ai-bom:2.0.2` | 2.0.x supports Boot 4.0.x and 4.1.x. Avoid 2.1 (RC, deprecates chat memory APIs). |
| Database | **PostgreSQL 18**, image `postgres:18.6` | The only database, for local runs and tests. No H2, no embedded database anywhere. Docker is a participant prerequisite. Pinned 2026-11-06 from https://www.postgresql.org/support/versioning/ (18.6 released 2026-08-13) and https://hub.docker.com/_/postgres. |
| Local database lifecycle | `bookstore/compose.yaml` + `org.springframework.boot:spring-boot-docker-compose` (4.1.2) | `spring-boot:run` starts the compose service and derives the datasource from it. `lifecycle-management=start-only`. Off in tests. |
| Test database | **Testcontainers 2.0.5**, `org.testcontainers:testcontainers-postgresql` (managed by the Boot BOM) | Boot 4.1.2 manages `testcontainers.version=2.0.5` (https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom), which is also the latest release (https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.5, 2026-04-20). Module names are prefixed `testcontainers-` in 2.x. One `@ServiceConnection` container shared by all tests. |
| PostgreSQL JDBC driver | 42.7.14 (managed by Boot) | Do not pin it yourself. |
| Migrations | **Flyway** 12.4.0 (managed by Boot; plain SQL, `V{n}__{description}.sql`) | `spring.jpa.hibernate.ddl-auto=validate` everywhere. The PostgreSQL support artifact (`flyway-database-postgresql`) is added by the `flyway` and `postgresql` starters, *verify at build time*. |
| JSON | Jackson 3 (Boot 4 default) | No custom ObjectMapper unless a spec asks for it. |
| HTTP client (acl) | Spring `RestClient` | No WebClient, no Feign, no RestTemplate. |
| Validation | Jakarta Validation (`spring-boot-starter-validation`) | On `*Payload` records only. |
| Observability | Spring Boot Actuator (`health`, `info` only) | Used by the guide's setup check. |

Generate both Spring modules with start.spring.io (Boot 4.1.2, Java 25, Maven, Jar) through its API with curl, so
starter names are correct for Boot 4's modular starters. Dependency ids (verified against
https://start.spring.io/metadata/client on 2026-11-06): `web`, `data-jpa`, `validation`, `flyway`, `postgresql`,
`docker-compose`, `testcontainers`, `actuator` (bookstore, plus `spring-ai-anthropic` for spec 08) and `web`,
`actuator` (distributor, plus the MCP server starter below). Never `h2`. *Verify at build time* that the generated
starter artifact names match Boot 4 (for example `spring-boot-starter-webmvc`).

## Storefront (spec 12)

| Item | Version | Notes |
|---|---|---|
| React | **19.3.1** | https://github.com/facebook/react/releases (2026-10-14), pinned 2026-11-06. |
| TypeScript | the version the Vite `react-ts` template pins | Do not pin separately. |
| Vite | **8.3.6** | https://github.com/vitejs/vite/releases/tag/v8.3.6 (2026-11-05), pinned 2026-11-06. Requires Node.js 20.19+ or 22.12+ (https://vite.dev/guide/), so Node.js 26 below is compatible. `base: '/shop/'`. |
| Vitest | **5.1.0** | https://github.com/vitest-dev/vitest/releases/tag/v5.1.0 (2026-11-02), pinned 2026-11-06. Supports Node.js 26. |
| React Testing Library | **@testing-library/react 16.3.3**, **@testing-library/jest-dom 7.0.1**, **@testing-library/user-event 14.6.8** | https://github.com/testing-library/react-testing-library/releases (2026-08-27), https://github.com/testing-library/jest-dom/releases (2026-08-09), https://github.com/testing-library/user-event/releases (2026-10-13), pinned 2026-11-06. |
| Node.js | **26.13.0** (Active LTS "Lithium") | https://nodejs.org/en/download and https://github.com/nodejs/Release (26.x Active LTS since 2026-10-28), pinned 2026-11-06. Installed locally into `frontend/node/` by frontend-maven-plugin; participants never install Node.js. |
| frontend-maven-plugin | **2.0.2** (`com.github.eirslett:frontend-maven-plugin`) | https://repo1.maven.org/maven2/com/github/eirslett/frontend-maven-plugin/maven-metadata.xml (release 2.0.2, 2026-07-28; the project publishes no GitHub releases), pinned 2026-11-06. Runs `npm ci`, `npm test` and `npm run build` inside `./mvnw verify`. |

## Spring AI artifacts

| Purpose | Artifact | Key properties |
|---|---|---|
| Chat model (bookstore assistant) | `org.springframework.ai:spring-ai-starter-model-anthropic` | `spring.ai.anthropic.api-key=${BOOKSTORE_AI_API_KEY:}`, `spring.ai.anthropic.chat.model=${BOOKSTORE_AI_MODEL:<current Haiku model id>}` (2.0 dropped the `.options` prefix) |
| MCP server (distributor) | `org.springframework.ai:spring-ai-starter-mcp-server-webmvc` | `spring.ai.mcp.server.protocol=STREAMABLE` (always set it; SSE is deprecated), `name=distributor-catalog`, `version=1.0.0`, `type=SYNC`, endpoint `/mcp` |

**Never name the bookstore key `ANTHROPIC_API_KEY`.** If that variable is exported in a participant's shell,
Claude Code offers to use it instead of the participant's subscription, which would bill the workshop key for
Claude Code usage. The variable is `BOOKSTORE_AI_API_KEY` everywhere (code, docs, slides, guide).

## Testing

| Item | Version | Notes |
|---|---|---|
| JUnit | Managed by Boot 4.1.2 (JUnit 6 generation) | Do not override. |
| AssertJ, Mockito, MockMvc | Managed by Boot | Use `@MockitoBean`, not the removed `@MockBean`. |
| ArchUnit | **1.5.1**, `com.tngtech.archunit:archunit-junit6` (test scope) | Not managed by Boot: pin it. Use `archunit-junit5` only if the JUnit generation turns out to be 5. |
| HTTP contract tests | `MockRestServiceServer` (spring-test) | No WireMock, no MockServer. |
| Testcontainers PostgreSQL | 2.0.5 (see above) | Used by every `@DataJpaTest` and `@SpringBootTest` in the bookstore. Docker must be running; a test is never skipped because Docker is missing. |
| Frontend tests | Vitest + React Testing Library (see above), `jsdom` environment | Run by Maven before the Vite build. |

## Agent tooling used in the workshop (not dependencies of the code)

| Tool | Version to pin for the workshop | Install |
|---|---|---|
| Claude Code | latest stable on the day (`claude --version`) | `curl -fsSL https://claude.ai/install.sh \| bash` (macOS, Linux, WSL); `irm https://claude.ai/install.ps1 \| iex` (Windows PowerShell) |
| OpenSpec | 1.16.0 | `curl -fsSL https://openspec.dev/cli/install.sh \| sh` or `npm install -g @fission-ai/openspec@1.16.0` (Node 22.12+) |
| Spec Kit (`specify` CLI) | 1.3.1 | `uv tool install specify-cli==1.3.1` (Python 3.11+, uv) |
| Agent OS | 3.0 (clone `main`, it includes post-3.0 install-script fixes) | `git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git` |
| MCP Inspector (facilitator only) | latest | `npx @modelcontextprotocol/inspector` |

## Workshop guide site

| Item | Version | Notes |
|---|---|---|
| Docusaurus | **3.10.2** | https://github.com/facebook/docusaurus/releases (2026-07-10), pinned 2026-11-06. Classic preset, docs-only mode, in `guide/`. Requires Node.js 20.0+ (https://docusaurus.io/docs/installation), so Node.js 26 is compatible. |
| Node.js | 26.13.0 (the same pin as the storefront) | Only for working on `guide/` and for its CI job (`actions/setup-node`), never for participants. |
| Hosting | GitHub Pages of this repository via GitHub Actions | Plus a PDF/offline export of the guide (see spec 11). |

## Ports

| Service | Port | Base path |
|---|---|---|
| bookstore | 8080 | `/` (redirects to `/shop/`), `/shop/` (storefront), `/books`, `/inventory`, `/orders` (REST), `/assistant` (chat), `/actuator/health` |
| distributor | 8090 | `/api` (REST), `/mcp` (MCP Streamable HTTP), `/actuator/health` |
| PostgreSQL (Docker Compose) | 5432 on 127.0.0.1 | database, user and password `bookstore` |
| Vite dev server (development only) | 5173 | proxies `/books`, `/inventory`, `/orders`, `/assistant` to 8080 |

Both services bind to `127.0.0.1` by default (`server.address`), because the MCP endpoint is unauthenticated.
