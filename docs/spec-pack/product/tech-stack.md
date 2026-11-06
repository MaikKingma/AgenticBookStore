# Tech Stack

> Versions verified against vendor release pages on 2026-11-06. Pin them; do not "upgrade to latest" during the
> build week. Re-check only the items marked *verify at build time*.

## Runtime and build

| Item | Version / choice | Notes |
|---|---|---|
| Java | **25** (Eclipse Temurin 25 LTS) | `maven.compiler.release=25`. Boot 4.1.x supports Java 17 up to 27. |
| Build | **Maven** via wrapper (`./mvnw`, `mvnw.cmd`), multi-module | Maven chosen over the start.spring.io default (Gradle) because the hooks, docs and labs all assume `./mvnw`. |
| Spring Boot | **4.1.2** | Do not move to 4.2.x (RC at the time of writing). |
| Spring AI | **2.0.2** via `org.springframework.ai:spring-ai-bom:2.0.2` | 2.0.x supports Boot 4.0.x and 4.1.x. Avoid 2.1 (RC, deprecates chat memory APIs). |
| Database (default) | **H2** in-memory, for tests and local runs | Zero-install, fresh state on every restart. Docker is NOT a workshop prerequisite. |
| Database (optional) | PostgreSQL via `postgres` profile + `compose.yaml` | For participants who want realism. Never required by a test. |
| Migrations | **Flyway** (plain SQL, `V{n}__{description}.sql`) | `spring.jpa.hibernate.ddl-auto=validate` everywhere. |
| JSON | Jackson 3 (Boot 4 default) | No custom ObjectMapper unless a spec asks for it. |
| HTTP client (acl) | Spring `RestClient` | No WebClient, no Feign, no RestTemplate. |
| Validation | Jakarta Validation (`spring-boot-starter-validation`) | On `*Payload` records only. |
| Observability | Spring Boot Actuator (`health`, `info` only) | Used by the guide's setup check. |

Generate both modules with start.spring.io (Boot 4.1.2, Java 25, Maven, Jar) so starter names are correct for
Boot 4's modular starters. Dependency ids: `web`, `data-jpa`, `validation`, `flyway`, `h2`, `actuator`
(bookstore, plus `spring-ai-anthropic` for spec 08) and `web`, `actuator` (distributor, plus the MCP server starter
below). *Verify at build time* that the generated starter artifact names match Boot 4 (for example
`spring-boot-starter-webmvc`).

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
| Testcontainers | 2.0.5 (only in the optional `postgres` profile tests, tagged `@Tag("postgres")`, excluded by default) | Module names are prefixed `testcontainers-` in 2.x. |

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
| Docusaurus | latest 3.x stable at build time (*verify*) | Classic preset, docs-only mode. |
| Node.js | 22 LTS | Only for the guide repository, never for participants. |
| Hosting | GitHub Pages via GitHub Actions | Plus a PDF/offline export of the guide (see spec 11). |

## Ports

| Service | Port | Base path |
|---|---|---|
| bookstore | 8080 | `/` (REST), `/assistant` (chat), `/actuator/health` |
| distributor | 8090 | `/api` (REST), `/mcp` (MCP Streamable HTTP), `/actuator/health` |

Both services bind to `127.0.0.1` by default (`server.address`), because the MCP endpoint is unauthenticated.
