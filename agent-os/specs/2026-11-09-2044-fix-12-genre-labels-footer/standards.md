# Standards

- `docs/spec-pack/specs/12-storefront.md`: Visual design (the bullets "Genre labels" and "Footer" of decision D11,
  plain CSS only, no new npm dependency), the catalogue listing, search and unknown-genre scenarios, and the test plan
  (`CataloguePage.test.tsx`; Vitest and Testing Library at the component layer).
- `docs/spec-pack/orchestrator/decisions.md` D11: the sentence-case labels are Maik's default; the select still sends
  the enum name; the footer sits at the bottom of the window on a short page and below the content on a long one.
- `docs/spec-pack/product/tech-stack.md`: the pinned frontend versions; no new dependency.
- `frontend/src/styles/contrast.test.ts`: every rule that sets `color` names its surface (a token background in the
  same rule or an `ob-on` comment). `frontend/src/styles/styleContract.test.ts`: colours only from `tokens.css`,
  reduced motion respected, the CSS contract read from disk (the pattern the T2 regression tests follow).
- `CLAUDE.md` rules: referee `./mvnw clean verify` within 60 seconds, ASCII and plain English without em dashes,
  never weaken a test or an ArchUnit rule, never `ANTHROPIC_API_KEY`, never `--no-verify`, never force-push.
- The `hexagonal-slice` skill governs bookstore Java; this cycle touches no Java.
