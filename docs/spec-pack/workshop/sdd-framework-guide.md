# Spec-Driven Development: OpenSpec, Spec Kit and Agent OS

Two audiences use this file:
- **Part 1**: Maik, building the Agentic Bookstore from this spec pack with the SDD framework of his choice.
- **Part 2**: participants in Level 4, choosing one of three tracks.

Versions verified on 2026-11-06: OpenSpec 1.16.0, Spec Kit (`specify-cli`) 1.3.1, Agent OS 3.0. All three install
into Claude Code as slash commands and skills. Re-check versions the day before the workshop and pin them.

## 0. The three frameworks at a glance

| | OpenSpec | Spec Kit | Agent OS |
|---|---|---|---|
| Maintainer | Fission-AI, MIT | GitHub, MIT | Builder Methods (Brian Casel) |
| Install needs | Standalone binary (no Node), or Node 22.12+ for npm | Python 3.11+ and uv | bash and git (Windows: WSL or Git Bash) |
| Weight | Light: propose, apply, archive | Heavier: constitution plus several gated steps | Light: standards plus Claude Code plan mode |
| Brownfield | Built for it: specs grow one change at a time as deltas | Supported: constitution, then spec the next bounded change | Supported: discover standards from the existing code |
| Per feature you get | `openspec/changes/<name>/` with proposal.md, design.md, tasks.md and delta specs; archived into `openspec/specs/` | `specs/NNN-name/` with spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md, tasks.md, checklists/ | `agent-os/specs/YYYY-MM-DD-HHMM-name/` with plan.md, shape.md, standards.md, references.md |
| Project conventions live in | `openspec/config.yaml` (`context`, `rules`) | `.specify/memory/constitution.md` | `agent-os/standards/` and `agent-os/product/` |
| Best for | Evolving an existing codebase with a living spec | Larger or regulated features, teams that want gates | Teams whose main problem is "the agent ignores our conventions" |
| Beginner time to first feature (estimate) | 30 to 45 min | 60 to 90 min (less on a prepared branch with the short path) | 30 to 45 min |

How this pack maps onto each:

| Pack file | OpenSpec | Spec Kit | Agent OS |
|---|---|---|---|
| `product/mission.md` | `config.yaml` context (summary) | constitution preamble / `/speckit-specify` background | `agent-os/product/mission.md` |
| `product/tech-stack.md` | `config.yaml` context | `/speckit-plan` input | `agent-os/product/tech-stack.md` |
| `product/architecture-standard.md` | `config.yaml` context + rules | constitution principles | `agent-os/standards/architecture/clean-hexagonal-onion.md` |
| `product/roadmap.md` | order of changes | order of features | `agent-os/product/roadmap.md` |
| `specs/NN-*.md` Why / What changes / Out of scope | `proposal.md` | `/speckit-specify` input | `/shape-spec` input (shape.md) |
| Requirements with Scenarios | delta spec (`## ADDED Requirements`, `### Requirement:`, `#### Scenario:`) | user stories with Given/When/Then, FR-nnn | shape.md scope and decisions |
| Design notes, API, persistence | `design.md` | `/speckit-plan` input | plan.md |
| Tasks, Tests, Definition of done | `tasks.md` | `/speckit-tasks` | plan.md tasks (Claude Code todo list) |

The requirement and scenario headings in this pack already use OpenSpec's exact syntax (`### Requirement:` and
`#### Scenario:` with bold GIVEN/WHEN/THEN bullets), so they paste into delta specs unchanged and read naturally as
Spec Kit acceptance scenarios.

---

## Part 1: Building the bookstore from this pack

Work on the `factory` branch (see `product/roadmap.md`); `main` is published later without any SDD artefacts.
Copy this pack into the repository as `docs/spec-pack/` on `factory` so every command can reference it.

### With OpenSpec
```bash
curl -fsSL https://openspec.dev/cli/install.sh | sh      # or: npm install -g @fission-ai/openspec@1.16.0
openspec config set telemetry.enabled false              # optional, telemetry is on by default
openspec init --tools claude
```
1. Put a summary of `product/tech-stack.md` and `product/architecture-standard.md` in `openspec/config.yaml` under
   `context:`, and the testing rules under `rules:` (example on branch `start/level-4-openspec`, spec 10).
2. For each spec, in order, in Claude Code:
   `/opsx:propose spec-01-foundation` with the message "Create this change from docs/spec-pack/specs/01-foundation.md.
   Copy its requirements and scenarios verbatim into the delta spec."
3. Terminal: `openspec validate spec-01-foundation --strict` until it passes. Review `openspec show spec-01-foundation`.
4. `/clear`, then `/opsx:apply`. Review, run `./mvnw verify` yourself.
5. `/opsx:archive` (merges the delta into `openspec/specs/`). Commit. Next spec.

### With Spec Kit
```bash
uv tool install specify-cli==1.3.1
specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation"
```
1. `/speckit-constitution` with the five principles from spec 10, but pointing at
   `docs/spec-pack/product/architecture-standard.md` (the team document `docs/architecture.md` only exists from spec 09 on).
