---
name: spring-runtime-probes
description: Spring Boot runtime probes - clean builds, Docker-down, live boot, validators, fixed clock, EPP, view controllers
applies-to:
  - bookstore/src/main/**
  - bookstore/pom.xml
metadata:
  type: feedback
---
Rule: run every probe as `./mvnw -B -ntp -pl bookstore clean test -Dtest=<Class> -l <scratch log>`, always with `clean`.
**Why:** Maven never deletes stale files from `target/`. A migration moved out of `db/migration` is still in `target/classes`, so the probe passes falsely; a removed probe file (test `application.yml`, temporary entity) leaks into the next probe.
**How to apply:**
- Docker-down without stopping the daemon: `DOCKER_HOST=unix:///nonexistent.sock` plus `-DargLine=-Duser.home=<empty scratch dir>` (`DOCKER_HOST` alone falls back to the Docker Desktop socket under the home dir).
- Live `spring-boot:run`: start through a script in `.claude/comms/tmp/` redirecting to a scratch log (the forked app does not write to `-l`); wait with an until-loop for "Started|FAILED|BUILD FAILURE"; stop by killing the java PID from lsof (exit 143 = SIGTERM, expected).
- Before trusting a spec claim about what a Jakarta constraint accepts, `javap -c` the validator class from the jar in `BOOT-INF/lib`. Hibernate Validator 9.x `NotBlankValidator` uses `String.isBlank()`: any `Character.isWhitespace` char (U+2003) is blank (400); U+00A0 is not whitespace and passes. Send both as live controls.
- A length rule written as a regex (`.{0,N}`, `@Pattern`) counts code points, while `@Size` and `String.length()` count UTF-16 chars. When a payload swaps `@Size` for a regex, witness 122 x U+1F600 + `@example.com` (256 chars, 134 code points): passes the payload and comes back as the domain's 422. Check the regex first in a single-file `java X.java` harness.
- "Stored, not echoed" request fields: MockMvc cannot see them, so probe the controller passing `null` instead. Witness: a temporary class in the same `@WebMvcTest` + `@Import(Service)` slice that `@Autowired`s the real service and reads the stored record after the 201.
- EnvironmentPostProcessor plus `@ConditionalOnProperty` (offline-mode switch): probe the `spring.factories` line (deleted, moved to the old `org.springframework.boot.env.EnvironmentPostProcessor` key; Boot 4.1.2 gives a context failure `is not assignable to factory type`), `addFirst` alone, the "unset" guard alone (source-list equality and last-source assertions kill each), `havingValue` dropped, `matchIfMissing=true` on an `ApplicationContextRunner` test. For an `isBlank()` guard also probe `trim().isEmpty()`: ASCII witnesses (`"   "`, `"\t\n"`) let it survive, only `Character.toString(0x2003)` kills; `strip().isEmpty()` is equivalent. EPP order witness (property-source index): Boot appends `applicationInfo` and `Management Server` after every post-processor, so "after every source containing `application`" is red on correct code; compare against sources starting with `Config resource`. `@Order(HIGHEST_PRECEDENCE)` fails only that witness (the blank-key test profile hides it).
Related: [[spring-advice-and-interceptor-probes]], [[env-switch-mutant-runner]], [[scaffold-verification-techniques]], [[shell-traps]]
