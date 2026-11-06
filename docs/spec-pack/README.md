# J-Fall 2026 Workshop Pack: Crafting your Agentic Harness

Everything needed to build and run the 4-hour pre-conference workshop: the specs to build the codebases with a
spec-driven development framework, the facilitator plan, the SDD track instructions, and the keynote outline.

## What is in here

| Path | What it is | Who uses it |
|---|---|---|
| `product/mission.md` | Why the Agentic Bookstore exists and what success looks like | SDD framework (product context) |
| `product/tech-stack.md` | Pinned versions and key properties (verified 2026-11-06) | SDD framework, builders |
| `product/architecture-standard.md` | Clean Hexagonal Onion rules: layers, dependencies, naming, testing | SDD framework (constitution / standard / context) |
| `product/roadmap.md` | Build order, repositories, factory branch, lab branches, build-week milestones | Maik |
| `specs/01` to `specs/11` | One spec per increment, each with Why, scope, design notes, requirements with scenarios, tests, tasks, definition of done | SDD framework, one spec per cycle |
| `workshop/workshop-plan.md` | Agenda, Labs 0 to 4 step by step, sync moments, debriefs, risks | Maik; source for the guide site |
| `workshop/sdd-framework-guide.md` | Using this pack with OpenSpec, Spec Kit or Agent OS, and the three Lab 4 tracks | Maik and participants |
| `workshop/lab-4-feature-briefs.md` | Three deliberately vague feature requests for Lab 4 | Participants |
| `keynote/keynote-outline.md` | Slide-by-slide text, speaker notes and image credits of the keynote deck | Maik |
| `factory-harness/skills/` | Project-local Claude Code skills for the factory build: `clean-hexagonal-onion`, `ddd-building-blocks`, `hexagonal-slice`, `layer-testing`. Installed into `.claude/skills/` before the first spec | Orchestrator, factory branch only |
| `orchestrator-prompt.md` | The paste-in prompt that bootstraps the mono repo and runs the SDD loop spec by spec | Maik |

Keynote deck (OpenValue design system, 31 slides): https://claude.ai/artifact/LzTLM2vMwHniX1fsdWg7ed

## How to use the specs
1. Create the `agentic-bookstore` repository with a `factory` branch and copy this pack to `docs/spec-pack/`.
2. Pick your SDD framework and follow `workshop/sdd-framework-guide.md` Part 1.
3. Run one spec per cycle, in order 01 to 10. Each spec is self-contained but assumes the previous ones are merged
   with `./mvnw verify` green.
4. Spec 09 publishes the participant `main` without any agent or SDD files. Spec 10 creates the lab branches.
5. Spec 11 builds the Docusaurus guide in a second repository.

Spec format: every requirement uses `### Requirement:` with SHALL wording and `#### Scenario:` blocks with
**GIVEN**/**WHEN**/**THEN** bullets. That is valid OpenSpec delta syntax and maps one-to-one to Spec Kit acceptance
scenarios and Agent OS shape documents.

## Decisions I made that you may want to change
| Decision | Choice in this pack | Alternative |
|---|---|---|
| Build tool | Maven wrapper, multi-module | Gradle (start.spring.io default) |
| Database | H2 in-memory for tests and local runs; optional PostgreSQL profile | Postgres + Testcontainers as default (adds Docker as a prerequisite) |
| Migrations | Flyway (plain SQL) | Liquibase, as in your earlier workshop |
| Lombok | Not used (records, Java 25) | Lombok as in the earlier workshop |
| Root package | `nl.openvalue.bookstore`, `nl.openvalue.distributor` | Any other |
| Domain | Original bookstore domain: catalogue, inventory, orders, restocking, assistant | Closer to the publishing domain of the earlier workshop |
| Distributor | Separate module, deliberately foreign model, read-only MCP tools, runs locally | Centrally hosted instance |
| Spec pack location | Copied to `docs/spec-pack/` on `factory` only; the publish script strips it from `main` (it contains the lab solutions) | Keep it in a separate private repository |
| Lab exercises kept out of the baseline | Discounts (Lab 1/2), cancel order (Lab 3), multi-role contributor contract test and `list_new_releases` MCP tool (Lab 3) | Different exercises |
| Agenda | Keynote 45 min, labs 25/35/50/45, break 15, wrap-up 15 | Longer keynote by shortening Lab 3 stretch goals |

## Verify before the build week
- Starter artifact names for Spring Boot 4 modular starters (generate with start.spring.io rather than typing them).
- Spring AI: how to keep the app starting without an API key (spec 08), and whether the webmvc MCP starter supports
  `stdio=true` (spec 05).
- MCP protocol: the current spec is 2026-07-28; Spring AI 2.0.2's Java SDK tracks 2025-11-25. Spec 05's Claude Code
  smoke test is the proof that they negotiate.
- ArchUnit artifact `archunit-junit6` versus `archunit-junit5`, depending on the JUnit generation Boot 4.1.2 ships.
- The current Haiku model id for the assistant (docs.claude.com/en/docs/about-claude/models).
- How Claude Code runs hook commands on native Windows (spec 10).
- Spring AI 2.0 tool loop location for the `ScriptedChatModel` test double (spec 08).
- Claude Code: new sessions start in auto permission mode (since v2.1.283); `/agents` no longer opens a wizard
  (since v2.1.198); AGENTS.md is read only when no CLAUDE.md exists, by default.

## Key sources
- Claude Code docs: code.claude.com/docs/en/ (memory, skills, hooks, settings, permissions, mcp, best-practices)
- Anthropic Academy: anthropic.skilljar.com (Claude Code 101, Claude Code in Action, Introduction to agent skills,
  Introduction to subagents, Introduction to Model Context Protocol, MCP Advanced Topics)
- Anthropic engineering: "Effective context engineering for AI agents" (2025-09-29), "Effective harnesses for
  long-running agents" (2025-11-26), "Harness design for long-running application development" (2026-03-24)
- Spring: docs.spring.io/spring-boot, docs.spring.io/spring-ai (getting started, chat client, tools, chat memory,
  MCP server and client starters, upgrade notes)
- SDD: github.com/Fission-AI/OpenSpec, github.com/github/spec-kit, buildermethods.com/agent-os
