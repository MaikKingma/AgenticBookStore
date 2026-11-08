---
name: spring-runtime-probes
description: Mutation and environment probes for the bookstore Spring Boot runtime (Flyway, Testcontainers, Compose, test profile) and the traps that make them pass falsely
metadata:
  type: feedback
---

Rule: run every probe as `./mvnw -B -ntp -pl bookstore clean test -Dtest=<Class> -l <scratch log>`, always with `clean`.

**Why:** Maven never deletes stale files from `target/`. If you move a migration out of `db/migration`, the old copy is still in `target/classes`, so the probe passes falsely. A removed probe file (a test `application.yml`, a temporary entity) also leaks into the next probe.
**How to apply:**
- Docker-down without stopping the daemon: `DOCKER_HOST=unix:///nonexistent.sock` plus `-DargLine=-Duser.home=<empty scratch dir>`. `DOCKER_HOST` alone falls back to the Docker Desktop socket under the home dir.
- Missing-migration probe: use an entity with `@Id` and a protected no-arg constructor in `nl.openvalue.bookstore.data`. If the `data` package does not exist yet, `rmdir` it after the restore, otherwise the snapshot `find` differs.
- Cleaner mutations: removing the empty-list guard produces `TRUNCATE TABLE  RESTART IDENTITY CASCADE` grammar errors that are raised from the base-class `@BeforeEach`. Read the stack frames to see which hook ran the branch.
- Removing `@BeforeEach` from the integration base keeps the suite green, because the cleaner test calls `clean()` directly. Report it and do not count it as a killed mutation.
- Live `spring-boot:run`: start it through a script in `.claude/comms/tmp/` that redirects to a scratch log (the forked app does not write to `-l`). Wait with an until-loop script for "Started|FAILED|BUILD FAILURE". Stop it by killing the java PID from lsof. Exit 143 is the expected SIGTERM.
- Compose activity in tests should be 0 lines ("Using Docker Compose file"), because Boot skips Compose in tests by default.

- Web-slice advice probes: a sort "by A then B" needs its own tiebreak probe (drop only `thenComparing`). A test whose input already arrives in B order lets the field-only sort survive; prove the fix with a temporary witness class (fails on the mutant, passes on real code), then rm it under the snapshot rule.
- Advice handlers: besides status, title, code, probe each handler's `details` argument separately (pass `List.of()` instead of `exception.details()`). Tests that only throw exceptions with empty details let that survive for the 404 and 409 handlers; the witness is a temporary web-slice test that builds the exception with the four-argument constructor (details non-empty) and asserts `$.details[0]...`; run it on real code, on each mutant, then `rm`.
- Handler pass-through fix tasks enumerate handlers by the plan's list; count the DISTINCT codes the tests feed each handler yourself, including the sibling 503 (`UnavailableException`) handlers. Probe each with its code and title hard-coded (title-only variant too, to prove `$.title` is load-bearing); a handler fed one code only survives.
- The Edit tool strips trailing whitespace from `new_string`: a probe that ends in a space (`String.join(", ", ` to `String.join(",", `) silently changes the text. Include a non-space tail in old and new strings, and re-`shasum` after every restore.
- `rg` works in the Bash tool but not inside scripts run with `bash <file>` (command not found; it is a zsh function around the Claude binary). In a script define `rg() { ( exec -a rg "${CLAUDE_CODE_EXECPATH:-$HOME/.local/bin/claude}" "$@" ); }` and print `rg --version` as proof.
- PostgreSQL 18 catalogues NOT NULL constraints in `pg_constraint` (contype `n`, names `<table>_<col>_not_null`). A "constraint names" check must add `and contype <> 'n'`, and show `select contype, count(*) ... group by contype` to explain the extra rows.
- Live app vs MockMvc: a malformed JSON body gives Boot's `/error` JSON (`timestamp, status, error, path`, application/json) plus a `DefaultHandlerExceptionResolver` WARN in the app log; MockMvc shows an empty body. Classify that WARN as provoked by the malformed-body request, not as product code.
- Smoke checks: cut request lines and bodies from the `.http` file with `sed -n`, send them with curl `-w '%{http_code} %{content_type}'`, and assert each response with a `python3 -I` checker that prints CHECK OK/FAIL lines. That gives one evidence file per run.
- `./mvnw ... -l <log>` prints nothing to the Bash tool and exits 1 on test failure: read the log with grep afterwards.
- Before trusting a spec claim about what a Jakarta constraint accepts, `javap -c` the validator class from the jar in `BOOT-INF/lib`. Hibernate Validator 9.x `NotBlankValidator` calls `String.isBlank()`, so any `Character.isWhitespace` character (U+2003, for example) is blank and gives 400. U+00A0 is not whitespace and passes. Send both as live controls; a "trims only up to U+0020" premise is false.
- A length rule written as a regex (`.{0,N}`, `@Pattern`) counts code points, but `@Size` and `String.length()` count UTF-16 chars. When a payload replaces `@Size` with a regex, send 122 x U+1F600 + `@example.com` (256 chars, 134 code points) as a witness: it gets past the payload and comes back as the domain's 422. Check the regex quickly in a single-file `java X.java` harness first.
- Payload-constraint fixes: run each variant as one script (copy the temp witness classes in, copy the variant over, one `clean test`, copy `.fixed` back, `mv` the witnesses out, compare the sha256). Also run the witnesses on the base; that shows whether a witness failure is new with the task.
- A curl `-i` response file read in Python text mode turns CRLF into LF, so `split("\r\n\r\n")` finds nothing and the parsed id is empty. Open the file with `newline=""`. Before a replay chains ids, check that the first parsed id is non-empty, because empty ids send later requests to `/books/` and still return plausible 404s.

Related: [[scaffold-verification-techniques]], [[shell-traps]]
