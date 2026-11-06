# Spec 10: Harness reference solutions and lab branches

> Modules: repository root · Depends on: 09 · Capability: `workshop-harness`

## Why
Participants need a known-good answer for every lab, to compare against and to catch up with. These branches are
also the facilitator's demo material. Every file below is deliberately small: the lesson is that a good harness is
lean, layered, and verified, not long.

## What changes
Branches created from `lab-1-start` (see `product/roadmap.md`):
`solution/lab-2`, `solution/lab-3`, `start/lab-4-openspec`, `start/lab-4-speckit`, `start/lab-4-agentos`.
Each branch adds files on top of the previous one. `./mvnw verify` stays green on all of them.

## Out of scope
- Feature code. Only two Lab 3 exercise references live on `solution/lab-3`: the "cancel order" use case (it proves
  the skill works) and the multi-role contributor ACL contract test from Part D (it proves the MCP exercise works).

---

## Branch `solution/lab-2`: context engineering

### `CLAUDE.md` (keep under 60 lines; this is the full reference text)
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
- **GIVEN** a fresh session on `solution/lab-2`
- **WHEN** the user asks a question that does not touch `domain` and then runs `/memory` or `/context`
- **THEN** `CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md` is; after Claude edits a file in `domain`, the rule is loaded

#### Scenario: Lab 1 task with context
- **WHEN** the Lab 1 discount prompt is run three times in fresh sessions
- **THEN** at least two runs end with `./mvnw verify` green and a `Money`-based discount in the domain with a new migration (record outcomes)

---

## Branch `solution/lab-3`: behavioural controls

### `.claude/settings.json`
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
note in the guide on how Windows participants point the hook command at them. *Verify on a Windows machine* which
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

### Lab 3 exercise reference implementation
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

#### Scenario: MCP is available
- **GIVEN** the distributor is running
- **WHEN** Claude Code starts in the repository and the user approves the project MCP server
- **THEN** `/mcp` shows `distributor` connected with 4 tools

---

## Branches `start/lab-4-*`: spec-driven development tracks
Each is `solution/lab-3` plus one framework installed, initialised and committed, so participants skip installation.
Pin versions from `product/tech-stack.md`. Commit the generated files as they are; review them for anything that
contradicts `CLAUDE.md`.

### `start/lab-4-openspec`
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

### `start/lab-4-speckit`
1. `uv tool install specify-cli==1.3.1`, then
   `specify init --here --force --integration claude --script sh --integration-options="--no-model-invocation"`
   (the option keeps Spec Kit's skills out of Claude's automatic skill listing; participants invoke them explicitly).
2. Run `/speckit-constitution` with this input, review, commit `.specify/memory/constitution.md`:
   > I. Onion layers: code follows docs/architecture.md; ArchitectureTest must stay green and its rules are never
   > weakened. II. Tests first, per layer, one test per acceptance scenario. III. Money is exact (Money, BigDecimal),
   > never floating point. IV. Schema changes only through new Flyway migrations. V. Small, scoped changes that
   > pass ./mvnw verify before every commit.
3. Diff `.claude/settings.json` after init and make sure the Lab 3 hooks and permissions are intact.

### `start/lab-4-agentos`
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
- **WHEN** a facilitator who has not used the framework before ships Lab 4 brief A following `workshop/sdd-framework-guide.md`
- **THEN** the feature is committed through `/ship` with verify green within 35 minutes (record the time per track)

## Tasks
1. Create `solution/lab-2` with the files above; run its scenarios.
2. Create `solution/lab-3`; write the PowerShell variants; implement the cancel-order exercise with the skill; run its scenarios on macOS or Linux and on Windows.
3. Create the three `start/lab-4-*` branches; rehearse each.
4. Optionally create `solution/lab-4-<track>-<brief>` from the rehearsals.

## Definition of done
- All branches exist, verify green on each, every acceptance scenario above checked and recorded.
