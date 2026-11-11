# Workshop Plan: Crafting your Agentic Harness

J-Fall 2026 pre-conference workshop, 4 hours, beginner level, English. Facilitator: Maik Kingma (OpenValue AI).

This is the facilitator's master document. The Docusaurus guide (spec 11) is generated from the level sections below,
so keep this file the single source of truth for level content.

## 1. Format decisions

| Decision | Choice | Why |
|---|---|---|
| Harness | **Claude Code** | Four hours, beginner level. Claude Code exposes every harness lever (context files, rules, skills, commands, subagents, hooks, permissions, MCP, language servers) as plain files or plugins, so participants learn the concepts without building a harness from scratch. Open harnesses such as OpenCode, or writing your own on the Claude Agent SDK, are the recommended next step after the workshop, not a 4-hour beginner topic. |
| Structure | **Levels 0 to 4 that build on each other** | Developers like games. The levels match the four levels of agentic harness engineering from the J-Spring talk: context engineering, give your agent eyes, behavioral control, spec-driven development. Level 0 is the tutorial level. Each level ends with a check participants run themselves: "Level cleared when". |
| Codebase | **Agentic Bookstore** (own build, Clean Hexagonal Onion) | A brownfield app with a strict, test-enforced architecture gives the harness something to protect. Own code, no reuse from earlier workshop repositories. |
| Guidance | **Docusaurus guide, Maik and Tom walking around** | Participants work at their own pace from the guide. Maik and Tom walk around and help, and each level closes with a short debrief, so the room stays together. Catch-up branches exist for every level. |
| Code intelligence | **Claude Code's built-in LSP tool with the `jdtls-lsp` plugin** | Hands-on for every participant in Level 2. The agent gets the same view of the code as an IDE: references, types and compiler diagnostics after every edit. |
| MCP | **Our own distributor MCP server, prebuilt** | Participants connect it and use it; they see a real Spring AI MCP server in the repo. Writing an extra MCP tool is a stretch goal. |
| LLM for the app's chat | **Workshop API key provided by Maik** | A Claude Pro subscription does not include API credits. The key is capped, handed out on the day, and revoked afterwards. Env var `BOOKSTORE_AI_API_KEY` (never `ANTHROPIC_API_KEY`). |
| SDD in Level 4 | **Participant's choice: OpenSpec, Spec Kit or Agent OS** | Prepared start branches per framework hold the project files, so the level skips the init time. Only Track A needs one install, the OpenSpec program (see the setup check). |

## 2. Agenda (4 hours)

Times are relative to the start. Adjust the clock to the J-Fall schedule.

| Time | Block | Minutes | Format |
|---|---|---|---|
| 0:00 | Welcome, setup check while people settle | 10 | Room |
| 0:10 | **Keynote**: Agentic engineering and the harness | 45 | Slides |
| 0:55 | **Level 0**: Break the agent | 25 | Solo 22, debrief 3 |
| 1:20 | **Level 1**: Context engineering | 35 | Solo 32, debrief 3 |
| 1:55 | Break | 15 | |
| 2:10 | **Level 2**: Give your agent eyes (LSP + MCP) | 20 | Solo 18 (LSP 10, MCP 8), debrief 2 |
| 2:30 | **Level 3**: Behavioral control (permissions, hooks, skills, commands, subagents) | 30 | Solo 28 (permissions 4, hooks 14, skill and command 10), debrief 2 |
| 3:00 | **Level 4**: Spec-driven development | 45 | Solo 42, debrief 3 |
| 3:45 | Wrap-up: who cleared which level, show and tell, Q&A | 15 | Room |

Total: 10 + 45 + 25 + 35 + 15 + 20 + 30 + 45 + 15 = 240 minutes.

During the levels Maik and Tom walk around and help.

Buffer strategy: Level 2 and Level 3 have stretch goals (the `list_new_releases` MCP tool, the review subagent) and
Level 3 has an optional Stop hook. If the room is behind, skip them and keep Level 4 intact, because shipping a
feature end-to-end is the payoff of the day.

## 3. Setup check (before and at the start)

