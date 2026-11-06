---
name: maven-build-traps
description: Non-obvious traps when running the Maven wrapper and shell checks in this repo (output truncation, zsh globbing, wrapper line endings, package-info.class generation)
metadata:
  type: reference
---

- Run Maven as `bash ~/.claude/scripts/with-budget.sh <s> ./mvnw -B -ntp <goals> -l <scratch>/x.log`, then grep the log.
  Without `-B -ntp` the download progress floods the tool output and the result lines get truncated.
- The Bash tool runs zsh: quote find parentheses (`\(` `\)`), `-path` patterns and `--include=*.java` globs, or zsh
  aborts with "no matches found".
- Initializr generates `mvnw.cmd` with LF, while the root `.gitattributes` says `*.cmd text eol=crlf`. The working
  copy stays byte-identical to the generated file; git normalises on checkout. Do not convert it by hand.
- Boot 4.1.2 skips Docker Compose support in tests by default; Testcontainers logs
  `Creating container for image: postgres:18.6` when the `@ServiceConnection` bean starts.
- `spring-boot:run` with `-l <log>`: the forked app's output goes to Maven's stdout, not the `-l` file. Run it in
  the background and grep the background task's output file for `Started`, not the `-l` log.
- The ArchUnit JUnit 6 engine (archunit-junit6 1.5.1) discovers `@ArchTest` fields on Boot's managed JUnit 6.0.3
  (spike in spec 01 T1); no JUnit override needed.
- maven-compiler-plugin 3.15.0 (Boot 4.1.2 managed) defaults `createMissingPackageInfoClass=true`: a Javadoc-only
  `package-info.java` still yields an empty `package-info.class` under target/classes. Only the property
  `maven.compiler.createMissingPackageInfoClass=false` stops it; javac's own behaviour is not the cause.
- `-q` with `-l <log>` leaves the log empty for `help:describe`; read the plugin descriptor instead with
  `unzip -p <plugin jar> META-INF/maven/plugin.xml` and grep it.

Stack skills: project-level `clean-hexagonal-onion`, `hexagonal-slice`, `layer-testing`, `ddd-building-blocks`
(under `.claude/skills/`); user-level `tdd`, `agent-comms`.
