# Spec 10: Harness reference solutions and level branches

> Modules: repository root · Depends on: 09 · Capability: `workshop-harness`

## Why
Participants need a known-good answer for every level, to compare against and to catch up with. These branches are
also the facilitator's demo material. Every file below is deliberately small: the lesson is that a good harness is
lean, layered, and verified, not long.

## What changes
Branches created from `level-0-start` (see `product/roadmap.md`):
`solution/level-1`, `solution/level-2`, `solution/level-3`, `start/level-4-openspec`, `start/level-4-speckit`,
`start/level-4-agentos`.
Each branch adds files on top of the previous one. `./mvnw verify` stays green on all of them.

## Out of scope
- Feature code. Only two exercise references exist: the multi-role contributor ACL contract test from Level 2
  Part B on `solution/level-2` (it proves the MCP exercise works) and the "cancel order" use case on
  `solution/level-3` (it proves the skill works).

---

## Branch `solution/level-1`: context engineering

### `CLAUDE.md` (at most 60 lines; this is the full reference text)
```markdown
# Agentic Bookstore

Spring Boot 4.1 / Java 25 online bookstore (module `bookstore`, PostgreSQL in Docker), a fake book distributor
(module `distributor`) and a React storefront (module `frontend`, built by Maven, served at `/shop/`).

## Commands
- Build and test everything: `./mvnw verify` (Windows: `mvnw.cmd verify`). Docker must be running (Testcontainers).
  Always use the wrapper, never `mvn`.
- One test class: `./mvnw -pl bookstore test -Dtest=BookTest`
- Run: `./mvnw -pl bookstore spring-boot:run` (port 8080), `./mvnw -pl distributor spring-boot:run` (port 8090)

## Definition of done
- `./mvnw verify` is green, including `ArchitectureTest`. Show the last lines of the output.
  Never say tests pass without having run them in this session.
- Every new behaviour has a test in the layer where the behaviour lives.

## Rules that are not obvious from a quick look
- Read docs/architecture.md before adding or moving classes. Inbound adapters (command, query, process, assistant)
  only call flows in `domaininteraction`. Only flows touch domain objects.
- `domain` is plain Java: no Spring, JPA, Jackson or Lombok imports.
- Money is always `Money` (BigDecimal, scale 2, HALF_EVEN). Never double or float.
- Schema changes only through a new Flyway migration in `bookstore/src/main/resources/db/migration`.
  Never edit an existing migration.
- No Lombok, no MapStruct: records and small hand-written mappers.
- The distributor is an external system. Bookstore code reaches it only through the `DistributorAppService` port.

## Working agreements
- Stay inside the scope of the request. Propose unrelated refactors instead of doing them.
- Small commits, conventional commit messages (feat:, fix:, test:, refactor:, docs:).
```

### `.claude/rules/domain.md`
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

### `.claude/rules/data-and-migrations.md`
```markdown
---
paths:
  - "bookstore/src/main/java/**/data/**"
  - "bookstore/src/main/resources/db/**"
---
# Data adapter and migrations
- `*DataServiceImpl` implements a `*DataService` port and speaks DTOs. JPA entities (`*JPA`) never leave `data`.
- Next migration number: the highest `V{n}__` file in `db/migration` plus one. File name `V{n}__{snake_case_description}.sql`.
- Hibernate only validates (`ddl-auto=validate`): every new column or table needs a migration, or startup fails.
- Seed data lives in `db/seed/R__seed_demo_data.sql` (repeatable, idempotent) and is not loaded in tests. Do not add
  test data there.
- Use `numeric(10,2)` plus a `varchar(3)` currency column for money.
```

### `.claude/rules/testing.md`
```markdown
---
paths:
  - "**/src/test/**"
---
# Tests
- One test class per production class, same package, suffix `Test`; end-to-end tests end with `IntegrationTest`.
- Web adapters: `@WebMvcTest` with `@MockitoBean` flows. Data adapters: `@DataJpaTest` on the shared Testcontainers
  PostgreSQL (`@Import(PostgresTestConfig.class)`, never an embedded database). ACL: `MockRestServiceServer`.
- Never call the real distributor or a real LLM. The assistant uses `ScriptedChatModel`.
- Name tests after behaviour: `rejectsOrderWhenStockIsInsufficient`.
- Never weaken or delete an `ArchitectureTest` rule to make a build pass.
```

