# Factory progress

Product goal: A small, realistic, test-enforced brownfield bookstore (Spring Boot 4, Java 25, Clean Hexagonal Onion) that workshop participants harness with Claude Code over four labs.
Current phase: 3, step 0 (precondition check)
Current spec: none (bootstrap)
Factory skills installed and invocable: yes (all four invoked on 2026-11-06, Phase 1 step 9)
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

- OQ4 (2026-11-06, Phase 2, D2): the frontend build (Node.js download on the first run, `npm ci`, Vitest, Vite build) will likely push `./mvnw clean verify` past the 60 second budget on later specs. Not a decision yet; recorded so it is no surprise when the measurement after spec 12 lands here.

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
- 2026-11-06 Phase 2, Maik (OQ2 closed): the Windows CI job is compile-only (`mvnw.cmd -B -DskipTests verify`); database tests run on the Linux job. Applied to spec 01.
- 2026-11-06 Phase 2, Maik (OQ3 closed): the guide deploys to GitHub Pages from `factory` on pushes touching `guide/**`. Applied to spec 11.
- 2026-11-06 Phase 2, Maik (OQ1): Maik copies `.claude/settings.json` in himself; it is committed when present.
- 2026-11-06 Phase 2 cross-check (read-only subagent over all specs, product files, workshop files and the four skills): no leftover H2, second repository, old build order, static chat page or placeholder. It found 20 cross-file inconsistencies and 15 skill-versus-spec disagreements; all fixed in the pack copy and the skills, except these, deliberately left: the architecture standard's header and the sdd-framework-guide still name `agent-os/standards/architecture/clean-hexagonal-onion.md` and `/plan-product` for the participants' Lab 4 track C branch (that is the participants' Agent OS install, not the factory's pointer standard); spec 08's system prompt sentence about pointing customers to the shop (product text, now that a shop exists Maik may want to revisit it); the OpenSpec CLI as a Track A prerequisite not listed in Lab 0 (pre-existing, outside D1 to D5); the standard's sentence "every external failure is translated into a port exception" versus spec 06's `Optional.empty()` for a 404 (the skills now carry the spec 06 exception; the standard was not changed).
- 2026-11-06 Phase 2, judgment calls: (1) the architecture standard section 1 now states that the bookstore root package holds `BookstoreApplication` and only spec-mandated cross-cutting configuration (spec 04 `Clock`, spec 12 `StorefrontConfig`). This is the one change to the standard; D3 requires a home for the serving code, and spec 04 already established the practice. (2) The skills' "never edit ArchitectureTest" became "never weaken or remove a rule", because specs 02, 06 and 07 add rules, a fixture and the one named exception. (3) The ddd skill's Clock rule now defers to the spec signatures and names how the Clock reaches aggregates whose spec signature has none (spec 03 events, spec 02 publication year). (4) The onion skill's package table puts the assistant row before command and query, so spec 08's `AssistantCommands` lands in `assistant`.
- 2026-11-06 Phase 2: versions pinned in tech-stack.md from the official release pages (React 19.3.1, Vite 8.3.6, Vitest 5.1.0, RTL 16.3.3, jest-dom 7.0.1, user-event 14.6.8, Node.js 26.13.0 Active LTS, frontend-maven-plugin 2.0.2, postgres:18.6, Testcontainers 2.0.5 as managed by Boot 4.1.2, Docusaurus 3.10.2). Compatibility: Vite 8 and Docusaurus 3.10 accept Node.js 26; Boot 4.1.2 manages Testcontainers 2.0.5.
- 2026-11-06 Phase 2: spec 12 written by the orchestrator in the pack structure (see decisions.md D3 for why the shaping command was not used for it).
- 2026-11-06 Phase 2: storefront uses hash routing and one `StorefrontConfig` in the bookstore root package (the standard allows spec-mandated cross-cutting configuration there, like spec 04's Clock bean). Frontend output reaches the bookstore as a Maven resource from `frontend/dist`, not as a jar dependency, so `./mvnw -pl bookstore spring-boot:run` keeps working.
- 2026-11-06 Phase 1: bootstrap committed as 1cd9170 and pushed to origin/factory. GitHub default branch stays main. Skills gate passed: hexagonal-slice, clean-hexagonal-onion, ddd-building-blocks and layer-testing all appear in the session and load.