2. For each spec, in order:
   - `/speckit-specify` with the Why, What changes, Out of scope and Requirements sections of the spec (no tech stack).
   - `/speckit-clarify` (answer from the spec; it should find little).
   - `/speckit-plan` with the Design notes, API, Persistence sections and `product/tech-stack.md`.
   - `/speckit-tasks`, then `/speckit-analyze` (read-only consistency check).
   - `/speckit-implement`, then `/speckit-converge` until it reports converged. Commit.
3. Spec Kit keeps one folder per feature as a record; it has no archive step.

### With Agent OS
```bash
git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git
~/agent-os/scripts/project-install.sh
```
1. `/plan-product` with `product/mission.md`, `product/roadmap.md`, `product/tech-stack.md`.
2. Copy `product/architecture-standard.md` to `agent-os/standards/architecture/clean-hexagonal-onion.md` and run
   `/index-standards`. (`/discover-standards` becomes useful once code exists, from spec 03 on.)
3. For each spec: enter plan mode (`/plan`), run `/shape-spec` with "Shape docs/spec-pack/specs/NN-*.md", let task 1
   save the spec folder, approve the plan, implement. `/inject-standards` when a task needs a specific standard.
   Commit.

---

## Part 2: Level 4 tracks for participants

Common start for every track:
```bash
git stash -u                         # keep your own Level 3 work if you like
git switch start/level-4-<track>     # openspec | speckit | agentos
claude                               # new session; /model sonnet
```
The start branches are built from `solution/level-3`: your Level 2 and Level 3 harness (`.mcp.json`, the `jdtls-lsp`
plugin declaration, hooks, permissions, skill, `/ship`) plus the framework, already installed and configured. Pick a
brief from `level-4-feature-briefs.md`. Write the brief into the first command in
your own words and add the answers to its questions: that is your contribution as the human.

### Track A: OpenSpec (about 35 minutes)
1. `/opsx:explore` "reviews and ratings" (or your brief): the agent reads the code you are about to change.
2. `/opsx:propose add-book-reviews` with your brief and decisions.
3. Terminal: `openspec validate add-book-reviews --strict`. Fix what it reports. Common error: a scenario heading
   with three `#` instead of four is silently ignored.
4. Read `openspec/changes/add-book-reviews/specs/.../spec.md`. Change what is wrong with `/opsx:update`.
5. `/clear`, then `/opsx:apply`. Watch the tasks tick off.
6. `/ship add book reviews`. The test-gate hook runs.
7. `/opsx:archive`. Your requirements are now part of the living spec in `openspec/specs/`.

Gotchas: `openspec ...` runs in the terminal, `/opsx:...` in Claude Code. If the commands do not show up, restart
Claude Code.

### Track B: Spec Kit (about 35 minutes, short path)
1. `/speckit-specify` with your brief and decisions. Describe what and why, not the tech stack.
2. `/speckit-clarify`: answer up to five questions. This is where Spec Kit shines; take it seriously.
3. `/speckit-plan` "Use the existing onion layers, see docs/architecture.md and the constitution."
4. `/speckit-tasks`.
5. `/speckit-implement`, then `/speckit-converge` once.
6. `/ship add book reviews`.
Skip `/speckit-checklist` and `/speckit-analyze` today; try them at home.

Gotchas: in Claude Code the commands are `/speckit-specify` (dash), the dotted names are documentation notation.
The active feature is stored in `.specify/feature.json`, not taken from the git branch.

### Track C: Agent OS (about 35 minutes)
1. Look at `agent-os/standards/` first: these were discovered from the codebase. Fix anything wrong.
2. Enter plan mode: `/plan` (or `Shift+Tab` until plan mode), then `/shape-spec` with your brief and decisions.
3. Answer its questions; let it save the spec folder in `agent-os/specs/`.
4. Approve the plan and let Claude implement it. Use `/inject-standards testing` if tests drift from the conventions.
5. `/ship add book reviews`.

Gotchas: `/shape-spec` refuses to run outside plan mode. Agent OS needs bash: on Windows use WSL or Git Bash.

### Level cleared when (all tracks)
- Spec artefacts committed with the code.
- `./mvnw verify` green in your terminal, including `ArchitectureTest`.
- Committed through `/ship`, and the test-gate hook passed.
- The agent lists which test covers which scenario, and you checked two of them yourself.

---

## Installing a framework yourself (after the workshop)
| Framework | Install | Init in an existing repo |
|---|---|---|
| OpenSpec | `curl -fsSL https://openspec.dev/cli/install.sh \| sh` (Windows: `irm https://openspec.dev/cli/install.ps1 \| iex`) or `npm install -g @fission-ai/openspec@latest` (Node 22.12+) | `openspec init --tools claude` |
| Spec Kit | `curl -LsSf https://astral.sh/uv/install.sh \| sh`, then `uv tool install specify-cli` | `specify init --here --force --integration claude --script sh` (Windows: `--script ps`); commit or branch first |
| Agent OS | `git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git` | `~/agent-os/scripts/project-install.sh` |

Sources: github.com/Fission-AI/OpenSpec (README, docs/), github.com/github/spec-kit (README, docs/, templates/),
github.com/buildermethods/agent-os and buildermethods.com/agent-os.
