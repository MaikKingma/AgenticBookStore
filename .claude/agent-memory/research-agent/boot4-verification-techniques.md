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
- Web-adapter probes: a scratch project with starters webmvc, validation, webmvc-test (brings RestTestClient) and `System.out.println("PROBE-...")` in tests, then grep the log; `mvn` from `~/.m2/wrapper/dists/apache-maven-3.9.*/` works offline. `@WebMvcTest` lives in `org.springframework.boot.webmvc.test.autoconfigure`. `spring.mvc.problemdetails.enabled=true` adds an `@Order(0)` advice that overrides unordered app advices.
- JPA probes: a plain Hibernate `MetadataSources` + `hbm2ddl.auto=validate` harness against a Testcontainer, one schema per DDL mutation, shows exactly what `validate` checks (only table, column, type family; not length, scale, nullability, unique, pk, fk). `@DataJpaTest` is `org.springframework.boot.data.jpa.test.autoconfigure`, `TestEntityManager` is `...boot.jpa.test.autoconfigure`.
- Probe inside the real module: `-Dtest=<Probe>` with `./mvnw -f <pom> -pl bookstore clean test`, then delete probe files AND their copies in `target/classes` (a stale probe migration would break other builds). `UUID.compareTo` is signed, PostgreSQL uuid order is unsigned: assert ordering via `toString`.
- JPQL on PostgreSQL: a null param inside `concat`/`lower` fails (`lower(bytea)`); pass Java-built `%x%` patterns or `cast(:x as string)`.
- Traps found for 4.1.2: `@Bean` containers start once per distinct context (use a static field for one per JVM); `flyway-database-postgresql` is not in the flyway starter; `TestRestTemplate` needs `spring-boot-restclient`; JSON health has no `db` without `show-components`.