### `CLAUDE.local.md.example` (the real `CLAUDE.local.md` is git-ignored)
```markdown
# My personal preferences (copy to CLAUDE.local.md, never commit)
- Explain your plan in three bullets before editing more than two files.
- I use IntelliJ; refer to files with their path from the repository root.
```

### Acceptance scenarios
#### Scenario: Rules load on demand
- **GIVEN** a fresh session on `solution/level-1`
- **WHEN** the user asks a question that does not touch `domain` and then runs `/memory` or `/context`
- **THEN** `CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md` is; after Claude edits a file in `domain`, the rule is loaded

#### Scenario: Level 0 task with context
- **WHEN** the Level 0 discount prompt is run three times in fresh sessions
- **THEN** at least two runs end with `./mvnw verify` green and a `Money`-based discount in the domain with a new migration (record outcomes)

---

## Branch `solution/level-2`: give your agent eyes (LSP + MCP)

`solution/level-1` plus `.mcp.json`, the MCP-assisted ACL contract test, the LSP setup note and the plugin
declaration below.

### `.mcp.json` (project scope)
```json
{
  "mcpServers": {
    "distributor": {
      "type": "http",
      "url": "${DISTRIBUTOR_MCP_URL:-http://localhost:8090/mcp}"
    }
  }
}
```

### `.claude/settings.json` (plugin declaration)
```json
{
  "extraKnownMarketplaces": {
    "claude-plugins-official": {
      "source": { "source": "github", "repo": "anthropics/claude-plugins-official" }
    }
  },
  "enabledPlugins": {
    "jdtls-lsp@claude-plugins-official": true
  }
}
```
Documented in the official docs (checked 2026-11-08): `enabledPlugins` and `extraKnownMarketplaces` may be set in a
repository's `.claude/settings.json` (https://code.claude.com/docs/en/settings-reference#enabledplugins,
https://code.claude.com/docs/en/settings-reference#extraknownmarketplaces, and "Require plugins per repository" at
https://code.claude.com/docs/en/plugins/org#require-plugins-per-repository). Claude Code honours
`extraKnownMarketplaces` only after the participant accepts the workspace trust dialog. A plugin that its
marketplace lists by a relative path loads from the marketplace copy once those entries apply; `jdtls-lsp` is listed
as `./plugins/jdtls-lsp` in the official `marketplace.json`. The docs describe no install prompt: the plugin should
simply load after trust, and the `jdtls` binary is still needed. *Re-verify at build time* (scenario below). If the
plugin does not load from this declaration, remove both keys and document only the install command
`/plugin install jdtls-lsp@claude-plugins-official`.

### `docs/lsp-setup.md` (the LSP setup note, for humans, at most 40 lines)
- What the language server gives: Claude Code's built-in, read-only LSP tool with go to definition, find
  references, type info, document and workspace symbols, implementations and call hierarchy; diagnostics are reported
  automatically after every edit. No enable flag exists or is needed.
- Install the binary (the plugin does not ship it; JDK 17 or later): macOS `brew install jdtls`; Arch Linux
  `yay -S jdtls`; other Linux: extract a JDT LS release and put a `jdtls` wrapper script on the `PATH`; Windows: WSL.
  Check with `which jdtls` (PowerShell: `Get-Command jdtls`).
- Install the plugin: `/plugin install jdtls-lsp@claude-plugins-official`; if the marketplace is missing, first
  `/plugin marketplace add anthropics/claude-plugins-official`. On this branch the plugin is also declared in
  `.claude/settings.json`.
- Verify: a deliberate type error in a `.java` file makes Claude Code print `Found N new diagnostic issues ...`.
- Troubleshooting: nothing appears, check `/plugin` (tab **Errors**) and `claude --debug`;
  `Executable not found in $PATH: "jdtls"` means the binary is missing.
