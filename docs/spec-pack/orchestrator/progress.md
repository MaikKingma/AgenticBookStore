# Factory progress

Product goal: A small, realistic, test-enforced brownfield bookstore (Spring Boot 4, Java 25, Clean Hexagonal Onion) that workshop participants harness with Claude Code over four labs.
Current phase: 1, step 8
Current spec: none (bootstrap)
Factory skills installed and invocable: pending (gate at Phase 1, step 9)
Clean `./mvnw clean verify` time: n/a, no code yet (budget 60)

| Spec | Status | Tag / commit | Verify time | Evidence and notes |
|---|---|---|---|---|
| 01 | todo | | | |
| 02 | todo | | | |
| 03 | todo | | | |
| 04 | todo | | | |
| 05 | todo | | | |
| 06 | todo | | | |
| 07 | todo | | | |
| 08 | todo | | | |
| 12 | todo | | | |
| 09 | todo | | | |
| 10 | todo | | | |
| 11 | todo | | | |

## Open questions

- OQ1 (2026-11-06, Phase 1): the auto-mode classifier refused to let me write `.claude/settings.json` (self-modification). The SDD cycle asks for a Bash allowlist so unattended agents do not stall on permission prompts. The intended content is in `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/settings.json`. Please copy it to `.claude/settings.json` yourself, or tell me the subagents may run without it under auto mode.

## Manual checks for Maik

- Copy or approve `.claude/settings.json` (see OQ1).

## Decision log

- 2026-11-06 Phase 0: tool checks all pass (Java 25.0.4, Docker 29.8.2, Node 24.20.0, npm 11.19.0, git 2.50.1, gh as MaikKingma with ADMIN on MaikKingma/AgenticBookStore, start.spring.io answers).
- 2026-11-06 Phase 0: SDD tooling found and mapped, see `sdd-mapping.md`.
- 2026-11-06 Phase 0, Maik: Phase 3 is driven by the `orchestrator` agent (one run per spec, `merge: true`), not by `/sdd-cycle` in the main session.
- 2026-11-06 Phase 0, Maik: branch model is PRs into `factory`. Task PRs squash-merge into `sdd/<slug>/spec`; the spec PR merges into `factory` with a merge commit; tag `spec-NN-done` on factory after the merge.
- 2026-11-06 Phase 0, Maik: `agent-os/product` is a symlink to `../docs/spec-pack/product`; a pointer standard `agent-os/standards/architecture.md` is indexed.
- 2026-11-06 Phase 0, Maik: keep the existing clone, branch `factory` from `main` (7c45791), commit the untracked agent-os commands and standards index in the bootstrap.
- 2026-11-06 Phase 1: D1 to D5 are listed in `decisions.md`; they are applied in Phase 2.
