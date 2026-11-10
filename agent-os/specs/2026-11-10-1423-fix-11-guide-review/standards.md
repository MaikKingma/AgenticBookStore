# Standards

- `CLAUDE.md` rules - referee `./mvnw clean verify` within 60 s; never skip, disable, delete or weaken a test or a
  check; docs in plain English, no em dashes; never `ANTHROPIC_API_KEY`.
- Spec 11 page conventions (`docs/spec-pack/specs/11-workshop-guide-site.md`) - the at a glance box (Goal, Start
  branch, Catch-up as one fenced bash block, Time, Compare), OS tabs only with the known groups, Windows PowerShell
  lines only from the pack.
- Spec 11 `strategy.md` G1 to G9 - Node with `npm ci`, no new dependency, no `npm audit fix`; the guide gates
  `check`, `typecheck`, `build`, `check:built`, `build:offline`; ASCII only.
- D17 - branch and tag names are a contract; renaming one needs Maik's OK. This cycle renames nothing: it creates
  participant-local branches `level-0` and `level-1` at the tag in the participant's clone only.
