---
name: guide-checker-probe-technique
description: How to map which guide/ checks pin a file without npm ci, and where the browser-check rig lives
applies-to:
  - guide/**
metadata:
  type: reference
---

- `node guide/scripts/check.mjs` runs from a bare checkout (no node_modules): prints every rule, all contrast pairs with ratios, test-case count. Use it as the green baseline before listing pins.
- Pins live in `guide/scripts/rules/*.mjs` plus swap-string tests (`brand.test.mjs`, `site.test.mjs`); `git grep -i` for hex values and `--var` names over `guide/scripts` finds them all. Hash baselines (`agent-os/specs/*workshop-guide/t*-hashes.txt`) cover only `guide/scripts/**` and `guide/docs/**`, enforced by hand by QA, not by CI.
- Contrast ratios: reuse `rules/contrast.mjs` formula via `node -e`; the checker has only `text` 4.5 and `nontext` 3.0 kinds, no large-text kind.
- Built-HTML rules: `guide/scripts/lib/html.mjs` `elementTree` is the existing parser; `scanBuilt` is fed tiny fixtures by three test files, so site-specific rules must run only for the default build dir.
- Browser rig: scratchpad folder `s11-guide-verify/` (own `npm install playwright axe-core`, scripts `s11-*-render.mjs`); evidence in spec 11 plan.md FF-9, FF-37. Rig scripts are per-run and not in the repo.
- Pack-text edits cascade: `coverage-manifest.test.mjs` `SWEEP_EXCLUDED` keys are `plan:<line> literal` (workshop-plan line numbers), `floor` per page equals the exact entry count, `SECTION_9_ROWS` count is pinned; `WP:N` row labels are opaque ids, never read as lines. Count entries with a node import of `coverage.mjs`.
- The deny-list in `rules/content.mjs` bans the word "facilitator" on guide pages; structure tests use `assertErrors` with exact counts, so example admonition titles matter.
- Content-edit probes without touching the repo: rsync `guide/` (minus node_modules/build) plus `docs/spec-pack` into a mktemp dir, symlink node_modules, then import `scripts/check.mjs` `runChecks(root, {structure: true, content: true, banSkeleton: true})` and mutate a page between calls. Only PowerShell lines are ever checked against the pack; other commands only via manifest entries. Removing a sync moment breaks `structure.mjs` (exactly 1 per level page) and 5 coverage entries whose text lives only there.
- Rendered probes reuse the session scratchpad rig by absolute import (`.../s14-guide-verify/node_modules/playwright/index.mjs`); axe `scrollable-region-focusable` is skipped when a scroller holds a focusable link, and duplicate `aria-label` on `role=region` wrappers trips `landmark-unique` (best-practice, in the default run).
- Docusaurus behaviour without node_modules: read `packages/docusaurus-plugin-content-docs/src/{numberPrefix,docs,frontMatter}.ts` and `sidebars/generator.ts` via raw.githubusercontent at the pinned tag; confirm against the live site (sitemap.xml, pagination-nav links in the page HTML via curl).
- Adding a guide page: the frozen `scripts/fixtures/skeleton-docs` snapshot runs through the full CLI in `skeleton-ban.test.mjs` with exact error counts, so any newly required page or manifest entry also needs a skeleton page there; `plan.mjs` demands exactly five `**Level cleared when**` lines in the workshop plan.
- Pack-literal audit of a page without a build: a scratch `.mjs` that imports `packCommandLines` (rules/content.mjs), `scanMarkdown`/`proseOf`/`collapse` (lib/mdscan.mjs) and `SOURCE_FILES` by absolute file URL and prints every fenced line or inline span not in the allow-list (spec 15 C0-9 asks for this by hand). `node --test "scripts/**/*.test.mjs"` and `check.mjs` also run under the local Node 24 (2 s), so run them before any install.
- Plan-section sweep trap: `coverage-manifest.test.mjs` `SWEEPS` walks the plan from `## 3. Setup check` to `## 10. Wrap-up`; a new `##` section in that range holds literals that belong to no page tuple and fail the sweep. Short-snippet pin: snippets under 8 chars (`/agents` is 7) must be in the pinned list.
- Node 24 cannot import `.tsx` (ERR_UNKNOWN_FILE_EXTENSION), so component tests need built HTML or a transform.
