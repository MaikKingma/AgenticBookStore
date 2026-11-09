# Standards

- `docs/spec-pack/specs/12-storefront.md`: Routes and serving (hash routing, no server-side fallback, `/shop` and
  `/shop/` forward to `/shop/index.html`, `StorefrontConfig` in the root package), the checkout scenarios (on 202 the
  basket is emptied and the order page is shown; the basket stays on every other answer), the test plan
  (`StorefrontConfigTest`, `CheckoutPage.test.tsx`, `basket.test.tsx`).
- `docs/spec-pack/specs/04-orders.md`: an order line holds 1 to 10 copies, enforced by `domain/OrderLine.java`.
- `docs/spec-pack/product/tech-stack.md`: the pinned versions; no new npm or Maven dependency.
- `agent-os/standards/architecture.md` (pointer to `docs/spec-pack/product/architecture-standard.md`): the root
  package holds `BookstoreApplication` and the cross-cutting configuration a spec puts there, `StorefrontConfig`
  included; ArchUnit stays as it is.
- `.claude/skills/hexagonal-slice` (steps 0 and 8 for configuration in the root package) and `layer-testing`
  (end-to-end integration tests on the real server with `IntegrationTestBase`).
- `CLAUDE.md` rules: referee `./mvnw clean verify` within 60 seconds, ASCII and plain English without em dashes,
  never weaken a test or an ArchUnit rule, never `ANTHROPIC_API_KEY`, never `--no-verify`, never force-push.
