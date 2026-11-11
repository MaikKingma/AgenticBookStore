# Spec 15 T5: table regions, A4 dropped, full sweep record

Implementer run (impl-t5) on the T5 branch (the spec tip 32d1dcd plus the T5 change). Pages build served by
`docusaurus serve`, offline zip build over `file://` from a path with a space (every non-file request aborted and counted,
plus one control fetch that moved the counter). Rig: Playwright 1.65.0 with Chromium 158.0.8127.18, Firefox 159.0,
WebKit 27.2, axe-core 4.14.0 (installed in a scratchpad folder, not in `guide/package.json`). The verifier and qa-final
run their own sweeps; this is the implementer's record. The rig is not committed (spec 14 R2 and S3 stay an owner decision).

## Rig scripts (scratchpad, evidence prefix s15-t5-impl-)

- `s15-t5-impl-sweep.mjs`: a copy of the spec 14 qa-final sweep `s14-final-qa-r1-sweep.mjs`. The only change is the deleted
  A4 exclusion: the `expected` function (a `scrollable-region-focusable` violation allowed at 390 px on three pages) and its
  filter are gone, and the check reads "axe default rule set (90 rules): 0 violations". `grep -n scrollable` on the script
  prints nothing. The spec 14 originals are untouched.
- `s15-t5-impl-regions.mjs`: new. Per page, mode, width, engine and build it checks the table regions (below).
- `s15-t5-impl-rectcompare.mjs`: cell rects of the task build against the T5 base build.
- `s15-t5-impl-count.mjs`: independent table counts that use none of the checker code.

## 1. Plugin-off control (A4 as recorded)

A build of the same tree with the plugin unregistered (`rehypePlugins: [rehypeTaskListLabels]`), Pages build, swept with the
unchanged sweep in axe-only mode at 390 px on the three pages, light and dark, three engines:

| Engine | `scrollable-region-focusable` | Pages |
|---|---|---|
| Chromium | 6 runs, 1 node each | level-4, reference/architecture, reference/cheat-sheet, light and dark |
| WebKit | 6 runs, 1 node each | the same three pages, light and dark |
| Firefox | 4 runs, 1 node each | level-4 and reference/architecture, light and dark (the cheat sheet table does not overflow there) |

16 hits, the same pages, engines and counts that spec 14 recorded as A4 (6 + 6 + 4). The task build shows none (section 2).

## 2. Full sweep of the task build, no exclusion filter

11 pages (index, setup-check, Level 0 to 4, wrap-up, three reference pages) x light and dark (theme asserted before every
measurement) x 1280 and 390 px x 3 engines x 2 builds = **264 page runs**, 44 per engine and build.

| Check | Result |
|---|---|
| runs | 264 of 264 completed, 6 invocations (3 engines x Pages and offline), 0 FAIL lines, exit 0 each |
| axe default rule set (90 rules per run) | **0 violations in all 264 runs**; `landmark-unique`, `region` and `scrollable-region-focusable` included |
| axe `color-contrast` violations | 0 in all 264 runs |
| theme equals the intended mode | 264 of 264 |
| no horizontal page scroll (html and body) | 264 of 264 (also at 390 px) |
| census: text in deck blue or on it, text below its AA ratio | 0 in all 264 runs |
| rendered agreement of computed colours with the resolved variables | all agree in every run |
| hover, active and TOC states at 1280 px | all at least 4.5:1, none deck blue |
| offline: page requests aborted before the control fetch | 0 in every run, control fetch blocked, counter moved |
| console | only the spec 11 shapes (below) |
| per invocation | Pages 1406 ok, offline 1428 ok, 0 FAIL |

axe `incomplete`: 216 entries over the 264 runs, all rule `color-contrast` (axe cannot decide wrapped text; "background
overlaps another element"). Each is dispositioned by the computed-style census of the same run (every text node at its AA
ratio): none below. No other rule appears in `incomplete`.