Participants should arrive with this done (see the participant email): Claude Code, Temurin 25, git, Docker
Desktop (or another Docker engine) installed and running, and the Java language server `jdtls` on the `PATH`.
Node.js is not needed for the build: it installs its own copy. You only need Node.js 22.12 or later if you pick Track A (OpenSpec) and install it with npm; the install script needs no Node.js.
On the day, in the first 10 minutes:

```bash
claude --version            # Claude Code installed
claude                      # then /status: logged in with a Pro/Max/Team/Enterprise account
java -version               # 25
docker info                 # Docker is running (tests and the local database need it)
which jdtls                 # Java language server on the PATH (Windows PowerShell: Get-Command jdtls)
git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore
./mvnw verify               # green, under a minute (dependencies and the PostgreSQL image were downloaded at home)
```

Installing `jdtls` (the plugin does not ship the binary; it needs JDK 21 or later, participants have 25):

| OS | Install |
|---|---|
| macOS | `brew install jdtls` |
| Arch Linux | `yay -S jdtls` |
| Other Linux | Extract a JDT LS release and put a `jdtls` wrapper script on the `PATH` |
| Windows | Use WSL and install as on Linux there. Native Windows is not verified yet. |

The setup check covers the binary only. The `jdtls-lsp` plugin is installed in Level 2 Part A, so Levels 0 and 1
run without it.

Installing OpenSpec (Track A of Level 4 only: the start branch holds the OpenSpec project files but not the `openspec`
program). The install script needs no Node.js. The version is pinned to 1.16.0, so everybody has the same one:

```bash
curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v1.16.0 sh     # macOS / Linux / WSL
```

In Windows PowerShell:

```powershell
$env:OPENSPEC_VERSION = "v1.16.0"
irm https://openspec.dev/cli/install.ps1 | iex
```

With Node.js 22.12 or later, npm installs the same version:

```bash
npm install -g @fission-ai/openspec@1.16.0
```

Check the install, it prints 1.16.0:

```bash
openspec --version
```

Then set the workshop key (shown on the screen, never committed):

```bash
export BOOKSTORE_AI_API_KEY=<key on screen>     # macOS / Linux / WSL
$env:BOOKSTORE_AI_API_KEY="<key on screen>"     # Windows PowerShell
```

Also inside Claude Code: `/model sonnet`. Pro usage limits are shared over a 5-hour window and Opus (the default)
drains them faster; Sonnet comfortably covers four hours of levels.

Facilitator fallback kit: USB sticks with the repo, a pre-filled `~/.m2` archive, the Temurin 25 installers, the
Docker Desktop installers, the PostgreSQL image (`docker save`), the frontend's `node/` and `node_modules/` folders,
and the distributor jar.

## 4. Level 0: Break the agent (25 min)

**Goal:** experience the failure modes on a codebase where the agent knows nothing about our conventions.

**Start:** `main` (tag `level-0-start`). No `CLAUDE.md`, no `.claude/`, no MCP. The app has a React storefront in
`frontend/`; the task below is about the catalogue API, and the storefront is where participants see the result.

Before pasting the task, participants look at the onion diagram on the guide's Architecture page and find the
`ArchitectureTest` in the repo: that is the standard the agent does not know yet.

**Task for participants (paste literally, do not add hints):**

> Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people
> search the catalogue.

**Steps**
1. Start a fresh session: `claude`. Notice that new sessions start in **auto** permission mode: the agent acts
   without asking. Leave it like that for this level.
2. Paste the task. Do not help. Do not answer questions with conventions, only with "you decide".
3. When it says it is done, check for yourself:
   - `./mvnw verify`: green? Which tests fail? Does `ArchitectureTest` fail?
   - `git status` and `git diff --stat`: which files did it touch? Any outside the catalogue?
   - Did it add a Flyway migration, or change an existing one, or rely on Hibernate DDL?
   - What type did it use for money? (`double` is a finding.)
   - Where does the discount rule live: domain, flow, controller, JPA entity?
   - Did it write tests? Did it run them? Did it claim things it did not verify?
