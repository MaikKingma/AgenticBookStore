# Spec 14: Guide header links, OpenValue logo, breadcrumb level bar and deck colours

> Module: `guide` · Depends on: 11 · Capability: `workshop-guide` · Decision D18

## Why
Maik, 2026-11-10: the guide should look like the keynote deck it accompanies and point people to the repository, to
Maik's LinkedIn and to OpenValue. The level bar should read as a breadcrumb that shows where you are.

## What changes
1. **Navbar, left:** the OpenValue logo in front of the title "Crafting your Agentic Harness". Clicking the logo opens
   https://openvalue.eu in a new tab (`target="_blank"`, `rel="noopener noreferrer"`). Clicking the title still goes to
   the guide's home page.
2. **Navbar, right:** a link to the repository, https://github.com/MaikKingma/AgenticBookStore (GitHub icon or the
   label "GitHub"), and a link "Follow on LinkedIn" to https://www.linkedin.com/in/maik-kingma/. Both open in a new tab
   with `rel="noopener noreferrer"` and have an accessible name.
3. **Level bar (`LevelProgress`)** becomes a breadcrumb: one chevron-shaped crumb per level, Level 0 to Level 4, in a
   row that wraps or scrolls on narrow screens without a horizontal page scroll.
   - Completed levels (lower than the current one): green fill, a check mark before the label, readable text.
   - Current level: solid deck blue `#3c7ec9` with white text, marked `aria-current="step"`.
   - Upcoming levels: slightly grayed out (muted fill and text), still readable (WCAG AA contrast for the label).
   - Every crumb links to its level page, as today. The page usage `<LevelProgress current={N}/>` does not change.
4. **Colours from the deck** ("Shift Left, Look Right" (Maik's local copy), read-only,
   outside the repo; title slide sampled: background `#1d242c`, accent blue `#3c7ec9`):
   - Primary colour (buttons, links, active navbar and sidebar items, admonition accents, the current crumb) is the
     deck blue in both modes, with the `--ifm-color-primary-*` shades derived from it. Text on blue meets AA.
   - Dark mode: page, navbar and sidebar backgrounds in the deck's slate grey (`#1d242c` and close shades for
     surfaces), light grey text.
   - Light mode: light background, the same blue as primary, slate grey for the navbar or headings where it helps.
   - Replace the current teal primary (`#014046`) and any colour that no longer fits, so every page, the tabs, the
     admonitions, the code blocks, the checklists and the observation card look coherent. Montserrat stays (D7).
5. **Logo asset:** extracted from the deck (Maik's choice). The white "OPENVALUE / TECH TRIBES" wordmark on the title
   slide (lower right). Render the page at high resolution (for example macOS `sips -s format png` on the PDF, or any
   tool available without new installs), crop the wordmark, make the background transparent, and commit:
   a light version for dark mode and a dark (slate) version for light mode, PNG, at least 2x the displayed height.
   Record the source (deck file name, page 1) next to the asset the way the fonts record theirs.

## Out of scope
The storefront (`frontend/`, D7 colours) and the bookstore. Page text, level content and the frozen command checker.

## Requirements
- The site builds, `npm run check`, `check:built`, `typecheck`, `build:offline` and the browser checks stay green.
  Never weaken or skip a check. Where a frozen hash, coverage entry or structure rule pins the changed component or
  CSS, update it in the same task, with the reason, so the check still guards the same thing.
- The offline build (zip) shows the logo and the colours too; external links stay external.
- Both colour modes pass a contrast check for body text, links, buttons and every crumb state.
- ASCII only in text files; no em dashes in docs.

## Tests (required)
- A component test (or the guide's existing test style) for `LevelProgress`: for `current` 0, 2 and 4, the right
  crumbs are done (with check mark), current (`aria-current="step"`) and upcoming.
- A built-HTML check that the navbar contains the logo link to https://openvalue.eu with `target="_blank"` and
  `rel` containing `noopener`, the GitHub link and the LinkedIn link.
- Screenshots of one level page in light and dark mode, desktop and phone width, attached to the spec PR.

## Tasks
1. Logo asset and navbar (items 1, 2, 5).
2. Breadcrumb `LevelProgress` (item 3).
3. Deck colours across the site (item 4), then a visual sweep of all 11 pages in both modes.

## Definition of done
- All requirements and tests above green on the spec PR, CI green, screenshots attached.
- After the merge into `factory` the Guide workflow redeploys and https://maikkingma.github.io/AgenticBookStore/
  shows the new header, breadcrumb and colours.
