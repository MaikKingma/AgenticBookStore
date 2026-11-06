---
name: boot4-verification-techniques
description: How to verify Spring Boot 4 / Testcontainers 2 claims cheaply for this project (source jars, throwaway probe project, traps in docs)
metadata:
  type: reference
---

- Spring Boot 4 is modular: read behaviour from `*-sources.jar` on Maven Central (spring-boot-health, -jdbc, -resttestclient, -docker-compose) instead of the docs; docs often omit defaults (health show-components).
- `docs.spring.io/spring-boot/4.1/...` redirects to `/spring-boot/reference/...` (latest); the page header shows the exact version, check it.
- Fastest proof: throwaway probe project under the scratchpad with the Boot parent pom, run via the Maven wrapper dist in `~/.m2/wrapper/dists` (no `mvn` on PATH), `-ntp -l <log>` and grep the log. Repo stays untouched.
- Docker-down simulation: `DOCKER_HOST=bogus` alone is not enough (falls back to Docker Desktop socket); add `-DargLine=-Duser.home=<empty dir>`.
- Traps found for 4.1.2: `@Bean` containers start once per distinct context (use a static field for one per JVM); `flyway-database-postgresql` is not in the flyway starter; `TestRestTemplate` needs `spring-boot-restclient`; JSON health has no `db` without `show-components`.