- Reference: https://code.claude.com/docs/en/plugins/code-intelligence

### Level 2 exercise reference implementation
The multi-role contributor ACL contract test from Level 2 Part B: a test in `DistributorAppServiceImplTest` (spec
06) with `MockRestServiceServer` and a response body under `src/test/resources/contracts/distributor/` whose shape
was taken from the distributor MCP (`get_title` on a title with a TRANSLATOR or ILLUSTRATOR contributor). It proves
that only authors end up in our catalogue. Built in Claude Code with the distributor MCP connected.

### Requirement: The language server works on the real repository
On `solution/level-2`, Claude Code's LSP tool SHALL report diagnostics and resolve references for the bookstore
module through the `jdtls-lsp` plugin.

#### Scenario: Type error is reported and fixed
- **GIVEN** `solution/level-2` and `jdtls` installed
- **WHEN** a type error is introduced in a bookstore domain class
- **THEN** Claude Code reports the diagnostic and the agent fixes it

#### Scenario: Find references across layers
- **GIVEN** `solution/level-2` and `jdtls` installed
- **WHEN** Claude is asked to find all references to `Money.of`
- **THEN** the LSP tool returns usages in at least two layers

#### Scenario: Plugin loads from the project declaration
- **GIVEN** a fresh clone of `solution/level-2`, `jdtls` on the `PATH`, and no user-scope install of `jdtls-lsp`
- **WHEN** Claude Code starts in the repository and the participant accepts the workspace trust dialog
- **THEN** `/plugin` lists `jdtls-lsp` as enabled without a manual install (record the result; if it fails, apply
  the fallback above)

### Acceptance scenarios
#### Scenario: MCP is available
- **GIVEN** the distributor is running
- **WHEN** Claude Code starts in the repository and the user approves the project MCP server
- **THEN** `/mcp` shows `distributor` connected with 4 tools

#### Scenario: ACL contract test from real MCP data
- **WHEN** `./mvnw verify` runs on `solution/level-2`
- **THEN** the multi-role contributor contract test is green and asserts that only authors are imported

---

## Branch `solution/level-3`: behavioral control

`solution/level-2` plus permissions, hooks, the git hook, the skill, the command, the subagent and the PowerShell
hook variants.

### `.claude/settings.json`
Extends the file from `solution/level-2`: keep its `extraKnownMarketplaces` and `enabledPlugins` keys and add
`permissions` and `hooks`.
```json
{
  "permissions": {
    "allow": [
      "Bash(./mvnw *)",
      "Bash(git status *)",
      "Bash(git diff *)",
      "Bash(git add *)",
      "Bash(git commit *)"
    ],
    "deny": [
      "Read(./.env)",
      "Read(./.env.*)",
      "Bash(git push)",
      "Bash(git push *)",
      "Bash(* --no-verify*)",
      "Bash(git commit -n *)"
    ]
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "if": "Bash(git commit *)",
            "timeout": 180,
            "onFailure": "block",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/tests-before-commit.sh"
          }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "timeout": 120,
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/architecture-check-on-stop.sh"
          }
        ]
      }
    ]
  }
}
```

Decision D9 (Maik, 2026-11-09): a commit hook that times out or fails to run blocks the commit
(`"onFailure": "block"`), so a slow first build never lets an untested commit through. Verify at build time that the
field exists in the hooks schema of the Claude Code version in use; if it does not, keep the pack's behaviour, record
the gap in the guide and report it to Maik.

### `.claude/hooks/tests-before-commit.sh` (chmod +x, LF line endings)
```bash
#!/usr/bin/env bash
# PreToolUse hook: block `git commit` unless the full build is green.
# Exit code 2 blocks the tool call and feeds stderr back to Claude. Any other non-zero code does NOT block.
input="$(cat)"
# The "if" filter in settings.json is best-effort, so check the command ourselves too.
if ! printf '%s' "$input" | grep -Eq '"command"[[:space:]]*:[[:space:]]*"[^"]*git[[:space:]]+commit'; then
  exit 0
fi
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 1
log="$(mktemp)"
if ./mvnw -q verify >"$log" 2>&1; then
  rm -f "$log"
  exit 0
fi
{
  echo "Commit blocked: ./mvnw verify failed. Fix the cause (do not weaken tests), then commit again."
  tail -n 40 "$log"
} >&2
rm -f "$log"
exit 2
```

