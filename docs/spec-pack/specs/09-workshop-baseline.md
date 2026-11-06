# Spec 09: Workshop baseline (seed data, human docs, sanitised main, lab-1 tag)

> Modules: both · Depends on: 01 to 08 · Capability: `workshop-baseline`

## Why
This spec turns a finished app into a workshop starting point: realistic demo data, the documentation a real
brownfield repo would have (for humans, not agents), a `main` branch without any agent configuration, and proof
that Lab 1 actually produces the failure modes the keynote promises.

## What changes
- Demo seed data loaded only outside tests.
- `http/` request collection and `docs/architecture.md` for humans.
- `README.md` written for developers.
- `scripts/publish-workshop-main.sh` and a CI guard that keeps `main` free of agent configuration.
- Tag `lab-1-start`; Lab 1 rehearsal record (facilitator-only, not in the repo).

## Out of scope
- Any `CLAUDE.md`, `.claude/`, `AGENTS.md`, `.mcp.json`, SDD folders on `main` (spec 10 adds them on lab branches).

## Seed data
- Flyway locations: the main `application.yml` (default profile) sets `classpath:db/migration,classpath:db/seed`;
  `application-test.yml` sets `classpath:db/migration` only.
- The seed is a repeatable migration `db/seed/R__seed_demo_data.sql` (no version number, so it never interferes
  with the numbering of new migrations in `db/migration`). Every statement is idempotent and portable between H2 and
  PostgreSQL (`INSERT INTO ... SELECT ... WHERE NOT EXISTS (...)`).
- 40 books. 20 of them are exactly the ISBNs in `distributor/src/main/resources/catalog/bookstore-shared-isbns.txt`
  (spec 05), with the same titles and authors. The other 20 are bookstore-only. Mix genres; a few discontinued
  (bookstore-only ones).
- Stock: varied; at least 5 books at or below their reorder level, all of them among the 20 shared ISBNs (so
  `demo-flow.http` can show a restock), 3 with zero stock, none with a pending restock.
- 6 demo orders for 3 customers (`reader@example.com`, `bookworm@example.org`, `night.owl@example.net`), 2 shipped.
  The `reserved` quantity of every stock item equals the sum of its lines in PLACED orders, so shipping a demo order works.
- ISBNs must pass the checksum; data is fictional demo data (state this in the README).

## Human documentation
- `README.md` (at most 120 lines): what the app is, prerequisites (Java 25 only), run commands for both modules,
  the module map, a short architecture paragraph with the onion diagram and a link to `docs/architecture.md`,
  endpoint overview, "Assistant" section (`BOOKSTORE_AI_API_KEY`, offline mode, and the warning not to use
  `ANTHROPIC_API_KEY`), test commands. It reads like a normal team README. It contains no instructions addressed to
  AI agents.
- `docs/architecture.md`: the architecture standard of this pack, rewritten for the team (layers, rules, naming,
  testing, the documented process exception, the synchronous restock trade-off). Lab 2 imports it from `CLAUDE.md`.
- `http/*.http`: `catalogue.http`, `inventory.http`, `orders.http`, `assistant.http`, `distributor.http`,
  and `demo-flow.http` (search, place order, see low stock, see pending restock, receive stock).

## Publishing `main`
`scripts/publish-workshop-main.sh` (bash, idempotent):
1. Requires a clean working tree on `factory`.
2. Creates a temporary worktree of `factory`, removes these paths if present: `CLAUDE.md`, `CLAUDE.local.md`,
   `AGENTS.md`, `.claude/`, `.mcp.json`, `openspec/`, `.specify/`, `specs/`, `agent-os/`, `.githooks/`,
   `docs/spec-pack/` (this pack contains the lab solutions and facilitator notes, so it must never reach `main`).
3. Commits the result as a single commit "Agentic Bookstore: workshop baseline" on an orphan-based `main`
   (history starts fresh, so participants do not see agent-generated history), force-updates `main`, and moves tag
   `lab-1-start` to it. Prints what it removed.

CI job `no-agent-config` (runs only on `main` and on tag `lab-1-start`): fails if any of the paths above exist.

## Lab 1 traps (verify they are present, do not add hints)
- No discount, percentage or promotion concept anywhere.
- `ddl-auto=validate`: a new column without a migration fails startup and the integration tests.
- ArchUnit money rule (no double/float) and JPA-only-in-data rule.
- `package-info.java` files stay one sentence.

## Requirements

### Requirement: Demo data in local runs only
The application SHALL load the demo data when run locally and SHALL never load it in tests.

#### Scenario: Local run
- **WHEN** the bookstore starts with the default profile and `GET /books` is called
- **THEN** 40 books are returned

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
- **GIVEN** `factory` contains `CLAUDE.md`, `.claude/`, `openspec/` and `docs/spec-pack/`
- **WHEN** `scripts/publish-workshop-main.sh` runs
- **THEN** `main` has one commit, none of those paths, `./mvnw verify` is green on it, and tag `lab-1-start` points to it

### Requirement: Lab 1 produces observable failures
The baseline SHALL make the Lab 1 task fail visibly for an unconfigured agent in most runs.

#### Scenario: Rehearsal (facilitator, manual)
- **GIVEN** a fresh clone of `lab-1-start` and a Claude Code session with no user-level `~/.claude/CLAUDE.md`
- **WHEN** the Lab 1 prompt from the workshop plan is run three times in fresh sessions with `/model sonnet` (what participants use)
- **THEN** at least two runs show at least one of: ArchUnit failure, failing or missing tests, missing migration, double for money, logic outside domain, unverified "tests pass" claim. Record the outcomes in the facilitator notes.
- If fewer than two runs fail, use the fallback prompts below and rehearse again.

Fallback Lab 1 prompts (more ambiguous, same area):
1. "Black Friday is coming. Make books cheaper for a weekend and show it in search."
2. "Marketing wants a 'sale' badge and a crossed-out old price in the catalogue API."

## Tests (required)
- Integration test asserting an empty catalogue in tests.
- A script test (bash, run in CI on Linux) for `publish-workshop-main.sh` against a temporary repository.

## Tasks
1. Seed SQL, Flyway location config per profile.
2. `docs/architecture.md`, `README.md`, `http/` collection.
3. Publish script, its test, CI guard job.
4. Run the script; verify `main`; create tag.
5. Lab 1 rehearsal three times; record results; adjust prompts if needed.

## Definition of done
- `main` = one commit, verify green, guard green, tag `lab-1-start` set, rehearsal recorded.
