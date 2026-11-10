---
spec: 14-guide-branding
pack: docs/spec-pack/specs/14-guide-branding.md
decision: D18
base: factory
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-10T16:24:19Z
qa-lead: on
---

# Plan: spec 14 guide branding

Shape: `shape.md` (scope and shaping decisions D-S1 to D-S9). QA strategy: `qa-strategy.md` (tiers, G-1 to G-12
rewrites, common directive C0, task directives, qa-final focus). Branches: spec `sdd/14-guide-branding/spec`, tasks
`sdd/14-guide-branding/T<n>`, each task PR squash-merged into the spec branch; the spec PR into `factory` is the owner's
merge.

| id | module | dependsOn | batch | summary | qaTier | qaModel | pr | branch | verdict | qa-lead |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | guide | [] | 1 | logo asset, navbar logo link, GitHub and LinkedIn links, navbar colours, logo provenance check, navbar built rule | elevated | sonnet | #165 | sdd/14-guide-branding/T1 | PASS r1 | ENDORSE |
| T2 | guide | [T1] | 2 | LevelProgress chevron breadcrumb with links, done/current/upcoming states, level-progress built rule | critical | sonnet (owner policy, see Decisions) | | sdd/14-guide-branding/T2 | | |
| T3 | guide | [T1, T2] | 3 | deck colours across the site, brand rule to D18, visual sweep, screenshots | elevated | sonnet | | sdd/14-guide-branding/T3 | | |

qaDirective per task = `qa-strategy.md` C0 plus that task's block under "Task directives", plus every
`## QA feed-forward` entry below.

## Owner decision OD-1 (resolved)

Resolved 2026-11-10 by the main session under delegated gates (Maik may overrule): OD-1a option 2 and OD-1b option 2.
The spec already requires AA for text on blue, and "never weaken a check" wins over the exact hex. The current crumb is
filled with `#3371B8` and keeps a normal-size label (pair stays `text` 4.5). `--ifm-color-primary` is the AA shade per
mode (light `#3371B8`, dark a lighter deck-blue shade at 4.5:1 or more on slate). The exact `#3c7ec9` is used only for
non-text accents. No contrast kind changes; amendment A1 applies this to T2 and T3. The question as it was put:

- OD-1 (blocked T2 and T3 only; T1 proceeded). Keeping the deck blue exact for text-bearing uses conflicts with the
  pinned 4.5:1 text pairs (white on `#3c7ec9` = 4.18:1, `#3c7ec9` on slate = 3.74:1). See `qa-strategy.md` OD-1,
  G-1, G-2. Options per part:
  - OD-1a current crumb: (1) exact `#3c7ec9` fill, white bold label at least 18.67 px (WCAG large text), new contrast
    kind `large` (3:1) for that one pair with the G-2 guards; or (2) derived shade `#3371B8` fill (5.01:1), normal
    label, the pair stays `text` 4.5.
  - OD-1b primary: (1) `--ifm-color-primary` exactly `#3c7ec9`, the existing `primary` pair becomes `nontext`
    (accent) and a new `primary text` pair guards the derived text shade, with the G-1 census; or (2)
    `--ifm-color-primary` is the AA shade per mode (light `#3371B8`, dark a lighter shade at 4.5:1 or more on slate),
    `#3c7ec9` is used for non-text accents, and the `primary` pair stays `text` 4.5.

## Tasks
All tasks: module `guide`, run one at a time in order T1 -> T2 -> T3 (batches 1, 2, 3). Every task: ASCII only, no em
dashes in docs, no new npm dependency, `guide/docs/**` byte-identical to origin/factory, never skip, disable, delete or
weaken a test or check; every edited `guide/scripts/**` file is listed in the outbox with its reason (the spec 11
checker freeze ended with spec 11; a guard may move or grow, never shrink). Local gates per task: `npm ci`,
`npm run check`, `npm run typecheck`, `npm run build`, `npm run check:built`, `npm run build:offline` in `guide/`, the
browser rig (Playwright + axe-core in a scratchpad folder, three engines, Pages build via `docusaurus serve` and the
offline build over `file://`), and the referee `./mvnw clean verify` from the repo root with
`env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT`.

