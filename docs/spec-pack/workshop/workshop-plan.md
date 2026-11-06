# Workshop Plan: Crafting your Agentic Harness

J-Fall 2026 pre-conference workshop, 4 hours, beginner level, English. Facilitator: Maik Kingma (OpenValue AI).

This is the facilitator's master document. The Docusaurus guide (spec 11) is generated from the lab sections below,
so keep this file the single source of truth for lab content.

## 1. Format decisions

| Decision | Choice | Why |
|---|---|---|
| Harness | **Claude Code** | Four hours, beginner level. Claude Code exposes every harness lever (context files, rules, skills, commands, subagents, hooks, permissions, MCP) as plain files in the repo, so participants learn the concepts without building a harness from scratch. Open harnesses such as OpenCode, or writing your own on the Claude Agent SDK, are the recommended next step after the workshop, not a 4-hour beginner topic. |
| Codebase | **Agentic Bookstore** (own build, Clean Hexagonal Onion) | A brownfield app with a strict, test-enforced architecture gives the harness something to protect. Own code, no reuse from earlier workshop repositories. |
| Guidance | **Docusaurus guide + live sync moments** | Participants work at their own pace from the guide. Each lab opens with a short live demo and closes with a 3-minute debrief, so the room stays together. Catch-up branches exist for every lab. |
| MCP | **Our own distributor MCP server, prebuilt** | Participants connect it and use it; they see a real Spring AI MCP server in the repo. Writing an extra MCP tool is a stretch goal. |
| LLM for the app's chat | **Workshop API key provided by Maik** | A Claude Pro subscription does not include API credits. The key is capped, handed out on the day, and revoked afterwards. Env var `BOOKSTORE_AI_API_KEY` (never `ANTHROPIC_API_KEY`). |
| SDD in Lab 4 | **Participant's choice: OpenSpec, Spec Kit or Agent OS** | Prepared start branches per framework remove the install and init time from the lab. |

## 2. Agenda (4 hours)

Times are relative to the start. Adjust the clock to the J-Fall schedule.

| Time | Block | Minutes | Format |
|---|---|---|---|
| 0:00 | Welcome, Lab 0 setup check while people settle | 10 | Room |
| 0:10 | **Keynote**: Agentic engineering and the harness | 45 | Slides |
| 0:55 | **Lab 1**: Break the agent | 25 | Sync 3, solo 19, debrief 3 |
| 1:20 | **Lab 2**: Tame it with context engineering | 35 | Sync 5, solo 27, debrief 3 |
| 1:55 | Break | 15 | |
| 2:10 | **Lab 3**: Behavioural controls: hooks, permissions, skills, commands, MCP | 50 | Sync 8, solo 39, debrief 3 |
| 3:00 | **Lab 4**: Ship with spec-driven development | 45 | Sync 5, solo 37, debrief 3 |
| 3:45 | Wrap-up, show and tell, Q&A | 15 | Room |

Buffer strategy: Lab 3 has optional parts (subagent, extra MCP tool). If the room is behind, skip them and keep
Lab 4 intact, because shipping a feature end-to-end is the payoff of the day.

## 3. Lab 0: Setup check (before and at the start)

Participants should arrive with this done (see the participant email): Claude Code, Temurin 25, git, and Docker
Desktop (or another Docker engine) installed and running. Node.js is not needed: the build installs its own copy.
On the day, in the first 10 minutes:

```bash
claude --version            # Claude Code installed
claude                      # then /status: logged in with a Pro/Max/Team/Enterprise account
java -version               # 25
docker info                 # Docker is running (tests and the local database need it)
git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore
./mvnw verify               # green, under a minute (dependencies and the PostgreSQL image were downloaded at home)
```

Then set the workshop key (shown on the screen, never committed):

```bash
export BOOKSTORE_AI_API_KEY=<key on screen>     # macOS / Linux / WSL
$env:BOOKSTORE_AI_API_KEY="<key on screen>"     # Windows PowerShell
```

Also inside Claude Code: `/model sonnet`. Pro usage limits are shared over a 5-hour window and Opus (the default)
drains them faster; Sonnet comfortably covers four hours of labs.

Facilitator fallback kit: USB sticks with the repo, a pre-filled `~/.m2` archive, the Temurin 25 installers, the
Docker Desktop installers, the PostgreSQL image (`docker save`), the frontend's `node/` and `node_modules/` folders,
and the distributor jar.

## 4. Lab 1: Break the agent (25 min)

