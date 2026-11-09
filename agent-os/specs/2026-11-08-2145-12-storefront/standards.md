# Standards for 12-storefront

- `agent-os/standards/architecture.md`: pointer to `docs/spec-pack/product/architecture-standard.md`, which wins over
  every skill. The bookstore root package holds `BookstoreApplication` and only spec-mandated cross-cutting
  configuration; spec 12 names `StorefrontConfig`. ArchitectureTest stays unchanged.
- `.claude/skills/hexagonal-slice`: steps 0 and 8 apply to T1 (configuration in the root package); steps 1 to 7 do not.
- `.claude/skills/layer-testing`: the root package configuration row for `StorefrontConfigTest`.
- `docs/spec-pack/product/tech-stack.md`: exact pins (React 19.3.1, Vite 8.3.6, Vitest 5.1.0, @testing-library/react
  16.3.3, @testing-library/jest-dom 7.0.1, @testing-library/user-event 14.6.8, Node.js 26.13.0, frontend-maven-plugin
  2.0.2). TypeScript is the version the create-vite react-ts template pins. Allowed additions: jsdom 31.0.0 (spec task
  4) and @testing-library/dom 10.4.2 (peer of RTL 16).
- Referee: `./mvnw clean verify` from the repo root, budget 60 seconds; over budget is an open question, never a
  reason to trim tests.
- Docs plain English, no em dashes; product files ASCII only.