### T1 Logo asset and navbar (pack items 1, 2, 5)
- dependsOn: []
- fileScope: `guide/static/img/openvalue-logo-light.png`, `guide/static/img/openvalue-logo-dark.png` (new),
  `guide/src/theme/Navbar/Logo/index.tsx` (new, wrap of the theme component), `guide/docusaurus.config.ts`,
  `guide/src/css/custom.css` (navbar block and navbar variables only), `guide/README.md` (logo provenance section),
  `guide/scripts/rules/navbar-built.mjs` (new), `guide/scripts/navbar.test.mjs` (new),
  `guide/scripts/rules/logo.mjs` (new) and `guide/scripts/logo.test.mjs` (new), `guide/scripts/rules/built.mjs`,
  `guide/scripts/check-built.mjs`, `guide/scripts/check.mjs`, `guide/scripts/lib/site.mjs`,
  `guide/scripts/rules/contrast-pairs.mjs`, `guide/scripts/contrast-pairs.test.mjs`.
- acceptanceCriteria:
  1. The two PNGs are produced by the stdlib recipe of res-3 (deck `Shift Left, Look Right.pdf`, page 1, image XObject
     /Im4 with its soft mask, recoloured, no resampling): 422x77 RGBA, alpha 0 in all four corners, byte-identical alpha
     channels, light = only RGB (255,255,255), dark = only RGB (0x1d,0x24,0x2c). The whole PDF never enters the repo.
  2. `guide/README.md` records for each PNG: source deck file name, page 1, how it was made, and its sha256. A new
     check in `npm run check` fails when a PNG under `guide/static/img/` has no README row or its sha256 differs
     (mirror of the font provenance rule), with node:test self-tests including those two mutants.
  3. Navbar left: the logo in front of the title "Crafting your Agentic Harness". The logo link has href exactly
     `https://openvalue.eu`, `target="_blank"`, `rel="noopener noreferrer"` and an accessible name; the logo is not
     inside the title link. The title link still goes to the guide home page (base URL) without a target. The dark
     (slate) logo shows in light mode, the light (white) logo in dark mode, displayed height 32 px, asset path through
     `useBaseUrl`/`ThemedImage` (no hard-coded `/AgenticBookStore/`).
  4. Navbar right: a link to `https://github.com/MaikKingma/AgenticBookStore` (label "GitHub" or an icon with an
     accessible name) and a link "Follow on LinkedIn" to `https://www.linkedin.com/in/maik-kingma/`; both
     `target="_blank"`, `rel` contains `noopener` and `noreferrer`, accessible name present.
  5. Navbar colours (needed so the dark logo is visible in light mode): light mode navbar has a light background with
     slate text; dark mode navbar background is `#1d242c` with light text. Every navbar text and link colour is a
     contrast pair (`text`, 4.5) in both modes; `MINIMUM_PAIR_COUNT` is raised to the new live count; no pair is removed
     or renamed.
  6. A built-HTML rule (`rules/navbar-built.mjs`, on `lib/html.mjs` `elementTree`) runs in `check:built` on every page
     of the default build dir only (never on the fixture pages of the existing `built*.test.mjs` and CLI tests, which
     stay unchanged and green) and asserts criteria 3 and 4 on the real SSR output. node:test self-tests cover a good
     fixture and mutants: wrong href, no target, rel without noopener, no accessible name, logo inside the title link,
     title link with a target, GitHub or LinkedIn link missing; plus one test through the `check-built` entry point.
  7. Offline build: `build:offline` green and the browser rig shows the logo loaded (`naturalWidth > 0`) in light and
     dark on the Pages build and on the offline zip over `file://`; the three external links keep their hrefs.

### T2 Breadcrumb level bar (pack item 3)
- dependsOn: [T1]  (ordering only: shared files `custom.css`, `contrast-pairs*`, `built.mjs`, `check-built.mjs`)
- fileScope: `guide/src/components/LevelProgress.tsx`, `guide/src/components/LevelProgress.module.css`,
  `guide/src/css/custom.css` (`--guide-level-*` variables only), `guide/scripts/rules/contrast-pairs.mjs`,
  `guide/scripts/contrast-pairs.test.mjs`, `guide/scripts/rules/contrast.mjs`, `guide/scripts/contrast.test.mjs`,
  `guide/scripts/rules/level-progress-built.mjs` (new), `guide/scripts/level-progress.test.mjs` (new),
  `guide/scripts/rules/built.mjs`, `guide/scripts/check-built.mjs`.
