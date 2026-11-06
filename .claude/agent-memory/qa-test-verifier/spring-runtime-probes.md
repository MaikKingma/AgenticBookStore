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

Related: [[scaffold-verification-techniques]], [[shell-traps]]
