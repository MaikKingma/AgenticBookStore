# Standards: spec 11, workshop guide site

- `agent-os/standards/architecture.md` (pointer to the Clean Hexagonal Onion standard): not applicable to `guide/`
  code; `reference/architecture.md` restates `docs/spec-pack/product/architecture-standard.md` for participants.
- Repo rules from `CLAUDE.md`: the referee `./mvnw clean verify` stays green; never skip, disable, delete or weaken a
  test or a check rule to get to green; never `--no-verify`; never force-push; plain English with no em dashes; the
  app's key is `BOOKSTORE_AI_API_KEY`, never `ANTHROPIC_API_KEY` (the troubleshooting page may warn about that
  pitfall, never instruct to set it); the word "lab" is not used.
- Tech stack pins (`docs/spec-pack/product/tech-stack.md`): Docusaurus 3.10.2, Node.js 26.13.0; no dependency beyond
  the pinned Docusaurus packages and what the official TypeScript template brings.
- CI workflow conventions from `.github/workflows/ci.yml` and the ci-performance proposals
  (`agent-os/specs/2026-11-08-1217-ci-performance/`): every action pinned to a full commit SHA with a `# vX.Y.Z`
  comment, `timeout-minutes` on every job, minimal per-job permissions, the same concurrency pattern, ASCII only, no
  masking flags, gates only on agent-readable evidence.
- Ground rules G1 to G9 of `strategy.md` (toolchain, standard gates, ASCII, MDX safety, no fact without a pack source,
  no internal paths on participant pages, checker ownership, git read-only for agents, evidence).
