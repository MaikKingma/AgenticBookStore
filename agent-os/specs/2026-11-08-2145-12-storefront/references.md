# References for 12-storefront

- Pack spec: `docs/spec-pack/specs/12-storefront.md` (factory f6ce0d7, with D7).
- Decisions: `docs/spec-pack/orchestrator/decisions.md` D3 (React storefront) and D7 (OpenBook design).
- Spec 08 contract text: `docs/spec-pack/specs/08-bookstore-assistant.md` (`POST /assistant/chat`).
- Wire contract of specs 02 to 04: the bookstore code under `bookstore/src/main/java/nl/openvalue/bookstore/`
  (views, payloads, the exception handlers); summarised in `shape.md`.
- Root-package configuration precedent: `ClockConfig` (spec 04).
- Shared real-server test base: `IntegrationTestBase` in `bookstore/src/test/java`.
- CI workflow and its properties: `.github/workflows/ci.yml`; caching follow-ups for this spec in
  `agent-os/specs/2026-11-08-1217-ci-performance/shape.md` ("Follow-ups").
- frontend-maven-plugin: https://github.com/eirslett/frontend-maven-plugin
- create-vite 9.3.4 react-ts template; Vitest https://vitest.dev; Testing Library https://testing-library.com
- WCAG 2.2 contrast: https://www.w3.org/TR/WCAG22/#contrast-minimum (logotype exemption in SC 1.4.3).
