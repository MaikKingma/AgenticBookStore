---
name: docs-census-probes
description: Cheap ways to verify README and run claims for the bookstore (jar run, endpoint census, http collection collisions, worktree git guard)
metadata:
  type: reference
---

- Run claims without Docker: an already built `bookstore/target/*.jar` of the main checkout can be started from any directory with `--server.port=<free>`. No datasource settings gives "Failed to configure a DataSource"; `SPRING_DATASOURCE_URL` pointing at a dead port proves the env var is read. A jar run never tries Docker Compose, so no compose flag is needed.
- Endpoint census: `grep -rnE "@(Get|Post)Mapping|@RequestMapping|@ResponseStatus"` over `bookstore/src/main` and `distributor/src/main`; payload and view records give the body shapes.
- http collections: compare every ISBN the `.http` files add or import with the seed and with `distributor/src/main/resources/catalog/bookstore-shared-isbns.txt`; a duplicate ISBN is a 409 and the captured variable stays unset.
- Unmerged spec branches: `git diff --name-status origin/factory origin/<branch>` shows what exists; read files with `git -C <worktree> show origin/<branch>:<path>`, one file per call.
- Worktree Bash guard refuses `cd ...; for ...` and `git ls-files ... | xargs`-style compounds with `$(...)`; plain pipes and single commands work. Mailbox dirs may not exist in a fresh worktree: create them when writing the outbox and the orchestrator inbox.
