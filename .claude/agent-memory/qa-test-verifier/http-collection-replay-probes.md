---
name: http-collection-replay-probes
description: Verifying JetBrains .http collections against a live seeded stack: replay driver, fresh DB per scenario, handler harness, mutant design, traps
metadata:
  type: feedback
---

Rule: replay the files' own bytes with a driver you have shown can fail; give every scenario its own freshly seeded database.

**Why:** a hand-run or retyped request list can pass while the file is wrong; a shared database hides order dependence (ISBN collisions, a re-run that no longer dips stock).
**How to apply:**
- One container (`docker run postgres:18.6`, free port), then per scenario `CREATE DATABASE`, start the bookstore jar on it (`SPRING_DATASOURCE_URL`, `--spring.docker.compose.enabled=false`; the default profile seeds via Flyway, startup is a few seconds), run the driver, kill the jar by pid file, `DROP DATABASE ... WITH (FORCE)`. A whole matrix (10 alone, order, twice, stopped, chaos, 14 mutants) takes under 3 minutes. Distributor jar is `distributor/target/distributor.jar`.
- Driver parts: split on `^###\s*$`; request line, headers up to the blank line, body up to `> {%`; handler text between `{%` and `%}`; substitute only `{{var}}` (`{{$uuid}}` generated). Evaluate the file's own handlers with a small node harness (`client.global.set/get`, `client.test`, `client.assert`, `response.{status,body}`), run through `new Function`.
- Expected status: parse `# Expected status: 202 (distributor running); 503 (distributor stopped).` and pick by active run-mode label; no matching label means SKIP. Anchor the regex at `^#\s*Expected status:` because the first block also holds the file header, which may quote the phrase.
- Oracle for every stated result: psql via `docker exec <pg> psql -At` (stock_item, customer_order, book_author), the live distributor, the ACL mapping in code. Never the implementer's evidence. Count-based oracles (`low-stock list has N items`) must come from psql over all rows, not from the seed count: earlier files of an in-order run add low-stock books and a hard-coded 8 fails there while the file is right.
- Unresolved `{{var}}` is a FAIL, except when the capturing block was expected to be refused (non-2xx). Reset captures per file, which is stricter than the IDE (globals persist there).
- Mutants: exact single replacement with an occurs-once guard, diff saved. Handler-weakening mutants (position instead of ISBN, `return true` instead of membership by id) survive a healthy run by construction; pair them with a data mutant (`q=a`, quantity 4) so the driver's oracle check is the killer, and say which check kills.
- "Run once" headers: run the same files twice on one database and check the claim itself (409 on every add/import; for a flow, which handler tests fail on run 2 and that statuses stay).

Trap: in this environment `rg -E` means encoding (config error, exit 2, looks like "no output"); use `rg -ni "a|b"`.
Related: [[readme-and-live-jar-probes]], [[spring-runtime-probes]], [[shell-traps]]