4. Fill in the observation card in the guide: one row per failure mode you found, each with its evidence (the
   failing test, the ArchUnit output or the wrong file) and its debrief column (*didn't know*, *didn't care*,
   *didn't check*), plus one line on what went right.
5. Reset: `git restore . && git clean -fd` (or press `Esc` twice / `/rewind` to see checkpoints; note that changes
   made through Bash are not tracked by checkpoints, which is why git is the real safety net).

**Level cleared when**
- Your observation card lists at least three failure modes, each with evidence (the failing test, the ArchUnit
  output or the wrong file).

**Debrief (3 min):** collect findings on the whiteboard in three columns: *didn't know* (context), *didn't care*
(behaviour), *didn't check* (verification). Levels 1 to 4 work through those columns.

**Expected findings (facilitator cheat sheet):** discount logic in the controller or the JPA entity, `double`
prices, no migration (app fails at startup with `ddl-auto=validate`), ArchUnit violations (JPA imports in
`domain`, controller calling a repository), `mvn` instead of `./mvnw`, "all tests pass" without running them.

## 5. Level 1: Context engineering (35 min)

**Goal:** give the agent the smallest set of high-signal context that makes the right thing the obvious thing.

**Start:** the tag `level-0-start` again (clean): `git stash -u && git switch -C level-1 level-0-start`. Catch-up / compare: `solution/level-1`.

**Steps**
1. Run `/init`. Read the result critically and cross out everything Claude could have read from the code itself.
   Anthropic's own guidance: every line costs attention on every task, and
   the most useful line is the command that runs the tests.
2. Rewrite `CLAUDE.md` to at most 60 lines. It must contain:
   - Build and test commands (`./mvnw verify`, single test: `./mvnw -pl bookstore test -Dtest=BookTest`).
   - Definition of done: verify green, including `ArchitectureTest`; never say "tests pass" without showing output.
   - The five rules an agent cannot infer quickly: domain is pure Java; money is `Money` (BigDecimal), never double;
     schema changes only via a *new* Flyway migration; inbound adapters only talk to flows; no Lombok.
   - A path instead of an essay: "Read docs/architecture.md before adding or moving classes." Compare with the
     import syntax `@docs/architecture.md`: an import loads the whole file at session start and costs context on
     every task; a plain path lets Claude read it only when needed. Try both and look at `/context`.
3. Add path-scoped rules. A path-scoped rule is a markdown file in `.claude/rules/` whose header names the files it
   is about. Create `.claude/rules/domain.md` with this exact content:

   ```markdown
   ---
   paths:
     - "bookstore/src/main/java/**/domain/**"
   ---
   # Domain layer
   - Aggregates: private constructor, an intention-revealing static factory for creation, `restore(...)` for rehydration.
   - Change state through behaviour methods named after the business action. No setters.
   - Check invariants inside the aggregate and throw an unchecked exception from the `domain` package.
   - Register domain events in the aggregate; flows publish them after saving via `pullDomainEvents()`.
   - Value objects are records that validate in the compact constructor.
   - Domain tests are plain JUnit and AssertJ, without a Spring context.
   ```

   The header between the two `---` lines is YAML frontmatter: `paths` lists globs relative to the project root, and
   the rule loads only when Claude reads or edits a file that matches one, not at session start. Add two more rules
   the same way:
   - `data-and-migrations.md` with `paths: ["bookstore/src/main/java/**/data/**", "bookstore/src/main/resources/db/**"]`
   - `testing.md` with `paths: ["**/src/test/**"]`
4. Explore the hierarchy. Add a conflicting line to your user file `~/.claude/CLAUDE.md` (for example
   "Always use Lombok") and a personal `CLAUDE.local.md` (for example your preferred commit style). Start a new
   session and run `/memory` and `/context`. Observe: files are **concatenated, not overridden**. When instructions
   conflict, the agent has to choose. Remove the Lombok line afterwards.
5. Context rot: run `/context` and read the grid. The grid below is an illustration, not a screenshot: your symbols,
   names and numbers differ. Each symbol stands for 2k tokens of a 200k window.

   ```text
   Context usage: 100k of 200k tokens (50%)

   S S S S S S S C K M      S  System prompt                  14k   (7%)
   M M M M M M M M M M      C  CLAUDE.md and auto memory       2k   (1%)
   M M M M M M M M M M      K  Skill descriptions              2k   (1%)
   M M M M M M M M M M      M  Messages and tool results      82k  (41%)
   M M M M M M M M M M      .  Free space                    100k  (50%)
   . . . . . . . . . .
   . . . . . . . . . .
   . . . . . . . . . .
   . . . . . . . . . .
   . . . . . . . . . .
   ```

   What to watch for:
   - S, C and K load before you type anything. Every line of your `CLAUDE.md` costs attention on every task.
   - M grows with every prompt, answer, file read and tool result. It is the part you control.
   - Free space shrinks as M grows. Long before it reaches zero, answers drift: that is context rot.
   - Same task but drifting: `/compact focus on <topic>`. Unrelated next task: `/clear`.

   Learn the three moves: `/clear` between unrelated tasks, `/compact focus on <topic>` when a long task drifts, and
   delegate big reads to a subagent ("use a subagent to find every place that formats money") so only the summary
   lands in your context. A 1M-token window is not an attention guarantee.
6. Redo the Level 0 task in a fresh session (`/clear`). Compare with your observation card.

**Make it yours:** The best harness has a creative engineer who keeps tweaking it. Pick one thing Claude got wrong in
Level 0 and write a rule of your own for it, for example `.claude/rules/acl.md` for the distributor client. Skills,
hooks and commands of your own can follow in later levels. This is optional.

**Level cleared when**
- `CLAUDE.md` has at most 60 lines.
- Asked to change a class in `domain`, Claude follows `.claude/rules/domain.md`: a behaviour method named after the business action, no setters.
- In a fresh session, the Level 0 task ends with `./mvnw verify` green, shown in the terminal, and no ArchUnit
  violation.

**Debrief (3 min):** what improved, and what still went wrong *even though it was written down*? That last list is
the bridge to Level 3: `CLAUDE.md` is guidance, the agent may decide a guideline does not apply.

## 6. Break (15 min)

Facilitator: check who is behind, point them to `solution/level-1`. Start the distributor on the projector machine.

## 7. Level 2: Give your agent eyes (LSP + MCP) (20 min)

**Goal:** let the agent see what you see: the code through a language server, and real external data through our
own MCP server, instead of guessing.

**Start:** your Level 1 result, or `solution/level-1`. Catch-up / compare: `solution/level-2`.

Part A starts with the LSP install check. Part B registers the distributor and asks one MCP question in the chat.

### Part A: LSP (10 min)
Claude Code has a built-in, read-only LSP tool. With a code intelligence plugin installed, it gives go to
definition, find references, type info, document and workspace symbols, implementations and call hierarchy, and
Claude Code reports diagnostics automatically after every edit.
1. Check the binary: `which jdtls` (Windows: inside WSL). If it is missing, install it as in the setup check.
2. Install the plugin: `/plugin install jdtls-lsp@claude-plugins-official` (if the marketplace is missing, first
   `/plugin marketplace add anthropics/claude-plugins-official`). Choose a scope: user scope keeps it out of the
   repository, project scope writes it to `.claude/settings.json` (as on `solution/level-2`). Confirm it on the
   **Installed** tab of `/plugin`.
3. Use it: "Find all references to `Money.of`." The answer comes from the language server, not from a text search,
   and spans several layers (domain, flows and mappers, tests).
4. Make one deliberate type error: "In `Book.java`, make `price()` return `String` instead of `Money`. Do not touch
   the callers yet." Claude Code prints `Found N new diagnostic issues ...` under the edit. Then: "Fix the new
   diagnostics." Watch the agent read them and repair the code.
5. Nothing appears? Check `/plugin` (tab **Errors**) and start `claude --debug`. The debug output goes to a log file
   under `~/.claude/debug/` and prints nothing extra in the terminal. Look in that file for a line with `LSP server` and
   `failed to start`. The error
   `Executable not found in $PATH: "jdtls"` means the binary is missing. The guide keeps the debugging options in one
   collapsed section, five checks to try in order until the diagnostics appear, and a diagnosis prompt:
   1. The binary: run `which jdtls` in the same terminal that you start `claude` from. It must print a path (Windows:
      inside WSL). If it prints nothing, install it as in the setup check, open a new terminal and start `claude` again.
   2. Java: run `java -version`. The language server needs Java 21 or later.
   3. The plugin: open `/plugin`. `jdtls-lsp` must be on the **Installed** tab and enabled, and the **Errors** tab must
      not list it.
   4. A new session: if you installed `jdtls` or the plugin after Claude Code started, or put `jdtls` on the `PATH` in
      another terminal, start a new session from a shell where `which jdtls` prints a path.
   5. The first Java edit: the language server starts the first time Claude edits a `.java` file. Repeat the deliberate
      type error (Part A, step 4) before you decide that it is broken.

   Or paste this prompt and let Claude check what it can. It only reads and changes nothing:
   ```text
   The jdtls language server plugin shows no diagnostics. Check without changing anything: run which jdtls and java -version and tell me the results, read .claude/settings.json and ~/.claude/settings.json and tell me whether jdtls-lsp@claude-plugins-official is enabled, and tell me what else I should check on the Errors tab of /plugin. Then name the most likely cause and the one fix to try first. Do not edit, install or delete anything.
   ```
   Claude may not see exactly the shell that you start it from, so trust your own `which jdtls` over its answer.

   Still stuck after two minutes: continue with Part B.

### Part B: MCP (8 min)
1. Start the distributor: `./mvnw -pl distributor spring-boot:run` (or `java -jar distributor.jar`).
2. Register it for the project:
   `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp`
   This writes `.mcp.json`. Restart Claude Code if needed and check `/mcp`.
3. Ask one question: "Which science-fiction titles does the distributor have that we do not sell yet?" Tools show
   up as `mcp__distributor__*`.
4. Use it as development context: "Using the distributor MCP, find a title that has a translator or illustrator
   among its contributors. Write an ACL contract test for `DistributorAppServiceImpl` with that exact payload shape
   and prove that only authors end up in our catalogue." (The existing tests deliberately do not cover this case.)
5. An MCP tool is little code: see `distributor/src/main/java/.../mcp/CatalogMcpTools.java`.

**Make it yours:** The best harness has a creative engineer tweaking it all the time. Think of a fifth tool that Claude
would use on the bookstore, and write down its name, its inputs and what it returns. Or add one rule to `CLAUDE.md` that
tells Claude to ask the language server before it searches the text.

Stretch: add an MCP tool `list_new_releases(days)` to the distributor and use it from Claude Code.

**Level cleared when**
- After a deliberate type error, Claude Code reports new diagnostics and fixes them.
- `/mcp` shows the distributor connected.
- The ACL contract test built from real MCP data is green.

**Debrief (2 min):** what did the agent see this time that it had to guess before: compiler diagnostics right after
an edit, references by symbol, real payloads instead of invented ones?

## 8. Level 3: Behavioral control (permissions, hooks, skills, commands, subagents) (30 min)

**Goal:** turn the things that must happen every time into guarantees, and package repeatable work.

**Start:** your Level 2 result, or `solution/level-2`. Catch-up / compare: `solution/level-3`.

### Part A: Permissions (4 min)
Create `.claude/settings.json` (shared, committed), or add the `permissions` block if the file already exists
(on `solution/level-2` it declares the `jdtls-lsp` plugin):
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

### Part B: Hooks: no green tests, no commit (14 min)
Watch for the moment the hook blocks a red commit with exit code 2 and the agent reads the stderr and fixes the test.
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

### Part C: A skill and a command (10 min)
1. Create `.claude/skills/new-use-case/SKILL.md`: a repeatable workflow for adding a use case through all onion
   layers (domain behaviour, flow method, port change, adapter, migration if needed, tests per layer, verify).
   Frontmatter: `name`, a precise `description` (this is what makes Claude pick it), `allowed-tools`.
   Put the layer cheat sheet in a supporting file `reference.md` so it loads only when needed.
2. Use it: "Customers must be able to cancel an order that has not shipped yet." Watch Claude load the skill.
3. Create a command `.claude/commands/ship.md` with `disable-model-invocation: true` semantics (only you trigger it):
   it shows `!`git status --short``, runs verify, proposes a conventional commit message for `$ARGUMENTS`, and
   commits (the hook from part B still guards it). Custom commands and skills are the same mechanism now; both
   create `/ship`.

Stretch: a review subagent `.claude/agents/architecture-reviewer.md` (`tools: Read, Glob, Grep, Bash`, instructed to use
Bash only for `git diff` and `git status`) that reviews the diff against the layer rules before `/ship`.

Make it yours (optional, never a step): the best harness has a creative engineer tweaking it all the time. Once your
hook and skill work, invent one more guardrail of your own: a deny rule for a command you never want Claude to run, a
hook that blocks a risky edit, or a second command next to `/ship`. Keep it only if it helps.

**Level cleared when**
- A deliberately red commit is blocked by the hook (exit 2) and then fixed by the agent.
- `git commit --no-verify` is denied.
- The cancel-order use case built with the skill is committed through `/ship`.

**Debrief (2 min):** guideline versus guarantee. Which of your Level 0 findings are now impossible, and which are
only less likely?

## 9. Level 4: Spec-driven development (45 min)

**Goal:** turn a vague request into a spec, a plan and tasks, then let the agent implement and verify it inside the
harness you built.

**Start:** pick a track and switch to its prepared branch (built from `solution/level-3`, with the framework's
project files already in place):

| Track | Branch | Weight | Commands you will use |
|---|---|---|---|
| A: OpenSpec | `start/level-4-openspec` | Light | `/opsx:explore`, `/opsx:propose`, `openspec validate --strict`, `/opsx:update`, `/opsx:apply`, `/opsx:archive` |
| B: Spec Kit | `start/level-4-speckit` | Heavier, most guard rails | `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, `/speckit-converge` |
| C: Agent OS | `start/level-4-agentos` | Light, standards-centric | `/plan` then `/shape-spec`, `/inject-standards`, implement the plan |

Detailed track instructions: `sdd-framework-guide.md`, Part 2.

A track takes about 35 minutes; the rest of the 42 solo minutes is for choosing a track and a brief at the start and for the final checks at the end.

**What each start branch already holds:**

**Track A:** The start branch holds an initialised OpenSpec project: `openspec/config.yaml` and one spec in `openspec/specs/ordering/spec.md`, plus the `/opsx` commands in `.claude/commands/opsx/`. It does not hold the `openspec` program.

**Track B:** The start branch holds a Spec Kit project: the `.specify/` folder with a constitution already written in `.specify/memory/constitution.md`, plus the `/speckit-*` skills in `.claude/skills/`.

**Track C:** The start branch holds Agent OS product docs in `agent-os/product/` and standards written for this repo and indexed in `agent-os/standards/index.yml`, plus the commands in `.claude/commands/agent-os/`.

**For your own brownfield project**, the documented first steps of each framework, in order:

**Track A:**

1. `openspec init` in your repository.
2. Optionally fill in `context:` in `openspec/config.yaml` with your stack and conventions.
3. Then the same flow as in this track: `/opsx:explore`, `/opsx:propose`, `/opsx:apply` and `/opsx:archive`.

**Track B:**

1. `specify init --here --force --integration claude` in your repository.
2. `/speckit-constitution` with the rules your project already follows.
3. Then `/speckit-specify` for one small change, followed by `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks` and `/speckit-implement`.

**Track C:**

1. `/discover-standards` to extract the patterns your code already follows.
2. `/inject-standards` before you implement.
3. `/plan-product` for the mission and the roadmap.
4. `/shape-spec` in plan mode, for each change.

**Pick a feature brief** (`level-4-feature-briefs.md`): A) book reviews and ratings, B) wishlist with back-in-stock
notification, C) the assistant recommends titles from the distributor.

**Make it yours:** Spec-driven does not mean someone else's rules. Invent one of your own that every future spec must follow, and see whether the agent keeps to it: a rule in `openspec/config.yaml`, a principle in `.specify/memory/constitution.md` or a standard in `agent-os/standards/`. The best harness has a creative engineer tweaking it all the time.

Whichever brief you pick, once your spec artefact is committed, ask someone on another track to show theirs: an OpenSpec delta spec, a Spec Kit
spec.md and an Agent OS shape.md hold the same idea in three shapes.

**Level cleared when**
- Spec artefacts committed next to the code.
- `./mvnw verify` green, including `ArchitectureTest`, shown in the terminal (not claimed).
- Commit made through your `/ship` command and passed the test-gate hook.
- Every acceptance scenario in your spec maps to a test (ask the agent to list the mapping).

**Debrief (3 min):** who finished, and where did the spec save you? Where did it slow you down?

## Bonus level: orchestrate your own team (optional)

This bonus level is optional: it is for you if you finish Level 4 early or want to carry on at home. It sits outside the agenda and has no solution branch, because you build your own flow. Anyone who is not finished with Level 4 goes straight on to the Wrap-up.

**Goal:** plan one feature as a flow with human gates, define a research agent and a QA agent as subagent files, carry one feature through the flow from the main session and, when the account has workflows, let Claude write the flow as a workflow. The guide gives questions and a table of parts, never a finished agent file or script.

**Start:** where Level 4 ended, on the same branch. Nothing new is prepared.

**Part A: Think first.** The steps of one feature are understand, research, plan, implement, verify and review. Everyone marks each step agent or human gate, starting from the spec flow of the Level 4 track, and keeps at least one human gate.

**Part B: Define two subagents.** A subagent works in its own context window and hands back a summary. A subagent is a Markdown file with frontmatter.

The file lives in `.claude/agents/` in your project. Below the frontmatter, the body holds the instructions for the subagent. Only `name` and `description` are required.

| Part | What it does | Question you answer |
|---|---|---|
| `name` | Names the subagent. Required. | What do you call it, so that you can ask for it by name? |
| `description` | Tells Claude when to hand a task to it. Required. | In which situations should the main session use it? |
| `tools` | Lists the tools it may use. Optional: leave it out and it inherits every tool available to subagents. | What does the job need, and what must it not touch? |
| `model` | Picks the model it runs on. Optional: use an alias such as `sonnet`, or `inherit` for the model of the main session. | Does the job need a stronger or a cheaper model? |
| The body | The instructions below the frontmatter. | What is its job, and how should it work? |
| What it hands back | The summary it gives to the main session. | What does the main session need to read to decide the next step? |

Two subagents are built: a research agent that reads and reports, and a QA agent that checks behaviour with the tests and the scenarios of the spec.

A research agent that only reads does not need `Edit` or `Write`: leave them out of its `tools`. Tool names such as `Read`, `Edit`, `Write` and `Bash` are listed in the [tools reference](https://code.claude.com/docs/en/tools-reference).

If an agent keeps `Bash`, limit risky commands with the permission rules from Level 3, not with the `tools` line. A Bash deny rule in your settings applies to the main session and to subagents. Your Level 3 hooks live in your settings too, and hooks from settings files also apply inside subagents.

Participants ask the main session to write each file from their answers, or write it themselves. This is the whole shape of the frontmatter, with every value left for the participant:

```yaml
---
name: <name>
description: <when to use it>
tools: <tools>
model: <model>
---
```

On the `solution/level-3` branch and on the `start/level-4-<track>` branches, `.claude/agents/architecture-reviewer.md` is an example of such a file. It is an architecture reviewer, so it is neither your research agent nor your QA agent.

The official [guide to custom subagents](https://code.claude.com/docs/en/sub-agents) lists every field.

**Part C: Coordinate them from the main session.** A small feature, for example the Level 4 feature brief that you did not build. Use the research agent, stop and read what it handed back (the first human gate), run the spec flow of the track, use the QA agent, then commit.

1. Start `claude` in your project. If `.claude/agents/` did not exist when your session started, restart Claude Code first so that it loads your first agent file.
2. Run `./mvnw verify`, then commit through your `/ship` command from Level 3.

**Part D: Let Claude write it as a workflow.** A workflow is a script that Claude writes for the task you describe, and a runtime runs it in the background while your session stays responsive. The guide shows no script.

1. Run `claude --version`. You need 2.1.154 or later, the version that introduced dynamic workflows.
2. Workflows are available on all paid plans. On Pro, open `/config` and turn on the Dynamic workflows row.
3. Watch the run with `/workflows`.
4. If a run is worth keeping, save it from the `/workflows` view, in the project location, and run it again later as `/<name>`.

The official [guide to dynamic workflows](https://code.claude.com/docs/en/workflows) has the rest.

**Make it yours:** change your team until it fits you. Add a human gate, drop one or swap a role.

**Bonus cleared when**
- Your flow for one feature is written down, with at least one step marked as a human gate.
- Two subagent files of your own are in `.claude/agents/`, each with a description,
  the tools you chose and what it hands back.
- One feature went through your flow with both subagents, and `./mvnw verify` is green.
- If you have workflows: you read the script of one run before it ran, and you can say where your human gate sits.

**Debrief:** which gate did you keep human, and why?

## 10. Wrap-up (15 min)
- Show of hands: who cleared which level? Count Level 0 to Level 4.
- Two or three participants show their harness folder (`tree .claude`) and their spec.
- Map back to the keynote: context, behaviour, structure, verification.
- Take-home: the guide stays online; Anthropic Academy courses (Claude Code 101, Claude Code in Action,
  Introduction to agent skills, Introduction to subagents, Introduction to MCP); next steps: open harnesses
  (OpenCode) or building one on the Claude Agent SDK.
- Remind: the workshop API key is revoked tonight.

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Conference wifi | Participants run `./mvnw verify` at home (Maven dependencies, the local Node.js, npm packages and the PostgreSQL image are then cached). USB kit with `~/.m2` archive, `docker save` of the image, and the frontend caches. Distributor runs locally. |
| Docker missing or not running | The setup check runs `docker info`. Docker Desktop installers on the USB kit. Pairing as fallback. |
| Pro usage limit reached | `/model sonnet` from the setup check. `/usage` to check. Pair up if someone hits the limit. Facilitator has a spare Max account for demos. |
| `ANTHROPIC_API_KEY` exported by a participant | Claude Code would switch to that key. We only ever use `BOOKSTORE_AI_API_KEY`. The setup check confirms `/status` shows the subscription. |
| Workshop key abuse | Console spend limit, key scoped to a workshop workspace, revoked after the session. |
| jdtls fails to start, indexes slowly, or misreads the Maven multi-module build | Participants check the binary at home in the setup check (`which jdtls`). The plugin is installed only in Level 2 Part A, so Levels 0 and 1 run without it. On the day, a participant whose jdtls still does not start after the debugging options of Level 2 Part A continues with Part B. Native Windows, multi-module behaviour and first-index time are rehearsed in spec 10. |
| Windows participants | Claude Code runs natively; hooks are bash scripts, so recommend Git for Windows (Git Bash) or WSL2. Provide `.ps1` variants of the hook scripts in `solution/level-3`. Agent OS needs bash (WSL or Git Bash). jdtls via WSL. |
| MCP protocol version mismatch (MCP spec 2026-07-28 vs Spring AI 2.0.2 SDK) | Smoke test in spec 05 on the build machine and a second laptop. Fallback: STDIO transport (`./mvnw -pl distributor package`, then `claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio`). |
| Corporate laptops block installs | Email asks for admin rights or a personal laptop. Pairing as fallback. |
| Someone is far behind | At the start of each level: Level 0 `git stash -u && git switch -C level-0 level-0-start`, Level 1 `git stash -u && git switch -C level-1 level-0-start`, Levels 2 and 3 `git stash -u && git switch solution/level-N` (N one lower than the level), Level 4 `git stash -u && git switch start/level-4-<track>`. |
