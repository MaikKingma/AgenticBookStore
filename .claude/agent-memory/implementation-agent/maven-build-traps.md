---
name: maven-build-traps
description: Non-obvious traps when running the Maven wrapper and shell checks in this repo (output truncation, zsh globbing, wrapper line endings, package-info.class generation, Edit and probe restores)
metadata:
  type: reference
---
- Run Maven as `bash ~/.claude/scripts/with-budget.sh <s> ./mvnw -B -ntp <goals> -l <scratch>/x.log`, then grep the log.
  Without `-B -ntp` the download progress floods the tool output and the result lines get truncated.
- The Bash tool runs zsh: quote find parentheses (`\(` `\)`), `-path` patterns and `--include=*.java` globs, or zsh
  aborts with "no matches found".
- Worktree Bash guard rejects backslash-escaped spaces in a path ("too complex"): double-quote every path containing
  "OpenValue AI". Run mvnw as `with-budget.sh N "<root>/mvnw" -f "<root>/pom.xml" -pl <mod> ...` with `> log 2>&1`.
- `spring-boot:run` with `-l <log>`: the forked app's output goes to Maven's stdout, not the `-l` file. Run it in
  the background and grep the background task's output file for `Started`.
- maven-compiler-plugin 3.15.0 (Boot 4.1.2 managed) defaults `createMissingPackageInfoClass=true`: a Javadoc-only
  `package-info.java` still yields an empty `package-info.class` under target/classes. Only the property
  `maven.compiler.createMissingPackageInfoClass=false` stops it; javac's own behaviour is not the cause.
- Edit: a trailing space at the end of old/new string can be lost: make edits whole-line and prove every probe restore with
  `shasum -a 256` against a baseline (`cp -p` from a scratchpad `.orig`). Edit cannot match a literal non-ASCII char (e.g.
  U+2003): replace that line by number with an ASCII-only `perl -i -pe 'if ($. == N) {...}'` and report the deviation.
- `cp -p` (probe install or restore) keeps the OLD mtime, so incremental compile reuses the stale class and the run proves
  nothing (a "red" IT once came out green): run such runs with `clean`. A probe resource under `src/test/resources` stays in
  `target/test-classes` after the source is removed: move it aside too before the no-probe run.
- Surefire `-Dtest=A+B` matches nothing; several classes are `-Dtest=A,B`.

Stack skills: project-level `clean-hexagonal-onion`, `hexagonal-slice`, `layer-testing`, `ddd-building-blocks`
(under `.claude/skills/`); user-level `tdd`, `agent-comms`.
