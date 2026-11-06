# Agentic Bookstore factory

This branch (`factory`) builds the J-Fall 2026 workshop app with agents and SDD tooling. Spec 09 strips this
file, `.claude/`, `agent-os/` and `docs/spec-pack/` from `main`, so participants start without agent configuration.

## Referee

`./mvnw clean verify` from the repo root. It must be green before any spec is marked done. Budget: 60 seconds.

## Build order

Specs 01, 02, 03, 04, 05, 06, 07, 08, 12, 09, 10, 11, one at a time, each on top of the previous one.
Spec files: `docs/spec-pack/specs/<NN>-*.md`. Product docs: `docs/spec-pack/product/`.

## Where to look

- Architecture standard: `docs/spec-pack/product/architecture-standard.md` (the standard wins over any skill).
- Progress and open questions: `docs/spec-pack/orchestrator/progress.md` (source of truth after a restart).
- SDD command mapping: `docs/spec-pack/orchestrator/sdd-mapping.md`.

## Rules

- Any bookstore code is written through the `hexagonal-slice` skill in `.claude/skills/`. It pulls in
  `clean-hexagonal-onion`, `ddd-building-blocks` and `layer-testing` at the steps where it names them.
- One spec at a time. Never start the next spec while the current one is not done.
- Never skip, disable, delete or weaken a test or an ArchUnit rule to get to green. Never use `--no-verify`.
  Never force-push. Never rewrite pushed history. Never mark anything done without the command output that proves it.
- Secrets: the app reads `BOOKSTORE_AI_API_KEY`. Never set, read or write `ANTHROPIC_API_KEY`. Never commit a key.
- Docs and specs are plain English with no em dashes.
