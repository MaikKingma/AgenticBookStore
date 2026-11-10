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
- Worktree Bash guard rejects backslash-escaped spaces in a path ("too complex"): double-quote every path containing
  "OpenValue AI". Run mvnw as `with-budget.sh N "<root>/mvnw" -f "<root>/pom.xml" -pl <mod> ...` with `> log 2>&1`.
- Initializr generates `mvnw.cmd` with LF, while the root `.gitattributes` says `*.cmd text eol=crlf`. The working
  copy stays byte-identical to the generated file; git normalises on checkout. Do not convert it by hand.
- `spring-boot:run` with `-l <log>`: the forked app's output goes to Maven's stdout, not the `-l` file. Run it in
  the background and grep the background task's output file for `Started`.
- maven-compiler-plugin 3.15.0 (Boot 4.1.2 managed) defaults `createMissingPackageInfoClass=true`: a Javadoc-only
  `package-info.java` still yields an empty `package-info.class`. Only `maven.compiler.createMissingPackageInfoClass=false`
  stops it.
- Edit (probes and normal edits): a trailing space at the end of old/new string can be lost (an anchor `X = ` once produced
  `X =new`). Make edits whole-line, never end an anchor with a space, and prove every probe restore with `shasum -a 256`
  against a baseline (in-place probes restored by `cp -p` from a scratchpad `.orig` work). Edit cannot match a literal
  non-ASCII char (e.g. U+2003) in `old_string`: replace that line by number with an ASCII-only `perl -i -pe 'if ($. == N)
  {...}'`, verify with `git diff -U0`, and report the deviation in the outbox.
- Surefire `-Dtest=A+B` matches nothing ("No tests matching pattern"); several classes are `-Dtest=A,B`.
- `-q` with `-l <log>` leaves the log empty for `help:describe`; read the plugin descriptor instead with
  `unzip -p <plugin jar> META-INF/maven/plugin.xml` and grep it.
- A probe resource under `src/test/resources` (e.g. a temporary migration) is copied to `target/test-classes` and stays
  there after the source is removed (no clean): move `target/test-classes/<dir>` aside too before the no-probe run.

Stack skills: project-level `clean-hexagonal-onion`, `hexagonal-slice`, `layer-testing`, `ddd-building-blocks`
(under `.claude/skills/`); user-level `tdd`, `agent-comms`.
