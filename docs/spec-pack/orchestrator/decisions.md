# Decisions D1 to D6

Applied in Phase 2 to `docs/spec-pack/` only. The files changed per decision are filled in as the edits land.

## D1. Mono repo

One repository `MaikKingma/AgenticBookStore`. Maven modules `bookstore` and `distributor`, React + Vite storefront in `frontend/`, Docusaurus guide in `guide/`. Root `pom.xml` aggregates `bookstore`, `distributor` and `frontend`. Every `agentic-bookstore`, `agentic-harness-workshop` and `<org>` placeholder is replaced.

Files changed: `product/roadmap.md` (repository table, build order with spec 12, factory branch PR model, milestones), `README.md` (clone step, build order, decisions table), `specs/01-foundation.md` (header, tree), `specs/11-workshop-guide-site.md` (folder `guide/`, paths, deployment workflow), `workshop/workshop-plan.md` (clone command)

## D2. PostgreSQL only

H2 removed everywhere. Local runs use `compose.yaml` with PostgreSQL plus Spring Boot Docker Compose support. Tests use Testcontainers PostgreSQL with `@ServiceConnection`. Start.spring.io ids: drop `h2`; add `postgresql`, `docker-compose`, `testcontainers` (all three verified against https://start.spring.io/metadata/client on 2026-11-06). Docker becomes a participant prerequisite. Verify time is tracked after every spec against the 60 second budget.

Files changed: `product/tech-stack.md` (database rows, dependency ids, Testcontainers row, ports), `product/architecture-standard.md` (test table, verify budget, no in-memory database rule), `specs/01-foundation.md` (compose.yaml, datasource, PostgresTestConfig, requirements, tasks, definition of done), `specs/09-workshop-baseline.md` (seed SQL, README prerequisites and run instructions), `specs/10-harness-reference-solutions.md` (reference CLAUDE.md and testing rule), `workshop/workshop-plan.md` (setup check Docker check, prerequisites, fallback kit, risk table), `README.md` (decisions table)

## D3. React storefront, new spec 12

`specs/12-storefront.md` in the structure of the other specs. React with TypeScript, Vite (react-ts template), Vitest and React Testing Library. Served as static files by the bookstore under a base path such as `/shop/`, with `/` redirecting there. Built by `./mvnw clean verify` through frontend-maven-plugin with a pinned local Node.js. Spec 08 keeps the `/assistant/chat` endpoint and drops its static page. Build order: after 08, before 09. Written by the orchestrator in the pack structure: the mapped shaping command (`/agent-os:shape-spec`, used inside the `orchestrator` agent) produces `agent-os/specs/<folder>/plan.md` artefacts, not pack specs, so it runs for spec 12 in Phase 3 like for every other spec.

Files changed: `specs/12-storefront.md` (new), `specs/08-bookstore-assistant.md` (static page dropped, live check over HTTP), `product/tech-stack.md` (storefront table, Node.js, ports), `product/roadmap.md` (build order), `specs/09-workshop-baseline.md` (README storefront section, local run scenario), `specs/10-harness-reference-solutions.md` (reference CLAUDE.md), `workshop/workshop-plan.md` (Level 0 start note), `README.md` (decisions table)

## D4. Unchanged

Java 25 (LTS), Maven wrapper, Spring Boot 4.1.2, Spring AI 2.0.2. Modules generated with start.spring.io through curl; never hand-written starter POMs.

Files changed: none. `product/tech-stack.md` keeps Java 25, Maven wrapper, Spring Boot 4.1.2 and Spring AI 2.0.2; the start.spring.io step now says curl explicitly.

## D5. Keep main clean

Every folder the SDD commands write to (see `sdd-mapping.md`) is added to spec 09, both to the list of paths stripped from `main` and to the CI guard: `.claude/` with `agent-memory/`, `comms/`, `worktrees/`, `commands/`, `skills/`, `settings.json`; `agent-os/` with the `product` symlink and `specs/`; `CLAUDE.md`; `docs/spec-pack/`.

Files changed: `specs/09-workshop-baseline.md` (publish script path list, CI guard, publishing scenario), `product/roadmap.md` (factory branch section)

## D6. Workshop levels 0 to 4

Change request from Maik (2026-11-08). The workshop is framed as levels that build on each other, matching the four
levels of agentic harness engineering from the J-Spring talk. Old Lab 3 is split so there is one level per lab, and
LSP becomes hands-on for every participant.

| New name | Was | Minutes |
|---|---|---|
| Setup check | Lab 0 | 10 |
| Level 0: Break the agent | Lab 1 | 25 |
| Level 1: Context engineering | Lab 2 | 35 |
| Level 2: Give your agent eyes (LSP + MCP) | Lab 3 Part D, plus a new LSP part | 20 |
| Level 3: Behavioral control (permissions, hooks, skills, commands, subagents) | Lab 3 Parts A to C | 30 |
| Level 4: Spec-driven development | Lab 4 | 45 |

Agenda: 0:00 setup, 0:10 keynote, 0:55 L0, 1:20 L1, 1:55 break, 2:10 L2, 2:30 L3, 3:00 L4, 3:45 wrap-up (240
minutes). Every level ends with "Level cleared when". Refs: tag `level-0-start`; `solution/level-1`;
`solution/level-2` (level 1 plus `.mcp.json`, the MCP-assisted ACL contract test, the LSP setup note);
`solution/level-3` (level 2 plus permissions, hooks, git hook, skill, command, subagent, PowerShell hook variants);
`start/level-4-<track>` built from `solution/level-3`; `solution/level-4-<track>-<brief>`. Catch-up:
`git stash -u && git switch solution/level-N`.

Choices made while applying D6:
- Level 2 Part B is old Part D trimmed: start the distributor, `claude mcp add ... --scope project`, the `/mcp`
  check, one MCP question, and the ACL contract test task stay; the `CatalogMcpTools.java` step is a one-line
  pointer; the `list_new_releases` MCP tool is the Level 2 stretch.
- Level 2 sync (3 min): the LSP install check and `claude mcp add` with one MCP question (the MCP half of the old
  Lab 3 sync). Level 3 sync (4 min): the hook demo. The review subagent is the Level 3 stretch; the Stop hook stays
  optional in Level 3.
- Solo minutes: Level 2 Part A LSP 8, Part B MCP 7; Level 3 permissions 4, hooks 11, skill and command 9.
- The setup check installs the `jdtls` binary and the `jdtls-lsp` plugin at user scope, so nothing lands in the
  repository before Level 0. Level 2 Part A starts with checking both and installs the plugin if it was skipped.
- The Level 0 observation card has one row per failure mode with its evidence and debrief column, so it supports the
  "at least three failure modes, each with evidence" check. Spec 11 copies it into `notes/level-0.md`.
- Old references to Lab 3 are mapped by content: cancel order, hooks, permissions, skill and command go to Level 3;
  the distributor MCP, the multi-role contributor contract test and `list_new_releases` go to Level 2.
- The LSP setup note lives in `docs/lsp-setup.md` on `solution/level-2`.
- Plugin declaration in project settings: documented (https://code.claude.com/docs/en/settings-reference#enabledplugins,
  https://code.claude.com/docs/en/settings-reference#extraknownmarketplaces,
  https://code.claude.com/docs/en/plugins/org#require-plugins-per-repository, checked 2026-11-08). Spec 10 puts
  `extraKnownMarketplaces` (`claude-plugins-official`) and `enabledPlugins` (`jdtls-lsp@claude-plugins-official`)
  in `.claude/settings.json` on `solution/level-2`. The docs describe loading after workspace trust for a
  relative-path plugin such as `jdtls-lsp`, not an install prompt; spec 10 re-verifies it and falls back to the
  install command.
- Spec 11: one page per level; Level 3 parts are sections and Level 4 tracks are tabs on the same page; a static
  `LevelProgress` component; no accounts, no backend, no stored progress.
- `ENABLE_LSP_TOOL` is not used anywhere.

Files changed: `workshop/workshop-plan.md` (format decisions, agenda, setup check with jdtls and plugin install,
Levels 0 to 4, Level 2 and Level 3 split, LSP part, "Level cleared when" per level, observation card, wrap-up,
risks), `workshop/level-4-feature-briefs.md` (renamed from `lab-4-feature-briefs.md` with `git mv`, title),
`workshop/sdd-framework-guide.md` (Level 4, branch names, briefs file, "Level cleared when"),
`product/roadmap.md` (guide row, level relevance column, branch and tag table, catch-up rule, milestone),
`product/mission.md` (levels, LSP in success criteria), `product/tech-stack.md` (Maven row wording),
`specs/01-foundation.md`, `specs/02-catalogue.md`, `specs/03-inventory.md`, `specs/04-orders.md`,
`specs/05-distributor-service.md`, `specs/06-distributor-acl.md`, `specs/07-restock-process.md`,
`specs/08-bookstore-assistant.md`, `specs/12-storefront.md` (level references), `specs/09-workshop-baseline.md`
(tag `level-0-start`, Level 0 traps and rehearsal, level wording), `specs/10-harness-reference-solutions.md`
(branch names, `solution/level-2` with `.mcp.json`, plugin declaration, LSP setup note, ACL contract test
reference, LSP requirement and scenarios; `solution/level-3`; rehearsal tasks; definition of done),
`specs/11-workshop-guide-site.md` (one page per level, progress indicator, "Level cleared when" checklists,
observation card, per-OS jdtls on the setup page, level coverage requirement), `README.md` (file table, level
wording, decisions table, verify list), `orchestrator/decisions.md` (D6, level names in D2 and D3).
Outside the pack, in a separate commit: `.claude/skills/layer-testing/SKILL.md` (Level 3 commit hook),
`.claude/agent-memory/tech-lead-strategist/distributor-design-decisions.md`, and the historical spec folders
`agent-os/specs/2026-11-06-2155-01-foundation/shape.md`, `agent-os/specs/2026-11-07-0122-02-catalogue/shape.md`,
`agent-os/specs/2026-11-07-0957-03-inventory/shape.md`, `agent-os/specs/2026-11-07-1724-04-orders/` (`shape.md`,
`standards.md`, `references.md`, `plan.md`), `agent-os/specs/2026-11-08-1040-05-distributor/shape.md`.
