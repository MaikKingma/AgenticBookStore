---
name: storefront-design-decisions
description: Spec 12 storefront rationale and frontend traps (router, oxlint, resources, no Maven edge, test style, styling contract, lockfile, inputs) for later frontend or guide work
metadata:
  type: project
---

Full strategy: lead outbox `strategy-12-storefront.md` (spec 12 cycle). Rationale worth keeping:

- **In-house `HashRouter`** (`src/router.tsx`, `useSyncExternalStore` on `hashchange`): react-router is not in the
  tech stack; seven fixed routes. Keeps the spec's component name.
- **oxlint removed**: not in the stack, not run by the referee, platform binaries in the lockfile; `tsc -b` gates.
- **bookstore resources**: re-declare the two Boot parent entries plus the unfiltered dist entry (spec literal,
  both cases probe-verified). A bare child `<resources>` drops application.yml and migrations.
- **No bookstore -> frontend Maven dependency**: it breaks `./mvnw -pl bookstore spring-boot:run` before a full
  build. Price: under `-fae` the bookstore builds after a red frontend test (build still red).
- **StorefrontConfigTest on IntegrationTestBase**: no new context, real forward and actuator; assert `Location`
  by resolving against the base URL (relative vs absolute differs between MockMvc and Tomcat).
- **Styling contract as tests**: contrast test reads tokens.css raw and auto-extracts color/background pairs from
  CSS rules; a style-contract test scans sources for colour literals. Blue (4.2:1) is never text, so "every pair
  >= 4.5" holds literally.

Frontend traps that cost rework if missed:
- Lockfile from a clean state, grep it for linux-x64 and win32-x64 native bindings (npm optional-deps bug).
- No `maxLength` on the chat input, no clamping number input, `<form noValidate>`: otherwise the over-limit and
  server-validation scenarios become untestable.
- `URLSearchParams` encodes `@`; assert parsed params for e-mails. API paths must start with `/` under base `/shop/`.
- `frontend/node/npm run dev` needs `frontend/node` on PATH (shebang `env node`).
- Browser walk-throughs and screenshots need a browser tool; plan who has one up front.

Related: [[strategy-constraints]], [[test-context-cost-model]].
