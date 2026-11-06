# Decisions D1 to D5

Applied in Phase 2 to `docs/spec-pack/` only. The files changed per decision are filled in as the edits land.

## D1. Mono repo

One repository `MaikKingma/AgenticBookStore`. Maven modules `bookstore` and `distributor`, React + Vite storefront in `frontend/`, Docusaurus guide in `guide/`. Root `pom.xml` aggregates `bookstore`, `distributor` and `frontend`. Every `agentic-bookstore`, `agentic-harness-workshop` and `<org>` placeholder is replaced.

Files changed: `product/roadmap.md` (repository table, build order with spec 12, factory branch PR model, milestones), `README.md` (clone step, build order, decisions table), `specs/01-foundation.md` (header, tree), `specs/11-workshop-guide-site.md` (folder `guide/`, paths, deployment workflow), `workshop/workshop-plan.md` (clone command)

## D2. PostgreSQL only

H2 removed everywhere. Local runs use `compose.yaml` with PostgreSQL plus Spring Boot Docker Compose support. Tests use Testcontainers PostgreSQL with `@ServiceConnection`. Start.spring.io ids: drop `h2`; add `postgresql`, `docker-compose`, `testcontainers` (all three verified against https://start.spring.io/metadata/client on 2026-11-06). Docker becomes a participant prerequisite. Verify time is tracked after every spec against the 60 second budget.

Files changed: `product/tech-stack.md` (database rows, dependency ids, Testcontainers row, ports), `product/architecture-standard.md` (test table, verify budget, no in-memory database rule), `specs/01-foundation.md` (compose.yaml, datasource, PostgresTestConfig, requirements, tasks, definition of done), `specs/09-workshop-baseline.md` (seed SQL, README prerequisites and run instructions), `specs/10-harness-reference-solutions.md` (reference CLAUDE.md and testing rule), `workshop/workshop-plan.md` (Lab 0 Docker check, prerequisites, fallback kit, risk table), `README.md` (decisions table)

## D3. React storefront, new spec 12

`specs/12-storefront.md` in the structure of the other specs. React with TypeScript, Vite (react-ts template), Vitest and React Testing Library. Served as static files by the bookstore under a base path such as `/shop/`, with `/` redirecting there. Built by `./mvnw clean verify` through frontend-maven-plugin with a pinned local Node.js. Spec 08 keeps the `/assistant/chat` endpoint and drops its static page. Build order: after 08, before 09. Written by the orchestrator in the pack structure: the mapped shaping command (`/agent-os:shape-spec`, used inside the `orchestrator` agent) produces `agent-os/specs/<folder>/plan.md` artefacts, not pack specs, so it runs for spec 12 in Phase 3 like for every other spec.

Files changed: `specs/12-storefront.md` (new), `specs/08-bookstore-assistant.md` (static page dropped, live check over HTTP), `product/tech-stack.md` (storefront table, Node.js, ports), `product/roadmap.md` (build order), `specs/09-workshop-baseline.md` (README storefront section, local run scenario), `specs/10-harness-reference-solutions.md` (reference CLAUDE.md), `workshop/workshop-plan.md` (Lab 1 start note), `README.md` (decisions table)

## D4. Unchanged

Java 25 (LTS), Maven wrapper, Spring Boot 4.1.2, Spring AI 2.0.2. Modules generated with start.spring.io through curl; never hand-written starter POMs.

Files changed: none. `product/tech-stack.md` keeps Java 25, Maven wrapper, Spring Boot 4.1.2 and Spring AI 2.0.2; the start.spring.io step now says curl explicitly.

## D5. Keep main clean

Every folder the SDD commands write to (see `sdd-mapping.md`) is added to spec 09, both to the list of paths stripped from `main` and to the CI guard: `.claude/` with `agent-memory/`, `comms/`, `worktrees/`, `commands/`, `skills/`, `settings.json`; `agent-os/` with the `product` symlink and `specs/`; `CLAUDE.md`; `docs/spec-pack/`.

Files changed: `specs/09-workshop-baseline.md` (publish script path list, CI guard, publishing scenario), `product/roadmap.md` (factory branch section)
