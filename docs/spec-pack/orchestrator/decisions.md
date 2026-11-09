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
- LSP timing (Maik's decision of 2026-11-08): the setup check installs and checks only the `jdtls` binary
  (`which jdtls` or `Get-Command jdtls`, per-OS install table). The `/plugin install` of `jdtls-lsp` happens in
  Level 2 Part A, so Levels 0 and 1 run without the plugin. This amends Maik's original D6 text "Setup check: add
  which jdtls and the plugin install". Also Maik's decision of 2026-11-08: the jdtls risk row no longer asks
  participants to open the repo at home with the plugin; they check the binary at home instead.
- The Level 0 observation card has one row per failure mode with its evidence and debrief column, so it supports the
  "at least three failure modes, each with evidence" check. Spec 11 copies it into `notes/level-0.md`.
- Rehearsal threshold (Maik's decision of 2026-11-08): spec 09's facilitator rehearsal of Level 0 must show at least
  three failure modes per run, each with evidence (the failing test, the ArchUnit output or the wrong file), the
  same bar as "Level cleared when" for Level 0. It replaces "at least two runs show at least one of".
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

Files changed: `workshop/workshop-plan.md` (format decisions, agenda, setup check with the jdtls binary,
Levels 0 to 4, Level 2 and Level 3 split, LSP part, "Level cleared when" per level, observation card, wrap-up,
risks), `workshop/level-4-feature-briefs.md` (renamed from `lab-4-feature-briefs.md` with `git mv`, title),
`workshop/sdd-framework-guide.md` (Level 4, branch names, briefs file, "Level cleared when"),
`product/roadmap.md` (guide row, level relevance column, branch and tag table, catch-up rule, milestone),
`product/mission.md` (levels, LSP in success criteria), `product/tech-stack.md` (Maven row wording),
`specs/01-foundation.md`, `specs/02-catalogue.md`, `specs/03-inventory.md`, `specs/04-orders.md`,
`specs/05-distributor-service.md`, `specs/06-distributor-acl.md`, `specs/07-restock-process.md`,
`specs/08-bookstore-assistant.md`, `specs/12-storefront.md` (level references), `specs/09-workshop-baseline.md`
(tag `level-0-start`, Level 0 traps, rehearsal threshold of three failure modes per run, level wording), `specs/10-harness-reference-solutions.md`
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

## D7. The storefront is OpenBook, in OpenValue colours

Maik (2026-11-08): the storefront (spec 12) gets a nice, modern design in OpenValue colours, and the shop is called
OpenBook. The colours come from the theme `OPENVALUE2026` of the OpenValue slides template (dk1 `#1D252D`, dk2
`#3D7DC9`, accent4 `#014046`, accent3 `#809FA2`, accent5 `#CBD9DA`, accent2 `#F1F5F6`), the font is Montserrat (the
template's heading and body font). No design-system package was available to the factory (the DesignSystem folder holds
only the slides template and a Teams background), so the tokens are written into spec 12 directly. Blue `#3D7DC9`
has 4.2:1 on white, so it is an accent colour only; teal `#014046` carries buttons and links. Montserrat is
self-hosted with its licence, no CDN, no new npm dependency.

Maik (2026-11-08, later the same day): the workshop guide site (spec 11) uses the same brand. Dark mode uses navy as
background and mist as primary (10.7:1); if a dark-mode pair cannot meet AA, the switch is turned off.

Files changed: `specs/12-storefront.md` (Visual design notes, requirement "OpenBook branding", definition of done
screenshots), `specs/11-workshop-guide-site.md` (Visual design section, requirement "OpenValue brand", task 1),
`orchestrator/decisions.md` (D7), `orchestrator/progress.md` (decision log lines).

## D8. LSP rehearsal on macOS only for now

Maik, 2026-11-09: the spec 10 LSP rehearsal focuses on macOS for now. Spec 10 task 3 and its definition of done
record the first-index time and the multi-module result on macOS only. The Linux and Windows-through-WSL runs are a
manual check for Maik before the workshop, listed in progress.md. Changed file: `specs/10-harness-reference-solutions.md`
(task 3, definition of done).

## D9. Level 3 commit hook blocks on failure; Windows needs Git Bash

Maik, 2026-11-09, on two questions from the spec 10 orchestrator:

- The Level 3 commit hook gets `"onFailure": "block"`: a hook that times out (180 s) or fails to run blocks the
  commit instead of letting it through. Verified against the hooks schema of the Claude Code version in use at build
  time; if the field does not exist, the pack's behaviour stays, the gap goes into the guide and Maik is told.
- Windows participants need Git for Windows (Git Bash). Without it the `Bash` matcher, the `if` field and the
  `Bash(...)` rules do not apply. The setup check and the guide state the prerequisite; no `PowerShell(...)` handlers
  or rules are added.

Changed file: `specs/10-harness-reference-solutions.md` (Level 3 settings and the PowerShell note). The guide (spec 11)
carries the Git Bash prerequisite on the setup and Level 3 pages.

## D10. Assistant: no temperature, and a 503 when a live call fails

Maik, 2026-11-09, after the spec 08 review:

- Spec 08 no longer configures `temperature: 0.2`. The Messages API rejects sampling parameters (`temperature`,
  `top_p`, `top_k`) with a 400 on the current models and from API version 2026-10-21 on every model, so the setting
  would break every live call. The code already sends none.
- A failed live model call (authentication, spend or rate limit at Anthropic, an Anthropic error, a timeout or a
  connection failure) answers 503 with code `assistant-unavailable` instead of Spring's generic 500, and the
  unanswered question does not stay in the conversation memory. The storefront shows "The assistant is unavailable
  right now, try again later".

Changed files: `specs/08-bookstore-assistant.md` (configuration, API table, new requirement "Failed model call"),
`specs/12-storefront.md` (assistant error messages). Implemented through /sdd-bugfix (`fix-08-assistant-review`).

## D11. Storefront: readable genre labels and a footer at the bottom of the window

Maik, 2026-11-09, during the live walk of the storefront:

- The storefront showed the raw enum names (`SCIENCE_FICTION`) in the genre select and on the book cards. The shop
  shows a readable label in sentence case instead ("Science fiction"); the select still sends the enum name to
  `GET /books`. The label casing is my default; the spec did not name one.
- The footer floated in the middle of the window on a short page. It now sits at the bottom of the window when the
  content is short, and below the content when it is long.

Changed file: `specs/12-storefront.md` (Visual design, catalogue scenario). Implemented through /sdd-bugfix
(`fix-12-genre-labels-footer`).

## D12. Seed data: at least 10 books per genre

Maik, 2026-11-09, during the live walk of the storefront: the demo catalogue needs at least 10 books in every genre,
so a genre filter never shows a near-empty page. The spec 09 seed grows from 40 to 100 books, with at least 10
AVAILABLE books per genre. The 20 books shared with the distributor stay exactly as they are; the 60 new books are
bookstore-only, invented, with ISBNs from the bookstore-only range, stocked above their reorder level, with no
pending restock. The stock, order and reserved rules of spec 09 are unchanged.

Spec 02 capped `GET /books` at 100 results without paging, so 100 seeded books plus one added in a demo would push a
book off the unfiltered list. Maik chose to raise the cap to 200 results (still sorted by title, no paging). The
assistant tools keep their own smaller limits.

Both changes are built by the spec 09 orchestrator in its normal SDD cycle (default model policy, QA and QA lead),
not by a separate agent.

Changed files: `specs/09-workshop-baseline.md` (Seed data, catalogue scenario), `specs/02-catalogue.md` (search
cap).

## D13. Assistant as a chat widget with streamed answers (deferred to after the first release)

Maik, 2026-11-09, during the live walk of the storefront: the assistant should feel like a classic chat assistant
(a launcher in the lower right corner of every page instead of a page), and answers must be streamed over a new
`POST /assistant/chat/stream`. Shortly after, Maik made it optional and deferred it to after the first release.

The requirements live in their own spec, `specs/13-assistant-widget.md`, built after the first release. Specs 08 and
12 stay as they were for the release (the amendment 3d467fc was reverted). The first release keeps the assistant page.