- acceptanceCriteria:
  1. Page usage `<LevelProgress current={N} />` and the import stay exactly as the structure rule pins them; no
     `guide/docs/**` change.
  2. Markup: `nav[aria-label="Level progress"]` > `ol` > five `li`, Level 0 to Level 4 in order, each with a link to
     its level page (the built href resolves to that page in both builds); stable `data-state="done|current|upcoming"`
     per crumb (no reliance on hashed CSS-module class names). Exactly one element per page carries
     `aria-current="step"`: the current crumb's link. Done crumbs (level < current) have a check mark element with
     `aria-hidden="true"` before the label and an accessible "completed" text; current and upcoming crumbs have no
     check mark. The check mark is written as an escape (ASCII rule).
  3. Look: chevron-shaped crumbs in one row that wraps on narrow screens; no horizontal page scroll at 375 px and
     720 px. Done: green fill, readable label. Current: fill exactly `#3c7ec9`, white label. Upcoming: muted fill and
     muted label. All colours as `--guide-level-*` variables in custom.css (keep the existing variable names that
     REQUIRED pins, re-point their values, add new ones).
  4. Contrast in both modes: done and upcoming labels are `text` pairs (4.5:1). The current label (white on
     `#3c7ec9` = 4.18:1) is WCAG AA large text: font-weight 700 and computed font-size at least 18.67 px (14 pt), and
     its pair uses a new contrast kind `large` (3:1) added to `rules/contrast.mjs` with tests; `large` is allowed only
     for pairs a test lists by name (initially only the current crumb label), so no other pair can drop to 3:1. Crumb
     outlines/separators that convey state are `nontext` pairs (3:1). `MINIMUM_PAIR_COUNT` only grows; the new crumb
     pairs join REQUIRED. axe `color-contrast` in the browser rig reports 0 violations for every crumb state in light
     and dark.
  5. Tests: `level-progress.test.mjs` (node:test, no new dependency) with hand-written built-HTML fixtures for current
     0, 2 and 4 asserting the done / current / upcoming split, plus mutants (two currents, no current, check mark on a
     current or upcoming crumb, missing check mark on a done crumb, wrong order, missing link, wrong nav label). The
     rule `rules/level-progress-built.mjs` runs in `check:built` on the five real level pages of the default build dir
     (current = the page's level) and finds no Level progress nav on non-level pages; one test through the entry point.
  6. The browser rig's LevelProgress check is updated to the new markup and passes in three engines on both builds.

### T3 Deck colours across the site and visual sweep (pack item 4, screenshots)
- dependsOn: [T1, T2]
- fileScope: `guide/src/css/custom.css`, `guide/src/components/*.module.css` (only `var(...)` fixes the sweep finds),
  `guide/scripts/rules/brand.mjs`, `guide/scripts/brand.test.mjs`, `guide/scripts/site.test.mjs`,
  `guide/scripts/rules/contrast-pairs.mjs`, `guide/scripts/contrast-pairs.test.mjs`, `guide/README.md` (Brand section),
  `agent-os/specs/2026-11-10-1610-14-guide-branding/visuals/**` (screenshots and sweep record).
- acceptanceCriteria:
  1. `--ifm-color-primary` is `#3C7EC9` in light and dark, with the six shades derived by the Docusaurus ratios
     (dark #3371B8, darker #306BAE, darkest #28588F, light #508CCF, lighter #5B92D1, lightest #79A7DA). The brand rule
     pins the new primary in both modes (`PRIMARY` and `BRAND` updated to the D18 palette: deck blue `#3C7EC9`, slate
     `#1D242C`, plus the D7 neutrals still in use); the swap tests still prove that any other primary fails (mutant:
     restoring `#014046` fails `npm run check`). Teal `#014046` and mist no longer appear in `guide/src/`.
  2. Text on or in blue meets AA: links, active navbar and sidebar items, tabs and breadcrumbs use a derived shade that
     reaches 4.5:1 on every background they sit on (darker in light mode, lighter in dark mode); filled buttons have
     white text at 4.5:1 or more. The restricted-text rule is kept and extended: `#3C7EC9` may not be the fg or bg of a
     `text` pair (only `nontext` or the named `large` pair).
  3. Dark mode: page, navbar and sidebar backgrounds `#1d242c`, surfaces (code blocks, cards, admonitions, tabs,
     checklists, observation card) in close slate shades, light grey text. Light mode: light background, slate for
     headings where it helps. Admonition accents use the blue family. Montserrat stays.
  4. Every former `var(--ov-teal)` use is re-decided; contrast pairs cover body text, links, buttons, every crumb
     state, admonitions, code, tabs, checklists and the observation card in both modes; the pair count only grows.
  5. `guide/README.md` Brand section describes the D18 palette (deck source, values, contrast ratios); no em dashes.
  6. Visual sweep: all 11 pages in light and dark at 1280 px and 390 px in the browser rig: axe full rule set and
     `color-contrast` 0 violations, no horizontal page scroll, console clean per the spec 11 accepted list. Screenshots
     of one level page (light and dark, 1280 px and 390 px, four PNGs) plus a short sweep record go to the spec
     folder's `visuals/`.

## Gate 1 rewrites (from the QA strategy, binding on the criteria above)

- T1: G-3 (provenance rule covers every file under `guide/static/img/**`, any extension, name and sha256 on one README
  line, witnesses as listed, ok line with the exact count), G-7 (entry-point tests exercise the default-dir switch on
  fixture builds, never read `guide/build`, never skip; a test kills the switch-disabled mutant), G-8 (docs-untouched
  proof by `git diff` against the spec base, never the stale spec 11 hash files), G-9 (pair variables only in `:root`
  or `html[data-theme='dark']`, or each other declaration dispositioned; rendered colours equal the checker's values),
  G-10 (visible logo file per mode, height 32 px, `naturalWidth` 422, links in the 390 px mobile sidebar, no
  horizontal scroll at 375 and 390 px).
- T2: G-2 (large kind guards a to d, if OD-1a option 1), G-6 (muted and wraps made measurable), G-7, G-8, G-9, G-11
  (visible keyboard focus on every crumb link, not clipped by the chevron, 3:1).
- T3: G-1 (`primary` pair disposition and the primary-as-text census, per OD-1b), G-4 (named `--ov-slate-*` tokens,
  light headings `#1D242C`, admonition borders from a listed set, every surface enumerated with its pairs), G-5 (full
  matrix 11 pages x 2 modes x 1280/390 px x 3 engines x 2 builds; screenshots of Level 2 in light and dark at 1280 and
  390 px), G-8, G-9, G-12 (sage kept or dropped explicitly; brand and site tests retargeted literal for literal with an
  old -> new title list, none deleted).

## Decisions

- Model policy (owner, 2026-11-10): implementation agents and per-task verifiers on sonnet, the QA lead and qa-final
  on opus. The QA strategy rates T2 critical and suggests an opus verifier; the owner policy wins, the opus QA lead
  meta-review compensates. Logged by the orchestrator.
- Shaping decisions D-S1 to D-S9: `shape.md`. Logo display height 32 px (navbar default; native 77 px is 2.4x).

## QA feed-forward

From the T1 meta-review (qa-lead ENDORSE r1; details in the QA ledger):

- FF-T1-1 (T2): one inserted test in `guide/scripts/navbar.test.mjs`: two copies of the good logo link in the left part
  of the navbar give exactly `['navbar: index.html: more than one logo link in the navbar (found 2)']` (deepEqual).
  The T2 verifier shows that mutant NM25 (the `logos.length > 1` check removed) and its twin (`> 2`) fail it, that every
  other existing assertion line is unchanged, and that `rules/navbar-built.mjs` keeps its T1 hash.
- FF-T1-2 (T2): T1's entry-point tests run `check-built.mjs` with no argument on fixture builds holding only
  `index.html` and `setup-check.html` and expect exit 0 and `ok   navbar: 2 pages`. If T2's level rule uses the same
  default-dir switch and fails on a missing level page, either the level rule takes its expected pages from a source
  the copied fixture lacks (T2 tests prove both directions) or the fixture builds gain level pages (listed old -> new
  under FF-T1-1). Never by loosening an assertion. The verifier re-runs T1 wiring mutants W1, W2, W3 and W8 after T2.
- FF-T1-3 (T2, T3; replaces C0-6 "N equals grep count"): node:test total at least the previous gate's (T2: 602), no
  titles lost (`comm -23` empty) except listed renames; the `check.mjs` `ascii`, `lab` and `colors` count lines change
  only by the number of new files in that rule's scope, each listed.
- FF-T1-4 (T3): the Level 2 admonition paragraph in axe `incomplete`: compute its ratio from computed styles on the
  final palette, or show it is gone.
- FF-T1-5 (T3): re-decide every T1 navbar variable that uses teal or mist, keep the 8 navbar pairs per mode; light-mode
  hover and active use `var(--ifm-color-primary)` (under A1 `#3371B8`, 5.01:1 on white); keep `--guide-navbar-logo`
  slate `#1D242C` in light and white in dark (`logo.test.mjs` ties it to the PNG pixels); state the D18 value for the
  navbar accent line (still `--ov-blue` `#3D7DC9`).
- FF-T1-6 (T2, T3; aligns the task blocks with A1): T2 items on the `large` kind lapse; `rules/contrast.mjs` and
  `contrast.test.mjs` keep their T1 hashes, `level progress current label` stays `text`, and a current fill of
  `#3C7EC9` must make `npm run check` fail at 4.18. T3 item 1 must-fails: light primary `#014046`, light primary
  `#3C7EC9`, dark primary `#3C7EC9`, dark primary swapped with the light value; drop every mention of `large`.

## Amendments

- A1 (2026-11-10, orchestrator, applying OD-1 resolved; gate re-run self-accepted). Supersedes the conflicting text
  of the T2 and T3 criteria above:
  - T2 AC3: current crumb fill `#3371B8` (the deck blue's derived `-dark` shade), white label at normal size.
  - T2 AC4: the current label is a `text` pair (4.5:1; white on `#3371B8` = 5.01:1) in both modes. No new contrast
    kind: `rules/contrast.mjs` and `contrast.test.mjs` leave T2's fileScope and stay byte-identical. G-2 lapses;
    `level progress current label` keeps kind `text` in REQUIRED.
  - T3 AC1: `--ifm-color-primary` is `#3371B8` in light mode and, in dark mode, a lighter shade of the deck blue that
    reaches 4.5:1 or more on every dark surface it is text on; the six shade variables per mode are taken from the
    deck-blue family (the derivation in `research-docusaurus-navbar-palette.md`, further steps of the same ratios if
    needed) and stay ordered. `BRAND` gains deck blue `#3C7EC9` and slate `#1D242C`; `PRIMARY` pins the two AA
    shades; the swap tests still prove any other primary fails (mutants: restoring `#014046`, and setting exactly
    `#3C7EC9`, both fail `npm run check`).
  - T3 AC2: the `primary` pair stays `text` 4.5; `#3C7EC9` appears only in non-text roles (borders, bars, outlines,
    `nontext` pairs) and the extended restricted rule forbids it as fg or bg of any `text` pair. G-1 reduces to the
    census: a rendered walk of every text node on the 11 pages in both modes (hover and active states, TOC active
    entry, tabs, breadcrumbs, pagination) finds no text in `rgb(60, 126, 201)` and no text on that background.
- A2 (2026-11-10, orchestrator, from the T1 meta-review; gate re-run self-accepted, test-only scope). T2's fileScope
  gains `guide/scripts/navbar.test.mjs`, limited to the FF-T1-1 inserted test and the FF-T1-2 fixture edits (listed
  old -> new). `guide/scripts/rules/navbar-built.mjs` stays byte-identical.

## Residuals for the owner

- R1 pack wording "Every crumb links to its level page, as today": today the crumbs are not links (F3).
- R2 the logo recipe script stays session-local; the README records the steps and hashes (F1).
- R3 OD-1 was decided by the main session under delegated gates: the exact deck blue `#3c7ec9` is used only for
  non-text accents; text and the current crumb use the AA shade `#3371B8` (Maik may overrule).
- R4 at 32 px the "TECH TRIBES" line of the logo is about 10 px tall (OD-2).
