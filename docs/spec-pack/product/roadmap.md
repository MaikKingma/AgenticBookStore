# Roadmap and Repository Strategy

## Repository

One mono repository: `MaikKingma/AgenticBookStore` (https://github.com/MaikKingma/AgenticBookStore).

| Path | Purpose | Built from specs |
|---|---|---|
| `bookstore/` | Maven module: the brownfield bookstore participants work on | 01 to 04, 06 to 09, 12 (backend tasks) |
| `distributor/` | Maven module: the fake distributor with REST API and our own MCP server | 01, 05 |
| `frontend/` | React + Vite storefront, built by Maven through frontend-maven-plugin and served by the bookstore | 12 |
| `guide/` | Docusaurus guide site with all lab instructions | 11 |
| `pom.xml` (root) | Aggregates `frontend`, `bookstore` and `distributor`; `guide/` builds on its own | 01, 12 |

## Build order

Build strictly in this order. Each spec assumes the previous ones are merged and `./mvnw verify` is green.

| # | Spec | Module | Lab relevance |
|---|---|---|---|
| 01 | Foundation: multi-module build, ArchUnit, CI | both | The referee every lab relies on |
| 02 | Catalogue: books, ISBN, money, search | bookstore | Lab 1 and 2 task area (discounts) |
| 03 | Inventory: stock levels, receiving stock, low-stock event | bookstore | Domain events for the process adapter |
| 04 | Orders: place and ship orders, stock reservation | bookstore | Lab 3 skill target (new use cases) |
| 05 | Distributor service: REST API + our own MCP server | distributor | Lab 3 MCP |
| 06 | Distributor ACL: import titles, request restock | bookstore | Lab 3 MCP-assisted contract test |
| 07 | Restock process adapter | bookstore | Shows event-driven layer to agents |
| 08 | Bookstore assistant with Spring AI | bookstore | Lab 4 brief C, "AI inside the app" demo |
| 12 | Storefront: React + Vite UI served by the bookstore | frontend, bookstore | Participants see the app they harness |
| 09 | Workshop baseline: seed data, http files, README, sanitising, lab-1 tag | all | Defines the participant start state |
| 10 | Harness reference solutions and lab branches | all | Catch-up branches for every lab |
| 11 | Workshop guide site (Docusaurus) | guide | Participant-facing instructions |

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
| `main` (tag `lab-1-start`) | Full app, zero agent configuration | Lab 1 |
| `solution/lab-2` | + lean `CLAUDE.md`, `.claude/rules/*`, `CLAUDE.local.md.example` | Lab 2 catch-up / compare |
| `solution/lab-3` | + skill, command, subagent, hooks, permissions, git pre-commit hook, `.mcp.json` | Lab 3 catch-up / compare |
| `start/lab-4-openspec` | `solution/lab-3` + OpenSpec initialised and configured | Lab 4, track A |
| `start/lab-4-speckit` | `solution/lab-3` + Spec Kit initialised, constitution written | Lab 4, track B |
| `start/lab-4-agentos` | `solution/lab-3` + Agent OS installed, standards discovered and indexed | Lab 4, track C |
| `solution/lab-4-<track>-<brief>` | One shipped feature per track (facilitator reference, optional) | Wrap-up demo |

Catch-up rule for participants: `git stash -u && git switch solution/lab-N`. Nobody falls behind for more than one lab.

## Milestones (build week)

| Day | Milestone |
|---|---|
| 1 | Specs 01 to 04 merged on `factory`, verify green |
| 2 | Specs 05 to 07 merged; MCP smoke test with Claude Code passes (spec 05, requirement "Claude Code can connect") |
| 3 | Spec 08 merged with the real workshop API key tested once; spec 12 merged; spec 09 publishes `main` |
| 4 | Spec 10 branches created and each lab rehearsed once on a clean machine; spec 11 site live |
| 5 | Dry run of the full 4 hours on a second laptop (Windows + WSL if possible) |
