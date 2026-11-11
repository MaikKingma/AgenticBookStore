---
name: guide-checker-probe-technique
description: How to map which guide/ checks pin a file without npm ci, and where the browser-check rig lives
metadata:
  type: reference
---

- `node guide/scripts/check.mjs` runs from a bare checkout (no node_modules): prints every rule, all contrast pairs with ratios, test-case count. Use it as the green baseline before listing pins.
- Pins live in `guide/scripts/rules/*.mjs` plus swap-string tests (`brand.test.mjs`, `site.test.mjs`); `git grep -i` for hex values and `--var` names over `guide/scripts` finds them all. Hash baselines (`agent-os/specs/*workshop-guide/t*-hashes.txt`) cover only `guide/scripts/**` and `guide/docs/**`, enforced by hand by QA, not by CI.
- Contrast ratios: reuse `rules/contrast.mjs` formula via `node -e`; the checker has only `text` 4.5 and `nontext` 3.0 kinds, no large-text kind.
- Built-HTML rules: `guide/scripts/lib/html.mjs` `elementTree` is the existing parser; `scanBuilt` is fed tiny fixtures by three test files, so site-specific rules must run only for the default build dir.
- Browser rig: scratchpad folder `s11-guide-verify/` (own `npm install playwright axe-core`, scripts `s11-*-render.mjs`); evidence in spec 11 plan.md FF-9, FF-37. Rig scripts are per-run and not in the repo.
- Node 24 cannot import `.tsx` (ERR_UNKNOWN_FILE_EXTENSION), so component tests need built HTML or a transform.
