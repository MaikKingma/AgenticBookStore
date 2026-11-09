# Spec 09: Workshop baseline (seed data, human docs, sanitised main, level-0 tag)

> Modules: all · Depends on: 01 to 08 and 12 · Capability: `workshop-baseline`

## Why
This spec turns a finished app into a workshop starting point: realistic demo data, the documentation a real
brownfield repo would have (for humans, not agents), a `main` branch without any agent configuration, and proof
that Level 0 actually produces the failure modes the keynote promises.

## What changes
- Demo seed data loaded only outside tests.
- `http/` request collection and `docs/architecture.md` for humans.
- `README.md` written for developers.
- `scripts/publish-workshop-main.sh` and a CI guard that keeps `main` free of agent configuration.
- Tag `level-0-start`; Level 0 rehearsal record (facilitator-only, not in the repo).

## Out of scope
- Any `CLAUDE.md`, `.claude/`, `AGENTS.md`, `.mcp.json`, SDD folders on `main` (spec 10 adds them on level branches).

## Seed data
- Flyway locations: the main `application.yml` (default profile) sets `classpath:db/migration,classpath:db/seed`;
  `application-test.yml` sets `classpath:db/migration` only.
- The seed is a repeatable migration `db/seed/R__seed_demo_data.sql` (no version number, so it never interferes
  with the numbering of new migrations in `db/migration`). Every statement is idempotent PostgreSQL SQL
  (`INSERT INTO ... ON CONFLICT DO NOTHING` or `INSERT INTO ... SELECT ... WHERE NOT EXISTS (...)`). There is no
  other database to stay portable with.
- 100 books, at least 10 AVAILABLE books per genre (decision D12). 20 of them are exactly
  the ISBNs in `distributor/src/main/resources/catalog/bookstore-shared-isbns.txt`
  (spec 05), with the same titles and authors. The other 80 are bookstore-only. Mix genres; a few discontinued
  (bookstore-only ones).
- Stock: varied; at least 5 books at or below their reorder level, all of them among the 20 shared ISBNs (so
  `demo-flow.http` can show a restock), 3 with zero stock, none with a pending restock.
- 6 demo orders for 3 customers (`reader@example.com`, `bookworm@example.org`, `night.owl@example.net`), 2 shipped.
  The `reserved` quantity of every stock item equals the sum of its lines in PLACED orders, so shipping a demo order works.
- ISBNs must pass the checksum; data is fictional demo data (state this in the README).

