# Standards

- `CLAUDE.md` rules - referee `./mvnw clean verify` within 60 s; never skip, disable, delete or weaken a test or a
  check; docs in plain English, no em dashes; never `ANTHROPIC_API_KEY`.
- Spec 11 strategy G1 to G9 (`agent-os/specs/2026-11-08-2206-11-workshop-guide/strategy.md`) - Node with `npm ci`, no
  new dependency, no `npm audit fix`; the guide gates `check`, `typecheck`, `build`, `check:built`, `build:offline`;
  ASCII only; colours only as variables in `guide/src/css/custom.css`.
- Spec 11 QA feed-forward (`plan.md`) - the browser rig lives in a scratchpad folder with its own Playwright and
  axe-core install (FF-9), the accepted console list (FF-5), contrast pairs only grow (FF-13 b).
- D7 for the storefront only; D18 replaces D7's teal primary for the guide. Montserrat stays.
- `agent-os/standards/agent-memory.md` where present - memory curation rules for every agent.
