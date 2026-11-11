# Standards: spec 16 bonus level orchestration

- `CLAUDE.md` rules: one spec at a time; never skip, disable, delete or weaken a test or check; never `--no-verify`;
  never force-push or rewrite pushed history; nothing marked done without the command output that proves it; docs in
  plain English with no em dashes; the referee `./mvnw clean verify` green in under 60 seconds.
- `guide/README.md`: ASCII only, forbidden word, dependency allowlist (no new npm dependency), zero-test guard,
  colours only in `src/css/custom.css`, contrast pairs pinned, admonitions in the bracket form, placeholders in inline
  code, Markdown images only, table regions, the build as the link and anchor gate, the offline build as a repackaging.
- Guide structure rule (`guide/scripts/rules/structure.mjs`) and content rules (`guide/scripts/rules/content.mjs`,
  deny-list of factory terms on pages), coverage map anchored to `docs/spec-pack/workshop/workshop-plan.md`
  (`guide/scripts/coverage.mjs`, `coverage-manifest.test.mjs`). Frozen rules change only by addition or an exact
  retarget with a stated reason.
- `agent-os/standards/architecture.md` is a pointer to the bookstore architecture standard; it does not apply to guide
  content and no bookstore code changes in this spec.
- Branch contract D17: the level branches and tags are frozen and live.
