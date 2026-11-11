# Standards for spec 15

- `agent-os/standards/index.yml` holds only the architecture pointer, which does not apply to guide content.
- Repo rules (`CLAUDE.md` on `factory`): never skip, disable, delete or weaken a test or check; never `--no-verify`;
  never force-push; docs in plain English with no em dashes; never set, read or write `ANTHROPIC_API_KEY`.
- The guide's own checker is the standard for pages: `guide/README.md` and `guide/scripts/**` (structure, coverage
  anchored to the pack, content rules, PowerShell lines, hygiene, brand and contrast, built rules). Text files are
  ASCII only; the content rule bans "facilitator" on pages.
- Pack spec rule: every new command on a page exists in the pack (one of the five SOURCE_FILES of the coverage map;
  this spec adds them to `docs/spec-pack/workshop/workshop-plan.md`).
- Spec 14 decisions that stay: D18 colours, A1 (AA shade for text), the contrast pairs and their minimum count.