## Human documentation
- `README.md` (at most 120 lines): what the app is, prerequisites (Java 25 and Docker; no Node.js, the build installs
  its own), run commands: `./mvnw verify` once, then `./mvnw -pl bookstore spring-boot:run` (starts PostgreSQL through Docker
  Compose, serves the storefront built by the full build at http://localhost:8080/shop/) and `./mvnw -pl distributor spring-boot:run`, plus how to run the jar
  against an existing PostgreSQL (`SPRING_DATASOURCE_URL`, user and password `bookstore`) and how to reset the
  database (`docker compose -f bookstore/compose.yaml down`); the module map including `frontend/` and `guide/`;
  a short architecture paragraph with the onion diagram and a link to `docs/architecture.md`; endpoint overview;
  a "Storefront" section (`/shop/`, Vite dev server for UI work with the Maven-installed npm: `frontend/node/npm run dev` in `frontend/`, proxy to 8080);
  "Assistant" section (`BOOKSTORE_AI_API_KEY`, offline mode, and the warning not to use `ANTHROPIC_API_KEY`);
  test commands (`./mvnw verify` needs Docker running). It reads like a normal team README. It contains no
  instructions addressed to AI agents.
- `docs/architecture.md`: the architecture standard of this pack, rewritten for the team (layers, rules, naming,
  testing, the documented process exception, the synchronous restock trade-off). Level 1 imports it from `CLAUDE.md`.
- `http/*.http`: `catalogue.http`, `inventory.http`, `orders.http`, `assistant.http`, `distributor.http`,
  and `demo-flow.http` (search, place order, see low stock, see pending restock, receive stock).

## Publishing `main`
`scripts/publish-workshop-main.sh` (bash, idempotent):
1. Requires a clean working tree on `factory`.
2. Creates a temporary worktree of `factory`, removes these paths if present: `CLAUDE.md`, `CLAUDE.local.md`,
   `AGENTS.md`, `.claude/` (this includes the SDD tooling's `.claude/agent-memory/`, `.claude/comms/`,
   `.claude/worktrees/`, `.claude/commands/`, `.claude/skills/` and `.claude/settings.json`), `.mcp.json`,
   `openspec/`, `.specify/`, `specs/`, `agent-os/` (including the `agent-os/product` symlink and
   `agent-os/specs/`, where the SDD cycle writes its plan artefacts), `.githooks/`,
   `docs/spec-pack/` (this pack contains the level solutions, the orchestrator progress file and facilitator notes,
   so it must never reach `main`). The list lives once, in the script, and the CI guard below reads the same list.
3. Commits the result as a single commit "Agentic Bookstore: workshop baseline" on an orphan-based `main`
   (history starts fresh, so participants do not see agent-generated history), force-updates `main`, and moves tag
   `level-0-start` to it. Prints what it removed.

CI job `no-agent-config` (runs only on `main` and on tag `level-0-start`): fails if any of the paths above exist,
including `.claude/agent-memory`, `.claude/comms`, `.claude/worktrees`, `agent-os/specs` and `docs/spec-pack`,
and names the offending path.

## Level 0 traps (verify they are present, do not add hints)
- No discount, percentage or promotion concept anywhere.
- `ddl-auto=validate`: a new column without a migration fails startup and the integration tests.
- ArchUnit money rule (no double/float) and JPA-only-in-data rule.
- `package-info.java` files stay one sentence.

## Requirements

### Requirement: Demo data in local runs only
The application SHALL load the demo data when run locally and SHALL never load it in tests.

#### Scenario: Local run
- **WHEN** the bookstore starts with the default profile (PostgreSQL through Docker Compose support) and `GET /books` is called
- **THEN** 100 books are returned, at least 10 AVAILABLE per genre,, and the storefront at `/shop/` lists them

#### Scenario: Tests are unaffected
- **WHEN** the test suite runs
- **THEN** no test sees seeded rows (an integration test asserts an empty catalogue at start)

### Requirement: Main has no agent configuration
The `main` branch SHALL contain no agent or SDD configuration, enforced by CI.

#### Scenario: Guard
- **GIVEN** a commit on `main` that adds `.claude/settings.json`
- **WHEN** CI runs
- **THEN** the `no-agent-config` job fails and names the path

#### Scenario: Publishing
- **GIVEN** `factory` contains `CLAUDE.md`, `.claude/` (with `agent-memory/` and `skills/`), `agent-os/` (with the `product` symlink and `specs/`) and `docs/spec-pack/`
- **WHEN** `scripts/publish-workshop-main.sh` runs
- **THEN** `main` has one commit, none of those paths, `./mvnw verify` is green on it (CI on `ubuntu-latest` with Docker), and tag `level-0-start` points to it

### Requirement: Level 0 produces observable failures
The baseline SHALL make the Level 0 task fail visibly for an unconfigured agent, with at least three failure modes per run.

#### Scenario: Rehearsal (facilitator, manual)
- **GIVEN** a fresh clone of `level-0-start` and a Claude Code session with no user-level `~/.claude/CLAUDE.md`
- **WHEN** the Level 0 prompt from the workshop plan is run three times in fresh sessions with `/model sonnet` (what participants use)
- **THEN** every run shows at least three of: ArchUnit failure, failing or missing tests, missing migration, double for money, logic outside domain, unverified "tests pass" claim, each with evidence (the failing test, the ArchUnit output or the wrong file). This is the same bar as "Level cleared when" for Level 0. Record the outcomes and the evidence in the facilitator notes.
- If any run shows fewer than three failure modes with evidence, use the fallback prompts below and rehearse again.

Fallback Level 0 prompts (more ambiguous, same area):
1. "Black Friday is coming. Make books cheaper for a weekend and show it in search."
2. "Marketing wants a 'sale' badge and a crossed-out old price in the catalogue API."

## Tests (required)
- Integration test asserting an empty catalogue in tests.
- A script test (bash, run in CI on Linux) for `publish-workshop-main.sh` against a temporary repository.

## Tasks
1. Seed SQL (PostgreSQL syntax), Flyway location config per profile.
2. `docs/architecture.md`, `README.md`, `http/` collection.
3. Publish script, its test, CI guard job.
4. Run the script; verify `main`; create tag.
5. Level 0 rehearsal three times; record results; adjust prompts if needed.

## Definition of done
- `main` = one commit, verify green, guard green, tag `level-0-start` set, rehearsal recorded.
