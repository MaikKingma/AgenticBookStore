# References

- `agent-os/specs/2026-11-08-2145-12-storefront/plan.md`: the spec 12 cycle, its QA feed-forward (the literal C0-6
  grep set for frontend files, fixture rules, the console trap of `setupTests.ts`) and its decisions.
- `bookstore/src/main/java/nl/openvalue/bookstore/StorefrontConfig.java` and
  `bookstore/src/test/java/nl/openvalue/bookstore/StorefrontConfigTest.java`: the storefront entry points and their
  tests on the real server (`IntegrationTestBase`, `restTestClient`).
- `bookstore/src/test/resources/static/shop/index.html`: the test copy of the entry document.
- `frontend/src/pages/CheckoutPage.tsx`, `frontend/src/state/basket.tsx` and their tests: the checkout and the basket
  context; `CheckoutPage.test.tsx` already has a `deferred()` helper and `mockFetch` routes.
- `frontend/src/pages/BookDetailPage.tsx` ("Add to basket") and `frontend/src/pages/BasketPage.tsx` ("Go to
  checkout"): the controls the regression tests use.
- `bookstore/src/main/java/nl/openvalue/bookstore/domain/OrderLine.java:30-37`: the one home of the 1 to 10 rule.
- `agent-os/specs/2026-11-09-0314-fix-06-distributor-review/`: the previous lite bugfix in this repo.
