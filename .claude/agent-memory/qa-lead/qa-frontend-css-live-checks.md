---
name: qa-frontend-css-live-checks
description: Falsifiers for contrast and style-guard tests, CSS layout and live-browser checks, and frontend negative controls in a throwaway worktree: calibration pairs, coverage rules, named-surface claims, band witnesses, cascade blind spots, jsdom layout gaps, rehype table wrappers, favicon and cache traps
metadata:
  type: feedback
---

Siblings: [[qa-frontend-falsifiers]] (React and Vitest witnesses), [[qa-docs-checker-oracle]] (contrast checkers in docs specs).

## Contrast and style-guard tests

- Auto-extracting text pairs from rules that declare colour and background together misses inherited pairs: demand a per-`color` coverage rule (same-rule background or a named-surface comment) that fails otherwise.
- Calibration must call the SAME predicate and threshold as the pair checks (4.22 blue on white kills a 4.0 threshold). A threshold constant is pinned only between the nearest calibration pairs: name a band witness (`#767676` on white 4.54 passes, `#777777` 4.48 fails), or record it as equivalent only after checking no pair of the pinned token set lies in the band.
- A large-text (3:1) kind in a static checker cannot see font size: demand an exact allow-list by name, band witnesses for the new threshold, a source pin on weight and size (no `@media` lowering it), and rendered computed size and weight at every width and engine. axe puts text over pseudo-element or `clip-path` shapes (chevron crumbs) into `incomplete`, so "0 violations" can be vacuous: require the nodes in `passes`.
- A nontext outline pair protects a colour, not visibility: the layer that paints it can vanish (`inset: 0` on the fill above it) while the pair stays green. Pin the layer geometry or measure the band pixels. Deleting a redundant `:hover` colour is equivalent when the base rule outranks Infima's `a:hover`.
- A palette rule that enumerates a list (admonition types, dark surface variables) gets tested on a subset; dropping an unpaired entry survives. Falsifier: "each list entry dies when dropped", the test list checked against the rule's length. Read every original criterion word against each amendment (a dark primary made "white button text" literally false).
- A text pin on a stylesheet (exact selector list plus value) cannot see the cascade: a later overriding rule survives it. Close that with a live `getComputedStyle(cell)` value plus the hash pin, not a REJECT. A cascade-aware reader that skips at-rules still misses an always-true media override and a same-specificity rule on another selector: pin a closed-world allow-list of (selector, context) pairs over ALL contexts.

## Layout and live-browser checks

- Layout claims in CSS (a scroll wrapper, wrapping, sticky) have no falsifier in jsdom: put a short live-browser measurement with realistic data at 390 px into the task that writes the rule, not only into the final walk. An `overflow-x: auto` wrapper needs `scrollWidth > clientWidth` on it and one client rect per word of every cell; an inherited `overflow-wrap: anywhere` (from a card) shrinks a table's min-content, so the wrapper never scrolls and headings break mid-word.
- A rehype plugin that wraps tables in labelled regions needs MDX witnesses: a heading `## X {/* #id */}` (expression text leaks into a derived label), inline code or `<kbd>` in the heading (element-only walks drop it), a table inside a JSX flow element (TabItem, details), two tables under one heading (landmark-unique), run twice (double wrap), role kept on the table. Dropping an axe exclusion needs a positive control: the same rig on a build with the plugin unregistered must show the old hits, or "0 violations" can be vacuous.
- The Boot jar serves `/shop/index.html` with `Last-Modified` 1980 and no `Cache-Control`, so a reused browser keeps the old page after a rebuild: every live check of a new build runs in a brand-new browser context.

## Frontend negative control in a throwaway worktree

A fresh worktree has no `frontend/node` or `node_modules`: `./mvnw -q -pl frontend generate-resources` from its root installs both (install-node-and-npm and `npm ci`, tests not run). Do not point Vitest at another tree's `node_modules` (imports resolve from the test file's directory, so trees mix). A test that imports a new export of the fix is green-after-only: leave it out of the red run and name the red set from the tests that exercise unchanged exports.
