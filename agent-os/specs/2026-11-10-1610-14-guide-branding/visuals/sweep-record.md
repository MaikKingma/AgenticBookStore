# Spec 14 T3: visual sweep record

Implementer run (impl-t3), final palette, Pages build (`docusaurus serve`) and offline zip build over `file://` from a
path with a space (every non-file request aborted and counted, plus one control fetch that moved the counter).
Rig: Playwright 1.65.0 with Chromium 158.0.8127.18, Firefox 159.0, WebKit 27.2, axe-core 4.14.0. The verifier runs its
own sweep; this is the implementer's record.

## Matrix

11 pages (index, setup-check, Level 0 to 4, wrap-up, three reference pages) x light and dark (theme asserted before every
measurement) x 1280 and 390 px x 3 engines x 2 builds = 264 page runs, 44 per engine and build.

Per run: no horizontal scroll (html and body); axe full rule set (92 rules reported); a census of every visible text node
(88 to 387 per page) with its computed colour and the colour under it; hover and active states at 1280 px (content link,
navbar title and links, sidebar link and active link, table of contents link and the active entry after scrolling, tabs,
breadcrumbs, pagination, code copy button); rendered agreement of computed colours with the checker's resolved values
(body, headings, links, navbar, inline code, code block, sidebar, TOC, tabs, breadcrumb, table stripe, the
admonition backgrounds, text and borders); page, navbar, sidebar and menu backgrounds (`rgb(29, 36, 44)` in dark mode,
white in light mode).

## Counts (all six engine and build runs identical unless stated)

| Check | Result |
|---|---|
| theme equals the intended mode | 44 of 44 per run |
| no horizontal scroll | 44 of 44 per run |
| axe `color-contrast` violations | 0 in all 264 runs |
| census: text in `rgb(60, 126, 201)` or on it | 0 in all 264 runs |
| census: text node below its AA ratio (computed) | 0 in all 264 runs |
| rendered agreement | 750 of 750 per run |
| hover, active and TOC active states | 420 per Pages run, 376 per offline run, all >= 4.5:1 and none deck blue |
| console | only the spec 11 FF-5 and FF-37 shapes (below) |
| axe full rule set, 0 violations | 232 of 264 runs; the 32 others are the finding below (and nothing else) |

Finding (pre-existing, not a palette defect, outside the T3 file scope): axe `scrollable-region-focusable` on one table
at 390 px, on level-4, reference/architecture and reference/cheat-sheet, light and dark, in Chromium and WebKit on both
builds (6 runs each) and on level-4 and reference/architecture in Firefox (4 runs each): 32 page runs. A table wider than the screen scrolls inside its own box and is not
keyboard focusable. The same violation is present at the spec tip before T3 (probe on a build of the committed tip, same
three pages). CSS cannot add focusability; wrapping the cells to remove the scroll breaks words (`inter/ac/tion`). The fix
is one `tabindex="0"` on `table` in the existing rehype plugin `src/plugins/rehype-task-list-labels.mjs` (outside the T3
file scope), left for the orchestrator.

## axe `incomplete`, dispositioned

Every `incomplete` entry is rule `color-contrast`, reason "background color could not be determined because it partially
overlaps (or is partially obscured by) another element" (axe cannot decide for wrapped paragraphs, list items and cells).
Each one is dispositioned by the computed-style census of the same run: every text node of the page, those included, is at
least 4.5:1. The Level 2 and Level 1 admonition paragraph "Look up, Maik demos this now ..." (FF-T1-4) is 14.27:1 in
light mode (`rgb(29, 36, 44)` on `rgb(241, 245, 246)`) and 12.83:1 in dark mode (white on `rgb(42, 51, 60)`). Other
entries: paragraphs and cells on the page background, 15.66:1 in both modes.

## Console

Pages build, all engines: nothing. Offline build: Chromium shows per axe run the FF-37 triple (CORS XHR error,
`net::ERR_FAILED`, "Couldn't load preload assets") once, and one `net::ERR_FAILED` of the control fetch (phase `control`).
WebKit shows the FF-5 pair (load and axe phases) and the FF-37 preload warning once per axe run; the FF-5 pair count per
page varies (28 to 258) with the page's prefetch requests. Firefox shows nothing. No other text anywhere.

## Crumbs (FF-T2-3), re-measured with the T2 rig plus two additions

Three engines x both builds x both modes x five level pages (1406 checks per engine, 0 failures): hover label colour of
all five crumbs equals the state's `--guide-level-*-fg`; focus ring band and corners unclipped (3 px, `--guide-level-focus`);
the outline band pixel of every crumb (one pixel inside the top edge) equals its state's border colour, upcoming crumbs
`--guide-level-inactive-border` (`rgb(122, 138, 148)` light, `rgb(111, 127, 138)` dark); the sr-only "completed" box is
1 x 1 px; axe lists all five labels in `passes`.

## Screenshots (Chromium, Pages build, Level 2: done, current and upcoming crumbs)

`level-2-light-1280.png`, `level-2-dark-1280.png`, `level-2-light-390.png`, `level-2-dark-390.png`, taken after the last
CSS change of T3. The right logo shows per mode (slate file on the light bar, white file on the slate bar).