### `.claude/hooks/architecture-check-on-stop.sh` (chmod +x)
```bash
#!/usr/bin/env bash
# Stop hook: when Java files changed, run only the fast ArchitectureTest before Claude may finish.
input="$(cat)"
if printf '%s' "$input" | grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  exit 0   # we already blocked once in this turn; avoid loops
fi
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 1
if git diff --quiet HEAD -- '*.java' && [ -z "$(git ls-files --others --exclude-standard -- '*.java')" ]; then
  exit 0   # no Java changes
fi
if ! out="$(./mvnw -q -pl bookstore test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1)"; then
  { echo "ArchitectureTest fails. Fix the layering before you finish:"; printf '%s\n' "$out" | tail -n 30; } >&2
  exit 2
fi
exit 0
```

PowerShell variants `tests-before-commit.ps1` and `architecture-check-on-stop.ps1` with identical behaviour, plus a
note in the guide on how Windows participants point the hook command at them. Windows participants need Git for
Windows (Git Bash), because the `Bash` matcher, the `if` field and the `Bash(...)` permission rules only apply when
Claude Code runs commands through Bash (decision D9); the setup check and the guide state this prerequisite, and no
`PowerShell(...)` handlers or rules are added. *Verify on a Windows machine* which
shell Claude Code uses for hook commands there and document the result.

### `.githooks/pre-commit` (chmod +x) and activation
```sh
#!/usr/bin/env sh
echo "pre-commit: ./mvnw -q verify"
./mvnw -q verify || { echo "pre-commit: build failed, commit aborted" >&2; exit 1; }
```
Activation (documented in the guide, run once per clone): `git config core.hooksPath .githooks`.

### `.claude/skills/new-use-case/SKILL.md`
```markdown
---
name: new-use-case
description: Adds or changes a bookstore use case end to end through the Clean Hexagonal Onion layers (domain behaviour, flow, ports, command or query adapter, data adapter, migration, tests). Use when asked to add a feature, endpoint, business rule, command or query in the bookstore module.
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(./mvnw *)
---
# New use case

Work through these steps in order. Do not skip tests.

1. Understand: restate the use case in one sentence and list its business rules. Find the closest existing use
   case and read it end to end (command or query class, flow method, domain behaviour, tests). Mirror its shape.
2. Domain first: add behaviour to the aggregate (no setters), invariants as domain exceptions, a domain event if
   something else must react. Write the domain test first.
3. Flow: add a method to the right `*Flow`: load through the port, map to domain, call the behaviour, save,
   publish pulled events. Translate domain exceptions into `NotFoundException`, `ConflictException`,
   `InvalidInputException` or `UnavailableException`. Unit test it with mocked ports.
4. Ports and data: extend a `*DataService` only if needed; implement it in `data`; add a new Flyway migration if the
   schema changes.
5. Adapter: a command (`POST .../commands/{action}`, `*Payload` with validation, 202) or a query (`GET`, `*View`).
   The adapter's exception handler already maps the four use-case exceptions to ProblemDetail. Add a web slice test.
6. End to end: one integration test per acceptance scenario.
7. Verify: run `./mvnw verify` and paste the summary. If `ArchitectureTest` fails, fix the design, never the rule.

Layer cheat sheet and naming rules: [reference.md](reference.md)
```

### `.claude/skills/new-use-case/reference.md`
A one-page table per layer: package, allowed dependencies, class suffixes, test type, and the path of one exemplary
existing class per layer (for example the place-order chain: `OrderCommands`, `PlaceOrderPayload`, `OrderFlow`,
`Order`, `OrderDataServiceImpl`, `OrderIntegrationTest`). Generate it from `docs/architecture.md`; keep it under 80 lines.

