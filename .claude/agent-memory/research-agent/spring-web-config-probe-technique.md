---
name: spring-web-config-probe-technique
description: Probing WebMvcConfigurer, static resources, Boot error JSON, ProblemDetail wire format and Maven resource inheritance with a scratch Boot 4.1.2 project
metadata:
  type: reference
---

- One scratch project (parent Boot 4.1.2, starters webmvc, actuator, webmvc-test) answers most web wire questions: `@WebMvcTest`, `@SpringBootTest`+MockMvc and `RANDOM_PORT`+`RestTestClient` side by side, print `status/Location/forwardedUrl/body` with `PROBE-` markers, grep the `-l` log.
- Slice vs real differences worth remembering: MockMvc never follows `forward:` (only `forwardedUrl`); redirect `Location` is relative in MockMvc, absolute on a real server; `@WebMvcTest` has no actuator (404); `RestTestClient` does not follow redirects; a `@Configuration implements WebMvcConfigurer` loads in the slice without `@Import`.
- Wire facts for Boot 4.1.2 without `spring.mvc.problemdetails.enabled`: unmapped errors (bad UUID, bad JSON, 405, 404, `ErrorResponseException`) are `application/json` `{timestamp,status,error,path}`, never ProblemDetail; a `ProblemDetail` with `setProperty` serialises flat, `type` omitted when `about:blank`.
- Maven: a child `<resources>` replaces the Boot parent's two entries (filtered `application*` include plus exclude entry); verify with `help:effective-pom -Doutput=<scratch>` and `process-resources`. Missing resource directory is skipped silently.
- Bash guard in worktree agents refuses globs and compound commands: spell the wrapper-dist mvn path out (`~/.m2/wrapper/dists/apache-maven-3.9.16-bin/<hash>/apache-maven-3.9.16/bin/mvn`, two hash dirs exist).