**Goal:** experience the failure modes on a codebase where the agent knows nothing about our conventions.

**Start:** `main` (tag `lab-1-start`). No `CLAUDE.md`, no `.claude/`, no MCP. The app has a React storefront in
`frontend/`; the task below is about the catalogue API, and the storefront is where participants see the result.

**Sync moment (3 min):** Maik opens the repo, shows the onion diagram and the `ArchitectureTest`, then gives the
task below to Claude Code live without any context. The room sees the first minute of what it does.

**Task for participants (paste literally, do not add hints):**

> Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people
> search the catalogue.

**Steps**
1. Start a fresh session: `claude`. Notice that new sessions start in **auto** permission mode: the agent acts
   without asking. Leave it like that for this lab.
2. Paste the task. Do not help. Do not answer questions with conventions, only with "you decide".
3. When it says it is done, check for yourself:
   - `./mvnw verify`: green? Which tests fail? Does `ArchitectureTest` fail?
   - `git status` and `git diff --stat`: which files did it touch? Any outside the catalogue?
   - Did it add a Flyway migration, or change an existing one, or rely on Hibernate DDL?
   - What type did it use for money? (`double` is a finding.)
   - Where does the discount rule live: domain, flow, controller, JPA entity?
   - Did it write tests? Did it run them? Did it claim things it did not verify?
4. Fill in the observation card in the guide (5 lines: what went right, what went wrong, why).
5. Reset: `git restore . && git clean -fd` (or press `Esc` twice / `/rewind` to see checkpoints; note that changes
   made through Bash are not tracked by checkpoints, which is why git is the real safety net).

**Debrief (3 min):** collect findings on the whiteboard in three columns: *didn't know* (context), *didn't care*
(behaviour), *didn't check* (verification). Those columns are Labs 2, 3 and 4.

**Expected findings (facilitator cheat sheet):** discount logic in the controller or the JPA entity, `double`
prices, no migration (app fails at startup with `ddl-auto=validate`), ArchUnit violations (JPA imports in
`domain`, controller calling a repository), `mvn` instead of `./mvnw`, "all tests pass" without running them.

## 5. Lab 2: Tame it with context engineering (35 min)

**Goal:** give the agent the smallest set of high-signal context that makes the right thing the obvious thing.

**Start:** `main` again (clean). Catch-up / compare: `solution/lab-2`.

**Sync moment (5 min):** run `/init` live, then read the generated `CLAUDE.md` together and cross out everything
Claude could have read from the code itself. Show `/context` and `/memory`.

**Steps**
1. Run `/init`. Read the result critically. Anthropic's own guidance: every line costs attention on every task, and
   the most useful line is the command that runs the tests.
2. Rewrite `CLAUDE.md` to at most 60 lines. It must contain:
   - Build and test commands (`./mvnw verify`, single test: `./mvnw -pl bookstore test -Dtest=BookTest`).
   - Definition of done: verify green, including `ArchitectureTest`; never say "tests pass" without showing output.
   - The five rules an agent cannot infer quickly: domain is pure Java; money is `Money` (BigDecimal), never double;
     schema changes only via a *new* Flyway migration; inbound adapters only talk to flows; no Lombok.
   - A path instead of an essay: "Read docs/architecture.md before adding or moving classes." Compare with the
     import syntax `@docs/architecture.md`: an import loads the whole file at session start and costs context on
     every task; a plain path lets Claude read it only when needed. Try both and look at `/context`.
3. Add path-scoped rules in `.claude/rules/` (they load only when Claude touches matching files):
   - `domain.md` with `paths: ["bookstore/src/main/java/**/domain/**"]`
   - `data-and-migrations.md` with `paths: ["bookstore/src/main/java/**/data/**", "bookstore/src/main/resources/db/**"]`
   - `testing.md` with `paths: ["**/src/test/**"]`
4. Explore the hierarchy. Add a conflicting line to your user file `~/.claude/CLAUDE.md` (for example
   "Always use Lombok") and a personal `CLAUDE.local.md` (for example your preferred commit style). Start a new
   session and run `/memory` and `/context`. Observe: files are **concatenated, not overridden**. When instructions
   conflict, the agent has to choose. Remove the Lombok line afterwards.