### `.claude/commands/ship.md`
```markdown
---
description: Verify, check scope and commit the current change with a conventional commit message
argument-hint: [what this change does]
disable-model-invocation: true
allowed-tools: Bash(git status *), Bash(git diff *), Bash(git add *), Bash(git commit *), Bash(./mvnw *)
---
## Current state
- Status: !`git status --short`
- Changed files: !`git diff --stat HEAD`

## Task
1. Run `./mvnw verify` and show the final summary lines. Stop and report if it fails.
2. Check that every changed file belongs to: $ARGUMENTS. List anything outside that scope and ask before including it.
3. Stage the relevant files and commit with a conventional commit message whose subject summarises: $ARGUMENTS.
4. Report the commit hash and the message.
```

### `.claude/agents/architecture-reviewer.md` (stretch)
```markdown
---
name: architecture-reviewer
description: Reviews the current uncommitted bookstore changes against the onion layer rules, money, migration and testing conventions. Use before committing a feature or when asked for an architecture review.
tools: Read, Glob, Grep, Bash
model: sonnet
---
You review changes in the Agentic Bookstore. Only use Bash for `git diff` and `git status`; never edit files.
Read docs/architecture.md first. Then review `git diff HEAD` and new untracked files.
Report either "PASS" or a numbered list of findings. Each finding: file and line, the rule it breaks
(quote it from docs/architecture.md), and a one-line fix suggestion. Do not report style nits.
```

### Level 3 exercise reference implementation
"Customers can cancel an order that has not shipped yet": `OrderStatus.CANCELLED`, `Order.cancel(Clock)`,
`StockItem.release(Quantity)`, `OrderFlow.cancelOrder`, `POST /orders/{id}/commands/cancel` (202; 404; 409 when
shipped or already cancelled), migration if a `cancelled_at` column is added, tests per layer. Built by running the
`new-use-case` skill in Claude Code, committed through `/ship`.

### Acceptance scenarios
#### Scenario: Red build blocks the commit
- **GIVEN** a deliberately failing test
- **WHEN** Claude runs `git commit -m "test"`
- **THEN** the hook exits 2, the commit does not happen, and Claude's next message refers to the failing test from stderr

#### Scenario: Escape hatch is closed
- **WHEN** Claude attempts `git commit --no-verify -m "x"`, `git commit -n -m "x"` or `git push`
- **THEN** the permission system denies the command

#### Scenario: Secrets stay unread
- **GIVEN** a `.env` file in the repository root
- **WHEN** Claude is asked to read it
- **THEN** the read is denied

#### Scenario: Skill is selected automatically
- **WHEN** a fresh session receives "Customers must be able to cancel an order that has not shipped yet."
- **THEN** Claude loads the `new-use-case` skill without being told to (visible in the transcript)

---

## Branches `start/level-4-*`: spec-driven development tracks
Each is `solution/level-3` plus one framework installed, initialised and committed, so participants skip installation.
Pin versions from `product/tech-stack.md`. Commit the generated files as they are; review them for anything that
contradicts `CLAUDE.md`.

