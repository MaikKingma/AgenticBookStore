---
name: consistency-check-techniques
description: Zero-model proof techniques for staged config files (glob tests, token resolution on a factory sha, sandbox builds) and their traps
metadata:
  type: reference
---

Techniques that held for the spec 10 consistency-check tasks (D14, no Claude runs):

- **Glob test for rule `paths`**: never use `fnmatch` (its `*` crosses `/`). Cross-check three implementations: a small
  regex translator, `pathlib.PurePosixPath.full_match` (Python 3.13) and `git ls-files ':(glob)<pattern>'` on the
  sandbox. Add widened and narrowed negative controls per rule. Test-tree folders named like the layer
  (`archfixtures/domain`) are the trap that a missing `main` anchor would match.
- **Resolve tokens on a recorded sha**: `git grep -n -e <token> <sha> -- <paths>` (read-only). A pattern that starts
  with `-` (for example `-pl bookstore`) needs `-e`, else git reports an unknown switch. `git grep -F ""` is an error:
  list files with `git ls-tree -r --name-only` instead.
- **Prove a Maven goal resolves without starting the app**: `./mvnw -pl <module> spring-boot:help -Dgoal=run`.
- **Sandbox**: build with the T1 scripts from the worktree (`simulate-level-0.sh`, `build-level.sh`), `TMPDIR` and
  `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir` in the scratchpad, `mktemp -d "$TMPDIR/sbx.XXXXXX"`. Full verify of the
  repository took about 30 seconds with a warm `~/.m2` and Docker running.
- Bash tool in a worktree agent: paths with spaces must be double-quoted (backslash escapes make the guard refuse the
  command); multi-step logic goes into a script under `.claude/comms/tmp/`.
- Pack text can contradict itself on wording (spec 09 says Level 1 "imports" `docs/architecture.md`, spec 10 forbids the
  `@` import): record it as a product finding, never edit verbatim staged files.
