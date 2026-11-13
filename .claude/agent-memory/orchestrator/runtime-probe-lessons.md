---
name: runtime-probe-lessons
description: Live runs, sandbox hygiene, headless Claude Code, staged agent config, LLM adapter and Spring/Maven probe traps for directives and briefs
applies-to:
  - .claude/**
  - bookstore/src/main/java/nl/openvalue/bookstore/assistant/**
  - "pom.xml"
metadata:
  type: project
---

## Live replays and sandbox hygiene
- Live replays on a shared machine name an isolated database: `spring-boot:run` creates, and `docker compose down -v`
  removes, the shared `bookstore-postgres-1` volume of every checkout; `bookstore/compose.yaml` binds host 5432. Ask
  for an own `docker run` Postgres on a free port, `java -jar` with `--spring.docker.compose.enabled=false`, app port
  never 8080 (other worktrees hold 8080/8090), a quoted teardown and a name-based docker proof (other cycles start
  containers mid-run, so a `docker ps -a` diff never holds).
- TMPDIR plus explicit `mktemp` templates (macOS `mktemp -d` ignores TMPDIR), `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...`
  (the JVM ignores TMPDIR), labelled containers removed after, never a script that appends to a file it reads (one
  filled 300 GB).

## Claude Code in briefs
- Headless Claude Code under an isolated HOME cannot log in (Keychain ignores HOME and CLAUDE_CONFIG_DIR; `--bare`
  needs an API key, which is forbidden). Plan "run its scenarios with Claude" as consistency checks plus an owner
  rehearsal checklist from Gate 1 (spec 10 D14).
- A smoke check with the normal login works through `claude -p "<prompt>" --mcp-config <scratch json>
  --strict-mcp-config --allowedTools mcp__<server>__<tool> --output-format stream-json --verbose` (never `claude mcp
  add`). Pre-authorize a `--model sonnet` retry on a 429; judge on the init event plus one tool_result.
- Stage agent config outside live paths (Claude Code loads CLAUDE.md, `.claude/rules`, `.claude/skills` from
  subfolders): spec 10 plan.md J1 and `docs/spec-pack/level-branches/README`.
- The owner's plugin guard prompts on any settings Write adding plugin keys and any Bash naming them: brief agents to
  write such a file once, in one Write, and inspect it with Read only.

## LLM adapter specs
- Check vendor API rules against the pack config before planning (a sampling setting the API rejects was dropped as a
  flagged, self-accepted decision).
- Tests offline by construction; run every gate's referee twice, plain and with a fake key exported.
- Offline pin: pin the blank key plus a loopback base URL, not `spring.ai.model.chat` (that makes the offline
  EnvironmentPostProcessor skip). Prove exported-variable safety with a decoy loopback URL and a per-host count.
- Live checks find what offline tests cannot (thinking-block replay order, follow-up lookups without tool results,
  fields the prompt never names): plan a stub-server contract test of the real client and a prompt-text test up front.

## Spring and Maven
- Static entry document: `no-cache` is not enough, the jar fixes `Last-Modified` at 1980-02-01 so revalidation answers
  304; a literal resource-handler pattern for one file gives an empty resource path (404), use a capture variable.
- Multi-class surefire selectors use a comma (`A#m1,B#m2`); `A#m1+B#m2` runs only one class.
- Participant-baseline probes: a temporary `V<n+1>__*.sql` under `bookstore/src/test/resources/db/migration/` is
  applied by the test profile; Maven keeps removed test resources in `target/test-classes`, so probes need `clean`.
- qa-final warning allow-lists are judged base vs tip, not by a fixed list.
