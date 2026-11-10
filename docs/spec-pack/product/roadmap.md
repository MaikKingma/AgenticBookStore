# Roadmap and Repository Strategy

## Repository

One mono repository: `MaikKingma/AgenticBookStore` (https://github.com/MaikKingma/AgenticBookStore).

| Path | Purpose | Built from specs |
|---|---|---|
| `bookstore/` | Maven module: the brownfield bookstore participants work on | 01 to 04, 06 to 09, 12 (backend tasks) |
| `distributor/` | Maven module: the fake distributor with REST API and our own MCP server | 01, 05 |
| `frontend/` | React + Vite storefront, built by Maven through frontend-maven-plugin and served by the bookstore | 12 |
| `guide/` | Docusaurus guide site with all level instructions | 11 |
| `pom.xml` (root) | Aggregates `frontend`, `bookstore` and `distributor`; `guide/` builds on its own | 01, 12 |

## Build order

Build strictly in this order. Each spec assumes the previous ones are merged and `./mvnw verify` is green.

| # | Spec | Module | Level relevance |
|---|---|---|---|
| 01 | Foundation: multi-module build, ArchUnit, CI | both | The referee every level relies on |
| 02 | Catalogue: books, ISBN, money, search | bookstore | Level 0 and 1 task area (discounts); `Money.of` is the Level 2 find-references target |
| 03 | Inventory: stock levels, receiving stock, low-stock event | bookstore | Domain events for the process adapter |
| 04 | Orders: place and ship orders, stock reservation | bookstore | Level 3 skill target (new use cases) |
| 05 | Distributor service: REST API + our own MCP server | distributor | Level 2 MCP |
| 06 | Distributor ACL: import titles, request restock | bookstore | Level 2 MCP-assisted contract test |
| 07 | Restock process adapter | bookstore | Shows event-driven layer to agents |
| 08 | Bookstore assistant with Spring AI | bookstore | Level 4 brief C, "AI inside the app" demo |
| 12 | Storefront: React + Vite UI served by the bookstore | frontend, bookstore | Participants see the app they harness |
| 09 | Workshop baseline: seed data, http files, README, sanitising, level-0 tag | all | Defines the participant start state |
| 10 | Harness reference solutions and level branches | all | Catch-up branches for every level |
| 11 | Workshop guide site (Docusaurus) | guide | Participant-facing instructions |
| 13 | Assistant chat widget with streamed answers (after the first release, decision D13) | bookstore, frontend | Optional; not part of the first release |
| 14 | Guide header links, OpenValue logo, breadcrumb level bar and deck colours (decision D18) | guide | The guide matches the keynote deck |
| 15 | Guide content revision after Maik's review (decision D19) | guide | Clearer steps, no sync moments, framework getting-started order |

## The factory branch (important)

The app is built *with* agents and SDD tooling, but participants must start from a codebase with **no** agent
configuration. Therefore:

- Build everything on a long-lived branch `factory`. SDD artefacts (`openspec/`, `.specify/`, `specs/`,
  `agent-os/`), `CLAUDE.md`, `.claude/` (including `.claude/agent-memory/`, `.claude/comms/`, `.claude/worktrees/`),
  `.mcp.json`, `AGENTS.md` and `docs/spec-pack/` are allowed there.
- Spec work lands on `factory` through pull requests: one spec branch `sdd/<slug>/spec` per spec with one task
  branch and one task PR per task, squash-merged into the spec branch; the spec PR is merged into `factory` with a
  merge commit, then tagged `spec-<NN>-done`.
- Spec 09 adds `scripts/publish-workshop-main.sh`, which produces `main` from `factory` as a single squashed commit
  without any of those paths, and a CI job on `main` that fails if any of them reappear.

## Branches and tags participants will use

| Ref | Content | Used in |
|---|---|---|
| `main` (tag `level-0-start`) | Full app, zero agent configuration | Level 0, and the start of Level 1 |
| `solution/level-1` | + lean `CLAUDE.md`, `.claude/rules/*`, `CLAUDE.local.md.example` | Level 1 catch-up / compare |
| `solution/level-2` | `solution/level-1` + `.mcp.json`, the MCP-assisted ACL contract test, the LSP setup note (`docs/lsp-setup.md`) and the `jdtls-lsp` plugin declaration in `.claude/settings.json` | Level 2 catch-up / compare |
| `solution/level-3` | `solution/level-2` + permissions, hooks, git pre-commit hook, skill, command, subagent (no PowerShell hook variants, D16) | Level 3 catch-up / compare |
| `start/level-4-openspec` | `solution/level-3` + OpenSpec initialised and configured | Level 4, track A |
| `start/level-4-speckit` | `solution/level-3` + Spec Kit initialised, constitution written | Level 4, track B |
| `start/level-4-agentos` | `solution/level-3` + Agent OS installed, standards discovered and indexed | Level 4, track C |

Catch-up rule for participants, at the start of each level: Level 0 `git stash -u && git switch -C level-0 level-0-start`, Level 1 `git stash -u && git switch -C level-1 level-0-start`, Levels 2 and 3 `git stash -u && git switch solution/level-N` (N one lower than the level), Level 4 `git stash -u && git switch start/level-4-<track>`. Nobody falls behind for more than one level. There are no Level 4 solution branches (D17).

## Milestones (build week)

| Day | Milestone |
|---|---|
| 1 | Specs 01 to 04 merged on `factory`, verify green |
| 2 | Specs 05 to 07 merged; MCP smoke test with Claude Code passes (spec 05, requirement "Claude Code can connect") |
| 3 | Spec 08 merged with the real workshop API key tested once; spec 12 merged; spec 09 publishes `main` |
| 4 | Spec 10 branches created and each level rehearsed once on a clean machine, LSP on macOS, Linux and WSL; spec 11 site live |
| 5 | Dry run of the full 4 hours on a second laptop (Windows + WSL if possible) |