### `start/level-4-openspec`
1. `openspec init --tools claude` (non-interactive).
2. `openspec/config.yaml`: add project context and artefact rules (*verify the key names against the installed
   version's `docs/customization.md`*):
   ```yaml
   context: |
     Java 25, Spring Boot 4.1.2, Spring AI 2.0.2, Maven wrapper (./mvnw). Modules: frontend, bookstore, distributor.
     Architecture: Clean Hexagonal Onion, see docs/architecture.md; ArchitectureTest is the referee.
     Definition of done: ./mvnw verify green, every scenario covered by a test.
   rules:
     design:
       - Name the onion layer of every new or changed class.
     tasks:
       - Order tasks domain, flow, data and migration, adapter, integration test.
       - Every requirement scenario maps to at least one test task.
   ```
3. Seed one living spec so participants see the format: `openspec/specs/ordering/spec.md` describing the existing
   place and ship order behaviour (copy the requirements from spec 04 of this pack, `### Requirement:` and
   `#### Scenario:` headings with WHEN/THEN bullets). `openspec validate --strict` must pass.

### `start/level-4-speckit`
1. `uv tool install specify-cli==1.3.1`, then
   `specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation"`
   (the option keeps Spec Kit's skills out of Claude's automatic skill listing; participants invoke them explicitly).
2. Run `/speckit-constitution` with this input, review, commit `.specify/memory/constitution.md`:
   > I. Onion layers: code follows docs/architecture.md; ArchitectureTest must stay green and its rules are never
   > weakened. II. Tests first, per layer, one test per acceptance scenario. III. Money is exact (Money, BigDecimal),
   > never floating point. IV. Schema changes only through new Flyway migrations. V. Small, scoped changes that
   > pass ./mvnw verify before every commit.
3. Diff `.claude/settings.json` after init and make sure the Level 3 hooks and permissions and the
   Level 2 plugin declaration are intact.

### `start/level-4-agentos`
1. `git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git` (main branch,
   includes install-script fixes after v3.0.0), then `~/agent-os/scripts/project-install.sh` in the repository.
2. `/plan-product` with `product/mission.md` and `product/tech-stack.md` of this pack as input (paste them from the
   facilitator's copy of the pack: `docs/spec-pack/` exists only on `factory`, never on `main`-based branches)
   (produces `agent-os/product/`).
3. `/discover-standards` for architecture, testing and persistence; review the generated files in
   `agent-os/standards/` against `docs/architecture.md`; then `/index-standards`.
4. Commit everything.

### Acceptance scenario (all three tracks)
#### Scenario: Rehearsed in time
- **GIVEN** a fresh clone on the track's start branch
- **WHEN** a facilitator who has not used the framework before ships Level 4 brief A following `workshop/sdd-framework-guide.md`
- **THEN** the feature is committed through `/ship` with verify green within 35 minutes (record the time per track)

## Tasks
Decision D16 (Maik, 2026-11-10): no PowerShell hook variants. Every PowerShell (`.ps1`) hook, PowerShell
hook tab and PowerShell parity requirement in this spec is dropped. Level 3 ships the bash hooks and the git
pre-commit hook only; on Windows they run through Git Bash (decision D9).

Decision D14 (Maik, 2026-11-09): no agent runs Claude Code headlessly for this spec. Maik rehearses every level
himself (2026-11-10). Agents build every file of every branch directly and replace each "run its scenarios" and
"rehearse" step below with a careful consistency check: every file matches this spec, the guide (spec 11) and the
code on `main`; every path, command, skill, hook, agent and setting a scenario names exists and is spelled the same
everywhere; `./mvnw clean verify` is green on each branch. Files that a Claude Code step would generate (the Spec Kit
constitution, the Agent OS `/plan-product` output and the discovered and indexed standards) are written directly
from this pack's product docs and listed for Maik's rehearsal review. The scenarios that need a Claude Code session
(including the LSP rehearsal of task 3 and the "Rehearsed in time" scenario) become Maik's manual rehearsal checks.

1. Create `solution/level-1` with the files above; run its scenarios.
2. Create `solution/level-2`: `.mcp.json`, the plugin declaration, `docs/lsp-setup.md`; build the multi-role ACL
   contract test with the distributor MCP; run its scenarios.
3. LSP rehearsal: run the LSP scenarios on macOS against the real repository (decision D8: macOS only for now; the
   Linux and Windows-through-WSL runs are a manual check for Maik before the workshop). Record the first-index time
   and whether the Maven multi-module build resolves (references across `bookstore` packages, no false
   diagnostics). If LSP fails, stop and ask Maik before writing a workaround.
4. Create `solution/level-3`; write the PowerShell variants; implement the cancel-order exercise with the skill; run its scenarios on macOS or Linux and on Windows.
5. Create the three `start/level-4-*` branches; rehearse each.
6. (Dropped by D17: no Level 4 solution branches; building a Level 4 feature is up to the participants.)

## Definition of done
- All branches exist, verify green on each, every acceptance scenario above checked and recorded: by the consistency
  check where no Claude Code session is needed, and listed as Maik's manual rehearsal check where it is (decision D14).
- LSP rehearsal on macOS listed as Maik's manual rehearsal check (decision D14); Linux and Windows through WSL listed as a
  manual check for Maik (decision D8).