5. Context rot: run `/context` and read the grid. Learn the three moves: `/clear` between unrelated tasks,
   `/compact focus on <topic>` when a long task drifts, and delegate big reads to a subagent ("use a subagent to
   find every place that formats money") so only the summary lands in your context. A 1M-token window is not an
   attention guarantee.
6. Redo the Lab 1 task in a fresh session (`/clear`). Compare with your observation card.

**Debrief (3 min):** what improved, and what still went wrong *even though it was written down*? That last list is
the bridge to Lab 3: `CLAUDE.md` is guidance, the agent may decide a guideline does not apply.

## 6. Break (15 min)

Facilitator: check who is behind, point them to `solution/lab-2`. Start the distributor on the projector machine.

## 7. Lab 3: Behavioural controls (50 min)

**Goal:** turn the things that must happen every time into guarantees, and package repeatable work.

**Start:** your Lab 2 result, or `solution/lab-2`. Catch-up / compare: `solution/lab-3`.

**Sync moment (8 min):** live demo of one hook blocking a commit (red tests, exit code 2, the agent reads the
stderr and fixes the test), then `claude mcp add` for the distributor and one MCP question in the chat.

### Part A: Permissions (4 min)
Create `.claude/settings.json` (shared, committed):
```json
{
  "permissions": {
    "allow": ["Bash(./mvnw *)", "Bash(git status *)", "Bash(git diff *)", "Bash(git add *)", "Bash(git commit *)"],
    "deny":  ["Read(./.env)", "Read(./.env.*)", "Bash(git push)", "Bash(git push *)", "Bash(* --no-verify*)", "Bash(git commit -n *)"]
  }
}
```
Rules are evaluated deny, then ask, then allow. Try `Shift+Tab` to cycle modes (from auto: default, acceptEdits,
plan) and `/plan` for read-only planning.

### Part B: Hooks: no green tests, no commit (12 min)
1. Add a PreToolUse hook to `.claude/settings.json` that runs before Bash commands that are git commits:
   ```json
   "hooks": {
     "PreToolUse": [
       { "matcher": "Bash",
         "hooks": [ { "type": "command", "if": "Bash(git commit *)", "timeout": 180,
                      "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/tests-before-commit.sh" } ] }
     ]
   }
   ```
2. Create `.claude/hooks/tests-before-commit.sh` (make it executable with `chmod +x`). It runs `./mvnw -q verify`;
   on failure it prints the last 40 lines to **stderr** and exits **2**, which blocks the commit and feeds the
   reason back to Claude. Any other non-zero exit code would NOT block.
3. Also install the classic git hook so humans are held to the same rule: `git config core.hooksPath .githooks`
   (the repo already ships `.githooks/pre-commit` in the solution branch; write your own first).
4. Test it: ask Claude to "change Money to round HALF_UP on purpose and commit". Watch the block, the feedback, the fix.
5. Discuss: why both? The Claude hook gives the agent feedback *inside its loop* and runs on every `git commit`,
   flags or not. The git hook protects the repo from everyone, humans included, but `--no-verify` (or `-n`) skips
   it; the deny rules close that escape hatch for the agent.

Optional: a Stop hook that runs only the fast `ArchitectureTest` when Java files changed, and checks
`stop_hook_active` to avoid loops.

### Part C: A skill and a command (12 min)
1. Create `.claude/skills/new-use-case/SKILL.md`: a repeatable workflow for adding a use case through all onion
   layers (domain behaviour, flow method, port change, adapter, migration if needed, tests per layer, verify).
   Frontmatter: `name`, a precise `description` (this is what makes Claude pick it), `allowed-tools`.
   Put the layer cheat sheet in a supporting file `reference.md` so it loads only when needed.
2. Use it: "Customers must be able to cancel an order that has not shipped yet." Watch Claude load the skill.
3. Create a command `.claude/commands/ship.md` with `disable-model-invocation: true` semantics (only you trigger it):
   it shows `!`git status --short``, runs verify, proposes a conventional commit message for `$ARGUMENTS`, and
   commits (the hook from part B still guards it). Custom commands and skills are the same mechanism now; both
   create `/ship`.

### Part D: Our own MCP server (11 min)
1. Start the distributor: `./mvnw -pl distributor spring-boot:run` (or `java -jar distributor.jar`).
2. Register it for the project:
   `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp`
   This writes `.mcp.json`. Restart Claude Code if needed and check `/mcp`.
3. Ask: "Which science-fiction titles does the distributor have that we do not sell yet?" The agent combines the
   MCP tools (`search_titles`, `get_title`) with our own code. Tools show up as `mcp__distributor__*`.
4. Use it as development context: "Using the distributor MCP, find a title that has a translator or illustrator
   among its contributors. Write an ACL contract test for `DistributorAppServiceImpl` with that exact payload shape
   and prove that only authors end up in our catalogue." (The existing tests deliberately do not cover this case.)
5. Open `distributor/src/main/java/.../mcp/CatalogMcpTools.java` to see how little code an MCP tool is.

Stretch: add an MCP tool `list_new_releases(days)` to the distributor and use it from Claude Code.
Stretch: a review subagent `.claude/agents/architecture-reviewer.md` (`tools: Read, Glob, Grep, Bash`, instructed to use
Bash only for `git diff` and `git status`) that reviews the diff against the layer rules before `/ship`.

**Debrief (3 min):** guideline versus guarantee. Which of your Lab 1 findings are now impossible, and which are only
less likely?

## 8. Lab 4: Ship with spec-driven development (45 min)

**Goal:** turn a vague request into a spec, a plan and tasks, then let the agent implement and verify it inside the
harness you built.

**Start:** pick a track and switch to its prepared branch (framework installed, project conventions captured):

| Track | Branch | Weight | Commands you will use |
|---|---|---|---|
| A: OpenSpec | `start/lab-4-openspec` | Light | `/opsx:explore`, `/opsx:propose`, `openspec validate --strict`, `/opsx:update`, `/opsx:apply`, `/opsx:archive` |
| B: Spec Kit | `start/lab-4-speckit` | Heavier, most guard rails | `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, `/speckit-converge` |
| C: Agent OS | `start/lab-4-agentos` | Light, standards-centric | `/plan` then `/shape-spec`, `/inject-standards`, implement the plan |

Detailed track instructions: `sdd-framework-guide.md`, Part 2.

**Pick a feature brief** (`lab-4-feature-briefs.md`): A) book reviews and ratings, B) wishlist with back-in-stock
notification, C) the assistant recommends titles from the distributor.

**Sync moment (5 min):** Maik shows one spec artefact per framework side by side (OpenSpec delta spec, Spec Kit
spec.md, Agent OS shape.md) so everyone sees the same idea in three shapes.

**Definition of done for Lab 4**
- Spec artefacts committed next to the code.
- `./mvnw verify` green, including `ArchitectureTest`, shown in the terminal (not claimed).
- Commit made through your `/ship` command and passed the test-gate hook.
- Every acceptance scenario in your spec maps to a test (ask the agent to list the mapping).

**Debrief (3 min):** who finished, and where did the spec save you? Where did it slow you down?

## 9. Wrap-up (15 min)
- Two or three participants show their harness folder (`tree .claude`) and their spec.
- Map back to the keynote: context, behaviour, structure, verification.
- Take-home: the guide stays online; Anthropic Academy courses (Claude Code 101, Claude Code in Action,
  Introduction to agent skills, Introduction to subagents, Introduction to MCP); next steps: open harnesses
  (OpenCode) or building one on the Claude Agent SDK.
- Remind: the workshop API key is revoked tonight.

## 10. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Conference wifi | Participants run `./mvnw verify` at home (Maven dependencies, the local Node.js, npm packages and the PostgreSQL image are then cached). USB kit with `~/.m2` archive, `docker save` of the image, and the frontend caches. Distributor runs locally. |
| Docker missing or not running | Lab 0 checks `docker info`. Docker Desktop installers on the USB kit. Pairing as fallback. |
| Pro usage limit reached | `/model sonnet` from Lab 0. `/usage` to check. Pair up if someone hits the limit. Facilitator has a spare Max account for demos. |
| `ANTHROPIC_API_KEY` exported by a participant | Claude Code would switch to that key. We only ever use `BOOKSTORE_AI_API_KEY`. Lab 0 checks `/status` shows the subscription. |
| Workshop key abuse | Console spend limit, key scoped to a workshop workspace, revoked after the session. |
| Windows participants | Claude Code runs natively; hooks are bash scripts, so recommend Git for Windows (Git Bash) or WSL2. Provide `.ps1` variants of the hook scripts in `solution/lab-3`. Agent OS needs bash (WSL or Git Bash). |
| MCP protocol version mismatch (MCP spec 2026-07-28 vs Spring AI 2.0.2 SDK) | Smoke test in spec 05 on the build machine and a second laptop. Fallback: STDIO transport (`./mvnw -pl distributor package`, then `claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio`). |
| Corporate laptops block installs | Email asks for admin rights or a personal laptop. Pairing as fallback. |
| Someone is far behind | `git stash -u && git switch solution/lab-N` at the start of each lab. |
