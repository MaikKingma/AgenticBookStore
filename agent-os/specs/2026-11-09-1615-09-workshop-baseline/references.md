# References for spec 09

Research reports (cycle comms, passed to agents by path):

- res-1 seed facts: `.claude/comms/mailboxes/res-1/outbox/research-seed-facts.md`. Schema has no CHECK constraints or
  defaults; ids are app-generated UUIDs; `version` NOT NULL; genre and status stored as enum names; restore rules
  (ISBN-13 checksum, price in (0, 500], publication year 1450 to next year, 1 to 5 authors, on hand at least reserved,
  1 to 20 lines of quantity 1 to 10, lower-case e-mails); every book needs a stock row; low stock means
  `on_hand - reserved <= reorder_level`; `GET /books` returns up to 100 books ordered by title and ISBN; the 20 shared
  titles and authors; `http/` ISBNs that collide with the seed.
- res-2 docs inputs: `.claude/comms/mailboxes/res-2/outbox/research-docs-inputs.md`. Endpoint census, run facts
  (ports 8080, 8090, 5173; Compose service `postgres`), spec 08 assistant facts from its branch, module map, spec 07
  trade-off sources, Level 0 traps present.
- res-3 publish script: `.claude/comms/mailboxes/res-3/outbox/research-publish-script.md`. Interface, git mechanics,
  bash 3.2 portability, the hermetic test, the guard workflow, a working prototype with a 39-assertion test.

Code and docs:

- `docs/spec-pack/specs/09-workshop-baseline.md`, `docs/spec-pack/specs/08-bookstore-assistant.md`
- `docs/spec-pack/orchestrator/progress.md` (manual checks: spec 07 trade-off, seed exclusion of 9789900010029)
- `docs/spec-pack/workshop/workshop-plan.md` section 4 (Level 0 prompt, observation card, "Level cleared when")
- `distributor/src/main/resources/catalog/bookstore-shared-isbns.txt`
- `bookstore/src/main/resources/db/migration/V0` to `V4`
- `bookstore/src/main/java/nl/openvalue/bookstore/process/RequestRestockDelegate.java` (trade-off Javadoc)
- `.github/workflows/ci.yml`
