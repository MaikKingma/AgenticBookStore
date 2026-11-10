# Shape: Spec 09 Workshop baseline

Source of truth: `docs/spec-pack/specs/09-workshop-baseline.md`. This file records the scope of this cycle and the
shaping decisions; it never changes a requirement, scenario, path or name of the pack spec.

## Scope of this cycle

- In: pack task 1 (seed data, Flyway locations per profile), pack task 2 (`README.md`, `docs/architecture.md`,
  `http/` collection), pack task 3 (`scripts/publish-workshop-main.sh`, its bash test, the CI guard job
  `no-agent-config`).
- Out: pack task 4 (run the script against the GitHub remote, verify `main`, create tag `level-0-start`). That is the
  owner's gate; the main session runs it after his approval. Nobody in this cycle pushes `main`, force-updates it or
  creates or moves `level-0-start` on the remote; every proof uses temporary local repositories.
- Out: pack task 5 (Level 0 rehearsal). It is a facilitator activity. This cycle prepares
  `rehearsal-record-template.md` in this folder and never claims a rehearsal was run.
- Spec 08 (assistant) is not merged into `factory` while this cycle runs. Assistant docs follow the spec 08 text and
  the code on `origin/sdd/08-bookstore-assistant/spec`; after spec 08 merges, `origin/factory` is merged into the spec
  branch (merge commit), the docs are re-checked against the merged code and the referee is re-run.

## Task mapping

| Cycle task | Pack task | Content |
|---|---|---|
| T1 | 1 | Seed SQL and Flyway locations per profile, with the required empty-catalogue test |
| T2 | 2 | `http/` collection |
| T3 | 2 | `docs/architecture.md` and `README.md` |
| T4 | 3 | Publish script, its bash test, the CI guard |

## Decisions

- D1 Seed proof. The pack scenario says no test sees seeded rows. Read as: no test that uses the application context
  or its datasource sees them. A seed census test applies `db/migration` plus `db/seed` with plain Flyway to an
  isolated database and checks every census fact of the pack (counts, shared ISBNs, low stock, zero stock, reserved
  sums). The empty-catalogue test also asserts that no repeatable migration ran and that the resolved Flyway
  locations are `classpath:db/migration`, because the test database cleaner truncates every table before each test
  and a count alone would pass even if the seed had run.
- D2 Seed ISBNs. The 20 bookstore-only books use checksum-valid ISBNs that appear neither in the distributor
  catalogue nor in the `http/` collection (9789900010029, 9780141439471, 9780141439600, 9780141439563,
  9789900010050). The `http/` files are reworked so that no request collides with a seeded ISBN.
- D3 Publish script interface. The script builds `main` and moves `level-0-start` in the local repository by
  default. It pushes only with `--push` (remote `origin` unless `--remote` names another), in one atomic push with a
  lease on each ref. `--list-paths` prints the single path list and `--check <rev>` is the guard, so the CI guard
  reads the same list. `--dry-run` changes nothing. Reason: a branch force-update is opt-in and the whole script is
  testable against a local bare repository.
- D4 Idempotency. A run reuses the existing `main` when its single commit already has the stripped tree and message;
  a real change makes a new single commit and moves the tag.
- D5 CI. The guard is its own workflow, `.github/workflows/no-agent-config.yml`, job `no-agent-config`, on push to
  `main`, push of tag `level-0-start` and pull requests into `main` (a pull request into `main` guards `main`). The
  script test is a new job in `ci.yml` on every push and pull request, so the task PR proves it on Linux.
- D6 README module map lists `guide/` as the pack says. `guide/` arrives with spec 11, which merges after spec 09, so
  a `main` published before spec 11 names a folder it does not have yet (manual check for the owner).
- D7 Level names (decisions D6) appear where workshop levels are named: the rehearsal record template and the tag
  `level-0-start`. The README stays a normal team README without workshop talk.
- D8 Assistant docs use the spec 08 values: `spring.ai.anthropic.api-key=${BOOKSTORE_AI_API_KEY:}`, offline mode
  when the key is blank (503 `assistant-offline`), chat contract `POST /assistant/chat` with `conversationId` and
  `message`. `ANTHROPIC_API_KEY` appears once, as the README warning.
- D9 No tech-lead strategy: the only foundational design (the publish script) came from a senior researcher with a
  working prototype; seed and docs follow existing patterns.

## Level 0 traps

Verified present by research before shaping and re-checked by QA, never hinted in docs: no discount, percentage or
promotion concept; `ddl-auto: validate`; ArchUnit rules `NO_DOUBLE_OR_FLOAT_FOR_MONEY` and
`PERSISTENCE_TYPES_ONLY_IN_DATA`; every `package-info.java` is one sentence.