Console: Pages build, all engines: nothing. Offline build: Chromium shows the FF-37 triple once per axe run and the control
fetch error (176 messages over 44 runs), WebKit the FF-5 pair and the FF-37 preload warning (24360 messages over 44 runs, the
FF-5 pair count follows the page's prefetch requests), Firefox nothing. Every message matches the spec 11 accepted list;
the sweep fails a run on any other text.

## 3. Table regions in the rendered pages

Rig `s15-t5-impl-regions.mjs`, same matrix (11 pages x 2 modes x 2 widths x 3 engines x 2 builds, 20 table instances per
engine and build, 5 tables on 4 pages: index 1, level-4 1, reference/architecture 2, reference/cheat-sheet 1):

| Check | Result |
|---|---|
| parent of every article table is a `div` with `tabindex="0"`, `role="region"`, a non-empty `aria-label`, the table as its only child, no role or tabindex on the table | all, all six runs |
| labels unique per page | all |
| `getByRole('region', {name})` finds each label once | all |
| `getByRole('table')` count equals the table count (the table keeps its role) | all |
| the table inside a region is `display: table` and no scroll container of its own (one scrollbar) | all |
| Tab reaches the regions in document order | all (20 of 20 regions per run) |
| U11 focus ring: `outline-style` solid, 3px, colour equal to the resolved `--guide-level-focus`, contrast 15.66:1 against the page, `:focus-visible` | all 120 focuses, both modes |
| ring unclipped: a screenshot crop shows the ring colour on all four sides and both top corners, at 1280 and 390 px | all 120 focuses |
| a focused region scrolled into view sits below the sticky navbar (`scroll-margin-top`) | all |
| A8 at 390 px: every overflowing region scrolls on ArrowRight (`scrollLeft` grows) | Chromium and WebKit 10 of 10 (5 tables x 2 modes), Firefox 8 of 8 (the cheat sheet table fits there); one WebKit offline press needed a second ArrowRight (WebKit drops a first key press now and then), reported in the run line |
| A8 at 1280 px: every th and td rect (page coordinates) of every table equals the T5 base build | 1332 cells compared (222 per engine and build), worst difference 0.00 px, 0 beyond 1 px |

Five tables overflow at 390 px in Chromium and WebKit (index Agenda 420 px, level-4 Tracks 511, architecture Packages 597
and Naming 367, cheat sheet 400, all in a 358 px region), four in Firefox. Axe flagged only the three that hold no link;
the others scroll too, and all of them are now regions.

Mutants of the stylesheet, injected as a rule after load on reference/architecture in Chromium (the rig fails each, or the
rect comparison does): `outline: none` (16 FAIL lines), `scroll-margin-top: 0` (16, the region ends under the navbar),
`overflow-x: visible` (10, page scroll 613 > 390), a literal outline colour (16), a 1px outline (16), an inside outline
offset (8), `display: block` on the inner table (4). Losing the region's bottom margin changes no cell rect on the five
tables: it collapses into the margin of the heading that follows each table, so it is an equivalent change on these pages.
Before `scroll-margin-top` existed, the rig failed 10 times in Chromium: a Tab-focused region came to rest under the sticky
navbar, which hid the top of the ring.

## 4. check:built on both builds (A6)

- Pages build: `ok   table regions: 5 tables on 4 pages, each in a focusable region with a name that is unique on its page`.
  The independent count (`s15-t5-impl-count.mjs`: `<table` inside the `<article>` slice of every html file, and wrapper
  `div`s with `tabindex=0 role=region aria-label` directly before a `<table`): 5 tables on 4 pages, 5 wrapped. On the base
  build: 5 tables, 0 wrapped.
- Offline build: `node scripts/check-built.mjs build-offline` exits 0 and prints no table line: the offline build is
  client rendered (one `index.html`, hash router, no static rendering), so its html holds no table and the static rule has
  nothing to read (0 tables, equal to the independent html count of 0). The count that exists there is in the compiled MDX of
  the js chunks: 5 table elements and 5 region wrappers (base build: 5 and 0), and the rendered rig above checked the DOM of the
  offline build: 20 table instances in 44 runs per engine, every one inside a region, all three engines.

## 5. Gates

Local gates and the referee are listed in the outbox artifact of the task (impl-t5), with their tails.
