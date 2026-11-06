# Decisions D1 to D5

Applied in Phase 2 to `docs/spec-pack/` only. The files changed per decision are filled in as the edits land.

## D1. Mono repo

One repository `MaikKingma/AgenticBookStore`. Maven modules `bookstore` and `distributor`, React + Vite storefront in `frontend/`, Docusaurus guide in `guide/`. Root `pom.xml` aggregates `bookstore`, `distributor` and `frontend`. Every `agentic-bookstore`, `agentic-harness-workshop` and `<org>` placeholder is replaced.

Files changed: pending (Phase 2)

## D2. PostgreSQL only

H2 removed everywhere. Local runs use `compose.yaml` with PostgreSQL plus Spring Boot Docker Compose support. Tests use Testcontainers PostgreSQL with `@ServiceConnection`. Start.spring.io ids: drop `h2`; add `postgresql`, `docker-compose`, `testcontainers`. Docker becomes a participant prerequisite. Verify time is tracked after every spec against the 60 second budget.

Files changed: pending (Phase 2)

## D3. React storefront, new spec 12

`specs/12-storefront.md` in the structure of the other specs. React with TypeScript, Vite (react-ts template), Vitest and React Testing Library. Served as static files by the bookstore under a base path such as `/shop/`, with `/` redirecting there. Built by `./mvnw clean verify` through frontend-maven-plugin with a pinned local Node.js. Spec 08 keeps the `/assistant/chat` endpoint and drops its static page. Build order: after 08, before 09.

Files changed: pending (Phase 2)

## D4. Unchanged

Java 25 (LTS), Maven wrapper, Spring Boot 4.1.2, Spring AI 2.0.2. Modules generated with start.spring.io through curl; never hand-written starter POMs.

Files changed: none expected

## D5. Keep main clean

Every folder the SDD commands write to (see `sdd-mapping.md`) is added to spec 09, both to the list of paths stripped from `main` and to the CI guard.

Files changed: pending (Phase 2)
